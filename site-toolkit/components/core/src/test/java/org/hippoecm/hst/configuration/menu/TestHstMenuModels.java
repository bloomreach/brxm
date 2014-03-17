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
package org.hippoecm.hst.configuration.menu;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.model.EventPathsInvalidator;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.hst.util.JcrSessionUtils;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestHstMenuModels extends AbstractTestConfigurations {

    private HstManager hstManager;
    private EventPathsInvalidator invalidator;
    private HippoSession session;

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


    protected HippoSession createSession() throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return (HippoSession)repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

    @Test
    public void test_menu_inheritance_without_workspace_menus() throws Exception {
        // mount1 has its own menu stored at /unittestproject/hst:sitemenus and
        // mount2 inherits it via unittestsubproject from /unittestcommon
        final String mainUnitTestProjectMenuIdentifier;
        {
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
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
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/subsite");
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
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
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
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
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
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
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
    public void test_inherited_configuration_workspace_menu_is_only_included_when_no_own_sitemenus() throws Exception {
        String commonConfigPath = "/hst:hst/hst:configurations/unittestcommon";
        session.getNode(commonConfigPath).addNode("hst:workspace").addNode("hst:sitemenus");
        // start with moving shared menu from 'unittestcommon' to hst:workspace of 'unittestcommon'
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:sitemenus/main",
                "/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemenus/footer");
        session.save();

        // nothing changed to own unittestproject sitemenu : BECAUSE IT HAS OWN sitemenus IT DOES NOT INHERIT FROM 'common workspace' the
        // 'footer; item
        {
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
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

        // subproject should have the menu from inherited hst:workspace SINCE it has NO own hst:sitemenu
        {
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/subsite");
            final HstSite hstSite = mount.getMount().getHstSite();
            final Map<String,HstSiteMenuConfiguration> unitTestSubProjectMenus = hstSite.getSiteMenusConfiguration().getSiteMenuConfigurations();
            final HstSiteMenuConfiguration mainUnitTestSubProjectMenu = unitTestSubProjectMenus.values().iterator().next();
            final Node unitTestSubProjectJcrNode = session.getNodeByIdentifier(((CanonicalInfo)mainUnitTestSubProjectMenu).getCanonicalIdentifier());
            // inherited node is not part of sub project configuration path
            assertFalse(unitTestSubProjectJcrNode.getPath().startsWith(hstSite.getConfigurationPath()));
            assertTrue(unitTestSubProjectJcrNode.getPath().startsWith("/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemenus"));

            assertEquals(1, mainUnitTestSubProjectMenu.getSiteMenuConfigurationItems().size());
            final HstSiteMenuItemConfiguration newsMenuItem = mainUnitTestSubProjectMenu.getSiteMenuConfigurationItems().get(0);

            assertEquals(newsMenuItem.getName(), "News");
            String mainMenuNodeIdentifier = ((CanonicalInfo)newsMenuItem).getCanonicalIdentifier();
            final Node mainMenuNode = session.getNodeByIdentifier(mainMenuNodeIdentifier);
            assertTrue(mainMenuNode.getPath().equals("/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemenus/footer/News"));
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
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
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
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
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
        // unittestproject should now use the hst:workspace menu from
        // /hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemenus
        // and thus only have the 'footer' menu

        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemenus").remove();
        pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);

        {
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
            final HstSite hstSite = mount.getMount().getHstSite();
            final Map<String,HstSiteMenuConfiguration> unitTestProjectMenus = hstSite.getSiteMenusConfiguration().getSiteMenuConfigurations();
            final HstSiteMenuConfiguration mainUnitTestProjectMenu = unitTestProjectMenus.values().iterator().next();
            assertTrue(((CanonicalInfo)mainUnitTestProjectMenu).isWorkspaceConfiguration());
            final Node unitTestMenuJcrNode = session.getNodeByIdentifier(((CanonicalInfo)mainUnitTestProjectMenu).getCanonicalIdentifier());
            assertTrue(unitTestMenuJcrNode.getPath().startsWith("/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemenus"));

            assertEquals(1, mainUnitTestProjectMenu.getSiteMenuConfigurationItems().size());
            for (HstSiteMenuItemConfiguration menuItem : mainUnitTestProjectMenu.getSiteMenuConfigurationItems()) {
                String mainMenuNodeIdentifier = ((CanonicalInfo)menuItem).getCanonicalIdentifier();
                final Node mainMenuNode = session.getNodeByIdentifier(mainMenuNodeIdentifier);
                assertTrue(mainMenuNode.getPath().startsWith("/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:sitemenus/footer"));
            }
        }
    }

    @Test
    public void test_workspace_and_non_workspace_menu_duplicate_nodes_skips_workspace_nodes() throws Exception {

        ResolvedMount beforeMount = hstManager.getVirtualHosts().matchMount("localhost", "", "/subsite");
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
        ResolvedMount afterMount = hstManager.getVirtualHosts().matchMount("localhost", "", "/subsite");
        final HstSite afterSite = afterMount.getMount().getHstSite();
        final Map<String,HstSiteMenuConfiguration> afterMenus = afterSite.getSiteMenusConfiguration().getSiteMenuConfigurations();

        assertFalse("There should had been a model reload!", beforeMenus == afterMenus);
        assertTrue(beforeMenus.size() == afterMenus.size());

        assertTrue("Workspace menu 'main' should be skipped as 'main' already present at unittestproject/hst:sitemenus",
                ((CanonicalInfo)beforeMenus.get("main")).getCanonicalIdentifier().equals(((CanonicalInfo)afterMenus.get("main")).getCanonicalIdentifier()));

    }

}
