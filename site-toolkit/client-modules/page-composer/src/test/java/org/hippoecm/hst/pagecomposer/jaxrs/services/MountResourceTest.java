/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.Arrays;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractPageComposerTest;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.repository.api.HippoSession;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MountResourceTest extends AbstractPageComposerTest {

    @Test
    public void testEditAndPublishMount() throws Exception {

        try (CommonHstConfigSetup setup = new CommonHstConfigSetup()) {
            Session session = setup.session;
            final Node unitTestConfigNode = session.getNode("/hst:hst/hst:configurations/unittestproject");
            final Node workspace = unitTestConfigNode.addNode("hst:workspace", "hst:workspace");
            final Node containers = workspace.addNode("hst:containers", "hst:containercomponentfolder");

            final Node containerNode = containers.addNode("testcontainer", "hst:containercomponent");
            containerNode.setProperty("hst:xtype", "HST.vBox");

            final Node catalog = unitTestConfigNode.addNode("hst:catalog", "hst:catalog");
            final Node catalogPackage = catalog.addNode("testpackage", "hst:containeritempackage");
            final Node catalogItem = catalogPackage.addNode("testitem", "hst:containeritemcomponent");
            catalogItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_TEMPLATE, "foo");
            catalogItem.setProperty("hst:xtype", "HST.Item");

            final String catalogItemUUID = catalogItem.getIdentifier();
            session.save();
            // give time for jcr events to evict model
            Thread.sleep(200);

            final MockHttpServletRequest request = new MockHttpServletRequest();
            final HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL(request, "localhost", "/home");

            ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, ctx.getResolvedMount().getMount().getIdentifier());

            final String previewConfigurationPath = ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath() + "-preview";
            assertFalse("Preview config node should not exist yet.",
                    session.nodeExists(previewConfigurationPath));

            ((HstMutableRequestContext) ctx).setSession(setup.session);

            MountResource mountResource = new MountResource();
            mountResource.startEdit(request);

            assertTrue("Live config node should exist",
                    session.nodeExists(ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath()));
            assertTrue("Preview config node should exist",
                    session.nodeExists(previewConfigurationPath));


            assertTrue("Live channel path node should exist",
                    session.nodeExists(ctx.getResolvedMount().getMount().getChannelPath()));
            assertTrue("Preview channel path node should exist",
                    session.nodeExists(ctx.getResolvedMount().getMount().getChannelPath()+ "-preview"));

            Set<String> usersWithLockedContainers = mountResource.findUsersWithLockedContainers((HippoSession) setup.session, previewConfigurationPath);
            assertTrue(usersWithLockedContainers.isEmpty());

            // reload model through new request, and then modify a container
            // give time for jcr events to evict model
            Thread.sleep(200);
            final MockHttpServletRequest secondRequest = new MockHttpServletRequest();
            final  HstRequestContext secondCtx = getRequestContextWithResolvedSiteMapItemAndContainerURL(secondRequest, "localhost", "/home");
            ((HstMutableRequestContext) secondCtx).setSession(session);

            final ContextualizableMount mount =  (ContextualizableMount)secondCtx.getResolvedMount().getMount();
            assertTrue(mount.getPreviewHstSite().getConfigurationPath().equals(mount.getHstSite().getConfigurationPath() + "-preview"));
            assertTrue(mount.getPreviewChannel().getHstConfigPath().equals(mount.getPreviewHstSite().getConfigurationPath()));
            assertEquals(0, mount.getPreviewChannel().getChangedBySet().size());
            assertTrue(mount.getPreviewChannel().getId().equals(mount.getChannel().getId()+"-preview"));

            final String previewContainerNodeUUID = session.getNode(previewConfigurationPath)
                    .getNode("hst:workspace/hst:containers/testcontainer").getIdentifier();
            secondCtx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, previewContainerNodeUUID);

            final ContainerComponentResource containerComponentResource = new ContainerComponentResource();
            containerComponentResource.createContainerItem(secondRequest, catalogItemUUID, System.currentTimeMillis());

            usersWithLockedContainers = mountResource.findUsersWithLockedContainers((HippoSession) session, previewConfigurationPath);
            assertTrue(usersWithLockedContainers.contains("admin"));


            // reload model through new request, and then modify a container
            // give time for jcr events to evict model
            Thread.sleep(200);
            final MockHttpServletRequest thirdRequest = new MockHttpServletRequest();
            final  HstRequestContext thirdCtx = getRequestContextWithResolvedSiteMapItemAndContainerURL(thirdRequest, "localhost", "/home");
            ((HstMutableRequestContext) thirdCtx).setSession(session);

            thirdCtx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, thirdCtx.getResolvedMount().getMount().getIdentifier());

            mountResource.publish(thirdRequest);

            usersWithLockedContainers = mountResource.findUsersWithLockedContainers((HippoSession) session, previewConfigurationPath);
            assertTrue(usersWithLockedContainers.isEmpty());
        }
    }


    @Test
    public void testXpathQueries(){
        assertEquals("/jcr:root/hst:hst/hst:configurations/myproject-preview//element(*,hst:containercomponent)[@hst:lockedby != '']",
                MountResource.buildXPathQueryToFindLockedContainersForUsers("/hst:hst/hst:configurations/myproject-preview"));
        assertEquals("/jcr:root/hst:hst/hst:configurations/_x0037__8-preview//element(*,hst:containercomponent)[@hst:lockedby != '']",
                MountResource.buildXPathQueryToFindLockedContainersForUsers("/hst:hst/hst:configurations/7_8-preview"));

        assertEquals("/jcr:root/hst:hst/hst:configurations/myproject-preview/*[@hst:lockedby != '']",
                MountResource.buildXPathQueryToFindLockedMainConfigNodesForUsers("/hst:hst/hst:configurations/myproject-preview"));
        assertEquals("/jcr:root/hst:hst/hst:configurations/_x0037__8-preview/*[@hst:lockedby != '']",
                MountResource.buildXPathQueryToFindLockedMainConfigNodesForUsers("/hst:hst/hst:configurations/7_8-preview"));

        assertEquals("/jcr:root/hst:hst/hst:configurations/myproject-preview//element(*,hst:containercomponent)[@hst:lockedby = 'admin' or @hst:lockedby = 'editor']",
                MountResource.buildXPathQueryToFindContainersForUsers("/hst:hst/hst:configurations/myproject-preview", Arrays.asList(new String[]{"admin","editor"})));
        assertEquals("/jcr:root/hst:hst/hst:configurations/_x0037__8-preview//element(*,hst:containercomponent)[@hst:lockedby = 'admin' or @hst:lockedby = 'editor']",
                MountResource.buildXPathQueryToFindContainersForUsers("/hst:hst/hst:configurations/7_8-preview", Arrays.asList(new String[]{"admin","editor"})));

        assertEquals("/jcr:root/hst:hst/hst:configurations/myproject-preview/*[@hst:lockedby = 'admin' or @hst:lockedby = 'editor']",
                MountResource.buildXPathQueryToFindMainfConfigNodesForUsers("/hst:hst/hst:configurations/myproject-preview", Arrays.asList(new String[]{"admin","editor"})));
        assertEquals("/jcr:root/hst:hst/hst:configurations/_x0037__8-preview/*[@hst:lockedby = 'admin' or @hst:lockedby = 'editor']",
                MountResource.buildXPathQueryToFindMainfConfigNodesForUsers("/hst:hst/hst:configurations/7_8-preview", Arrays.asList(new String[]{"admin","editor"})));
    }


    @Test
    public void testEditAndPublishProjectThatStartsWithNumber() throws Exception {

        try (CommonHstConfigSetup setup = new CommonHstConfigSetup()) {
            Session session = setup.session;

            session.move("/hst:hst/hst:configurations/unittestproject", "/hst:hst/hst:configurations/7_8");
            final Node unitTestConfigNode = session.getNode("/hst:hst/hst:configurations/7_8");

            final Node workspace = unitTestConfigNode.addNode("hst:workspace", "hst:workspace");
            final Node containers = workspace.addNode("hst:containers", "hst:containercomponentfolder");

            final Node containerNode = containers.addNode("testcontainer", "hst:containercomponent");
            containerNode.setProperty("hst:xtype", "HST.vBox");

            final Node catalog = unitTestConfigNode.addNode("hst:catalog", "hst:catalog");
            final Node catalogPackage = catalog.addNode("testpackage", "hst:containeritempackage");
            final Node catalogItem = catalogPackage.addNode("testitem", "hst:containeritemcomponent");
            catalogItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_TEMPLATE, "foo");
            catalogItem.setProperty("hst:xtype", "HST.Item");
            final String catalogItemUUID = catalogItem.getIdentifier();

            // change default unittestproject site to map to /hst:hst/hst:configurations/7_8
            Node testSideNode = session.getNode("/hst:hst/hst:sites/unittestproject");
            testSideNode.setProperty("hst:configurationpath", "/hst:hst/hst:configurations/7_8");

            setup.session.save();
            // give time for jcr events to evict model
            Thread.sleep(200);

            final MockHttpServletRequest request = new MockHttpServletRequest();
            final HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL(request, "localhost", "/home");

            assertEquals("/hst:hst/hst:configurations/7_8", ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath());

            ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, ctx.getResolvedMount().getMount().getIdentifier());

            final String previewConfigurationPath = ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath() + "-preview";
            assertFalse("Preview config node should not exist yet.",
                    setup.session.nodeExists(previewConfigurationPath));

            ((HstMutableRequestContext) ctx).setSession(setup.session);

            MountResource mountResource = new MountResource();
            mountResource.startEdit(request);

            assertTrue("Live config node should exist",
                    setup.session.nodeExists(ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath()));
            assertTrue("Preview config node should exist",
                    setup.session.nodeExists(previewConfigurationPath));

            Set<String> usersWithLockedContainers = mountResource.findUsersWithLockedContainers((HippoSession) setup.session, previewConfigurationPath);
            assertTrue(usersWithLockedContainers.isEmpty());

            // reload model through new request, and then modify a container
            // give time for jcr events to evict model
            Thread.sleep(200);
            final MockHttpServletRequest secondRequest = new MockHttpServletRequest();
            final  HstRequestContext secondCtx = getRequestContextWithResolvedSiteMapItemAndContainerURL(secondRequest, "localhost", "/home");
            ((HstMutableRequestContext) secondCtx).setSession(setup.session);

            final String previewContainerNodeUUID = setup.session.getNode(previewConfigurationPath)
                    .getNode("hst:workspace/hst:containers/testcontainer").getIdentifier();
            secondCtx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, previewContainerNodeUUID);

            final ContainerComponentResource containerComponentResource = new ContainerComponentResource();
            containerComponentResource.createContainerItem(secondRequest, catalogItemUUID, System.currentTimeMillis());

            usersWithLockedContainers = mountResource.findUsersWithLockedContainers((HippoSession) setup.session, previewConfigurationPath);
            assertTrue(usersWithLockedContainers.contains("admin"));


            // reload model through new request, and then modify a container
            // give time for jcr events to evict model
            Thread.sleep(200);
            final MockHttpServletRequest thirdRequest = new MockHttpServletRequest();
            final  HstRequestContext thirdCtx = getRequestContextWithResolvedSiteMapItemAndContainerURL(thirdRequest, "localhost", "/home");
            ((HstMutableRequestContext) thirdCtx).setSession(setup.session);

            thirdCtx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, thirdCtx.getResolvedMount().getMount().getIdentifier());

            mountResource.publish(thirdRequest);

            usersWithLockedContainers = mountResource.findUsersWithLockedContainers((HippoSession) setup.session, previewConfigurationPath);
            assertTrue(usersWithLockedContainers.isEmpty());

        }

    }


}
