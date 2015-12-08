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

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageCopyContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageCopyEvent;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.junit.Before;
import org.junit.Test;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_PAGES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_SITEMAP;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_REF_ID;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError.INVALID_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
        assertEquals("admin", session.getNode(previewSiteMapItemNodePath).getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
        assertEquals("admin", session.getNode(previewPageNodePath).getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());

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
    public void page_copy_does_not_copy_refId() throws Exception {
        copyHomePageWithinSameChannel(true, "copiedHome", null);
        assertEquals("homeRefId", session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/home").getProperty(SITEMAPITEM_PROPERTY_REF_ID).getString());
        assertFalse(session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/copiedHome").hasProperty(SITEMAPITEM_PROPERTY_REF_ID));
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
        assertEquals("admin", session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/copiedHome/container")
                .getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());

        mountResource.publish();
        Thread.sleep(100);
        assertFalse(session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/copiedHome/container")
                .hasProperty(GENERAL_PROPERTY_LOCKED_BY));

    }

    public static class PageCopyEventListener {

        private ComponentManager componentManager;
        protected PageCopyEvent receivedEvent;

        public void destroy() {
            componentManager.unregisterEventSubscriber(this);
        }

        public PageCopyEventListener(final ComponentManager componentManager) {
            this.componentManager = componentManager;
            componentManager.registerEventSubscriber(this);
        }

        @Subscribe
        @AllowConcurrentEvents
        public void onPageCopyEvent(PageCopyEvent event) {
            if (event.getException() != null) {
                return;
            }
            try {
                final PageCopyContext pageCopyContext = event.getPageCopyContext();
                assertEquals("admin", pageCopyContext.getNewSiteMapItemNode().getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
                assertEquals("admin", pageCopyContext.getNewPageNode().getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
            } catch (Exception e) {
                event.setException(new RuntimeException(e));
            }
            receivedEvent = event;
        }
    }

    @Test
    public void page_copy_guava_event() throws Exception {
        PageCopyEventListener pageCopyEventListener = new PageCopyEventListener(componentManager);
        try {
            copyHomePageWithinSameChannel(true, "copiedHome", null);
            final PageCopyEvent pce = pageCopyEventListener.receivedEvent;
            assertNotNull(pce);
            assertNull(pce.getException());
            final PageCopyContext pcc = pce.getPageCopyContext();
            assertNotNull(pcc);

            final String previewSiteMapItemNodePath = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/copiedHome";
            final String previewPageNodePath = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/copiedHome";
            final String liveSiteMapItemNodePath = "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/copiedHome";
            final String livePageNodePath = "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/copiedHome";

            assertEquals(previewSiteMapItemNodePath, pcc.getNewSiteMapItemNode().getPath());
            assertEquals(previewPageNodePath, pcc.getNewPageNode().getPath());

            // successful publication has been done
            assertTrue(session.nodeExists(liveSiteMapItemNodePath));
            assertTrue(session.nodeExists(livePageNodePath));

            // lock assertions : Assert that during processing event the 'new sitemap node' was locked, but
            // after the publication it now has been unlocked
            assertFalse(session.getNode(previewSiteMapItemNodePath).hasProperty(GENERAL_PROPERTY_LOCKED_BY));
            assertFalse(session.getNode(previewPageNodePath).hasProperty(GENERAL_PROPERTY_LOCKED_BY));

        } finally {
            pageCopyEventListener.destroy();
        }
    }

    public static class FailingPageCopyEventListener extends PageCopyEventListener {
        final RuntimeException exception;
        public FailingPageCopyEventListener(final ComponentManager componentManager, final RuntimeException exception) {
            super(componentManager);
            this.exception = exception;
        }

        @Override
        public void onPageCopyEvent(final PageCopyEvent event) {
            event.setException(exception);
            super.onPageCopyEvent(event);
        }
    }

    @Test
    public void page_copy_guava_event_short_circuiting_with_runtime_exception() throws Exception {
        FailingPageCopyEventListener failingCopyEventListener = new FailingPageCopyEventListener(componentManager, new RuntimeException());
        try {
            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "localhost", "/home");
            SiteMapResource siteMapResource = createResource();
            final Mount editingMount = mountResource.getPageComposerContextService().getEditingMount();
            final Response copy = siteMapResource.copy(editingMount.getIdentifier(), home.getId(), null, "copiedHome");
            assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), copy.getStatus());

            final String previewSiteMapItemNodePath = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/copiedHome";
            final String previewPageNodePath = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/copiedHome";

            // FailingPageCopyEventListener should have short circuited the entire copy and also must have removed the session changes
            assertFalse(session.nodeExists(previewSiteMapItemNodePath));
            assertFalse(session.nodeExists(previewPageNodePath));
            assertFalse(session.hasPendingChanges());
        } finally {
            failingCopyEventListener.destroy();
        }
    }

    @Test
    public void page_copy_guava_event_short_circuiting_with_client_exception() throws Exception {
        FailingPageCopyEventListener failingCopyEventListener = new FailingPageCopyEventListener(componentManager,
                new ClientException("client exception", INVALID_NAME));
        try {
            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "localhost", "/home");
            SiteMapResource siteMapResource = createResource();
            final Mount editingMount = mountResource.getPageComposerContextService().getEditingMount();
            final Response copy = siteMapResource.copy(editingMount.getIdentifier(), home.getId(), null, "copiedHome");
            assertEquals(BAD_REQUEST.getStatusCode(), copy.getStatus());
        } finally {
            failingCopyEventListener.destroy();
        }
    }


    private Mount getTargetMountByAlias(final String alias) {
        final Mount editingMount = mountResource.getPageComposerContextService().getEditingMount();
        final VirtualHost virtualHost = editingMount.getVirtualHost();
        final Mount target = virtualHost.getVirtualHosts().getMountByGroupAliasAndType(virtualHost.getHostGroupName(), alias, "preview");
        return target;
    }

    @Test
    public void page_copy_creates_missing_workspace_config_nodes() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "localhost", "/home");
        SiteMapResource siteMapResource = createResource();
        final Mount targetMount = getTargetMountByAlias("subsite");

        final String previewConfigurationPath = targetMount.getHstSite().getConfigurationPath();
        final String liveConfigurationPath = previewConfigurationPath.replace("-preview/", "/");
        final String[] configPaths = {previewConfigurationPath, liveConfigurationPath};
        for (String configPath : configPaths) {
            assertTrue(session.nodeExists(configPath + "/" + NODENAME_HST_WORKSPACE));
            assertFalse("pages should not yet exist.", session.nodeExists(configPath + "/" + NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_PAGES));
            assertFalse("sitemap should not yet exist.", session.nodeExists(configPath + "/" + NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_SITEMAP));
        }

        final Response copy = siteMapResource.copy(targetMount.getIdentifier(), home.getId(), null, "copy");
        assertEquals(OK.getStatusCode(), copy.getStatus());

        //  assert target channel has now also automatically added 'hst:sitemap' and 'hst:pages' below its workspace
        for (String configPath : configPaths) {
            assertTrue(session.nodeExists(configPath + "/" + NODENAME_HST_WORKSPACE));
            assertTrue("pages should not yet exist.", session.nodeExists(configPath + "/" + NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_PAGES));
            assertTrue("sitemap should not yet exist.", session.nodeExists(configPath + "/" + NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_SITEMAP));
        }

    }

    @Test
    public void page_copy_cross_channel() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "localhost", "/home");
        SiteMapResource siteMapResource = createResource();
        final Mount targetMount = getTargetMountByAlias("subsite");
        final Response copy = siteMapResource.copy(targetMount.getIdentifier(), home.getId(), null, "copy");
        assertEquals(OK.getStatusCode(), copy.getStatus());
        final String newSiteMapItemNodePath = "/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:sitemap/copy";
        final String newPageNodePath = "/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:pages/copy";
        assertTrue(session.nodeExists(newSiteMapItemNodePath));
        assertTrue(session.nodeExists(newPageNodePath));
        assertEquals("admin", session.getNode(newSiteMapItemNodePath).getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
        assertEquals("admin", session.getNode(newPageNodePath).getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
        assertFalse(session.nodeExists(newSiteMapItemNodePath.replace("-preview/", "/")));
        assertFalse(session.nodeExists(newPageNodePath.replace("-preview/", "/")));

        // before we can use 'mountResource' to publish, we first have to 'switch' current request context to 'subsite'
        getSiteMapItemRepresentation(session, "localhost", "/subsite");

        assertEquals("subsite",RequestContextProvider.get().getResolvedMount().getMount().getName());

        mountResource.publish();

        Thread.sleep(100);
        assertTrue(session.nodeExists(newSiteMapItemNodePath.replace("-preview/","/")));
        assertTrue(session.nodeExists(newPageNodePath.replace("-preview/","/")));
        assertFalse(session.getNode(newSiteMapItemNodePath).hasProperty(GENERAL_PROPERTY_LOCKED_BY));
        assertFalse(session.getNode(newPageNodePath).hasProperty(GENERAL_PROPERTY_LOCKED_BY));
    }

    @Test
    public void page_copy_cross_channel_pageNode_already_exists_results_in_counter_added() throws Exception {
        // first add the 'copy' page node already, which is created by xyz
        createPreviewWithSiteMapWorkspace("localhost", "/subsite");
        session.getNode("/hst:hst/hst:configurations/unittestsubproject/hst:workspace").addNode("hst:pages").addNode("copy", HstNodeTypes.NODETYPE_HST_COMPONENT);
        session.getNode("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace").addNode("hst:pages").addNode("copy", HstNodeTypes.NODETYPE_HST_COMPONENT);
        session.save();
        // time for jcr events to arrive
        Thread.sleep(100);

        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "localhost", "/home");
        SiteMapResource siteMapResource = createResource();
        final Mount targetMount = getTargetMountByAlias("subsite");
        final Response copy = siteMapResource.copy(targetMount.getIdentifier(), home.getId(), null, "copy");
        assertEquals(OK.getStatusCode(), copy.getStatus());
        final String newSiteMapItemNodePath = "/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:sitemap/copy";
        final String expectedPageNodePathWithCounter = "/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:pages/copy-1";
        assertTrue(session.nodeExists(newSiteMapItemNodePath));
        assertTrue(session.nodeExists(expectedPageNodePathWithCounter));
        // and assert new sitemap item points to page with counter
        assertEquals("hst:pages/copy-1", session.getNode(newSiteMapItemNodePath).getProperty(SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID).getString());
    }

    @Test
    public void page_copy_cross_channel_already_locked_due_to_other_copy() throws Exception {

    }

}
