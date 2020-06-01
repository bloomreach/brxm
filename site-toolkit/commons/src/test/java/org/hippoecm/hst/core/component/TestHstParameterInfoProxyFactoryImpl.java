/*
 *  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.component;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.container.TestRequestContextProvider;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ParameterConfiguration;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TestHstParameterInfoProxyFactoryImpl
 * @version $Id$
 */
public class TestHstParameterInfoProxyFactoryImpl {

    private HstParameterInfoProxyFactory paramInfoProxyFactory = new HstParameterInfoProxyFactoryImpl();

    private HstComponent component;
    private HstRequestContext requestContext;
    private HstRequest request;
    private ResolvedSiteMapItem resolvedSiteMapItem;
    private ParameterConfiguration parameterConfig;
    private HstParameterValueConverter converter;
    private Object[] mocks;

    private Map<String, Object> params = new HashMap<String, Object>();
    {
        params.put("queryOption", "queryOptionValue");
        params.put("cellWidth", new Integer(400));
        params.put("cellHeight", new Integer(300));
        params.put("name", "Combined");
    }

    @Before
    public void setUp() throws Exception {
        component = new TestComponent();
        converter = new HstParameterValueConverter() {
            @Override
            public Object convert(String parameterValue, Class<?> returnType)
                    throws HstParameterValueConversionException {
                if (returnType == int.class || returnType == Integer.class) {
                    return Integer.parseInt(parameterValue);
                }
                return parameterValue;
            }
        };

        request = createNiceMock(HstRequest.class);
        requestContext = createNiceMock(HstRequestContext.class);
        expect(request.getRequestContext()).andReturn(requestContext).anyTimes();
        resolvedSiteMapItem = createNiceMock(ResolvedSiteMapItem.class);
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();
        parameterConfig = createNiceMock(ParameterConfiguration.class);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            expect(parameterConfig.getParameter(name, resolvedSiteMapItem)).andReturn(value.toString()).anyTimes();
        }

        mocks = new Object[]{parameterConfig, request, requestContext, resolvedSiteMapItem};

        TestRequestContextProvider.setCurrentRequestContext(requestContext);
    }

    @After
    public void tearDown() {
        TestRequestContextProvider.setCurrentRequestContext(null);
    }

    @Test
    public void testToString() throws Exception {
        replay(mocks);

        ParametersInfo parametersInfo = component.getClass().getAnnotation(ParametersInfo.class);

        CombinedInfo combinedInfo1 = paramInfoProxyFactory.createParameterInfoProxy(parametersInfo, parameterConfig, request, converter);
        int combinedInfo1HashCode = combinedInfo1.hashCode();
        String combinedInfo1String = combinedInfo1.toString();

        CombinedInfo combinedInfo2 = paramInfoProxyFactory.createParameterInfoProxy(parametersInfo, parameterConfig, request, converter);
        int combinedInfo2HashCode = combinedInfo2.hashCode();
        String combinedInfo2String = combinedInfo2.toString();

        // We don't need to support #equals(o) on each getter properties.
        // So, parameters info proxy should always return false on different instance.
        assertFalse(combinedInfo1.equals(combinedInfo2));

        assertFalse(combinedInfo1HashCode == 0);
        assertFalse(combinedInfo2HashCode == 0);
        assertFalse(combinedInfo1HashCode == combinedInfo2HashCode);

        assertNotNull(combinedInfo1String);
        assertFalse("".equals(combinedInfo1String.trim()));
        assertTrue("ParametersInfo 'CombinedInfo' class name expected in String ",
                combinedInfo1String.contains(CombinedInfo.class.getName()));

        assertNotNull(combinedInfo2String);
        assertFalse("".equals(combinedInfo1String.trim()));

        assertEquals(combinedInfo1String, combinedInfo2String);
    }

    @Test
    public void testMultiInheritedParametersInfoType() throws Exception {
        replay(mocks);
        ParametersInfo parametersInfo = component.getClass().getAnnotation(ParametersInfo.class);
        CombinedInfo combinedInfo = paramInfoProxyFactory.createParameterInfoProxy(parametersInfo, parameterConfig, request, converter);

        assertEquals(params.get("queryOption"), combinedInfo.getQueryOption());
        assertEquals(params.get("cellWidth"), combinedInfo.getCellWidth());
        assertEquals(params.get("cellHeight"), combinedInfo.getCellHeight());
        assertEquals(params.get("name"), combinedInfo.getName());
    }

    @Test
    public void component_rendering_request_parameters_have_precedence() {

        HttpServletRequest httpServletRequest = createNiceMock(HttpServletRequest.class);
        expect(httpServletRequest.getMethod()).andReturn("POST").anyTimes();

        HstContainerURL containerURL = createNiceMock(HstContainerURL.class);
        expect(containerURL.getComponentRenderingWindowReferenceNamespace()).andReturn("r1_r2");

        expect(requestContext.getServletRequest()).andReturn(httpServletRequest).anyTimes();
        expect(requestContext.isCmsRequest()).andReturn(true).anyTimes();
        expect(requestContext.getBaseURL()).andReturn(containerURL).anyTimes();

        Map<String, String []> postParameters = new HashMap<>();
        postParameters.put("queryOption", new String[]{"requestParameterQueryOptionValue"});
        expect(request.getParameterMap(eq(""))).andReturn(postParameters).anyTimes();

        replay(mocks);
        replay(httpServletRequest, containerURL);
        ParametersInfo parametersInfo = component.getClass().getAnnotation(ParametersInfo.class);
        CombinedInfo combinedInfo = paramInfoProxyFactory.createParameterInfoProxy(parametersInfo, parameterConfig, request, converter);

        assertEquals("requestParameterQueryOptionValue", combinedInfo.getQueryOption());
        assertEquals(params.get("cellWidth"), combinedInfo.getCellWidth());
        assertEquals(params.get("cellHeight"), combinedInfo.getCellHeight());
        assertEquals(params.get("name"), combinedInfo.getName());
    }

    public interface SearchInfo {

        @Parameter(name = "queryOption")
        public String getQueryOption();

    }

    public interface CellInfo {

        @Parameter(name = "cellWidth")
        public int getCellWidth();

        @Parameter(name = "cellHeight")
        public int getCellHeight();

    }

    public interface CombinedInfo extends SearchInfo, CellInfo {

        @Parameter(name = "name")
        public String getName();

    }

    @ParametersInfo(type=CombinedInfo.class)
    public class TestComponent implements HstComponent {

        @Override
        public void init(ServletContext servletContext, ComponentConfiguration componentConfig)
                throws HstComponentException {
        }

        @Override
        public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        }

        @Override
        public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        }

        @Override
        public void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {
        }

        @Override
        public void destroy() throws HstComponentException {
        }

    }
}
