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

import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEventListenerRegistry;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageMoveContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageMoveEvent;
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

public class PageMoveEventTest extends AbstractSiteMapResourceTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        createPreviewWithSiteMapWorkspace("localhost", "/subsite");
        session.save();
        // time for jcr events to arrive
        Thread.sleep(100);
    }

    public class PageMoveEventListener {

        protected PageMoveEvent receivedEvent;

        public void init() {
            ChannelEventListenerRegistry.get().register(this);
        }

        public void destroy() {
            ChannelEventListenerRegistry.get().unregister(this);
        }

        @Subscribe
        public void onPageMoveEvent(PageMoveEvent event) {
            if (event.getException() != null) {
                return;
            }
            try {
                final PageMoveContext pageMoveContext = event.getPageActionContext();

                assertEquals("Original parent sitemapitem node is not set correctly",
                        getPreviewConfigurationWorkspaceSitemapPath(), pageMoveContext.getOriginalParentSiteMapNode().getPath());
                assertEquals("Parent sitemapitem node of moved item is not set correctly",
                        getPreviewConfigurationWorkspaceSitemapPath() + "/news", pageMoveContext.getNewParentSiteMapNode().getPath());
                assertEquals("Moved sitemapitem node is not at the expected path",
                        getPreviewConfigurationWorkspaceSitemapPath() + "/news/home", pageMoveContext.getNewSiteMapItemNode().getPath());
                assertEquals("Moved sitemapitem node should be locked by admin",
                        "admin", pageMoveContext.getNewSiteMapItemNode().getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
            } catch (Exception e) {
                event.setException(new RuntimeException(e));
            }
            receivedEvent = event;
        }
    }

    public class FailingPageMoveEventListener extends PageMoveEventListener {
        final RuntimeException exception;

        public FailingPageMoveEventListener(final RuntimeException exception) {
            super();
            this.exception = exception;
        }

        @Override
        public void onPageMoveEvent(final PageMoveEvent event) {
            event.setException(exception);
            super.onPageMoveEvent(event);
        }
    }

    @Test
    public void test_move_guava_event() throws Exception {
        PageMoveEventListener pageMoveEventListener = new PageMoveEventListener();
        try {
            pageMoveEventListener.init();

            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
            final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
            SiteMapResource siteMapResource = createResource();
            final Response response = siteMapResource.move(home.getId(), news.getId());
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    OK.getStatusCode(), response.getStatus());

            final PageMoveEvent pme = pageMoveEventListener.receivedEvent;
            assertNotNull(pme);
            assertNull(pme.getException());
            final PageMoveContext pmc = pme.getPageActionContext();
            assertNotNull(pmc);
        } finally {
            pageMoveEventListener.destroy();
        }
    }

    @Test
    public void page_move_guava_event_short_circuiting_with_runtime_exception() throws Exception {
        FailingPageMoveEventListener failingPageMoveEventListener = new FailingPageMoveEventListener(new RuntimeException());
        try {
            failingPageMoveEventListener.init();

            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
            final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
            SiteMapResource siteMapResource = createResource();
            final Response response = siteMapResource.move(home.getId(), news.getId());
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

            final String originalSiteMapItemNodePath = getPreviewConfigurationWorkspaceSitemapPath() + "/home";
            final String originalPageNodePath = getPreviewConfigurationWorkspacePagesPath() + "/homepage";

            // FailingPageDeleteEventListener should have short circuited the entire move and also must have removed the session changes
            assertTrue(session.nodeExists(originalSiteMapItemNodePath));
            assertTrue(session.nodeExists(originalPageNodePath));
            assertFalse(session.hasPendingChanges());
        } finally {
            failingPageMoveEventListener.destroy();
        }
    }

    @Test
    public void page_move_guava_event_short_circuiting_with_client_exception() throws Exception {
        FailingPageMoveEventListener failingPageMoveEventListener = new FailingPageMoveEventListener(new ClientException("client exception", INVALID_NAME));
        try {
            failingPageMoveEventListener.init();

            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
            final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "news");
            SiteMapResource siteMapResource = createResource();
            final Response response = siteMapResource.move(home.getId(), news.getId());
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    BAD_REQUEST.getStatusCode(), response.getStatus());

            final String originalSiteMapItemNodePath = getPreviewConfigurationWorkspaceSitemapPath() + "/home";
            final String originalPageNodePath = getPreviewConfigurationWorkspacePagesPath() + "/homepage";

            // FailingPageDeleteEventListener should have short circuited the entire move and also must have removed the session changes
            assertTrue(session.nodeExists(originalSiteMapItemNodePath));
            assertTrue(session.nodeExists(originalPageNodePath));
            assertFalse(session.hasPendingChanges());
        } finally {
            failingPageMoveEventListener.destroy();
        }
    }

}
