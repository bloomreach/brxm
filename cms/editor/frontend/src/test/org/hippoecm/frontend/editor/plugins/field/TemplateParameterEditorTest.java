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

package org.hippoecm.frontend.editor.plugins.field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.PropertyType;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.PropertyDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TemplateParameterEditorTest {

    private static final String PROPERTY_NAME = "a-test-property-name";
    private static final String ADD_FIELD_LINK = "dummy-panel:form:tpe:properties:0:value:add";

    private WicketTester tester;
    private FormTester formTester;
    private IPluginConfig pluginConfig;

    private Map<Object, Object> propertyValues = new HashMap<>();

    @Before
    public void setUp(){
        tester = new HippoTester();
        pluginConfig = createMockPluginConfig();
    }

    @Test
    public void inputSingleValueShouldStoreToBackEnd(){

        final IClusterConfig cluster = createMockClusterConfig(Arrays.asList(PROPERTY_NAME), false);

        tester.startComponentInPage(new DummyDialog("dummy-panel", Model.of(pluginConfig), cluster, true));
        tester.assertComponent("dummy-panel:form:tpe", TemplateParameterEditor.class);

        formTester = tester.newFormTester("dummy-panel:form");
        formTester.setValue("tpe:properties:0:value:widget", "a test input value");
        formTester.submit();

        assertEquals("a test input value", propertyValues.get(PROPERTY_NAME));
    }

    @Test
    public void inputMultipleValuesShouldStoreToBackEnd(){
        final IClusterConfig cluster = createMockClusterConfig(Arrays.asList(PROPERTY_NAME), true);

        tester.startComponentInPage(new DummyDialog("dummy-panel", Model.of(pluginConfig), cluster, true));
        tester.assertComponent("dummy-panel:form:tpe", TemplateParameterEditor.class);

        formTester = tester.newFormTester("dummy-panel:form");

        //add two input fields
        tester.executeAjaxEvent(ADD_FIELD_LINK, "onclick");
        tester.executeAjaxEvent(ADD_FIELD_LINK, "onclick");

        tester.assertComponent("dummy-panel:form:tpe:properties:0:value:values:0:value", TextField.class);
        tester.assertComponent("dummy-panel:form:tpe:properties:0:value:values:1:value", TextField.class);

        // set two test values
        formTester.setValue("tpe:properties:0:value:values:0:value", "input-value-1");
        formTester.setValue("tpe:properties:0:value:values:1:value", "input-value-2");
        formTester.submit();

        String[] storedValues = (String[]) propertyValues.get(PROPERTY_NAME);
        assertNotNull(storedValues);
        assertTrue(storedValues.length == 2);
        assertEquals("input-value-1", storedValues[0]);
        assertEquals("input-value-2", storedValues[1]);
    }

    private IClusterConfig createMockClusterConfig(final List<String> propertyNames, final boolean isMultiple) {
        IClusterConfig clusterConfig = mock(IClusterConfig.class);
        final List<PropertyDescriptor> mockProperties = new ArrayList<>();

        propertyNames.forEach(name -> mockProperties.add(new PropertyDescriptor(name, PropertyType.STRING, isMultiple)));

        when(clusterConfig.getPropertyDescriptors()).thenReturn(mockProperties);
        return clusterConfig;
    }

    private IPluginConfig createMockPluginConfig() {
        IPluginConfig mockConfig = mock(IPluginConfig.class);

        when(mockConfig.getName()).thenReturn("mock-plugin-config");

        when(mockConfig.getStringArray(Mockito.any())).then(invocation -> {
            final Object[] args = invocation.getArguments();
            if (args != null && args.length == 1) {
                return propertyValues.get(args[0]);
            }
            return null;
        });

        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            if (args != null && args.length == 2) {
                propertyValues.put(args[0], args[1]);
            }
            return null;
        }).when(mockConfig).put(Mockito.any(), Mockito.any());

        return mockConfig;
    }
}