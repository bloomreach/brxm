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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.webfiles.watch.GlobFileNameMatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms7.services.webfiles.vault.FileNameComparatorUtils.BASE_NAME_COMPARATOR;
import static org.onehippo.cms7.services.webfiles.watch.WebFilesWatcherConfig.DEFAULT_MAX_FILE_LENGTH_KB;

public class WebFilesZipArchiveTest {

    private ZipFile testBundleZip;
    private GlobFileNameMatcher fileNameMatcher;
    private WebFilesZipArchive archive;

    @Before
    public void setUp() throws IOException {
        File testBundleZipFile = FileUtils.toFile(getClass().getResource("/testbundle.zip"));
        testBundleZip = new ZipFile(testBundleZipFile);
        fileNameMatcher = new GlobFileNameMatcher();
        fileNameMatcher.includeFiles("*");
        archive = new WebFilesZipArchive(testBundleZip, fileNameMatcher, 1024 *  DEFAULT_MAX_FILE_LENGTH_KB);
    }

    @After
    public void tearDown() {
        archive.close();
    }

    @Test
    public void root() throws IOException {
        archive.open(true);
        final Archive.Entry root = archive.getRoot();
        assertEquals(testBundleZip.getName(), root.getName());

        final Collection<? extends Archive.Entry> children = root.getChildren();
        assertEquals("number of children of bundle root", 1, children.size());
    }

    @Test
    public void jcr_root() throws IOException {
        archive.open(true);
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
    public void bundle_root() throws IOException {
        archive.open(true);
        final Archive.Entry bundleRoot = archive.getBundleRoot();
        assertEquals("testbundle", bundleRoot.getName());

        final Archive.Entry jcrRoot = archive.getJcrRoot();
        final Archive.Entry bundleRootAsChild = jcrRoot.getChildren().iterator().next();
        assertSame(bundleRootAsChild, bundleRoot);

        final Archive.Entry bundleRootByName = jcrRoot.getChild("testbundle");
        assertSame(bundleRootByName, bundleRoot);
    }

    @Test
    public void bundle_root_contains_web_resources_directories() throws IOException {
        archive.open(true);
        final Collection<? extends Archive.Entry> bundleRootChildren = archive.getBundleRoot().getChildren();
        assertEquals("number of children of bundle root", 4, bundleRootChildren.size());
        assertTrue("archive contains entries 'css' and 'js'", containsNames(bundleRootChildren, "css", "empty", "ftl", "js"));
    }

    @Test
    public void bundle_root_skips_excluded_directory() throws IOException {
        fileNameMatcher.excludeDirectories("css");
        archive.open(true);

        final Archive.Entry bundleRoot = archive.getBundleRoot();
        Collection<? extends Archive.Entry> bundleRootChildren = bundleRoot.getChildren();
        assertEquals("number of children of bundle root", 3, bundleRootChildren.size());
        assertFalse("archive should not contain entry for ignored 'css' dir", containsNames(bundleRootChildren, "css"));
        assertNull("entry for ignored 'css' dir should not returned;", bundleRoot.getChild("css"));
    }

    @Test
    public void excluded_directory_is_skipped_in_sub_directories() throws IOException {
        fileNameMatcher.reset();
        fileNameMatcher.includeFiles("*.js");
        archive.open(true);

        final Archive.Entry bundleRoot = archive.getBundleRoot();
        Collection<? extends Archive.Entry> bundleRootChildren = bundleRoot.getChildren();
        //assertEquals("number of children of bundle root", 2, bundleRootChildren.size());

        final Archive.Entry cssDirEntry = bundleRoot.getChild("css");
        assertNotNull("entry for directory 'css' should not be ignored;", cssDirEntry);
        final Archive.Entry styleCss = cssDirEntry.getChild("style.css");
        assertNull("entry for file 'css/style.css' should be ignored;", styleCss);
    }

    @Test
    public void files_are_returned_in_alphabetical_order_of_base_name() throws IOException {
        archive.open(true);
        final Collection<? extends Archive.Entry> cssChildren = archive.getBundleRoot().getChild("css").getChildren();
        final Archive.Entry[] cssEntries = cssChildren.toArray(new Archive.Entry[0]);

        // plain alphabetical sorting on file name puts "style-extra.css" before "style.css",
        // but we want alphabetical sorting on the base name (i.e. the file name without the extension)
        assertEquals("__style_extra.css", cssEntries[0].getName());
        assertEquals("style.css", cssEntries[1].getName());
        assertEquals("style-extra.css", cssEntries[2].getName());
    }

    @Test
    public void directories_are_returned_in_alphabetical_order() throws IOException {
        archive.open(true);
        final Collection<? extends Archive.Entry> bundleRootChildren = archive.getBundleRoot().getChildren();
        final Archive.Entry[] archiveChildren = bundleRootChildren.toArray(new Archive.Entry[0]);

        assertEquals("css", archiveChildren[0].getName());
        assertEquals("empty", archiveChildren[1].getName());
        assertEquals("ftl", archiveChildren[2].getName());
        assertEquals("js", archiveChildren[3].getName());
    }

    @Test
    public void files_and_folder_of_same_name_except_extension_do_not_collapse_to_one() throws IOException {
        archive.open(true);
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
        archive.open(true);
        traverseAndCheckOrder(archive.getBundleRoot());
    }

    @Test
    public void files_that_exceed_maxFileLengthBytes_are_skipped() throws IOException {
        archive = new WebFilesZipArchive(testBundleZip, fileNameMatcher, 1);
        archive.open(true);
        final Collection<? extends Archive.Entry> cssChildren = archive.getBundleRoot().getChild("css").getChildren();
        final Archive.Entry[] cssEntries = cssChildren.toArray(new Archive.Entry[0]);
        assertEquals("css files should be skipped because larger than 1 byte",0, cssEntries.length);
    }

    private void traverseAndCheckOrder(Archive.Entry current) throws IOException {

        final Collection<? extends Archive.Entry> bundleRootChildren = current.getChildren();
        if (bundleRootChildren == null) {
            return;
        }

        List<String> childNames = new ArrayList<>();
        for (Archive.Entry bundleRootChild : bundleRootChildren) {
            childNames.add(bundleRootChild.getName());
        }

        Collections.sort(childNames, BASE_NAME_COMPARATOR);

        final Archive.Entry[] archiveChildren = bundleRootChildren.toArray(new Archive.Entry[0]);
        for (int i = 0; i < archiveChildren.length; i++) {
            assertEquals(childNames.get(i), archiveChildren[i].getName());
        }

        for (Archive.Entry bundleRootChild : bundleRootChildren) {
            traverseAndCheckOrder(bundleRootChild);
        }

    }

    private boolean containsNames(final Collection<? extends Archive.Entry> archiveChildren, final String... names) {
        List<String> toFind = new ArrayList<>(Arrays.asList(names));
        for (Archive.Entry archiveChild : archiveChildren) {
            for (String name : names) {
                if (archiveChild.getName().equals(name)) {
                    toFind.remove(name);
                }
            }
        }
        return toFind.isEmpty();
    }

}