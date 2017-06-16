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
package org.onehippo.cm.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.onehippo.cm.ResourceInputProvider;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.SourceImpl;
import org.onehippo.cm.model.impl.ValueImpl;
import org.onehippo.cm.model.mapper.ValueFileMapperProvider;
import org.onehippo.cm.model.parser.SourceResourceCrawler;
import org.onehippo.cm.model.serializer.ResourceNameResolver;
import org.onehippo.cm.model.serializer.ResourceNameResolverImpl;
import org.onehippo.cm.model.util.FileConfigurationUtils;

/**
 * Incapsulates module's input/output providers and unique name resolver
 */
public class ModuleContext {

    protected ResourceInputProvider configInputProvider;
    protected ResourceInputProvider contentInputProvider;
    protected ResourceOutputProvider configOutputProvider;
    protected ResourceOutputProvider contentOutputProvider;

    protected final ModuleImpl module;
    protected final boolean multiModule;
    private final Path moduleDescriptorPath;

    private Path actionsDescriptorPath;
    private Path configRootPath;
    private Path contentRootPath;

    private ResourceNameResolver configNameResolver = new ResourceNameResolverImpl();
    private ResourceNameResolver contentNameResolver = new ResourceNameResolverImpl();

    public ModuleContext(ModuleImpl module, Path moduleRootPath) {
        this.module = module;
        this.multiModule = false;
        this.moduleDescriptorPath = moduleRootPath.resolve(Constants.HCM_MODULE_YAML);
    }

    public ModuleContext(ModuleImpl module, Path moduleDescriptorPath, boolean multiModule) throws IOException {
        this.module = module;
        this.moduleDescriptorPath = moduleDescriptorPath;
        this.multiModule = multiModule;
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
            configRootPath = FileConfigurationUtils.getModuleBasePath(moduleDescriptorPath, module, multiModule);
        }
        return configRootPath;
    }

    /**
     * @return {@link Path} to hcm-content folder of current module
     */
    public Path getContentRoot() {
        if (contentRootPath == null) {
            contentRootPath = FileConfigurationUtils.getModuleContentBasePath(moduleDescriptorPath, module, multiModule);
        }
        return contentRootPath;
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
        return SourceType.CONFIG == source.getType() ? getConfigInputProvider() : getContentInputProvider();
    }

    public ResourceOutputProvider getOutputProvider(Source source) {
        return SourceType.CONFIG == source.getType() ? configOutputProvider : contentOutputProvider;
    }

    /**
     * @param destinationPath the path to the module descriptor file
     */
    public void createOutputProviders(Path destinationPath) {
        Path configModuleBasePath = FileConfigurationUtils.getModuleBasePath(destinationPath, module, multiModule);
        configOutputProvider = new FileResourceOutputProvider(configModuleBasePath);

        Path contentModuleBasePath = FileConfigurationUtils.getModuleContentBasePath(destinationPath, module, multiModule);
        contentOutputProvider = new FileResourceOutputProvider(contentModuleBasePath);
    }

    public String generateUniqueName(Source source, String filePath) {
        return SourceType.CONFIG == source.getType() ? configNameResolver.generateName(filePath) : contentNameResolver.generateName(filePath);
    }

    /**
     * Adds predefined resource files to known files list. Should be invoked only after OutputProvider had been created
     *
     * @throws IOException
     */
    public void collectExistingFilesAndResolveNewResources() throws IOException {

        if (configOutputProvider == null || contentOutputProvider == null) {
            throw new IOException(String.format("Output provider should be initialized for module: %s", module));
        }

        final ValueFileMapperProvider mapperProvider = ValueFileMapperProvider.getInstance();

        final SourceResourceCrawler crawler = new SourceResourceCrawler();
        final LinkedHashMap<SourceImpl, List<Pair<ValueImpl, String>>> allNewResources = new LinkedHashMap<>();
        for (final SourceImpl source : module.getSources()) {
            final List<Pair<ValueImpl, String>> resources = crawler.collect(source);
            for (final Pair<ValueImpl, String> pair : resources) {
                if (pair.getLeft().isNewResource()) {
                    // postpone mapping new resources until existing files are all known (to prevent clashes)
                    final List<Pair<ValueImpl, String>> newResources = allNewResources.computeIfAbsent(source, k -> new ArrayList<>());
                    newResources.add(pair);
                } else {
                    final Path resourcePath = getOutputProvider(source).getResourceOutputPath(source, pair.getRight());
                    generateUniqueName(source, resourcePath.toString());
                }
            }
        }
        // now generate file mappings for new resources and set their resource file path to the actual value
        for (final Map.Entry<SourceImpl, List<Pair<ValueImpl, String>>> sourceEntry : allNewResources.entrySet()) {
            final SourceImpl source = sourceEntry.getKey();
            for (final Pair<ValueImpl, String> pair : sourceEntry.getValue()) {
                // TODO: tricky cast or always valid? In the latter case the APIs should reflect that

                final String resourceFileName = mapperProvider.generateName(pair.getLeft());
                final FileResourceOutputProvider outputProvider = (FileResourceOutputProvider) getOutputProvider(source);
                final Path resourceFilePath = outputProvider.getResourceOutputPath(source, resourceFileName);
                final String filePath = generateUniqueName(source, resourceFilePath.toString());
                final String resourcePath = filePath.substring(outputProvider.getModulePath().toString().length());
                pair.getLeft().setResourceValue(resourcePath);
            }
        }
    }
}
