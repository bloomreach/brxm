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

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
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

    void unlock(final Node workspaceNode) throws RepositoryException {
        if (workspaceNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE)) {
            log.warn("Removing lock for '{}' since ancestor gets published", workspaceNode.getPath());
            workspaceNode.removeMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        }
        for (Node child : new NodeIterable(workspaceNode.getNodes())) {
            unlock(child);
        }
    }

    /**
     * returns the userID that contains the deep lock or <code>null</code> when no deep lock present
     */
    String getSelfOrAncestorLockedBy(final Node node) throws RepositoryException {
        if (node == null || node.isSame(node.getSession().getRootNode())) {
            return null;
        }

        if (node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            return node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString();
        }

        if (node.isNodeType(HstNodeTypes.NODETYPE_HST_WORKSPACE)) {
            return null;
        }

        return getSelfOrAncestorLockedBy(node.getParent());
    }

    /**
     * if the <code>node</code> is already locked for the user <code>node.getSession()</code> this method does not do
     * anything. If there is no lock yet, a lock for the current session userID gets set on the node. If there is
     * already a lock by another user a IllegalStateException is thrown,
     */
    void acquireLock(final Node node) throws RepositoryException {
        if (hasSelfOrDescendantLockBySomeOneElse(node)) {
            final String message = String.format("Node '%s' cannot be locked due to someone else who has the lock (possibly a descendant that is locked).", node.getPath());
            // TODO (meggermont): fill the map
            final Map<?, ?> parameterMap = Collections.emptyMap();
            throw new ClientException(message, ClientError.ITEM_ALREADY_LOCKED, parameterMap);
        }
        String selfOrAncestorLockedBy = getSelfOrAncestorLockedBy(node);
        if (selfOrAncestorLockedBy != null) {
            if (selfOrAncestorLockedBy.equals(node.getSession().getUserID())) {
                log.debug("Node '{}' already locked", node.getSession().getUserID());
                return;
            }
            final String message = String.format("Node '%s' cannot be locked due to someone else who has the lock (possibly an ancestor that is locked).", node.getPath());
            // TODO (meggermont): fill the map
            final Map<?, ?> parameterMap = Collections.emptyMap();
            throw new ClientException(message, ClientError.ITEM_ALREADY_LOCKED, parameterMap);
        }
        doLock(node);
    }

    /**
     * @see {@link #acquireLock(javax.jcr.Node)} only for the acquireSimpleLock there is no need to check descendant or
     * ancestors. It is only the node itself that needs to be checked. This is for example the case for sitemenu nodes
     * where there is no fine-grained locking for the items below it
     */
    void acquireSimpleLock(final Node node) throws RepositoryException {
        if (isLockedBySessionUser(node)) {
            final String message = String.format("Node '%s' cannot be locked due to someone else who has the lock.", node.getPath());
            throw new ClientException(message, ClientError.ITEM_ALREADY_LOCKED, getParameterMap(node));
        }
        doLock(node);
    }

    private Map<?, ?> getParameterMap(final Node node) throws RepositoryException {
        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("lockedBy", node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        parameterMap.put("lockedOn", node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON).getDate().getTimeInMillis());
        return parameterMap;
    }


    boolean hasSelfOrAncestorLockBySomeOneElse(final Node node) throws RepositoryException {
        String selfOrAncestorLockedBy = getSelfOrAncestorLockedBy(node);
        if (selfOrAncestorLockedBy == null) {
            return false;
        }
        return !node.getSession().getUserID().equals(selfOrAncestorLockedBy);
    }

    private void doLock(final Node node) throws RepositoryException {
        if (!node.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE)) {
            node.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        }

        final Session session = node.getSession();
        log.info("Node '{}' gets a lock for user '{}'.", node.getPath(), session.getUserID());
        node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, session.getUserID());
        Calendar now = Calendar.getInstance();
        if (!node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
            node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON, now);
        }
        node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED_BY, session.getUserID());
    }

    private boolean hasSelfOrDescendantLockBySomeOneElse(final Node node) throws RepositoryException {
        if (isLockedBySessionUser(node)) {
            return true;
        }
        for (Node child : new NodeIterable(node.getNodes())) {
            boolean hasDescendantLock = hasSelfOrDescendantLockBySomeOneElse(child);
            if (hasDescendantLock) {
                return true;
            }
        }
        return false;
    }

    private boolean isLockedBySessionUser(final Node node) throws RepositoryException {
        if (node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            final String lockedBy = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString();
            return !lockedBy.equals(node.getSession().getUserID());
        }
        return false;
    }

}
