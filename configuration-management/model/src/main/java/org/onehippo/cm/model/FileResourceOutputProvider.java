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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.onehippo.cm.model.util.FileConfigurationUtils;

public class FileResourceOutputProvider implements ResourceOutputProvider {
    private final Path modulePath;

    public FileResourceOutputProvider(final Path modulePath) {
        this.modulePath = modulePath;
    }

    public Path getModulePath() {
        return modulePath;
    }

    @Override
    public OutputStream getResourceOutputStream(final Source source, final String resourcePath) throws IOException {
        final Path path = getResourceOutputPath(source, resourcePath);
        Files.createDirectories(path.getParent());
        return new FileOutputStream(path.toFile());
    }

    @Override
    public Path getResourceOutputPath(final Source source, final String resourcePath) throws IOException {
        return FileConfigurationUtils.getResourcePath(modulePath, source, resourcePath);
    }
}
