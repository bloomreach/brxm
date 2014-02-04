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

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.AbstractConfigResource;
import org.hippoecm.repository.util.NodeIterable;
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

        //HstConfigurationUtils.persistChanges(session);
    }

    public void create(final SiteMapItemRepresentation siteMapItem, final String parentId) {

    }

    public void move(final String sourceId, final String parentId) {

    }

    public void delete(final String id) {

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

    // TODO check lastModifiedTimestamp
    protected void acquireLock(final Node node) throws RepositoryException {
        if (lockPresent(node)) {
            return;
        }
        node.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        final Session session = node.getSession();
        log.info("Container '{}' gets a lock for user '{}'.", node.getPath(), session.getUserID());
        node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, session.getUserID());
        Calendar now = Calendar.getInstance();
        if (!node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
            node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON, now);
        }
        node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED_BY, session.getUserID());
        // remove all present descendant locks for current user.
        removeDescendantLocks(new NodeIterable(node.getNodes()));
    }

    private void removeDescendantLocks(final NodeIterable nodes) throws RepositoryException {
        for (Node node : nodes) {
            removeProperty(node, HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY);
            removeProperty(node, HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON);
        }
    }

    private boolean lockPresent(final Node node) throws RepositoryException {
        if(node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
            String lockedBy = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON).getString();
            if (!node.getSession().getUserID().equals(lockedBy)) {
                throw new IllegalStateException("Locked by someone else");
            }
            return true;
        }

        if (node.isNodeType(HstNodeTypes.NODETYPE_HST_SITEMAP)) {
            return false;
        }
        return lockPresent(node.getParent());
    }


}
