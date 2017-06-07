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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.onehippo.cm.ResourceInputProvider;
import org.onehippo.cm.model.util.FileConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides access to resource InputStreams using path references relative to a base path. The base path is typically
 * the config or content root folder, not the module root.
 */
public class FileResourceInputProvider implements ResourceInputProvider {

    private static final Logger logger = LoggerFactory.getLogger(FileResourceInputProvider.class);

    private final Path basePath;

    public FileResourceInputProvider(final Path basePath) {
        this.basePath = basePath;
    }

    @Override
    public boolean hasResource(final Source source, final String resourcePath) {
        return Files.isRegularFile(FileConfigurationUtils.getResourcePath(basePath, source, resourcePath));
    }

    @Override
    public InputStream getResourceInputStream(final Source source, final String resourcePath) throws IOException {
        return FileConfigurationUtils.getResourcePath(basePath, source, resourcePath).toRealPath().toUri().toURL().openStream();
    }

    @Override
    public URL getBaseURL() {
        try {
            return basePath.toUri().toURL();
        } catch (MalformedURLException e) {
            logger.error("Cannot create URL from basePath '{}'", basePath, e.getMessage());
            return null;
        }
    }

    @Override
    public Path getBasePath() {
        return basePath;
    }

}
