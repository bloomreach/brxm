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
package org.hippoecm.hst.core.component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.time.DateUtils;
import org.hippoecm.hst.configuration.components.DynamicComponentInfo;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.components.ParameterValueType;
import org.hippoecm.hst.container.TestRequestContextProvider;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.parameters.DefaultHstParameterValueConverter;
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
    private HstParameterValueConverter converter = new DefaultHstParameterValueConverter();
    private Object[] mocks;

    private Map<String, Object> params = new HashMap<String, Object>();
    {
        params.put("queryOption", "queryOptionValue");
        params.put("cellWidth", new Integer(400));
        params.put("cellHeight", new Integer(300));
        params.put("name", "Combined");
    }

    private HashMap<String, Object[]> dynamicComponentParameters = new HashMap<>();
    {
        dynamicComponentParameters.put("residualStringParam",
                new Object[] { ParameterValueType.STRING, "residualParameterValue" });
        dynamicComponentParameters.put("residualBoolean", new Object[] { ParameterValueType.BOOLEAN, Boolean.TRUE });
        dynamicComponentParameters.put("residualInteger", new Object[] { ParameterValueType.INTEGER, new Long(100) });
        dynamicComponentParameters.put("residualDecimal",
                new Object[] { ParameterValueType.DECIMAL, new Double(100.03) });
        try {
            dynamicComponentParameters.put("residualDate",
                    new Object[] { ParameterValueType.DATE,
                            DateUtils.parseDate("2020-03-19T11:09:27",
                                    DefaultHstParameterValueConverter.ISO_DATE_FORMAT,
                                    DefaultHstParameterValueConverter.ISO_DATETIME_FORMAT) });
            dynamicComponentParameters.put("residualDatetime",
                    new Object[] { ParameterValueType.DATE,
                            DateUtils.parseDate("2020-03-19", DefaultHstParameterValueConverter.ISO_DATE_FORMAT,
                                    DefaultHstParameterValueConverter.ISO_DATETIME_FORMAT) });
        } catch (ParseException e) {
            //nothing to do
        }
    }
    
    @Before
    public void setUp() throws Exception {
        component = new TestComponent();

        request = createNiceMock(HstRequest.class);
        requestContext = createNiceMock(HstRequestContext.class);
        expect(request.getRequestContext()).andReturn(requestContext).anyTimes();
        resolvedSiteMapItem = createNiceMock(ResolvedSiteMapItem.class);
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();
        parameterConfig = createNiceMock(ComponentConfiguration.class);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            expect(parameterConfig.getParameter(name, resolvedSiteMapItem)).andReturn(value.toString()).anyTimes();
        }

        mocks = new Object[]{parameterConfig, request, requestContext, resolvedSiteMapItem};

        TestRequestContextProvider.setCurrentRequestContext(requestContext);
        
        final ArrayList<DynamicParameter> dynamicParameters = new ArrayList<>();
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DefaultHstParameterValueConverter.ISO_DATETIME_FORMAT);
        for(Map.Entry<String, Object[]> entry : dynamicComponentParameters.entrySet()) {
            final DynamicParameter dynamicParameter = createNiceMock(DynamicParameter.class);
            expect(dynamicParameter.isResidual()).andReturn(true);
            expect(dynamicParameter.getName()).andReturn(entry.getKey()).anyTimes();
            expect(dynamicParameter.getValueType()).andReturn((ParameterValueType)entry.getValue()[0]);
            if (((ParameterValueType) entry.getValue()[0]).name().equals(ParameterValueType.DATE.name())) {
                expect(parameterConfig.getParameter(entry.getKey(), resolvedSiteMapItem))
                        .andReturn(simpleDateFormat.format((Date) entry.getValue()[1]));
            } else {
                expect(parameterConfig.getParameter(entry.getKey(), resolvedSiteMapItem))
                        .andReturn(entry.getValue()[1].toString());
            }
            dynamicParameters.add(dynamicParameter);
        }
        for (int i = 0; i < 2; i++) {
            final DynamicParameter dynamicParameter = createNiceMock(DynamicParameter.class);
            expect(dynamicParameter.isResidual()).andReturn(false);
            expect(dynamicParameter.getName()).andReturn("namedParameter" + i).anyTimes();
            expect(dynamicParameter.getDefaultValue()).andReturn("namedParameterValue" + i);
            expect(dynamicParameter.getValueType()).andReturn(ParameterValueType.STRING);
            dynamicParameters.add(dynamicParameter);
        }

        expect(((ComponentConfiguration) parameterConfig).getDynamicComponentParameters()).andReturn(dynamicParameters);

        for (final DynamicParameter dynamicParameter : dynamicParameters) {
            replay(dynamicParameter);
        }
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
    public void testResidualParameterValueTypesInDynamicComponentInfo() throws Exception {
        replay(mocks);
        final ParametersInfo parametersInfo = component.getClass().getAnnotation(ParametersInfo.class);
        final CombinedInfo combinedInfo = paramInfoProxyFactory.createParameterInfoProxy(parametersInfo,
                parameterConfig, request, converter);
        final Map<String, Object> residualParameters = combinedInfo.getResidualParameterValues();
        for (final Map.Entry<String, Object[]> entry : dynamicComponentParameters.entrySet()) {
            final Object value = residualParameters.get(entry.getKey());
            assertNotNull("The value of residual parameter with name " + entry.getKey() + " is null", value);
            assertEquals("The value type of residual parameter with name " + entry.getKey() + " is not correct",
                    ((ParameterValueType) entry.getValue()[0]).getDefaultReturnType(), value.getClass());
            assertEquals("The value of residual parameter with name " + entry.getKey() + " is not correct",
                    entry.getValue()[1].toString(), value.toString());
        }
    }
   
    @Test
    public void testResidualParameterValuesMethodInDynamicComponentInfo() throws Exception {
        replay(mocks);
        final ParametersInfo parametersInfo = component.getClass().getAnnotation(ParametersInfo.class);
        final CombinedInfo combinedInfo = paramInfoProxyFactory.createParameterInfoProxy(parametersInfo,
                parameterConfig, request, converter);

        final Map<String, Object> residualParameters = combinedInfo.getResidualParameterValues();
        assertEquals("The size of the map returned from getResidualParameterValues method is not correct",
                residualParameters.size(), dynamicComponentParameters.size());
        for (final Map.Entry<String, Object[]> entry : dynamicComponentParameters.entrySet()) {
            final Object value = residualParameters.get(entry.getKey());
            assertNotNull("The value of residual parameter with name " + entry.getKey() + " is null", value);
            assertEquals("The value of residual parameter with name " + entry.getKey() + " is not correct",
                    value.toString(), entry.getValue()[1].toString());
        }
    }

    @Test
    public void testDynamicComponentParametersMethodInDynamicComponentInfo() throws Exception {
        replay(mocks);
        final ParametersInfo parametersInfo = component.getClass().getAnnotation(ParametersInfo.class);
        final CombinedInfo combinedInfo = paramInfoProxyFactory.createParameterInfoProxy(parametersInfo,
                parameterConfig, request, converter);

        final List<DynamicParameter> dynamicParameters = combinedInfo.getDynamicComponentParameters();
        assertEquals("The size of the map returned from getResidualParameterValues method is not correct",
                dynamicParameters.size(), dynamicComponentParameters.size()+2);
        for (final DynamicParameter dynamicParameter : dynamicParameters) {
            if (dynamicParameter.getName().startsWith("residualParameter")) {
                assertTrue(
                        "The residual parameter with name " + dynamicParameter.getName() + " is not marked as residual",
                        dynamicParameter.isResidual());
            } else if (dynamicParameter.getName().startsWith("namedParameter")) {
                assertFalse("The named parameter with name " + dynamicParameter.getName() + " is marked as residual",
                        dynamicParameter.isResidual());
            }
        }
    }
    
    @Test
    public void component_rendering_request_parameters_have_precedence() {

        HttpServletRequest httpServletRequest = createNiceMock(HttpServletRequest.class);
        expect(httpServletRequest.getMethod()).andReturn("POST").anyTimes();

        HstContainerURL containerURL = createNiceMock(HstContainerURL.class);
        expect(containerURL.getComponentRenderingWindowReferenceNamespace()).andReturn("r1_r2");

        expect(requestContext.getServletRequest()).andReturn(httpServletRequest).anyTimes();
        expect(requestContext.isChannelManagerPreviewRequest()).andReturn(true).anyTimes();
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

    public interface CombinedInfo extends SearchInfo, CellInfo, DynamicComponentInfo{

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
