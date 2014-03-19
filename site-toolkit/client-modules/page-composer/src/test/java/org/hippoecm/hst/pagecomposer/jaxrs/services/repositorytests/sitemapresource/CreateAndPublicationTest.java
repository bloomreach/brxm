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


import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.LockHelper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * tests to primarily confirm correct page creation / publication after adding a new sitemap item
 */
public class CreateAndPublicationTest extends AbstractSiteMapResourceTest {


    private final LockHelper helper = new LockHelper();

    private void initContext() throws Exception {
        // call below will init request context
        getSiteMapItemRepresentation(session, "home");
    }

    @Test
    public void test_create_and_publish() throws Exception {
        initContext();
        final SiteMapItemRepresentation newFoo = createSiteMapItemRepresentation("foo", getPrototypePageUUID());
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.create(newFoo);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final Response publish = mountResource.publish();
        assertEquals(Response.Status.OK.getStatusCode(), publish.getStatus());

        String newPageNodeName = "foo-" + session.getNodeByIdentifier(getPrototypePageUUID()).getName();

        assertTrue(session.nodeExists(getLiveConfigurationWorkspacePagesPath() + "/" + newPageNodeName));
        assertTrue(session.nodeExists(getPreviewConfigurationWorkspacePagesPath() + "/" + newPageNodeName));

        Node livePageNode = session.getNode(getLiveConfigurationWorkspacePagesPath() + "/" + newPageNodeName);
        Node previewPageNode = session.getNode(getLiveConfigurationWorkspacePagesPath() + "/" + newPageNodeName);

        // assert pages not locked for bob
        final Session bob = createSession("bob", "bob");

        Node[] newPageNodes = {livePageNode, previewPageNode};
        for (Node newPageNode : newPageNodes) {
            Node newPageNodeByBob = bob.getNodeByIdentifier(newPageNode.getIdentifier());
            //  acquiring should fail now
            try {
                final long versionStamp = 0;
                helper.acquireLock(newPageNodeByBob, versionStamp);
            } catch (ClientException e) {
                fail("Should not be locked");
            }
        }
        bob.logout();
    }

    @Test
    public void test_create_and_publish_succeeds_when_workspace_missing() throws Exception {
        session.removeItem("/hst:hst/hst:configurations/unittestproject/hst:workspace");
        session.removeItem("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace");
        session.save();
        initContext();
        final SiteMapItemRepresentation newFoo = createSiteMapItemRepresentation("foo", getPrototypePageUUID());
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.create(newFoo);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final Response publish = mountResource.publish();
        assertEquals(Response.Status.OK.getStatusCode(), publish.getStatus());
    }

    @Test
    public void test_create_and_publish_succeeds_when_workspace_pages_missing() throws Exception {
        session.removeItem("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages");
        session.removeItem("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages");
        session.save();
        initContext();
        final SiteMapItemRepresentation newFoo = createSiteMapItemRepresentation("foo", getPrototypePageUUID());
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.create(newFoo);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final Response publish = mountResource.publish();
        assertEquals(Response.Status.OK.getStatusCode(), publish.getStatus());
    }

    @Test
    public void test_create_and_publish_succeeds_when_workspace_sitemap_missing() throws Exception {
        session.removeItem("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap");
        session.removeItem("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap");
        session.save();
        initContext();
        final SiteMapItemRepresentation newFoo = createSiteMapItemRepresentation("foo", getPrototypePageUUID());
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.create(newFoo);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final Response publish = mountResource.publish();
        assertEquals(Response.Status.OK.getStatusCode(), publish.getStatus());
    }


}
