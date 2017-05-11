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
package org.onehippo.cm.migration;

import java.io.IOException;
import java.nio.file.Path;

import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.engine.FileConfigurationUtils;
import org.onehippo.cm.engine.FileResourceInputProvider;
import org.onehippo.cm.engine.FileResourceOutputProvider;
import org.onehippo.cm.engine.ModuleContext;

/**
 * Support for esv2yaml tool. Input provider relies onto the root of module instead of module/hcm-config folder
 */
public class LegacyModuleContext extends ModuleContext {

    LegacyModuleContext(Module module, Path moduleDescriptorPath, boolean multiModule) throws IOException {

        super(module, moduleDescriptorPath, multiModule);
        this.configInputProvider = this.contentInputProvider = new FileResourceInputProvider(moduleDescriptorPath);
    }

    @Override
    public void createOutputProviders(Path destinationPath) {
        Path configModuleBasePath = FileConfigurationUtils.getModuleBasePath(destinationPath, module, multiModule);
        configOutputProvider = new FileResourceOutputProvider(configModuleBasePath);

        Path contentModuleBasePath = FileConfigurationUtils.getModuleContentBasePath(destinationPath, module, multiModule);
        contentOutputProvider = new FileResourceOutputProvider(contentModuleBasePath);
    }
}
