/*
 *  Copyright 2015-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.linking;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.ConfigurationUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DocumentParamsScannerTest {

    private final static ClassLoader classLoader = DocumentParamsScannerTest.class.getClassLoader();

    private HstModelRegistry hstModelRegistry;

    @Before
    public void setUp() throws Exception {

        hstModelRegistry = EasyMock.createNiceMock(HstModelRegistry.class);

        final ComponentManager componentManager = EasyMock.createNiceMock(ComponentManager.class);
        expect(componentManager.getComponent(EasyMock.anyString())).andStubReturn(null);
        final HstModel hstModel = EasyMock.createNiceMock(HstModel.class);
        expect(hstModel.getComponentManager()).andStubReturn(componentManager);
        expect(hstModelRegistry.getHstModel(EasyMock.anyObject(ClassLoader.class))).andStubReturn(hstModel);

        replay(hstModelRegistry, componentManager, hstModel);

        HippoServiceRegistry.register(hstModelRegistry, HstModelRegistry.class);

    }

    @After
    public void tearDown() {
        HippoServiceRegistry.unregister(hstModelRegistry, HstModelRegistry.class);
    }
    
    @Test
    public void non_existing_component_class_returns_empty_set_parameters() {
        HstComponentConfiguration componentConfiguration = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(componentConfiguration.getComponentClassName()).andReturn("NonExistingclass");
        expect(componentConfiguration.getParametersInfoClassName()).andReturn(null);
        expect(componentConfiguration.getDynamicComponentParameters()).andStubReturn(Collections.emptyList());

        replay(componentConfiguration);
        
        assertEquals(0, DocumentParamsScanner.getNames(componentConfiguration, classLoader).size());
    }

    @Test
    public void component_without_parametersInfo_returns_empty_set_parameters() {
        HstComponentConfiguration componentConfiguration = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(componentConfiguration.getComponentClassName()).andReturn(GenericHstComponent.class.getName());
        expect(componentConfiguration.getParametersInfoClassName()).andReturn(null);
        expect(componentConfiguration.getDynamicComponentParameters()).andStubReturn(Collections.emptyList());
        
        replay(componentConfiguration);

        assertEquals(0, DocumentParamsScanner.getNames(componentConfiguration, classLoader).size());
    }

    public static interface JcrPathParametersInfo {
        @Parameter(name = "news-jcrPath", displayName = "Picked News")
        @JcrPath(isRelative = false, pickerInitialPath = "")
        String getPickedNews();
    }

    @ParametersInfo(type = JcrPathParametersInfo.class)
    public static class JcrPathComponent extends GenericHstComponent {
    }

    @Test
    public void jcrPath_parameter() {
        HstComponentConfiguration componentConfiguration = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(componentConfiguration.getComponentClassName()).andReturn(JcrPathComponent.class.getName());
        expect(componentConfiguration.getParametersInfoClassName()).andReturn(JcrPathParametersInfo.class.getName());
        expect(componentConfiguration.getDynamicComponentParameters()).andStubReturn(Collections.emptyList());

        replay(componentConfiguration);

        final Set<String> names = DocumentParamsScanner.getNames(componentConfiguration, classLoader);
        assertEquals(1, names.size());
        assertTrue(names.contains("news-jcrPath"));
    }

    public interface JcrPathParametersInfo2 {
        @Parameter(name = "news-jcrPath-2", displayName = "Picked News")
        @JcrPath()
        String getPickedNews();
    }

    @ParametersInfo(type = JcrPathParametersInfo2.class)
    public static class JcrPathComponent2 extends GenericHstComponent {
    }

    public interface TwoJcrPathParametersInfo {
        @Parameter(name = "news-jcrPath", displayName = "Picked News")
        @JcrPath(isRelative = false, pickerInitialPath = "")
        String getPickedNewsJcrPath();

        @Parameter(name = "news-jcrPath-2", displayName = "Picked News")
        @JcrPath()
        String getPickedNewsJcrPath2();
    }

    @ParametersInfo(type = TwoJcrPathParametersInfo.class)
    public static class TwoJcrPathsComponent extends GenericHstComponent {
    }

    @Test
    public void two_jcrPath_parameters_combined() {
        HstComponentConfiguration componentConfiguration = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(componentConfiguration.getComponentClassName()).andReturn(TwoJcrPathsComponent.class.getName());
        expect(componentConfiguration.getParametersInfoClassName()).andReturn(TwoJcrPathParametersInfo.class.getName());
        expect(componentConfiguration.getDynamicComponentParameters()).andStubReturn(Collections.emptyList());
        replay(componentConfiguration);

        final Set<String> names = DocumentParamsScanner.getNames(componentConfiguration, classLoader);
        assertEquals(2, names.size());
        assertTrue(names.contains("news-jcrPath"));
        assertTrue(names.contains("news-jcrPath-2"));
    }

    public interface InheritanceParametersInfo extends JcrPathParametersInfo, JcrPathParametersInfo2 {
    }

    @ParametersInfo(type = InheritanceParametersInfo.class)
    public static class InheritanceComponent extends GenericHstComponent {
    }

    @Test
    public void jcrPaths_parameter_inheritance() {
        HstComponentConfiguration componentConfiguration = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(componentConfiguration.getComponentClassName()).andReturn(InheritanceComponent.class.getName());
        expect(componentConfiguration.getParametersInfoClassName()).andReturn(InheritanceParametersInfo.class.getName());
        expect(componentConfiguration.getDynamicComponentParameters()).andStubReturn(Collections.emptyList());

        replay(componentConfiguration);

        final Set<String> names = DocumentParamsScanner.getNames(componentConfiguration, classLoader);
        assertEquals(2, names.size());
        assertTrue(names.contains("news-jcrPath"));
        assertTrue(names.contains("news-jcrPath-2"));
    }

    @Test
    public void component_scanning_cached() {
        HstComponentConfiguration componentConfiguration = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(componentConfiguration.getComponentClassName()).andReturn(JcrPathComponent.class.getName()).times(2);
        expect(componentConfiguration.getParametersInfoClassName()).andReturn(JcrPathParametersInfo.class.getName()).times(2);
        expect(componentConfiguration.getDynamicComponentParameters()).andStubReturn(Collections.emptyList());

        HstComponentConfiguration componentConfiguration2 = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(componentConfiguration2.getComponentClassName()).andReturn(JcrPathComponent2.class.getName()).times(2);
        expect(componentConfiguration2.getParametersInfoClassName()).andReturn(JcrPathParametersInfo2.class.getName()).times(2);
        expect(componentConfiguration2.getDynamicComponentParameters()).andStubReturn(Collections.emptyList());

        replay(componentConfiguration, componentConfiguration2);

        assertSame(DocumentParamsScanner.getNames(componentConfiguration, classLoader), 
                   DocumentParamsScanner.getNames(componentConfiguration, classLoader));

        assertSame(DocumentParamsScanner.getNames(componentConfiguration2, classLoader), 
                   DocumentParamsScanner.getNames(componentConfiguration2, classLoader));
    }

    @Test
    public void two_jcrPaths_from_ComponentConfiguration() {
        HstComponentConfiguration componentConfiguration = EasyMock.createNiceMock(HstComponentConfiguration.class);

        expect(componentConfiguration.getComponentClassName()).andReturn(InheritanceComponent.class.getName());
        expect(componentConfiguration.getParameter(eq("news-jcrPath"))).andReturn("/jcrPathNews");
        expect(componentConfiguration.getParameter(eq("news-jcrPath-2"))).andReturn("/jcrPathTwoNews");
        expect(componentConfiguration.getParameterPrefixes()).andReturn(Collections.emptySet()).anyTimes();
        expect(componentConfiguration.getChildren()).andReturn(Collections.emptyMap()).anyTimes();
        expect(componentConfiguration.getDynamicComponentParameters()).andStubReturn(Collections.emptyList());
        replay(componentConfiguration);

        final List<String> documentPaths = DocumentParamsScanner.findDocumentPathsRecursive(componentConfiguration, classLoader);
        assertEquals(2, documentPaths.size());
        assertTrue(documentPaths.contains("/jcrPathNews"));
        assertTrue(documentPaths.contains("/jcrPathTwoNews"));
    }

    @Test
    public void two_jcrPaths_from_ComponentConfiguration_tree() {
        HstComponentConfiguration root = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(root.getComponentClassName()).andReturn(GenericHstComponent.class.getName());
        expect(root.getDynamicComponentParameters()).andStubReturn(Collections.emptyList());

        HstComponentConfiguration child1 = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(child1.getComponentClassName()).andReturn(JcrPathComponent.class.getName());
        expect(child1.getParameter(eq("news-jcrPath"))).andReturn("/jcrPathNews");
        expect(child1.getParameterPrefixes()).andReturn(Collections.emptySet()).anyTimes();
        expect(child1.getChildren()).andReturn(Collections.emptyMap()).anyTimes();
        expect(child1.getDynamicComponentParameters()).andStubReturn(Collections.emptyList());

        HstComponentConfiguration child2 = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(child2.getComponentClassName()).andReturn(JcrPathComponent2.class.getName());
        expect(child2.getParameter(eq("news-jcrPath-2"))).andReturn("/jcrPathTwoNews");
        expect(child2.getParameterPrefixes()).andReturn(Collections.emptySet()).anyTimes();
        expect(child2.getChildren()).andReturn(Collections.emptyMap()).anyTimes();
        expect(child2.getDynamicComponentParameters()).andStubReturn(Collections.emptyList());

        expect(root.getParameterPrefixes()).andReturn(Collections.emptySet()).anyTimes();
        Map<String, HstComponentConfiguration> expectedChildren = new HashMap<>();
        expectedChildren.put("left", child1);
        expectedChildren.put("right", child2);
        expect(root.getChildren()).andReturn(expectedChildren).anyTimes();
        replay(root, child1, child2);

        final List<String> documentPaths = DocumentParamsScanner.findDocumentPathsRecursive(root, classLoader);
        assertEquals(2, documentPaths.size());
        assertTrue(documentPaths.contains("/jcrPathNews"));
        assertTrue(documentPaths.contains("/jcrPathTwoNews"));
    }

    @Test
    public void two_jcrPaths_from_ComponentConfiguration_tree_with_variants() {
        HstComponentConfiguration root = setUpHstConfiguration();

        final List<String> documentPaths = DocumentParamsScanner.findDocumentPathsRecursive(root, classLoader);
        assertEquals(4, documentPaths.size());
        assertTrue(documentPaths.contains("/jcrPathNews"));
        assertTrue(documentPaths.contains("/jcrPathTwoNews"));
        assertTrue(documentPaths.contains("/jcrPathNewsProfessional"));
        assertTrue(documentPaths.contains("/jcrPathTwoNewsProfessional"));

    }

    private HstComponentConfiguration setUpHstConfiguration() {
        HstComponentConfiguration root = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(root.getComponentClassName()).andReturn(GenericHstComponent.class.getName());
        expect(root.getDynamicComponentParameters()).andStubReturn(Collections.emptyList());

        HstComponentConfiguration child1 = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(child1.getComponentClassName()).andReturn(JcrPathComponent.class.getName());
        expect(child1.getParameter(eq("news-jcrPath"))).andReturn("/jcrPathNews");
        expect(child1.getParameter(
                eq(ConfigurationUtils.createPrefixedParameterName("professional", "news-jcrPath"))))
                .andReturn("/jcrPathNewsProfessional");
        Set<String> expectedParameterPrefixes = new HashSet<>();
        expectedParameterPrefixes.add("professional");
        expect(child1.getParameterPrefixes()).andReturn(expectedParameterPrefixes).anyTimes();
        expect(child1.getChildren()).andReturn(Collections.emptyMap()).anyTimes();
        expect(child1.getDynamicComponentParameters()).andStubReturn(Collections.emptyList());

        HstComponentConfiguration child2 = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(child2.getComponentClassName()).andReturn(JcrPathComponent2.class.getName());
        expect(child2.getParameter(eq("news-jcrPath-2"))).andReturn("/jcrPathTwoNews");
        expect(child2.getParameter(
                eq(ConfigurationUtils.createPrefixedParameterName("professional","news-jcrPath-2"))))
                .andReturn("/jcrPathTwoNewsProfessional");
        expect(child2.getParameterPrefixes()).andReturn(expectedParameterPrefixes).anyTimes();
        expect(child2.getChildren()).andReturn(Collections.emptyMap()).anyTimes();
        expect(child2.getDynamicComponentParameters()).andStubReturn(Collections.emptyList());

        expect(root.getParameterPrefixes()).andReturn(Collections.emptySet()).anyTimes();
        Map<String, HstComponentConfiguration> expectedChildren = new HashMap<>();
        expectedChildren.put("left", child1);
        expectedChildren.put("right", child2);
        expect(root.getChildren()).andReturn(expectedChildren).anyTimes();
        expect(root.getChildByName("child1")).andReturn(child1).anyTimes();
        expect(root.getChildByName("child2")).andReturn(child2).anyTimes();
        replay(root, child1, child2);
        return root;
    }


    @Test
    public void test_with_predicate() {
        HstComponentConfiguration root = setUpHstConfiguration();
        final HstComponentConfiguration toSkip = root.getChildByName("child1");
        final List<String> documentPaths = DocumentParamsScanner.findDocumentPathsRecursive(root, classLoader, new SkipChildPredicate(toSkip));
        assertEquals(2, documentPaths.size());
        assertTrue(documentPaths.contains("/jcrPathTwoNews"));
        assertTrue(documentPaths.contains("/jcrPathTwoNewsProfessional"));
    }

    class SkipChildPredicate implements Predicate<HstComponentConfiguration> {

        final HstComponentConfiguration toSkip;
        SkipChildPredicate(final HstComponentConfiguration toSkip){
            this.toSkip = toSkip;
        }

        @Override
        public boolean test(final HstComponentConfiguration componentConfiguration) {
            if (toSkip == componentConfiguration) {
                return false;
            }
            return true;
        }
    }
}
