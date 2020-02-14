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
import java.util.function.Predicate;

import javax.jcr.Session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.hippoecm.hst.pagemodelapi.common.AbstractPageModelApiITCases;
import org.hippoecm.hst.platform.configuration.hosting.MountService;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.testutils.log4j.Log4jInterceptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_HST_LINK_URL_PREFIX;
import static org.hippoecm.hst.configuration.HstNodeTypes.VIRTUALHOST_PROPERTY_CDN_HOST;


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
        super.setUp();
        DeterministicJsonPointerFactory.reset();
    }

    @Test
    public void homepage_api_compatibility_v10_assertion() throws Exception {

        String actual = getActualJson("/spa/resourceapi", "1.0");

        InputStream expected = PageModelApiV10CompatibilityIT.class.getResourceAsStream("pma_spec_homepage.json");

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

        System.out.println(actual);


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
    public void dynamic_contentblocks_api_compatibility_v10_assertion() throws Exception {

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

    private void assertions(final String actual, final InputStream expectedStream) throws IOException, JSONException {
        String expected = IOUtils.toString(expectedStream, "UTF-8");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
        JsonNode jsonNodeRoot = new ObjectMapper().readTree(expected);
        JsonValidationUtil.validateReferences(jsonNodeRoot, jsonNodeRoot);
    }

}
