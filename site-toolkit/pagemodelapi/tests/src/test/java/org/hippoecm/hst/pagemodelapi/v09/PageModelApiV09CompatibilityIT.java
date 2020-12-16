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
package org.hippoecm.hst.pagemodelapi.v09;


import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.pagemodelapi.common.AbstractPageModelApiITCases;
import org.hippoecm.hst.pagemodelapi.common.context.ApiVersionProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_HST_LINK_URL_PREFIX;
import static org.hippoecm.hst.pagemodelapi.common.context.ApiVersionProvider.ApiVersion.V09;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_DOCBASE;

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
public class PageModelApiV09CompatibilityIT extends AbstractPageModelApiITCases {

    @Before
    public void setUp() {
        ApiVersionProvider.set(V09);
    }

    @After
    public void tearDown() {
        ApiVersionProvider.clear();
    }

    @Test
    public void homepage_api_compatibility_v09_assertion() throws Exception {

        String actual = getActualJson("/spa/resourceapi");

        InputStream inputStream = PageModelApiV09CompatibilityIT.class.getResourceAsStream("pma_spec_homepage.json");

        String expected = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
    }

    @Test
    public void channel_manager_preview_homepage_api_compatibility_v09_assertion() throws Exception {

        String actual = getActualJson("/_cmsinternal/spa/resourceapi", "0.9",
                ContainerConstants.RENDERING_HOST  +"=localhost", EDITOR_CREDS);

        InputStream inputStream = PageModelApiV09CompatibilityIT.class.getResourceAsStream("preview_channel_mgr_pma_spec_homepage.json");

        String expected = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
    }

    @Test
    public void newspage_api_compatibility_v09_assertion() throws Exception {

        String actual = getActualJson("/spa/resourceapi/news/News1");

        InputStream inputStream = PageModelApiV09CompatibilityIT.class.getResourceAsStream("pma_spec_newspage.json");

        String expected = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
    }

    @Test
    public void dynamic_content_api_compatibility_v09_assertion() throws Exception {

        String actual = getActualJson("/spa/resourceapi/genericdetail/dynamiccontent");

        InputStream inputStream = PageModelApiV09CompatibilityIT.class.getResourceAsStream("pma_spec_dynamiccontent.json");

        String expected = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
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

            String actual = getActualJson("/spa/resourceapi");

            InputStream inputStream = PageModelApiV09CompatibilityIT.class.getResourceAsStream("pma_spec_explicit_hst_url_prefix_host.json");

            String expected = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

            JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
        } finally {
            if (session != null) {
                session.getNode(LOCALHOST_JCR_PATH).getProperty(GENERAL_PROPERTY_HST_LINK_URL_PREFIX).remove();
                session.save();
                session.logout();
            }
        }
    }

    /**
     * In v0.9, the default in the the HtmlContentRewriter is to remove anchor tags of broken links completely, see
     * org.hippoecm.hst.pagemodelapi.v09.content.rewriter.HtmlContentRewriter#setRemoveAnchorTagOfBrokenLink(boolean)
     *
     *
     * In v1.0, we flip the behavior to by default output <a data-type="unknow">foo</a> for broken links such that the
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

            String actual = getActualJson("/spa/resourceapi");

            InputStream inputStream = PageModelApiV09CompatibilityIT.class.getResourceAsStream("pma_spec_homepage_broken_content_link.json");

            String expected = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

            JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
        } finally {
            linkNode.setProperty(HIPPO_DOCBASE, docbaseBefore);
            session.save();
            session.logout();
        }

    }

}
