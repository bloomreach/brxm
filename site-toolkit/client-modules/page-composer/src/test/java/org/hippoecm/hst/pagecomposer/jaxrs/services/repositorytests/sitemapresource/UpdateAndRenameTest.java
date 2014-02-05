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
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validaters.PreviewWorkspaceNodeValidator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validaters.Validator;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UpdateAndRenameTest extends AbstractSiteMapResourceTest {

    @Test
    public void test_preview_workspace() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL(request, "localhost", "/home");
        ((HstMutableRequestContext) ctx).setSession(session);
        final ContextualizableMount mount =  (ContextualizableMount)ctx.getResolvedMount().getMount();

        {
            final HstSiteMapItem home = mount.getPreviewHstSite().getSiteMap().getSiteMapItem("home");
            assertTrue(home instanceof CanonicalInfo);
            assertTrue( ((CanonicalInfo)home).isWorkspaceConfiguration() );
            final String homeUuid = ((CanonicalInfo)home).getCanonicalIdentifier();
            Validator validator = new PreviewWorkspaceNodeValidator(homeUuid, HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
            validator.validate();
        }

        {
            final HstSiteMapItem news = mount.getPreviewHstSite().getSiteMap().getSiteMapItem("news");
            assertTrue( ((CanonicalInfo)news).isWorkspaceConfiguration() );
            final String newsUuid = ((CanonicalInfo)news).getCanonicalIdentifier();
            new PreviewWorkspaceNodeValidator(newsUuid, HstNodeTypes.NODETYPE_HST_SITEMAPITEM).validate();

            final HstSiteMapItem newsWildcardChild = news.getChild("_default_");
            assertTrue( ((CanonicalInfo)newsWildcardChild).isWorkspaceConfiguration() );
            final String newsWildcardChildUuid = ((CanonicalInfo)newsWildcardChild).getCanonicalIdentifier();
            new PreviewWorkspaceNodeValidator(newsWildcardChildUuid, HstNodeTypes.NODETYPE_HST_SITEMAPITEM).validate();
        }


        {
            final HstSiteMapItem aboutUs = mount.getPreviewHstSite().getSiteMap().getSiteMapItem("about-us");
            assertFalse(((CanonicalInfo) aboutUs).isWorkspaceConfiguration());
            final String aboutUsUuid = ((CanonicalInfo)aboutUs).getCanonicalIdentifier();
            try {
                new PreviewWorkspaceNodeValidator(aboutUsUuid, HstNodeTypes.NODETYPE_HST_SITEMAPITEM).validate();
                fail("Expected PreviewWorkspaceNodeValidator to fail on non workspace sitemap item");
            } catch (IllegalArgumentException e){

            }
        }

    }

    @Test
    public void test_update_properties() throws Exception {

        final SiteMapItemRepresentation home = getSiteMapItemRepresentation("home");
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


        SiteMapResource siteMapResource = new SiteMapResource();
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
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation("home");

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
        SiteMapResource siteMapResource = new SiteMapResource();

        Response response = siteMapResource.update(home);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        // check only acquiring lock now
        try {
            helper.acquireLock(homeNode);
            fail("Expected an IllegalStateException when trying to acquire lock");
        } catch (IllegalStateException e) {
            System.out.println(e.toString());
            assertTrue(e.getMessage().contains("descendant locked node"));
        }

        try {
            helper.acquireLock(homeNodeByBob);
        } catch (IllegalStateException e) {
           fail("Bob should still have the lock");
        }

        bob.logout();

    }

    @Test
    public void test_update_properties_fails_cause_descendant_locked() throws Exception {
        // with bob user we lock first the sitemap item /news/_any_ : This makes 'news' item partially locked:
        // partially locked nodes cannot be moved / renamed or updated any more.
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation("news");
        final SiteMapItemRepresentation newsAny = getSiteMapItemRepresentation("news/_any_");

        final Session bob = createSession("bob", "bob");
        Node newsAnyNodeByBob = bob.getNodeByIdentifier(newsAny.getId());
        SiteMapHelper helper = new SiteMapHelper();
        helper.acquireLock(newsAnyNodeByBob);
        bob.save();

        // now news/_any_ has explicit lock: as a result, 'news' is partially locked

        news.setComponentConfigurationId("foo");
        SiteMapResource siteMapResource = new SiteMapResource();

        session.getRootNode().addNode("dummy-to-show-changes-are-discarded-when-update-fails");
        {
            Response response = siteMapResource.update(news);
            assertEquals("update should fail because of partial lock.",Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
        assertFalse("After failing update, session should had its changes discarded", session.hasPendingChanges());


        // assert update on /news/_default_ still works

        final SiteMapItemRepresentation newsDefault = getSiteMapItemRepresentation("news/_default_");
        {
            newsDefault.setComponentConfigurationId("foobar");
            Response response = siteMapResource.update(newsDefault);
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
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
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            final Node newsNode = session.getNodeByIdentifier(news.getId());
            assertEquals("foo", newsNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID).getString());
            assertEquals("admin", newsNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        }

    }

    @Test
    public void test_update_properties_fails_cause_ancestor_locked() throws Exception {

        final SiteMapItemRepresentation news = getSiteMapItemRepresentation("news");
        final Session bob = createSession("bob", "bob");
        Node newsNodeByBob = bob.getNodeByIdentifier(news.getId());
        SiteMapHelper helper = new SiteMapHelper();
        helper.acquireLock(newsNodeByBob);
        bob.save();


        SiteMapResource siteMapResource = new SiteMapResource();
        // now news/_any_ has implicit lock due to ancestor lock

        final SiteMapItemRepresentation newsAny = getSiteMapItemRepresentation("news/_any_");
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
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            final Node newsAnyNode = session.getNodeByIdentifier(newsAny.getId());
            assertEquals("foo", newsAnyNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID).getString());
            assertEquals("admin", newsAnyNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        }

    }


    @Test
    public void test_update_rename() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation("home");
        Node homeNode = session.getNodeByIdentifier(home.getId());
        String parentPath = homeNode.getParent().getPath();
        home.setName("renamedHome");

        SiteMapResource siteMapResource = new SiteMapResource();
        Response response = siteMapResource.update(home);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("renamedHome", homeNode.getName());
        assertEquals(parentPath + "/renamedHome", homeNode.getPath());

        assertTrue(session.nodeExists(parentPath + "/home"));
        Node deletedMarkerNode = session.getNode(parentPath + "/home");
        assertEquals("admin", deletedMarkerNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());

    }

    @Test
    public void test_update_rename_and_back_again() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation("home");
        Node homeNode = session.getNodeByIdentifier(home.getId());
        String parentPath = homeNode.getParent().getPath();
        home.setName("renamedHome");

        SiteMapResource siteMapResource = new SiteMapResource();
        siteMapResource.update(home);

        home.setName("home");
        Response response = siteMapResource.update(home);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("home", homeNode.getName());
        assertEquals("admin", homeNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        assertFalse(session.nodeExists(parentPath + "/renamedHome"));
    }

    @Test
    public void test_update_rename_and_rename_back_again() throws Exception {

    }


    @Test
    public void test_update_rename_fails_target_existing_sibling() throws Exception {

    }

    @Test
    public void test_update_rename_succeeds_with_sibling_locks() throws Exception {

    }

    @Test
    public void test_update_rename_fails_cause_target_existing_in_non_workspace() throws Exception {

    }

}
