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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.assertj.core.api.Assertions;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstComponentParameters;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class XPageContainerItemComponentResourceTest extends AbstractXPageComponentResourceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void get_container_item() throws Exception {

        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String componentItemId = getNodeId(unpublishedExpPageVariant.getPath() + "/hst:page/body/container/banner");

        getComponentItemAs(ADMIN_CREDENTIALS, mountId, componentItemId);
        getComponentItemAs(EDITOR_CREDENTIALS, mountId, componentItemId);
        // author is also allowed to do a GET on XPageContainerItemComponentResource.getVariant()
        getComponentItemAs(AUTHOR_CREDENTIALS, mountId, componentItemId);
    }

    private void getComponentItemAs(final SimpleCredentials creds, final String mountId, final String componentItemId) throws IOException, ServletException {
        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + componentItemId + "./hippo-default/en", null, "GET");


        final MockHttpServletResponse response = render(mountId, requestResponse, creds);
        final String restResponse = response.getContentAsString();

        final Map<String, Object> responseMap = mapper.readerFor(Map.class).readValue(restResponse);

        // see default BannerComponentInfo
        List<Map<String, String>> properties = (List) responseMap.get("properties");
        assertEquals(1, properties.size());

        Map<String, String> propertyRepresentation = properties.get(0);

        assertEquals("path", propertyRepresentation.get("name"));
        assertEquals("/some/default", propertyRepresentation.get("defaultValue"));
        assertEquals("/content/document", propertyRepresentation.get("value"));

    }

    @Test
    public void get_container_item_published_variant_not_allowed() throws Exception {

        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String componentItemId = getNodeId(publishedExpPageVariant.getPath() + "/hst:page/body/container/banner");

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + componentItemId + "./hippo-default/en", null, "GET");


        final MockHttpServletResponse response = render(mountId, requestResponse, ADMIN_CREDENTIALS);

        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());

    }

    @Test
    public void retain_existing_variant_container_item_also_remains_default() throws Exception {
        expectationsRetainingVariant(new String[]{"variant1"});
    }

    @Test
    public void retain_existing_and_non_existing_variant_container_item_also_remains_default() throws Exception {
        expectationsRetainingVariant(new String[]{"variant1", "non-existing"});
    }

    private void expectationsRetainingVariant(final String[] retainVariants) throws RepositoryException, IOException, ServletException, WorkflowException {
        final Session session = createSession(ADMIN_CREDENTIALS);

        try {
            final String mountId = getNodeId(session, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final String componentItemId = getNodeId(session, unpublishedExpPageVariant.getPath() + "/hst:page/body/container/banner");

            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + componentItemId, null, "POST");


            requestResponse.getRequest().setContent(objectMapper.writeValueAsBytes(retainVariants));
            requestResponse.getRequest().setContentType("application/json;charset=UTF-8");

            // Do it as author which : author should be allowed
            final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // assert container never locked for XPage, and not publishable since it did NOT change
            assertFalse("XPage container should never get locked!!",
                    session.getNode(unpublishedExpPageVariant.getPath() + "/hst:page/body/container").hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));

            session.getNode(unpublishedExpPageVariant.getPath() + "/hst:page/body/container/banner").getProperty(COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES);

            final String[] variants = JcrUtils.getMultipleStringProperty(session.getNode(unpublishedExpPageVariant.getPath() + "/hst:page/body/container/banner"),
                    COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES, null);

            Assertions.assertThat(variants)
                    .as("Expected the default (empty) variant and 'variant1' to be present")
                    .containsExactly("", "variant1");


            final DocumentWorkflow documentWorkflow = getDocumentWorkflow(session);
            assertEquals("Expected no changes because retained all the variants",
                    FALSE, documentWorkflow.hints().get("publish"));

        } finally {
            session.logout();
        }
    }

    @Test
    public void retain_empty_variant_removes_all_except_default() throws Exception {

        final Session session = createSession(ADMIN_CREDENTIALS);

        try {
            final Node container = session.getNode(unpublishedExpPageVariant.getPath() + "/hst:page/body/container");
            assertTrue("Container has some variants bootstrapped",
                    container.getNode("banner").hasProperty(COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES));

            final String mountId = getNodeId(session, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final String componentItemId = getNodeId(session, unpublishedExpPageVariant.getPath() + "/hst:page/body/container/banner");

            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + componentItemId, null, "POST");


            final MockHttpServletRequest request = requestResponse.getRequest();
            request.setContent(objectMapper.writeValueAsBytes(new String[]{}));
            request.setContentType("application/json;charset=UTF-8");


            // Do it as author which : author should be allowed
            final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());


            session.refresh(false);

            // assert container never locked for XPAGE
            assertFalse("XPage container should never get locked!!",
                    container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));

            // container should have timestamp updated
            assertTrue(container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED));


            assertFalse("No variants left so prefixes expected to be removed",
                    container.getNode("banner").hasProperty(COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES));

            final DocumentWorkflow documentWorkflow = getDocumentWorkflow(session);
            assertEquals("Expected changes hence publishable", TRUE, documentWorkflow.hints().get("publish"));

        } finally {
            session.logout();
        }
    }

    @Test
    public void create_extra_variant_while_already_one_exists() throws Exception {
        final Set<String> set = Stream.of(new String[]{"", "variant1", "newvariant"}).collect(Collectors.toSet());
        createVariantExpectations("newvariant", set);
    }

    private void createVariantExpectations(final String newVariant, final Set<String> variantsExpected) throws RepositoryException, IOException, ServletException {
        final Session session = createSession(ADMIN_CREDENTIALS);

        try {

            final String mountId = getNodeId(session, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final String componentItemId = getNodeId(session, unpublishedExpPageVariant.getPath() + "/hst:page/body/container/banner");

            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + componentItemId + "./" + newVariant, null, "POST");

            // Do it as author which : author should be allowed
            final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS);

            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());


            session.refresh(false);
            final Node container = session.getNode(unpublishedExpPageVariant.getPath() + "/hst:page/body/container");

            // assert container never locked for XPAGE
            assertFalse("XPage container should never get locked!!",
                    container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));

            // container should have timestamp updated
            assertTrue(container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED));


            final List<String> prefixes = JcrUtils.getStringListProperty(container.getNode("banner"), COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES, Collections.emptyList());

            assertEquals(variantsExpected, new HashSet(prefixes));

        } finally {
            session.logout();
        }
    }

    @Test
    public void create_extra_variant_while_none_exists() throws Exception {
        // first remove all of them which is done in 'retain_empty_variant_removes_all_except_default'
        retain_empty_variant_removes_all_except_default();

        final Set<String> set = Stream.of(new String[]{"", "newvariant"}).collect(Collectors.toSet());
        createVariantExpectations("newvariant", set);
    }

    @Test
    public void delete_last_variant_removes_prefixes()  throws Exception{

        final Session session = createSession(ADMIN_CREDENTIALS);

        try {

            final String mountId = getNodeId(session, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final String componentItemId = getNodeId(session, unpublishedExpPageVariant.getPath() + "/hst:page/body/container/banner");

            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + componentItemId + "./variant1", null, "DELETE");

            // Do it as author which : author should be allowed
            final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());


            session.refresh(false);
            final Node container = session.getNode(unpublishedExpPageVariant.getPath() + "/hst:page/body/container");

            // assert container never locked for XPAGE
            assertFalse("XPage container should never get locked!!",
                    container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));

            // container should have timestamp updated
            assertTrue(container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED));

            assertFalse(container.getNode("banner").hasProperty(COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES));

        } finally {
            session.logout();
        }
    }

    @Test
    public void delete_not_last_variant() throws Exception {
        // first create new variant
        final Set<String> set = Stream.of(new String[]{"", "variant1", "newvariant"}).collect(Collectors.toSet());
        createVariantExpectations("newvariant", set);
        final Session session = createSession(ADMIN_CREDENTIALS);

        try {

            final String mountId = getNodeId(session, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final String componentItemId = getNodeId(session, unpublishedExpPageVariant.getPath() + "/hst:page/body/container/banner");

            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + componentItemId + "./variant1", null, "DELETE");

            // Do it as author which : author should be allowed
            final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            session.refresh(false);
            final Node container = session.getNode(unpublishedExpPageVariant.getPath() + "/hst:page/body/container");

            // assert container never locked for XPAGE
            assertFalse("XPage container should never get locked!!",
                    container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));

            // container should have timestamp updated
            assertTrue(container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED));

            final List<String> prefixes = JcrUtils.getStringListProperty(container.getNode("banner"), COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES, Collections.emptyList());

            assertEquals(Stream.of(new String[]{"", "newvariant"}).collect(Collectors.toSet()), new HashSet(prefixes));

        } finally {
            session.logout();
        }

    }

    @Test
    public void delete_non_existing_variant_is_a_bas_request() throws Exception {

        final String mountId = getNodeId( "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String componentItemId = getNodeId( unpublishedExpPageVariant.getPath() + "/hst:page/body/container/banner");

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + componentItemId + "./non-existing", null, "DELETE");

        // Do it as author which : author should be allowed
        final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

    }

    @Test
    public void update_params_for_variant() throws Exception {
        final Session session = createSession(ADMIN_CREDENTIALS);

        try {

            final String mountId = getNodeId(session, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final String componentItemId = getNodeId(session, unpublishedExpPageVariant.getPath() + "/hst:page/body/container/banner");

            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + componentItemId + "./variant1",
                    "path=/my/new/value&newparam=newvalue", "PUT");


            final MultivaluedMap<String, String> updatedParams = new MultivaluedHashMap<>();
            updatedParams.putSingle("path", "/my/new/value");
            updatedParams.putSingle("newparam", "newvalue");

            final MockHttpServletRequest request = requestResponse.getRequest();
            request.setContent(objectMapper.writeValueAsBytes(updatedParams));
            request.setContentType("application/json;charset=UTF-8");

            // Do it as author which : author should be allowed
            final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            session.refresh(false);
            final Node container = session.getNode(unpublishedExpPageVariant.getPath() + "/hst:page/body/container");

            // assert container never locked for XPAGE
            assertFalse("XPage container should never get locked!!",
                    container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));

            // container should have timestamp updated
            assertTrue(container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED));

            ContainerItemHelper cih = HstServices.getComponentManager().getComponent("containerItemHelper", "org.hippoecm.hst.pagecomposer");

            final HstComponentParameters parameters = new HstComponentParameters(container.getNode("banner"), cih);
            // assert parameters updated
            assertEquals("/my/new/value", parameters.getValue("variant1", "path"));
            assertEquals("newvalue", parameters.getValue("variant1", "newparam"));

        } finally {
            session.logout();
        }
    }

    @Test
    public void update_params_and_rename_variant() throws Exception {

        final Session session = createSession(ADMIN_CREDENTIALS);

        try {

            final String mountId = getNodeId(session, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final String componentItemId = getNodeId(session, unpublishedExpPageVariant.getPath() + "/hst:page/body/container/banner");

            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + componentItemId + "./variant1",
                    "path=/my/new/value&newparam=newvalue", "PUT");


            final MultivaluedMap<String, String> updatedParams = new MultivaluedHashMap<>();
            updatedParams.putSingle("path", "/my/new/value");
            updatedParams.putSingle("newparam", "newvalue");

            final MockHttpServletRequest request = requestResponse.getRequest();
            request.setContent(objectMapper.writeValueAsBytes(updatedParams));
            request.setContentType("application/json;charset=UTF-8");

            request.addHeader("Move-To", "moveToName");

            // Do it as author which : author should be allowed
            final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            session.refresh(false);
            final Node container = session.getNode(unpublishedExpPageVariant.getPath() + "/hst:page/body/container");

            // assert container never locked for XPAGE
            assertFalse("XPage container should never get locked!!",
                    container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));

            // container should have timestamp updated
            assertTrue(container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED));

            ContainerItemHelper cih = HstServices.getComponentManager().getComponent("containerItemHelper", "org.hippoecm.hst.pagecomposer");

            final HstComponentParameters parameters = new HstComponentParameters(container.getNode("banner"), cih);
            // assert parameters updated and new variant name 'moveToName'
            assertEquals("/my/new/value", parameters.getValue("moveToName", "path"));
            assertEquals("newvalue", parameters.getValue("moveToName", "newparam"));
            assertNull(parameters.getValue("variant1", "newparam"));

        } finally {
            session.logout();
        }
    }
}
