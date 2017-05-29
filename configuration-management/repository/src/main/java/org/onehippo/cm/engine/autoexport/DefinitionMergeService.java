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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.onehippo.cm.model.ConfigDefinition;
import org.onehippo.cm.model.ConfigurationNode;
import org.onehippo.cm.model.Definition;
import org.onehippo.cm.model.DefinitionItem;
import org.onehippo.cm.model.NamespaceDefinition;
import org.onehippo.cm.model.PropertyType;
import org.onehippo.cm.model.Source;
import org.onehippo.cm.model.impl.AbstractDefinitionImpl;
import org.onehippo.cm.model.impl.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.ConfigSourceImpl;
import org.onehippo.cm.model.impl.ConfigurationItemImpl;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ConfigurationNodeImpl;
import org.onehippo.cm.model.impl.ConfigurationPropertyImpl;
import org.onehippo.cm.model.impl.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.DefinitionItemImpl;
import org.onehippo.cm.model.impl.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.DefinitionPropertyImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.NamespaceDefinitionImpl;
import org.onehippo.cm.model.impl.SourceImpl;
import org.onehippo.cm.model.impl.ValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import static java.util.Arrays.asList;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.onehippo.cm.model.Constants.YAML_EXT;
import static org.onehippo.cm.model.DefinitionType.NAMESPACE;
import static org.onehippo.cm.model.PropertyOperation.OVERRIDE;

public class DefinitionMergeService {

    private static final Logger log = LoggerFactory.getLogger(DefinitionMergeService.class);

    private final Configuration autoExportConfig;

    private static class ModuleMapping {
        String mvnPath;
        Collection<String> repositoryPaths;

        PatternSet pathPatterns;

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
        this.autoExportConfig = autoExportConfig;

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
     * @param toMerge the Modules that we expect to be revised to include changes
     * @param baseline the currently stored configuration baseline B
     * @return a new version of each toMerge module, if revisions are necessary
     */
    public Collection<ModuleImpl> mergeChangesToModules(final ModuleImpl changes, final Set<ModuleImpl> toMerge,
                                                        final ConfigurationModelImpl baseline) {
        log.debug("Merging changes to modules: {}", toMerge);

        // clone the toMerge modules and keep a copy indexed by mvnSourceRoot
        final HashMap<String, ModuleImpl> toExport = new HashMap<>();
        for (final ModuleImpl module : toMerge) {
            toExport.put(module.getMvnPath(), module.clone().build());
        }

        // make sure the changes module has all the definitions nicely sorted
        changes.build();

        // handle namespaces before rebuilding, since we want any validation to happen after this
        for (NamespaceDefinitionImpl nsd : changes.getNamespaceDefinitions()) {
            mergeNamespace(nsd, toExport, baseline);
        }

        // TODO does it make sense to auto-export webfilebundle definitions?

        // create a copy of the baseline that includes the new modules
        // This model includes copies of upstream modules that reference the actual Source and Definition objects
        // of the upstream modules from the baseline. We must take care not to modify those Sources!
        // However, the toExport modules are represented with cloned copies with reparsed Sources distinct from the
        // original baseline. We can safely modify those Sources and Definitions as needed.
        ConfigurationModelImpl model = rebuild(toExport, baseline);

        // merge config changes
        // ConfigDefinitions are already sorted by root path
        for (final ConfigDefinitionImpl change : changes.getConfigDefinitions()) {
            // run the full and complex merge logic, recursively
            model = mergeConfigDefinitionNode(change.getNode(), toExport, model);
        }

        // sort all ContentDefinitions by root path
        changes.getContentDefinitions().sort(Comparator.naturalOrder());

        // for each ContentDefinition
        for (final ContentDefinitionImpl change : changes.getContentDefinitions()) {
            // todo run the full and complex merge logic
//            model =
                    mergeContentDefinition(change, toExport, baseline);
        }

        return toExport.values();
    }

    /**
     * Merge a single namespace definition into the appropriate toExport module.
     * TODO should this abort the merge process completely with an exception if merge is unsuccessful?
     * @param nsd the definition to merge
     * @param toExport modules that may be merged into
     * @param baseline a complete ConfigurationModel baseline for context
     */
    protected void mergeNamespace(final NamespaceDefinitionImpl nsd, final HashMap<String, ModuleImpl> toExport, final ConfigurationModelImpl baseline) {
        // find the corresponding definition by namespace prefix -- only one is permitted
        final Optional<NamespaceDefinitionImpl> found = baseline.getNamespaceDefinitions().stream()
                .filter(namespaceDefinition -> namespaceDefinition.getPrefix().equals(nsd.getPrefix()))
                .findFirst();

        // clone the CndPath Value and set a "foreign source" back-reference for use later when copying data
        final ValueImpl cndPath = nsd.getCndPath().clone();
        cndPath.setForeignSource(nsd.getSource());

        if (found.isPresent()) {
            // this is an update to an existing namespace def
            // find the corresponding source by path
            final Source oldSource = found.get().getSource();

            // this source will have a reference to a module in the baseline, not our clones, so lookup by mvnPath
            final ModuleImpl newModule = toExport.get(((ModuleImpl)oldSource.getModule()).getMvnPath());

            // short-circuit this loop iteration if we cannot create a valid merged definition
            // TODO should this abort the merge process completely with an exception?
            if (newModule == null) {
                log.error("Cannot merge a namespace {} that belongs to an upstream module", nsd.getPrefix());
                return;
            }

            final SourceImpl newSource = newModule.getSourceByPath(oldSource.getPath());

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
                }
            }
        }
        else {
            // this is a new namespace def -- pretend that it is a node under /hippo:namespaces for sake of file mapping
            // TODO is this the right "magic path" for mapping new namespaces to modules?
            final String incomingPath = "/hippo:namespaces/jcr-namespaces";

            // what module should we put it in?
            ModuleImpl newModule = getModuleByAutoExportConfig(incomingPath, toExport);

            // what source should we put it in?
            ConfigSourceImpl newSource;
            if (newModule.getNamespaceDefinitions().isEmpty()) {
                // We don't have any namespaces yet, so we can generate a new source and put it there
                newSource = getSourceForNewDefinition(incomingPath, newModule);
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
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // note: we assume that the original baseline will perform any required cleanup in close(), so we don't need
        //       to copy FileSystems etc. here
        ConfigurationModelImpl model = new ConfigurationModelImpl();
        toExport.values().forEach(model::addModule);
        baseline.getSortedGroups().forEach(model::addGroup);
        model.build();

        stopWatch.stop();
        log.info("ConfigurationModel rebuilt for auto-export merge in {}", stopWatch.toString());
        return model;
    }

    protected ConfigurationModelImpl mergeConfigDefinitionNode(final DefinitionNodeImpl incomingDefNode,
                                                               final HashMap<String, ModuleImpl> toExport,
                                                               ConfigurationModelImpl model) {
        log.debug("Merging config change for path: {}", incomingDefNode.getPath());

        final boolean nodeIsNew = isNewNodeDefinition(incomingDefNode);
        if (nodeIsNew) {
            createNewNode(incomingDefNode, toExport, model);

            // rebuild the ConfigurationModel after adding the new def, to keep the back-references accurate
            // TODO do this only once at the end of mergeChangesToModules(), once mergeProperty can update the model incrementally
            return rebuild(toExport, model);
        }
        else {
            // if the incoming node is not new, we should expect its path to exist -- find it
            final String incomingDefPath = incomingDefNode.getPath();
            ConfigurationNodeImpl incomingConfigNode = model.resolveNode(incomingDefPath);

            if (incomingConfigNode == null) {
                throw new IllegalStateException("Cannot modify a node that doesn't exist in baseline: " + incomingDefPath);
            }

            log.debug("Changed path has existing definition: {}", incomingDefPath);

            // is this a delete?
            if (incomingDefNode.isDelete()) {
                // handle node delete
                deleteNode(incomingDefNode, incomingConfigNode, toExport);

                // don't bother checking any other properties or children -- they're gone
                return rebuild(toExport, model);
            }

            // handle properties, then child nodes
            for (final DefinitionPropertyImpl defProperty : incomingDefNode.getModifiableProperties().values()) {
                // handle properties on an existing node
                mergeProperty(defProperty, incomingConfigNode, toExport);

                // rebuild after each property, since a new local def should be available for other props
                // TODO update the ConfigNodes incrementally instead
                model = rebuild(toExport, model);

                // lookup the rootConfigNode again to reflect updated model
                incomingConfigNode = model.resolveNode(incomingDefPath);
            }

            // any child node here may or may not be new -- do full recursion
            for (DefinitionNodeImpl childNodeDef : incomingDefNode.getModifiableNodes().values()) {
                model = mergeConfigDefinitionNode(childNodeDef, toExport, model);
            }

            // we don't need to rebuild here, since any real change will have triggered a rebuild elsewhere
            return model;
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
                && defNode.getProperties().containsKey(JCR_PRIMARYTYPE)
                && !defNode.getProperties().get(JCR_PRIMARYTYPE).getOperation().equals(OVERRIDE);
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
            createNewDef(incomingDefNode, true, toExport);
        }
        else {
            // where was the parent node mentioned?
            // is one of the existing defs for the parent in the toMerge modules? grab the last one
            // TODO if there is more than one mention in toMerge, should we prefer the def with jcr:primaryType?
            Optional<DefinitionItemImpl> maybeDef = getLastLocalDef(existingParent, toExport);

            // TODO should we attempt any kind of sorting on output? current behavior is append, with history-dependent output
            // TODO i.e. the sequence of changes to the repository and the timing of auto-export will produce different files
            // TODO also, definition ordering will likely not match order implied by .meta:order-before
            if (maybeDef.isPresent()) {
                // since we have a parent defNode in a valid module, use that for this new child
                DefinitionNodeImpl parentDefNode = (DefinitionNodeImpl) maybeDef.get();

                // we know that this is the only place that mentions this node, because it's new
                // -- put all descendent properties and nodes in this def
                recursiveAdd(incomingDefNode, parentDefNode, toExport);
            }
            else {
                // there's no existing parent defNode that we can reuse, so we need a new definition
                // TODO should this take into account the modules where siblings are defined, to handle ordering properly?
                createNewDef(incomingDefNode, true, toExport);
            }
        }
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
        ModuleImpl destModule = getModuleByAutoExportConfig(incomingPath, toExport);

        // what source should we put it in?
        ConfigSourceImpl destSource = getSourceForNewDefinition(incomingPath, destModule);

        log.debug("... stored in {}/hcm-config/{}", destModule.getName(), destSource.getPath());

        // create the new ConfigDefinition and add it to the source
        // we know that this is the only place that mentions this node, because it's new
        // -- put all descendent properties and nodes in this def
        final String rootDefPath = incomingDefNode.getPath();
        final String name = StringUtils.substringAfterLast(rootDefPath, "/");
        final ConfigDefinitionImpl configDef = destSource.addConfigDefinition();

        final DefinitionNodeImpl newRootNode = new DefinitionNodeImpl(rootDefPath, name, configDef);
        configDef.setNode(newRootNode);

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

        // if order-before is set, we need to do an insert, not an add-at-end
        DefinitionNodeImpl to;
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
     * @param from
     * @param to
     * @param toExport
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

        for (DefinitionPropertyImpl newProperty : from.getProperties().values()) {
            // TODO move referenced resources from old module to new module
            if (newProperty.getType().equals(PropertyType.SINGLE)) {
                to.addProperty(newProperty.getName(), newProperty.getValue());
            }
            else {
                ValueImpl[] values = new ValueImpl[newProperty.getValues().length];
                Collections.copy(asList(values), asList(newProperty.getValues()));
                to.addProperty(newProperty.getName(), newProperty.getValueType(), values);
            }
        }

        // TODO do we need to sort accounting for order-before, or does the diff step order things w/o explicit order-before?
        for (final DefinitionNodeImpl childNode : from.getModifiableNodes().values()) {
            // for each new childNode, we need to check if LocationMapper wants a new source file
            String incomingPath = childNode.getPath();
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

    protected void deleteNode(final DefinitionNodeImpl defNode, final ConfigurationNode configNode,
                              final HashMap<String, ModuleImpl> toExport) {
        log.debug("Deleting node: {}", defNode.getPath());

        List<? extends DefinitionItem> defsForNode = configNode.getDefinitions();

        // if last existing node def is upstream,
        boolean lastDefIsUpstream = !isLastDefLocal(defsForNode, toExport);

        if (lastDefIsUpstream) {
            log.debug("Last def for node is upstream of export: {}", defNode.getPath());

            // create new defnode w/ delete
            createNewDef(defNode, true, toExport);

            // we know that there was no local def for the node we're deleting, but there may be defs for its children
            // so for all descendants, remove all definitions and possibly sources
            removeChildDefinitions(configNode, new ArrayList<>(), toExport);
        }
        else {
            // there are local node defs for this node
            // are there ONLY local node defs?
            boolean onlyLocalDefs = areAllDefsLocal(defsForNode, toExport);

            if (onlyLocalDefs) {
                log.debug("Only local defs for node: {}", defNode.getPath());

                // since there's only local defs, we want this node to disappear from the record completely
                removeDefinitionsAtOrBelow(configNode, toExport);
            }
            else {
                log.debug("Both local and upstream defs for node: {}", defNode.getPath());

                // since there's also an upstream def, we want to collapse all local references to a single delete def
                // if exists, change one local def to delete and remove other properties and subnodes
                final List<Definition> alreadyRemovedFrom = new ArrayList<>();
                final List<DefinitionItem> localDefs = defsForNode.stream()
                        .filter(isLocalDef(toExport)).collect(Collectors.toList());
                final DefinitionNodeImpl defToKeep = (DefinitionNodeImpl) localDefs.get(0);

                // mark chosen node as a delete
                final ConfigDefinition defToKeepDefinition = (ConfigDefinition) defToKeep.getDefinition();
                log.debug("Marking delete on node: {} from definition of: {} in source: {}",
                        defToKeep.getPath(),
                        defToKeepDefinition.getNode().getPath(),
                        defToKeepDefinition.getSource().getPath());
                defToKeep.delete();
                alreadyRemovedFrom.add(defToKeepDefinition);

                // remove all other defs and children
                final List<DefinitionItem> localDefsExceptFirst = localDefs.subList(1, localDefs.size());
                removeDefsAndChildren(configNode, localDefsExceptFirst, alreadyRemovedFrom, toExport);

            }
            // TODO remove referenced resources?
        }
    }

    /**
     * Remove all definitions mentioning the given rootConfigNode or any of its descendants, because we are deleting
     * the referenced node from all config.
     * @param rootConfigNode
     * @param toExport
     */
    protected void removeDefinitionsAtOrBelow(final ConfigurationNode rootConfigNode,
                                              final HashMap<String, ModuleImpl> toExport) {
        // keep track of the definitions that we've already handled
        final List<Definition> alreadyRemovedFrom = new ArrayList<>();
        final List<? extends DefinitionItem> defsToRemove = rootConfigNode.getDefinitions();
        removeDefsAndChildren(rootConfigNode, defsToRemove, alreadyRemovedFrom, toExport);
    }

    protected void removeDefsAndChildren(final ConfigurationNode rootConfigNode, final List<? extends DefinitionItem> defsToRemove,
                                         final List<Definition> alreadyRemovedFrom,
                                         final HashMap<String, ModuleImpl> toExport) {
        log.debug("Removing defs and children for node: {} with exceptions: {}", rootConfigNode.getPath(), alreadyRemovedFrom);

        for (DefinitionItem definitionItem : defsToRemove) {
            removeOneDefinitionItem(definitionItem, alreadyRemovedFrom, toExport);
        }

        // properties can only exist on definitions with root paths of the node itself, which are already removed above

        // scan downwards for child definitions, which could be rooted on the children directly
        removeChildDefinitions(rootConfigNode, alreadyRemovedFrom, toExport);
    }

    protected void removeOneDefinitionItem(final DefinitionItem definitionItem,
                                           final List<Definition> alreadyRemovedFrom,
                                           final HashMap<String, ModuleImpl> toExport) {
        final ConfigDefinition definition = (ConfigDefinition) definitionItem.getDefinition();

        log.debug("Removing one def item for node: {} with exceptions: {}", definitionItem.getPath(), alreadyRemovedFrom);

        // remove the node itself
        // if this node is the root
        if (definitionItem.isRoot()) {
            // remove the definition
            removeDefinition(definition, toExport);
        }
        else {
            removeFromParentDefinitionItem(definitionItem, toExport);
        }
        alreadyRemovedFrom.add(definition);
    }

    /**
     * Remove child definitions from the given configNode, because configNode is being deleted.
     * @param configNode the node being deleted
     * @param alreadyRemovedFrom keep track of the definitions that we've already handled
     * @param toExport modules we're merging/exporting -- changes should stay inside this scope
     */
    protected void removeChildDefinitions(final ConfigurationNode configNode, final List<Definition> alreadyRemovedFrom,
                                          final HashMap<String, ModuleImpl> toExport) {
        log.debug("Removing child defs for node: {} with exceptions: {}", configNode.getPath(), alreadyRemovedFrom);

        for (ConfigurationNode childConfigNode : configNode.getNodes().values()) {
            for (DefinitionItem childDefItem : childConfigNode.getDefinitions()) {
                // if childNode def was part of a parent def, it was already removed above
                final Definition childDefinition = childDefItem.getDefinition();
                if (!alreadyRemovedFrom.contains(childDefinition)) {
                    // otherwise, remove it now -- it must be a root of its definition
                    // or the direct child of a parent that is also being removed
                    removeDefinition((ConfigDefinition) childDefinition, toExport);
                    alreadyRemovedFrom.add(childDefinition);
                }
            }
            removeChildDefinitions(childConfigNode, alreadyRemovedFrom, toExport);
        }
    }

    protected void removeFromParentDefinitionItem(final DefinitionItem definitionItem,
                                                  final HashMap<String, ModuleImpl> toExport) {
        final ConfigDefinition definition = (ConfigDefinition) definitionItem.getDefinition();
        final SourceImpl source = (SourceImpl) definition.getSource();
        final ModuleImpl module = source.getModule();

        // check if the definition is in one of the toExport modules -- if not, we can't change it
        if (!toExport.containsValue(module)) {
            throw new IllegalStateException
                    ("Cannot change a definition from module that is not being merged: " + module.getFullName()
                            + " for node: " + definitionItem.getPath());
        }
        log.debug("Removing definition item for node: {} from definition of: {} in source: {}",
                definitionItem.getPath(),
                definition.getNode().getPath(), source.getPath());

        // if this node isn't the root of its definition, remove the node from its parent
        DefinitionNodeImpl parentNode = (DefinitionNodeImpl) definitionItem.getParent();
        parentNode.getModifiableNodes().remove(definitionItem.getName());

        // if this was the last item in the parent node
        if ((parentNode.getModifiableNodes().size() == 0)
                && (parentNode.getModifiableProperties().size() == 0)) {
            // the parent must be the root! because the only way to modify an existing node is for it to be root
            // of a new definition, and the only way for the parent to be empty is if it was only a modify with
            // the one new child that we just removed
            if (!parentNode.isRoot()) {
                throw new IllegalStateException("Empty parent of removed node must be a root definition node: "
                        + parentNode.getPath());
            }

            // remove the definition
            removeDefinition(definition, toExport);
        }
    }

    protected void removeDefinition(final ConfigDefinition definition, final HashMap<String, ModuleImpl> toExport) {
        // remove the definition from its source and from its module
        final SourceImpl source = (SourceImpl) definition.getSource();
        final ModuleImpl module = source.getModule();

        // check if the definition is in one of the toExport modules -- if not, we can't change it
        if (!toExport.containsValue(module)) {
            throw new IllegalStateException
                    ("Cannot remove a definition from module that is not being merged: " + module.getFullName()
                            + " for node: " + definition.getNode().getPath());
        }
        log.debug("Removing definition for node: {} from source: {}", definition.getNode().getPath(), source.getPath());

        source.getModifiableDefinitions().remove(definition);
        module.getConfigDefinitions().remove(definition);

        // if the definition was the last one from its source
        if (source.getModifiableDefinitions().size() == 0) {
            log.debug("Removing source: {}", source.getPath());

            // remove the source from its module
            module.getModifiableSources().remove(source);
        }
    }

    protected void mergeProperty(final DefinitionPropertyImpl defProperty,
                                 final ConfigurationNodeImpl rootConfigNode,
                                 final HashMap<String, ModuleImpl> toExport) {

        log.debug("Merging property: {}", defProperty.getPath());

        final boolean propertyExists = rootConfigNode.getProperties().containsKey(defProperty.getName());

//        boolean lastDefIsUpstream = !isLastDefLocal(defs, toExport);

        switch (defProperty.getOperation()) {
            case REPLACE:
                if (!propertyExists) {
                    // this is a totally new property
                    log.debug(".. which is totally new", defProperty.getPath());

                    addLocalProperty(defProperty, rootConfigNode, toExport);
                }
                else {
                    // this is an existing property being replaced
                    log.debug(".. which already exists", defProperty.getPath());

                    // is there a local def for this specific property?
                    final ConfigurationPropertyImpl configProperty = rootConfigNode.getProperties().get(defProperty.getName());
                    final Optional<DefinitionItemImpl> maybeLocalPropertyDef = getLastLocalDef(configProperty, toExport);
                    if (maybeLocalPropertyDef.isPresent()) {
                        // yes, there's a local def for the specific property
                        DefinitionPropertyImpl localPropDef = (DefinitionPropertyImpl) maybeLocalPropertyDef.get();

                        log.debug(".. and already has a local property def in: {} from source: {}",
                                localPropDef.getDefinition().getNode().getPath(),
                                localPropDef.getDefinition().getSource().getPath());

                        // change it to reflect new state
                        // TODO copy resource from old module to new module
                        localPropDef.updateFrom(defProperty);
                    }
                    else {
                        // no, there's no local def for the specific property
                        log.debug("... but has no local def yet");

                        addLocalProperty(defProperty, rootConfigNode, toExport);
                    }
                }
                break;
            case ADD:
                break;
            case OVERRIDE:
                break;
            case DELETE:
                break;
        }

//add/append
//    if existing property def is local, update that def (keep override if present)
//    if existing property def is upstream, create new one w/ add
//
//delete
//    if existing property def is local, check if another def is upstream
//        if another def is upstream, replace current local def with delete def
//        if no other def is upstream, remove current local def
//            if that was last property on node, remove node
//            if that was last node in def, remove def
//            if that was last def in source, remove source
//
//change type
//change multiplicity
//    if existing def is local, update that def (recompute if override is necessary)
//    if existing def is upstream,
//        check for local node def,
//            if exists, add property def w/ override
//            if doesn't exist, create new node def and property def w/ override
//
//override primary type
//    if existing jcr:primaryType def is local, update that def (recompute if override is necessary)
//    if existing jcr:primaryType def is upstream
//        check for local node def,
//            if exists, add jcr:primaryType def w/ override
//            if doesn't exist, create new node def and jcr:primaryType def w/ override
//
//change mixin types
//    if existing jcr:mixinTypes def is local, update that def
//        check if add or override is still needed
//    if existing jcr:mixinTypes def is upstream,
//        check for local node def,
//             if exists, create new jcr:mixinTypes def (keep add or override as present)
//             if doesn't exist, create new node def and jcr:mixinTypes def
    }

    protected void addLocalProperty(final DefinitionPropertyImpl defProperty, final ConfigurationNodeImpl rootConfigNode, final HashMap<String, ModuleImpl> toExport) {
        // is there a local def for the parent node, where I can put this property?
        final Optional<DefinitionItemImpl> maybeLocalNodeDef = getLastLocalDef(rootConfigNode, toExport);
        if (maybeLocalNodeDef.isPresent()) {
            // yes, there's a local def for parent node -- add the property
            final DefinitionNodeImpl definitionNode = (DefinitionNodeImpl) maybeLocalNodeDef.get();

            log.debug("Adding new local property def in: {} from source: {}",
                    definitionNode.getDefinition().getNode().getPath(),
                    definitionNode.getDefinition().getSource().getPath());

            // TODO copy resource from old module to new module
            definitionNode.addProperty(defProperty);
        }
        else {
            // no, there's no local def for parent node -- create a new local definition with this property
            final DefinitionNodeImpl newDefNode =
                    createNewDef(defProperty.getParent(), false, toExport);

            log.debug("Adding new local property def: {}", defProperty.getPath());
            newDefNode.addProperty(defProperty);
        }
    }

    protected Optional<DefinitionItemImpl> getLastLocalDef(final ConfigurationItemImpl item,
                                                           final HashMap<String, ModuleImpl> toExport) {
        List<DefinitionItemImpl> existingDefs = item.getDefinitions();
        return Lists.reverse(existingDefs).stream()
                .filter(isLocalDef(toExport))
                .findFirst();
    }

    protected boolean isLastDefLocal(final List<? extends DefinitionItem> definitionItems,
                                     final HashMap<String, ModuleImpl> toExport) {
        return isLocalDef(toExport).test(definitionItems.get(definitionItems.size()-1));
    }

    protected boolean areAllDefsLocal(final List<? extends DefinitionItem> definitionItems,
                                      final HashMap<String, ModuleImpl> toExport) {
        return definitionItems.stream().allMatch(isLocalDef(toExport));
    }

    protected Predicate<DefinitionItem> isLocalDef(final HashMap<String, ModuleImpl> toExport) {
        return def -> toExport.containsKey(getMvnPathFromDefinitionItem(def));
    }

    protected static String getMvnPathFromDefinitionItem(final DefinitionItem item) {
        return ((ModuleImpl)item.getDefinition().getSource().getModule()).getMvnPath();
    }

    protected void mergeContentDefinition(final ContentDefinitionImpl change, final HashMap<String, ModuleImpl> toExport,
                                final ConfigurationModelImpl model) {
        // TODO!!!!
//         is this root path already a root path of an existing ContentDefinition?
//             yes, we have an exactly matching path for an existing ContentDefinition
//             is the existing definition part of a toMerge module?
//                 yes, existing matching definition is in a toMerge module
//                 root DefinitionNode.isDelete()?
//                     yes, root DefinitionNode.isDelete()
//                     remove the Source containing this definition from the module
//                     add a DELETE action for this path
//
//                     no, root DefinitionNode is a regular node, not delete
//                     TODO should we check if the existing definition is the module that matches current auto-export config?
//                     replace existing DefinitionNode with a clone from changes
//                         apply file-splitting rules from LocationMapper
//                         check for separate content definitions for subnodes
//                     TODO do we need a REPLACE action for the sake of upstream environments? (we don't need one here)
//
//                 no, existing definition is deeper in the baseline
//                 root DefinitionNode.isDelete()?
//                     yes, we're deleting a content node originally defined in the baseline
//                     add a DELETE action for this path in the appropriate toMerge module
//
//                     no, this is a regular node on a path already defined in the baseline
//                     createContentSource()
//                     TODO which action should we use? APPEND-PROPERTIES, UPDATE, or REPLACE?
//                     add the appropriate action
//
//             no exactly matching ContentDefinition
//             is this root path a subpath of an existing ContentDefinition?
//                 yes, existing ancestor ContentDefinition (this should be the closest/deepest available ancestor)
//                 is the ancestor part of a toMerge module?
//                     yes, ancestor is from a toMerge module
//                     is the new root path defined in the toMerge ancestor definition?
//                         yes, the new root path overlaps an existing toMerge ancestor
//                         root definitionNode.isDelete()?
//                             yes, deleting an existing part of a toMerge definition
//                             remove the existing definitionNode from the toMerge definition
//
//                             no, not a delete
//                             TODO merge changes property-by-property and recursively into existing definition
//
//                         no, the new root path isn't available in the toMerge ancestor
//                         is the parent of the new root path defined in the toMerge ancestor definition?
//                             yes, the new root path can be attached under an existing toMerge ancestor parent DefinitionNode
//
//                             TODO apply file splitting rules
//                             clone definition into the existing ancestor definition at the appropriate DefinitionNode
//                                 apply file-splitting rules from LocationMapper
//                                 check for separate content definitions for subnodes
//
//                             no, the parent isn't available in the toMerge ancestor
//                             THROW EXCEPTION! this should be impossible, and the diff that generated changes must be broken!
//                             (diff should have intermediate nodes)
//
//                     no, ancestor is from deeper in the baseline
//                     is the new root path defined in the deep-baseline ancestor definition?
//                         yes, the new root path overlaps an existing deep-baseline ancestor
//                         root definitionNode.isDelete()?
//                             yes, deleting a path from within a deep-baseline ancestor
//                             create a DELETE action for this path in the appropriate toMerge module
//
//                             no, not a delete
//                             createContentSource()
//                             TODO which action should we use? APPEND-PROPERTIES, UPDATE, or REPLACE?
//                             add the appropriate action
//
//                         no, the new root path isn't available in the deep-baseline ancestor
//                         is the parent of the new root path defined?
//                             yes, the parent is already defined in the deep-baseline ancestor, but the changes root is not defined yet
//                             root definitionNode.isDelete()?
//                                 yes, we're trying to delete a node that should never have existed
//                                 THROW EXCEPTION! this should be impossible, and the diff that generated changes must be broken!
//                                 (this just shouldn't have been mentioned)
//
//                                 no, this is a regular node with an existing deep-baseline parent, but not already defined
//                                 createContentSource()
//
//                             no, the parent is not defined yet anywhere
//                             THROW EXCEPTION! this should be impossible, and the diff that generated changes must be broken!
//                             (diff should have intermediate nodes)
//
//                 no existing ancestor ContentDefinition
//                 ... look for a ConfigDefinition with the parent node
//                 is the root path a subpath of an existing ConfigDefinition?
//                     yes, part of an existing ConfigDefinition
//                     is the parent of the root path defined in this ConfigDefinition?
//                         yes, the parent is defined
//                         is the changes root node a delete?
//                             yes, a delete on a child of an existing config node but not existing content node
//                             THROW EXCEPTION! this should be impossible, and the diff that generated changes must be broken!
//                             (should be a config change or not mentioned)
//
//                             no, not a delete
//                             is the changes root node defined?
//                                 yes, changes root is defined in config
//                                 THROW EXCEPTION! this should be impossible, and the diff that generated changes must be broken!
//                                 (a diff should not remap a config node to content)
//
//                                 no, changes root is not already defined anywhere
//                                 createContentSource() (this is a new content as a child of a config)
//
//                         no, the parent of the root path isn't defined
//                         THROW EXCEPTION! this should be impossible, and the diff that generated changes must be broken!
//                         (diff should have intermediate nodes)
//
//                     no, not part of any existing ConfigDefinition
//                     THIS SHOULD BE IMPOSSIBLE! (there should be a root "/" DefinitionNode at the least)
    }

    void createContentSource() {
        // TODO!!!
        // double-check translations handling in LocationMapper

        // figure out which mvn path corresponds to which module name

        // assert: this is a regular node, not a delete!
        // create a new Source in the appropriate toMerge Module
        // use LocationMapper to generate a default filename, except use .yaml extension instead of .xml
            // String contextPath = LocationMapper.contextNodeForPath("/", true);
        // clone the ContentDefinition from changes into this Source

        // check for separate content definitions for subnodes
    }

    /**
     * Find the module within toExport that should match the given path according to the AutoExport module config.
     * @param path the path to test
     * @param toExport the set of Modules being merged here and eventually to be exported
     * @return a single Module that represents the best match for this path
     */
    protected ModuleImpl getModuleByAutoExportConfig(final String path, final HashMap<String, ModuleImpl> toExport) {
        // TODO extra logic from EventProcessor.getModuleForPath() and getModuleForNSPrefix()
        ModuleImpl result = null;
        for (ModuleMapping mapping : moduleMappings.values()) {
            if (mapping.matchesPath(path)) {
                result = toExport.get(mapping.mvnPath);
                break;
            }
        }
        if (result == null) {
            result = toExport.get(defaultModuleMapping.mvnPath);
        }
        return result;
    }

    /**
     * Get a source within a given module to use when adding a new node definition. The source is chosen based on
     * the conventions from the AutoExport LocationMapper class, and it might in theory already exist.
     * @param path the JCR path of the new node we'll be defining
     * @param module the module where we want this definition to live
     * @return a new or existing ConfigSourceImpl
     */
    protected ConfigSourceImpl getSourceForNewDefinition(final String path, final ModuleImpl module) {
        // what does LocationMapper say?
        final String sourcePath = getFilePathByLocationMapper(path, true);

        // does this Source already exist?
        final Optional<ConfigSourceImpl> maybeSource =
                module.getConfigSources().stream()
                        .filter(source -> source.getPath().equals(sourcePath))
                        .findFirst();
        if (maybeSource.isPresent()) {
            return maybeSource.get();
        }
        else {
            return module.addConfigSource(sourcePath);
        }
    }

    /**
     * Lookup the file path that the old AutoExport LocationMapper class would recommend for the given JCR path,
     * then adjust it to use a YAML extension instead of an XML extension.
     * @param path the JCR path for which we want to generate a new source file
     * @param isNode does the path represent a node (true) or a property (false)?
     * @return a config-root-relative path with no leading slash for a potentially new yaml source file
     */
    protected String getFilePathByLocationMapper(String path, boolean isNode) {
        String xmlFile = LocationMapper.fileForPath(path, isNode);
        return StringUtils.removeEnd(xmlFile, ".xml") + YAML_EXT;
    }
}
