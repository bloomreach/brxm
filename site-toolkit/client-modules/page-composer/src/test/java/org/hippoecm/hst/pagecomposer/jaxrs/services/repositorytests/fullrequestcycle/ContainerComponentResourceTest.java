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
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletException;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ResponseRepresentation;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_COMPONENTDEFINITION;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ContainerComponentResourceTest extends AbstractComponentResourceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        final String containerId = getNodeId(PREVIEW_CONTAINER_TEST_PAGE_PATH + "/main/container");

        String oldStyleCatalogItemPath = "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/testitem";
        String newStyleCatalogItemPath = "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/testdefinition";

        final String oldStyleCatalogId = getNodeId(oldStyleCatalogItemPath);
        final String newStyleCatalogId = getNodeId(newStyleCatalogItemPath);


        final String oldStyleCreatedUUID = create(creds, allowed, mountId, containerId, oldStyleCatalogId);
        final String newStyleCreatedUUID = create(creds, allowed, mountId, containerId, newStyleCatalogId);

        if (allowed) {
            final Session session = createSession(ADMIN_CREDENTIALS);
            assertTrue(session.nodeExists(PREVIEW_CONTAINER_TEST_PAGE_PATH + "/main/container/testitem"));
            assertTrue(session.nodeExists(PREVIEW_CONTAINER_TEST_PAGE_PATH + "/main/container/testdefinition"));

            final Node oldStyleContainerItem = session.getNodeByIdentifier(oldStyleCreatedUUID);
            final Node newStyleContainerItem = session.getNodeByIdentifier(newStyleCreatedUUID);

            // assert newly created container item does not have the catalog item copied (old style) but keeps a
            // hst:componentdefinition reference
            assertThat(session.getNode(oldStyleCatalogItemPath)
                    .isNodeType("hst:containeritemcomponent"))
                    .as("Catalog item is a hst:containeritemcomponent")
                    .isTrue();
            assertThat(session.getNode(oldStyleCatalogItemPath)
                    .hasProperty(COMPONENT_PROPERTY_COMPONENT_CLASSNAME))
                    .as("Catalog item expected to have classname")
                    .isTrue();
            assertThat(oldStyleContainerItem.hasProperty(COMPONENT_PROPERTY_COMPONENT_CLASSNAME))
                    .as("Component item referencing catalog item expected to have classname")
                    .isTrue();
            assertThat(oldStyleContainerItem.hasProperty(COMPONENT_PROPERTY_COMPONENTDEFINITION))
                    .as("Expected newly created container item not to have hst:componentdefinition property")
                    .isFalse();

            assertThat(session.getNode(newStyleCatalogItemPath)
                    .isNodeType("hst:componentdefinition"))
                    .as("Catalog item is a hst:componentdefinition")
                    .isTrue();
            assertThat(session.getNode(newStyleCatalogItemPath)
                    .hasProperty(COMPONENT_PROPERTY_COMPONENT_CLASSNAME))
                    .as("Catalog item expected to have classname")
                    .isTrue();
            assertThat(newStyleContainerItem.hasProperty(COMPONENT_PROPERTY_COMPONENT_CLASSNAME))
                    .as("Component item referencing catalog item expected not to have classname")
                    .isFalse();
            assertThat(JcrUtils.getStringProperty(newStyleContainerItem, COMPONENT_PROPERTY_COMPONENTDEFINITION, null))
                    .as("Expected newly created container item to have hst:componentdefinition property pointing " +
                            "to catalog item")
                    .isEqualTo("hst:components/testpackage/testdefinition");

            RequestResponseMock deleteRequestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + containerId + "./" + oldStyleCreatedUUID, null,
                    "DELETE");
            MockHttpServletResponse deleteResponse = render(mountId, deleteRequestResponse, creds);
            assertEquals(Response.Status.OK.getStatusCode(), deleteResponse.getStatus());

            deleteRequestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + containerId + "./" + newStyleCreatedUUID, null,
                    "DELETE");
            deleteResponse = render(mountId, deleteRequestResponse, creds);
            assertEquals(Response.Status.OK.getStatusCode(), deleteResponse.getStatus());

            try {
                session.getNodeByIdentifier(oldStyleCreatedUUID);
                fail("Item expected to have been deleted again");
            } catch (ItemNotFoundException e) {
                // expected
            }

            try {
                session.getNodeByIdentifier(newStyleCreatedUUID);
                fail("Item expected to have been deleted again");
            } catch (ItemNotFoundException e) {
                // expected
            }

            session.logout();
        }
    }

    private String create(final SimpleCredentials creds, final boolean allowed, final String mountId, final String containerId, final String catalogId) throws IOException, ServletException, RepositoryException {
        final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + containerId + "./" + catalogId, null,
                "POST");


        final MockHttpServletResponse createResponse = render(mountId, createRequestResponse, creds);
        final ResponseRepresentation extResponse = mapper.readerFor(ResponseRepresentation.class).readValue(createResponse.getContentAsString());
        if (allowed) {
            assertEquals(Response.Status.CREATED.getStatusCode(), createResponse.getStatus());
            final Map<String, ?> map = (Map) extResponse.getData();
            final String createdUUID = map.get("id").toString();
            return createdUUID;
        } else {
            assertEquals("FORBIDDEN", extResponse.getErrorCode());
        }

        return null;
    }

    @Test
    public void move_container_item_within_container() throws Exception {

        //creates the preview
        startEdit(ADMIN_CREDENTIALS);

        final Session session = createSession(ADMIN_CREDENTIALS);
        try {


            final String mountId = getNodeId(session, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final String containerId = getNodeId(session, PREVIEW_CONTAINER_TEST_PAGE_PATH + "/main/container");
            final String itemId1 = getNodeId(session, PREVIEW_CONTAINER_TEST_PAGE_PATH + "/main/container/banner-new-style");
            final String itemId2 = getNodeId(session, PREVIEW_CONTAINER_TEST_PAGE_PATH + "/main/container/banner-old-style");

            final RequestResponseMock updateContainerReqRes = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + containerId, null, "PUT");

            final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

            containerRepresentation.setId(containerId);
            // children in reversed order
            containerRepresentation.setChildren(Stream.of(itemId2, itemId1).collect(Collectors.toList()));


            updateContainerReqRes.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
            updateContainerReqRes.getRequest().setContentType("application/json;charset=UTF-8");

            final MockHttpServletResponse updateResponse = render(mountId, updateContainerReqRes, ADMIN_CREDENTIALS);

            assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());

            // assert container items have been flipped and container is locked
            final Node container = session.getNodeByIdentifier(containerId);

            final NodeIterator nodes = container.getNodes();
            while (nodes.hasNext()) {

                assertEquals("testcatalogmenuitem", nodes.nextNode().getName());
                assertEquals("testcatalogqueryitem", nodes.nextNode().getName());
                assertEquals("testcatalogitem", nodes.nextNode().getName());
                assertEquals("banner-old-style", nodes.nextNode().getName());
                assertEquals("banner-new-style", nodes.nextNode().getName());
            }

            assertEquals("Container expected locked by",
                    "admin", container.getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());

        } finally {
            session.logout();
        }
    }

    @Test
    public void move_container_item_between_container() throws Exception {
        //creates the preview
        startEdit(ADMIN_CREDENTIALS);

        final Session session = createSession(ADMIN_CREDENTIALS);
        try {

            final String mountId = getNodeId(session, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final String targetContainerId = getNodeId(session, PREVIEW_CONTAINER_TEST_PAGE_PATH + "/main/container2");
            final String itemId1 = getNodeId(session, PREVIEW_CONTAINER_TEST_PAGE_PATH + "/main/container/banner-new-style");

            final RequestResponseMock updateContainerReqRes = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + targetContainerId, null, "PUT");

            final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

            containerRepresentation.setId(targetContainerId);
            // move itemId1 to other container
            containerRepresentation.setChildren(Stream.of(itemId1).collect(Collectors.toList()));


            updateContainerReqRes.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
            updateContainerReqRes.getRequest().setContentType("application/json;charset=UTF-8");

            final MockHttpServletResponse updateResponse = render(mountId, updateContainerReqRes, ADMIN_CREDENTIALS);

            assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());

            // assert the container from which the item is move is locked as well as the container *to* which the item
            // has been moved is locked
            final Node sourceContainer = session.getNode(PREVIEW_CONTAINER_TEST_PAGE_PATH + "/main/container");
            final Node targetContainer = session.getNodeByIdentifier(targetContainerId);

            final NodeIterator sourceChildren = sourceContainer.getNodes();
            // banner-old-style still present
            assertEquals(4, sourceChildren.getSize());

            final NodeIterator targetChildren = targetContainer.getNodes();
            assertEquals(1, targetChildren.getSize());
            assertEquals("banner-new-style", targetChildren.nextNode().getName());

            assertEquals("Container expected locked by",
                    "admin", sourceContainer.getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());

            assertEquals("Container expected locked by",
                    "admin", targetContainer.getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());

        } finally {
            session.logout();
        }
    }

    /**
     * <p>
     * It is not allowed to move a container item from an XPage to HST Configuration: A document has a different life
     * cycle than HST configuration, thus if we would support such a move, we'd get problems if either the XPage or
     * HST Config gets published
     * </p>
     * <p>
     * This tests covers the move of an XPage container item to HST Config container. The reverse test is covered in
     * {@link XPageContainerComponentResourceTest}
     * </p>
     */
    @Test
    public void move_container_item_from_XPAGE_to_hst_config_is_not_allowed() throws Exception {
        //creates the preview
        startEdit(ADMIN_CREDENTIALS);

        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String targetContainerId = getNodeId(PREVIEW_CONTAINER_TEST_PAGE_PATH + "/main/container");

        final String itemFromXPage = getNodeId("/unittestcontent/documents/unittestproject/experiences/expPage1/expPage1/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");

        final RequestResponseMock updateContainerReqRes = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + targetContainerId, null, "PUT");

        final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

        containerRepresentation.setId(targetContainerId);
        // try to move itemFromXPage to other container
        containerRepresentation.setChildren(Stream.of(itemFromXPage).collect(Collectors.toList()));

        updateContainerReqRes.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
        updateContainerReqRes.getRequest().setContentType("application/json;charset=UTF-8");

        final MockHttpServletResponse updateResponse = render(mountId, updateContainerReqRes, ADMIN_CREDENTIALS);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateResponse.getStatus());

    }

    @Test
    public void move_container_item_which_does_not_exist_is_bad_request() throws Exception {
        //creates the preview
        startEdit(ADMIN_CREDENTIALS);

        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String targetContainerId = getNodeId(PREVIEW_CONTAINER_TEST_PAGE_PATH + "/main/container");

        final RequestResponseMock updateContainerReqRes = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + targetContainerId, null, "PUT");

        final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

        containerRepresentation.setId(targetContainerId);
        // try to move non existing child to other container
        containerRepresentation.setChildren(Stream.of(UUID.randomUUID().toString()).collect(Collectors.toList()));

        updateContainerReqRes.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
        updateContainerReqRes.getRequest().setContentType("application/json;charset=UTF-8");

        final MockHttpServletResponse updateResponse = render(mountId, updateContainerReqRes, ADMIN_CREDENTIALS);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateResponse.getStatus());
    }

    @Test
    public void move_invalid_container_item_id_is_bad_request() throws Exception {
        //creates the preview
        startEdit(ADMIN_CREDENTIALS);

        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String targetContainerId = getNodeId(PREVIEW_CONTAINER_TEST_PAGE_PATH + "/main/container");

        final RequestResponseMock updateContainerReqRes = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + targetContainerId, null, "PUT");

        final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

        containerRepresentation.setId(targetContainerId);
        // try to move non existing child to other container
        containerRepresentation.setChildren(Stream.of("invalid-UUID").collect(Collectors.toList()));

        updateContainerReqRes.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
        updateContainerReqRes.getRequest().setContentType("application/json;charset=UTF-8");

        final MockHttpServletResponse updateResponse = render(mountId, updateContainerReqRes, ADMIN_CREDENTIALS);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateResponse.getStatus());
    }
}
