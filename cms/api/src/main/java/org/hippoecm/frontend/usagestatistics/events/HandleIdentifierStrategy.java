/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.usagestatistics.events;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Strategy to find the id of the handle of a node.</p>
 *
 * <p>The strategy returns the identifier of the handle if:
 * <ul>
 *     <li>The node is the handle</li>
 *     <li>The node is a descendant of a handle</li>
 *     <li>The node is revision</li>
 * </ul>
 * In all other cases {@code null} is returned.
 * </p>
 */
public class HandleIdentifierStrategy implements IdentifierStrategy {

    public static final Logger log = LoggerFactory.getLogger(HandleIdentifierStrategy.class);

    /**
     * <p>Traverses up the tree until a node of the type {@link HippoNodeType#NT_HANDLE} is found
     * and returns the identifier of that node.</p>
     * <p>If the node is a revision, the handle is found and the identifier of the handle is returned.</p>
     * <p>Otherwise {@code null} is returned.</p>
     *
     * @param node Node that is related to a handle
     * @return the identifier of the handle or {@code null}
     * @throws RepositoryException if an identifier of the handle cannot be obtained
     */
    public String getIdentifier(Node node) throws RepositoryException {
        String identifier = null;
        if (node != null){
            identifier = isRevision(node) ? getHandleIdentifierAssociatedWithRevision(node) :
                    getHandleIdentifierAssociatedWithDescendant(node);
            if (identifier == null){
                log.warn("Node { path: {} } is not a handle, descendant of a handle or revision, " +
                        "please provide a path to a handle, document or revision", node.getPath());
            }
        }
        return identifier;
    }

    private boolean isRevision(final Node node) throws RepositoryException {
        return node.isNodeType(JcrConstants.NT_VERSION) || node.isNodeType(JcrConstants.NT_FROZEN_NODE);
    }

    private Node ascentToHandleOrNode(final Node node) throws RepositoryException{
        return (isHandle(node) || isRoot(node)) ? node : ascentToHandleOrNode(node.getParent());
    }

    private boolean isRoot(final Node node) throws RepositoryException {
        return node.getDepth() == 0;
    }

    private String getHandleIdentifierAssociatedWithDescendant(final Node node) throws RepositoryException {
        Node ascendant = ascentToHandleOrNode(node);
        if (ascendant != null && !isRoot(ascendant)){
                log.debug("Return handle { path: {} } as ascendant of descendant { path: {} }",
                        ascendant.getPath(), node.getPath());
            return ascendant.getIdentifier();
        }
        return null;
    }

    private String getHandleIdentifierAssociatedWithRevision(final Node node) throws RepositoryException {
        Node jcrFrozenNode = null;
        if (node.isNodeType(JcrConstants.NT_VERSION)) {
            final NodeIterator nodes = node.getNodes();
            if (nodes.hasNext()) {
                jcrFrozenNode = nodes.nextNode();
            }
        } else if (node.isNodeType(JcrConstants.NT_FROZEN_NODE)) {
            jcrFrozenNode = node;
        }
        if (jcrFrozenNode != null) {
            final Property property = jcrFrozenNode.getProperty(HippoNodeType.HIPPO_RELATED);
            if (property != null) {
                final Value[] values = property.getValues();
                if (values.length > 0) {
                    final String documentIdentifier = values[0].getString();
                    final Node document = node.getSession().getNodeByIdentifier(documentIdentifier);
                    return getHandleIdentifierAssociatedWithDescendant(document);
                }
            }
        }
        return null;
    }

    private boolean isHandle(final Node handle) throws RepositoryException {
        return handle.isNodeType(HippoNodeType.NT_HANDLE);
    }

}
