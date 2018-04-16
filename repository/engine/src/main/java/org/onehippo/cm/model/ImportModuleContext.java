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
package org.onehippo.cm.model;

import java.nio.file.Path;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.source.FileResourceInputProvider;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.serializer.ModuleContext;
import org.onehippo.cm.model.source.ResourceInputProvider;

import static org.onehippo.cm.model.util.FilePathUtils.getParentOrFsRoot;

public class ImportModuleContext extends ModuleContext {

    private Path moduleRoot;
    public ImportModuleContext(final ModuleImpl module, final Path moduleRootPath) {
        super(module, moduleRootPath);
        moduleRoot = moduleRootPath;
    }

    @Override
    public Path getContentRoot() {
        return moduleRoot;
    }

    @Override
    public ResourceInputProvider getContentInputProvider() {
        if (contentInputProvider == null) {
            contentInputProvider = new FileResourceInputProvider(getParentOrFsRoot(moduleRoot), StringUtils.EMPTY);
        }
        return contentInputProvider;
    }

    @Override
    public ResourceInputProvider getInputProvider(final SourceImpl source) {
        return super.getInputProvider(source);
    }
}