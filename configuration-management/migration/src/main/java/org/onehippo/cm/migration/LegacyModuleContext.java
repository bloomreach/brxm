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
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.model.Constants;
import org.onehippo.cm.model.MigrationMode;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.source.FileResourceInputProvider;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.serializer.FileResourceOutputProvider;
import org.onehippo.cm.model.serializer.ModuleContext;
import org.onehippo.cm.model.source.SourceType;

import static org.onehippo.cm.migration.ResourceProcessor.deleteEmptyDirectory;
import static org.onehippo.cm.model.Constants.HCM_CONFIG_FOLDER;
import static org.onehippo.cm.model.Constants.HCM_CONTENT_FOLDER;
import static org.onehippo.cm.model.util.FilePathUtils.nativePath;

/**
 * Support for esv2yaml tool. Input provider relies onto the root of module instead of module/hcm-config folder
 */
public class LegacyModuleContext extends ModuleContext {

    private MigrationMode mode;

    LegacyModuleContext(final ModuleImpl module, final Path moduleDescriptorPath, final MigrationMode mode) throws IOException {
        super(module, moduleDescriptorPath);
        this.mode = mode;
        this.configInputProvider = this.contentInputProvider = new FileResourceInputProvider(moduleDescriptorPath, "");
    }

    @Override
    public void createOutputProviders(Path destinationPath) {
        configOutputProvider = new FileResourceOutputProvider(destinationPath, Constants.HCM_CONFIG_FOLDER);
        contentOutputProvider = new FileResourceOutputProvider(destinationPath, Constants.HCM_CONTENT_FOLDER);
    }

    /**
     * Serialize resource item. Depending on a migration mode:
     * <pre/>
     * GIT: performs git mv -f source target and thus, preserve version control history
     * <pre/>
     * MOVE: DELETES source file and folder if it is empty
     * <pre/>
     * COPY: Does nothing
     */
    public void serializeResourceValue(final SourceImpl source, final ValueImpl resourceValue) {
        final String hcmFolder = source.getType() == SourceType.CONFIG ? HCM_CONFIG_FOLDER : HCM_CONTENT_FOLDER;
        final Path sourceBasePath = ((FileResourceInputProvider)getInputProvider(source)).getBasePath();
        final Path destBasePath = ((FileResourceOutputProvider)getOutputProvider(source)).getBasePath().resolve(hcmFolder);

        String itemRelativePath = StringUtils.stripStart(resourceValue.getString(), "/");

        final Path oldItemPath = sourceBasePath.resolve(nativePath(itemRelativePath));
        final Path newItemPath = destBasePath.resolve(nativePath(itemRelativePath));

        try {
            switch (mode) {
                case GIT:
                    new ResourceProcessor().moveGitResource(oldItemPath, newItemPath);
                    deleteEmptyDirectory(sourceBasePath.getParent());
                    break;
                case COPY:
                    super.serializeResourceValue(source, resourceValue);
                    break;
                case MOVE:
                    super.serializeResourceValue(source, resourceValue);
                    Files.deleteIfExists(oldItemPath);
                    deleteEmptyDirectory(sourceBasePath.getParent());
                    break;
                default:
                    throw new UnsupportedOperationException(String.format("Unknown operation: %s", mode));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
