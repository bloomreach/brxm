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
import java.util.Collections;
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
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_ROLES;

public abstract class AbstractHelper {

    private static final Logger log = LoggerFactory.getLogger(AbstractHelper.class);

    protected LockHelper lockHelper = new LockHelper();
    protected PageComposerContextService pageComposerContextService;

    public void setPageComposerContextService(final PageComposerContextService pageComposerContextService) {
        this.pageComposerContextService = pageComposerContextService;
    }

    void setLockHelper(LockHelper lockHelper) {
        this.lockHelper = lockHelper;
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

    protected void setProperty(final Node jcrNode, final String propName, final String propValue) throws RepositoryException {
        if (StringUtils.isEmpty(propValue)) {
            removeProperty(jcrNode, propName);
        } else {
            jcrNode.setProperty(propName, propValue);
        }
    }

    protected void setLocalParameters(final Node node, final Map<String, String> modifiedLocalParameters) throws RepositoryException {
        if (modifiedLocalParameters == null) {
            return;
        }
        if (modifiedLocalParameters.isEmpty()) {
            removeProperty(node, GENERAL_PROPERTY_PARAMETER_NAMES);
            removeProperty(node, GENERAL_PROPERTY_PARAMETER_VALUES);
        } else {
            final String[][] namesAndValues = mapToNameValueArrays(modifiedLocalParameters);
            node.setProperty(GENERAL_PROPERTY_PARAMETER_NAMES, namesAndValues[0], PropertyType.STRING);
            node.setProperty(GENERAL_PROPERTY_PARAMETER_VALUES, namesAndValues[1], PropertyType.STRING);
        }
    }

    protected void setRoles(final Node node, final Set<String> modifiedRoles) throws RepositoryException {
        if (modifiedRoles == null) {
            return;
        }
        if (modifiedRoles.isEmpty()) {
            removeProperty(node, SITEMENUITEM_PROPERTY_ROLES);
        } else {
            final String[] roles = Iterables.toArray(modifiedRoles, String.class);
            node.setProperty(SITEMENUITEM_PROPERTY_ROLES, roles, PropertyType.STRING);
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

    public void publishChanges(final List<String> userIds) throws RepositoryException {
        List<Node> lockedNodes = getLockedNodeRoots(userIds);
        publishNodeList(lockedNodes);
    }

    protected void publishNodeList(final List<Node> lockedNodes) throws RepositoryException {
        String liveConfigurationPath = pageComposerContextService.getEditingLiveSite().getConfigurationPath();
        String previewConfigurationPath = pageComposerContextService.getEditingPreviewSite().getConfigurationPath();
        final Session session = pageComposerContextService.getRequestContext().getSession();
        for (Node lockedNode : lockedNodes) {
            String relPath = lockedNode.getPath().substring(previewConfigurationPath.length());

            if (session.nodeExists(liveConfigurationPath + relPath)) {
                session.removeItem(liveConfigurationPath + relPath);
            }
            if (lockedNode.hasProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE) &&
                    "deleted".equals(lockedNode.getProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE).getString())) {
                lockedNode.remove();
            } else {
                lockHelper.unlock(lockedNode);
                // we can only publish *IF* and only *IF* the parent exists. Otherwise we log an error and continue
                String liveParentRelPath = StringUtils.substringBeforeLast(relPath, "/");
                if (!session.nodeExists(liveConfigurationPath + liveParentRelPath)) {
                    log.warn("Cannot publish preview node '{}' because the live parent '{}' is missing. Skip publishing node",
                            lockedNode.getPath(), liveConfigurationPath + liveParentRelPath);
                } else {
                    log.info("Publishing '{}'", lockedNode.getPath());
                    JcrUtils.copy(session, lockedNode.getPath(), liveConfigurationPath + relPath);
                }
            }

        }
    }

    public void discardChanges(final List<String> userIds) throws RepositoryException {
        List<Node> lockedNodes = getLockedNodeRoots(userIds);
        discardNodeList(lockedNodes);
    }

    protected void discardNodeList(final List<Node> lockedNodeRoots) throws RepositoryException {
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
        final Session session = pageComposerContextService.getRequestContext().getSession();

        if (!session.nodeExists(previewConfigurationPath)) {
            // there is no preview workspace
            return Collections.emptyList();
        }

        List<Node> lockedNodes = findLockedNodesForUsers(previewConfigurationPath, userIds);
        if (lockedNodes.isEmpty()) {
            return Collections.emptyList();
        }

        final Node previewConfigurationNode = session.getNode(previewConfigurationPath);
        List<Node> lockedNodeRoots = new ArrayList<>();

        for (Node lockedNode : lockedNodes) {
            if (!lockedNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
                // already processed for this session but not yet persisted (hence still in search result)
                continue;
            }
            if (!containsAncestorLock(lockedNode, previewConfigurationNode)) {
               lockedNodeRoots.add(lockedNode);
            } else {
                // possibly incorrect lock because ancestor locked by someone else.
                // to be sure, unlock 'lockedNode'. If the ancestor is locked by current user, the ancestor
                // will be published
                lockHelper.unlock(lockedNode);
            }
        }
        return lockedNodeRoots;
    }


    protected boolean containsAncestorLock(final Node lockedNode, final Node previewConfigurationNode) throws RepositoryException {
        Node ancestor = lockedNode.getParent();
        while (!ancestor.isSame(previewConfigurationNode)) {
            if (ancestor.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
                log.info("Ancestor '{}' already contains a lock.", ancestor.getPath());
                return true;
            }
            ancestor = ancestor.getParent();
        }
        return false;
    }

    protected void createMarkedDeletedIfLiveExists(final Session session, final String oldLocation) throws RepositoryException {
        boolean liveExists = liveExists(session, oldLocation);
        if (liveExists) {
            Node deleted = session.getRootNode().addNode(oldLocation.substring(1), HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
            markDeleted(deleted);
        }
    }

    protected void deleteOrMarkDeletedIfLiveExists(final Node toDelete) throws RepositoryException {
        boolean liveExists = liveExists(toDelete.getSession(), toDelete.getPath());
        if (liveExists) {
            markDeleted(toDelete);
        } else {
            toDelete.remove();
        }
    }

    protected boolean liveExists(final Session session, final String previewLocation) throws RepositoryException {
        if (!previewLocation.contains("-preview/hst:workspace/")) {
            throw new IllegalStateException("Unexpected location '" + previewLocation + "'");
        }
        String liveLocation = previewLocation.replace("-preview/hst:workspace/", "/hst:workspace/");
        return session.nodeExists(liveLocation);
    }

    protected List<Node> findLockedNodesForUsers(final String previewConfigurationPath, final List<String> userIds)
            throws RepositoryException {
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        final Session session = pageComposerContextService.getRequestContext().getSession();
        final String xpath = buildXPathQueryLockedNodesForUsers(previewConfigurationPath, userIds);
        final QueryResult result = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH).execute();

        List<Node> lockedNodesForUsers = new ArrayList<>();
        for (Node lockedNodeForUsers : new NodeIterable(result.getNodes())) {
            lockedNodesForUsers.add(lockedNodeForUsers);
        }
        return lockedNodesForUsers;
    }

    protected String getPreviewWorkspacePath() {
        return pageComposerContextService.getEditingPreviewSite().getConfigurationPath() + "/" + NODENAME_HST_WORKSPACE;
    }


    // to override for helpers that need to be able to publish/discard
    protected String buildXPathQueryLockedNodesForUsers(final String previewConfigurationPath,
                                                        final List<String> userIds) {
        throw new UnsupportedOperationException("buildXPathQueryLockedNodesForUsers not supported for: " +
                this.getClass().getName());
    }

    protected void markDeleted(final Node deleted) throws RepositoryException {
        lockHelper.acquireLock(deleted, 0);
        deleted.setProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE, "deleted");
        // and remove directly descendants
        for (Node child : new NodeIterable(deleted.getNodes())) {
            child.remove();
        }
    }

    protected boolean isMarkedDeleted(final Node node) throws RepositoryException {
        return "deleted".equals(JcrUtils.getStringProperty(node, HstNodeTypes.EDITABLE_PROPERTY_STATE, null));
    }

}
