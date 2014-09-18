/*
 * Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.freemarker;

import java.io.IOException;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.servlet.ServletContext;

import org.easymock.EasyMock;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.TemplateLoader;
import freemarker.cache.WebappTemplateLoader;

/**
 * DelegatingTemplateLoaderTest
 */
public class DelegatingTemplateLoaderTest {

    private static Logger log = LoggerFactory.getLogger(DelegatingTemplateLoaderTest.class);

    // HSTTWO-2976: to avoid unexpected Exception on Windows environment.
    @Test
    public void testDelegatingWebappTemplateLoader() throws Exception {
        final String basePath = "/";
        final String normalWebResourcePath = "a/b/c/vbox.ftl";
        final String normalWebResourceRealPath = "/home/dev/tomcat/webapps/site/a/b/c/vbox.ftl";
        final String classpathResourcePath = "classpath:/org/hippoecm/hst/pagecomposer/builtin/components/vbox.ftl";
        final String exceptionMessageOnWindows = "The filename, directory name, or volume label syntax is incorrect";

        ServletContext servletContext = EasyMock.createNiceMock(ServletContext.class);
        expect(servletContext.getRealPath(basePath + normalWebResourcePath)).andReturn(normalWebResourceRealPath).anyTimes();
        expect(servletContext.getRealPath(basePath + classpathResourcePath)).andThrow(new RuntimeException(exceptionMessageOnWindows)).anyTimes();
        replay(servletContext);

        TemplateLoader templateLoader = 
                new DelegatingTemplateLoader(new WebappTemplateLoader(servletContext, basePath), null,
                        new String [] {ContainerConstants.FREEMARKER_CLASSPATH_TEMPLATE_PROTOCOL,
                                ContainerConstants.FREEMARKER_JCR_TEMPLATE_PROTOCOL,
                                ContainerConstants.FREEMARKER_WEBRESOURCE_TEMPLATE_PROTOCOL});

        try {
            templateLoader.findTemplateSource(normalWebResourcePath);
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
    public void test_supported_protocols() throws IOException {

        final String classpathResourcePath = "classpath:/org/hippoecm/hst/pagecomposer/builtin/components/vbox.ftl";
        final String jcrResourcePath = "jcr:/org/hippoecm/hst/pagecomposer/builtin/components/vbox.ftl";
        final String webResourceResourcePath = "webresource:/components/vbox.ftl";
        final String unsupportedResourcePath = "unsupported:/components/vbox.ftl";

        final TemplateLoader delegatee = EasyMock.createNiceMock(TemplateLoader.class);

        expect(delegatee.findTemplateSource(eq(unsupportedResourcePath))).andThrow(new IOException());

        replay(delegatee);

        TemplateLoader templateLoader =
                new DelegatingTemplateLoader(delegatee, null,
                        new String [] {ContainerConstants.FREEMARKER_CLASSPATH_TEMPLATE_PROTOCOL,
                                ContainerConstants.FREEMARKER_JCR_TEMPLATE_PROTOCOL,
                                ContainerConstants.FREEMARKER_WEBRESOURCE_TEMPLATE_PROTOCOL});

        try {
            assertNull(templateLoader.findTemplateSource(classpathResourcePath));
        } catch (Exception e) {
            fail(String.format("Unexpected Exception because '%s' should be supported", classpathResourcePath));
        }
        try {
            assertNull(templateLoader.findTemplateSource(jcrResourcePath));
        } catch (Exception e) {
            fail(String.format("Unexpected Exception because '%s' should be supported", jcrResourcePath));
        }

        try {
            assertNull(templateLoader.findTemplateSource(webResourceResourcePath));
        } catch (Exception e) {
            fail(String.format("Unexpected Exception because '%s' should be supported", webResourceResourcePath));
        }

        try {
            templateLoader.findTemplateSource(unsupportedResourcePath);
            fail(String.format("Expected an exception for unsupported resource path '%s'.", unsupportedResourcePath));
        } catch (Exception e) {

        }
    }

}
