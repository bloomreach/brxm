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
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXBException;

import org.apache.cxf.jaxrs.impl.MetadataMap;
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
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstComponentParameters;
import org.hippoecm.hst.platform.configuration.components.DynamicComponentParameter;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockNodeFactory;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ContainerItemComponentServiceTest extends AbstractPageComposerTest{

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
        EasyMock.expect(mockPageComposerContextService.getRequestConfigNode(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT))
                .andReturn(node).anyTimes();

        Mount editingMount = createMock(Mount.class);
        HstSite hstSite = createMock(HstSite.class);
        HstComponentsConfiguration componentsConfiguration = createMock(HstComponentsConfiguration.class);
        EasyMock.expect(mockPageComposerContextService.getEditingMount())
                .andReturn(editingMount).anyTimes();

        EasyMock.expect(editingMount.getHstSite())
                .andReturn(hstSite).anyTimes();

        EasyMock.expect(hstSite.getComponentsConfiguration())
                .andReturn(componentsConfiguration).anyTimes();

		final List<Parameter> parameters = Arrays.stream(DummyInfo.class.getMethods())
				.map(x -> x.getAnnotation(Parameter.class))
				.sorted((param1, param2) -> param1.name().compareTo(param2.name())).collect(Collectors.toList());

        final MockHstComponentConfiguration componentReference = new MockHstComponentConfiguration("id");
        componentReference.setComponentClassName(DummyComponent.class.getName());
        componentReference.setCanonicalStoredLocation("/");
        List<DynamicParameter> dynamicParameters = getDynamicParameters(parameters);
        componentReference.setDynamicComponentParameters(dynamicParameters);
        final HashMap<String, HstComponentConfiguration> componentConfigurations = new HashMap<>();
        componentConfigurations.put("/", componentReference);

        EasyMock.expect(componentsConfiguration.getComponentConfigurations())
                .andReturn(componentConfigurations).anyTimes();

        EasyMock.replay(mockPageComposerContextService);
        EasyMock.replay(editingMount);
        EasyMock.replay(hstSite);
        EasyMock.replay(componentsConfiguration);
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

    @Test
    public void testVariantCreation() throws RepositoryException, JAXBException, IOException, ServerErrorException {
        Node node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/services/ContainerItemComponentResourceTest-empty-component.xml");
        configureMockContainerItemComponent(node);

        MultivaluedMap<String, String> params;

        // 1. add a non annotated parameter for 'default someNonAnnotatedParameter = lux
        params = new MetadataMap<String, String>();
        params.add("parameterOne", "bar");
        params.add("someNonAnnotatedParameter", "lux");

        containerItemComponentService.updateVariant("", 0, params);

        assertTrue(node.hasProperty(HST_PARAMETERNAMES));
        assertTrue(node.hasProperty(HST_PARAMETERVALUES));
        // do not contain HST_PARAMETERNAMEPREFIXES
        assertTrue(!node.hasProperty(HST_PARAMETERNAMEPREFIXES));

        Set<String> variants =  this.containerItemComponentService.getVariants();
        assertTrue(variants.size() == 1);
        assertTrue(variants.contains("hippo-default"));

        // 2. create a new variant 'lux' : The creation of the variant should
        // pick up the explicitly defined parameters from 'default' that are ALSO annotated (thus parameterOne, and NOT someNonAnnotatedParameter) PLUS
        // the implicit parameters from the DummyInfo (parameterTwo but not parameterOne because already from 'default')

        containerItemComponentService.createVariant("newvar", 0);
        assertTrue(node.hasProperty(HST_PARAMETERNAMES));
        assertTrue(node.hasProperty(HST_PARAMETERVALUES));
        // now it must contain HST_PARAMETERNAMEPREFIXES
        assertTrue(node.hasProperty(HST_PARAMETERNAMEPREFIXES));

        variants = this.containerItemComponentService.getVariants();
        assertTrue(variants.size() == 2);
        assertTrue(variants.contains("hippo-default"));
        assertTrue(variants.contains("newvar"));

        final HstComponentParameters componentParameters = new HstComponentParameters(node, helper);
        assertTrue(componentParameters.hasParameter("newvar", "parameterOne"));
        assertEquals("bar", componentParameters.getValue("newvar", "parameterOne"));
        assertTrue(componentParameters.hasParameter("newvar", "parameterTwo"));
        // from  @Parameter(name = "parameterTwo", required = true, defaultValue = "test")
        assertEquals("test", componentParameters.getValue("newvar", "parameterTwo"));
        assertFalse(componentParameters.hasParameter("newvar", "someNonAnnotatedParameter"));

        // 3. try to remove the new variant
        containerItemComponentService.deleteVariant("newvar", 0);
        variants = this.containerItemComponentService.getVariants();
        assertTrue(variants.size() == 1);
        assertTrue(variants.contains("hippo-default"));
    }

    @Test(expected = ClientException.class)
    public void default_variant_should_not_be_deleted() throws RepositoryException, IOException, JAXBException {
        Node node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/services/ContainerItemComponentResourceTest-empty-component.xml");
        configureMockContainerItemComponent(node);

        containerItemComponentService.deleteVariant("hippo-default", 0);
        fail("Default variant should not be possible to be removed");
    }
}
