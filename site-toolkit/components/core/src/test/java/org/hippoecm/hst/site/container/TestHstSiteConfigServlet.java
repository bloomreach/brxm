/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.File;

import javax.servlet.ServletConfig;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;


public class TestHstSiteConfigServlet {
    
    private static final String SIMPLE_PROPS = "default.sites.name = test-hst-config-project";
    
    private static final String SIMPLE_XMLCONF =
        "<?xml version='1.0'?>\n" +
        "<configuration>\n" +
        "  <system/>\n" +
        "  <properties fileName='${currentWorkingDirectory}/target/test-hst-config.properties'/>" +
        "</configuration>";
    
    private MockServletContext servletContext = null;
    private MockServletConfig servletConfig = null;
    
    @Before
    public void setUp() throws Exception {
        FileUtils.writeStringToFile(new File("target/test-hst-config.properties"), SIMPLE_PROPS);
        FileUtils.writeStringToFile(new File("target/test-hst-config.xml"), SIMPLE_XMLCONF);
        System.setProperty("currentWorkingDirectory", new File(".").getCanonicalPath());
        System.setProperty("test.xml.config.foo", "bar");
        servletContext = new MockServletContext("target", new FileSystemResourceLoader());
        servletConfig = new MockServletConfig(servletContext);
    }
    
    @After
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(new File("target/test-hst-config.properties"));
        FileUtils.deleteQuietly(new File("target/test-hst-config.xml"));
        System.setProperty("currentWorkingDirectory", "");
        System.setProperty("test.xml.config.foo", "");
    }
    
    @Test
    public void testGetConfigurationWithFileURI() throws Exception {
        File confFile = new File("target/test-hst-config.properties");
        servletConfig.addInitParameter(HstSiteConfigServlet.HST_CONFIG_PROPERTIES_PARAM, confFile.toURI().toString());
        HstSiteConfigServlet siteConfigServlet = new HstSiteConfigServletForTest(servletConfig);
        Configuration configuration = siteConfigServlet.getConfiguration(servletConfig);
        assertEquals("test-hst-config-project", configuration.getString("default.sites.name"));
    }
    
    @Test
    public void testGetConfigurationWithRelativePath1() throws Exception {
        servletConfig.addInitParameter(HstSiteConfigServlet.HST_CONFIG_PROPERTIES_PARAM, "target/test-hst-config.properties");
        HstSiteConfigServlet siteConfigServlet = new HstSiteConfigServletForTest(servletConfig);
        Configuration configuration = siteConfigServlet.getConfiguration(servletConfig);
        assertEquals("test-hst-config-project", configuration.getString("default.sites.name"));
    }
    
   // @Test
    public void testGetConfigurationWithRelativePath2() throws Exception {
        servletConfig.addInitParameter(HstSiteConfigServlet.HST_CONFIG_PROPERTIES_PARAM, "./target/test-hst-config.properties");
        HstSiteConfigServlet siteConfigServlet = new HstSiteConfigServletForTest(servletConfig);
        Configuration configuration = siteConfigServlet.getConfiguration(servletConfig);
        assertEquals("test-hst-config-project", configuration.getString("default.sites.name"));
    }
    
    @Test
    public void testGetConfigurationWithFileURIAndContextParam() throws Exception {
        File confFile = new File("target/test-hst-config.properties");
        servletContext.addInitParameter(HstSiteConfigServlet.HST_CONFIG_PROPERTIES_PARAM, confFile.toURI().toString());
        HstSiteConfigServlet siteConfigServlet = new HstSiteConfigServletForTest(servletConfig);
        Configuration configuration = siteConfigServlet.getConfiguration(servletConfig);
        assertEquals("test-hst-config-project", configuration.getString("default.sites.name"));
    }
    
    @Test
    public void testGetConfigurationWithContextRelativePath() throws Exception {
        servletConfig.addInitParameter(HstSiteConfigServlet.HST_CONFIG_PROPERTIES_PARAM, "/test-hst-config.properties");
        HstSiteConfigServlet siteConfigServlet = new HstSiteConfigServletForTest(servletConfig);
        Configuration configuration = siteConfigServlet.getConfiguration(servletConfig);
        assertEquals("test-hst-config-project", configuration.getString("default.sites.name"));
    }
    
    @Test
    public void testGetConfigurationWithContextRelativePathAndContextParam() throws Exception {
        servletContext.addInitParameter(HstSiteConfigServlet.HST_CONFIG_PROPERTIES_PARAM, "/test-hst-config.properties");
        HstSiteConfigServlet siteConfigServlet = new HstSiteConfigServletForTest(servletConfig);
        Configuration configuration = siteConfigServlet.getConfiguration(servletConfig);
        assertEquals("test-hst-config-project", configuration.getString("default.sites.name"));
    }
    
    @Test
    public void testGetConfigurationWithXmlFileURI() throws Exception {
        File confFile = new File("target/test-hst-config.xml");
        servletConfig.addInitParameter(HstSiteConfigServlet.HST_CONFIGURATION_PARAM, confFile.toURI().toString());
        HstSiteConfigServlet siteConfigServlet = new HstSiteConfigServletForTest(servletConfig);
        Configuration configuration = siteConfigServlet.getConfiguration(servletConfig);
        assertEquals("test-hst-config-project", configuration.getString("default.sites.name"));
        assertEquals("bar", configuration.getString("test.xml.config.foo"));
    }
    
    @Test
    public void testGetConfigurationWithXmlRelativePath1() throws Exception {
        servletConfig.addInitParameter(HstSiteConfigServlet.HST_CONFIGURATION_PARAM, "target/test-hst-config.xml");
        HstSiteConfigServlet siteConfigServlet = new HstSiteConfigServletForTest(servletConfig);
        Configuration configuration = siteConfigServlet.getConfiguration(servletConfig);
        assertEquals("test-hst-config-project", configuration.getString("default.sites.name"));
        assertEquals("bar", configuration.getString("test.xml.config.foo"));
    }
    
    @Test
    public void testGetConfigurationWithXmlRelativePath2() throws Exception {
        servletConfig.addInitParameter(HstSiteConfigServlet.HST_CONFIGURATION_PARAM, "./target/test-hst-config.xml");
        HstSiteConfigServlet siteConfigServlet = new HstSiteConfigServletForTest(servletConfig);
        Configuration configuration = siteConfigServlet.getConfiguration(servletConfig);
        assertEquals("test-hst-config-project", configuration.getString("default.sites.name"));
        assertEquals("bar", configuration.getString("test.xml.config.foo"));
    }
    
    @Test
    public void testGetConfigurationWithXmlFileURIAndContextParam() throws Exception {
        File confFile = new File("target/test-hst-config.xml");
        servletContext.addInitParameter(HstSiteConfigServlet.HST_CONFIGURATION_PARAM, confFile.toURI().toString());
        HstSiteConfigServlet siteConfigServlet = new HstSiteConfigServletForTest(servletConfig);
        Configuration configuration = siteConfigServlet.getConfiguration(servletConfig);
        assertEquals("test-hst-config-project", configuration.getString("default.sites.name"));
        assertEquals("bar", configuration.getString("test.xml.config.foo"));
    }
    
    @Test
    public void testGetConfigurationWithXmlContextRelativePath() throws Exception {
        servletConfig.addInitParameter(HstSiteConfigServlet.HST_CONFIGURATION_PARAM, "/test-hst-config.xml");
        HstSiteConfigServlet siteConfigServlet = new HstSiteConfigServletForTest(servletConfig);
        Configuration configuration = siteConfigServlet.getConfiguration(servletConfig);
        assertEquals("test-hst-config-project", configuration.getString("default.sites.name"));
        assertEquals("bar", configuration.getString("test.xml.config.foo"));
    }
    
   // @Test
    public void testGetConfigurationWithXmlContextRelativePathAndContextParam() throws Exception {
        servletContext.addInitParameter(HstSiteConfigServlet.HST_CONFIGURATION_PARAM, "/test-hst-config.xml");
        HstSiteConfigServlet siteConfigServlet = new HstSiteConfigServletForTest(servletConfig);
        Configuration configuration = siteConfigServlet.getConfiguration(servletConfig);
        assertEquals("test-hst-config-project", configuration.getString("default.sites.name"));
        assertEquals("bar", configuration.getString("test.xml.config.foo"));
    }
    
    @Ignore
    private class HstSiteConfigServletForTest extends HstSiteConfigServlet {
        
        private static final long serialVersionUID = 1L;
        
        private ServletConfig servletConfig;
        
        public HstSiteConfigServletForTest(ServletConfig servletConfig) {
            this.servletConfig = servletConfig;
        }
        
        @Override
        public ServletConfig getServletConfig() {
            return servletConfig;
        }
        
    }
    
}
