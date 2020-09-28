/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.component.support.bean.dynamic;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.proxy.Invoker;
import org.hippoecm.hst.configuration.components.DropdownListParameterConfig;
import org.hippoecm.hst.configuration.components.DynamicComponentInfo;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.components.DynamicParameterConfig;
import org.hippoecm.hst.configuration.components.JcrPathParameterConfig;
import org.hippoecm.hst.configuration.components.ParameterValueType;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactoryImpl;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.jcr.pool.util.ProxyFactory;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.mock.core.component.MockHstRequest;
import org.hippoecm.hst.mock.core.component.MockHstResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockServletContext;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

public class TestBaseHstDynamicComponent {

    private MockServletContext servletContext;
    private HstRequestContext requestContext;

    @Before
    public void setUp() throws Exception {
        servletContext = new MockServletContext(new ClassPathXmlApplicationContext());

        requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getParameterInfoProxyFactory()).andReturn(new HstParameterInfoProxyFactoryImpl()).anyTimes();

        ModifiableRequestContextProvider.set(requestContext);
    }

    @After
    public void tearDown() {
        ModifiableRequestContextProvider.set(null);
    }

    @Test
    public void test_jcrpath_params_are_set_as_models() throws ObjectBeanManagerException {

        ProxyFactory factory = new ProxyFactory();
        ComponentConfiguration compConfig = (ComponentConfiguration) factory.createInvokerProxy(new Invoker() {
            public Object invoke(Object o, Method m, Object[] args) throws Throwable {
                if (args == null) {
                    return null;
                } else {
                    if ("document1".equals(args[0])) {
                        return "banners/banner1";
                    }
                    else if ("image1".equals(args[0])) {
                        return "/content/images/image1";
                    }
                    else if ("string1".equals(args[0])) {
                        return "string1";
                    }
                    else if ("string2".equals(args[0])) {
                        return "string2";
                    }
                    else if ("dropdown1".equals(args[0])) {
                        return "dropdown1";
                    }
                    else if ("dropdown2".equals(args[0])) {
                        return "dropdown2";
                    }
                }
                return null;
            }
        }, new Class [] { ComponentConfiguration.class });

        List<DynamicParameter> componentParameters = new ArrayList<>();
        componentParameters.add(createHstComponentParameter("document1", new RelativeJcrPathParameterConfig()));
        componentParameters.add(createHstComponentParameter("image1", new AbsoluteJcrPathParameterConfig()));
        componentParameters.add(
            createHstComponentParameter("dropdown1", createNiceMock(DropdownListParameterConfig.class)));
        componentParameters.add(
            createHstComponentParameter("dropdown2", createNiceMock(DropdownListParameterConfig.class)));
        componentParameters.add(createHstComponentParameter("string1", null));
        componentParameters.add(createHstComponentParameter("string2", null));

        BaseHstDynamicComponent baseHstDynamicComponent = new MyBaseHstDynamicComponent(componentParameters);
        baseHstDynamicComponent.init(servletContext, compConfig);

        HippoBean siteContentBean = createNiceMock(HippoBean.class);
        HippoBean document1 = createNiceMock(HippoBean.class);
        HippoBean image1 = createNiceMock(HippoBean.class);
        ObjectBeanManager objectBeanManager = createNiceMock(ObjectBeanManager.class);
        expect(siteContentBean.getBean("banners/banner1")).andReturn(document1).anyTimes();
        expect(requestContext.getSiteContentBaseBean()).andReturn(siteContentBean).anyTimes();
        expect(objectBeanManager.getObject("/content/images/image1")).andReturn(image1).anyTimes();
        expect(requestContext.getObjectBeanManager()).andReturn(objectBeanManager).anyTimes();
        replay(requestContext, siteContentBean, objectBeanManager);

        MockHstRequest hstRequest = new MockHstRequest();
        hstRequest.setRequestContext(requestContext);
        MockHstResponse hstResponse = new MockHstResponse();
        baseHstDynamicComponent.doBeforeRender(hstRequest, hstResponse);

        assertEquals(2, hstRequest.getModelsMap().size());
        assertEquals(document1, hstRequest.getModelsMap().get("document1"));
        assertEquals(image1, hstRequest.getModelsMap().get("image1"));
    }

    @Test(expected = NullPointerException.class)
    public void test_dynamic_component_without_paramsinfo() {
        replay(requestContext);

        ProxyFactory factory = new ProxyFactory();
        ComponentConfiguration compConfig = (ComponentConfiguration) factory.createInvokerProxy(new Invoker() {
            public Object invoke(Object o, Method m, Object[] args) {
                return null;
            }
        }, new Class [] { ComponentConfiguration.class });

        DynamicComponentWithoutAnnotation dynamicComponentWithoutAnnotation = new DynamicComponentWithoutAnnotation();
        dynamicComponentWithoutAnnotation.init(servletContext, compConfig);

        MockHstRequest hstRequest = new MockHstRequest();
        hstRequest.setRequestContext(requestContext);
        MockHstResponse hstResponse = new MockHstResponse();
        dynamicComponentWithoutAnnotation.doBeforeRender(hstRequest, hstResponse);
    }

    @Test( expected = ClassCastException.class)
    public void test_dynamic_component_with_invalid_paramsinfo() {
        replay(requestContext);

        ProxyFactory factory = new ProxyFactory();
        ComponentConfiguration compConfig = (ComponentConfiguration) factory.createInvokerProxy(new Invoker() {
            public Object invoke(Object o, Method m, Object[] args) {
                return null;
            }
        }, new Class [] { ComponentConfiguration.class });

        DynamicComponentWithWrongAnnotation dynamicComponentWithWrongAnnotation =
                new DynamicComponentWithWrongAnnotation();
        dynamicComponentWithWrongAnnotation.init(servletContext, compConfig);

        MockHstRequest hstRequest = new MockHstRequest();
        hstRequest.setRequestContext(requestContext);
        MockHstResponse hstResponse = new MockHstResponse();
        dynamicComponentWithWrongAnnotation.doBeforeRender(hstRequest, hstResponse);
    }

    private DynamicParameter createHstComponentParameter(String name, DynamicParameterConfig dynamicParameterConfig) {
        return new DynamicParameter(){

            @Override
            public String getName() {
                return name;
            }

            @Override
            public boolean isRequired() {
                return false;
            }

            @Override
            public String getDefaultValue() {
                return null;
            }

            @Override
            public String getDisplayName() {
                return null;
            }

            @Override
            public boolean isHideInChannelManager() {
                return false;
            }

            @Override
            public ParameterValueType getValueType() {
                return null;
            }

            @Override
            public boolean isResidual() {
                return true;
            }

            @Override
            public DynamicParameterConfig getComponentParameterConfig() {
                return dynamicParameterConfig;
            }
        };
    }

    private static class RelativeJcrPathParameterConfig implements JcrPathParameterConfig {

        @Override
        public String getPickerConfiguration() {
            return null;
        }

        @Override
        public String getPickerInitialPath() {
            return null;
        }

        @Override
        public boolean isPickerRemembersLastVisited() {
            return false;
        }

        @Override
        public String[] getPickerSelectableNodeTypes() {
            return new String[0];
        }

        @Override
        public boolean isRelative() {
            return true;
        }

        @Override
        public String getPickerRootPath() {
            return null;
        }

        @Override
        public Type getType() {
            return null;
        }
    }

    private static class AbsoluteJcrPathParameterConfig implements JcrPathParameterConfig {

        @Override
        public String getPickerConfiguration() {
            return null;
        }

        @Override
        public String getPickerInitialPath() {
            return null;
        }

        @Override
        public boolean isPickerRemembersLastVisited() {
            return false;
        }

        @Override
        public String[] getPickerSelectableNodeTypes() {
            return new String[0];
        }

        @Override
        public boolean isRelative() {
            return false;
        }

        @Override
        public String getPickerRootPath() {
            return null;
        }

        @Override
        public Type getType() {
            return null;
        }
    }

    @ParametersInfo(type = DynamicComponentInfo.class)
    private static class MyBaseHstDynamicComponent extends BaseHstDynamicComponent {

        private List<DynamicParameter> componentParameters;

        public MyBaseHstDynamicComponent(List<DynamicParameter> componentParameters){
            this.componentParameters = componentParameters;
        }

        @Override
        protected DynamicComponentInfo getComponentParametersInfo(final HstRequest request) {
            return new DynamicComponentInfo(){

                @Override
                public Map<String, Object> getResidualParameterValues() {
                    return componentParameters.stream().filter(DynamicParameter::isResidual).collect(Collectors.toMap(parameter -> parameter.getName(), parameter -> getComponentParameter(parameter.getName())));
                }

                @Override
                public List<DynamicParameter> getDynamicComponentParameters() {
                    return componentParameters;
                }
            };
        }
    }


}
