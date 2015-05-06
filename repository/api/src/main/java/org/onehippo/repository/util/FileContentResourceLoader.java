/**
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.onehippo.repository.xml.ContentResourceLoader;


/**
 * {@link ContentResourceLoader} implementation for loading resources from a directory.
 */
public class FileContentResourceLoader implements ContentResourceLoader {

    private final File baseDir;

    public FileContentResourceLoader(final File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Returns a URL in file URL scheme.
     * 
     * @see <a href="http://en.wikipedia.org/wiki/URI_scheme">http://en.wikipedia.org/wiki/URI_scheme</a>
     */
    @Override
    public URL getResource(String path) throws MalformedURLException {
        if (path == null) {
            throw new IllegalArgumentException("Path is null.");
        }

        final String relPath = path.startsWith("/") ? path.substring(1) : path;
        File file = getRelativeResourceFile(relPath);

        if (file != null) {
            return file.toURI().toURL();
        }

        return null;
    }

    @Override
    public InputStream getResourceAsStream(String path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path is null.");
        }

        final String relPath = path.startsWith("/") ? path.substring(1) : path;
        File file = getRelativeResourceFile(relPath);

        if (file != null) {
            return new FileInputStream(file);
        }

        return null;
    }

    private File getRelativeResourceFile(String relPath) {
        File file = new File(baseDir, relPath);

        if (file.exists()) {
            return file;
        }

        return null;
    }
}
