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

import javax.jcr.LoginException;
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

        acquireLock(jcrNode, HstNodeTypes.NODETYPE_HST_SITEMAP);

        final String modifiedName = siteMapItem.getName();
        if (modifiedName != null && !modifiedName.equals(jcrNode.getName())) {
            String oldLocation = jcrNode.getPath();
            session.move(jcrNode.getPath(), jcrNode.getParent() + "/" + modifiedName);
            createDeletedNode(session, oldLocation);
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

        final Node newChild = parent.addNode(siteMapItem.getName(), HstNodeTypes.NODETYPE_HST_SITEMAPITEM);

        setSitemapItemProperties(siteMapItem, newChild);

        final Map<String, String> modifiedLocalParameters = siteMapItem.getLocalParameters();
        setLocalParameters(newChild, modifiedLocalParameters);

        final Set<String> modifiedRoles = siteMapItem.getRoles();
        setRoles(newChild,modifiedRoles);

    }

    public void move(final String id, final String parentId) throws RepositoryException {
        HstRequestContext requestContext = RequestContextProvider.get();
        final Session session = requestContext.getSession();
        Node toMove = session.getNode(id);
        Node newParent = session.getNode(parentId);
        String oldLocation = toMove.getPath();
        session.move(oldLocation, newParent.getPath() + "/" + toMove.getName());
        acquireLock(toMove, HstNodeTypes.NODETYPE_HST_SITEMAP);
        createDeletedNode(session, oldLocation);
    }

    public void delete(final String id) throws RepositoryException {
        HstRequestContext requestContext = RequestContextProvider.get();
        final Session session = requestContext.getSession();
        Node deleted = session.getNode(id);
        markDeleted(deleted);
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


    private void createDeletedNode(final Session session, final String oldLocation) throws RepositoryException {
        Node deleted = session.getRootNode().addNode(oldLocation.substring(1), HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
        markDeleted(deleted);
    }



}
