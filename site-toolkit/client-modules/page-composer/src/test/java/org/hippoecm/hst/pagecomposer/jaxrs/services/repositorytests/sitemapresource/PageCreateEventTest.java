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

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEventListenerRegistry;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageCreateContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageCreateEvent;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
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

public class PageCreateEventTest extends AbstractSiteMapResourceTest {

    final String newPageName = "foo";
    final String newPageTitle = "Foo Page";

    private void initContext() throws Exception {
        // call below will init request context
        getSiteMapItemRepresentation(session, "home");
    }

    public class PageCreateEventListener {

        protected PageCreateEvent receivedEvent;

        public void init() {
            ChannelEventListenerRegistry.get().register(this);
        }

        public void destroy() {
            ChannelEventListenerRegistry.get().unregister(this);
        }

        @Subscribe
        public void onPageCreateEvent(PageCreateEvent event) {
            if (event.getException() != null) {
                return;
            }
            try {
                final PageCreateContext pageCreateContext = event.getPageActionContext();

                assertEquals("Created sitemapitem is not at the right path",
                        getPreviewConfigurationWorkspaceSitemapPath() + "/" + newPageName, pageCreateContext.getNewSiteMapItemNode().getPath());

                final String previewPageNodePath = getPreviewConfigurationWorkspacePagesPath() + "/" + newPageName + "-" + session.getNodeByIdentifier(getPrototypePageUUID()).getName();
                assertEquals("Created sitemapitem is not at the right path",
                        previewPageNodePath, pageCreateContext.getNewPageNode().getPath());

                assertEquals("Title of new page is not set correctly",
                        newPageTitle, pageCreateContext.getNewSiteMapItemNode().getProperty(HstNodeTypes.SITEMAPITEM_PAGE_TITLE).getString());

            } catch (Exception e) {
                event.setException(new RuntimeException(e));
            }
            receivedEvent = event;
        }
    }

    public class FailingPageCreateEventListener extends PageCreateEventListener {
        final RuntimeException exception;

        public FailingPageCreateEventListener(final RuntimeException exception) {
            super();
            this.exception = exception;
        }

        @Override
        public void onPageCreateEvent(final PageCreateEvent event) {
            event.setException(exception);
            super.onPageCreateEvent(event);
        }
    }


    @Test
    public void test_create_guava_event() throws Exception {
        initContext();
        PageCreateEventListener pageCreateEventListener = new PageCreateEventListener();
        try {
            pageCreateEventListener.init();

            final String prototypeUUID = getPrototypePageUUID();
            final SiteMapItemRepresentation newItem = createSiteMapItemRepresentation(newPageName, prototypeUUID);
            newItem.setPageTitle(newPageTitle);
            final SiteMapResource siteMapResource = createResource();
            final Response response = siteMapResource.create(newItem);
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    OK.getStatusCode(), response.getStatus());


            final PageCreateEvent pce = pageCreateEventListener.receivedEvent;
            assertNotNull(pce);
            assertNull(pce.getException());
            final PageCreateContext pcc = pce.getPageActionContext();
            assertNotNull(pcc);

        } finally {
            pageCreateEventListener.destroy();
        }
    }


    @Test
    public void page_create_guava_event_short_circuiting_with_runtime_exception() throws Exception {
        initContext();
        FailingPageCreateEventListener failingPageCreateEventListener = new FailingPageCreateEventListener(new RuntimeException());
        try {
            failingPageCreateEventListener.init();

            final String prototypeUUID = getPrototypePageUUID();
            final SiteMapItemRepresentation newItem = createSiteMapItemRepresentation(newPageName, prototypeUUID);
            newItem.setPageTitle(newPageTitle);
            final SiteMapResource siteMapResource = createResource();
            final Response response = siteMapResource.create(newItem);
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

            final String previewSiteMapItemNodePath = getPreviewConfigurationWorkspaceSitemapPath() + "/" + newPageName;
            final String previewPageNodePath = getPreviewConfigurationWorkspacePagesPath() + "/" + newPageName + "-" + session.getNodeByIdentifier(getPrototypePageUUID()).getName();

            // FailingPageDeleteEventListener should have short circuited the entire create and also must have removed the session changes
            assertFalse(session.nodeExists(previewSiteMapItemNodePath));
            assertFalse(session.nodeExists(previewPageNodePath));
            assertFalse(session.hasPendingChanges());
        } finally {
            failingPageCreateEventListener.destroy();
        }
    }


    @Test
    public void page_delete_guava_event_short_circuiting_with_client_exception() throws Exception {
        initContext();
        FailingPageCreateEventListener failingPageCreateEventListener = new FailingPageCreateEventListener(new ClientException("client exception", INVALID_NAME));
        try {
            failingPageCreateEventListener.init();

            final String prototypeUUID = getPrototypePageUUID();
            final SiteMapItemRepresentation newItem = createSiteMapItemRepresentation(newPageName, prototypeUUID);
            newItem.setPageTitle(newPageTitle);
            final SiteMapResource siteMapResource = createResource();
            final Response response = siteMapResource.create(newItem);
            assertEquals(((ResponseRepresentation) response.getEntity()).getMessage(),
                    BAD_REQUEST.getStatusCode(), response.getStatus());

            final String previewSiteMapItemNodePath = getPreviewConfigurationWorkspaceSitemapPath() + "/" + newPageName;
            final String previewPageNodePath = getPreviewConfigurationWorkspacePagesPath() + "/" + newPageName + "-" + session.getNodeByIdentifier(getPrototypePageUUID()).getName();

            // FailingPageDeleteEventListener should have short circuited the entire create and also must have removed the session changes
            assertFalse(session.nodeExists(previewSiteMapItemNodePath));
            assertFalse(session.nodeExists(previewPageNodePath));
            assertFalse(session.hasPendingChanges());
        } finally {
            failingPageCreateEventListener.destroy();
        }
    }
}
