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
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onehippo.cms7.services.autoreload.AutoReloadService;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.cms7.services.webfiles.LogRecorder;
import org.onehippo.cms7.services.webfiles.WebFileEvent;
import org.onehippo.cms7.services.webfiles.WebFileException;
import org.onehippo.cms7.services.webfiles.WebFilesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms7.services.webfiles.watch.WatchTestUtils.awaitQuietly;

/**
 * Tests {@link org.onehippo.cms7.services.webfiles.watch.WebFilesWatcher}, which is assumed
 * to watch file system changes in a single background thread. The test ensures that
 * file system modifications by the test never interleave with this background thread
 * by synchronizing via cyclic barriers for two parties (one for the testing thread,
 * and one for the background thread of the file system observer). These barriers ensure
 * the following thread interleaving:
 *
 * 1. [watcher] start watching
 * 2. [test] modify file system
 * --> both threads await startFileSystemChanges
 * 3. [watcher] process changes, which may trigger imports in the web files service
 * --> both threads await stopRecording
 * 4. [test] verify imports in the web file service
 */
public class WebFilesWatcherIT extends AbstractWatcherIT {

    private static final long WATCH_DELAY_MS = 500;
    private static final long TIMEOUT_MS = 5000;
    private static final Logger log = LoggerFactory.getLogger(WebFilesWatcherIT.class);

    private static LogRecorder logRecorder;

    private WebFilesService webFilesService;
    private Session session;
    private WebFilesWatcher watcher;
    private HippoEventBus eventBus;
    private AutoReloadService autoReload;
    private CyclicBarrier startFileSystemChanges;
    private CyclicBarrier stopFileSystemChanges;

    @BeforeClass
    public static void beforeClass() throws Exception {
        logRecorder = new LogRecorder(WebFilesWatcher.log);
        WebFilesWatcher.log = logRecorder.getLogger();
    }

    @Before
    public void setup() throws Exception {
        super.setUp();
        webFilesService = createMock(WebFilesService.class);
        session = createNiceMock(Session.class);
        eventBus = createNiceMock(HippoEventBus.class);
        autoReload = createMock(AutoReloadService.class);
        startFileSystemChanges = new CyclicBarrier(2);
        stopFileSystemChanges = new CyclicBarrier(2);
    }

    private void createWatcher(final String includedFiles, final String... excludedDirectories) {
        final WebFilesWatcherJavaConfig config = new WebFilesWatcherJavaConfig();
        config.addWatchedModule(watchedModule);
        config.includeFiles(includedFiles);
        config.excludeDirs(excludedDirectories);
        config.useWatchServiceOnOsNames(WebFilesWatcherConfig.DEFAULT_USE_WATCH_SERVICE_ON_OS_NAMES);
        config.setWatchDelayMillis(WATCH_DELAY_MS);

        watcher = new WebFilesWatcher(config, webFilesService, session, eventBus, autoReload) {
            @Override
            public void onStart() {
                awaitQuietly(startFileSystemChanges, TIMEOUT_MS, TimeUnit.MILLISECONDS);
            }

            @Override
            public void onStop() {
                awaitQuietly(stopFileSystemChanges, TIMEOUT_MS, TimeUnit.MILLISECONDS);
            }
        };
    }

    private void replayWebFilesService() {
        replayIfNotNull(webFilesService, session, eventBus, autoReload);
    }

    private void replayIfNotNull(final Object... mocks) {
        for (Object mock : mocks) {
            if (mock != null) {
                replay(mock);
            }
        }
    }

    private void verifyWebFilesService() {
        verifyIfNotNull(webFilesService, session, eventBus, autoReload);
    }

    private void verifyIfNotNull(final Object... mocks) {
        for (Object mock : mocks) {
            if (mock != null) {
                verify(mock);
            }
        }
    }

    @After
    public void tearDown() throws InterruptedException {
        watcher.shutdown();
        logRecorder.clear();
        super.tearDown();
    }

    @Test
    public void onPathsChanged_invoked_directly() throws IOException {
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), anyObject(File.class), anyBoolean());
        expectLastCall();
        webFilesService.importJcrWebFiles(anyObject(Session.class), eq("testbundle"), eq("css/style.css"), eq(styleCss));
        expectLastCall();

        final Capture<WebFileEvent> event = new Capture<>();
        eventBus.post(capture(event));
        expectLastCall();

        autoReload.broadcastPageReload();
        expectLastCall();

        replayWebFilesService();

        createWatcher("*");
        watcher.onPathsChanged(webFilesDirectory.toPath(), Collections.singleton(styleCss.toPath()));

        verifyWebFilesService();
        logRecorder.assertNoWarningsOrErrorsLogged();

        assertThat(event.getValue().getChangedPath(), is(styleCss.toPath()));
    }

    @Test
    public void onPathsChanged_invoked_directly_without_eventBus() throws IOException {
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), anyObject(File.class), anyBoolean());
        expectLastCall();
        webFilesService.importJcrWebFiles(anyObject(Session.class), eq("testbundle"), eq("css/style.css"), eq(styleCss));
        expectLastCall();

        autoReload.broadcastPageReload();
        expectLastCall();

        replayWebFilesService();

        eventBus = null;
        createWatcher("*");

        watcher.onPathsChanged(webFilesDirectory.toPath(), Collections.singleton(styleCss.toPath()));

        verifyWebFilesService();
        logRecorder.assertNoWarningsOrErrorsLogged();
    }

    @Test
    public void onPathsChanged_invoked_directly_without_autoReload_service() throws IOException {
        webFilesService.importJcrWebFiles(anyObject(Session.class), eq("testbundle"), eq("css/style.css"), eq(styleCss));
        expectLastCall();

        final Capture<WebFileEvent> event = new Capture<>();
        eventBus.post(capture(event));
        expectLastCall();

        replayWebFilesService();

        autoReload = null;
        createWatcher("*");

        watcher.onPathsChanged(webFilesDirectory.toPath(), Collections.singleton(styleCss.toPath()));

        verifyWebFilesService();
        logRecorder.assertNoWarningsOrErrorsLogged();

        assertThat(event.getValue().getChangedPath(), is(styleCss.toPath()));
    }

    @Test(timeout = TIMEOUT_MS)
    public void creating_file_imports_file() throws IOException, InterruptedException {
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), anyObject(File.class), anyBoolean());
        expectLastCall();
        final File cssDir = styleCss.getParentFile();
        final File newCss = new File(cssDir, "new.css");

        webFilesService.importJcrWebFiles(anyObject(Session.class), eq("testbundle"), eq("css/new.css"), eq(newCss));
        expectLastCall();
        autoReload.broadcastPageReload();
        expectLastCall();

        replayWebFilesService();

        createWatcher("*");
        assertTrue(newCss.createNewFile());
        waitForFileSystemChanges();

        verifyWebFilesService();
        logRecorder.assertNoWarningsOrErrorsLogged();
    }

    @Test(timeout = TIMEOUT_MS)
    public void creating_directory_imports_directory() throws IOException, InterruptedException {
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), anyObject(File.class), anyBoolean());
        expectLastCall();
        final File newDir = new File(testBundleDir, "newDir");

        webFilesService.importJcrWebFiles(anyObject(Session.class), eq("testbundle"), eq("newDir"), eq(newDir));
        expectLastCall();
        autoReload.broadcastPageReload();
        expectLastCall();

        replayWebFilesService();

        createWatcher("*");
        FileUtils.forceMkdir(newDir);
        waitForFileSystemChanges();

        verifyWebFilesService();
        logRecorder.assertNoWarningsOrErrorsLogged();
    }

    @Test
    public void creating_excluded_directory_imports_nothing() throws IOException, InterruptedException {
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), anyObject(File.class), anyBoolean());
        expectLastCall();
        final File newDir = new File(testBundleDir, ".svn");

        replayWebFilesService();

        createWatcher("*", ".svn");
        FileUtils.forceMkdir(newDir);
        waitForFileSystemChanges();

        verifyWebFilesService();
        logRecorder.assertNoWarningsOrErrorsLogged();
    }

    @Test(timeout = TIMEOUT_MS)
    public void touching_file_imports_file() throws IOException, InterruptedException {
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), anyObject(File.class), anyBoolean());
        expectLastCall();
        webFilesService.importJcrWebFiles(anyObject(Session.class), eq("testbundle"), eq("css/style.css"), eq(styleCss));
        expectLastCall();
        autoReload.broadcastPageReload();
        expectLastCall();

        replayWebFilesService();

        createWatcher("*");
        FileUtils.touch(styleCss);
        waitForFileSystemChanges();

        verifyWebFilesService();
        logRecorder.assertNoWarningsOrErrorsLogged();
    }

    @Test
    public void creating_excluded_file_imports_nothing() throws IOException, InterruptedException {
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), anyObject(File.class), anyBoolean());
        expectLastCall();
        final File stylePdf = new File(cssDir, "style.pdf");
        replayWebFilesService();

        // do not watch .pdf files, only .css files
        createWatcher("*.css");

        FileUtils.touch(stylePdf);
        waitForFileSystemChanges();

        verifyWebFilesService();
        logRecorder.assertNoWarningsOrErrorsLogged();
    }

    @Test(timeout = TIMEOUT_MS)
    public void removing_file_imports_parent_directory() throws IOException, InterruptedException {
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), anyObject(File.class), anyBoolean());
        expectLastCall();
        final File cssDir = styleCss.getParentFile();

        webFilesService.importJcrWebFiles(anyObject(Session.class), eq("testbundle"), eq(cssDir.getName()), eq(cssDir));
        expectLastCall();
        autoReload.broadcastPageReload();
        expectLastCall();

        replayWebFilesService();

        createWatcher("*");
        FileUtils.forceDelete(styleCss);
        waitForFileSystemChanges();

        verifyWebFilesService();
        logRecorder.assertNoWarningsOrErrorsLogged();
    }

    @Test(timeout = TIMEOUT_MS)
    public void renaming_file_imports_parent_directory() throws IOException, InterruptedException {
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), anyObject(File.class), anyBoolean());
        expectLastCall();
        final File cssDir = styleCss.getParentFile();

        webFilesService.importJcrWebFiles(anyObject(Session.class), eq("testbundle"), eq(cssDir.getName()), eq(cssDir));
        expectLastCall();
        autoReload.broadcastPageReload();
        expectLastCall();

        replayWebFilesService();

        createWatcher("*");
        final File newCss = new File(cssDir, "new.css");
        FileUtils.moveFile(styleCss, newCss);
        waitForFileSystemChanges();

        verifyWebFilesService();
        logRecorder.assertNoWarningsOrErrorsLogged();
    }

    @Test(timeout = TIMEOUT_MS)
    public void removing_subdirectory_imports_parent() throws IOException, InterruptedException {
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), anyObject(File.class), anyBoolean());
        expectLastCall();
        final File cssDir = styleCss.getParentFile();
        final File cssSubDir = new File(cssDir, "cssSubDir");
        assertTrue(cssSubDir.mkdir());

        webFilesService.importJcrWebFiles(anyObject(Session.class), eq("testbundle"), eq(cssDir.getName()), eq(cssDir));
        expectLastCall();
        autoReload.broadcastPageReload();
        expectLastCall();

        replayWebFilesService();

        createWatcher("*");
        FileUtils.deleteDirectory(cssSubDir);
        waitForFileSystemChanges();

        verifyWebFilesService();
        logRecorder.assertNoWarningsOrErrorsLogged();
    }

    @Test(timeout = TIMEOUT_MS)
    public void removing_root_directory_imports_everything() throws IOException, InterruptedException {
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), anyObject(File.class), anyBoolean());
        expectLastCall();
        webFilesService.importJcrWebFiles(anyObject(Session.class), eq("testbundle"), eq(""), eq(testBundleDir));
        expectLastCall();
        autoReload.broadcastPageReload();
        expectLastCall();

        replayWebFilesService();

        createWatcher("*");
        FileUtils.deleteDirectory(cssDir);
        waitForFileSystemChanges();

        verifyWebFilesService();
        logRecorder.assertNoWarningsOrErrorsLogged();
    }

    private void waitForFileSystemChanges() throws InterruptedException {
        log.debug("Wait until file system changes start...");
        awaitQuietly(startFileSystemChanges);
        log.debug("Wait until file system changes stop...");
        awaitQuietly(stopFileSystemChanges);
        log.debug("Waiting done");
    }

    private void waitForNoFileSystemChanges() throws InterruptedException {
        Thread.sleep(2000);
    }

    @Test
    public void failing_import_logs_warning_resets_session_and_reimport_whole_bundle() throws IOException, InterruptedException, RepositoryException {
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), anyObject(File.class), anyBoolean());
        expectLastCall();
        webFilesService.importJcrWebFiles(anyObject(Session.class), eq("testbundle"), eq("css"), eq(cssDir));
        expectLastCall().andThrow(new WebFileException("Simulating failed import"));
        session.refresh(eq(false));
        expectLastCall();
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), eq(testBundleDir), anyBoolean());
        expectLastCall();
        autoReload.broadcastPageReload();
        expectLastCall();
        replayWebFilesService();

        createWatcher("*");
        final Set<Path> changedPaths = Collections.singleton(cssDir.toPath());
        watcher.onPathsChanged(webFilesDirectory.toPath(), changedPaths);

        verifyWebFilesService();

        logRecorder.assertNoWarningsOrErrorsLogged();
    }

    @Test
    public void windows_paths_are_converted_to_correct_jcr_paths() throws IOException, InterruptedException, RepositoryException {
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), anyObject(File.class), anyBoolean());
        expectLastCall();
        final Path windowsBundleRoot = new MockPath("C:\\Users\\John\\myhippoproject\\repository-data\\webfiles\\src\\main\\resources", '\\');
        final Path windowsStyleCss = new MockPath("C:\\Users\\John\\myhippoproject\\repository-data\\webfiles\\src\\main\\resources\\bundle\\css\\style.css", '\\');

        webFilesService.importJcrWebFiles(anyObject(Session.class), eq("bundle"), eq("css/style.css"), eq(windowsStyleCss.toFile()));
        expectLastCall();
        autoReload.broadcastPageReload();
        expectLastCall();
        replayWebFilesService();

        createWatcher("*");
        watcher.onPathsChanged(windowsBundleRoot, Collections.singleton(windowsStyleCss));

        verifyWebFilesService();
        logRecorder.assertNoWarningsOrErrorsLogged();
    }

    @Test
    public void modification_after_watcher_shutdown_does_not_trigger_import() throws InterruptedException, IOException {
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), anyObject(File.class), anyBoolean());
        expectLastCall();
        final AtomicInteger counter = new AtomicInteger();

        webFilesService.importJcrWebFiles(anyObject(Session.class), anyObject(String.class), anyObject(String.class), anyObject(File.class));
        expectLastCall().andAnswer(() -> {
            counter.incrementAndGet();
            return null;
        }).anyTimes();
        replayWebFilesService();

        createWatcher("*");
        watcher.shutdown();

        FileUtils.touch(styleCss);
        waitForNoFileSystemChanges();

        verifyWebFilesService();
        assertEquals("nothing should have been imported after the watcher has been shut down", 0, counter.get());
        logRecorder.assertNoWarningsOrErrorsLogged();
    }

    @Test
    public void unset_project_basedir_disables_file_system_observation_and_auto_reload() throws IOException, InterruptedException {
        System.clearProperty("project.basedir");

        autoReload.setEnabled(eq(false));
        expectLastCall();

        // no expectations for webFilesService since the creation of a new directory should not trigger a reimport
        replayWebFilesService();
        createWatcher("*");

        final File newDir = new File(testBundleDir, "newDir");
        FileUtils.forceMkdir(newDir);
        waitForNoFileSystemChanges();

        verifyWebFilesService();
        logRecorder.assertNoWarningsOrErrorsLogged();
    }

}
