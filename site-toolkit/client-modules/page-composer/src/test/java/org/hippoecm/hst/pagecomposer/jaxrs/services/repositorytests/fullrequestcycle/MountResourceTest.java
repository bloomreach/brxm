/*
 *  Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.fullrequestcycle;

import java.io.IOException;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletException;

import org.hippoecm.hst.pagecomposer.jaxrs.AbstractFullRequestCycleTest;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractPageComposerTest;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtIdsRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.site.HstServices;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static java.util.Collections.singletonList;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MountResourceTest extends AbstractFullRequestCycleTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Session session = backupHstAndCreateWorkspace();

        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap").remove();

        session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemap",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap");

        session.save();
        session.logout();

    }

    @After
    public void tearDown() throws Exception {
        try {
            final Session session = createSession("admin", "admin");
            restoreHstConfigBackup(session);
            session.logout();
        } finally {
            super.tearDown();
        }
    }

    @Test
    public void start_edit_creating_preview_config_as_admin() throws Exception {
        startEditAssertions(ADMIN_CREDENTIALS, true);
    }

    @Test
    public void start_edit_creating_preview_config_as_webmaster() throws Exception {
        startEditAssertions(EDITOR_CREDENTIALS, true);
    }

    @Test
    public void liveuser_cannot_invoke_start_edit() throws Exception {
        Credentials liveUserCreds = HstServices.getComponentManager().getComponent(Credentials.class.getName() + ".default.delegating");
        startEditAssertions(liveUserCreds, ClientError.FORBIDDEN);
    }

    protected void startEditAssertions(final Credentials creds, final boolean shouldSucceed) throws RepositoryException, IOException, ServletException {
        final Map<String, Object> responseMap = startEdit(creds);
        if (shouldSucceed) {
            assertEquals(Boolean.TRUE, responseMap.get("success"));
            assertEquals("Site can be edited now", responseMap.get("message"));
        } else {
            assertEquals(Boolean.FALSE, responseMap.get("success"));
            assertTrue(responseMap.get("message").toString().contains("Could not create a preview configuration"));
        }
    }

    protected void startEditAssertions(final Credentials creds, final ClientError expectedClientError) throws RepositoryException, IOException, ServletException {
        final Map<String, Object> responseMap = startEdit(creds);
        assertEquals(Boolean.FALSE, responseMap.get("success"));
        assertEquals(expectedClientError.name(), responseMap.get("errorCode"));
    }

    @Test
    public void copy_a_page_as_admin() throws Exception {
        copyPageAssertions(ADMIN_CREDENTIALS, true);
    }

    @Test
    public void copy_a_page_as_editor() throws Exception {
        copyPageAssertions(EDITOR_CREDENTIALS, true);
    }

    @Test
    public void liveuser_cannot_copy_page() throws Exception {
        Credentials liveUserCreds = HstServices.getComponentManager().getComponent(Credentials.class.getName() + ".default.delegating");
        copyPageAssertions(liveUserCreds, false);
    }

    protected Map<String, Object> copyPage(final Credentials creds) throws RepositoryException, IOException, ServletException {
        startEdit(ADMIN_CREDENTIALS);

        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
        final String siteMapId = getNodeId("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap");
        final String siteMapItemToCopyId = getNodeId("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/home");

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + siteMapId + "./copy", null, "POST");

        requestResponse.getRequest().addHeader("mountId", mountId);
        requestResponse.getRequest().addHeader("siteMapItemUUID", siteMapItemToCopyId);
        requestResponse.getRequest().addHeader("targetName", "home-copy");

        final MockHttpServletResponse response = render(mountId, requestResponse, creds);
        final String restResponse = response.getContentAsString();
        return mapper.readerFor(Map.class).readValue(restResponse);
    }

    protected void copyPageAssertions(final Credentials creds, final boolean shouldSucceed) throws Exception {
        // first create preview config with admin creds
        final Map<String, Object> responseMap = copyPage(creds);
        if (shouldSucceed) {
            assertEquals(Boolean.TRUE, responseMap.get("success"));
            assertEquals("Item created successfully", responseMap.get("message"));

            final Session admin = createSession("admin", "admin");
            final String workspacePath = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace";
            assertTrue(admin.getNode(workspacePath + "/hst:sitemap/home-copy").getProperty(GENERAL_PROPERTY_LOCKED_BY).getString().equals(((SimpleCredentials) creds).getUserID()));
            assertTrue(admin.getNode(workspacePath + "/hst:pages/home-copy").getProperty(GENERAL_PROPERTY_LOCKED_BY).getString().equals(((SimpleCredentials) creds).getUserID()));
            admin.logout();
        } else {
            assertEquals(Boolean.FALSE, responseMap.get("success"));
        }
    }

    @Test
    public void publish_as_admin_changes_of_admin() throws Exception {
        publishAssertions(ADMIN_CREDENTIALS, true);
    }

    @Test
    public void publish_as_editor_changes_of_editor() throws Exception {
        publishAssertions(EDITOR_CREDENTIALS, true);
    }

    protected Map<String, Object> publish(final Credentials creds) throws Exception {
        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + mountId + "./publish", null, "POST");

        final MockHttpServletResponse response = render(mountId, requestResponse, creds);
        final String restResponse = response.getContentAsString();
        return mapper.readerFor(Map.class).readValue(restResponse);
    }

    protected void publishAssertions(final Credentials creds, final boolean shouldSucceed) throws Exception {
        // first force a change
        copyPage(creds);
        final Map<String, Object> responseMap = publish(creds);
        if (shouldSucceed) {
            assertEquals(Boolean.TRUE, responseMap.get("success"));
            assertEquals("Site is published", responseMap.get("message"));
        } else {
            assertEquals(Boolean.FALSE, responseMap.get("success"));
        }
    }

    @Test
    public void discard_as_admin_changes_of_admin() throws Exception {
        discardAssertions(ADMIN_CREDENTIALS, true);
    }

    @Test
    public void discard_as_editor_changes_of_editor() throws Exception {
        discardAssertions(EDITOR_CREDENTIALS, true);
    }

    protected Map<String, Object> discard(final Credentials creds) throws Exception {
        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + mountId + "./discard", null, "POST");

        final MockHttpServletResponse response = render(mountId, requestResponse, creds);
        final String restResponse = response.getContentAsString();
        return mapper.readerFor(Map.class).readValue(restResponse);
    }

    protected void discardAssertions(final Credentials creds, final boolean shouldSucceed) throws Exception {
        // first force a change
        copyPage(creds);
        final Map<String, Object> responseMap = discard(creds);
        if (shouldSucceed) {
            assertEquals(Boolean.TRUE, responseMap.get("success"));
            assertTrue(responseMap.get("message").toString().contains("discarded"));
        } else {
            assertEquals(Boolean.FALSE, responseMap.get("success"));
        }
    }

    @Test
    public void publish_userswithchanges_as_admin_succeeds() throws Exception {
        publishAssertions(ADMIN_CREDENTIALS, EDITOR_CREDENTIALS, true);
    }

    @Test
    public void publish_userswithchanges_as_editor_fails() throws Exception {
        publishAssertions(EDITOR_CREDENTIALS, EDITOR_CREDENTIALS, false);
    }

    protected MockHttpServletResponse publish(final Credentials publishCreds, final SimpleCredentials changesCreds) throws Exception {
        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + mountId + "./userswithchanges/publish", null, "POST");

        final MockHttpServletRequest request = requestResponse.getRequest();
        final ExtIdsRepresentation extIdsRepresentation = new ExtIdsRepresentation();
        extIdsRepresentation.setData(singletonList(changesCreds.getUserID()));
        final String idsToPublish = mapper.writeValueAsString(extIdsRepresentation);
        request.setContent(idsToPublish.getBytes("UTF-8"));
        request.setContentType("application/json");
        return render(mountId, requestResponse, publishCreds);
    }

    protected void publishAssertions(final Credentials publishCreds, final SimpleCredentials changesCreds, final boolean shouldSucceed) throws Exception {
        // first force a change *by* changesCreds
        copyPage(changesCreds);
        final MockHttpServletResponse response = publish(publishCreds, changesCreds);

        if (shouldSucceed) {
            final String restResponse = response.getContentAsString();
            final Map<String, Object> responseMap = mapper.readerFor(Map.class).readValue(restResponse);

            assertEquals(Boolean.TRUE, responseMap.get("success"));
            assertEquals("Site is published", responseMap.get("message"));
        } else {
            assertEquals(SC_FORBIDDEN, response.getStatus());
        }
    }

    protected MockHttpServletResponse discard(final Credentials publishCreds, final SimpleCredentials changesCreds) throws Exception {
        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + mountId + "./userswithchanges/discard", null, "POST");

        final MockHttpServletRequest request = requestResponse.getRequest();
        final ExtIdsRepresentation extIdsRepresentation = new ExtIdsRepresentation();
        extIdsRepresentation.setData(singletonList(changesCreds.getUserID()));
        final String idsToPublish = mapper.writeValueAsString(extIdsRepresentation);
        request.setContent(idsToPublish.getBytes("UTF-8"));
        request.setContentType("application/json");
        return render(mountId, requestResponse, publishCreds);
    }

}
