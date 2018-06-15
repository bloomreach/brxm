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
package org.hippoecm.hst.platform.configuration.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.core.request.ResolvedMount;
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
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_SITEMENUITEM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HstMenuModelsIT extends AbstractTestConfigurations {

    private HstManager hstManager;
    private EventPathsInvalidator invalidator;
    private Session session;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        session = createSession();
        createHstConfigBackup(session);
        hstManager = getComponent(HstManager.class.getName());
        invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        restoreHstConfigBackup(session);
        session.logout();
        super.tearDown();
    }

    @Test
    public void test_menu_inheritance_without_workspace_menus() throws Exception {
        // mount1 has its own menu stored at /unittestproject/hst:sitemenus and
        // mount2 inherits it via unittestsubproject from /unittestcommon
        final String mainUnitTestProjectMenuIdentifier;
        {
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
            final HstSite hstSite = mount.getMount().getHstSite();
            final Map<String,HstSiteMenuConfiguration> unitTestProjectMenus = hstSite.getSiteMenusConfiguration().getSiteMenuConfigurations();
            assertEquals(1, unitTestProjectMenus.size());
            final HstSiteMenuConfiguration mainUnitTestProjectMenu = unitTestProjectMenus.values().iterator().next();
            assertFalse(((CanonicalInfo)mainUnitTestProjectMenu).isWorkspaceConfiguration());
            mainUnitTestProjectMenuIdentifier = ((CanonicalInfo)mainUnitTestProjectMenu).getCanonicalIdentifier();
            final Node unitTestMenuJcrNode = session.getNodeByIdentifier(mainUnitTestProjectMenuIdentifier);
            assertTrue(unitTestMenuJcrNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:sitemenus"));
        }

        final String mainUnitTestSubProjectMenuIdentifier;

        {
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/subsite");
            final HstSite hstSite = mount.getMount().getHstSite();
            final Map<String,HstSiteMenuConfiguration> unitTestSubProjectMenus = hstSite.getSiteMenusConfiguration().getSiteMenuConfigurations();
            assertEquals(1, unitTestSubProjectMenus.size());
            final HstSiteMenuConfiguration mainUnitTestSubProjectMenu = unitTestSubProjectMenus.values().iterator().next();
            // sub project inherits main menu
            assertFalse(((CanonicalInfo) mainUnitTestSubProjectMenu).isWorkspaceConfiguration());
            mainUnitTestSubProjectMenuIdentifier = ((CanonicalInfo)mainUnitTestSubProjectMenu).getCanonicalIdentifier();
            assertFalse(mainUnitTestProjectMenuIdentifier.equals(mainUnitTestSubProjectMenuIdentifier));
            final Node unitTestSubProjectJcrNode = session.getNodeByIdentifier(mainUnitTestSubProjectMenuIdentifier);
            // inherited node is not part of sub project configuration path
            assertFalse(unitTestSubProjectJcrNode.getPath().startsWith(hstSite.getConfigurationPath()));
            assertTrue(unitTestSubProjectJcrNode.getPath().startsWith("/hst:hst/hst:configurations/unittestcommon/hst:sitemenus"));
        }

        // now delete /unittestproject/hst:sitemenus and show the inherited one is used from common
        final Node mainMenuNode = session.getNodeByIdentifier(mainUnitTestProjectMenuIdentifier);
        mainMenuNode.remove();
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);

        {
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
            final HstSite hstSite = mount.getMount().getHstSite();
            final Map<String,HstSiteMenuConfiguration> unitTestProjectMenus = hstSite.getSiteMenusConfiguration().getSiteMenuConfigurations();
            assertEquals(1, unitTestProjectMenus.size());
            final HstSiteMenuConfiguration mainUnitTestProjectMenu = unitTestProjectMenus.values().iterator().next();
            // menu is now inherited
            assertFalse(((CanonicalInfo) mainUnitTestProjectMenu).isWorkspaceConfiguration());
            final String newMainUnitTestProjectMenuIdentifier = ((CanonicalInfo)mainUnitTestProjectMenu).getCanonicalIdentifier();
            assertFalse(mainUnitTestProjectMenuIdentifier.equals(newMainUnitTestProjectMenuIdentifier));
            // now the unittestproject inherits the menu from common config
            final Node unitTestMenuJcrNode = session.getNodeByIdentifier(((CanonicalInfo)mainUnitTestProjectMenu).getCanonicalIdentifier());
            assertFalse(unitTestMenuJcrNode.getPath().startsWith(hstSite.getConfigurationPath()));
        }

    }

    @Test
    public void test_menu_in_workspace() throws Exception {
        // start with moving shared menu from 'unittestcommon' to hst:workspace of 'unittestcommon'
        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace").addNode("hst:sitemenus");
        session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemenus/main",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemenus/main");

        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);

        // unittestproject sitemenu should now be loaded from 'hst:workspace'
        {
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
            final HstSite hstSite = mount.getMount().getHstSite();
            final Map<String,HstSiteMenuConfiguration> unitTestProjectMenus = hstSite.getSiteMenusConfiguration().getSiteMenuConfigurations();
            final HstSiteMenuConfiguration mainUnitTestProjectMenu = unitTestProjectMenus.values().iterator().next();
            assertTrue(((CanonicalInfo) mainUnitTestProjectMenu).isWorkspaceConfiguration());
            final Node unitTestMenuJcrNode = session.getNodeByIdentifier(((CanonicalInfo)mainUnitTestProjectMenu).getCanonicalIdentifier());
            assertTrue(unitTestMenuJcrNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:workspace/hst:sitemenus"));

            assertEquals(7, mainUnitTestProjectMenu.getSiteMenuConfigurationItems().size());
            for (HstSiteMenuItemConfiguration menuItem : mainUnitTestProjectMenu.getSiteMenuConfigurationItems()) {
                String mainMenuNodeIdentifier = ((CanonicalInfo)menuItem).getCanonicalIdentifier();
                final Node mainMenuNode = session.getNodeByIdentifier(mainMenuNodeIdentifier);
                assertTrue(mainMenuNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:workspace/hst:sitemenus/main"));
            }
        }
    }

    @Test
    public void test_combined_menus_in_workspace_and_non_workspace() throws Exception {
        // start with COPYING 'main' to 'footer' in workspace
        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace").addNode("hst:sitemenus");
        JcrUtils.copy(session,
                "/hst:hst/hst:configurations/unittestproject/hst:sitemenus/main",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemenus/footer");
        session.save();
        // unittestproject sitemenu 'footer' should now be loaded from 'hst:workspace' but 'main' not from workspace
        {
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
            final HstSite hstSite = mount.getMount().getHstSite();
            final Map<String,HstSiteMenuConfiguration> unitTestProjectMenus = hstSite.getSiteMenusConfiguration().getSiteMenuConfigurations();
            assertEquals(2, unitTestProjectMenus.values().size());

            {
                final HstSiteMenuConfiguration mainUnitTestProjectMenu = unitTestProjectMenus.get("main");
                assertFalse(((CanonicalInfo) mainUnitTestProjectMenu).isWorkspaceConfiguration());
                final Node unitTestMenuJcrNode = session.getNodeByIdentifier(((CanonicalInfo)mainUnitTestProjectMenu).getCanonicalIdentifier());
                assertTrue(unitTestMenuJcrNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:sitemenus"));

                assertEquals(7, mainUnitTestProjectMenu.getSiteMenuConfigurationItems().size());
                for (HstSiteMenuItemConfiguration menuItem : mainUnitTestProjectMenu.getSiteMenuConfigurationItems()) {
                    String mainMenuNodeIdentifier = ((CanonicalInfo)menuItem).getCanonicalIdentifier();
                    final Node mainMenuNode = session.getNodeByIdentifier(mainMenuNodeIdentifier);
                    assertTrue(mainMenuNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:sitemenus/main"));
                }
            }

            {
                final HstSiteMenuConfiguration footerUnitTestProjectMenu = unitTestProjectMenus.get("footer");
                assertTrue(((CanonicalInfo) footerUnitTestProjectMenu).isWorkspaceConfiguration());
                final Node unitTestMenuJcrNode = session.getNodeByIdentifier(((CanonicalInfo)footerUnitTestProjectMenu).getCanonicalIdentifier());
                assertTrue(unitTestMenuJcrNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:workspace/hst:sitemenus"));

                assertEquals(7, footerUnitTestProjectMenu.getSiteMenuConfigurationItems().size());
                for (HstSiteMenuItemConfiguration menuItem : footerUnitTestProjectMenu.getSiteMenuConfigurationItems()) {
                    String mainMenuNodeIdentifier = ((CanonicalInfo)menuItem).getCanonicalIdentifier();
                    final Node mainMenuNode = session.getNodeByIdentifier(mainMenuNodeIdentifier);
                    assertTrue(mainMenuNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:workspace/hst:sitemenus/footer"));
                }
            }
        }
    }

    @Test
    public void test_inherited_configuration_workspace_menu_is_not_included() throws Exception {
        String commonConfigPath = "/hst:hst/hst:configurations/unittestcommon";
        session.getNode(commonConfigPath).addNode("hst:workspace").addNode("hst:sitemenus");
        // start with moving shared menu from 'unittestcommon' to hst:workspace of 'unittestcommon'
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:sitemenus/main",
                "/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemenus/footer");
        session.save();

        // nothing changed to own unittestproject sitemenu : BECAUSE IT HAS OWN sitemenus IT DOES NOT INHERIT FROM 'common workspace' the
        // 'footer; item
        {
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
            final HstSite hstSite = mount.getMount().getHstSite();
            final Map<String,HstSiteMenuConfiguration> unitTestProjectMenus = hstSite.getSiteMenusConfiguration().getSiteMenuConfigurations();
            final HstSiteMenuConfiguration mainUnitTestProjectMenu = unitTestProjectMenus.values().iterator().next();
            assertFalse(((CanonicalInfo) mainUnitTestProjectMenu).isWorkspaceConfiguration());
            final Node unitTestMenuJcrNode = session.getNodeByIdentifier(((CanonicalInfo)mainUnitTestProjectMenu).getCanonicalIdentifier());
            assertTrue(unitTestMenuJcrNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:sitemenus"));

            assertEquals(7, mainUnitTestProjectMenu.getSiteMenuConfigurationItems().size());
            for (HstSiteMenuItemConfiguration menuItem : mainUnitTestProjectMenu.getSiteMenuConfigurationItems()) {
                String mainMenuNodeIdentifier = ((CanonicalInfo)menuItem).getCanonicalIdentifier();
                final Node mainMenuNode = session.getNodeByIdentifier(mainMenuNodeIdentifier);
                assertTrue(mainMenuNode.getPath().startsWith("/hst:hst/hst:configurations/unittestproject/hst:sitemenus/main"));
            }

        }

        // subproject should NOT have the menu from inherited hst:workspace even though it has NO own hst:sitemenu : by default,
        // hst:workspace is not inherited
        {
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/subsite");
            final HstSite hstSite = mount.getMount().getHstSite();
            final Map<String,HstSiteMenuConfiguration> unitTestSubProjectMenus = hstSite.getSiteMenusConfiguration().getSiteMenuConfigurations();
            assertTrue(unitTestSubProjectMenus.values().isEmpty());
        }

        // now move /hst:hst/hst:configurations/unittestproject/hst:sitemenus
        // to /hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemenus

        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace").addNode("hst:sitemenus");
        session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemenus/main",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemenus/main");

        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);

        // unittestproject sitemenu should now be loaded from 'hst:workspace'
        {
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
            final HstSite hstSite = mount.getMount().getHstSite();
            final Map<String,HstSiteMenuConfiguration> unitTestProjectMenus = hstSite.getSiteMenusConfiguration().getSiteMenuConfigurations();
            final HstSiteMenuConfiguration mainUnitTestProjectMenu = unitTestProjectMenus.values().iterator().next();
            assertTrue(((CanonicalInfo) mainUnitTestProjectMenu).isWorkspaceConfiguration());
            final Node unitTestMenuJcrNode = session.getNodeByIdentifier(((CanonicalInfo)mainUnitTestProjectMenu).getCanonicalIdentifier());
            assertTrue(unitTestMenuJcrNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:workspace/hst:sitemenus"));

            assertEquals(7, mainUnitTestProjectMenu.getSiteMenuConfigurationItems().size());
            for (HstSiteMenuItemConfiguration menuItem : mainUnitTestProjectMenu.getSiteMenuConfigurationItems()) {
                String mainMenuNodeIdentifier = ((CanonicalInfo)menuItem).getCanonicalIdentifier();
                final Node mainMenuNode = session.getNodeByIdentifier(mainMenuNodeIdentifier);
                assertTrue(mainMenuNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:workspace/hst:sitemenus/main"));
            }
        }

        // now delete /hst:hst/hst:configurations/unittestproject/hst:sitemenus
        // unittestproject should now use the hst:workspace menu only
        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemenus").remove();
        pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);

        // unittestproject sitemenu should now be loaded from 'unittestproject/hst:workspace'
        {
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
            final HstSite hstSite = mount.getMount().getHstSite();
            final Map<String,HstSiteMenuConfiguration> unitTestProjectMenus = hstSite.getSiteMenusConfiguration().getSiteMenuConfigurations();
            final HstSiteMenuConfiguration mainUnitTestProjectMenu = unitTestProjectMenus.values().iterator().next();
            assertTrue(((CanonicalInfo) mainUnitTestProjectMenu).isWorkspaceConfiguration());
            final Node unitTestMenuJcrNode = session.getNodeByIdentifier(((CanonicalInfo)mainUnitTestProjectMenu).getCanonicalIdentifier());
            assertTrue(unitTestMenuJcrNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:workspace/hst:sitemenus"));

            assertEquals(7, mainUnitTestProjectMenu.getSiteMenuConfigurationItems().size());
            for (HstSiteMenuItemConfiguration menuItem : mainUnitTestProjectMenu.getSiteMenuConfigurationItems()) {
                String mainMenuNodeIdentifier = ((CanonicalInfo)menuItem).getCanonicalIdentifier();
                final Node mainMenuNode = session.getNodeByIdentifier(mainMenuNodeIdentifier);
                assertTrue(mainMenuNode.getPath().startsWith(hstSite.getConfigurationPath() + "/hst:workspace/hst:sitemenus/main"));
            }
        }

        // now delete /hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemenus
        // unittestproject should still not use the hst:workspace menu from
        // /hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemenus and thus have no menu items

        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemenus").remove();
        pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);

        {
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
            final HstSite hstSite = mount.getMount().getHstSite();
            final Map<String,HstSiteMenuConfiguration> unitTestProjectMenus = hstSite.getSiteMenusConfiguration().getSiteMenuConfigurations();
            assertTrue(unitTestProjectMenus.values().isEmpty());
        }
    }

    @Test
    public void test_workspace_and_non_workspace_menu_duplicate_nodes_skips_workspace_nodes() throws Exception {

        ResolvedMount beforeMount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/subsite");
        final HstSite beforeSite = beforeMount.getMount().getHstSite();
        final Map<String,HstSiteMenuConfiguration> beforeMenus = beforeSite.getSiteMenusConfiguration().getSiteMenuConfigurations();

        // start with COPYING 'main' to 'main' in workspace : 'main' in workspace should be ignored as duplicate and
        // default hst:sitemenus has precedence
        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace").addNode("hst:sitemenus");
        JcrUtils.copy(session,
                "/hst:hst/hst:configurations/unittestproject/hst:sitemenus/main",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemenus/main");

        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);

        // reloading and them fetching menu again should result in exact same menu as before
        ResolvedMount afterMount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/subsite");
        final HstSite afterSite = afterMount.getMount().getHstSite();
        final Map<String,HstSiteMenuConfiguration> afterMenus = afterSite.getSiteMenusConfiguration().getSiteMenuConfigurations();

        assertFalse("There should had been a model reload!", beforeMenus == afterMenus);
        assertTrue(beforeMenus.size() == afterMenus.size());

        assertTrue("Workspace menu 'main' should be skipped as 'main' already present at unittestproject/hst:sitemenus",
                ((CanonicalInfo)beforeMenus.get("main")).getCanonicalIdentifier().equals(((CanonicalInfo)afterMenus.get("main")).getCanonicalIdentifier()));

    }

    // test inheritance from hst:workspace via inheritsfrom = ../xyz/hst:workspace
    @Test
    public void menu_from_workspace_by_default_not_inherited_unless_explicitly_inherited() throws Exception {
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestcommon/hst:sitemenus/main",
                "/hst:hst/hst:configurations/unittestcommon/hst:sitemenus/footer");
        session.save();

        {
            VirtualHosts vhosts = hstManager.getVirtualHosts();
            final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
            final Map<String, HstSiteMenuConfiguration> siteMenuConfigurations = mount.getHstSite().getSiteMenusConfiguration().getSiteMenuConfigurations();
            assertNotNull(siteMenuConfigurations.get("main"));
            assertNotNull(siteMenuConfigurations.get("footer"));
            assertFalse(((CanonicalInfo)siteMenuConfigurations.get("main")).isWorkspaceConfiguration());
        }

        // now move the [/hst:hst/hst:configurations/unittestcommon/hst:sitemenus/footer] to
        // [/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemenus/footer] and show the 'footer' is not
        // there any more in the model

        if (!session.nodeExists("/hst:hst/hst:configurations/unittestcommon/hst:workspace")) {
            session.getNode("/hst:hst/hst:configurations/unittestcommon").addNode(NODENAME_HST_WORKSPACE);
        }
        if (!session.nodeExists("/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemenus")) {
            session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:workspace").addNode(HstNodeTypes.NODENAME_HST_SITEMENUS);
        }

        session.move("/hst:hst/hst:configurations/unittestcommon/hst:sitemenus/footer",
                "/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemenus/footer");

        EventPathsInvalidator invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);
        {
            VirtualHosts vhosts = hstManager.getVirtualHosts();
            final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
            final Map<String, HstSiteMenuConfiguration> siteMenuConfigurations = mount.getHstSite().getSiteMenusConfiguration().getSiteMenuConfigurations();
            assertNotNull(siteMenuConfigurations.get("main"));
            assertNull(siteMenuConfigurations.get("footer"));
        }

        // for all kind of inheritance variants below, the 'footer' menu should be inherited now
        List<String[]> inheritanceVariants = new ArrayList<>();
        inheritanceVariants.add(new String[]{"../unittestcommon", "../unittestcommon/hst:workspace"});
        inheritanceVariants.add(new String[]{"../unittestcommon", "../unittestcommon/hst:workspace/hst:sitemenus"});
        inheritanceVariants.add(new String[]{"../unittestcommon/hst:workspace", "../unittestcommon"});
        inheritanceVariants.add(new String[]{"../unittestcommon/hst:workspace/hst:sitemenus", "../unittestcommon"});
        inheritanceVariants.add(new String[]{"../unittestcommon/hst:workspace"});
        inheritanceVariants.add(new String[]{"../unittestcommon/hst:workspace/hst:sitemenus"});
        inheritanceVariants.add(new String[]{"../unittestcommon/hst:workspace/hst:sitemenus", "../unittestcommon/hst:workspace"});
        inheritanceVariants.add(new String[]{"../unittestcommon/hst:workspace", "../unittestcommon/hst:workspace/hst:sitemenus"});

        for (String[] inheritanceVariant : inheritanceVariants) {

            setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                    inheritanceVariant);

            pathsToBeChanged = new String[]{"/hst:hst/hst:configurations/unittestproject"};
            invalidator.eventPaths(pathsToBeChanged);
            {
                VirtualHosts vhosts = hstManager.getVirtualHosts();
                final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
                final Map<String, HstSiteMenuConfiguration> siteMenuConfigurations = mount.getHstSite().getSiteMenusConfiguration().getSiteMenuConfigurations();
                assertNotNull(siteMenuConfigurations.get("main"));
                assertNotNull(siteMenuConfigurations.get("footer"));
                assertFalse(((CanonicalInfo)siteMenuConfigurations.get("main")).isWorkspaceConfiguration());
                assertTrue(((CanonicalInfo)siteMenuConfigurations.get("footer")).isWorkspaceConfiguration());
            }
            // make sure a change triggers a reload!
            session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemenus/footer").addNode("newItem", NODETYPE_HST_SITEMENUITEM);
            pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
            session.save();
            invalidator.eventPaths(pathsToBeChanged);
            {
                VirtualHosts vhosts = hstManager.getVirtualHosts();
                final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
                final Map<String, HstSiteMenuConfiguration> siteMenuConfigurations = mount.getHstSite().getSiteMenusConfiguration().getSiteMenuConfigurations();
                boolean expectedItemFound = false;
                for (HstSiteMenuItemConfiguration itemConfiguration : siteMenuConfigurations.get("footer").getSiteMenuConfigurationItems()) {
                    if ("newItem".equals(itemConfiguration.getName())) {
                        expectedItemFound = true;
                    }
                }
                assertTrue(expectedItemFound);
            }

            session.removeItem("/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemenus/footer/newItem");
            pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
            session.save();
            invalidator.eventPaths(pathsToBeChanged);
        }
    }


    @Test
    public void test_inheritance_precedence() throws Exception {
        if (!session.nodeExists("/hst:hst/hst:configurations/unittestcommon/hst:workspace")) {
            session.getNode("/hst:hst/hst:configurations/unittestcommon").addNode(NODENAME_HST_WORKSPACE);
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
}
