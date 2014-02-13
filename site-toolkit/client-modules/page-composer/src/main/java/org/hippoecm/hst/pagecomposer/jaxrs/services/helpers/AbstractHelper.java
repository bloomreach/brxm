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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import com.google.common.collect.Iterables;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_ROLES;

public abstract class AbstractHelper {

    private static final Logger log = LoggerFactory.getLogger(AbstractHelper.class);

    protected PageComposerContextService pageComposerContextService;

    public void setPageComposerContextService(final PageComposerContextService pageComposerContextService) {
        this.pageComposerContextService = pageComposerContextService;
    }

    /**
     * @return the configuration object for <code>id</code> and <code>null</code> if not existing
     */
    public abstract <T> T getConfigObject(String id);

    protected void removeProperty(Node node, String name) throws RepositoryException {
        if (node.hasProperty(name)) {
            node.getProperty(name).remove();
        }
    }

    /**
     * if the <code>node</code> is already locked for the user <code>node.getSession()</code> this method does not do anything.
     * If there is no lock yet, a lock for the current session userID gets set on the node. If there is already a lock by another
     * user a IllegalStateException is thrown,
     */
    public void acquireLock(final Node node) throws RepositoryException {
        if (hasSelfOrDescendantLockBySomeOneElse(node)) {
            throw new IllegalStateException("Node '"+node.getPath()+"' cannot be locked due to someone else who " +
                    "has the lock (possibly a descendant that is locked).");
        }
        String selfOrAncestorLockedBy = getSelfOrAncestorLockedBy(node);
        if (selfOrAncestorLockedBy != null) {
            if (selfOrAncestorLockedBy.equals(node.getSession().getUserID())) {
                log.debug("Node '{}' already locked", node.getSession().getUserID());
                return;
            }
            throw new IllegalStateException("Node '"+node.getPath()+"' cannot be locked due to someone else who " +
                    "has the lock (possibly an ancestor that is locked).");
        }
        doLock(node);
    }

    /**
     * @see {@link #acquireLock(javax.jcr.Node)} only for the acquireSimpleLock there is no need to check descendant or
     * ancestors. It is only the node itself that needs to be checked. This is for example the case for sitemenu nodes
     * where there is no fine-grained locking for the items below it
     */
    public void acquireSimpleLock(final Node node) throws RepositoryException {
        if (hasLockBySomeOneElse(node)) {
            throw new IllegalStateException("Node '"+node.getPath()+"' cannot be locked due to someone else who " +
                    "has the lock.");
        }
        doLock(node);
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


    protected void unlock(final Node workspaceNode) throws RepositoryException {
        if (workspaceNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE)) {
            log.warn("Removing lock for '{}' since ancestor gets published", workspaceNode.getPath());
            workspaceNode.removeMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        }
        for (Node child : new NodeIterable(workspaceNode.getNodes())) {
            unlock(child);
        }
    }

    protected void setProperty(final Node jcrNode, final String propName, final String propValue) throws RepositoryException {
        if (StringUtils.isEmpty(propValue)) {
            removeProperty(jcrNode, propName);
        } else {
            jcrNode.setProperty(propName, propValue);
        }
    }

    protected void setLocalParameters(final Node node, final Map<String, String> modifiedLocalParameters) throws RepositoryException {
        if (modifiedLocalParameters != null && !modifiedLocalParameters.isEmpty()) {
            final String[][] namesAndValues = mapToNameValueArrays(modifiedLocalParameters);
            node.setProperty(GENERAL_PROPERTY_PARAMETER_NAMES, namesAndValues[0], PropertyType.STRING);
            node.setProperty(GENERAL_PROPERTY_PARAMETER_VALUES, namesAndValues[1], PropertyType.STRING);
        } else if (modifiedLocalParameters != null && modifiedLocalParameters.isEmpty()) {
            removeProperty(node, GENERAL_PROPERTY_PARAMETER_NAMES);
            removeProperty(node, GENERAL_PROPERTY_PARAMETER_VALUES);
        }

    }

    protected void setRoles(final Node node, final Set<String> modifiedRoles) throws RepositoryException {
        if (modifiedRoles != null && !modifiedRoles.isEmpty()) {
            final String[] roles = Iterables.toArray(modifiedRoles, String.class);
            node.setProperty(SITEMENUITEM_PROPERTY_ROLES, roles, PropertyType.STRING);
        } else if (modifiedRoles != null && modifiedRoles.isEmpty()) {
            removeProperty(node, SITEMENUITEM_PROPERTY_ROLES);
        }
    }

    private String[][] mapToNameValueArrays(final Map<String, String> map) {
        final int size = map.size();
        final String[][] namesAndValues = {
                map.keySet().toArray(new String[size]),
                new String[size]
        };
        for (int i = 0; i < size; i++) {
            namesAndValues[1][i] = map.get(namesAndValues[0][i]);
        }
        return namesAndValues;
    }

    /**
     * returns the userID that contains the deep lock or <code>null</code> when no deep lock present
     */
    protected String getSelfOrAncestorLockedBy(final Node node) throws RepositoryException {
        if (node == null || node.isSame(node.getSession().getRootNode())) {
            return null;
        }

        if (node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            String lockedBy = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString();
            return lockedBy;
        }

        if (node.isNodeType(HstNodeTypes.NODETYPE_HST_WORKSPACE)) {
            return null;
        }
        return getSelfOrAncestorLockedBy(node.getParent());
    }

    protected boolean hasSelfOrAncestorLockBySomeOneElse(final Node node) throws RepositoryException {
        String selfOrAncestorLockedBy = getSelfOrAncestorLockedBy(node);
        if (selfOrAncestorLockedBy == null) {
            return false;
        }
        return !node.getSession().getUserID().equals(selfOrAncestorLockedBy);
    }

    protected boolean hasSelfOrDescendantLockBySomeOneElse(final Node node) throws RepositoryException {
        if (hasLockBySomeOneElse(node)) {
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

    protected boolean hasLockBySomeOneElse(final Node node) throws RepositoryException {
        if (node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            String lockedBy = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString();
            if (!lockedBy.equals(node.getSession().getUserID())) {
                return true;
            }
        }
        return false;
    }



    public void publishWorkspaceChanges(final List<String> userIds)  throws RepositoryException {
        List<Node> lockedNodeRoots = getLockedNodeRoots(userIds);
        publishWorkspaceNodeList(lockedNodeRoots);
    }

    protected void publishWorkspaceNodeList(final List<Node> lockedNodeRoots) throws RepositoryException {
        String liveConfigurationPath = pageComposerContextService.getEditingLiveSite().getConfigurationPath();
        String previewConfigurationPath = pageComposerContextService.getEditingPreviewSite().getConfigurationPath();
        final Session session = pageComposerContextService.getRequestContext().getSession();
        for (Node lockedNodeRoot : lockedNodeRoots) {
            String relPath = lockedNodeRoot.getPath().substring(previewConfigurationPath.length());

            if (session.nodeExists(liveConfigurationPath + relPath)) {
                session.removeItem(liveConfigurationPath + relPath);
            }
            if (lockedNodeRoot.hasProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE) &&
                    "deleted".equals(lockedNodeRoot.getProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE).getString())) {
                lockedNodeRoot.remove();
            } else {
                lockedNodeRoot.removeMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
                // we can only publish *IF* and only *IF* the parent exists. Otherwise we log an error and continue
                String liveParentRelPath = StringUtils.substringBeforeLast(relPath, "/");
                if (!session.nodeExists(liveConfigurationPath + liveParentRelPath)) {
                    log.warn("Cannot publish preview node '{}' because the live parent '{}' is missing. Skip publishing node",
                            lockedNodeRoot.getPath(), liveConfigurationPath + liveParentRelPath);
                } else {
                    log.info("Publishing '{}'", lockedNodeRoot.getPath());
                    JcrUtils.copy(session, lockedNodeRoot.getPath(), liveConfigurationPath + relPath);
                }
            }

        }
    }

    public void discardWorkspaceChanges(final List<String> userIds)  throws RepositoryException {
        List<Node> lockedNodeRoots = getLockedNodeRoots(userIds);
        discardWorkspaceNodeList(lockedNodeRoots);
    }

    protected void discardWorkspaceNodeList(final List<Node> lockedNodeRoots) throws RepositoryException {
        String liveConfigurationPath = pageComposerContextService.getEditingLiveSite().getConfigurationPath();
        String previewConfigurationPath = pageComposerContextService.getEditingPreviewSite().getConfigurationPath();
        final Session session = pageComposerContextService.getRequestContext().getSession();

        for (Node lockedNodeRoot : lockedNodeRoots) {
            final String lockedNodePath = lockedNodeRoot.getPath();
            String relPath = lockedNodePath.substring(previewConfigurationPath.length());

            if (!session.nodeExists(liveConfigurationPath + relPath)) {
                // there is no live version. Discard is thus removing the node
                lockedNodeRoot.remove();
                continue;
            }
            lockedNodeRoot.remove();
            JcrUtils.copy(session, liveConfigurationPath + relPath, lockedNodePath);
        }
    }

    protected List<Node> getLockedNodeRoots(final List<String> userIds) throws RepositoryException {

        String previewConfigurationPath = pageComposerContextService.getEditingPreviewSite().getConfigurationPath();
        final String previewWorkspacePath = previewConfigurationPath + "/"+HstNodeTypes.NODENAME_HST_WORKSPACE;
        final Session session = pageComposerContextService.getRequestContext().getSession();

        if (!session.nodeExists(previewWorkspacePath)) {
            // there is no preview workspace
            return Collections.emptyList();
        }

        List<Node> lockedNodes = findChangedWorkspaceNodesForUsers(previewWorkspacePath, userIds);
        if (lockedNodes.isEmpty()) {
            return Collections.emptyList();
        }

        final Node previewWorkspaceNode = session.getNode(previewWorkspacePath);

        // in principle, locked items should not contain descendant locked items, however, in a clustered or highly
        // concurrent environment, this is possible. Hence extra checks here

        Set<String> lockedNodePaths = new HashSet<>();
        for (Node lockedNode : lockedNodes) {
            if (!lockedNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE)) {
                throw new IllegalStateException("locked '" + lockedNode.getPath() + "' does not have mixin '" +
                        "" + HstNodeTypes.MIXINTYPE_HST_EDITABLE + "'. Should not happen");

            }
            lockedNodePaths.add(lockedNode.getPath());
        }

        List<Node> lockedNodeRoots = new ArrayList<>();

        for (Node lockedNode : lockedNodes) {
            Node ancestor = lockedNode.getParent();
            while (!ancestor.isSame(previewWorkspaceNode)) {
                if (lockedNodePaths.contains(ancestor.getPath())) {
                    // an ancestor is getting published already due to lock. Normally does not happen. Just remove lock
                    log.info("Removing double lock of '{}' since ancestor already has lock '{}'", lockedNode.getPath(),
                            ancestor.getPath());
                    lockedNode.removeMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
                    break;
                }
                ancestor = ancestor.getParent();
            }

            if (lockedNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE)) {
                // the mixin is not removed above
                lockedNodeRoots.add(lockedNode);
            }
        }

        // now for all lockedNodeRoots we need to check that there are no descendant locks contained by other users than
        // for userIds. Again, normally does not happen, but because of clustered setups / concurrency this state might happen
        for (Node lockedNodeRoot : lockedNodeRoots) {
            for (Node child : new NodeIterable(lockedNodeRoot.getNodes())) {
                unlock(child);
            }
        }
        return lockedNodeRoots;
    }

    protected List<Node> findChangedWorkspaceNodesForUsers(final String previewWorkspacePath, final List<String> userIds)
            throws RepositoryException {
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        final Session session = pageComposerContextService.getRequestContext().getSession();
        final String xpath = buildXPathQueryLockedWorkspaceNodesForUsers(previewWorkspacePath, userIds);
        final QueryResult result = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH).execute();

        List<Node> lockedNodesForUsers = new ArrayList<>();
        for (Node lockedNodeForUsers : new NodeIterable(result.getNodes())){
            lockedNodesForUsers.add(lockedNodeForUsers);
        }
        return lockedNodesForUsers;
    }

    // to override for helpers that need to be able to publish
    protected String buildXPathQueryLockedWorkspaceNodesForUsers(final String previewWorkspacePath,
                                                                 final List<String> userIds) {
        throw new UnsupportedOperationException("buildXPathQueryLockedWorkspaceNodesForUsers not supported for: " +
                this.getClass().getName());
    }

}
