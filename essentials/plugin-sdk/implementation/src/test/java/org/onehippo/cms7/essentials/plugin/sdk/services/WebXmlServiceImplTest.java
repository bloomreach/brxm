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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.onehippo.cms7.essentials.ResourceModifyingTest;
import org.onehippo.cms7.essentials.plugin.sdk.service.model.TargetPom;
import org.onehippo.cms7.essentials.plugin.sdk.service.WebXmlService;
import org.onehippo.cms7.essentials.plugin.sdk.utils.Dom4JUtils;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WebXmlServiceImplTest extends ResourceModifyingTest {

    private static final String SITE_WEB_XML = "site/src/main/webapp/WEB-INF/web.xml";
    @Inject private WebXmlService service;

    @Test
    public void add_beans_mapping() throws Exception {
        final String pattern = "classpath*:org/onehippo/forge/**/*.class";
        final File webxml = createModifiableFile("/services/webxml/pom1.xml", SITE_WEB_XML);

        assertEquals(0, nrOfOccurrences(webxml, pattern));
        assertTrue(service.addHstBeanClassPattern(pattern));
        assertEquals(1, nrOfOccurrences(webxml, pattern));
        assertTrue(service.addHstBeanClassPattern(pattern));
        assertEquals(1, nrOfOccurrences(webxml, pattern));
    }

    @Test
    public void append_beans_mapping() throws Exception {
        final String pattern = "classpath*:org/onehippo/forge/**/*.class";
        final File webxml = createModifiableFile("/services/webxml/pom2.xml", SITE_WEB_XML);

        assertEquals(0, nrOfOccurrences(webxml, pattern));
        assertTrue(service.addHstBeanClassPattern(pattern));
        assertEquals(1, nrOfOccurrences(webxml, pattern));
    }

    @Test
    public void add_beans_mapping_no_webxml() throws Exception {
        createModifiableDirectory("site/src/main/webapp/WEB-INF");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(Dom4JUtils.class).build()) {
            service.addHstBeanClassPattern("foo");
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Failed to update XML file")));
        }
    }

    @Test
    public void add_beans_mapping_no_cms() throws Exception {
        createModifiableDirectory("foo"); // point test context to temp dir with dummy content

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(Dom4JUtils.class).build()) {
            service.addHstBeanClassPattern("foo");
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Failed to update XML file")));
        }
    }

    @Test
    public void add_filter_already_exists() throws Exception {
        final File webxml = createModifiableFile("/services/webxml/pom1.xml", SITE_WEB_XML);

        assertEquals(2, nrOfOccurrences(webxml, "Console"));
        assertTrue(service.addFilter(TargetPom.SITE, "Console", null, null));
        assertEquals(2, nrOfOccurrences(webxml, "Console"));
    }

    @Test
    public void add_filter_gets_added() throws Exception {
        final Map<String, String> initParams = new HashMap<>();
        initParams.put("filter-init-param-name1", "filter-init-param-value1");
        initParams.put("filter-init-param-name2", "filter-init-param-value2");
        final File webxml = createModifiableFile("/services/webxml/pom1.xml", SITE_WEB_XML);

        final String before = contentOf(webxml);
        assertFalse(before.contains("DummyFilter"));
        assertFalse(before.contains("f.q.c.n.Dummy"));
        assertFalse(before.contains("filter-init-param-name1"));
        assertFalse(before.contains("filter-init-param-value2"));

        assertTrue(service.addFilter(TargetPom.SITE, "DummyFilter", "f.q.c.n.Dummy", initParams));

        final String after = contentOf(webxml);
        assertEquals(1, StringUtils.countMatches(after, "DummyFilter"));
        assertEquals(1, StringUtils.countMatches(after, "f.q.c.n.Dummy"));
        assertEquals(1, StringUtils.countMatches(after, "filter-init-param-name1"));
        assertEquals(1, StringUtils.countMatches(after, "filter-init-param-value2"));
    }

    @Test
    public void add_filter_invalid_pom() throws Exception {
        createModifiableFile("/services/webxml/pom0.xml", SITE_WEB_XML);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(Dom4JUtils.class).build()) {
            assertFalse(service.addFilter(TargetPom.SITE, "Console", null, null));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Failed to update XML file")));
        }
    }

    @Test
    public void add_filter_mapping() throws Exception {
        final List<String> urlPatterns = Arrays.asList("/foo/*", "/bar/baz/*");
        final File webxml = createModifiableFile("/services/webxml/pom1.xml", SITE_WEB_XML);

        final String before = contentOf(webxml);
        assertFalse(before.contains("<filter-name>DummyFilter</filter-name>"));
        assertFalse(before.contains("<url-pattern>/foo/*</url-pattern>"));
        assertFalse(before.contains("<url-pattern>/bar/baz/*</url-pattern>"));

        assertTrue(service.addFilterMapping(TargetPom.SITE, "DummyFilter", urlPatterns));

        final String after = contentOf(webxml);
        assertEquals(1, StringUtils.countMatches(after, "<filter-name>DummyFilter</filter-name>"));
        assertEquals(1, StringUtils.countMatches(after, "<url-pattern>/foo/*</url-pattern>"));
        assertEquals(1, StringUtils.countMatches(after, "<url-pattern>/bar/baz/*</url-pattern>"));
    }

    @Test
    public void add_filter_mapping_invalid_pom() throws Exception {
        createModifiableFile("/services/webxml/pom0.xml", SITE_WEB_XML);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(Dom4JUtils.class).build()) {
            assertFalse(service.addFilterMapping(TargetPom.SITE, "Console", null));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Failed to update XML file")));
        }
    }

    @Test
    public void add_dispatchers_filter_mapping_absent() throws Exception {
        createModifiableFile("/services/webxml/pom1.xml", SITE_WEB_XML);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(Dom4JUtils.class).build()) {
            assertFalse(service.addDispatchersToFilterMapping(TargetPom.SITE, "non-existent-filter", null));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Failed to find filter-mapping for filter 'non-existent-filter' in web.xml file of module 'site'.")));
        }
    }

    @Test
    public void add_dispatchers_to_filter_mapping() throws Exception {
        final List<WebXmlService.Dispatcher> dispatchers = Arrays.asList(WebXmlService.Dispatcher.REQUEST, WebXmlService.Dispatcher.FORWARD);
        final File webxml = createModifiableFile("/services/webxml/pom1.xml", SITE_WEB_XML);

        final String before = contentOf(webxml);
        assertEquals(2, StringUtils.countMatches(before, "<filter-name>Console</filter-name>"));
        assertEquals(1, StringUtils.countMatches(before, "<dispatcher>REQUEST</dispatcher>"));
        assertFalse(before.contains("<dispatcher>FORWARD</dispatcher>"));

        assertTrue(service.addDispatchersToFilterMapping(TargetPom.SITE, "Console", dispatchers));

        final String after = contentOf(webxml);
        assertEquals(2, StringUtils.countMatches(after, "<filter-name>Console</filter-name>"));
        assertEquals(1, StringUtils.countMatches(after, "<dispatcher>REQUEST</dispatcher>"));
        assertEquals(1, StringUtils.countMatches(after, "<dispatcher>FORWARD</dispatcher>"));
    }

    @Test
    public void add_dispatchers_invalid_pom() throws Exception {
        createModifiableFile("/services/webxml/pom0.xml", SITE_WEB_XML);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(Dom4JUtils.class).build()) {
            assertFalse(service.addDispatchersToFilterMapping(TargetPom.SITE, "dummy", null));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Failed to update XML file")));
        }
    }

    @Test
    public void add_servlet_already_exists() throws Exception {
        final File webxml = createModifiableFile("/services/webxml/pom1.xml", SITE_WEB_XML);

        assertEquals(2, nrOfOccurrences(webxml, "AngularResourceServlet"));
        assertTrue(service.addServlet(TargetPom.SITE, "AngularResourceServlet", null, null));
        assertEquals(2, nrOfOccurrences(webxml, "AngularResourceServlet"));
    }

    @Test
    public void add_servlet_gets_added() throws Exception {
        final File webxml = createModifiableFile("/services/webxml/pom1.xml", SITE_WEB_XML);

        final String before = contentOf(webxml);
        assertFalse(before.contains("<servlet-name>DummyServlet</servlet-name>"));
        assertFalse(before.contains("<servlet-class>f.q.c.n.Dummy</servlet-class>"));
        assertFalse(before.contains("<load-on-startup>3</load-on-startup>"));

        assertTrue(service.addServlet(TargetPom.SITE, "DummyServlet", "f.q.c.n.Dummy", 3));

        final String after = contentOf(webxml);
        assertEquals(1, StringUtils.countMatches(after, "<servlet-name>DummyServlet</servlet-name>"));
        assertEquals(1, StringUtils.countMatches(after, "<servlet-class>f.q.c.n.Dummy</servlet-class>"));
        assertEquals(1, StringUtils.countMatches(after, "<load-on-startup>3</load-on-startup>"));
    }

    @Test
    public void add_servlet_invalid_pom() throws Exception {
        createModifiableFile("/services/webxml/pom0.xml", SITE_WEB_XML);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(Dom4JUtils.class).build()) {
            assertFalse(service.addServlet(TargetPom.SITE, "Console", null, null));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Failed to update XML file")));
        }
    }

    @Test
    public void add_servlet_mapping() throws Exception {
        final List<String> urlPatterns = Arrays.asList("/bla/*", "/foo/bar/*");
        final File webxml = createModifiableFile("/services/webxml/pom1.xml", SITE_WEB_XML);

        final String before = contentOf(webxml);
        assertFalse(before.contains("<servlet-name>DummyServlet</servlet-name>"));
        assertFalse(before.contains("<url-pattern>/bla/*</url-pattern>"));
        assertFalse(before.contains("<url-pattern>/foo/bar/*</url-pattern>"));

        assertTrue(service.addServletMapping(TargetPom.SITE, "DummyServlet", urlPatterns));

        final String after = contentOf(webxml);
        assertEquals(1, StringUtils.countMatches(after, "<servlet-name>DummyServlet</servlet-name>"));
        assertEquals(1, StringUtils.countMatches(after, "<url-pattern>/bla/*</url-pattern>"));
        assertEquals(1, StringUtils.countMatches(after, "<url-pattern>/foo/bar/*</url-pattern>"));
    }

    @Test
    public void update_servlet_mapping() throws Exception {
        final List<String> urlPatterns = Arrays.asList("/angular/*", "/test/*");
        final File webxml = createModifiableFile("/services/webxml/pom1.xml", SITE_WEB_XML);

        final String before = contentOf(webxml);
        assertEquals(2, StringUtils.countMatches(before, "<servlet-name>AngularResourceServlet</servlet-name>"));
        assertEquals(1, StringUtils.countMatches(before, "<url-pattern>/angular/*</url-pattern>"));
        assertFalse(before.contains("<url-pattern>/test/*</url-pattern>"));

        assertTrue(service.addServletMapping(TargetPom.SITE, "AngularResourceServlet", urlPatterns));

        final String after = contentOf(webxml);
        assertEquals(2, StringUtils.countMatches(after, "<servlet-name>AngularResourceServlet</servlet-name>"));
        assertEquals(1, StringUtils.countMatches(after, "<url-pattern>/angular/*</url-pattern>"));
        assertEquals(1, StringUtils.countMatches(after, "<url-pattern>/test/*</url-pattern>"));
    }

    @Test
    public void add_servlet_mapping_invalid_pom() throws Exception {
        createModifiableFile("/services/webxml/pom0.xml", SITE_WEB_XML);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(Dom4JUtils.class).build()) {
            assertFalse(service.addServletMapping(TargetPom.SITE, "Console", null));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Failed to update XML file")));
        }
    }
}
