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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.cms7.services.webfiles.watch.WatchTestUtils.awaitQuietly;
import static org.onehippo.cms7.services.webfiles.watch.WatchTestUtils.forceTouch;

/**
 * Tests a file system observer implementation. Each file system observer is assumed
 * to process file system changes in a single background thread. The test ensures that
 * file system modifications by the test never interleave with this background thread
 * by synchronizing via cyclic barriers for two parties (one for the testing thread,
 * and one for the background thread of the file system observer). These barriers ensure
 * the following thread interleaving:
 *
 * 1. [fs observer] start observing
 * 2. [test] modify file system
 * --> both threads await startRecording
 * 3. [fs observer] process changes, which are recorded in the changes listener of the test
 * --> both threads await stopRecording
 * 4. [test] verify recorded changes
 */
public abstract class AbstractFileSystemObserverIT extends AbstractWatcherIT {

    private static final long TIMEOUT_MS = 5000;
    
    protected GlobFileNameMatcher fileNameMatcher;
    private ChangesListener changesListener;
    private FileSystemObserver fileSystemObserver;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        fileNameMatcher = new GlobFileNameMatcher();
        fileNameMatcher.includeFiles("*.css");
        fileNameMatcher.includeFiles("*.js");
        changesListener = new ChangesListener();
        fileSystemObserver = createFileSystemObserver();
    }

    protected abstract FileSystemObserver createFileSystemObserver() throws Exception;

    private void observeTestBundle() throws IOException {
        fileSystemObserver.registerDirectory(testBundleDir.toPath(), changesListener);
    }

    @After
    public void tearDown() throws InterruptedException {
        fileSystemObserver.shutdown();
        super.tearDown();
    }

    @Test(timeout = TIMEOUT_MS)
    public void create_file() throws IOException, InterruptedException {
        observeTestBundle();
        final File newFile = new File(jsDir, "new.js");
        forceTouch(newFile);
        changesListener.awaitChanges();
        changesListener.assertCreated(newFile);
    }

    @Test(timeout = TIMEOUT_MS)
    public void modify_file() throws IOException, InterruptedException {
        observeTestBundle();
        forceTouch(scriptJs);
        changesListener.awaitChanges();
        changesListener.assertModified(scriptJs);
    }

    @Test(timeout = TIMEOUT_MS)
    public void modify_two_files() throws IOException, InterruptedException {
        observeTestBundle();
        forceTouch(styleCss);
        forceTouch(scriptJs);
        changesListener.awaitChanges();
        changesListener.assertModified(styleCss, scriptJs);
    }

    @Test(timeout = TIMEOUT_MS)
    public void modify_file_multiple_times() throws IOException, InterruptedException {
        observeTestBundle();

        forceTouch(scriptJs);
        changesListener.awaitChanges();
        changesListener.assertModified(scriptJs);

        forceTouch(scriptJs);
        changesListener.awaitChanges();
        changesListener.assertModified(scriptJs);

        forceTouch(scriptJs);
        forceTouch(scriptJs);
        changesListener.awaitChanges();
        changesListener.assertModified(scriptJs);
    }

    @Test(timeout = TIMEOUT_MS)
    public void delete_file() throws IOException, InterruptedException {
        observeTestBundle();
        assertTrue(scriptJs.delete());
        changesListener.awaitChanges();
        changesListener.assertDeleted(scriptJs);
    }

    @Test(timeout = TIMEOUT_MS)
    public void rename_file() throws IOException, InterruptedException {
        observeTestBundle();

        File fooJs = new File(jsDir, "foo.js");
        FileUtils.moveFile(scriptJs, fooJs);
        changesListener.awaitChanges();

        changesListener.assertDeleted(scriptJs);
        changesListener.assertCreated(fooJs);
    }

    @Test(timeout = TIMEOUT_MS)
    public void rename_file_and_revert() throws IOException, InterruptedException {
        observeTestBundle();

        File fooJs = new File(jsDir, "foo.js");
        FileUtils.moveFile(scriptJs, fooJs);
        changesListener.awaitChanges();

        FileUtils.moveFile(fooJs, scriptJs);
        changesListener.awaitChanges();

        changesListener.assertDeleted(fooJs);
        changesListener.assertCreated(scriptJs);
    }

    @Test(timeout = TIMEOUT_MS)
    public void create_directory_rename_it_and_create_file_in_it() throws IOException, InterruptedException {
        observeTestBundle();

        final File newDir = new File(testBundleDir, "newDir");
        assertTrue(newDir.mkdir());
        changesListener.awaitChanges();
        changesListener.assertCreated(newDir);

        final File newDirRenamed = new File(testBundleDir, "newDirRenamed");
        FileUtils.moveDirectory(newDir, newDirRenamed);
        changesListener.awaitChanges();
        changesListener.assertDeleted(newDir);
        changesListener.assertCreated(newDirRenamed);

        final File newFileinRenamedDir = new File(newDirRenamed, "newFile.js");
        forceTouch(newFileinRenamedDir);
        changesListener.awaitChanges();
        changesListener.assertCreated(newFileinRenamedDir);
    }

    @Test(timeout = TIMEOUT_MS)
    public void move_file_to_other_dir() throws IOException, InterruptedException {
        observeTestBundle();

        File fooJsInCssDir = new File(cssDir, "foo.js");
        FileUtils.moveFile(scriptJs, fooJsInCssDir);
        changesListener.awaitChanges();

        changesListener.assertDeleted(scriptJs);
        changesListener.assertCreated(fooJsInCssDir);
    }

    @Test(timeout = TIMEOUT_MS)
    public void create_dir_then_delete_it() throws IOException, InterruptedException {
        observeTestBundle();

        File newDir = new File(testBundleDir, "newDir");
        FileUtils.forceMkdir(newDir);
        changesListener.awaitChanges();
        changesListener.assertCreated(newDir);

        FileUtils.forceDelete(newDir);
        changesListener.awaitChanges();
        changesListener.assertDeleted(newDir);
    }

    @Test(timeout = TIMEOUT_MS)
    public void create_dir_then_file() throws IOException, InterruptedException {
        observeTestBundle();

        File newDir = new File(testBundleDir, "newDir");
        FileUtils.forceMkdir(newDir);
        changesListener.awaitChanges();
        changesListener.assertCreated(newDir);

        File newFile = new File(newDir, "newFile.css");
        forceTouch(newFile);
        changesListener.awaitChanges();
        changesListener.assertCreated(newFile);
    }

    @Test(timeout = TIMEOUT_MS)
    public void create_dir_and_file_and_delete_again() throws IOException, InterruptedException {
        observeTestBundle();

        File newDir = new File(testBundleDir, "newDir");
        assertTrue(newDir.mkdir());
        File fooCss = new File(newDir, "foo.css");
        assertTrue(fooCss.createNewFile());
        changesListener.awaitChanges();

        FileUtils.deleteDirectory(newDir);
        changesListener.awaitChanges();

        changesListener.assertDeleted(newDir);
    }

    @Test(timeout = TIMEOUT_MS)
    public void delete_empty_dir() throws IOException, InterruptedException {
        observeTestBundle();
        FileUtils.deleteDirectory(emptyDir);
        changesListener.awaitChanges();
        changesListener.assertDeleted(emptyDir);
    }

    @Test(timeout = TIMEOUT_MS)
    public void delete_dir_with_file() throws IOException, InterruptedException {
        observeTestBundle();
        FileUtils.deleteDirectory(jsDir);
        changesListener.awaitChanges();
        changesListener.assertDeleted(jsDir);
    }

    @Test(timeout = TIMEOUT_MS)
    public void delete_all_sub_dirs() throws IOException, InterruptedException {
        observeTestBundle();

        final List<File> deletedDirs = new ArrayList<>();

        final File[] bundleFiles = testBundleDir.listFiles();
        assertNotNull(bundleFiles);

        for (File file : bundleFiles) {
            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
                deletedDirs.add(file);
            }
        }
        changesListener.awaitChanges();

        for (File dir : deletedDirs) {
            changesListener.assertDeleted(dir);
        }
    }

    @Test(timeout = TIMEOUT_MS)
    public void delete_all_sub_dirs_and_create_new_ones() throws IOException, InterruptedException {
        observeTestBundle();

        final File[] bundleFiles = testBundleDir.listFiles();
        assertNotNull(bundleFiles);

        for (File file : bundleFiles) {
            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            }
        }
        changesListener.awaitChanges();

        File newDir1 = new File(testBundleDir, "newDir1");
        assertTrue(newDir1.mkdir());
        changesListener.awaitChanges();
        changesListener.assertCreated(newDir1);

        File newDir2 = new File(testBundleDir, "newDir2");
        assertTrue(newDir2.mkdir());
        changesListener.awaitChanges();
        changesListener.assertCreated(newDir2);
    }

    @Test(timeout = TIMEOUT_MS)
    public void excluded_file_is_ignored() throws IOException, InterruptedException {
        observeTestBundle();
        File tmpFile = new File(cssDir, "pdf-files-are-not-included.pdf");
        assertTrue(tmpFile.createNewFile());
        changesListener.awaitChanges();
        changesListener.assertNoChangesFor(tmpFile);
    }

    @Test(timeout = TIMEOUT_MS)
    public void excluded_dir_is_ignored() throws IOException, InterruptedException {
        fileNameMatcher.excludeDirectories(".git");

        observeTestBundle();

        File excludedDir = new File(testBundleDir, ".git");
        FileUtils.forceMkdir(excludedDir);
        changesListener.awaitChanges();
        changesListener.assertNoChangesFor(excludedDir);
    }

    @Test(timeout = TIMEOUT_MS)
    public void included_file_in_excluded_dir_is_ignored() throws IOException, InterruptedException {
        fileNameMatcher.excludeDirectories(".svn");

        observeTestBundle();

        File svnDir = new File(testBundleDir, ".svn");
        FileUtils.forceMkdir(svnDir);

        File file = new File(svnDir, "test.css");
        forceTouch(file);

        changesListener.awaitChanges();
        changesListener.assertNoChangesFor(svnDir, file);
    }

    @Test(timeout = TIMEOUT_MS)
    public void changes_in_separate_registered_directories_are_processed_by_separate_listeners() throws IOException, InterruptedException {
        File secondBundleDir = new File(webFilesDirectory, "secondbundle");
        assertTrue(secondBundleDir.mkdir());

        ChangesListener secondBundleListener = new ChangesListener();

        // first: only keep the start barrier of the first and the stop barrier of the second listener,
        // so both can be updated simultaneously
        changesListener.removeStopBarrier();
        secondBundleListener.removeStartBarrier();

        // second: observe both bundles
        observeTestBundle();
        fileSystemObserver.registerDirectory(secondBundleDir.toPath(), secondBundleListener);

        // modify test bundle
        forceTouch(scriptJs);

        // modify second bundle
        File fileInSecondBundle = new File(secondBundleDir, "file.js");
        forceTouch(fileInSecondBundle);

        // wait until both listeners recorded changes
        changesListener.awaitStartRecordingChanges();
        secondBundleListener.awaitStopRecordingChanges();

        changesListener.assertModified(scriptJs);
        changesListener.assertNoChangesFor(fileInSecondBundle);

        secondBundleListener.assertNoChangesFor(scriptJs);
        secondBundleListener.assertCreated(fileInSecondBundle);
    }

    @Test
    public void shutdown_ends_observation() throws InterruptedException, IOException {
        observeTestBundle();

        fileSystemObserver.shutdown();

        forceTouch(scriptJs);
        Thread.sleep(1000);
        changesListener.assertNoChanges();
    }

    private static class ChangesListener implements FileSystemListener {

        private CyclicBarrier startRecording = new CyclicBarrier(2);
        private CyclicBarrier stopRecording = new CyclicBarrier(2);
        private boolean recordingChanges = false;
        private List<String> recordedErrors = Collections.synchronizedList(new ArrayList<>());
        private final List<Path> created = Collections.synchronizedList(new ArrayList<>());
        private final List<Path> modified = Collections.synchronizedList(new ArrayList<>());
        private final List<Path> deleted = Collections.synchronizedList(new ArrayList<>());

        void awaitChanges() {
            awaitStartRecordingChanges();
            awaitStopRecordingChanges();
        }

        void awaitStartRecordingChanges() {
            awaitQuietly(startRecording);
        }

        void awaitStopRecordingChanges() {
            awaitQuietly(stopRecording);
        }

        synchronized void removeStartBarrier() {
            startRecording = null;
        }

        synchronized void removeStopBarrier() {
            stopRecording = null;
        }

        private void recordAssert(boolean ok, String errorMessage) {
            if (!ok) {
                recordedErrors.add(errorMessage);
            }
        }

        @Override
        public synchronized void fileSystemChangesStarted() {
            awaitQuietly(startRecording, TIMEOUT_MS, TimeUnit.MILLISECONDS);
            recordAssert(!recordingChanges, "Changes should not be recorded yet");
            recordingChanges = true;
        }

        @Override
        public synchronized void directoryCreated(final Path directory) {
            recordAssert(Files.isDirectory(directory), "Not a directory: " + directory);
            created.add(directory);
        }

        @Override
        public synchronized void directoryModified(final Path directory) {
            recordAssert(Files.isDirectory(directory), "Not a directory: " + directory);
            modified.add(directory);
        }

        @Override
        public synchronized void directoryDeleted(final Path directory) {
            recordAssert(!Files.exists(directory), "Directory still exists: " + directory);
            deleted.add(directory);
        }

        @Override
        public synchronized void fileCreated(final Path file) {
            recordAssert(Files.isRegularFile(file), "Not a file: " + file);
            created.add(file);
        }

        @Override
        public synchronized void fileModified(final Path file) {
            modified.add(file);
        }

        @Override
        public synchronized void fileDeleted(final Path file) {
            recordAssert(!Files.exists(file), "File still exists: " + file);
            deleted.add(file);
        }

        @Override
        public synchronized void fileSystemChangesStopped() {
            recordAssert(recordingChanges, "Changes should have been recorded");
            recordingChanges = false;
            awaitQuietly(stopRecording, TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }

        private void checkRecordedErrors() {
            if (!recordedErrors.isEmpty()) {
                fail("Recorded errors: " + recordedErrors);
            }
        }

        synchronized void assertCreated(File... expectedFiles) {
            assertContains("creation", created, expectedFiles);
        }

        synchronized void assertModified(File... expectedFiles) {
            assertContains("modification", modified, expectedFiles);
        }

        synchronized void assertDeleted(File... expectedFiles) {
            assertContains("deletion", deleted, expectedFiles);
        }

        private void assertContains(final String action, final List<Path> actualPaths, final File[] expectedFiles) {
            checkRecordedErrors();
            for (File expectedFile : expectedFiles) {
                assertTrue("expected " + action + " of " + expectedFile, actualPaths.contains(expectedFile.toPath()));
            }
        }

        synchronized void assertNoChanges() {
            checkRecordedErrors();
            assertEquals(0, created.size());
            assertEquals(0, modified.size());
            assertEquals(0, deleted.size());
        }

        synchronized void assertNoChangesFor(final File... files) {
            checkRecordedErrors();
            assertContainsNot("creation", created, files);
            assertContainsNot("modification", modified, files);
            assertContainsNot("deletion", deleted, files);
        }

        private void assertContainsNot(final String action, final List<Path> actualPaths, final File... files) {
            for (File file : files) {
                assertFalse("expected no " + action + " of " + file, actualPaths.contains(file.toPath()));
            }
        }

    }

}
