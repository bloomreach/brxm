/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.impl.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.api.model.ConfigurationModel;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.ConfigDefinition;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.engine.serializer.ModuleDescriptorSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.engine.Constants.ACTIONS_YAML;
import static org.onehippo.cm.engine.Constants.DEFAULT_DIGEST;
import static org.onehippo.cm.engine.Constants.DEFAULT_EXPLICIT_SEQUENCING;
import static org.onehippo.cm.engine.Constants.HCM_CONFIG_FOLDER;
import static org.onehippo.cm.engine.Constants.HCM_CONTENT_FOLDER;
import static org.onehippo.cm.engine.Constants.HCM_MODULE_YAML;

public class ModuleImpl implements Module, Comparable<Module> {

    private static final Logger log = LoggerFactory.getLogger(ModuleImpl.class);

    private final String name;
    private final Project project;

    /**
     * Warning! not preserved in final ConfigurationModel!
     */
    private final Set<String> modifiableAfter = new LinkedHashSet<>();
    private final Set<String> after = Collections.unmodifiableSet(modifiableAfter);

    private final Set<SourceImpl> sortedSources = new TreeSet<>(Comparator.comparing(Source::getPath));

    private final Set<Source> sources = Collections.unmodifiableSet(sortedSources);

    private final List<NamespaceDefinitionImpl> namespaceDefinitions = new ArrayList<>();

    private final List<NodeTypeDefinitionImpl> nodeTypeDefinitions = new ArrayList<>();

    private final List<ContentDefinitionImpl> contentDefinitions = new ArrayList<>();

    private final List<ConfigDefinitionImpl> configDefinitions = new ArrayList<>();

    private final List<WebFileBundleDefinitionImpl> webFileBundleDefinitions = new ArrayList<>();

    private ResourceInputProvider configResourceInputProvider;

    private ResourceInputProvider contentResourceInputProvider;

    public ModuleImpl(final String name, final ProjectImpl project) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter 'name' cannot be null");
        }
        this.name = name;

        if (project == null) {
            throw new IllegalArgumentException("Parameter 'project' cannot be null");
        }
        this.project = project;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public Set<String> getAfter() {
        return after;
    }

    public ModuleImpl addAfter(final Set<String> after) {
        modifiableAfter.addAll(after);
        return this;
    }

    @Override
    public Set<Source> getSources() {
        return sources;
    }

    @Override
    public Set<Source> getContentSources() {
        return sources.stream().filter(x -> x instanceof ContentSourceImpl).collect(Collectors.toSet());
    }

    @Override
    public Set<Source> getConfigSources() {
        return sources.stream().filter(x -> x instanceof ConfigSourceImpl).collect(Collectors.toSet());
    }

    public SourceImpl addContentSource(final String path) {
        final SourceImpl source = new ContentSourceImpl(path, this);
        return addSource(source);
    }

    public SourceImpl addConfigSource(final String path) {
        final SourceImpl source = new ConfigSourceImpl(path, this);
        return addSource(source);
    }

    /**
     * Returns existing or adds new source to the source list
     * @param source
     * @return
     */
    private SourceImpl addSource(SourceImpl source) {
        return sortedSources.add(source) ? source : sortedSources
                .stream()
                .filter(s -> s.getPath().equals(source.getPath()) && s.getClass().equals(source.getClass()))
                .findFirst().get();
    }

    public Set<SourceImpl> getModifiableSources() {
        return sortedSources;
    }

    /**
     * @return a sorted list of namespace definitions in insertion order.
     * Note that these definitions are only populated for Modules that are part of the {@link ConfigurationModel}.
     */
    public List<NamespaceDefinitionImpl> getNamespaceDefinitions() {
        return namespaceDefinitions;
    }

    /**
     * @return a sorted list of node type definitions in insertion order.
     * Note that these definitions are only populated for Modules that are part of the {@link ConfigurationModel}.
     */
    public List<NodeTypeDefinitionImpl> getNodeTypeDefinitions() {
        return nodeTypeDefinitions;
    }


    /**
     * @return a sorted list of config definitions.
     * Note that these definitions are only populated for Modules that are part of the {@link ConfigurationModel}.
     */
    public List<ConfigDefinitionImpl> getConfigDefinitions() {
        return configDefinitions;
    }

    /**
     * @return a sorted list of content definitions.
     * Note that these definitions are only populated for Modules that are part of the {@link ConfigurationModel}.
     */
    public List<ContentDefinitionImpl> getContentDefinitions() {
        return contentDefinitions;
    }

    /**
     * @return a sorted list of web file bundle definitions in insertion order.
     * Note that these definitions are only populated for Modules that are part of the {@link ConfigurationModel}.
     */
    public List<WebFileBundleDefinitionImpl> getWebFileBundleDefinitions() {
        return webFileBundleDefinitions;
    }

    @Override
    public ResourceInputProvider getConfigResourceInputProvider() {
        return configResourceInputProvider;
    }

    public ModuleImpl setConfigResourceInputProvider(final ResourceInputProvider configResourceInputProvider) {
        this.configResourceInputProvider = configResourceInputProvider;
        return this;
    }

    @Override
    public ResourceInputProvider getContentResourceInputProvider() {
        return contentResourceInputProvider;
    }

    public ModuleImpl setContentResourceInputProvider(final ResourceInputProvider contentResourceInputProvider) {
        this.contentResourceInputProvider = contentResourceInputProvider;
        return this;
    }

    void pushDefinitions(final ModuleImpl module) {
        this.sortedSources.addAll(module.sortedSources);

        // sort definitions into the different types
        module.getSources().forEach(source ->
                source.getDefinitions().forEach(definition -> {
                    if (definition instanceof NamespaceDefinitionImpl) {
                        namespaceDefinitions.add((NamespaceDefinitionImpl) definition);
                    } else if (definition instanceof NodeTypeDefinitionImpl) {
                        ensureSingleSourceForNodeTypes(definition);
                        nodeTypeDefinitions.add((NodeTypeDefinitionImpl) definition);
                    } else if (definition instanceof ConfigDefinitionImpl) {
                        configDefinitions.add((ConfigDefinitionImpl) definition);
                    } else if (definition instanceof ContentDefinitionImpl) {
                        contentDefinitions.add((ContentDefinitionImpl) definition);
                    } else if (definition instanceof WebFileBundleDefinitionImpl) {
                        webFileBundleDefinitions.add((WebFileBundleDefinitionImpl) definition);
                    } else {
                        throw new IllegalStateException("Failed to sort unsupported definition class '"
                                + definition.getClass().getName() + "'.");
                    }
                })
        );

        // sort the content/config definitions, all other remain in insertion order
        configDefinitions.sort(new ContentDefinitionComparator());
    }

    private void ensureSingleSourceForNodeTypes(final Definition nodeTypeDefinition) {
        if (!nodeTypeDefinitions.isEmpty()
                && !nodeTypeDefinition.getSource().getPath().equals(nodeTypeDefinitions.get(0).getSource().getPath())) {
            final String msg = String.format("CNDs are specified in multiple sources of a module: '%s' and '%s'. "
                    + "For proper ordering, they must be specified in a single source.",
                    ModelUtils.formatDefinition(nodeTypeDefinition),
                    ModelUtils.formatDefinition(nodeTypeDefinitions.get(0)));
            throw new IllegalStateException(msg);
        }
    }

    protected void compileManifest(ConfigurationModel model, TreeMap<Module,TreeMap<String,String>> manifest) {
        TreeMap<String,String> items = new TreeMap<>();

        // get the resource input provider, which provides access to raw data for module content
        ResourceInputProvider rip = configResourceInputProvider;

        // digest the descriptor
        // TODO this is an ugly hack in part because RIP uses config root instead of module root
        boolean hasDescriptor = digestResource(null, "/../" + HCM_MODULE_YAML, rip, items);

        // special-case handle a missing descriptor by generating a dummy one, for demo case
        // TODO remove when demo is restructured to use module-specific descriptors
        if (!hasDescriptor) {
            // create a manifest item for a dummy descriptor
            String descriptor = this.compileDummyDescriptor();
            String digest = digestFromStream(IOUtils.toInputStream(descriptor, StandardCharsets.UTF_8));
            items.put("/" + HCM_MODULE_YAML, digest);
        }

        // digest the actions file
        digestResource(null, "/../" + ACTIONS_YAML, rip, items);

        // for each content source
        for (Source source : this.getContentSources()) {
            // assume that there is exactly one content definition here, as required
            ContentDefinition firstDef = (ContentDefinition) source.getDefinitions().get(0);

            // add the first definition path to manifest
            items.put("/" + HCM_CONTENT_FOLDER + "/" + source.getPath(), firstDef.getNode().getPath());
        }

        // for each config source
        for (Source source : this.getConfigSources()) {
            // digest the source
            digestResource(source, "/" + source.getPath(), rip, items);

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
        manifest.put(this, items);
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
    protected boolean digestResource(Source source, String path, ResourceInputProvider rip, TreeMap<String,String> items) {
        // if path starts with /, this is already relative to the config root, otherwise we must adjust it
        if (!path.startsWith("/")) {
            String sourcePath = source.getPath();

            if (sourcePath.contains("/")) {
                String sourceParent = sourcePath.substring(0, sourcePath.lastIndexOf('/'));
                path = "/" + sourceParent + "/" + path;
            }
            else {
                path = "/" + path;
            }
        }

        // if the RIP cannot provide an InputStream, short-circuit here
        if (!rip.hasResource(null, path)) {
            return false;
        }

        try {
            final InputStream is = rip.getResourceInputStream(null, path);
            String digestString = digestFromStream(is);

            // TODO dirty hack because RIP uses paths relative to content root instead of module root
            if (path.startsWith("/../")) {
                path = StringUtils.removeStart(path, "/..");
            }
            else {
                path = "/" + HCM_CONFIG_FOLDER + path;
            }

            items.put(path, digestString);
            return true;
        }
        catch (IOException e) {
            throw new RuntimeException("Exception while computing resource digest", e);
        }
    }

    /**
     * Helper to compute a digest string from an InputStream. This method ensures that the stream is closed.
     * @param is the InputStream to digest
     * @return a digest string suitable for use in a module manifest
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    private String digestFromStream(final InputStream is) {
        try {
            // use MD5 because it's fast and guaranteed to be supported, and crypto attacks are not a concern here
            MessageDigest md = MessageDigest.getInstance(DEFAULT_DIGEST);

            // digest the InputStream by copying it and discarding the output
            try (InputStream dis = new DigestInputStream(is, md)) {
                IOUtils.copyLarge(dis, new NullOutputStream());
            }

            // prepend algorithm using same style as used in Hippo CMS password hashing
            return toDigestHexString(md.digest());
        }
        catch (IOException|NoSuchAlgorithmException e) {
            throw new RuntimeException("Exception while computing resource digest", e);
        }
    }

    /**
     * Helper method to convert a byte[] produced by MessageDigest into a hex string marked with the digest algorithm.
     * @param digest the raw digest byte[]
     * @return a String suitable for long-term storage and eventual comparisons
     */
    public static String toDigestHexString(final byte[] digest) {
        // prepend algorithm using same style as used in Hippo CMS password hashing
        return "$" + DEFAULT_DIGEST + "$" + DatatypeConverter.printHexBinary(digest);
    }

    /**
     * Compile a dummy YAML descriptor file to stand in for special case where demo project uses an aggregated
     * descriptor for a set of modules.
     * TODO remove when demo project is restructured to get rid of aggregated module structure
     * @return a YAML string representing the group->project->module hierarchy and known dependencies for this Module
     */
    public String compileDummyDescriptor() {
        // serialize a dummy module descriptor for this module
        final ModuleDescriptorSerializer moduleDescriptorSerializer = new ModuleDescriptorSerializer(DEFAULT_EXPLICIT_SEQUENCING);

        // create a dummy group->project->module setup with just the data relevant to this Module
        GroupImpl group = new GroupImpl(getProject().getGroup().getName());
        group.addAfter(getProject().getGroup().getAfter());
        ProjectImpl project = group.addProject(getProject().getName());
        project.addAfter(getProject().getAfter());
        ModuleImpl dummyModule = project.addModule(getName());
        dummyModule.addAfter(getAfter());

        log.debug("Creating dummy module descriptor for {}/{}/{}",
                group.getName(), project.getName(), getName());

        HashMap<String, GroupImpl> groups = new HashMap<>();
        groups.put(group.getName(), group);

        // serialize that dummy group
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            moduleDescriptorSerializer.serialize(baos, groups);
            return baos.toString(StandardCharsets.UTF_8.name());
        }
        catch (IOException e) {
            throw new RuntimeException("Problem compiling dummy descriptor", e);
        }
    }

    private class ContentDefinitionComparator implements Comparator<ContentDefinitionImpl> {
        public int compare(final ContentDefinitionImpl def1, final ContentDefinitionImpl def2) {
            final String rootPath1 = def1.getNode().getPath();
            final String rootPath2 = def2.getNode().getPath();

            if (def1 != def2 && rootPath1.equals(rootPath2)) {
                final String msg = String.format(
                        "Duplicate content root paths '%s' in module '%s' in source files '%s' and '%s'.",
                        rootPath1,
                        getName(),
                        ModelUtils.formatDefinition(def1),
                        ModelUtils.formatDefinition(def2));
                throw new IllegalStateException(msg);
            }
            return rootPath1.compareTo(rootPath2);
        }
    }

    @Override
    public String toString() {
        return "ModuleImpl{" +
                "name='" + name + '\'' +
                ", project=" + project +
                '}';
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Module) {
            Module otherModule = (Module)other;
            return this.getName().equals(otherModule.getName()) &&
                    this.getProject().equals(otherModule.getProject());
        }
        return false;
    }

    // hashCode() and equals() should be consistent!
    @Override
    public int hashCode() {
        return Objects.hash(name, project);
    }

    /**
     * @return the full group/project/module name for this module
     */
    public String getFullName() {
        return buildFullName(this);
    }

    /**
     * @return the full group/project/module name for the given module
     */
    public static String buildFullName(Module m) {
        return String.join("/",
                m.getProject().getGroup().getName(),
                m.getProject().getName(),
                m.getName());
    }

    /**
     * Compare on the basis of lexical order of the full group/project/module names.
     * @param o another Module
     */
    @Override
    public int compareTo(final Module o) {
        return getFullName().compareTo(buildFullName(o));
    }
}
