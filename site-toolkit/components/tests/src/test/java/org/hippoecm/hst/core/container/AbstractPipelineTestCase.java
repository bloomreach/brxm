/*
 *  Copyright 2012-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import javax.jcr.RepositoryException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.container.HstContainerConfigImpl;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class AbstractPipelineTestCase extends AbstractSpringTestCase {

    protected HstComponentFactory componentFactory;
    protected Pipelines pipelines;
    protected Pipeline defaultPipeline;
    protected ServletConfig servletConfig;
    protected HstContainerConfig requestContainerConfig;

    /**
     * addAnnotatedClassesConfigurationParam must be added before super setUpClass, hence redefine same setUpClass method
     * to hide the super.setUpClass and invoke that explicitly
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        String classXmlFileName = AbstractPipelineTestCase.class.getName().replace(".", "/") + ".xml";
        AbstractSpringTestCase.addAnnotatedClassesConfigurationParam(classXmlFileName);
        AbstractSpringTestCase.setUpClass();
    }

    @AfterClass
    public static void afterClass() throws RepositoryException {
        String classXmlFileName = AbstractPipelineTestCase.class.getName().replace(".", "/") + ".xml";
        AbstractSpringTestCase.removeAnnotatedClassesConfigurationParam(classXmlFileName);
        AbstractSpringTestCase.afterClass();
    }


    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.componentFactory = getComponent(HstComponentFactory.class.getName());
        this.pipelines = getComponent(Pipelines.class.getName());
        this.defaultPipeline = this.pipelines.getDefaultPipeline();
        this.servletConfig = getComponent(ServletConfig.class.getName());
        this.requestContainerConfig = new HstContainerConfigImpl(this.servletConfig.getServletContext(), getClass().getClassLoader());
    }

    protected HttpServletResponse mockResponse() {
        final MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCharacterEncoding("UTF-8");
        return response;
    }

    protected MockHttpServletRequest mockRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest(webappContext.getServletContext());
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("GET");
        request.setContextPath("/site");
        return request;
    }

}
