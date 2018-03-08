/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.model.Module;
import org.onehippo.cm.model.definition.ActionItem;
import org.onehippo.cm.model.definition.NamespaceDefinition;
import org.onehippo.cm.model.impl.definition.AbstractDefinitionImpl;
import org.onehippo.cm.model.impl.definition.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.definition.NamespaceDefinitionImpl;
import org.onehippo.cm.model.impl.definition.TreeDefinitionImpl;
import org.onehippo.cm.model.impl.definition.WebFileBundleDefinitionImpl;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.impl.source.ConfigSourceImpl;
import org.onehippo.cm.model.impl.source.ContentSourceImpl;
import org.onehippo.cm.model.impl.source.FileResourceInputProvider;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.parser.ConfigSourceParser;
import org.onehippo.cm.model.parser.ContentSourceHeadParser;
import org.onehippo.cm.model.parser.ParserException;
import org.onehippo.cm.model.parser.SourceParser;
import org.onehippo.cm.model.serializer.ModuleDescriptorSerializer;
import org.onehippo.cm.model.source.ResourceInputProvider;
import org.onehippo.cm.model.source.SourceType;
import org.onehippo.cm.model.util.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.model.Constants.ACTIONS_YAML;
import static org.onehippo.cm.model.Constants.HCM_CONFIG_FOLDER;
import static org.onehippo.cm.model.Constants.HCM_CONTENT_FOLDER;
import static org.onehippo.cm.model.Constants.HCM_MODULE_YAML;

public class ModuleImpl implements Module, Comparable<Module>, Cloneable {

    private static final Logger log = LoggerFactory.getLogger(ModuleImpl.class);

    private final String name;
    private final ProjectImpl project;

    private final Set<String> modifiableAfter = new LinkedHashSet<>();
    private final Set<String> after = Collections.unmodifiableSet(modifiableAfter);

    private final Set<SourceImpl> sortedSources = new TreeSet<>(Comparator
            .comparing(SourceImpl::getPath)
            .thenComparing(x -> x.getClass().getSimpleName()));

    private final Set<SourceImpl> sources = Collections.unmodifiableSet(sortedSources);

    private final List<NamespaceDefinitionImpl> namespaceDefinitions = new ArrayList<>();

    private final List<ContentDefinitionImpl> contentDefinitions = new ArrayList<>();

    private final List<ConfigDefinitionImpl> configDefinitions = new ArrayList<>();

    private final List<WebFileBundleDefinitionImpl> webFileBundleDefinitions = new ArrayList<>();

    private Map<String, Set<ActionItem>> actionsMap = new LinkedHashMap<>();

    private String lastExecutedAction;

    private ResourceInputProvider configResourceInputProvider;

    private ResourceInputProvider contentResourceInputProvider;

    // store marker here to indicate whether this came from a filesystem mvn path at startup
    private String mvnPath;

    private File archiveFile;

    private String extension;

    // set of resource paths that should be removed during auto-export write step
    private Set<String> removedConfigResources = new HashSet<>();
    private Set<String> removedContentResources = new HashSet<>();

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

    /**
     * Special case clone constructor to be used only by {@link ProjectImpl#addModule(ModuleImpl)}.
     */
    ModuleImpl(final ModuleImpl module, final ProjectImpl project) {
        this(module.getName(), project);
        modifiableAfter.addAll(module.getAfter());
        configResourceInputProvider = module.getConfigResourceInputProvider();
        contentResourceInputProvider = module.getContentResourceInputProvider();
        lastExecutedAction = module.lastExecutedAction;
        actionsMap.putAll(module.getActionsMap());

        // TODO: the following two methods require ModuleImpl access, but clone/creation should only use/need Module interface
        sortedSources.addAll(module.getSources());
        // update the back-reference, since this new module will now "own" these Sources
        sortedSources.forEach(source -> source.setModule(this));

        mvnPath = module.getMvnPath();
        extension = module.getExtension();
        archiveFile = module.getArchiveFile();
        build();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ProjectImpl getProject() {
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
    public boolean isArchive() {
        return archiveFile != null;
    }

    @Override
    public File getArchiveFile() {
        return archiveFile;
    }

    public void setArchiveFile(File archiveFile) {
        this.archiveFile = archiveFile;
    }

    @Override
    public Set<SourceImpl> getSources() {
        return sources;
    }

    @Override
    public Set<ContentSourceImpl> getContentSources() {
        return sources.stream()
                .filter(SourceType.CONTENT::isOfType)
                .map(ContentSourceImpl.class::cast)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<ConfigSourceImpl> getConfigSources() {
        return sources.stream()
                .filter(SourceType.CONFIG::isOfType)
                .map(ConfigSourceImpl.class::cast)
                .collect(Collectors.toSet());
    }

    public ContentSourceImpl addContentSource(final String path) {
        return addSource(new ContentSourceImpl(path, this));
    }

    public ConfigSourceImpl addConfigSource(final String path) {
        return addSource(new ConfigSourceImpl(path, this));
    }

    public Optional<ConfigSourceImpl> getConfigSource(final String path) {
        return sources.stream()
                .filter(SourceType.CONFIG::isOfType)
                .map(ConfigSourceImpl.class::cast)
                .filter(source -> source.getPath().equals(path))
                .findFirst();
    }

    public Optional<ContentSourceImpl> getContentSource(final String path) {
        return sources.stream()
                .filter(SourceType.CONTENT::isOfType)
                .map(ContentSourceImpl.class::cast)
                .filter(source -> source.getPath().equals(path))
                .findFirst();
    }

    @Override
    public Map<String, Set<ActionItem>> getActionsMap() {
        return actionsMap;
    }

    @Override
    public String getLastExecutedAction() {
        return lastExecutedAction;
    }

    public void setLastExecutedAction(String value) {
        lastExecutedAction = value;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(final String extension) {
        this.extension = extension;
    }

    /**
     * Returns existing or adds new source to the source list
     * @param source
     * @return
     */
    @SuppressWarnings("unchecked") // source.equals does a class.equals check
    private <S extends SourceImpl> S addSource(S source) {
        return sortedSources.add(source) ? source : (S) sortedSources
                .stream()
                .filter(source::equals)
                .findFirst().get();
    }

    public Set<SourceImpl> getModifiableSources() {
        return sortedSources;
    }

    /**
     * @return a sorted list of namespace definitions in insertion order.
     * Note that these definitions are only populated for Modules that are part of the {@link org.onehippo.cm.model.ConfigurationModel}.
     */
    public List<NamespaceDefinitionImpl> getNamespaceDefinitions() {
        return namespaceDefinitions;
    }

    /**
     * @return a sorted list of config definitions.
     * Note that these definitions are only populated for Modules that are part of the {@link org.onehippo.cm.model.ConfigurationModel}.
     */
    public List<ConfigDefinitionImpl> getConfigDefinitions() {
        return configDefinitions;
    }

    /**
     * @return a list of content definitions.
     * Note that these definitions are only populated for Modules that are part of the {@link org.onehippo.cm.model.ConfigurationModel}.
     */
    public List<ContentDefinitionImpl> getContentDefinitions() {
        return contentDefinitions;
    }

    /**
     * @return a sorted list of web file bundle definitions in insertion order.
     * Note that these definitions are only populated for Modules that are part of the {@link org.onehippo.cm.model.ConfigurationModel}.
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

    /**
     * This property stores the String used to find mvn source files relative to the project.basedir. This is
     * the input expected from repo.bootstrap.modules and autoexport:config's autoexport:modules properties.
     * Used to map parsed Modules back to source. Expect a null value for modules that are not loaded from
     * mvn source files.
     */
    public String getMvnPath() {
        return mvnPath;
    }

    public ModuleImpl setMvnPath(final String mvnPath) {
        this.mvnPath = mvnPath;
        return this;
    }

    /**
     * Track a resource that should be removed when this module is serialized.
     * @param resourcePath a resource path relative to the config root (should start with '/')
     */
    public void addConfigResourceToRemove(final String resourcePath) {
        removedConfigResources.add(resourcePath);
    }

    /**
     * Track a resource that should be removed when this module is serialized.
     * @param resourcePath a resource path relative to the content root (should start with '/')
     */
    public void addContentResourceToRemove(final String resourcePath) {
        removedContentResources.add(resourcePath);
    }

    /**
     * @return a set of config resource paths that should be removed when serializing this module
     */
    public Set<String> getRemovedConfigResources() {
        return removedConfigResources;
    }

    /**
     * @return a set of content resource paths that should be removed when serializing this module
     */
    public Set<String> getRemovedContentResources() {
        return removedContentResources;
    }

    public ModuleImpl build() {
        // TODO: add safety check to prevent stale calls to getXxxDefinitions() after addSource() but before build()

        // clear and sort definitions into the different types
        namespaceDefinitions.clear();
        configDefinitions.clear();
        contentDefinitions.clear();
        webFileBundleDefinitions.clear();

        for (SourceImpl source : getSources()) {
            for (AbstractDefinitionImpl definition : source.getDefinitions()) {
                switch (definition.getType()) {
                    case NAMESPACE:
                        ensureSingleSourceForNamespaces(definition);
                        namespaceDefinitions.add((NamespaceDefinitionImpl) definition);
                        break;
                    case CONFIG:
                        configDefinitions.add((ConfigDefinitionImpl) definition);
                        break;
                    case CONTENT:
                        contentDefinitions.add((ContentDefinitionImpl) definition);
                        break;
                    case WEBFILEBUNDLE:
                        webFileBundleDefinitions.add((WebFileBundleDefinitionImpl) definition);
                        break;
                }
            }
        }

        // sort the content/config definitions, all other remain in insertion order
        configDefinitions.sort(new UniquenessCheckingTreeDefinitionComparator());
        contentDefinitions.sort(new UniquenessCheckingTreeDefinitionComparator());

        return this;
    }

    /**
     * @return true iff no definitions are defined here -- client must call build() before this method
     */
    public boolean isEmpty() {
        return namespaceDefinitions.isEmpty()
            && configDefinitions.isEmpty()
            && contentDefinitions.isEmpty()
            && webFileBundleDefinitions.isEmpty();
    }

    private void ensureSingleSourceForNamespaces(final AbstractDefinitionImpl namespaceDefinition) {
        if (!namespaceDefinitions.isEmpty()
                && !namespaceDefinition.getSource().getPath().equals(namespaceDefinitions.get(0).getSource().getPath())) {
            final String msg = String.format("Namespaces are specified in multiple sources of a module: '%s' and '%s'. "
                    + "To ensure proper ordering, they must be specified in a single source.",
                    namespaceDefinition.getOrigin(), namespaceDefinitions.get(0).getOrigin());
            throw new IllegalStateException(msg);
        }
    }

    protected void compileManifest(ConfigurationModelImpl model, TreeMap<ModuleImpl,TreeMap<String,String>> manifest) {
        TreeMap<String,String> items = new TreeMap<>();

        // get the resource input provider, which provides access to raw data for module content
        ResourceInputProvider rip = configResourceInputProvider;

        // digest the descriptor
        // TODO this is an ugly hack in part because RIP uses config root instead of module root
        boolean hasDescriptor = digestResource(null, "/../" + HCM_MODULE_YAML, null, rip, items);

        // special-case handle a missing descriptor by generating a dummy one, for demo case
        // TODO remove when demo is restructured to use module-specific descriptors
        if (!hasDescriptor) {
            // create a manifest item for a dummy descriptor
            String descriptor = this.compileDummyDescriptor();
            String digest = DigestUtils.digestFromStream(IOUtils.toInputStream(descriptor, StandardCharsets.UTF_8));
            items.put("/" + HCM_MODULE_YAML, digest);
        }

        // digest the actions file
        digestResource(null, "/../" + ACTIONS_YAML, null, rip, items);

        // for each content source
        for (ContentSourceImpl source : this.getContentSources()) {
            // add the definition path to manifest
            items.put("/" + HCM_CONTENT_FOLDER + "/" + source.getPath(), source.getContentDefinition().getNode().getJcrPath().toString());
        }

        // for each config source
        for (ConfigSourceImpl source : this.getConfigSources()) {
            // digest the source
            digestResource(source, "/" + source.getPath(), null, rip, items);

            // for each definition
            for (AbstractDefinitionImpl def : source.getDefinitions()) {
                switch (def.getType()) {
                    case NAMESPACE:
                        NamespaceDefinition namespaceDefinition = (NamespaceDefinition)def;
                        if (namespaceDefinition.getCndPath() != null) {
                            digestResource(source, namespaceDefinition.getCndPath().getString(), null, rip, items);
                        }
                        break;
                    case WEBFILEBUNDLE:
                        // no special processing required
                    case CONTENT:
                        // this shouldn't exist here anymore, but we'll let the verifier handle it
                        break;
                    case CONFIG:
                        // recursively find all config resources and digest them
                        ConfigDefinitionImpl configDef = (ConfigDefinitionImpl) def;
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
    protected void digestResourcesForNode(DefinitionNodeImpl defNode, SourceImpl source, ResourceInputProvider rip,
                                          TreeMap<String,String> items) {

        final Consumer<ValueImpl> digester = v -> {
            if (v.isResource()) {
                try {
                    digestResource(source, v.getString(), v.getResourceInputStream(), rip, items);
                }
                catch (IOException e) {
                    throw new RuntimeException("Exception while computing resource digest", e);
                }
            }
        };

        // find resource values
        defNode.getProperties().forEach(dp -> {
            // if value is a resource, digest it
            switch (dp.getKind()) {
                case SINGLE:
                    digester.accept(dp.getValue());
                    break;
                case SET: case LIST:
                    dp.getValues().forEach(digester);
                    break;
            }
        });

        // recursively visit child definition nodes
        defNode.getNodes().forEach(dn -> digestResourcesForNode(dn, source, rip, items));
    }

    /**
     * Compute and accumulate a crypto digest for the resource referenced by source at path.
     * @param source the Source from which path might be a relative reference
     * @param path iff starts with '/', a module-relative path, else a path relative to source
     * @param valueIs if we're digesting a value, pass in the IS from the value.getResourceInputStream() method
     *                instead of looking up a new IS via the RIP
     * @param rip provides access to raw data streams
     * @param items accumulator of [path,digest] Strings
     */
    protected boolean digestResource(final SourceImpl source, String path, final InputStream valueIs,
                                     final ResourceInputProvider rip, final TreeMap<String,String> items) {
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

        try (final InputStream is = useOrCreateInputStream(valueIs, path, rip)) {
            if (is == null) {
                return false;
            }

            String digestString = DigestUtils.digestFromStream(is);

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
     * Helper for digestResource().
     * @throws IOException
     */
    protected InputStream useOrCreateInputStream(final InputStream valueIs, final String path,
                                                 final ResourceInputProvider rip) throws IOException {
        if (valueIs == null) {
            // if the RIP cannot provide an InputStream, short-circuit here
            if (!rip.hasResource(null, path)) {
                return null;
            }
            return rip.getResourceInputStream(null, path);
        }
        else {
            return valueIs;
        }
    }

    /**
     * Compile a dummy YAML descriptor file to stand in for special case where demo project uses an aggregated
     * descriptor for a set of modules.
     * @return a YAML string representing the group->project->module hierarchy and known dependencies for this Module
     */
    public String compileDummyDescriptor() {
        // serialize a dummy module descriptor for this module

        // create a dummy group->project->module setup with just the data relevant to this Module
        GroupImpl group = new GroupImpl(getProject().getGroup().getName());
        group.addAfter(getProject().getGroup().getAfter());
        ProjectImpl project = group.addProject(getProject().getName());
        project.addAfter(getProject().getAfter());
        ModuleImpl dummyModule = project.addModule(getName());
        dummyModule.addAfter(getAfter());

        log.debug("Creating dummy module descriptor for {}/{}/{}",
                group.getName(), project.getName(), getName());

        // serialize that dummy group
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // todo switch to single-module alternate serializer
            final ModuleDescriptorSerializer descriptorSerializer = new ModuleDescriptorSerializer();
            descriptorSerializer.serialize(baos, dummyModule);
            return baos.toString(StandardCharsets.UTF_8.name());
        }
        catch (IOException e) {
            throw new RuntimeException("Problem compiling dummy descriptor", e);
        }
    }

    // TODO why is this defined here and not as natural order of ContentDefinitionImpl?
    private class UniquenessCheckingTreeDefinitionComparator implements Comparator<TreeDefinitionImpl> {
        public int compare(final TreeDefinitionImpl def1, final TreeDefinitionImpl def2) {
            final JcrPath rootPath1 = def1.getNode().getJcrPath();
            final JcrPath rootPath2 = def2.getNode().getJcrPath();

            if (def1 != def2 && rootPath1.equals(rootPath2) && !rootPath1.startsWith("/hippo:configuration/hippo:translations")) {
                final String msg = String.format(
                        "Duplicate definition root paths '%s' in module '%s' in source files '%s' and '%s'.",
                        rootPath1, getName(), def1.getOrigin(), def2.getOrigin());
                throw new IllegalStateException(msg);
            }
            return rootPath1.compareTo(rootPath2);
        }
    }

    @Override
    public String toString() {
        return "ModuleImpl{" +
                ((extension==null)? "": ("extension='" + extension +"', ")) +
                ((mvnPath==null)? "": ("mvnPath='" + mvnPath +"', ")) +
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

    @Override
    public ModuleImpl clone() {
        // deep clone
        try {
            GroupImpl newGroup = new GroupImpl(project.getGroup().getName());
            newGroup.addAfter(project.getGroup().getAfter());

            ProjectImpl newProject = newGroup.addProject(project.getName());
            newProject.addAfter(project.getAfter());

            ModuleImpl newModule = newProject.addModule(name);
            newModule.addAfter(after);
            newModule.setMvnPath(mvnPath);
            newModule.setConfigResourceInputProvider(configResourceInputProvider);
            newModule.setContentResourceInputProvider(contentResourceInputProvider);
            newModule.setExtension(extension);
            // probably not needed as archive module aren't supposed to (need to) be cloned
            newModule.setArchiveFile(archiveFile);

            newModule.lastExecutedAction = lastExecutedAction;
            newModule.getActionsMap().putAll(actionsMap);

            // reload sources from raw YAML instead of attempting to copy the full parsed structure
            // TODO is this good enough? for auto-export, there is a potential failure mode if files are changed on disk
            final SourceParser configSourceParser = new ConfigSourceParser(configResourceInputProvider);
            final SourceParser contentSourceParser = new ContentSourceHeadParser(contentResourceInputProvider);

            for (ConfigSourceImpl source : getConfigSources()) {
                // TODO adding the slash here is a silly hack to load a source path without needing the source first
                try(InputStream resourceInputStream = configResourceInputProvider.getResourceInputStream(null, "/" + source.getPath())) {
                    configSourceParser.parse(resourceInputStream,
                            source.getPath(), getFullSourcePath(source, configResourceInputProvider), newModule);
                }
            }
            for (ContentSourceImpl source : getContentSources()) {
                try(InputStream resourceInputStream = contentResourceInputProvider.getResourceInputStream(null, "/" + source.getPath())) {
                    contentSourceParser.parse(resourceInputStream,
                            source.getPath(), getFullSourcePath(source, contentResourceInputProvider), newModule);
                }
            }

            return newModule.build();
        } catch (ParserException | IOException e) {
            throw new RuntimeException("Unable to clone Module: "+getFullName(), e);
        }
    }

    private String getFullSourcePath(final SourceImpl source, final ResourceInputProvider rip) {
        if (rip instanceof FileResourceInputProvider) {
            return ((FileResourceInputProvider)rip).getFullSourcePath(source);
        }
        else {
            return source.getPath();
        }
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
        return String.join("/",
                getProject().getGroup().getName(),
                getProject().getName(),
                getName());
    }

    /**
     * Compare on the basis of lexical order of the full group/project/module names.
     * @param o another Module
     */
    @Override
    public int compareTo(final Module o) {
        return getFullName().compareTo(o.getFullName());
    }
}
