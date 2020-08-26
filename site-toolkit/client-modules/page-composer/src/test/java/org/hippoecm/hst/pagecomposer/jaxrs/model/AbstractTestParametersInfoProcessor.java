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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.DynamicFieldGroup;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.parameters.FieldGroupList;
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
        return createComponentReference(Bar.class);
    }

    protected MockHstComponentConfiguration createComponentReference(Class<?> componentClass) {
        final ParametersInfo parameterInfo = componentClass.getAnnotation(ParametersInfo.class);
        final MockHstComponentConfiguration componentReference = new MockHstComponentConfiguration("id");
        componentReference.setComponentClassName(componentClass.getName());
        componentReference.setCanonicalStoredLocation("/");
        componentReference.setRenderPath(ContainerConstants.FREEMARKER_WEB_FILE_TEMPLATE_PROTOCOL + "/ftl/main/layout.ftl");
        final Stream<Method> stream = Arrays.stream(parameterInfo.type().getMethods());
        final Map<Parameter, Method> paramMap = stream.collect(Collectors.toMap(x -> x.getAnnotation(Parameter.class), a -> a));
        final List<DynamicParameter> dynamicParameters = getDynamicParameters(paramMap);
        componentReference.setDynamicComponentParameters(dynamicParameters);
        FieldGroupList fieldGroupList = parameterInfo.type().getAnnotation(FieldGroupList.class);
        if (fieldGroupList != null) {
            Arrays.stream(fieldGroupList.value()).forEach(group -> componentReference.getFieldGroups().add(new DynamicFieldGroup(group)));
        }
        return componentReference;
    }

    private List<DynamicParameter> getDynamicParameters(Map<Parameter, Method> parameters) {
        return parameters.entrySet().stream()
                .map(e -> new DynamicComponentParameter(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

}
