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

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.currentTimeMillis;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * File system observer that uses a {@link java.nio.file.WatchService} to get notified about changes.
 */
class FileSystemWatcher implements FileSystemObserver, Runnable {

    private static final Logger log = LoggerFactory.getLogger(SubDirectoriesWatcher.class);

    private static final Thread.UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER =
            (thread, exception) -> log.warn("FileSystemWatcher '{}' crashed", thread.getName(), exception);

    private static int instanceCounter = 0;

    static final int POLLING_TIME_MILLIS = 50;

    private final GlobFileNameMatcher watchedFiles;
    private final Map<Path, ChangesProcessor> changesProcessors;
    private final WatchService watcher;
    private final Thread thread;

    /**
     * The {@link java.nio.file.WatchService} used by this class has a percularity: when a directory is moved,
     * the associated watch key's watchable() still returns the old path. This map is therefore used to
     * keep track of which watch key actually matches to which path.
     */
    private final Map<WatchKey, Path> watchedPaths;

    FileSystemWatcher(final GlobFileNameMatcher watchedFiles) throws IOException {
        this.watchedFiles = watchedFiles;
        this.changesProcessors = new HashMap<>();

        watcher = FileSystems.getDefault().newWatchService();
        watchedPaths = new HashMap<>();

        thread = new Thread(this);
        thread.setName("FileSystemWatcher-" + instanceCounter);
        instanceCounter++;
        thread.setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER);
        thread.start();
    }

    @Override
    public void registerDirectory(final Path directory, final FileSystemListener listener) throws IOException {
        if (watchedFiles.matchesDirectory(directory)) {
            changesProcessors.put(directory, new ChangesProcessor(listener));
            registerRecursively(directory);
        } else {
            log.debug("Do not observe ignored directory {}", directory);
        }
    }

    private void registerRecursively(final Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path visitedDirectory, final BasicFileAttributes attrs) throws IOException {
                if (!FileSystemWatcher.this.watchedFiles.matchesDirectory(visitedDirectory)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                final WatchKey key = visitedDirectory.register(watcher,
                        ENTRY_CREATE,
                        ENTRY_MODIFY,
                        ENTRY_DELETE);

                Path previouslyRegisteredPath = watchedPaths.put(key, visitedDirectory);

                if (previouslyRegisteredPath == null) {
                    log.info("Registering new directory '{}'", visitedDirectory);
                } else if (!visitedDirectory.equals(previouslyRegisteredPath)) {
                    log.info("Registering moved directory '{}' -> '{}' to watcher '{}'", previouslyRegisteredPath, visitedDirectory, watcher);
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void run() {
        try {
            log.info("Watch started");
            while(true) {
                processChanges();
            }
        } catch (ClosedWatchServiceException e) {
            log.info("Watch closed", e);
        } finally {
            IOUtils.closeQuietly(watcher);
        }
    }

    private void processChanges() throws ClosedWatchServiceException {
        try {
            log.info("Waiting for changes...");
            final long pollStopTime = watchChange();
            pollForChangesUntil(pollStopTime);
            stopProcessingChanges();
        } catch (ClosedWatchServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Exception while processing watch keys: {}", e.toString(), e);
        }
    }

    private long watchChange() throws ClosedWatchServiceException, InterruptedException {
        final WatchKey key = watcher.take();
        final long pollStopTime = currentTimeMillis() + POLLING_TIME_MILLIS;
        processWatchKey(key);
        return pollStopTime;
    }

    /**
     * Keep polling for a short time: when (multiple) directories get deleted the watch keys might
     * arrive just a bit later
     */
    private void pollForChangesUntil(final long pollStopTime) throws ClosedWatchServiceException, InterruptedException {
        long timeout;
        while ((timeout = pollStopTime - currentTimeMillis()) > 0) {
            log.debug("Waiting {} ms for more changes...", timeout);
            WatchKey key = watcher.poll(timeout, TimeUnit.MILLISECONDS);
            if (key != null) {
                processWatchKey(key);
            }
        }
    }

    private void processWatchKey(final WatchKey key) {
        try {
            final Path watchedDirectory = watchedPaths.get(key);
            if (watchedDirectory == null) {
                log.warn("Ignoring watch event for unknown directory: {}", key.watchable());
            } else {
                log.debug("Processing watch key for '{}'", watchedDirectory);
                processFileSystemChanges(watchedDirectory, key);
            }
        } finally {
            key.reset();
        }
    }

    private void processFileSystemChanges(final Path watchedDirectory, final WatchKey key) {
        final ChangesProcessor processor = getChangesProcessorOrNull(watchedDirectory);

        if (processor == null) {
            log.warn("Ignoring change in {}: no change processor found", watchedDirectory);
            return;
        }

        processor.start();

        for (WatchEvent<?> event: key.pollEvents()) {
            final WatchEvent.Kind<?> kind = event.kind();
            final Object eventContext = event.context();

            log.debug("Processing {} {} in {}", kind.name(), eventContext, watchedDirectory);

            if (kind == StandardWatchEventKinds.OVERFLOW) {
                log.info("event overflow in {}. Reimporting and registering watchedDirectory '{}' to avoid half synced state",
                        watchedDirectory, watchedDirectory);
                if (Files.exists(watchedDirectory)) {
                    registerQuietly(watchedDirectory);
                }
                processor.processChange(kind, watchedDirectory, true);
            } else {
                final Path changedRelPath = (Path) eventContext;
                final Path changedAbsPath = watchedDirectory.resolve(changedRelPath);
                final boolean isDirectory = isDirectory(changedAbsPath, kind);
                if (watchedFiles.matches(changedAbsPath, isDirectory)) {
                    if (isDirectory && kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        registerQuietly(changedAbsPath);
                    }
                    processor.processChange(kind, changedAbsPath, isDirectory);
                } else {
                    log.debug("Skipping excluded path {}", changedAbsPath);
                }
            }
        }
    }

    private boolean isDirectory(Path path, WatchEvent.Kind<?> eventKind) {
        if (eventKind == StandardWatchEventKinds.ENTRY_DELETE) {
            // we cannot ask the file system whether a deleted path is a file or directory, but we
            // can use our own administration: if the path is watched, it must be a directory
            return watchedPaths.containsValue(path);
        }
        return Files.isDirectory(path);
    }

    private ChangesProcessor getChangesProcessorOrNull(final Path watchedDirectory) {
        for (Map.Entry<Path, ChangesProcessor> entry : changesProcessors.entrySet()) {
            final Path watchedRootDirectory = entry.getKey();
            if (watchedDirectory.startsWith(watchedRootDirectory)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void stopProcessingChanges() {
        for (ChangesProcessor processor : changesProcessors.values()) {
            processor.stop();
        }
    }

    private void registerQuietly(final Path changedAbsPath) {
        try {
            registerRecursively(changedAbsPath);
        } catch (IOException e) {
            log.error("Failed to register changed directory '{}'. Changes in this directory will not be picked up.", changedAbsPath, e);
        }
    }

    @Override
    public synchronized void shutdown() {
        try {
            watcher.close();
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // ignore, but don't wait for the thread
            log.debug("Ignoring exception while closing watcher", e);
        }
    }

    private static class ChangesProcessor {

        private final FileSystemListener listener;
        private boolean started;

        ChangesProcessor(final FileSystemListener listener) {
            this.listener = listener;
            started = false;
        }

        void start() {
            if (!started) {
                started = true;
                listener.fileSystemChangesStarted();
            }
        }

        void processChange(final WatchEvent.Kind<?> kind, final Path changedAbsPath, final boolean isDirectory) {
            if (isDirectory) {
                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    listener.directoryCreated(changedAbsPath);
                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    listener.directoryModified(changedAbsPath);
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    listener.directoryDeleted(changedAbsPath);
                } else if (kind == StandardWatchEventKinds.OVERFLOW) {
                    if (Files.exists(changedAbsPath)) {
                        log.info("Having an event overflow for '{}'. Entire directory '{}' will be recreated",
                                changedAbsPath, changedAbsPath);
                        listener.directoryCreated(changedAbsPath);
                    } else {
                        log.info("Having an event overflow for non existing directory '{}'. Directory will be removed",
                                changedAbsPath, changedAbsPath);
                        listener.directoryDeleted(changedAbsPath);
                    }
                }
            } else {
                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    listener.fileCreated(changedAbsPath);
                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    listener.fileModified(changedAbsPath);
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    listener.fileDeleted(changedAbsPath);
                } else if (kind == StandardWatchEventKinds.OVERFLOW) {
                    throw new IllegalStateException("Only a directory should even possibly overflow in events, for example" +
                            " by saving 1000 new files in one go.");
                }
            }
        }

        void stop() {
            if (started) {
                listener.fileSystemChangesStopped();
                started = false;
            }
        }

    }

}
