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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.google.common.base.Charsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseResourceTest;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.generated.jaxb.WebXml;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WebXmlUtilsTest extends BaseResourceTest {

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

    @Test
    public void add_servlet() throws Exception {
        final String webxml = "/utils/webxml/web-without-servlet.xml";
        final String servletName = "RepositoryJaxrsServlet";
        final PluginContext context = getContext();

        final Map<String, String> resourceToProjectLocation = new HashMap<>();
        resourceToProjectLocation.put(webxml, "cms/src/main/webapp/WEB-INF/web.xml");
        final Map<String, File> resourceToFile = createModifiableProject(resourceToProjectLocation);
        final File webXmlFile = resourceToFile.get(webxml);

        assertEquals(0, nrOfOccurrences(webXmlFile, servletName));
        assertTrue(WebXmlUtils.addServlet(context, TargetPom.CMS, "RepositoryJaxrsServlet", getClass(),
                23, new String[]{"/my/mapping/*", "/another/mapping/*"}));
        assertEquals(2, nrOfOccurrences(webXmlFile, servletName));
        assertEquals(1, nrOfOccurrences(webXmlFile, getClass().getName()));
        assertTrue(WebXmlUtils.addServlet(context, TargetPom.CMS, "RepositoryJaxrsServlet", getClass(),
                23, new String[]{"/my/mapping/*", "/another/mapping/*"}));
        assertEquals(2, nrOfOccurrences(webXmlFile, servletName));
        assertEquals(1, nrOfOccurrences(webXmlFile, getClass().getName()));
    }

    @Test
    public void add_servlet_no_webxml() throws Exception {
        final PluginContext context = getContext();

        System.setProperty("project.basedir", getClass().getResource("/project").getPath());

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(WebXmlUtils.class).build()) {
            WebXmlUtils.addServlet(context, TargetPom.CMS, "RepositoryJaxrsServlet", getClass(),
                    23, null);
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Failed adding servlet 'RepositoryJaxrsServlet' to web.xml of module 'cms'.")));
        }
    }

    @Test
    public void add_servlet_no_cms() throws Exception {
        final PluginContext context = getContext();

        System.setProperty("project.basedir", getClass().getResource("/utils/webxml").getPath());

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(WebXmlUtils.class).build()) {
            WebXmlUtils.addServlet(context, TargetPom.CMS, "RepositoryJaxrsServlet", getClass(),
                    23, null);
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Failed to add servlet, module 'cms' has no web.xml file.")));
        }
    }

    private Map<String, File> createModifiableProject(final Map<String, String> resourceToProjectLocation) throws IOException {
        final Map<String, File> resourceToFile = new HashMap<>();
        final Path projectPath = Files.createTempDirectory("test");
        System.setProperty("project.basedir", projectPath.toString());

        for (String resource : resourceToProjectLocation.keySet()) {
            final String[] projectLegs = resourceToProjectLocation.get(resource).split("/");
            Path outputPath = projectPath;
            for (String leg : projectLegs) {
                outputPath = outputPath.resolve(leg);
            }
            final File output = new File(outputPath.toUri());
            final File input = new File(getClass().getResource(resource).getPath());

            FileUtils.copyFile(input, output);
            resourceToFile.put(resource, output);
        }

        return resourceToFile;
    }

    private int nrOfOccurrences(final File file, final String value) throws IOException {
        final String fileContent = com.google.common.io.Files.asCharSource(file, Charsets.UTF_8).read();
        return StringUtils.countMatches(fileContent, value);
    }
}
