/*
 * Copyright 2020-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.v10;


import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.assertj.core.api.Assertions;
import org.hippoecm.hst.component.support.bean.dynamic.MenuDynamicComponent;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.pagemodelapi.common.AbstractPageModelApiITCases;
import org.hippoecm.hst.pagemodelapi.common.context.ApiVersionProvider;
import org.hippoecm.hst.pagemodelapi.v10.core.container.PageModelAggregationValve;
import org.hippoecm.hst.platform.configuration.components.HstComponentConfigurationService;
import org.hippoecm.hst.platform.configuration.hosting.MountService;
import org.hippoecm.repository.util.JcrUtils;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.testutils.log4j.Log4jInterceptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_HST_LINK_URL_PREFIX;
import static org.hippoecm.hst.configuration.HstNodeTypes.VIRTUALHOST_PROPERTY_CDN_HOST;
import static org.hippoecm.hst.pagemodelapi.common.context.ApiVersionProvider.ApiVersion.V10;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_DOCBASE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * <p>
 *     This integration test will fail if the v09 Page Model API changes backward incompatible. Backward incompatible
 *     changes are
 *     <ol>
 *         <li>Removing fields</li>
 *         <li>Changing the value of a field, for example a site link value</li>
 *         <li>Changing the order of arrays</li>
 *     </ol>
 *     Backward compatible changes are
 *     <ol>
 *         <li>Adding new fields</li>
 *         <li>Reodering objects</li>
 *     </ol>
 * </p>
 */
public class PageModelApiV10CompatibilityIT extends AbstractPageModelApiITCases {

    @Before
    public void setUp() throws Exception {
        ApiVersionProvider.set(V10);
        DeterministicJsonPointerFactory.reset();
    }

    @After
    public void tearDown() throws Exception {
        ApiVersionProvider.clear();
    }


    @Test
    public void homepage_api_compatibility_v10_assertion() throws Exception {

        String actual = getActualJson("/spa/resourceapi", "1.0");

        InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_homepage.json");

        assertions(actual, expected);
    }

    @Test
    public void xpage_api_compatibility_v10_assertion() throws Exception {

        String actual = getActualJson("/spa/resourceapi/experiences/expPage1.html", "1.0");

        InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_experience_page_expPage1.json");

        assertions(actual, expected);
    }

    @Test
    public void homepage_api_URL_non_encoded_request_queryString_for_current_page() throws Exception {

        String actual = getActualJson("/spa/resourceapi", "1.0", "c1:page=4&c2_c1:page=6");

        InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_homepage_current_root_component_page_4.json");

        assertions(actual, expected);
    }

    /**
     * With encoded query params, the result should be the same as for not-encoded query params
     */
    @Test
    public void homepage_api_URL_encoded_request_queryString_for_current_page() throws Exception {

        // in the PMA output, the '%3A' should be replaced with ':'
        final String encoded = URLEncoder.encode(":", "utf-8");

        String actual = getActualJson("/spa/resourceapi", "1.0", "c1" + encoded + "page=4&c2_c1" +encoded + "page=6");

        InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_homepage_current_root_component_page_4.json");

        assertions(actual, expected);
    }


    @Test
    public void homepage_api_URL_encoded_queryString_returns_in_same_order() throws Exception {
        // if the querystring is something like ?b=c&a=d&e=f&i=j&g=h
        // it should be returned in that order in the PMA
        // this works since ServletRequest#getParameterMap returns an unmodifiable Map which wraps a LinkedHashMap
        String actual = getActualJson("/spa/resourceapi", "1.0", "b=c&a=d&e=f&i=j&g=h");
        InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_homepage_ordered_query_params.json");

        assertions(actual, expected);
    }

    @Test
    public void test_api_residual_parameters_v10_assertion() throws Exception {
        final String actual = getActualJson("/spa/resourceapi/residualparamstest", "1.0");

        final JsonNode root = mapper.readTree(actual);
        final String dynamicParam1 = root.path("page").path("uid2").path("meta").path("paramsInfo").path("param1").asText();
        assertEquals("Field 'dynamicParameter1' does not contain expected value", "value 1 in container item", dynamicParam1);

        final String dynamicParam2 = root.path("page").path("uid2").path("meta").path("paramsInfo").path("param2").asText();
        assertEquals("Field 'param2' does not contain expected value", "value 2 in container item", dynamicParam2);

        final int integerParam = root.path("page").path("uid2").path("meta").path("paramsInfo").path("integerParam").asInt();
        assertEquals("Field 'integerParam' does not contain expected value", 15, integerParam);

        final double decimalParam = root.path("page").path("uid2").path("meta").path("paramsInfo").path("decimalParam").asDouble();
        assertEquals("Field 'decimalParam' does not contain expected value", 20.5, decimalParam, 0);

        final boolean booleanParam = root.path("page").path("uid2").path("meta").path("paramsInfo").path("booleanParam").asBoolean();
        assertEquals("Field 'booleanParam' does not contain expected value", true, booleanParam);

        final JsonNode paramsNode = root.path("page").path("uid2").path("meta").get("params");
        assertEquals("Params is supposed to have only one item", 1, paramsNode.size());
        assertTrue("Params child is supposed to be 'truly-residual'", paramsNode.has("truly-residual"));
        final Date dateParam = new Date(root.path("page").path("uid2").path("meta").path("paramsInfo").path("dateParam").asLong());

        assertEquals("Field 'dateParam' does not contain expected value",DateUtils.parseDate("2020-03-19T11:09:27", "yyyy-MM-dd'T'HH:mm:ss"), dateParam);

        assertTrue("Field 'document' is missing", root.path("page").path("uid2").path("models").has("document"));
        final String news1Reference = root.path("page").path("uid2").path("models").path("document").path("$ref").asText();
        assertEquals("Output does not contain reference to news document", news1Reference, "/page/u303d40ebf98c4d6184c7a1ba14b5ceb3");
        assertTrue("News document is missing from the pma output", root.path("page").has("u303d40ebf98c4d6184c7a1ba14b5ceb3"));
    }

    @Test
    public void test_api_residual_parameters_default_value_v10_assertion() throws Exception {
        final String actual = getActualJson("/spa/resourceapi/residualparamstest", "1.0");

        final JsonNode root = mapper.readTree(actual);
        final String dynamicParamDefaultValue = root.path("page").path("uid2").path("meta").path("paramsInfo").path("paramWithDefaultValue").asText();
        final int paramCount = root.path("page").path("uid2").path("meta").path("paramsInfo").size();
        assertEquals("Number of parameters inside paramsinfo", 9, paramCount);
        assertEquals("Field 'paramWithDefaultValue' does not contain expected value", "a default value", dynamicParamDefaultValue);
    }

    @Test
    public void test_api_residual_parameters_menu_component_invalid_site_menu_param_v10_assertion() throws Exception {

        Session session = createSession("admin", "admin");
        Node catalogItemNode = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:pages/residualparamstestpage/container/testcatalogitemenucomponentinstance");

        final Value[] valueBefore1 = catalogItemNode.getProperty("hst:parameternames").getValues();
        final Value[] valueBefore2 = catalogItemNode.getProperty("hst:parametervalues").getValues();

        try {
            catalogItemNode.setProperty("hst:parameternames", new String[]{"menu"});
            catalogItemNode.setProperty("hst:parametervalues", new String[]{"invalid ref"});
            session.save();

            eventPathsInvalidator.eventPaths("/hst:hst/hst:configurations/unittestproject/hst:pages/residualparamstestpage/container/testcatalogitemenucomponentinstance");

            try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(MenuDynamicComponent.class).build()) {

                String actual = getActualJson("/spa/resourceapi/residualparamstest", "1.0", "_maxreflevel=0");

                final JsonNode root = mapper.readTree(actual);
                final String dynamicParamOverriddenValue = root.path("page").path("uid3").path("meta").path("paramsInfo").path("menu").asText();
                assertEquals("Field 'siteMenu' does not contain expected value", "invalid ref", dynamicParamOverriddenValue);

                List<LogEvent> messages = interceptor.getEvents();
                Assert.assertTrue(interceptor.messages().anyMatch(m -> m.equals("Invalid site menu is selected within MenuDynamicComponent: " + dynamicParamOverriddenValue)));


            }
        } finally {
            catalogItemNode.setProperty("hst:parameternames", valueBefore1);
            catalogItemNode.setProperty("hst:parametervalues", valueBefore2);
            session.logout();
        }
    }

    @Test
    public void test_api_residual_parameters_menu_component_valid_site_menu_param_v10_assertion() throws Exception {
        String actual = getActualJson("/spa/resourceapi/residualparamstest", "1.0");

        final JsonNode root = mapper.readTree(actual);
        final String validSiteMenu = root.path("page").path("uid3").path("meta").path("paramsInfo").path("menu").asText();
        assertEquals("Field 'siteMenu' does not contain expected value", "main", validSiteMenu);

        final String menuReference = root.path("page").path("uid3").path("models").path("menu").path("$ref").asText();
        assertEquals("Output does not contain reference to menu document", menuReference, "/page/uid6");
    }

    @Test
    public void test_api_residual_parameters_query_component_v10_assertion() throws Exception {
        String actual = getActualJson("/spa/resourceapi/residualparamstest", "1.0","r22_r1_r5:page=1");

        final JsonNode root = mapper.readTree(actual);

        final int paramCount = root.path("page").path("uid4").path("meta").path("paramsInfo").size();
        assertEquals("Number of parameters inside paramsinfo", 9, paramCount);

        final String scope = root.path("page").path("uid4").path("meta").path("paramsInfo").path("scope").asText();
        assertEquals("Field 'scope' does not contain expected value", "contentforquery", scope);

        final int pageSize = root.path("page").path("uid4").path("meta").path("paramsInfo").path("pageSize")
                .asInt();
        assertEquals("Field 'pageSize' does not contain expected value", 10, pageSize);

        final String sortField = root.path("page").path("uid4").path("meta").path("paramsInfo").path("sortField").asText();
        assertEquals("Field 'sortField' does not contain expected value", "", sortField);

        final String documentTypes = root.path("page").path("uid4").path("meta").path("paramsInfo").path("documentTypes").asText();
        assertEquals("Field 'documentTypes' does not contain expected value", "pagemodelapitest:contentforquery", documentTypes);

        final boolean booleanSubType = root.path("page").path("uid4").path("meta").path("paramsInfo").path("includeSubtypes")
                .asBoolean();
        assertEquals("Field 'includeSubtypes' does not contain expected value", false, booleanSubType);

        final String sortOrder = root.path("page").path("uid4").path("meta").path("paramsInfo").path("sortOrder").asText();
        assertEquals("Field 'sortOrder' does not contain expected value", "ASC", sortOrder);

        final String dateField = root.path("page").path("uid4").path("meta").path("paramsInfo").path("dateField").asText();
        assertEquals("Field 'dateField' does not contain expected value", "", dateField);

        final boolean booleanHidePastItems = root.path("page").path("uid4").path("meta").path("paramsInfo").path("hidePastItems")
                .asBoolean();
        assertEquals("Field 'hidePastItems' does not contain expected value", false, booleanHidePastItems);

        final boolean booleanFuturePastItems = root.path("page").path("uid4").path("meta").path("paramsInfo").path("hideFutureItems")
                .asBoolean();
        assertEquals("Field 'hideFutureItems' does not contain expected value", false, booleanFuturePastItems);

        assertTrue("Field 'pagination' is missing", root.path("page").path("uid4").path("models").has("pagination"));
        final String contentReference = root.path("page").path("uid4").path("models").path("pagination").path("$ref").asText();
        assertEquals("Output does not contain reference to content document", contentReference, "/page/uid7");
        assertFalse("Field 'scope' must be missing, because it's not a residual field", root.path("page").path("uid4").path("models").has("scope"));
    }

    @Test
    public void test_api_residual_parameters_query_component_pagination_v10_assertion() throws Exception {
        String actual = getActualJson("/spa/resourceapi/residualparamstest", "1.0","r22_r1_r5:page=1");

        final JsonNode root = mapper.readTree(actual);

        final int size = root.path("page").path("uid7").path("size")
                .asInt();
        assertEquals("Field 'size' does not contain expected value", 6, size);

        final int currentPage = root.path("page").path("uid7").path("current").path("number")
                .asInt();
        assertEquals("Field 'current' does not contain expected value", 1, currentPage);

        final int totalRecords = root.path("page").path("uid7").path("total")
                .asInt();
        assertEquals("Field 'total' does not contain expected value", 6, totalRecords);

        final boolean paginationFlag = root.path("page").path("uid7").path("enabled")
                .asBoolean();
        assertEquals("Field 'enabled' does not contain expected value", true, paginationFlag);

        final int firstPage = root.path("page").path("uid7").path("first").path("number")
                .asInt();
        assertEquals("Field 'first' does not contain expected value", 1, firstPage);

        final int offset = root.path("page").path("uid7").path("offset")
                .asInt();
        assertEquals("Field 'offset' does not contain expected value", 0, offset);

        final int lastPage = root.path("page").path("uid7").path("last").path("number")
                .asInt();
        assertEquals("Field 'last' does not contain expected value", 1, lastPage);

        final int nextPage = root.path("page").path("uid7").path("next")
                .asInt();
        assertEquals("Field 'next' does not contain expected value", 0, nextPage);

        final int previousPage = root.path("page").path("uid7").path("previous")
                .asInt();
        assertEquals("Field 'previous' does not contain expected value", 0, previousPage);

        final int pageCount = root.path("page").path("uid7").path("size").asInt();
        assertEquals("Number of elements inside pages", 6, pageCount);

        final int contentItemLength = root.path("page").path("uid7").path("items").size();
        assertEquals("Number of elements inside items", 6, contentItemLength);

        JsonNode itemsArray = root.path("page").path("uid7").path("items");
        for(int counter = 0; counter<contentItemLength; counter++)
        {
            assertNotNull("Content items", itemsArray.get(counter).path("$ref"));
        }
    }

    /**
     * This test specifically confirms that _cmsinternal preview Channel Manager PMA requests will
     * <ul>
     *     <li>Have _cmsinternal in the PMA URLs</li>
     *     <li>Won't have _cmsinternal in the URLs for (container) resources in the PMA response</li>
     * </ul>
     */
    @Test
    public void channel_manager_preview_homepage_api_compatibility_v10_assertion() throws Exception {

        String actual = getActualJson("/_cmsinternal/spa/resourceapi", "1.0",
                ContainerConstants.RENDERING_HOST  +"=localhost",
                EDITOR_CREDS);

        InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("preview_channel_mgr_pma_spec_homepage_.json");

        assertions(actual, expected);
    }

    /**
     * _maxreflevel = 0 means that referenced documents from documents do not get serialized
     * also the query string should be embedded in the URLS
     * @throws Exception
     */
    @Test
    public void homepage_api_compatibility_v10_assertion_with_max_document_ref_level_0() throws Exception {

        String actual = getActualJson("/spa/resourceapi", "1.0", "_maxreflevel=0");

        InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_homepage_maxreflevel0.json");

        assertions(actual, expected);
    }

    /**
     * For partial rendering, we do want the querystring to be repeated in BOTH the 'SELF' link as well as the 'SITE'
     * link *HOWEVER* the partial rendering part should only be present in the 'SELF' link and never in the 'SITE' link
     * @throws Exception
     */
    @Test
    public void partial_homepage_api_compatibility_v10_assertion() throws Exception {

        String actual = getActualJson("/spa/resourceapi./header", "1.0", "dummy=bar");

        InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_partial_homepage.json");

        assertions(actual, expected);
    }

    /**
     * Even though explicit /home in URL we still expect main page links without 'home' (only in component rendering
     *  links the /home part will be present)
     *
     *   "links" : {
     *      "self" : {
     *"        href" : "http://localhost/site/spa/resourceapi",
     *        "type" : "external"
     *      },
     *      "site" : {
     *         "href" : "/",
     *         "type" : "internal"
     *      }
     *},
     * @throws Exception
     */
    @Test
    public void homepage_api_compatibility_v10_assertion_explicit_home() throws Exception {

        // explicitly include /home
        String actual = getActualJson("/spa/resourceapi/home", "1.0");

        InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_homepage_explicit_home.json");

        assertions(actual, expected);
    }

    @Test
    public void newspage_api_compatibility_v10_assertion() throws Exception {

        String actual = getActualJson("/spa/resourceapi/news/News1", "1.0");

        InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_newspage.json");

        assertions(actual, expected);
    }

    @Test
    public void dynamic_content_api_compatibility_v10_assertion() throws Exception {

        String actual = getActualJson("/spa/resourceapi/genericdetail/dynamiccontent", "1.0");

        InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_dynamiccontent.json");

        assertions(actual, expected);
    }


    @Test
    public void explicit_hst_link_prefix_for_page_model_mount() throws Exception {

        // since only on page model api mount configured link url prefix,
        // we expect binaries still to have the fully qualified URL to the cms host (which is localhost in this test setup)
        Session session = null;
        try {

            session = createSession("admin", "admin");

            session.getNode(SPA_MOUNT_JCR_PATH).setProperty(GENERAL_PROPERTY_HST_LINK_URL_PREFIX, "http://www.example.com");
            session.save();

            // trigger direct invalidation of model without waiting for jcr event
            eventPathsInvalidator.eventPaths(SPA_MOUNT_JCR_PATH);

            String actual = getActualJson("/spa/resourceapi", "1.0");

            InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_explicit_hst_url_prefix_mount.json");

            assertions(actual, expected);
        } finally {
            if (session != null) {
                session.getNode(SPA_MOUNT_JCR_PATH).getProperty(GENERAL_PROPERTY_HST_LINK_URL_PREFIX).remove();
                session.save();
                session.logout();
            }
        }
    }

    @Test
    public void explicit_hst_link_prefix_for_host() throws Exception {

        // since only on HOST configured link url prefix,
        // we expect binaries still to also have the prefix (which is localhost in this test setup)
        Session session = null;
        try {

            session = createSession("admin", "admin");

            session.getNode(LOCALHOST_JCR_PATH).setProperty(GENERAL_PROPERTY_HST_LINK_URL_PREFIX, "http://www.example.com");
            session.save();

            // trigger direct invalidation of model without waiting for jcr event
            eventPathsInvalidator.eventPaths(LOCALHOST_JCR_PATH);

            String actual = getActualJson("/spa/resourceapi", "1.0");

            InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_explicit_hst_url_prefix_host.json");

            assertions(actual, expected);
        } finally {
            if (session != null) {
                session.getNode(LOCALHOST_JCR_PATH).getProperty(GENERAL_PROPERTY_HST_LINK_URL_PREFIX).remove();
                session.save();
                session.logout();
            }
        }
    }

    @Test
    public void explicit_hst_link_prefix_with_path() throws Exception {

        // since only on HOST configured link url prefix,
        // we expect binaries still to also have the prefix (which is localhost in this test setup)
        Session session = null;
        try {

            session = createSession("admin", "admin");

            session.getNode(LOCALHOST_JCR_PATH).setProperty(GENERAL_PROPERTY_HST_LINK_URL_PREFIX, "http://www.example.com/withpathinfo");
            session.save();

            // trigger direct invalidation of model without waiting for jcr event
            eventPathsInvalidator.eventPaths(LOCALHOST_JCR_PATH);

            String actual = getActualJson("/spa/resourceapi", "1.0");

            InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_explicit_hst_url_with_pathinfo.json");

            assertions(actual, expected);
        } finally {
            if (session != null) {
                session.getNode(LOCALHOST_JCR_PATH).getProperty(GENERAL_PROPERTY_HST_LINK_URL_PREFIX).remove();
                session.save();
                session.logout();
            }
        }
    }

    @Test
    public void invalid_api_host_name_for_host_is_ignored() throws Exception {

        Session session = null;
        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(MountService.class).build()){

            session = createSession("admin", "admin");

            session.getNode(SPA_MOUNT_JCR_PATH).setProperty(GENERAL_PROPERTY_HST_LINK_URL_PREFIX, "invalid://www.example.com");
            session.save();

            // trigger direct invalidation of model without waiting for jcr event
            eventPathsInvalidator.eventPaths(SPA_MOUNT_JCR_PATH);

            String actual = getActualJson("/spa/resourceapi", "1.0");

            Assertions.assertThat(interceptor.messages())
                    .as("Expect only an error log for invalid hst:linkurlprefix")
                    .allMatch(s -> s.startsWith("Ignoring invalid property 'hst:linkurlprefix=invalid://www.example.com' on Mount '/hst:hst/hst:hosts/dev-localhost/localhost/hst:root/spa'"));

            // expected same result as without link url prefix
            InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_homepage.json");

            assertions(actual, expected);
        } finally {
            if (session != null) {
                session.getNode(SPA_MOUNT_JCR_PATH).getProperty(GENERAL_PROPERTY_HST_LINK_URL_PREFIX).remove();
                session.save();
                session.logout();
            }
        }
    }

    @Test
    public void cdn_host_has_precendence_over_hst_link_prefix_for_binaries() throws Exception {
        Session session = null;
        try {

            session = createSession("admin", "admin");

            session.getNode(LOCALHOST_JCR_PATH).setProperty(GENERAL_PROPERTY_HST_LINK_URL_PREFIX, "http://www.example.com");
            session.getNode(LOCALHOST_JCR_PATH).setProperty(VIRTUALHOST_PROPERTY_CDN_HOST, "http://cdn.example.com");
            session.save();

            // trigger direct invalidation of model without waiting for jcr event
            eventPathsInvalidator.eventPaths(LOCALHOST_JCR_PATH);

            String actual = getActualJson("/spa/resourceapi", "1.0");

            InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_cdn_and_hst_url_prefix.json");

            assertions(actual, expected);
        } finally {
            if (session != null) {
                session.getNode(LOCALHOST_JCR_PATH).getProperty(GENERAL_PROPERTY_HST_LINK_URL_PREFIX).remove();
                session.getNode(LOCALHOST_JCR_PATH).getProperty(VIRTUALHOST_PROPERTY_CDN_HOST).remove();
                session.save();
                session.logout();
            }
        }
    }

    /**
     * Also with CND host, no _cmsinternal should be present for (container) resources
     */
    @Test
    public void channel_manager_preview_cdn_host_and_hst_link_prefix_request() throws Exception {
        Session session = null;
        try {

            session = createSession("admin", "admin");

            session.getNode(LOCALHOST_JCR_PATH).setProperty(GENERAL_PROPERTY_HST_LINK_URL_PREFIX, "http://www.example.com");
            session.getNode(LOCALHOST_JCR_PATH).setProperty(VIRTUALHOST_PROPERTY_CDN_HOST, "http://cdn.example.com");
            session.save();

            // trigger direct invalidation of model without waiting for jcr event
            eventPathsInvalidator.eventPaths(LOCALHOST_JCR_PATH);

            String actual = getActualJson("/_cmsinternal/spa/resourceapi", "1.0",
                    ContainerConstants.RENDERING_HOST  +"=localhost", EDITOR_CREDS);

            InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("preview_channel_mgr_pma_spec_cdn_and_hst_url_prefix.json");

            assertions(actual, expected);
        } finally {
            if (session != null) {
                session.getNode(LOCALHOST_JCR_PATH).getProperty(GENERAL_PROPERTY_HST_LINK_URL_PREFIX).remove();
                session.getNode(LOCALHOST_JCR_PATH).getProperty(VIRTUALHOST_PROPERTY_CDN_HOST).remove();
                session.save();
                session.logout();
            }
        }
    }

    /**
     * The /search sitemap item does not have a relative content path and thus does not have a content bean
     */
    @Test
    public void no_request_content_bean() throws Exception {
        String actual = getActualJson("/spa/resourceapi/search", "1.0");

        InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_searchpage.json");

        assertions(actual, expected);

        // assert there is not "document" field on 'root-level' present since there is no request primary document,
        // only a "root" field for the root component
        JsonNode jsonNodeRoot = new ObjectMapper().readTree(actual);

        assertNotNull(jsonNodeRoot.get("root"));
        // See org.hippoecm.hst.pagemodelapi.v10.core.container.AggregatedPageModel.getDocument() the
        // @JsonInclude(JsonInclude.Include.NON_NULL)
        assertNull(jsonNodeRoot.get("document"));
    }

    /**
     * The /news sitemap item does have a relative content path that points to a folder
     */
    @Test
    public void no_request_content_bean_if_folder() throws Exception {

        String actual = getActualJson("/spa/resourceapi/news", "1.0");

        InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_newsfolder.json");

        assertions(actual, expected);

        // assert there is not "document" field on 'root-level' present since there is no request primary document (only folder),
        // only a "root" field for the root component
        JsonNode jsonNodeRoot = new ObjectMapper().readTree(actual);

        assertNotNull(jsonNodeRoot.get("root"));
        // See org.hippoecm.hst.pagemodelapi.v10.core.container.AggregatedPageModel.getDocument() the
        // @JsonInclude(JsonInclude.Include.NON_NULL)
        assertNull(jsonNodeRoot.get("document"));
    }

    /**
     * In v0.9, the default in the the HtmlContentRewriter is to remove anchor tags of broken links completely, see
     * org.hippoecm.hst.pagemodelapi.v09.content.rewriter.HtmlContentRewriter#setRemoveAnchorTagOfBrokenLink(boolean)
     *
     * In v1.0, we have flipped the behavior to by default output <a data-type="unknow">foo</a> for broken links such that the
     * SPA can handle it as desired
     */
    @Test
    public void broken_link_in_content_for_homepage_document() throws Exception {

        final Session session = createSession("admin", "admin");
        final Node linkNode = session.getNode("/unittestcontent/documents/unittestproject/common/homepage/homepage/unittestproject:body/news1");
        final String docbaseBefore = linkNode.getProperty(HIPPO_DOCBASE).getString();

        try {


            // break the internal link in the content of homepage to do expectation on how it gets serialized in json
            // we expecte broken links to be <a data-type="unknown"/>

            linkNode.setProperty(HIPPO_DOCBASE, "broken");
            session.save();

            String actual = getActualJson("/spa/resourceapi", "1.0");

            InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_homepage_broken_content_link.json");

            assertions(actual, expected);

        } finally {
            linkNode.setProperty(HIPPO_DOCBASE, docbaseBefore);
            session.save();
            session.logout();
        }

    }

    private void assertions(final String actual, final InputStream expectedStream) throws IOException, JSONException {
        String expected = IOUtils.toString(expectedStream, StandardCharsets.UTF_8);
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
        JsonNode jsonNodeRoot = new ObjectMapper().readTree(expected);
        JsonValidationUtil.validateReferences(jsonNodeRoot, jsonNodeRoot);
    }

    @Test
    public void test_api_residual_parameters_override_named_parameters_v10_assertion() throws Exception {
        final Session session = createSession("admin", "admin");
        final Node catalogItemNode = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:catalog/unittestpackage/testcatalogitemparameteroverriding/hideFutureItems");

        final Value valueBefore = catalogItemNode.getProperty("hst:valuetype").getValue();

        try {
            catalogItemNode.setProperty("hst:valuetype", "text");
            session.save();

            eventPathsInvalidator.eventPaths("/hst:hst/hst:configurations/unittestcommon/hst:catalog/unittestpackage/testcatalogitemparameteroverriding/hideFutureItems");

            String actual;
            try (final Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(HstComponentConfigurationService.class).build()) {
                actual = getActualJson("/spa/resourceapi/residualparamstest", "1.0");
                assertTrue("Warning was not logged",
                        interceptor.messages().anyMatch("Jcr and annotation based parameters are defined with the same name but with different type: hideFutureItems"::equals));
            }

            final JsonNode root = mapper.readTree(actual);
            final JsonNode paramsInfo = root.path("page").path("uid5").path("meta").path("paramsInfo");

            final int paramCount = paramsInfo.size();
            assertEquals("Number of parameters inside paramsinfo", 9, paramCount);

            final String sortOrder = paramsInfo.path("sortOrder").asText();
            assertEquals("Field 'sortOrder' does not contain expected value", "asc", sortOrder);

            final boolean futurePastItems = paramsInfo.path("futurePastItems").asBoolean();
            assertFalse("Field 'futurePastItems' does not contain expected value", futurePastItems);
        } finally {
            catalogItemNode.setProperty("hst:valuetype", valueBefore);
            session.save();
            session.logout();
        }
    }

    @Test
    public void test_hidden_component() throws Exception {
        {
            String actual = getActualJson("/spa/resourceapi", "1.0");
            final JsonNode root = mapper.readTree(actual);
            // assert the component 'hidden' field is missing if not hidden
            final JsonNode paramsInfo = root.path("page").path("uid1").path("meta").path("hidden");
            assertTrue(paramsInfo.isEmpty());
        }

        final Session session = createSession("admin", "admin");
        final Node headerComponentNode = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:components/header");

        String[] paramNamesBefore = JcrUtils.getMultipleStringProperty(headerComponentNode, "hst:parameternames", null);
        String[] paramValuesBefore = JcrUtils.getMultipleStringProperty(headerComponentNode, "hst:parametervalues", null);


        try {
            // mark the component to be hidden
            headerComponentNode.setProperty("hst:parameternames", new String[]{PageModelAggregationValve.HIDE_PARAMETER_NAME});
            headerComponentNode.setProperty("hst:parametervalues", new String[]{"true"});
            session.save();
            eventPathsInvalidator.eventPaths("/hst:hst/hst:configurations/unittestcommon/hst:components/header");
            DeterministicJsonPointerFactory.reset();

            {
                String actual = getActualJson("/spa/resourceapi", "1.0");
                final JsonNode root = mapper.readTree(actual);
                // assert the component is hidden
                final JsonNode paramsInfo = root.path("page").path("uid1").path("meta").path("hidden");
                assertTrue(paramsInfo.asBoolean());
            }
            // mark the component to be hidden but now through ON instead of 'true'
            headerComponentNode.setProperty("hst:parametervalues", new String[]{"ON"});
            session.save();
            eventPathsInvalidator.eventPaths("/hst:hst/hst:configurations/unittestcommon/hst:components/header");
            DeterministicJsonPointerFactory.reset();

            {
                String actual = getActualJson("/spa/resourceapi", "1.0");
                final JsonNode root = mapper.readTree(actual);
                // assert the component is hidden
                final JsonNode paramsInfo = root.path("page").path("uid1").path("meta").path("hidden");
                assertTrue(paramsInfo.asBoolean());
            }
            // mark the component to be NOT hidden but now through OFF
            headerComponentNode.setProperty("hst:parametervalues", new String[]{"OFF"});
            session.save();
            eventPathsInvalidator.eventPaths("/hst:hst/hst:configurations/unittestcommon/hst:components/header");
            DeterministicJsonPointerFactory.reset();

            {
                String actual = getActualJson("/spa/resourceapi", "1.0");
                final JsonNode root = mapper.readTree(actual);
                // assert the component is hidden
                final JsonNode paramsInfo = root.path("page").path("uid1").path("meta").path("hidden");
                assertFalse(paramsInfo.asBoolean());
            }
            // mark the component to be NOT hidden but now through "LOREM"...some falsy value
            headerComponentNode.setProperty("hst:parametervalues", new String[]{"LOREM"});
            session.save();
            eventPathsInvalidator.eventPaths("/hst:hst/hst:configurations/unittestcommon/hst:components/header");
            DeterministicJsonPointerFactory.reset();

            {
                String actual = getActualJson("/spa/resourceapi", "1.0");
                final JsonNode root = mapper.readTree(actual);
                // assert the component is hidden
                final JsonNode paramsInfo = root.path("page").path("uid1").path("meta").path("hidden");
                assertFalse(paramsInfo.asBoolean());
            }
            // mark the component to be NOT hidden but now through ""...some falsy value
            headerComponentNode.setProperty("hst:parametervalues", new String[]{""});
            session.save();
            eventPathsInvalidator.eventPaths("/hst:hst/hst:configurations/unittestcommon/hst:components/header");
            DeterministicJsonPointerFactory.reset();

            {
                String actual = getActualJson("/spa/resourceapi", "1.0");
                final JsonNode root = mapper.readTree(actual);
                // assert the component is hidden
                final JsonNode paramsInfo = root.path("page").path("uid1").path("meta").path("hidden");
                assertFalse(paramsInfo.asBoolean());
            }
        } finally {
            if (paramNamesBefore == null) {
                headerComponentNode.getProperty("hst:parameternames").remove();
            } else {
                headerComponentNode.setProperty("hst:parameternames", paramNamesBefore);
            }
            if (paramValuesBefore == null) {
                headerComponentNode.getProperty("hst:parametervalues").remove();
            } else {
                headerComponentNode.setProperty("hst:parametervalues", paramValuesBefore);
            }
            session.save();
            session.logout();
        }
    }

}
