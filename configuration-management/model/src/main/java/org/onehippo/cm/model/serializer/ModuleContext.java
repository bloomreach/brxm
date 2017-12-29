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
package org.onehippo.cm.model.serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.onehippo.cm.model.Constants;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.definition.AbstractDefinitionImpl;
import org.onehippo.cm.model.impl.definition.NamespaceDefinitionImpl;
import org.onehippo.cm.model.impl.definition.TreeDefinitionImpl;
import org.onehippo.cm.model.impl.source.FileResourceInputProvider;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.tree.DefinitionPropertyImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.mapper.ValueFileMapperProvider;
import org.onehippo.cm.model.source.ResourceInputProvider;
import org.onehippo.cm.model.source.SourceType;
import org.onehippo.cm.model.tree.PropertyOperation;
import org.onehippo.cm.model.tree.PropertyType;

import static org.onehippo.cm.model.util.FilePathUtils.getParentOrFsRoot;

/**
 * Incapsulates module's input/output providers and unique name resolver. It also is responsible for serializing
 * resource values and (embedded) binary values.
 * This class should only be used with modules backed by File-based providers.
 */
public class ModuleContext {

    protected ResourceInputProvider configInputProvider;
    protected ResourceInputProvider contentInputProvider;
    protected ResourceOutputProvider configOutputProvider;
    protected ResourceOutputProvider contentOutputProvider;

    protected final ModuleImpl module;
    private final Path moduleDescriptorPath;

    private Path actionsDescriptorPath;
    private Path configRootPath;
    private Path contentRootPath;

    protected final ValueFileMapperProvider mapperProvider = ValueFileMapperProvider.getInstance();
    protected final ResourceNameResolver configNameResolver = new ResourceNameResolverImpl();
    protected final ResourceNameResolver contentNameResolver = new ResourceNameResolverImpl();

    public ModuleContext(ModuleImpl module, Path moduleDescriptorPath) {
        this.module = module;
        this.moduleDescriptorPath = moduleDescriptorPath;
    }

    public ModuleImpl getModule() {
        return module;
    }

    /**
     * @return {@link Path} to hcm-actions.yaml of current module
     */
    public Path getActionsDescriptorPath() {
        if (actionsDescriptorPath == null) {
            actionsDescriptorPath = moduleDescriptorPath.resolveSibling(Constants.ACTIONS_YAML);
        }
        return actionsDescriptorPath;
    }

    /**
     * @return {@link Path} to hcm-config folder of current module
     */
    public Path getConfigRoot() {
        if (configRootPath == null) {
            configRootPath = moduleDescriptorPath.resolveSibling(Constants.HCM_CONFIG_FOLDER);
        }
        return configRootPath;
    }

    protected void setConfigRoot(final Path configRootPath) {
        this.configRootPath = configRootPath;
    }

    /**
     * @return {@link Path} to hcm-content folder of current module
     */
    public Path getContentRoot() {
        if (contentRootPath == null) {
            contentRootPath = moduleDescriptorPath.resolveSibling(Constants.HCM_CONTENT_FOLDER);
        }
        return contentRootPath;
    }

    protected void setContentRoot(final Path contentRootPath) {
        this.contentRootPath = contentRootPath;
    }

    public ResourceInputProvider getConfigInputProvider() {
        if (configInputProvider == null) {
            configInputProvider = new FileResourceInputProvider(getParentOrFsRoot(moduleDescriptorPath), Constants.HCM_CONFIG_FOLDER);
        }
        return configInputProvider;
    }

    public ResourceInputProvider getContentInputProvider() {
        if (contentInputProvider == null) {
            contentInputProvider = new FileResourceInputProvider(getParentOrFsRoot(moduleDescriptorPath), Constants.HCM_CONTENT_FOLDER);
        }
        return contentInputProvider;
    }

    public ResourceInputProvider getInputProvider(SourceImpl source) {
        return SourceType.CONFIG == source.getType() ? getConfigInputProvider() : getContentInputProvider();
    }

    public ResourceOutputProvider getOutputProvider(SourceImpl source) {
        return SourceType.CONFIG == source.getType() ? configOutputProvider : contentOutputProvider;
    }

    public ResourceOutputProvider getConfigOutputProvider() {
        return configOutputProvider;
    }

    public ResourceOutputProvider getContentOutputProvider() {
        return contentOutputProvider;
    }

    /**
     * @param moduleDescriptorPath the path to the module descriptor file
     */
    public void createOutputProviders(Path moduleDescriptorPath) {
        configOutputProvider = new FileResourceOutputProvider(getParentOrFsRoot(moduleDescriptorPath), Constants.HCM_CONFIG_FOLDER);

        contentOutputProvider = new FileResourceOutputProvider(getParentOrFsRoot(moduleDescriptorPath), Constants.HCM_CONTENT_FOLDER);
    }

    protected ResourceNameResolver getResourcePathResolver(final SourceImpl source) {
        return SourceType.CONFIG == source.getType() ? configNameResolver : contentNameResolver;
    }

    protected String generateUniqueName(final SourceImpl source, final String filePath) {
        return getResourcePathResolver(source).generateName(filePath);
    }

    public String generateUniqueName(final SourceImpl source, final ValueImpl value) {
        return generateUniqueName(source, mapperProvider.generateName(value));
    }

    /**
     * Adds predefined resource file paths to known files list. Should be invoked only after OutputProvider had been created
     *
     * @throws IOException
     */
    public void collectKnownResourcePaths() throws IOException {

        if (configOutputProvider == null || contentOutputProvider == null) {
            throw new IOException(String.format("Output provider should be initialized for module: %s", module));
        }

        configNameResolver.clear();
        contentNameResolver.clear();

        for (final SourceImpl source : module.getSources()) {
            final ResourceNameResolver resourceNameResolver = getResourcePathResolver(source);
            for (AbstractDefinitionImpl definition : source.getDefinitions()) {
                switch (definition.getType()) {
                    case CONFIG:
                    case CONTENT:
                        collectResourcePathsForNode(resourceNameResolver, ((TreeDefinitionImpl)definition).getNode());
                        break;
                    case NAMESPACE:
                        collectResourcePathForNamespace(resourceNameResolver, (NamespaceDefinitionImpl)definition);
                        break;
                }
            }
        }
    }

    protected void collectResourcePathsForNode(final ResourceNameResolver resourceNameResolver, final DefinitionNodeImpl node) {
        for (DefinitionPropertyImpl childProperty : node.getProperties().values()) {
            collectResourcePathsForProperty(resourceNameResolver, childProperty);
        }

        for (DefinitionNodeImpl childNode : node.getNodes().values()) {
            collectResourcePathsForNode(resourceNameResolver, childNode);
        }
    }

    protected void collectResourcePathsForProperty(final ResourceNameResolver resourceNameResolver, final DefinitionPropertyImpl property) {

        if (property.getOperation() == PropertyOperation.DELETE) {
            return;
        }

        if (property.getType() == PropertyType.SINGLE) {
            final ValueImpl value = property.getValue();
            if (value.isResource() && !value.isNewResource()) {
                resourceNameResolver.seedName(value.getString());
            }
        } else {
            for (ValueImpl value : property.getValues()) {
                if (value.isResource() && !value.isNewResource()) {
                    resourceNameResolver.seedName(value.getString());
                }
            }
        }
    }

    protected void collectResourcePathForNamespace(final ResourceNameResolver resourceNameResolver, final NamespaceDefinitionImpl definition) {
        if (definition.getCndPath() != null) {
            resourceNameResolver.seedName(definition.getCndPath().getString());
        }
    }
    public void resolveNewResourceValuePath(final SourceImpl source, final ValueImpl value) {
        final String candidateResourceName = mapperProvider.generateName(value);
        final FileResourceOutputProvider outputProvider = (FileResourceOutputProvider) getOutputProvider(source);
        final String candidateResourceModulePath = outputProvider.getResourceModulePath(source, candidateResourceName);
        final String resourceModulePath = generateUniqueName(source, candidateResourceModulePath);
        final String resourcePath = resourceModulePath.substring(outputProvider.getSourceBasePath().length());
        value.setResourceValue(resourcePath);
    }

    public void serializeBinaryValue(final SourceImpl source, final String finalName, final ValueImpl value) {
        final ResourceOutputProvider outputProvider = getOutputProvider(source);
        final byte[] content = (byte[]) value.getObject();
        try (final InputStream inputStream = new ByteArrayInputStream(content)) {
            try (final OutputStream resourceOutputStream = outputProvider.getResourceOutputStream(source, finalName)) {
                IOUtils.copy(inputStream, resourceOutputStream);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void serializeResourceValue(final SourceImpl source, final ValueImpl resourceValue) {
        final ResourceInputProvider rip =  resourceValue.getResourceInputProvider();
        final ResourceOutputProvider outputProvider = getOutputProvider(source);

        if (rip instanceof FileResourceInputProvider && outputProvider instanceof FileResourceOutputProvider
                // don't try to do a basePath comparison if this is actually backed by the JCR
                && resourceValue.getInternalResourcePath() == null) {
            final FileResourceInputProvider frip = (FileResourceInputProvider) rip;
            final FileResourceOutputProvider fout = (FileResourceOutputProvider) outputProvider;

            if (frip.getBasePath().equals(fout.getBasePath())) {
                // don't copy when src and dest are the same file
                return;
            }
        }

        try (final InputStream resourceInputStream = getResourceInputStream(resourceValue);
             final OutputStream resourceOutputStream =
                     outputProvider.getResourceOutputStream(source, resourceValue.getString())) {
            // TODO: after Java 9, use InputStream.transferTo()
            IOUtils.copy(resourceInputStream, resourceOutputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected InputStream getResourceInputStream(final ValueImpl value) throws IOException {
        return value.getResourceInputStream();
    }
}
