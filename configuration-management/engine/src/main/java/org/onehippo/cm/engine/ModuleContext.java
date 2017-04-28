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

import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.Module;

import java.nio.file.Path;

public class ModuleContext {

    protected ResourceInputProvider configInputProvider;
    protected ResourceInputProvider contentInputProvider;

    protected ResourceOutputProvider configOutputProvider;
    protected ResourceOutputProvider contentOutputProvider;

    protected final Module module;
    protected final Path repoConfigPath;

    protected final boolean multiModule;

    protected Path moduleConfigRootPath;
    protected Path moduleContentRootPath;

    public ModuleContext(Module module, Path repoConfigPath, boolean multiModule) {
        this.module = module;
        this.repoConfigPath = repoConfigPath;
        this.multiModule = multiModule;
    }

    public Path getConfigRoot() {
        if (moduleConfigRootPath == null) {
            moduleConfigRootPath = FileConfigurationUtils.getModuleBasePath(repoConfigPath, module, multiModule);
        }
        return moduleConfigRootPath;
    }

    public Path getContentRoot() {
        if (moduleContentRootPath == null) {
            moduleContentRootPath = FileConfigurationUtils.getModuleContentBasePath(repoConfigPath, module, multiModule);
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

    public ResourceOutputProvider createConfigOutputProvider(Path destinationPath) {
        Path moduleBasePath = FileConfigurationUtils.getModuleBasePath(destinationPath, module, multiModule);
        configOutputProvider = new FileResourceOutputProvider(moduleBasePath);
        return configOutputProvider;
    }

    public ResourceOutputProvider createContentOutputProvider(Path destinationPath) {
        Path moduleBasePath = FileConfigurationUtils.getModuleContentBasePath(destinationPath, module, multiModule);
        contentOutputProvider = new FileResourceOutputProvider(moduleBasePath);
        return contentOutputProvider;
    }

    public void createOutputProviders(Path destinationPath) {
        Path configModuleBasePath = FileConfigurationUtils.getModuleBasePath(destinationPath, module, multiModule);
        configOutputProvider = new FileResourceOutputProvider(configModuleBasePath);

        Path contentModuleBasePath = FileConfigurationUtils.getModuleContentBasePath(destinationPath, module, multiModule);
        contentOutputProvider = new FileResourceOutputProvider(contentModuleBasePath);
    }


    public ResourceOutputProvider getConfigOutputProvider() {
        return configOutputProvider;
    }

    public ResourceOutputProvider getContentOutputProvider() {
        return contentOutputProvider;
    }

    public void createInputProviders(Path path) {
        configInputProvider = new FileResourceInputProvider(path);
        contentInputProvider = new FileResourceInputProvider(path);
    }
}
