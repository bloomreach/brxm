/*
 * Copyright 2013-2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.platform.configuration;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.ConstraintViolationException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.EventPathsInvalidator;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ContainerComponentConfigurationsIT extends AbstractTestConfigurations {

    private static final String TEST_COMPONENT_NODE_NAME = "test";
    private HstManager hstSitesManager;
    private Session session;
    private Node testComponent;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.hstSitesManager = getComponent(HstManager.class.getName());
        this.session = createSession();
        createHstConfigBackup(session);
        movePagesInheritedPagesToProject(session);
        testComponent = addTestComponent();
        session.save();
    }

    private void movePagesInheritedPagesToProject(final Session session) throws RepositoryException {
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:pages",
                "/hst:hst/hst:configurations/unittestproject/hst:pages");
    }

    private Node addTestComponent() throws RepositoryException {
        Node homePageComponent = getHomePageComponentNode();
        Node testComponent = homePageComponent.addNode("test", HstNodeTypes.NODETYPE_HST_COMPONENT);
        return testComponent;
    }

    public Node getHomePageComponentNode() throws RepositoryException {
        return session.getNode("/hst:hst/hst:configurations/unittestproject/hst:pages/homepage");
    }


    @Override
    @After
    public void tearDown() throws Exception {
        restoreHstConfigBackup(session);
        session.logout();
        super.tearDown();
    }


    @Test
    public void testCorrectContainerComponent() throws Exception {
        final String containerName = "canonicalContainer";
        add_non_workspace_referencing_correct_container(testComponent, containerName);
        VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
        final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
        final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
        final HstComponentConfiguration testComponent = pageComponent.getChildByName(TEST_COMPONENT_NODE_NAME);
        assertNotNull(testComponent);
        final HstComponentConfiguration canonicalContainer = testComponent.getChildByName("canonicalContainer");
        assertNotNull(canonicalContainer);
        assertNotNull(canonicalContainer.getChildByName("item"));
    }

    private void add_non_workspace_referencing_correct_container(final Node parent, final String containerName) throws Exception {
        /*
           Add to test component a container:
             + containerName [hst:containercomponent]
                 - hst:xtype =  HST.vBox
                 + item [hst:containeritemcomponent]
                     - hst:xtype = HST.Item
       */
        Node canonicalContainer = parent.addNode(containerName, HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
        canonicalContainer.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.vBox");
        Node canonicalComponentItem = canonicalContainer.addNode("item", HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        canonicalComponentItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.Item");
        session.save();
    }

    @Test(expected = ConstraintViolationException.class)
    public void reference_component_property_not_allowed_on_container() throws Exception {
        final String containerName = "canonicalContainer";
        addIllegalHomePageContainer(testComponent, containerName);
    }


    private void addIllegalHomePageContainer(final Node parent, final String containerName) throws Exception {
        /*
           try to add to test component a not allowed container:
             + containerName [hst:containercomponent]
                 - hst:xtype =  HST.vBox
                 // reference component is illegal for hst:containercomponent
                 - hst:referencecomponent = hst:components/overview
       */
        Node canonicalContainer = parent.addNode(containerName, HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
        canonicalContainer.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.vBox");
        canonicalContainer.setProperty(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT, "hst:pages/not_allowed_for_container");
    }

    @Test(expected = ConstraintViolationException.class)
    public void reference_component_property_mandatory_on_containercomponentreference() throws Exception {
        testComponent.addNode("containerRef", HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENTREFERENCE);
        session.save();
    }

    @Test
    public void reference_component_not_in_model_when_missing_workspace() throws Exception {
        final String containerReference = "canonicalContainerComponentReference";
        addComponentReference(testComponent, containerReference, "someReference");
        VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
        final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
        final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
        final HstComponentConfiguration testComponent = pageComponent.getChildByName(TEST_COMPONENT_NODE_NAME);
        assertNotNull(testComponent);
        // since the 'canonicalContainerComponentReference' does not have an existing referenced component it should
        // be completely removed from hstcomponentconfiguration

        assertNull(testComponent.getChildByName("canonicalContainerComponentReference"));
    }

    private void addComponentReference(final Node parent, final String containerReferenceNodeName, String reference) throws RepositoryException {
        /*
         try to add to test component a non existing containerComponentReference:
           + containerName [hst:containercomponent]
               - hst:xtype =  HST.vBox
               - hst:referencecomponent = reference
        */
        Node canonicalContainerReference = parent.addNode(containerReferenceNodeName, HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENTREFERENCE);
        canonicalContainerReference.setProperty(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT, reference);
        session.save();
    }

    @Test
    public void container_component_not_part_of_model_for_non_existing_reference() throws Exception {

        createHstWorkspaceAndReferenceableContainer("dummyContainer", "/hst:hst/hst:configurations/unittestproject");
        final String containerReference = "canonicalContainerComponentReference";
        addComponentReference(testComponent, containerReference, "folderA/containerNonExisting");
        VirtualHosts vhosts = hstSitesManager.getVirtualHosts();

        final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
        final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
        final HstComponentConfiguration testComponent = pageComponent.getChildByName(TEST_COMPONENT_NODE_NAME);
        assertNotNull(testComponent);
        // since the 'canonicalContainerComponentReference' does not have an existing referenced component it should
        // be completely removed from hstcomponentconfiguration

        assertNull(testComponent.getChildByName("canonicalContainerComponentReference"));

    }

    @Test
    public void container_component_referenceName_is_used_instead_of_referenced_name() throws Exception {

        createHstWorkspaceAndReferenceableContainer("myReferenceableContainer", "/hst:hst/hst:configurations/unittestproject");
        addComponentReference(testComponent, "containerReferencePreserveMyName", "myReferenceableContainer");
        VirtualHosts vhosts = hstSitesManager.getVirtualHosts();

        final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
        final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
        final HstComponentConfiguration testComponent = pageComponent.getChildByName(TEST_COMPONENT_NODE_NAME);
        assertNotNull(testComponent);
        // the componentcontainerreference nodename is 'containerReferencePreserveMyName' : it points to
        // a container called 'myReferenceableContainer', so it should inherit everything from that except the name!
        final HstComponentConfiguration component = testComponent.getChildByName("containerReferencePreserveMyName");
        assertNotNull(component);

        assertTrue(component.getComponentType() == HstComponentConfiguration.Type.CONTAINER_COMPONENT);

        // the id is within the current unittestproject hst:pages, while the component is inherited
        // from hst:workspace below unittestcommon

        assertTrue(component.getId().equals("hst:pages/homepage/test/containerReferencePreserveMyName"));
        assertTrue(component.getCanonicalStoredLocation().equals("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:containers/myReferenceableContainer"));
        String canonId = component.getCanonicalIdentifier();
        Node canonicalNode = session.getNodeByIdentifier(canonId);
        assertTrue(canonicalNode.getPath().equals("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:containers/myReferenceableContainer"));
        assertTrue(component.getChildren().size() == 1);
        final HstComponentConfiguration child = component.getChildByName("item");
        assertNotNull(child);
        assertTrue(child.getId().equals("hst:pages/homepage/test/containerReferencePreserveMyName/item"));
        assertTrue(child.getCanonicalStoredLocation().equals("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:containers/myReferenceableContainer/item"));
        String canonChildId = child.getCanonicalIdentifier();
        Node canonChildNode = session.getNodeByIdentifier(canonChildId);
        assertTrue(canonChildNode.getPath().equals("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:containers/myReferenceableContainer/item"));

    }

    @Test
    public void deeper_nested_referenced_container() throws Exception {

        createHstWorkspaceAndReferenceableContainer("foo/bar/myReferenceableContainer", "/hst:hst/hst:configurations/unittestproject");
        addComponentReference(testComponent, "containerReferencePreserveMyName", "foo/bar/myReferenceableContainer");
        VirtualHosts vhosts = hstSitesManager.getVirtualHosts();

        final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
        final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
        final HstComponentConfiguration testHstConfigComponent = pageComponent.getChildByName(TEST_COMPONENT_NODE_NAME);
        assertNotNull(testHstConfigComponent);
        final HstComponentConfiguration component = testHstConfigComponent.getChildByName("containerReferencePreserveMyName");
        assertNotNull(component);

        assertTrue(component.getComponentType() == HstComponentConfiguration.Type.CONTAINER_COMPONENT);

        // the id is within the current unittestproject hst:pages, while the component is inherited
        // from hst:modifiable below unittestcommon

        assertTrue(component.getId().equals("hst:pages/homepage/test/containerReferencePreserveMyName"));
        assertTrue(component.getCanonicalStoredLocation().equals("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:containers/foo/bar/myReferenceableContainer"));
        String canonId = component.getCanonicalIdentifier();
        Node canonicalNode = session.getNodeByIdentifier(canonId);
        assertTrue(canonicalNode.getPath().equals("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:containers/foo/bar/myReferenceableContainer"));
        assertTrue(component.getChildren().size() == 1);
        final HstComponentConfiguration child = component.getChildByName("item");
        assertNotNull(child);
        assertTrue(child.getId().equals("hst:pages/homepage/test/containerReferencePreserveMyName/item"));
        assertTrue(child.getCanonicalStoredLocation().equals("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:containers/foo/bar/myReferenceableContainer/item"));
        String canonChildId = child.getCanonicalIdentifier();
        Node canonChildNode = session.getNodeByIdentifier(canonChildId);
        assertTrue(canonChildNode.getPath().equals("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:containers/foo/bar/myReferenceableContainer/item"));

    }

    @Test
    public void referenced_container_triggers_model_reload_on_change() throws Exception {
        {
            // start directly with loading hst model first:
            final VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
            final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
            final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
            final HstComponentConfiguration testHstConfigComponent = pageComponent.getChildByName(TEST_COMPONENT_NODE_NAME);
            assertNotNull(testHstConfigComponent);
            final HstComponentConfiguration component = testHstConfigComponent.getChildByName("containerReferencePreserveMyName");
            assertNull(component);
        }
        // add new config nodes
        createHstWorkspaceAndReferenceableContainer("foo/bar/myReferenceableContainer", "/hst:hst/hst:configurations/unittestproject");
        addComponentReference(testComponent, "containerReferencePreserveMyName", "foo/bar/myReferenceableContainer");

        // trigger events as during tests the jcr event listeners are not enabled
        EventPathsInvalidator invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());
        invalidator.eventPaths("/hst:hst/hst:configurations/unittestproject/" + HstNodeTypes.NODENAME_HST_WORKSPACE, testComponent.getPath());

        {
            // reload model after changes
            final VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
            Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
            final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
            final HstComponentConfiguration testHstConfigComponent = pageComponent.getChildByName(TEST_COMPONENT_NODE_NAME);
            assertNotNull(testHstConfigComponent);
            HstComponentConfiguration component = testHstConfigComponent.getChildByName("containerReferencePreserveMyName");
            assertNotNull(component);
            assertTrue(component.getChildren().size() == 1);
            HstComponentConfiguration child = component.getChildByName("item");
            assertNotNull(child);
            assertEquals(child.getParameter("name1"), "value1");

            // add a extra parameter to child component
            String canonicalJcrPath = child.getCanonicalStoredLocation();
            Node componentNode = session.getNode(canonicalJcrPath);

            componentNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"name1", "name2"});
            componentNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"value1", "value2"});
            session.save();

            // trigger reload
            invalidator.eventPaths(canonicalJcrPath);
            mount = hstSitesManager.getVirtualHosts().getMountByIdentifier(getLocalhostRootMountId());
            component = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage").
                    getChildByName(TEST_COMPONENT_NODE_NAME).getChildByName("containerReferencePreserveMyName");
            child = component.getChildByName("item");
            assertNotNull(child);
            assertEquals(child.getParameter("name1"), "value1");
            assertEquals(child.getParameter("name2"), "value2");

        }

    }

    @Test
    public void referenceable_containers_from_inherited_configuration_not_included() throws Exception {
        // below add a container to hst:workspace in 'unittestcommon' : this workspace is invisible for
        // 'unittestproject' as workspace nodes are not IMPLICITLY inherited UNLESS hst:inheritsfrom EXPLICITLY
        // includes ../xyz/hst:workspace
        createHstWorkspaceAndReferenceableContainer("myReferenceableContainer",
                "/hst:hst/hst:configurations/unittestcommon");
        final String inheritedContainerName = "inheritedcontainer";
        addComponentReference(testComponent, inheritedContainerName, "myReferenceableContainer");
        {
            final VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
            final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
            final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
            final HstComponentConfiguration testComponent = pageComponent.getChildByName(TEST_COMPONENT_NODE_NAME);
            assertNotNull(testComponent);
            // container NOT available since part of inherited
            final HstComponentConfiguration component = testComponent.getChildByName(inheritedContainerName);
            assertNull(component);
        }

        createHstWorkspaceAndReferenceableContainer("myReferenceableContainer",
                "/hst:hst/hst:configurations/unittestproject");
        addComponentReference(testComponent, "localcontainer", "myReferenceableContainer");

        // trigger events as during tests the jcr event listeners are not enabled
        EventPathsInvalidator invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());
        invalidator.eventPaths("/hst:hst/hst:configurations/unittestproject/" + HstNodeTypes.NODENAME_HST_WORKSPACE, testComponent.getPath());

        {
            final VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
            final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
            final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
            final HstComponentConfiguration testComponent = pageComponent.getChildByName(TEST_COMPONENT_NODE_NAME);
            assertNotNull(testComponent);
            // container available since part of inherited workspace (which by default is not inherited)
            final HstComponentConfiguration component = testComponent.getChildByName("localcontainer");
            assertNotNull(component);
        }

    }


    private void setWorkspaceInheritance(final String hstConfigurationPath, final String[] inheritsFrom) throws RepositoryException {
        final Node hstConfigNode = session.getNode(hstConfigurationPath);
        hstConfigNode.setProperty(GENERAL_PROPERTY_INHERITS_FROM, inheritsFrom);
        session.save();
    }

    @Test
    public void referenceable_containers_from_inherited_configuration_included_when_workspace_explicitly_inherited_and_invalidation_works() throws Exception {
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                new String[]{"../unittestcommon", "../unittestcommon/hst:workspace"});
        assertInheritanceAndModelReloadWorks(getLocalhostRootMountId(), true);
    }

    @Test
    public void referenceable_containers_from_inherited_configuration_included_when_only_workspace_inherited_and_invalidation_works() throws Exception {
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                new String[]{"../unittestcommon/hst:workspace"});
        assertInheritanceAndModelReloadWorks(getLocalhostRootMountId(), true);
    }

    @Test
    public void referenceable_containers_from_containers_inherited_configuration_included_when_workspace_explicitly_inherited_and_invalidation_works() throws Exception {
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                new String[]{"../unittestcommon", "../unittestcommon/hst:workspace/hst:containers"});
        assertInheritanceAndModelReloadWorks(getLocalhostRootMountId(), true);
    }

    @Test
    public void referenceable_containers_from_containers_inherited_configuration_included_when_only_workspace_inherited_and_invalidation_works() throws Exception {
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                new String[]{"../unittestcommon/hst:workspace/hst:containers"});
        assertInheritanceAndModelReloadWorks(getLocalhostRootMountId(), true);
    }


    private void removePagesAndSiteMapFromSubConfig() throws RepositoryException {
        // from unittestproject remove the 'pages' and 'sitemap' it has itself
        session.getNode("/hst:hst/hst:configurations/unittestsubproject/hst:pages").remove();
        session.getNode("/hst:hst/hst:configurations/unittestsubproject/hst:sitemap").remove();
        session.save();
    }


    @Test
    public void cascading_workspace_inheritance() throws Exception {
        removePagesAndSiteMapFromSubConfig();
        /**
         * set up config such that 'unittestproject' inherits from 'unittestcommon' and '../unittestcommon/hst:workspace'
         * hence 'unittestsubproject' should get to inherit all config from 'unittestproject' and 'unittestcommon' and '../unittestcommon/hst:workspace'
         */
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                new String[]{"../unittestcommon", "../unittestcommon/hst:workspace"});
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestsubproject",
                new String[]{"../unittestproject"});
        assertInheritanceAndModelReloadWorks(getLocalhostSubProjectMountId(), true);
    }

    @Test
    public void cascading_workspace_inheritance2() throws Exception {
        removePagesAndSiteMapFromSubConfig();
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                new String[]{"../unittestcommon", "../unittestcommon/hst:workspace/hst:containers"});
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestsubproject",
                new String[]{"../unittestproject"});
        assertInheritanceAndModelReloadWorks(getLocalhostSubProjectMountId(), true);
    }

    @Test
    public void cascading_workspace_inheritance3() throws Exception {
        removePagesAndSiteMapFromSubConfig();
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                new String[]{"../unittestcommon/hst:workspace/hst:containers"});
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestsubproject",
                new String[]{"../unittestproject"});

        assertInheritanceAndModelReloadWorks(getLocalhostSubProjectMountId(), true);
    }

    public void cascading_workspace_inheritance_order_by_first_inheritance_first() throws Exception {
        removePagesAndSiteMapFromSubConfig();
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                new String[]{"../unittestcommon","../unittestcommon/hst:workspace"});
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestsubproject",
                new String[]{"../unittestproject","../unittestproject/hst:workspace"});

        // we add both to unittestproject and unittestcommon the same 'myReferenceableContainer' in the hst:workspace
        createHstWorkspaceAndReferenceableContainer("myReferenceableContainer",
                "/hst:hst/hst:configurations/unittestproject");

        createHstWorkspaceAndReferenceableContainer("myReferenceableContainer",
                "/hst:hst/hst:configurations/unittestcommon");

        final String inheritedContainerName = "inheritedcontainer";
        addComponentReference(testComponent, inheritedContainerName, "myReferenceableContainer");

        final VirtualHosts vhosts = hstSitesManager.getVirtualHosts();

        {
            /**
             * For subsite, we expect that the '../unittestcommon/hst:workspace' has precedence over '../unittestproject/hst:workspace'
             * because first all inheritance for '../unittestproject' is resolved, but ../unittestproject inherits already the
             * '../unittestcommon/hst:workspace'. After that, the '../unittestproject/hst:workspace' is inherited
             */
            final Mount mount = vhosts.getMountByIdentifier(getLocalhostSubProjectMountId());
            final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
            final HstComponentConfiguration testComponent = pageComponent.getChildByName(TEST_COMPONENT_NODE_NAME);
            assertNotNull(testComponent);
            // container available since part of inherited BUT inherited workspace IS now inherited because explicitly inherited
            final HstComponentConfiguration component = testComponent.getChildByName(inheritedContainerName);
            assertNotNull(component);
            final HstComponentConfiguration item = component.getChildByName("item");
            assertEquals("/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:containers/myReferenceableContainer/item",
                    item.getCanonicalStoredLocation());
        }

        {
            final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
            final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
            final HstComponentConfiguration testComponent = pageComponent.getChildByName(TEST_COMPONENT_NODE_NAME);
            assertNotNull(testComponent);
            // container available since part of inherited BUT inherited workspace IS now inherited because explicitly inherited
            final HstComponentConfiguration component = testComponent.getChildByName(inheritedContainerName);
            assertNotNull(component);
            final HstComponentConfiguration item = component.getChildByName("item");
            assertEquals("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:containers/myReferenceableContainer/item",
                    item.getCanonicalStoredLocation());
        }

        // now revert the inheritance for 'unittestsubproject' from {"../unittestproject","../unittestproject/hst:workspace"} to
        // {"../unittestproject/hst:workspace", ../unittestproject"} . Then, we expect the 'hst:workspace/hst:containers/myReferenceableContainer/item'
        // in 'unittestsubproject' to be from 'unittestproject' instead of from 'unittestcommon'
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestsubproject",
                new String[]{"../unittestproject/hst:workspace", "../unittestproject"});
        EventPathsInvalidator invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());
        invalidator.eventPaths("/hst:hst/hst:configurations/unittestsubproject");
        final VirtualHosts vhostsNew = hstSitesManager.getVirtualHosts();
        {
            final Mount mount = vhostsNew.getMountByIdentifier(getLocalhostSubProjectMountId());
            final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
            final HstComponentConfiguration testComponent = pageComponent.getChildByName(TEST_COMPONENT_NODE_NAME);
            assertNotNull(testComponent);
            // container available since part of inherited BUT inherited workspace IS now inherited because explicitly inherited
            final HstComponentConfiguration component = testComponent.getChildByName(inheritedContainerName);
            assertNotNull(component);
            final HstComponentConfiguration item = component.getChildByName("item");
            assertEquals("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:containers/myReferenceableContainer/item",
                    item.getCanonicalStoredLocation());
        }
    }


    private void assertInheritanceAndModelReloadWorks(final String mountId, final boolean expectPresentPagesFromCommon) throws Exception {

        createHstWorkspaceAndReferenceableContainer("myReferenceableContainer",
                "/hst:hst/hst:configurations/unittestcommon");

        final String inheritedContainerName = "inheritedcontainer";
        addComponentReference(testComponent, inheritedContainerName, "myReferenceableContainer");
        final String itemUUID;
        {
            final VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
            final Mount mount = vhosts.getMountByIdentifier(mountId);
            final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
            if (!expectPresentPagesFromCommon) {
                assertNull(pageComponent);
                return;
            }
            final HstComponentConfiguration testComponentConfig = pageComponent.getChildByName(TEST_COMPONENT_NODE_NAME);
            assertNotNull(testComponentConfig);
            // container available since part of inherited BUT inherited workspace IS now inherited because explicitly inherited
            final HstComponentConfiguration component = testComponentConfig.getChildByName(inheritedContainerName);
            assertNotNull(component);
            final HstComponentConfiguration item = component.getChildByName("item");
            itemUUID = item.getCanonicalIdentifier();
            assertNotNull(item);
            assertEquals("value1", item.getParameter("name1"));
            assertTrue(item.isInherited());
        }

        // now trigger a change in the inherited component from workspace. This should trigger a reload
        final Node componentItem = session.getNodeByIdentifier(itemUUID);
        componentItem.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"valueNew"});
        session.save();
        // trigger events as during tests the jcr event listeners are not enabled
        EventPathsInvalidator invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());
        invalidator.eventPaths(componentItem.getPath());
        {
            final VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
            final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
            final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
            final HstComponentConfiguration testComponent = pageComponent.getChildByName(TEST_COMPONENT_NODE_NAME);
            assertNotNull(testComponent);
            final HstComponentConfiguration component = testComponent.getChildByName(inheritedContainerName);
            assertNotNull(component);
            final HstComponentConfiguration item = component.getChildByName("item");
            assertNotNull(item);
            // a change in inherited workspace node should trigger a reload of the model
            assertEquals("valueNew", item.getParameter("name1"));
            assertTrue(item.isInherited());
        }

    }

    @Test
    public void cascading_workspace_inheritance4() throws Exception {

        removePagesAndSiteMapFromSubConfig();

        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                new String[]{"../unittestcommon", "../unittestcommon/hst:workspace/hst:containers", "../unittestcommon/hst:workspace/hst:pages", "../unittestcommon/hst:workspace/hst:sitemap"});
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestsubproject",
                new String[]{"../unittestproject/hst:workspace/hst:containers", "../unittestproject/hst:workspace/hst:pages", "../unittestproject/hst:workspace/hst:sitemap"});

        final Node workspace = session.getNode("/hst:hst/hst:configurations/unittestcommon").addNode("hst:workspace");
        workspace.addNode("hst:pages");
        session.move(testComponent.getParent().getPath(), "/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:pages/homepage");
        session.save();
        assertInheritanceAndModelReloadWorks(getLocalhostSubProjectMountId(), true);

    }

    @Test
    public void cascading_workspace_inheritance5() throws Exception {
        removePagesAndSiteMapFromSubConfig();
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                new String[]{"../unittestcommon/hst:workspace/hst:containers", "../unittestcommon/hst:workspace/hst:pages", "../unittestcommon/hst:workspace/hst:sitemap"});
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestsubproject",
                new String[]{"../unittestproject/hst:workspace/hst:containers", "../unittestproject/hst:workspace/hst:pages", "../unittestproject/hst:workspace/hst:sitemap"});

        // move homepage since only the workspace pages are inherited
        final Node workspace = session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace");
        workspace.addNode("hst:pages");
        session.move(testComponent.getParent().getPath(), "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/homepage");
        assertInheritanceAndModelReloadWorks(getLocalhostSubProjectMountId(), true);
    }

    @Test
    public void cascading_workspace_inheritance_when_unittestproject_does_not_inherit_pages_from_common_workspace() throws Exception {
        removePagesAndSiteMapFromSubConfig();
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                new String[]{"../unittestcommon/hst:workspace/hst:containers", "../unittestcommon/hst:workspace/hst:sitemap"});
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestsubproject",
                new String[]{"../unittestproject/hst:workspace/hst:containers", "../unittestproject/hst:workspace/hst:sitemap"});

        final Node workspace = session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace");
        workspace.addNode("hst:pages");
        session.move(testComponent.getParent().getPath(), "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/homepage");
        assertInheritanceAndModelReloadWorks(getLocalhostSubProjectMountId(), false);
    }

    @Test
    public void cascading_workspace_inheritance_unknown_inherit_paths() throws Exception {
        removePagesAndSiteMapFromSubConfig();
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                new String[]{"../unittestcommon/hst:workspace/hst:foo", "../unittestcommon/hst:workspace/hst:bar", "../unittestcommon/hst:workspace/hst:lux"});
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestsubproject",
                new String[]{"../unittestproject/hst:workspace/hst:foo", "../unittestproject/hst:workspace/hst:bar", "../unittestproject/hst:workspace/hst:lux"});

        final Node workspace = session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace");
        workspace.addNode("hst:pages");
        session.move(testComponent.getParent().getPath(), "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/homepage");
        assertInheritanceAndModelReloadWorks(getLocalhostSubProjectMountId(), false);
    }

    @Test
    public void test_illegal_reference_of_to_deeply_nested_container() throws Exception {
        setWorkspaceInheritance("/hst:hst/hst:configurations/unittestproject",
                new String[]{"../unittestcommon/hst:workspace/hst:containers/myReferenceableContainer"});
        createHstWorkspaceAndReferenceableContainer("myReferenceableContainer",
                "/hst:hst/hst:configurations/unittestcommon");

        final String inheritedContainerName = "inheritedcontainer";
        addComponentReference(testComponent, inheritedContainerName, "myReferenceableContainer");
        {
            final VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
            final Mount mount = vhosts.getMountByIdentifier(getLocalhostRootMountId());
            final HstComponentConfiguration pageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
            final HstComponentConfiguration testComponent = pageComponent.getChildByName(TEST_COMPONENT_NODE_NAME);
            assertNotNull(testComponent);
            // container available since part of inherited BUT inherited workspace IS now inherited because explicitly inherited
            final HstComponentConfiguration component = testComponent.getChildByName(inheritedContainerName);
            assertNull(component);
        }
    }

    /**
     * @return the highest ancestor path of newly created nodes: This is the node that needs to be cleanup at the end
     * again
     */
    private String createHstWorkspaceAndReferenceableContainer(final String containerRelPath,
                                                               final String projectPath) throws RepositoryException {
        String highestAncestorPath = null;
        final Node hstConfigurationNode = session.getNode(projectPath);
        Node modifiableHstNode;
        if (hstConfigurationNode.hasNode(HstNodeTypes.NODENAME_HST_WORKSPACE)) {
            modifiableHstNode = hstConfigurationNode.getNode(HstNodeTypes.NODENAME_HST_WORKSPACE);
        } else {
            modifiableHstNode = hstConfigurationNode.addNode(HstNodeTypes.NODENAME_HST_WORKSPACE);
            highestAncestorPath = modifiableHstNode.getPath();
        }
        Node hstReferenceableContainers;
        if (modifiableHstNode.hasNode(HstNodeTypes.NODENAME_HST_CONTAINERS)) {
            hstReferenceableContainers = modifiableHstNode.getNode(HstNodeTypes.NODENAME_HST_CONTAINERS);
        } else {
            hstReferenceableContainers = modifiableHstNode.addNode(HstNodeTypes.NODENAME_HST_CONTAINERS);
            if (highestAncestorPath == null) {
                highestAncestorPath = hstReferenceableContainers.getPath();
            }
        }
        Node folder = hstReferenceableContainers;
        final String[] elems = containerRelPath.split("/");
        if (elems.length > 1) {
            // first create folders
            for (int i = 0; i < (elems.length - 1); i++) {
                if (folder.hasNode(elems[i])) {
                    folder = folder.getNode(elems[i]);
                } else {
                    folder = folder.addNode(elems[i], HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENTSFOLDER);
                    if (highestAncestorPath == null) {
                        highestAncestorPath = folder.getPath();
                    }
                }
            }
        }
        final Node container = folder.addNode(elems[elems.length - 1], HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
        if (highestAncestorPath == null) {
            highestAncestorPath = container.getPath();
        }
        container.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.vBox");
        Node componentItem = container.addNode("item", HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        componentItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.Item");
        componentItem.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"name1"});
        componentItem.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"value1"});
        session.save();
        //hstSitesManager.invalidate();
        return highestAncestorPath;
    }

    public String getLocalhostRootMountId() throws RepositoryException {
        return session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root").getIdentifier();
    }
    public String getLocalhostSubProjectMountId() throws RepositoryException {
        return session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root/subsite").getIdentifier();
    }
}