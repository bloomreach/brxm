/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.autoexport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.serializer.ModuleContext;
import org.onehippo.cm.model.source.ResourceInputProvider;

public class AutoExportModuleContext extends ModuleContext {

    private ResourceInputProvider internalResourceInputProvider;

    public AutoExportModuleContext(final ModuleImpl module, final Path moduleDescriptorPath,
                                   final ResourceInputProvider internalResourceInputProvider) {
        super(module, moduleDescriptorPath);
        this.internalResourceInputProvider = internalResourceInputProvider;
    }

    protected InputStream getResourceInputStream(final ValueImpl value) throws IOException {
        final String internalResourcePath = value.getInternalResourcePath();
        if (internalResourcePath != null) {
            return internalResourceInputProvider.getResourceInputStream(null, internalResourcePath);
        }
        else {
            return super.getResourceInputStream(value);
        }
    }
}
