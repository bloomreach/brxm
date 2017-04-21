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
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import com.google.common.eventbus.Subscribe;

import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.log4j.Log4jListener;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentServiceImpl;
import org.hippoecm.hst.pagecomposer.jaxrs.services.MountResourceAccessor;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.AbstractHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerHelper;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.Test;
import org.onehippo.repository.testutils.ExecuteOnLogLevel;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_EDITABLE;
import static org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent.ChannelEventType.DISCARD;
import static org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent.ChannelEventType.PREVIEW_CREATION;
import static org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent.ChannelEventType.PUBLISH;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.AbstractHelper.SEEMS_TO_INDICATE_LIVE_AND_PREVIEW_CONFIGURATIONS_ARE_OUT_OF_SYNC_WHICH_INDICATES_AN_ERROR;
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
        } else {
            assertEquals("Because hst:channel not below hst:workspace, it should not be copied to the preview config.",
                    "/hst:hst/hst:configurations/unittestproject/hst:channel", previewChannel.getChannelPath());

            assertFalse("Because hst:channel not below hst:workspace, it should not be editable.", previewChannel.isChannelSettingsEditable());
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

        changedBySet = previewChannel.getChangedBySet();
        assertTrue(changedBySet.contains("admin"));

        mountResource.publish();

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "/home");

        // there should be no locks
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
    public void testXpathQueries() {

        assertEquals("/jcr:root/hst:hst/hst:configurations/myproject-preview//element(*,hst:containercomponent)[@hst:lockedby = 'admin' or @hst:lockedby = 'editor']",
                MountResourceAccessor.buildXPathQueryToFindContainersForUsers("/hst:hst/hst:configurations/myproject-preview", Arrays.asList(new String[]{"admin", "editor"})));
        assertEquals("/jcr:root/hst:hst/hst:configurations/_x0037__8-preview//element(*,hst:containercomponent)[@hst:lockedby = 'admin' or @hst:lockedby = 'editor']",
                MountResourceAccessor.buildXPathQueryToFindContainersForUsers("/hst:hst/hst:configurations/7_8-preview", Arrays.asList(new String[]{"admin", "editor"})));

        assertEquals("/jcr:root/hst:hst/hst:configurations/myproject-preview/*[@hst:lockedby = 'admin' or @hst:lockedby = 'editor']",
                MountResourceAccessor.buildXPathQueryToFindMainfConfigNodesForUsers("/hst:hst/hst:configurations/myproject-preview", Arrays.asList(new String[]{"admin", "editor"})));
        assertEquals("/jcr:root/hst:hst/hst:configurations/_x0037__8-preview/*[@hst:lockedby = 'admin' or @hst:lockedby = 'editor']",
                MountResourceAccessor.buildXPathQueryToFindMainfConfigNodesForUsers("/hst:hst/hst:configurations/7_8-preview", Arrays.asList(new String[]{"admin", "editor"})));
    }


    @Test
    public void publication_of_mode_than_1_new_page_does_not_result_in_reordering_warnings() throws Exception {
        movePagesFromCommonToUnitTestProject();

        final Node unitTestConfigNode = session.getNode("/hst:hst/hst:configurations/unittestproject");
        unitTestConfigNode.addNode("hst:workspace", "hst:workspace");
        // only sitemap in workspace is copied over to preview
        session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemap",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap");

        session.save();

        mockNewRequest(session, "localhost", "");

        final PageComposerContextService pccs = mountResource.getPageComposerContextService();
        final HstRequestContext ctx = pccs.getRequestContext();

        String liveConfigurationPath = ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath();
        final String previewConfigurationPath = liveConfigurationPath + "-preview";

        mountResource.startEdit();

        // add manually two new pages to the preview
        Node previewConfigurationNode = session.getNode(previewConfigurationPath);
        Node aboutUsSiteMapItemNode = previewConfigurationNode.getNode("hst:workspace/hst:sitemap/about-us");

        Node about2 = JcrUtils.copy(aboutUsSiteMapItemNode, "about-us-2", previewConfigurationNode.getNode("hst:workspace/hst:sitemap"));
        Node about3 = JcrUtils.copy(aboutUsSiteMapItemNode, "about-us-3", previewConfigurationNode.getNode("hst:workspace/hst:sitemap"));

        about2.addMixin(MIXINTYPE_HST_EDITABLE);
        about2.setProperty(GENERAL_PROPERTY_LOCKED_BY, "admin");

        about3.addMixin(MIXINTYPE_HST_EDITABLE);
        about3.setProperty(GENERAL_PROPERTY_LOCKED_BY, "admin");

        session.save();

        mockNewRequest(session, "localhost", "/home");

        ExecuteOnLogLevel.debug((ExecuteOnLogLevel.Executable)() -> {
            try (Log4jListener listener = Log4jListener.onDebug()) {
                mountResource.publish();
                assertTrue(listener.messages().anyMatch(m -> m.contains("Successfully ordered 'about-us-2' before 'about-us-3'")));
                assertFalse(listener.messages().anyMatch(m -> m.contains(SEEMS_TO_INDICATE_LIVE_AND_PREVIEW_CONFIGURATIONS_ARE_OUT_OF_SYNC_WHICH_INDICATES_AN_ERROR)));
            }

        }, AbstractHelper.class);

        assertTrue(session.nodeExists(liveConfigurationPath + "/hst:workspace/hst:sitemap/about-us-2"));
        assertTrue(session.nodeExists(liveConfigurationPath + "/hst:workspace/hst:sitemap/about-us-3"));
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
            HstServices.getComponentManager().registerEventSubscriber(listener);
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
            HstServices.getComponentManager().unregisterEventSubscriber(listener);
        }

    }

    @Test
    public void publish_mount_with_ChannelEventListener() throws Exception {
        final ChannelEventListener listener = new ChannelEventListener();
        try {
            HstServices.getComponentManager().registerEventSubscriber(listener);
            createSomePreviewChanges();

            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

            mountResource.publish();

            assertEquals(1, listener.getProcessed().size());
            // during the event dispatching, the locks should already have been removed from preview!
            assertFalse("during event dispatching locks should be already removed!",
                    listener.locksOnConfigurationPresentDuringEventDispatching);
        } finally {
            HstServices.getComponentManager().unregisterEventSubscriber(listener);
        }
    }

    @Test
    public void discard_mount_with_ChannelEventListener() throws Exception {
        final ChannelEventListener listener = new ChannelEventListener();
        try {
            HstServices.getComponentManager().registerEventSubscriber(listener);
            createSomePreviewChanges();
            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));
            mountResource.discardChanges();
            assertEquals(1, listener.getProcessed().size());

            // during the event dispatching, the locks should already have been removed from preview!
            assertFalse("during event dispatching locks should be already removed!",
                    listener.locksOnConfigurationPresentDuringEventDispatching);
        } finally {
            HstServices.getComponentManager().unregisterEventSubscriber(listener);
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
            HstServices.getComponentManager().registerEventSubscriber(listener);
            createSomePreviewChanges();

            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

            Response response = mountResource.publish();

            assertNotNull(listener.handledEvent.getException());
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            assertEquals(listener.handledEvent.getException().toString(), ((ExtResponseRepresentation)response.getEntity()).getMessage());

            // session contains not more changes as should be reset
            assertFalse(session.hasPendingChanges());

            // locks should still be present since publication failed
            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

        } finally {
            HstServices.getComponentManager().unregisterEventSubscriber(listener);
        }
    }


    @Test
    public void discard_mount_with_ChannelEventListener_that_sets_ClientException_does_not_result_in_discard_but_bad_request() throws Exception {
        final ChannelEventListenerSettingClientException listener = new ChannelEventListenerSettingClientException();
        try {
            HstServices.getComponentManager().registerEventSubscriber(listener);
            createSomePreviewChanges();

            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

            Response response = mountResource.discardChanges();

            assertNotNull(listener.handledEvent.getException());
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            assertEquals(listener.handledEvent.getException().toString(), ((ExtResponseRepresentation)response.getEntity()).getMessage());

            // session contains not more changes as should be reset
            assertFalse(session.hasPendingChanges());

            // locks should still be present since discard failed
            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

        } finally {
            HstServices.getComponentManager().unregisterEventSubscriber(listener);
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
            HstServices.getComponentManager().registerEventSubscriber(listener);
            createSomePreviewChanges();

            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

            Response response = mountResource.publish();

            assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

            assertEquals("IllegalStateException message", ((ExtResponseRepresentation)response.getEntity()).getMessage());
            // session contains not more changes as should be reset
            assertFalse(session.hasPendingChanges());

            // locks should still be present since publication failed
            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

        } finally {
            HstServices.getComponentManager().unregisterEventSubscriber(listener);
        }
    }


    @Test
    public void discard_mount_with_ChannelEventListener_that_sets_IllegalStateException_does_not_result_in_discard_but_server_error() throws Exception {
        final ChannelEventListenerSettingIllegalStateException listener = new ChannelEventListenerSettingIllegalStateException();
        try {
            HstServices.getComponentManager().registerEventSubscriber(listener);
            createSomePreviewChanges();

            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

            Response response = mountResource.discardChanges();

            assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
            assertEquals("IllegalStateException message", ((ExtResponseRepresentation)response.getEntity()).getMessage());

            // session contains not more changes as should be reset
            assertFalse(session.hasPendingChanges());

            // locks should still be present since discard failed
            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

        } finally {
            HstServices.getComponentManager().unregisterEventSubscriber(listener);
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

}
