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
package org.onehippo.cm.engine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.engine.parser.SourceResourceCrawler;
import org.onehippo.cm.engine.serializer.ResourceNameResolver;
import org.onehippo.cm.engine.serializer.ResourceNameResolverImpl;
import org.onehippo.cm.impl.model.ConfigSourceImpl;

/**
 * Incapsulates module's input/output providers and unique name resolver
 */
//todo SS: extract interface to API module
public class ModuleContext {

    protected ResourceInputProvider configInputProvider;
    protected ResourceInputProvider contentInputProvider;
    protected ResourceOutputProvider configOutputProvider;
    protected ResourceOutputProvider contentOutputProvider;

    protected final Module module;
    protected final boolean multiModule;
    private final Path moduleDescriptorPath;

    private Path moduleActionsDescriptorPath;
    private Path moduleConfigRootPath;
    private Path moduleContentRootPath;

    private ResourceNameResolver configNameResolver = new ResourceNameResolverImpl();
    private ResourceNameResolver contentNameResolver = new ResourceNameResolverImpl();


    public ModuleContext(Module module, Path moduleDescriptorPath, boolean multiModule) throws IOException {
        this.module = module;
        this.moduleDescriptorPath = moduleDescriptorPath;
        this.multiModule = multiModule;
    }

    /**
     * @return {@link Path} to hcm-actions.yaml of current module
     */
    public Path getActionsDecriptorPath() {
        if (moduleActionsDescriptorPath == null) {
            moduleActionsDescriptorPath = moduleDescriptorPath.resolveSibling(Constants.ACTIONS_YAML);
        }
        return moduleActionsDescriptorPath;
    }

    /**
     * @return {@link Path} to hcm-config folder of current module
     */
    public Path getConfigRoot() {
        if (moduleConfigRootPath == null) {
            moduleConfigRootPath = FileConfigurationUtils.getModuleBasePath(moduleDescriptorPath, module, multiModule);
        }
        return moduleConfigRootPath;
    }

    /**
     * @return {@link Path} to hcm-content folder of current module
     */
    public Path getContentRoot() {
        if (moduleContentRootPath == null) {
            moduleContentRootPath = FileConfigurationUtils.getModuleContentBasePath(moduleDescriptorPath, module, multiModule);
        }
        return moduleContentRootPath;
    }

    public ResourceInputProvider getConfigInputProvider() {
        if (configInputProvider == null) {
            configInputProvider = new FileResourceInputProvider(getConfigRoot());
        }
        return configInputProvider;
    }

    public ResourceInputProvider getContentInputProvider() {
        if (contentInputProvider == null) {
            contentInputProvider = new FileResourceInputProvider(getContentRoot());
        }
        return contentInputProvider;
    }

    public ResourceInputProvider getInputProvider(Source source) {
        return source instanceof ConfigSourceImpl ? getConfigInputProvider() : getContentInputProvider();
    }

    public ResourceOutputProvider getOutputProvider(Source source) {
        return source instanceof ConfigSourceImpl ? configOutputProvider : contentOutputProvider;
    }

    public void createOutputProviders(Path destinationPath) {
        Path configModuleBasePath = FileConfigurationUtils.getModuleBasePath(destinationPath, module, multiModule);
        configOutputProvider = new FileResourceOutputProvider(configModuleBasePath);

        Path contentModuleBasePath = FileConfigurationUtils.getModuleContentBasePath(destinationPath, module, multiModule);
        contentOutputProvider = new FileResourceOutputProvider(contentModuleBasePath);
    }

    public String generateUniqueName(Source source, String filePath) {
        return source instanceof ConfigSourceImpl ? configNameResolver.generateName(filePath) : contentNameResolver.generateName(filePath);
    }

    /**
     * Adds predefined resource files to known files list. Should be invoked only after OutputProvider had been created
     * @throws IOException
     */
    public void addExistingFilesToKnownList() throws IOException {

        if (configOutputProvider == null || contentOutputProvider == null) {
            throw new IOException(String.format("Output provider should be initialized for module: %s", module));
        }

        final SourceResourceCrawler crawler = new SourceResourceCrawler();
        for (final Source source : module.getSources()) {
            final Set<String> resources = crawler.collect(source);
            for (final String resource : resources) {
                final Path resourcePath = getOutputProvider(source).getResourceOutputPath(source, resource);
                generateUniqueName(source, resourcePath.toString());
            }
        }
    }
}
