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

import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.LockHelper;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DeleteTest extends AbstractSiteMapResourceTest {

    private LockHelper helper = new LockHelper();

    private void initContext() throws Exception {
        // call below will init request context
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
    }

    @Test
    public void test_delete_with_page() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");

        final HstComponentConfiguration componentConfiguration = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(home.getComponentConfigurationId());

        final SiteMapResource siteMapResource = createResource();
        final Response delete = siteMapResource.delete(home.getId());
        assertEquals(Response.Status.OK.getStatusCode(), delete.getStatus());
        assertTrue(((ExtResponseRepresentation) delete.getEntity()).getMessage().contains("deleted"));

        assertTrue(session.nodeExists(componentConfiguration.getCanonicalStoredLocation()));
        assertEquals("deleted",
                session.getNode(componentConfiguration.getCanonicalStoredLocation()).getProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE).getString());
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
        assertTrue(((ExtResponseRepresentation) delete.getEntity()).getMessage().contains("deleted"));

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
        assertTrue(((ExtResponseRepresentation) delete.getEntity()).getMessage().contains("deleted"));
        assertTrue(session.nodeExists(componentConfiguration.getCanonicalStoredLocation()));
        assertFalse(session.getNode(componentConfiguration.getCanonicalStoredLocation()).hasProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE));

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
        final ExtResponseRepresentation representation = (ExtResponseRepresentation) delete.getEntity();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), delete.getStatus());
        assertThat(representation.getMessage(), is(ClientError.ITEM_NOT_CORRECT_LOCATION.name()));
    }

    @Test
    public void test_deleted_item_not_part_of_model() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        final SiteMapResource siteMapResource = createResource();
        final Response delete = siteMapResource.delete(home.getId());
        assertEquals(Response.Status.OK.getStatusCode(), delete.getStatus());
        assertTrue(((ExtResponseRepresentation) delete.getEntity()).getMessage().contains("deleted"));

        // a refetch for deleted item should return null as not present in hst model any more
        assertNull(getSiteMapItemRepresentation(session, "home"));

    }

    @Test
    public void test_deleted_item_jcr_node_still_present_and_locked() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        final SiteMapResource siteMapResource = createResource();
        final Response delete = siteMapResource.delete(home.getId());
        assertEquals(Response.Status.OK.getStatusCode(), delete.getStatus());
        assertTrue(((ExtResponseRepresentation) delete.getEntity()).getMessage().contains("deleted"));

        try {
            final Node deletedNode = session.getNodeByIdentifier(home.getId());
            assertEquals("deleted", deletedNode.getProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE).getString());

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
    public void test_fail_to_delete_deleted_item() throws Exception {

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
            final ExtResponseRepresentation representation = (ExtResponseRepresentation) deleteAgain.getEntity();
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), deleteAgain.getStatus());
            assertThat(representation.getMessage(), is(ClientError.ITEM_NOT_IN_PREVIEW.name()));
        }
    }
}
