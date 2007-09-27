/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.api;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public interface HippoNode extends Node {

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
     * Returns a human representational name for the information represented
     * by this node.  In case there is no specific resolution to a human
     * representation this name should return the same value as getName().
     * This method should not return <code>null</code>.
     *
     * @returns a string which represents a human readable representation of
     * the name of the node
     */
    public String getDisplayName() throws RepositoryException;

}
