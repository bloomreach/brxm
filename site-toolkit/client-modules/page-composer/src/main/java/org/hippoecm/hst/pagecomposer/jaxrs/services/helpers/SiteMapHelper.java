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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.util.ISO9075;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageCopyContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.util.HstSiteMapUtils;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_PAGES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_SITEMAP;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_SITEMAPITEM;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID;

public class SiteMapHelper extends AbstractHelper {

    private static final Logger log = LoggerFactory.getLogger(SiteMapHelper.class);
    private static final String WORKSPACE_PATH_ELEMENT = "/" + NODENAME_HST_WORKSPACE + "/";

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

    /**
     * @throws ClientException if not found
     */
    @SuppressWarnings("unchecked")
    public HstSiteMapItem getConfigObject(final String itemId, final Mount mount) {
        final HstSite editingPreviewSite = mount.getHstSite();
        return getSiteMapItem(editingPreviewSite.getSiteMap(), itemId);
    }

    public void update(final SiteMapItemRepresentation siteMapItem, final boolean reApplyPrototype) throws RepositoryException {
        HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        final Session session = requestContext.getSession();
        final String itemId = siteMapItem.getId();
        Node siteMapItemNode = session.getNodeByIdentifier(itemId);

        lockHelper.acquireLock(siteMapItemNode, 0);

        final String modifiedName = NodeNameCodec.encode(siteMapItem.getName());
        if (modifiedName != null && !modifiedName.equals(siteMapItemNode.getName())) {
            // we do not need to check lock for parent as this is a rename within same parent
            String oldLocation = siteMapItemNode.getPath();
            String target = siteMapItemNode.getParent().getPath() + "/" + modifiedName;
            validateTarget(session, target, pageComposerContextService.getEditingPreviewSite().getSiteMap());
            session.move(siteMapItemNode.getPath(), siteMapItemNode.getParent().getPath() + "/" + modifiedName);
            createMarkedDeletedIfLiveExists(session, oldLocation);
        }

        if (reApplyPrototype) {
            final Node newPrototypePage = session.getNodeByIdentifier(siteMapItem.getComponentConfigurationId());
            final String targetPageNodeName = getSiteMapPathPrefixPart(siteMapItemNode) + "-" + newPrototypePage.getName();

            final HstComponentConfiguration existingPageConfig = getHstComponentConfiguration(siteMapItemNode);

            Node updatedPage;
            if (existingPageConfig == null) {
                log.warn("Unexpected update with re-apply prototype of sitemap item '{}' because there is no" +
                        " existing page in workspace for sitemap item. Instead of re-apply prototype, just create page" +
                        " from prototype", siteMapItemNode.getPath());

                updatedPage = pagesHelper.create(newPrototypePage, targetPageNodeName);
            } else {
                Node existingPage = session.getNodeByIdentifier(existingPageConfig.getCanonicalIdentifier());
                if (existingPage.getPath().startsWith(getPreviewWorkspacePath())) {
                    // Really re-apply a prototype now
                    updatedPage = pagesHelper.reapply(newPrototypePage, existingPage, targetPageNodeName);
                } else {
                    log.warn("Unexpected update with re-apply prototype of sitemap item '{}' because existing page '{}'" +
                            " is not stored in workspace. Instead of re-apply prototype, just create page" +
                            " from prototype", siteMapItemNode.getPath(), existingPage.getPath());
                    updatedPage = pagesHelper.create(newPrototypePage, targetPageNodeName);
                }
            }

            if (updatedPage == null) {
                throw new ClientException("Failed to re-apply prototype", ClientError.UNKNOWN);
            }
            siteMapItemNode.setProperty(SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID,
                    NODENAME_HST_PAGES + "/" + updatedPage.getName());

        }

        setSitemapItemProperties(siteMapItem, siteMapItemNode);

        final Map<String, String> modifiedLocalParameters = siteMapItem.getLocalParameters();
        setLocalParameters(siteMapItemNode, modifiedLocalParameters);

        final Set<String> modifiedRoles = siteMapItem.getRoles();
        setRoles(siteMapItemNode, modifiedRoles);
    }

    private HstComponentConfiguration getHstComponentConfiguration(final Node siteMapItemNode) throws RepositoryException {
        // comp id is not uuid but in general something like hst:pages/myPageNodeName
        final String componentId =
                JcrUtils.getStringProperty(siteMapItemNode, SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID, null);

        if (componentId == null) {
            return null;
        } else {
            return pageComposerContextService.getEditingPreviewSite()
                    .getComponentsConfiguration().getComponentConfiguration(componentId);
        }
    }


    public Node create(final SiteMapItemRepresentation siteMapItem, final String parentId) throws RepositoryException {
        final String finalParentId;
        if (parentId == null) {
            finalParentId = getWorkspaceSiteMapId();
        } else {
            finalParentId = parentId;
        }
        HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        final Session session = requestContext.getSession();
        Node parent = session.getNodeByIdentifier(finalParentId);

        final String encodedName = NodeNameCodec.encode(siteMapItem.getName());
        validateTarget(session, parent.getPath() + "/" + encodedName, pageComposerContextService.getEditingPreviewSite().getSiteMap());

        final Node newSitemapNode = parent.addNode(encodedName, NODETYPE_HST_SITEMAPITEM);
        lockHelper.acquireLock(newSitemapNode, 0);

        setSitemapItemProperties(siteMapItem, newSitemapNode);
        // clone page definition

        final Node prototypePage = session.getNodeByIdentifier(siteMapItem.getComponentConfigurationId());
        final String targetPageNodeName = getSiteMapPathPrefixPart(newSitemapNode) + "-" + prototypePage.getName();
        Node newPage = pagesHelper.create(prototypePage, targetPageNodeName);

        newSitemapNode.setProperty(SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID,
                NODENAME_HST_PAGES + "/" + newPage.getName());

        final Map<String, String> modifiedLocalParameters = siteMapItem.getLocalParameters();
        setLocalParameters(newSitemapNode, modifiedLocalParameters);

        final Set<String> modifiedRoles = siteMapItem.getRoles();
        setRoles(newSitemapNode, modifiedRoles);
        return newSitemapNode;
    }

    /**
     * utility method to create a <strong>shallow copy</strong> of {@code siteMapItemUUId} : Shallow copy means that
     * child pages of {@code siteMapItemUUId} are not copied. The copy will be a sibbling of <code>siteMapItemUUId</code>
     */
    public Node duplicate(final String siteMapItemUUId) throws RepositoryException {
        HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        HstSiteMapItem hstSiteMapItem = getConfigObject(siteMapItemUUId);
        if (hstSiteMapItem == null) {
            String message = String.format("Cannot duplicate because there is no siteMapItem for id '%s'.", siteMapItemUUId);
            throw new ClientException(message, ClientError.ITEM_CANNOT_BE_CLONED);
        }

        final Session session = requestContext.getSession();
        final String postfix = "-duplicate";
        String targetName = hstSiteMapItem.getValue() + postfix;

        final String parentPath;

        final String targetSiteMapItemUUID;
        final CanonicalInfo parentItem = (CanonicalInfo)hstSiteMapItem.getParentItem();
        if (parentItem == null) {
            parentPath = getPreviewWorkspacePath() + "/" + NODENAME_HST_SITEMAP;
            targetSiteMapItemUUID = null;
        } else {
            parentPath = parentItem.getCanonicalPath();
            targetSiteMapItemUUID = parentItem.getCanonicalIdentifier();
        }
        String nonWorkspaceParentPath = parentPath.replace("/" + NODENAME_HST_WORKSPACE + "/", "/");
        int counter = 0;
        while (session.nodeExists(parentPath + "/" + targetName) || session.nodeExists(nonWorkspaceParentPath + "/" + targetName)) {
            counter++;
            targetName = hstSiteMapItem.getValue() + "-" + counter + postfix;
        }

        PageCopyContext pcc = copy(pageComposerContextService.getEditingMount().getIdentifier(),
                siteMapItemUUId, targetSiteMapItemUUID, targetName);
        return pcc.getNewSiteMapNode();
    }

    /**
     * utility method to create a <strong>shallow copy</strong> of {@code siteMapItemId} : Shallow copy means that
     * child pages of {@code siteMapItemId} are not copied
     *
     *
     * @param mountId the target mount for the copy. If <code>null</code>, the current edited mount is used
     * @param copyFromSiteMapItemUUId       the uuid of the {@code siteMapItem} to copy, not
     * @param targetSiteMapItemUUID the uuid of the target siteMapItem, can be {@code null} in which case the same
     *                              location as {@code siteMapItem} will be used, only with name {@code targetName}
     * @param targetName            the name of the copy, not allowed to be {@code null}
     * @return the {@link javax.jcr.Node} of the created new siteMapItem
     * @throws RepositoryException
     */
    public PageCopyContext copy(final String mountId, final String copyFromSiteMapItemUUId, final String targetSiteMapItemUUID, final String targetName)
            throws RepositoryException {

        HstRequestContext requestContext = pageComposerContextService.getRequestContext();

        final Mount editingMount = pageComposerContextService.getEditingMount();
        validateSiteMapItem(copyFromSiteMapItemUUId, editingMount);
        final Mount targetMount;
        if (mountId == null) {
            targetMount = editingMount;
        } else {
            targetMount = requestContext.getVirtualHost().getVirtualHosts().getMountByIdentifier(mountId);
        }

        final String workspaceSiteMapPath = getWorkspacePath(targetMount) + "/" + NODENAME_HST_SITEMAP;

        final Session session = requestContext.getSession();
        if (!session.nodeExists(workspaceSiteMapPath)) {
            String message = String.format("No workspace found at '%s'", workspaceSiteMapPath);
            throw new ClientException(message, ClientError.ITEM_NOT_FOUND, Collections.singletonMap("errorReason", message));
        }

        if (targetSiteMapItemUUID != null) {
            validateSiteMapItem(targetSiteMapItemUUID, targetMount);
        }

        final Node workspaceSiteMapNode = session.getNode(workspaceSiteMapPath);

        final String target;
        if (targetSiteMapItemUUID != null) {
            target = session.getNodeByIdentifier(targetSiteMapItemUUID).getPath() + "/" + targetName;
        } else {
            target = workspaceSiteMapNode.getPath() + "/" + targetName;
        }
        String nonWorkspaceLocation = target.replace("/" + NODENAME_HST_WORKSPACE + "/", "/");

        if (session.nodeExists(target) || session.nodeExists(nonWorkspaceLocation)) {
            String message = String.format("Cannot copy because there is a siteMapItem with same name and " +
                    "location already located at '%s' or '%s'.", target, nonWorkspaceLocation);
            throw new ClientException(message, ClientError.ITEM_CANNOT_BE_CLONED, Collections.singletonMap("errorReason", message));
        }

        validateTarget(session, target, targetMount.getHstSite().getSiteMap());
        Node toShallowCopy = session.getNodeByIdentifier(copyFromSiteMapItemUUId);
        final Node newSiteMapNode = JcrUtils.copy(session, toShallowCopy.getPath(), target);
        for (Node child : new NodeIterable(newSiteMapNode.getNodes())) {
            // we need shallow copy so remove children again
            child.remove();
        }
        lockHelper.acquireLock(newSiteMapNode, 0);

        // copy the page definition
        // we need to find the page node uuid to copy through hstSiteMapItem.getComponentConfigurationId() which is NOT
        // the page UUID
        final HstSiteMapItem copyFromSiteMapItem = getConfigObject(copyFromSiteMapItemUUId);
        final HstComponentConfiguration pageToCopy = pageComposerContextService.getEditingPreviewSite().getComponentsConfiguration()
                .getComponentConfiguration(copyFromSiteMapItem.getComponentConfigurationId());

        if (pageToCopy == null) {
            final String message = "Cannot duplicate page since backing hst component configuration object not found";
            throw new ClientException(message,
                    ClientError.ITEM_CANNOT_BE_CLONED, Collections.singletonMap("errorReason", message));
        }

        String fromPathInfo = HstSiteMapUtils.getPath(copyFromSiteMapItem);
        String fromPageNodeNamePrefix = fromPathInfo.replace("/", "-");

        final String prefix = getPreviewConfigurationPath() + "/" + NODENAME_HST_PAGES + "/" + fromPageNodeNamePrefix + "-";
        String targetPageName;
        if (pageToCopy.getCanonicalStoredLocation().startsWith(prefix)){
            targetPageName = targetName + "-" + pageToCopy.getCanonicalStoredLocation().substring(prefix.length());
        } else {
            targetPageName = targetName;
        }
        if (targetSiteMapItemUUID != null) {
            HstSiteMapItem targetItem = getConfigObject(targetSiteMapItemUUID, targetMount);
            String targetPathInfo = HstSiteMapUtils.getPath(targetItem);
            targetPageName = targetPathInfo.replace("/", "-") + "-" + targetPageName;
        }

        Node clonedPage = pagesHelper.create(session.getNodeByIdentifier(pageToCopy.getCanonicalIdentifier()),
                targetPageName, pageToCopy, false, getWorkspacePath(targetMount) + "/" + NODENAME_HST_PAGES);
        newSiteMapNode.setProperty(SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID,
                NODENAME_HST_PAGES + "/" + clonedPage.getName());

        PageCopyContext pcc = new PageCopyContext(requestContext);
        pcc.setNewSiteMapNode(newSiteMapNode);
        return pcc;
    }

    private void validateSiteMapItem(final String siteMapItemUUId, final Mount targetMount) {
        HstSiteMapItem hstSiteMapItem = getConfigObject(siteMapItemUUId, targetMount);
        if (hstSiteMapItem == null) {
            String message = String.format("Cannot copy because there is no siteMapItem for id '%s'.", siteMapItemUUId);
            throw new ClientException(message, ClientError.ITEM_CANNOT_BE_CLONED, Collections.singletonMap("errorReason", message));
        }
        String pathInfo = HstSiteMapUtils.getPath(hstSiteMapItem);
        if (pathInfo.contains(HstNodeTypes.WILDCARD) || pathInfo.contains(HstNodeTypes.ANY)) {
            String message = String.format("Cannot copy a page for siteMapItem '%s' because it contains " +
                    "wildcards and this is not supported.", ((CanonicalInfo) hstSiteMapItem).getCanonicalPath());
            throw new ClientException(message, ClientError.ITEM_CANNOT_BE_CLONED, Collections.singletonMap("errorReason", message));
        }
    }

    public void move(final String id, final String parentId) throws RepositoryException {
        final String finalParentId;
        if (parentId == null) {
            finalParentId = getWorkspaceSiteMapId();
        } else {
            finalParentId = parentId;
        }
        if (id.equals(finalParentId)) {
            final String message = "Cannot move node to become child of itself";
            throw new ClientException(message, ClientError.INVALID_MOVE_TO_SELF);
        }
        HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        final Session session = requestContext.getSession();
        Node nodeToMove = session.getNodeByIdentifier(id);
        Node newParent = session.getNodeByIdentifier(finalParentId);
        Node oldParent = nodeToMove.getParent();
        if (oldParent.isSame(newParent)) {
            log.info("Move to same parent for '" + nodeToMove.getPath() + "' does not result in a real move");
            return;
        }
        final Node unLockableNode = lockHelper.getUnLockableNode(newParent, true, false);
        if (unLockableNode != null) {
            String message = String.format("Cannot move node to '%s' because that node is locked through node '%s' by '%s'",
                    newParent.getPath(), unLockableNode.getPath(), unLockableNode.getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
            throw new ClientException(message, ClientError.ITEM_ALREADY_LOCKED);
        }

        lockHelper.acquireLock(nodeToMove, 0);
        String nodeName = nodeToMove.getName();
        validateTarget(session, newParent.getPath() + "/" + nodeName, pageComposerContextService.getEditingPreviewSite().getSiteMap());
        String oldLocation = nodeToMove.getPath();
        session.move(oldParent.getPath() + "/" + nodeName, newParent.getPath() + "/" + nodeName);
        lockHelper.acquireLock(nodeToMove, 0);

        createMarkedDeletedIfLiveExists(session, oldLocation);
    }

    public void delete(final String id) throws RepositoryException {
        HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        final Session session = requestContext.getSession();
        Node sitemapItemNodeToDelete = session.getNodeByIdentifier(id);
        lockHelper.acquireLock(sitemapItemNodeToDelete, 0);
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
        if (siteMapItem.getScheme() != null) {
            setProperty(jcrNode, HstNodeTypes.SITEMAPITEM_PROPERTY_SCHEME, siteMapItem.getScheme());
        }
        if (siteMapItem.getPrimaryDocumentRepresentation() != null && siteMapItem.getPrimaryDocumentRepresentation().getPath() != null) {
            final String absPath = siteMapItem.getPrimaryDocumentRepresentation().getPath();
            final String rootContentPath = pageComposerContextService.getEditingMount().getContentPath();
            if (absPath.startsWith(rootContentPath + "/")) {
                setProperty(jcrNode, HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH, absPath.substring(rootContentPath.length() + 1));
            } else {
                log.info("Cannot set '{}' for relative content path because does not start with root channel content path '{}'",
                        absPath, rootContentPath + "/");
            }
        } else if (siteMapItem.getRelativeContentPath() != null) {
            setProperty(jcrNode, HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH, siteMapItem.getRelativeContentPath());
        }

        if (siteMapItem.getPageTitle() != null) {
            setProperty(jcrNode, HstNodeTypes.SITEMAPITEM_PAGE_TITLE, siteMapItem.getPageTitle());
        }
    }


    private void validateTarget(final Session session, final String target, final HstSiteMap siteMap) throws RepositoryException {
        // check non workspace sitemap for collisions
        if (!(siteMap instanceof CanonicalInfo)) {
            String msg = String.format("Unexpected sitemap for site '%s' because not an instanceof CanonicalInfo", siteMap.getSite().getName());
            throw new ClientException(msg, ClientError.UNKNOWN);
        }
        if (!target.contains(WORKSPACE_PATH_ELEMENT)) {
            String msg = String.format("Target '%s' does not contain '%s'.", target, WORKSPACE_PATH_ELEMENT);
            throw new ClientException(msg, ClientError.ITEM_NOT_CORRECT_LOCATION);
        }
        if (session.nodeExists(target)) {
            Node targetNode = session.getNode(target);
            if (isMarkedDeleted(targetNode)) {
                // see if we own the lock
                lockHelper.acquireLock(targetNode, 0);
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
                String msg = String.format("Expected node type '%s' for '%s' but was '%s'.",
                        HstNodeTypes.NODETYPE_HST_CONFIGURATION, siteNode.getPath(), siteNode.getPrimaryNodeType().getName());
                throw new ClientException(msg, ClientError.UNKNOWN);
            }
            if (!target.startsWith(siteNode.getPath() + "/")) {
                String msg = String.format("Target '%s' does not start with the path of the targeted hst site '%s'.",
                        target, siteMapNode.getPath());
                throw new ClientException(msg, ClientError.UNKNOWN);
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
    protected String buildXPathQueryLockedNodesForUsers(final String previewConfigurationPath,
                                                        final List<String> userIds) {
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("List of user IDs cannot be empty");
        }

        StringBuilder xpath = new StringBuilder("/jcr:root");
        xpath.append(ISO9075.encodePath(previewConfigurationPath));
        xpath.append("//element(*,");
        xpath.append(NODETYPE_HST_SITEMAPITEM);
        xpath.append(")[");

        String concat = "";
        for (String userId : userIds) {
            xpath.append(concat);
            xpath.append('@');
            xpath.append(GENERAL_PROPERTY_LOCKED_BY);
            xpath.append(" = '");
            xpath.append(userId);
            xpath.append("'");
            concat = " or ";
        }
        xpath.append("]");

        return xpath.toString();
    }

    private String getSiteMapPathPrefixPart(final Node siteMapNode) throws RepositoryException {
        Node crNode = siteMapNode;
        StringBuilder siteMapPathPrefixBuilder = new StringBuilder();
        while (crNode.isNodeType(NODETYPE_HST_SITEMAPITEM)) {
            if (siteMapPathPrefixBuilder.length() > 0) {
                siteMapPathPrefixBuilder.insert(0, "-");
            }
            siteMapPathPrefixBuilder.insert(0, crNode.getName());
            crNode = crNode.getParent();
        }
        return siteMapPathPrefixBuilder.toString();
    }

    private String getWorkspaceSiteMapId() throws RepositoryException {
        final String workspaceSiteMapId;
        final HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        final HstSite editingPreviewSite = pageComposerContextService.getEditingPreviewSite();
        final HstSiteMap siteMap = editingPreviewSite.getSiteMap();
        String siteMapId = ((CanonicalInfo)(siteMap)).getCanonicalIdentifier();
        Node siteMapNode = requestContext.getSession().getNodeByIdentifier(siteMapId);
        if (siteMapNode.getParent().isNodeType(NODETYPE_HST_WORKSPACE)) {
            workspaceSiteMapId = siteMapId;
        } else {
            // not the workspace sitemap node. Take the workspace sitemap. If not existing, an exception is thrown
            final String relSiteMapPath = NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_SITEMAP;
            final Node configNode = siteMapNode.getParent();
            workspaceSiteMapId = configNode.getNode(relSiteMapPath).getIdentifier();
        }
        return workspaceSiteMapId;
    }

}
