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
package org.onehippo.cms7.services.webfiles.vault;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.webfiles.watch.GlobFileNameMatcher;
import org.onehippo.cms7.services.webfiles.watch.WebFilesWatcherConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms7.services.webfiles.vault.AbstractWebFilesArchive.defeatNamespaceMangling;
import static org.onehippo.cms7.services.webfiles.vault.FileNameComparatorUtils.FILE_BASE_NAME_COMPARATOR;
import static org.onehippo.cms7.services.webfiles.watch.WebFilesWatcherConfig.DEFAULT_MAX_FILE_LENGTH_KB;

public class WebFilesFileArchiveTest {

    private File testBundleDir;
    private GlobFileNameMatcher fileNameMatcher;
    private WebFilesFileArchive archive;

    @Before
    public void setUp() {
        testBundleDir = FileUtils.toFile(getClass().getResource("/testbundle"));
        fileNameMatcher = new GlobFileNameMatcher();
        fileNameMatcher.includeFiles("*");
        archive = new WebFilesFileArchive(testBundleDir, fileNameMatcher, 1024 *  DEFAULT_MAX_FILE_LENGTH_KB);
        archive.open(true);
    }

    @After
    public void tearDown() {
        archive.close();
    }

    @Test
    public void root() {
        final Archive.Entry root = archive.getRoot();
        assertEquals("testbundle", root.getName());

        final Collection<? extends Archive.Entry> children = root.getChildren();
        assertEquals("number of children of bundle root", 1, children.size());
    }

    @Test
    public void jcr_root() {
        final Archive.Entry jcrRoot = archive.getJcrRoot();
        assertEquals("jcr_root", jcrRoot.getName());

        final Archive.Entry root = archive.getRoot();
        final Archive.Entry jcrRootAsChild = root.getChildren().iterator().next();
        assertEquals("jcr_root", jcrRootAsChild.getName());
        assertSame(jcrRootAsChild, jcrRoot);

        final Archive.Entry jcrRootByName = root.getChild("jcr_root");
        assertEquals("jcr_root", jcrRootByName.getName());
        assertSame(jcrRootByName, jcrRoot);
    }

    @Test
    public void bundle_root() {
        final Archive.Entry bundleRoot = archive.getBundleRoot();
        assertEquals("testbundle", bundleRoot.getName());

        final Archive.Entry jcrRoot = archive.getJcrRoot();
        final Archive.Entry bundleRootAsChild = jcrRoot.getChildren().iterator().next();
        assertSame(bundleRootAsChild, bundleRoot);

        final Archive.Entry bundleRootByName = jcrRoot.getChild("testbundle");
        assertSame(bundleRootByName, bundleRoot);
    }

    @Test
    public void bundle_root_contains_web_resources_directories() {
        final File[] bundleChildren = testBundleDir.listFiles();
        final Collection<? extends Archive.Entry> bundleRootChildren = archive.getBundleRoot().getChildren();
        assertEquals("number of children of bundle root", bundleChildren.length, bundleRootChildren.size());
        for (File bundleChild : bundleChildren) {
            assertTrue("archive contains entry for " + bundleChild,
                    containsName(bundleRootChildren, bundleChild.getName()));
        }
    }

    @Test
    public void files_are_returned_in_alphabetical_order_of_base_name() {
        final Collection<? extends Archive.Entry> cssChildren = archive.getBundleRoot().getChild("css").getChildren();
        final Archive.Entry[] cssEntries = cssChildren.toArray(new Archive.Entry[0]);

        // plain alphabetical sorting on file name puts "style-extra.css" before "style.css",
        // but we want alphabetical sorting on the base name (i.e. the file name without the extension)
        assertEquals("__style_extra.css", cssEntries[0].getName());
        assertEquals("style.css", cssEntries[1].getName());
        assertEquals("style-extra.css", cssEntries[2].getName());
    }

    @Test
    public void files_and_folder_of_same_name_except_extension_do_not_collapse_to_one() {
        final Collection<? extends Archive.Entry> mainChildren = archive.getBundleRoot().getChild("ftl").getChild("main").getChildren();
        final Archive.Entry[] mainEntries = mainChildren.toArray(new Archive.Entry[0]);

        // plain alphabetical sorting on file name and folder puts folder on top, then files
        // but we want alphabetical sorting on the base name (i.e. the file name without the extension)
        assertEquals("list", mainEntries[0].getName());
        assertEquals("list.ftl", mainEntries[1].getName());
        assertEquals("list.properties", mainEntries[2].getName());
    }

    @Test
    public void directories_and_files_are_returned_in_alphabetical_order() throws IOException {
        traverseAndCheckOrder(testBundleDir, archive.getBundleRoot());
    }

    private void traverseAndCheckOrder(final File currentDir, Archive.Entry current) throws IOException {
        final File[] bundleChildren = currentDir.listFiles();
        Arrays.sort(bundleChildren, FILE_BASE_NAME_COMPARATOR);

        final Collection<? extends Archive.Entry> bundleRootChildren = current.getChildren();
        if (bundleRootChildren == null) {
            return;
        }
        final Archive.Entry[] archiveChildren = bundleRootChildren.toArray(new Archive.Entry[0]);
        for (int i = 0; i < bundleChildren.length; i++) {
            assertEquals(defeatNamespaceMangling(bundleChildren[i].getName()), archiveChildren[i].getName());
        }
        for (File bundleChild : bundleChildren) {
            if (bundleChild.isDirectory()) {
                traverseAndCheckOrder(bundleChild, current.getChild(bundleChild.getName()));
            }
        }
    }

    @Test
    public void bundle_root_skips_excluded_directory() {
        fileNameMatcher.excludeDirectories("css");

        File[] bundleChildren = testBundleDir.listFiles();
        final Archive.Entry bundleRoot = archive.getBundleRoot();
        Collection<? extends Archive.Entry> bundleRootChildren = bundleRoot.getChildren();
        assertEquals("number of children of bundle root", bundleChildren.length - 1, bundleRootChildren.size());
        assertFalse("archive should not contain entry for excluded 'css' dir", containsName(bundleRootChildren, "css"));
        assertNull("entry for excluded 'css' dir should not returned;", bundleRoot.getChild("css"));
    }

    @Test
    public void excluded_file_is_skipped_in_sub_directories() {
        fileNameMatcher.reset();
        fileNameMatcher.includeFiles("*.js");

        File[] bundleChildren = testBundleDir.listFiles();
        final Archive.Entry bundleRoot = archive.getBundleRoot();
        Collection<? extends Archive.Entry> bundleRootChildren = bundleRoot.getChildren();
        assertEquals("number of children of bundle root", bundleChildren.length, bundleRootChildren.size());

        final Archive.Entry cssDirEntry = bundleRoot.getChild("css");
        assertNotNull("entry for directory 'css' should not be excluded", cssDirEntry);
        final Archive.Entry styleCss = cssDirEntry.getChild("style.css");
        assertNull("entry for file 'css/style.css' should be excluded", styleCss);
    }

    @Test
    public void only_included_files_are_returned() {
        fileNameMatcher.reset();
        fileNameMatcher.includeFiles("*.css");
        archive = new WebFilesFileArchive(testBundleDir, fileNameMatcher, 1024 * 256);
        archive.open(true);

        final Archive.Entry bundleRoot = archive.getBundleRoot();

        final Archive.Entry cssDirEntry = bundleRoot.getChild("css");
        final Archive.Entry styleCss = cssDirEntry.getChild("style.css");
        assertNotNull("entry for file 'css/style.css' should be included;", styleCss);

        final Archive.Entry jsDirEntry = bundleRoot.getChild("js");
        final Archive.Entry scriptJs = jsDirEntry.getChild("script.js");
        assertNull("entry for file 'js/script.js' should be excluded;", scriptJs);
    }

    @Test
    public void files_that_exceed_maxFileLengthBytes_are_skipped() {
        fileNameMatcher.reset();
        fileNameMatcher.includeFiles("*.css");
        archive = new WebFilesFileArchive(testBundleDir, fileNameMatcher, 1);
        archive.open(true);

        final Archive.Entry bundleRoot = archive.getBundleRoot();

        final Archive.Entry cssDirEntry = bundleRoot.getChild("css");
        final Archive.Entry styleCss = cssDirEntry.getChild("style.css");
        assertNull("entry for file 'css/style.css' should not be included since style.css is too large;", styleCss);

        final Archive.Entry jsDirEntry = bundleRoot.getChild("js");
        final Archive.Entry scriptJs = jsDirEntry.getChild("script.js");
        assertNull("entry for file 'js/script.js'should not be included since script.js is too large", scriptJs);
    }

    private boolean containsName(final Collection<? extends Archive.Entry> archiveChildren, final String name) {
        for (Archive.Entry archiveChild : archiveChildren) {
            if (archiveChild.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

}