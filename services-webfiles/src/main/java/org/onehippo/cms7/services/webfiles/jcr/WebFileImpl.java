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
package org.onehippo.cms7.services.webfiles.jcr;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.webfiles.Binary;
import org.onehippo.cms7.services.webfiles.WebFile;
import org.onehippo.cms7.services.webfiles.WebFileException;
import org.onehippo.cms7.services.webfiles.WebFileNotFoundException;

import static org.onehippo.repository.util.JcrConstants.JCR_CONTENT;
import static org.onehippo.repository.util.JcrConstants.JCR_DATA;
import static org.onehippo.repository.util.JcrConstants.JCR_FROZEN_PRIMARY_TYPE;
import static org.onehippo.repository.util.JcrConstants.JCR_LAST_MODIFIED;
import static org.onehippo.repository.util.JcrConstants.JCR_MIME_TYPE;
import static org.onehippo.repository.util.JcrConstants.NT_FILE;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;

public class WebFileImpl implements WebFile {

    private final Node resource;
    private final Node content;
    private final String path;

    public WebFileImpl(final Node file) {
        try {
            assertExpectedNodeType(file);
            this.resource = file;
            this.path = file.getPath();
            content = resource.getNode(JCR_CONTENT);
        } catch (RepositoryException e) {
             throw new WebFileException("Cannot initialize web file.", e);
        }
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getName() {
        try {
            return resource.getName();
        } catch (RepositoryException e) {
            throw new WebFileException(e.toString() , e);
        }
    }

    @Override
    public String getEncoding() {
        return null;
    }

    @Override
    public Calendar getLastModified() {
        try {
            return JcrUtils.getDateProperty(content, JCR_LAST_MODIFIED, null);
        } catch (RepositoryException e) {
            throw new WebFileException(e.toString(), e);
        }
    }

    @Override
    public String getMimeType() {
        try {
            return JcrUtils.getStringProperty(content, JCR_MIME_TYPE, null);
        } catch (RepositoryException e) {
            throw new WebFileException(e.toString(), e);
        }
    }

    @Override
    public Binary getBinary() {
        try {
            return new JcrBinaryImpl(content.getProperty(JCR_DATA).getBinary());
        } catch (RepositoryException e) {
            throw new WebFileException(e.toString(), e);
        }
    }

    private void assertExpectedNodeType(final Node file) throws RepositoryException {
        if (! (file.isNodeType(NT_FILE) || file.isNodeType(NT_FROZEN_NODE))) {
            throwUnexpectedNode(file);
        }
        if (file.isNodeType(NT_FROZEN_NODE) && !NT_FILE.equals(file.getProperty(JCR_FROZEN_PRIMARY_TYPE).getString())) {
            throwUnexpectedNode(file);
        }
    }

    private void throwUnexpectedNode(final Node file) throws RepositoryException {
        String msg = String.format("JCR Node at '%s' is not of (frozen) type '%s'", file.getPath(), "nt:file");
        throw new WebFileNotFoundException(msg);
    }


}
