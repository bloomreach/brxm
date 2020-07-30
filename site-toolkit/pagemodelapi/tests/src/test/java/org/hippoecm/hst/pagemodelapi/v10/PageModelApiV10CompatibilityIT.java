/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.Session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.Assertions;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.pagemodelapi.common.AbstractPageModelApiITCases;
import org.hippoecm.hst.pagemodelapi.common.context.ApiVersionProvider;
import org.hippoecm.hst.platform.configuration.hosting.MountService;
import org.hippoecm.repository.api.HippoNodeType;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.testutils.log4j.Log4jInterceptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_HST_LINK_URL_PREFIX;
import static org.hippoecm.hst.configuration.HstNodeTypes.VIRTUALHOST_PROPERTY_CDN_HOST;
import static org.hippoecm.hst.pagemodelapi.common.context.ApiVersionProvider.ApiVersion.V10;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_DOCBASE;
import static org.junit.Assert.assertEquals;
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
    @Override
    public void setUp() throws Exception {
        ApiVersionProvider.set(V10);
        super.setUp();
        DeterministicJsonPointerFactory.reset();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        ApiVersionProvider.clear();
    }


    @Test
    public void homepage_api_compatibility_v10_assertion() throws Exception {

        String actual = getActualJson("/spa/resourceapi", "1.0");

        InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_homepage.json");

        assertions(actual, expected);
    }

    @Test
    public void test_api_residual_parameters_v10_assertion() throws Exception {
        final String actual = getActualJson("/spa/resourceapi/residualparamstest", "1.0");

        final JsonNode root = mapper.readTree(actual);
        final String dynamicParam1 = root.path("page").path("uid2").path("meta").path("paramsInfo").path("param1")
                .asText();
        assertEquals("Field 'dynamicParameter1' does not contain expected value", "value 1 in container item",
                dynamicParam1);

        final String dynamicParam2 = root.path("page").path("uid2").path("meta").path("paramsInfo").path("param2")
                .asText();
        assertEquals("Field 'param2' does not contain expected value", "value 2 in container item", dynamicParam2);

        final int integerParam = root.path("page").path("uid2").path("meta").path("paramsInfo").path("integerParam")
                .asInt();
        assertEquals("Field 'integerParam' does not contain expected value", 15, integerParam);

        final double decimalParam = root.path("page").path("uid2").path("meta").path("paramsInfo").path("decimalParam")
                .asDouble();
        assertEquals("Field 'decimalParam' does not contain expected value", 20.5, decimalParam, 0);

        final boolean booleanParam = root.path("page").path("uid2").path("meta").path("paramsInfo").path("booleanParam")
                .asBoolean();
        assertEquals("Field 'booleanParam' does not contain expected value", true, booleanParam);

        final JsonNode paramsNode = root.path("page").path("uid2").path("meta").get("params");
        assertEquals("Params is supposed to have only one item", 1, paramsNode.size());
        assertTrue("Params child is supposed to be 'truly-residual'", paramsNode.has("truly-residual"));
        final Date dateParam = new Date(
                root.path("page").path("uid2").path("meta").path("paramsInfo").path("dateParam").asLong());

        assertEquals("Field 'dateParam' does not contain expected value",
                DateUtils.parseDate("2020-03-19T11:09:27", "yyyy-MM-dd'T'HH:mm:ss"), dateParam);

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

            String actual = getActualJson("/_cmsinternal/spa/resourceapi", "1.0", null, EDITOR_CREDS);

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


}
