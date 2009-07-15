/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.model;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

/**
 * Helper class for common jcr operations
 */
public class JcrHelper {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private JcrHelper() {
    }

    /**
     * Determine whether node is of the specified type, by using the jcr:mixinTypes property.
     * This is necessary when a mixin has been added to the node, but the node hasn't been
     * saved yet.
     * 
     * @param node
     * @param type
     * @return true when the node is of the specified type
     * @throws RepositoryException
     */
    public static boolean isNodeType(Node node, String type) throws RepositoryException {
        // check primary type and mixins that have already been saved
        if (node.isNodeType(type)) {
            return true;
        }
        if (node.hasProperty("jcr:mixinTypes")) {
            NodeTypeManager ntMgr = node.getSession().getWorkspace().getNodeTypeManager();
            for (Value nodeType : node.getProperty("jcr:mixinTypes").getValues()) {
                NodeType nt = ntMgr.getNodeType(nodeType.getString());
                if (nt.isNodeType(type)) {
                    return true;
                }
            }
        }
        return false;
    }
}
