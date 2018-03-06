/*
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

package org.onehippo.cms7.services.webfiles;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

import javax.jcr.Session;

import org.onehippo.cms7.services.SingletonService;
import org.onehippo.cms7.services.WhiteboardService;

@SingletonService
@WhiteboardService
@SuppressWarnings("UnusedDeclaration")
public interface WebFilesService {

    String JCR_ROOT_PATH = "/webfiles";

    /**
     * Dont reload bundle even if it is modified at classpath level
     */
    String RELOAD_NEVER = "RELOAD_NEVER";

    /**
     * Reload bundle from JAR if bundle's runtime digest is consistent with the one from baseline
     */
    String RELOAD_IF_RUNTIME_UNCHANGED = "RELOAD_IF_RUNTIME_UNCHANGED";

    /**
     * Reload bundle even if runtime digest is inconsistent with the one from baseline (runtime was modified)
     */
    String RELOAD_DISCARD_RUNTIME_CHANGES = "RELOAD_DISCARD_RUNTIME_CHANGES";

    /**
     * @param file {@link File}
     * @return Check if given file is part of webfile bundle
     */
    boolean fileMatches(File file);

    /**
     * @return Reload mode for bundles
     */
    String getReloadMode();

    /**
     * Creates a web files implementation based on JCR.
     *
     * @param session the JCR session used to access web files.
     * @param bundleName the name of the web files bundle.
     * @return a JCR-based web files implementation for <code>bundleName</code>.
     * @throws WebFileException if the {@link WebFileBundle} for <code>bundleName</code> cannot be found
     */
    WebFileBundle getJcrWebFileBundle(Session session, String bundleName) throws WebFileException;

    /**
     * Imports a web file bundle from a directory. The name of the directory is used as the name of the bundle.
     * Existing web files in JCR are replaced by the new ones. Missing web files are deleted from JCR.
     * The caller of this method is responsible for saving the changes made in the session.
     *
     * @param session the JCR session used to access web files.
     * @param directory the directory containing the web files to import.
     * @param skipIfAlreadyAutoLoaded If <code>true</code> and AutoReloadService is enabled, then skip loading of webfilebundle
     *                                as it already will have been loaded by AutoReloadService. If AutoReloadService is disabled,
     *                                load webfilebundle.<br/><code>false</code>
     *                                Webfilebundle must be loaded, regardless of the state of AutoReloadService.
     * @throws IOException if an I/O error occurs while reading web files from file system
     * @throws WebFileException if another error occurs while importing web files
     */
    void importJcrWebFileBundle(Session session, File directory, boolean skipIfAlreadyAutoLoaded) throws IOException, WebFileException;

    /**
     * Imports a web file bundle from the given zip file. The zip file should contain a single root directory entry
     * that contains all web files. The name of the root directory entry is used as the name of the bundle.
     * Existing web files in JCR are replaced by the new ones. Missing web files are deleted from JCR.
     * The caller of this method is responsible for saving the changes made in the session.
     *
     * @param session the JCR session used to access web files.
     * @param zip the ZIP file containing the web files to import.
     * @param skipIfAlreadyAutoLoaded If <code>true</code> and AutoReloadService is enabled, then skip loading of webfilebundle
     *                                as it already will have been loaded by AutoReloadService. If AutoReloadService is disabled,
     *                                load webfilebundle.<br/><code>false</code>
     *                                Webfilebundle must be loaded, regardless of the state of AutoReloadService.
     * @throws IOException if an I/O error occurs while reading web files from the ZIP file
     * @throws WebFileException if another error occurs while importing web files
     */
    void importJcrWebFileBundle(Session session, ZipFile zip, boolean skipIfAlreadyAutoLoaded) throws IOException, WebFileException;

    /**
     * Imports a sub-tree of a web file bundle from a directory. The sub-tree can consist of a directory or
     * a single file. The caller of this method is responsible for saving the changes made in the session.
     *
     * @param session the JCR session used to access web files.
     * @param bundleName the name of the web file bundle
     * @param bundleSubPath the relative sub-path in the web file bundle to import the resources into.
     * @param fileOrDirectory the file or directory to import web files from.
     * @throws IOException if an I/O error occurs while reading web files from the fileOrDirectory
     * @throws WebFileException if an error occurs while importing web files
     */
    void importJcrWebFiles(Session session, String bundleName, String bundleSubPath, File fileOrDirectory) throws IOException, WebFileException;

}
