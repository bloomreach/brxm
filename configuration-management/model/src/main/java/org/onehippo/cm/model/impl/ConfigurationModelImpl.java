/*
 * Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cm.model.impl;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.Site;
import org.onehippo.cm.model.impl.definition.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.definition.NamespaceDefinitionImpl;
import org.onehippo.cm.model.impl.definition.WebFileBundleDefinitionImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationNodeImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationPropertyImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationTreeBuilder;
import org.onehippo.cm.model.parser.ParserException;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.util.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.model.Site.CORE_NAME;

public class ConfigurationModelImpl implements ConfigurationModel {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationModelImpl.class);

    private static final OrderableByNameListSorter<Site> siteSorter = new OrderableByNameListSorter<>(Site.class);

    private final Map<String, SiteImpl> buildingSites = new HashMap<>();
    private final List<SiteImpl> sites = new ArrayList<>();
    private final List<GroupImpl> groups = new ArrayList<>();
    private final List<GroupImpl> sortedGroups = Collections.unmodifiableList(groups);

    private final Set<ModuleImpl> replacements = new HashSet<>();

    private ConfigurationNodeImpl configurationRootNode;

    private final List<NamespaceDefinitionImpl> modifiableNamespaceDefinitions = new ArrayList<>();
    private final List<NamespaceDefinitionImpl> namespaceDefinitions = Collections.unmodifiableList(modifiableNamespaceDefinitions);
    private final List<WebFileBundleDefinitionImpl> modifiableWebFileBundleDefinitions = new ArrayList<>();
    private final List<WebFileBundleDefinitionImpl> webFileBundleDefinitions = Collections.unmodifiableList(modifiableWebFileBundleDefinitions);
    private final List<ContentDefinitionImpl> modifiableContentDefinitions = new ArrayList<>();
    private final List<ContentDefinitionImpl> contentDefinitions = Collections.unmodifiableList(modifiableContentDefinitions);
    private final List<ConfigDefinitionImpl> modifiableConfigDefinitions = new ArrayList<>();
    private final Map<JcrPath, ConfigurationNodeImpl> modifiableDeletedConfigNodes = new HashMap<>();
    private final Map<JcrPath, ConfigurationNodeImpl> deletedConfigNodes = Collections.unmodifiableMap(modifiableDeletedConfigNodes);

    private final Map<JcrPath, ConfigurationPropertyImpl> modifiableDeletedConfigProperties = new HashMap<>();
    private final Map<JcrPath, ConfigurationPropertyImpl> deletedConfigProperties = Collections.unmodifiableMap(modifiableDeletedConfigProperties);
    private final Set<String> modifiableSiteNames = new HashSet<>();
    private final Set<String> siteNames = Collections.unmodifiableSet(modifiableSiteNames);
    private final Set<JcrPath> modifiableHstRoots = new HashSet<>();
    private final Set<JcrPath> hstRoots = Collections.unmodifiableSet(modifiableHstRoots);

    // Used for cleanup when done with this ConfigurationModel
    private Set<FileSystem> filesystems = new HashSet<>();

    private long buildTimeStamp = 0;

    public List<SiteImpl> getSites() {
        return Collections.unmodifiableList(sites);
    }

    @Override
    public List<GroupImpl> getSortedGroups() {
        return sortedGroups;
    }

    /**
     * Convenience method for client code that wants to iterate over modules using streams.
     */
    public Stream<ModuleImpl> getModulesStream() {
        return getSortedGroups().stream()
                .flatMap(g -> g.getProjects().stream())
                .flatMap(p -> p.getModules().stream());
    }

    /**
     * Convenience method for client code that wants to iterate over modules using a for-each loop.
     * DO NOT reuse the return value in more than one loop, as the underlying stream will not support this!
     */
    public Iterable<ModuleImpl> getModules() {
        return getModulesStream()::iterator;
    }

    /**
     * The set of all names of sites present in this model. The "core" is always assumed to be present and
     * does not have an explicit representation. Thus, a core-only model will return an empty Set.
     * @return a Set of names for hcm sites present in this model; does not contain null
     * @since 2.0
     */
    @Override
    public Set<String> getSiteNames() {
        return siteNames;
    }

    @Override
    public Set<JcrPath> getHstRoots() {
        return hstRoots;
    }

    @Override
    public List<NamespaceDefinitionImpl> getNamespaceDefinitions() {
        return namespaceDefinitions;
    }

    @Override
    public List<WebFileBundleDefinitionImpl> getWebFileBundleDefinitions() {
        return webFileBundleDefinitions;
    }

    @Override
    public List<ContentDefinitionImpl> getContentDefinitions() {
        return contentDefinitions;
    }

    @Override
    public ConfigurationNodeImpl getConfigurationRootNode() {
        return configurationRootNode;
    }

    public Map<JcrPath, ConfigurationNodeImpl> getDeletedConfigNodes() {
        return deletedConfigNodes;
    }

    private void addContentDefinitions(final Collection<ContentDefinitionImpl> definitions) {
        modifiableContentDefinitions.addAll(definitions);
    }

    private void addConfigDefinitions(final Collection<ConfigDefinitionImpl> definitions) {
        modifiableConfigDefinitions.addAll(definitions);
    }

    private void addNamespaceDefinitions(final Collection<NamespaceDefinitionImpl> definitions) {
        modifiableNamespaceDefinitions.addAll(definitions);
    }

    private void addWebFileBundleDefinitions(final Collection<WebFileBundleDefinitionImpl> definitions) {
        for (WebFileBundleDefinitionImpl definition : definitions) {
            ensureUniqueBundleName(definition);
        }
        modifiableWebFileBundleDefinitions.addAll(definitions);
    }

    private void setConfigurationRootNode(final ConfigurationNodeImpl configurationRootNode) {
        this.configurationRootNode = configurationRootNode;
    }

    /**
     * Add a module to this model. Note: This is merely a convenience for {@link #addGroup(GroupImpl)}. Other modules
     * in the same group will also be added as a side-effect.
     * @param module the module to add
     * @return this, for chaining
     */
    public ConfigurationModelImpl addModule(final ModuleImpl module) {
        return addGroup(module.getProject().getGroup());
    }

    /**
     * Add a group of projects and modules to this model. This method is directly useful mainly for copying modules
     * from an existing model.
     * @param group the group to add
     * @return this, for chaining
     */
    public ConfigurationModelImpl addGroup(final GroupImpl group) {
        final GroupImpl consolidatedGroup = getConsolidatedGroup(group);
        for (ProjectImpl project : group.getProjects()) {
            final ProjectImpl consolidatedProject = consolidatedGroup.getOrAddProject(project.getName());
            consolidatedProject.addAfter(project.getAfter());
            for (ModuleImpl module : project.getModules()) {
                if (!replacements.contains(module)) {
                    consolidatedProject.addModule(module);
                }
            }
        }
        return this;
    }

    private GroupImpl getConsolidatedGroup(final GroupImpl group) {
        // Ensure uniqueness of Sites by name
        final String siteName = group.getSite().getName();
        if (!buildingSites.containsKey(siteName)) {
            buildingSites.put(siteName, new SiteImpl(siteName));
        }
        final SiteImpl consolidatedSite = buildingSites.get(siteName);

        final GroupImpl consolidatedGroup = consolidatedSite.getOrAddGroup(group.getName());
        consolidatedGroup.addAfter(group.getAfter());
        return consolidatedGroup;
    }

    /**
     * Note: calling this method directly, rather than addGroup(), has the important effect of disabling the normal
     * validation check to prevent adding two modules with the same full-name. This is intended to support the use-case
     * where a new clone of a module will be used to replace an existing module from an existing ConfigurationModel.
     * Call this method with any new replacement modules before adding the groups from the existing model instance.
     * @param module the new module to add as a replacement
     * @return this, for chaining
     */
    public ConfigurationModelImpl addReplacementModule(final ModuleImpl module) {
        // this duplicates much of the logic of addGroup(),
        // but it's necessary to avoid grabbing undesired sibling modules
        final GroupImpl group = module.getProject().getGroup();
        final GroupImpl consolidatedGroup = getConsolidatedGroup(group);

        // don't loop here -- just append the param module (and no siblings)
        final ProjectImpl project = module.getProject();
        final ProjectImpl consolidatedProject = consolidatedGroup.getOrAddProject(project.getName());
        consolidatedProject.addAfter(project.getAfter());

        if (!replacements.contains(module)) {
            consolidatedProject.addModule(module);
        }

        // now that we've added the module, store a reference for filtering out new copies we might encounter later
        replacements.add(module);
        return this;
    }

    /**
     * Rebuild internal data structures, including definition lists and the merged configuration tree.
     * @return this, for chaining
     */
    public ConfigurationModelImpl build() {
        buildTimeStamp = System.currentTimeMillis();

        buildModel();
        buildConfiguration();
        return this;
    }

    public ConfigurationModelImpl buildModel() {
        // if no groups or modules have been added since the last build, this method is a noop
        if (buildingSites.isEmpty()) {
            return this;
        }

        replacements.clear();
        groups.clear();

        // TODO: assume build is always additive -- start from a new ConfigurationModelImpl instance to remove sites
//        sites.clear();

        sites.addAll(buildingSites.values());
        buildingSites.clear();

        // TODO: tests do not always have a "core", which is odd
        siteSorter.sort(sites, Collections.singleton(CORE_NAME));

        // core groups are special -- they satisfy dependencies for other groups in any site
        final SiteImpl core = sites.get(0);
        core.sortGroups(null);
        groups.addAll(core.getGroups());

        // there might be only the core site
        if (sites.size() > 1) {
            for (SiteImpl site : sites.subList(1, sites.size())) {
                site.sortGroups(core);
                groups.addAll(site.getGroups());
            }
        }

        return this;
    }

    public ConfigurationModelImpl buildConfiguration() {

        modifiableNamespaceDefinitions.clear();
        modifiableConfigDefinitions.clear();
        modifiableContentDefinitions.clear();
        modifiableWebFileBundleDefinitions.clear();
        modifiableDeletedConfigNodes.clear();
        modifiableDeletedConfigProperties.clear();
        modifiableSiteNames.clear();
        modifiableHstRoots.clear();

        final ConfigurationTreeBuilder configurationTreeBuilder = new ConfigurationTreeBuilder();

        // sort modules so that hcm site modules would be at the bottom of the list (including dependencies)
        // TODO: fix groupSorter to do this properly with lexical sort by HCM Site name, so getModulesStream() does this consistently
        // TODO: disallow dependencies that force an ordering that violates HCM Site isolation
        getModulesStream()
                .filter(m -> !m.isNotCore())
                .forEach(module -> buildModule(configurationTreeBuilder, module));
        getModulesStream()
                .filter(ModuleImpl::isNotCore)
                .forEach(module -> buildModule(configurationTreeBuilder, module));

        setConfigurationRootNode(configurationTreeBuilder.build());

        modifiableDeletedConfigNodes.putAll(configurationTreeBuilder.getDeletedNodes());
        modifiableDeletedConfigProperties.putAll(configurationTreeBuilder.getDeletedProperties());

        return this;
    }

    private void buildModule(final ConfigurationTreeBuilder configurationTreeBuilder, final ModuleImpl module) {
        log.debug("Merging module {}", module.getFullName());
        if (!module.getNamespaceDefinitions().isEmpty() && module.isNotCore()) {
            final ParserException parserException = new ParserException(String.format("Namespace definition can not be a part of site module: %s",
                    module.getFullName()));
            parserException.setSource(module.getNamespaceDefinitions().get(0).getSource().toString());
            throw parserException;
        }

        addNamespaceDefinitions(module.getNamespaceDefinitions());
        addConfigDefinitions(module.getConfigDefinitions());
        addContentDefinitions(module.getContentDefinitions());
        addWebFileBundleDefinitions(module.getWebFileBundleDefinitions());
        module.getConfigDefinitions().forEach(configurationTreeBuilder::push);
        configurationTreeBuilder.finishModule();

        // TODO: do this more efficiency with Sites, not Modules
        if (module.isNotCore()) {
            modifiableSiteNames.add(module.getSiteName());
        }
        if (module.getHstRoot() != null) {
            modifiableHstRoots.add(module.getHstRoot());
        }
    }

    /**
     * Resolve top deleted node
     */
    public ConfigurationNodeImpl resolveDeletedNode(final JcrPath path) {
        return deletedConfigNodes.get(path);
    }

    public ConfigurationNodeImpl resolveDeletedSubNodeRoot(final JcrPath path) {
        return deletedConfigNodes.values().stream()
                .filter(deletedRootNode -> !path.equals(deletedRootNode.getJcrPath()) && path.startsWith(deletedRootNode.getJcrPath()))
                .findFirst().orElse(null);
    }

    /**
     * Resolve child in deleted node
     */
    public ConfigurationNodeImpl resolveDeletedSubNode(final JcrPath path) {

        final ConfigurationNodeImpl deletedRootNode = resolveDeletedSubNodeRoot(path);
        if (deletedRootNode != null) {
            final JcrPath pathDiff = deletedRootNode.getJcrPath().relativize(path);
            ConfigurationNodeImpl currentNode = deletedRootNode;
            for (final JcrPathSegment jcrPathSegment : pathDiff) {
                ConfigurationNodeImpl nextNode = currentNode.getNode(jcrPathSegment);
                if (nextNode == null) {
                    nextNode = currentNode.getNode(jcrPathSegment.forceIndex());
                }

                currentNode = nextNode;
                if (currentNode == null) {
                    break; //wrong path
                } else if (currentNode.getJcrPath().equals(path)) {
                    return currentNode;
                }
            }
        }
        return null;
    }

    public ConfigurationPropertyImpl resolveDeletedProperty(final JcrPath path) {
        return deletedConfigProperties.get(path);
    }

    /**
     * Compare core and all sites of this model with the corresponding core and sites in another model,
     * based on the algorithm described in {@link #getDigest(String)}. If this model has a site that the other
     * does not, this method returns false. If the other model has additional sites not represented here, but
     * core and all other sites match, this method returns true.
     *
     * @param other another model with which to compare this one
     * @return true if there is a match of all sites here, and false if there is any explicit mismatch or missing
     *              sites in other
     */
    public boolean currentSitesMatchByDigests(final ConfigurationModel other) {
        if (!getDigest(null).equals(other.getDigest(null))) {
            return false;
        }

        for (final String site : siteNames) {
            if (!getDigest(site).equals(other.getDigest(site))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compute a digest of contents of this model.
     *
     * The digest is internally based on a manifest of contents. The format will be a YAML document as follows.
     * <pre>
     * for each Module:
     * [group-name]/[project-name]/[module-name]:
     *     for module descriptor:
     *     MODULE_MANIFEST_PATH:[MD5-digest]
     *     for actions file:
     *     ACTIONS_MANIFEST_PATH:[MD5-digest]
     *     for each configuration Source:
     *     [module-relative-path]:[MD5-digest]
     *         for each referenced configuration resource:
     *     [module-relative-path]:[MD5-digest]
     *     for each content Source:
     *     [module-relative-path]:[content-path]
     * </pre>
     * Note that to preserve stability of output, the manifest must be consistently sorted. This format will use
     * lexical order of the full group-project-module name string to sort Modules, and then lexical order of the path
     * strings to sort all Source and resource references together within a Module. Note also that resource paths will
     * be normalized to use Module-relative paths, rather than the mix of Module- and Source-relative
     * paths as used within the Source text. Resource paths will use 4-spaces indentation, and Modules will use none.
     * Lines will use a single "\n" line separator, and the final resource reference will end in a line separator.
     * @param hcmSiteName the name of an HCM Site whose digest is desired, or null for the core digest
     * @return String representation of complete manifest of contents
     */
    @Override
    public String getDigest(String hcmSiteName) {
        TreeMap<ModuleImpl,TreeMap<String,String>> manifest = new TreeMap<>();
        if (hcmSiteName == null) {
            hcmSiteName = CORE_NAME;
        }

        // for each module, accumulate manifest items
        for (ModuleImpl m : getModules()) {
            if (StringUtils.equalsIgnoreCase(hcmSiteName, m.getSiteName())) {
                m.compileManifest(this, manifest);
            }
        }

        final String modelManifest = manifestToString(manifest);
        log.debug("model manifest for HCM Site {}:\n{}", hcmSiteName, modelManifest);

        return DigestUtils.computeManifestDigest(modelManifest);
    }
    
    /**
     * Helper for getDigest()
     * @param manifest
     * @return
     */
    protected static String manifestToString(final TreeMap<ModuleImpl, TreeMap<String, String>> manifest) {
        // print to final manifest String (with ~10k initial buffer size)
        StringBuilder sb = new StringBuilder(10000);

        manifest.forEach((m,items) -> {
            sb.append(m.getFullName());
            sb.append(":\n");
            items.forEach((p,d) -> {
                sb.append("    ");
                sb.append(p);
                sb.append(": ");
                sb.append(d);
                sb.append('\n');
            });
        });

        return sb.toString();
    }

    /**
     * Used for cleaning up resources in close(), when client code is done with this model.
     */
    public void setFileSystems(Set<FileSystem> fs) {
        this.filesystems = fs;
    }

    /**
     * Closes open FileSystems used by ResourceInputProviders.
     */
    public void close() {
        for (FileSystem fs : filesystems) {
            try {
                fs.close();
            } catch (IOException e) {
                log.warn("Failed to close ConfigurationModel file system", e);
            }
        }
        filesystems.clear();
    }

    private void ensureUniqueBundleName(final WebFileBundleDefinitionImpl newDefinition) {
        for (WebFileBundleDefinitionImpl existingDefinition : webFileBundleDefinitions) {
            if (existingDefinition.getName().equals(newDefinition.getName())) {
                final String msg = String.format(
                        "Duplicate web file bundle with name '%s' found in source files '%s' and '%s'.",
                        newDefinition.getName(),
                        existingDefinition.getOrigin(),
                        newDefinition.getOrigin());
                throw new IllegalStateException(msg);
            }
        }
    }

    public ConfigurationNodeImpl resolveNode(String path) {
        return resolveNode(JcrPaths.getPath(path));
    }

    /**
     * Find a ConfigurationNode by its absolute path.
     * @param path the path of a node
     * @return a ConfigurationNode or null, if no node exists with this path
     */
    @Override
    public ConfigurationNodeImpl resolveNode(JcrPath path) {
        // special handling for root node
        if (path.isRoot()) {
            return getConfigurationRootNode();
        }

        ConfigurationNodeImpl currentNode = getConfigurationRootNode();
        for (JcrPathSegment segment : path) {
            currentNode = currentNode.getNode(segment.forceIndex());
            if (currentNode == null) {
                return null;
            }
        }
        return currentNode;
    }

    public ConfigurationPropertyImpl resolveProperty(String path) {
        return resolveProperty(JcrPaths.getPath(path));
    }

    /**
     * Find a ConfigurationProperty by its absolute path.
     * @param path the path of a property
     * @return a ConfigurationProperty or null, if no property exists with this path
     */
    @Override
    public ConfigurationPropertyImpl resolveProperty(JcrPath path) {
        ConfigurationNodeImpl node = resolveNode(path.getParent());
        if (node == null) {
            return null;
        }
        else {
            return node.getProperty(path.getLastSegment().toString());
        }
    }

    public ContentDefinitionImpl getNearestContentDefinition(final String path) {
        return getNearestContentDefinition(JcrPaths.getPath(path));
    }

    @Override
    public ContentDefinitionImpl getNearestContentDefinition(final JcrPath path) {
        JcrPath originPath = JcrPaths.ROOT;
        ContentDefinitionImpl defValue = null;
        for (ContentDefinitionImpl def : getContentDefinitions()) {
            // this def is a better candidate if it has a starting substring match
            if (path.startsWith(def.getRootPath())
                    // and that subpath is longer than the previous match
                    && (def.getRootPath().getSegmentCount() > originPath.getSegmentCount())) {
                originPath = def.getRootPath();
                defValue = def;
            }
        }
        return defValue;
    }

    @Override
    public String toString() {
        return ConfigurationModelImpl.class.getSimpleName()
                + "{built="
                + ((buildTimeStamp == 0)?
                    "never"
                    :DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(buildTimeStamp))
                + "}";

    }

    /**
     * Combine the provided source modules with all of the other modules from an existing model.
     * @param sourceModules the new source modules
     * @param model model from which we want to extract all modules that don't overlap with source modules
     * @return a new, fully-built model combining modules from the params
     */
    public static ConfigurationModelImpl mergeWithSourceModules(final Collection<ModuleImpl> sourceModules,
                                                                final ConfigurationModelImpl model) {
        try (ConfigurationModelImpl mergedModel = new ConfigurationModelImpl()) {
            // start with the source modules
            sourceModules.forEach(mergedModel::addReplacementModule);

            // then layer on top all of the other modules
            model.getSortedGroups().forEach(mergedModel::addGroup);

            return mergedModel.build();
        }
    }

    /**
     * Combine the source modules from an existingModel with all of the other modules from a newModel.
     * @param existingModel model from which we want to extract source modules and no other modules
     * @param newModel model from which we want to extract all modules that don't overlap with source modules
     * @return a new, fully-built model combining modules from the params
     */
    public static ConfigurationModelImpl mergeWithSourceModules(final ConfigurationModelImpl existingModel,
                                                                final ConfigurationModelImpl newModel) {
        try (ConfigurationModelImpl mergedModel = new ConfigurationModelImpl()) {
            // preserve the source modules
            for (ModuleImpl module : existingModel.getModules()) {
                if (module.getMvnPath() != null) {
                    log.debug("Merging module: {}", module);
                    mergedModel.addReplacementModule(module);
                }
            }

            // layer on top all of the other modules
            newModel.getSortedGroups().forEach(mergedModel::addGroup);

            return mergedModel.build();

        }
    }
}
