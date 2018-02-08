/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.source;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstracts access to configuration module content from various storage formats, e.g. classpath, the native FileSystem,
 * or the JCR.
 */
public interface ResourceInputProvider {

    /**
     * Can an InputStream be created for a given resource reference, relative to the given YAML Source or relative to
     * the config or content base path within a module?
     * @param source the base YAML Source from which a reference should be resolved -- should not be null
     * @param resourcePath a relative path from the source (if no leading slash), or an absolute path from the base path
     *                     (if a leading slash is present), to the desired resource
     */
    boolean hasResource(final Source source, final String resourcePath);

    /**
     * Get an InputStream to resolve a resource reference in a YAML Source or relative to
     * the config or content base path within a module. Note, caller is responsible for closing the stream when finished
     * with it.
     * @param source the base YAML Source from which a reference should be resolved -- should not be null
     * @param resourcePath a relative path from the source (if no leading slash), or an absolute path from the base path
     *                     (if a leading slash is present), to the desired resource
     * @throws IOException in case of any unexpected problem in opening the desired InputStream
     */
    InputStream getResourceInputStream(final Source source, final String resourcePath) throws IOException;

}
