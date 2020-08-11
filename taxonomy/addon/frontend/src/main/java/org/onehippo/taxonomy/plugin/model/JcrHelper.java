/*
 *  Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin.model;

import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

/**
 * Helper class for common jcr operations
 */
public class JcrHelper {
    private static final String JCR_FROZEN_MIXIN_TYPES = "jcr:frozenMixinTypes";
    private static final String JCR_FROZEN_PRIMARY_TYPE = "jcr:frozenPrimaryType";
    private static final String JCR_MIXIN_TYPES = "jcr:mixinTypes";

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
        if (!node.isNodeType("nt:frozenNode")) {
            if (node.isNodeType(type)) {
                return true;
            }
            if (node.hasProperty(JCR_MIXIN_TYPES)) {
                NodeTypeManager ntMgr = node.getSession().getWorkspace().getNodeTypeManager();
                for (Value nodeType : node.getProperty(JCR_MIXIN_TYPES).getValues()) {
                    NodeType nt = ntMgr.getNodeType(nodeType.getString());
                    if (nt.isNodeType(type)) {
                        return true;
                    }
                }
            }
        } else {
            NodeTypeManager ntMgr = node.getSession().getWorkspace().getNodeTypeManager();
            String primaryType = node.getProperty(JCR_FROZEN_PRIMARY_TYPE).getString();
            if (ntMgr.getNodeType(primaryType).isNodeType(type)) {
                return true;
            }
            if (node.hasProperty(JCR_FROZEN_MIXIN_TYPES)) {
                for (Value nodeType : node.getProperty(JCR_FROZEN_MIXIN_TYPES).getValues()) {
                    NodeType nt = ntMgr.getNodeType(nodeType.getString());
                    if (nt.isNodeType(type)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Get a node name from a Locale. It will take for example the format "en_GB".
     *
     * @param locale that is the base for the name
     * @return the node name
     */
    public static String getNodeName(final Locale locale) {
        return locale.toString();
    }
}
