/*
 * Copyright 2013-2014 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.Set;

import javax.jcr.Node;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.MountResourceAccessor;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerHelper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MountResourceTest extends AbstractMountResourceTest {

    @Test
    public void testEditAndPublishMount() throws Exception {

        movePagesFromCommonToUnitTestProject();
        createWorkspaceWithTestContainer();
        addReferencedContainerForHomePage();
        String catalogItemUUID = addCatalogItem();
        session.save();
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "/home");

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


        assertTrue("Live channel path node should exist",
                session.nodeExists(ctx.getResolvedMount().getMount().getChannelPath()));
        assertTrue("Preview channel path node should exist",
                session.nodeExists(ctx.getResolvedMount().getMount().getChannelPath() + "-preview"));

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "/home");

        assertTrue(pccs.getEditingPreviewSite().getConfigurationPath().equals(pccs.getEditingLiveConfigurationPath() + "-preview"));
        assertTrue(pccs.getEditingPreviewConfigurationPath().equals(pccs.getEditingLiveConfigurationPath() + "-preview"));
        assertTrue(pccs.getEditingPreviewChannel().getHstConfigPath().equals(pccs.getEditingPreviewSite().getConfigurationPath()));
        assertEquals(0, pccs.getEditingPreviewChannel().getChangedBySet().size());

        assertTrue(pccs.getEditingPreviewChannel().getId().endsWith("-preview"));
        assertTrue(pccs.getEditingPreviewChannel().getId().equals(pccs.getEditingMount().getChannel().getId()));

        final String previewContainerNodeUUID = session.getNode(previewConfigurationPath)
                .getNode("hst:workspace/hst:containers/testcontainer").getIdentifier();
        pccs.getRequestContext().setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, previewContainerNodeUUID);

        // there should be not yet any locks
        Set<String> changedBySet = mountResource.getPageComposerContextService().getEditingPreviewChannel().getChangedBySet();
        assertTrue(changedBySet.isEmpty());

        final ContainerComponentResource containerComponentResource = createContainerResource();
        final Response response = containerComponentResource.createContainerItem(catalogItemUUID, 0);
        assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                Response.Status.OK.getStatusCode(), response.getStatus());

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "/home");

        changedBySet = pccs.getEditingPreviewChannel().getChangedBySet();
        assertTrue(changedBySet.contains("admin"));

        mountResource.publish();

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "/home");

        // there should be no locks
        changedBySet = pccs.getEditingPreviewChannel().getChangedBySet();
        assertTrue(changedBySet.isEmpty());

    }

    protected ContainerComponentResource createContainerResource() {
        final ContainerComponentResource containerComponentResource = new ContainerComponentResource();
        containerComponentResource.setPageComposerContextService(mountResource.getPageComposerContextService());
        ContainerHelper helper = new ContainerHelper();
        helper.setPageComposerContextService(mountResource.getPageComposerContextService());
        containerComponentResource.setContainerHelper(helper);
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
        mockNewRequest(session, "localhost", "/home");

        assertEquals("/hst:hst/hst:configurations/7_8", pccs.getRequestContext().getResolvedMount().getMount().getHstSite().getConfigurationPath());

        mountResource.startEdit();

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "/home");

        final String previewContainerNodeUUID = session.getNode(pccs.getEditingPreviewSite().getConfigurationPath())
                .getNode("hst:workspace/hst:containers/testcontainer").getIdentifier();

        pccs.getRequestContext().setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, previewContainerNodeUUID);

        // there should be not yet any locks
        Set<String> usersWithLockedContainers = pccs.getEditingPreviewChannel().getChangedBySet();
        assertTrue(usersWithLockedContainers.isEmpty());

        final ContainerComponentResource containerComponentResource = createContainerResource();
        final Response response = containerComponentResource.createContainerItem(catalogItemUUID, 0);
        assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                Response.Status.OK.getStatusCode(), response.getStatus());

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

}
