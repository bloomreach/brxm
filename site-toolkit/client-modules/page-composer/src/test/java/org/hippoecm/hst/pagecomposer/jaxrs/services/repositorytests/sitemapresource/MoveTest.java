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

import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError.INVALID_MOVE_TO_SELF_OR_DESCENDANT;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MoveTest extends AbstractSiteMapResourceTest {

    @Test
    public void test_move() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        String homePathBeforeMove = session.getNodeByIdentifier(home.getId()).getPath();
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
        SiteMapResource siteMapResource = createResource();
        final Response move = siteMapResource.move(home.getId(), news.getId());
        Assert.assertEquals(Response.Status.OK.getStatusCode(), move.getStatus());
        assertEquals("'home' sitemap item is moved hence _default_ should match instead",
                "_default_", getSiteMapItemRepresentation(session, "home").getPathInfo());
        final SiteMapItemRepresentation newsHome = getSiteMapItemRepresentation(session, "news/home");
        assertNotNull(newsHome);

        // assert at old 'home' node location there is a deleted marker node
        final Node markedDeletedHome = session.getNode(homePathBeforeMove);
        assertEquals("deleted", markedDeletedHome.getProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE).getString());
        assertEquals("admin", markedDeletedHome.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
    }

    @Test
    public void test_cannot_move_deleted() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        String homePathBeforeMove = session.getNodeByIdentifier(home.getId()).getPath();
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
        SiteMapResource siteMapResource = createResource();
        siteMapResource.move(home.getId(), news.getId());

        final Node markedDeletedHome = session.getNode(homePathBeforeMove);
        final Response moveDeletedFail = siteMapResource.move(markedDeletedHome.getIdentifier(), news.getId());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), moveDeletedFail.getStatus());
        assertThat(((ResponseRepresentation) moveDeletedFail.getEntity()).getErrorCode(), is(ClientError.ITEM_NOT_IN_PREVIEW.name()));
    }

    @Test
    public void test_move_and_back_again() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        String homePathBeforeMove = session.getNodeByIdentifier(home.getId()).getPath();
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
        SiteMapResource siteMapResource = createResource();
        final Response move = siteMapResource.move(home.getId(), news.getId());
        Assert.assertEquals(Response.Status.OK.getStatusCode(), move.getStatus());

        // because of Jackrabbit issue we need a fresh session for tests with this move because
        // JR sometimes throws a incorrect repository exception during SessionMoveOperation
        {
            final Session admin = createSession("admin", "admin");
            final SiteMapItemRepresentation newsHome = getSiteMapItemRepresentation(admin, "news/home");
            String newsHomePath = admin.getNodeByIdentifier(newsHome.getId()).getPath();
            final Response moveBack = siteMapResource.move(newsHome.getId(), null);
            Assert.assertEquals(Response.Status.OK.getStatusCode(), moveBack.getStatus());
            final SiteMapItemRepresentation movedBackHome = getSiteMapItemRepresentation(admin, "home");
            final Node movedBackNode = admin.getNodeByIdentifier(movedBackHome.getId());
            assertEquals(homePathBeforeMove, movedBackNode.getPath());

            // the news/home node should be gone since not present in live
            assertFalse(admin.nodeExists(newsHomePath));
            admin.logout();
        }

    }

    @Test
    public void test_node_to_descendant_fails() throws Exception {
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
        final SiteMapItemRepresentation newsAny = getSiteMapItemRepresentation(session, "news/_any_");
        SiteMapResource siteMapResource = createResource();
        final Response fail = siteMapResource.move(news.getId(), newsAny.getId());
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), fail.getStatus());
        assertTrue(((ResponseRepresentation) fail.getEntity()).getMessage().contains("move operation"));
    }

    @Test
    public void test_node_move_to_self_fails() throws Exception {
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
        SiteMapResource siteMapResource = createResource();
        final Response fail = siteMapResource.move(news.getId(), news.getId());
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), fail.getStatus());
        assertThat(((ResponseRepresentation) fail.getEntity()).getErrorCode(), is(INVALID_MOVE_TO_SELF_OR_DESCENDANT.name()));
    }


    @Test
    public void test_node_move_to_same_parent_changes_nothing() throws Exception {
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
        final SiteMapItemRepresentation newsAny = getSiteMapItemRepresentation(session, "news/_any_");
        SiteMapResource siteMapResource = createResource();
        final Response nothing = siteMapResource.move(newsAny.getId(), news.getId());
        Assert.assertEquals(Response.Status.OK.getStatusCode(), nothing.getStatus());
        final Node newsAnyNode = session.getNodeByIdentifier(newsAny.getId());
        assertFalse(newsAnyNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertFalse(newsAnyNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));
    }


    @Test
    public void test_node_move_from_workspace_to_non_workspace_fails() throws Exception {
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
        // about-us is non workspace
        final SiteMapItemRepresentation aboutUs = getSiteMapItemRepresentation(session, "about-us");
        SiteMapResource siteMapResource = createResource();
        final Response fail = siteMapResource.move(news.getId(), aboutUs.getId());
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), fail.getStatus());
        assertThat(((ResponseRepresentation) fail.getEntity()).getErrorCode(), is(ClientError.ITEM_NOT_CORRECT_LOCATION.name()));
    }

    @Test
    public void test_node_move_from_non_workspace_to_workspace_fails() throws Exception {
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
        // about-us is non workspace
        final SiteMapItemRepresentation aboutUs = getSiteMapItemRepresentation(session, "about-us");
        SiteMapResource siteMapResource = createResource();
        final Response fail = siteMapResource.move(aboutUs.getId(), news.getId());
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), fail.getStatus());
        assertThat(((ResponseRepresentation) fail.getEntity()).getErrorCode(), is(ClientError.ITEM_NOT_CORRECT_LOCATION.name()));
    }
}
