/*
 *  Copyright 2015-2023 Bloomreach
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
import java.lang.reflect.InvocationTargetException;
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

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.servletRequest = mockRequest();
        this.servletResponse = mockResponse();

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
                    } catch (IllegalStateException e) {
                        fail(String.format("Getters on request context should not throw IllegalStateException for '%s' during default pipeline invocation",
                                method.getName()));

                    } catch (Exception e) {
                        if (e.getCause() instanceof IllegalStateException) {
                            fail(String.format("Getters on request context should throw exception with cause 'IllegalStateException' for '%s' direct after pipeline cleanup invocation",
                                    method.getName()));
                        }
                        // safely ignore other errors like a session not being live any more, not what this test is about
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
                } catch (IllegalStateException e) {
                    fail(String.format("Getters on request context should not throw IllegalStateException for '%s' direct after pipeline cleanup invocation",
                            method.getName()));

                } catch (Exception e) {
                    if (e.getCause() instanceof IllegalStateException) {
                        if ("Invalid session which is already returned to the pool!".equals(e.getCause().getMessage())) {
                            // after cleanup valve for this test the resource lifecycle management returns the session to the ppool,
                            // see ResourceLifeCycleManagementValve.xml hence will throw IllegalStateException
                            // safely ignore, expected
                        } else {
                            fail(String.format("Getters on request context should throw exception with cause 'IllegalStateException' for '%s' direct after pipeline cleanup invocation",
                                    method.getName()));
                        }
                    }
                    // safely ignore other errors like a session not being live any more, not what this test is about
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
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof IllegalStateException) {
                        assertTrue(e.getCause() instanceof IllegalStateException);
                        assertTrue(e.getCause().getMessage().contains("Invocation on an invalid HstRequestContext instance"));
                    } else {
                        fail(String.format("Getters on request context should throw exception with cause 'IllegalStateException' for '%s' after disposal",
                                method.getName()));
                    }
                } catch (Exception e) {
                    fail(String.format("Getters on request context should throw exception with cause 'IllegalStateException' for '%s' after disposal",
                            method.getName()));
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
