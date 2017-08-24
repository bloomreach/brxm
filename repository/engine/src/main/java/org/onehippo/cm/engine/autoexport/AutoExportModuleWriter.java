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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import org.onehippo.cm.model.Module;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.serializer.ModuleContext;
import org.onehippo.cm.model.serializer.ModuleWriter;
import org.onehippo.cm.model.serializer.ResourceOutputProvider;
import org.onehippo.cm.model.source.ResourceInputProvider;
import org.onehippo.cm.model.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoExportModuleWriter extends ModuleWriter {

    private static final Logger log = LoggerFactory.getLogger(AutoExportModuleWriter.class);

    private ResourceInputProvider inputProvider;

    public AutoExportModuleWriter(final ResourceInputProvider inputProvider) {
        this.inputProvider = inputProvider;
    }

    public void writeModule(final Module module, final boolean explicitSequencing, final ModuleContext moduleContext) throws IOException {
        // remove deleted resources before processing new resource names
        removeDeletedResources(moduleContext);
        super.writeModule(module, explicitSequencing, moduleContext);

    }

    protected void removeDeletedResources(final ModuleContext moduleContext) throws IOException {
        final ModuleImpl module = moduleContext.getModule();
        removeResources(module.getRemovedConfigResources(), moduleContext.getConfigOutputProvider(), moduleContext.getConfigRoot());
        removeResources(module.getRemovedContentResources(), moduleContext.getContentOutputProvider(), moduleContext.getContentRoot());
    }

    /**
     * Remove file resources and parent folders in case if they're empty
     */
    protected void removeResources(final Set<String> removedResources, final ResourceOutputProvider resourceOutputProvider, final Path rootFolder)
            throws IOException {
        for (final String removed : removedResources) {
            final Path removedPath = resourceOutputProvider.getResourcePath(null, removed);
            final boolean wasDeleted = Files.deleteIfExists(removedPath);
            log.debug("File to be deleted: {}, was deleted: {}", removedPath, wasDeleted);
            removeEmptyFolders(removedPath.getParent(), rootFolder);
        }
    }

    private void removeEmptyFolders(Path folder, Path rootFolder) throws IOException {
            if (!folder.equals(rootFolder) && isDirectoryEmpty(folder)) {
            Files.delete(folder);
            removeEmptyFolders(folder.getParent(), rootFolder);
        }
    }

    private boolean isDirectoryEmpty(final Path folder) throws IOException {
        try(Stream<Path> list = Files.list(folder)) {
            return !list.findAny().isPresent();
        }
    }

    protected boolean sourceShouldBeSkipped(final Source source) {
        // short-circuit processing of unchanged sources
        return !source.hasChangedSinceLoad();
    }

    protected InputStream getValueInputProvider(final ValueImpl value) throws IOException {
        final String internalResourcePath = value.getInternalResourcePath();
        if (internalResourcePath != null) {
            return inputProvider.getResourceInputStream(null, internalResourcePath);
        }
        else {
            return super.getValueInputProvider(value);
        }
    }
}
