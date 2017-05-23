/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cm.backend;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.junit.Test;
import org.onehippo.cm.api.model.ConfigurationModel;
import org.onehippo.testutils.jcr.event.ExpectedEvents;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests related to the handling of node types.
 */
public class ConfigurationConfigServiceNodeTypeTest extends BaseConfigurationConfigServiceTest {

    @Test
    public void expect_namespace_and_cnd_to_be_registered() throws Exception {
        final String source
                = "definitions:\n"
                + "  namespace:\n"
                + "  - prefix: test\n"
                + "    uri: http://www.onehippo.org/test/nt/1.0\n"
                + "  cnd:\n"
                + "  - |\n"
                + "    <'nt'='http://www.jcp.org/jcr/nt/1.0'>\n"
                + "    <'test'='http://www.onehippo.org/test/nt/1.0'>\n"
                + "    [test:type] > nt:base\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /node:\n"
                + "        jcr:primaryType: test:type\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectNodeAdded("/test/node", JCR_PRIMARYTYPE);

        applyDefinitions(source, expectedEvents);

        expectNode("/test/node", "[]", "[jcr:primaryType]");
        expectProp("/test/node/jcr:primaryType", PropertyType.NAME, "test:type");
    }

    @Test
    public void expect_cnd_reloads_to_work() throws Exception {
        /* Test in three steps:
         *  - step 1: load a basic cnd for a node type that does not allow sibling properties
         *  - step 2: validate that it is not possible to create a sibling property
         *  - step 3: reload the cnd, allowing a sibling property and test it is possible to load some content
         */

        // step 1
        final String startConfiguration
                = "definitions:\n"
                + "  namespace:\n"
                + "  - prefix: test2\n"
                + "    uri: http://www.onehippo.org/test/nt/2.0\n"
                + "  cnd:\n"
                + "  - |\n"
                + "    <'nt'='http://www.jcp.org/jcr/nt/1.0'>\n"
                + "    <'test2'='http://www.onehippo.org/test/nt/2.0'>\n"
                + "    [test2:type] > nt:base\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /node:\n"
                + "        jcr:primaryType: test2:type\n"
                + "";

        ConfigurationModel baseline = applyDefinitions(startConfiguration);

        expectNode("/test/node", "[]", "[jcr:primaryType]");
        expectProp("/test/node/jcr:primaryType", PropertyType.NAME, "test2:type");

        // step 2
        final String additionalPropertyConfiguration
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /node:\n"
                + "        jcr:primaryType: test2:type\n"
                + "        test2:property: value\n"
                + "";

        try {
            applyDefinitions(additionalPropertyConfiguration, baseline);
            fail("an exception should have occurred");
        } catch (Exception e) {
            assertEquals(
                    "Failed to process property '/test/node/test2:property' defined in"
                            + " [test-group/test-project/test-module-0 [string]]: no matching property definition"
                            + " found for {http://www.onehippo.org/test/nt/2.0}property",
                    e.getMessage());
        }

        // step 3
        final String reregisterConfiguration
                = "definitions:\n"
                + "  cnd:\n"
                + "  - |\n"
                + "    <'nt'='http://www.jcp.org/jcr/nt/1.0'>\n"
                + "    <'test2'='http://www.onehippo.org/test/nt/2.0'>\n"
                + "    [test2:type] > nt:base\n"
                + "     - test2:property (string)\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /node:\n"
                + "        jcr:primaryType: test2:type\n"
                + "        test2:property: value\n"
                + "";

        applyDefinitions(reregisterConfiguration, baseline);

        expectNode("/test/node", "[]", "[jcr:primaryType, test2:property]");
        expectProp("/test/node/jcr:primaryType", PropertyType.NAME, "test2:type");
        expectProp("/test/node/test2:property", PropertyType.STRING, "value");
    }

    @Test
    public void expect_debug_message_to_be_logged_upon_loading_of_cnd() throws Exception {
        final String source
                = "definitions:\n"
                + "  namespace:\n"
                + "  - prefix: test3\n"
                + "    uri: http://www.onehippo.org/test/nt/3.0\n"
                + "  cnd:\n"
                + "  - |\n"
                + "    <'nt'='http://www.jcp.org/jcr/nt/1.0'>\n"
                + "    <'test3'='http://www.onehippo.org/test/nt/3.0'>\n"
                + "    [test3:type] > nt:base";

        try (Log4jInterceptor interceptor = Log4jInterceptor.onDebug().trap(ConfigurationConfigService.class).build()) {
            applyDefinitions(source);
            assertTrue(interceptor.messages().anyMatch(m -> m.equals("processing inline CND defined in test-group/test-project/test-module-0 [string].")));
        }

        // as well as on reloading...
        try (Log4jInterceptor interceptor = Log4jInterceptor.onDebug().trap(ConfigurationConfigService.class).build()) {
            applyDefinitions(source);
            assertTrue(interceptor.messages().anyMatch(m -> m.equals("processing inline CND defined in test-group/test-project/test-module-0 [string].")));
        }
    }

    @Test
    public void expect_cnd_to_load_from_resource() throws Exception {
        final String source
                = "definitions:\n"
                + "  namespace:\n"
                + "  - prefix: example\n"
                + "    uri: http://www.onehippo.org/example/nt/1.0\n"
                + "  cnd:\n"
                + "  - resource: example.cnd\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /example:\n"
                + "        jcr:primaryType: example:type";

        try (Log4jInterceptor interceptor = Log4jInterceptor.onDebug().trap(ConfigurationConfigService.class).build()) {
            applyDefinitions(source);
            assertTrue(interceptor.messages().anyMatch(m -> m.equals("processing CND 'example.cnd' defined in test-group/test-project/test-module-0 [string].")));
        }
    }

    @Test
    public void expect_parse_error_in_cnd() throws Exception {
        final String source
                = "definitions:\n"
                + "  cnd:\n"
                + "  - |\n"
                + "    <'nt'='http://www.jcp.org/jcr/nt/1.0'>\n"
                + "    [unknown:type] > nt:unstructured\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /node:\n"
                + "        jcr:primaryType: test:type\n"
                + "";

        try {
            applyDefinitions(source);
            fail("an exception should have occurred");
        } catch (RepositoryException e) {
            assertTrue(e.getMessage().contains("Failed to parse cnd test-group/test-project/test-module-0 [string]"));
        }
    }
}
