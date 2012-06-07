/*
 *  Copyright 2012 Hippo.
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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.easymock.EasyMock;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.junit.Before;
import org.junit.Test;

/**
 * TestHstParameterInfoProxyFactoryImpl
 * @version $Id$
 */
public class TestHstParameterInfoProxyFactoryImpl {

    private HstParameterInfoProxyFactory paramInfoProxyFactory = new HstParameterInfoProxyFactoryImpl();

    private HstComponent component;
    private HstRequestContext requestContext;
    private HstRequest request;
    private ComponentConfiguration componentConfig;
    private HstParameterValueConverter converter;

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

        request = EasyMock.createNiceMock(HstRequest.class);
        requestContext = EasyMock.createNiceMock(HstRequestContext.class);
        EasyMock.expect(request.getRequestContext()).andReturn(requestContext).anyTimes();
        ResolvedSiteMapItem resolvedSiteMapItem = EasyMock.createNiceMock(ResolvedSiteMapItem.class);
        EasyMock.expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();
        componentConfig = EasyMock.createNiceMock(ComponentConfiguration.class);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            EasyMock.expect(componentConfig.getParameter(name, resolvedSiteMapItem)).andReturn(value.toString()).anyTimes();
        }

        EasyMock.replay(componentConfig);
        EasyMock.replay(request);
        EasyMock.replay(requestContext);
        EasyMock.replay(resolvedSiteMapItem);
    }

    @Test
    public void testMultiInheritedParametersInfoType() throws Exception {
        ParametersInfo parametersInfo = component.getClass().getAnnotation(ParametersInfo.class);
        CombinedInfo combinedInfo = paramInfoProxyFactory.createParameterInfoProxy(parametersInfo, componentConfig, request, converter);

        assertEquals(params.get("queryOption"), combinedInfo.getQueryOption());
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
