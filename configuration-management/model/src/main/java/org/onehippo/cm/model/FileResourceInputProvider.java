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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.ResourceInputProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.model.util.FilePathUtils.nativePath;

/**
 * Provides access to resource InputStreams using path references relative to a base path. The base path is typically
 * the config or content root folder, not the module root.
 */
public class FileResourceInputProvider implements ResourceInputProvider {

    private static final Logger logger = LoggerFactory.getLogger(FileResourceInputProvider.class);

    private final Path basePath;
    private final String sourceBasePath;

    public FileResourceInputProvider(final Path basePath, final String sourceBasePath) {
        this.basePath = basePath;
        this.sourceBasePath = sourceBasePath;
    }

    @Override
    public boolean hasResource(final Source source, final String resourcePath) {
        return Files.isRegularFile(getResourcePath(source, resourcePath));
    }

    @Override
    public InputStream getResourceInputStream(final Source source, final String resourcePath) throws IOException {
        return Files.newInputStream(getResourcePath(source, resourcePath).toRealPath());
    }

    public Path getResourcePath(final Source source, final String resourcePath) {
        return basePath.resolve(nativePath(getResourceModulePath(source, resourcePath)));
    }

    public Path getBasePath() {
        return basePath;
    }

    public String getSourceBasePath() {
        return sourceBasePath;
    }

    /**
     * @param source a Source file in the module managed by this ResourceInputProvider
     * @return a full native-style path to a given source file, suitable for use in error reporting
     */
    public String getFullSourcePath(final Source source) {
        return basePath.resolve(nativePath(source.getPath())).toString();
    }

    public String getResourceModulePath(final Source source, final String resourcePath) {
        final String resourceModulePath;
        if (resourcePath.startsWith("/")) {
            resourceModulePath = sourceBasePath + resourcePath;
        } else {
            resourceModulePath = sourceBasePath + getSourceFolder(source) + "/"+ resourcePath;
        }
        return StringUtils.stripStart(resourceModulePath, "/");
    }

    private String getSourceFolder(final Source source) {
        return source.getPath().indexOf('/') != -1 ? "/" + StringUtils.substringBeforeLast(source.getPath(), "/") : "";
    }
}
