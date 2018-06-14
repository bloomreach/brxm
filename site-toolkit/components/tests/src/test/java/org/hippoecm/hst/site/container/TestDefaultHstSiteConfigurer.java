/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.site.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;


public class TestDefaultHstSiteConfigurer {

    private static final String SIMPLE_PROPS_1 = 
            "default.site.name = test-hst-config-project-1\n"
            + "default.site1.alias = test-hst-1";

    private static final String SIMPLE_PROPS_2 = 
            "default.site.name = test-hst-config-project-2\n"
            + "default.site2.alias = test-hst-2";

    private static final String SIMPLE_PROPS_ENV_CONF = 
            "default.site.name = test-hst-config-project-env\n"
            + "default.site.env.alias = test-hst-env";

    private static final String SIMPLE_XMLCONF_1 =
        "<?xml version='1.0'?>\n" +
        "<configuration>\n" +
        "  <system/>\n" +
        "  <properties fileName='${currentWorkingDirectory}/target/test-hst-config-1.properties'/>" +
        "</configuration>";

    private MockServletContext servletContext = null;

    @Before
    public void setUp() throws Exception {
        FileUtils.writeStringToFile(new File("target/test-hst-config-1.properties"), SIMPLE_PROPS_1, "utf-8");
        FileUtils.writeStringToFile(new File("target/test-hst-config-2.properties"), SIMPLE_PROPS_2, "utf-8");
        FileUtils.writeStringToFile(new File("target/conf/hst.properties"), SIMPLE_PROPS_ENV_CONF, "utf-8");
        FileUtils.writeStringToFile(new File("target/test-hst-config-1.xml"), SIMPLE_XMLCONF_1, "utf-8");
        System.setProperty("currentWorkingDirectory", new File("").getCanonicalPath());
        System.setProperty("test.xml.config.foo", "bar");
        servletContext = new MockServletContext("target", new FileSystemResourceLoader());
    }

    @After
    public void tearDown() throws Exception {
        try {
            FileUtils.deleteQuietly(new File("target/test-hst-config-1.properties"));
            FileUtils.deleteQuietly(new File("target/test-hst-config-2.properties"));
            FileUtils.deleteQuietly(new File("target/test-hst-config-3.properties"));
            FileUtils.deleteQuietly(new File("target/conf/hst.properties"));
            FileUtils.deleteQuietly(new File("target/test-hst-config-1.xml"));
        } finally {
            System.clearProperty("currentWorkingDirectory");
            System.clearProperty("test.xml.config.foo");
        }
    }

    @Test
    public void testGetConfigurationWithFileURIAndContextParam() throws Exception {
        File confFile = new File("target/test-hst-config-1.properties");
        servletContext.addInitParameter(HstSiteConfigurer.HST_CONFIG_PROPERTIES_PARAM, confFile.toURI().toString());
        DefaultHstSiteConfigurer hstSiteConfigurer = createHstSiteConfigurer();
        Configuration configuration = hstSiteConfigurer.getConfiguration();
        assertEquals("test-hst-config-project-1", configuration.getString("default.site.name"));
    }

    @Test
    public void testGetConfigurationWithContextRelativePathAndContextParam() throws Exception {
        servletContext.addInitParameter(HstSiteConfigurer.HST_CONFIG_PROPERTIES_PARAM, "/test-hst-config-1.properties");
        DefaultHstSiteConfigurer hstSiteConfigurer = createHstSiteConfigurer();
        Configuration configuration = hstSiteConfigurer.getConfiguration();
        assertEquals("test-hst-config-project-1", configuration.getString("default.site.name"));
    }

    @Test
    public void testGetConfigurationWithXmlFileURI() throws Exception {
        File confFile = new File("target/test-hst-config-1.xml");
        servletContext.addInitParameter(HstSiteConfigurer.HST_CONFIGURATION_PARAM, confFile.toURI().toString());
        DefaultHstSiteConfigurer hstSiteConfigurer = createHstSiteConfigurer();
        Configuration configuration = hstSiteConfigurer.getConfiguration();
        assertEquals("test-hst-config-project-1", configuration.getString("default.site.name"));
        assertEquals("bar", configuration.getString("test.xml.config.foo"));
    }

    @Test
    public void testGetConfigurationWithXmlRelativePath1() throws Exception {
        servletContext.addInitParameter(HstSiteConfigurer.HST_CONFIGURATION_PARAM, "target/test-hst-config-1.xml");
        DefaultHstSiteConfigurer hstSiteConfigurer = createHstSiteConfigurer();
        Configuration configuration = hstSiteConfigurer.getConfiguration();
        assertEquals("test-hst-config-project-1", configuration.getString("default.site.name"));
        assertEquals("bar", configuration.getString("test.xml.config.foo"));
    }

    @Test
    public void testGetConfigurationWithXmlRelativePath2() throws Exception {
        servletContext.addInitParameter(HstSiteConfigurer.HST_CONFIGURATION_PARAM, "./target/test-hst-config-1.xml");
        DefaultHstSiteConfigurer hstSiteConfigurer = createHstSiteConfigurer();
        Configuration configuration = hstSiteConfigurer.getConfiguration();
        assertEquals("test-hst-config-project-1", configuration.getString("default.site.name"));
        assertEquals("bar", configuration.getString("test.xml.config.foo"));
    }

    @Test
    public void testGetConfigurationWithXmlFileURIAndContextParam() throws Exception {
        File confFile = new File("target/test-hst-config-1.xml");
        servletContext.addInitParameter(HstSiteConfigurer.HST_CONFIGURATION_PARAM, confFile.toURI().toString());
        DefaultHstSiteConfigurer hstSiteConfigurer = createHstSiteConfigurer();
        Configuration configuration = hstSiteConfigurer.getConfiguration();
        assertEquals("test-hst-config-project-1", configuration.getString("default.site.name"));
        assertEquals("bar", configuration.getString("test.xml.config.foo"));
    }

    @Test
    public void testGetConfigurationWithXmlContextRelativePath() throws Exception {
        servletContext.addInitParameter(HstSiteConfigurer.HST_CONFIGURATION_PARAM, "/test-hst-config-1.xml");
        DefaultHstSiteConfigurer hstSiteConfigurer = createHstSiteConfigurer();
        Configuration configuration = hstSiteConfigurer.getConfiguration();
        assertEquals("test-hst-config-project-1", configuration.getString("default.site.name"));
        assertEquals("bar", configuration.getString("test.xml.config.foo"));
    }

    @Test
    public void testGetConfigurationWithXmlContextRelativePathAndContextParam() throws Exception {
        servletContext.addInitParameter(HstSiteConfigurer.HST_CONFIGURATION_PARAM, "/test-hst-config-1.xml");
        DefaultHstSiteConfigurer hstSiteConfigurer = createHstSiteConfigurer();
        Configuration configuration = hstSiteConfigurer.getConfiguration();
        assertEquals("test-hst-config-project-1", configuration.getString("default.site.name"));
        assertEquals("bar", configuration.getString("test.xml.config.foo"));
    }

    @Test
    public void testCheckCompositeConfiguration() throws Exception {
        servletContext.addInitParameter(HstSiteConfigurer.HST_CONFIG_PROPERTIES_PARAM, "/test-hst-config-1.properties");
        DefaultHstSiteConfigurer hstSiteConfigurer = createHstSiteConfigurer();
        Configuration configuration = hstSiteConfigurer.getConfiguration();
        assertTrue(configuration instanceof CompositeConfiguration);
    }

    @Test
    public void testCheckCompositeConfigurationOverridenByEnvProperties() throws Exception {
        servletContext.addInitParameter(HstSiteConfigurer.HST_CONFIG_PROPERTIES_PARAM, "/test-hst-config-1.properties");
        System.setProperty("catalina.base", new File("target").getCanonicalPath());
        DefaultHstSiteConfigurer hstSiteConfigurer = createHstSiteConfigurer();
        Configuration configuration = hstSiteConfigurer.getConfiguration();
        try {
            System.setProperty("catalina.base", "");
            assertEquals("test-hst-env", configuration.getString("default.site.env.alias"));
        } finally {
            System.clearProperty("catalina.base");
        }
    }

    @Test
    public void testCheckCompositeConfigurationOverridenByContextProperties() throws Exception {
        servletContext.addInitParameter(HstSiteConfigurer.HST_CONFIG_PROPERTIES_PARAM, "/test-hst-config-2.properties");
        DefaultHstSiteConfigurer hstSiteConfigurer = createHstSiteConfigurer();
        Configuration configuration = hstSiteConfigurer.getConfiguration();
        assertEquals("test-hst-config-project-2", configuration.getString("default.site.name"));
        assertEquals("test-hst-2", configuration.getString("default.site2.alias"));
    }

    @Test
    public void testCheckCompositeConfigurationOverridenByEnvAndContextProperties() throws Exception {
        servletContext.addInitParameter(HstSiteConfigurer.HST_CONFIG_PROPERTIES_PARAM, "/test-hst-config-2.properties");
        System.setProperty("catalina.base", new File("target").getCanonicalPath());
        DefaultHstSiteConfigurer hstSiteConfigurer = createHstSiteConfigurer();
        Configuration configuration = hstSiteConfigurer.getConfiguration();
        try {
            System.setProperty("catalina.base", "");
            assertEquals("test-hst-config-project-2", configuration.getString("default.site.name"));
            assertEquals("test-hst-2", configuration.getString("default.site2.alias"));
            assertEquals("test-hst-env", configuration.getString("default.site.env.alias"));
        } finally {
            System.clearProperty("catalina.base");
        }
    }

    @Test
    public void testCheckCompositeConfigurationOverridenBySystemAndEnvAndContextProperties() throws Exception {
        servletContext.addInitParameter(HstSiteConfigurer.HST_CONFIG_PROPERTIES_PARAM, "/test-hst-config-2.properties");
        System.setProperty("catalina.base", new File("target").getCanonicalPath());
        System.setProperty("default.site.name", "test-hst-system");
        DefaultHstSiteConfigurer hstSiteConfigurer = createHstSiteConfigurer();
        Configuration configuration = hstSiteConfigurer.getConfiguration();
        try {
            System.setProperty("catalina.base", "");
            assertEquals("test-hst-system", configuration.getString("default.site.name"));
            assertEquals("test-hst-2", configuration.getString("default.site2.alias"));
            assertEquals("test-hst-env", configuration.getString("default.site.env.alias"));
        }finally {
            System.clearProperty("catalina.base");
            System.clearProperty("default.site.name");
        }
    }

    @Test
    public void testDefaultHstConfForMissingProperties() throws Exception {
        File confFile = new File("target/test-hst-config-1.properties");
        servletContext.addInitParameter(HstSiteConfigurer.HST_CONFIG_PROPERTIES_PARAM, confFile.toURI().toString());
        DefaultHstSiteConfigurer hstSiteConfigurer = createHstSiteConfigurer();
        Configuration configuration = hstSiteConfigurer.getConfiguration();
        assertNotNull(configuration.getString("default.site.name"));
        assertEquals("\uFFFF", configuration.getString("repository.pool.user.name.separator"));
    }

    private DefaultHstSiteConfigurer createHstSiteConfigurer() {
        DefaultHstSiteConfigurer hstSiteConfigurer = new DefaultHstSiteConfigurer();
        hstSiteConfigurer.setServletContext(servletContext);
        return hstSiteConfigurer;
    }
}
