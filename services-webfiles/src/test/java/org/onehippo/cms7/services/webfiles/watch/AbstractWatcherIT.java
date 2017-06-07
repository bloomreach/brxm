/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.webfiles.watch;

import java.io.File;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms7.services.webfiles.util.WatchFilesUtils.PROJECT_BASEDIR_PROPERTY;
import static org.onehippo.cms7.services.webfiles.util.WatchFilesUtils.WEB_FILES_LOCATION_IN_MODULE;

public class AbstractWatcherIT {

    protected File webFilesDirectory;
    protected String watchedModule = "repository-data" + File.separator + "webfiles";
    protected Path projectBaseDir;
    protected Path watchModuleDir;
    protected File testBundleDir;
    protected File cssDir;
    protected File styleCss;
    protected File jsDir;
    protected File scriptJs;
    protected File emptyDir;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        String webFilesDirectoryPath = tmpFolder.getRoot().getCanonicalPath()
                + File.separator + watchedModule
                + File.separator + WEB_FILES_LOCATION_IN_MODULE;
        webFilesDirectory = new File(webFilesDirectoryPath);
        FileUtils.forceMkdir(webFilesDirectory);
        webFilesDirectory.deleteOnExit();

        watchModuleDir = webFilesDirectory.toPath().getParent().getParent().getParent();
        projectBaseDir = watchModuleDir.getParent().getParent();

        System.setProperty(PROJECT_BASEDIR_PROPERTY, projectBaseDir.toString());

        testBundleDir = new File(webFilesDirectory, "testbundle");

        final File testBundleFixture = FileUtils.toFile(getClass().getResource("/testbundle"));
        assertNotNull(testBundleFixture);
        FileUtils.copyDirectory(testBundleFixture, testBundleDir);

        emptyDir = new File(testBundleDir, "empty");
        emptyDir.mkdir();

        cssDir = new File(testBundleDir, "css");
        assertTrue(cssDir.isDirectory());

        styleCss = new File(cssDir, "style.css");
        assertTrue(styleCss.isFile());

        jsDir = new File(testBundleDir, "js");
        assertTrue(jsDir.isDirectory());

        scriptJs = new File(jsDir, "script.js");
        assertTrue(scriptJs.isFile());
    }

    @After
    public void tearDown() throws InterruptedException {
        System.clearProperty(PROJECT_BASEDIR_PROPERTY);
    }

}
