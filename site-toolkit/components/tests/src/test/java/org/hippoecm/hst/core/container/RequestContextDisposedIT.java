/*
 *  Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.tool.DefaultContentBeansTool;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RequestContextDisposedIT extends AbstractPipelineTestCase {

    protected HttpServletRequest servletRequest;
    protected HttpServletResponse servletResponse;

    /**
     * addAnnotatedClassesConfigurationParam must be added before super setUpClass, hence redefine same setUpClass method
     * to hide the super.setUpClass and invoke that explicitly
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        String classXmlFileName = RequestContextDisposedIT.class.getName().replace(".", "/") + ".xml";
        AbstractSpringTestCase.addAnnotatedClassesConfigurationParam(classXmlFileName);
        AbstractSpringTestCase.setUpClass();
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.servletRequest = getComponent(HttpServletRequest.class.getName());
        this.servletResponse = getComponent(HttpServletResponse.class.getName());

    }

    @Test
    public void disposed_requestContext_throws_IllegalStateException_after_disposal() throws ContainerException, UnsupportedEncodingException {

        ((MockHttpServletRequest) servletRequest).setPathInfo("/news");
        ((MockHttpServletRequest) servletRequest).addHeader("Host", servletRequest.getServerName());

        ((MockHttpServletRequest) servletRequest).setRequestURI(servletRequest.getContextPath() + servletRequest.getServletPath() + servletRequest.getPathInfo());


        HstRequestContext requestContext = resolveRequest(servletRequest, servletResponse);

        HstQueryManagerFactory qmf = getComponent(HstQueryManagerFactory.class.getName());
        DefaultContentBeansTool contentBeansTool = new DefaultContentBeansTool(qmf);
        contentBeansTool.setAnnotatedClassesResourcePath("classpath*:org/hippoecm/hst/core/beans/**.class");
        ((HstMutableRequestContext) requestContext).setContentBeansTool(contentBeansTool);

        ModifiableRequestContextProvider.set(requestContext);

        final Method[] methods = HstRequestContext.class.getMethods();
        try {
            this.defaultPipeline.invoke(this.requestContainerConfig, requestContext, this.servletRequest, this.servletResponse);
            // requestContext.getSession() returns live user which cannot read /hst:hst hence /content
            assertTrue("Request context getSession should not throw exception during default pipeline invocation", requestContext.getSession().nodeExists("/unittestcontent"));

            for (Method method : methods) {
                if (isGetter(method)) {
                    try {
                        method.invoke(requestContext);
                    } catch (Exception e) {
                        fail(String.format("Getters on request context should not throw exception for '%s' during default pipeline invocation",
                                method.getName()));

                    }
                }
            }

        } catch (Exception e) {
            throw new ContainerException(e);
        } finally {
            this.defaultPipeline.cleanup(this.requestContainerConfig, requestContext, this.servletRequest, this.servletResponse);
        }

        for (Method method : methods) {
            if (isGetter(method)) {
                try {
                    method.invoke(requestContext);
                } catch (Exception e) {
                    fail(String.format("Getters on request context should not throw exception for '%s' direct after pipeline cleanup invocation",
                            method.getName()));

                }
            }
        }

        final HstRequestContextComponent rcc = HstServices.getComponentManager().getComponent(HstRequestContextComponent.class.getName());
        rcc.release(requestContext);
        for (Method method : methods) {
            if (isGetter(method)) {
                try {
                    method.invoke(requestContext);
                    fail(String.format("Getters on request context should throw exception 'IllegalStateException' for '%s' after disposal",
                            method.getName()));
                } catch (Exception e) {
                    assertTrue(e.getCause() instanceof IllegalStateException);
                    assertTrue(e.getCause().getMessage().contains("Invocation on an invalid HstRequestContext instance"));
                }
            }
        }

    }

    private static final boolean isGetter(final Method method) {
        if (method.getParameterCount() > 0) {
            return false;
        }
        final String methodName = method.getName();
        return methodName.startsWith("get") || methodName.startsWith("is");

    }
}
