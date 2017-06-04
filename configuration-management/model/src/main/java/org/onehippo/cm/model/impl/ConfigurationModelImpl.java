/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.model.Constants.DEFAULT_DIGEST;
import static org.onehippo.cm.model.util.SnsUtils.createIndexedName;

public class ConfigurationModelImpl implements ConfigurationModel {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationModelImpl.class);

    private static final OrderableListSorter<GroupImpl> groupSorter = new OrderableListSorter<>(Group.class.getSimpleName());

    private final Map<String, GroupImpl> groupMap = new HashMap<>();
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
    private final List<ConfigDefinitionImpl> configDefinitions = Collections.unmodifiableList(modifiableConfigDefinitions);

    // Used for cleanup when done with this ConfigurationModel
    private Set<FileSystem> filesystems = new HashSet<>();

    @Override
    public List<GroupImpl> getSortedGroups() {
        return sortedGroups;
    }

    @Override
    public List<NamespaceDefinitionImpl> getNamespaceDefinitions() {
        return namespaceDefinitions;
    }

    @Override
    public ConfigurationNodeImpl getConfigurationRootNode() {
        return configurationRootNode;
    }

    @Override
    public List<WebFileBundleDefinitionImpl> getWebFileBundleDefinitions() {
        return webFileBundleDefinitions;
    }

    @Override
    public List<ContentDefinitionImpl> getContentDefinitions() {
        return contentDefinitions;
    }

    private void addContentDefinitions(final Collection<ContentDefinitionImpl> definitions) {
        modifiableContentDefinitions.addAll(definitions);
    }

    private void addConfigDefinitions(final Collection<ConfigDefinitionImpl> definitions) {
        modifiableConfigDefinitions.addAll(definitions);
    }

    private void addNamespaceDefinitions(final List<NamespaceDefinitionImpl> definitions) {
        modifiableNamespaceDefinitions.addAll(definitions);
    }

    private void setConfigurationRootNode(final ConfigurationNodeImpl configurationRootNode) {
        this.configurationRootNode = configurationRootNode;
    }

    private void addWebFileBundleDefinitions(final List<WebFileBundleDefinitionImpl> definitions) {
        for (WebFileBundleDefinitionImpl definition : definitions) {
            ensureUniqueBundleName(definition);
        }
        modifiableWebFileBundleDefinitions.addAll(definitions);
    }

    public ConfigurationModelImpl addGroup(final GroupImpl group) {
        if (!groupMap.containsKey(group.getName())) {
            groupMap.put(group.getName(), new GroupImpl(group.getName()));
        }
        final GroupImpl consolidatedGroup = groupMap.get(group.getName());
        consolidatedGroup.addAfter(group.getAfter());
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

    /**
     * Note: calling this method directly, rather than addGroup(), has the important effect of disabling the normal
     * validation check to prevent adding two modules with the same full-name. This is intended to support the use-case
     * where a new clone of a module will be used to replace an existing module from an existing ConfigurationModel.
     * Call this method with any new replacement modules before adding the groups from the existing model instance.
     * @param module the new module to add as a replacement
     * @return this
     */
    public ConfigurationModelImpl addModule(final ModuleImpl module) {
        addGroup(module.getProject().getGroup());
        replacements.add(module);
        return this;
    }

    public ConfigurationModelImpl build() {
        buildModel();
        buildConfiguration();
        return this;
    }

    public ConfigurationModelImpl buildModel() {
        groups.clear();
        groups.addAll(groupMap.values());
        groupSorter.sort(groups);
        groups.forEach(GroupImpl::sortProjects);
        return this;
    }

    public ConfigurationModelImpl buildConfiguration() {

        modifiableNamespaceDefinitions.clear();
        modifiableConfigDefinitions.clear();
        modifiableContentDefinitions.clear();
        modifiableWebFileBundleDefinitions.clear();

        final ConfigurationTreeBuilder configurationTreeBuilder = new ConfigurationTreeBuilder();
        for (GroupImpl g : groups) {
            for (ProjectImpl p : g.getProjects()) {
                for (ModuleImpl module : p.getModules()) {
                    log.info("Merging module {}", module.getFullName());
                    addNamespaceDefinitions(module.getNamespaceDefinitions());
                    addConfigDefinitions(module.getConfigDefinitions());
                    addContentDefinitions(new HashSet<>(module.getContentDefinitions()));
                    addWebFileBundleDefinitions(module.getWebFileBundleDefinitions());
                    module.getConfigDefinitions().forEach(configurationTreeBuilder::push);
                }
            }
        }
        setConfigurationRootNode(configurationTreeBuilder.build());
        return this;
    }
    
    /**
     * Compile a manifest of contents. Format will be a YAML document as follows.
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
     * @return String representation of complete manifest of contents
     */
    @Override
    public String getDigest() {
        TreeMap<ModuleImpl,TreeMap<String,String>> manifest = new TreeMap<>();
        // for each module, accumulate manifest items
        for (GroupImpl g : getSortedGroups()) {
            for (ProjectImpl p : g.getProjects()) {
                for (ModuleImpl m : p.getModules()) {
                    m.compileManifest(this, manifest);
                }
            }
        }

        final String modelManifest = manifestToString(manifest);
        log.debug("model manifest:\n{}", modelManifest);

        return computeManifestDigest(modelManifest);
    }

    /**
     * Helper method to compute a digest string from a ConfigurationModel manifest.
     * @param modelManifest the manifest whose digest we want to compute
     * @return a digest string comparable to the baseline digest string, or "" if none can be computed
     */
    protected String computeManifestDigest(final String modelManifest) {
        try {
            MessageDigest md = MessageDigest.getInstance(DEFAULT_DIGEST);
            byte[] digest = md.digest(StandardCharsets.UTF_8.encode(modelManifest).array());
            String modelDigestString = ModuleImpl.toDigestHexString(digest);
            log.debug("model digest:\n{}", modelDigestString);

            return modelDigestString;
        }
        catch (NoSuchAlgorithmException e) {
            // NOTE: this should never happen, since the Java spec requires MD5 to be supported
            log.error("{} algorithm not available for configuration baseline diff", DEFAULT_DIGEST, e);
            return "";
        }
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

    @Override
    public ConfigurationNodeImpl resolveNode(String path) {
        String[] segments = StringUtils.stripStart(path, "/").split("/");

        ConfigurationNodeImpl currentNode = getConfigurationRootNode();
        for (String segment : segments) {
            currentNode = currentNode.getNode(createIndexedName(segment));
            if (currentNode == null) {
                return null;
            }
        }
        return currentNode;
    }

    @Override
    public ConfigurationPropertyImpl resolveProperty(String path) {
        ConfigurationNodeImpl node = resolveNode(StringUtils.substringBeforeLast(path, "/"));
        if (node == null) {
            return null;
        }
        else {
            return node.getProperty(StringUtils.substringAfterLast(path, "/"));
        }
    }

    /**
     * Find the ContentDefinition (not config) that has the longest common substring to the given path
     * @param path
     * @return
     */
    public Optional<ContentDefinitionImpl> findClosestContentDefinition(final String path) {
        // Use the Path class to represent the JCR path, since all we need is a simple startsWith() comparison
        Path p = Paths.get(path);

        // make sure content definitions are sorted by lexical order of root path
        final TreeSet<ContentDefinitionImpl> reverse = new TreeSet<>(Comparator.reverseOrder());
        reverse.addAll(getContentDefinitions());

        // check for prefix match on path in reverse lexical order -- first match is longest prefix match
        return getContentDefinitions().stream().filter(cd -> p.startsWith(Paths.get(cd.getNode().getPath()))).findFirst();
    }
}
