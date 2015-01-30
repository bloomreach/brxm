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
package org.onehippo.cms7.services.webfiles.watch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.cms7.services.webfiles.watch.WatchTestUtils.awaitQuietly;
import static org.onehippo.cms7.services.webfiles.watch.WatchTestUtils.forceTouch;

/**
 * Tests a sub directories watcher with a file system observer implementation.
 * Each file system observer is assumed to process file system changes in a single
 * background thread. The test ensures that file system modifications by the test never
 * interleave with this background thread by synchronizing via cyclic barriers for
 * two parties (one for the testing thread, and one for the background thread of the file
 * system observer). These barriers ensure the following thread interleaving:
 *
 * 1. [fs observer] start observing
 * 2. [test] modify file system
 * --> both threads await startChanges
 * 3. [fs observer] processes changes, which are recorded in the callback tracker of the test
 * --> both threads await stopChanges
 * 4. [test] verify recorded changes
 */
public abstract class AbstractSubDirectoriesWatcherWithFileSystemObserverIT extends AbstractWatcherIT {

    private static final long TIMEOUT_MS = 5000;
    private static final Logger log = LoggerFactory.getLogger(AbstractSubDirectoriesWatcherWithFileSystemObserverIT.class);

    protected GlobFileNameMatcher fileNameMatcher;
    private FileSystemObserver fileSystemObserver;
    private CallbackTracker callbackTracker;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        fileNameMatcher = new GlobFileNameMatcher();
        fileNameMatcher.includeFiles("*.css");
        fileNameMatcher.includeFiles("*.js");
        fileSystemObserver = createFileSystemObserver();
        callbackTracker = new CallbackTracker();
        new SubDirectoriesWatcher(webFilesDirectory.toPath(), fileSystemObserver, callbackTracker);
    }

    protected abstract FileSystemObserver createFileSystemObserver() throws Exception;

    @After
    public void tearDown() throws InterruptedException {
        fileSystemObserver.shutdown();
        super.tearDown();
    }

    @Test(timeout = TIMEOUT_MS)
    public void single_file_modification() throws IOException, InterruptedException {
        forceTouch(scriptJs);
        callbackTracker.awaitChanges();
        callbackTracker.assertCallbacks(1, scriptJs.toPath());
    }

    @Test(timeout = TIMEOUT_MS)
    public void two_files_modification() throws IOException, InterruptedException {
        forceTouch(styleCss);
        callbackTracker.awaitChanges();

        forceTouch(scriptJs);
        callbackTracker.awaitChanges();

        callbackTracker.assertCallbacks(2, styleCss.toPath(), scriptJs.toPath());
    }

    @Test(timeout = TIMEOUT_MS)
    public void single_file_multiple_modifications() throws IOException, InterruptedException {
        forceTouch(scriptJs);
        callbackTracker.awaitChanges();

        forceTouch(scriptJs);
        callbackTracker.awaitChanges();

        forceTouch(scriptJs);
        callbackTracker.awaitChanges();

        callbackTracker.assertCallbacks(3, scriptJs.toPath(), scriptJs.toPath(), scriptJs.toPath());
    }

    @Test(timeout = TIMEOUT_MS)
    public void rename_file_and_revert() throws IOException, InterruptedException {
        File fooJs = new File(jsDir, "foo.js");
        FileUtils.moveFile(scriptJs, fooJs);
        callbackTracker.awaitChanges();

        FileUtils.moveFile(fooJs, scriptJs);
        callbackTracker.awaitChanges();

        callbackTracker.assertCallbacks(2, jsDir.toPath(), jsDir.toPath());
    }

    @Test(timeout = TIMEOUT_MS)
    public void rename_file() throws IOException, InterruptedException {
        File fooJs = new File(jsDir, "foo.js");
        FileUtils.moveFile(scriptJs, fooJs);
        callbackTracker.awaitChanges();

        callbackTracker.assertCallbacks(1, jsDir.toPath());
    }

    @Test(timeout = TIMEOUT_MS)
    public void create_directory_rename_it_and_create_file_in_it() throws IOException, InterruptedException {
        final File newDir = new File(testBundleDir, "newDir");
        assertTrue(newDir.mkdir());
        callbackTracker.awaitChanges();

        final File newDirRenamed = new File(testBundleDir, "newDirRenamed");
        FileUtils.moveDirectory(newDir, newDirRenamed);
        callbackTracker.awaitChanges();

        final File newFileInRenamedDir = new File(newDirRenamed, "newFile.js");
        forceTouch(newFileInRenamedDir);
        callbackTracker.awaitChanges();

        callbackTracker.assertCallbacks(3, newDir.toPath(), testBundleDir.toPath(), newFileInRenamedDir.toPath());
    }

    @Test(timeout = TIMEOUT_MS)
    public void move_to_other_dir_results_in_1_listener_event_for_two_paths() throws IOException, InterruptedException {
        final File cssDir = styleCss.getParentFile();
        final File jsDir = scriptJs.getParentFile();

        File fooJsInCssDir = new File(cssDir, "foo.js");
        FileUtils.moveFile(scriptJs, fooJsInCssDir);
        callbackTracker.awaitChanges();

        callbackTracker.assertCallbacks(1, fooJsInCssDir.toPath(), jsDir.toPath());
    }

    @Test(timeout = TIMEOUT_MS)
    public void delete_file() throws IOException, InterruptedException {
        assertTrue(scriptJs.delete());
        callbackTracker.awaitChanges();

        callbackTracker.assertCallbacks(1, jsDir.toPath());
    }

    @Test(timeout = TIMEOUT_MS)
    public void delete_empty_dir() throws IOException, InterruptedException {
        FileUtils.deleteDirectory(emptyDir);
        callbackTracker.awaitChanges();

        callbackTracker.assertCallbacks(1, testBundleDir.toPath());
    }

    @Test(timeout = TIMEOUT_MS)
    public void delete_dir_with_file() throws IOException, InterruptedException {
        FileUtils.deleteDirectory(jsDir);
        callbackTracker.awaitChanges();

        callbackTracker.assertCallbacks(1, testBundleDir.toPath());
    }

    @Test(timeout = TIMEOUT_MS)
    public void create_dir() throws IOException, InterruptedException {
        File newDir = new File(testBundleDir, "newDir");
        assertTrue(newDir.mkdir());
        callbackTracker.awaitChanges();

        callbackTracker.assertCallbacks(1, newDir.toPath());
    }

    @Test(timeout = TIMEOUT_MS)
    public void create_dir_then_delete_it() throws IOException, InterruptedException {
        File newDir = new File(testBundleDir, "newDir");
        assertTrue(newDir.mkdir());
        callbackTracker.awaitChanges();

        assertTrue(newDir.delete());
        callbackTracker.awaitChanges();

        callbackTracker.assertCallbacks(2, newDir.toPath(), testBundleDir.toPath());
    }

    @Test(timeout = TIMEOUT_MS)
    public void create_dir_then_file() throws IOException, InterruptedException {
        File newDir = new File(testBundleDir, "newDir");
        FileUtils.forceMkdir(newDir);
        callbackTracker.awaitChanges();

        File newFile = new File(newDir, "newFile.css");
        forceTouch(newFile);
        callbackTracker.awaitChanges();

        callbackTracker.assertCallbacks(2, newDir.toPath(), newFile.toPath());
    }

    @Test(timeout = TIMEOUT_MS)
    public void create_dir_and_file_in_one_go() throws IOException, InterruptedException {
        File newDir = new File(testBundleDir, "newDir");
        File fooCss = new File(newDir, "foo.css");
        assertTrue(newDir.mkdirs());
        assertTrue(fooCss.createNewFile());
        callbackTracker.awaitChanges();

        callbackTracker.assertCallbacks(1, newDir.toPath());
    }

    @Test(timeout = TIMEOUT_MS)
    public void create_dir_and_file_and_delete_again() throws IOException, InterruptedException {
        File newDir = new File(testBundleDir, "newDir");
        File fooCss = new File(newDir, "foo.css");
        assertTrue(newDir.mkdirs());
        assertTrue(fooCss.createNewFile());
        callbackTracker.awaitChanges();

        FileUtils.deleteDirectory(newDir);
        callbackTracker.awaitChanges();

        callbackTracker.assertCallbacks(2, newDir.toPath(), newDir.getParentFile().toPath());
    }

    @Test(timeout = TIMEOUT_MS)
    public void delete_all_sub_dirs() throws IOException, InterruptedException {
        final File[] testBundleFiles = testBundleDir.listFiles();
        assertNotNull(testBundleFiles);

        for (File file : testBundleFiles) {
            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            }
        }
        callbackTracker.awaitChanges();

        callbackTracker.assertCallbacks(1, testBundleDir.toPath());
    }

    @Test(timeout = TIMEOUT_MS)
    public void delete_all_sub_dirs_with_pauses_result_in_separate_callbacks() throws IOException, InterruptedException {
        List<Path> expectedChangedPaths = new ArrayList<>();

        final File[] testBundleFiles = testBundleDir.listFiles();
        assertNotNull(testBundleFiles);

        for (File file : testBundleFiles) {
            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
                callbackTracker.awaitChanges();
                expectedChangedPaths.add(testBundleDir.toPath());
            }
        }

        // expect the same number of callbacks as subdirectories in the testbundle since after every delete, we wait for file system changes
        callbackTracker.assertCallbacks(expectedChangedPaths.size(), expectedChangedPaths.toArray(new Path[expectedChangedPaths.size()]));
    }

    @Test(timeout = TIMEOUT_MS)
    public void delete_all_sub_dirs_and_create_new_ones() throws IOException, InterruptedException {
        final File[] testBundleFiles = testBundleDir.listFiles();
        assertNotNull(testBundleFiles);

        for (File file : testBundleFiles) {
            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            }
        }
        callbackTracker.awaitChanges();

        callbackTracker.assertCallbacks(1, testBundleDir.toPath());

        File newDir1 = new File(testBundleDir, "newDir1");
        File newDir2 = new File(testBundleDir, "newDir2");

        assertTrue(newDir1.mkdir());
        callbackTracker.awaitChanges();

        assertTrue(newDir2.mkdir());
        callbackTracker.awaitChanges();

        callbackTracker.assertCallbacks(3, testBundleDir.toPath(), newDir1.toPath(), newDir2.toPath());
    }

    @Test(timeout = TIMEOUT_MS)
    public void intellij_safe_write_generates_one_event_for_modified_file() throws IOException, InterruptedException {
        // mimic IntelliJ's safe write sequence: save to a temp file, delete the original and rename the temp file
        File bak = new File(styleCss.getPath() + "__jb_bak__");
        assertTrue(bak.createNewFile());
        FileUtils.write(bak, "new contents");
        File old = new File(styleCss.getPath() + "__jb_old__");
        FileUtils.moveFile(styleCss, old);
        FileUtils.moveFile(bak, styleCss);
        assertTrue(old.delete());
        callbackTracker.awaitChanges();

        callbackTracker.assertCallbacks(1, styleCss.toPath());
    }

    @Test
    public void excluded_file_is_ignored() throws IOException, InterruptedException {
        File tmpFile = new File(cssDir, "pdf-files-are-not-included.pdf");
        assertTrue(tmpFile.createNewFile());
        callbackTracker.awaitChanges();
        callbackTracker.assertCallbacks(0);
    }

    @Test
    public void excluded_dir_is_ignored() throws IOException, InterruptedException {
        fileNameMatcher.excludeDirectories(".git");

        File excludedDir = new File(testBundleDir, ".git");
        FileUtils.forceMkdir(excludedDir);

        callbackTracker.awaitChanges();
        callbackTracker.assertCallbacks(0);
    }

    @Test
    public void included_file_in_excluded_dir_is_ignored() throws IOException, InterruptedException {
        fileNameMatcher.excludeDirectories(".svn");

        File svnDir = new File(testBundleDir, ".svn");
        FileUtils.forceMkdir(svnDir);

        File file = new File(svnDir, "file.css");
        forceTouch(file);

        callbackTracker.awaitChanges();
        callbackTracker.assertCallbacks(0);
    }

    private static class CallbackTracker implements SubDirectoriesWatcher.PathChangesListener {

        private CyclicBarrier startChanges = new CyclicBarrier(2);
        private CyclicBarrier stopChanges = new CyclicBarrier(2);
        private int callbackCount = 0;
        private List<Path> changedPaths = Collections.synchronizedList(new ArrayList<>());

        void awaitChanges() {
            awaitQuietly(startChanges);
            awaitQuietly(stopChanges);
        }

        @Override
        public void onStart() {
            awaitQuietly(startChanges, TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }

        @Override
        public synchronized void onPathsChanged(final Path watchedRootDir, final Set<Path> changedPaths) {
            this.changedPaths.addAll(changedPaths);
            callbackCount++;
            notifyAll();
        }

        @Override
        public void onStop() {
            awaitQuietly(stopChanges, TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }

        public synchronized void assertCallbacks(final int expectedCount, final Path... expectedChangedPaths) throws InterruptedException {
            while (callbackCount < expectedCount) {
                log.debug("Waiting for {} callbacks, observed only {}...", expectedCount, callbackCount);
                wait();
            }

            assertChangedPaths(expectedChangedPaths);
            assertEquals("number of callbacks", expectedCount, callbackCount);
        }

        private void assertChangedPaths(Path... expectedPaths) {
            if (changedPaths.size() != expectedPaths.length) {
                fail("Incorrect number of changed paths.\n"
                        + "Expected " + expectedPaths.length + ": " + Arrays.toString(expectedPaths) + " \n"
                        + "But got " + changedPaths.size() + ": " + changedPaths);
            } else {
                assertArrayEquals("Unexpected changed paths", expectedPaths, changedPaths.toArray());
            }
        }

    }
}
