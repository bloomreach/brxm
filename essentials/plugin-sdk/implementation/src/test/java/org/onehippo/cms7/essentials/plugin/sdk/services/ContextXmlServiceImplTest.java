/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.services;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.onehippo.cms7.essentials.ResourceModifyingTest;
import org.onehippo.cms7.essentials.plugin.sdk.utils.Dom4JUtils;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ContextXmlServiceImplTest extends ResourceModifyingTest {

    @Inject private ContextXmlServiceImpl contextXmlService;

    @Test
    public void add_resource_new() throws Exception {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("a1", "v1");
        attributes.put("a2", "v2");
        File contextXml = createModifiableFile("/services/contextxml/context.xml", "conf/context.xml");

        String before = contentOf(contextXml);
        assertEquals(1, StringUtils.countMatches(before, "<Resource name=\""));
        assertFalse(before.contains("<Resource name=\"test\""));

        assertTrue(contextXmlService.addResource("test", attributes));

        String after = contentOf(contextXml);
        assertEquals(2, StringUtils.countMatches(after, "<Resource name=\""));
        assertTrue(after.contains("<Resource name=\"test\" a1=\"v1\" a2=\"v2\"/>"));
    }

    @Test
    public void add_resource_existing() throws Exception {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("a1", "v1");
        attributes.put("a2", "v2");
        File contextXml = createModifiableFile("/services/contextxml/context.xml", "conf/context.xml");

        String before = contentOf(contextXml);
        assertEquals(1, StringUtils.countMatches(before, "<Resource name=\"existing\""));

        assertTrue(contextXmlService.addResource("existing", attributes));

        String after = contentOf(contextXml);
        assertEquals(1, StringUtils.countMatches(after, "<Resource name=\"existing\" a1=\"v1\" a2=\"v2\"/>"));
    }

    @Test
    public void add_environment() throws Exception {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("a1", "v1");
        attributes.put("a2", "v2");
        File contextXml = createModifiableFile("/services/contextxml/context-no-context.xml", "conf/context.xml");

        String before = contentOf(contextXml);
        assertFalse(before.contains("<Environment"));

        assertTrue(contextXmlService.addEnvironment("new", attributes));

        String after = contentOf(contextXml);
        assertEquals(1, StringUtils.countMatches(after, "<Environment name=\"new\" a1=\"v1\" a2=\"v2\"/>"));
    }

    @Test
    public void add_environment_invalid_context_xml() throws Exception {
        createModifiableFile("/services/contextxml/context-invalid.xml", "conf/context.xml");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(Dom4JUtils.class).build()) {
            assertFalse(contextXmlService.addEnvironment("new", null));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Failed to update XML file")));
        }
    }
}
