/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.webresources;

import javax.jcr.Binary;

/**
 * A collection of web resources, i.e. binary data objects. Each web resource is identified by an absolute path
 * starting at the web resources root. Paths start with a slash and consist of elements separated by slashes
 * (e.g. "/css/style.css").
 */
public interface WebResources {

    /**
     * @param absPath the absolute path to the web resource, starting at the web resources root. The path must start with a slash.
     * @return whether a web resource exists at the given path.
     */
    boolean exists(String absPath);

    /**
     * @param absPath the absolute path to the web resource, starting at the web resources root. The path must start with a slash.
     * @return the web resource located at the given path.
     * @throws WebResourceNotFoundException if no web resource exists at the given path.
     */
    WebResource get(String absPath) throws WebResourceNotFoundException;

    /**
     * Creates a new web resource at the given location.
     * @param absPath the absolute path to the web resource, starting at the web resources root. The path must start with a slash.
     * @param content the content of the new web resource.
     * @return the created web resource.
     * @throws WebResourceAlreadyExistsException if another web resource already exists at the given location.
     */
    WebResource create(String absPath, Binary content) throws WebResourceAlreadyExistsException;

}
