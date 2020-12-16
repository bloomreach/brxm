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

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEventListenerRegistry;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageDeleteContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageDeleteEvent;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.eventbus.Subscribe;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hippoecm.hst.configuration.HstNodeTypes.EDITABLE_PROPERTY_STATE;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError.INVALID_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PageDeleteEventTest extends AbstractSiteMapResourceTest {

    protected SiteMapItemRepresentation homeRepresentation;
    protected HstSiteMapItem homeSiteMapItem;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        createPreviewWithSiteMapWorkspace("localhost", "/subsite");
        session.save();
        // time for jcr events to arrive
        Thread.sleep(100);
    }

    private void initContext() throws Exception {
        final HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        ((HstMutableRequestContext) ctx).setSession(session);
        final PageComposerContextService pageComposerContextService = mountResource.getPageComposerContextService();
        final HstSite site = pageComposerContextService.getEditingPreviewSite();

        homeSiteMapItem = ctx.getResolvedSiteMapItem().getHstSiteMapItem();

        // override the config identifier to have sitemap id
        ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, ((CanonicalInfo) site.getSiteMap()).getCanonicalIdentifier());

        assertNotNull(homeSiteMapItem);

        Response response = createResource().getSiteMapItem(((CanonicalInfo) homeSiteMapItem).getCanonicalIdentifier());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        homeRepresentation = (SiteMapItemRepresentation) ((ResponseRepresentation) response.getEntity()).getData();
    }


    public class PageDeleteEventListener {

        protected PageDeleteEvent receivedEvent;

        public void init() {
            ChannelEventListenerRegistry.get().register(this);
        }

        public void destroy() {
            ChannelEventListenerRegistry.get().unregister(this);
        }

        @Subscribe
        public void onPageDeleteEvent(PageDeleteEvent event) {
            if (event.getException() != null) {
                return;
            }
            final PageDeleteContext pageDeleteContext = event.getPageActionContext();

            assertEquals("Deleted sitemap item is not home",
                    getPreviewConfigurationWorkspaceSitemapPath() + "/home", pageDeleteContext.getSourceSiteMapPath());
            assertEquals("Deleted sitemap item object does not equal home sitemapitem object",
                    homeSiteMapItem, pageDeleteContext.getSourceSiteMapItem());

            try {
                assertEquals("Deleted sitemap item has not been marked for deletion",
                        "deleted", session.getNode(pageDeleteContext.getSourceSiteMapPath()).getProperty(EDITABLE_PROPERTY_STATE).getString());
            } catch (Exception e) {
                event.setException(new RuntimeException("Exception while accessing " + pageDeleteContext.getSourceSiteMapPath()));
            }

            receivedEvent = event;
        }
    }

    public class FailingPageDeleteEventListener extends PageDeleteEventListener {
        final RuntimeException exception;

        public FailingPageDeleteEventListener(final RuntimeException exception) {
            super();
            this.exception = exception;
        }

        @Override
        public void onPageDeleteEvent(final PageDeleteEvent event) {
            event.setException(exception);
            super.onPageDeleteEvent(event);
        }
    }


    @Test
    public void test_delete_guava_event() throws Exception {
        initContext();
        PageDeleteEventListener pageDeleteEventListener = new PageDeleteEventListener();
        try {
            pageDeleteEventListener.init();

            deleteHomePage(true);

            final PageDeleteEvent receivedEvent = pageDeleteEventListener.receivedEvent;
            assertNotNull(receivedEvent);
            assertNull(receivedEvent.getException());
            final PageDeleteContext pageDeleteContext = receivedEvent.getPageActionContext();
            assertNotNull(pageDeleteContext);

            final String previewSiteMapItemNodePath = getPreviewConfigurationWorkspaceSitemapPath() + "/home";

            assertEquals("Deleted sitemapitem path doesn't match the path in page delete context",
                    previewSiteMapItemNodePath, pageDeleteContext.getSourceSiteMapPath());

        } finally {
            pageDeleteEventListener.destroy();
        }

    }


    @Test
    public void page_delete_guava_event_short_circuiting_with_runtime_exception() throws Exception {
        FailingPageDeleteEventListener failingPageCreateEventListener = new FailingPageDeleteEventListener(new RuntimeException());
        try {
            failingPageCreateEventListener.init();
            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "localhost", "/home");
            SiteMapResource siteMapResource = createResource();

            final Response response = siteMapResource.delete(home.getId());
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

            final String previewSiteMapItemNodePath = getPreviewConfigurationWorkspaceSitemapPath() + "/home";
            final String previewPageNodePath = getPreviewConfigurationWorkspacePagesPath() + "/homepage";

            // FailingPageDeleteEventListener should have short circuited the entire delete and also must have removed the session changes
            assertTrue(session.nodeExists(previewSiteMapItemNodePath));
            assertTrue(session.nodeExists(previewPageNodePath));
            assertFalse(session.hasPendingChanges());
        } finally {
            failingPageCreateEventListener.destroy();
        }
    }


    @Test
    public void page_delete_guava_event_short_circuiting_with_client_exception() throws Exception {
        FailingPageDeleteEventListener failingPageCreateEventListener = new FailingPageDeleteEventListener(new ClientException("client exception", INVALID_NAME));
        try {
            failingPageCreateEventListener.init();
            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "localhost", "/home");
            SiteMapResource siteMapResource = createResource();

            final Response response = siteMapResource.delete(home.getId());
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    BAD_REQUEST.getStatusCode(), response.getStatus());

            final String previewSiteMapItemNodePath = getPreviewConfigurationWorkspaceSitemapPath() + "/home";
            final String previewPageNodePath = getPreviewConfigurationWorkspacePagesPath() + "/homepage";

            // FailingPageDeleteEventListener should have short circuited the entire delete and also must have removed the session changes
            assertTrue(session.nodeExists(previewSiteMapItemNodePath));
            assertTrue(session.nodeExists(previewPageNodePath));
            assertFalse(session.hasPendingChanges());
        } finally {
            failingPageCreateEventListener.destroy();
        }
    }

    private void deleteHomePage(boolean publish) throws Exception {
        SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.delete(homeRepresentation.getId());
        assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                OK.getStatusCode(), response.getStatus());

        final String previewSiteMapItemNodePath = getPreviewConfigurationWorkspaceSitemapPath() + "/home";
        final String previewPageNodePath = getPreviewConfigurationWorkspacePagesPath() + "/homepage";
        final String liveSiteMapItemNodePath = getLiveConfigurationWorkspaceSitemapPath() + "/home";
        final String livePageNodePath = getLiveConfigurationWorkspacePagesPath() + "/homepage";
        assertTrue(session.nodeExists(previewSiteMapItemNodePath));
        assertTrue(session.nodeExists(previewPageNodePath));

        final HstComponentConfiguration componentConfiguration = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(homeRepresentation.getComponentConfigurationId());

        assertEquals("deleted",
                session.getNode(componentConfiguration.getCanonicalStoredLocation()).getProperty(EDITABLE_PROPERTY_STATE).getString());

        assertEquals("admin", session.getNode(previewSiteMapItemNodePath).getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
        assertEquals("admin", session.getNode(previewPageNodePath).getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());

        if (publish) {
            mountResource.publish();
            assertFalse(session.nodeExists(previewSiteMapItemNodePath));
            assertFalse(session.nodeExists(previewPageNodePath));
            assertFalse(session.nodeExists(liveSiteMapItemNodePath));
            assertFalse(session.nodeExists(livePageNodePath));
        }
    }

}
