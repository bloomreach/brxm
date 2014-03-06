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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.util.ISO9075;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SiteMapHelper extends AbstractHelper {

    private static final Logger log = LoggerFactory.getLogger(SiteMapHelper.class);
    private static final String WORKSPACE_PATH_ELEMENT = "/" + HstNodeTypes.NODENAME_HST_WORKSPACE + "/";

    private PagesHelper pagesHelper;

    public void setPagesHelper(final PagesHelper pagesHelper) {
        this.pagesHelper = pagesHelper;
    }

    /**
     * @throws ClientException if not found
     */
    @SuppressWarnings("unchecked")
    @Override
    public HstSiteMapItem getConfigObject(final String itemId) {
        final HstSite editingPreviewSite = pageComposerContextService.getEditingPreviewSite();
        return getSiteMapItem(editingPreviewSite.getSiteMap(), itemId);
    }

    public void update(final SiteMapItemRepresentation siteMapItem) throws RepositoryException {
        HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        final Session session = requestContext.getSession();
        final String itemId = siteMapItem.getId();
        Node jcrNode = session.getNodeByIdentifier(itemId);

        lockHelper.acquireLock(jcrNode);

        final String modifiedName = siteMapItem.getName();
        if (modifiedName != null && !modifiedName.equals(jcrNode.getName())) {
            // we do not need to check lock for parent as this is a rename within same parent
            String oldLocation = jcrNode.getPath();
            String target = jcrNode.getParent().getPath() + "/" + modifiedName;
            validateTarget(session, target);
            session.move(jcrNode.getPath(), jcrNode.getParent().getPath() + "/" + modifiedName);
            createMarkedDeletedIfLiveExists(session, oldLocation);
        }

        setSitemapItemProperties(siteMapItem, jcrNode);

        final Map<String, String> modifiedLocalParameters = siteMapItem.getLocalParameters();
        setLocalParameters(jcrNode, modifiedLocalParameters);

        final Set<String> modifiedRoles = siteMapItem.getRoles();
        setRoles(jcrNode, modifiedRoles);
    }


    public Node create(final SiteMapItemRepresentation siteMapItem, final String parentId) throws RepositoryException {

        HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        final Session session = requestContext.getSession();
        Node parent = session.getNodeByIdentifier(parentId);

        validateTarget(session, parent.getPath() + "/" + siteMapItem.getName());

        final Node newSitemapNode = parent.addNode(siteMapItem.getName(), HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
        lockHelper.acquireLock(newSitemapNode);

        setSitemapItemProperties(siteMapItem, newSitemapNode);
        // clone page definition

        final Node prototypePage = session.getNodeByIdentifier(siteMapItem.getComponentConfigurationId());
        Node newPage = pagesHelper.create(prototypePage, newSitemapNode);

        newSitemapNode.setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID,
                HstNodeTypes.NODENAME_HST_PAGES + "/" + newPage.getName());

        final Map<String, String> modifiedLocalParameters = siteMapItem.getLocalParameters();
        setLocalParameters(newSitemapNode, modifiedLocalParameters);

        final Set<String> modifiedRoles = siteMapItem.getRoles();
        setRoles(newSitemapNode, modifiedRoles);
        return newSitemapNode;
    }

    public void move(final String id, final String parentId) throws RepositoryException {
        if (id.equals(parentId)) {
            final String message = "Cannot move node to become child of itself";
            throw new ClientException(message, ClientError.INVALID_MOVE_TO_SELF);
        }
        HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        final Session session = requestContext.getSession();
        Node nodeToMove = session.getNodeByIdentifier(id);
        Node newParent = session.getNodeByIdentifier(parentId);
        Node oldParent = nodeToMove.getParent();
        if (oldParent.isSame(newParent)) {
            log.info("Move to same parent for '" + nodeToMove.getPath() + "' does not result in a real move");
            return;
        }
        final Node unLockableNode = lockHelper.getUnLockableNode(newParent, true, false);
        if (unLockableNode != null) {
            throw new IllegalStateException("Cannot move node to '" + newParent.getPath() + "' because that node is locked " +
                    "by node '" + unLockableNode.getPath() + "' by '" + unLockableNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString() + "'");
        }

        lockHelper.acquireLock(nodeToMove);
        String nodeName = nodeToMove.getName();
        validateTarget(session, newParent.getPath() + "/" + nodeName);
        String oldLocation = nodeToMove.getPath();
        session.move(oldParent.getPath() + "/" + nodeName, newParent.getPath() + "/" + nodeName);
        lockHelper.acquireLock(nodeToMove);

        createMarkedDeletedIfLiveExists(session, oldLocation);
    }

    public void delete(final String id) throws RepositoryException {
        HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        final Session session = requestContext.getSession();
        Node sitemapItemNodeToDelete = session.getNodeByIdentifier(id);
        lockHelper.acquireLock(sitemapItemNodeToDelete);
        pagesHelper.delete(sitemapItemNodeToDelete);
        deleteOrMarkDeletedIfLiveExists(sitemapItemNodeToDelete);
    }

    /**
     * @throws ClientException if not found
     */
    public static HstSiteMapItem getSiteMapItem(HstSiteMap siteMap, String siteMapItemId) {

        for (HstSiteMapItem hstSiteMapItem : siteMap.getSiteMapItems()) {
            final HstSiteMapItem siteMapItem = getSiteMapItem(hstSiteMapItem, siteMapItemId);
            if (siteMapItem != null) {
                return siteMapItem;
            }
        }

        final String message = String.format("SiteMap item with id '%s' is not part of currently edited preview site.", siteMapItemId);
        throw new ClientException(message, ClientError.ITEM_NOT_IN_PREVIEW);
    }

    public static HstSiteMapItem getSiteMapItem(HstSiteMapItem siteMapItem, String siteMapItemId) {
        if (!(siteMapItem instanceof CanonicalInfo)) {
            return null;
        }
        if (((CanonicalInfo) siteMapItem).getCanonicalIdentifier().equals(siteMapItemId)) {
            return siteMapItem;
        }
        for (HstSiteMapItem child : siteMapItem.getChildren()) {
            HstSiteMapItem o = getSiteMapItem(child, siteMapItemId);
            if (o != null) {
                return o;
            }
        }
        return null;
    }

    private void setSitemapItemProperties(final SiteMapItemRepresentation siteMapItem, final Node jcrNode) throws RepositoryException {
        setProperty(jcrNode, HstNodeTypes.SITEMAPITEM_PROPERTY_SCHEME, siteMapItem.getScheme());
        setProperty(jcrNode, HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH, siteMapItem.getRelativeContentPath());
    }




    private void validateTarget(final Session session, final String target) throws RepositoryException {
        // check non workspace sitemap for collisions
        final HstSiteMap siteMap = pageComposerContextService.getEditingPreviewSite().getSiteMap();
        if (!(siteMap instanceof CanonicalInfo)) {
            throw new IllegalStateException("Unexpected sitemap for site '" + siteMap.getSite().getName() + "' because not an instanceof CanonicalInfo");
        }
        if (!target.contains(WORKSPACE_PATH_ELEMENT)) {
            throw new IllegalArgumentException("Target '" + target + "' expected to at least contain ");
        }
        if (session.nodeExists(target)) {
            Node targetNode = session.getNode(target);
            if (isMarkedDeleted(targetNode)) {
                // see if we own the lock
                lockHelper.acquireLock(targetNode);
                targetNode.remove();
            } else {
                final String message = String.format("Target node '%s' already exists", targetNode.getPath());
                throw new ClientException(message, ClientError.ITEM_NAME_NOT_UNIQUE);
            }
        }

        final CanonicalInfo canonical = (CanonicalInfo) siteMap;
        if (canonical.isWorkspaceConfiguration()) {
            // the hst:sitemap node is from workspace so there is no non workspace sitemap for current site (inherited one
            // does not have precendence)
            return;
        } else {
            // non workspace sitemap
            final Node siteMapNode = session.getNodeByIdentifier(canonical.getCanonicalIdentifier());
            final Node siteNode = siteMapNode.getParent();
            if (!siteNode.isNodeType(HstNodeTypes.NODETYPE_HST_CONFIGURATION)) {
                throw new IllegalStateException("Expected node type '" + HstNodeTypes.NODETYPE_HST_CONFIGURATION + "' for " +
                        "'" + siteNode.getPath() + "' but was '" + siteNode.getPrimaryNodeType().getName() + "'.");
            }
            if (!target.startsWith(siteNode.getPath() + "/")) {
                throw new IllegalArgumentException("Target '" + target + "' does not start with the path of the " +
                        "targeted hst site '" + siteMapNode.getPath() + "'.");
            }
            // check whether non workspace sitemap does not already contain the target without /hst:workspace/ part
            String nonWorkspaceTarget = target.replace(WORKSPACE_PATH_ELEMENT, "/");
            // now we have a path like /hst:hst/hst:configurations/myproject/hst:sitemap/foo/bar/lux
            // we need to make sure 'foo' does not already exist
            String siteMapRelPath = nonWorkspaceTarget.substring(siteMapNode.getPath().length() + 1);
            String[] segments = siteMapRelPath.split("/");
            if (siteMapNode.hasNode(segments[0])) {
                final String message = String.format("Target '%s' not allowed since the *non-workspace* sitemap already contains '%s'", target, siteMapNode.getPath() + "/" + segments[0]);
                throw new ClientException(message, ClientError.ITEM_EXISTS_OUTSIDE_WORKSPACE);
            }
            // valid!
            return;
        }
    }

    @Override
    protected String buildXPathQueryLockedWorkspaceNodesForUsers(final String previewWorkspacePath,
                                                                 final List<String> userIds) {
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("List of user IDs cannot be empty");
        }

        StringBuilder xpath = new StringBuilder("/jcr:root");
        xpath.append(ISO9075.encodePath(previewWorkspacePath + "/" + HstNodeTypes.NODENAME_HST_SITEMAP));

        xpath.append("//element(*,");
        xpath.append(HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
        xpath.append(")[");

        String concat = "";
        for (String userId : userIds) {
            xpath.append(concat);
            xpath.append('@');
            xpath.append(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY);
            xpath.append(" = '");
            xpath.append(userId);
            xpath.append("'");
            concat = " or ";
        }
        xpath.append("]");

        return xpath.toString();
    }

}
