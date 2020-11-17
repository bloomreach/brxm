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

import java.util.Collections;
import java.util.Map;

import javax.jcr.Node;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import static java.lang.Boolean.FALSE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class XPageResourceTest extends AbstractXPageComponentResourceTest {

    private String containerNodeName;
    private Node hstXpageDocNode;
    private String mountId;

    @Before
    public void setup() throws Exception {
        // the containerNodeName is the equivalent of the 'hippo:identifier' on the XPage Layout container
        containerNodeName = "430df2da-3dc8-40b5-bed5-bdc44b8445c6";

        // first delete the container from the XPage : this is for the fixture: this mimics the situation that to an
        // existing XPage Layout a new container gets added which does not have its equivalent container on existing
        // XPage Docs

        hstXpageDocNode = admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage");

        admin.getNode(hstXpageDocNode.getPath() + "/" + containerNodeName).remove();
        admin.save();

        mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");


    }

    @Test
    public void move_container_item_to_container_missing_in_xpage() throws Exception {

        //  try to move the container item from /430df2da-3dc8-40b5-bed5-bdc44b8445c7/banner to 'containerNodeName'
        // container which does not exist any more: as a result, it should be created

        // the 'endpoint' is against the hst:xpage node
        final RequestResponseMock updateRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + hstXpageDocNode.getIdentifier(), null,
                "PUT");

        final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

        // this container does not yet exist for the hst:xpage and should gete created as a result
        containerRepresentation.setId(containerNodeName);

        final String itemToMove = hstXpageDocNode.getNode("430df2da-3dc8-40b5-bed5-bdc44b8445c7/banner").getIdentifier();

        containerRepresentation.setChildren(Collections.singletonList(itemToMove));

        updateRequestResponse.getRequest().setContent(new ObjectMapper().writeValueAsBytes(containerRepresentation));
        updateRequestResponse.getRequest().setContentType("application/json;charset=UTF-8");

        final MockHttpServletResponse updateResponse = render(mountId, updateRequestResponse, ADMIN_CREDENTIALS);

        assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());

        // assert container has been created and item has been moved
        assertTrue("Expected new container with name equal to 'containerNodeName' to have been created", hstXpageDocNode.hasNode(containerNodeName));

        final Node container = hstXpageDocNode.getNode(containerNodeName);

        assertTrue("Expected the banner item was moved",container.hasNode("banner"));

    }

    @Test
    public void create_container_item_when_container_missing_in_xpage() throws Exception {

        final String catalogId = getNodeId(admin, "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/newstyle-testitem");

        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(admin);
        // since document got published and nothing yet changed, should not be published

        assertEquals("No changes yet in unpublished, hence not expected publication option",
                FALSE, documentWorkflow.hints().get("publish"));

        /*
         * the invocation that is done from the CM is:
         *
         * _rp/{uuid}./{xpage_container_hippo_identifier}/{catalog_uuid}
         *
         * where {uuid} is the uuid of the hst:xpage node below the XPage document : the reason for this form is as
         * follows:
         *
         * - An author should be able to add items to the container
         * - An author cannot read the hst XPage Layout configuration
         *
         * So the only thing we can do to provide enough info is as follows:
         *
         * - provide the hst:xpage uuid below the unpublished variant
         * - provide the node name of the container to be created: this is the value of the hippo:identifier on the
         *   XPage Layout in hst config
         * - provide the catalog item id
         *
         * With the above info, the backend can know which container node to created and which item to inject into it
         */

        final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + hstXpageDocNode.getIdentifier() + "./" + containerNodeName + "/" + catalogId, null,
                "POST");


        // use author credentials: if author succeeds, then surely for ADMIN it will also work
        final MockHttpServletResponse createResponse = render(mountId, createRequestResponse, AUTHOR_CREDENTIALS);
        final ExtResponseRepresentation extResponseRepresentation = mapper.readerFor(ExtResponseRepresentation.class).readValue(createResponse.getContentAsString());

        // for a newly added container, ALWAYS a page reload is needed
        assertRequiresReload(createResponse, true);

        assertEquals(CREATED.getStatusCode(), createResponse.getStatus());

        // assert modifying the preview did not create a draft variant!!! changes are directly on unpublished
        assertNull(getVariant(handle, "draft"));

        Map<String, ?> map = (Map) extResponseRepresentation.getData();
        final String createdUUID = map.get("id").toString();



        assertTrue(admin.nodeExists(unpublishedExpPageVariant.getPath() + "/hst:xpage/" + containerNodeName));

        final Node newlyCreatedContainer = admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage/" + containerNodeName);

        assertTrue(newlyCreatedContainer.hasNode("newstyle-testitem"));
        // assert the createdUUID is for the created container ITEM, not the container!
        assertEquals(createdUUID, newlyCreatedContainer.getNode("newstyle-testitem").getIdentifier());

    }
}
