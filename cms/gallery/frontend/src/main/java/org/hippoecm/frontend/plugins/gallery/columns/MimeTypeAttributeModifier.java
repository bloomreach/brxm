/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.columns;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeAttributeModifier;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds a 'title' attribute that displays the MIME type of a child node or the rendered node.
 */
class MimeTypeAttributeModifier extends AbstractNodeAttributeModifier {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(MimeTypeAttributeModifier.class);
    private final String relPath;

    MimeTypeAttributeModifier(final String relPath) {
        this.relPath = relPath;
    }

    @Override
    protected AttributeModifier getCellAttributeModifier(final Node handleOrDocument) {
        try {
            final Node document = getDocumentNode(handleOrDocument);
            if (document.hasNode(relPath)) {
                final Node child = document.getNode(relPath);
                final Property mimeType = child.getProperty(JcrConstants.JCR_MIME_TYPE);
                return new AttributeAppender("title", mimeType.getString());
            }
        } catch (RepositoryException e) {
            log.debug("Cannot get MIME type of node {}", JcrUtils.getNodePathQuietly(handleOrDocument), e);
        }
        return null;
    }

    private Node getDocumentNode(final Node handleOrDocument) throws RepositoryException {
        if (handleOrDocument.isNodeType(HippoNodeType.NT_HANDLE)) {
            final String name = handleOrDocument.getName();
            if (handleOrDocument.hasNode(name)) {
                return handleOrDocument.getNode(name);
            }
        }
        return handleOrDocument;
    }
}
