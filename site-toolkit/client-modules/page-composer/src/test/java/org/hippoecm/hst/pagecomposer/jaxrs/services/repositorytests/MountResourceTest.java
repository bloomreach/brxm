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
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.MountResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.MountResourceAccessor;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MountResourceTest extends AbstractMountResourceTest {

    @Test
    public void testEditAndPublishMount() throws Exception {

        movePagesFromCommonToUnitTestProject();
        createWorkspaceWithTestContainer();
        addReferencedContainerToHomePage();
        String catalogItemUUID = addCatalogItem();
        session.save();
        // give time for jcr events to evict model
        Thread.sleep(200);

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL(request, "localhost", "/home");
        final String mountId = ctx.getResolvedMount().getMount().getIdentifier();
        ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, mountId);

        setMountIdOnHttpSession(request, mountId);

        final String previewConfigurationPath = ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath() + "-preview";
        assertFalse("Preview config node should not exist yet.",
                session.nodeExists(previewConfigurationPath));

        ((HstMutableRequestContext) ctx).setSession(session);

        MountResource mountResource = createResource();
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
        final MockHttpServletRequest secondRequest = new MockHttpServletRequest();
        final HstRequestContext secondCtx = getRequestContextWithResolvedSiteMapItemAndContainerURL(secondRequest, "localhost", "/home");
        ((HstMutableRequestContext) secondCtx).setSession(session);

        final ContextualizableMount mount = (ContextualizableMount) secondCtx.getResolvedMount().getMount();

        setMountIdOnHttpSession(secondRequest, mount.getIdentifier());
        assertTrue(mount.getPreviewHstSite().getConfigurationPath().equals(mount.getHstSite().getConfigurationPath() + "-preview"));
        assertTrue(mount.getPreviewChannel().getHstConfigPath().equals(mount.getPreviewHstSite().getConfigurationPath()));
        assertEquals(0, mount.getPreviewChannel().getChangedBySet().size());
        assertTrue(mount.getPreviewChannel().getId().equals(mount.getChannel().getId() + "-preview"));

        final String previewContainerNodeUUID = session.getNode(previewConfigurationPath)
                .getNode("hst:workspace/hst:containers/testcontainer").getIdentifier();
        secondCtx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, previewContainerNodeUUID);

        // there should be not yet any locks
        Set<String> usersWithLockedContainers = mountResource.getPageComposerContextService().getEditingPreviewChannel().getChangedBySet();
        assertTrue(usersWithLockedContainers.isEmpty());

        final ContainerComponentResource containerComponentResource = createContainerComponentResource(mountResource);
        containerComponentResource.createContainerItem(catalogItemUUID, System.currentTimeMillis());

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);
        final MockHttpServletRequest thirdRequest = new MockHttpServletRequest();
        final HstRequestContext thirdCtx = getRequestContextWithResolvedSiteMapItemAndContainerURL(thirdRequest, "localhost", "/home");
        ((HstMutableRequestContext) thirdCtx).setSession(session);

        final String thirdMountIdentifier = thirdCtx.getResolvedMount().getMount().getIdentifier();

        setMountIdOnHttpSession(thirdRequest, thirdMountIdentifier);
        thirdCtx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, thirdMountIdentifier);

        usersWithLockedContainers = mountResource.getPageComposerContextService().getEditingPreviewChannel().getChangedBySet();
        assertTrue(usersWithLockedContainers.contains("admin"));

        mountResource.publish();

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);
        final MockHttpServletRequest fourthRequest = new MockHttpServletRequest();
        final HstRequestContext fourthCtx = getRequestContextWithResolvedSiteMapItemAndContainerURL(fourthRequest, "localhost", "/home");
        ((HstMutableRequestContext) fourthCtx).setSession(session);

        final String fourthMountIdentifier = fourthCtx.getResolvedMount().getMount().getIdentifier();

        setMountIdOnHttpSession(fourthRequest, fourthMountIdentifier);
        fourthCtx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, fourthMountIdentifier);

        // there should be no locks
        usersWithLockedContainers = mountResource.getPageComposerContextService().getEditingPreviewChannel().getChangedBySet();
        assertTrue(usersWithLockedContainers.isEmpty());

    }

    private ContainerComponentResource createContainerComponentResource(MountResource mountResource) {
        ContainerComponentResource ccr = new ContainerComponentResource();
        ccr.setPageComposerContextService(mountResource.getPageComposerContextService());
        return ccr;
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
        addReferencedContainerToHomePage();
        String catalogItemUUID = addCatalogItem();

        session.move("/hst:hst/hst:configurations/unittestproject", "/hst:hst/hst:configurations/7_8");
        // change default unittestproject site to map to /hst:hst/hst:configurations/7_8
        Node testSideNode = session.getNode("/hst:hst/hst:sites/unittestproject");
        testSideNode.setProperty("hst:configurationpath", "/hst:hst/hst:configurations/7_8");

        session.save();
        // give time for jcr events to evict model
        Thread.sleep(200);

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL(request, "localhost", "/home");

        assertEquals("/hst:hst/hst:configurations/7_8", ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath());

        final String mountId = ctx.getResolvedMount().getMount().getIdentifier();
        ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, mountId);
        setMountIdOnHttpSession(request, mountId);

        final String previewConfigurationPath = ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath() + "-preview";
        assertFalse("Preview config node should not exist yet.",
                session.nodeExists(previewConfigurationPath));

        ((HstMutableRequestContext) ctx).setSession(session);

        MountResource mountResource = createResource();
        mountResource.startEdit();

        assertTrue("Live config node should exist",
                session.nodeExists(ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath()));
        assertTrue("Preview config node should exist",
                session.nodeExists(previewConfigurationPath));


        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        final MockHttpServletRequest secondRequest = new MockHttpServletRequest();
        final HstRequestContext secondCtx = getRequestContextWithResolvedSiteMapItemAndContainerURL(secondRequest, "localhost", "/home");
        ((HstMutableRequestContext) secondCtx).setSession(session);

        final String secondMountId = secondCtx.getResolvedMount().getMount().getIdentifier();
        ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, secondMountId);
        setMountIdOnHttpSession(secondRequest, secondMountId);


        final String previewContainerNodeUUID = session.getNode(previewConfigurationPath)
                .getNode("hst:workspace/hst:containers/testcontainer").getIdentifier();

        secondCtx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, previewContainerNodeUUID);

        // there should be not yet any locks
        Set<String> usersWithLockedContainers = mountResource.getPageComposerContextService().getEditingPreviewChannel().getChangedBySet();
        assertTrue(usersWithLockedContainers.isEmpty());

        final ContainerComponentResource containerComponentResource = createContainerComponentResource(mountResource);
        containerComponentResource.createContainerItem(catalogItemUUID, System.currentTimeMillis());

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);
        final MockHttpServletRequest thirdRequest = new MockHttpServletRequest();
        final HstRequestContext thirdCtx = getRequestContextWithResolvedSiteMapItemAndContainerURL(thirdRequest, "localhost", "/home");
        ((HstMutableRequestContext) thirdCtx).setSession(session);

        final String thirdMountId = thirdCtx.getResolvedMount().getMount().getIdentifier();
        ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, thirdMountId);
        setMountIdOnHttpSession(thirdRequest, thirdMountId);
        thirdCtx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, thirdCtx.getResolvedMount().getMount().getIdentifier());

        usersWithLockedContainers = mountResource.getPageComposerContextService().getEditingPreviewChannel().getChangedBySet();
        assertTrue(usersWithLockedContainers.contains("admin"));

        mountResource.publish();

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        final MockHttpServletRequest fourthRequest = new MockHttpServletRequest();
        final HstRequestContext fourthCtx = getRequestContextWithResolvedSiteMapItemAndContainerURL(fourthRequest, "localhost", "/home");
        ((HstMutableRequestContext) fourthCtx).setSession(session);

        final String fourthMountId = fourthCtx.getResolvedMount().getMount().getIdentifier();
        ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, fourthMountId);
        setMountIdOnHttpSession(fourthRequest, fourthMountId);
        fourthCtx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, fourthCtx.getResolvedMount().getMount().getIdentifier());
        usersWithLockedContainers = mountResource.getPageComposerContextService().getEditingPreviewChannel().getChangedBySet();
        assertTrue(usersWithLockedContainers.isEmpty());

    }

}
