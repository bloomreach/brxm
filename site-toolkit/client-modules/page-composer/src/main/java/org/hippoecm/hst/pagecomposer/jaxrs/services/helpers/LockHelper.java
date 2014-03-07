/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.helpers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockHelper {

    private static final Logger log = LoggerFactory.getLogger(LockHelper.class);

    /**
     * recursively unlocks <code>workspaceNode</code> and/or any descendant
     */
    void unlock(final Node workspaceNode) throws RepositoryException {
        if (workspaceNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE)) {
            log.warn("Removing lock for '{}' since ancestor gets published", workspaceNode.getPath());
            workspaceNode.removeMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        } else if(workspaceNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)){
            workspaceNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).remove();
            if(workspaceNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)){
                workspaceNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON).remove();
            }
        }
        for (Node child : new NodeIterable(workspaceNode.getNodes())) {
            unlock(child);
        }
    }

    /**
     * if the <code>node</code> is already locked for the user <code>node.getSession()</code> this method does not do
     * anything. If there is no lock yet, a lock for the current session userID gets set on the node. If there is
     * already a lock by another user a ClientException is thrown,
     * @param node the node to lock
     * @param versionStamp if > 0, it will be used as a requirement that the <code>node</code> has the same stamp
     */
    public void acquireLock(final Node node, final long versionStamp) throws RepositoryException {
        final Node unLockableNode = getUnLockableNode(node, true, true);
        if (unLockableNode != null) {
            final String message = String.format("Node '%s' cannot be locked due to someone else who has the lock (possibly a descendant or ancestor that is locked).", node.getPath());
            throw new ClientException(message, ClientError.ITEM_ALREADY_LOCKED, getParameterMap(unLockableNode));
        }
        doLock(node, versionStamp);
    }

    /**
     * @see {@link #acquireLock(javax.jcr.Node, long)} only for the acquireSimpleLock there is no need to check descendant or
     * ancestors. It is only the node itself that needs to be checked. This is for example the case for sitemenu nodes
     * where there is no fine-grained locking for the items below it
     * @param node the node to lock
     * @param versionStamp if > 0, it will be used as a requirement that the <code>node</code> has the same stamp
     */
    void acquireSimpleLock(final Node node, final long versionStamp) throws RepositoryException {
        final Node unLockableNode = getUnLockableNode(node, false, false);
        if (unLockableNode != null) {
            final String message = String.format("Node '%s' cannot be locked due to someone else who has the lock.", node.getPath());
            throw new ClientException(message, ClientError.ITEM_ALREADY_LOCKED, getParameterMap(node));
        }
        doLock(node, versionStamp);
    }

    private void doLock(final Node node, final long versionStamp) throws RepositoryException {
        if (!node.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE)
                && !node.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT)) {
            node.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        }
        if (node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            // user already has the lock
            return;
        }

        final Session session = node.getSession();
        if (versionStamp != 0 && node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED)) {
            long existingStamp = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED).getDate().getTimeInMillis();
            if (existingStamp != versionStamp) {
                Calendar existing = Calendar.getInstance();
                existing.setTimeInMillis(existingStamp);
                String msg = String.format("Node '%s' has been modified wrt versionStamp. Cannot acquire lock now for user '%s'.",
                        node.getPath() , session.getUserID());
                log.info(msg);
                throw new ClientException(msg, ClientError.ITEM_NOT_IN_PREVIEW);
            }
        }
        log.info("Node '{}' gets a lock for user '{}'.", node.getPath(), session.getUserID());
        node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, session.getUserID());
        if (!node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
            node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON, Calendar.getInstance());
        }
        node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED_BY, session.getUserID());
    }

    private Map<?, ?> getParameterMap(final Node node) throws RepositoryException {
        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("lockedBy", node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        parameterMap.put("lockedOn", node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON).getDate().getTimeInMillis());
        return parameterMap;
    }

    /**
     * if present, returns the unlockable {@link Node} wrt <code>node</code> : A <code>node</code> can be unlockable
     * (lock contained by someone else) due to an ancestor or descendant {@link Node} or because it is unlockable
     * itself. If there are no unLockable nodes wrt <code>node</code>, then <code>node</code> is returned
     */
    Node getUnLockableNode(final Node node, boolean checkAncestors, boolean checkDescendants) throws RepositoryException {
        if (!canLock(node)) {
            return node;
        }
        if (checkAncestors) {
            final Node root = node.getSession().getRootNode();
            Node ancestor = node;
            while (!ancestor.isSame(root)) {
                if (!canLock(ancestor)) {
                    return ancestor;
                }
                ancestor = ancestor.getParent();
            }
        }
        if (checkDescendants) {
            for (Node child : new NodeIterable(node.getNodes())) {
                Node unLockableDescendant = getUnLockableNode(child, false, true);
                if (unLockableDescendant != null) {
                    return unLockableDescendant;
                }
            }
        }
        return null;
    }

    /**
     * returns <code>true</code> if the {@link Session} tight to <code>node</code> can lock or already contains a lock
     * on <code>node</code>
     */
    private boolean canLock(final Node node) throws RepositoryException {
        if (node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            final String lockedBy = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString();
            return node.getSession().getUserID().equals(lockedBy);
        }
        return true;
    }

}
