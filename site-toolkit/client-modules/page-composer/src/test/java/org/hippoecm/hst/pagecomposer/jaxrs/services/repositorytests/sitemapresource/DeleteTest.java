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

import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeleteTest extends AbstractSiteMapResourceTest {



    private void initContext() throws Exception {
        // call below will init request context
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
    }

    @Test
    public void test_delete() throws Exception {
        final SiteMapResource siteMapResource = new SiteMapResource();
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");

        final Response delete = siteMapResource.delete(home.getId());
        assertEquals(Response.Status.OK.getStatusCode(), delete.getStatus());
        assertTrue(((ExtResponseRepresentation) delete.getEntity()).getMessage().contains("deleted"));
    }

    @Test
    public void test_delete_non_existing() throws Exception {
        initContext();
        final SiteMapResource siteMapResource = new SiteMapResource();
        final Response delete = siteMapResource.delete(UUID.randomUUID().toString());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), delete.getStatus());
    }

    @Test
    public void test_delete_invalid_uuid() throws Exception {
        initContext();
        final SiteMapResource siteMapResource = new SiteMapResource();
        final Response delete = siteMapResource.delete("invalid-uuid");
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), delete.getStatus());
    }

    @Test
    public void test_delete_non_workspace_item_fails() throws Exception {
        final SiteMapItemRepresentation nonWorkspaceItem = getSiteMapItemRepresentation(session, "about-us");
        final SiteMapResource siteMapResource = new SiteMapResource();
        final Response delete = siteMapResource.delete(nonWorkspaceItem.getId());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), delete.getStatus());
        System.out.println(((ExtResponseRepresentation) delete.getEntity()).getMessage());
    }



//    final Session bob = createSession("bob", "bob");
//    Node homeNodeByBob = bob.getNodeByIdentifier(home.getId());




}
