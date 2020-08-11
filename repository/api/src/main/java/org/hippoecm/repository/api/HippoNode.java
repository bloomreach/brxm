/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository.api;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * Any {@link javax.jcr.Node} instance returned by any method of a Hippo Repostitory may be cast to the HippoNode interface to expose additional functionality.
 */
public interface HippoNode extends Node {

    /**
     * Obtain a display name for this node. If not available falls back on {@link #getName()}.
     * If display name is available the result is the same as calling {@code getProperty("hippo:name").getString()}.
     *
     * If this node does not have a display name but is a child node of a hippo:handle node having a display name,
     * the display name of the parent handle node will be returned.
     *
     * @return the display name for this node if available, otherwise the JCR name of this node.
     * @throws RepositoryException
     */
    String getDisplayName() throws RepositoryException;

    /**
     * Get the most accurate and complete version available of the information
     * represented in the current node.  Certain operations are only allowed
     * on the most complete version of a node, rather than on any presentation
     * of the node.
     *
     * @throws ItemNotFoundException indicates the canonical node is no longer available
     * @throws RepositoryException indicates a generic unspecified repository error
     * @return the node with the most accurate representation of this node, may be nonsense if
     * there is no sensible canonical version available
     */
    public Node getCanonicalNode() throws ItemNotFoundException, RepositoryException;

    /**
     * Obtains an iterator over the set of nodes that potentially contain
     * changes, starting (and not including) this node.
     * Only nodes for which <code>Node.isNodeType(nodeType)</code> returns
     * true are included in the resulting set.  If the prune boolean value is
     * true, then the nodes matching in the hierarchy first are returned.  If
     * matching modified node exists beneath the nodes, these are not
     * included.
     *
     * @param nodeType Only nodes that are (derived) of this nodeType are
     *                 included in the result
     * @param prune Whether only to return the first matching modified node in
     *              a subtree (true), or provide a depth search for all modified
     *              nodes (false)
     * @throws NamespaceException indicates an invalid nodeType parameter
     * @throws NoSuchNodeTypeException indicates an invalid nodeType parameter
     * @throws RepositoryException indicates a generic unspecified repository error
     * @return A NodeIterator instance which iterates over all modified
     *         nodes, not including this node
     * @see HippoSession#pendingChanges(Node,String,boolean)
     */
    public NodeIterator pendingChanges(String nodeType, boolean prune) throws NamespaceException, NoSuchNodeTypeException,
                                                                              RepositoryException;

    /** Conveniance method for <code>pendingChanges(nodeType,false)</code>
     *
     * @param nodeType Only nodes that are (derived) of this nodeType are
     *                 included in the result
     * @throws NamespaceException indicates an invalid nodeType parameter
     * @throws RepositoryException indicates a generic unspecified repository error
     * @throws NoSuchNodeTypeException indicates an invalid nodeType parameter
     * @return A NodeIterator instance which iterates over all modified
     *          nodes, not including the passed node
     * @see #pendingChanges(String,boolean)
     */
    public NodeIterator pendingChanges(String nodeType) throws NamespaceException, NoSuchNodeTypeException, RepositoryException;

    /** Conveniance method for <code>pendingChanges("nt:base", false)</code>
     *
     * @throws RepositoryException indicates a generic unspecified repository error
     * @return A NodeIterator instance which iterates over all modified
     *          nodes, not including the passed node
     * @see #pendingChanges(String,boolean)
     */
    public NodeIterator pendingChanges() throws RepositoryException;

    /**
     * Whether this is a virtual node or not.
     *
     * @return  whether this is a virtual node or not.
     * @throws RepositoryException
     */
    public boolean isVirtual() throws RepositoryException;

    /**
     * Some operations may leave some nodes' derived data stale. With this method
     * the derived data of a node is made up to date.
     *
     * @return whether the node was changed during the recomputation
     */
    public boolean recomputeDerivedData() throws RepositoryException;
}
