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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.mock.configuration.components.MockHstComponentConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PropertyRepresentationFactory;
import org.hippoecm.hst.pagecomposer.jaxrs.property.SwitchTemplatePropertyRepresentationFactory;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;
import org.hippoecm.hst.platform.configuration.components.DynamicComponentParameter;
import org.junit.Before;
import org.onehippo.repository.mock.MockNode;

public class AbstractTestParametersInfoProcessor {

    protected MockNode containerItemNode;
    protected MockHstComponentConfiguration mockHstComponentConfiguration;
    protected ContainerItemHelper helper;
    protected ParametersInfo parameterInfo = Bar.class.getAnnotation(ParametersInfo.class);
    protected List<PropertyRepresentationFactory> propertyPresentationFactories= new ArrayList<>();
    {
        propertyPresentationFactories.add(new SwitchTemplatePropertyRepresentationFactory());
    }

    @Before
    public void setup() throws RepositoryException {
        mockHstComponentConfiguration = new MockHstComponentConfiguration("pages/newsList");

        containerItemNode = MockNode.root().addNode("item", HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);

        final Map<String, HstComponentConfiguration> compConfigMocks = new HashMap<>();
        compConfigMocks.put(containerItemNode.getIdentifier(), mockHstComponentConfiguration);
        helper = new ContainerItemHelper() {
            @Override
            public HstComponentConfiguration getConfigObject(final String itemId) {
                return compConfigMocks.get(itemId);
            }
        };
    }

    @ParametersInfo(type = BarParameters.class)
    static class Bar {
    }

    /**
     * Creates a component reference populated using annotation based parameters of BarParameters
     */
    protected MockHstComponentConfiguration createComponentReference() {
        final MockHstComponentConfiguration componentReference = new MockHstComponentConfiguration("id");
        componentReference.setComponentClassName(Bar.class.getName());
        componentReference.setCanonicalStoredLocation("/");
        final List<Parameter> parameters = Arrays.stream(BarParameters.class.getMethods()).map(x -> x.getAnnotation(Parameter.class)).collect(Collectors.toList());
        final List<DynamicParameter> dynamicParameters = getDynamicParameters(parameters);
        componentReference.setDynamicComponentParameters(dynamicParameters);
        return componentReference;
    }

    private List<DynamicParameter> getDynamicParameters(List<Parameter> parameters) {
        final List<DynamicParameter> dynamicParameters = new ArrayList<>();
        for (Parameter parameter : parameters) {
            DynamicParameter dynamicParameter = new DynamicComponentParameter(parameter, "STRING");
            dynamicParameters.add(dynamicParameter);
        }
        return dynamicParameters;
    }

}
