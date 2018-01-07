/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class TemplateUtilsTest extends BaseTest {

    private static final String BEAN_REF = "com.test.MyBean";
    private static Logger log = LoggerFactory.getLogger(TemplateUtilsTest.class);

    @Test
    public void testWhitespaceNewlines() throws Exception {
        String result = TemplateUtils.injectTemplate("/test_java_parsing.txt", getContext().getPlaceholderData());
        final Object beansPackage = getContext().getPlaceholderData().get("beansPackage");
        assertTrue(result.contains((CharSequence) beansPackage));
        log.info("{}", result);
    }

    @Test
    public void testJavaParsing() throws Exception {
        String result = TemplateUtils.injectTemplate("/test_java_parsing.txt", getContext().getPlaceholderData());
        final Object beansPackage = getContext().getPlaceholderData().get("beansPackage");
        assertTrue(result.contains((CharSequence) beansPackage));
        log.info("{}", result);
    }

    @Test
    public void testParsing() throws Exception {
        final String template = "{{#sortBy}}\n" +
                '\n' +
                "<sv:property sv:name=\"sortBy\" sv:type=\"String\">\n" +
                "    <sv:value>label</sv:value>\n" +
                "</sv:property>\n" +
                "{{/sortBy}}";

        final Map<String, Object> data = new HashMap<>();
        data.put("sortBy", "namespace:document");
        final String result = TemplateUtils.replaceTemplateData(template, data);
        log.info("result {}", result);
        assertTrue(result.length() > 20);
    }

    @Test
    public void testReplaceTemplateDataHttl() throws Exception {
        final Map<String, Object> data = new HashMap<>();
        data.put("beanReference", "com.foo.bar");
        final String result = TemplateUtils.replaceTemplateData("test_template_httl.ftl", data);
        log.info("result {}", result);
    }

    @Test
    public void testInjectTemplateContent() throws Exception {
        final Map<String, Object> data = new HashMap<>();
        data.put("namespace", "myNamespace");
        String result = TemplateUtils.replaceTemplateData("test {{namespace}}", data);
        log.info("result {}", result);
        assertTrue(result.contains("myNamespace"));
    }

    @Test
    public void testInjectTemplate() throws Exception {

        final Map<String, Object> data = new HashMap<>();
        data.put("beanReference", BEAN_REF);
        final Collection<TemplateObject> listObject = new ArrayList<>();
        listObject.add(new TemplateObject("repeatable item"));
        data.put("repeatable", listObject);
        String result = TemplateUtils.injectTemplate("/test_template.ftl", data);
        log.info("result {}", result);
        assertTrue("Expected " + BEAN_REF, result.contains(BEAN_REF));
        assertTrue(result.contains("repeatable item"));
       /* result = TemplateUtils.injectTemplate("test_template_freemarker.ftl", data, getClass());
        log.info("result {}", result);
        assertTrue("Expected " + BEAN_REF, result.contains(BEAN_REF));*/

    }

    public class TemplateObject {
        private String name;

        public TemplateObject(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }
}
