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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.hippoecm.repository.api.HippoNodeType;

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

}
