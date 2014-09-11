/*
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

package org.onehippo.cms7.services.webresources;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

import javax.jcr.Session;

import org.onehippo.cms7.services.SingletonService;
import org.onehippo.cms7.services.WhiteboardService;

@SingletonService
@WhiteboardService
@SuppressWarnings("UnusedDeclaration")
public interface WebResourcesService {

    public static final String JCR_ROOT_PATH = "/webresources";

    /**
     * Creates a web resources implementation based on JCR.
     *
     * @param session the JCR session used to access web resources.
     * @param bundleName the name of the web resources bundle.
     * @return a JCR-based web resources implementation for <code>bundleName</code>.
     * @throw WebResourceException if the {@link WebResourceBundle} for <code>bundleName</code> cannot be found
     */
    WebResourceBundle getJcrWebResourceBundle(Session session, String bundleName) throws WebResourceException;

    /**
     * Imports a web resource bundle from a directory. The name of the directory is used as the name of the bundle.
     * Existing web resources in JCR are replaced by the new ones. Missing web resources are deleted from JCR.
     *
     * @param session the JCR session used to access web resources.
     * @param directory the directory containing the web resources to import.
     * @throws IOException if an I/O error occurs while reading web resources from file system
     * @throws WebResourceException if another error occurs while importing web resources
     */
    void importJcrWebResourceBundle(Session session, File directory) throws IOException, WebResourceException;

    /**
     * Imports a web resource bundle from the given zip file. The zip file should contain a single root directory entry
     * that contains all web resources. The name of the root directory entry is used as the name of the bundle.
     * Existing web resources in JCR are replaced by the new ones. Missing web resources are deleted from JCR.
     *
     * @param session the JCR session used to access web resources.
     * @param zip the ZIP file containing the web resources to import.
     * @throws IOException if an I/O error occurs while reading web resources from the ZIP file
     * @throws WebResourceException if another error occurs while importing web resources
     */
    void importJcrWebResourceBundle(Session session, ZipFile zip) throws IOException, WebResourceException;

    /**
     * Imports a sub-tree of a web resource bundle from a directory. The sub-tree can consist of a directory or
     * a single file.
     *
     * @param session the JCR session used to access web resources.
     * @param bundleName the name of the web resource bundle
     * @param bundleSubPath the relative sub-path in the web resource bundle to import the resources into.
     * @param fileOrDirectory the file or directory to import web resources from.
     * @throws WebResourceException if an error occurs while importing web resources
     */
    void importJcrWebResources(Session session, String bundleName, String bundleSubPath, File fileOrDirectory) throws WebResourceException;

}
