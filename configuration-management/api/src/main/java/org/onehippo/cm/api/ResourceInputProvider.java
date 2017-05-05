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
package org.onehippo.cm.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.onehippo.cm.api.model.Source;

/**
 * Abstracts access to configuration module content from various storage formats, e.g. classpath, the native FileSystem,
 * or the JCR.
 */
public interface ResourceInputProvider {
    /**
     * Can an InputProvider be created for a given resource reference, relative to the given YAML Source or relative to
     * a base path within a module.
     * @param source the base YAML Source from which a reference should be resolved, or null if the base path should be used
     * @param resourcePath a relative path from the source, or an absolute path from the base path, to the desired resource
     */
    boolean hasResource(final Source source, final String resourcePath);

    /**
     * Get an InputProvider to resolve a resource reference in a YAML Source or relative to
     * a base path within a module. Note, caller is responsible for closing the stream when finished with it.
     * @param source the base YAML Source from which a reference should be resolved, or null if the base path should be used
     * @param resourcePath a relative path from the source, or an absolute path from the base path, to the desired resource
     * @throws IOException
     */
    InputStream getResourceInputStream(final Source source, final String resourcePath) throws IOException;

    /**
     * @return the root URL of the base path within a Module for which this ResourceInputProvider is responsible.
     */
    URL getBaseURL();
}
