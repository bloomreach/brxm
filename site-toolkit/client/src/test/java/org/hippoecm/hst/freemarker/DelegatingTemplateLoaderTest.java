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

import static org.junit.Assert.fail;

import javax.servlet.ServletContext;

import org.easymock.EasyMock;
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
        final String problematicResourcePath = "classpath:/org/hippoecm/hst/pagecomposer/builtin/components/vbox.ftl";
        final String exceptionMessageOnWindows = "The filename, directory name, or volume label syntax is incorrect";

        ServletContext servletContext = EasyMock.createNiceMock(ServletContext.class);
        EasyMock.expect(servletContext.getRealPath(basePath + normalWebResourcePath)).andReturn(normalWebResourceRealPath).anyTimes();
        EasyMock.expect(servletContext.getRealPath(basePath + problematicResourcePath)).andThrow(new RuntimeException(exceptionMessageOnWindows)).anyTimes();
        EasyMock.replay(servletContext);

        TemplateLoader templateLoader = 
                new DelegatingTemplateLoader(new WebappTemplateLoader(servletContext, basePath), null, new String [] { "classpath:", "jcr:" });

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
