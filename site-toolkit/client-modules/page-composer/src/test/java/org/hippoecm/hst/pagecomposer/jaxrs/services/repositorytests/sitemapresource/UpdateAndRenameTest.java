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

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validaters.PreviewWorkspaceNodeValidator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validaters.Validator;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UpdateAndRenameTest extends AbstractSiteMapResourceTest {

    @Test
    public void test_preview_workspace() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL(request, "localhost", "/home");
        ((HstMutableRequestContext) ctx).setSession(session);
        final ContextualizableMount mount = (ContextualizableMount) ctx.getResolvedMount().getMount();

        {
            final HstSiteMapItem home = mount.getPreviewHstSite().getSiteMap().getSiteMapItem("home");
            assertTrue(home instanceof CanonicalInfo);
            assertTrue(((CanonicalInfo) home).isWorkspaceConfiguration());
            final String homeUuid = ((CanonicalInfo) home).getCanonicalIdentifier();
            Validator validator = new PreviewWorkspaceNodeValidator(homeUuid, HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
            validator.validate(ctx);
        }

        {
            final HstSiteMapItem news = mount.getPreviewHstSite().getSiteMap().getSiteMapItem("news");
            assertTrue(((CanonicalInfo) news).isWorkspaceConfiguration());
            final String newsUuid = ((CanonicalInfo) news).getCanonicalIdentifier();
            new PreviewWorkspaceNodeValidator(newsUuid, HstNodeTypes.NODETYPE_HST_SITEMAPITEM).validate(ctx);

            final HstSiteMapItem newsWildcardChild = news.getChild("_default_");
            assertTrue(((CanonicalInfo) newsWildcardChild).isWorkspaceConfiguration());
            final String newsWildcardChildUuid = ((CanonicalInfo) newsWildcardChild).getCanonicalIdentifier();
            new PreviewWorkspaceNodeValidator(newsWildcardChildUuid, HstNodeTypes.NODETYPE_HST_SITEMAPITEM).validate(ctx);
        }

        {
            final HstSiteMapItem aboutUs = mount.getPreviewHstSite().getSiteMap().getSiteMapItem("about-us");
            assertFalse(((CanonicalInfo) aboutUs).isWorkspaceConfiguration());
            final String aboutUsUuid = ((CanonicalInfo) aboutUs).getCanonicalIdentifier();
            try {
                new PreviewWorkspaceNodeValidator(aboutUsUuid, HstNodeTypes.NODETYPE_HST_SITEMAPITEM).validate(ctx);
                fail("Expected PreviewWorkspaceNodeValidator to fail on non workspace sitemap item");
            } catch (ClientException e) {
                assertThat(e.getError(), is(ClientError.ITEM_NOT_IN_WORKSPACE));
            }
        }

    }

    @Test
    public void test_update_properties() throws Exception {

        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        assertNotNull(home);

        home.setComponentConfigurationId("testCompId");
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

        Node homeNode = session.getNodeByIdentifier(home.getId());
        assertEquals("testCompId", homeNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID).getString());
        assertEquals("testRelPath", homeNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH).getString());
        assertEquals("https", homeNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_SCHEME).getString());


        Set<String> storedRoles = new HashSet<>();
        for (Value value : homeNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_ROLES).getValues()) {
            storedRoles.add(value.getString());
        }
        assertEquals(storedRoles, roles);

        Map<String, String> storedLocalParams = new HashMap<>();
        List<String> keys = new ArrayList<>();
        for (Value value : homeNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES).getValues()) {
            keys.add(value.getString());
        }
        List<String> values = new ArrayList<>();
        for (Value value : homeNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES).getValues()) {
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
    public void test_update_properties_fails_cause_node_locked() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");

        final Session bob = createSession("bob", "bob");
        Node homeNodeByBob = bob.getNodeByIdentifier(home.getId());
        SiteMapHelper helper = new SiteMapHelper();
        helper.acquireLock(homeNodeByBob);
        bob.save();

        final Node homeNode = session.getNodeByIdentifier(home.getId());
        assertTrue(homeNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertEquals("bob", homeNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());

        // attempt now to update home with admin session
        home.setComponentConfigurationId("foo");
        final SiteMapResource siteMapResource = createResource();

        Response response = siteMapResource.update(home);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        // check only acquiring lock now
        try {
            helper.acquireLock(homeNode);
            fail("Expected an IllegalStateException when trying to acquire lock");
        } catch (ClientException e) {
            assertTrue(e.getMessage().contains("cannot be locked"));
        }

        try {
            helper.acquireLock(homeNodeByBob);
        } catch (ClientException e) {
            fail("Bob should still have the lock");
        }

        bob.logout();

    }

    @Test
    public void test_update_properties_fails_cause_descendant_locked() throws Exception {
        // with bob user we lock first the sitemap item /news/_any_ : This makes 'news' item partially locked:
        // partially locked nodes cannot be moved / renamed or updated any more.
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
        final SiteMapItemRepresentation newsAny = getSiteMapItemRepresentation(session, "news/_any_");

        final Session bob = createSession("bob", "bob");
        Node newsAnyNodeByBob = bob.getNodeByIdentifier(newsAny.getId());
        SiteMapHelper helper = new SiteMapHelper();
        helper.acquireLock(newsAnyNodeByBob);
        bob.save();

        // now news/_any_ has explicit lock: as a result, 'news' is partially locked

        news.setComponentConfigurationId("foo");
        final SiteMapResource siteMapResource = createResource();

        session.getRootNode().addNode("dummy-to-show-changes-are-discarded-when-update-fails");
        {
            Response response = siteMapResource.update(news);
            assertEquals("update should fail because of partial lock.", Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
        assertFalse("After failing update, session should had its changes discarded", session.hasPendingChanges());


        // assert update on /news/_default_ still works

        final SiteMapItemRepresentation newsDefault = getSiteMapItemRepresentation(session, "news/_default_");
        {
            newsDefault.setComponentConfigurationId("foobar");
            Response response = siteMapResource.update(newsDefault);
            assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                    Response.Status.OK.getStatusCode(), response.getStatus());
            final Node newsDefaultNode = session.getNodeByIdentifier(newsDefault.getId());
            assertEquals("foobar", newsDefaultNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID).getString());
            assertEquals("admin", newsDefaultNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        }

        // assert after removing lock from _any_ we can call siteMapResource.update(news);
        newsAnyNodeByBob.removeMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        assertFalse(newsAnyNodeByBob.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));
        bob.save();
        bob.logout();

        {
            Response response = siteMapResource.update(news);
            assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                    Response.Status.OK.getStatusCode(), response.getStatus());
            final Node newsNode = session.getNodeByIdentifier(news.getId());
            assertEquals("foo", newsNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID).getString());
            assertEquals("admin", newsNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        }

    }

    @Test
    public void test_update_properties_fails_cause_ancestor_locked() throws Exception {

        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
        final Session bob = createSession("bob", "bob");
        Node newsNodeByBob = bob.getNodeByIdentifier(news.getId());
        SiteMapHelper helper = new SiteMapHelper();
        helper.acquireLock(newsNodeByBob);
        bob.save();

        final SiteMapResource siteMapResource = createResource();
        // now news/_any_ has implicit lock due to ancestor lock

        final SiteMapItemRepresentation newsAny = getSiteMapItemRepresentation(session, "news/_any_");
        newsAny.setComponentConfigurationId("foo");
        {
            Response response = siteMapResource.update(newsAny);
            assertEquals("update should fail because of partial lock.", Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // assert after removing lock from _any_ we can call siteMapResource.update(news);
        assertTrue(newsNodeByBob.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));
        newsNodeByBob.removeMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        assertFalse(newsNodeByBob.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));
        bob.save();
        bob.logout();

        {
            Response response = siteMapResource.update(newsAny);
            assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                    Response.Status.OK.getStatusCode(), response.getStatus());
            final Node newsAnyNode = session.getNodeByIdentifier(newsAny.getId());
            assertEquals("foo", newsAnyNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID).getString());
            assertEquals("admin", newsAnyNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        }

    }

    @Test
    public void test_rename() throws Exception {

        final SiteMapResource siteMapResource = createResource();

        {
            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
            Node homeNode = session.getNodeByIdentifier(home.getId());
            String parentPath = homeNode.getParent().getPath();
            home.setName("renamedHome");
            Response response = siteMapResource.update(home);
            assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                    Response.Status.OK.getStatusCode(), response.getStatus());
            assertEquals("renamedHome", homeNode.getName());
            assertEquals(parentPath + "/renamedHome", homeNode.getPath());

            assertTrue(session.nodeExists(parentPath + "/home"));
            Node deletedMarkerNode = session.getNode(parentPath + "/home");
            assertEquals("admin", deletedMarkerNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
            assertEquals("deleted", deletedMarkerNode.getProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE).getString());
        }
        // assert bob cannot rename 'news' to 'home' now as it is locked.
        // also bob cannot rename 'renamedHome' as is locked

        {
            // bob sees the old home item locked
            final Session bob = createSession("bob", "bob");
            final SiteMapItemRepresentation news = getSiteMapItemRepresentation(bob, "news");
            news.setName("home");
            Response bobResponse = siteMapResource.update(news);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), bobResponse.getStatus());
            final ExtResponseRepresentation representation = (ExtResponseRepresentation) bobResponse.getEntity();
            assertThat(representation.getMessage(), is(ClientError.ITEM_ALREADY_LOCKED.name()));

            // bob also sees the 'renamedHome' locked
            news.setName("renamedHome");
            Response bobResponse2 = siteMapResource.update(news);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), bobResponse2.getStatus());
            final ExtResponseRepresentation entity = (ExtResponseRepresentation) bobResponse2.getEntity();
            assertThat(entity.getMessage(), is(ClientError.ITEM_NAME_NOT_UNIQUE.name()));
        }
    }

    @Test
    public void test_rename_to_same_name() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        Node homeNode = session.getNodeByIdentifier(home.getId());
        home.setName("home");
        final SiteMapResource siteMapResource = createResource();
        Response response = siteMapResource.update(home);
        assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(homeNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));
    }

    @Test
    public void test_rename_and_back_again() throws Exception {
        final SiteMapResource siteMapResource = createResource();
        String parentPath;
        {
            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
            Node homeNode = session.getNodeByIdentifier(home.getId());
            parentPath = homeNode.getParent().getPath();
            home.setName("renamedHome");
            siteMapResource.update(home);
        }

        {
            // because of Jackrabbit issue we need a fresh session for tests with this move because
            // JR sometimes throws a incorrect repository exception during SessionMoveOperation
            final Session admin = createSession("admin", "admin");
            final SiteMapItemRepresentation renamedHome = getSiteMapItemRepresentation(admin, "renamedHome");
            renamedHome.setName("home");
            Response response = siteMapResource.update(renamedHome);
            Node homeNode = admin.getNodeByIdentifier(renamedHome.getId());
            assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                    Response.Status.OK.getStatusCode(), response.getStatus());
            assertEquals("home", homeNode.getName());
            assertEquals("admin", homeNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
            assertFalse(admin.nodeExists(parentPath + "/renamedHome"));
            admin.logout();
        }

    }


    @Test
    public void test_rename_and_rename_and_back_again() throws Exception {
        final SiteMapResource siteMapResource = createResource();
        String parentPath;
        {
            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
            Node homeNode = session.getNodeByIdentifier(home.getId());
            parentPath = homeNode.getParent().getPath();

            home.setName("rename1");
            siteMapResource.update(home);


            home.setName("rename2");
            siteMapResource.update(home);
        }
        // because of Jackrabbit issue we need a fresh session for tests with this move because
        // JR sometimes throws a incorrect repository exception during SessionMoveOperation
        {
            final Session admin = createSession("admin", "admin");
            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(admin, "rename2");
            home.setName("home");
            Response response = siteMapResource.update(home);
            assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                    Response.Status.OK.getStatusCode(), response.getStatus());
            assertEquals("home", home.getName());
            Node homeNode = admin.getNodeByIdentifier(home.getId());
            assertEquals("admin", homeNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
            assertFalse(admin.nodeExists(parentPath + "/rename1"));
            assertFalse(admin.nodeExists(parentPath + "/rename2"));
            admin.logout();
        }

    }

    @Test
    public void test_rename_home_and_rename_news_to_home() throws Exception {

        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        String oldHomeParentPath = session.getNodeByIdentifier(home.getId()).getParent().getPath();
        home.setName("renamedHome");

        final SiteMapResource siteMapResource = createResource();
        siteMapResource.update(home);

        // bob sees the old home item locked
        final Session bob = createSession("bob", "bob");
        Node deleteHomeNodeByBob = bob.getNode(oldHomeParentPath + "/home");
        SiteMapHelper helper = new SiteMapHelper();
        try {
            helper.acquireLock(deleteHomeNodeByBob);
            fail("Bob should 'see' locked deleted home node");
        } catch (ClientException e) {

        }

        Node deleteHomeNodeByAdmin = session.getNode(oldHomeParentPath + "/home");
        try {
            helper.acquireLock(deleteHomeNodeByAdmin);
        } catch (IllegalStateException e) {
            fail("Admin should have the locked deleted home node");
        }


        // because of Jackrabbit issue we need a fresh session for tests with this move because
        // JR sometimes throws a incorrect repository exception during SessionMoveOperation
        {
            final Session admin = createSession("admin", "admin");
            final SiteMapItemRepresentation news = getSiteMapItemRepresentation(admin, "news");
            news.setName("home");
            siteMapResource.update(news);

            assertTrue(admin.nodeExists(oldHomeParentPath + "/home"));
            Node renamedNewsNode = admin.getNode(oldHomeParentPath + "/home");
            assertEquals("admin", renamedNewsNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
            assertFalse(renamedNewsNode.hasProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE));
            admin.logout();
        }

        // bob sees the new home item locked
        Node moveNewsNodeToHome = bob.getNode(oldHomeParentPath + "/home");
        try {
            helper.acquireLock(moveNewsNodeToHome);
            fail("Bob should 'see' the news move to home node as locked");
        } catch (ClientException e) {

        }
        bob.logout();
    }

    @Test
    public void test_rename_fails_target_existing_sibling() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        // news already exists
        home.setName("news");
        final SiteMapResource siteMapResource = createResource();
        Response response = siteMapResource.update(home);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        final ExtResponseRepresentation entity = (ExtResponseRepresentation) response.getEntity();
        assertThat(entity.getMessage(), is(ClientError.ITEM_NAME_NOT_UNIQUE.name()));
    }

    @Test
    public void test_rename_succeeds_with_sibling_locks() throws Exception {
        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");

        SiteMapHelper helper = new SiteMapHelper();
        // lock home by 'bob'
        final Session bob = createSession("bob", "bob");
        helper.acquireLock(bob.getNodeByIdentifier(home.getId()));
        bob.save();

        // assert home is locked for 'admin'
        home.setName("renamed");
        Response failResponse = siteMapResource.update(home);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), failResponse.getStatus());
        final ExtResponseRepresentation entity = (ExtResponseRepresentation) failResponse.getEntity();
        assertThat(entity.getMessage(), is(ClientError.ITEM_ALREADY_LOCKED.name()));

        // news should still be possible to move
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
        Response response = siteMapResource.update(news);
        assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                Response.Status.OK.getStatusCode(), response.getStatus());

        bob.logout();
    }

    @Test
    public void test_rename_fails_cause_target_existing_in_non_workspace() throws Exception {
        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation aboutUs = getSiteMapItemRepresentation(session, "about-us");
        assertNotNull(aboutUs);
        // about-us is part of non-workspace
        assertFalse(aboutUs.isWorkspaceConfiguration());
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");

        // 'about-us' is part of the non-workspace sitemap and should thus fail!
        home.setName("about-us");
        Response failResponse = siteMapResource.update(home);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), failResponse.getStatus());
        final String message = ((ExtResponseRepresentation) failResponse.getEntity()).getMessage();
        assertThat(message, is(ClientError.ITEM_EXISTS_IN_NON_WORKSPACE.name()));
    }

    @Test
    public void test_rename_succeeds_when_no_non_workspace_sitemap() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap").remove();
        session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:sitemap").remove();
        session.save();
        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation aboutUs = getSiteMapItemRepresentation(session, "about-us");
        assertNull(aboutUs);
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");

        // 'about-us' is part of the non-workspace sitemap and should thus fail!
        home.setName("about-us");
        Response correctResponse = siteMapResource.update(home);
        assertEquals(Response.Status.OK.getStatusCode(), correctResponse.getStatus());

    }

}
