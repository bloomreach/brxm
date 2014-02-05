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

import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.AbstractConfigResource;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SiteMapHelper extends AbstractHelper {

    private static final Logger log = LoggerFactory.getLogger(SiteMapHelper.class);

    @Override
    public <T> T getConfigObject(final String itemId) {
        final HstRequestContext requestContext = RequestContextProvider.get();
        final HstSite editingPreviewSite = AbstractConfigResource.getEditingPreviewSite(requestContext);
        return (T) getSiteMapItem(editingPreviewSite.getSiteMap(), itemId);
    }

    public void update(final SiteMapItemRepresentation siteMapItem) throws RepositoryException {
        HstRequestContext requestContext = RequestContextProvider.get();
        final Session session = requestContext.getSession();
        final String itemId = siteMapItem.getId();
        Node jcrNode = session.getNodeByIdentifier(itemId);

        acquireLock(jcrNode);

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



    public void create(final SiteMapItemRepresentation siteMapItem, final String parentId) throws RepositoryException {

        HstRequestContext requestContext = RequestContextProvider.get();
        final Session session = requestContext.getSession();
        Node parent = session.getNodeByIdentifier(parentId);

        validateTarget(session, parent.getPath() + "/" + siteMapItem.getName());

        final Node newChild = parent.addNode(siteMapItem.getName(), HstNodeTypes.NODETYPE_HST_SITEMAPITEM);

        setSitemapItemProperties(siteMapItem, newChild);

        final Map<String, String> modifiedLocalParameters = siteMapItem.getLocalParameters();
        setLocalParameters(newChild, modifiedLocalParameters);

        final Set<String> modifiedRoles = siteMapItem.getRoles();
        setRoles(newChild,modifiedRoles);

    }

    public void move(final String id, final String parentId) throws RepositoryException {
        if (id.equals(parentId)) {
            return;
        }
        HstRequestContext requestContext = RequestContextProvider.get();
        final Session session = requestContext.getSession();
        Node nodeToMove = session.getNode(id);
        Node newParent = session.getNode(parentId);
        Node oldParent = nodeToMove.getParent();
        if (hasSelfOrAncestorLockBySomeOneElse(newParent)) {
            throw new IllegalStateException("Cannot move node to '"+newParent.getPath()+"' because that node is locked " +
                    "by '"+getSelfOrAncestorLockedBy(newParent)+"'");
        }
        acquireLock(nodeToMove);
        String nodeName = nodeToMove.getName();
        validateTarget(session, newParent.getPath() + "/" + nodeName);
        String oldLocation = nodeToMove.getPath();
        session.move(oldParent.getPath() + "/" + nodeName, newParent.getPath() + "/" + nodeName);
        acquireLock(nodeToMove);

        createMarkedDeletedIfLiveExists(session, oldLocation);
    }

    public void delete(final String id) throws RepositoryException {
        HstRequestContext requestContext = RequestContextProvider.get();
        final Session session = requestContext.getSession();
        Node toDelete = session.getNode(id);
        acquireLock(toDelete);
        deleteOrMarkDeletedIfLiveExists(toDelete);
    }


    public static HstSiteMapItem getSiteMapItem(HstSiteMap siteMap, String siteMapItemId) {

        for (HstSiteMapItem hstSiteMapItem : siteMap.getSiteMapItems()) {
            final HstSiteMapItem siteMapItem = getSiteMapItem(hstSiteMapItem, siteMapItemId);
            if (siteMapItem != null) {
                return siteMapItem;
            }
        }

        throw new IllegalStateException(String.format("SiteMap item with id '%s' is not part of currently edited preview site.", siteMapItemId));
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
        setProperty(jcrNode, HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID, siteMapItem.getComponentConfigurationId());
        setProperty(jcrNode, HstNodeTypes.SITEMAPITEM_PROPERTY_SCHEME, siteMapItem.getScheme());
        setProperty(jcrNode, HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH, siteMapItem.getRelativeContentPath());
    }



    private void createMarkedDeletedIfLiveExists(final Session session, final String oldLocation) throws RepositoryException {
        boolean liveExists =  liveExists(session, oldLocation);
        if (liveExists) {
            Node deleted = session.getRootNode().addNode(oldLocation.substring(1), HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
            markDeleted(deleted);
        }
    }

    private void deleteOrMarkDeletedIfLiveExists(final Node toDelete) throws RepositoryException {
        boolean liveExists =  liveExists(toDelete.getSession(), toDelete.getPath());
        if (liveExists) {
            markDeleted(toDelete);
        } else {
            toDelete.remove();
        }
    }

    private boolean liveExists(final Session session, final String oldLocation) throws RepositoryException {
        if (!oldLocation.contains("-preview/hst:workspace")) {
            throw new IllegalStateException("Unexpected location '"+oldLocation+"'");
        }
        oldLocation.replace("-preview/hst:workspace", "/hst:workspace");
        return session.nodeExists(oldLocation);
    }

    private void markDeleted(final Node deleted) throws RepositoryException {
        acquireLock(deleted);
        deleted.setProperty("hst:state", "deleted");
    }

    private boolean isMarkedDeleted(final Node node) throws RepositoryException {
       return "deleted".equals(JcrUtils.getStringProperty(node, "hst:state", null));
    }

    private void validateTarget(final Session session, final String target) throws RepositoryException {
        if (session.nodeExists(target)) {
            Node targetNode = session.getNode(target);
            if (isMarkedDeleted(targetNode)) {
                // see if we own the lock
                acquireLock(targetNode);
                targetNode.remove();
            } else {
                throw new IllegalStateException("Target node '"+targetNode.getPath()+"' already exists");
            }
        }
    }


}
