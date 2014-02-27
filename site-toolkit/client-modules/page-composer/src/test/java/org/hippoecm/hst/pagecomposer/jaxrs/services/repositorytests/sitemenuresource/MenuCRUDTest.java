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

package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.sitemenuresource;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.LinkType;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMenuResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MenuCRUDTest extends AbstractMenuResourceTest {

    @Test
    public void test_update() throws Exception {

        final SiteMenuResource resource = createResource();
        final SiteMenuItemRepresentation newsItem = getSiteMenuItemRepresentation(session, "main", "News");
        assertNotNull(newsItem);
        newsItem.setLinkType(LinkType.SITEMAPITEM);
        newsItem.setLink("test");
        final Response update = resource.update(newsItem);
        assertEquals(Response.Status.OK.getStatusCode(), update.getStatus());

        final Node newsNode = session.getNodeByIdentifier(newsItem.getId());
        assertEquals("test", newsNode.getProperty(HstNodeTypes.SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM).getString());

        final Node menuNode = newsNode.getParent();
        assertEquals(HstNodeTypes.NODETYPE_HST_SITEMENU, menuNode.getPrimaryNodeType().getName());
        // assert locked
        assertTrue(menuNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertEquals("admin", menuNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());

        assertBobCannotMakeModications(resource);
    }


    @Test
    public void test_rename() throws Exception {
        final SiteMenuResource resource = createResource();
        final SiteMenuItemRepresentation newsItem = getSiteMenuItemRepresentation(session, "main", "News");
        newsItem.setTitle("NewsRenamed");
        final Response update = resource.update(newsItem);
        assertEquals(Response.Status.OK.getStatusCode(), update.getStatus());
        final Node newsNode = session.getNodeByIdentifier(newsItem.getId());
        assertEquals("NewsRenamed", newsNode.getName());

        final Node menuNode = newsNode.getParent();
        assertEquals(HstNodeTypes.NODETYPE_HST_SITEMENU, menuNode.getPrimaryNodeType().getName());
        // assert locked
        assertTrue(menuNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertEquals("admin", menuNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        assertBobCannotMakeModications(resource);
    }

    @Test
    public void test_delete() throws Exception {
        final SiteMenuResource resource = createResource();
        final SiteMenuItemRepresentation newsItem = getSiteMenuItemRepresentation(session, "main", "News");
        final Node menuNode = session.getNodeByIdentifier(newsItem.getId()).getParent();
        String oldPath = session.getNodeByIdentifier(newsItem.getId()).getPath();

        final Response delete = resource.delete(newsItem.getId());
        assertEquals(Response.Status.OK.getStatusCode(), delete.getStatus());
        assertFalse(session.nodeExists(oldPath));

        assertEquals(HstNodeTypes.NODETYPE_HST_SITEMENU, menuNode.getPrimaryNodeType().getName());
        // assert locked
        assertTrue(menuNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertEquals("admin", menuNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        assertBobCannotMakeModications(resource);
    }

    @Test
    public void test_move() throws Exception {
        final SiteMenuResource resource = createResource();
        final SiteMenuItemRepresentation newsItem = getSiteMenuItemRepresentation(session, "main", "News");
        final SiteMenuItemRepresentation contactItem = getSiteMenuItemRepresentation(session, "main", "Contact");
        String oldPath = session.getNodeByIdentifier(newsItem.getId()).getPath();
        final Response move = resource.move(newsItem.getId(), contactItem.getId(), 0);
        assertEquals(Response.Status.OK.getStatusCode(), move.getStatus());
        assertFalse(session.nodeExists(oldPath));
        String newPath = session.getNodeByIdentifier(contactItem.getId()).getPath() + "/News";
        assertTrue(session.nodeExists(newPath));

        final Node menuNode = session.getNodeByIdentifier(contactItem.getId()).getParent();
        assertEquals(HstNodeTypes.NODETYPE_HST_SITEMENU, menuNode.getPrimaryNodeType().getName());
        // assert locked
        assertTrue(menuNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE));
        assertEquals("admin", menuNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        assertBobCannotMakeModications(resource);
    }

    private void assertBobCannotMakeModications(final SiteMenuResource resource) throws Exception {
        final Session bob = createSession("bob", "bob");
        final SiteMenuItemRepresentation contactItem = getSiteMenuItemRepresentation(bob, "main", "Contact");
        contactItem.setTitle("test");

        final Response fail = resource.update(contactItem);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), fail.getStatus());
        assertThat(((ExtResponseRepresentation) fail.getEntity()).getMessage(), is(ClientError.ITEM_ALREADY_LOCKED.name()));
        bob.logout();
    }
}
