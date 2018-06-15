/*
 *  Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.platform.linking;


import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class HstContainerResourceLinkRewritingIT extends AbstractHstLinkRewritingIT {

    @Test
    public void root_mount_container_resource_link_implicit_container_resource() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        assertEquals("", requestContext.getResolvedMount().getMount().getMountPath());
        HstLink cssLink = linkCreator.create("/css/style.css", requestContext.getResolvedMount().getMount());
        assertTrue(cssLink.isContainerResource());
        assertEquals("css/style.css" , cssLink.getPath());
        assertEquals("/site/css/style.css" , cssLink.toUrlForm(requestContext, false));
    }

    @Test
    public void root_mount_container_resource_link_explicit_true_container_resource() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        assertEquals("", requestContext.getResolvedMount().getMount().getMountPath());
        final boolean containerResource = true;
        HstLink cssLink = linkCreator.create("/css/style.css", requestContext.getResolvedMount().getMount(), containerResource);
        assertTrue(cssLink.isContainerResource());
        assertEquals("css/style.css" , cssLink.getPath());
        assertEquals("/site/css/style.css" , cssLink.toUrlForm(requestContext, false));
    }

    @Test
    public void root_mount_container_resource_link_explicit_false_container_resource() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        assertEquals("", requestContext.getResolvedMount().getMount().getMountPath());
        final boolean containerResource = false;
        HstLink cssLink = linkCreator.create("/css/style.css", requestContext.getResolvedMount().getMount(), containerResource);
        assertFalse(cssLink.isContainerResource());
        assertEquals("css/style.css" , cssLink.getPath());
        assertEquals("/site/css/style.css" , cssLink.toUrlForm(requestContext, false));
    }

    @Test
    public void root_mount_non_sitemapitem_resolvable_path_info_results_in_container_resource_link() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        // .xyz is does not map *any* sitemap item. As a result, it will be treated as a *container resource*
        HstLink xyzLink = linkCreator.create("/xyz/foo.xyz", requestContext.getResolvedMount().getMount());
        assertTrue(xyzLink.isContainerResource());
        assertEquals("xyz/foo.xyz", xyzLink.getPath());
        assertEquals("/site/xyz/foo.xyz", xyzLink.toUrlForm(requestContext, false));
    }

    @Test
    public void sub_mount_non_sitemapitem_resolvable_path_info_results_in_container_resource_link() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "subsite/home");
        // .xyz is does not map *any* sitemap item. As a result, it will be treated as a *container resource*
        HstLink xyzLink = linkCreator.create("/xyz/foo.xyz", requestContext.getResolvedMount().getMount());
        assertTrue(xyzLink.isContainerResource());
        assertEquals("xyz/foo.xyz", xyzLink.getPath());
        // a 'container resource' does *NOT* get *sub mount* path included
        assertEquals("/site/xyz/foo.xyz", xyzLink.toUrlForm(requestContext, false));
    }

    /**
     * this test is to assert that for submounts, a container resource URL does *not* include the mountpath
     */
    @Test
    public void sub_mount_container_resource_link_implicit_container_resource() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "subsite/home");
        assertEquals("/subsite", requestContext.getResolvedMount().getMount().getMountPath());
        HstLink cssLink = linkCreator.create("/css/style.css", requestContext.getResolvedMount().getMount());
        assertTrue(cssLink.isContainerResource());
        assertEquals("css/style.css" , cssLink.getPath());
        assertEquals("/site/css/style.css" , cssLink.toUrlForm(requestContext, false));
    }

    /**
     * this test is to assert that for submounts, a *non* container resource URL *does* include the mountpath
     */
    @Test
    public void sub_mount_container_resource_link_implicit_non_container_resource() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "subsite/home");
        assertEquals("/subsite", requestContext.getResolvedMount().getMount().getMountPath());
        HstLink documentLink = linkCreator.create("/news/2010/item.html", requestContext.getResolvedMount().getMount());
        assertFalse(documentLink.isContainerResource());
        assertEquals("news/2010/item.html", documentLink.getPath());
        assertEquals("/site/subsite/news/2010/item.html", documentLink.toUrlForm(requestContext, false));
    }

    @Test
    public void sub_mount_container_resource_link_explicit_true_container_resource() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "subsite/home");
        assertEquals("/subsite", requestContext.getResolvedMount().getMount().getMountPath());
        final boolean containerResource = true;
        HstLink cssLink = linkCreator.create("/css/style.css", requestContext.getResolvedMount().getMount(), containerResource);
        assertTrue(cssLink.isContainerResource());
        assertEquals("css/style.css" , cssLink.getPath());
        assertEquals("/site/css/style.css" , cssLink.toUrlForm(requestContext, false));
    }

    @Test
    public void sub_mount_container_resource_link_explicit_false_container_resource() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "subsite/home");
        assertEquals("/subsite", requestContext.getResolvedMount().getMount().getMountPath());
        final boolean containerResource = false;
        HstLink cssLink = linkCreator.create("/css/style.css", requestContext.getResolvedMount().getMount(), containerResource);
        assertFalse(cssLink.isContainerResource());
        assertEquals("css/style.css" , cssLink.getPath());
        // because HstLink is explicitly created with  'containerResource = false', the URL should include the
        // sub mount path '/subsite'
        assertEquals("/site/subsite/css/style.css" , cssLink.toUrlForm(requestContext, false));
    }
}
