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

package org.onehippo.cm.engine;

import java.util.Arrays;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.junit.Test;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.testutils.jcr.event.ExpectedEvents;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests related to the handling of namespaces.
 */
public class ConfigurationConfigServiceNamespaceTest extends BaseConfigurationConfigServiceTest {

    @Test
    public void expect_reapplying_unchanged_namespace_to_be_allowed() throws Exception {
        ConfigurationModel configurationModel;

        final String source
                = "definitions:\n"
                + "  namespace:\n"
                + "    test1:\n"
                + "      uri: http://www.onehippo.org/test/nt/1.0";

        configurationModel = applyDefinitions(source);

        assertTrue(getNamespacePrefixes().contains("test1"));
        assertEquals(getNamespaceURIForPrefix("test1"), "http://www.onehippo.org/test/nt/1.0");

        final String source2
                = "definitions:\n"
                + "  namespace:\n"
                + "    test1:\n"
                + "      uri: http://www.onehippo.org/test/nt/1.0";

        applyDefinitions(source2, configurationModel);

        assertTrue(getNamespacePrefixes().contains("test1"));
        assertEquals(getNamespaceURIForPrefix("test1"), "http://www.onehippo.org/test/nt/1.0");
    }

    @Test
    public void expect_adding_already_existing_namespace_to_be_allowed() throws Exception {

        assertFalse(getNamespacePrefixes().contains("foo"));
        session.getWorkspace().getNamespaceRegistry().registerNamespace("foo", "http://www.onehippo.org/foo/nt/1.0");
        assertTrue(getNamespacePrefixes().contains("foo"));

        final String source
                = "definitions:\n"
                + "  namespace:\n"
                + "    foo:\n"
                + "      uri: http://www.onehippo.org/foo/nt/1.0";

        applyDefinitions(source);

        assertTrue(getNamespacePrefixes().contains("foo"));
        assertEquals(getNamespaceURIForPrefix("foo"), "http://www.onehippo.org/foo/nt/1.0");
    }

    @Test
    public void expect_namespace_removal_to_have_no_effect() throws Exception {
        final String source
                = "definitions:\n"
                + "  namespace:\n"
                + "    test3:\n"
                + "      uri: http://www.onehippo.org/test/nt/3.0";
        ConfigurationModel configurationModel = applyDefinitions(source);

        assertTrue(getNamespacePrefixes().contains("test3"));

        final String source2
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured";
        applyDefinitions(source2, configurationModel);

        assertTrue(getNamespacePrefixes().contains("test3"));
        assertEquals(getNamespaceURIForPrefix("test3"), "http://www.onehippo.org/test/nt/3.0");
    }

    @Test
    public void expect_namespace_change_to_abort_with_exception() throws Exception {
        final String source
                = "definitions:\n"
                + "  namespace:\n"
                + "    test4:\n"
                + "      uri: http://www.onehippo.org/test/nt/4.0";
        ConfigurationModel configurationModel = applyDefinitions(source);

        final String source2
                = "definitions:\n"
                + "  namespace:\n"
                + "    test4:\n"
                + "      uri: http://www.onehippo.org/test/nt/4.0-changed";
        try {
            applyDefinitions(source2, configurationModel);
            fail("Should have thrown a RuntimeException");
        } catch (RuntimeException e) {
            assertEquals("Failed to process namespace definition defined in test-group/test-project/test-module-0 [string]: " +
                    "namespace with prefix 'test4' already exists in repository with different URI. " +
                    "Existing: 'http://www.onehippo.org/test/nt/4.0', target: 'http://www.onehippo.org/test/nt/4.0-changed'. " +
                    "Changing existing namespaces is not supported. Aborting.", e.getMessage());
        }
    }

    @Test
    public void expect_namespace_and_cnd_to_be_registered() throws Exception {
        final String source
                = "definitions:\n"
                + "  namespace:\n"
                + "    test1:\n"
                + "      uri: http://www.onehippo.org/test/nt/1.0\n"
                + "      cnd: test1.cnd\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /node:\n"
                + "        jcr:primaryType: test1:type\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectNodeAdded("/test/node", JCR_PRIMARYTYPE);

        applyDefinitions(source, expectedEvents);

        expectNode("/test/node", "[]", "[jcr:primaryType]");
        expectProp("/test/node/jcr:primaryType", PropertyType.NAME, "test1:type");
    }

    @Test
    public void expect_cnd_reloads_to_work() throws Exception {
        /* Test in three steps:
         *  - step 1: load a basic cnd for a node type that does not allow sibling properties
         *  - step 2: validate that it is not possible to create a sibling property
         *  - step 3: reload the cnd, allowing a sibling property and test it is possible to load some content
         *  - step 4: reload the identical cnd again, and expect a log message stating that the reload was skipped
         */

        // step 1
        final String startConfiguration
                = "definitions:\n"
                + "  namespace:\n"
                + "    test2:\n"
                + "      uri: http://www.onehippo.org/test/nt/2.0\n"
                + "      cnd: test2.cnd\n"
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
                + "  namespace:\n"
                + "    test2:\n"
                + "      uri: http://www.onehippo.org/test/nt/2.0\n"
                + "      cnd: test2b.cnd\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /node:\n"
                + "        jcr:primaryType: test2:type\n"
                + "        test2:property: value\n"
                + "";

        baseline = applyDefinitions(reregisterConfiguration, baseline);

        expectNode("/test/node", "[]", "[jcr:primaryType, test2:property]");
        expectProp("/test/node/jcr:primaryType", PropertyType.NAME, "test2:type");
        expectProp("/test/node/test2:property", PropertyType.STRING, "value");

        // step 4
        try (Log4jInterceptor interceptor = Log4jInterceptor.onDebug().trap(ConfigurationConfigService.class).build()) {
            applyDefinitions(reregisterConfiguration, baseline);
            assertTrue(interceptor.messages().anyMatch(m -> m.equals("skipping CND already loaded in baseline: 'test2b.cnd' defined in test-group/test-project/test-module-0 [string].")));
        }
    }

    @Test
    public void expect_debug_message_to_be_logged_upon_loading_of_cnd() throws Exception {
        final String source
                = "definitions:\n"
                + "  namespace:\n"
                + "    test3:\n"
                + "      uri: http://www.onehippo.org/test/nt/3.0\n"
                + "      cnd: test3.cnd";

        try (Log4jInterceptor interceptor = Log4jInterceptor.onDebug().trap(ConfigurationConfigService.class).build()) {
            applyDefinitions(source);
            assertTrue(interceptor.messages().anyMatch(m -> m.equals("processing CND 'test3.cnd' defined in test-group/test-project/test-module-0 [string].")));
        }

        // as well as on reloading...
        try (Log4jInterceptor interceptor = Log4jInterceptor.onDebug().trap(ConfigurationConfigService.class).build()) {
            applyDefinitions(source);
            assertTrue(interceptor.messages().anyMatch(m -> m.equals("processing CND 'test3.cnd' defined in test-group/test-project/test-module-0 [string].")));
        }
    }

    @Test
    public void expect_parse_error_in_cnd() throws Exception {
        final String source
                = "definitions:\n"
                + "  namespace:\n"
                + "    test1:\n"
                + "      uri: http://www.onehippo.org/test/nt/1.0\n"
                + "      cnd: unknown.cnd\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /node:\n"
                + "        jcr:primaryType: test1:type\n"
                + "";

        try {
            applyDefinitions(source);
            fail("an exception should have occurred");
        } catch (RepositoryException e) {
            assertTrue(e.getMessage().contains("Failed to parse cnd 'unknown.cnd' (test-group/test-project/test-module-0 [string])"));
        }
    }

    private List<String> getNamespacePrefixes() throws Exception {
        return Arrays.asList(session.getNamespacePrefixes());
    }

    private String getNamespaceURIForPrefix(final String prefix) throws Exception {
        return session.getNamespaceURI(prefix);
    }
}
