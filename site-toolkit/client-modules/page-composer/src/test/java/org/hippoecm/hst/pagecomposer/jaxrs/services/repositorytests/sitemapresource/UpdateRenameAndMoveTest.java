/*
 * Copyright 2014-2020 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.NotFoundException;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.LockHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.NodePathPrefixValidator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.Validator;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.hst.configuration.HstNodeTypes.EDITABLE_PROPERTY_STATE;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError.UNKNOWN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UpdateRenameAndMoveTest extends AbstractSiteMapResourceTest {

    private LockHelper helper = new LockHelper();

    private long versionStamp = 0;

    @Test
    public void test_preview_workspace() throws Exception {
        final HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "");
        ((HstMutableRequestContext) ctx).setSession(session);
        final Mount mount = ctx.getResolvedMount().getMount();

        {
            final HstSiteMapItem home = mount.getHstSite().getSiteMap().getSiteMapItem("home");
            assertTrue(home instanceof CanonicalInfo);
            assertTrue(((CanonicalInfo) home).isWorkspaceConfiguration());
            final String homeUuid = ((CanonicalInfo) home).getCanonicalIdentifier();
            Validator validator = new NodePathPrefixValidator(getPreviewConfigurationWorkspacePath() , homeUuid, HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
            validator.validate(ctx);
        }

        {
            final HstSiteMapItem news = mount.getHstSite().getSiteMap().getSiteMapItem("news");
            assertTrue(((CanonicalInfo) news).isWorkspaceConfiguration());
            final String newsUuid = ((CanonicalInfo) news).getCanonicalIdentifier();
            new NodePathPrefixValidator(getPreviewConfigurationWorkspacePath(), newsUuid, HstNodeTypes.NODETYPE_HST_SITEMAPITEM).validate(ctx);

            final HstSiteMapItem newsWildcardChild = news.getChild("_default_");
            assertTrue(((CanonicalInfo) newsWildcardChild).isWorkspaceConfiguration());
            final String newsWildcardChildUuid = ((CanonicalInfo) newsWildcardChild).getCanonicalIdentifier();
            new NodePathPrefixValidator(getPreviewConfigurationWorkspacePath(), newsWildcardChildUuid, HstNodeTypes.NODETYPE_HST_SITEMAPITEM).validate(ctx);
        }

        {
            final HstSiteMapItem aboutUs = mount.getHstSite().getSiteMap().getSiteMapItem("about-us");
            assertFalse(((CanonicalInfo) aboutUs).isWorkspaceConfiguration());
            final String aboutUsUuid = ((CanonicalInfo) aboutUs).getCanonicalIdentifier();
            try {
                new NodePathPrefixValidator(getPreviewConfigurationWorkspacePath(), aboutUsUuid, HstNodeTypes.NODETYPE_HST_SITEMAPITEM).validate(ctx);
                fail("Expected PreviewWorkspaceNodeValidator to fail on non workspace sitemap item");
            } catch (ClientException e) {
                assertThat(e.getError(), is(ClientError.ITEM_NOT_CORRECT_LOCATION));
            }
        }

    }

    @Test
    public void test_update_properties() throws Exception {

        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        assertNotNull(home);

        home.setRelativeContentPath("testRelPath");
        home.setPrimaryDocumentRepresentation(null);

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
        helper.acquireLock(homeNodeByBob, versionStamp);
        bob.save();

        final Node homeNode = session.getNodeByIdentifier(home.getId());
        assertTrue(homeNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertEquals("bob", homeNode.getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());

        // attempt now to update home with admin session
        home.setComponentConfigurationId("foo");
        final SiteMapResource siteMapResource = createResource();

        Response response = siteMapResource.update(home);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        // check only acquiring lock now
        try {
            helper.acquireLock(homeNode, versionStamp);
            fail("Expected an IllegalStateException when trying to acquire lock");
        } catch (ClientException e) {
            assertTrue(e.getMessage().contains("cannot be locked"));
        }

        try {
            helper.acquireLock(homeNodeByBob, versionStamp);
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
        helper.acquireLock(newsAnyNodeByBob, versionStamp);
        bob.save();

        // now news/_any_ has explicit lock: as a result, 'news' is partially locked

        news.setRelativeContentPath("foo");
        news.setPrimaryDocumentRepresentation(null);
        final SiteMapResource siteMapResource = createResource();

        session.getRootNode().addNode("dummy-to-show-changes-are-discarded-when-update-fails");
        {
            Response response = siteMapResource.update(news);
            assertEquals("update should fail because of partial lock.", Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
        assertFalse("After failing update, session should had its changes discarded", session.hasPendingChanges());


        // assert update on /news/_default_ still works

        final SiteMapItemRepresentation newsDefault = getSiteMapItemRepresentation(session, "news/2015");
        {
            newsDefault.setRelativeContentPath("foobar");
            newsDefault.setPrimaryDocumentRepresentation(null);
            newsDefault.setParentId(news.getId());

            Response response = siteMapResource.update(newsDefault);
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    Response.Status.OK.getStatusCode(), response.getStatus());
            final Node newsDefaultNode = session.getNodeByIdentifier(newsDefault.getId());
            assertEquals("foobar", newsDefaultNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH).getString());
            assertEquals("admin", newsDefaultNode.getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
        }

        // assert after removing lock from _any_ we can call siteMapResource.update(news);
        newsAnyNodeByBob.removeMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        assertFalse(newsAnyNodeByBob.hasProperty(GENERAL_PROPERTY_LOCKED_BY));
        bob.save();
        bob.logout();

        {
            Response response = siteMapResource.update(news);
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    Response.Status.OK.getStatusCode(), response.getStatus());
            final Node newsNode = session.getNodeByIdentifier(news.getId());
            assertEquals("foo", newsNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH).getString());
            assertEquals("admin", newsNode.getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
        }

    }

    @Test
    public void test_update_properties_fails_cause_ancestor_locked() throws Exception {

        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
        final Session bob = createSession("bob", "bob");
        Node newsNodeByBob = bob.getNodeByIdentifier(news.getId());
        helper.acquireLock(newsNodeByBob, versionStamp);
        bob.save();

        final SiteMapResource siteMapResource = createResource();
        // now news/_any_ has implicit lock due to ancestor lock

        final SiteMapItemRepresentation newsAny = getSiteMapItemRepresentation(session, "news/_any_");
        newsAny.setRelativeContentPath("foo");
        newsAny.setPrimaryDocumentRepresentation(null);
        newsAny.setParentId(news.getId());

        {
            Response response = siteMapResource.update(newsAny);
            assertEquals("update should fail because of partial lock.", Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // assert after removing lock from _any_ we can call siteMapResource.update(news);
        assertTrue(newsNodeByBob.hasProperty(GENERAL_PROPERTY_LOCKED_BY));
        newsNodeByBob.removeMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        assertFalse(newsNodeByBob.hasProperty(GENERAL_PROPERTY_LOCKED_BY));
        bob.save();
        bob.logout();

        {
            Response response = siteMapResource.update(newsAny);
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    Response.Status.OK.getStatusCode(), response.getStatus());
            final Node newsAnyNode = session.getNodeByIdentifier(newsAny.getId());
            assertEquals("foo", newsAnyNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH).getString());
            assertEquals("admin", newsAnyNode.getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
        }

    }

    @Test
    public void test_rename() throws Exception {

        final SiteMapResource siteMapResource = createResource();

        {
            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");

            assertEquals("hst:pages/homepage", home.getComponentConfigurationId());
            Node homeNode = session.getNodeByIdentifier(home.getId());
            String configurationsPath = homeNode.getParent().getParent().getPath();
            String parentPath = homeNode.getParent().getPath();
            home.setName("renamedHome");
            Response response = siteMapResource.update(home);
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    Response.Status.OK.getStatusCode(), response.getStatus());
            assertEquals("renamedHome", homeNode.getName());

            assertEquals("Rename of sitemap item should *not* rename hst page",
                  "hst:pages/homepage", homeNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID).getString());

            assertEquals(parentPath + "/renamedHome", homeNode.getPath());

            assertTrue(session.nodeExists(parentPath + "/home"));
            Node deletedMarkerNode = session.getNode(parentPath + "/home");
            assertEquals("admin", deletedMarkerNode.getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
            assertEquals("deleted", deletedMarkerNode.getProperty(EDITABLE_PROPERTY_STATE).getString());

            // assert page is not deleted and *not* locked
            assertTrue(session.nodeExists(configurationsPath + "/hst:pages/homepage"));
            Node pageNode = session.getNode(configurationsPath + "/hst:pages/homepage");
            assertFalse(pageNode.hasProperty(GENERAL_PROPERTY_LOCKED_BY));
            assertFalse(pageNode.hasProperty(EDITABLE_PROPERTY_STATE));
            assertFalse(session.nodeExists(configurationsPath + "/hst:pages/renamedHomepage"));
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
            final ResponseRepresentation representation = (ResponseRepresentation) bobResponse.getEntity();
            assertThat(representation.getErrorCode(), is(ClientError.ITEM_ALREADY_LOCKED.name()));

            // bob also sees the 'renamedHome' locked
            news.setName("renamedHome");
            Response bobResponse2 = siteMapResource.update(news);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), bobResponse2.getStatus());
            final ResponseRepresentation entity = (ResponseRepresentation) bobResponse2.getEntity();
            assertThat(entity.getErrorCode(), is(ClientError.ITEM_NAME_NOT_UNIQUE.name()));
        }
    }

    @Test
    public void test_rename_to_same_name() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        Node homeNode = session.getNodeByIdentifier(home.getId());
        home.setName("home");
        final SiteMapResource siteMapResource = createResource();
        Response response = siteMapResource.update(home);
        assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(homeNode.hasProperty(GENERAL_PROPERTY_LOCKED_BY));
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
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    Response.Status.OK.getStatusCode(), response.getStatus());
            assertEquals("home", homeNode.getName());
            assertEquals("admin", homeNode.getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
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


        }
        // because of Jackrabbit issue we need a fresh session for tests with this move because
        // JR sometimes throws a incorrect repository exception during SessionMoveOperation
        {
            final Session admin = createSession("admin", "admin");
            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(admin, "rename1");
            // trigger model reload
            home.setName("rename2");
            siteMapResource.update(home);
            admin.logout();
        }

        {
            final Session admin = createSession("admin", "admin");
            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(admin, "rename2");
            home.setName("home");
            Response response = siteMapResource.update(home);
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    Response.Status.OK.getStatusCode(), response.getStatus());
            assertEquals("home", home.getName());
            Node homeNode = admin.getNodeByIdentifier(home.getId());
            assertEquals("admin", homeNode.getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
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
        try {
            helper.acquireLock(deleteHomeNodeByBob, versionStamp);
            fail("Bob should 'see' locked deleted home node");
        } catch (ClientException e) {

        }

        Node deleteHomeNodeByAdmin = session.getNode(oldHomeParentPath + "/home");
        try {
            helper.acquireLock(deleteHomeNodeByAdmin, versionStamp);
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
            assertEquals("admin", renamedNewsNode.getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
            assertFalse(renamedNewsNode.hasProperty(EDITABLE_PROPERTY_STATE));
            admin.logout();
        }

        // bob sees the new home item locked
        Node moveNewsNodeToHome = bob.getNode(oldHomeParentPath + "/home");
        try {
            helper.acquireLock(moveNewsNodeToHome, versionStamp);
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
        final ResponseRepresentation entity = (ResponseRepresentation) response.getEntity();
        assertThat(entity.getErrorCode(), is(ClientError.ITEM_NAME_NOT_UNIQUE.name()));
    }

    @Test
    public void test_rename_succeeds_with_sibling_locks() throws Exception {
        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");

        // lock home by 'bob'
        final Session bob = createSession("bob", "bob");
        helper.acquireLock(bob.getNodeByIdentifier(home.getId()), versionStamp);
        bob.save();

        // assert home is locked for 'admin'
        home.setName("renamed");
        Response failResponse = siteMapResource.update(home);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), failResponse.getStatus());
        final ResponseRepresentation entity = (ResponseRepresentation) failResponse.getEntity();
        assertThat(entity.getErrorCode(), is(ClientError.ITEM_ALREADY_LOCKED.name()));

        // news should still be possible to move
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
        Response response = siteMapResource.update(news);
        assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
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
        final String errorCode = ((ResponseRepresentation) failResponse.getEntity()).getErrorCode();
        assertThat(errorCode, is(ClientError.ITEM_EXISTS_OUTSIDE_WORKSPACE.name()));
    }

    @Test
    public void test_rename_succeeds_when_no_non_workspace_sitemap() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap").remove();
        session.save();
        final SiteMapResource siteMapResource = createResource();
        try {
            final SiteMapItemRepresentation aboutUs = getSiteMapItemRepresentation(session, "about-us");
            fail("'about-us' sitemap item AND all wildcard matchers AND pageNotFound sitemap item have been removed " +
                    "and thus 'about-us' is not expected to match");
        } catch (NotFoundException e){
            // expected
        }

        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        // 'about-us' is not part any more of the non-workspace sitemap and thus rename to 'about-us' should pass
        home.setName("about-us");
        Response correctResponse = siteMapResource.update(home);
        assertEquals(Response.Status.OK.getStatusCode(), correctResponse.getStatus());

    }

    @Test
    public void validate_invalid_renames() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        for (String invalidChar : new String[]{"?", ";", "#", "\\"}) {
            invalidRenameAssertions(home, invalidChar, "Invalid pathInfo");
        }
        for (String invalidChar : new String[]{":", "/"}) {
            invalidRenameAssertions(home, invalidChar, "is invalid");
        }
        // %3A = :
        // %2F = /
        // %2f = /
        // %5c = \
        // %5C = \
        // %2e = .
        // %2E = .
        // %3F = ?
        // %3B = ;
        // %23 = #
        for (String checkURLEncodedChar : new String[]{"%3A", "%2F", "%2f", "%5c", "%5C", "%2e", "%2E", "%3F", "%3B", "%23"}) {
            invalidRenameAssertions(home, checkURLEncodedChar, "Invalid pathInfo");
        }
    }

    private void invalidRenameAssertions(final SiteMapItemRepresentation home, final String invalidChar, final String messagePart) {
        home.setName("ho" + invalidChar+"me");
        final SiteMapResource siteMapResource = createResource();
        Response response = siteMapResource.update(home);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertTrue(((ResponseRepresentation) response.getEntity()).getMessage().contains(messagePart));
    }

    @Test
    public void test_move_succeeds() throws Exception {
        moveAndAssertHomeToNews(null, null);
    }

    private void moveAndAssertHomeToNews(final String newName, final String expectedHomePageNodeLockedBy) throws Exception {
        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
        assertEquals("hst:pages/homepage", home.getComponentConfigurationId());
        Node homeNode = session.getNodeByIdentifier(home.getId());
        Node newsNode = session.getNodeByIdentifier(news.getId());
        String oldLocation = homeNode.getPath();
        String configurationsPath = homeNode.getParent().getParent().getPath();

        home.setParentId(news.getId());
        final String finalName;
        if (newName != null){
            home.setName(newName);
            finalName = newName;
        } else {
            finalName = home.getName();
        }

        Response response = siteMapResource.update(home);
        if (response.getStatus() != 200) {
            throw new ClientException("failed", UNKNOWN);
        }
        assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                Response.Status.OK.getStatusCode(), response.getStatus());

        assertTrue(session.nodeExists(newsNode.getPath() + "/" + finalName));
        Node deletedMarkerNode = session.getNode(oldLocation);
        assertEquals("admin", deletedMarkerNode.getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
        assertEquals("deleted", deletedMarkerNode.getProperty(EDITABLE_PROPERTY_STATE).getString());


        assertTrue(session.nodeExists(configurationsPath + "/hst:pages/homepage"));
        Node pageNode = session.getNode(configurationsPath + "/hst:pages/homepage");
        if (expectedHomePageNodeLockedBy == null) {
            // assert page is not deleted and *not* locked as result of move of sitemap item
            assertFalse(pageNode.hasProperty(GENERAL_PROPERTY_LOCKED_BY));
            assertFalse(pageNode.hasProperty(EDITABLE_PROPERTY_STATE));
        } else {
            // assert page lock is not affected as result of move of sitemap item
            assertEquals(expectedHomePageNodeLockedBy, pageNode.getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
        }

        // assert home page node does not get renamed as a result of a rename
        assertFalse(session.nodeExists(configurationsPath + "/hst:pages/renamedHomepage"));
    }

    @Test
    public void test_move_succeeds_if_target_contains_locked_child() throws Exception {
        final SiteMapItemRepresentation newsDefaultMatcher = getSiteMapItemRepresentation(session, "news/2015");
        final Node newsDefaultMatcherNode = session.getNodeByIdentifier(newsDefaultMatcher.getId());
        new LockHelper().acquireLock(newsDefaultMatcherNode, "bob", 0L);
        session.save();
        moveAndAssertHomeToNews(null, null);
    }

    @Test(expected = ClientException.class)
    public void test_move_fails_if_target_is_locked() throws Exception {
        final SiteMapItemRepresentation newsDefaultMatcher = getSiteMapItemRepresentation(session, "news");
        final Node newsDefaultMatcherNode = session.getNodeByIdentifier(newsDefaultMatcher.getId());
        new LockHelper().acquireLock(newsDefaultMatcherNode, "bob", 0L);
        session.save();
        moveAndAssertHomeToNews(null, null);
    }

    @Test
    public void test_move_and_rename() throws Exception {
        moveAndAssertHomeToNews("foo", null);
    }

    @Test
    public void test_move_and_rename_succeed_if_a_container_in_backing_page_is_locked_by_someone_else() throws Exception {
        // first lock page that is used by 'home' page by bob
        Node homePageNode = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/homepage");
        new LockHelper().acquireLock(homePageNode, "bob", 0L);
        session.save();
        moveAndAssertHomeToNews(null, "bob");
    }

    @Test
    public void move_non_workspace_site_map_item_fails() throws Exception {
        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        final SiteMapItemRepresentation notFound = getSiteMapItemRepresentation(session, "xyz");

        assertEquals("hst:pages/pagenotfound",notFound.getComponentConfigurationId());
        assertFalse(notFound.isWorkspaceConfiguration());

        notFound.setParentId(home.getId());
        final Response update = siteMapResource.update(notFound);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), update.getStatus());
        final String message = ((ResponseRepresentation)update.getEntity()).getMessage();
        assertTrue(message.contains("is not part of required node path"));
    }


    @Test
    public void after_move_a_descendant_of_moved_item_can_be_moved_by_same_user() throws Exception {
        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");

        Node homeNode = session.getNodeByIdentifier(home.getId());

        // move news to 'home'
        news.setParentId(home.getId());
        siteMapResource.update(news);
        assertTrue(session.nodeExists(homeNode.getPath() + "/" + "news"));
        assertTrue(session.nodeExists(homeNode.getPath() + "/" + "news/_default_"));

        final SiteMapItemRepresentation newsDefault = getSiteMapItemRepresentation(session, "home/news/2015");
        // try to move /home/news/_default_ to /home/_default_
        newsDefault.setParentId(home.getId());
        siteMapResource.update(newsDefault);
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/home/_default_"));
    }

    @Test
    public void move_fails_cause_target_existing_in_non_workspace() throws Exception {
        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");

        Node homeNode = session.getNodeByIdentifier(home.getId());

        // move news to a child of 'home'
        news.setParentId(home.getId());
        siteMapResource.update(news);
        assertTrue(session.nodeExists(homeNode.getPath() + "/" + "news"));
        assertTrue(session.nodeExists(homeNode.getPath() + "/" + "news/_default_"));

        final SiteMapItemRepresentation newsDefault = getSiteMapItemRepresentation(session, "home/news/2015");
        // try to move /home/news/_default_ to /_default_ : Should fail since the non-workspace sitemap already contains _default_
        newsDefault.setParentId(null);

        final Response update = siteMapResource.update(newsDefault);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), update.getStatus());
        assertThat(((ResponseRepresentation) update.getEntity()).getErrorCode(), is(ClientError.ITEM_EXISTS_OUTSIDE_WORKSPACE.name()));
    }

    @Test
    public void after_move_a_descendant_of_moved_item_cannot_be_moved_by_someone_else() throws Exception {
        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");

        Node homeNode = session.getNodeByIdentifier(home.getId());

        // move news to 'home'
        news.setParentId(home.getId());
        siteMapResource.update(news);
        assertTrue(session.nodeExists(homeNode.getPath() + "/" + "news"));
        assertTrue(session.nodeExists(homeNode.getPath() + "/" + "news/_default_"));

        // since /home/news is now locked by 'admin', we should not be able to move '/home/news/_default_' by bob
        final Session bob = createSession("bob", "bob");
        try {
            final SiteMapItemRepresentation newsDefaultByBob = getSiteMapItemRepresentation(bob, "home/news/2015");
            // try to move /home/news/_default_ to /home/_default_
            newsDefaultByBob.setParentId(home.getId());

            final Response update = siteMapResource.update(newsDefaultByBob);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), update.getStatus());
            final String message = ((ResponseRepresentation)update.getEntity()).getMessage();
            assertTrue(message.contains("cannot be locked due to someone else who has the lock"));
        } finally {
            if (bob != null) {
                bob.logout();
            }
        }
    }

}
