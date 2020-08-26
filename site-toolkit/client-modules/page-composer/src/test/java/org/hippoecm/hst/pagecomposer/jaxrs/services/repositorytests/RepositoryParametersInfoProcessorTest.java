/*
 *  Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.DynamicFieldGroup;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.mock.configuration.components.MockHstComponentConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractPageComposerTest;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PropertyRepresentationFactory;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentPropertyRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.property.SwitchTemplatePropertyRepresentationFactory;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;
import org.hippoecm.hst.platform.configuration.components.DynamicComponentParameter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.hippoecm.hst.core.container.ContainerConstants.DEFAULT_PARAMETER_PREFIX;
import static org.hippoecm.hst.pagecomposer.jaxrs.model.ParametersInfoProcessor.getPopulatedProperties;
import static org.junit.Assert.assertEquals;

public class RepositoryParametersInfoProcessorTest extends AbstractPageComposerTest {

    @FieldGroupList({
            @FieldGroup(value = { "englishOnly" }, titleKey = "englishOnlyGroup"),
            @FieldGroup(value = { "both", "dropdown" }, titleKey = "bothGroup")
    })

    private interface TestParameterInfo {
        @Parameter(name = "englishOnly")
        @SuppressWarnings("unused")
        String getEnglishOnly();

        @Parameter(name = "both")
        @SuppressWarnings("unused")
        String getBoth();

        @Parameter(name = "dropdown")
        @DropDownList(value = { "englishOnly", "both" } )
        @SuppressWarnings("unused")
        String getDropdown();
    }

    @ParametersInfo(type=TestParameterInfo.class)
    private static class TestComponent {
    }

    private static final String configurationRoot =
            "/hippo:configuration/hippo:translations/hippo:hst/componentparameters/org/hippoecm/hst/pagecomposer/jaxrs/services/repositorytests/RepositoryParametersInfoProcessorTest$TestParameterInfo";
    private static final String[] contents = {
            "/hippo:configuration/hippo:translations/hippo:hst/componentparameters/org/hippoecm/hst/pagecomposer", "hipposys:resourcebundles",
            "/hippo:configuration/hippo:translations/hippo:hst/componentparameters/org/hippoecm/hst/pagecomposer/jaxrs", "hipposys:resourcebundles",
            "/hippo:configuration/hippo:translations/hippo:hst/componentparameters/org/hippoecm/hst/pagecomposer/jaxrs/services", "hipposys:resourcebundles",
            "/hippo:configuration/hippo:translations/hippo:hst/componentparameters/org/hippoecm/hst/pagecomposer/jaxrs/services/repositorytests", "hipposys:resourcebundles",
            configurationRoot,          "hipposys:resourcebundles",
            configurationRoot + "/en",  "hipposys:resourcebundle",
                "englishOnlyGroup",     "englishOnlyGroup EN",
                "bothGroup",            "bothGroup EN",
                "englishOnly",          "englishOnly EN",
                "both",                 "both EN",
                "dropdown#englishOnly", "dropdown#englishOnly EN",
                "dropdown#both",        "dropdown#both EN",
            configurationRoot + "/nl",  "hipposys:resourcebundle",
                "bothGroup",            "bothGroup NL",
                "both",                 "both NL",
                "dropdown#both",        "dropdown#both NL"
    };

    protected MockNode containerItemNode;
    protected MockHstComponentConfiguration mockHstComponentConfiguration;
    protected ContainerItemHelper helper;

    @Before
    public void importTranslations() throws Exception {
        RepositoryTestCase.build(contents, session);
        session.save();

        // allow the resource bundle to be loaded by the LocalizationModule
        Thread.sleep(1000);

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

    @After
    public void removeTranslations() throws Exception {
        session.getNode("/hippo:configuration/hippo:translations/hippo:hst/componentparameters/org/hippoecm/hst/pagecomposer/jaxrs/services/repositorytests").remove();
        session.save();
    }

    protected List<PropertyRepresentationFactory> propertyPresentationFactories= new ArrayList<>();
    {
        propertyPresentationFactories.add(new SwitchTemplatePropertyRepresentationFactory());
    }


    @Test
    public void test_repository_translation_fallback() throws IOException, RepositoryException {
        final String contentPath = "/content/documents/testchannel";
        final ParametersInfo parameterInfo = TestComponent.class.getAnnotation(ParametersInfo.class);

        final MockHstComponentConfiguration component = createComponentReference(TestComponent.class);
        List<ContainerItemComponentPropertyRepresentation> properties = getPopulatedProperties(parameterInfo.type(),
                new Locale("en"),
                contentPath,
                DEFAULT_PARAMETER_PREFIX,
                containerItemNode,
                component,
                helper,
                propertyPresentationFactories);

        assertEquals("englishOnly EN", getProperty(properties, "englishOnly").getLabel());
        assertEquals("both EN", getProperty(properties, "both").getLabel());
        assertEquals("englishOnlyGroup EN", getProperty(properties, "englishOnly").getGroupLabel());
        assertEquals("bothGroup EN", getProperty(properties, "both").getGroupLabel());
        assertEquals("dropdown#englishOnly EN", getDropDownDisplayValue(properties, "dropdown", "englishOnly"));
        assertEquals("dropdown#both EN", getDropDownDisplayValue(properties, "dropdown", "both"));

        properties = getPopulatedProperties(parameterInfo.type(),
                new Locale("nl"),
                contentPath,
                DEFAULT_PARAMETER_PREFIX,
                containerItemNode,
                component,
                helper,
                propertyPresentationFactories
                );
        assertEquals("englishOnly EN", getProperty(properties, "englishOnly").getLabel());
        assertEquals("both NL", getProperty(properties, "both").getLabel());
        assertEquals("englishOnlyGroup EN", getProperty(properties, "englishOnly").getGroupLabel());
        assertEquals("bothGroup NL", getProperty(properties, "both").getGroupLabel());
        assertEquals("dropdown#englishOnly EN", getDropDownDisplayValue(properties, "dropdown", "englishOnly"));
        assertEquals("dropdown#both NL", getDropDownDisplayValue(properties, "dropdown", "both"));
    }

    protected MockHstComponentConfiguration createComponentReference(Class<?> componentClass) {
        final ParametersInfo parameterInfo = componentClass.getAnnotation(ParametersInfo.class);
        final MockHstComponentConfiguration componentReference = new MockHstComponentConfiguration("id");
        componentReference.setComponentClassName(componentClass.getName());
        componentReference.setCanonicalStoredLocation("/");
        final Stream<Method> stream = Arrays.stream(parameterInfo.type().getMethods());
        final Map<Parameter, Method> paramMap = stream.collect(Collectors.toMap(x -> x.getAnnotation(Parameter.class), a -> a));
        final List<DynamicParameter> dynamicParameters = getDynamicParameters(paramMap);
        componentReference.setDynamicComponentParameters(dynamicParameters);
        final FieldGroupList fieldGroupList = parameterInfo.type().getAnnotation(FieldGroupList.class);
        Arrays.stream(fieldGroupList.value()).forEach(group -> componentReference.getFieldGroups().add(new DynamicFieldGroup(group)));
        return componentReference;
    }

    private List<DynamicParameter> getDynamicParameters(Map<Parameter, Method> parameters) {
        return parameters.entrySet().stream()
                .map(e -> new DynamicComponentParameter(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    private ContainerItemComponentPropertyRepresentation getProperty(
            final List<ContainerItemComponentPropertyRepresentation> properties, final String propertyName)
    {
        for (final ContainerItemComponentPropertyRepresentation property : properties) {
            if (property.getName().equals(propertyName)) {
                return property;
            }
        }
        throw new IllegalArgumentException("Property " + propertyName + "not found");
    }

    private String getDropDownDisplayValue(
            final List<ContainerItemComponentPropertyRepresentation> properties, final String propertyName, final String valueName)
    {
        final ContainerItemComponentPropertyRepresentation property = getProperty(properties, propertyName);
        final String[] values = property.getDropDownListValues();

        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(valueName)) {
                return property.getDropDownListDisplayValues()[i];
            }
        }
        throw new IllegalArgumentException("Value " + valueName + "not found");
    }

}
