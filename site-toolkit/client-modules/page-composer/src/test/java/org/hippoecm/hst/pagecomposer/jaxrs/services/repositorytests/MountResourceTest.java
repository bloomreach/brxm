/*
 * Copyright 2013-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.AbstractConfigResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentServiceImpl;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.AbstractHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerHelper;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.Test;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.onehippo.cms7.services.hst.Channel;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hippoecm.hst.configuration.HstNodeTypes.CONFIGURATION_PROPERTY_LOCKED;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_EDITABLE;
import static org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent.ChannelEventType.DISCARD;
import static org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent.ChannelEventType.PREVIEW_CREATION;
import static org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent.ChannelEventType.PUBLISH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MountResourceTest extends AbstractMountResourceTest {

    @Test
    public void testEditAndPublishMount_with_non_workspace_channel() throws Exception {
        movePagesFromCommonToUnitTestProject();
        createWorkspaceWithTestContainer();
        edit_and_publish_assertions(false);
    }

    @Test
    public void testEditAndPublishMount_with_workspace_channel() throws Exception {
        movePagesFromCommonToUnitTestProject();
        createWorkspaceWithTestContainer();
        moveChannelToWorkspace();
        edit_and_publish_assertions(true);
    }

    private void edit_and_publish_assertions(final boolean channelNodeInWorkspace) throws Exception {

        addReferencedContainerForHomePage();
        String catalogItemUUID = addCatalogItem();
        session.save();
        // give time for jcr events to evict model
        Thread.sleep(200);
        mockNewRequest(session, "localhost", "");
        final PageComposerContextService pccs = mountResource.getPageComposerContextService();
        final HstRequestContext ctx = pccs.getRequestContext();

        final String previewConfigurationPath = ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath() + "-preview";
        assertFalse("Preview config node should not exist yet.",
                session.nodeExists(previewConfigurationPath));

        assertTrue("Preview and live channel should have the same paths, as preview does not exist yet",
                pccs.getEditingPreviewChannelPath().equals(pccs.getEditingLiveChannelPath()));

        //PageComposerContextService tests prior to editing
        if (channelNodeInWorkspace) {
            assertEquals("Wrong value for live channel path from page composer context service (before editing channel).",
                    "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:channel", pccs.getEditingLiveChannelPath());
            assertEquals("Wrong value for preview channel path from page composer context service (before editing channel).",
                    "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:channel", pccs.getEditingPreviewChannelPath());
        } else {
            assertEquals("Wrong value for live channel path from page composer context service (before editing channel).",
                    "/hst:hst/hst:configurations/unittestproject/hst:channel", pccs.getEditingLiveChannelPath());
            assertEquals("Wrong value for preview channel path from page composer context service (before editing channel).",
                    "/hst:hst/hst:configurations/unittestproject/hst:channel", pccs.getEditingPreviewChannelPath());
        }

        mountResource.startEdit();

        assertTrue("Live config node should exist",
                session.nodeExists(ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath()));
        assertTrue("Preview config node should exist",
                session.nodeExists(previewConfigurationPath));

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "");

        assertTrue(pccs.getEditingPreviewSite().getConfigurationPath().equals(pccs.getEditingLiveConfigurationPath() + "-preview"));
        assertTrue(pccs.getEditingPreviewConfigurationPath().equals(pccs.getEditingLiveConfigurationPath() + "-preview"));


        Channel previewChannel = pccs.getEditingPreviewChannel();
        assertTrue(previewChannel.getHstConfigPath().equals(pccs.getEditingPreviewSite().getConfigurationPath()));
        assertEquals(0, previewChannel.getChangedBySet().size());

        assertTrue("Although the channel node might not be stored below the preview configuration if it " +
                "is not in the hst:workspace (and then still in live), the id should still end with '-preview'",
                previewChannel.getId().endsWith("-preview"));

        if (channelNodeInWorkspace) {
            assertEquals("Because hst:channel *is* below hst:workspace, it should be copied to the preview config.",
                    "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:channel", previewChannel.getChannelPath());

            assertTrue("Because hst:channel below hst:workspace, it should be editable.", previewChannel.isChannelSettingsEditable());

            //PageComposerContextService tests after editing
            assertEquals("Wrong value for live channel path from page composer context service (after editing channel).",
                    "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:channel", pccs.getEditingLiveChannelPath());
            assertEquals("Wrong value for preview channel path from page composer context service (after editing channel).",
                    "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:channel", pccs.getEditingPreviewChannelPath());

        } else {
            assertEquals("Because hst:channel not below hst:workspace, it should not be copied to the preview config.",
                    "/hst:hst/hst:configurations/unittestproject/hst:channel", previewChannel.getChannelPath());

            assertFalse("Because hst:channel not below hst:workspace, it should not be editable.", previewChannel.isChannelSettingsEditable());

            //PageComposerContextService tests after editing
            assertEquals("Wrong value for live channel path from page composer context service (after editing channel).",
                    "/hst:hst/hst:configurations/unittestproject/hst:channel", pccs.getEditingLiveChannelPath());
            assertEquals("Wrong value for preview channel path from page composer context service (after editing channel).",
                    "/hst:hst/hst:configurations/unittestproject/hst:channel", pccs.getEditingPreviewChannelPath());

        }

        final String previewContainerNodeUUID = session.getNode(previewConfigurationPath)
                .getNode("hst:workspace/hst:containers/testcontainer").getIdentifier();
        pccs.getRequestContext().setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, previewContainerNodeUUID);

        // there should be not yet any locks
        Set<String> changedBySet = mountResource.getPageComposerContextService().getEditingPreviewChannel().getChangedBySet();
        assertTrue(changedBySet.isEmpty());

        final ContainerComponentResource containerComponentResource = createContainerResource();
        final Response response = containerComponentResource.createContainerItem(catalogItemUUID, 0);
        assertEquals("New container item should be created", Response.Status.CREATED.getStatusCode(), response.getStatus());

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "/home");
        // reload the preview channel : After changes, it *must* be a different object
        assertTrue("Since there are changes, pccs.getEditingPreviewChannel should return a new object.", previewChannel != pccs.getEditingPreviewChannel());

        previewChannel = pccs.getEditingPreviewChannel();
        changedBySet = previewChannel.getChangedBySet();
        assertTrue(changedBySet.contains("admin"));

        mountResource.publish();

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "/home");

        // there should be no locks
        previewChannel = pccs.getEditingPreviewChannel();
        changedBySet = previewChannel.getChangedBySet();
        assertTrue(changedBySet.isEmpty());
    }

    protected ContainerComponentResource createContainerResource() {
        final ContainerComponentResource containerComponentResource = new ContainerComponentResource();
        final PageComposerContextService pageComposerContextService = mountResource.getPageComposerContextService();

        final ContainerHelper helper = new ContainerHelper();
        helper.setPageComposerContextService(pageComposerContextService);


        final ContainerComponentService containerComponentService = new ContainerComponentServiceImpl(pageComposerContextService, helper);
        containerComponentResource.setContainerComponentService(containerComponentService);
        containerComponentResource.setPageComposerContextService(pageComposerContextService);
        return containerComponentResource;
    }

    @Test
    public void publication_of_more_than_1_new_component_does_not_result_in_reordering_warnings() throws Exception {
        movePagesFromCommonToUnitTestProject();

        final Node unitTestConfigNode = session.getNode("/hst:hst/hst:configurations/unittestproject");
        unitTestConfigNode.addNode("hst:workspace", "hst:workspace");

        session.move("/hst:hst/hst:configurations/unittestproject/hst:pages",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages");

        session.save();

        mockNewRequest(session, "localhost", "");

        final PageComposerContextService pccs = mountResource.getPageComposerContextService();
        final HstRequestContext ctx = pccs.getRequestContext();

        String liveConfigurationPath = ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath();
        final String previewConfigurationPath = liveConfigurationPath + "-preview";

        mountResource.startEdit();

        // add manually two new pages to the preview
        Node previewConfigurationNode = session.getNode(previewConfigurationPath);
        Node standardComponentBody = previewConfigurationNode.getNode("hst:workspace/hst:pages/standardoverview/body");

        Node body2 = JcrUtils.copy(standardComponentBody, "body-2", previewConfigurationNode.getNode("hst:workspace/hst:pages/standardoverview"));
        Node body3 = JcrUtils.copy(standardComponentBody, "body-3", previewConfigurationNode.getNode("hst:workspace/hst:pages/standardoverview"));

        body2.addMixin(MIXINTYPE_HST_EDITABLE);
        body2.setProperty(GENERAL_PROPERTY_LOCKED_BY, "admin");

        body3.addMixin(MIXINTYPE_HST_EDITABLE);
        body3.setProperty(GENERAL_PROPERTY_LOCKED_BY, "admin");

        session.save();

        mockNewRequest(session, "localhost", "/home");

        try ( Log4jInterceptor listener = Log4jInterceptor.onWarn().trap(AbstractHelper.class).build()) {
            mountResource.publish();
            assertTrue(listener.messages().count() == 0);
        };

        assertTrue(session.nodeExists(liveConfigurationPath + "/hst:workspace/hst:pages/standardoverview/body-2"));
        assertTrue(session.nodeExists(liveConfigurationPath + "/hst:workspace/hst:pages/standardoverview/body-3"));
    }

    @Test
    public void publication_of_containers_keep_order_containers_in_live_same_as_order_in_preview() throws Exception {
        movePagesFromCommonToUnitTestProject();
        createWorkspaceWithTestContainer();
        // add second and third sibling container
        final Node containers = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:containers");
        final Node containerNode2 = containers.addNode("testcontainer2", "hst:containercomponent");
        containerNode2.setProperty("hst:xtype", "HST.vBox");
        final Node containerNode3 = containers.addNode("testcontainer3", "hst:containercomponent");
        containerNode3.setProperty("hst:xtype", "HST.vBox");

        addReferencedContainerForHomePage();
        String catalogItemUUID = addCatalogItem();
        session.save();
        // give time for jcr events to evict model
        Thread.sleep(200);

        assertOrderOfContainerNode(containers, "testcontainer", "testcontainer2", "testcontainer3");

        mockNewRequest(session, "localhost", "");

        final PageComposerContextService pccs = mountResource.getPageComposerContextService();
        final HstRequestContext ctx = pccs.getRequestContext();

        final String previewConfigurationPath = ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath() + "-preview";

        mountResource.startEdit();

        final Node previewContainers = session.getNode(previewConfigurationPath).getNode("hst:workspace/hst:containers");

        final String previewFirstContainerNodeUUID = session.getNode(previewConfigurationPath)
                .getNode("hst:workspace/hst:containers/testcontainer").getIdentifier();
        pccs.getRequestContext().setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, previewFirstContainerNodeUUID);

        // modify
        ContainerComponentResource containerComponentResource = createContainerResource();
        containerComponentResource.createContainerItem(catalogItemUUID, 0);

        // publish changes : now the order of 'testcontainer' and 'testcontainer2' should be maintained in live configuration
        mockNewRequest(session, "localhost", "/home");
        mountResource.publish();

        // after publication, the order of the containers should be preserved
        assertOrderOfContainerNode(containers, "testcontainer", "testcontainer2", "testcontainer3");
        assertOrderOfContainerNode(previewContainers, "testcontainer", "testcontainer2", "testcontainer3");

        // modify middle container to make sure works also correctly
        final String previewSecondContainerNodeUUID = session.getNode(previewConfigurationPath)
                .getNode("hst:workspace/hst:containers/testcontainer2").getIdentifier();
        pccs.getRequestContext().setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, previewSecondContainerNodeUUID);

        // modify
        containerComponentResource = createContainerResource();
        containerComponentResource.createContainerItem(catalogItemUUID, 0);

        // publish changes : now the order of 'testcontainer' and 'testcontainer2' should be maintained in live configuration
        mockNewRequest(session, "localhost", "/home");
        mountResource.publish();

        // after publication, the order of the containers should be preserved
        assertOrderOfContainerNode(containers, "testcontainer", "testcontainer2", "testcontainer3");
        assertOrderOfContainerNode(previewContainers, "testcontainer", "testcontainer2", "testcontainer3");

        // modify last container to make sure works also correctly
        final String previewThirdContainerNodeUUID = session.getNode(previewConfigurationPath)
                .getNode("hst:workspace/hst:containers/testcontainer3").getIdentifier();
        pccs.getRequestContext().setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, previewThirdContainerNodeUUID);

        // modify
        containerComponentResource = createContainerResource();
        containerComponentResource.createContainerItem(catalogItemUUID, 0);

        // publish changes : now the order of 'testcontainer' and 'testcontainer2' should be maintained in live configuration
        mockNewRequest(session, "localhost", "/home");
        mountResource.publish();

        // after publication, the order of the containers should be preserved
        assertOrderOfContainerNode(containers, "testcontainer", "testcontainer2", "testcontainer3");
        assertOrderOfContainerNode(previewContainers, "testcontainer", "testcontainer2", "testcontainer3");
    }

    @Test
    public void discarding_changed_containers_keeps_order_containers_in_live_same_as_order_in_preview() throws Exception {
        movePagesFromCommonToUnitTestProject();
        createWorkspaceWithTestContainer();
        // add second and third sibling container
        final Node containers = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:containers");
        final Node containerNode2 = containers.addNode("testcontainer2", "hst:containercomponent");
        containerNode2.setProperty("hst:xtype", "HST.vBox");
        final Node containerNode3 = containers.addNode("testcontainer3", "hst:containercomponent");
        containerNode3.setProperty("hst:xtype", "HST.vBox");

        addReferencedContainerForHomePage();
        String catalogItemUUID = addCatalogItem();
        session.save();
        // give time for jcr events to evict model
        Thread.sleep(200);

        assertOrderOfContainerNode(containers, "testcontainer", "testcontainer2", "testcontainer3");

        mockNewRequest(session, "localhost", "");

        final PageComposerContextService pccs = mountResource.getPageComposerContextService();
        final HstRequestContext ctx = pccs.getRequestContext();

        final String previewConfigurationPath = ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath() + "-preview";

        mountResource.startEdit();

        final Node previewContainers = session.getNode(previewConfigurationPath).getNode("hst:workspace/hst:containers");

        final String previewFirstContainerNodeUUID = session.getNode(previewConfigurationPath)
                .getNode("hst:workspace/hst:containers/testcontainer").getIdentifier();
        pccs.getRequestContext().setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, previewFirstContainerNodeUUID);

        // modify
        ContainerComponentResource containerComponentResource = createContainerResource();
        containerComponentResource.createContainerItem(catalogItemUUID, 0);

        // discard changes : now the order of 'testcontainer' and 'testcontainer2' should be maintained in preview and live configuration
        mockNewRequest(session, "localhost", "/home");
        mountResource.discardChanges();

        // after publication, the order of the containers should be preserved
        assertOrderOfContainerNode(containers, "testcontainer", "testcontainer2", "testcontainer3");
        assertOrderOfContainerNode(previewContainers, "testcontainer", "testcontainer2", "testcontainer3");

        // modify middle container to make sure works also correctly
        final String previewSecondContainerNodeUUID = session.getNode(previewConfigurationPath)
                .getNode("hst:workspace/hst:containers/testcontainer2").getIdentifier();
        pccs.getRequestContext().setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, previewSecondContainerNodeUUID);

        // modify
        containerComponentResource = createContainerResource();
        containerComponentResource.createContainerItem(catalogItemUUID, 0);

        // discard changes : now the order of 'testcontainer' and 'testcontainer2' should be maintained in preview and live configuration
        mockNewRequest(session, "localhost", "/home");
        mountResource.discardChanges();

        // after publication, the order of the containers should be preserved
        assertOrderOfContainerNode(containers, "testcontainer", "testcontainer2", "testcontainer3");
        assertOrderOfContainerNode(previewContainers, "testcontainer", "testcontainer2", "testcontainer3");

        // modify last container to make sure works also correctly
        final String previewThirdContainerNodeUUID = session.getNode(previewConfigurationPath)
                .getNode("hst:workspace/hst:containers/testcontainer3").getIdentifier();
        pccs.getRequestContext().setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, previewThirdContainerNodeUUID);

        // modify
        containerComponentResource = createContainerResource();
        containerComponentResource.createContainerItem(catalogItemUUID, 0);

        // discard changes : now the order of 'testcontainer' and 'testcontainer2' should be maintained in preview and live configuration
        mockNewRequest(session, "localhost", "/home");
        mountResource.discardChanges();

        // after publication, the order of the containers should be preserved
        assertOrderOfContainerNode(containers, "testcontainer", "testcontainer2", "testcontainer3");
        assertOrderOfContainerNode(previewContainers, "testcontainer", "testcontainer2", "testcontainer3");
    }

    private void assertOrderOfContainerNode(final Node containers, final String... names) throws RepositoryException {
        int i = 0;
        for (Node container : new NodeIterable(containers.getNodes())) {
            assertEquals(container.getName(), names[i]);
            i++;
        }
    }

    @Test
    public void testEditAndPublishProjectThatStartsWithNumber() throws Exception {

        movePagesFromCommonToUnitTestProject();
        createWorkspaceWithTestContainer();
        addReferencedContainerForHomePage();
        String catalogItemUUID = addCatalogItem();

        session.move("/hst:hst/hst:configurations/unittestproject", "/hst:hst/hst:configurations/7_8");
        // change default unittestproject site to map to /hst:hst/hst:configurations/7_8
        Node testSideNode = session.getNode("/hst:hst/hst:sites/unittestproject");
        testSideNode.setProperty("hst:configurationpath", "/hst:hst/hst:configurations/7_8");

        session.save();
        // give time for jcr events to evict model
        Thread.sleep(200);

        final PageComposerContextService pccs = mountResource.getPageComposerContextService();
        mockNewRequest(session, "localhost", "");

        assertEquals("/hst:hst/hst:configurations/7_8", pccs.getRequestContext().getResolvedMount().getMount().getHstSite().getConfigurationPath());

        mountResource.startEdit();

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "");

        final String previewContainerNodeUUID = session.getNode(pccs.getEditingPreviewSite().getConfigurationPath())
                .getNode("hst:workspace/hst:containers/testcontainer").getIdentifier();

        pccs.getRequestContext().setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, previewContainerNodeUUID);

        // there should be not yet any locks
        Set<String> usersWithLockedContainers = pccs.getEditingPreviewChannel().getChangedBySet();
        assertTrue(usersWithLockedContainers.isEmpty());

        final ContainerComponentResource containerComponentResource = createContainerResource();
        final Response response = containerComponentResource.createContainerItem(catalogItemUUID, 0);
        assertEquals("New container item should be created", Response.Status.CREATED.getStatusCode(), response.getStatus());

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);
        mockNewRequest(session, "localhost", "/home");

        usersWithLockedContainers = pccs.getEditingPreviewChannel().getChangedBySet();
        assertTrue(usersWithLockedContainers.contains("admin"));

        mountResource.publish();

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "/home");

        usersWithLockedContainers = pccs.getEditingPreviewChannel().getChangedBySet();
        assertTrue(usersWithLockedContainers.isEmpty());

    }


    public static class ChannelEventListener {

        private List<ChannelEvent> processed = new ArrayList<>();
        private boolean previewCreatedEventProcessed;
        private boolean locksOnConfigurationPresentDuringEventDispatching;

        @Subscribe
        public void onChannelEvent(ChannelEvent event) throws RepositoryException {
            final Session session = event.getRequestContext().getSession();

            // to assure that the event publishSynchronousEvent(event); in MountResource happens *AFTER* all locks are
            // removed without yet saving by the calls 1-5 below, we need to validate that the locks in preview are all gone!

            // 1 : copyChangedMainConfigNodes(session, previewConfigurationPath, liveConfigurationPath, mainConfigNodeNamesToPublish);
            // 2 : publishChannelChanges(session, userIds);

            // 3 : siteMapHelper.publishChanges(userIds);
            // 4 : pagesHelper.publishChanges(userIds);
            // 5 : siteMenuHelper.publishChanges(userIds);
            if (event.getChannelEventType() == DISCARD || event.getChannelEventType() == PUBLISH) {
                locksOnConfigurationPresentDuringEventDispatching = lockForPresentBelow(session, event.getEditingPreviewSite().getConfigurationPath());
                processed.add(event);
            } else if (event.getChannelEventType() == PREVIEW_CREATION) {
                previewCreatedEventProcessed = true;
            }

        }

        public List<ChannelEvent> getProcessed() {
            return processed;
        }
    }

    private static boolean lockForPresentBelow(final Session session, final String rootPath) throws RepositoryException {
        final Node start = session.getNode(rootPath);
        return checkRecursiveForLock(start);

    }

    private static boolean checkRecursiveForLock(final Node current) throws RepositoryException {
        if (current.hasProperty(GENERAL_PROPERTY_LOCKED_BY)) {
            return true;
        }
        for (Node child : new NodeIterable(current.getNodes())) {
            boolean hasLock = checkRecursiveForLock(child);
            if (hasLock) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void create_preview_channel_event() throws Exception {
        final ChannelEventListener listener = new ChannelEventListener();
        try {
            registerChannelEventListener(listener);
            mockNewRequest(session, "localhost", "");
            mountResource.startEdit();
            assertTrue(listener.previewCreatedEventProcessed);

            // reset and 'startEdit again : since there is already a preview, this should not result in another
            // PREVIEW_CREATED event
            listener.previewCreatedEventProcessed = false;
            // reset the request
            mockNewRequest(session, "localhost", "");
            mountResource.startEdit();
            assertFalse(listener.previewCreatedEventProcessed);
        } finally {
            unregisterChannelEventListener(listener);
        }

    }

    @Test
    public void publish_mount_with_ChannelEventListener() throws Exception {
        final ChannelEventListener listener = new ChannelEventListener();
        try {
            registerChannelEventListener(listener);
            createSomePreviewChanges();

            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

            mountResource.publish();

            assertEquals(1, listener.getProcessed().size());
            // during the event dispatching, the locks should already have been removed from preview!
            assertFalse("during event dispatching locks should be already removed!",
                    listener.locksOnConfigurationPresentDuringEventDispatching);
        } finally {
            unregisterChannelEventListener(listener);
        }
    }

    @Test
    public void discard_mount_with_ChannelEventListener() throws Exception {
        final ChannelEventListener listener = new ChannelEventListener();
        try {
            registerChannelEventListener(listener);
            createSomePreviewChanges();
            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));
            mountResource.discardChanges();
            assertEquals(1, listener.getProcessed().size());

            // during the event dispatching, the locks should already have been removed from preview!
            assertFalse("during event dispatching locks should be already removed!",
                    listener.locksOnConfigurationPresentDuringEventDispatching);
        } finally {
            unregisterChannelEventListener(listener);
        }

    }

    public static class ChannelEventListenerSettingClientException {
        private ChannelEvent handledEvent;

        @Subscribe
        public void onChannelEvent(ChannelEvent event) throws RepositoryException {
            if (event.getChannelEventType() == PUBLISH || event.getChannelEventType() == DISCARD) {
                this.handledEvent = event;
                event.setException(new ClientException("ClientException message", ClientError.UNKNOWN));
            }
        }
    }

    @Test
    public void publish_mount_with_ChannelEventListener_that_sets_ClientException_does_not_result_in_publication_but_bad_request() throws Exception {
        final ChannelEventListenerSettingClientException listener = new ChannelEventListenerSettingClientException();
        try {
            registerChannelEventListener(listener);
            createSomePreviewChanges();

            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

            Response response = mountResource.publish();

            assertNotNull(listener.handledEvent.getException());
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            assertEquals(listener.handledEvent.getException().toString(), ((ResponseRepresentation)response.getEntity()).getMessage());

            // session contains not more changes as should be reset
            assertFalse(session.hasPendingChanges());

            // locks should still be present since publication failed
            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

        } finally {
            unregisterChannelEventListener(listener);
        }
    }


    @Test
    public void discard_mount_with_ChannelEventListener_that_sets_ClientException_does_not_result_in_discard_but_bad_request() throws Exception {
        final ChannelEventListenerSettingClientException listener = new ChannelEventListenerSettingClientException();
        try {
            registerChannelEventListener(listener);
            createSomePreviewChanges();

            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

            Response response = mountResource.discardChanges();

            assertNotNull(listener.handledEvent.getException());
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            assertEquals(listener.handledEvent.getException().toString(), ((ResponseRepresentation)response.getEntity()).getMessage());

            // session contains not more changes as should be reset
            assertFalse(session.hasPendingChanges());

            // locks should still be present since discard failed
            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

        } finally {
            unregisterChannelEventListener(listener);
        }
    }

    public static class ChannelEventListenerSettingIllegalStateException {
        @Subscribe
        public void onChannelEvent(ChannelEvent event) throws RepositoryException {
            if (event.getChannelEventType() == PUBLISH || event.getChannelEventType() == DISCARD) {
                event.setException(new IllegalStateException("IllegalStateException message"));
            }
        }
    }

    @Test
    public void publish_mount_with_ChannelEventListener_that_sets_IllegalStateException_does_not_result_in_publication_but_server_error() throws Exception {
        final ChannelEventListenerSettingIllegalStateException listener = new ChannelEventListenerSettingIllegalStateException();
        try {
            registerChannelEventListener(listener);
            createSomePreviewChanges();

            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

            try (Log4jInterceptor ignore = Log4jInterceptor.onWarn().trap(AbstractConfigResource.class).deny().build()) {
                Response response = mountResource.publish();


                assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

                assertEquals("IllegalStateException message", ((ResponseRepresentation) response.getEntity()).getMessage());
                // session contains not more changes as should be reset
                assertFalse(session.hasPendingChanges());

                // locks should still be present since publication failed
                assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));
            }
        } finally {
            unregisterChannelEventListener(listener);
        }
    }


    @Test
    public void discard_mount_with_ChannelEventListener_that_sets_IllegalStateException_does_not_result_in_discard_but_server_error() throws Exception {
        final ChannelEventListenerSettingIllegalStateException listener = new ChannelEventListenerSettingIllegalStateException();
        try {
            registerChannelEventListener(listener);
            createSomePreviewChanges();

            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

            try (Log4jInterceptor ignore = Log4jInterceptor.onWarn().trap(AbstractConfigResource.class).build()) {
                Response response = mountResource.discardChanges();
                assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
                assertEquals("IllegalStateException message", ((ResponseRepresentation)response.getEntity()).getMessage());

                // session contains not more changes as should be reset
                assertFalse(session.hasPendingChanges());

                // locks should still be present since discard failed
                assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));
            }


        } finally {
            unregisterChannelEventListener(listener);
        }
    }

    private void createSomePreviewChanges() throws Exception {
        movePagesFromCommonToUnitTestProject();
        createWorkspaceWithTestContainer();
        addReferencedContainerForHomePage();
        String catalogItemUUID = addCatalogItem();
        session.save();
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "");
        mountResource.startEdit();

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "");

        final PageComposerContextService pccs = mountResource.getPageComposerContextService();
        final String previewConfigurationPath = pccs.getRequestContext().getResolvedMount().getMount().getHstSite().getConfigurationPath();

        final String previewContainerNodeUUID = session.getNode(previewConfigurationPath)
                .getNode("hst:workspace/hst:containers/testcontainer").getIdentifier();
        pccs.getRequestContext().setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, previewContainerNodeUUID);

        final ContainerComponentResource containerComponentResource = createContainerResource();
        final Response response = containerComponentResource.createContainerItem(catalogItemUUID, 0);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "/home");

    }

    @Test
    public void preview_creation_of_live_is_locked_results_in_locked_preview() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject").setProperty(CONFIGURATION_PROPERTY_LOCKED, true);

        session.save();
        // give time for jcr events to evict model
        Thread.sleep(200);
        mockNewRequest(session, "localhost", "");
        final PageComposerContextService pccs = mountResource.getPageComposerContextService();
        final HstRequestContext ctx = pccs.getRequestContext();

        mountResource.startEdit();

        final String previewConfigurationPath = ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath() + "-preview";
        assertTrue(session.getNode(previewConfigurationPath).getProperty(CONFIGURATION_PROPERTY_LOCKED).getBoolean());
    }

}
