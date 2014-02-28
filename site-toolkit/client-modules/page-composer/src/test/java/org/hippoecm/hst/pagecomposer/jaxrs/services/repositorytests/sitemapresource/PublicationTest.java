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

package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.sitemapresource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.ws.rs.core.Response;

import com.google.common.collect.Lists;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtIdsRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * these tests do not cover new sitemap item with a new page tests. These are more complex and covered in
 * {@link CreateAndPublicationTest}
 */
public class PublicationTest extends AbstractSiteMapResourceTest {

    @Test
    public void test_update() throws Exception {

        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        home.setRelativeContentPath("testRelPath");
        home.setScheme("https");
        Set<String> roles = new HashSet<>();
        roles.add("foo");
        roles.add("bar");
        home.setRoles(roles);
        Map<String, String> localParams = new HashMap<>();
        localParams.put("foo", "bar");
        localParams.put("lux", "qux");
        home.setLocalParameters(localParams);

        final SiteMapResource siteMapResource = createResource();
        Response response = siteMapResource.update(home);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        {
            Node previewHomeNode = session.getNodeByIdentifier(home.getId());
            assertTrue(previewHomeNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
            assertEquals("admin", previewHomeNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        }
        mountResource.publish();
        {
            Node previewHomeNode = session.getNodeByIdentifier(home.getId());
            assertFalse(previewHomeNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
            assertFalse(previewHomeNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));
        }

        String previewHomePath = session.getNodeByIdentifier(home.getId()).getPath();
        String liveHomeNodePath = previewHomePath.replace("-preview/", "/");
        assertNotSame(previewHomePath, liveHomeNodePath);

        Node liveHomeNode = session.getNode(liveHomeNodePath);
        assertFalse(liveHomeNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertFalse(liveHomeNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));
        assertEquals("testRelPath", liveHomeNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH).getString());
        assertEquals("https", liveHomeNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_SCHEME).getString());


        Set<String> storedRoles = new HashSet<>();
        for (Value value : liveHomeNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_ROLES).getValues()) {
            storedRoles.add(value.getString());
        }
        assertEquals(storedRoles, roles);

        Map<String, String> storedLocalParams = new HashMap<>();
        List<String> keys = new ArrayList<>();
        for (Value value : liveHomeNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES).getValues()) {
            keys.add(value.getString());
        }
        List<String> values = new ArrayList<>();
        for (Value value : liveHomeNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES).getValues()) {
            values.add(value.getString());
        }

        final Iterator<String> keyIt = keys.iterator();
        final Iterator<String> valueIt = values.iterator();

        while (keyIt.hasNext()) {
            storedLocalParams.put(keyIt.next(), valueIt.next());
        }
        assertEquals(storedLocalParams, localParams);
    }

    @Test
    public void test_rename() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");

        final String previewHomePathBefore = session.getNodeByIdentifier(home.getId()).getPath();
        final String liveHomePathBefore = previewHomePathBefore.replace("-preview/", "/");

        assertNotSame(previewHomePathBefore, liveHomePathBefore);
        assertTrue(session.nodeExists(previewHomePathBefore));
        assertTrue(session.nodeExists(liveHomePathBefore));

        home.setName("homeNew");
        final SiteMapResource siteMapResource = createResource();
        Response response = siteMapResource.update(home);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final String previewHomePathAfter = session.getNodeByIdentifier(home.getId()).getPath();
        final String liveHomePathAfter = session.getNodeByIdentifier(home.getId()).getPath().replace("-preview/", "/");
        // after move, the preview should have a deleted marker at the hold location
        assertTrue(session.nodeExists(previewHomePathBefore));
        assertTrue(session.nodeExists(liveHomePathBefore));
        assertTrue(session.nodeExists(previewHomePathAfter));
        assertFalse(session.nodeExists(liveHomePathAfter));

        mountResource.publish();
        assertFalse(session.nodeExists(previewHomePathBefore));
        assertFalse(session.nodeExists(liveHomePathBefore));
        assertTrue(session.nodeExists(previewHomePathAfter));
        assertTrue(session.nodeExists(liveHomePathAfter));
        assertFalse(session.getNode(previewHomePathAfter).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertFalse(session.getNode(liveHomePathAfter).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
    }

    @Test
    public void test_move() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
        final SiteMapResource siteMapResource = createResource();

        final String previewHomePathBefore = session.getNodeByIdentifier(home.getId()).getPath();
        final String liveHomePathBefore = previewHomePathBefore.replace("-preview/", "/");
        assertNotSame(previewHomePathBefore, liveHomePathBefore);
        assertTrue(session.nodeExists(previewHomePathBefore));
        assertTrue(session.nodeExists(liveHomePathBefore));

        Response response = siteMapResource.move(home.getId(), news.getId());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final String previewHomePathAfter = session.getNodeByIdentifier(home.getId()).getPath();
        final String liveHomePathAfter = previewHomePathAfter.replace("-preview/", "/");
        // after move, the preview should have a deleted marker at the hold location
        assertTrue(session.nodeExists(previewHomePathBefore));
        assertTrue(session.nodeExists(liveHomePathBefore));
        assertTrue(session.nodeExists(previewHomePathAfter));
        assertFalse(session.nodeExists(liveHomePathAfter));

        mountResource.publish();
        assertFalse(session.nodeExists(previewHomePathBefore));
        assertFalse(session.nodeExists(liveHomePathBefore));
        assertTrue(session.nodeExists(previewHomePathAfter));
        assertTrue(session.nodeExists(liveHomePathAfter));
        assertFalse(session.getNode(previewHomePathAfter).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertFalse(session.getNode(liveHomePathAfter).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));

        // assert live home is now child of preview
        assertTrue(session.getNode(previewHomePathAfter).getParent().isSame(session.getNodeByIdentifier(news.getId())));
    }

    @Test
    public void test_delete() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        final SiteMapResource siteMapResource = createResource();
        final Response delete = siteMapResource.delete(home.getId());
        assertEquals(Response.Status.OK.getStatusCode(), delete.getStatus());

        final String previewHomePathBefore = session.getNodeByIdentifier(home.getId()).getPath();
        final String liveHomePathBefore = previewHomePathBefore.replace("-preview/", "/");

        assertNotSame(previewHomePathBefore, liveHomePathBefore);

        // node even in preview still exists but marked as 'deleted'
        assertTrue(session.nodeExists(previewHomePathBefore));
        assertTrue(session.nodeExists(liveHomePathBefore));

        mountResource.publish();

        assertFalse(session.nodeExists(previewHomePathBefore));
        assertFalse(session.nodeExists(liveHomePathBefore));
    }

    @Test
    public void test_multi_user_change_single_user_publication() throws Exception {
        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");

        home.setName("adminHome");
        siteMapResource.update(home);

        final Session bob = createSession("bob", "bob");

        final SiteMapItemRepresentation newsByBob = getSiteMapItemRepresentation(bob, "news");

        newsByBob.setName("bobNews");
        siteMapResource.update(newsByBob);

        // bobs session is currently on the requestcontextprovider, hence publication below should publish bobs changes only
        mountResource.publish();


        final String previewHomePathAfter = session.getNodeByIdentifier(home.getId()).getPath();
        final String liveHomePathAfter = previewHomePathAfter.replace("-preview/", "/");
        final String previewNewsPathAfter = session.getNodeByIdentifier(newsByBob.getId()).getPath();
        final String liveNewsPathAfter = previewNewsPathAfter.replace("-preview/", "/");

        bob.logout();

        assertTrue(session.nodeExists(previewHomePathAfter));
        assertTrue(session.getNode(previewHomePathAfter).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        // home is not published
        assertFalse(session.nodeExists(liveHomePathAfter));

        assertTrue(session.nodeExists(previewNewsPathAfter));
        assertTrue(session.nodeExists(liveNewsPathAfter));
        assertFalse(session.getNode(previewNewsPathAfter).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));

    }

    @Test
    public void test_multi_user_change_multi_user_publication() throws Exception {
        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");

        home.setName("adminHome");
        siteMapResource.update(home);

        final Session bob = createSession("bob", "bob");
        final SiteMapItemRepresentation newsByBob = getSiteMapItemRepresentation(bob, "news");

        newsByBob.setName("bobNews");
        siteMapResource.update(newsByBob);

        // bobs session is currently on the requestcontextprovider, hence publication below should publish bobs changes only
        final ExtIdsRepresentation ids = new ExtIdsRepresentation();
        ids.setData(Lists.newArrayList("admin", "bob"));
        mountResource.publishChangesOfUsers(ids);

        final String previewHomePathAfter = session.getNodeByIdentifier(home.getId()).getPath();
        final String liveHomePathAfter = previewHomePathAfter.replace("-preview/", "/");
        final String previewNewsPathAfter = session.getNodeByIdentifier(newsByBob.getId()).getPath();
        final String liveNewsPathAfter = previewNewsPathAfter.replace("-preview/", "/");

        bob.logout();

        assertTrue(session.nodeExists(previewHomePathAfter));
        assertFalse(session.getNode(previewHomePathAfter).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        // home is also published now
        assertTrue(session.nodeExists(liveHomePathAfter));

        assertTrue(session.nodeExists(previewNewsPathAfter));
        assertTrue(session.nodeExists(liveNewsPathAfter));
        assertFalse(session.getNode(previewNewsPathAfter).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));

    }


    @Test
    public void test_succeeds_illegal_state_ancestor_multi_user_lock_publication() throws Exception {
        // this test is to ensure that when we have the following exceptional situation

        // -news (locked by 'admin')
        //    ` ** (locked by 'bob')

        // that when publishing by admin, also * gets published and lock of 'bob' gets removed.

        // NOTE the above is about handling incorrect states, which can occur due to concurrency or clustering

        // below ADMIN publishes
        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
        news.setRelativeContentPath("bar");
        siteMapResource.update(news);

        // now on jcr level modify the news/* item to be locked by bob. This cannot be done through siteMapResource
        // as that will result in lock exceptions
        final Node newsDefault = session.getNodeByIdentifier(news.getId()).getNode("_default_");
        // make it now as if it is locked by 'bob'
        newsDefault.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        newsDefault.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, "bob");
        newsDefault.setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH, "foo");

        session.save();

        // now publish by 'admin'. 'news/_default_' contains not allowed lock. It should be removed
        mountResource.publish();

        final String previewNewsPath= session.getNodeByIdentifier(news.getId()).getPath();
        final String liveNewsPath = previewNewsPath.replace("-preview/", "/");
        final String previewDefaultNewsPath= newsDefault.getPath();
        final String liveDefaultNewsPath = previewDefaultNewsPath.replace("-preview/", "/");

        assertTrue(session.nodeExists(previewNewsPath));
        assertFalse(session.getNode(previewNewsPath).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertEquals("bar" ,session.getNode(liveNewsPath).getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH).getString());

        // even though only 'admin' nodes are published, also the incorrect descendant news/_default_ must be published
        assertTrue(session.nodeExists(previewDefaultNewsPath));
        assertFalse(session.getNode(previewDefaultNewsPath).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertEquals("foo", session.getNode(liveDefaultNewsPath).getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH).getString());
    }

    @Test
    public void test_succeeds_illegal_state_descendant_multi_user_lock_publication() throws Exception {
        // this test is to ensure that when we have the following exceptional situation

        // -news (locked by 'bob')
        //    ` * (locked by 'admin')

        // that when publishing by admin, only the lock of 'admin' gets removed because ancestor is locked by 'bob'. 'news' stays locked
        // by bob

        // NOTE the above is about handling incorrect states, which can occur due to concurrency or clustering

        // below ADMIN publishes
        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation newsDefault = getSiteMapItemRepresentation(session, "news/_default_");
        newsDefault.setRelativeContentPath("bar");
        siteMapResource.update(newsDefault);

        // now on jcr level modify the 'news' item to be locked by bob. This cannot be done through siteMapResource
        // as that will result in lock exceptions
        final Node news = session.getNodeByIdentifier(newsDefault.getId()).getParent();
        // make it now as if it is locked by 'bob'
        news.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        news.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, "bob");
        news.setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH, "foo");

        session.save();

        // now publish by 'admin'. 'news' should keep lock by 'bob'
        mountResource.publish();

        final String previewNewsPath = news.getPath();
        final String liveNewsPath = previewNewsPath.replace("-preview/", "/");
        final String previewDefaultNewsPath = session.getNodeByIdentifier(newsDefault.getId()).getPath();
        final String liveDefaultNewsPath = previewDefaultNewsPath.replace("-preview/", "/");

        assertTrue(session.nodeExists(previewDefaultNewsPath));
        assertFalse(session.getNode(previewDefaultNewsPath).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertNotSame("bar" ,session.getNode(liveDefaultNewsPath).getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH).getString());

        assertTrue(session.nodeExists(previewNewsPath));
        // news is still locked by 'bob'
        assertTrue(session.getNode(previewNewsPath).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertEquals("bob", session.getNode(previewNewsPath).getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        assertEquals("foo", session.getNode(previewNewsPath).getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH).getString());
        assertNotSame("foo" ,session.getNode(liveNewsPath).getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH).getString());

    }

    @Test
    public void test_illegal_state_multi_lock_with_rename_descendant_locked_no_publication() throws Exception {
        // this test is to ensure that when we have the following exceptional situation

        // -newsRenamed (locked by 'bob')
        //    ` * (locked by 'admin')

        // when 'admin' tries to publish, it cannot publish to live since live does not have 'renamedNews'
        // only the lock gets removed from renamedNews/* since in the first place, 'bob' should be the
        // owner any way

        // NOTE the above is about handling incorrect states, which can occur due to concurrency or clustering

        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation newsDefault = getSiteMapItemRepresentation(session, "news/_default_");
        newsDefault.setRelativeContentPath("bar");
        siteMapResource.update(newsDefault);

        // now on jcr level rename 'news' and lock it for bob. This cannot be done through siteMapResource
        // as that will result in lock exceptions
        final Node news = session.getNodeByIdentifier(newsDefault.getId()).getParent();
        // make it now as if it is locked by 'bob'
        news.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        news.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, "bob");
        news.setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH, "foo");
        session.move(news.getPath(), news.getPath() + "Renamed");
        session.save();

        // now publish by 'admin'. Nothing should make it to live, only lock by 'admin' removed from 'newsRenamed/**
        mountResource.publish();

        final String previewDefaultNewsPath= session.getNodeByIdentifier(newsDefault.getId()).getPath();
        final String liveDefaultNewsPath = previewDefaultNewsPath.replace("-preview/", "/");

        assertTrue(session.nodeExists(previewDefaultNewsPath));

        final Node previewDefaultNewsNode = session.getNode(previewDefaultNewsPath);
        assertEquals("newsRenamed", previewDefaultNewsNode.getParent().getName());

        // not locked any more
        assertFalse(previewDefaultNewsNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        // now assert it does *not* exist in live!
        assertFalse(session.nodeExists(liveDefaultNewsPath));

        // assert the old location in live still exists
        String oldLiveLocation = liveDefaultNewsPath.replace("newsRenamed", "news");
        assertTrue(session.nodeExists(oldLiveLocation));


        final String previewNewsPath = news.getPath();
        final String liveNewsPath = previewNewsPath.replace("-preview/", "/");

        assertTrue(session.nodeExists(previewNewsPath));
        assertEquals("newsRenamed", session.getNode(previewNewsPath).getName());

        // newsRenamed is still locked by 'bob'
        assertTrue(session.getNode(previewNewsPath).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertEquals("bob", session.getNode(previewNewsPath).getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        assertEquals("foo" ,session.getNode(previewNewsPath).getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH).getString());

        // live path should not exist
        assertFalse(session.nodeExists(liveNewsPath));

        // old live news location should exist
        assertTrue(session.nodeExists(liveNewsPath.replace("newsRenamed", "news")));

    }

}
