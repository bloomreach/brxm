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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.onehippo.cm.model.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watches all sub-directories of a directory for changes using a {@link FileSystemObserver}, and calls the provided
 * listener whenever a change is detected in one of the sub-directories.
 */
class SubDirectoriesWatcher implements FileSystemListener {

    private static final Logger log = LoggerFactory.getLogger(SubDirectoriesWatcher.class);

    private static final DirectoryStream.Filter<Path> DIRECTORY_FILTER = path ->
            Files.isDirectory(path) && !(path.endsWith(Constants.HCM_CONFIG_FOLDER) || path.endsWith(Constants.HCM_CONTENT_FOLDER));

    private final Path rootDirectory;
    private final PathChangesListener listener;
    private final FileSystemObserver fsObserver;
    private final Set<Path> createdPaths;
    private final SortedSet<Path> modifiedPaths;
    private final Set<Path> deletedPaths;


    public static void watch(final Path webFilesDirectory, final FileSystemObserver fsObserver, final PathChangesListener listener) throws IOException {
        new SubDirectoriesWatcher(webFilesDirectory, fsObserver, listener);
    }

    private SubDirectoriesWatcher(final Path directory, final FileSystemObserver fsObserver, final PathChangesListener listener) throws IOException {
        this.rootDirectory = directory;
        this.listener = listener;
        this.fsObserver = fsObserver;

        createdPaths = new HashSet<>();
        modifiedPaths = new TreeSet<>();
        deletedPaths = new HashSet<>();

        observeSubDirectories(directory);
    }

    private void observeSubDirectories(final Path directory) throws IOException {
        try (DirectoryStream<Path> subDirectories = Files.newDirectoryStream(directory, DIRECTORY_FILTER)) {
            for (Path subDirectory : subDirectories) {
                log.info("Watching directory: {}", subDirectory);
                fsObserver.registerDirectory(subDirectory, this);
            }
        }
    }

    @Override
    public void fileSystemChangesStarted() {
        createdPaths.clear();
        modifiedPaths.clear();
        deletedPaths.clear();
        notifyStart();
    }

    @Override
    public void directoryCreated(final Path directory) {
        pathCreated(directory);
    }

    @Override
    public void fileCreated(final Path file) {
        pathCreated(file);
    }

    private void pathCreated(final Path path) {
        createdPaths.add(path);
        deletedPaths.remove(path);
    }

    @Override
    public void directoryModified(final Path directory) {
        // ignore event
    }

    @Override
    public void fileModified(final Path file) {
        modifiedPaths.add(file);
    }

    @Override
    public void directoryDeleted(final Path directory) {
        pathDeleted(directory);
    }

    @Override
    public void fileDeleted(final Path file) {
        pathDeleted(file);
    }

    private void pathDeleted(final Path path) {
        if (!createdPaths.remove(path)) {
            deletedPaths.add(path);
        } else {
            modifiedPaths.remove(path);
        }
    }

    @Override
    public void fileSystemChangesStopped() {
        modifiedPaths.addAll(createdPaths);

        for (Path deleted : deletedPaths) {
            modifiedPaths.add(deleted.getParent());
        }

        removeSubPaths(modifiedPaths);

        if (!modifiedPaths.isEmpty()) {
            notifyPathsChanged(modifiedPaths);
        }

        notifyStop();
    }

    private static void removeSubPaths(final SortedSet<Path> sortedPaths) {
        final Iterator<Path> iterator = sortedPaths.iterator();
        Path current = null;
        while (iterator.hasNext()) {
            Path next = iterator.next();
            if (current != null && next.startsWith(current)) {
                iterator.remove();
            } else {
                current = next;
            }
        }
    }

    private void notifyStart() {
        log.debug("Start change");
        try {
            listener.onStart();
        } catch (RuntimeException e) {
            log.warn("Exception by listener '{}' when change started", listener, e);
        }
    }

    private void notifyPathsChanged(final Set<Path> changedPaths) {
        log.debug("Paths changed: {}", changedPaths);
        try {
            listener.onPathsChanged(rootDirectory, changedPaths);
        } catch (RuntimeException e) {
            if (log.isDebugEnabled()) {
                log.warn("Exception by listener '{}' while processing paths {}", listener, changedPaths, e);
            } else {
                log.warn("Exception by listener '{}' while processing paths {} : {}", listener, changedPaths, e.toString());
            }
        }
    }

    private void notifyStop() {
        log.debug("Stop change");
        try {
            listener.onStop();
        } catch (RuntimeException e) {
            log.warn("Exception by listener '{}' when change stopped", listener, e);
        }
    }

    static interface PathChangesListener {

        /**
         * Called when one or more directories in one of the watched subdirectories start changing.
         */
        void onStart();

        /**
         * Called when one of more directories in one of the watched subdirectories have changed.
         *
         * @param watchedRootDir the (absolute) root directory of the watcher
         * @param changedPaths the (absolute) paths that changed. Each path is either the path of a created or
         *                     modified entry, or the containing directory of a deleted entry.
         */
        void onPathsChanged(final Path watchedRootDir, final Set<Path> changedPaths);

        /**
         * Called when one or more directories in one of the watched subdirectories have stopped changing.
         */
        void onStop();

    }

}
