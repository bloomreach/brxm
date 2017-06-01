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

package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.generated.jaxb.WebXml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WebXmlUtilsTest {
    private static final String EXISTING_VALUE = "classpath*:org/example/beans/**/*.class\n" +
            "      ,classpath*:org/onehippo/forge/**/*.class";
    private static final String ADDED_VALUE = "classpath*:org/onehippo/cms7/hst/beans/**/*.class," +
            "    classpath*:org/onehippo/forge/robotstxt/**/*.class";
    private static final String EXPECTED_RESULT = EXISTING_VALUE + ',' + ADDED_VALUE;

    @Test
    public void testParsing() throws Exception {
        final String webXmlContent = readResource("test_web.xml");
        final WebXml webXml = parseWebXml(webXmlContent);
        assertEquals(2, webXml.getParameters().size());
        final String hstBeanContextValue = WebXmlUtils.getHstBeansAnnotatedClassesValue(webXml);
        assertNotNull(hstBeanContextValue);

        final String newWebXmlContent = WebXmlUtils.addToHstBeansAnnotatedClassesValue(webXmlContent, hstBeanContextValue, ADDED_VALUE);
        final WebXml newWebXml = parseWebXml(newWebXmlContent);
        assertEquals(EXPECTED_RESULT, WebXmlUtils.getHstBeansAnnotatedClassesValue(newWebXml));
    }

    @Test
    public void add_filter() throws Exception {
        final String FILTER_CLASS = "org.onehippo.forge.rewriting.HippoRewriteFilter";

        final String webXmlContent = readResource("test_web.xml");
        final WebXml webXml = parseWebXml(webXmlContent);
        assertEquals(3, webXml.getFilters().size());
        assertFalse(WebXmlUtils.hasFilter(webXml, FILTER_CLASS));
        assertTrue(WebXmlUtils.hasFilter(webXml, "org.hippoecm.hst.container.HstFilter"));

        final String filter = readResource("filter_for_web.xml");
        final String newWebXmlContent = WebXmlUtils.addFilter(webXmlContent, filter);
        final WebXml newWebXml = parseWebXml(newWebXmlContent);
        assertTrue(newWebXmlContent.contains(filter));
        assertEquals(4, newWebXml.getFilters().size());
        assertEquals(newWebXml.getFilters().get(3).getFilterClass(), FILTER_CLASS);
    }

    @Test
    public void add_filter_mapping() throws Exception {
        final String FILTER_NAME = "RewriteFilter";

        final String webXmlContent = readResource("test_web.xml");
        final WebXml webXml = parseWebXml(webXmlContent);
        assertEquals(3, webXml.getFilterMappings().size());
        assertFalse(WebXmlUtils.hasFilterMapping(webXml, FILTER_NAME));
        assertTrue(WebXmlUtils.hasFilterMapping(webXml, "HstFilter"));

        assertEquals(1, WebXmlUtils.findFilterMapping(webXml, "XSSUrlFilter"));

        final String filterMapping = readResource("filter_mapping_for_web.xml");
        final String newWebXmlContent = WebXmlUtils.addFilterMapping(webXmlContent, filterMapping, 1);
        final WebXml newWebXml = parseWebXml(newWebXmlContent);
        assertTrue(newWebXmlContent.contains(filterMapping));
        assertEquals(4, newWebXml.getFilterMappings().size());
        assertEquals(newWebXml.getFilterMappings().get(2).getFilterName(), FILTER_NAME);
    }

    @Test
    public void add_filter_mapping_dispatcher() throws Exception {
        final String webXmlContent = readResource("test_web.xml");
        final WebXml webXml = parseWebXml(webXmlContent);

        assertTrue(WebXmlUtils.hasDispatcher(webXml.getFilterMappings().get(2), WebXmlUtils.Dispatcher.REQUEST));
        assertFalse(WebXmlUtils.hasDispatcher(webXml.getFilterMappings().get(2), WebXmlUtils.Dispatcher.FORWARD));

        final String newWebXmlContent = WebXmlUtils.addDispatcher(webXmlContent, 2, WebXmlUtils.Dispatcher.FORWARD);
        final WebXml newWebXml = parseWebXml(newWebXmlContent);

        assertTrue(WebXmlUtils.hasDispatcher(newWebXml.getFilterMappings().get(2), WebXmlUtils.Dispatcher.REQUEST));
        assertTrue(WebXmlUtils.hasDispatcher(newWebXml.getFilterMappings().get(2), WebXmlUtils.Dispatcher.FORWARD));
    }

    private String readResource(final String name) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(name)) {
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
    }

    private WebXml parseWebXml(final String webXmlContent) throws JAXBException {
        final JAXBContext context = JAXBContext.newInstance(WebXml.class);
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        return (WebXml) unmarshaller.unmarshal(new StringReader(webXmlContent));
    }
}
