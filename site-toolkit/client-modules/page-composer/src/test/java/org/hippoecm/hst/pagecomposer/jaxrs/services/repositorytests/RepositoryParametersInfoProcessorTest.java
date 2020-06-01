/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
import java.util.Locale;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractPageComposerTest;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentPropertyRepresentation;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.hippoecm.hst.pagecomposer.jaxrs.model.ParametersInfoProcessor.getProperties;
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
            "/hippo:configuration/hippo:translations/hippo:hst/componentparameters/org/hippoecm", "hipposys:resourcebundles",
            "/hippo:configuration/hippo:translations/hippo:hst/componentparameters/org/hippoecm/hst", "hipposys:resourcebundles",
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

    @Before
    public void importTranslations() throws Exception {
        RepositoryTestCase.build(contents, session);
        session.save();

        // allow the resource bundle to be loaded by the LocalizationModule
        Thread.sleep(1000);
    }

    @After
    public void removeTranslations() throws Exception {
        session.getNode("/hippo:configuration/hippo:translations/hippo:hst").remove();
        session.save();
    }

    @Test
    public void test_repository_translation_fallback() throws IOException, RepositoryException {
        final String contentPath = "/content/documents/testchannel";
        final ParametersInfo parameterInfo = TestComponent.class.getAnnotation(ParametersInfo.class);

        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(parameterInfo, new Locale("en"), contentPath);
        assertEquals("englishOnly EN", getProperty(properties, "englishOnly").getLabel());
        assertEquals("both EN", getProperty(properties, "both").getLabel());
        assertEquals("englishOnlyGroup EN", getProperty(properties, "englishOnly").getGroupLabel());
        assertEquals("bothGroup EN", getProperty(properties, "both").getGroupLabel());
        assertEquals("dropdown#englishOnly EN", getDropDownDisplayValue(properties, "dropdown", "englishOnly"));
        assertEquals("dropdown#both EN", getDropDownDisplayValue(properties, "dropdown", "both"));

        properties = getProperties(parameterInfo, new Locale("nl"), contentPath);
        assertEquals("englishOnly EN", getProperty(properties, "englishOnly").getLabel());
        assertEquals("both NL", getProperty(properties, "both").getLabel());
        assertEquals("englishOnlyGroup EN", getProperty(properties, "englishOnly").getGroupLabel());
        assertEquals("bothGroup NL", getProperty(properties, "both").getGroupLabel());
        assertEquals("dropdown#englishOnly EN", getDropDownDisplayValue(properties, "dropdown", "englishOnly"));
        assertEquals("dropdown#both NL", getDropDownDisplayValue(properties, "dropdown", "both"));
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
