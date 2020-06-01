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
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.services.autoreload.AutoReloadService;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.cms7.services.webfiles.WebFileEvent;
import org.onehippo.cms7.services.webfiles.WebFileException;
import org.onehippo.cms7.services.webfiles.WebFilesService;
import org.onehippo.cms7.services.webfiles.util.WatchFilesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watches a directory with web files for changes, and applies the observed changes to the
 * provided web file service. The provided directory should contain a child directory
 * per web file bundle, with the name of the directory being the name of the bundle.
 * Only existing bundles are watched for changes.
 */
public class WebFilesWatcher implements WebFilesWatcherService, SubDirectoriesWatcher.PathChangesListener {

    public static Logger log = LoggerFactory.getLogger(WebFilesWatcher.class);

    private final WebFilesWatcherConfig config;
    private WebFilesService service;
    private final Session session;
    private final HippoEventBus eventBus;
    private final AutoReloadService autoReload;
    private final FileSystemObserver fileSystemObserver;

    public WebFilesWatcher(final WebFilesWatcherConfig config, final WebFilesService service,
                           final Session session, final HippoEventBus eventBus, final AutoReloadService autoReload) {
        this.config = config;
        this.service = service;
        this.session = session;
        this.eventBus = eventBus;
        this.autoReload = autoReload;
        this.fileSystemObserver = observeFileSystemIfNeeded();

        if (fileSystemObserver == null && autoReload != null) {
            autoReload.setEnabled(false);
        } else if (autoReload != null){
            log.info("Auto reload and Web Files watching enabled. Start with initial (re-)import");
            for (Path webFilesRootDirectory : fileSystemObserver.getObservedRootDirectories()) {
                try {
                    log.info("Importing directory '{}'", webFilesRootDirectory);
                    service.importJcrWebFileBundle(session, webFilesRootDirectory.toFile(), false);
                    session.save();
                } catch (IOException | RepositoryException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to import directory '{}'", webFilesRootDirectory, e);
                    } else {
                        log.warn("Failed to import directory '{}' : '{}'", webFilesRootDirectory, e.toString());
                    }
                    resetSilently(session);
                }
            }
        }
    }

    private FileSystemObserver observeFileSystemIfNeeded() {
        final Path projectBaseDir = WatchFilesUtils.getProjectBaseDir();
        if (projectBaseDir == null) {
            return null;
        }
        if (config.getWatchedModules().size() > 0) {
            return observeFileSystem(projectBaseDir);
        } else {
            log.info("Watching web files is disabled: no web file modules configured to watch");
        }
        return null;
    }

    private FileSystemObserver observeFileSystem(final Path projectBaseDir) {
        FileSystemObserver fsObserver;
        try {
            fsObserver = createFileSystemObserver();
        } catch (Exception e) {
            log.error("Watching web files is disabled: cannot create file system observer", e);
            return null;
        }

        List<Path> webFilesDirectories = WatchFilesUtils.getWebFilesDirectories(projectBaseDir, config);
        for (Path webFilesDirectory : webFilesDirectories) {
            try {
                SubDirectoriesWatcher.watch(webFilesDirectory, fsObserver, this);
            } catch (Exception e) {
                log.error("Failed to watch or import web files in module '{}'", webFilesDirectory, e);
            }
        }
        return fsObserver;
    }

    private FileSystemObserver createFileSystemObserver() throws Exception {
        final GlobFileNameMatcher watchedFiles = new GlobFileNameMatcher();
        watchedFiles.includeFiles(config.getIncludedFiles());
        watchedFiles.excludeDirectories(config.getExcludedDirectories());

        if (useWatchService()) {
            log.info("Using file system watcher");
            return new FileSystemWatcher(watchedFiles);
        } else {
            long watchDelayMillis = config.getWatchDelayMillis();
            log.info("Using file system poller (delay: {} ms)", watchDelayMillis);
            return new FileSystemPoller(watchedFiles, watchDelayMillis);
        }
    }

    private boolean useWatchService() {
        final OsNameMatcher matcher = new OsNameMatcher();
        for (String pattern : config.getUseWatchServiceOnOsNames()) {
            try {
                matcher.include(pattern);
            } catch (IllegalArgumentException e) {
                log.warn("Ignoring OS name '{}': {}. On this OS web files will be watched using file system polling.",
                        pattern, e.getMessage());
            }
        }
        return matcher.matchesCurrentOs();
    }

    @Override
    public void onStart() {
        // nothing to do, but needed for thread synchronization in tests
    }

    @Override
    public void onPathsChanged(final Path watchedRootDir, final Set<Path> changedPaths) {
        final long startTime = System.currentTimeMillis();
        Set<Path> processedPaths = new HashSet<>(changedPaths.size());
        try {
            for (Path changedPath : changedPaths) {
                final Path relChangedDir = watchedRootDir.relativize(changedPath);
                final String bundleName = relChangedDir.getName(0).toString();
                final String bundleSubDir = getBundleSubDir(relChangedDir);

                log.info("Replacing web files in bundle '{}': /{}", bundleName, bundleSubDir);
                try {
                    service.importJcrWebFiles(session, bundleName, bundleSubDir, changedPath.toFile());
                    processedPaths.add(changedPath);
                } catch (IOException e) {
                    // we do not have to take action. An IOException is the result of a concurrent change (delete/move)
                    // during creation or processing of the archive. The change will trigger a new import
                    log.debug("IOException during importing '{}'. This is typically the result of a file that is deleted" +
                            "during the import of a directory. This delete will trigger an event shortly after this" +
                            " exception.", changedPath, e);
                } catch (NullPointerException e) {
                    // sigh....because org.apache.jackrabbit.vault.util.FileInputSource.getByteStream() returns null
                    // on a IOException (for example when a file is deleted during processing) I get an NPE I cannot avoid,
                    // however, it is just similar to the IOException above, typically the result of an event that will
                    // be processed shortly after this exception. Hence, ignore
                    log.debug("NullPointerException we cannot avoid because org.apache.jackrabbit.vault.util.FileInputSource.getByteStream() " +
                            "returns null on IOException. We can ignore this event since it is the result of an event that will " +
                            " be processed shortly after this exception. Hence, ignore change path '{}'", changedPath, e);

                }
            }
            if (!processedPaths.isEmpty()) {
                session.save();
                publishEvents(watchedRootDir, processedPaths);
            }
        } catch (WebFileException | RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.info("Failed to reload web files from '{}', resetting session and trying to reimport whole bundle(s)",
                        changedPaths, e);
            } else {
                log.info("Failed to reload web files from '{}' : '{}', resetting session and trying to reimport whole bundle(s)",
                        changedPaths, e.toString());
            }
            resetSilently(session);
            tryReimportBundles(watchedRootDir, changedPaths);
        }
        final long endTime = System.currentTimeMillis();
        log.info("Replacing web files took {} ms", endTime - startTime);
    }

    @Override
    public void onStop() {
        // nothing to do, but needed for thread synchronization in tests
    }

    private String getBundleSubDir(final Path relChangedDir) {
        if (relChangedDir.getNameCount() == 1) {
            return StringUtils.EMPTY;
        }
        final Path subPath = relChangedDir.subpath(1, relChangedDir.getNameCount());
        // ensure that we use '/' as the JCR path separator, even if the filesystem path uses something else
        return StringUtils.join(subPath.iterator(), '/');
    }

    private void publishEvents(final Path watchedRootDir, final Set<Path> changedPaths) {
        if (eventBus != null) {
            for (Path changedPath : changedPaths) {
                eventBus.post(new WebFileEvent(changedPath, watchedRootDir));
            }
        }
        if (autoReload != null) {
            autoReload.broadcastPageReload();
        }
    }

    private void resetSilently(final Session session) {
        try {
            session.refresh(false);
        } catch (RepositoryException e) {
            log.debug("Ignoring that session.refresh(false) failed", e);
        }
    }

    private void tryReimportBundles(final Path watchedRootDir, final Set<Path> changedPaths) {
        final Set<Path> reimportedBundleRoots = new HashSet<>();
        try {
            for (Path changedPath: changedPaths) {
                final Path relChangedDir = watchedRootDir.relativize(changedPath);
                final String bundleName = relChangedDir.getName(0).toString();
                final Path bundleRootDir = watchedRootDir.resolve(bundleName);
                if (reimportedBundleRoots.add(bundleRootDir)) {
                    log.info("Reimporting bundle '{}'", bundleName);
                    service.importJcrWebFileBundle(session, bundleRootDir.toFile(), false);
                }
            }
            session.save();
            publishEvents(watchedRootDir, reimportedBundleRoots);
        } catch (WebFileException | RepositoryException | IOException e) {
            log.warn("Failed to reimport web file bundles {}, resetting session", reimportedBundleRoots, e);
            resetSilently(session);
        }
    }

    public void shutdown() throws InterruptedException {
        if (fileSystemObserver != null) {
            fileSystemObserver.shutdown();
        }
    }

    @Override
    public List<Path> getWebFilesDirectories() {
        return fileSystemObserver.getObservedRootDirectories();
    }
}
