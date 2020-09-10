/*
 * Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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

import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEventListenerRegistry;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageCopyContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageCopyEvent;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.eventbus.Subscribe;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError.INVALID_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PageCopyEventTest extends AbstractSiteMapResourceTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        createPreviewWithSiteMapWorkspace("localhost", "/subsite");
        session.save();
        // time for jcr events to arrive
        Thread.sleep(100);
    }


    public class PageCopyEventListener {

        protected PageCopyEvent receivedEvent;

        public void init() {
            ChannelEventListenerRegistry.get().register(this);
        }

        public void destroy() {
            ChannelEventListenerRegistry.get().unregister(this);
        }

        @Subscribe
        public void onPageCopyEvent(PageCopyEvent event) {
            if (event.getException() != null) {
                return;
            }
            final PageCopyContext pageCopyContext = event.getPageActionContext();

            try {
                assertEquals("Copied sitemap item should be locked by admin", "admin",
                        pageCopyContext.getNewSiteMapItemNode().getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
                assertEquals("Copied page should be locked by admin", "admin",
                        pageCopyContext.getNewPageNode().getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
            } catch (Exception e) {
                event.setException(new RuntimeException(e));
            }

            receivedEvent = event;
        }
    }

    public class FailingPageCopyEventListener extends PageCopyEventListener {
        final RuntimeException exception;

        public FailingPageCopyEventListener(final RuntimeException exception) {
            super();
            this.exception = exception;
        }

        @Override
        public void onPageCopyEvent(final PageCopyEvent event) {
            event.setException(exception);
            super.onPageCopyEvent(event);
        }
    }

    @Test
    public void page_copy_guava_event() throws Exception {
        PageCopyEventListener pageCopyEventListener = new PageCopyEventListener();
        try {
            pageCopyEventListener.init();
            copyHomePageWithinSameChannel(true, "copiedHome", null);
            final PageCopyEvent pce = pageCopyEventListener.receivedEvent;
            assertNotNull(pce);
            assertNull(pce.getException());
            final PageCopyContext pcc = pce.getPageActionContext();
            assertNotNull(pcc);

            final String previewSiteMapItemNodePath = getPreviewConfigurationWorkspaceSitemapPath() + "/copiedHome";
            final String previewPageNodePath = getPreviewConfigurationWorkspacePagesPath() + "/copiedHome";
            final String liveSiteMapItemNodePath = getLiveConfigurationWorkspaceSitemapPath() + "/copiedHome";
            final String livePageNodePath = getLiveConfigurationWorkspacePagesPath() + "/copiedHome";

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

    @Test
    public void page_copy_guava_event_short_circuiting_with_runtime_exception() throws Exception {
        FailingPageCopyEventListener failingCopyEventListener = new FailingPageCopyEventListener(new RuntimeException());
        try {
            failingCopyEventListener.init();
            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "localhost", "/home");
            SiteMapResource siteMapResource = createResource();
            final Mount editingMount = mountResource.getPageComposerContextService().getEditingMount();
            final Response response = siteMapResource.copy(editingMount.getIdentifier(), home.getId(), null, "copiedHome");
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

            final String previewSiteMapItemNodePath = getPreviewConfigurationWorkspaceSitemapPath() + "/copiedHome";
            final String previewPageNodePath = getPreviewConfigurationWorkspacePagesPath() + "/copiedHome";

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
        FailingPageCopyEventListener failingCopyEventListener = new FailingPageCopyEventListener(new ClientException("client exception", INVALID_NAME));
        try {
            failingCopyEventListener.init();
            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "localhost", "/home");
            SiteMapResource siteMapResource = createResource();
            final Mount editingMount = mountResource.getPageComposerContextService().getEditingMount();
            final Response response = siteMapResource.copy(editingMount.getIdentifier(), home.getId(), null, "copiedHome");
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    BAD_REQUEST.getStatusCode(), response.getStatus());

            final String previewSiteMapItemNodePath = getPreviewConfigurationWorkspaceSitemapPath() + "copiedHome";
            final String previewPageNodePath = getPreviewConfigurationWorkspacePagesPath() + "/copiedHome";

            // FailingPageCopyEventListener should have short circuited the entire copy and also must have removed the session changes
            assertFalse(session.nodeExists(previewSiteMapItemNodePath));
            assertFalse(session.nodeExists(previewPageNodePath));
            assertFalse(session.hasPendingChanges());
        } finally {
            failingCopyEventListener.destroy();
        }
    }

    private void copyHomePageWithinSameChannel(final boolean publish, final String copyName, final SiteMapItemRepresentation targetParent) throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "localhost", "/home");
        SiteMapResource siteMapResource = createResource();
        final Mount editingMount = mountResource.getPageComposerContextService().getEditingMount();
        final Response response = siteMapResource.copy(editingMount.getIdentifier(), home.getId(), (targetParent == null ? null : targetParent.getId()), copyName);
        assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                OK.getStatusCode(), response.getStatus());

        final String pathInfo;
        if (targetParent == null) {
            pathInfo = copyName;
        } else if (targetParent.getIsHomePage()) {
            pathInfo = targetParent.getName() + "/" + copyName;
        } else {
            pathInfo = targetParent.getPathInfo() + "/" + copyName;
        }

        final String pageName = pathInfo.replaceAll("/", "-");
        final String previewSiteMapItemNodePath = getPreviewConfigurationWorkspaceSitemapPath() + "/" + pathInfo;
        final String previewPageNodePath = getPreviewConfigurationWorkspacePagesPath() + "/" + pageName;
        final String liveSiteMapItemNodePath = getLiveConfigurationWorkspaceSitemapPath() + "/" + pathInfo;
        final String livePageNodePath = getLiveConfigurationWorkspacePagesPath() + "/" + pageName;
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

}
