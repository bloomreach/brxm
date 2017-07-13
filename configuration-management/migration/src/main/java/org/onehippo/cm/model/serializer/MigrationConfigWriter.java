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
package org.onehippo.cm.model.serializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.migration.ResourceProcessor;
import org.onehippo.cm.model.MigrationMode;
import org.onehippo.cm.model.impl.source.FileResourceInputProvider;
import org.onehippo.cm.model.source.Source;
import org.onehippo.cm.model.source.SourceType;

import static org.onehippo.cm.migration.ResourceProcessor.deleteEmptyDirectory;
import static org.onehippo.cm.model.Constants.HCM_CONFIG_FOLDER;
import static org.onehippo.cm.model.Constants.HCM_CONTENT_FOLDER;
import static org.onehippo.cm.model.util.FilePathUtils.nativePath;

public class MigrationConfigWriter extends ModuleWriter {

    private MigrationMode mode;

    public MigrationConfigWriter(MigrationMode mode) {
        this.mode = mode;
    }

    /**
     * Copy existing item. Depending on a migration mode:
     * <pre/>
     * GIT: performs git mv -f source target and thus, preserve version control history
     * <pre/>
     * MOVE: DELETES source file and folder if it is empty
     * <pre/>
     * COPY: Does nothing
     */
    void processCopyItem(Source source, CopyItem copyItem, ModuleContext moduleContext) throws IOException {

        final String hcmFolder = source.getType() == SourceType.CONFIG ? HCM_CONFIG_FOLDER : HCM_CONTENT_FOLDER;
        final Path sourceBasePath = ((FileResourceInputProvider)moduleContext.getInputProvider(source)).getBasePath();
        final Path destBasePath = ((FileResourceOutputProvider) moduleContext.getOutputProvider(source)).getBasePath().resolve(hcmFolder);

        String itemRelativePath = StringUtils.stripStart(copyItem.getValue().getString(), "/");

        final Path oldItemPath = sourceBasePath.resolve(nativePath(itemRelativePath));
        final Path newItemPath = destBasePath.resolve(nativePath(itemRelativePath));

        switch (mode) {
            case GIT:
                new ResourceProcessor().moveGitResource(oldItemPath, newItemPath);
                deleteEmptyDirectory(sourceBasePath.getParent());
                break;
            case COPY:
                super.processCopyItem(source, copyItem, moduleContext);
                break;
            case MOVE:
                super.processCopyItem(source, copyItem, moduleContext);
                Files.deleteIfExists(oldItemPath);
                deleteEmptyDirectory(sourceBasePath.getParent());
                break;
            default:
                throw new UnsupportedOperationException(String.format("Unknown operation: %s", mode));
        }
    }


}
