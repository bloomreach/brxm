/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.bootstrap.instructions;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.webfiles.WebFileException;
import org.onehippo.cms7.services.webfiles.WebFilesService;
import org.onehippo.repository.bootstrap.InitializeInstruction;
import org.onehippo.repository.bootstrap.InitializeItem;
import org.onehippo.repository.bootstrap.PostStartupTask;
import org.onehippo.repository.bootstrap.util.PartialZipFile;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_WEB_FILE_BUNDLE;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.log;
import static org.onehippo.repository.bootstrap.util.BootstrapUtils.getBaseZipFileFromURL;
import static org.onehippo.repository.bootstrap.util.BootstrapUtils.removeNode;

public class WebFileBundleInstruction extends InitializeInstruction {

    public WebFileBundleInstruction(final InitializeItem item, final Session session) {
        super(item, session);
    }

    @Override
    protected String getName() {
        return HIPPO_WEB_FILE_BUNDLE;
    }

    @Override
    public PostStartupTask execute() throws RepositoryException {
        if (!session.nodeExists(WebFilesService.JCR_ROOT_PATH)) {
            log.error("Failed to initialize item {}: web file root {} is missing", item.getName(), WebFilesService.JCR_ROOT_PATH);
            return null;
        }

        String bundlePath = item.getWebFileBundle();
        // remove leading and trailing /
        bundlePath = bundlePath.indexOf('/') == 0 && bundlePath.length() > 1 ? bundlePath.substring(1) : bundlePath;
        bundlePath = bundlePath.lastIndexOf('/') == bundlePath.length()-1 ? bundlePath.substring(0, bundlePath.length()-1) : bundlePath;
        if (bundlePath.isEmpty()) {
            log.error("Failed to initialize item {}: invalid {} property", item.getName(), HIPPO_WEB_FILE_BUNDLE);
            return null;
        }
        final String extensionSource = item.getExtensionSource();

        try {
            final PostStartupTask importTask = createImportWebFileTask(extensionSource, bundlePath, session);
            if (importTask != null && item.isReloadable()) {
                final String bundleName = bundlePath.indexOf('/') == -1 ? bundlePath : bundlePath.substring(bundlePath.lastIndexOf('/') + 1);
                final String contextNodePath = WebFilesService.JCR_ROOT_PATH + "/" + bundleName;
                if (session.nodeExists(contextNodePath)) {
                    removeNode(session, contextNodePath, false);
                }
            }
            return importTask;
        } catch (URISyntaxException |IOException e) {
            log.error("Error initializing web file bundle {} at {}", bundlePath, WebFilesService.JCR_ROOT_PATH, e);
            return null;
        }

    }

    private PostStartupTask createImportWebFileTask(final String extensionSource, final String bundlePath, final Session session) throws IOException, URISyntaxException {
        if (extensionSource == null) {
            return null;
        } else if (extensionSource.contains("jar!")) {
            final PartialZipFile bundleZipFile = new PartialZipFile(getBaseZipFileFromURL(new URL(extensionSource)), bundlePath);
            return new ImportWebFileBundleFromZipTask(session, bundleZipFile);
        } else if (extensionSource.startsWith("file:")) {
            final File extensionFile = FileUtils.toFile(new URL(extensionSource));
            final File bundleDir = new File(extensionFile.getParent(), bundlePath);
            return new ImportWebFileBundleFromDirectoryTask(session, bundleDir);
        }
        return null;
    }

    private class ImportWebFileBundleFromZipTask implements PostStartupTask {

        private final Session session;
        private final PartialZipFile bundleZipFile;

        public ImportWebFileBundleFromZipTask(final Session session, final PartialZipFile bundleZipFile) {
            this.session = session;
            this.bundleZipFile = bundleZipFile;
        }

        @Override
        public void execute() {
            final WebFilesService service = HippoServiceRegistry.getService(WebFilesService.class);
            if (service == null) {
                log.error("Failed to import web file bundle '{}' from '{}': missing service for '{}'",
                        bundleZipFile.getSubPath(), bundleZipFile.getName(), WebFilesService.class.getName());
                return;
            }
            try {
                service.importJcrWebFileBundle(session, bundleZipFile, true);
                session.save();
            } catch (IOException | RepositoryException | WebFileException e) {
                log.error("Failed to import web file bundle '{}' from '{}'", bundleZipFile.getSubPath(),
                        bundleZipFile.getName(), e);
            }
        }
    }

    private class ImportWebFileBundleFromDirectoryTask implements PostStartupTask {

        private final Session session;
        private final File bundleDir;

        public ImportWebFileBundleFromDirectoryTask(final Session session, final File bundleDir) {
            this.session = session;
            this.bundleDir = bundleDir;
        }

        @Override
        public void execute() {
            final WebFilesService service = HippoServiceRegistry.getService(WebFilesService.class);
            if (service == null) {
                log.error("Failed to import web file bundle from '{}': missing service for '{}'",
                        bundleDir, WebFilesService.class.getName());
                return;
            }
            try {
                service.importJcrWebFileBundle(session, bundleDir, true);
                session.save();
            } catch (IOException | RepositoryException | WebFileException e) {
                log.error("Failed to import web file bundle from '{}'", bundleDir, e);
            }
        }
    }

}
