/*
 *  Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Locale;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.junit.Test;

import static org.hippoecm.hst.core.container.ContainerConstants.DEFAULT_PARAMETER_PREFIX;
import static org.hippoecm.hst.pagecomposer.jaxrs.model.ParametersInfoProcessor.getPopulatedProperties;
import static org.hippoecm.hst.pagecomposer.jaxrs.model.ParametersInfoProcessor.getProperties;
import static org.junit.Assert.assertEquals;

public class ParametersInfoProcessorPopulatedPropertiesTest extends AbstractTestParametersInfoProcessor {

    @Test
    public void get_unpopulated_properties() {
        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(parameterInfo, null, null);
        assertEquals(1, properties.size());
        final ContainerItemComponentPropertyRepresentation prop = properties.get(0);
        assertEquals("bar", prop.getName());
        assertEquals("Bar", prop.getLabel());
        assertEquals("Bar Hint", prop.getHint());
        assertEquals(0, prop.getValue().length());

        {
            List<ContainerItemComponentPropertyRepresentation> propertiesFr = getProperties(parameterInfo, Locale.FRENCH, null);
            final ContainerItemComponentPropertyRepresentation propFr = propertiesFr.get(0);
            assertEquals("Bar (fr)", propFr.getLabel());
            assertEquals("BAR hint (fr)", propFr.getHint());
        }
        {
            List<ContainerItemComponentPropertyRepresentation> propertiesFrFR = getProperties(parameterInfo, Locale.FRANCE, null);
            final ContainerItemComponentPropertyRepresentation propFrFR = propertiesFrFR.get(0);
            assertEquals("Bar (fr_FR)", propFrFR.getLabel());
            assertEquals("Bar Hint (fr_FR)", propFrFR.getHint());
        }
    }

    @Test
    public void get_populated_defaultPrefix_properties() throws RepositoryException {

        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"bar"});
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"barValue"});

        List<ContainerItemComponentPropertyRepresentation> properties =
                getPopulatedProperties(parameterInfo, null, null, DEFAULT_PARAMETER_PREFIX,
                        containerItemNode, helper, propertyPresentationFactories);
        assertEquals(1, properties.size());
        final ContainerItemComponentPropertyRepresentation prop = properties.get(0);
        assertEquals("bar", prop.getName());
        assertEquals("barValue",prop.getValue());

    }

    @Test
    public void get_populated_prefixed_properties() throws RepositoryException {
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"bar"});
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"barValue"});

        {
            List<ContainerItemComponentPropertyRepresentation> prefixedProperties =
                    getPopulatedProperties(parameterInfo, null, null, "some-prefix",
                            containerItemNode, helper, propertyPresentationFactories);

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
                    getPopulatedProperties(parameterInfo, null, null, "some-prefix",
                            containerItemNode, helper, propertyPresentationFactories);


            final ContainerItemComponentPropertyRepresentation prefixedProp = prefixedProperties.get(0);
            assertEquals("bar", prefixedProp.getName());
            assertEquals("barrrrrValue", prefixedProp.getValue());
        }
    }


}
