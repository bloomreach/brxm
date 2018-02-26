/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.webfiles.jcr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;

import org.apache.commons.lang.StringUtils;
import org.apache.tika.Tika;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.webfiles.Binary;
import org.onehippo.cms7.services.webfiles.WebFile;
import org.onehippo.cms7.services.webfiles.WebFileBundle;
import org.onehippo.cms7.services.webfiles.WebFileException;
import org.onehippo.cms7.services.webfiles.WebFileNotFoundException;
import org.onehippo.cms7.services.webfiles.WebFileTagNotFoundException;
import org.onehippo.repository.tika.TikaFactory;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.util.JcrUtils.getNodeIfExists;
import static org.onehippo.cms7.services.webfiles.WebFilesServiceImpl.logEvent;
import static org.onehippo.repository.util.JcrConstants.JCR_CONTENT;
import static org.onehippo.repository.util.JcrConstants.JCR_DATA;
import static org.onehippo.repository.util.JcrConstants.JCR_LAST_MODIFIED;
import static org.onehippo.repository.util.JcrConstants.JCR_MIME_TYPE;
import static org.onehippo.repository.util.JcrConstants.JCR_ROOT_VERSION;
import static org.onehippo.repository.util.JcrConstants.NT_FILE;
import static org.onehippo.repository.util.JcrConstants.NT_FOLDER;
import static org.onehippo.repository.util.JcrConstants.NT_RESOURCE;

public class WebFileBundleImpl implements WebFileBundle {

    private static final Logger log = LoggerFactory.getLogger(WebFileBundleImpl.class);

    public static final String PROPERTY_WEB_FILE_BUNDLE_ANTICACHE = "webfiles:anticache";

    private static final Tika tika = TikaFactory.newTika();

    private final Session session;
    private final Node bundle;
    private final String bundlePath;
    private final boolean versionable;

    public WebFileBundleImpl(final Session session, final Node bundle) {
        this.session = session;
        this.bundle = bundle;
        try {
            this.versionable = bundle.isNodeType(JcrConstants.MIX_VERSIONABLE);
            this.bundlePath = bundle.getPath();
        } catch (RepositoryException e) {
            throw new WebFileException(e);
        }
    }

    @Override
    public boolean exists(final String absPath) {
        if (!isValidAbsPath(absPath)) {
            return false;
        }
        try {
            get(absPath);
            return true;
        } catch (WebFileNotFoundException e) {
            return false;
        }
    }

    private boolean isValidAbsPath(final String path) {
        return StringUtils.isNotBlank(path) && path.startsWith("/") && path.length() > 1;
    }

    @Override
    public WebFile get(final String absPath) {
        if (!isValidAbsPath(absPath)) {
            throwNotFound(absPath);
        }
        final String relPath = relativePath(absPath);
        try {
            final Node resource = getNodeIfExists(bundle, relPath);
            if (resource == null) {
                throwNotFound(absPath);
            }
            return new WebFileImpl(resource);
        } catch (RepositoryException e) {
            throw new WebFileException("Error while getting web file '" + absPath + "'", e);
        }
    }

    private void throwNotFound(final String absPath) {
        String errorMessage = String.format("Web file '%s' not found with session '%s'", absPath, session.getUserID());
        throw new WebFileNotFoundException(errorMessage);
    }


    @Override
    public WebFile create(final String absPath, final Binary content) throws WebFileException, IllegalArgumentException {
        if (!isValidAbsPath(absPath)) {
            String msg = String.format("Not an absolute path: '%s'", absPath);
            throw new WebFileException(msg);
        }
        try {
            if (bundle.hasNode(relativePath(absPath))) {
                String msg = String.format("At '%s' there already exists a node.", bundlePath +  absPath);
                throw new WebFileException(msg);
            }
            final Node parent = getOrCreateParentFolder(absPath);
            if (!parent.isNodeType(NT_FOLDER)) {
                String msg = String.format("Cannot create web file at '%s' because node at '%s' is of type " +
                        "'%s'.", absPath, parent.getPath(), parent.getPrimaryNodeType().getName());
                throw new WebFileException(msg);
            }
            final WebFileImpl result = create(parent, absPath, content);
            persistChanges(session);
            String msg = String.format("created web file '%s'", bundlePath + absPath);
            logEvent("create", session.getUserID(), msg);
            return result;
        } catch (RepositoryException | IOException e) {
            final String msg = String.format("Failed to create resource '%s'", bundlePath + absPath);
            throw new WebFileException(msg, e);
        }
    }

    @Override
    public WebFile update(final String absPath, final Binary binary) throws WebFileNotFoundException, IllegalArgumentException {
        final WebFile webFile = get(absPath);

        try {
            doUpdate(webFile, binary);
            return get(absPath);
        } catch (RepositoryException e) {
            final String msg = String.format("Failed to create resource '%s'", bundlePath + absPath);
            throw new WebFileException(msg, e);
        }
    }

    @Override
    public void delete(String absPath) throws WebFileException {
        final WebFile webFile = get(absPath);
        try {
            final Session session = bundle.getSession();
            session.getNode(webFile.getPath());
            persistChanges(session);
            String msg = String.format("Deleted resource at '%s'", absPath);
            logEvent("delete ", session.getUserID(), msg);
        } catch (RepositoryException e) {
            String msg = String.format("Failed to delete binary at '%s' : %s", absPath, e.toString());
            throw new WebFileException(msg, e);
        }
    }

    @Override
    public String getLatestTagName() throws WebFileNotFoundException {
        if (!versionable) {
            return null;
        }
        try {
            final Version base = bundle.getSession().getWorkspace().getVersionManager().getBaseVersion(bundlePath);
            if (base.getName().equals(JCR_ROOT_VERSION)) {
                log.info("Found base version but content '{}' has not yet been checked in before. " +
                        " Return head", bundlePath);
                return null;
            }
            return base.getName();
        } catch (RepositoryException e) {
            throw new WebFileException(e.toString(), e);
        }

    }

    @Override
    public List<String> getTagNames() {
        if (!versionable) {
            log.info("Bundle '{}' has no tags because not versionable.", bundlePath);
            return Collections.emptyList();
        }
        try {
            final VersionIterator versions = bundle.getSession().getWorkspace().getVersionManager().getVersionHistory(bundlePath).getAllVersions();
            ArrayList<String> revisions = new ArrayList<>();
            while (versions.hasNext()) {
                final Version version = versions.nextVersion();
                revisions.add(version.getName());
            }
            return Collections.unmodifiableList(revisions);
        } catch (RepositoryException e) {
            throw new WebFileException(e.toString(), e);
        }
    }

    @Override
    public WebFile get(final String absPath, final String tagName) throws WebFileNotFoundException, WebFileTagNotFoundException {
        if (tagName == null) {
            return get(absPath);
        }
        if (!versionable) {
            String msg = String.format("Bundle '%s' does not support tagging so cannot return tag '%s' for '%s'.",
                    bundlePath, tagName, absPath);
            throw new WebFileException(msg);
        }
        if (!isValidAbsPath(absPath)) {
            throwNotFound(absPath);
        }
        try {
            final Node version = bundle.getSession().getWorkspace().getVersionManager()
                    .getVersionHistory(bundlePath)
                    .getVersion(tagName)
                    .getFrozenNode()
                    .getNode(absPath.substring(1));
            return new WebFileImpl(version);
        } catch (RepositoryException e) {
            throw new WebFileException(e.toString(), e);
        }
    }

    @Override
    public String createTag() throws WebFileException {
        if (!versionable) {
            String msg = String.format("Bundle '%s' cannot be tagged because not versionable.", bundlePath);
            throw new WebFileException(msg);
        }
        try {
            final Version newTag = bundle.getSession().getWorkspace().getVersionManager().checkin(bundlePath);
            log.debug("Created tag '{}' for bundle '{}'.", newTag.getName(), bundlePath);
            return newTag.getName();
        } catch (RepositoryException e) {
            String msg = String.format("Exception happend while trying to tag '%s' : '%s'.", bundlePath, e.toString());
            throw new WebFileException(msg, e);
        }
    }

    @Override
    public String getAntiCacheValue() {
        try {
            final String value = JcrUtils.getStringProperty(bundle, PROPERTY_WEB_FILE_BUNDLE_ANTICACHE, null);
            if (value != null && value.length() > 0) {
                return value;
            }

            log.info("No anti cache value found for '{}'. Return as anti cache value first 6 chars of the bundle node uuid",
                    PROPERTY_WEB_FILE_BUNDLE_ANTICACHE);

            return bundle.getIdentifier().substring(0,6);
        } catch (RepositoryException e) {
            log.warn("Error trying to read anti cache property", e);
            throw new WebFileException("Error trying to read anti cache property", e);
        }
    }

    private WebFileImpl create(Node folder, String absPath, Binary content) throws RepositoryException, IOException {
        final String fileName = getFileName(absPath);
        final Node file = folder.addNode(fileName, NT_FILE);
        //  TODO do not use tika.detect, see CMS-9511 and CMS-9478
        doCreate(file, content, tika.detect(content.getStream(), fileName));
        return new WebFileImpl(file);
    }

    private String relativePath(final String absPath) {
        return absPath.substring(1);
    }

    private static String getFileName(final String absPath) {
        return absPath.substring(absPath.lastIndexOf('/')+1);
    }

    private Node getOrCreateParentFolder(final String absPath) throws RepositoryException {
        Node current = bundle;
        final String[] pathElements = absPath.split("/");
        for (int i = 1; i < pathElements.length-1; i++) {
            if (current.hasNode(pathElements[i])) {
                current = current.getNode(pathElements[i]);
            } else {
                current = current.addNode(pathElements[i], NT_FOLDER);
            }
        }
        return current;
    }

    private void doUpdate(WebFile webFile, Binary binary) throws RepositoryException {

        final Session session = bundle.getSession();
        final Node content = session.getNode(webFile.getPath()).getNode(JCR_CONTENT);
        final ValueFactory valueFactory = bundle.getSession().getValueFactory();
        final Value value = valueFactory.createValue(valueFactory.createBinary(binary.getStream()));
        content.setProperty(JCR_DATA, value);
        content.setProperty(JCR_LAST_MODIFIED, Calendar.getInstance());
        persistChanges(session);

        String msg = String.format("Updated resource at '%s'", webFile.getPath());
        logEvent("updated ", session.getUserID(), msg);
    }

    private void doCreate(final Node file, final Binary binary, final String mimeType) throws RepositoryException {
        {
            final Node content = file.addNode(JCR_CONTENT, NT_RESOURCE);
            final ValueFactory valueFactory = content.getSession().getValueFactory();
            final Value value = valueFactory.createValue(valueFactory.createBinary(binary.getStream()));
            content.setProperty(JCR_DATA, value);
            content.setProperty(JCR_MIME_TYPE, mimeType);
            persistChanges(file.getSession());
            String msg = String.format("Created resource at '%s'", file.getPath());
            logEvent("created ", file.getSession().getUserID(), msg);
        }
    }


    private void persistChanges(final Session session) throws RepositoryException {
        if (session.hasPendingChanges()) {
            bundle.setProperty(PROPERTY_WEB_FILE_BUNDLE_ANTICACHE, String.valueOf(System.currentTimeMillis()));
            session.save();
        }
    }

}
