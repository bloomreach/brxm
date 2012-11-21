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

import java.util.Calendar;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for common jcr operations
 */
public class JcrHelper {

    private static final Logger log = LoggerFactory.getLogger(JcrHelper.class);
    
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
        if (node.isNodeType("nt:version")) {
            Node frozen = node.getNode("jcr:frozenNode");
            String primary = frozen.getProperty("jcr:frozenPrimaryType").getString();
            NodeTypeManager ntMgr = node.getSession().getWorkspace().getNodeTypeManager();
            if (ntMgr.getNodeType(primary).isNodeType(type)) {
                return true;
            }
            if (frozen.hasProperty("jcr:frozenMixinTypes")) {
                for (Value values : frozen.getProperty("jcr:frozenMixinTypes").getValues()) {
                    if (ntMgr.getNodeType(values.getString()).isNodeType(type)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Find the first checked-in version of the parent of a node that contains a particular
     * version of the child.  I.e. given a version of a child, the oldest version of the parent
     * that contains the child is returned.
     * 
     * @param version
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public static Version getVersionParent(Version version) throws ItemNotFoundException, RepositoryException {
        // when the node is not translated, return the decoded name at
        // the time of version creation.  Fall back to handle name if necessary.
        Node node = version.getNode("jcr:frozenNode");
        Calendar nodeCreated = version.getProperty("jcr:created").getDate();
        if (!node.hasProperty(HippoNodeType.HIPPO_PATHS)) {
            throw new ItemNotFoundException("No hippo:paths property present");
        }

        Value[] ancestors = node.getProperty(HippoNodeType.HIPPO_PATHS).getValues();
        if (ancestors.length <= 1) {
            throw new ItemNotFoundException("No ancestors in hippo:paths property");
        }

        String uuid = ancestors[1].getString();
        Node parent = node.getSession().getNodeByUUID(uuid);
        VersionHistory history = parent.getVersionHistory();
        Version best = null;
        for (VersionIterator versionIter = history.getAllVersions(); versionIter.hasNext();) {
            Version candidate = versionIter.nextVersion();
            Calendar versionDate = candidate.getCreated();
            if (!versionDate.after(nodeCreated)) {
                continue;
            }

            if (best == null || versionDate.before(best.getCreated())) {
                best = candidate;
            }
        }
        if (best != null) {
            return best;
        }
        throw new ItemNotFoundException();
    }

    /**
     * Retrieve the primary item of a node, according to the primary node type hierarchy.
     * Plain JackRabbit only checks the declared items of a type, not the inherited ones.
     * 
     * @param node
     * @return primary item
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public static Item getPrimaryItem(Node node) throws ItemNotFoundException, RepositoryException {
        NodeType primaryType = node.getPrimaryNodeType();
        String primaryItemName = primaryType.getPrimaryItemName();
        while (primaryItemName == null && !"nt:base".equals(primaryType.getName())) {
            for (NodeType nt : primaryType.getSupertypes()) {
                if (nt.getPrimaryItemName() != null) {
                    primaryItemName = nt.getPrimaryItemName();
                    break;
                }
                if (nt.isNodeType("nt:base")) {
                    primaryType = nt;
                }
            }
        }
        if (primaryItemName == null) {
            throw new ItemNotFoundException("No primary item definition found in type hierarchy");
        }
        return node.getSession().getItem(node.getPath() + "/" + primaryItemName);
    }
    
    /**
     * Determine whether node in question is a virtual node.
     * 
     * @param node  the node to check for virtuality
     * @return  whether the node is a virtual node or not
     */
    public static boolean isVirtualNode(Node node) throws RepositoryException {
        return node instanceof HippoNode && ((HippoNode) node).isVirtual();
    }

    /**
     * Determine whether the node is a virtual root node.  (facet search or facet navigation)
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public static boolean isVirtualRoot(final Node node) throws RepositoryException {
        return node.isNodeType(HippoNodeType.NT_FACETSEARCH) || node.isNodeType("hippofacnav:facetnavigation");
    }
}
