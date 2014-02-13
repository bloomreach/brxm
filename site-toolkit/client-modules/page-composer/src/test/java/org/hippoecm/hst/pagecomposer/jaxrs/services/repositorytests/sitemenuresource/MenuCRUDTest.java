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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.LinkType;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMenuResource;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MenuCRUDTest extends AbstractMenuResourceTest{

    @Test
    public void test_update() throws Exception {

        final SiteMenuResource resource = createResource();
        final SiteMenuItemRepresentation newsItem = getSiteMenuItemRepresentation(session, "main", "News");
        assertNotNull(newsItem);
        newsItem.setLinkType(LinkType.SITEMAPITEM);
        newsItem.setLink("test");
        resource.update(newsItem);
        final Node newsNode = session.getNodeByIdentifier(newsItem.getId());
        assertEquals("test", newsNode.getProperty(HstNodeTypes.SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM).getString());

    }

    @Test
    public void test_rename() throws Exception {
        final SiteMenuResource resource = createResource();
        final SiteMenuItemRepresentation newsItem = getSiteMenuItemRepresentation(session, "main", "News");
        newsItem.setName("NewsRenamed");
        resource.update(newsItem);
        final Node newsNode = session.getNodeByIdentifier(newsItem.getId());
        assertEquals("NewsRenamed", newsNode.getName());
    }

    @Test
    public void test_delete() throws Exception {
        final SiteMenuResource resource = createResource();
        final SiteMenuItemRepresentation newsItem = getSiteMenuItemRepresentation(session, "main", "News");
        String oldPath = session.getNodeByIdentifier(newsItem.getId()).getPath();
        resource.delete(newsItem.getId());
        assertFalse(session.nodeExists(oldPath));
    }

    @Test
    public void test_move() throws Exception {
        final SiteMenuResource resource = createResource();
        final SiteMenuItemRepresentation newsItem = getSiteMenuItemRepresentation(session, "main", "News");
        final SiteMenuItemRepresentation contactItem = getSiteMenuItemRepresentation(session, "main", "Contact");
        String oldPath = session.getNodeByIdentifier(newsItem.getId()).getPath();
        resource.move(newsItem.getId(), contactItem.getId(), 0);
        assertFalse(session.nodeExists(oldPath));
        String newPath = session.getNodeByIdentifier(contactItem.getId()).getPath() + "/News";
        assertTrue(session.nodeExists(newPath));
    }
}
