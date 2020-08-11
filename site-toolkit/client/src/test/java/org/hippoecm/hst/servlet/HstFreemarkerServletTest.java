/**
 * Copyright 2014-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashSet;
import java.util.Set;

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
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateScalarModel;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * HstFreemarkerServletTest
 */
public class HstFreemarkerServletTest {

    private static Logger log = LoggerFactory.getLogger(HstFreemarkerServletTest.class);

    final String basePath = "/";
    final String normalWebAppResourcePath = "a/b/c/vbox.ftl";
    final String normalWebResourceRealPath = "/home/dev/tomcat/webapps/site/a/b/c/vbox.ftl";
    final String classpathResourcePath = "classpath:/org/hippoecm/hst/pagecomposer/builtin/components/vbox.ftl";
    final String exceptionMessageOnWindows = "The filename, directory name, or volume label syntax is incorrect";

    @SuppressWarnings("serial")
    private static class TestHstFreemarkerServlet extends HstFreemarkerServlet {
        // expose Configurtion for testing below.
        public Configuration getConfiguration() {
            return super.getConfiguration();
        }
    };

    private TestHstFreemarkerServlet servlet;

    @Before
    public void before() throws Exception {
        ServletContext servletContext = EasyMock.createNiceMock(ServletContext.class);
        EasyMock.expect(servletContext.getRealPath(basePath + normalWebAppResourcePath)).andReturn(normalWebResourceRealPath).anyTimes();
        EasyMock.expect(servletContext.getRealPath(basePath + classpathResourcePath)).andThrow(new RuntimeException(exceptionMessageOnWindows)).anyTimes();
        EasyMock.replay(servletContext);

        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("TemplatePath", basePath);
        servletConfig.addInitParameter("ContentType", "text/html; charset=UTF-8");

        servlet = new TestHstFreemarkerServlet();
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
            templateLoader.findTemplateSource(normalWebAppResourcePath);
        } catch (Exception e) {
            log.warn("Unexpected Exception.", e);
            fail("Unexpected Exception.");
        }

        try {
            templateLoader.findTemplateSource(classpathResourcePath);
        } catch (Exception e) {
            log.warn("Unexpected Exception.", e);
            fail("Unexpected Exception.");
        }
    }

    @Test
    public void testTreatDefaultMethodsAsBeanMembers() throws Exception {
        final ObjectWrapper objectWrapper = servlet.getConfiguration().getObjectWrapper();
        final PurchaseOrder po = new PurchaseOrderImpl("1234567890");
        // Note: ObjectWrapper#wrap(obj) is how FreeMarkerServlet creates a TemplateModel
        //       through freemarker.ext.servlet.ServletContextHashModel.get(String)
        final TemplateHashModelEx model = (TemplateHashModelEx) objectWrapper.wrap(po);
        final TemplateCollectionModel keys = model.keys();

        // Collect all the property/method keys of the templateModel.
        final Set<String> modelPropKeys = new HashSet<>();
        for (TemplateModelIterator it = keys.iterator(); it.hasNext(); ) {
            final TemplateScalarModel keyModel = (TemplateScalarModel) it.next();
            modelPropKeys.add(keyModel.getAsString());
        }

        // Assert if both properties were recognized.
        assertTrue("id property is not recognized: " + modelPropKeys, modelPropKeys.contains("id"));
        assertTrue("billingAddress property is not recognized: " + modelPropKeys, modelPropKeys.contains("billingAddress"));
    }

    public static interface PurchaseOrder {

        public String getId();

        @SuppressWarnings("unused")
        public default String getBillingAddress() {
            return "Unknown";
        }
    }

    public static class PurchaseOrderImpl implements PurchaseOrder {

        private final String id;

        PurchaseOrderImpl(final String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }
    }
}
