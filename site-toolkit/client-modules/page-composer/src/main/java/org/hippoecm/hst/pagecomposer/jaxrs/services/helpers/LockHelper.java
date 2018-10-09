/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON;
import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_EDITABLE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CATALOG;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CHANNEL;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_COMPONENTS;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_PAGES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_SITEMAP;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_SITEMENUS;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_TEMPLATES;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError.ITEM_ALREADY_LOCKED;

public class LockHelper {

    private static final Logger log = LoggerFactory.getLogger(LockHelper.class);

    private static final String[] LOCKABLE_NODE_TYPES = {NODETYPE_HST_TEMPLATES, NODETYPE_HST_CONTAINERCOMPONENT,
            NODETYPE_HST_CATALOG, NODETYPE_HST_PAGES, NODETYPE_HST_SITEMAP, NODETYPE_HST_SITEMENUS,
            NODETYPE_HST_CHANNEL, NODETYPE_HST_COMPONENTS};

    /**
     * recursively unlocks <code>configNode</code> and/or any descendant
     */
    public void unlock(final Node configNode) throws RepositoryException {
        log.info("Removing lock for '{}' since node or ancestor gets published", configNode.getPath());
        if (configNode.isNodeType(MIXINTYPE_HST_EDITABLE)) {
            configNode.removeMixin(MIXINTYPE_HST_EDITABLE);
        }
        if (configNode.hasProperty(GENERAL_PROPERTY_LOCKED_BY)) {
            configNode.getProperty(GENERAL_PROPERTY_LOCKED_BY).remove();
        }
        if (configNode.hasProperty(GENERAL_PROPERTY_LOCKED_ON)) {
            configNode.getProperty(GENERAL_PROPERTY_LOCKED_ON).remove();
        }
        for (Node child : new NodeIterable(configNode.getNodes())) {
            unlock(child);
        }
    }

    /**
     * if the <code>node</code> is already locked for the user <code>node.getSession()</code> this method does not do
     * anything. If there is no lock yet, a lock for the current session userID gets set on the node. If there is
     * already a lock by another user a ClientException is thrown,
     *
     * @param node         the node to lock
     * @param versionStamp if > 0, it will be used as a requirement that the <code>node</code> has the same stamp
     */
    public void acquireLock(final Node node, final long versionStamp) throws RepositoryException {
        acquireLock(node, node.getSession().getUserID(), versionStamp);
    }

    /**
     * if the <code>node</code> is already locked for the <code>user</code> this method does not do
     * anything. If there is no lock yet, a lock for the user gets set on the node. If there is
     * already a lock by another user a ClientException is thrown,
     *
     * @param node         the node to lock
     * @param lockFor      the user to lock the node for
     * @param versionStamp if > 0, it will be used as a requirement that the <code>node</code> has the same stamp
     */
    public void acquireLock(final Node node, final String lockFor, final long versionStamp) throws RepositoryException {
        final Node unLockableNode = getUnLockableNode(node, lockFor, true, true);
        if (unLockableNode != null) {
            final String message = String.format("Node '%s' cannot be locked due to someone else who has the lock (possibly a descendant or ancestor that is locked).", node.getPath());
            throw new ClientException(message, ITEM_ALREADY_LOCKED, getErrorParameterMap(unLockableNode, message));
        }
        doLock(node, lockFor, versionStamp);
    }

    /**
     * @param node         the node to lock
     * @param versionStamp if > 0, it will be used as a requirement that the <code>node</code> has the same stamp
     * @see {@link #acquireLock(javax.jcr.Node, String, long)} only for the acquireSimpleLock there is no need to check
     * descendant or
     * ancestors. It is only the node itself that needs to be checked. This is for example the case for sitemenu nodes
     * where there is no fine-grained locking for the items below it
     */
    void acquireSimpleLock(final Node node, final long versionStamp) throws RepositoryException {
        final Node unLockableNode = getUnLockableNode(node, false, false);
        if (unLockableNode != null) {
            final String message = String.format("Node '%s' cannot be locked due to someone else who has the lock.", node.getPath());
            throw new ClientException(message, ITEM_ALREADY_LOCKED, getErrorParameterMap(node, message));
        }
        doLock(node, node.getSession().getUserID(), versionStamp);
    }

    private void doLock(final Node node, final String lockFor, final long versionStamp) throws RepositoryException {
        if (isLockableNodeType(node)) {
            // due historical reasons, the HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT does not need
            // editable mixin
            log.debug("node '{}' is of type '{}' so not editable mixin needed.",
                    node.getPath(), node.getPrimaryNodeType().getName());
        } else if (!node.isNodeType(MIXINTYPE_HST_EDITABLE)) {
            log.debug("Adding mixin '{}' to '{}'.",
                    MIXINTYPE_HST_EDITABLE, node.getPath());
            node.addMixin(MIXINTYPE_HST_EDITABLE);
        }
        if (node.hasProperty(GENERAL_PROPERTY_LOCKED_BY)) {
            // user already has the lock
            return;
        }

        if (versionStamp != 0 && node.hasProperty(GENERAL_PROPERTY_LAST_MODIFIED)) {
            long existingStamp = node.getProperty(GENERAL_PROPERTY_LAST_MODIFIED).getDate().getTimeInMillis();
            if (existingStamp != versionStamp) {
                Calendar existing = Calendar.getInstance();
                existing.setTimeInMillis(existingStamp);
                String msg = String.format("Node '%s' has been modified wrt versionStamp. Cannot acquire lock now for user '%s'.",
                        node.getPath(), lockFor);
                log.info(msg);
                throw new ClientException(msg, ClientError.ITEM_CHANGED);
            }
        }
        log.info("Node '{}' gets a lock for user '{}'.", node.getPath(), lockFor);
        node.setProperty(GENERAL_PROPERTY_LOCKED_BY, lockFor);
        if (!node.hasProperty(GENERAL_PROPERTY_LOCKED_ON)) {
            node.setProperty(GENERAL_PROPERTY_LOCKED_ON, Calendar.getInstance());
        }
    }

    private boolean isLockableNodeType(final Node node) throws RepositoryException {
        for (String lockableNodeType : LOCKABLE_NODE_TYPES) {
            if (node.isNodeType(lockableNodeType)) {
                return true;
            }
        }
        return false;
    }

    private Map<?, ?> getErrorParameterMap(final Node node, final String message) throws RepositoryException {
        final Map<String, Object> parameterMap = new HashMap<>();
        if (node.hasProperty(GENERAL_PROPERTY_LOCKED_BY)) {
            parameterMap.put("lockedBy", node.getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
        }
        if (node.hasProperty(GENERAL_PROPERTY_LOCKED_ON)) {
            parameterMap.put("lockedOn", node.getProperty(GENERAL_PROPERTY_LOCKED_ON).getDate().getTimeInMillis());
        }
        parameterMap.put("errorReason", message);
        return parameterMap;
    }

    /**
     * if present, returns the unlockable {@link Node} wrt <code>node</code> : A <code>node</code> can be unlockable
     * (lock contained by someone else) due to an ancestor or descendant {@link Node} or because it is unlockable
     * itself. If there are no unLockable nodes wrt <code>node</code>, then <code>null</code> is returned
     */
    Node getUnLockableNode(final Node node, boolean checkAncestors, boolean checkDescendants) throws RepositoryException {
        return getUnLockableNode(node, node.getSession().getUserID(), checkAncestors, checkDescendants);
    }

    private Node getUnLockableNode(final Node node, final String user, boolean checkAncestors, boolean checkDescendants) throws RepositoryException {
        if (!canLock(node, user)) {
            return node;
        }
        if (checkAncestors) {
            final Node root = node.getSession().getRootNode();
            Node ancestor = node;
            while (!ancestor.isSame(root)) {
                if (!canLock(ancestor, user)) {
                    return ancestor;
                }
                ancestor = ancestor.getParent();
            }
        }
        if (checkDescendants) {
            for (Node child : new NodeIterable(node.getNodes())) {
                Node unLockableDescendant = getUnLockableNode(child, user, false, true);
                if (unLockableDescendant != null) {
                    return unLockableDescendant;
                }
            }
        }
        return null;
    }

    /**
     * returns <code>true</code> if the user can lock or already contains a lock on <code>node</code>.
     */
    private boolean canLock(final Node node, final String user) throws RepositoryException {
        if (node.hasProperty(GENERAL_PROPERTY_LOCKED_BY)) {
            final String lockedBy = node.getProperty(GENERAL_PROPERTY_LOCKED_BY).getString();
            return user.equals(lockedBy);
        }
        return true;
    }

}
