/**
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Strategy interface for loading resources (e.g, file system resources, zip entry resources or URI based resources).
 */
public interface ContentResourceLoader {

    /**
     * Returns a URL to the resource that is mapped to the given path.
     *
     * <p>The path must begin with a <tt>/</tt> and is interpreted
     * as relative to the current content root.
     * 
     * <p>This method returns <code>null</code>
     * if no resource is mapped to the pathname.
     * 
     * @param path a <code>String</code> specifying
     * the path to the resource
     *
     * @return the resource located at the named path,
     * or <code>null</code> if there is no resource at that path
     *
     * @exception MalformedURLException if the pathname is not given in 
     * the correct form
     */
    public URL getResource(String path) throws MalformedURLException;

    /**
     * Returns the resource located at the named path as
     * an <code>InputStream</code> object.
     *
     * <p>
     * The path must be specified according
     * to the rules given in <code>getResource</code>.
     * This method returns <code>null</code> if no resource exists at
     * the specified path. 
     *
     * @param path  a <code>String</code> specifying the path
     *          to the resource
     *
     * @return the <code>InputStream</code> returned to the 
     *         caller, or <code>null</code> if no resource
     *         exists at the specified path
     * 
     * @throws IOException
     */
    public InputStream getResourceAsStream(String path) throws IOException;

}
