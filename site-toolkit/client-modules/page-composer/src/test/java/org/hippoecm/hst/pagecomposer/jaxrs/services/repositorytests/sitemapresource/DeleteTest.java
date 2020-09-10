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

import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.pagecomposer.jaxrs.model.ResponseRepresentation;
import org.onehippo.cms7.services.hst.Channel;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapPageRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.LockHelper;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.hst.configuration.HstNodeTypes.EDITABLE_PROPERTY_STATE;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DeleteTest extends AbstractSiteMapResourceTest {

    private LockHelper helper = new LockHelper();

    private void initContext() throws Exception {
        // call below will init request context
        getSiteMapItemRepresentation(session, "home");
    }

    @Test
    public void test_delete_with_page() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");

        final HstComponentConfiguration componentConfiguration = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(home.getComponentConfigurationId());

        final SiteMapResource siteMapResource = createResource();
        final Response delete = siteMapResource.delete(home.getId());
        assertEquals(Response.Status.OK.getStatusCode(), delete.getStatus());
        assertTrue(((ResponseRepresentation) delete.getEntity()).getMessage().contains("deleted"));

        assertTrue(session.nodeExists(componentConfiguration.getCanonicalStoredLocation()));
        assertEquals("deleted",
                session.getNode(componentConfiguration.getCanonicalStoredLocation()).getProperty(EDITABLE_PROPERTY_STATE).getString());
    }

    @Test
    public void test_delete_with_page_which_does_not_exist_live() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/homepage").remove();
        session.save();
        Thread.sleep(200);

        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");

        final HstComponentConfiguration componentConfiguration = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(home.getComponentConfigurationId());

        final SiteMapResource siteMapResource = createResource();
        final Response delete = siteMapResource.delete(home.getId());
        assertEquals(Response.Status.OK.getStatusCode(), delete.getStatus());
        assertTrue(((ResponseRepresentation) delete.getEntity()).getMessage().contains("deleted"));

        assertFalse(session.nodeExists(componentConfiguration.getCanonicalStoredLocation()));

    }

    @Test
    public void test_delete_with_page_that_is_referenced_twice() throws Exception {
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/home",
                "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/home2");

        session.save();
        Thread.sleep(200);
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");

        final HstComponentConfiguration componentConfiguration = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(home.getComponentConfigurationId());


        final SiteMapResource siteMapResource = createResource();
        final Response delete = siteMapResource.delete(home.getId());
        assertEquals(Response.Status.OK.getStatusCode(), delete.getStatus());
        assertTrue(((ResponseRepresentation) delete.getEntity()).getMessage().contains("deleted"));
        assertTrue(session.nodeExists(componentConfiguration.getCanonicalStoredLocation()));
        assertFalse(session.getNode(componentConfiguration.getCanonicalStoredLocation()).hasProperty(EDITABLE_PROPERTY_STATE));
    }

    @Test
    public void delete_item_with_descendant_items_should_also_remove_page_for_descendants() throws Exception {
        initContext();
        final String prototypeUUID = getPrototypePageUUID();
        final SiteMapItemRepresentation newFoo = createSiteMapItemRepresentation("foo", prototypeUUID);
        final SiteMapResource siteMapResource = createResource();
        final Response newFooResponse = siteMapResource.create(newFoo);

        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/foo"));
        String expectedNewPageFooNodeName = "foo-" + session.getNodeByIdentifier(prototypeUUID).getName();
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/"+expectedNewPageFooNodeName));

        // create 'bar' below 'foo'
        // first have to re-init the context to have the above added page to be part of the hst model
        initContext();
        SiteMapPageRepresentation siteMapPageFooRepresentation = (SiteMapPageRepresentation) ((ResponseRepresentation) newFooResponse.getEntity()).getData();
        final SiteMapItemRepresentation newBar = createSiteMapItemRepresentation("bar", prototypeUUID);
        siteMapResource.create(newBar, siteMapPageFooRepresentation.getId());
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/foo/bar"));
        String expectedNewPageBarNodeName = "foo-bar-" + session.getNodeByIdentifier(prototypeUUID).getName();
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/"+expectedNewPageBarNodeName));

        // now deleting 'foo' should also delete 'bar' PLUS its page because 'bar' is below 'foo'
        // first re-init the context
        initContext();
        siteMapResource.delete(siteMapPageFooRepresentation.getId());

        assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/foo"));
        assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/"+expectedNewPageFooNodeName));
        assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/foo/bar"));
        assertFalse("page for 'bar' should also had been deleted",
                session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/" + expectedNewPageBarNodeName));

    }

    @Test
    public void delete_item_with_descendant_items_fails_if_a_descendant_has_a_page_with_a_lock_by_other_user() throws Exception {
        initContext();
        final String prototypeUUID = getPrototypePageUUID();
        final SiteMapItemRepresentation newFoo = createSiteMapItemRepresentation("foo", prototypeUUID);
        final SiteMapResource siteMapResource = createResource();
        final Response newFooResponse = siteMapResource.create(newFoo);
        // create 'bar' below 'foo'
        // first have to re-init the context to have the above added page to be part of the hst model
        initContext();
        SiteMapPageRepresentation siteMapPageFooRepresentation = (SiteMapPageRepresentation) ((ResponseRepresentation) newFooResponse.getEntity()).getData();
        final SiteMapItemRepresentation newBar = createSiteMapItemRepresentation("bar", prototypeUUID);
        siteMapResource.create(newBar, siteMapPageFooRepresentation.getId());

        // publish changes for free locks
        initContext();
        mountResource.publish();
        // lock a container on the page belonging to 'bar'
        String expectedNewPageBarNodeName = "foo-bar-" + session.getNodeByIdentifier(prototypeUUID).getName();

        final Node container = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/" + expectedNewPageBarNodeName + "/main/container1");
        container.setProperty(GENERAL_PROPERTY_LOCKED_BY, "JohnDoe");
        session.save();
        Thread.sleep(200);

        // reload model and context
        initContext();
        // the deletion will result in a client error because one container on a page has been locked. Hence, the
        // delete call should not have removed the sitemap nodes (or have marked them as deleted)
        siteMapResource.delete(siteMapPageFooRepresentation.getId());

        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/foo"));
        assertFalse(session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/foo").hasProperty(EDITABLE_PROPERTY_STATE));
        String expectedNewPageFooNodeName = "foo-" + session.getNodeByIdentifier(prototypeUUID).getName();
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/" + expectedNewPageFooNodeName));
        assertFalse(session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/" + expectedNewPageFooNodeName).hasProperty(EDITABLE_PROPERTY_STATE));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/foo/bar"));
        assertFalse(session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/foo/bar").hasProperty(EDITABLE_PROPERTY_STATE));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/" + expectedNewPageBarNodeName));
        assertFalse(session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/" + expectedNewPageBarNodeName).hasProperty(EDITABLE_PROPERTY_STATE));
    }

    @Test
    public void delete_item_with_descendant_items_should_also_mark_page_for_descendants_deleted_when_descendants_are_live() throws Exception {
        initContext();
        final String prototypeUUID = getPrototypePageUUID();
        final SiteMapItemRepresentation newFoo = createSiteMapItemRepresentation("foo", prototypeUUID);
        final SiteMapResource siteMapResource = createResource();
        final Response newFooResponse = siteMapResource.create(newFoo);
        // create 'bar' below 'foo'
        // first have to re-init the context to have the above added page to be part of the hst model
        initContext();
        SiteMapPageRepresentation siteMapPageFooRepresentation = (SiteMapPageRepresentation) ((ResponseRepresentation) newFooResponse.getEntity()).getData();
        final SiteMapItemRepresentation newBar = createSiteMapItemRepresentation("bar", prototypeUUID);
        siteMapResource.create(newBar, siteMapPageFooRepresentation.getId());

        // publish changes for free locks
        initContext();
        mountResource.publish();
        // reload model and context
        initContext();
        // the deletion will result in items marked 'deleted' because live still has them. Only removed item should be the
        // child of sitemap item 'foo' since 'foo' is marked deleted already
        siteMapResource.delete(siteMapPageFooRepresentation.getId());
        final String absSiteMapFooPath = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/foo";
        String expectedNewPageFooNodeName = "foo-" + session.getNodeByIdentifier(prototypeUUID).getName();
        final String absPageFooPath = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/" + expectedNewPageFooNodeName;
        String expectedNewPageBarNodeName = "foo-bar-" + session.getNodeByIdentifier(prototypeUUID).getName();
        final String absPageBarPath = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/" + expectedNewPageBarNodeName;

        for (String absPath : new String[]{absSiteMapFooPath, absPageFooPath, absPageBarPath}) {
            assertTrue(session.nodeExists(absPath));
            assertTrue(session.getNode(absPath).hasProperty(EDITABLE_PROPERTY_STATE));
            assertEquals("deleted", session.getNode(absPath).getProperty(EDITABLE_PROPERTY_STATE).getString());
        }

        final String absSiteMapFooBarPath = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/foo/bar";
        assertFalse("child of deleted marked item should have been deleted", session.nodeExists(absSiteMapFooBarPath));

    }

    @Test
    public void discard_delete_with_descendant_items_should_also_discard_page_for_descendants_deleted_when_descendants_are_live() throws Exception {
        initContext();
        final String prototypeUUID = getPrototypePageUUID();
        final SiteMapItemRepresentation newFoo = createSiteMapItemRepresentation("foo", prototypeUUID);
        final SiteMapResource siteMapResource = createResource();
        final Response newFooResponse = siteMapResource.create(newFoo);
        // create 'bar' below 'foo'
        // first have to re-init the context to have the above added page to be part of the hst model
        initContext();
        SiteMapPageRepresentation siteMapPageFooRepresentation = (SiteMapPageRepresentation) ((ResponseRepresentation) newFooResponse.getEntity()).getData();
        final SiteMapItemRepresentation newBar = createSiteMapItemRepresentation("bar", prototypeUUID);
        siteMapResource.create(newBar, siteMapPageFooRepresentation.getId());

        // publish changes for free locks
        initContext();
        mountResource.publish();
        // reload model and context
        initContext();
        // the deletion will result in items marked 'deleted' because live still has them. Only removed item should be the
        // child of sitemap item 'foo' since 'foo' is marked deleted already
        siteMapResource.delete(siteMapPageFooRepresentation.getId());
        initContext();
        mountResource.discardChanges();
        // all nodes should be copied-back from live
        final String absSiteMapFooPath = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/foo";
        final String absSiteMapFooBarPath = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/foo/bar";
        String expectedNewPageFooNodeName = "foo-" + session.getNodeByIdentifier(prototypeUUID).getName();
        final String absPageFooPath = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/" + expectedNewPageFooNodeName;
        String expectedNewPageBarNodeName = "foo-bar-" + session.getNodeByIdentifier(prototypeUUID).getName();
        final String absPageBarPath = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/" + expectedNewPageBarNodeName;

        for (String absPath : new String[]{absSiteMapFooPath, absSiteMapFooBarPath, absPageFooPath, absPageBarPath}) {
            assertTrue(session.nodeExists(absPath));
            assertFalse(session.getNode(absPath).hasProperty(EDITABLE_PROPERTY_STATE));
        }
    }

    @Test
    public void delete_of_page_that_is_live_results_in_channel_having_changes() throws Exception {
        initContext();
        final String prototypeUUID = getPrototypePageUUID();
        final SiteMapItemRepresentation newFoo = createSiteMapItemRepresentation("foo", prototypeUUID);
        final SiteMapResource siteMapResource = createResource();
        final Response newFooResponse = siteMapResource.create(newFoo);

        SiteMapPageRepresentation siteMapPageFooRepresentation = (SiteMapPageRepresentation) ((ResponseRepresentation) newFooResponse.getEntity()).getData();
        initContext();
        mountResource.publish();
        // reload model and context
        initContext();
        siteMapResource.delete(siteMapPageFooRepresentation.getId());
        initContext();

        final String absSiteMapFooPath = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/foo";
        assertTrue(session.getNode(absSiteMapFooPath).hasProperty(EDITABLE_PROPERTY_STATE));
        assertEquals("deleted", session.getNode(absSiteMapFooPath).getProperty(EDITABLE_PROPERTY_STATE).getString());
        assertTrue(session.getNode(absSiteMapFooPath).hasProperty(GENERAL_PROPERTY_LOCKED_BY));
        assertEquals("admin", session.getNode(absSiteMapFooPath).getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());

        final Channel channel = RequestContextProvider.get().getResolvedMount().getMount().getChannel();
        assertEquals("There should be 'admin' in the changed by set.", 1, channel.getChangedBySet().size());
    }

    @Test
    public void test_delete_non_existing() throws Exception {
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response delete = siteMapResource.delete(UUID.randomUUID().toString());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), delete.getStatus());
    }

    @Test
    public void test_delete_invalid_uuid() throws Exception {
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response delete = siteMapResource.delete("invalid-uuid");
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), delete.getStatus());
    }

    @Test
    public void test_delete_non_workspace_item_fails() throws Exception {
        final SiteMapItemRepresentation nonWorkspaceItem = getSiteMapItemRepresentation(session, "about-us");
        final SiteMapResource siteMapResource = createResource();
        final Response delete = siteMapResource.delete(nonWorkspaceItem.getId());
        final ResponseRepresentation representation = (ResponseRepresentation) delete.getEntity();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), delete.getStatus());
        assertThat(representation.getErrorCode(), is(ClientError.ITEM_NOT_CORRECT_LOCATION.name()));
    }

    @Test
    public void test_deleted_item_not_part_of_model() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        final SiteMapResource siteMapResource = createResource();
        final Response delete = siteMapResource.delete(home.getId());
        assertEquals(Response.Status.OK.getStatusCode(), delete.getStatus());
        assertTrue(((ResponseRepresentation) delete.getEntity()).getMessage().contains("deleted"));

        // a refetch for deleted item should not return return the home sitemap item any more since it is marked for deletion
        final SiteMapItemRepresentation homeAgain = getSiteMapItemRepresentation(session, "home");
        assertEquals("_default_", homeAgain.getPathInfo());
        // assert the 'home' page sitemap item is nonetheless still part of the hst in memory model:
        assertNotNull(RequestContextProvider.get().getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSiteMapItem("home"));
    }

    @Test
    public void test_deleted_item_jcr_node_still_present_and_locked() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        final SiteMapResource siteMapResource = createResource();
        final Response delete = siteMapResource.delete(home.getId());
        assertEquals(Response.Status.OK.getStatusCode(), delete.getStatus());
        assertTrue(((ResponseRepresentation) delete.getEntity()).getMessage().contains("deleted"));

        try {
            final Node deletedNode = session.getNodeByIdentifier(home.getId());
            assertEquals("deleted", deletedNode.getProperty(EDITABLE_PROPERTY_STATE).getString());

            final Session bob = createSession("bob", "bob");
            Node deleteHomeNodeByBob = bob.getNodeByIdentifier(home.getId());
            try {
                final long versionStamp = 0;
                helper.acquireLock(deleteHomeNodeByBob, versionStamp);
                fail("Bob should 'see' locked deleted home node");
            } catch (ClientException e) {

            }
            bob.logout();

        } catch (ClientException e) {
            fail("Node should still exist but marked as deleted");
        }
    }

    @Test
    public void deleting_marked_deleted_item_passes_without_change() throws Exception {

        final SiteMapResource siteMapResource = createResource();
        String homeId;
        {
            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
            homeId = home.getId();
            final Response delete = siteMapResource.delete(homeId);
            assertEquals(Response.Status.OK.getStatusCode(), delete.getStatus());
        }
        {
            // force reload of hst model
            getSiteMapItemRepresentation(session, "home");
            final Response deleteAgain = siteMapResource.delete(homeId);
            final ResponseRepresentation representation = (ResponseRepresentation) deleteAgain.getEntity();
            assertEquals(Response.Status.OK.getStatusCode(), deleteAgain.getStatus());
        }
    }
}
