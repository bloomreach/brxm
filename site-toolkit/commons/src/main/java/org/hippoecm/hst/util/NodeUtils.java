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
package org.hippoecm.hst.util;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNode;

/**
 * NodeUtils
 * 
 * @version $Id$
 */
public class NodeUtils {
    
    /**
     * Get the most accurate and complete version available of the information
     * represented in the current node if available.
     * 
     * @param node
     * @param defaultNode
     * @return the node with the most accurate representation of this node if available.
     * @throws java.lang.IllegalArgumentException when canonical node is not found for the node.
     * @throws java.lang.RuntimeException when the repository throws a general repository exception
     */
    public static Node getCanonicalNode(Node node) {
        return getCanonicalNode(node, null);
    }
    
    /**
     * Get the most accurate and complete version available of the information
     * represented in the current node if available.
     * Otherwise, returns the defaultNode instead of throwing an exception.
     * 
     * @param node
     * @param defaultNode
     * @return the node with the most accurate representation of this node if available. Otherwise returns the defaultNode.
     * @throws java.lang.IllegalArgumentException when canonical node is not found for the node.
     * @throws java.lang.RuntimeException when the repository throws a general repository exception
     */
    public static Node getCanonicalNode(Node node, Node defaultNode) {
        if (node == null) {
            throw new IllegalArgumentException("Node should be not null in finding canonical node.");
        }
        
        Node canonicalNode = null;
        
        if (node instanceof HippoNode) {
            try {
                canonicalNode = ((HippoNode) node).getCanonicalNode();
                
                if (canonicalNode == null) {
                    throw new IllegalArgumentException("No canonical node found for '" + node.getPath() + "'");
                }
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
        
        return (canonicalNode != null ? canonicalNode : defaultNode);
    }
}
