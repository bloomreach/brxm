/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.services.content;

import java.util.Iterator;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.onehippo.cms7.services.contenttype.ContentTypes;

/**
 * A ContentManager for retrieving and persisting {@link ContentTypes} based {@link ContentNode} objects.
 * <p>
 * A ContentManager is JCR Session bound. Writing ContentNode objects will <em>only</em> change the transient state of its JCR Session.
 * Persisting such changes requires a subsequent call to {@link javax.jcr.Session#save()}.
 */
public interface ContentManager {

    /**
     * @return the JCR Session this ContentManager is bound to.
     */
    Session getSession();

    /**
     * @return the current ContentTypes model definition used by this ContentManager
     * @throws RepositoryException if a repository error occurs.
     */
    ContentTypes getContentTypes() throws RepositoryException;

    /**
     * @return a ValueFactory from the {@link #getSession() JCR Session} of this ContentManager to create new {@link Value} objects
     * @throws UnsupportedOperationException if writing to the repository is not supported.
     * @throws RepositoryException if another error occurs.
     */
    ValueFactory getValueFactory() throws UnsupportedOperationException, RepositoryException;

    /**
     * @param node A JCR Node to create a new ContentNode object instance for
     * @return a ContentNode instance for the specified JCR node
     * @throws ItemNotFoundException if this ContentManager its JCR Session does not have read access to the provided Node instance
     * @throws RepositoryException if another error occurs.
     */
    ContentNode getContentNode(Node node) throws ItemNotFoundException, RepositoryException;

    /**
     * @param identifier The identifier of an existing and accessible JCR Node to create a new ContentNode object instance for
     * @return a ContentNode instance for the specified JCR node
     * @throws ItemNotFoundException if no node with the specified identifier exists or if this Session does not have read access to the node with the specified identifier.
     * @throws RepositoryException if another error occurs.
     */
    ContentNode getContentNodeByIdentifier(String identifier) throws ItemNotFoundException, RepositoryException;

    /**
     * @param path An absolute JCR path.
     * @return a ContentNode instance for the Node at the specified absolute JCR path.
     * @throws PathNotFoundException If no accessible node is found at the specified path.
     * @throws RepositoryException if another error occurs.
     */
    ContentNode getContentNodeByPath(String path) throws PathNotFoundException, RepositoryException;
    RangeIterable<ContentNode> getContentNodesByIdentifiers(Iterable<String> identifiers);
    RangeIterable<ContentNode> getContentNodesByIdentifiers(Iterator<String> identifiers);
    RangeIterable<ContentNode> getContentNodesByPaths(Iterable<String> paths);
    RangeIterable<ContentNode> getContentNodesByPaths(Iterator<String> paths);
    RangeIterable<ContentNode> getContentNodes(Iterable<Node> nodes);
    RangeIterable<ContentNode> getContentNodes(NodeIterator nodes);
    void attach(ContentNode node);
    void write(ContentNode node);
    void write(Iterable<ContentNode> nodes);
    void write(Iterator<ContentNode> nodes);
}
