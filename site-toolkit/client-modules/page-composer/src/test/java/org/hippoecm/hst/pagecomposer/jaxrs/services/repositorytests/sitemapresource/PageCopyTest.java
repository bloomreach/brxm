/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.sitemapresource;


import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.junit.Before;
import org.junit.Test;

import static javax.ws.rs.core.Response.Status.OK;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_SITEMAP;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PageCopyTest extends AbstractSiteMapResourceTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        createPreviewWithSiteMapWorkspace("localhost", "/subsite");
        session.getNode("/hst:hst/hst:configurations/unittestsubproject").addNode(NODENAME_HST_WORKSPACE);
        session.getNode("/hst:hst/hst:configurations/unittestsubproject-preview").addNode(NODENAME_HST_WORKSPACE);
        session.save();
        // time for jcr events to arrive
        Thread.sleep(100);
    }

    @Test
    public void assert_setup_configuration_fixture() throws Exception {
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject/" + NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_SITEMAP + "/home"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/" + NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_SITEMAP + "/home"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/" + NODENAME_HST_WORKSPACE));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/" + NODENAME_HST_WORKSPACE));
    }

    @Test
    public void page_copy_to_root_item_within_same_channel() throws Exception {
        copyHomePageWithinSameChannel(true, "copiedHome", null);
    }

    private void copyHomePageWithinSameChannel(final boolean publish, final String copyName, final SiteMapItemRepresentation targetParent) throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "localhost", "/home");
        SiteMapResource siteMapResource = createResource();
        final Mount editingMount = mountResource.getPageComposerContextService().getEditingMount();
        final Response copy = siteMapResource.copy(editingMount.getIdentifier(), home.getId(), (targetParent == null ? null : targetParent.getId()), copyName);
        assertEquals(OK.getStatusCode(), copy.getStatus());

        final String pathInfo;
        if (targetParent == null) {
            pathInfo = copyName;
        } else if (targetParent.getIsHomePage()){
            pathInfo = targetParent.getName() + "/" + copyName;
        } else {
            pathInfo = targetParent.getPathInfo() + "/" + copyName;
        }

        final String pageName = pathInfo.replaceAll("/", "-");
        final String previewSiteMapItemNodePath = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/" + pathInfo;
        final String previewPageNodePath = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/" + pageName;
        final String liveSiteMapItemNodePath = "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/" + pathInfo;
        final String livePageNodePath = "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/" + pageName;
        assertTrue(session.nodeExists(previewSiteMapItemNodePath));
        assertTrue(session.nodeExists(previewPageNodePath));
        assertFalse(session.nodeExists(liveSiteMapItemNodePath));
        assertFalse(session.nodeExists(livePageNodePath));
        assertEquals(session.getNode(previewSiteMapItemNodePath).getProperty(GENERAL_PROPERTY_LOCKED_BY).getString(), "admin");
        assertEquals(session.getNode(previewPageNodePath).getProperty(GENERAL_PROPERTY_LOCKED_BY).getString(), "admin");

        // no templates copied since within same channel, so templates are not locked (templates are inherited from common any way
        assertFalse(session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:templates").hasProperty(GENERAL_PROPERTY_LOCKED_BY));
        if (publish) {
            mountResource.publish();
            assertTrue(session.nodeExists(previewSiteMapItemNodePath));
            assertTrue(session.nodeExists(previewPageNodePath));
            assertTrue(session.nodeExists(liveSiteMapItemNodePath));
            assertTrue(session.nodeExists(livePageNodePath));
            assertFalse(session.getNode(previewSiteMapItemNodePath).hasProperty(GENERAL_PROPERTY_LOCKED_BY));
            assertFalse(session.getNode(previewPageNodePath).hasProperty(GENERAL_PROPERTY_LOCKED_BY));
        }
    }

    @Test
    public void page_copy_to_parent_item_within_same_channel() throws Exception {
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "localhost", "/news");
        copyHomePageWithinSameChannel(true, "copiedHome", news);
    }

    @Test
    public void page_copy_to_itself_within_same_channel() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "localhost", "/home");
        copyHomePageWithinSameChannel(true, "copiedHome", home);
    }

    @Test
    public void non_workspace_page_can_be_copied() throws Exception {
        // move homepage out of 'hst:workspace config
        moveHomePageOutWorkspace();
        session.save();
        Thread.sleep(100);
        copyHomePageWithinSameChannel(true, "copiedHome", null);
    }

    private void moveHomePageOutWorkspace() throws RepositoryException {
        session.move("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/home",
                "/hst:hst/hst:configurations/unittestproject/hst:sitemap/home");
        session.move("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/home",
                "/hst:hst/hst:configurations/unittestproject-preview/hst:sitemap/home");
        session.move("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/homepage",
                "/hst:hst/hst:configurations/unittestproject/hst:pages/homepage");
        session.move("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/homepage",
                "/hst:hst/hst:configurations/unittestproject-preview/hst:pages/homepage");
    }


    @Test
    public void non_workspace_page_copy_with_reference_components_get_denormalized() throws Exception {
        moveHomePageOutWorkspace();

        final Node previewWorkspace = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace");
        final Node liveWorkspace = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace");
        final Node[] workspaces = {previewWorkspace, liveWorkspace};
        for (Node workspace : workspaces) {
            final Node containers = workspace.addNode("hst:containers", "hst:containercomponentfolder");
            final Node containerNode = containers.addNode("testcontainer", "hst:containercomponent");
            containerNode.setProperty("hst:xtype", "HST.vBox");
            final Node containerItem = containerNode.addNode("item", HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
            containerItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.Item");
            final Node homePageContainer = session.getNode(workspace.getParent().getPath() + "/hst:pages/homepage")
                    .addNode("container", "hst:containercomponentreference");
            homePageContainer.setProperty("hst:referencecomponent", "testcontainer");
        }
        session.save();
        Thread.sleep(100);
        copyHomePageWithinSameChannel(false, "copiedHome", null);

        // assert denormalization of the hst:containercomponentreference 'container' which now should have 'item' as child node
        // instead of a pointer to 'hst:workspace/hst:containers/testcontainer'.
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/copiedHome/container/item"));

        // assert the container is locked
        assertEquals(session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/copiedHome/container")
                .getProperty(GENERAL_PROPERTY_LOCKED_BY).getString(), "admin");

        mountResource.publish();
        Thread.sleep(100);
        assertFalse(session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/copiedHome/container")
                .hasProperty(GENERAL_PROPERTY_LOCKED_BY));

    }

    @Test
    public void page_copy_guava_event() throws Exception {
        // TODO
    }

    @Test
    public void page_copy_guava_event_short_circuiting() throws Exception {
        // TODO
    }

    @Test
    public void page_copy_already_locked() throws Exception {

    }
}
