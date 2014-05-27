/**
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.servlet;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.servlet.ServletContext;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockServletConfig;

import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;

/**
 * HstFreemarkerServletTest
 */
public class HstFreemarkerServletTest {

    private static Logger log = LoggerFactory.getLogger(HstFreemarkerServletTest.class);

    final String basePath = "/";
    final String normalWebResourcePath = "a/b/c/vbox.ftl";
    final String normalWebResourceRealPath = "/home/dev/tomcat/webapps/site/a/b/c/vbox.ftl";
    final String problematicResourcePath = "classpath:/org/hippoecm/hst/pagecomposer/builtin/components/vbox.ftl";
    final String exceptionMessageOnWindows = "The filename, directory name, or volume label syntax is incorrect";

    private HstFreemarkerServlet servlet;

    @Before
    public void before() throws Exception {
        ServletContext servletContext = EasyMock.createNiceMock(ServletContext.class);
        EasyMock.expect(servletContext.getRealPath(basePath + normalWebResourcePath)).andReturn(normalWebResourceRealPath).anyTimes();
        EasyMock.expect(servletContext.getRealPath(basePath + problematicResourcePath)).andThrow(new RuntimeException(exceptionMessageOnWindows)).anyTimes();
        EasyMock.replay(servletContext);

        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("TemplatePath", basePath);
        servletConfig.addInitParameter("ContentType", "text/html; charset=UTF-8");

        servlet = new HstFreemarkerServlet();
        servlet.init(servletConfig);
    }

    @After
    public void after() throws Exception{
        servlet.destroy();
    }

    @Test
    public void testTemplateLoader() throws Exception {
        TemplateLoader templateLoader = servlet.createTemplateLoader("/");
        assertTrue(templateLoader instanceof MultiTemplateLoader);

        try {
            templateLoader.findTemplateSource(normalWebResourcePath);
        } catch (Exception e) {
            log.warn("Unexpected Exception.", e);
            fail("Unexpected Exception.");
        }

        try {
            templateLoader.findTemplateSource(problematicResourcePath);
        } catch (Exception e) {
            log.warn("Unexpected Exception.", e);
            fail("Unexpected Exception.");
        }
    }

}
