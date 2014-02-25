/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.sitemenuresource;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.LinkType;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMenuResource;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MenuDiscardTest extends AbstractMenuResourceTest{

    @Test
    public void test_update() throws Exception {

        final SiteMenuResource resource = createResource();
        final SiteMenuItemRepresentation newsItem = getSiteMenuItemRepresentation(session, "main", "News");
        final Node newsNode = session.getNodeByIdentifier(newsItem.getId());
        String previewNewsNodePath = newsNode.getPath();
        String refSiteMapItemBefore = newsNode.getProperty(HstNodeTypes.SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM).getString();
        assertNotNull(newsItem);
        newsItem.setLinkType(LinkType.SITEMAPITEM);
        newsItem.setLink("test");
        resource.update(newsItem);

        assertNotSame(refSiteMapItemBefore, "test");
        assertEquals("test", newsNode.getProperty(HstNodeTypes.SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM).getString());

        mountResource.discardChanges();

        try {
            // after discard the newsNode above is replaced with live and should thus not work any more
            newsNode.getName();
            fail("newsNode should not exist any more");
        } catch (InvalidItemStateException e) {
            // expected!
        }

        final Node newsNodeAfter = session.getNode(previewNewsNodePath);

        assertEquals(refSiteMapItemBefore, newsNodeAfter.getProperty(HstNodeTypes.SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM).getString());

        final Node menuNode = newsNodeAfter.getParent();
        assertFalse(menuNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));

        String liveLocation = newsNodeAfter.getPath().replace("-preview/","/");
        assertTrue(session.nodeExists(liveLocation));

        Node liveNewsNode = session.getNode(liveLocation);
        assertEquals(refSiteMapItemBefore, liveNewsNode.getProperty(HstNodeTypes.SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM).getString());

        assertBobCanMakeModications(resource);
    }

    @Test
    public void test_rename() throws Exception {
        final SiteMenuResource resource = createResource();
        final SiteMenuItemRepresentation newsItem = getSiteMenuItemRepresentation(session, "main", "News");

        final Node newsNode = session.getNodeByIdentifier(newsItem.getId());
        final String oldPreviewLocation = newsNode.getPath();
        final String menuPath = newsNode.getParent().getPath();
        newsItem.setName("NewsRenamed");
        resource.update(newsItem);

        final String newPreviewLocation = session.getNodeByIdentifier(newsItem.getId()).getPath();

        mountResource.discardChanges();
        try {
            session.getNodeByIdentifier(newsItem.getId());
            fail("newsNode should not exist any more");
        } catch (ItemNotFoundException e) {
            // expected
        }
        assertFalse(session.getNode(menuPath).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));

        assertTrue(session.nodeExists(oldPreviewLocation));
        assertFalse(session.nodeExists(newPreviewLocation));
        String oldLiveLocation = oldPreviewLocation.replace("-preview/","/");
        String newLiveLocation = newPreviewLocation.replace("-preview/","/");
        assertTrue(session.nodeExists(oldLiveLocation));
        assertFalse(session.nodeExists(newLiveLocation));

        assertBobCanMakeModications(resource);
    }

    @Test
    public void test_delete() throws Exception {
        final SiteMenuResource resource = createResource();
        final SiteMenuItemRepresentation newsItem = getSiteMenuItemRepresentation(session, "main", "News");
        String oldPath = session.getNodeByIdentifier(newsItem.getId()).getPath();
        resource.delete(newsItem.getId());

        mountResource.discardChanges();

        assertTrue(session.nodeExists(oldPath));
        String oldLiveLocation = oldPath.replace("-preview/","/");
        assertTrue(session.nodeExists(oldLiveLocation));

        assertBobCanMakeModications(resource);
    }

    @Test
    public void test_move() throws Exception {
        final SiteMenuResource resource = createResource();
        final SiteMenuItemRepresentation newsItem = getSiteMenuItemRepresentation(session, "main", "News");
        final SiteMenuItemRepresentation contactItem = getSiteMenuItemRepresentation(session, "main", "Contact");
        String oldPath = session.getNodeByIdentifier(newsItem.getId()).getPath();
        resource.move(newsItem.getId(), contactItem.getId(), 0);
        String newPath = session.getNodeByIdentifier(contactItem.getId()).getPath() + "/News";

        mountResource.discardChanges();

        assertTrue(session.nodeExists(oldPath));
        assertFalse(session.nodeExists(newPath));

        String oldLiveLocation = oldPath.replace("-preview/","/");
        String newLiveLocation = newPath.replace("-preview/", "/");
        assertTrue(session.nodeExists(oldLiveLocation));
        assertFalse(session.nodeExists(newLiveLocation));

        assertBobCanMakeModications(resource);
    }



}
