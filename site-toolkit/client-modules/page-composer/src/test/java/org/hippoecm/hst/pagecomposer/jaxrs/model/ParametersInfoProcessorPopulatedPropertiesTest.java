/*
 *  Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.jcr.RepositoryException;

import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.components.ParameterValueType;
import org.hippoecm.hst.mock.configuration.components.MockHstComponentConfiguration;
import org.hippoecm.hst.resourcebundle.ResourceBundleUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.hippoecm.hst.core.container.ContainerConstants.DEFAULT_PARAMETER_PREFIX;
import static org.hippoecm.hst.pagecomposer.jaxrs.model.ParametersInfoProcessor.getPopulatedProperties;
import static org.junit.Assert.assertEquals;
import static org.easymock.EasyMock.expect;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest(ResourceBundleUtils.class)
public class ParametersInfoProcessorPopulatedPropertiesTest extends AbstractTestParametersInfoProcessor {

    @Test
    public void get_unpopulated_properties() throws RepositoryException {
        final MockHstComponentConfiguration componentReference = createComponentReference();
        final List<ContainerItemComponentPropertyRepresentation> properties = getPopulatedProperties(parameterInfo.type(), null, null, DEFAULT_PARAMETER_PREFIX,
                containerItemNode, helper, propertyPresentationFactories, componentReference);

        assertEquals(1, properties.size());
        final ContainerItemComponentPropertyRepresentation prop = properties.get(0);
        assertEquals("bar", prop.getName());
        assertEquals("Bar", prop.getLabel());
        assertEquals("Bar Hint", prop.getHint());
        assertEquals(0, prop.getValue().length());

        {
            List<ContainerItemComponentPropertyRepresentation> propertiesFr = getPopulatedProperties(parameterInfo.type(),
                    Locale.FRENCH, null, DEFAULT_PARAMETER_PREFIX,
                    containerItemNode, helper, propertyPresentationFactories, componentReference);
            final ContainerItemComponentPropertyRepresentation propFr = propertiesFr.get(0);
            assertEquals("Bar (fr)", propFr.getLabel());
            assertEquals("BAR hint (fr)", propFr.getHint());
        }
        {
            List<ContainerItemComponentPropertyRepresentation> propertiesFrFR = getPopulatedProperties(parameterInfo.type(),
                    Locale.FRANCE, null, DEFAULT_PARAMETER_PREFIX,
                    containerItemNode, helper, propertyPresentationFactories, componentReference);

            final ContainerItemComponentPropertyRepresentation propFrFR = propertiesFrFR.get(0);
            assertEquals("Bar (fr_FR)", propFrFR.getLabel());
            assertEquals("Bar Hint (fr_FR)", propFrFR.getHint());
        }
    }

    @Test
    public void get_populated_defaultPrefix_properties() throws RepositoryException {

        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"bar"});
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"barValue"});

        final MockHstComponentConfiguration componentReference = createComponentReference();

        List<ContainerItemComponentPropertyRepresentation> properties =
                getPopulatedProperties(parameterInfo.type(), null, null, DEFAULT_PARAMETER_PREFIX,
                        containerItemNode, helper, propertyPresentationFactories, componentReference);

        assertEquals(1, properties.size());
        final ContainerItemComponentPropertyRepresentation prop = properties.get(0);
        assertEquals("bar", prop.getName());
        assertEquals("barValue",prop.getValue());

    }

    private void initDynamicParameterTranslation(final MockHstComponentConfiguration componentReference) {
        final ResourceBundle catalogitemResourcebundle = new ListResourceBundle() {
            protected Object[][] getContents() {
                return new Object[][] {
                    {"residualParameter", "residualParamLabel"},
                };
            }
        };

        PowerMock.mockStatic(ResourceBundleUtils.class);
        expect(ResourceBundleUtils.getBundle("hst:components.essentials-catalog.catalogitem", Locale.GERMAN, false)).andReturn(catalogitemResourcebundle);
        PowerMock.replayAll();

        final DynamicParameter dynamicParameter = EasyMock.createMock(DynamicParameter.class);
        expect(dynamicParameter.getName()).andReturn("residualParameter").anyTimes();
        expect(dynamicParameter.getDefaultValue()).andReturn("residualParamDefault");
        expect(dynamicParameter.getValueType()).andReturn(ParameterValueType.STRING).anyTimes();
        expect(dynamicParameter.isRequired()).andReturn(true);
        expect(dynamicParameter.isHideInChannelManager()).andReturn(false);
        expect(dynamicParameter.getComponentParameterConfig()).andReturn(null).anyTimes();
        EasyMock.replay(dynamicParameter);

        componentReference.getDynamicComponentParameters().add(dynamicParameter);

        componentReference.setComponentDefinition("hst:components/essentials-catalog/catalogitem");
    }

	@Test
    public void get_populated_properties_with_dynamic_parameter_translations() throws RepositoryException {
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"bar", "residualParameter"});
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"barValue", "residualParameterValue"});

        final MockHstComponentConfiguration componentReference = createComponentReference();
        initDynamicParameterTranslation(componentReference);

        List<ContainerItemComponentPropertyRepresentation> properties =
                getPopulatedProperties(parameterInfo.type(), Locale.GERMAN, null, DEFAULT_PARAMETER_PREFIX,
                        containerItemNode, helper, propertyPresentationFactories, componentReference);

        assertEquals(2, properties.size());
        final ContainerItemComponentPropertyRepresentation prop = properties.get(0);
        assertEquals("bar", prop.getName());
        assertEquals("barValue",prop.getValue());

        final ContainerItemComponentPropertyRepresentation prop2 = properties.get(1);
        assertEquals("residualParameter", prop2.getName());
        assertEquals("residualParameterValue",prop2.getValue());
        assertEquals("residualParamLabel",prop2.getLabel());
    }

    @Test
    public void get_populated_prefixed_properties() throws RepositoryException {
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"bar"});
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"barValue"});

        final MockHstComponentConfiguration componentReference = createComponentReference();

        {
            List<ContainerItemComponentPropertyRepresentation> prefixedProperties =
                    getPopulatedProperties(parameterInfo.type(), null, null, "some-prefix",
                            containerItemNode, helper, propertyPresentationFactories, componentReference);

            assertEquals(1, prefixedProperties.size());
            final ContainerItemComponentPropertyRepresentation prefixedProp = prefixedProperties.get(0);
            assertEquals("bar", prefixedProp.getName());
            assertEquals(0, prefixedProp.getValue().length());
        }

        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"bar", "bar"});
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"barValue", "barrrrrValue"});
        containerItemNode.setProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES, new String[]{"", "some-prefix"});

        {
            List<ContainerItemComponentPropertyRepresentation> prefixedProperties =
                    getPopulatedProperties(parameterInfo.type(), null, null, "some-prefix",
                            containerItemNode, helper, propertyPresentationFactories, componentReference);


            final ContainerItemComponentPropertyRepresentation prefixedProp = prefixedProperties.get(0);
            assertEquals("bar", prefixedProp.getName());
            assertEquals("barrrrrValue", prefixedProp.getValue());
        }
    }


}
