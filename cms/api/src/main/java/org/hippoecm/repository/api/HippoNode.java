/*
 *  Copyright 2008 Hippo.
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

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

public interface HippoNode extends Node {
    final static String SVN_ID = "$Id$";

    /*
     * Other (future) extensions to the interface may include:
     *
     *   public boolean isDocument();
     * Which would at this time be equivalent to
     * isNodeType(HippoNodeType.NT_DOCUMENT)
     * and could be used to check whether the Node is in fact
     * a full blown document bonzai.
     *
     *   public Node getPrimaryNode();
     *   public Node getPrimaryNode(Map<String,String> facets);
     * These calls can be used to look up the primary handle of
     * the document.  The handle of a document is the JCR Node
     * under which all instances of a particular document reside
     * and which should be used for reference purposes.
     * The second version of this call returns in a pre-initialized
     * map the facets which should (according to the handle definition)
     * uniquely identify the document node for which the handle was
     * requested.
     */

    /**
     * Get the most accurate and complete version available of the information
     * represented in the current node.  Certain operations are only allowed
     * on the most complete version of a node, rather then on any presentation
     * of the node.
     *
     * @returns the node with the most accurate representation of this node.
     */
    public Node getCanonicalNode() throws RepositoryException;

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
     * @param prune Wheter only to return the first matching modified node in
     *              a subtree (true), or provide a depth search for all modified
     *              nodes (false)
     * @returns A NodeIterator instance which iterates over all modified
     *          nodes, not including this node
     * @see Session.pendingChanges(Node,String,boolean)
     */
    public NodeIterator pendingChanges(String nodeType, boolean prune) throws NamespaceException, NoSuchNodeTypeException,
                                                                              RepositoryException;

    /** Conveniance method for <code>pendingChanges(nodeType,false)</code>
     *
     * @param nodeType Only nodes that are (derived) of this nodeType are
     *                 included in the result
     * @returns A NodeIterator instance which iterates over all modified
     *          nodes, not including the passed node
     * @see pendingChanges(String,boolean)
     */
    public NodeIterator pendingChanges(String nodeType) throws NamespaceException, NoSuchNodeTypeException, RepositoryException;

    /** Conveniance method for <code>pendingChanges("nt:base", false)</code>
     *
     * @returns A NodeIterator instance which iterates over all modified
     *          nodes, not including the passed node
     * @see pendingChanges(String,boolean)
     */
    public NodeIterator pendingChanges() throws RepositoryException;
}
