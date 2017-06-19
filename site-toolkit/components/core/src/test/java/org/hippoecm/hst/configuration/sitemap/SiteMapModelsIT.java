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
package org.hippoecm.hst.configuration.sitemap;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.model.EventPathsInvalidator;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.hst.util.JcrSessionUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SiteMapModelsIT extends AbstractTestConfigurations {

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
    public void test_plain_sitemap_without_workspace_sitemap() throws Exception {
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstSiteMap siteMap = hstSite.getSiteMap();
        assertTrue(siteMap instanceof CanonicalInfo);

        CanonicalInfo siteMapCanonicalInfo = (CanonicalInfo)siteMap;
        assertFalse(siteMapCanonicalInfo.isWorkspaceConfiguration());
        final Node siteMapNode = session.getNodeByIdentifier(siteMapCanonicalInfo.getCanonicalIdentifier());
        assertTrue(siteMapNode.getPath().equals(hstSite.getConfigurationPath() + "/hst:sitemap"));

        for (HstSiteMapItem hstSiteMapItem : siteMap.getSiteMapItems()) {
            if (hstSiteMapItem.getQualifiedId().contains("hst:default")) {
                log.debug("Skip hst:default sitemap");
                continue;
            }
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
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstSiteMap siteMap = hstSite.getSiteMap();

        CanonicalInfo siteMapCanonicalInfo = (CanonicalInfo)siteMap;
        assertTrue(siteMapCanonicalInfo.isWorkspaceConfiguration());
        final Node siteMapNode = session.getNodeByIdentifier(siteMapCanonicalInfo.getCanonicalIdentifier());
        assertTrue(siteMapNode.getPath().equals(hstSite.getConfigurationPath() + "/hst:workspace/hst:sitemap"));

        for (HstSiteMapItem hstSiteMapItem : siteMap.getSiteMapItems()) {
            if (hstSiteMapItem.getQualifiedId().contains("hst:default")) {
                log.debug("Skip hst:default sitemap");
                continue;
            }
            CanonicalInfo siteMapItemCanonicalInfo = (CanonicalInfo)hstSiteMapItem;
            assertTrue(siteMapItemCanonicalInfo.isWorkspaceConfiguration());
            final Node siteMapItemNode = session.getNodeByIdentifier(siteMapItemCanonicalInfo.getCanonicalIdentifier());
            assertTrue(siteMapItemNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:workspace/hst:sitemap"));
        }
    }

    @Test
    public void test_sitemap_partially_in_workspace() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace").addNode("hst:sitemap");
        session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemap/home",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/home");
        session.save();

        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstSiteMap siteMap = hstSite.getSiteMap();

        CanonicalInfo siteMapCanonicalInfo = (CanonicalInfo)siteMap;
        // since not the entire sitemap is in workspace, below gives false
        assertFalse(siteMapCanonicalInfo.isWorkspaceConfiguration());

        final Node siteMapNode = session.getNodeByIdentifier(siteMapCanonicalInfo.getCanonicalIdentifier());
        assertTrue(siteMapNode.getPath().equals(hstSite.getConfigurationPath() + "/hst:sitemap"));

        HstSiteMapItem home = siteMap.getSiteMapItem("home");
        CanonicalInfo homeCanonicalInfo = (CanonicalInfo)home;
        assertTrue(homeCanonicalInfo.isWorkspaceConfiguration());
        final Node homeNode = session.getNodeByIdentifier(homeCanonicalInfo.getCanonicalIdentifier());
        assertTrue(homeNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:workspace/hst:sitemap"));

        for (HstSiteMapItem hstSiteMapItem : siteMap.getSiteMapItems()) {
            CanonicalInfo siteMapItemCanonicalInfo = (CanonicalInfo)hstSiteMapItem;
            if (hstSiteMapItem.getValue().equals("home")) {
                assertTrue(siteMapItemCanonicalInfo.isWorkspaceConfiguration());
                final Node siteMapItemNode = session.getNodeByIdentifier(siteMapItemCanonicalInfo.getCanonicalIdentifier());
                assertTrue(siteMapItemNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:workspace/hst:sitemap"));
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
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstSiteMap siteMap = hstSite.getSiteMap();

        CanonicalInfo siteMapCanonicalInfo = (CanonicalInfo)siteMap;
        // since not the entire sitemap is in workspace, below gives false
        assertFalse(siteMapCanonicalInfo.isWorkspaceConfiguration());

        final Node siteMapNode = session.getNodeByIdentifier(siteMapCanonicalInfo.getCanonicalIdentifier());
        assertTrue(siteMapNode.getPath().equals(hstSite.getConfigurationPath() + "/hst:sitemap"));

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

        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstSiteMap siteMap = hstSite.getSiteMap();

        {
            HstSiteMapItem home = siteMap.getSiteMapItem("home");
            CanonicalInfo homeCanonicalInfo = (CanonicalInfo)home;
            assertTrue(homeCanonicalInfo.isWorkspaceConfiguration());
            final Node homeNode = session.getNodeByIdentifier(homeCanonicalInfo.getCanonicalIdentifier());
            assertTrue(homeNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:workspace/hst:sitemap"));
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

        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstSiteMap siteMap = hstSite.getSiteMap();

        HstSiteMapItem home = siteMap.getSiteMapItem("home");
        CanonicalInfo homeCanonicalInfo = (CanonicalInfo)home;
        assertTrue(homeCanonicalInfo.isWorkspaceConfiguration());
        final Node homeNode = session.getNodeByIdentifier(homeCanonicalInfo.getCanonicalIdentifier());
        assertTrue(homeNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:workspace/hst:sitemap"));
    }

    @Test
    public void workspace_sitemap_from_inherited_configuration_is_by_default_ignored() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestcommon").addNode("hst:workspace").addNode("hst:sitemap");
        session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemap/home",
                "/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemap/home");
        session.save();
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstSiteMap siteMap = hstSite.getSiteMap();
        assertNull(siteMap.getSiteMapItem("home"));
    }

    // test inheritance from hst:workspace via inheritsfrom = ../xyz/hst:workspace
    @Test
    public void hst_sitemap_from_workspace_by_default_not_inherited_unless_explicitly_inherited() throws Exception {
        {
            VirtualHosts vhosts = hstManager.getVirtualHosts();
            final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
            final HstSiteMapItem item = mount.getHstSite().getSiteMap().getSiteMapItem("home");
            assertNotNull(item);
            assertFalse(((CanonicalInfo)item).isWorkspaceConfiguration());
        }
        // now move the [/hst:hst/hst:configurations/unittestcommon/hst:pages] to
        // [/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:pages] and show the 'homepage' is not there
        // any more in the model

        if (!session.nodeExists("/hst:hst/hst:configurations/unittestcommon/hst:workspace")) {
            session.getNode("/hst:hst/hst:configurations/unittestcommon").addNode(HstNodeTypes.NODENAME_HST_WORKSPACE);
        }
        session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemap",
                "/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemap");

        EventPathsInvalidator invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);
        {
            VirtualHosts vhosts = hstManager.getVirtualHosts();
            final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
            final HstSiteMapItem item = mount.getHstSite().getSiteMap().getSiteMapItem("home");
            assertNull(item);
        }

        // for all kind of inheritance variants below, the 'home' sitemap item should be inherited
        List<String[]> inheritanceVariants = new ArrayList<>();
        inheritanceVariants.add(new String[]{"../unittestcommon", "../unittestcommon/hst:workspace"});
        inheritanceVariants.add(new String[]{"../unittestcommon", "../unittestcommon/hst:workspace/hst:sitemap"});
        inheritanceVariants.add(new String[]{"../unittestcommon/hst:workspace", "../unittestcommon"});
        inheritanceVariants.add(new String[]{"../unittestcommon/hst:workspace/hst:sitemap", "../unittestcommon"});
        inheritanceVariants.add(new String[]{"../unittestcommon/hst:workspace"});
        inheritanceVariants.add(new String[]{"../unittestcommon/hst:workspace/hst:sitemap"});
        inheritanceVariants.add(new String[]{"../unittestcommon/hst:workspace/hst:sitemap", "../unittestcommon/hst:workspace"});
        inheritanceVariants.add(new String[]{"../unittestcommon/hst:workspace", "../unittestcommon/hst:workspace/hst:sitemap"});

        for (String[] inheritanceVariant : inheritanceVariants) {

            setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                    inheritanceVariant);

            pathsToBeChanged = new String[]{"/hst:hst/hst:configurations/unittestproject"};
            invalidator.eventPaths(pathsToBeChanged);
            {
                VirtualHosts vhosts = hstManager.getVirtualHosts();
                final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
                final HstSiteMapItem item = mount.getHstSite().getSiteMap().getSiteMapItem("home");
                assertNotNull(item);
                assertTrue(((CanonicalInfo)item).isWorkspaceConfiguration());
                assertNull(item.getParameter("foo"));
            }
            // make sure a change triggers a reload!
            final Node homePageNode = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemap/home");
            homePageNode.setProperty(GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"foo"});
            homePageNode.setProperty(GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"bar"});
            pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
            session.save();
            invalidator.eventPaths(pathsToBeChanged);
            {
                VirtualHosts vhosts = hstManager.getVirtualHosts();
                final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
                final HstSiteMapItem item = mount.getHstSite().getSiteMap().getSiteMapItem("home");
                // assert that the change is reloaded
                assertEquals("bar", item.getParameter("foo"));
            }

            homePageNode.getProperty(GENERAL_PROPERTY_PARAMETER_NAMES).remove();
            homePageNode.getProperty(GENERAL_PROPERTY_PARAMETER_VALUES).remove();
            pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
            session.save();
            invalidator.eventPaths(pathsToBeChanged);
        }
    }


    @Test
    public void test_inheritance_precedence() throws Exception {
        if (!session.nodeExists("/hst:hst/hst:configurations/unittestcommon/hst:workspace")) {
            session.getNode("/hst:hst/hst:configurations/unittestcommon").addNode(HstNodeTypes.NODENAME_HST_WORKSPACE);
        }

        session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemap",
                "/hst:hst/hst:configurations/unittestcommon/hst:sitemap");
        // hst:pages both below unittestcommon AND below unittestcommon/hst:workspace
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestcommon/hst:sitemap",
                "/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemap");

        // both have hst:sitemap/home : Depending on precedence of the hst:inheritsfrom, one of them is merged into the 'unittestproject' model
        final Node homePageNode = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:sitemap/home");
        homePageNode.setProperty(GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"location"});
        homePageNode.setProperty(GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"non-workspace"});
        final Node workspaceHomePageNode = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemap/home");
        workspaceHomePageNode.setProperty(GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"location"});
        workspaceHomePageNode.setProperty(GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"workspace"});

        // add an extra sitemap to workspace
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemap/home",
                "/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemap/homeAgain");


        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                new String[]{"../unittestcommon", "../unittestcommon/hst:workspace"});
        session.save();

        {
            VirtualHosts vhosts = hstManager.getVirtualHosts();
            final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
            final HstSiteMapItem item = mount.getHstSite().getSiteMap().getSiteMapItem("home");
            assertNotNull(item);
            // since we first inherit ../unittestcommon and *then* ../unittestcommon/hst:workspace, we expect the 'home' from
            // unittestcommon/hst:sitemap and not from unittestcommon/hst:workspace/hst:sitemap
            assertEquals("non-workspace", item.getParameter("location"));

            // assert that 'homeAgain' which is only present below 'unittestcommon/hst:workspace/hst:sitemap' is inherited still
            final HstSiteMapItem itemAgain = mount.getHstSite().getSiteMap().getSiteMapItem("homeAgain");
            assertNotNull(itemAgain);
        }

        // switch the inheritance
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                new String[]{"../unittestcommon/hst:workspace", "../unittestcommon"});

        EventPathsInvalidator invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());
        session.save();
        invalidator.eventPaths(new String[]{"/hst:hst/hst:configurations/unittestproject"});

        {
            VirtualHosts vhosts = hstManager.getVirtualHosts();
            final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
            final HstSiteMapItem item = mount.getHstSite().getSiteMap().getSiteMapItem("home");
            assertNotNull(item);
            // since we first inherit ../unittestcommon and *then* ../unittestcommon/hst:workspace, we expect the homepage from
            // unittestcommon/hst:pages and not from unittestcommon/hst:workspace/hst:pages
            assertEquals("workspace", item.getParameter("location"));
        }
    }

    private void setWorkspaceInheritance(final String hstConfigurationPath, final String[] inheritsFrom) throws RepositoryException {
        final Node hstConfigNode = session.getNode(hstConfigurationPath);
        hstConfigNode.setProperty(GENERAL_PROPERTY_INHERITS_FROM, inheritsFrom);
        session.save();
    }

    private String getLocalhostRootMountId() throws RepositoryException {
        return session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root").getIdentifier();
    }


    @Test
    public void marked_deleted_nodes_are_part_of_hst_model_but_ignored_while_matching() throws Exception {

        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
        ResolvedSiteMapItem matchedItemForHomePage = mount.matchSiteMapItem("/");
        ResolvedSiteMapItem matchedItemForNewsFoo = mount.matchSiteMapItem("/news/foo");
        assertEquals("home", matchedItemForHomePage.getHstSiteMapItem().getId());
        assertEquals("news/_default_", matchedItemForNewsFoo.getHstSiteMapItem().getId());

        final Node home = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap/home");
        home.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        home.setProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE, "deleted");
        final Node newsDefault = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap/news/_default_");
        newsDefault.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        newsDefault.setProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE, "deleted");

        EventPathsInvalidator invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);

        mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstSiteMap siteMap = hstSite.getSiteMap();
        assertNotNull(siteMap.getSiteMapItem("home"));
        assertNotNull(siteMap.getSiteMapItem("news").getChild("_default_"));
        assertTrue(siteMap.getSiteMapItem("home").isMarkedDeleted());
        assertTrue(siteMap.getSiteMapItem("news").getChild("_default_").isMarkedDeleted());

        // matching again "/" and "/news/foo" should not result in same sitemap items because they are excluded from matching
        matchedItemForHomePage = mount.matchSiteMapItem("/");
        matchedItemForNewsFoo = mount.matchSiteMapItem("/news/foo");
        assertFalse("home".equals(matchedItemForHomePage.getHstSiteMapItem().getId()));
        assertFalse("news/_default_".equals(matchedItemForNewsFoo.getHstSiteMapItem().getId()));
    }

    @Test
    public void marked_deleted_nodes_their_children_are_marked_deleted_as_well() throws Exception {
        final Node news = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap/news");
        news.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        news.setProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE, "deleted");
        session.save();

        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstSiteMap siteMap = hstSite.getSiteMap();
        assertTrue(siteMap.getSiteMapItem("news").isMarkedDeleted());
        assertTrue(siteMap.getSiteMapItem("news").getChild("_default_").isMarkedDeleted());
    }

    @Test
    public void parameternames_values_from_mount_are_resolved_on_sitemap_items() throws Exception {
        final Node testprojectMount = session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
        testprojectMount.setProperty(GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"foo"});
        testprojectMount.setProperty(GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"bar"});
        final Node anyGIF = session.getNode("/hst:hst/hst:configurations/hst:default/hst:sitemap/_any_.GIF");
        anyGIF.setProperty(GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"lux"});
        // ${foo} is a property lookup from the mount
        anyGIF.setProperty(GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"${foo}"});
        session.save();

        HstSiteMapItem siteMapItem = hstManager.getVirtualHosts().getMountByIdentifier(testprojectMount.getIdentifier()).getHstSite()
                .getSiteMap().getSiteMapItem("_any_.GIF");

        assertEquals("bar", siteMapItem.getParameter("lux"));
    }

    @Test
    public void parameternames_values_from_multiple_mounts_are_resolved_per_mount_for_shared_sitemap_items() throws Exception {
        final Node testprojectMount = session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
        testprojectMount.setProperty(GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"foo"});
        testprojectMount.setProperty(GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"bar"});
        final Node testsubprojectMount = session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root/subsite");
        testsubprojectMount.setProperty(GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"foo"});
        testsubprojectMount.setProperty(GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"sub_bar"});

        final Node anyGIF = session.getNode("/hst:hst/hst:configurations/hst:default/hst:sitemap/_any_.GIF");
        anyGIF.setProperty(GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"lux"});
        // ${foo} is a property lookup from the mount
        anyGIF.setProperty(GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"${foo}"});
        session.save();

        HstSiteMapItem siteMapItem = hstManager.getVirtualHosts().getMountByIdentifier(testprojectMount.getIdentifier()).getHstSite()
                .getSiteMap().getSiteMapItem("_any_.GIF");

        assertEquals("bar", siteMapItem.getParameter("lux"));

        HstSiteMapItem siteMapItemSub = hstManager.getVirtualHosts().getMountByIdentifier(testsubprojectMount.getIdentifier()).getHstSite()
                .getSiteMap().getSiteMapItem("_any_.GIF");

        assertEquals("sub_bar", siteMapItemSub.getParameter("lux"));
    }
}
