/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.util;

import javax.jcr.Node;
import javax.jcr.Property;

import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ParametersInfoAnnotationUtilsTest {

    @Test
    public void testGetParametersInfoAnnotation_Class_ComponentConfiguration() throws Exception {
        Class<?> componentClazz = null;
        ComponentConfiguration componentConfig = null;

        // when both componentClazz and componentConfig are null...
        ParametersInfo paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazz,
                componentConfig);
        assertNull(paramsInfo);

        // when componentClazz is an annotated class and componentConfig are null...
        componentClazz = AnnotatedComponent.class;
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazz, componentConfig);
        assertNotNull(paramsInfo);
        assertEquals(ExampleParametersInfoType1.class, paramsInfo.type());

        // when componentClazz is an annotated class and componentConfig is non-null but no overriding...
        componentConfig = createMockComponentConfiguration(null);
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazz, componentConfig);
        assertNotNull(paramsInfo);
        assertEquals(ExampleParametersInfoType1.class, paramsInfo.type());

        // when componentClazz is an annotated class and componentConfig is non-null with overriding...
        componentConfig = createMockComponentConfiguration(ExampleParametersInfoType2.class.getName());
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazz, componentConfig);
        assertNotNull(paramsInfo);
        assertEquals(ExampleParametersInfoType2.class, paramsInfo.type());

        // when componentClazz is a non-annotated class and componentConfig are null...
        componentClazz = NonAnnotatedComponent.class;
        componentConfig = null;
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazz, componentConfig);
        assertNull(paramsInfo);

        // when componentClazz is a non-annotated class and componentConfig is non-null but no overriding...
        componentConfig = createMockComponentConfiguration(null);
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazz, componentConfig);
        assertNull(paramsInfo);

        // when componentClazz is a non-annotated class and componentConfig is non-null with overriding...
        componentConfig = createMockComponentConfiguration(ExampleParametersInfoType2.class.getName());
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazz, componentConfig);
        assertNotNull(paramsInfo);
        assertEquals(ExampleParametersInfoType2.class, paramsInfo.type());
    }

    @Test
    public void testGetParametersInfoAnnotation_Class_HstComponentConfiguration() throws Exception {
        Class<?> componentClazz = null;
        HstComponentConfiguration componentConfig = null;

        // when both componentClazz and componentConfig are null...
        ParametersInfo paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazz,
                componentConfig);
        assertNull(paramsInfo);

        // when componentClazz is an annotated class and componentConfig are null...
        componentClazz = AnnotatedComponent.class;
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazz, componentConfig);
        assertNotNull(paramsInfo);
        assertEquals(ExampleParametersInfoType1.class, paramsInfo.type());

        // when componentClazz is an annotated class and componentConfig is non-null but no overriding...
        componentConfig = createMockHstComponentConfiguration(AnnotatedComponent.class.getName(), null);
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazz, componentConfig);
        assertNotNull(paramsInfo);
        assertEquals(ExampleParametersInfoType1.class, paramsInfo.type());

        // when componentClazz is an annotated class and componentConfig is non-null with overriding...
        componentConfig = createMockHstComponentConfiguration(AnnotatedComponent.class.getName(),
                ExampleParametersInfoType2.class.getName());
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazz, componentConfig);
        assertNotNull(paramsInfo);
        assertEquals(ExampleParametersInfoType2.class, paramsInfo.type());

        // when componentClazz is a non-annotated class and componentConfig are null...
        componentClazz = NonAnnotatedComponent.class;
        componentConfig = null;
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazz, componentConfig);
        assertNull(paramsInfo);

        // when componentClazz is a non-annotated class and componentConfig is non-null but no overriding...
        componentConfig = createMockHstComponentConfiguration(NonAnnotatedComponent.class.getName(), null);
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazz, componentConfig);
        assertNull(paramsInfo);

        // when componentClazz is a non-annotated class and componentConfig is non-null with overriding...
        componentConfig = createMockHstComponentConfiguration(NonAnnotatedComponent.class.getName(),
                ExampleParametersInfoType2.class.getName());
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazz, componentConfig);
        assertNotNull(paramsInfo);
        assertEquals(ExampleParametersInfoType2.class, paramsInfo.type());
    }

    @Test
    public void testGetParametersInfoAnnotation_HstComponentConfiguration() throws Exception {
        HstComponentConfiguration componentConfig = null;

        // when componentConfig are null...
        ParametersInfo paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentConfig);
        assertNull(paramsInfo);

        // with an annotated class, when componentConfig is non-null but no overriding...
        componentConfig = createMockHstComponentConfiguration(AnnotatedComponent.class.getName(), null);
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentConfig);
        assertNotNull(paramsInfo);
        assertEquals(ExampleParametersInfoType1.class, paramsInfo.type());

        // with an annotated class, when componentConfig is non-null with overriding...
        componentConfig = createMockHstComponentConfiguration(AnnotatedComponent.class.getName(),
                ExampleParametersInfoType2.class.getName());
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentConfig);
        assertNotNull(paramsInfo);
        assertEquals(ExampleParametersInfoType2.class, paramsInfo.type());

        // with a non-annotated class, when componentConfig is non-null but no overriding...
        componentConfig = createMockHstComponentConfiguration(NonAnnotatedComponent.class.getName(), null);
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentConfig);
        assertNull(paramsInfo);

        // with a non-annotated class, when componentConfig is non-null with overriding...
        componentConfig = createMockHstComponentConfiguration(NonAnnotatedComponent.class.getName(),
                ExampleParametersInfoType2.class.getName());
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentConfig);
        assertNotNull(paramsInfo);
        assertEquals(ExampleParametersInfoType2.class, paramsInfo.type());
    }

    @Test
    public void testGetParametersInfoAnnotation_Node() throws Exception {
        Node componentConfigNode = null;

        // when componentConfig are null...
        ParametersInfo paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentConfigNode);
        assertNull(paramsInfo);

        // with an annotated class name, when componentConfigNode is non-null but no overriding...
        componentConfigNode = createMockComponentConfigurationNode(AnnotatedComponent.class.getName(), null,
                HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentConfigNode);
        assertNotNull(paramsInfo);
        assertEquals(ExampleParametersInfoType1.class, paramsInfo.type());

        // with an annotated class name, when componentConfigNode is non-null with overriding...
        componentConfigNode = createMockComponentConfigurationNode(AnnotatedComponent.class.getName(),
                ExampleParametersInfoType2.class.getName(), HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentConfigNode);
        assertNotNull(paramsInfo);
        assertEquals(ExampleParametersInfoType2.class, paramsInfo.type());

        // with a non-annotated class name, when componentConfigNode is non-null but no overriding...
        componentConfigNode = createMockComponentConfigurationNode(NonAnnotatedComponent.class.getName(), null,
                HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentConfigNode);
        assertNull(paramsInfo);

        // with a non-annotated class name, when componentConfigNode is non-null with overriding...
        componentConfigNode = createMockComponentConfigurationNode(NonAnnotatedComponent.class.getName(),
                ExampleParametersInfoType2.class.getName(), HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentConfigNode);
        assertNotNull(paramsInfo);
        assertEquals(ExampleParametersInfoType2.class, paramsInfo.type());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetParametersInfoAnnotation_Node_only_works_for_containeritemcomponent() throws Exception {
        final Node componentConfigNode = createMockComponentConfigurationNode(AnnotatedComponent.class.getName(), null,
                HstNodeTypes.NODETYPE_HST_COMPONENT);
        ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentConfigNode);
    }

    @Test
    public void testGetParametersInfoAnnotation_String_String() throws Exception {
        String componentClazzName = null;
        String parametersInfoClassName = null;

        // when both are null...
        ParametersInfo paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazzName, parametersInfoClassName);
        assertNull(paramsInfo);

        // with annotated component class name and null parametersInfo class name...
        componentClazzName = AnnotatedComponent.class.getName();
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazzName, parametersInfoClassName);
        assertEquals(ExampleParametersInfoType1.class, paramsInfo.type());

        // with annotated component class name and overriding non-null parametersInfo class name...
        componentClazzName = AnnotatedComponent.class.getName();
        parametersInfoClassName = ExampleParametersInfoType2.class.getName();
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazzName, parametersInfoClassName);
        assertEquals(ExampleParametersInfoType2.class, paramsInfo.type());

        // with non-annotated component class name and null parametersInfo class name...
        componentClazzName = NonAnnotatedComponent.class.getName();
        parametersInfoClassName = null;
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazzName, parametersInfoClassName);
        assertNull(paramsInfo);

        // with non-annotated component class name and non-null parametersInfo class name...
        componentClazzName = NonAnnotatedComponent.class.getName();
        parametersInfoClassName = ExampleParametersInfoType2.class.getName();
        paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClazzName, parametersInfoClassName);
        assertEquals(ExampleParametersInfoType2.class, paramsInfo.type());
    }

    private ComponentConfiguration createMockComponentConfiguration(String paramsInfoClassName) {
        ComponentConfiguration componentConfig = EasyMock.createNiceMock(ComponentConfiguration.class);
        EasyMock.expect(componentConfig.getParametersInfoClassName()).andReturn(paramsInfoClassName).anyTimes();
        EasyMock.replay(componentConfig);
        return componentConfig;
    }

    private HstComponentConfiguration createMockHstComponentConfiguration(String componentClazzName,
            String paramsInfoClassName) {
        HstComponentConfiguration componentConfig = EasyMock.createNiceMock(HstComponentConfiguration.class);
        EasyMock.expect(componentConfig.getComponentClassName()).andReturn(componentClazzName).anyTimes();
        EasyMock.expect(componentConfig.getParametersInfoClassName()).andReturn(paramsInfoClassName).anyTimes();
        EasyMock.replay(componentConfig);
        return componentConfig;
    }

    private Node createMockComponentConfigurationNode(final String componentClazzName, final String paramsInfoClassName,
                                                      final String primaryNodeType)
            throws Exception {
        Node node = EasyMock.createNiceMock(Node.class);
        Property compClazzNameProp = EasyMock.createNiceMock(Property.class);
        EasyMock.expect(compClazzNameProp.getString()).andReturn(componentClazzName).anyTimes();
        Property paramsInfoClazzNameProp = EasyMock.createNiceMock(Property.class);
        EasyMock.expect(paramsInfoClazzNameProp.getString()).andReturn(paramsInfoClassName).anyTimes();
        EasyMock.expect(node.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME))
                .andReturn(componentClazzName != null).anyTimes();
        EasyMock.expect(node.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETERSINFO_CLASSNAME))
                .andReturn(paramsInfoClassName != null).anyTimes();
        EasyMock.expect(node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME))
                .andReturn(compClazzNameProp).anyTimes();
        EasyMock.expect(node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETERSINFO_CLASSNAME))
                .andReturn(paramsInfoClazzNameProp).anyTimes();
        EasyMock.expect(node.isNodeType(EasyMock.eq(primaryNodeType))).andReturn(true).anyTimes();
        EasyMock.replay(compClazzNameProp);
        EasyMock.replay(paramsInfoClazzNameProp);
        EasyMock.replay(node);
        return node;
    }

    @ParametersInfo(type = ExampleParametersInfoType1.class)
    public static class AnnotatedComponent {

    }

    public static class NonAnnotatedComponent {

    }

    public interface ExampleParametersInfoType1 {

        @Parameter(name = "param1")
        public String getParam1();

        @Parameter(name = "param2")
        public String getParam2();
    }

    public interface ExampleParametersInfoType2 {

        @Parameter(name = "param1")
        public String getParam1();

        @Parameter(name = "param2")
        public String getParam2();
    }
}
