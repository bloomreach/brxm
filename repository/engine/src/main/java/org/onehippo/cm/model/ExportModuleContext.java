/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.serializer.FileResourceOutputProvider;
import org.onehippo.cm.model.serializer.ModuleContext;

/**
 * Module Context export implementation. Omits generating hcm-content folder.
 */
public class ExportModuleContext extends ModuleContext {

    public ExportModuleContext(ModuleImpl module, Path moduleDescriptorPath) throws IOException {
        super(module, moduleDescriptorPath);
        createOutputProviders(moduleDescriptorPath);
    }

    @Override
    public void createOutputProviders(Path moduleDescriptorPath) {
        configOutputProvider = new FileResourceOutputProvider(moduleDescriptorPath, "");
        contentOutputProvider = new FileResourceOutputProvider(moduleDescriptorPath, "");
    }
}