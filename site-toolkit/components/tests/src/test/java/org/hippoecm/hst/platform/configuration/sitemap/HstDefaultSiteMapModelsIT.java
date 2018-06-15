/*
 *  Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.sitemap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HstDefaultSiteMapModelsIT extends AbstractTestConfigurations {

    private HstManager hstManager;
    private Session session;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        session = createSession();
        createHstConfigBackup(session);
        hstManager = getComponent(HstManager.class.getName());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        restoreHstConfigBackup(session);
        session.logout();
        super.tearDown();
    }

    @Test
    public void default_sitemap_item_container_resource_for_root_mount() throws Exception {
        createContainerResourceSiteMapItem("_any_.xyz", "hst:hst/hst:configurations/hst:default/hst:sitemap");
        session.save();
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/foo/bar.xyz");
        final ResolvedSiteMapItem resolvedSiteMapItem = mount.matchSiteMapItem("/foo/bar.xyz");
        assertTrue(resolvedSiteMapItem.getHstSiteMapItem().isContainerResource());
    }

    @Test
    public void default_sitemap_item_container_resource_for_sub_mount() throws Exception {
        createContainerResourceSiteMapItem("_any_.xyz", "hst:hst/hst:configurations/hst:default/hst:sitemap");
        session.save();
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/intranet/foo/bar.xyz");
        final ResolvedSiteMapItem resolvedSiteMapItem = mount.matchSiteMapItem("/foo/bar.xyz");
        assertTrue(resolvedSiteMapItem.getHstSiteMapItem().isContainerResource());
    }

    @Test
    public void default_sitemap_item_container_resource_implicit_properties() throws Exception {
        createContainerResourceSiteMapItem("_any_.xyz", "hst:hst/hst:configurations/hst:default/hst:sitemap");
        session.save();
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/foo/bar.xyz");
        final ResolvedSiteMapItem resolvedSiteMapItem = mount.matchSiteMapItem("/foo/bar.xyz");
        assertEquals("ContainerResourcePipeline", resolvedSiteMapItem.getNamedPipeline());
        assertTrue(resolvedSiteMapItem.getHstSiteMapItem().isSchemeAgnostic());
        assertTrue(resolvedSiteMapItem.getHstSiteMapItem().isExcludedForLinkRewriting());
    }

    @Test
    public void default_sitemap_item_container_resource_override_implicit_properties() throws Exception {
        final Node containerResourceSiteMapItem = createContainerResourceSiteMapItem("_any_.xyz", "hst:hst/hst:configurations/hst:default/hst:sitemap");
        containerResourceSiteMapItem.setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_NAMEDPIPELINE, "myPipeline");
        containerResourceSiteMapItem.setProperty(HstNodeTypes.GENERAL_PROEPRTY_SCHEME_AGNOSTIC, false);
        containerResourceSiteMapItem.setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_EXCLUDEDFORLINKREWRITING, false);
        session.save();
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/foo/bar.xyz");
        final ResolvedSiteMapItem resolvedSiteMapItem = mount.matchSiteMapItem("/foo/bar.xyz");
        assertEquals("myPipeline", resolvedSiteMapItem.getNamedPipeline());
        assertFalse(resolvedSiteMapItem.getHstSiteMapItem().isSchemeAgnostic());
        assertFalse(resolvedSiteMapItem.getHstSiteMapItem().isExcludedForLinkRewriting());
    }

    @Test
    public void sitemap_item_container_resource_not_allowed_outside_hst_default() throws Exception {
        createContainerResourceSiteMapItem("_any_.xyz", "hst:hst/hst:configurations/unittestproject/hst:sitemap");
        session.save();
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/foo/bar.xyz");
        final ResolvedSiteMapItem resolvedSiteMapItem = mount.matchSiteMapItem("/foo/bar.xyz");
        assertEquals("pagenotfound",resolvedSiteMapItem.getPathInfo());

        // assert rest of sitemap still works
        final ResolvedSiteMapItem cssItem = mount.matchSiteMapItem("/css/style.css");
        assertTrue(cssItem.getHstSiteMapItem().isContainerResource());
        assertEquals("css/style.css",cssItem.getPathInfo());
        final ResolvedSiteMapItem homePage = mount.matchSiteMapItem("/home");

        assertFalse(homePage.getHstSiteMapItem().isContainerResource());
        assertEquals("home",homePage.getPathInfo());
    }

    private Node createContainerResourceSiteMapItem(final String siteMapItemName, final String parent) throws RepositoryException {
        final Node parentNode = session.getRootNode().getNode(parent);
        final Node child = parentNode.addNode(siteMapItemName, HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
        child.setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_CONTAINER_RESOURCE, true);
        return child;
    }


}
