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

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.onehippo.cm.api.model.ConfigurationModel;

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
                + "  - prefix: test\n"
                + "    uri: http://www.onehippo.org/test/nt/1.0";

        configurationModel = applyDefinitions(source);

        assertTrue(getNamespacePrefixes().contains("test"));
        assertEquals(getNamespaceURIForPrefix("test"), "http://www.onehippo.org/test/nt/1.0");

        final String source2
                = "definitions:\n"
                + "  namespace:\n"
                + "  - prefix: test\n"
                + "    uri: http://www.onehippo.org/test/nt/1.0";

        applyDefinitions(source2, configurationModel);

        assertTrue(getNamespacePrefixes().contains("test"));
        assertEquals(getNamespaceURIForPrefix("test"), "http://www.onehippo.org/test/nt/1.0");
    }

    @Test
    public void expect_adding_already_existing_namespace_to_be_allowed() throws Exception {

        assertFalse(getNamespacePrefixes().contains("test2"));
        session.getWorkspace().getNamespaceRegistry().registerNamespace("test2", "http://www.onehippo.org/test/nt/2.0");
        assertTrue(getNamespacePrefixes().contains("test2"));

        final String source
                = "definitions:\n"
                + "  namespace:\n"
                + "  - prefix: test2\n"
                + "    uri: http://www.onehippo.org/test/nt/2.0";

        applyDefinitions(source);

        assertTrue(getNamespacePrefixes().contains("test2"));
        assertEquals(getNamespaceURIForPrefix("test2"), "http://www.onehippo.org/test/nt/2.0");
    }

    @Test
    public void expect_namespace_removal_to_have_no_effect() throws Exception {
        final String source
                = "definitions:\n"
                + "  namespace:\n"
                + "  - prefix: test3\n"
                + "    uri: http://www.onehippo.org/test/nt/3.0";
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
                + "  - prefix: test4\n"
                + "    uri: http://www.onehippo.org/test/nt/4.0";
        ConfigurationModel configurationModel = applyDefinitions(source);

        final String source2
                = "definitions:\n"
                + "  namespace:\n"
                + "  - prefix: test4\n"
                + "    uri: http://www.onehippo.org/test/nt/4.0-changed";
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


    private List<String> getNamespacePrefixes() throws Exception {
        return Arrays.asList(session.getNamespacePrefixes());
    }

    private String getNamespaceURIForPrefix(final String prefix) throws Exception {
        return session.getNamespaceURI(prefix);
    }
}
