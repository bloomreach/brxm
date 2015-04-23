/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.ConfigurationUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.junit.Test;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DocumentParamsScannerTest {

    private final static ClassLoader classLoader = DocumentParamsScannerTest.class.getClassLoader();

    @Test
    public void non_existing_component_class_returns_empty_set_parameters() {
        assertEquals(0, DocumentParamsScanner.getNames("NonExistingClass", classLoader).size());
    }

    @Test
    public void component_without_parametersInfo_returns_empty_set_parameters() {
        assertEquals(0, DocumentParamsScanner.getNames(GenericHstComponent.class.getName(), classLoader).size());
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
        final Set<String> names = DocumentParamsScanner.getNames(JcrPathComponent.class.getName(), classLoader);
        assertEquals(1, names.size());
        assertTrue(names.contains("news-jcrPath"));
    }

    public static interface DocumentLinkParametersInfo {
        @Parameter(name = "news-documentLink", displayName = "Picked News")
        @DocumentLink
        String getPickedNews();
    }

    @ParametersInfo(type = DocumentLinkParametersInfo.class)
    public static class DocumentLinkComponent extends GenericHstComponent {
    }

    @Test
    public void documentLink_parameter() {
        final Set<String> names = DocumentParamsScanner.getNames(DocumentLinkComponent.class.getName(), classLoader);
        assertEquals(1, names.size());
        assertTrue(names.contains("news-documentLink"));
    }

    public static interface JcrPathDocumentLinkParametersInfo {
        @Parameter(name = "news-jcrPath", displayName = "Picked News")
        @JcrPath(isRelative = false, pickerInitialPath = "")
        String getPickedNewsJcrPath();

        @Parameter(name = "news-documentLink", displayName = "Picked News")
        @DocumentLink
        String getPickedNewsDocumentLink();
    }

    @ParametersInfo(type = JcrPathDocumentLinkParametersInfo.class)
    public static class JcrPathDocumentLinkComponent extends GenericHstComponent {
    }

    @Test
    public void jcrPath_and_documentLink_parameter_combined() {
        final Set<String> names = DocumentParamsScanner.getNames(JcrPathDocumentLinkComponent.class.getName(), classLoader);
        assertEquals(2, names.size());
        assertTrue(names.contains("news-jcrPath"));
        assertTrue(names.contains("news-documentLink"));
    }


    public static interface InheritanceParametersInfo extends JcrPathParametersInfo, DocumentLinkParametersInfo {

    }

    @ParametersInfo(type = InheritanceParametersInfo.class)
    public static class InheritanceComponent extends GenericHstComponent {
    }

    @Test
    public void jcrPath_and_documentLink_parameter_inheritance() {
        final Set<String> names = DocumentParamsScanner.getNames(InheritanceComponent.class.getName(), classLoader);
        assertEquals(2, names.size());
        assertTrue(names.contains("news-jcrPath"));
        assertTrue(names.contains("news-documentLink"));
    }


    @Test
    public void component_scanning_cached() {
        assertTrue(DocumentParamsScanner.getNames(JcrPathComponent.class.getName(), classLoader) ==
                DocumentParamsScanner.getNames(JcrPathComponent.class.getName(), classLoader));
        assertTrue(DocumentParamsScanner.getNames(DocumentLinkComponent.class.getName(), classLoader) ==
                DocumentParamsScanner.getNames(DocumentLinkComponent.class.getName(), classLoader));
        assertTrue(DocumentParamsScanner.getNames(JcrPathDocumentLinkComponent.class.getName(), classLoader) ==
                DocumentParamsScanner.getNames(JcrPathDocumentLinkComponent.class.getName(), classLoader));
    }

    @Test
    public void jcrPath_and_documentLink_from_ComponentConfiguration() {
        HstComponentConfiguration componentConfiguration = EasyMock.createNiceMock(HstComponentConfiguration.class);

        expect(componentConfiguration.getComponentClassName()).andReturn(InheritanceComponent.class.getName());
        expect(componentConfiguration.getParameter(eq("news-jcrPath"))).andReturn("/jcrPathNews");
        expect(componentConfiguration.getParameter(eq("news-documentLink"))).andReturn("/documentLinkNews");
        expect(componentConfiguration.getParameterPrefixes()).andReturn(Collections.emptySet()).anyTimes();
        expect(componentConfiguration.getChildren()).andReturn(Collections.emptyMap()).anyTimes();
        replay(componentConfiguration);

        final List<String> documentPaths = DocumentParamsScanner.findDocumentPathsRecursive(componentConfiguration, classLoader);
        assertEquals(2, documentPaths.size());
        assertTrue(documentPaths.contains("/jcrPathNews"));
        assertTrue(documentPaths.contains("/documentLinkNews"));
    }

    @Test
    public void jcrPath_and_documentLink_from_ComponentConfiguration_tree() {
        HstComponentConfiguration root = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(root.getComponentClassName()).andReturn(GenericHstComponent.class.getName());

        HstComponentConfiguration child1 = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(child1.getComponentClassName()).andReturn(JcrPathComponent.class.getName());
        expect(child1.getParameter(eq("news-jcrPath"))).andReturn("/jcrPathNews");
        expect(child1.getParameterPrefixes()).andReturn(Collections.emptySet()).anyTimes();
        expect(child1.getChildren()).andReturn(Collections.emptyMap()).anyTimes();

        HstComponentConfiguration child2 = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(child2.getComponentClassName()).andReturn(DocumentLinkComponent.class.getName());
        expect(child2.getParameter(eq("news-documentLink"))).andReturn("/documentLinkNews");
        expect(child2.getParameterPrefixes()).andReturn(Collections.emptySet()).anyTimes();
        expect(child2.getChildren()).andReturn(Collections.emptyMap()).anyTimes();

        expect(root.getParameterPrefixes()).andReturn(Collections.emptySet()).anyTimes();
        expect(root.getChildren()).andReturn(ImmutableMap.of("left", child1, "right", child2)).anyTimes();
        replay(root, child1, child2);

        final List<String> documentPaths = DocumentParamsScanner.findDocumentPathsRecursive(root, classLoader);
        assertEquals(2, documentPaths.size());
        assertTrue(documentPaths.contains("/jcrPathNews"));
        assertTrue(documentPaths.contains("/documentLinkNews"));
    }

    @Test
    public void jcrPath_and_documentLink_from_ComponentConfiguration_tree_with_variants() {
        HstComponentConfiguration root = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(root.getComponentClassName()).andReturn(GenericHstComponent.class.getName());

        HstComponentConfiguration child1 = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(child1.getComponentClassName()).andReturn(JcrPathComponent.class.getName());
        expect(child1.getParameter(eq("news-jcrPath"))).andReturn("/jcrPathNews");
        expect(child1.getParameter(
                eq(ConfigurationUtils.createPrefixedParameterName("professional","news-jcrPath"))))
                .andReturn("/jcrPathNewsProfessional");
        expect(child1.getParameterPrefixes()).andReturn(ImmutableSet.of("professional")).anyTimes();
        expect(child1.getChildren()).andReturn(Collections.emptyMap()).anyTimes();

        HstComponentConfiguration child2 = EasyMock.createNiceMock(HstComponentConfiguration.class);
        expect(child2.getComponentClassName()).andReturn(DocumentLinkComponent.class.getName());
        expect(child2.getParameter(eq("news-documentLink"))).andReturn("/documentLinkNews");
        expect(child2.getParameter(
                eq(ConfigurationUtils.createPrefixedParameterName("professional","news-documentLink"))))
                .andReturn("/documentLinkNewsProfessional");
        expect(child2.getParameterPrefixes()).andReturn(ImmutableSet.of("professional")).anyTimes();
        expect(child2.getChildren()).andReturn(Collections.emptyMap()).anyTimes();

        expect(root.getParameterPrefixes()).andReturn(Collections.emptySet()).anyTimes();
        expect(root.getChildren()).andReturn(ImmutableMap.of("left", child1, "right", child2)).anyTimes();
        replay(root, child1, child2);

        final List<String> documentPaths = DocumentParamsScanner.findDocumentPathsRecursive(root, classLoader);
        assertEquals(4, documentPaths.size());
        assertTrue(documentPaths.contains("/jcrPathNews"));
        assertTrue(documentPaths.contains("/documentLinkNews"));
        assertTrue(documentPaths.contains("/jcrPathNewsProfessional"));
        assertTrue(documentPaths.contains("/documentLinkNewsProfessional"));

    }
}
