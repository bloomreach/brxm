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
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapPageRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapPagesRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.util.HstSiteMapUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PagesTest extends AbstractSiteMapResourceTest{

    private void initContext() throws Exception {
        // call below will init request context
        getSiteMapItemRepresentation(session, "home");
    }

    @Test
    public void test_pages_sorted_by_pathInfo() throws Exception {
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.getSiteMapPages();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SiteMapPagesRepresentation siteMapPagesRepresentation =
                (SiteMapPagesRepresentation) ((ExtResponseRepresentation) response.getEntity()).getData();

        SiteMapPageRepresentation prev = null;
        for (SiteMapPageRepresentation siteMapPageRepresentation : siteMapPagesRepresentation.getPages()) {
            if (prev == null) {
                prev = siteMapPageRepresentation;
                continue;
            }
            assertTrue(siteMapPageRepresentation.getPathInfo().compareTo(prev.getPathInfo()) >= 0);
            prev = siteMapPageRepresentation;
        }
    }

    @Test
    public void test_homepage_is_first_and_pathInfo_equals_slash() throws Exception {
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.getSiteMapPages();
        SiteMapPagesRepresentation siteMapPagesRepresentation =
                (SiteMapPagesRepresentation) ((ExtResponseRepresentation) response.getEntity()).getData();

        assertEquals("/", siteMapPagesRepresentation.getPages().get(0).getPathInfo());
        assertEquals("home", siteMapPagesRepresentation.getPages().get(0).getName());
    }


    @Test
    public void test_sitemap_item_page_title() throws Exception {
        final Node home = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/home");
        home.setProperty(HstNodeTypes.SITEMAPITEM_PAGE_TITLE, "foo");
        session.save();
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.getSiteMapPages();
        SiteMapPagesRepresentation siteMapPagesRepresentation =
                (SiteMapPagesRepresentation) ((ExtResponseRepresentation) response.getEntity()).getData();

        assertEquals("/", siteMapPagesRepresentation.getPages().get(0).getPathInfo());
        assertEquals("home", siteMapPagesRepresentation.getPages().get(0).getName());
        assertEquals("foo", siteMapPagesRepresentation.getPages().get(0).getPageTitle());
    }

}
