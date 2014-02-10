/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.sitemap;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestSiteMapModels extends AbstractTestConfigurations {

    private HstManager hstManager;
    private HippoSession session;

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


    protected HippoSession createSession() throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return (HippoSession)repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

    @Test
    public void test_plain_sitemap_without_workspace_sitemap() throws Exception {
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstSiteMap siteMap = hstSite.getSiteMap();
        assertTrue(siteMap instanceof CanonicalInfo);

        CanonicalInfo siteMapCanonicalInfo = (CanonicalInfo)siteMap;
        assertFalse(siteMapCanonicalInfo.isWorkspaceConfiguration());
        final Node siteMapNode = session.getNodeByIdentifier(siteMapCanonicalInfo.getCanonicalIdentifier());
        assertTrue(siteMapNode.getPath().equals(hstSite.getConfigurationPath()+"/hst:sitemap"));

        for (HstSiteMapItem hstSiteMapItem : siteMap.getSiteMapItems()) {
            CanonicalInfo siteMapItemCanonicalInfo = (CanonicalInfo)hstSiteMapItem;
            assertFalse(siteMapItemCanonicalInfo.isWorkspaceConfiguration());
            final Node siteMapItemNode = session.getNodeByIdentifier(siteMapItemCanonicalInfo.getCanonicalIdentifier());
            assertTrue(siteMapItemNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:sitemap"));
        }

    }

    @Test
    public void test_sitemap_completely_in_workspace() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace");
        session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemap",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap");
        session.save();
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstSiteMap siteMap = hstSite.getSiteMap();

        CanonicalInfo siteMapCanonicalInfo = (CanonicalInfo)siteMap;
        assertTrue(siteMapCanonicalInfo.isWorkspaceConfiguration());
        final Node siteMapNode = session.getNodeByIdentifier(siteMapCanonicalInfo.getCanonicalIdentifier());
        assertTrue(siteMapNode.getPath().equals(hstSite.getConfigurationPath()+"/hst:workspace/hst:sitemap"));

        for (HstSiteMapItem hstSiteMapItem : siteMap.getSiteMapItems()) {
            CanonicalInfo siteMapItemCanonicalInfo = (CanonicalInfo)hstSiteMapItem;
            assertTrue(siteMapItemCanonicalInfo.isWorkspaceConfiguration());
            final Node siteMapItemNode = session.getNodeByIdentifier(siteMapItemCanonicalInfo.getCanonicalIdentifier());
            assertTrue(siteMapItemNode.getPath().startsWith(hstSite.getConfigurationPath()+"/hst:workspace/hst:sitemap"));
        }
    }

    @Test
    public void test_sitemap_partially_in_workspace() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace").addNode("hst:sitemap");
        session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemap/home",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/home");
        session.save();

        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstSiteMap siteMap = hstSite.getSiteMap();

        CanonicalInfo siteMapCanonicalInfo = (CanonicalInfo)siteMap;
        // since not the entire sitemap is in workspace, below gives false
        assertFalse(siteMapCanonicalInfo.isWorkspaceConfiguration());

        final Node siteMapNode = session.getNodeByIdentifier(siteMapCanonicalInfo.getCanonicalIdentifier());
        assertTrue(siteMapNode.getPath().equals(hstSite.getConfigurationPath()+"/hst:sitemap"));

        HstSiteMapItem home = siteMap.getSiteMapItem("home");
        CanonicalInfo homeCanonicalInfo = (CanonicalInfo)home;
        assertTrue(homeCanonicalInfo.isWorkspaceConfiguration());
        final Node homeNode = session.getNodeByIdentifier(homeCanonicalInfo.getCanonicalIdentifier());
        assertTrue(homeNode.getPath().startsWith(hstSite.getConfigurationPath()+"/hst:workspace/hst:sitemap"));

        for (HstSiteMapItem hstSiteMapItem : siteMap.getSiteMapItems()) {
            CanonicalInfo siteMapItemCanonicalInfo = (CanonicalInfo)hstSiteMapItem;
            if (hstSiteMapItem.getValue().equals("home")) {
                assertTrue(siteMapItemCanonicalInfo.isWorkspaceConfiguration());
                final Node siteMapItemNode = session.getNodeByIdentifier(siteMapItemCanonicalInfo.getCanonicalIdentifier());
                assertTrue(siteMapItemNode.getPath().startsWith(hstSite.getConfigurationPath()+"/hst:workspace/hst:sitemap"));
            } else {
                assertFalse(siteMapItemCanonicalInfo.isWorkspaceConfiguration());
                final Node siteMapItemNode = session.getNodeByIdentifier(siteMapItemCanonicalInfo.getCanonicalIdentifier());
                assertFalse(siteMapItemNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:workspace/hst:sitemap"));
            }
        }
    }

    @Test
    public void test_sitemap_duplicate_node_in_workspace_is_skipped() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace").addNode("hst:sitemap");
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:sitemap/home",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/home");

        session.save();
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstSiteMap siteMap = hstSite.getSiteMap();

        CanonicalInfo siteMapCanonicalInfo = (CanonicalInfo)siteMap;
        // since not the entire sitemap is in workspace, below gives false
        assertFalse(siteMapCanonicalInfo.isWorkspaceConfiguration());

        final Node siteMapNode = session.getNodeByIdentifier(siteMapCanonicalInfo.getCanonicalIdentifier());
        assertTrue(siteMapNode.getPath().equals(hstSite.getConfigurationPath()+"/hst:sitemap"));

        HstSiteMapItem home = siteMap.getSiteMapItem("home");
        CanonicalInfo homeCanonicalInfo = (CanonicalInfo)home;
        // since 'home' item is duplicate, it will be taken from 'unittestproject/hst:sitemap' and not from
        // 'unittestproject/hst:workspace/hst:sitemap'
        assertFalse(homeCanonicalInfo.isWorkspaceConfiguration());
        final Node homeNode = session.getNodeByIdentifier(homeCanonicalInfo.getCanonicalIdentifier());
        assertFalse(homeNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:workspace/hst:sitemap"));
    }

    @Test
    public void test_sitemap_node_in_workspace_and_inherited_config() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace").addNode("hst:sitemap");
        session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemap/home",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/home");
        session.getNode("/hst:hst/hst:configurations/unittestcommon").addNode("hst:sitemap");

        session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemap/news",
                "/hst:hst/hst:configurations/unittestcommon/hst:sitemap/news");
        session.save();

        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstSiteMap siteMap = hstSite.getSiteMap();

        {
            HstSiteMapItem home = siteMap.getSiteMapItem("home");
            CanonicalInfo homeCanonicalInfo = (CanonicalInfo)home;
            assertTrue(homeCanonicalInfo.isWorkspaceConfiguration());
            final Node homeNode = session.getNodeByIdentifier(homeCanonicalInfo.getCanonicalIdentifier());
            assertTrue(homeNode.getPath().startsWith(hstSite.getConfigurationPath()+"/hst:workspace/hst:sitemap"));
        }

        {
            HstSiteMapItem news = siteMap.getSiteMapItem("news");
            CanonicalInfo newsCanonicalInfo = (CanonicalInfo)news;
            assertFalse(newsCanonicalInfo.isWorkspaceConfiguration());
            final Node newsNode = session.getNodeByIdentifier(newsCanonicalInfo.getCanonicalIdentifier());
            assertFalse(newsNode.getPath().startsWith(hstSite.getConfigurationPath()));
            assertEquals("/hst:hst/hst:configurations/unittestcommon/hst:sitemap/news", newsNode.getPath());
        }
    }

    @Test
    public void test_sitemap_node_in_workspace_same_as_inherited_takes_workspace() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace").addNode("hst:sitemap");
        session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemap/home",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/home");
        session.getNode("/hst:hst/hst:configurations/unittestcommon").addNode("hst:sitemap");

        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/home",
                "/hst:hst/hst:configurations/unittestcommon/hst:sitemap/home");
        session.save();

        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstSiteMap siteMap = hstSite.getSiteMap();

        HstSiteMapItem home = siteMap.getSiteMapItem("home");
        CanonicalInfo homeCanonicalInfo = (CanonicalInfo)home;
        assertTrue(homeCanonicalInfo.isWorkspaceConfiguration());
        final Node homeNode = session.getNodeByIdentifier(homeCanonicalInfo.getCanonicalIdentifier());
        assertTrue(homeNode.getPath().startsWith(hstSite.getConfigurationPath()+"/hst:workspace/hst:sitemap"));
    }

    @Test
    public void test_workspace_in_inherited_sitemap_is_ignored() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestcommon").addNode("hst:workspace").addNode("hst:sitemap");
        session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemap/home",
                "/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemap/home");
        session.save();
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstSiteMap siteMap = hstSite.getSiteMap();
        assertNull(siteMap.getSiteMapItem("home"));
    }

    @Test
    public void test_marked_deleted_nodes_are_ignored()  throws Exception {
        final Node home = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap/home");
        home.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        home.setProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE, "deleted");
        final Node newsDefault = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap/news/_default_");
        newsDefault.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        newsDefault.setProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE, "deleted");
        session.save();

        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstSiteMap siteMap = hstSite.getSiteMap();
        assertNull(siteMap.getSiteMapItem("home"));
        assertNull(siteMap.getSiteMapItem("news").getChild("_default_"));

    }
}
