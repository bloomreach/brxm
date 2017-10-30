/*
 *  Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.lang.reflect.InvocationHandler;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactory;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactoryImpl;
import org.hippoecm.hst.core.component.HstParameterValueConverter;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hippoecm.hst.core.container.ContainerConstants.HST_COMPONENT_WINDOW;
import static org.hippoecm.hst.core.container.ContainerConstants.HST_REQUEST;
import static org.hippoecm.hst.core.container.ContainerConstants.HST_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestHstComponentInvokerImpl {

    private HstContainerConfig hstContainerConfig;
    private ComponentConfiguration componentConfiguration;
    private ResolvedSiteMapItem resolvedSiteMapItem;
    private HstURLFactory hstUrlFactory;
    private HstContainerURLProvider hstContainerURLProvider;
    private HstComponent hstComponent;
    private HstResponseState responseState;
    private HstComponentWindow componentWindow;
    private HstRequestContext requestContext;
    private HstParameterInfoProxyFactory hstParameterInfoProxyFactory;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private InvokeDispatchResult invokeDispatchResult;


    @Before
    public void setup() {
        hstContainerConfig = createNiceMock(HstContainerConfig.class);
        componentConfiguration = createNiceMock(ComponentConfiguration.class);
        resolvedSiteMapItem = createNiceMock(ResolvedSiteMapItem.class);
        hstUrlFactory = createNiceMock(HstURLFactory.class);
        expect(hstUrlFactory.isReferenceNamespaceIgnored()).andReturn(false).anyTimes();

        hstContainerURLProvider = createMock(HstContainerURLProvider.class);
        expect(hstContainerURLProvider.getParameterNameComponentSeparator()).andReturn(":").anyTimes();

        hstComponent = createNiceMock(HstComponent.class);
        expect(hstComponent.getComponentConfiguration()).andReturn(componentConfiguration);

        responseState = createNiceMock(HstResponseState.class);

        componentWindow = createNiceMock(HstComponentWindow.class);
        expect(componentWindow.getComponent()).andReturn(hstComponent).anyTimes();
        expect(componentWindow.getRenderPath()).andReturn("window-renderer.ftl").anyTimes();
        expect(componentWindow.getResponseState()).andReturn(responseState);

        requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getURLFactory()).andReturn(hstUrlFactory).anyTimes();
        expect(requestContext.getContainerURLProvider()).andReturn(hstContainerURLProvider);
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem);

        hstParameterInfoProxyFactory = new HstParameterInfoProxyFactoryImpl();

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    public void invoke_without_component_item_template_parameter() throws ContainerException {

        expect(requestContext.getParameterInfoProxyFactory()).andReturn(hstParameterInfoProxyFactory).anyTimes();

        Object[] mocks = {hstContainerConfig, hstUrlFactory, hstContainerURLProvider, hstComponent, componentWindow,
                responseState, componentConfiguration, resolvedSiteMapItem, requestContext};
        replay(mocks);

        invokeDispatchResult = new InvokeDispatchResult();
        invokeComponentInvoker(invokeDispatchResult);

        assertTrue(invokeDispatchResult.invoked);
        assertEquals("window-renderer.ftl", invokeDispatchResult.dispatchUrl);
        verify(mocks);
    }

    @Test
    public void invoke_with_component_item_template_parameter() throws ContainerException {

        expect(requestContext.getParameterInfoProxyFactory()).andReturn(hstParameterInfoProxyFactory).anyTimes();

        expect(componentConfiguration.getParameter(and(isA(String.class), eq(HstParameterInfoProxyFactoryImpl.TEMPLATE_PARAM_NAME)),
                anyObject())).andReturn("window-renderer-variant.ftl");

        Object[] mocks = {hstContainerConfig, hstUrlFactory, hstContainerURLProvider, hstComponent, componentWindow,
                responseState, componentConfiguration, resolvedSiteMapItem, requestContext};
        replay(mocks);

        invokeDispatchResult = new InvokeDispatchResult();
        invokeComponentInvoker(invokeDispatchResult);

        assertTrue(invokeDispatchResult.invoked);
        assertEquals("window-renderer-variant.ftl", invokeDispatchResult.dispatchUrl);
        // make sure to verify the mocks as HstParameterInfoProxyFactoryImpl.TEMPLATE_PARAM_NAME must have been used
        // as argument in componentConfiguration.getParameter
        verify(mocks);
    }

    /**
     * To guarantee that projects that inject a custom hstParameterInfoProxyFactory have their custom
     * hstParameterInfoProxyFactory used when finding out which template renderer to use. This is for
     * example done in targeting and experiments
     */
    @Test
    public void invoke_with_component_item_template_parameter_and_custom_hstParameterInfoProxyFactory() throws ContainerException {
    // set the custom HstParameterInfoProxyFactory that returns a specific prefixed (targeted or experiments) parameter
        expect(requestContext.getParameterInfoProxyFactory()).andReturn(new CustomParameterInfoProxyFactoryImpl()).anyTimes();

        expect(componentConfiguration.getParameter(and(isA(String.class), eq("professional-" + HstParameterInfoProxyFactoryImpl.TEMPLATE_PARAM_NAME)),
                anyObject())).andReturn("window-renderer-professional.ftl");

        Object[] mocks = {hstContainerConfig, hstUrlFactory, hstContainerURLProvider, hstComponent, componentWindow,
                responseState, componentConfiguration, resolvedSiteMapItem, requestContext};
        replay(mocks);

        invokeDispatchResult = new InvokeDispatchResult();
        invokeComponentInvoker(invokeDispatchResult);

        assertTrue(invokeDispatchResult.invoked);
        assertEquals("window-renderer-professional.ftl", invokeDispatchResult.dispatchUrl);
        // make sure to verify the mocks as ("professional-"  + HstParameterInfoProxyFactoryImpl.TEMPLATE_PARAM_NAME)
        // must have been used as argument in componentConfiguration.getParameter
        verify(mocks);
    }

    @Test
    public void invoke_sets_request_attributes() throws ContainerException {

        expect(requestContext.getParameterInfoProxyFactory()).andReturn(hstParameterInfoProxyFactory).anyTimes();

        Object[] mocks = {hstContainerConfig, hstUrlFactory, hstContainerURLProvider, hstComponent, componentWindow,
                responseState, componentConfiguration, resolvedSiteMapItem, requestContext};
        replay(mocks);

        invokeDispatchResult = new InvokeDispatchResult();
        invokeComponentInvoker(invokeDispatchResult);

        assertTrue(invokeDispatchResult.requestAttributeRequest instanceof HstRequest);
        assertTrue(invokeDispatchResult.requestAttributeResponse instanceof HstResponse);
        assertTrue(invokeDispatchResult.requestAttributeComponentWindow instanceof HstComponentWindow);

        // request attributes should be cleared after the invocation
        assertNull(request.getAttribute(HST_REQUEST));
        assertNull(request.getAttribute(HST_RESPONSE));
        assertNull(request.getAttribute(HST_COMPONENT_WINDOW));

        verify(mocks);
    }

    class CustomParameterInfoProxyFactoryImpl extends HstParameterInfoProxyFactoryImpl implements HstParameterInfoProxyFactory {
        @Override
        protected InvocationHandler createHstParameterInfoInvocationHandler(final ComponentConfiguration componentConfig, final HstRequest request, final HstParameterValueConverter converter, final Class<?> parametersInfoType) {
            return new ParameterInfoInvocationHandler(componentConfig, request, converter, parametersInfoType) {
                @Override
                protected String getPrefixedParameterName(final String parameterName, final ComponentConfiguration config, final HstRequest req) {
                    return "professional-" + parameterName;
                }
            };
        }
    }


    class InvokeDispatchResult {
        boolean invoked;
        String dispatchUrl;

        Object requestAttributeRequest;
        Object requestAttributeResponse;
        Object requestAttributeComponentWindow;
    }

    private void invokeComponentInvoker(final InvokeDispatchResult invokeDispatchResult) throws ContainerException {
        HstRequestImpl hstRequest = new HstRequestImpl(request, requestContext, componentWindow, HstRequest.RENDER_PHASE);
        HstResponseImpl hstResponse = new HstResponseImpl(request, response, requestContext, componentWindow, null);

        HstComponentInvoker invoker = new HstComponentInvokerImpl() {
            @Override
            protected void invokeDispatcher(final HstContainerConfig requestContainerConfig,
                                            final ServletRequest servletRequest,
                                            final ServletResponse servletResponse,
                                            final boolean namedDispatching,
                                            final String dispatchUrl,
                                            final HstComponentWindow window) throws Exception {

                invokeDispatchResult.invoked = true;
                invokeDispatchResult.dispatchUrl = dispatchUrl;
                invokeDispatchResult.requestAttributeRequest = servletRequest.getAttribute(HST_REQUEST);
                invokeDispatchResult.requestAttributeResponse = servletRequest.getAttribute(HST_RESPONSE);
                invokeDispatchResult.requestAttributeComponentWindow = servletRequest.getAttribute(HST_COMPONENT_WINDOW);
            }
        };

        invoker.invokeRender(hstContainerConfig, hstRequest, hstResponse);
    }
}
