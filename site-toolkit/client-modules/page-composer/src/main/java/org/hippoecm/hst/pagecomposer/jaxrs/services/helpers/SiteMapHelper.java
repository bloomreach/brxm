/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.StringUtils;
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
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.HstSiteMapUtils;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.StringUtils.substringBeforeLast;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_PAGES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_SITEMAP;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_SITEMAP;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_SITEMAPITEM;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.PROTOTYPE_META_PROPERTY_APPLICATION_ID;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_APPLICATION_ID;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_REF_ID;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError.INVALID_MOVE_TO_SELF_OR_DESCENDANT;

public class SiteMapHelper extends AbstractHelper {

    private static final Logger log = LoggerFactory.getLogger(SiteMapHelper.class);
    private static final String WORKSPACE_PATH_ELEMENT = "/" + NODENAME_HST_WORKSPACE + "/";

    private PagesHelper pagesHelper;
    private TemplateHelper templateHelper;

    public void setPagesHelper(final PagesHelper pagesHelper) {
        this.pagesHelper = pagesHelper;
    }

    public void setTemplateHelper(final TemplateHelper templateHelper) {
        this.templateHelper = templateHelper;
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
    @Override
    public HstSiteMapItem getConfigObject(final String itemId, final Mount mount) {
        final HstSite site = mount.getHstSite();
        return getSiteMapItem(site.getSiteMap(), itemId);
    }

    @Override
    protected String getNodeType() {
        return NODETYPE_HST_SITEMAPITEM;
    }

    public void update(final SiteMapItemRepresentation siteMapItem, final boolean reApplyPrototype) throws RepositoryException {
        HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        final Session session = requestContext.getSession();
        final String itemId = siteMapItem.getId();
        Node siteMapItemNode = session.getNodeByIdentifier(itemId);

        lockHelper.acquireLock(siteMapItemNode, 0);

        final String modifiedName = getURLDecodedJcrEncodedName(siteMapItem.getName());

        moveIfNeeded(itemId, siteMapItem.getParentId(), modifiedName);

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

        final String previewWorkspaceSiteMapPath = getPreviewWorkspacePath() + "/" + NODENAME_HST_SITEMAP;
        if (!session.nodeExists(previewWorkspaceSiteMapPath)) {
            createWorkspaceSiteMapInPreviewAndLive(previewWorkspaceSiteMapPath, session);
        }
        Node parent = session.getNodeByIdentifier(finalParentId);

        final String encodedName = getURLDecodedJcrEncodedName(siteMapItem.getName());
        validateTarget(session, parent.getPath() + "/" + encodedName,
                pageComposerContextService.getEditingPreviewSite().getSiteMap());

        final Node newSitemapNode = parent.addNode(encodedName, NODETYPE_HST_SITEMAPITEM);
        lockHelper.acquireLock(newSitemapNode, 0);

        setSitemapItemProperties(siteMapItem, newSitemapNode);
        // clone page definition

        final Node prototypePage = session.getNodeByIdentifier(siteMapItem.getComponentConfigurationId());
        final String prototypeApplicationId = JcrUtils.getStringProperty(prototypePage,
                PROTOTYPE_META_PROPERTY_APPLICATION_ID, null);
        final String targetPageNodeName = getSiteMapPathPrefixPart(newSitemapNode) + "-" + prototypePage.getName();
        Node newPage = pagesHelper.create(prototypePage, targetPageNodeName);

        newSitemapNode.setProperty(SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID,
                NODENAME_HST_PAGES + "/" + newPage.getName());

        final Map<String, String> modifiedLocalParameters = siteMapItem.getLocalParameters();
        setLocalParameters(newSitemapNode, modifiedLocalParameters);

        final Set<String> modifiedRoles = siteMapItem.getRoles();
        setRoles(newSitemapNode, modifiedRoles);

        if (prototypeApplicationId != null) {
            newSitemapNode.setProperty(SITEMAPITEM_PROPERTY_APPLICATION_ID, prototypeApplicationId);
        }

        return newSitemapNode;
    }

    /**
     * utility method to create a <strong>shallow copy</strong> of {@code siteMapItemId} : Shallow copy means that
     * child pages of {@code siteMapItemId} are not copied
     *
     * @param mountId               the target mount for the copy. If <code>null</code>, the current edited mount is
     *                              used
     * @param sourceSiteMapItemUUID the uuid of the {@code siteMapItem} to copy, not allowed to be <code>null</code>
     * @param targetSiteMapItemUUID the uuid of the target siteMapItem, can be {@code null} in which case the same
     *                              location as {@code siteMapItem} will be used, only with name {@code targetName}
     * @param targetName            the name of the copy, not allowed to be {@code null}
     * @return the {@link javax.jcr.Node} of the created new siteMapItem
     * @throws RepositoryException
     */
    public PageCopyContext copy(final String mountId, final String sourceSiteMapItemUUID, final String targetSiteMapItemUUID, final String targetName)
            throws RepositoryException {

        HstRequestContext requestContext = pageComposerContextService.getRequestContext();

        final Mount editingMount = pageComposerContextService.getEditingMount();
        final HstSiteMapItem sourceSiteMapItem = validateAndReturnSiteMapItem(sourceSiteMapItemUUID, editingMount);
        final Mount targetMount;
        if (mountId == null) {
            targetMount = editingMount;
        } else {
            targetMount = requestContext.getVirtualHost().getVirtualHosts().getMountByIdentifier(mountId);
        }

        final String previewWorkspaceSiteMapPath = getWorkspacePath(targetMount) + "/" + NODENAME_HST_SITEMAP;

        final Session session = requestContext.getSession();
        if (!session.nodeExists(previewWorkspaceSiteMapPath)) {
            createWorkspaceSiteMapInPreviewAndLive(previewWorkspaceSiteMapPath, session);
        }

        final HstSiteMapItem targetSiteMapItem;
        if (targetSiteMapItemUUID != null) {
            targetSiteMapItem = validateAndReturnSiteMapItem(targetSiteMapItemUUID, targetMount);
        } else {
            targetSiteMapItem = null;
        }

        final Node workspaceSiteMapNode = session.getNode(previewWorkspaceSiteMapPath);

        final String encodedName = getURLDecodedJcrEncodedName(targetName);
        final String target;
        if (targetSiteMapItemUUID != null) {
            target = session.getNodeByIdentifier(targetSiteMapItemUUID).getPath() + "/" + encodedName;
        } else {
            target = workspaceSiteMapNode.getPath() + "/" + encodedName;
        }
        String nonWorkspaceLocation = target.replace("/" + NODENAME_HST_WORKSPACE + "/", "/");

        if (session.nodeExists(target) || session.nodeExists(nonWorkspaceLocation)) {
            String message = String.format("Cannot copy because there is a siteMapItem with same name and " +
                    "location already located at '%s' or '%s'.", target, nonWorkspaceLocation);
            throw new ClientException(message, ClientError.ITEM_CANNOT_BE_CLONED, Collections.singletonMap("errorReason", message));
        }

        validateTarget(session, target, targetMount.getHstSite().getSiteMap());
        final Node newSiteMapNode = shallowCopy(session, sourceSiteMapItemUUID, substringBeforeLast(target, "/"), encodedName);
        lockHelper.acquireLock(newSiteMapNode, 0);

        // copy the page definition
        // we need to find the page node uuid to copy through hstSiteMapItem.getComponentConfigurationId() which is NOT
        // the page UUID
        final HstComponentConfiguration sourcePage = pageComposerContextService.getEditingPreviewSite().getComponentsConfiguration()
                .getComponentConfiguration(sourceSiteMapItem.getComponentConfigurationId());

        if (sourcePage == null) {
            final String message = "Cannot duplicate page since backing hst component configuration object not found";
            throw new ClientException(message,
                    ClientError.ITEM_CANNOT_BE_CLONED, Collections.singletonMap("errorReason", message));
        }

        String sourcePathInfo = HstSiteMapUtils.getPath(sourceSiteMapItem);
        String sourcePageNodeNamePrefix = sourcePathInfo.replace("/", "-");

        final String prefix = getPreviewConfigurationPath() + "/" + NODENAME_HST_PAGES + "/" + sourcePageNodeNamePrefix + "-";
        String targetPageName;
        if (sourcePage.getCanonicalStoredLocation().startsWith(prefix)) {
            targetPageName = encodedName + "-" + sourcePage.getCanonicalStoredLocation().substring(prefix.length());
        } else {
            targetPageName = encodedName;
        }
        if (targetSiteMapItem != null) {
            String targetPathInfo = HstSiteMapUtils.getPath(targetSiteMapItem);
            targetPageName = targetPathInfo.replace("/", "-") + "-" + targetPageName;
        }

        Node clonedPage = pagesHelper.copy(session, targetPageName, sourcePage, editingMount, targetMount);
        newSiteMapNode.setProperty(SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID,
                NODENAME_HST_PAGES + "/" + clonedPage.getName());

        PageCopyContext pcc = new PageCopyContext(requestContext, editingMount, sourceSiteMapItem, session.getNodeByIdentifier(sourceSiteMapItemUUID),
                sourcePage, session.getNodeByIdentifier(sourcePage.getCanonicalIdentifier()), targetMount, targetSiteMapItem, newSiteMapNode, clonedPage);

        templateHelper.copyTemplates(pcc);
        return pcc;
    }

    private void createWorkspaceSiteMapInPreviewAndLive(final String previewWorkspaceSiteMapPath, final Session session) throws RepositoryException {
        final String previewWorkspacePath = substringBeforeLast(previewWorkspaceSiteMapPath, "/");
        final String liveWorkspacePath = previewWorkspacePath.replace("-preview/", "/");
        session.getNode(previewWorkspacePath).addNode(NODENAME_HST_SITEMAP, NODETYPE_HST_SITEMAP);
        if (!session.nodeExists(liveWorkspacePath + "/" + NODETYPE_HST_SITEMAP)) {
            session.getNode(liveWorkspacePath).addNode(NODENAME_HST_SITEMAP, NODETYPE_HST_SITEMAP);
        }
    }

    /**
     * copies the node for <code>sourceSiteMapItemUUID</code> to the node for <code>targetSiteMapItemUUID</code>. The
     * copied
     * node will have name <code>targetName</code>. The node is copied <strong>without</strong> its children!
     */
    private Node shallowCopy(final Session session, final String sourceSiteMapItemUUID,
                             final String targetParentPath,
                             final String targetName) throws RepositoryException {
        Node source = session.getNodeByIdentifier(sourceSiteMapItemUUID);
        Node parentTarget = session.getNode(targetParentPath);

        final Node newItem = parentTarget.addNode(targetName, NODETYPE_HST_SITEMAPITEM);
        for (NodeType mixin : source.getMixinNodeTypes()) {
            newItem.addMixin(mixin.getName());
        }
        final PropertyIterator properties = source.getProperties();
        while (properties.hasNext()) {
            final Property property = properties.nextProperty();
            if (!property.getName().startsWith("hst:")) {
                // only hst properties are needed
                continue;
            }
            if (property.getName().equals(SITEMAPITEM_PROPERTY_REF_ID)) {
                // refId must be unique, hence skipped completely for copied nodes
                continue;
            }
            if (property.isMultiple()) {
                newItem.setProperty(property.getName(), property.getValues());
            } else {
                newItem.setProperty(property.getName(), property.getValue());
            }
        }
        return newItem;
    }

    private HstSiteMapItem validateAndReturnSiteMapItem(final String siteMapItemUUId, final Mount targetMount) {
        HstSiteMapItem hstSiteMapItem = getConfigObject(siteMapItemUUId, targetMount);
        if (hstSiteMapItem == null) {
            String message = String.format("Cannot copy because there is no siteMapItem for id '%s'.", siteMapItemUUId);
            throw new ClientException(message, ClientError.ITEM_CANNOT_BE_CLONED, Collections.singletonMap("errorReason", message));
        }
        String pathInfo = HstSiteMapUtils.getPath(hstSiteMapItem);
        if (pathInfo.contains(HstNodeTypes.WILDCARD) || pathInfo.contains(HstNodeTypes.ANY)) {
            String message = String.format("Cannot copy a page for siteMapItem '%s' because it contains " +
                    "wildcards and this is not supported.", ((CanonicalInfo)hstSiteMapItem).getCanonicalPath());
            throw new ClientException(message, ClientError.ITEM_CANNOT_BE_CLONED, Collections.singletonMap("errorReason", message));
        }
        return hstSiteMapItem;
    }

    public void move(final String sourceId, final String targetParentId) throws RepositoryException {
        moveIfNeeded(sourceId, targetParentId, null);
    }

    public void moveIfNeeded(final String sourceId, final String targetParentId, final String name) throws RepositoryException {
        final String finalTargetParentId;
        final String finalName;
        if (targetParentId == null) {
            finalTargetParentId = getWorkspaceSiteMapId();
        } else {
            finalTargetParentId = targetParentId;
        }
        if (sourceId.equals(finalTargetParentId)) {
            final String message = "Cannot move node to become child of itself";
            throw new ClientException(message, INVALID_MOVE_TO_SELF_OR_DESCENDANT);
        }
        HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        final Session session = requestContext.getSession();
        Node sourceNode = session.getNodeByIdentifier(sourceId);
        if (StringUtils.isEmpty(name)) {
            finalName = sourceNode.getName();
        } else {
            finalName = name;
        }
        Node targetParent = session.getNodeByIdentifier(finalTargetParentId);
        Node currentParent = sourceNode.getParent();

        if (currentParent.isSame(targetParent)) {
            if (!sourceNode.getName().equals(finalName)) {
                // we do not need to check lock for parent as this is a rename within same parent
                String oldLocation = sourceNode.getPath();
                String target = sourceNode.getParent().getPath() + "/" + finalName;
                validateTarget(session, target, pageComposerContextService.getEditingPreviewSite().getSiteMap());
                session.move(sourceNode.getPath(), sourceNode.getParent().getPath() + "/" + finalName);
                createMarkedDeletedIfLiveExists(session, oldLocation);
                log.info("Renamed item from '{}' to '{}'", oldLocation, target);
                return;
            }
            log.debug("No move was required since same name and same parent");
            return;
        }
        final Node unLockableNode = lockHelper.getUnLockableNode(targetParent, true, false);
        if (unLockableNode != null) {
            String message = String.format("Cannot move node to '%s' because that node is locked through node '%s' by '%s'",
                    targetParent.getPath(), unLockableNode.getPath(), unLockableNode.getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
            throw new ClientException(message, ClientError.ITEM_ALREADY_LOCKED);
        }
        lockHelper.acquireLock(sourceNode, 0);
        validateTarget(session, targetParent.getPath() + "/" + finalName,
                pageComposerContextService.getEditingPreviewSite().getSiteMap());
        String oldLocation = sourceNode.getPath();
        session.move(currentParent.getPath() + "/" + sourceNode.getName(), targetParent.getPath() + "/" + finalName);
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
        if (((CanonicalInfo)siteMapItem).getCanonicalIdentifier().equals(siteMapItemId)) {
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
            } else if (absPath.equals("")) {
                removeProperty(jcrNode, HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH);
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
        if (!session.nodeExists(StringUtils.substringBeforeLast(target, "/"))) {
            final String message = String.format("Parent of target node '%s' does not exist", target);
            throw new ClientException(message, ClientError.INVALID_URL);
        }

        if (session.nodeExists(target)) {
            Node targetNode = session.getNode(target);
            if (isMarkedDeleted(targetNode)) {
                // see if we own the lock
                lockHelper.acquireLock(targetNode, 0);
                targetNode.remove();
                // target is valid to be created
                return;
            } else {
                final String message = String.format("Target node '%s' already exists", target);
                throw new ClientException(message, ClientError.ITEM_NAME_NOT_UNIQUE);
            }
        }

        // check whether there is an explicit sitemap item match in the siteMap : If there is, the
        // target is only valid if the found sitemap item is part of *a* (inherited) workspace and it is not allowed if it is a
        // non-workspace item. Also if the item cannot be found or is not an explicit (non wildcard) sitemap item *or* is not a
        // workspace item, the target is not valid

        final CanonicalInfo canonical = (CanonicalInfo)siteMap;

        final Node siteMapNode = session.getNodeByIdentifier(canonical.getCanonicalIdentifier());
        String siteMapPath = siteMapNode.getPath();

        final String targetConfig = StringUtils.substringBefore(target, "/hst:workspace");

        if (!siteMapPath.startsWith(targetConfig)) {
            // in an exceptional cae, it can occur that the sitemap in workspace has just been created and hence the
            // currently loaded model contains a reference to the old sitemap node. If this is the case, the target is
            // still valid
            if (!target.startsWith(StringUtils.substringBefore(siteMapPath, "/hst:sitemap") + "-preview/hst:workspace/hst:sitemap")) {
                String msg = String.format("Target '%s' is not valid for sitemap '%s'.",
                        target, siteMapPath);
                throw new ClientException(msg, ClientError.ITEM_EXISTS_OUTSIDE_WORKSPACE);
            }

        }

        final String siteMapRelPath = target.substring(siteMapNode.getPath().length() + 1);

        String[] elements = siteMapRelPath.split("/");

        HstSiteMapItem siteMapItem = siteMap.getSiteMapItem(elements[0]);
        for (int i = 1; i < elements.length ; i++ ) {
            if (siteMapItem == null) {
                break;
            }
            siteMapItem = siteMapItem.getChild(elements[i]);
        }
        if (siteMapItem == null) {
            log.debug("Target path '{}' can be created because it does not yet exist.", target);
            return;
        }

        log.debug("Target '{}' can be matched in current sitemap. Now check whether the sitemap item that is matched " +
                "belongs to the current hst:workspace/hst:sitemap, otherwise, it still can't be used");

        CanonicalInfo item = (CanonicalInfo) siteMapItem;
        Node siteMapItemNode = session.getNodeByIdentifier(item.getCanonicalIdentifier());

        String existingItem = siteMapItemNode.getPath();
        if (existingItem.startsWith(siteMapPath)) {
            String msg = String.format("Target path '%s' already exists in current sitemap.",
                    target, siteMapPath);
            throw new ClientException(msg, ClientError.ITEM_EXISTS);
        }
        if (!existingItem.contains("/hst:workspace/hst:sitemap")) {
            String msg = String.format("Target path '%s' already exists in inherited configuration but is there not " +
                    "below hst:workspace/hst:sitemap and thus cannot be added in subproject.",
                    target);
            throw new ClientException(msg, ClientError.ITEM_EXISTS_OUTSIDE_WORKSPACE);
        }


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


    private String getURLDecodedJcrEncodedName(final String name) {
        final String encoding = getEncoding(pageComposerContextService.getRequestContext());
        try {
            final String urlDecodedName = URLDecoder.decode(name, encoding);
            return NodeNameCodec.encode(urlDecodedName);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(String.format("Could not ULR  decode '%s'", name), e);
        }
    }

    private String getEncoding(final HstRequestContext requestContext) {
        return HstRequestUtils.getURIEncoding(requestContext.getServletRequest());
    }

}
