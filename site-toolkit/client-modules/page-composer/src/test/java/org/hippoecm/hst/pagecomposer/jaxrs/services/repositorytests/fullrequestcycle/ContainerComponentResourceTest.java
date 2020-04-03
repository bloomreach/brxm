/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletException;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ContainerComponentResourceTest extends AbstractComponentResourceTest {

    @Test
    public void create_and_delete_container_item_as_admin() throws Exception {

        //creates the preview
        startEdit(ADMIN_CREDENTIALS);
        createAndDeleteAs(ADMIN_CREDENTIALS, true);
    }

    @Test
    public void create_and_delete_container_item_as_editor() throws Exception {

        //creates the preview
        startEdit(ADMIN_CREDENTIALS);
        createAndDeleteAs(EDITOR_CREDENTIALS, true);
    }
    @Test
    public void try_create_container_item_as_author() throws Exception {

        //creates the preview
        startEdit(ADMIN_CREDENTIALS);
        // author is not allowed to do a GET on ContainerItemComponentResource.getVariant()
        createAndDeleteAs(AUTHOR_CREDENTIALS, false);
    }

    private void createAndDeleteAs(final SimpleCredentials creds, final boolean allowed) throws IOException, ServletException, RepositoryException {


        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String containerId = getNodeId("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/containertestpage/main/container");
        final String catalogId = getNodeId("/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/testitem");


        final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + containerId + "./" + catalogId, null,
                "POST");


        final MockHttpServletResponse createResponse = render(mountId, createRequestResponse, creds);
        final Map<String, String> createResponseMap = mapper.readerFor(Map.class).readValue(createResponse.getContentAsString());

        if (allowed) {

            final Session session = createSession(ADMIN_CREDENTIALS);

            assertEquals(Response.Status.CREATED.getStatusCode(), createResponse.getStatus());
            final String createdUUID = createResponseMap.get("id");

            // newly created item
            assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/containertestpage/main/container/testitem"));
            assertTrue(session.getNodeByIdentifier(createdUUID) != null);

            final RequestResponseMock  deleteRequestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + containerId + "./" + createdUUID, null,
                    "DELETE");
            final MockHttpServletResponse deleteResponse = render(mountId, deleteRequestResponse, creds);
            assertEquals(Response.Status.OK.getStatusCode(), deleteResponse.getStatus());

            try {
                session.getNodeByIdentifier(createdUUID);
                fail("Item expected to have been deleted again");
            } catch (ItemNotFoundException e) {
                // expected
            }

            session.logout();

        } else {
            assertEquals("FORBIDDEN", createResponseMap.get("errorCode"));
        }
    }
}
