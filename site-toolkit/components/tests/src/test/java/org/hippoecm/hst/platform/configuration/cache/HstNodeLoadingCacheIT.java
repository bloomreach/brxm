/*
 *  Copyright 2013-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.cache;


import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.observation.EventListener;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.provider.ValueProvider;
import org.hippoecm.hst.provider.jcr.JCRValueProviderImpl;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.hippoecm.hst.configuration.HstNodeTypes.BRANCH_PROPERTY_BRANCH_ID;
import static org.hippoecm.hst.configuration.HstNodeTypes.BRANCH_PROPERTY_BRANCH_OF;
import static org.hippoecm.hst.configuration.HstNodeTypes.CONFIGURATION_PROPERTY_LOCKED;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_BRANCH;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_PAGES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_SITEMAP;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_WORKSPACE;
import static org.junit.Assert.fail;

public class HstNodeLoadingCacheIT extends AbstractHstLoadingCacheTestCase {


    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testHstNodeLoadingCache() throws RepositoryException {

        assertNotNull(hstNodeLoadingCache.getNode("/hst:hst"));
        assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:hosts"));
        assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost"));
        assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root"));
        assertNotNull(hstNodeLoadingCache.getNode("/hst:hst").getNode("hst:hosts").getNode("dev-localhost/localhost/hst:root"));

        assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:configurations"));
        assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestcommon"));
        assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestcommon/hst:components"));
        assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestcommon/hst:components/header"));
        assertNotNull(hstNodeLoadingCache.getNode("/hst:hst").getNode("hst:configurations").getNode("unittestcommon/hst:components/header"));

        assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:blueprints"));
        assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:blueprints/testblueprint"));
        assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:blueprints/testblueprint/hst:configuration"));
        assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:blueprints/testblueprint/hst:configuration/hst:sitemenus"));
        assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:blueprints/testblueprint/hst:configuration/hst:sitemenus/mainnavigation"));

        assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:sites"));
        assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:sites/unittestproject"));
        assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:sites/global"));


        final Session session = createSession();
        try {
            assertTrue(session.nodeExists("/hst:hst/hst:domains"));
            assertNull("Although node '/hst:hst/hst:domains' exists, it should never be loaded as " +
                    "HstNode objects since not part of the HST Model", hstNodeLoadingCache.getNode("/hst:hst/hst:domains"));
        } finally {
            session.logout();
        }
    }

    @Test
    public void testHstNodeValueProviderDoesNotContainProtectedProperties() throws RepositoryException {
        final HstNode hstHostsNode = hstNodeLoadingCache.getNode("/hst:hst/hst:hosts");
        Session session = createSession();
        final Node hostsNode = session.getNode("/hst:hst/hst:hosts");
        final ValueProvider lazyValueProviderWithAllProps = new JCRValueProviderImpl(hostsNode);
        final ValueProvider nonLazyValueProviderWithAllProps = new JCRValueProviderImpl(hostsNode, false);
        final ValueProvider nonLazyValueProviderWithoutProtectedProps = new JCRValueProviderImpl(hostsNode, false, true, false);
        boolean protectedPropCheckDone = false;
        boolean normalPropCheckDone = false;
        for (Property property : new PropertyIterable(hostsNode.getProperties())) {
            String propName = property.getName();
            final PropertyDefinition definition = property.getDefinition();
            if (definition.isProtected()) {
                if(PropertyType.STRING == property.getType() ||
                        PropertyType.BOOLEAN  == property.getType() ||
                        PropertyType.DATE  == property.getType() ||
                        PropertyType.DOUBLE  == property.getType() ||
                        PropertyType.LONG == property.getType()) {

                    protectedPropCheckDone = true;
                    assertFalse(hstHostsNode.getValueProvider().hasProperty(propName));
                    assertTrue(lazyValueProviderWithAllProps.hasProperty(propName));
                    assertTrue(nonLazyValueProviderWithAllProps.hasProperty(propName));
                    assertFalse(nonLazyValueProviderWithoutProtectedProps.hasProperty(propName));
                }

            } else {
                normalPropCheckDone = true;
                assertTrue(hstHostsNode.getValueProvider().hasProperty(propName));
                assertTrue(lazyValueProviderWithAllProps.hasProperty(propName));
                assertTrue(nonLazyValueProviderWithAllProps.hasProperty(propName));
                assertTrue(nonLazyValueProviderWithoutProtectedProps.hasProperty(propName));
            }
        }

        assertTrue(protectedPropCheckDone);
        assertTrue(normalPropCheckDone);

        session.logout();
    }

    @Test
    public void testHstNodeRemoval() throws Exception {
        try (CommonHstConfigSetup setup = new CommonHstConfigSetup()) {
            // load model cache first
            hstNodeLoadingCache.getNode("/hst:hst");
            setup.session.getNode("/hst:hst/hst:hosts/dev-localhost").remove();
            setup.session.getNode("/hst:hst/hst:blueprints/testblueprint/hst:configuration/hst:sitemenus").remove();
            setup.session.getNode("/hst:hst/hst:blueprints/testblueprint").remove();

            saveAndInvalidate(setup.session);


            assertNull(hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost"));
            assertNull(hstNodeLoadingCache.getNode("/hst:hst/hst:blueprints/testblueprint/hst:configuration/hst:sitemenus"));
            assertNull(hstNodeLoadingCache.getNode("/hst:hst/hst:blueprints/testblueprint"));

            assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:hosts"));
            assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:blueprints"));
        }
    }

    @Test
    public void test_node_modification_and_child_removal() throws Exception {
        try (CommonHstConfigSetup setup = new CommonHstConfigSetup()) {
            // load model cache first
            hstNodeLoadingCache.getNode("/hst:hst");
            setup.session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemenus").remove();
            setup.session.getNode("/hst:hst/hst:configurations/unittestproject")
                    .setProperty("hst:inheritsfrom",new String[]{"../common"});

            saveAndInvalidate(setup.session);


            try {
                hstNodeLoadingCache.getNode("/hst:hst");
            } catch (ConcurrentModificationException e) {
                fail("HSTTWO-2968 should had solved possible java.util.ConcurrentModificationException");
            }
        }
    }

    @Test
    public void testHstPropertyChange() throws Exception {

        try (CommonHstConfigSetup setup = new CommonHstConfigSetup()) {
            // load model cache first
            hstNodeLoadingCache.getNode("/hst:hst");

            HstNode before1 = hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost/localhost");
            HstNode before2 = hstNodeLoadingCache.getNode("/hst:hst/hst:blueprints/testblueprint/hst:configuration/hst:sitemenus");


            final String schemeBefore = before1.getValueProvider().getString(HstNodeTypes.VIRTUALHOST_PROPERTY_SCHEME);
            final String lockedBefore = before2.getValueProvider().getString(GENERAL_PROPERTY_LOCKED_BY);

            Session session = setup.session;
            session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost").setProperty(HstNodeTypes.VIRTUALHOST_PROPERTY_SCHEME, "XXX");
            session.getNode("/hst:hst/hst:blueprints/testblueprint/hst:configuration/hst:sitemenus").setProperty(GENERAL_PROPERTY_LOCKED_BY, "YYY");

            saveAndInvalidate(session);



            final HstNode after1 = hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost/localhost");
            final HstNode after2 = hstNodeLoadingCache.getNode("/hst:hst/hst:blueprints/testblueprint/hst:configuration/hst:sitemenus");

            assertNotNull(after1);
            assertNotNull(after2);
            assertTrue(before1 == after1);
            assertTrue(before2 == after2);

            final String schemeAfter = after1.getValueProvider().getString(HstNodeTypes.VIRTUALHOST_PROPERTY_SCHEME);

            assertFalse(schemeAfter.equals(schemeBefore));

            final String lockedAfter = after2.getValueProvider().getString(GENERAL_PROPERTY_LOCKED_BY);

            assertFalse(lockedAfter.equals(lockedBefore));
        }
    }

    @Test
    public void testHstNodeAdd() throws Exception {
        try (CommonHstConfigSetup setup = new CommonHstConfigSetup()) {
            // load model cache first
            hstNodeLoadingCache.getNode("/hst:hst");
            final Session session = setup.session;

            HstNode hostsBefore = hstNodeLoadingCache.getNode("/hst:hst/hst:hosts");
            HstNode hostGroupBefore = hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost");
            HstNode hostBefore = hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost/localhost");

            assertTrue(hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost").getNodes().size() == 1);
            JcrUtils.copy(session, "/hst:hst/hst:hosts/dev-localhost/localhost",
                                   "/hst:hst/hst:hosts/dev-localhost/localhost-copy");

            saveAndInvalidate(session);


            assertTrue(hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost").getNodes().size() == 2);
            assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost/localhost-copy").getNodes().size() == 2);

            // assert that the previously loaded HstNodes are the same instances (efficient reloading possible since
            // child order does not matter)

            HstNode hostsAfter = hstNodeLoadingCache.getNode("/hst:hst/hst:hosts");
            HstNode hostGroupAfter = hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost");
            HstNode hostAfter = hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost/localhost");

            assertTrue(hostsAfter == hostsBefore);
            assertTrue(hostGroupAfter == hostGroupBefore);
            assertTrue(hostAfter == hostBefore);

        }
    }


    @Test
    public void testHstNodeAddAndDirectRemove() throws Exception {
        try (CommonHstConfigSetup setup = new CommonHstConfigSetup()) {
            hstNodeLoadingCache.getNode("/hst:hst");
            final Session session = setup.session;

            assertTrue(hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost").getNodes().size() == 1);
            JcrUtils.copy(session, "/hst:hst/hst:hosts/dev-localhost/localhost",
                    "/hst:hst/hst:hosts/dev-localhost/localhost-copy");

            saveAndInvalidate(session);

            session.removeItem("/hst:hst/hst:hosts/dev-localhost/localhost-copy");

            saveAndInvalidate(session);


            assertTrue(hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost").getNodes().size() == 1);
        }
    }

    @Test
    public void testHstNodeRemoveAndDirectAdd() throws Exception {
        try (CommonHstConfigSetup setup = new CommonHstConfigSetup()) {
            hstNodeLoadingCache.getNode("/hst:hst");
            final Session session = setup.session;

            assertTrue(hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost").getNodes().size() == 1);

            session.removeItem("/hst:hst/hst:hosts/dev-localhost");
            saveAndInvalidate(session);
            //restore
            JcrUtils.copy(session, "/hst-backup/hst:hosts/dev-localhost",
                    "/hst:hst/hst:hosts/dev-localhost");

            saveAndInvalidate(session);


            assertTrue(hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost").getNodes().size() == 1);
            assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root"));
        }
    }

    @Test
    public void testHstNodeAddAndDirectParentRemove() throws Exception {
        try (CommonHstConfigSetup setup = new CommonHstConfigSetup()) {
            hstNodeLoadingCache.getNode("/hst:hst");
            final Session session = setup.session;

            assertTrue(hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost").getNodes().size() == 1);

            JcrUtils.copy(session, "/hst:hst/hst:hosts/dev-localhost/localhost",
                    "/hst:hst/hst:hosts/dev-localhost/localhost2");

            saveAndInvalidate(session);
            session.removeItem("/hst:hst/hst:hosts/dev-localhost");
            saveAndInvalidate(session);

            assertNull("/hst:hst/hst:hosts/dev-localhost has been removed, expect null ",
                    hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost"));
        }
    }

    @Test
    public void testHstNodeAddAndDirectAncestorRemove() throws Exception {
        try (CommonHstConfigSetup setup = new CommonHstConfigSetup()) {
            hstNodeLoadingCache.getNode("/hst:hst");
            final Session session = setup.session;

            assertTrue(hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost").getNodes().size() == 1);

            JcrUtils.copy(session, "/hst:hst/hst:hosts/dev-localhost/localhost/8081",
                    "/hst:hst/hst:hosts/dev-localhost/localhost/8082");

            saveAndInvalidate(session);
            session.removeItem("/hst:hst/hst:hosts/dev-localhost");
            saveAndInvalidate(session);

            assertNull("/hst:hst/hst:hosts/dev-localhost has been removed, expect null ",
                    hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost"));
        }
    }

    /**
     * nodes directly below /hst:hst/hst:configurations get special treatment : They do not trigger an ancestor
     * full reload if a node is added / removed there. Order does not matter, and reloading ALL hst:configuration nodes
     * is potentially extremely expensive because there can be so many. Hence, special tests for these nodes.
     */
    @Test
    public void testHstConfigurationAddRemoveDoesNotReloadSibling() throws Exception {
        try (CommonHstConfigSetup setup = new CommonHstConfigSetup()) {
            final HstNode hstConfigurationsNode = hstNodeLoadingCache.getNode("/hst:hst/hst:configurations");

            assertNotNull(hstConfigurationsNode.getNode("hst:default"));
            assertNotNull(hstConfigurationsNode.getNode("unittestcommon"));
            assertNotNull(hstConfigurationsNode.getNode("unittestproject"));
            assertNotNull(hstConfigurationsNode.getNode("unittestsubproject"));
            assertNotNull(hstConfigurationsNode.getNode("global"));
            assertNotNull(hstConfigurationsNode.getNode("sub1"));
            assertNotNull(hstConfigurationsNode.getNode("subsub1"));
            assertNotNull(hstConfigurationsNode.getNode("sub2"));

            Session session = setup.session;

            final HstNode instanceBefore = hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost/localhost");
            // now we ADD a sibling to localhost : This triggers a reload of dev-localhost.
            JcrUtils.copy(session, "/hst:hst/hst:hosts/dev-localhost/localhost",
                    "/hst:hst/hst:hosts/dev-localhost/localhost-copy");

            saveAndInvalidate(session);


            final HstNode instanceAfter = hstNodeLoadingCache.getNode("/hst:hst/hst:hosts/dev-localhost/localhost");

            assertTrue(instanceAfter == instanceBefore);

            // now adding / removing a sibling below /hst:hst/hst:configurations does *NOT* trigger other siblings to reload

            final HstNode configNodeInstanceBefore = hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject");

            JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject",
                    "/hst:hst/hst:configurations/unittestproject-new");

            saveAndInvalidate(session);


            final HstNode configNodeInstanceAfter1 = hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject");
            final HstNode newConfigNodeInstance = hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject-new");
            assertTrue(configNodeInstanceBefore == configNodeInstanceAfter1);

            session.getNode("/hst:hst/hst:configurations/unittestproject-new").setProperty(CONFIGURATION_PROPERTY_LOCKED, true);

            saveAndInvalidate(session);


            final HstNode configNodeInstanceAfter2 = hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject");
            assertTrue(configNodeInstanceBefore == configNodeInstanceAfter2);


            final HstNode newConfigNodeInstanceAfter = hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject-new");
            // unittestproject-new changed only a property, so instance should still be the same!!
            assertTrue(newConfigNodeInstance == newConfigNodeInstanceAfter);
            assertTrue(newConfigNodeInstanceAfter.getValueProvider().getBoolean(CONFIGURATION_PROPERTY_LOCKED));

            session.getNode("/hst:hst/hst:configurations/unittestproject-new").remove();

            saveAndInvalidate(session);


            final HstNode configNodeInstanceAfter3 = hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject");
            assertTrue(configNodeInstanceBefore == configNodeInstanceAfter3);
            assertNull(hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject-new"));
        }
    }

    @Test
    public void testHstConfigurationSiteMenuItemsChangedKeepBeingOrderedAfterReload() throws Exception {
        try (CommonHstConfigSetup setup = new CommonHstConfigSetup()) {
            HstNode mainMenuBefore = hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemenus/main");
            List<HstNode> menuItemsBefore = hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemenus/main").getNodes();

            Session session = setup.session;

            JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:sitemenus/main/News",
                    "/hst:hst/hst:configurations/unittestproject/hst:sitemenus/main/News-copy");

            saveAndInvalidate(session);



            HstNode mainMenuAfter = hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemenus/main");
            List<HstNode> menuItemsAfter = hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemenus/main").getNodes();

            assertTrue(mainMenuBefore== mainMenuAfter);
            // main menu got reloaded
            assertFalse(menuItemsAfter == menuItemsBefore);

            // main menu must still be in order but one added
            assertTrue(menuItemsAfter.size() == (menuItemsBefore.size() + 1));

            final Iterator<HstNode> iteratorBefore = menuItemsBefore.iterator();
            final Iterator<HstNode> iteratorAfter = menuItemsAfter.iterator();
            while(iteratorBefore.hasNext()) {
                assertEquals(iteratorBefore.next().getName(), iteratorAfter.next().getName());
            }
        }
    }

    @Test
    public void assert_hst_upstream_is_not_loading_into_the_hst_node_cache() throws Exception {
        try (CommonHstConfigSetup setup = new CommonHstConfigSetup()) {

            final Session session = setup.session;
            final Node configuration = session.getNode("/hst:hst/hst:configurations/unittestproject");
            final Node workspace = configuration.addNode(NODENAME_HST_WORKSPACE, NODETYPE_HST_WORKSPACE);
            workspace.addNode(NODENAME_HST_PAGES);
            workspace.addNode(NODENAME_HST_SITEMAP);

            configuration.addMixin(MIXINTYPE_HST_BRANCH);
            configuration.setProperty(BRANCH_PROPERTY_BRANCH_ID, "dummy");
            configuration.setProperty(BRANCH_PROPERTY_BRANCH_OF, "dummy");
            JcrUtils.copy(session, workspace.getPath(), "/hst:hst/hst:configurations/unittestproject/hst:upstream");

            saveAndInvalidate(session);



            assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace"));
            assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages"));
            assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap"));

            assertNull(hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject/hst:upstream"));

        }
    }

    @Test
    public void assert_hst_upstream_is_not_loading_into_the_hst_node_cache_as_a_result_of_a_change() throws Exception {
        try (CommonHstConfigSetup setup = new CommonHstConfigSetup()) {
            // Trigger initial loading of all HstNode : after this make changes and make sure the hst:upstream is not
            // added to the hst node model
            hstNodeLoadingCache.getNode("/hst:hst");
            final Session session = setup.session;

            final Node configuration = session.getNode("/hst:hst/hst:configurations/unittestproject");
            final Node workspace = configuration.addNode(NODENAME_HST_WORKSPACE, NODETYPE_HST_WORKSPACE);
            workspace.addNode(NODENAME_HST_PAGES);
            workspace.addNode(NODENAME_HST_SITEMAP);

            configuration.addMixin(MIXINTYPE_HST_BRANCH);
            configuration.setProperty(BRANCH_PROPERTY_BRANCH_ID, "dummy");
            configuration.setProperty(BRANCH_PROPERTY_BRANCH_OF, "dummy");
            JcrUtils.copy(session, workspace.getPath(), "/hst:hst/hst:configurations/unittestproject/hst:upstream");

            saveAndInvalidate(session);



            assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace"));
            assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages"));
            assertNotNull(hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap"));

            assertNull("hst:upstream should not have been loaded",hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject/hst:upstream"));

            // make sure that a change to hst:upstream does not result in loaded hst:upstream

            session.getNode("/hst:hst/hst:configurations/unittestproject/hst:upstream/hst:sitemap").setProperty(GENERAL_PROPERTY_LOCKED_BY, "admin");
            saveAndInvalidate(session);



            // changes on or below hst:upstream should not result in a collected event
            assertTrue("events below and on hst:upstream should be skipped",hstEventsDispatcher.getHstEventsCollector().getHstEvents().isEmpty());

            assertNull("hst:upstream should not have been loaded", hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject/hst:upstream"));

            // make sure that a change to the configuration just above hst:upstream does not result in hst:upstream to be loaded

            configuration.setProperty(BRANCH_PROPERTY_BRANCH_ID, "dummy-again");

            saveAndInvalidate(session);



            assertNull(hstNodeLoadingCache.getNode("/hst:hst/hst:configurations/unittestproject/hst:upstream"));

        }
    }

    private void saveAndInvalidate(final Session session) throws RepositoryException {
        final List<String> changedPaths = new ArrayList<>();
        for (Node changes : new NodeIterable(((HippoSession) session).pendingChanges())) {
            changedPaths.add(changes.getPath());
        }
        session.save();
        invalidator.eventPaths(changedPaths.toArray(new String[0]));
        hstEventsDispatcher.dispatchHstEvents();
    }

    class CommonHstConfigSetup implements AutoCloseable {
        Session session;
        EventListener listener;

        CommonHstConfigSetup() throws RepositoryException {
            session = createSession();
            createHstConfigBackup();
        }

        @Override
        public void close() throws Exception {
            restoreHstConfigBackup();
            session.logout();
        }

        protected void createHstConfigBackup() throws RepositoryException {
            if (!session.nodeExists("/hst-backup")) {
                JcrUtils.copy(session, "/hst:hst", "/hst-backup");
                session.save();
            }
        }

        protected void restoreHstConfigBackup() throws RepositoryException, InterruptedException {
            if (session.nodeExists("/hst-backup")) {
                session.removeItem("/hst:hst");
                JcrUtils.copy(session, "/hst-backup", "/hst:hst");
                saveAndInvalidate(session);

            }
        }

    }
}
