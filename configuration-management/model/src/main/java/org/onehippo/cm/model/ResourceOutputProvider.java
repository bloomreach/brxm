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
import java.io.OutputStream;
import java.nio.file.Path;

public interface ResourceOutputProvider {

    /**
     * Get an OutputStream to write a YAML Source.
     * Note, caller is responsible for closing the stream when finished with it.
     * @param source
     * @throws IOException
     */
    OutputStream getSourceOutputStream(final Source source) throws IOException;

    /**
     * Get an OutputStream to write to a resource referenced in a YAML Source.
     * Note, caller is responsible for closing the stream when finished with it.
     * @param source
     * @param resourcePath
     * @throws IOException
     */
    OutputStream getResourceOutputStream(final Source source, final String resourcePath) throws IOException;

    /**
     * Gets absolute filesystem path based on combination of source and resource path
     * @param source
     * @param resourcePath
     * @return
     */
    Path getResourcePath(final Source source, final String resourcePath);

    String getSourceBasePath();
    String getResourceModulePath(final Source source, final String resourcePath);
}
