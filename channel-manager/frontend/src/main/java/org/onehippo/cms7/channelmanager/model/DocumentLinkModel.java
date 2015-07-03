/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.model;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model that stores a JCR path in a delegate model. The path can be absolute or relative to a certain root.
 * When relative paths are used, relative paths returned by the delegate are converted to absolute paths,
 * and absolute paths are converted to relative paths before being set in the delegate. Also the document name is
 * stored in the delegate.
 */
public class DocumentLinkModel implements IModel<String> {
    private static final Logger log = LoggerFactory.getLogger(DocumentLinkModel.class);

    private final IModel<DocumentLinkInfo> delegate;
    private final boolean isRelative;
    private final String rootPath;

    public DocumentLinkModel(final IModel<DocumentLinkInfo> delegate, final String path, final boolean isRelative, final String rootPath) {
        this.delegate = delegate;
        this.isRelative = isRelative;
        this.rootPath = rootPath;
        setObject(path);
    }

    @Override
    public String getObject() {
        String pickedPath = delegate.getObject().getPath();

        if (isRelative) {
            // picked path is relative; prepend the root path
            StringBuilder absPath = new StringBuilder(rootPath);
            if (!pickedPath.isEmpty() && !pickedPath.startsWith("/")) {
                absPath.append("/");
            }
            absPath.append(pickedPath);
            return absPath.toString();
        } else {
            // picked path is absolute; return as-is
            return pickedPath;
        }
    }

    @Override
    public void setObject(String path) {
        String relPath = path;
        if (isRelative && path.startsWith(rootPath)) {
            // convert absolute path to relative path
            relPath = path.substring(rootPath.length() + 1);
        }

        final DocumentLinkInfo documentLinkInfo = new DocumentLinkInfo();
        documentLinkInfo.setPath(relPath);
        documentLinkInfo.setDocumentName(getDocumentName(path));

        delegate.setObject(documentLinkInfo);
    }

    private String getDocumentName(final String path) {
        if (path.startsWith("/")) {
            final javax.jcr.Session session = ((UserSession)Session.get()).getJcrSession();

            try {
                final Node node = session.getNode(path);
                if (node instanceof HippoNode) {
                    return ((HippoNode)node).getLocalizedName();
                }
            } catch (PathNotFoundException e){
                log.info("Cannot find node for '{}'", path);
            } catch (RepositoryException e) {
                log.warn("Cannot retrieve node with path '{}'",path, e);
            }
        }

        return StringUtils.substringAfterLast(path, "/");
    }

    @Override
    public void detach() {
        delegate.detach();
    }

}