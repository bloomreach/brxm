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

import org.onehippo.cm.model.Constants;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.source.FileResourceInputProvider;
import org.onehippo.cm.model.serializer.FileResourceOutputProvider;
import org.onehippo.cm.model.serializer.ModuleContext;

/**
 * Support for esv2yaml tool. Input provider relies onto the root of module instead of module/hcm-config folder
 */
public class LegacyModuleContext extends ModuleContext {

    LegacyModuleContext(ModuleImpl module, Path moduleDescriptorPath) throws IOException {

        super(module, moduleDescriptorPath);
        this.configInputProvider = this.contentInputProvider = new FileResourceInputProvider(moduleDescriptorPath, "");
    }

    @Override
    public void createOutputProviders(Path destinationPath) {
        configOutputProvider = new FileResourceOutputProvider(destinationPath, Constants.HCM_CONFIG_FOLDER);
        contentOutputProvider = new FileResourceOutputProvider(destinationPath, Constants.HCM_CONTENT_FOLDER);
    }
}
