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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.model.Source;

import static org.onehippo.cm.model.util.FilePathUtils.nativePath;

public class FileResourceOutputProvider implements ResourceOutputProvider {
    private final Path basePath;
    private final String sourceBasePath;

    public FileResourceOutputProvider(final Path basePath, final String sourceBasePath)
    {
        this.basePath = basePath;
        this.sourceBasePath = sourceBasePath;
    }

    public Path getBasePath() {
        return basePath;
    }

    public String getSourceBasePath() {
        return sourceBasePath;
    }

    @Override
    public OutputStream getSourceOutputStream(final Source source) throws IOException {
        final String sourcePath = StringUtils.stripStart(sourceBasePath + "/" + source.getPath(), "/");
        final Path path = basePath.resolve(nativePath(sourcePath));
        Files.createDirectories(path.getParent());
        return new BufferedOutputStream(Files.newOutputStream(path));
    }

    @Override
    public OutputStream getResourceOutputStream(final Source source, final String resourcePath) throws IOException {
        final Path path = getResourcePath(source, resourcePath);
        Files.createDirectories(path.getParent());
        return new BufferedOutputStream(Files.newOutputStream(path));
    }

    @Override
    public Path getResourcePath(final Source source, final String resourcePath) {
        return basePath.resolve(nativePath(getResourceModulePath(source, resourcePath)));
    }

    @Override
    public String getResourceModulePath(final Source source, final String resourcePath) {
        final String resourceModulePath;
        if (resourcePath.startsWith("/")) {
            resourceModulePath = sourceBasePath + resourcePath;
        } else {
            resourceModulePath = sourceBasePath + getSourceFolder(source) + "/"+ resourcePath;
        }
        return StringUtils.stripStart(resourceModulePath, "/");
    }

    /**
     * @param source a Source with a reasonable getPath()
     * @return a relative path to the folder containing the given source, using UNIX style forward-slash separators
     */
    private String getSourceFolder(final Source source) {
        return source.getPath().indexOf('/') != -1 ? "/" + StringUtils.substringBeforeLast(source.getPath(), "/") : "";
    }
}
