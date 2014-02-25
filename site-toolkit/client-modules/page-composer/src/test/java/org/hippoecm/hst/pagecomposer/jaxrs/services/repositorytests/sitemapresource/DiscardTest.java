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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DiscardTest extends AbstractSiteMapResourceTest {

    @Test
    public void test_multi_user_change_single_user_discard() throws Exception {
        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");

        final String previewHomePathBefore = session.getNodeByIdentifier(home.getId()).getPath();

        home.setName("adminHome");
        siteMapResource.update(home);

        final String previewHomePathAfter = session.getNodeByIdentifier(home.getId()).getPath();

        final Session bob = createSession("bob", "bob");

        final SiteMapItemRepresentation newsByBob = getSiteMapItemRepresentation(bob, "news");

        final String previewNewsPathBefore = session.getNodeByIdentifier(newsByBob.getId()).getPath();

        newsByBob.setName("bobNews");
        siteMapResource.update(newsByBob);

        final String previewNewsPathAfter = session.getNodeByIdentifier(newsByBob.getId()).getPath();

        assertNotSame(previewNewsPathBefore, previewNewsPathAfter);

        // bobs session is currently on the requestcontextprovider, hence discard below should discard bobs changes only
        mountResource.discardChanges();
        bob.logout();

        // the 'news' rename has been reverted
        assertTrue(session.nodeExists(previewNewsPathBefore));
        assertFalse(session.nodeExists(previewNewsPathAfter));

        // the 'home' rename has *not* been reverted.
        assertTrue(session.nodeExists(previewHomePathBefore));
        assertTrue(session.getNode(previewHomePathBefore).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertEquals("admin", session.getNode(previewHomePathBefore).getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        assertTrue(session.nodeExists(previewHomePathAfter));

    }

    @Test
    public void test_succeeds_illegal_state_ancestor_multi_user_lock_discard() throws Exception {
        // this test is to ensure that when we have the following exceptional situation

        // -news (locked by 'admin')
        //    ` * (locked by 'bob')

        // that when discarding by admin, also * gets discarded and lock of 'bob' gets removed.

        // NOTE the above is about handling incorrect states, which can occur due to concurrency or clustering

        // below ADMIN discards
        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
        news.setComponentConfigurationId("bar");
        siteMapResource.update(news);

        final String previewNewsPath= session.getNodeByIdentifier(news.getId()).getPath();
        final String previewDefaultNewsPath= previewNewsPath + "/_default_";

        // now on jcr level modify the news/* item to be locked by bob. This cannot be done through siteMapResource
        // as that will result in lock exceptions
        final Node newsDefault = session.getNodeByIdentifier(news.getId()).getNode("_default_");
        String newsDefaultId = newsDefault.getIdentifier();
        // make it now as if it is locked by 'bob'
        newsDefault.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        newsDefault.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, "bob");
        newsDefault.setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID, "foo");

        session.save();

        // now discard by 'admin'..... 'news/_default_' contains not allowed lock. It should be removed
        mountResource.discardChanges();

        try {
            session.getNodeByIdentifier(news.getId());
            fail(previewNewsPath + " should have its id replaced");
        } catch (ItemNotFoundException e) {
            // expected
        }
        try {
            session.getNodeByIdentifier(newsDefaultId);
            fail(previewNewsPath + "/_default_ should have its id replaced");
        } catch (ItemNotFoundException e) {
            // expected
        }

        assertTrue(session.nodeExists(previewNewsPath));
        assertFalse(session.getNode(previewNewsPath).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertNotSame("bar", session.getNode(previewNewsPath).getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID).getString());

        assertTrue(session.nodeExists(previewDefaultNewsPath));
        assertFalse(session.getNode(previewDefaultNewsPath).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertNotSame("foo" ,session.getNode(previewDefaultNewsPath).getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID).getString());

    }

    @Test
    public void test_succeeds_illegal_state_descendant_multi_user_lock_discard() throws Exception {
        // this test is to ensure that when we have the following exceptional situation

        // -news (locked by 'bob')
        //    ` * (locked by 'admin')

        // that when discarding by admin, only news/* gets discarded and lock of 'admin' gets removed. 'news' stay locked
        // by bob

        // NOTE the above is about handling incorrect states, which can occur due to concurrency or clustering

        // below ADMIN discards
        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation newsDefault = getSiteMapItemRepresentation(session, "news/_default_");
        final String previewDefaultNewsPath = session.getNodeByIdentifier(newsDefault.getId()).getPath();
        String newsDefaultComponentConfigIdBefore = newsDefault.getComponentConfigurationId();
        newsDefault.setComponentConfigurationId("bar");
        siteMapResource.update(newsDefault);

        // now on jcr level modify the 'news' item to be locked by bob. This cannot be done through siteMapResource
        // as that will result in lock exceptions
        final Node news = session.getNodeByIdentifier(newsDefault.getId()).getParent();
        // make it now as if it is locked by 'bob'
        news.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        news.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, "bob");
        news.setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID, "foo");

        session.save();

        // now discard by 'admin'. 'news' should keep lock by 'bob'
        mountResource.discardChanges();

        final String previewNewsPath = news.getPath();

        assertTrue(session.nodeExists(previewDefaultNewsPath));
        // previewDefault should be discarded
        assertFalse(session.getNode(previewDefaultNewsPath).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertEquals(newsDefaultComponentConfigIdBefore,
                session.getNode(previewDefaultNewsPath).getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID).getString());

        assertTrue(session.nodeExists(previewNewsPath));
        // news is still locked by 'bob'
        assertTrue(session.getNode(previewNewsPath).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertEquals("bob", session.getNode(previewNewsPath).getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        assertEquals("foo", session.getNode(previewNewsPath).getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID).getString());

    }

    @Test
    public void test_illegal_state_multi_lock_with_rename_descendant_locked_no_discard() throws Exception {
        // this test is to ensure that when we have the following exceptional situation

        // -newsRenamed (locked by 'bob')
        //    ` * (locked by 'admin')

        // when 'admin' tries to discard, it cannot discard from live since live does not have 'renamedNews'
        // only the lock gets removed from renamedNews/* since in the first place, 'bob' should be the
        // owner any way

        // NOTE the above is about handling incorrect states, which can occur due to concurrency or clustering

        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation newsDefault = getSiteMapItemRepresentation(session, "news/_default_");
        newsDefault.setComponentConfigurationId("bar");
        siteMapResource.update(newsDefault);

        // now on jcr level rename 'news' and lock it for bob. This cannot be done through siteMapResource
        // as that will result in lock exceptions
        final Node news = session.getNodeByIdentifier(newsDefault.getId()).getParent();
        // make it now as if it is locked by 'bob'
        news.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        news.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, "bob");
        news.setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID, "foo");
        session.move(news.getPath(), news.getPath() + "Renamed");
        session.save();

        // now discard by 'admin'. Discard should not revert the preview, only lock by 'admin' removed from 'newsRenamed/*
        mountResource.discardChanges();

        final String previewDefaultNewsPath= session.getNodeByIdentifier(newsDefault.getId()).getPath();

        assertTrue(session.nodeExists(previewDefaultNewsPath));

        final Node previewDefaultNewsNode = session.getNode(previewDefaultNewsPath);
        assertEquals("newsRenamed", previewDefaultNewsNode.getParent().getName());

        // not locked any more
        assertFalse(previewDefaultNewsNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));

        // assert component config id is not reverted from live but still 'bar'
        assertEquals("bar", previewDefaultNewsNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID).getString());


        final String previewNewsPath = news.getPath();

        assertTrue(session.nodeExists(previewNewsPath));
        assertEquals("newsRenamed", session.getNode(previewNewsPath).getName());

        // newsRenamed is still locked by 'bob'
        assertTrue(session.getNode(previewNewsPath).isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertEquals("bob", session.getNode(previewNewsPath).getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        assertEquals("foo" ,session.getNode(previewNewsPath).getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID).getString());

    }
}
