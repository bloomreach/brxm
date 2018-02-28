/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.zip.ZipFile;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.ws.WebServiceException;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.vault.fs.config.ConfigurationException;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.fs.io.Importer;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.autoreload.AutoReloadService;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.cms7.services.webfiles.jcr.WebFileBundleImpl;
import org.onehippo.cms7.services.webfiles.vault.AbstractWebFilesArchive;
import org.onehippo.cms7.services.webfiles.vault.WebFileBundleArchive;
import org.onehippo.cms7.services.webfiles.vault.WebFilesFileArchive;
import org.onehippo.cms7.services.webfiles.vault.WebFilesZipArchive;
import org.onehippo.cms7.services.webfiles.watch.GlobFileNameMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms7.services.webfiles.jcr.WebFileBundleImpl.PROPERTY_WEB_FILE_BUNDLE_ANTICACHE;
import static org.onehippo.cms7.services.webfiles.vault.AbstractWebFilesArchive.logSizeExceededWarning;

public class WebFilesServiceImpl implements WebFilesService {

    private static final Logger log = LoggerFactory.getLogger(WebFilesServiceImpl.class);

    public static final String NT_WEB_RESOURCE_BUNDLE = "webfiles:bundle";

    private final GlobFileNameMatcher importedFiles;
    private final long  maxFileLengthBytes;
    private final String reloadMode;

    public WebFilesServiceImpl(final GlobFileNameMatcher importedFiles, final long maxFileLengthBytes, final String reloadMode) {
        this.importedFiles = importedFiles;
        this.maxFileLengthBytes = maxFileLengthBytes;
        this.reloadMode = reloadMode;
    }

    @Override
    public WebFileBundle getJcrWebFileBundle(final Session session, final String bundleName) {
        try {
            final Node bundleRoot = getBundleRoot(session, bundleName);
            return new WebFileBundleImpl(session, bundleRoot);
        } catch (RepositoryException e) {
            throw new WebFileException(warn("Cannot instantiate web file bundle for '%s' : '%s'", bundleName, e.toString()));
        }
    }

    @Override
    public boolean fileMatches(File file) {
        boolean fileMatches = importedFiles.accept(file);
        return file.isFile() ? fileMatches && file.length() < maxFileLengthBytes : fileMatches;
    }

    @Override
    public String getReloadMode() {
        return reloadMode;
    }


    private Node getBundleRoot(final Session session, final String bundleName) throws RepositoryException {
        final Node webFilesRoot = getWebFilessRoot(session);
        final Node bundleRoot = JcrUtils.getNodeIfExists(webFilesRoot, bundleName);
        if (bundleRoot == null) {
            warnAndThrow("Cannot find web files bundle '%s' below '%s'", bundleName, JCR_ROOT_PATH);
        }
        if (!bundleRoot.isNodeType(NT_WEB_RESOURCE_BUNDLE)) {
            warnAndThrow("Cannot instantiate web file bundle for '%s' because" +
                    " it does not point to a node of type '%s'", bundleName, NT_WEB_RESOURCE_BUNDLE);
        }
        return bundleRoot;
    }

    private Node getWebFilessRoot(final Session session) throws RepositoryException, WebFileException {
        final Node webFilesRoot = JcrUtils.getNodeIfExists(JCR_ROOT_PATH, session);
        if (webFilesRoot == null) {
            warnAndThrow("Cannot find web files root at '%s'", JCR_ROOT_PATH);
        }
        return webFilesRoot;
    }

    @Override
    public void importJcrWebFileBundle(final Session session, final File directory, boolean bootstrapPhase) throws IOException, WebFileException {
        final AutoReloadService autoReload = HippoServiceRegistry.getService(AutoReloadService.class);
        if (bootstrapPhase && autoReload != null && autoReload.isEnabled()) {
            // (re)import files will be done directly from file system. In case of an existing local repository, existing
            // web files will be replaced completely to sync possible local changes after restart
            log.debug("Auto reload is enabled hence webfiles are (re-)imported directly from filesystem instead of from configuration module.");
        }
        final WebFilesFileArchive archive = new WebFilesFileArchive(directory, importedFiles, maxFileLengthBytes);
        importJcrWebFileBundle(session, archive);

    }

    @Override
    public void importJcrWebFileBundle(final Session session, final ZipFile zip, boolean bootstrapPhase) throws IOException, WebFileException {
        final AutoReloadService autoReload = HippoServiceRegistry.getService(AutoReloadService.class);
        if (bootstrapPhase && autoReload != null && autoReload.isEnabled()) {
            // (re)import files will be done directly from file system. In case of an existing local repository, existing
            // web files will be replaced completely to sync possible local changes after restart
           log.debug("Auto reload is enabled hence webfiles are (re-)imported directly from filesystem instead of from configuration module.");
        } else {
            final WebFilesZipArchive archive = new WebFilesZipArchive(zip, importedFiles, maxFileLengthBytes);
            importJcrWebFileBundle(session, archive);
        }
    }

    @Override
    public void importJcrWebFileBundle(final Session session, final ZipFile zip) throws IOException, WebFileException {
        final WebFilesZipArchive archive = new WebFilesZipArchive(zip, importedFiles, maxFileLengthBytes);
        importJcrWebFileBundle(session, archive);
    }


    private void importJcrWebFileBundle(final Session session, final AbstractWebFilesArchive archive) throws IOException, WebFileException{
        WebFileBundleArchive bundleArchive = new WebFileBundleArchive(archive);
        String bundleName = null;
        try {
            bundleArchive.open(true);
            bundleName = bundleArchive.getBundleName();
            final Node webFilesRoot = getWebFilessRoot(session);
            final String bundleRootPath = webFilesRoot.getPath() + '/' + bundleName;
            replaceWebFiles(session, bundleArchive, bundleRootPath, bundleRootPath);
        } catch (RepositoryException|ConfigurationException e) {
            throw new WebFileException(warn("Cannot import web file bundle '%s' : '%s' ", bundleName, e.toString()), e);
        } finally {
            bundleArchive.close();
        }
    }

    @Override
    public void importJcrWebFiles(final Session session,
                                  final String bundleName,
                                  final String bundleSubPath,
                                  final File fileOrDirectory) throws IOException, WebServiceException {
        if (fileOrDirectory.isFile() && fileOrDirectory.length() > maxFileLengthBytes) {
            logSizeExceededWarning(fileOrDirectory, maxFileLengthBytes);
            return;
        }
        final WebFilesFileArchive archive = new WebFilesFileArchive(fileOrDirectory, importedFiles, maxFileLengthBytes);
        try {
            archive.open(true);
            final Node webFilesRoot = getWebFilessRoot(session);
            final String bundleRootPath = webFilesRoot.getPath() + '/' + bundleName;
            String archiveRootPath = bundleRootPath;
            if (StringUtils.isNotEmpty(bundleSubPath)) {
                archiveRootPath += '/' + bundleSubPath;
            }
            replaceWebFiles(session, archive, bundleRootPath, archiveRootPath);
        } catch (IOException e) {
            throw e;
        } catch (RepositoryException | ConfigurationException e) {
            // just log at info level. WebFileException will be handled later and result in complete bundle re-import
            throw new WebFileException(info("Cannot import web files from '%s' : '%s'", fileOrDirectory, e.toString()), e);
        } finally {
            archive.close();
        }
    }

    private void replaceWebFiles(final Session session,
                                 final Archive archive,
                                 final String bundleRootPath,
                                 final String archiveRootPath) throws RepositoryException, IOException, ConfigurationException {
        long startTime = System.currentTimeMillis();
        final Node archiveRootNode = JcrUtils.getNodeIfExists(archiveRootPath, session);
        if (archiveRootNode != null) {
            log.debug("Removing existing children of '{}'", archiveRootPath);
            removeChildren(archiveRootNode);
        }

        final String importPath = StringUtils.substringBeforeLast(archiveRootPath, "/");
        final Node importRootNode = JcrUtils.getNodeIfExists(importPath, session);
        if (importRootNode == null) {
            // just log at info level. WebFileException will be handled later and result in complete bundle re-import
            throw new WebFileException(info("Cannot import web files at '%s': no such node", importPath));
        }

        log.debug("Importing archive at '{}'", importPath);
        final ImportOptions options = new ImportOptions();
        options.setAutoSaveThreshold(Integer.MAX_VALUE);  // never save after the import is done
        final Importer importer = new Importer(options);
        importer.run(archive, importRootNode);

        final Node bundleRootNode = session.getNode(bundleRootPath);
        final Property antiCache = JcrUtils.getPropertyIfExists(bundleRootNode, PROPERTY_WEB_FILE_BUNDLE_ANTICACHE);
        if (antiCache == null) {
            bundleRootNode.setProperty(PROPERTY_WEB_FILE_BUNDLE_ANTICACHE, String.valueOf(System.currentTimeMillis()));
        } else {
            antiCache.setValue(String.valueOf(System.currentTimeMillis()));
        }

        long endTime = System.currentTimeMillis();
        log.info("Replacing web files at '{}' took {} ms", StringUtils.removeStart(archiveRootPath, JCR_ROOT_PATH), endTime - startTime);
    }

    private static void removeChildren(final Node node) throws RepositoryException {
        final NodeIterator children = node.getNodes();
        while (children.hasNext()) {
            final Node child = children.nextNode();
            child.remove();
        }
    }

    public static void logEvent(String action, String user, String message) {
        final HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus != null) {
            final HippoEvent event = new HippoEvent("webfiles-service");
            event.category("webfiles-service").user(user).action(action);
            event.message(message);
            eventBus.post(event);
        }
    }

    private static void warnAndThrow(final String message, final Object... args) {
        throw new WebFileException(warn(message, args));
    }

    private static String warn(final String message, final Object... args) {
        String warning = String.format(message, args);
        log.warn(warning);
        return warning;
    }

    private static String info(final String message, final Object... args) {
        String warning = String.format(message, args);
        log.info(warning);
        return warning;
    }

}
