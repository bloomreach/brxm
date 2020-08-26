/*
 *  Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.bind.JAXBException;

import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.components.ParameterValueType;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.mock.configuration.components.MockHstComponentConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractPageComposerTest;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentPropertyRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ServerErrorException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;
import org.hippoecm.hst.platform.configuration.components.DynamicComponentParameter;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockNodeFactory;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ContainerItemMenuServiceTest extends AbstractPageComposerTest{

    private static final String HST_PARAMETERVALUES = "hst:parametervalues";
    private static final String HST_PARAMETERNAMES = "hst:parameternames";
    private static final String HST_PARAMETERNAMEPREFIXES = "hst:parameternameprefixes";
    private ContainerItemComponentService containerItemComponentService;
    private ContainerItemHelper helper;

    private PageComposerContextService mockPageComposerContextService;

    @Before
    public void setUp() throws Exception {

        super.setUp();
        helper = new ContainerItemHelper();
        mockPageComposerContextService = EasyMock.createNiceMock(PageComposerContextService.class);

        helper.setPageComposerContextService(mockPageComposerContextService);
        containerItemComponentService = new ContainerItemComponentServiceImpl(mockPageComposerContextService, helper, Collections.emptyList());
    }

    @Test
    public void testGetParametersForEmptyPrefix() throws RepositoryException, ClassNotFoundException, JAXBException, IOException, ServerErrorException {
        MockNode node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/services/ContainerItemComponentResourceTest-test-component.xml");
        configureMockContainerItemComponent(node);

        List<ContainerItemComponentPropertyRepresentation> result = containerItemComponentService.getVariant("", null).getProperties();
        assertEquals(2, result.size());
        assertNameValueDefault(result.get(0), "parameterOne", "bar", "");
        assertNameValueDefault(result.get(1), "parameterTwo", "", "test");
    }

    private void configureMockContainerItemComponent(final Node node) throws RepositoryException {
        expect(mockPageComposerContextService.getRequestConfigNode(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT))
                .andReturn(node).anyTimes();

        Mount editingMount = createMock(Mount.class);
        HstSite hstSite = createMock(HstSite.class);

        expect(mockPageComposerContextService.getEditingPreviewSite()).andStubReturn(hstSite);

        HstComponentsConfiguration componentsConfiguration = createMock(HstComponentsConfiguration.class);
        expect(hstSite.getComponentsConfiguration()).andStubReturn(componentsConfiguration);

        final List<Parameter> parameters = Arrays.stream(DummyInfo.class.getMethods())
                .map(x -> x.getAnnotation(Parameter.class))
                .sorted(Comparator.comparing(Parameter::name)).collect(Collectors.toList());

        final MockHstComponentConfiguration componentReference = new MockHstComponentConfiguration("id");
        componentReference.setComponentClassName(DummyComponent.class.getName());
        componentReference.setCanonicalStoredLocation("/");
        componentReference.setCanonicalIdentifier(node.getIdentifier());
        List<DynamicParameter> dynamicParameters = getDynamicParameters(parameters);
        componentReference.setDynamicComponentParameters(dynamicParameters);
        final HashMap<String, HstComponentConfiguration> componentConfigurations = new HashMap<>();
        componentConfigurations.put("/", componentReference);

        expect(componentsConfiguration.getComponentConfigurations())
                .andReturn(componentConfigurations).anyTimes();

        EasyMock.replay(mockPageComposerContextService, editingMount, hstSite, componentsConfiguration);
    }

    @NotNull
    private List<DynamicParameter> getDynamicParameters(List<Parameter> parameters) {
        List<DynamicParameter> dynamicParameters = new ArrayList<>();
        for (Parameter parameter : parameters) {
            DynamicParameter dynamicParameter = new DynamicComponentParameter(parameter, ParameterValueType.STRING);
            dynamicParameters.add(dynamicParameter);
        }
        return dynamicParameters;
    }

    @Test
    public void testGetParametersForDefaultPrefix() throws RepositoryException, ClassNotFoundException, JAXBException, IOException, ServerErrorException {
        MockNode node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/services/ContainerItemComponentResourceTest-test-component.xml");
        configureMockContainerItemComponent(node);

        List<ContainerItemComponentPropertyRepresentation> result = containerItemComponentService.getVariant("hippo-default", null).getProperties();
        assertEquals(2, result.size());
        assertNameValueDefault(result.get(0), "parameterOne", "bar", "");
        assertNameValueDefault(result.get(1), "parameterTwo", "", "test");
    }

    @Test
    public void testGetParametersForPrefix() throws RepositoryException, ClassNotFoundException, JAXBException, IOException, ServerErrorException {
        MockNode node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/services/ContainerItemComponentResourceTest-test-component.xml");
        configureMockContainerItemComponent(node);
        List<ContainerItemComponentPropertyRepresentation> result = containerItemComponentService.getVariant("prefix", null).getProperties();
        assertEquals(2, result.size());
        assertNameValueDefault(result.get(0), "parameterOne", "baz", "");
        assertNameValueDefault(result.get(1), "parameterTwo", "", "test");
    }

    private static void assertNameValueDefault(ContainerItemComponentPropertyRepresentation property, String name, String value, String defaultValue) {
        assertEquals("Wrong name", name, property.getName());
        assertEquals("Wrong value", value, property.getValue());
        assertEquals("Wrong default value", defaultValue, property.getDefaultValue());
    }

    @Test(expected = ClientException.class)
    public void default_variant_should_not_be_deleted() throws RepositoryException, IOException, JAXBException {
        Node node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/services/ContainerItemComponentResourceTest-empty-component.xml");
        configureMockContainerItemComponent(node);

        containerItemComponentService.deleteVariant("hippo-default", 0);
        fail("Default variant should not be possible to be removed");
    }
}
