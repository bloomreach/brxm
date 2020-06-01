/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NodeUtils
 * 
 * @version $Id$
 */
public class NodeUtils {

    private static final Logger log = LoggerFactory.getLogger(NodeUtils.class);

    /**
     * Returns the canonical version of this node, and <code>null</code> when there is no canonical node
     * 
     * @param node
     * @return  the canonical version of this node, and <code>null</code> when there is no canonical node.
     * @throws java.lang.RuntimeException when the repository throws a general repository exception
     */
    public static Node getCanonicalNode(Node node) {
        if(node instanceof HippoNode) {
            HippoNode hnode = (HippoNode)node;
            try {
                Node canonical = hnode.getCanonicalNode();
                if(canonical == null) {
                    log.debug("Cannot get canonical node for '{}'. This means there is no phyiscal equivalence of the " +
                            "virtual node. Return null", node.getPath());
                }
                return canonical;
            } catch (RepositoryException e) {
                log.error("Repository exception while fetching canonical node. Return null", e);
                throw new RuntimeException(e);
            }
        } 
        return node;
    }

    /**
     * Checks if the <code>node</code> is dereferenceable.
     * When the <code>node</code> is type of either "hippo:facetselect" or "hippo:mirror" (or subtype of one of these),
     * the node is regarded as dereferenceable and the dereferenced node can be retrieved
     * by using {@link #getDeref(Node)} method.
     * @param node
     * @return <code>true</code> when the node is dereferenceable
     * @throws RepositoryException
     */
    public static boolean isDereferenceable(Node node) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_FACETSELECT) || node.isNodeType(HippoNodeType.NT_MIRROR)) {
            return true;
        }
        
        return false;
    }

    /**
     * Returns the canonical {@link Node} this <code>mirrorNode</code> is pointing to and returns <code>null</code> when
     * the <code>mirrorNode</code> is not a node of (sub)type hippo:mirror or hippo:facetselect
     * @param mirrorNode
     * @return the dereferenced node and <code>null</code> when the mirrorNode is not a dereferenceable type or when no dereferenced node can be found
     */
    public static Node getDeref(Node mirrorNode) {
        String docBaseUUID = null;
        try {
            if (!isDereferenceable(mirrorNode)) {
                log.info("Cannot deref a node that is not of (sub)type '{}' or '{}'. Return null", HippoNodeType.NT_FACETSELECT, HippoNodeType.NT_MIRROR);
                return null;
            }
            // HippoNodeType.HIPPO_DOCBASE is a mandatory property so no need to test if exists
            docBaseUUID = mirrorNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
            try {
                return mirrorNode.getSession().getNodeByIdentifier(docBaseUUID);
            } catch (IllegalArgumentException e) {
                log.warn("Docbase cannot be parsed to a valid uuid. Return null");
                return null;
            }
        } catch (ItemNotFoundException e) {
            String path = null;
            try {
                path = mirrorNode.getPath();
            } catch (RepositoryException e1) {
                log.error("RepositoryException, cannot return deferenced node: {}", e1);
            }
            log.info("ItemNotFoundException, cannot return deferenced node because docbase uuid '{}' cannot be found. The docbase property is at '{}/hippo:docbase'. Return null", docBaseUUID, path);
        } catch (RepositoryException e) {
            log.error("RepositoryException, cannot return deferenced node: {}", e);
        }
        
        return null;
    }

    /**
     * Checks if the <code>node</code> is type of the <code>nodeTypeName</code>.
     * Returns <code>true</code> if this node is of the specified primary node
     * type or mixin type, or a subtype thereof of the <code>nodeTypeName</code>. Returns <code>false</code>
     * otherwise. Also see {@link Node#isNodeType(String)}
     * </P>
     * @param node
     * @param nodeTypeName
     * @return <code>true</code> when <code>node</code> is of the specified primary node type or mixin type, 
     * or a subtype thereof of any of the <code>nodeTypeName</code>
     * @throws RepositoryException
     */
    public static boolean isNodeType(Node node, String nodeTypeName) throws RepositoryException {
        if (nodeTypeName != null) {
            if (node.isNodeType(nodeTypeName)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Checks if the <code>node</code> is type of any of the <code>nodeTypeNames</code>.
     * Returns <code>true</code> if this node is of the specified primary node
     * type or mixin type, or a subtype thereof of any of the <code>nodeTypeNames</code>. Returns <code>false</code>
     * otherwise. Also see {@link Node#isNodeType(String)}
     * </P>
     * @param node the jcr node
     * @param nodeTypeNames the list of  <code>nodeTypeNames</code> to check.
     * @return <code>true</code> when <code>node</code> is of the specified primary node type or mixin type, 
     * or a subtype thereof of any of the <code>nodeTypeNames</code>. If the <code>node</code> is <code>null</code> then
     * <code>false</code> is returned
     * @throws RepositoryException
     */
    public static boolean isNodeType(Node node, String ... nodeTypeNames) throws RepositoryException {
        if (node == null) {
            return false;
        }
        if (nodeTypeNames != null) {
            for (String nodeTypeName : nodeTypeNames) {
                if (node.isNodeType(nodeTypeName)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Checks if the primary nodetype of <code>node</code> is equal to one of the supplied <code>primaryNodeTypeNames</code>.
     * <P>
     * It returns true if <code>node.getPrimaryNodeType().getName().equals(primaryNodeTypeName) returns true
     * for any of the <code>primaryNodeTypeNames</code>
     * </P>
     * @param node
     * @param primaryNodeTypeNames
     * @return <code>true</code> if <code>node.getPrimaryNodeType().getName().equals(primaryNodeTypeName)</code> returns true
     * for any of the <code>primaryNodeTypeNames</code>
     * @throws RepositoryException
     */
    public static boolean isPrimaryNodeType(Node node, String ... primaryNodeTypeNames) throws RepositoryException {
        if (primaryNodeTypeNames != null) {
            String primaryNodeTypeName = node.getPrimaryNodeType().getName();
            for (String nodeTypeName : primaryNodeTypeNames) {
                if (primaryNodeTypeName.equals(nodeTypeName)) {
                    return true;
                }
            }
        }
        
        return false;
    }

}
