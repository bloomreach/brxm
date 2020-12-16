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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageContainerItemComponentResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstComponentParameters;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.testutils.log4j.Log4jInterceptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.util.JcrUtils.getMultipleStringProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class XPageContainerItemComponentResourceTest extends AbstractXPageComponentResourceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void get_container_item() throws Exception {

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String componentItemId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");

        getComponentItemAs(ADMIN_CREDENTIALS, mountId, componentItemId);
        getComponentItemAs(EDITOR_CREDENTIALS, mountId, componentItemId);
        // author is also allowed to do a GET on XPageContainerItemComponentResource.getVariant()
        getComponentItemAs(AUTHOR_CREDENTIALS, mountId, componentItemId);


        final String componentItemIdNewStyle = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner-new-style");

        getComponentItemAs(ADMIN_CREDENTIALS, mountId, componentItemIdNewStyle);
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
        assertEquals("common/aboutfolder/about-us", propertyRepresentation.get("value"));

    }

    @Test
    public void get_container_item_of_branched_xpage_for_VERSIONED_XPage() throws Exception {

        final Node frozenBannerComponent = getFrozenBannerComponent();

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
        getComponentItemAs(ADMIN_CREDENTIALS, mountId, frozenBannerComponent.getIdentifier());
        getComponentItemAs(EDITOR_CREDENTIALS, mountId, frozenBannerComponent.getIdentifier());
        // author is also allowed to do a GET on XPageContainerItemComponentResource.getVariant()
        getComponentItemAs(AUTHOR_CREDENTIALS, mountId, frozenBannerComponent.getIdentifier());
    }

    @Test
    public void get_container_item_new_style_of_branched_xpage_for_VERSIONED_XPage() throws Exception {

        final Node frozenBannerNewStyleComponent = getFrozenBannerNewStyleComponent();

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
        getComponentItemAs(ADMIN_CREDENTIALS, mountId, frozenBannerNewStyleComponent.getIdentifier());
        getComponentItemAs(EDITOR_CREDENTIALS, mountId, frozenBannerNewStyleComponent.getIdentifier());
        // author is also allowed to do a GET on XPageContainerItemComponentResource.getVariant()
        getComponentItemAs(AUTHOR_CREDENTIALS, mountId, frozenBannerNewStyleComponent.getIdentifier());
    }


    @Test
    public void get_container_item_published_variant_not_allowed() throws Exception {

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String componentItemId = getNodeId(admin, publishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + componentItemId + "./hippo-default/en", null, "GET");


        final MockHttpServletResponse response = render(mountId, requestResponse, ADMIN_CREDENTIALS);

        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());

    }

    @Test
    public void retain_existing_variant_container_item_also_remains_default() throws Exception {
        expectationsRetainingVariant(new String[]{"variant1"}, false);
    }

    @Test
    public void retain_existing_and_non_existing_variant_container_item_also_remains_default() throws Exception {
        expectationsRetainingVariant(new String[]{"variant1", "non-existing"}, false);
    }

    @Test
    public void retain_existing_variant_container_item_also_remains_default_for_VERSIONED_XPage() throws Exception {
        expectationsRetainingVariant(new String[]{"variant1"}, true);
    }


    private void expectationsRetainingVariant(final String[] retainVariants, final boolean versionedXPageTest) throws Exception {

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String componentItemId;
        if (versionedXPageTest) {
            componentItemId = getFrozenBannerComponent().getIdentifier();
            // assert current unpublished variant is for a branch and not for MASTER due to getFrozenBannerComponent
            assertThat(unpublishedExpPageVariant.hasProperty(HIPPO_PROPERTY_BRANCH_ID)).isTrue();
        } else {
            componentItemId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");
        }


        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + componentItemId, null, "POST");


        requestResponse.getRequest().setContent(objectMapper.writeValueAsBytes(retainVariants));
        requestResponse.getRequest().setContentType("application/json;charset=UTF-8");

        // Do it as author which : author should be allowed
        final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        if (versionedXPageTest) {
            // assert current unpublished variant is now for MASTER since master should have been checked out
            assertThat(unpublishedExpPageVariant.hasProperty(HIPPO_PROPERTY_BRANCH_ID)).isFalse();
        }

        // assert container never locked for XPage, and not publishable since it did NOT change
        assertFalse("XPage container should never get locked!!",
                admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6").hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));

        admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner").getProperty(COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES);

        final String[] variants = getMultipleStringProperty(admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner"),
                COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES, null);

        assertThat(variants)
                .as("Expected the default (empty) variant and 'variant1' to be present")
                .containsExactly("", "variant1");


        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(admin);
        assertEquals("Expected no changes because retained all the variants",
                FALSE, documentWorkflow.hints().get("publish"));

        assertRequiresReload(response, versionedXPageTest);

    }

    @Test
    public void retain_empty_variant_removes_all_except_default() throws Exception {

        final Node container = admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");
        assertTrue("Container has some variants bootstrapped",
                container.getNode("banner").hasProperty(COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES));

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String componentItemId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + componentItemId, null, "POST");


        final MockHttpServletRequest request = requestResponse.getRequest();
        request.setContent(objectMapper.writeValueAsBytes(new String[]{}));
        request.setContentType("application/json;charset=UTF-8");


        // Do it as author which : author should be allowed
        final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());


        admin.refresh(false);

        // assert container never locked for XPAGE
        assertFalse("XPage container should never get locked!!",
                container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));

        // container should have timestamp updated
        assertTrue(container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED));


        assertFalse("No variants left so prefixes expected to be removed",
                container.getNode("banner").hasProperty(COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES));

        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(admin);
        assertEquals("Expected changes hence publishable", TRUE, documentWorkflow.hints().get("publish"));


    }

    @Test
    public void create_extra_variant_while_already_one_exists() throws Exception {
        final Set<String> set = Stream.of(new String[]{"hippo-default", "variant1", "newvariant"}).collect(Collectors.toSet());
        createVariantExpectations("newvariant", set, false);
    }

    @Test
    public void create_extra_variant_while_already_one_exists_for_VERSIONED_XPage() throws Exception {
        final Set<String> set = Stream.of(new String[]{"hippo-default", "variant1", "newvariant"}).collect(Collectors.toSet());
        createVariantExpectations("newvariant", set, true);
    }

    /**
     * Note quite awkward but the frontend code invokes XPageContainerItemComponentResource#moveAndUpdateVariant() for
     * creating a new variant, with a PUT method, hence the somewhat unexpected call below returning also a 200 instead
     * of a CREATED response code
     */
    private void createVariantExpectations(final String newVariant, final Set<String> variantsExpected,
                                           final boolean frozenXPageTest) throws Exception {

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String componentItemId;
        if (frozenXPageTest) {
            componentItemId = getFrozenBannerComponent().getIdentifier();
            // assert current unpublished variant is for a branch and not for MASTER due to getFrozenBannerComponent
            assertThat(unpublishedExpPageVariant.hasProperty(HIPPO_PROPERTY_BRANCH_ID)).isTrue();
        } else {
            componentItemId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");
        }

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + componentItemId + "./hippo-new-configuration", null, "PUT");

        requestResponse.getRequest().addHeader("Move-To", newVariant);


        final MultivaluedMap<String, String> updatedParams = new MultivaluedHashMap<>();
        updatedParams.putSingle("path", "/my/new/value");
        updatedParams.putSingle("newparam", "newvalue");

        final MockHttpServletRequest request = requestResponse.getRequest();
        request.setContent(objectMapper.writeValueAsBytes(updatedParams));
        request.setContentType("application/json;charset=UTF-8");

        // Do it as author which : author should be allowed
        final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());


        admin.refresh(false);
        final Node container = admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");

        // assert container never locked for XPAGE
        assertFalse("XPage container should never get locked!!",
                container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));

        // container should have timestamp updated
        assertTrue(container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED));

        ContainerItemHelper cih = HstServices.getComponentManager().getComponent("containerItemHelper", "org.hippoecm.hst.pagecomposer");
        final HstComponentParameters parameters = new HstComponentParameters(container.getNode("banner"), cih);

        assertThat(parameters.getPrefixes())
                .isEqualTo(variantsExpected);

        // assert parameters updated
        assertEquals("/my/new/value", parameters.getValue("newvariant", "path"));
        assertEquals("newvalue", parameters.getValue("newvariant", "newparam"));

        assertRequiresReload(response, frozenXPageTest);
    }

    @Test
    public void create_extra_variant_while_none_exists() throws Exception {
        // first remove all of them which is done in 'retain_empty_variant_removes_all_except_default'
        retain_empty_variant_removes_all_except_default();

        final Set<String> set = Stream.of(new String[]{"hippo-default", "newvariant"}).collect(Collectors.toSet());
        createVariantExpectations("newvariant", set, false);
    }

    @Test
    public void create_extra_variant_while_none_exists_for_VERSIONED_XPage() throws Exception {
        // first remove all of them which is done in 'retain_empty_variant_removes_all_except_default'
        retain_empty_variant_removes_all_except_default();

        final Set<String> set = Stream.of(new String[]{"hippo-default", "newvariant"}).collect(Collectors.toSet());
        createVariantExpectations("newvariant", set, true);
    }


    @Test
    public void delete_last_variant_removes_prefixes() throws Exception {

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String componentItemId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + componentItemId + "./variant1", null, "DELETE");

        // Do it as author which : author should be allowed
        final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());


        admin.refresh(false);
        final Node container = admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");

        // assert container never locked for XPAGE
        assertFalse("XPage container should never get locked!!",
                container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));

        // container should have timestamp updated
        assertTrue(container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED));

        assertFalse(container.getNode("banner").hasProperty(COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES));

    }

    @Test
    public void delete_not_last_variant() throws Exception {
        // first create new variant
        final Set<String> set = Stream.of(new String[]{"hippo-default", "variant1", "newvariant"}).collect(Collectors.toSet());
        createVariantExpectations("newvariant", set, false);

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String componentItemId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + componentItemId + "./variant1", null, "DELETE");

        // Do it as author which : author should be allowed
        final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        admin.refresh(false);
        final Node container = admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");

        // assert container never locked for XPAGE
        assertFalse("XPage container should never get locked!!",
                container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));

        // container should have timestamp updated
        assertTrue(container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED));

        final List<String> prefixes = JcrUtils.getStringListProperty(container.getNode("banner"), COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES, Collections.emptyList());

        assertEquals(Stream.of(new String[]{"", "newvariant"}).collect(Collectors.toSet()), new HashSet(prefixes));

    }

    @Test
    public void delete_variant_for_VERSIONED_XPage() throws Exception {
        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final Node frozenBannerComponent = getFrozenBannerComponent();

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + frozenBannerComponent.getIdentifier() + "./variant1", null, "DELETE");

        // Do it as author which : author should be allowed
        final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        admin.refresh(false);
        final Node container = admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");

        // container should have timestamp updated
        assertTrue(container.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED));
        assertFalse(container.getNode("banner").hasProperty(COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES));

        // expected version check out
        assertRequiresReload(response, true);
    }

    @Test
    public void delete_non_existing_variant_is_a_bad_request() throws Exception {

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String componentItemId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + componentItemId + "./non-existing", null, "DELETE");

        // Do it as author which : author should be allowed
        try (Log4jInterceptor ignore = Log4jInterceptor.onWarn().deny(XPageContainerItemComponentResource.class).build()) {
            final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            // no version checkout so not reload expected
            assertRequiresReload(response, false);
        }

    }

    @Test
    public void delete_non_existing_variant_is_a_bad_request_for_VERSIONED_XPage() throws Exception {

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final Node frozenBannerComponent = getFrozenBannerComponent();

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + frozenBannerComponent.getIdentifier() + "./non-existing", null, "DELETE");

        // Do it as author which : author should be allowed
        try (Log4jInterceptor ignore = Log4jInterceptor.onWarn().deny(XPageContainerItemComponentResource.class).build()) {
            final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

            // Even though the variant did not exist, we first did a check out from version history, so most likely a
            // reload is required, however for now, we do not yet include 'reload' for bad request / server errors
            assertRequiresReload(response, false);
        }

    }

    @Test
    public void update_params_for_variant() throws Exception {

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String componentItemId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");

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

        // not a frozen node checkout involved, hence no reload of page needed
        assertRequiresReload(response, false);

        admin.refresh(false);
        final Node container = admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");

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

    }

    @Test
    public void update_params_and_rename_variant() throws Exception {


        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String componentItemId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");

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

        // not a frozen node checkout involved, hence no reload of page needed
        assertRequiresReload(response, false);

        final ResponseRepresentation responseRepresentation = mapper.readerFor(ResponseRepresentation.class).readValue(response.getContentAsString());

        final Map<String, Object> data = (Map<String, Object>) responseRepresentation.getData();

        final Node container = admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");

        assertEquals("response should have the component item id", data.get("id"), componentItemId);

        admin.refresh(false);

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

    }


    @Test
    public void update_params_for_variant_for_VERSIONED_XPage() throws Exception {

        ContainerItemHelper cih = HstServices.getComponentManager().getComponent("containerItemHelper", "org.hippoecm.hst.pagecomposer");

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        // this is the componentItemId of the banner for the 'Foo' branch : we'll assert later that AFTER 'foo' has moved
        // to VERSION HISTORY, we can still update 'Foo' branch wich this componentItemId!! This MUST be supported since
        // it is equivalent to CMS User 1 looking at master version in CM, after which CMS User 2 in content editor
        // checks out branch 'foo' and modifies it, after which CMS User 1 does his/her action in the CM : this means
        // that after the page was rendered in the CM but before it was modified, the workspace version got replaced. The
        // only way this doesn't work is IF the 'foo' version does not have the 'banner' component with the same UUID.
        // in this case an exception is expected.
        final String componentItemId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");

        {
            final Node frozenBannerComponent = getFrozenBannerComponent();

            // unpublished is for branch 'foo', where after modifying the frozen 'master' banner, we expect 'master' checked out
            assertThat(unpublishedExpPageVariant.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString()).isEqualTo("foo");


            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + frozenBannerComponent.getIdentifier() + "./variant1",
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

            // frozen node check out involved, hence a page reload is required
            assertRequiresReload(response, true);

            admin.refresh(false);

            // assert the unpublished variant is NOW for MASTER although it was for 'foo' before
            assertThat(unpublishedExpPageVariant.hasProperty(HIPPO_PROPERTY_BRANCH_ID)).isFalse();

            final Node container = admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");

            final HstComponentParameters parameters = new HstComponentParameters(container.getNode("banner"), cih);
            // assert parameters updated
            assertEquals("/my/new/value", parameters.getValue("variant1", "path"));
            assertEquals("newvalue", parameters.getValue("variant1", "newparam"));
        }

        final String componentItemIdMasterBanner = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");

        assertEquals("Even though via version history restored, UUIDs between different branches are in general " +
                "the same.", componentItemIdMasterBanner, componentItemId);

        // now componentItemId which was fetched from the workspace 'foo' branch we use to modify the 'Foo' branch
        // which is already in version history now!!
        {
            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + componentItemId + "./variant1",
                    "path=/my/new/value&newparam=newvalueFOO", "PUT");
            final MultivaluedMap<String, String> updatedParams = new MultivaluedHashMap<>();
            updatedParams.putSingle("path", "/my/new/value");
            updatedParams.putSingle("newparam", "newvalueFOO");

            final MockHttpServletRequest request = requestResponse.getRequest();
            request.setContent(objectMapper.writeValueAsBytes(updatedParams));
            request.setContentType("application/json;charset=UTF-8");

            // Do it as author which : author should be allowed
            final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS, "foo");

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // assert the unpublished variant is NOW for 'foo' again!
            assertThat(unpublishedExpPageVariant.hasProperty(HIPPO_PROPERTY_BRANCH_ID)).isTrue();

            final Node container = admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");

            final HstComponentParameters parameters = new HstComponentParameters(container.getNode("banner"), cih);
            // assert parameters updated
            assertEquals("/my/new/value", parameters.getValue("variant1", "path"));
            assertEquals("newvalueFOO", parameters.getValue("variant1", "newparam"));
        }

        // now it becomes even more specific: we now delete the BANNER for the workspace 'foo' node. Now we will see
        // that it is not possible to use 'componentItemIdMasterBanner' any more: We'll get an exception since there is
        // no node for componentItemIdMasterBanner UUID any more...only frozen nodes with jcr:frozenUUID equal to
        // componentItemIdMasterBanner, however, we cannot lookup those nodes. Hence in this case, the CM will return
        // an error after which the CM reloads, not a big deal

        admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner").remove();
        admin.save();

        {
            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + componentItemIdMasterBanner + "./variant1",
                    "path=/my/new/value&newparam=newvalueMaster", "PUT");
            final MultivaluedMap<String, String> updatedParams = new MultivaluedHashMap<>();
            updatedParams.putSingle("path", "/my/new/value");
            updatedParams.putSingle("newparam", "newvalueMaster");

            final MockHttpServletRequest request = requestResponse.getRequest();
            request.setContent(objectMapper.writeValueAsBytes(updatedParams));
            request.setContentType("application/json;charset=UTF-8");

            // Do it as author which : author should be allowed
            final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS);

            assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

        }
    }


    /**
     * This test is very similar only the 'workspace' MASTER container ID is used *BUT* after that, unpublished version is
     * replaced with a branched version, and then the 'master' branch is changed: the UUID points to a 'workspace'
     * version which *might* not exist any more but in general does exist and in that case, the request can be fulfilled
     *
     */
    @Test
    public void update_params_for_variant_which_got_moved_to_VERSIONED_PAGE() throws Exception {

        final String componentItemId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");

        // now make sure branch 'foo' is the unpublished version
        getFrozenBannerComponent();

        // unpublished is for branch 'foo', where after modifying the frozen 'master' banner, we expect 'master' checked out
        assertThat(unpublishedExpPageVariant.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString()).isEqualTo("foo");

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        // componentItemId is for the 'master' unpublished workspace version while master is now already moved to
        // version history! The request however should still succeed since the UUID happen to match. Master should
        // be checked out from version history
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

        // frozen node check out involved, hence a page reload is required
        assertRequiresReload(response, true);

        admin.refresh(false);

        // assert the unpublished variant is NOW for MASTER although it was for 'foo' before
        assertThat(unpublishedExpPageVariant.hasProperty(HIPPO_PROPERTY_BRANCH_ID)).isFalse();

        final Node container = admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");

        ContainerItemHelper cih = HstServices.getComponentManager().getComponent("containerItemHelper", "org.hippoecm.hst.pagecomposer");

        final HstComponentParameters parameters = new HstComponentParameters(container.getNode("banner"), cih);
        // assert parameters updated
        assertEquals("/my/new/value", parameters.getValue("variant1", "path"));
        assertEquals("newvalue", parameters.getValue("variant1", "newparam"));

    }


    @Test
    public void update_params_and_rename_variant_for_variant_for_VERSIONED_XPage() throws Exception {
        final Node frozenBannerComponent = getFrozenBannerComponent();

        // unpublished is for branch 'foo', where after modifying the frozen 'master' banner, we expect 'master' checked out
        assertThat(unpublishedExpPageVariant.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString()).isEqualTo("foo");

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + frozenBannerComponent.getIdentifier() + "./variant1",
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

        // frozen node check out involved, hence a page reload is required
        assertRequiresReload(response, true);

        final ResponseRepresentation responseRepresentation = mapper.readerFor(ResponseRepresentation.class).readValue(response.getContentAsString());

        final Map<String, Object> data = (Map<String, Object>) responseRepresentation.getData();

        final Node container = admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");

        assertEquals("The data in the response should have the uuid of the WORKSPACE version " +
                "of the container item", data.get("id"), container.getNode("banner").getIdentifier());
        assertNotEquals(data.get("id"), frozenBannerComponent.getIdentifier());

        admin.refresh(false);

        // assert the unpublished variant is NOW for MASTER although it was for 'foo' before
        assertThat(unpublishedExpPageVariant.hasProperty(HIPPO_PROPERTY_BRANCH_ID)).isFalse();


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
    }


    /**
     * XPage docs can also have static components which can also have containers with container items which are not
     * linked back to an XPage Layout container
     * @throws Exception
     */
    @Test
    public void get_and_modify_container_item_from_NON_XPAGE_LAYOUT_container() throws Exception {
        try {
            // ******* SETUP FIXTURE ************ //
            JcrUtils.copy(admin, EXPERIENCE_PAGE_WITH_STATIC_COMPONENTS_HANDLE_PATH, "/backupXPage");
            admin.save();

            // make sure the unpublished variant exists (just by depublishing for now....)
            final WorkflowManager workflowManager = ((HippoSession) admin).getWorkspace().getWorkflowManager();

            handle = admin.getNode(EXPERIENCE_PAGE_WITH_STATIC_COMPONENTS_HANDLE_PATH);
            final DocumentWorkflow documentWorkflow = (DocumentWorkflow) workflowManager.getWorkflow("default", handle);
            documentWorkflow.depublish();
            // and publish again such that there is a live variant
            documentWorkflow.publish();

            // ******* END SETUP FIXTURE ************ //
            final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            String componentItemId = getVariant(handle, "unpublished").getNode("hst:xpage/header/header-container-xpage-doc-only/banner-new-style").getIdentifier();
            {
                // READ ITEM
                final RequestResponseMock requestResponse = mockGetRequestResponse(
                        "http", "localhost", "/_rp/" + componentItemId + "./hippo-default/en", null, "GET");

                final MockHttpServletResponse response = render(mountId, requestResponse, ADMIN_CREDENTIALS);
                final String restResponse = response.getContentAsString();

                final Map<String, Object> responseMap = mapper.readerFor(Map.class).readValue(restResponse);

                // see default BannerComponentInfo
                List<Map<String, String>> properties = (List) responseMap.get("properties");
                assertEquals(1, properties.size());

                Map<String, String> propertyRepresentation = properties.get(0);

                assertEquals("path", propertyRepresentation.get("name"));
                assertEquals("/some/default", propertyRepresentation.get("defaultValue"));
                assertEquals("", propertyRepresentation.get("value"));
            }
            {
                // UPDATE ITEM
                final RequestResponseMock requestResponse = mockGetRequestResponse(
                        "http", "localhost", "/_rp/" + componentItemId + "./variant1",
                        "path=/my/new/value&newparam=newvalue", "PUT");


                final MultivaluedMap<String, String> updatedParams = new MultivaluedHashMap<>();
                updatedParams.putSingle("path", "/my/new/value");

                final MockHttpServletRequest request = requestResponse.getRequest();
                request.setContent(objectMapper.writeValueAsBytes(updatedParams));
                request.setContentType("application/json;charset=UTF-8");

                // Do it as author which : author should be allowed
                final MockHttpServletResponse response = render(mountId, requestResponse, AUTHOR_CREDENTIALS);

                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

                assertThat(getMultipleStringProperty(admin.getNodeByIdentifier(componentItemId), GENERAL_PROPERTY_PARAMETER_VALUES, null))
                        .containsExactly("/my/new/value");
            }
        } finally {
            if (admin.nodeExists("/backupXPage")) {
                admin.getNode(EXPERIENCE_PAGE_WITH_STATIC_COMPONENTS_HANDLE_PATH).remove();
                admin.move("/backupXPage", EXPERIENCE_PAGE_WITH_STATIC_COMPONENTS_HANDLE_PATH);
                admin.save();
            }
        }
    }
}
