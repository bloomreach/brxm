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

package org.onehippo.cm.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.onehippo.cm.api.MergedModel;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.ConfigDefinition;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.WebFileBundleDefinition;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ModelUtils;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.NamespaceDefinitionImpl;
import org.onehippo.cm.impl.model.NodeTypeDefinitionImpl;
import org.onehippo.cm.impl.model.WebFileBundleDefinitionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.engine.Constants.ACTIONS_YAML;
import static org.onehippo.cm.engine.Constants.DEFAULT_DIGEST;
import static org.onehippo.cm.engine.Constants.REPO_CONFIG_YAML;

public class MergedModelImpl implements MergedModel {

    private static final Logger log = LoggerFactory.getLogger(MergedModelImpl.class);

    private List<Configuration> sortedConfigurations;
    private final List<NamespaceDefinition> namespaceDefinitions = new ArrayList<>();
    private final List<NodeTypeDefinition> nodeTypeDefinitions = new ArrayList<>();
    private ConfigurationNode configurationRootNode;
    private final List<WebFileBundleDefinition> webFileBundleDefinitions = new ArrayList<>();
    private final Map<Module, ResourceInputProvider> resourceInputProviders = new HashMap<>();

    // Used for cleanup when done with this MergedModel
    private Set<FileSystem> filesystems = new HashSet<>();

    @Override
    public List<Configuration> getSortedConfigurations() {
        return sortedConfigurations;
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
    public Map<Module, ResourceInputProvider> getResourceInputProviders() {
        return resourceInputProviders;
    }

    public void setSortedConfigurations(final List<ConfigurationImpl> sortedConfigurations) {
        this.sortedConfigurations = new ArrayList<>(sortedConfigurations);
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

    public void addResourceInputProviders(Map<Module, ResourceInputProvider> resourceInputProviders) {
        for (Module module : resourceInputProviders.keySet()) {
            if (this.resourceInputProviders.containsKey(module)) {
                final String msg = String.format(
                        "ResourceInputProviders for module '%s' already added before.",
                        ModelUtils.formatModule(module)
                );
                throw new IllegalArgumentException(msg);
            }
            this.resourceInputProviders.put(module, resourceInputProviders.get(module));
        }
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
    public String compileManifest() {
        // accumulate modules in sorted order
        TreeSet<Module> modules = new TreeSet<>();
        getSortedConfigurations().forEach(g -> g.getProjects().forEach(p -> p.getModules().forEach(m -> modules.add(m))));

        // for each module, accumulate manifest items
        TreeMap<Module,TreeMap<String,String>> manifest = new TreeMap<>();
        for (Module module : modules) {
            TreeMap<String,String> items = new TreeMap<>();

            // get the resource input provider, which provides access to raw data for module content
            ResourceInputProvider rip = getResourceInputProviders().get(module);
            if (rip == null) {
                log.warn("Cannot find ResourceInputProvider for module {}", module.getName());
            }

            // digest the descriptor
            digestResource(null, "/"+REPO_CONFIG_YAML, rip, items);

            // digest the actions file
            digestResource(null, "/"+ACTIONS_YAML, rip, items);

            // for each content source
            for (Source source : module.getContentSources()) {
                // assume that there is exactly one content definition here, as required
                ContentDefinition firstDef = (ContentDefinition) source.getDefinitions().get(0);

                // add the first definition path to manifest
                items.put("/"+source.getPath(), firstDef.getNode().getPath());
            }

            // for each config source
            for (Source source : module.getConfigSources()) {
                // digest the source
                digestResource(source, "/"+source.getPath(), rip, items);

                // for each definition
                for (Definition def : source.getDefinitions()) {
                    switch (def.getType()) {
                        case NAMESPACE:
                        case WEBFILEBUNDLE:
                            // no special processing required
                            break;
                        case CONTENT:
                            // this shouldn't exist here anymore, but we'll let the verifier handle it
                            break;
                        case CND:
                            // digest cnd, if stored in a resource
                            NodeTypeDefinition ntd = (NodeTypeDefinition) def;
                            if (ntd.isResource()) {
                                String cndPath = ntd.getValue();
                                digestResource(source, cndPath, rip, items);
                            }
                            break;
                        case CONFIG:
                            // recursively find all config resources and digest them
                            ConfigDefinition configDef = (ConfigDefinition) def;
                            digestResourcesForNode(configDef.getNode(), source, rip, items);
                            break;
                    }
                }
            }

            // add items to manifest
            manifest.put(module, items);
        }

        // print to final manifest String (with ~10k initial buffer size)
        StringBuilder sb = new StringBuilder(10000);

        manifest.forEach((m,items) -> {
            sb.append(ModuleImpl.buildFullName(m));
            sb.append(":\n");
            items.forEach((p,d) -> {
                sb.append("    ");
                sb.append(p);
                sb.append(':');
                sb.append(d);
                sb.append('\n');
            });
        });

        return sb.toString();
    }

    /**
     * Recursively accumulate digests for resources referenced from the given DefinitionNode or its descendants.
     * @param defNode the DefinitionNode to scan for resource references
     * @param source the Source within which the defNode is defined
     * @param rip provides access to raw data streams
     * @param items accumulator of [path,digest] Strings
     */
    protected void digestResourcesForNode(DefinitionNode defNode, Source source, ResourceInputProvider rip,
                                          TreeMap<String,String> items) {
        // find resource values
        for (DefinitionProperty dp : defNode.getProperties().values()) {
            // if value is a resource, digest it
            Consumer<Value> digester = v -> {
                if (v.isResource()) {
                    digestResource(source, v.getString(), rip, items);
                }
            };

            switch (dp.getType()) {
                case SINGLE:
                    digester.accept(dp.getValue());
                    break;
                case SET: case LIST:
                    for (Value value : dp.getValues()) {
                        digester.accept(value);
                    }
                    break;
            }
        }

        // recursively visit child definition nodes
        for (DefinitionNode dn : defNode.getNodes().values()) {
            digestResourcesForNode(dn, source, rip, items);
        }
    }

    /**
     * Compute and accumulate a crypto digest for the resource referenced by source at path.
     * @param source the Source from which path might be a relative reference
     * @param path iff starts with '/', a module-relative path, else a path relative to source
     * @param rip provides access to raw data streams
     * @param items accumulator of [path,digest] Strings
     */
    protected void digestResource(Source source, String path, ResourceInputProvider rip, TreeMap<String,String> items) {
        // if path starts with /, this is already module-relative, otherwise we must adjust it
        if (!path.startsWith("/")) {
            String sourcePath = source.getPath();

            if (sourcePath.contains("/")) {
                String sourceParent = sourcePath.substring(0, sourcePath.lastIndexOf('/'));
                path = "/" + sourceParent + "/" + path;
            }
            else {
                // TODO: this is a source at the module root -- only happens in temporary aggregate form of config
                path = "/" + path;
            }
        }

        // if the RIP cannot provide an InputStream, short-circuit here
        if (!rip.hasResource(null, path)) {
            return;
        }

        try {
            // use MD5 because it's fast and guaranteed to be supported, and crypto attacks are not a concern here
            MessageDigest md = MessageDigest.getInstance(DEFAULT_DIGEST);

            // digest the InputStream by copying it and discarding the output
            InputStream is = new DigestInputStream(rip.getResourceInputStream(null, path), md);
            IOUtils.copyLarge(is, new NullOutputStream());

            // prepend algorithm using same style as used in Hippo CMS password hashing
            String digestString = "$"+DEFAULT_DIGEST+"$"+ DatatypeConverter.printHexBinary(md.digest());

            items.put(path, digestString);
        }
        catch (IOException|NoSuchAlgorithmException e) {
            throw new RuntimeException("Exception while computing resource digest", e);
        }
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
