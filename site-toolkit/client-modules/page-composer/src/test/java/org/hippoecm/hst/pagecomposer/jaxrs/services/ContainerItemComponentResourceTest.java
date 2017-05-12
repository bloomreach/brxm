/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.HashSet;

import javax.jcr.RepositoryException;
import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.easymock.EasyMock;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentPropertyRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ServerErrorException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.UnknownClientException;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.jaxrs.cxf.CXFTest;

import static com.jayway.restassured.http.ContentType.JSON;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class ContainerItemComponentResourceTest extends CXFTest {

    public static final String MOCK_REST_PATH = "test-containeritemcomponent/";
    private ContainerItemComponentService containerItemComponentService;

    private ContainerItemComponentResource containerItemComponentResource;

    /**
     * Override the @Path annotation in the {@link ContainerItemComponentResource} for ease of testing
     */
    @Path(MOCK_REST_PATH)
    private static class ContainerItemComponentResourceWithMockPath extends ContainerItemComponentResource {
    }

    @Before
    public void setUp() {
        containerItemComponentService = EasyMock.createNiceMock(ContainerItemComponentService.class);

        containerItemComponentResource = new ContainerItemComponentResourceWithMockPath();
        containerItemComponentResource.setContainerItemComponentService(containerItemComponentService);

        Config config = createDefaultConfig(JsonPojoMapperProvider.class)
                .addServerSingleton(containerItemComponentResource);
        setup(config);
    }

    @Test
    public void can_get_all_variants() throws RepositoryException {
        EasyMock.expect(containerItemComponentService.getVariants())
                .andReturn(new HashSet<>(Arrays.asList("foo-variant", "bah-variant")));

        EasyMock.replay(containerItemComponentService);

        when()
            .get(MOCK_REST_PATH)
        .then()
            .statusCode(200)
            .body("data", containsInAnyOrder("foo-variant", "bah-variant"));
    }

    @Test
    public void gets_all_variants_with_a_client_side_error() throws RepositoryException {
        EasyMock.expect(containerItemComponentService.getVariants())
                .andThrow(new UnknownClientException("bad request"));

        EasyMock.replay(containerItemComponentService);

        when()
            .get(MOCK_REST_PATH)
        .then()
            .statusCode(400)
            .body("data.error", equalTo(ClientError.UNKNOWN.toString()),
                    "data.parameterMap.errorReason", equalTo("bad request"));
    }

    @Test
    public void gets_all_variants_with_a_server_side_error() throws RepositoryException {
        EasyMock.expect(containerItemComponentService.getVariants())
                .andThrow(new RepositoryException("foo error"));

        EasyMock.replay(containerItemComponentService);

        when()
            .get(MOCK_REST_PATH)
        .then()
            .statusCode(500)
            .body("data.error", equalTo(ClientError.UNKNOWN.toString()),
                    "data.parameterMap.errorReason", equalTo("foo error"));
    }

    @Test
    public void can_retain_variants() throws RepositoryException {
        EasyMock.expect(containerItemComponentService.retainVariants(new HashSet<>(Arrays.asList("foo", "bah")), 1234))
                .andReturn(new HashSet<>(Arrays.asList("deleted-1", "deleted-2")));
        EasyMock.replay(containerItemComponentService);

        given()
                .contentType(JSON)
                .body(Arrays.asList("foo", "bah"))
                .header("lastModifiedTimestamp", 1234)
        .when()
            .post(MOCK_REST_PATH)
        .then()
            .statusCode(200)
            .body("message", equalTo("Removed variants:"),
                    "data", hasItem("deleted-1"),
                    "data", hasItem("deleted-2"));

        verify(containerItemComponentService);
    }

    @Test
    public void can_get_a_variant() throws RepositoryException, ServerErrorException {
        final ContainerItemComponentRepresentation mockVariant = new ContainerItemComponentRepresentation();
        final ContainerItemComponentPropertyRepresentation cicpp = new ContainerItemComponentPropertyRepresentation();
        cicpp.setName("Foo Variant");
        mockVariant.setProperties(Arrays.asList(cicpp));
        EasyMock.expect(containerItemComponentService.getVariant("foo-variant", "en"))
                .andReturn(mockVariant);

        EasyMock.replay(containerItemComponentService);

        when()
            .get(MOCK_REST_PATH + "foo-variant/en")
        .then()
            .statusCode(200)
            .body("properties", hasItem(hasEntry("name", "Foo Variant")));
    }

    @Test
    public void get_a_variant_with_server_side_error() throws RepositoryException, ServerErrorException {
        EasyMock.expect(containerItemComponentService.getVariant("foo-variant", "en"))
                .andThrow(new RepositoryException("foo error"));

        EasyMock.replay(containerItemComponentService);

        when()
            .get(MOCK_REST_PATH + "foo-variant/en")
        .then()
            .statusCode(204);
    }


    @Test
    public void can_update_a_variant() throws RepositoryException, ServerErrorException {
        final MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.put("key1", Arrays.asList("value1"));
        params.put("key2", Arrays.asList("value2"));

        containerItemComponentService.updateVariant("foo-variant", 1234, params);
        EasyMock.replay(containerItemComponentService);

        given()
            .contentType(JSON)
            .header("lastModifiedTimestamp", 1234)
            .body(params)
        .when()
            .put(MOCK_REST_PATH + "foo-variant")
        .then()
            .statusCode(200)
            .body("message", equalTo("Parameters for 'foo-variant' saved successfully."));

        verify(containerItemComponentService);
    }

    @Test
    public void can_update_and_rename_a_variant() throws RepositoryException, ServerErrorException {
        final MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.put("key1", Arrays.asList("value1"));
        params.put("key2", Arrays.asList("value2"));

        containerItemComponentService.moveAndUpdateVariant("foo-variant", "bah-variant", 1234, params);
        EasyMock.replay(containerItemComponentService);

        given()
            .contentType(JSON)
            .header("Move-To", "bah-variant")
            .header("lastModifiedTimestamp", 1234)
            .body(params)
        .when()
            .put(MOCK_REST_PATH + "foo-variant")
        .then()
            .statusCode(200)
            .body("message", equalTo("Parameters renamed from 'foo-variant' to 'bah-variant' and saved successfully."));

        verify(containerItemComponentService);
    }

    @Test
    public void cannot_update_and_rename_a_variant_when_its_container_is_locked() throws RepositoryException, ServerErrorException {
        final MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.put("key1", Arrays.asList("value1"));
        params.put("key2", Arrays.asList("value2"));

        containerItemComponentService.moveAndUpdateVariant("foo-variant", "bah-variant", 1234, params);
        EasyMock.expectLastCall().andThrow(new ClientException("bad request", ClientError.ITEM_ALREADY_LOCKED));
        EasyMock.replay(containerItemComponentService);

        given()
            .contentType(JSON)
            .header("Move-To", "bah-variant")
            .header("lastModifiedTimestamp", 1234)
            .body(params)
        .when()
            .put(MOCK_REST_PATH + "foo-variant")
        .then()
            .statusCode(400)
            .body("message", equalTo("Unable to set the parameters of component"),
                    "data.error", equalTo("ITEM_ALREADY_LOCKED"));

        verify(containerItemComponentService);
    }

    @Test
    public void cannot_update_and_rename_a_variant_when_server_has_error() throws RepositoryException, ServerErrorException {
        final MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.put("key1", Arrays.asList("value1"));
        params.put("key2", Arrays.asList("value2"));

        containerItemComponentService.moveAndUpdateVariant("foo-variant", "bah-variant", 1234, params);
        EasyMock.expectLastCall().andThrow(new RepositoryException("something wrong at server"));
        EasyMock.replay(containerItemComponentService);

        given()
            .contentType(JSON)
            .header("Move-To", "bah-variant")
            .header("lastModifiedTimestamp", 1234)
            .body(params)
        .when()
            .put(MOCK_REST_PATH + "foo-variant")
        .then()
            .statusCode(500)
            .body("message", equalTo("Unable to set the parameters of component"),
                    "data.error", equalTo("UNKNOWN"),
                    "data.parameterMap.errorReason", equalTo("something wrong at server"));

        verify(containerItemComponentService);
    }

    @Test
    public void can_create_a_new_variant() throws RepositoryException, ServerErrorException {
        containerItemComponentService.createVariant("foo-variant", 1234);
        EasyMock.replay(containerItemComponentService);

        given()
            .header("lastModifiedTimestamp", 1234)
        .when()
            .post(MOCK_REST_PATH + "foo-variant")
        .then()
            .statusCode(201)
            .body("message", equalTo("Variant 'foo-variant' created successfully"));

        verify(containerItemComponentService);
    }

    @Test
    public void cannot_create_an_existing_variant() throws RepositoryException, ServerErrorException {
        containerItemComponentService.createVariant("foo-variant", 1234);
        EasyMock.expectLastCall().andThrow(new ClientException("bad request", ClientError.ITEM_EXISTS));
        EasyMock.replay(containerItemComponentService);

        given()
            .header("lastModifiedTimestamp", 1234)
        .when()
            .post(MOCK_REST_PATH + "foo-variant")
        .then()
            .statusCode(400)
            .body("message", equalTo("Could not create variant 'foo-variant'"),
                    "data.error", equalTo("ITEM_EXISTS"));

        verify(containerItemComponentService);
    }

    @Test
    public void cannot_create_a_variant_when_container_is_locked() throws RepositoryException, ServerErrorException {
        containerItemComponentService.createVariant("foo-variant", 1234);
        EasyMock.expectLastCall().andThrow(new ClientException("bad request", ClientError.ITEM_ALREADY_LOCKED));
        EasyMock.replay(containerItemComponentService);

        given()
            .header("lastModifiedTimestamp", 1234)
        .when()
            .post(MOCK_REST_PATH + "foo-variant")
        .then()
            .statusCode(400)
            .body("message", equalTo("Could not create variant 'foo-variant'"),
                    "data.error", equalTo("ITEM_ALREADY_LOCKED"));

        verify(containerItemComponentService);
    }

    @Test
    public void cannot_create_a_new_variant_when_sever_has_error() throws RepositoryException, ServerErrorException {
        containerItemComponentService.createVariant("foo-variant", 1234);
        EasyMock.expectLastCall().andThrow(new ServerErrorException("something wrong at server"));
        EasyMock.replay(containerItemComponentService);

        given()
            .header("lastModifiedTimestamp", 1234)
        .when()
            .post(MOCK_REST_PATH + "foo-variant")
        .then()
            .statusCode(500)
            .body("message", equalTo("Could not create variant 'foo-variant'"),
                    "data.error", equalTo("UNKNOWN"),
                    "data.parameterMap.errorReason", equalTo("something wrong at server"));

        verify(containerItemComponentService);
    }

    @Test
    public void can_delete_a_variant() throws RepositoryException, ServerErrorException {
        containerItemComponentService.deleteVariant("foo-variant", 1234);
        EasyMock.replay(containerItemComponentService);

        given()
            .header("lastModifiedTimestamp", 1234)
        .when()
            .delete(MOCK_REST_PATH + "foo-variant")
        .then()
            .statusCode(200)
            .body("message", equalTo("Variant 'foo-variant' deleted successfully"));

        verify(containerItemComponentService);
    }

    @Test
    public void cannot_delete_a_variant_when_its_container_is_locked() throws RepositoryException, ServerErrorException {
        containerItemComponentService.deleteVariant("foo-variant", 1234);
        EasyMock.expectLastCall().andThrow(new ClientException("bad request", ClientError.ITEM_ALREADY_LOCKED));

        EasyMock.replay(containerItemComponentService);

        given()
            .header("lastModifiedTimestamp", 1234)
        .when()
            .delete(MOCK_REST_PATH + "foo-variant")
        .then()
            .statusCode(400)
            .body("message", equalTo("Could not delete variant 'foo-variant'"),
                    "data.error", equalTo("ITEM_ALREADY_LOCKED"));

        verify(containerItemComponentService);
    }

    @Test
    public void cannot_delete_a_variant_when_server_has_error() throws RepositoryException, ServerErrorException {
        containerItemComponentService.deleteVariant("foo-variant", 1234);
        EasyMock.expectLastCall().andThrow(new RepositoryException("something wrong at server"));
        EasyMock.replay(containerItemComponentService);

        given()
            .header("lastModifiedTimestamp", 1234)
        .when()
            .delete(MOCK_REST_PATH + "foo-variant")
        .then()
            .statusCode(500)
            .body("message", equalTo("Could not delete variant 'foo-variant'"),
                    "data.error", equalTo("UNKNOWN"),
                    "data.parameterMap.errorReason", equalTo("something wrong at server"));

        verify(containerItemComponentService);
    }
}