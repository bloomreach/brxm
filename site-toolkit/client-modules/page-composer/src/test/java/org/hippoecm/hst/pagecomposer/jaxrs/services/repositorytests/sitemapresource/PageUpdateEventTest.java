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

import javax.jcr.Node;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEventListenerRegistry;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageEvent;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageUpdateContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageUpdateEvent;
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
import static org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError.INVALID_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PageUpdateEventTest extends AbstractSiteMapResourceTest {

    final String newPageTitle = "new-page-title";
    final String newComponentConfigurationPath = "home-prototype-page";
    boolean componentConfigurationIdChanged;

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
        // call below will init request context
        getSiteMapItemRepresentation(session, "home");
    }


    public class PageUpdateEventListener {

        protected PageUpdateEvent receivedEvent;

        public void init() {
            ChannelEventListenerRegistry.get().register(this);
        }

        public void destroy() {
            ChannelEventListenerRegistry.get().unregister(this);
        }

        @Subscribe
        public void onPageUpdateEvent(PageUpdateEvent event) {
            if (event.getException() != null) {
                return;
            }
            try {
                final PageUpdateContext pageUpdateContext = event.getPageActionContext();

                final String previewSiteMapItemNodePath = getPreviewConfigurationWorkspaceSitemapPath() + "/home";

                assertEquals("Updated sitemapitem is not at the right path",
                        previewSiteMapItemNodePath, pageUpdateContext.getUpdatedSiteMapItemNode().getPath());

                if (componentConfigurationIdChanged) {
                    final String previewPageNodePath = getPreviewConfigurationWorkspacePagesPath() + "/" + newComponentConfigurationPath;
                    assertEquals("Updated page is not at the right path",
                            previewPageNodePath, pageUpdateContext.getUpdatedPageNode().getPath());
                } else {
                    assertNull("Updated page should be null, as there was no change in componentconfigurationid", pageUpdateContext.getUpdatedPageNode());
                }

                assertEquals("Updated page has unexpected page title",
                        newPageTitle, pageUpdateContext.getUpdatedSiteMapItemNode().getProperty(HstNodeTypes.SITEMAPITEM_PAGE_TITLE).getString());

            } catch (Exception e) {
                event.setException(new RuntimeException(e));
            }
            receivedEvent = event;
        }
    }

    public class FailingPageUpdateEventListener extends PageUpdateEventListener {
        final RuntimeException exception;

        public FailingPageUpdateEventListener(final RuntimeException exception) {
            super();
            this.exception = exception;
        }

        @Override
        public void onPageUpdateEvent(final PageUpdateEvent event) {
            event.setException(exception);
            super.onPageUpdateEvent(event);
        }
    }

    @Test
    public void test_update_guava_event() throws Exception {
        initContext();
        PageUpdateEventListener pageUpdateEventListener = new PageUpdateEventListener();
        try {
            pageUpdateEventListener.init();

            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
            home.setPageTitle(newPageTitle);
            componentConfigurationIdChanged = false;
            final SiteMapResource siteMapResource = createResource();
            Response response = siteMapResource.update(home);
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    OK.getStatusCode(), response.getStatus());

            final PageEvent pce = pageUpdateEventListener.receivedEvent;
            assertNotNull(pce);
            assertNull(pce.getException());
            final PageUpdateContext pcc = (PageUpdateContext) pce.getPageActionContext();
            assertNotNull(pcc);

        } finally {
            pageUpdateEventListener.destroy();
        }
    }

    @Test
    public void test_update_guava_event_with_reapply_prototypes() throws Exception {
        initContext();
        PageUpdateEventListener pageUpdateEventListener = new PageUpdateEventListener();
        try {
            pageUpdateEventListener.init();

            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
            home.setPageTitle(newPageTitle);
            home.setComponentConfigurationId(getPrototypePageUUID());
            componentConfigurationIdChanged = true;

            final SiteMapResource siteMapResource = createResource();
            Response response = siteMapResource.update(home);
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    OK.getStatusCode(), response.getStatus());

            final PageEvent pce = pageUpdateEventListener.receivedEvent;
            assertNotNull(pce);
            assertNull(pce.getException());
            final PageUpdateContext pcc = (PageUpdateContext) pce.getPageActionContext();
            assertNotNull(pcc);

        } finally {
            pageUpdateEventListener.destroy();
        }
    }

    @Test
    public void page_update_guava_event_short_circuiting_with_runtime_exception() throws Exception {
        initContext();
        FailingPageUpdateEventListener failingPageUpdateEventListener = new FailingPageUpdateEventListener(new RuntimeException());
        try {
            failingPageUpdateEventListener.init();

            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");

            Node homeSitemapitemNode = session.getNodeByIdentifier(home.getId());
            final String originalPageTitle = homeSitemapitemNode.hasProperty(HstNodeTypes.SITEMAPITEM_PAGE_TITLE) ?
                    homeSitemapitemNode.getProperty(HstNodeTypes.SITEMAPITEM_PAGE_TITLE).getString() : null;

            home.setPageTitle(newPageTitle);
            final SiteMapResource siteMapResource = createResource();
            Response response = siteMapResource.update(home);
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

            // FailingPageUpdateEventListener should have short circuited the entire update and also must have removed the session changes
            if (originalPageTitle == null) {
                assertFalse("Sitemap item should not have its page title property set", homeSitemapitemNode.hasProperty(HstNodeTypes.SITEMAPITEM_PAGE_TITLE));
            } else {
                assertEquals("Sitemap item should not have its page title property changed",
                        originalPageTitle, session.getNodeByIdentifier(home.getId()).getProperty(HstNodeTypes.SITEMAPITEM_PAGE_TITLE).getString());
            }
            assertFalse(session.hasPendingChanges());
        } finally {
            failingPageUpdateEventListener.destroy();
        }
    }

    @Test
    public void page_delete_guava_event_short_circuiting_with_client_exception() throws Exception {
        initContext();
        FailingPageUpdateEventListener failingPageUpdateEventListener = new FailingPageUpdateEventListener(new ClientException("client exception", INVALID_NAME));
        try {
            failingPageUpdateEventListener.init();

            final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");

            Node homeSitemapitemNode = session.getNodeByIdentifier(home.getId());
            final String originalPageTitle = homeSitemapitemNode.hasProperty(HstNodeTypes.SITEMAPITEM_PAGE_TITLE) ?
                    homeSitemapitemNode.getProperty(HstNodeTypes.SITEMAPITEM_PAGE_TITLE).getString() : null;

            home.setPageTitle(newPageTitle);
            final SiteMapResource siteMapResource = createResource();
            Response response = siteMapResource.update(home);
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    BAD_REQUEST.getStatusCode(), response.getStatus());

            // FailingPageUpdateEventListener should have short circuited the entire update and also must have removed the session changes
            if (originalPageTitle == null) {
                assertFalse("Sitemap item should not have its page title property set", homeSitemapitemNode.hasProperty(HstNodeTypes.SITEMAPITEM_PAGE_TITLE));
            } else {
                assertEquals("Sitemap item should not have its page title property changed",
                        originalPageTitle, session.getNodeByIdentifier(home.getId()).getProperty(HstNodeTypes.SITEMAPITEM_PAGE_TITLE).getString());
            }
            assertFalse(session.hasPendingChanges());
        } finally {
            failingPageUpdateEventListener.destroy();
        }
    }
}
