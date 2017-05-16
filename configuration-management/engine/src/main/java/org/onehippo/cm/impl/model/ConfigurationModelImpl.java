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

package org.onehippo.cm.impl.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.onehippo.cm.api.model.ConfigurationModel;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Group;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.WebFileBundleDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.engine.Constants.DEFAULT_DIGEST;

public class ConfigurationModelImpl implements ConfigurationModel {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationModelImpl.class);

    private List<Group> sortedGroups;
    private final List<NamespaceDefinition> namespaceDefinitions = new ArrayList<>();
    private final List<NodeTypeDefinition> nodeTypeDefinitions = new ArrayList<>();

    private ConfigurationNode configurationRootNode;

    private final List<WebFileBundleDefinition> webFileBundleDefinitions = new ArrayList<>();
    private final List<ContentDefinition> contentDefinitions = new ArrayList<>();

    // Used for cleanup when done with this ConfigurationModel
    private Set<FileSystem> filesystems = new HashSet<>();

    @Override
    public List<Group> getSortedGroups() {
        return sortedGroups;
    }

    @Override
    public List<NamespaceDefinition> getNamespaceDefinitions() {
        return namespaceDefinitions;
    }

    @Override
    public List<NodeTypeDefinition> getNodeTypeDefinitions() {
        return nodeTypeDefinitions;
    }

    @Override
    public ConfigurationNode getConfigurationRootNode() {
        return configurationRootNode;
    }

    @Override
    public List<WebFileBundleDefinition> getWebFileBundleDefinitions() {
        return webFileBundleDefinitions;
    }

    @Override
    public List<ContentDefinition> getContentDefinitions() {
        return contentDefinitions;
    }

    @Override
    public void addContentDefinition(final ContentDefinition definition) {
        contentDefinitions.add(definition);
    }

    @Override
    public void addContentDefinitions(final Collection<ContentDefinition> definitions) {
        contentDefinitions.addAll(definitions);
    }

    public void setSortedGroups(final List<GroupImpl> sortedGroups) {
        this.sortedGroups = new ArrayList<>(sortedGroups);
    }

    public void addNamespaceDefinitions(final List<NamespaceDefinitionImpl> definitions) {
        namespaceDefinitions.addAll(definitions);
    }

    public void addNodeTypeDefinitions(final List<NodeTypeDefinitionImpl> definitions) {
        nodeTypeDefinitions.addAll(definitions);
    }

    public void setConfigurationRootNode(final ConfigurationNode configurationRootNode) {
        this.configurationRootNode = configurationRootNode;
    }

    public void addWebFileBundleDefinitions(final List<WebFileBundleDefinitionImpl> definitions) {
        for (WebFileBundleDefinitionImpl definition : definitions) {
            ensureUniqueBundleName(definition);
        }
        webFileBundleDefinitions.addAll(definitions);
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
        // accumulate modules in sorted order
        TreeSet<Module> modules = new TreeSet<>();
        getSortedGroups().forEach(g -> g.getProjects().forEach(p -> p.getModules().forEach(m -> modules.add(m))));

        // for each module, accumulate manifest items
        TreeMap<Module,TreeMap<String,String>> manifest = new TreeMap<>();
        for (Module module : modules) {
            ((ModuleImpl)module).compileManifest(this, manifest);
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
    protected static String manifestToString(final TreeMap<Module, TreeMap<String, String>> manifest) {
        // print to final manifest String (with ~10k initial buffer size)
        StringBuilder sb = new StringBuilder(10000);

        manifest.forEach((m,items) -> {
            sb.append(ModuleImpl.buildFullName(m));
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
     * @throws IOException
     */
    public void close() throws IOException {
        for (FileSystem fs : filesystems) {
            fs.close();
        }
    }

    private void ensureUniqueBundleName(final WebFileBundleDefinitionImpl newDefinition) {
        for (WebFileBundleDefinition existingDefinition : webFileBundleDefinitions) {
            if (existingDefinition.getName().equals(newDefinition.getName())) {
                final String msg = String.format(
                        "Duplicate web file bundle with name '%s' found in source files '%s' and '%s'.",
                        newDefinition.getName(),
                        ModelUtils.formatDefinition(existingDefinition),
                        ModelUtils.formatDefinition(newDefinition));
                throw new IllegalStateException(msg);
            }
        }
    }
}
