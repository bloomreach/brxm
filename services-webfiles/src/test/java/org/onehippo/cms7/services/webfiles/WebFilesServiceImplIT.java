/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.webfiles;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipFile;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onehippo.cms7.services.webfiles.watch.GlobFileNameMatcher;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms7.services.webfiles.watch.WebFilesWatcherConfig.DEFAULT_MAX_FILE_LENGTH_KB;

public class WebFilesServiceImplIT extends RepositoryTestCase {

    private static final String[] CONTENTS = {
            "/test", "nt:unstructured"
    };

    private GlobFileNameMatcher importedFiles;
    private WebFilesServiceImpl service;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(CONTENTS, session);
        importedFiles = new GlobFileNameMatcher();
        importedFiles.includeFiles("*.css");
        importedFiles.includeFiles("*.js");
        service = new WebFilesServiceImpl(importedFiles, 1024 * DEFAULT_MAX_FILE_LENGTH_KB, WebFilesService.RELOAD_NEVER);
    }

    @After
    public void tearDown() throws Exception {
        session.removeItem("/test");
        final NodeIterator bundles = session.getNode("/webfiles").getNodes();
        while (bundles.hasNext()) {
            bundles.nextNode().remove();
        }
        session.save();
        super.tearDown();
    }

    @Test
    public void importBundleFromDirectory() throws RepositoryException, IOException {
        File testBundleDir = FileUtils.toFile(getClass().getResource("/testbundle"));
        service.importJcrWebFileBundle(session, testBundleDir, true);
        assertTestBundle();
    }

    @Test
    public void importEmptyBundleFromDirectory() throws IOException, RepositoryException {
        File emptyBundleDir = testFolder.newFolder("testbundle");

        service.importJcrWebFileBundle(session, emptyBundleDir, true);

        final WebFileBundle testBundle = service.getJcrWebFileBundle(session, "testbundle");
        assertNotNull("testbundle should exist", testBundle);
    }

    @Test
    public void reimportBundleFromDirectory() throws RepositoryException, IOException {
        File testBundleDir = FileUtils.toFile(getClass().getResource("/testbundle"));
        service.importJcrWebFileBundle(session, testBundleDir, true);
        service.importJcrWebFileBundle(session, testBundleDir, true);
        assertTestBundle();
    }

    @Test
    public void importBundleWithoutFileDeletesJcrFile() throws IOException, RepositoryException {
        File testBundleDir = FileUtils.toFile(getClass().getResource("/testbundle"));
        service.importJcrWebFileBundle(session, testBundleDir, true);

        File changedBundleDir = testFolder.newFolder("testbundle");
        FileUtils.copyDirectory(testBundleDir, changedBundleDir);

        File styleCss = new File(changedBundleDir, "css/style.css");
        FileUtils.forceDelete(styleCss);

        service.importJcrWebFileBundle(session, changedBundleDir, true);

        final WebFileBundle testBundle = service.getJcrWebFileBundle(session, "testbundle");
        assertFalse("Deleted file 'css/style.css' should have been deleted from web files too", testBundle.exists("/css/style.css"));
    }

    @Test
    public void importChangedFile() throws IOException, RepositoryException, InterruptedException {
        File testBundleDir = FileUtils.toFile(getClass().getResource("/testbundle"));
        service.importJcrWebFileBundle(session, testBundleDir, true);

        File changedBundleDir = testFolder.newFolder("testbundle");
        FileUtils.copyDirectory(testBundleDir, changedBundleDir);

        File styleCss = new File(changedBundleDir, "css" + File.separator + "style.css");
        final String newStyleCssData = "/* new style.css data */";
        FileUtils.write(styleCss, newStyleCssData);

        service.importJcrWebFiles(session, "testbundle", "css/style.css", styleCss);

        final WebFileBundle testBundle = service.getJcrWebFileBundle(session, "testbundle");
        assertContent(testBundle.get("/css/style.css"), newStyleCssData);
    }

    @Test
    public void importDeletedDirectoryIsRemoved() throws IOException, RepositoryException {
        File testBundleDir = FileUtils.toFile(getClass().getResource("/testbundle"));
        service.importJcrWebFileBundle(session, testBundleDir, true);

        File changedBundleDir = testFolder.newFolder("testbundle");
        FileUtils.copyDirectory(testBundleDir, changedBundleDir);

        File css = new File(changedBundleDir, "css");
        FileUtils.forceDelete(css);

        service.importJcrWebFileBundle(session, changedBundleDir, true);

        final WebFileBundle testBundle = service.getJcrWebFileBundle(session, "testbundle");
        assertFalse("Deleting directory 'css' should have deleted 'css/style.css' from web files too", testBundle.exists("/css/style.css"));
    }

    @Test
    public void excludedDirectoryIsNotImported() throws IOException, RepositoryException, InterruptedException {
        importedFiles.excludeDirectories(".git");

        File testBundleDir = FileUtils.toFile(getClass().getResource("/testbundle"));
        File changedBundleDir = testFolder.newFolder("testbundle");
        FileUtils.copyDirectory(testBundleDir, changedBundleDir);

        File ignoredDir = new File(changedBundleDir, ".git");
        FileUtils.forceMkdir(ignoredDir);

        service.importJcrWebFileBundle(session, changedBundleDir, true);

        assertFalse("Ignored .git directory should not have been imported", session.nodeExists("/webfiles/testbundle/.git"));
    }

    @Test
    public void importBundleFromDirectoryWithCustomContentXmlInRoot() throws IOException, RepositoryException {
        File testBundleDir = FileUtils.toFile(getClass().getResource("/testbundle"));

        File customBundleDir = testFolder.newFolder("testbundle");
        FileUtils.copyDirectory(testBundleDir, customBundleDir);

        File contentXml = new File(customBundleDir, ".content.xml");
        FileUtils.write(contentXml,
                "<jcr:root xmlns:jcr=\"http://www.jcp.org/jcr/1.0\"\n" +
                        "          jcr:primaryType=\"webfiles:bundle\">\n" +
                        "  <test jcr:primaryType=\"nt:folder\"/>\n" +
                        "</jcr:root>");

        importedFiles.includeFiles(".content.xml");
        service.importJcrWebFileBundle(session, customBundleDir, true);

        assertTestBundle();
        assertTrue("test folder defined in .content.xml exists", session.nodeExists("/webfiles/testbundle/test"));
        assertEquals("nt:folder", session.getNode("/webfiles/testbundle/test").getPrimaryNodeType().getName());
    }

    @Test
    public void importBundleFromDirectoryWithEmptyDirectory() throws IOException, RepositoryException {
        File testBundleDir = FileUtils.toFile(getClass().getResource("/testbundle"));
        File bundleWithEmptyDir = testFolder.newFolder("testbundle");
        FileUtils.copyDirectory(testBundleDir, bundleWithEmptyDir);

        File emptyDir = new File(bundleWithEmptyDir, "emptyDir");
        FileUtils.forceMkdir(emptyDir);

        service.importJcrWebFileBundle(session, bundleWithEmptyDir, true);

        final String emptyDirNodePath = WebFilesService.JCR_ROOT_PATH + "/testbundle/emptyDir";
        assertTrue("node exists: " + emptyDirNodePath, session.nodeExists(emptyDirNodePath));
        final Node emptyDirNode = session.getNode(emptyDirNodePath);
        assertEquals("nt:folder", emptyDirNode.getPrimaryNodeType().getName());
    }

    @Test
    public void importBundleFromZip() throws RepositoryException, IOException {
        File testBundleFile = FileUtils.toFile(getClass().getResource("/testbundle.zip"));
        ZipFile testBundleZip = new ZipFile(testBundleFile);
        service.importJcrWebFileBundle(session, testBundleZip, true);
        assertTestBundle();
    }

    @Test
    public void importEmptyBundleFromZip() throws IOException, RepositoryException {
        File emptyBundleFile = FileUtils.toFile(getClass().getResource("/testbundle-empty.zip"));
        ZipFile emptyBundleZip = new ZipFile(emptyBundleFile);
        service.importJcrWebFileBundle(session, emptyBundleZip, true);
        assertNotNull("testbundle should exist", service.getJcrWebFileBundle(session, "testbundle"));
    }

    @Test(expected = WebFileException.class)
    public void importEmptyZip() throws IOException, RepositoryException {
        File emptyZipFile = FileUtils.toFile(getClass().getResource("/empty.zip"));
        ZipFile emptyZip = new ZipFile(emptyZipFile);
        service.importJcrWebFileBundle(session, emptyZip, true);
    }

    @Test
    public void importBundleFromZipWithCustomContentXmlInRoot() throws IOException, RepositoryException {
        importedFiles.includeFiles(".content.xml");

        File testBundleFile = FileUtils.toFile(getClass().getResource("/testbundle-with-custom-dot-content-xml.zip"));
        ZipFile testBundleZip = new ZipFile(testBundleFile);
        service.importJcrWebFileBundle(session, testBundleZip, true);

        assertTestBundle();
        assertTrue("test folder defined in .content.xml exists", session.nodeExists("/webfiles/testbundle/test"));
        assertEquals("nt:folder", session.getNode("/webfiles/testbundle/test").getPrimaryNodeType().getName());
    }

    @Test
    public void importBundleFromZipWithEmptyDirectory() throws IOException, RepositoryException {
        File testBundleFile = FileUtils.toFile(getClass().getResource("/testbundle-with-empty-dir.zip"));
        ZipFile testBundleZip = new ZipFile(testBundleFile);
        service.importJcrWebFileBundle(session, testBundleZip, true);

        assertTestBundle();
        final String emptyDirNodePath = WebFilesService.JCR_ROOT_PATH + "/testbundle/emptyDir";
        assertTrue("node exists: " + emptyDirNodePath, session.nodeExists(emptyDirNodePath));
        final Node emptyDirNode = session.getNode(emptyDirNodePath);
        assertEquals("nt:folder", emptyDirNode.getPrimaryNodeType().getName());
    }

    @Test
    public void reimportAllWebFilesAfterDeletingRootDirectory() throws IOException, RepositoryException {
        File testBundleDir = FileUtils.toFile(getClass().getResource("/testbundle"));
        service.importJcrWebFileBundle(session, testBundleDir, true);

        File changedBundleDir = testFolder.newFolder("testbundle");
        FileUtils.copyDirectory(testBundleDir, changedBundleDir);

        File css = new File(changedBundleDir, "css");
        FileUtils.forceDelete(css);

        service.importJcrWebFiles(session, "testbundle", "", changedBundleDir);

        final WebFileBundle testBundle = service.getJcrWebFileBundle(session, "testbundle");
        assertFalse("deleting directory 'css' should have deleted 'css/style.css' from web files too", testBundle.exists("/css/style.css"));
        assertTrue("the 'js' directory should still exist", testBundle.exists("/js/script.js"));
    }

    private void assertTestBundle() throws IOException {
        final WebFileBundle testBundle = service.getJcrWebFileBundle(session, "testbundle");
        assertTrue(testBundle.exists("/css/style.css"));
        assertTrue(testBundle.exists("/js/script.js"));

        final WebFile styleCss = testBundle.get("/css/style.css");
        assertEquals("text/css", styleCss.getMimeType());
        assertContent(styleCss, "/* style.css */");

        final WebFile scriptJs = testBundle.get("/js/script.js");
        assertEquals("application/javascript", scriptJs.getMimeType());
        assertContent(scriptJs, "/* script.js */");
    }

    private void assertContent(WebFile resource, String... expectedLines) throws IOException {
        final List<String> readLines = IOUtils.readLines(resource.getBinary().getStream());
        assertEquals(expectedLines.length, readLines.size());
        for (int i = 0; i < expectedLines.length; i++) {
            assertEquals(expectedLines[i], readLines.get(i));
        }
    }

}
