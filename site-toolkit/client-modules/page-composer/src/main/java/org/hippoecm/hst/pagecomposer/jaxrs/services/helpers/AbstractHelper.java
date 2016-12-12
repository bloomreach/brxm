/*
 * Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import com.google.common.collect.Iterables;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
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

    public abstract <T> T getConfigObject(final String itemId, final Mount mount);

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
        String liveConfigurationPath = pageComposerContextService.getEditingLiveConfigurationPath();
        String previewConfigurationPath = pageComposerContextService.getEditingPreviewConfigurationPath();
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
                    Node copy = JcrUtils.copy(session, lockedNode.getPath(), liveConfigurationPath + relPath);
                    reorderCopyIfNeeded(lockedNode, copy);
                }
            }

        }
    }

    /**
     * Reorder the {@code copy} ot be in the same location as {@code source} with respect to their parents in case the
     * parents are orderable.
     * @param source
     * @param copy
     * @throws RepositoryException
     */
    private void reorderCopyIfNeeded(final Node source, final Node copy) throws RepositoryException {

        if (!source.getParent().getPrimaryNodeType().hasOrderableChildNodes()) {
            return;
        }

        Node parentOfCopy = copy.getParent();

        if (!parentOfCopy.getPrimaryNodeType().hasOrderableChildNodes()) {
            return;
        }

        // reorder the copied source if needed and if the parent is orderable
        // find the next sibling of 'source' : We need to try to order the 'copy' before the
        // same node as the source in preview. In case for some reason the next sibling does not
        // exist in live, we need to catch the exception and log an error (because it indicates a
        // live and preview that is out of sync)
        Node nextSibling = nextSibling(source);

        if (nextSibling != null) {
            try {
                // HST nodes do not allow same name siblings so we do not take into account the index
                // if nextSibling is null, the copy will just be at the end of the list which is fine
                parentOfCopy.orderBefore(copy.getName(), nextSibling.getName());
            } catch (ItemNotFoundException e) {
                log.error("Cannot reorder '{}' before '{}' because a node with the name '{}' does " +
                        "not exist in the live configuration which should not be the case because preview " +
                        "and live configuration are out of sync. The preview configuration node that misses " +
                        "in live is '{}'." +
                        "", copy.getPath(), nextSibling.getName(), nextSibling.getPath());
            }
        }
    }

    /**
     * <p>
     * Returns the next sibling {@link Node} of {@code current}. If there is no next sibling it returns {@code null}.
     * If
     * the
     * parent {@link Node} of {@code current} is not an orderable {@link Node}, an {@link
     * UnsupportedRepositoryOperationException}
     * will be thrown. Checking whether the parent is orderable can be done easily as follows
     * <pre>
     *         Node parent = current.getParent();
     *         parent.getPrimaryNodeType().hasOrderableChildNodes()
     *     </pre>
     * </p>
     * <p>
     * If the caller of this method uses this method to reorder nodes, then take into account whether the
     * parent node allows same name siblings or not. If you do not know, use
     * <pre>
     *         Node parent = current.getParent();
     *         if (parent.getDefinition().allowsSameNameSiblings()) {
     *             Node next = nextSibling(current);
     *             parent.orderBefore(current.getName() + "[" + current.getIndex() + "]",
     *                                next.getName()+ "[" + next.getIndex() + "]");
     *         } else {
     *            parent.orderBefore(current.getName(), nextSibling(current).getName());
     *         }
     *     </pre>
     * </p>
     *
     * @param current the {@link Node} for which to find the next sibling
     * @return the next sibling of current if there is a next one and {@code null} in case there is no next sibling
     * @throws UnsupportedRepositoryOperationException if the parent of {@code current} is not orderable
     * @throws RepositoryException                     if some repository exception happens
     */
    private Node nextSibling(final Node current) throws RepositoryException {
        Node parent = current.getParent();
        if (!parent.getPrimaryNodeType().hasOrderableChildNodes()) {
            throw new UnsupportedRepositoryOperationException(String.format("Node '%s' does not have an orderable parent " +
                    "hence invoking #nextSibling does not make sense."));
        }

        NodeIterator nodeIterator = parent.getNodes();
        while (nodeIterator.hasNext()) {
            Node sibling = nodeIterator.nextNode();
            if (sibling.isSame(current)) {
                return nodeIterator.hasNext() ? nodeIterator.nextNode() : null;
            }
        }
        throw new ItemNotFoundException("The 'current' item does not longer exist below its parent");
    }

    public void discardChanges(final List<String> userIds) throws RepositoryException {
        List<Node> lockedNodes = getLockedNodeRoots(userIds);
        discardNodeList(lockedNodes);
    }

    protected void discardNodeList(final List<Node> lockedNodeRoots) throws RepositoryException {
        String liveConfigurationPath = pageComposerContextService.getEditingLiveConfigurationPath();
        String previewConfigurationPath = pageComposerContextService.getEditingPreviewConfigurationPath();
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
            reorderCopyIfNeeded(session.getNode(liveConfigurationPath + relPath), session.getNode(lockedNodePath));
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

    protected final void createMarkedDeletedIfLiveExists(final Session session, final String oldLocation) throws RepositoryException {
        if (liveExists(session, oldLocation)) {
            Node deleted = session.getRootNode().addNode(oldLocation.substring(1), HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
            markDeleted(deleted);
        }
    }

    protected final void deleteOrMarkDeletedIfLiveExists(final Node toDelete) throws RepositoryException {
        if (liveExists(toDelete.getSession(), toDelete.getPath())) {
            markDeleted(toDelete);
        } else {
            toDelete.remove();
        }
    }

    protected final boolean liveExists(final Session session, final String previewLocation) throws RepositoryException {
        final String workspace = "/hst:workspace/";
        final String previewWorkspace = "-preview" + workspace;
        if (!previewLocation.contains(previewWorkspace)) {
            throw new IllegalStateException("Unexpected location '" + previewLocation + "'");
        }
        final String liveLocation = previewLocation.replace(previewWorkspace, workspace);
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

    protected String getPreviewConfigurationPath() {
        return pageComposerContextService.getEditingPreviewConfigurationPath();
    }

    protected String getPreviewWorkspacePath() {
        return getPreviewConfigurationPath() + "/" + NODENAME_HST_WORKSPACE;
    }

    protected String getWorkspacePath(final Mount mount) {
        return mount.getHstSite().getConfigurationPath() + "/" + NODENAME_HST_WORKSPACE;
    }

}
