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

package org.onehippo.cms7.essentials.dashboard.services;

import java.io.File;

import org.junit.Test;
import org.onehippo.cms7.essentials.ResourceModifyingTest;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.service.WebXmlService;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WebXmlServiceImplTest extends ResourceModifyingTest {

    private static final String SITE_WEB_XML = "site/src/main/webapp/WEB-INF/web.xml";
    private final WebXmlService service = new WebXmlServiceImpl();

    @Test
    public void add_beans_mapping() throws Exception {
        final String pattern = "classpath*:org/onehippo/forge/**/*.class";
        final PluginContext context = getContext();
        final File webxml = createModifiableFile("/services/webxml/no-hst-beans-annotated-classes.xml", SITE_WEB_XML);

        assertEquals(0, nrOfOccurrences(webxml, pattern));
        assertTrue(service.addHstBeanClassPattern(context, pattern));
        assertEquals(1, nrOfOccurrences(webxml, pattern));
        assertTrue(service.addHstBeanClassPattern(context, pattern));
        assertEquals(1, nrOfOccurrences(webxml, pattern));
    }

    @Test
    public void append_beans_mapping() throws Exception {
        final String pattern = "classpath*:org/onehippo/forge/**/*.class";
        final PluginContext context = getContext();
        final File webxml = createModifiableFile("/services/webxml/with-hst-beans-annotated-classes.xml", SITE_WEB_XML);

        assertEquals(0, nrOfOccurrences(webxml, pattern));
        assertTrue(service.addHstBeanClassPattern(context, pattern));
        assertEquals(1, nrOfOccurrences(webxml, pattern));
    }

    @Test
    public void add_beans_mapping_no_webxml() throws Exception {
        final PluginContext context = getContext();
        createModifiableDirectory("site/src/main/webapp/WEB-INF");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(WebXmlServiceImpl.class).build()) {
            service.addHstBeanClassPattern(context, "foo");
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Failed to add HST bean class pattern 'foo'.")));
        }
    }

    @Test
    public void add_beans_mapping_no_cms() throws Exception {
        final PluginContext context = getContext();

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(WebXmlServiceImpl.class).build()) {
            service.addHstBeanClassPattern(context, "foo");
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Failed to add HST bean class pattern, module 'site' has no web.xml file.")));
        }
    }
}
