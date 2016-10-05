/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.repository.util;

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DocumentUtils provides utility methods for dealing with JCR nodes that represent documents.
 */
public class DocumentUtils {
    private static final Logger log = LoggerFactory.getLogger(DocumentUtils.class);

    /**
     * Retrieve a document handle node.
     *
     * @param uuid    UUID of the node
     * @param session JCR session to access the node
     * @return        Handle node or nothing, wrapped in an Optional
     */
    public static Optional<Node> getHandle(final String uuid, final Session session) {
        try {
            final Node node = session.getNodeByIdentifier(uuid);
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                return Optional.of(node);
            }
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Problem retrieving handle node", e);
            } else {
                log.warn("Problem retrieving handle node: {}", e.getMessage());
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieve the display name of a document
     *
     * @param handle JCR node representing a document handle
     * @return       Display string or nothing, wrapped in an Optional
     */
    public static Optional<String> getDisplayName(final Node handle) {
        try {
            return Optional.of(((HippoNode) handle).getDisplayName());
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Problem retrieving document display name", e);
            } else {
                log.warn("Problem retrieving document display name: {}", e.getMessage());
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieve the node type of a variant node of a document identified by its handle node, e.g. "project:blogpost".
     *
     * @param handle JCR node representing a document handle
     * @return       Node type or nothing, wrapped in an Optional
     */
    public static Optional<String> getVariantNodeType(final Node handle) {
        try {
            return Optional.of(getAnyVariant(handle).getPrimaryNodeType().getName());
        } catch (PathNotFoundException e) {
            log.info(e.getMessage());
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Problem retrieving variant node type for handle(?) node '{}'", handle, e);
            } else {
                log.warn("Problem retrieving variant node type for handle(?) node '{}': {}", handle, e.getMessage());
            }
        }
        return Optional.empty();
    }

    private static Node getAnyVariant(final Node handle) throws RepositoryException {
        return handle.getNode(handle.getName());
    }
}
