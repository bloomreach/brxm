/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cm.engine.autoexport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.onehippo.cm.engine.ExportContentProcessor;
import org.onehippo.cm.model.Definition;
import org.onehippo.cm.model.DefinitionItem;
import org.onehippo.cm.model.DefinitionNode;
import org.onehippo.cm.model.NamespaceDefinition;
import org.onehippo.cm.model.PropertyOperation;
import org.onehippo.cm.model.Source;
import org.onehippo.cm.model.SourceType;
import org.onehippo.cm.model.impl.AbstractDefinitionImpl;
import org.onehippo.cm.model.impl.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.ConfigSourceImpl;
import org.onehippo.cm.model.impl.ConfigurationItemImpl;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ConfigurationNodeImpl;
import org.onehippo.cm.model.impl.ConfigurationPropertyImpl;
import org.onehippo.cm.model.impl.ConfigurationTreeBuilder;
import org.onehippo.cm.model.impl.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.DefinitionItemImpl;
import org.onehippo.cm.model.impl.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.DefinitionPropertyImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.NamespaceDefinitionImpl;
import org.onehippo.cm.model.impl.SourceImpl;
import org.onehippo.cm.model.impl.ValueImpl;
import org.onehippo.cm.model.util.FilePathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.onehippo.cm.engine.autoexport.Constants.DEFAULT_MAIN_CONFIG_FILE;
import static org.onehippo.cm.model.Constants.YAML_EXT;
import static org.onehippo.cm.model.DefinitionType.NAMESPACE;
import static org.onehippo.cm.model.PropertyOperation.OVERRIDE;

public class DefinitionMergeService {

    private static final Logger log = LoggerFactory.getLogger(DefinitionMergeService.class);

    private static class ModuleMapping {
        final String mvnPath;
        final Collection<String> repositoryPaths;

        final PatternSet pathPatterns;

        ModuleMapping(final String mvnPath, final Collection<String> repositoryPaths) {
            this.mvnPath = mvnPath;
            this.repositoryPaths = repositoryPaths;

            List<String> patterns = new ArrayList<>();
            for (String repositoryPath : repositoryPaths) {
                patterns.add(repositoryPath);
                patterns.add(repositoryPath.equals("/") ? "/**" : repositoryPath + "/**");
            }
            pathPatterns = new PatternSet(patterns);
        }

        boolean matchesPath(String path) {
            return pathPatterns.matches(path);
        }
    }

    private ModuleMapping defaultModuleMapping;
    private final HashMap<String, ModuleMapping> moduleMappings = new HashMap<>();

    /**
     * @param autoExportConfig the current auto-export module config, which includes module:path mappings and exclusions
     */
    public DefinitionMergeService(final Configuration autoExportConfig) {
        // preprocess config mapping paths to moduleMapping objects
        // note: this is very similar to the old auto-export EventProcessor init
        for (Map.Entry<String, Collection<String>> entry : autoExportConfig.getModules().entrySet()) {
            String modulePath = entry.getKey();
            Collection<String> repositoryPaths = entry.getValue();
            ModuleMapping mapping = new ModuleMapping(modulePath, repositoryPaths);
            if (repositoryPaths.contains("/")) {
                defaultModuleMapping = mapping;
            } else {
                moduleMappings.put(mapping.mvnPath, mapping);
            }
        }
    }

    /**
     * Given a baseline B, a set of changes to that baseline in the current JCR runtime (R) R∆B expressed as a
     * ModuleImpl, and a set of destination modules Sm, produce a new version of the destination modules Sm' such that
     * B-Sm+Sm' = B+R∆B. Also, make a best effort for Sources and Definitions in Sm' to be as minimally changed compared
     * to the corresponding Sources and Definitions in Sm as possible (for stable output), and for any new Sources and
     * Definitions to follow the sorting schemes encoded in org.onehippo.cms7.autoexport.LocationMapper.
     * @param changes R∆B expressed as a Module with one ConfigSource with zero-or-more Definitions and zero-or-more ContentSources
     * @param baseline the currently stored configuration baseline B
     * @param jcrSession JCR session to be used for regenerating changed content sources
     * @return a new version of each toMerge module, if revisions are necessary
     */
    public Collection<ModuleImpl> mergeChangesToModules(final ModuleImpl changes,
                                                        final EventJournalProcessor.Changes contentChanges,
                                                        final ConfigurationModelImpl baseline,
                                                        final Session jcrSession) {
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // find the modules that are configured for auto-export and also have a mvnPath indicating a source location
        final Set<String> configuredMvnPaths = new HashSet<>();
        configuredMvnPaths.addAll(moduleMappings.keySet());
        configuredMvnPaths.add(defaultModuleMapping.mvnPath);

        final HashMap<String, ModuleImpl> toExport = new HashMap<>();
        for (final ModuleImpl m : baseline.getModules()) {
            if (m.getMvnPath() != null && configuredMvnPaths.contains(m.getMvnPath())) {
                toExport.put(m.getMvnPath(), m);
            }
        }

        log.debug("Merging changes to modules: {}", toExport.values());
        log.debug("Content added: {} changed: {}", contentChanges.getAddedContent(), contentChanges.getChangedContent());

        // make sure the changes module has all the definitions nicely sorted
        changes.build();

        // handle namespaces before rebuilding, since we want any validation to happen after this
        for (final NamespaceDefinitionImpl nsd : changes.getNamespaceDefinitions()) {
            mergeNamespace(nsd, toExport, baseline);
        }

        // TODO does it make sense to auto-export webfilebundle definitions?

        // merge config changes
        // ConfigDefinitions are already sorted by root path
        for (final ConfigDefinitionImpl change : changes.getConfigDefinitions()) {
            // run the full and complex merge logic, recursively
            mergeConfigDefinitionNode(change.getNode(), toExport, baseline);
        }

        // merge content changes
        mergeContentDefinitions(changes, contentChanges, toExport, jcrSession);

        stopWatch.stop();
        log.info("Completed full auto-export merge in {}", stopWatch.toString());

        return toExport.values();
    }

    /**
     * Merge a single namespace definition into the appropriate toExport module.
     * @param nsd the definition to merge
     * @param toExport modules that may be merged into
     * @param baseline a complete ConfigurationModel baseline for context
     */
    protected void mergeNamespace(final NamespaceDefinitionImpl nsd, final HashMap<String, ModuleImpl> toExport, final ConfigurationModelImpl baseline) {
        // find the corresponding definition by namespace prefix -- only one is permitted
        final Optional<NamespaceDefinitionImpl> found = baseline.getNamespaceDefinitions().stream()
                .filter(namespaceDefinition -> namespaceDefinition.getPrefix().equals(nsd.getPrefix()))
                .findFirst();

        // clone the CndPath Value which retains a "foreign source" back-reference for use later when copying data
        final ValueImpl cndPath = nsd.getCndPath().clone();

        if (found.isPresent()) {
            // this is an update to an existing namespace def
            // find the corresponding source by path
            final Source oldSource = found.get().getSource();

            // this source will have a reference to a module in the baseline, not our clones, so lookup by mvnPath
            final ModuleImpl newModule = toExport.get(((ModuleImpl)oldSource.getModule()).getMvnPath());

            // short-circuit this loop iteration if we cannot create a valid merged definition
            if (newModule == null) {
                log.error("Cannot merge a namespace: {} that belongs to an upstream module", nsd.getPrefix());
                return;
            }

            final String oldSourcePath = oldSource.getPath();
            final SourceImpl newSource = newModule.getModifiableSources().stream()
                    .filter(SourceType.CONFIG::isOfType)
                    .filter(source -> source.getPath().equals(oldSourcePath))
                    .findFirst().get();

            log.debug("Merging namespace definition: {} to module: {} aka {} in file {}",
                    nsd.getPrefix(), newModule.getMvnPath(), newModule.getFullName(), newSource.getPath());

            final List<AbstractDefinitionImpl> defs = newSource.getModifiableDefinitions();
            for (int i = 0; i < defs.size(); i++) {
                Definition def = defs.get(i);

                // find the corresponding def within the source by type and namespace prefix
                if (def.getType().equals(NAMESPACE) && ((NamespaceDefinition)def).getPrefix().equals(nsd.getPrefix())) {
                    // replace the def with a clone of the new def
                    final NamespaceDefinitionImpl newNsd =
                            new NamespaceDefinitionImpl(newSource, nsd.getPrefix(), nsd.getURI(), cndPath);
                    defs.set(i, newNsd);
                    newSource.markChanged();
                }
            }
        }
        else {
            // this is a new namespace def -- pretend that it is a node under /hippo:namespaces for sake of file mapping
            final String incomingPath = "/hippo:namespaces/" + nsd.getPrefix();

            // what module should we put it in?
            final ModuleImpl newModule = getModuleByAutoExportConfig(incomingPath, toExport);

            // what source should we put it in?
            final ConfigSourceImpl newSource;
            if (newModule.getNamespaceDefinitions().isEmpty()) {
                // We don't have any namespaces yet, so we can generate a new source and put it there
                newSource = createConfigSourceIfNecessary(DEFAULT_MAIN_CONFIG_FILE, newModule);
            }
            else {
                // if we have any existing namespace definitions in the destination module, we have to keep all
                // new namespaces in that same source file, due to validation rules on our model
                newSource = newModule.getNamespaceDefinitions().get(0).getSource();
            }

            log.debug("Creating new namespace definition: {} in module: {} aka {} in file {}",
                    nsd.getPrefix(), newModule.getMvnPath(), newModule.getFullName(), newSource.getPath());

            newSource.addNamespaceDefinition(nsd.getPrefix(), nsd.getURI(), cndPath);
        }
    }

    /**
     * Create a new ConfigurationModel with the updated definitions in the toExport modules.
     * @param toExport replacement modules that should override what is in the baseline
     * @param baseline the existing model upon which we'll base the new one
     * @return the new ConfigurationModel, which references Sources from the old Modules in baseline and toExport
     */
    protected static ConfigurationModelImpl rebuild(final HashMap<String, ModuleImpl> toExport,
                                                    final ConfigurationModelImpl baseline) {
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // note: we assume that the original baseline will perform any required cleanup in close(), so we don't need
        //       to copy FileSystems etc. here
        final ConfigurationModelImpl model = new ConfigurationModelImpl();
        toExport.values().forEach(model::addModule);
        baseline.getSortedGroups().forEach(model::addGroup);
        model.build();

        stopWatch.stop();
        log.debug("Model rebuilt for auto-export merge in {}", stopWatch.toString());
        return model;
    }

    protected void mergeConfigDefinitionNode(final DefinitionNodeImpl incomingDefNode,
                                                               final HashMap<String, ModuleImpl> toExport,
                                                               ConfigurationModelImpl model) {
        log.debug("Merging config change for path: {}", incomingDefNode.getPath());

        final boolean nodeIsNew = isNewNodeDefinition(incomingDefNode);
        if (nodeIsNew) {
            createNewNode(incomingDefNode, toExport, model);
        }
        else {
            // if the incoming node is not new, we should expect its path to exist -- find it
            final String incomingDefPath = incomingDefNode.getPath();
            final ConfigurationNodeImpl incomingConfigNode = model.resolveNode(incomingDefPath);

            if (incomingConfigNode == null) {
                throw new IllegalStateException("Cannot modify a node that doesn't exist in baseline: " + incomingDefPath);
            }

            log.debug("Changed path has existing definition: {}", incomingDefPath);

            // is this a delete?
            if (incomingDefNode.isDelete()) {
                // handle node delete
                final DefinitionNodeImpl deleteDef = deleteNode(incomingDefNode, incomingConfigNode, toExport);

                // incremental update of model
                new ConfigurationTreeBuilder(model.getConfigurationRootNode())
                        .markNodeAsDeletedBy(incomingConfigNode, deleteDef).pruneDeletedItems(incomingConfigNode);

                // don't bother checking any other properties or children -- they're gone
                return;
            }

            // todo: handle new order-before!

            // handle properties, then child nodes
            for (final DefinitionPropertyImpl defProperty : incomingDefNode.getProperties().values()) {
                // handle properties on an existing node
                mergeProperty(defProperty, incomingConfigNode, toExport, model);
            }

            // any child node here may or may not be new -- do full recursion
            for (DefinitionNodeImpl childNodeDef : incomingDefNode.getNodes().values()) {
                mergeConfigDefinitionNode(childNodeDef, toExport, model);
            }
        }
    }

    /**
     * Does this DefinitionNode represent a newly-created node, or does it reference an existing node defined upstream?
     * @param defNode a DefinitionNode to test for newness
     * @return true iff this is a reference to a brand-new node, not referenced upstream yet
     */
    protected boolean isNewNodeDefinition(final DefinitionNodeImpl defNode) {
        // the node is a new node iff a jcr:primaryType is defined here and it's not an override or delete
        return !defNode.isDelete()
                && defNode.getProperty(JCR_PRIMARYTYPE) != null
                && !defNode.getProperty(JCR_PRIMARYTYPE).getOperation().equals(OVERRIDE);
    }

    /**
     * Create a new DefinitionNode in one of the toExport modules for a brand-new node, not mentioned anywhere else.
     * @param incomingDefNode the DefinitionNode from the diff that we are merging
     * @param toExport the modules to which we are merging
     * @param model the full ConfigurationModel, which references Sources from the old Modules in baseline and toExport
     */
    protected void createNewNode(final DefinitionNodeImpl incomingDefNode, final HashMap<String, ModuleImpl> toExport,
                                 final ConfigurationModelImpl model) {
        // if the incoming node path is new, we should expect its parent to exist -- find it
        final String incomingPath = incomingDefNode.getPath();
        final String parentPath = StringUtils.substringBeforeLast(incomingPath, "/");
        final ConfigurationNodeImpl existingParent = model.resolveNode(parentPath);

        if (existingParent == null) {
            throw new IllegalStateException("Cannot add a node whose parent doesn't exist in baseline: " + incomingPath);
        }

        log.debug("Changed path is newly defined: {}", incomingPath);

        // does LocationMapper think that this path should be a new context path?
        // if so, create a new ConfigDefinition rather than attempting to add to an existing one
        if (shouldPathCreateNewSource(incomingPath)) {
            // we don't care if there's an existing def -- LocationMapper is making us split to a new file
            // TODO should this take into account the modules where siblings are defined, to handle ordering properly?
            final DefinitionNodeImpl newDef = createNewDef(incomingDefNode, true, toExport);

            // update model
            new ConfigurationTreeBuilder(model.getConfigurationRootNode())
                    .push(newDef.getDefinition());
        }
        else {
            // where was the parent node mentioned?
            // is one of the existing defs for the parent in the toMerge modules? grab the last one
            // TODO if there is more than one mention in toMerge, should we prefer the def with jcr:primaryType?
            final Optional<DefinitionNodeImpl> maybeDef = getLastLocalDef(existingParent, toExport);

            // TODO should we attempt any kind of sorting on output? current behavior is append, with history-dependent output
            // TODO i.e. the sequence of changes to the repository and the timing of auto-export will produce different files
            // TODO also, definition ordering will likely not match order implied by .meta:order-before
            if (maybeDef.isPresent()) {
                // since we have a parent defNode in a valid module, use that for this new child
                final DefinitionNodeImpl parentDefNode = maybeDef.get();

                // we know that this is the only place that mentions this node, because it's new
                // -- put all descendent properties and nodes in this def
                final DefinitionNodeImpl newDefNode = recursiveAdd(incomingDefNode, parentDefNode, toExport);

                // update model
                final ConfigurationTreeBuilder builder = new ConfigurationTreeBuilder(model.getConfigurationRootNode());
                final ConfigurationNodeImpl newConfigNode = builder.createChildNode(existingParent, newDefNode.getName(), newDefNode);
                builder.mergeNode(newConfigNode, newDefNode);
            }
            else {
                // there's no existing parent defNode that we can reuse, so we need a new definition
                // TODO should this take into account the modules where siblings are defined, to handle ordering properly?
                final DefinitionNodeImpl newDef = createNewDef(incomingDefNode, true, toExport);

                // update model
                new ConfigurationTreeBuilder(model.getConfigurationRootNode())
                        .push(newDef.getDefinition());
            }
        }
    }

    /**
     * Find the module within toExport that should match the given path according to the AutoExport module config.
     * @param path the path to test
     * @param toExport the set of Modules being merged here and eventually to be exported
     * @return a single Module that represents the best match for this path
     */
    protected ModuleImpl getModuleByAutoExportConfig(final String path, final HashMap<String, ModuleImpl> toExport) {
        // TODO extra logic from EventProcessor.getModuleForPath() and getModuleForNSPrefix()
        return moduleMappings.values().stream()
                .filter(mapping -> mapping.matchesPath(path))
                .map(mapping -> toExport.get(mapping.mvnPath))
                .findFirst().orElseGet(()->toExport.get(defaultModuleMapping.mvnPath));
    }

    /**
     * Get a source within a given module to use when adding a new node definition. The source is chosen based on
     * the conventions from the AutoExport LocationMapper class, and it might in theory already exist.
     * @param path the JCR path of the new node we'll be defining
     * @param module the module where we want this definition to live
     * @return a new or existing ConfigSourceImpl
     */
    protected ConfigSourceImpl getSourceForNewConfig(final String path, final ModuleImpl module) {
        // what does LocationMapper say?
        final String sourcePath = getFilePathByLocationMapper(path);
        return createConfigSourceIfNecessary(sourcePath, module);
    }

    /**
     * Get a source within a given module to use when adding a new node definition, creating it if necessary.
     * @param sourcePath the desired module-config-root-relative path
     * @param module the module where we want this definition to live
     * @return a Source that can be used to create new definitions
     */
    protected ConfigSourceImpl createConfigSourceIfNecessary(final String sourcePath, final ModuleImpl module) {
        // does this Source already exist?
        return module.getConfigSource(sourcePath)
                // if not, add it
                .orElseGet(() -> module.addConfigSource(sourcePath));
    }

    /**
     * Does LocationMapper think that this path should be defined in a new context root path in a new file?
     * @param incomingPath the JCR node path to test
     * @return true iff this path should go in a new file, different than its parent node
     */
    protected static boolean shouldPathCreateNewSource(final String incomingPath) {
        return LocationMapper.contextNodeForPath(incomingPath, true).equals(incomingPath);
    }

    /**
     * Lookup the file path that the old AutoExport LocationMapper class would recommend for the given JCR path,
     * then adjust it to use a YAML extension instead of an XML extension.
     * @param path the JCR path for which we want to generate a new source file
     * @return a module-base-relative path with no leading slash for a potentially new yaml source file
     */
    protected String getFilePathByLocationMapper(String path) {
        String xmlFile = LocationMapper.fileForPath(path, true);
        if (xmlFile == null) {
            return "main.yaml";
        }
        return StringUtils.removeEnd(xmlFile, ".xml") + YAML_EXT;
    }

    /**
     * Create a new ConfigDefinition to contain the contents of the given DefinitionNode, which may be copied here.
     * When copying, this will also create new definitions in new source files for descendant nodes as determined via
     * {@link #shouldPathCreateNewSource(String)}.
     * @param incomingDefNode a DefinitionNode that will be copied to form the content of the new ConfigDefinition
     * @param copyContents should the contents of the incomingDefNode be recursively copied into the new def?
     */
    protected DefinitionNodeImpl createNewDef(final DefinitionNodeImpl incomingDefNode, final boolean copyContents,
                                final HashMap<String, ModuleImpl> toExport) {
        final String incomingPath = incomingDefNode.getPath();

        log.debug("Creating new top-level definition for path: {} ...", incomingPath);

        // what module should we put it in?
        // TODO should this take into account the modules where siblings are defined, to handle ordering properly?
        final ModuleImpl destModule = getModuleByAutoExportConfig(incomingPath, toExport);

        // what source should we put it in?
        final ConfigSourceImpl destSource = getSourceForNewConfig(incomingPath, destModule);

        log.debug("... stored in {}/hcm-config/{}", destModule.getName(), destSource.getPath());

        // create the new ConfigDefinition and add it to the source
        // we know that this is the only place that mentions this node, because it's new
        // -- put all descendent properties and nodes in this def
        //... but when we create the def, make sure to walk up until we don't have an indexed node in the def root
        final DefinitionNodeImpl newRootNode = destSource.getOrCreateDefinitionFor(incomingDefNode.getPath());

        if (copyContents) {
            recursiveCopy(incomingDefNode, newRootNode, toExport);
        }
        return newRootNode;
    }

    /**
     * Recursively copy the new def as a child-plus-descendants of this node.
     * This will also create new definitions in new source files for descendant nodes as determined via
     * {@link #shouldPathCreateNewSource(String)}.
     * @param from the definition we want to copy as a child of toParent
     * @param toParent the parent of the desired new definition node
     * @return the newly created child node, already populated with properties and descendants
     */
    protected DefinitionNodeImpl recursiveAdd(final DefinitionNodeImpl from, final DefinitionNodeImpl toParent,
                                           final HashMap<String, ModuleImpl> toExport) {
        log.debug("Adding new node definition to existing definition: {}", from.getPath());

        // mark source changed
        toParent.getDefinition().getSource().markChanged();

        // if order-before is set, we need to do an insert, not an add-at-end
        final DefinitionNodeImpl to;
        if (from.getOrderBefore() != null) {
            log.debug("Inserting before: {}", from.getOrderBefore());

            to = toParent.addNodeBefore(from.getName(), from.getOrderBefore());

            // if the new node does not have order-before set, we don't need to copy and keep order-before anymore
            if (to.getOrderBefore() == null) {
                from.setOrderBefore(null);
            }
            else {
                log.debug("Couldn't find node that we want to insert before");
            }
        }
        else {
            to = toParent.addNode(from.getName());
        }
        recursiveCopy(from, to, toExport);
        return to;
    }

    /**
     * Recursively copy all properties and descendants of the from-node to the to-node.
     * Creates new definitions as required by LocationMapper.
     * @param from the definition we want to copy
     * @param to the definition we are copying into
     */
    protected void recursiveCopy(final DefinitionNodeImpl from, final DefinitionNodeImpl to,
                                 final HashMap<String, ModuleImpl> toExport) {
        if (from.isDelete()) {
            // delete clears everything, so there's no point continuing with other properties or recursion
            to.delete();
            return;
        }

        to.setOrderBefore(from.getOrderBefore());
        to.setIgnoreReorderedChildren(from.getIgnoreReorderedChildren());

        // copy properties using special method that migrates resources properly
        for (final DefinitionPropertyImpl fromProperty : from.getProperties().values()) {
            to.addProperty(fromProperty);
        }

        // TODO do we need to sort accounting for order-before, or does the diff step order things w/o explicit order-before?
        for (final DefinitionNodeImpl childNode : from.getNodes().values()) {
            // for each new childNode, we need to check if LocationMapper wants a new source file
            final String incomingPath = childNode.getPath();
            if (shouldPathCreateNewSource(incomingPath)) {
                // yes, we need a new definition in a new source file
                // TODO should this take into account the modules where siblings are defined, to handle ordering properly?
                createNewDef(childNode, true, toExport);
            } else {
                // no, just keep adding to the current destination defNode
                recursiveAdd(childNode, to, toExport);
            }
        }
    }

    /**
     * Handle a diff entry indicating that a single node should be deleted.
     * @param defNode a DefinitionNode from the diff, describing a single to-be-deleted node
     * @param configNode the ConfigurationNode corresponding to the to-be-deleted node in the current config model
     */
    protected DefinitionNodeImpl deleteNode(final DefinitionNodeImpl defNode, final ConfigurationNodeImpl configNode,
                              final HashMap<String, ModuleImpl> toExport) {
        log.debug("Deleting node: {}", defNode.getPath());

        final List<DefinitionNodeImpl> defsForConfigNode = configNode.getDefinitions();

        // if last existing node def is upstream,
        final boolean lastDefIsUpstream = !isLastDefLocal(defsForConfigNode, toExport);
        if (lastDefIsUpstream) {
            log.debug("Last def for node is upstream of export: {}", defNode.getPath());

            // create new defnode w/ delete
            final DefinitionNodeImpl newDef = createNewDef(defNode, true, toExport);

            // we know that there was no local def for the node we're deleting, but there may be defs for its children
            // so for all descendants, remove all definitions and possibly sources
            removeDescendantDefinitions(configNode, new ArrayList<>(), toExport);

            return newDef;
        }
        else {
            // there are local node defs for this node
            // are there ONLY local node defs?
            final List<DefinitionNodeImpl> localDefs = getLocalDefs(defsForConfigNode, toExport);
            final boolean onlyLocalDefs = (localDefs.size() == defsForConfigNode.size());
            if (onlyLocalDefs) {
                log.debug("Only local defs for node: {}", defNode.getPath());

                // since there's only local defs, we want this node to disappear from the record completely
                // i.e. "some" = "all" defs, in this case
                removeSomeDefsAndDescendants(configNode, defsForConfigNode, new ArrayList<>(), toExport);

                return defNode;
            }
            else {
                log.debug("Both local and upstream defs for node: {}", defNode.getPath());

                // since there's also an upstream def, we want to collapse all local references to a single delete def
                // if exists, change one local def to delete and remove other properties and subnodes
                final DefinitionNodeImpl defToKeep = (DefinitionNodeImpl) localDefs.get(0);

                // mark chosen node as a delete
                final ConfigDefinitionImpl defToKeepDefinition = (ConfigDefinitionImpl) defToKeep.getDefinition();
                log.debug("Marking delete on node: {} from definition of: {} in source: {}",
                        defToKeep.getPath(),
                        defToKeepDefinition.getNode().getPath(),
                        defToKeepDefinition.getSource().getPath());
                defToKeep.delete();
                defToKeepDefinition.getSource().markChanged();

                // remove all other defs and children (but not the first one, that we are keeping)
                final List<DefinitionNodeImpl> localDefsExceptFirst = localDefs.subList(1, localDefs.size());
                removeSomeDefsAndDescendants(configNode, localDefsExceptFirst, new ArrayList<>(), toExport);

                return defToKeep;
            }
        }
    }

    /**
     * Remove some (perhaps all) definitions associated with configNode, plus all definitions associated with
     * descendants of configNode. Recurs down the ConfigurationNode tree and up the DefinitionItem tree(s).
     * Call this method to start the entire removal process, possibly preserving one or more existing definitions.
     * @param configNode the configNode whose descendants we're deleting
     * @param defsToRemove the subset of configNode.getDefinitions() that we want to delete
     * @param alreadyRemoved an accumulator for Definitions whose children we don't have to check,
     *                           because the root is already gone
     * @param toExport modules we're merging/exporting -- changes should stay inside this scope
     */
    protected void removeSomeDefsAndDescendants(final ConfigurationNodeImpl configNode,
                                                final List<? extends DefinitionItemImpl> defsToRemove,
                                                final List<AbstractDefinitionImpl> alreadyRemoved,
                                                final HashMap<String, ModuleImpl> toExport) {
        log.debug("Removing defs and children for node: {} with exceptions: {}", configNode.getPath(), alreadyRemoved);

        for (final DefinitionItemImpl definitionItem : defsToRemove) {
            removeOneDefinitionItem(definitionItem, alreadyRemoved, toExport);
        }

        // we don't need to handle properties specifically, because we will remove all the nodes that contain them

        // scan downwards for child definitions, which could be rooted on the children directly
        removeDescendantDefinitions(configNode, alreadyRemoved, toExport);
    }

    /**
     * Remove child definitions from the given configNode, because configNode is being deleted.
     * This method recurs down the ConfigurationNode tree, and then recurs up the DefinitionNode tree(s) to clean up
     * any parent DefinitionNodes, Definitions, or Sources that may have been made empty.
     * @param configNode the node being deleted
     * @param alreadyRemoved an accumulator for Definitions whose children we don't have to check,
     *                           because the root is already gone
     * @param toExport modules we're merging/exporting -- changes should stay inside this scope
     */
    protected void removeDescendantDefinitions(final ConfigurationNodeImpl configNode,
                                               final List<AbstractDefinitionImpl> alreadyRemoved,
                                               final HashMap<String, ModuleImpl> toExport) {
        log.debug("Removing child defs for node: {} with exceptions: {}", configNode.getPath(), alreadyRemoved);

        for (final ConfigurationNodeImpl childConfigNode : configNode.getNodes().values()) {
            for (final DefinitionNodeImpl childDefItem : childConfigNode.getDefinitions()) {
                // if child's DefinitionNode was part of a parent Definition, it may have already been removed
                final AbstractDefinitionImpl childDefinition = childDefItem.getDefinition();
                if (!alreadyRemoved.contains(childDefinition)) {
                    // otherwise, remove it now
                    removeOneDefinitionItem(childDefItem, alreadyRemoved, toExport);
                }
            }
            removeDescendantDefinitions(childConfigNode, alreadyRemoved, toExport);
        }
    }

    /**
     * Remove one definition item, either by removing it from its parent or (if root) removing the entire definition.
     * Recurs up the DefinitionItem tree to clean up newly-emptied items.
     * @param definitionItem the node or property to remove
     * @param alreadyRemoved an accumulator for Definitions whose children we don't have to check,
     *                           because the root is already gone
     * @param toExport modules we're merging/exporting -- changes should stay inside this scope
     */
    protected void removeOneDefinitionItem(final DefinitionItemImpl definitionItem,
                                           final List<AbstractDefinitionImpl> alreadyRemoved,
                                           final HashMap<String, ModuleImpl> toExport) {

        log.debug("Removing one def item for node: {} with exceptions: {}", definitionItem.getPath(), alreadyRemoved);

        // remove the node itself
        // if this node is the root
        if (definitionItem.isRoot()) {
            // remove the definition
            final ConfigDefinitionImpl definition = (ConfigDefinitionImpl) definitionItem.getDefinition();
            removeDefinition(definition, toExport);
            alreadyRemoved.add(definition);
        }
        else {
            // otherwise, remove from parent
            removeFromParentDefinitionItem(definitionItem, alreadyRemoved, toExport);
        }

    }

    /**
     * Remove a DefinitionItem from its parent. This method assumes that you've already checked that the parent exists.
     * Recurs up the DefinitionItem tree to clean up newly-emptied items.
     * @param definitionItem the node or property to remove
     * @param alreadyRemoved an accumulator for Definitions whose children we don't have to check,
     *                           because the root is already gone
     * @param toExport modules we're merging/exporting -- changes should stay inside this scope
     */
    protected void removeFromParentDefinitionItem(final DefinitionItemImpl definitionItem,
                                                  final List<AbstractDefinitionImpl> alreadyRemoved,
                                                  final HashMap<String, ModuleImpl> toExport) {
        final ConfigDefinitionImpl definition = (ConfigDefinitionImpl) definitionItem.getDefinition();
        final SourceImpl source = definition.getSource();
        final ModuleImpl module = source.getModule();

        // check if the definition is in one of the toExport modules -- if not, we can't change it
        if (!toExport.containsValue(module)) {
            throw new IllegalStateException
                    ("Cannot change a definition from module that is not being merged: " + module.getFullName()
                            + " for node: " + definitionItem.getPath());
        }
        log.debug("Removing definition item for: {} from definition of: {} in source: {}",
                definitionItem.getPath(),
                definition.getNode().getPath(), source.getPath());

        final DefinitionNodeImpl parentNode = definitionItem.getParent();
        if (definitionItem instanceof DefinitionNode) {
            // remove the node from its parent
            // todo: one of very few remaining uses of getModifiableNodes()
            parentNode.getModifiableNodes().remove(definitionItem.getName());

            // remove referenced resources
            removeResources((DefinitionNodeImpl) definitionItem);
        }
        else {
            // remove the property from its parent
            // todo: one of very few remaining uses of getModifiableProperties()
            parentNode.getModifiableProperties().remove(definitionItem.getName());

            // remove referenced resources
            removeResources((DefinitionPropertyImpl) definitionItem);
        }
        source.markChanged();

        // if this was the last item in the parent node ...
        if (parentNode.isEmpty()) {
            // ... remove the parent node and keep moving up
            removeOneDefinitionItem(parentNode, alreadyRemoved, toExport);
        }
    }

    /**
     * Remove an entire Definition, and if it is the last Definition in its Source, also remove the Source.
     * @param definition the definition to remove
     * @param toExport modules we're merging/exporting -- changes should stay inside this scope
     */
    protected void removeDefinition(final ConfigDefinitionImpl definition, final HashMap<String, ModuleImpl> toExport) {
        // remove the definition from its source and from its module
        final SourceImpl source = definition.getSource();
        final ModuleImpl module = source.getModule();

        // check if the definition is in one of the toExport modules -- if not, we can't change it
        if (!toExport.containsValue(module)) {
            throw new IllegalStateException
                    ("Cannot remove a definition from module that is not being merged: " + module.getFullName()
                            + " for node: " + definition.getNode().getPath());
        }
        log.debug("Removing definition for node: {} from source: {}", definition.getNode().getPath(), source.getPath());

        source.removeDefinition(definition);
        module.getConfigDefinitions().remove(definition);

        // remove referenced resources
        removeResources(definition.getNode());

        // if the definition was the last one from its source
        if (source.getModifiableDefinitions().size() == 0) {
            log.debug("Removing source: {}", source.getPath());

            // remove the source from its module
            module.getModifiableSources().remove(source);
            module.addConfigResourceToRemove("/" + source.getPath());
        }
    }

    /**
     * Merge an incoming property change into toExport modules.
     * @param defProperty the incoming property change
     * @param configNode the ConfigurationNode representing the parent node of defProperty
     * @param toExport modules we're merging/exporting -- changes should stay inside this scope
     * @param model
     */
    protected void mergeProperty(final DefinitionPropertyImpl defProperty,
                                                   final ConfigurationNodeImpl configNode,
                                                   final HashMap<String, ModuleImpl> toExport, final ConfigurationModelImpl model) {

        log.debug("Merging property: {} with operation: {}", defProperty.getPath(), defProperty.getOperation());

        final ConfigurationPropertyImpl configProperty = configNode.getProperty(defProperty.getName());

        switch (defProperty.getOperation()) {
            case REPLACE:
            case ADD:
            case OVERRIDE:
                mergePropertyThatShouldExist(defProperty, configNode, configProperty, toExport, model);
                break;
            default:
                // case DELETE:
                deleteProperty(defProperty, configNode, configProperty, toExport, model);
                break;
        }
    }

    protected void mergePropertyThatShouldExist(final DefinitionPropertyImpl defProperty,
                                                final ConfigurationNodeImpl configNode,
                                                final ConfigurationPropertyImpl configProperty,
                                                final HashMap<String, ModuleImpl> toExport,
                                                final ConfigurationModelImpl model) {
        final boolean propertyExists = (configProperty != null);
        if (propertyExists) {
            // this is an existing property being replaced
            log.debug(".. which already exists", defProperty.getPath());

            // is there a local def for this specific property?
            final Optional<DefinitionPropertyImpl> maybeLocalPropertyDef = getLastLocalDef(configProperty, toExport);
            if (maybeLocalPropertyDef.isPresent()) {
                // yes, there's a local def for the specific property
                final DefinitionPropertyImpl localPropDef = maybeLocalPropertyDef.get();

                log.debug(".. and already has a local property def in: {} from source: {}",
                        localPropDef.getDefinition().getNode().getPath(),
                        localPropDef.getDefinition().getSource().getPath());

                final List<DefinitionPropertyImpl> defsForConfigProperty = configProperty.getDefinitions();

                // cases:
                // 1. local is replace and only def, diff is override => replace
                if (localPropDef.getOperation() == PropertyOperation.REPLACE
                        && defProperty.getOperation() == PropertyOperation.OVERRIDE
                        && defsForConfigProperty.size() == 1) {
                    defProperty.setOperation(PropertyOperation.REPLACE);
                }
                // 2. local is replace and not only def, diff is override => override (do nothing)

                if (localPropDef.getOperation() == PropertyOperation.OVERRIDE) {
                    // 3. local is override, diff is replace => override
                    if (defProperty.getOperation() == PropertyOperation.REPLACE) {
                        defProperty.setOperation(PropertyOperation.OVERRIDE);
                    }

                    if (defProperty.getOperation() == PropertyOperation.OVERRIDE) {
                        final DefinitionPropertyImpl nextUpDefProperty = (DefinitionPropertyImpl)
                                defsForConfigProperty.get(defsForConfigProperty.size() - 2);
                        final boolean diffMatchesNextUp =
                                defProperty.getType() == nextUpDefProperty.getType()
                                        && defProperty.getValueType() == nextUpDefProperty.getValueType();
                        if (diffMatchesNextUp) {
                            // 4. local is override, diff is override, upstream is same as diff => replace
                            defProperty.setOperation(PropertyOperation.REPLACE);
                        }
                        // 5. local is override, diff is override, upstream is still different => override (do nothing)
                    }
                }

                // 6. local is add, diff is replace => replace (do nothing)
                // 7. local is add, diff is override => override (do nothing)
                // 8. local is add, diff is add => add (do nothing)
                // 9. local is replace, diff is replace => replace (do nothing)
                // 10. local is replace, diff is add => replace (updateFrom handles)
                // 11. local is override, diff is add => override (updateFrom handles)

                // change local def to reflect new state
                localPropDef.updateFrom(defProperty);
                localPropDef.getDefinition().getSource().markChanged();

                // update the model incrementally, since a new local def should be available for other props
                final ConfigurationTreeBuilder builder =
                        new ConfigurationTreeBuilder(model.getConfigurationRootNode());

                // build the property back up from scratch using all of the definitions
                configNode.removeProperty(defProperty.getName());
                for (final DefinitionPropertyImpl def : defsForConfigProperty) {
                    builder.mergeProperty(configNode, def);
                }
            }
            else {
                // no, there's no local def for the specific property
                log.debug("... but has no local def yet");

                addLocalProperty(defProperty, configNode, toExport, model);
            }
        }
        else {
            // this is a totally new property
            // note: this is effectively unreachable for case: OVERRIDE
            log.debug(".. which is totally new", defProperty.getPath());

            addLocalProperty(defProperty, configNode, toExport, model);
        }
    }

    protected void deleteProperty(final DefinitionPropertyImpl defProperty,
                                  final ConfigurationNodeImpl configNode,
                                  final ConfigurationPropertyImpl configProperty,
                                  final HashMap<String, ModuleImpl> toExport,
                                  final ConfigurationModelImpl model) {
        final boolean propertyExists = (configProperty != null);
        if (!propertyExists) {
            throw new IllegalArgumentException("Cannot delete a property that doesn't exist in config model!");
        }

        final List<DefinitionPropertyImpl> defsForConfigProperty = configProperty.getDefinitions();
        final boolean lastDefIsUpstream = !isLastDefLocal(defsForConfigProperty, toExport);

        // add local property
        if (lastDefIsUpstream) {
            addLocalProperty(defProperty, configNode, toExport, model);
        }
        else {
            final List<DefinitionPropertyImpl> localDefs = getLocalDefs(defsForConfigProperty, toExport);
            final boolean onlyLocalDefs = (localDefs.size() == defsForConfigProperty.size());

            // remove all but the first local def
            final DefinitionPropertyImpl firstLocalDef = localDefs.get(0);
            firstLocalDef.getDefinition().getSource().markChanged();

            for (final DefinitionPropertyImpl localDef : localDefs.subList(1, localDefs.size())) {
                removeFromParentDefinitionItem(localDef, new ArrayList<>(), toExport);
            }

            // clear the property in the model
            configNode.removeProperty(defProperty.getName());

            if (onlyLocalDefs) {
                // if the first local def is the only def left, remove that, too
                removeFromParentDefinitionItem(firstLocalDef, new ArrayList<>(), toExport);
            }
            else {
                // otherwise, replace first local def with delete
                firstLocalDef.updateFrom(defProperty);
            }
        }
    }

    /**
     * Add a local definition of a given property, either by adding to an existing definition for the
     * containing node, or by creating a new definition for the containing node and adding to that.
     * @param defProperty the property to add
     * @param configNode the ConfigurationNode for the containing node
     * @param toExport modules we're merging/exporting -- changes should stay inside this scope
     * @param model
     */
    protected void addLocalProperty(final DefinitionPropertyImpl defProperty,
                                    final ConfigurationNodeImpl configNode,
                                    final HashMap<String, ModuleImpl> toExport,
                                    final ConfigurationModelImpl model) {
        // is there a local def for the parent node, where I can put this property?
        final Optional<DefinitionNodeImpl> maybeLocalNodeDef = getLastLocalDef(configNode, toExport);
        if (maybeLocalNodeDef.isPresent()) {
            // yes, there's a local def for parent node -- add the property
            final DefinitionNodeImpl definitionNode = maybeLocalNodeDef.get();

            log.debug("Adding new local property: {} in existing def: {} from source: {}",
                    defProperty.getPath(),
                    definitionNode.getDefinition().getNode().getPath(),
                    definitionNode.getDefinition().getSource().getPath());

            final DefinitionPropertyImpl newProperty = definitionNode.addProperty(defProperty);
            definitionNode.getDefinition().getSource().markChanged();

            // update the model incrementally, since a new local def should be available for other props
            new ConfigurationTreeBuilder(model.getConfigurationRootNode())
                    .mergeProperty(configNode, newProperty).pruneDeletedItems(configNode);
        }
        else {
            // no, there's no local def for parent node
            // create a new local definition with this property
            final DefinitionNodeImpl newDefNode =
                    createNewDef(defProperty.getParent(), false, toExport);

            log.debug("Adding new local def for property: {} in source: {}", defProperty.getPath(),
                    newDefNode.getDefinition().getSource().getPath());

            newDefNode.addProperty(defProperty);

            // update the model incrementally, since a new local def should be available for other props
            new ConfigurationTreeBuilder(model.getConfigurationRootNode())
                    .push(newDefNode.getDefinition()).pruneDeletedItems(configNode);
        }
    }

    protected <C extends ConfigurationItemImpl<D>, D extends DefinitionItemImpl>
        Optional<D> getLastLocalDef(final C item, final HashMap<String, ModuleImpl> toExport) {
        final List<D> existingDefs = item.getDefinitions();
        return Lists.reverse(existingDefs).stream()
                .filter(isLocalDef(toExport))
                .findFirst();
    }

    protected <D extends DefinitionItemImpl> List<D> getLocalDefs(final List<D> defsForNode,
                                                final HashMap<String, ModuleImpl> toExport) {
        return defsForNode.stream()
                .filter(isLocalDef(toExport)).collect(Collectors.toList());
    }

    protected boolean isLastDefLocal(final List<? extends DefinitionItem> definitionItems,
                                     final HashMap<String, ModuleImpl> toExport) {
        return isLocalDef(toExport).test(definitionItems.get(definitionItems.size()-1));
    }

    protected Predicate<DefinitionItem> isLocalDef(final HashMap<String, ModuleImpl> toExport) {
        return def -> toExport.containsKey(getMvnPathFromDefinitionItem(def));
    }

    protected static String getMvnPathFromDefinitionItem(final DefinitionItem item) {
        return ((ModuleImpl)item.getDefinition().getSource().getModule()).getMvnPath();
    }

    protected void mergeContentDefinitions(final ModuleImpl changes,
                                           final EventJournalProcessor.Changes contentChanges,
                                           final HashMap<String, ModuleImpl> toExport, final Session jcrSession) {

        // set of content change paths in lexical order, so that shorter common sub-paths come first
        // use a PATRICIA Trie, which stores strings efficiently when there are common prefixes
        final Set<String> contentChangesByPath = Collections.newSetFromMap(new PatriciaTrie<>());
        contentChangesByPath.addAll(contentChanges.getAddedContent().getPaths());
        contentChangesByPath.addAll(contentChanges.getChangedContent().getPaths());

        // set of existing sources in reverse lexical order, so that longer paths come first
        // note: we can use an ordinary TreeMap here, because we don't expect as many sources as raw paths
        final SortedMap<String, ContentDefinitionImpl> existingSourcesByPath = collectContentSourcesByPath(toExport);

        // process deletes, including resource removal
        for (final String deletePath : contentChanges.getDeletedContent()) {
            // if a delete path is -above- a content root path, we need to delete one or more entire sources
            final Set<String> toRemove = new HashSet<>();
            for (final String sourcePath : existingSourcesByPath.keySet()) {
                if (sourcePath.startsWith(deletePath)) {
                    final ContentDefinitionImpl contentDef = existingSourcesByPath.get(sourcePath);
                    final SourceImpl source = contentDef.getSource();
                    final ModuleImpl module = source.getModule();

                    // mark all referenced resources for delete
                    removeResources(contentDef.getNode());

                    // remove the source from its module
                    module.getModifiableSources().remove(source);
                    module.addContentResourceToRemove("/" + source.getPath());
                    toRemove.add(source.getPath());
                }
            }
            // if a delete path is -below- one of the sources that remains, treat it as a change
            for (final String sourcePath : Sets.difference(existingSourcesByPath.keySet(), toRemove)) {
                if (deletePath.startsWith(sourcePath)) {
                    contentChangesByPath.add(deletePath);
                }
            }
        }

        for (final String changePath : contentChangesByPath) {
            // is there an existing source for this exact path? if so, use that
            if (existingSourcesByPath.containsKey(changePath)) {
                // mark it changed for later re-export, and then we're done with this path
                existingSourcesByPath.get(changePath).getSource().markChanged();
                continue;
            }

            // there was no exactly-matching source, so we need to decide whether to reuse or create new
            // if LocationMapper tells us we should have a new source file...
            if (shouldPathCreateNewSource(changePath)) {
                // create a new source file
                existingSourcesByPath.put(changePath, createNewContentSource(changePath, toExport));

                // REPO-1715 We have a potential for a race condition where child nodes can be accidentally
                //           exported to source files for an ancestor node before we process the add events
                //           for the child nodes. To clean up this state, we also need to re-export any
                //           source on the direct ancestor path for the change path.
                for (ContentDefinitionImpl def : existingSourcesByPath.values()) {
                    if (changePath.startsWith(def.getRootPath())) {
                        def.getSource().markChanged();
                    }
                }
            }
            else {
                // check if there's an existing file that is an ancestor of the changed path
                // find the source with the longest matching substring of the changed path
                final Optional<ContentDefinitionImpl> maybeDef = existingSourcesByPath.entrySet().stream()
                        .filter(e -> changePath.startsWith(e.getKey()))
                        .map(Map.Entry::getValue)
                        .findFirst();
                if (maybeDef.isPresent()) {
                    // there's an existing matching source, so just mark it changed for later re-export
                    maybeDef.get().getSource().markChanged();
                }
                else {
                    // otherwise, create a new source file
                    // REPO-1715 We don't have to walk up the tree in this case, since we know there's
                    //           no source on an ancestor path that might have picked up these changes.
                    existingSourcesByPath.put(changePath, createNewContentSource(changePath, toExport));
                }
            }
        }

        // we've added new defs, so we need to update the modules to reflect that
        for (ModuleImpl module : toExport.values()) {
            module.build();
        }

        // for all changed content sources, regenerate definitions from JCR
        // todo: move this to serialization stage instead of merge stage
        final Set<String> newSourcePaths = collectContentSourcesByPath(toExport).keySet();
        toExport.values().stream().flatMap(m -> m.getContentSources().stream())
                .filter(SourceImpl::hasChangedSinceLoad)
                .forEach(source -> {
                    final ContentDefinitionImpl def = (ContentDefinitionImpl) source.getDefinitions().get(0);
                    final String rootPath = def.getNode().getPath();
                    try {
                        new ExportContentProcessor().exportNode(jcrSession.getNode(rootPath), def, true,
                                // exclude all paths that have their own sources
                                Sets.difference(newSourcePaths, Sets.newTreeSet(Collections.singleton(rootPath))));
                    }
                    catch (RepositoryException e) {
                        throw new RuntimeException(
                                "Exception while regenerating changed content source file for " + rootPath, e);
                    }
                });
    }

    /**
     * Helper to collect all content sources of given modules by root path in reverse lexical order of root paths.
     * @param toExport modules in whose sources we're interested
     */
    protected SortedMap<String, ContentDefinitionImpl> collectContentSourcesByPath(final HashMap<String, ModuleImpl> toExport) {
        final Function<ContentDefinitionImpl, String> cdPath = cd -> cd.getModifiableNode().getPath();
        final BinaryOperator<ContentDefinitionImpl> pickOne = (l, r) -> l;
        final Supplier<TreeMap<String, ContentDefinitionImpl>> reverseTreeMapper =
                () -> new TreeMap<>(Comparator.reverseOrder());
        return toExport.values().stream()
                .flatMap(m -> Lists.reverse(m.getContentDefinitions()).stream())
                .collect(Collectors.toMap(cdPath, Function.identity(), pickOne, reverseTreeMapper));
    }

    /**
     * Recursively find all resource paths in this node or descendants, then tell the containing module to remove
     * the resources at those paths.
     */
    protected void removeResources(final DefinitionNodeImpl node) {
        // find resource values
        for (final DefinitionPropertyImpl dp : node.getProperties().values()) {
            removeResources(dp);
        }

        // recursively visit child definition nodes
        for (final DefinitionNodeImpl childNode : node.getNodes().values()) {
            removeResources(childNode);
        }
    }

    /**
     * Find all resource paths in this property, then tell the containing module to remove
     * the resources at those paths.
     */
    protected void removeResources(final DefinitionPropertyImpl dp) {
        switch (dp.getType()) {
            case SINGLE:
                removeResourceIfNecessary(dp.getValue());
                break;
            case SET:
            case LIST:
                for (final ValueImpl value : dp.getValues()) {
                    removeResourceIfNecessary(value);
                }
                break;
        }
    }

    /**
     * If the given value represents a resource, tell the containing module to remove the resource at the path
     * represented by value.getString().
     */
    protected void removeResourceIfNecessary(final ValueImpl value) {
        if (value.isResource()) {
            final SourceImpl source = value.getDefinition().getSource();
            final String resourcePath = source.toModulePath(value.getString());

            if (source.getType() == SourceType.CONFIG) {
                source.getModule().addConfigResourceToRemove(resourcePath);
            }
            else {
                source.getModule().addContentResourceToRemove(resourcePath);
            }
        }
    }

    /**
     * Create a new ContentSourceImpl within one of the toExport modules to store content for the provided contentPath.
     * @param changePath the path whose content we want to store in the new source
     * @param toExport the set of modules that are being exported, which may contain the new source
     */
    protected ContentDefinitionImpl createNewContentSource(final String changePath, final HashMap<String, ModuleImpl> toExport) {
        // there's no existing source, so we need to create one
        final ModuleImpl module = getModuleByAutoExportConfig(changePath, toExport);
        final String sourcePath = getFilePathByLocationMapper(changePath);

        // TODO should we export the changePath into this new source, or the LocationMapper contextPath?
        // TODO ... we want the source root def to match the node expected from the source file name, right?

        final Predicate<String> sourceExists = s ->
            module.getModifiableSources().stream()
                .filter(SourceType.CONTENT::isOfType)
                .anyMatch(source -> source.getPath().equals(s));

        // if there's already a source with this path, generate a unique name
        final String uniqueSourcePath =
                FilePathUtils.generateUniquePath(sourcePath, sourceExists, 0);

        // create a new source and content definition with change path
        return module.addContentSource(uniqueSourcePath).addContentDefinition(changePath);
    }

}
