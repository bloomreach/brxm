/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.Path;
import javax.xml.bind.JAXBException;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.mock.configuration.components.MockHstComponentConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNodeFactory;

import static io.restassured.http.ContentType.JSON;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.equalTo;

public class ContainerComponentResourceTest extends AbstractResourceTest {

    private static final String MOCK_REST_PATH = "test-containercomponent/";

    private ContainerComponentService containerComponentService;

    /**
     * Override the @Path annotation in the {@link ContainerItemComponentResource} for ease of testing
     */
    @Path(MOCK_REST_PATH)
    private class ContainerComponentResourceWithMockPath extends ContainerComponentResource {
    }

    @Before
    public void setUp() throws RepositoryException {
        final HstRequestContext context = createMockHstContext();
        final PageComposerContextService mockPageComposerContextService = createNiceMock(PageComposerContextService.class);
        expect(mockPageComposerContextService.getRequestContext()).andReturn(context).anyTimes();
        replay(mockPageComposerContextService);

        containerComponentService = createMock(ContainerComponentService.class);

        final ContainerComponentResource containerComponentResource = new ContainerComponentResourceWithMockPath();
        containerComponentResource.setContainerComponentService(containerComponentService);
        containerComponentResource.setPageComposerContextService(mockPageComposerContextService);

        Config config = createDefaultConfig()
                .addServerSingleton(containerComponentResource);
        setup(config);
    }

    @Test
    public void can_create_a_new_container_item() throws RepositoryException, IOException, JAXBException {
        final ContainerComponentService.ContainerItem mockContainerItem = createNiceMock(ContainerComponentService.ContainerItem.class);
        final Node node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/services/ContainerComponentResourceTest-test-containeritem.xml");
        final Node containerItemNode = node.getNode("foo-component");

        final MockHstComponentConfiguration componentDefinition = new MockHstComponentConfiguration("id");
        componentDefinition.setXType(containerItemNode.getProperty("hst:xtype").getString());
        componentDefinition.setComponentClassName(containerItemNode.getProperty("hst:componentclassname").getString());

        expect(mockContainerItem.getContainerItem()).andReturn(containerItemNode).anyTimes();
        expect(mockContainerItem.getComponentDefinition()).andReturn(componentDefinition).anyTimes();
        expect(mockContainerItem.getTimeStamp()).andReturn(3456L);

        expect(containerComponentService.createContainerItem(mockSession, "cafebabe-cafe-babe-cafe-babecafebabe", 1234))
                .andReturn(mockContainerItem);

        replay(containerComponentService, mockContainerItem);

        given()
            .contentType(JSON)
        .when()
            .post(MOCK_REST_PATH + "cafebabe-cafe-babe-cafe-babecafebabe?lastModifiedTimestamp=1234")
        .then()
            .statusCode(201)
            .body("id", equalTo(containerItemNode.getIdentifier()),
                    "name", equalTo(containerItemNode.getName()),
                    "lastModifiedTimestamp", equalTo(3456));

        verify(containerComponentService);
    }

    @Test
    public void cannot_create_a_new_containeritem_when_uuid_is_invalid() throws RepositoryException, IOException, JAXBException {
        replay(containerComponentService);
        given()
            .contentType(JSON)
        .when()
            .post(MOCK_REST_PATH + "cafebabe?lastModifiedTimestamp=1234")
        .then()
            .statusCode(400);

        verify(containerComponentService);

    }

    @Test
    public void cannot_create_a_new_container_item_when_request_is_invalid() throws RepositoryException, IOException, JAXBException {
        expect(containerComponentService.createContainerItem(mockSession, "cafebabe-cafe-babe-cafe-babecafebabe", 1234))
                .andThrow(new ClientException("unknown error", ClientError.UNKNOWN));

        replay(containerComponentService);

        given()
            .contentType(JSON)
        .when()
            .post(MOCK_REST_PATH + "cafebabe-cafe-babe-cafe-babecafebabe?lastModifiedTimestamp=1234")
        .then()
            .statusCode(400)
            .body("error", equalTo("UNKNOWN"));

        verify(containerComponentService);
    }

    @Test
    public void cannot_create_a_new_container_item_when_the_container_is_locked() throws RepositoryException, IOException, JAXBException {
        final Map<String, Object> params = new HashMap<>();
        params.put("lockedBy", "admin");
        final long lockedOnTime = System.currentTimeMillis();
        params.put("lockedOn", lockedOnTime);

        expect(containerComponentService.createContainerItem(mockSession, "cafebabe-cafe-babe-cafe-babecafebabe", 1234))
                .andThrow(new ClientException("foo bah", ClientError.ITEM_ALREADY_LOCKED, params));

        replay(containerComponentService);

        given()
            .contentType(JSON)
        .when()
            .post(MOCK_REST_PATH + "cafebabe-cafe-babe-cafe-babecafebabe?lastModifiedTimestamp=1234")
        .then()
            .statusCode(400)
            .body("error", equalTo("ITEM_ALREADY_LOCKED"),
                    "parameterMap.lockedBy", equalTo("admin"),
                    "parameterMap.lockedOn", equalTo(lockedOnTime));

        verify(containerComponentService);
    }

    @Test
    public void cannot_create_a_new_container_item_when_server_has_errors() throws RepositoryException, IOException, JAXBException {
        expect(containerComponentService.createContainerItem(mockSession, "cafebabe-cafe-babe-cafe-babecafebabe", 1234))
                .andThrow(new RepositoryException("unknown error"));

        replay(containerComponentService);

        given()
            .contentType(JSON)
        .when()
            .post(MOCK_REST_PATH + "cafebabe-cafe-babe-cafe-babecafebabe?lastModifiedTimestamp=1234")
        .then()
            .statusCode(500)
            .body("error", equalTo("UNKNOWN"),
                    "parameterMap.errorReason", equalTo("unknown error"));

        verify(containerComponentService);
    }

    @Test
    public void can_delete_a_container_item() throws RepositoryException, IOException, JAXBException {
        containerComponentService.deleteContainerItem(mockSession, "cafebabe-cafe-babe-cafe-babecafebabe", 1234);
        expectLastCall();
        replay(containerComponentService);

        given()
            .contentType(JSON)
        .when()
            .delete(MOCK_REST_PATH + "cafebabe-cafe-babe-cafe-babecafebabe?lastModifiedTimestamp=1234")
        .then()
            .statusCode(200);

        verify(containerComponentService);
    }

    @Test
    public void cannot_delete_a_container_item_when_its_container_is_locked() throws RepositoryException, IOException, JAXBException {
        final Map<String, Object> params = new HashMap<>();
        params.put("lockedBy", "admin");
        final long lockedOnTime = System.currentTimeMillis();
        params.put("lockedOn", lockedOnTime);

        containerComponentService.deleteContainerItem(mockSession, "cafebabe-cafe-babe-cafe-babecafebabe", 1234);
        expectLastCall().andThrow(new ClientException("foo bah", ClientError.ITEM_ALREADY_LOCKED, params));
        replay(containerComponentService);

        given()
            .contentType(JSON)
        .when()
            .delete(MOCK_REST_PATH + "cafebabe-cafe-babe-cafe-babecafebabe?lastModifiedTimestamp=1234")
        .then()
            .statusCode(400)
            .body("error", equalTo("ITEM_ALREADY_LOCKED"),
                    "parameterMap.lockedBy", equalTo("admin"),
                    "parameterMap.lockedOn", equalTo(lockedOnTime));

        verify(containerComponentService);
    }

    @Test
    public void cannot_delete_a_new_container_item_when_request_is_invalid() throws RepositoryException, IOException, JAXBException {
        containerComponentService.deleteContainerItem(mockSession, "cafebabe-cafe-babe-cafe-babecafebabe", 1234);
        expectLastCall().andThrow(new ClientException("foo bah", ClientError.UNKNOWN));

        replay(containerComponentService);

        given()
            .contentType(JSON)
        .when()
            .delete(MOCK_REST_PATH + "cafebabe-cafe-babe-cafe-babecafebabe?lastModifiedTimestamp=1234")
        .then()
            .statusCode(400)
            .body("error", equalTo("UNKNOWN"));

        verify(containerComponentService);
    }

    @Test
    public void cannot_delete_a_new_container_item_when_server_has_errors() throws RepositoryException, IOException, JAXBException {
        containerComponentService.deleteContainerItem(mockSession, "cafebabe-cafe-babe-cafe-babecafebabe", 1234);
        expectLastCall().andThrow(new RepositoryException("unknown error"));

        replay(containerComponentService);

        given()
            .contentType(JSON)
        .when()
            .delete(MOCK_REST_PATH + "cafebabe-cafe-babe-cafe-babecafebabe?lastModifiedTimestamp=1234")
        .then()
            .statusCode(500)
            .body("error", equalTo("UNKNOWN"),
                    "parameterMap.errorReason", equalTo("unknown error"));

        verify(containerComponentService);
    }

    @Test
    public void can_update_a_container_item() throws RepositoryException, IOException, JAXBException {
        containerComponentService.updateContainer(eq(mockSession), isA(ContainerRepresentation.class));
        expectLastCall();
        final ContainerRepresentation containerToBeUpdated = createMockContainerRepresentation();
        replay(containerComponentService);

        given()
            .contentType(JSON)
            .body(containerToBeUpdated)
        .when()
            .put(MOCK_REST_PATH)
        .then()
            .statusCode(200)
            .body("id", equalTo("cafebabe"),
                    "name", equalTo("foo-item"),
                    "lastModifiedTimestamp", equalTo(1234));

        verify(containerComponentService);
    }

    @Test
    public void cannot_update_a_container_item_when_its_container_is_locked() throws RepositoryException, IOException, JAXBException {
        final Map<String, Object> params = new HashMap<>();
        params.put("lockedBy", "admin");
        final long lockedOnTime = System.currentTimeMillis();
        params.put("lockedOn", lockedOnTime);

        containerComponentService.updateContainer(eq(mockSession), isA(ContainerRepresentation.class));
        expectLastCall().andThrow(new ClientException("foo bah", ClientError.ITEM_ALREADY_LOCKED, params));
        final ContainerRepresentation containerToBeUpdated = createMockContainerRepresentation();
        replay(containerComponentService);

        given()
            .contentType(JSON)
            .body(containerToBeUpdated)
        .when()
            .put(MOCK_REST_PATH)
        .then()
            .statusCode(400)
            .body("error", equalTo("ITEM_ALREADY_LOCKED"),
                    "parameterMap.lockedBy", equalTo("admin"),
                    "parameterMap.lockedOn", equalTo(lockedOnTime));

        verify(containerComponentService);
    }

    @Test
    public void cannot_update_a_new_container_item_when_request_is_invalid() throws RepositoryException, IOException, JAXBException {
        containerComponentService.updateContainer(eq(mockSession), isA(ContainerRepresentation.class));
        expectLastCall().andThrow(new ClientException("foo bah", ClientError.UNKNOWN));

        final ContainerRepresentation containerToBeUpdated = createMockContainerRepresentation();
        replay(containerComponentService);

        given()
            .contentType(JSON)
            .body(containerToBeUpdated)
        .when()
            .put(MOCK_REST_PATH)
        .then()
            .statusCode(400)
            .body("error", equalTo("UNKNOWN"));

        verify(containerComponentService);
    }

    @Test
    public void cannot_update_a_new_container_item_when_server_has_errors() throws RepositoryException, IOException, JAXBException {
        containerComponentService.updateContainer(eq(mockSession), isA(ContainerRepresentation.class));
        expectLastCall().andThrow(new RepositoryException("unknown error"));

        final ContainerRepresentation containerToBeUpdated = createMockContainerRepresentation();
        replay(containerComponentService);

        given()
            .contentType(JSON)
            .body(containerToBeUpdated)
        .when()
            .put(MOCK_REST_PATH)
        .then()
            .statusCode(500)
            .body("error", equalTo("UNKNOWN"),
                    "parameterMap.errorReason", equalTo("unknown error"));

        verify(containerComponentService);
    }

    private ContainerRepresentation createMockContainerRepresentation() {
        final ContainerRepresentation containerToBeUpdated = new ContainerRepresentation();

        containerToBeUpdated.setId("cafebabe");
        containerToBeUpdated.setName("foo-item");
        containerToBeUpdated.setLastModifiedTimestamp(1234L);
        return containerToBeUpdated;
    }
}
