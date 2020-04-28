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
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.ws.rs.core.Response;

import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.springframework.mock.web.MockHttpServletResponse;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class XPageContainerComponentResourceTest extends AbstractXPageComponentResourceTest {

    @Test
    public void modifying_live_or_draft_variants_not_allowed() throws Exception {
        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");


        final String catalogId = getNodeId("/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/testitem");

        final String containerId = getNodeId(publishedExpPageVariant.getPath() + "/hst:page/body/container");

        failCreateAssertions(mountId, catalogId, containerId);


        final Session admin = createSession(ADMIN_CREDENTIALS);
        DocumentWorkflow documentWorkflow = getDocumentWorkflow(admin);
        documentWorkflow.obtainEditableInstance();

        Node draft = getVariant(handle, "draft");

        final String containerIdDraft = getNodeId(draft.getPath() + "/hst:page/body/container");

        failCreateAssertions(mountId, containerIdDraft, containerId);
    }

    private void failCreateAssertions(final String mountId, final String catalogId, final String containerId) throws IOException, ServletException {
        final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + containerId + "./" + catalogId, null,
                "POST");

        final MockHttpServletResponse createResponse = render(mountId, createRequestResponse, ADMIN_CREDENTIALS);
        final Map<String, String> createResponseMap = mapper.readerFor(Map.class).readValue(createResponse.getContentAsString());

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createResponse.getStatus());
        assertEquals(false, createResponseMap.get("success"));
        assertTrue(createResponseMap.get("message").contains("Does not below to unpublished variant of Experience Page."));
    }

    @Test
    public void create_and_delete_container_item_as_admin() throws Exception {

        createAndDeleteItemAs(ADMIN_CREDENTIALS, true);

    }

    @Test
    public void create_and_delete_container_item_as_editor() throws Exception {

        createAndDeleteItemAs(EDITOR_CREDENTIALS, true);

    }


    /**
     * Note an author cannot modify hst config pages but *CAN* modify experience pages if the cms user as role author
     */
    @Test
    public void create_and_delete_container_item_as_author() throws Exception {

        // author is not allowed to do a GET on ContainerItemComponentResource.getVariant()
        createAndDeleteItemAs(AUTHOR_CREDENTIALS, true);
    }

    /**
     * Note an author who does not have role hippo:author on the experience page is not allowed to modify the hst:page
     * in the experience page document
     */
    @Test
    public void create_container_item_NOT_allowed_it_not_role_author() throws Exception {
        // for author user, temporarily remove the role 'hippo:author' : Without this role, (s)he should not be allowed
        // to invoked the XPageContainerComponentResource

        final Session admin = createSession(ADMIN_CREDENTIALS);
        Property privilegesProp = admin.getNode("/hippo:configuration/hippo:roles/author").getProperty("hipposys:privileges");
        Value[] before = privilegesProp.getValues();
        privilegesProp.remove();
        admin.save();

        try {
            // since author does not have privilege hippo:author anymore, expect a FORBIDDEN
            createAndDeleteItemAs(AUTHOR_CREDENTIALS, false);
        } finally {
            // restore privileges
            admin.getNode("/hippo:configuration/hippo:roles/author").setProperty("hipposys:privileges", before);
            admin.save();
        }
    }

    /**
     * The 'creds' are used to invoke the HST pipeline, the adminSession is used to invoke workflow from here (like
     * publish)
     */
    private void createAndDeleteItemAs(final SimpleCredentials creds, final boolean allowed)
            throws IOException, ServletException, RepositoryException, WorkflowException {

        final Session adminSession = createSession(ADMIN_CREDENTIALS);

        try {
            final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final String containerId = getNodeId(unpublishedExpPageVariant.getPath() + "/hst:page/body/container");
            final String catalogId = getNodeId("/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/testitem");

            final DocumentWorkflow documentWorkflow = getDocumentWorkflow(adminSession);
            // since document got published and nothing yet changed, should not be published

            assertEquals("No changes yet in unpublished, hence not expected publication option",
                    FALSE, documentWorkflow.hints().get("publish"));

            final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + containerId + "./" + catalogId, null,
                    "POST");


            final MockHttpServletResponse createResponse = render(mountId, createRequestResponse, creds);
            final Map<String, String> createResponseMap = mapper.readerFor(Map.class).readValue(createResponse.getContentAsString());

            if (!allowed) {
                assertEquals("FORBIDDEN", createResponseMap.get("errorCode"));
                return;
            }


            assertEquals(CREATED.getStatusCode(), createResponse.getStatus());

            // assert modifying the preview did not create a draft variant!!! changes are directly on unpublished
            assertNull(getVariant(handle, "draft"));

            final String createdUUID = createResponseMap.get("id");

            // assertion on newly created item
            assertTrue(adminSession.nodeExists(unpublishedExpPageVariant.getPath() + "/hst:page/body/container/testitem"));
            assertTrue(adminSession.getNodeByIdentifier(createdUUID) != null);

            // assert document can now be published
            assertEquals("Unpublished has changes, publication should be enabled",
                    TRUE, documentWorkflow.hints().get("publish"));

            assertEquals("Expected unpublished to have been last modified by current user", creds.getUserID(),
                    unpublishedExpPageVariant.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY).getString());

            // now publish the document,
            documentWorkflow.publish();
            // assert that published variant now has extra container item 'testitem'
            assertTrue(adminSession.nodeExists(publishedExpPageVariant.getPath() + "/hst:page/body/container/testitem"));


            // now delete
            final RequestResponseMock deleteRequestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + containerId + "./" + createdUUID, null,
                    "DELETE");
            final MockHttpServletResponse deleteResponse = render(mountId, deleteRequestResponse, creds);
            assertEquals(Response.Status.OK.getStatusCode(), deleteResponse.getStatus());

            try {
                adminSession.getNodeByIdentifier(createdUUID);
                fail("Item expected to have been deleted again");
            } catch (ItemNotFoundException e) {
                // expected
            }

            // published variant still has the container item
            assertTrue(adminSession.nodeExists(publishedExpPageVariant.getPath() + "/hst:page/body/container/testitem"));

            // after delete, the unpublished has become publishable again
            assertEquals("Unpublished has changes, publication should be enabled",
                    TRUE, documentWorkflow.hints().get("publish"));

            documentWorkflow.publish();

            // published variant should not have the container item any more
            assertFalse(adminSession.nodeExists(publishedExpPageVariant.getPath() + "/hst:page/body/container/testitem"));

            // now assert an existing component item (or catalog) not from the current XPAGE cannot be deleted via
            // the container id

            final RequestResponseMock deleteRequestResponseInvalid = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + containerId + "./" + catalogId, null,
                    "DELETE");
            final MockHttpServletResponse invalidResponse = render(mountId, deleteRequestResponseInvalid, creds);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), invalidResponse.getStatus());

        } finally {
            adminSession.logout();
        }

    }

    private DocumentWorkflow getDocumentWorkflow(final Session session) throws RepositoryException {
        final WorkflowManager workflowManager = ((HippoSession) session).getWorkspace().getWorkflowManager();
        return (DocumentWorkflow) workflowManager.getWorkflow("default", handle);
    }
}
