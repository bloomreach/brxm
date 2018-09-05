/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.pages;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.EventPathsInvalidator;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.platform.HstModelProvider;
import org.hippoecm.hst.platform.api.model.PlatformHstModel;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.hst.util.JcrSessionUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNull;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PagesModelsIT extends AbstractTestConfigurations {

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
    public void test_unittestcommon_pages_without_workspace_pages() throws Exception {
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        for (HstComponentConfiguration hstComponentConfiguration : hstSite.getComponentsConfiguration().getComponentConfigurations().values()) {
            assertTrue(hstComponentConfiguration.isInherited());
            assertTrue(hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestcommon"));
        }
    }

    @Test
    public void test_unittestproject_pages_without_workspace_pages() throws Exception {
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:pages",
                "/hst:hst/hst:configurations/unittestproject/hst:pages");
        saveSession();
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        for (HstComponentConfiguration hstComponentConfiguration : hstSite.getComponentsConfiguration().getComponentConfigurations().values()) {
            if (hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestcommon/")) {
                // components are still from /unittestcommon
                assertTrue(hstComponentConfiguration.isInherited());
            } else {
                assertFalse(hstComponentConfiguration.isInherited());
            }
        }
    }

    @Test
    public void test_pages_completely_in_workspace() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace");
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:pages",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages");
        saveSession();
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        for (HstComponentConfiguration hstComponentConfiguration : hstSite.getComponentsConfiguration().getComponentConfigurations().values()) {
            if (hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestcommon/")) {
                // components are still from /unittestcommon
                assertTrue(hstComponentConfiguration.isInherited());
            } else {
                assertFalse(hstComponentConfiguration.isInherited());
            }
        }
    }

    @Test
    public void test_page_partially_in_workspace() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace");
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:pages",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages");

        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:pages");
        session.move("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/standarddetail",
                "/hst:hst/hst:configurations/unittestproject/hst:pages/standarddetail");

        saveSession();

        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        for (HstComponentConfiguration hstComponentConfiguration : hstSite.getComponentsConfiguration().getComponentConfigurations().values()) {
            if (hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestcommon/")) {
                // components are still from /unittestcommon
                assertTrue(hstComponentConfiguration.isInherited());
            } else {
                HstComponentConfiguration root = hstComponentConfiguration;
                while (root.getParent() != null) {
                    root = root.getParent();
                }
                if (root.getName().equals("standarddetail")) {
                    assertFalse(hstComponentConfiguration.isInherited());
                    assertTrue(hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestproject/hst:pages/standarddetail"));
                } else {
                    assertFalse(hstComponentConfiguration.isInherited());
                    assertTrue(hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestproject/hst:workspace"));
                }
            }
        }
    }

    @Test
    public void test_page_duplicate_node_in_workspace_is_skipped() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace");
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:pages",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages");

        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:pages");

        JcrUtils.copy(session,"/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/standarddetail",
                "/hst:hst/hst:configurations/unittestproject/hst:pages/standarddetail");

        saveSession();

        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        for (HstComponentConfiguration hstComponentConfiguration : hstSite.getComponentsConfiguration().getComponentConfigurations().values()) {
            if (hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestcommon/")) {
                // components are still from /unittestcommon
                assertTrue(hstComponentConfiguration.isInherited());
            } else {
                HstComponentConfiguration root = hstComponentConfiguration;
                while (root.getParent() != null) {
                    root = root.getParent();
                }
                if (root.getName().equals("standarddetail")) {
                    assertFalse(hstComponentConfiguration.isInherited());
                    assertTrue(hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestproject/hst:pages/standarddetail"));
                } else {
                    assertFalse(hstComponentConfiguration.isInherited());
                    assertTrue(hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestproject/hst:workspace"));
                }
            }
        }
    }

    @Test
    public void workspace_pages_from_inherited_configuration_are_by_default_ignored() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestcommon").addNode("hst:workspace");
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:pages",
                "/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:pages");
        saveSession();
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        for (HstComponentConfiguration hstComponentConfiguration : hstSite.getComponentsConfiguration().getComponentConfigurations().values()) {
            assertTrue(hstComponentConfiguration.isInherited());
            assertTrue(hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestcommon/"));
        }
        assertNull(hstSite.getComponentsConfiguration().getComponentConfigurations().get("standarddetail"));
    }

    // test inheritance from hst:workspace via inheritsfrom = ../xyz/hst:workspace
    @Test
    public void hst_components_from_workspace_by_default_not_inherited_unless_explicitly_inherited() throws Exception {
        {
            VirtualHosts vhosts = hstManager.getVirtualHosts();
            final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
            final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
            assertNotNull(pageComponent);
            assertTrue(pageComponent.isInherited());
        }
        // now move the [/hst:hst/hst:configurations/unittestcommon/hst:pages] to
        // [/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:pages] and show the 'homepage' is not there any more

        if (!session.nodeExists("/hst:hst/hst:configurations/unittestcommon/hst:workspace")) {
            session.getNode("/hst:hst/hst:configurations/unittestcommon").addNode(HstNodeTypes.NODENAME_HST_WORKSPACE);
        }
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:pages",
                "/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:pages");

        final HstModelProvider provider = HstServices.getComponentManager().getComponent(HstModelProvider.class);
        final EventPathsInvalidator invalidator = ((PlatformHstModel) provider.getHstModel()).getEventPathsInvalidator();
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        saveSession();

        invalidator.eventPaths(pathsToBeChanged);
        {
            VirtualHosts vhosts = hstManager.getVirtualHosts();
            final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
            final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
            Assert.assertNull(pageComponent);
        }

        // for all kind of inheritance variants below, the 'homepage' should be inherited
        List<String[]> inheritanceVariants = new ArrayList<>();
        inheritanceVariants.add(new String[]{"../unittestcommon", "../unittestcommon/hst:workspace"});
        inheritanceVariants.add(new String[]{"../unittestcommon", "../unittestcommon/hst:workspace/hst:pages"});
        inheritanceVariants.add(new String[]{"../unittestcommon/hst:workspace", "../unittestcommon"});
        inheritanceVariants.add(new String[]{"../unittestcommon/hst:workspace/hst:pages", "../unittestcommon"});
        inheritanceVariants.add(new String[]{"../unittestcommon/hst:workspace"});
        inheritanceVariants.add(new String[]{"../unittestcommon/hst:workspace/hst:pages"});
        inheritanceVariants.add(new String[]{"../unittestcommon/hst:workspace/hst:pages", "../unittestcommon/hst:workspace"});
        inheritanceVariants.add(new String[]{"../unittestcommon/hst:workspace", "../unittestcommon/hst:workspace/hst:pages"});

        for (String[] inheritanceVariant : inheritanceVariants) {

            setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                    inheritanceVariant);

            pathsToBeChanged = new String[]{"/hst:hst/hst:configurations/unittestproject"};
            invalidator.eventPaths(pathsToBeChanged);
            {
                VirtualHosts vhosts = hstManager.getVirtualHosts();
                final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
                final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
                assertNotNull(pageComponent);
                assertTrue(pageComponent.isInherited());
                Assert.assertNull(pageComponent.getParameter("foo"));
            }
            // make sure a change triggers a reload!
            final Node homePageNode = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:pages/homepage");
            homePageNode.setProperty(GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"foo"});
            homePageNode.setProperty(GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"bar"});
            pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
            saveSession();
            invalidator.eventPaths(pathsToBeChanged);
            {
                VirtualHosts vhosts = hstManager.getVirtualHosts();
                final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
                final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
                // assert that the change is reloaded
                assertEquals("bar", pageComponent.getParameter("foo"));
            }

            homePageNode.getProperty(GENERAL_PROPERTY_PARAMETER_NAMES).remove();
            homePageNode.getProperty(GENERAL_PROPERTY_PARAMETER_VALUES).remove();
            pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
            saveSession();
            invalidator.eventPaths(pathsToBeChanged);
        }
    }

    @Test
    public void test_inheritance_precedence() throws Exception {
        if (!session.nodeExists("/hst:hst/hst:configurations/unittestcommon/hst:workspace")) {
            session.getNode("/hst:hst/hst:configurations/unittestcommon").addNode(HstNodeTypes.NODENAME_HST_WORKSPACE);
        }

        // hst:pages both below unittestcommon AND below unittestcommon/hst:workspace
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestcommon/hst:pages", "/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:pages");

        // both have hst:pages/homepage : Depending on precedence of the hst:inheritsfrom, one of them is merged into the 'unittestproject' model
        final Node homePageNode = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/homepage");
        homePageNode.setProperty(GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"location"});
        homePageNode.setProperty(GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"non-workspace"});
        final Node workspaceHomePageNode = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:pages/homepage");
        workspaceHomePageNode.setProperty(GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"location"});
        workspaceHomePageNode.setProperty(GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"workspace"});

        // add an extra component to workspace
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:pages/homepage",
                "/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:pages/homepageAgain");


        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                new String[]{"../unittestcommon", "../unittestcommon/hst:workspace"});
        saveSession();

        {
            VirtualHosts vhosts = hstManager.getVirtualHosts();
            final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
            final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
            assertNotNull(pageComponent);
            // since we first inherit ../unittestcommon and *then* ../unittestcommon/hst:workspace, we expect the homepage from
            // unittestcommon/hst:pages and not from unittestcommon/hst:workspace/hst:pages
            assertEquals("non-workspace", pageComponent.getParameter("location"));

            // assert that 'homepageAgain' which is only present below 'unittestcommon/hst:workspace/hst:pages' is inherited still
            final HstComponentConfiguration pageAgainComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepageAgain");
            assertNotNull(pageAgainComponent);
        }

        // switch the inheritance
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                new String[]{"../unittestcommon/hst:workspace", "../unittestcommon"});

        final HstModelProvider provider = HstServices.getComponentManager().getComponent(HstModelProvider.class);
        final EventPathsInvalidator invalidator = ((PlatformHstModel) provider.getHstModel()).getEventPathsInvalidator();
        saveSession();
        invalidator.eventPaths(new String[] {"/hst:hst/hst:configurations/unittestproject"});

        {
            VirtualHosts vhosts = hstManager.getVirtualHosts();
            final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
            final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
            assertNotNull(pageComponent);
            // since we first inherit ../unittestcommon and *then* ../unittestcommon/hst:workspace, we expect the homepage from
            // unittestcommon/hst:pages and not from unittestcommon/hst:workspace/hst:pages
            assertEquals("workspace", pageComponent.getParameter("location"));
        }
    }

    private void setWorkspaceInheritance(final String hstConfigurationPath, final String[] inheritsFrom) throws RepositoryException {
        final Node hstConfigNode = session.getNode(hstConfigurationPath);
        hstConfigNode.setProperty(GENERAL_PROPERTY_INHERITS_FROM, inheritsFrom);
        saveSession();
    }

    private String getLocalhostRootMountId() throws RepositoryException {
        return session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root").getIdentifier();
    }


    @Test
    public void test_marked_deleted_hstconfiguration_nodes_are_present_in_model()  throws Exception {
        Node homePage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/homepage");
        homePage.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        homePage.setProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE, "deleted");
        final Node standardBody = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/standardoverview/body");
        standardBody.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        standardBody.setProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE, "deleted");
        saveSession();

        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        assertNotNull(hstSite.getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage"));
        assertNotNull(hstSite.getComponentsConfiguration().getComponentConfiguration("hst:pages/standardoverview/body"));
        assertNotNull(hstSite.getComponentsConfiguration().getComponentConfiguration("hst:pages/standardoverview").getChildByName("body"));
        assertTrue(hstSite.getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage").isMarkedDeleted());
        assertTrue(hstSite.getComponentsConfiguration().getComponentConfiguration("hst:pages/standardoverview/body").isMarkedDeleted());
        assertFalse(hstSite.getComponentsConfiguration().getComponentConfiguration("hst:pages/standardoverview").isMarkedDeleted());

    }

    private void saveSession() throws RepositoryException {
        session.save();
        //TODO SS: Clarify what could be the cause of failures without delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {}
    }
}
