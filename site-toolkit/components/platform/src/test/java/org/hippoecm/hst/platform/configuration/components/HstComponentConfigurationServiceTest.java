/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.components;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.groovy.util.Maps;
import org.hippoecm.hst.configuration.components.DynamicFieldGroup;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.provider.ValueProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_FIELD_GROUPS;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HstComponentConfigurationServiceTest {

    @Test
    public void testFieldGroups() {
        final Map<String, List<String>> fieldGroupsDefs = Maps.of(
                "fieldgroup1", Lists.newArrayList("g1param1", "", "g1param2", "g1param3"),
                "fieldgroup2", Lists.newArrayList("g2param1", null, "g2param2"),
                "fieldgroup3", Lists.newArrayList("g3param1")
        );

        final HstNode mockedNode = setupComponentNode(fieldGroupsDefs);

        final HstComponentConfigurationService component = new HstComponentConfigurationService();

        component.readJcrFieldGroups(mockedNode);

        assertEquals(3, component.getFieldGroups().size());

        final List<DynamicFieldGroup> fieldGroups = component.getFieldGroups();
        assertEquals("fieldgroup1", fieldGroups.get(0).getTitleKey());
        assertEquals("fieldgroup2", fieldGroups.get(1).getTitleKey());
        assertEquals("fieldgroup3", fieldGroups.get(2).getTitleKey());

        assertEquals(ImmutableList.of("g1param1", "g1param2", "g1param3"), fieldGroups.get(0).getParameters());
        assertEquals(ImmutableList.of("g2param1", "g2param2"), fieldGroups.get(1).getParameters());
        assertEquals(ImmutableList.of("g3param1"), fieldGroups.get(2).getParameters());
    }

    @Test
    public void testDuplicatedGroupParameters() {
        final Map<String, List<String>> fieldGroupsDefs = Maps.of(
                "fieldgroup1", Lists.newArrayList("g1param1", "", "g1param2", "g1param3"),
                "fieldgroup2", Lists.newArrayList("g2param1", "g1param1", "g2param2"), //Duplicated parameter
                "fieldgroup3", Lists.newArrayList("g3param1", "g1param1") //Duplicated parameter
        );

        final HstNode mockedNode = setupComponentNode(fieldGroupsDefs);
        final HstComponentConfigurationService component = new HstComponentConfigurationService();
        component.readJcrFieldGroups(mockedNode);

        assertEquals(3, component.getFieldGroups().size());

        final List<DynamicFieldGroup> fieldGroups = component.getFieldGroups();
        assertEquals("fieldgroup1", fieldGroups.get(0).getTitleKey());
        assertEquals("fieldgroup2", fieldGroups.get(1).getTitleKey());
        assertEquals("fieldgroup3", fieldGroups.get(2).getTitleKey());

        assertEquals(ImmutableList.of("g1param1", "g1param2", "g1param3"), fieldGroups.get(0).getParameters());
        assertEquals(ImmutableList.of("g2param1", "g2param2"), fieldGroups.get(1).getParameters());
        assertEquals(ImmutableList.of("g3param1"), fieldGroups.get(2).getParameters());
    }

    @Test
    public void testNullOrEmptyGroupParameters() {
        final Map<String, List<String>> fieldGroupsDefs = Maps.of(
                "fieldgroup1", Lists.newArrayList("g1param1", "", "g1param2", "g1param3"),
                "fieldgroup2", Lists.newArrayList("g2param1", null, "g2param2")
        );

        final HstNode mockedNode = setupComponentNode(fieldGroupsDefs);
        final HstComponentConfigurationService component = new HstComponentConfigurationService();
        component.readJcrFieldGroups(mockedNode);

        assertEquals(2, component.getFieldGroups().size());

        final List<DynamicFieldGroup> fieldGroups = component.getFieldGroups();
        assertEquals("fieldgroup1", fieldGroups.get(0).getTitleKey());
        assertEquals("fieldgroup2", fieldGroups.get(1).getTitleKey());

        assertEquals(ImmutableList.of("g1param1", "g1param2", "g1param3"), fieldGroups.get(0).getParameters());
        assertEquals(ImmutableList.of("g2param1", "g2param2"), fieldGroups.get(1).getParameters());
    }

    @Test
    public void testDuplicateFieldGroups() {
        final String[] groups = new String[] {"fieldgroup1", "fieldgroup2", "fieldgroup1"};

        final HstNode mockedNode = mock(HstNode.class);
        final ValueProvider mockedValueProvider = mock(ValueProvider.class);
        when(mockedNode.getValueProvider()).thenReturn(mockedValueProvider);
        when(mockedValueProvider.getStrings(COMPONENT_PROPERTY_FIELD_GROUPS)).thenReturn(groups);

        final HstComponentConfigurationService component = new HstComponentConfigurationService();
        component.readJcrFieldGroups(mockedNode);
        assertEquals(2, component.getFieldGroups().size());

        final List<DynamicFieldGroup> fieldGroups = component.getFieldGroups();
        assertEquals("fieldgroup1", fieldGroups.get(0).getTitleKey());
        assertEquals("fieldgroup2", fieldGroups.get(1).getTitleKey());
    }

    @Test
    public void testEmptyGroups() {

        final String[] groups = new String[] {"fieldgroup1", null, "", "fieldgroup4"};

        final HstNode mockedNode = mock(HstNode.class);
        final ValueProvider mockedValueProvider = mock(ValueProvider.class);
        when(mockedNode.getValueProvider()).thenReturn(mockedValueProvider);
        when(mockedValueProvider.getStrings(COMPONENT_PROPERTY_FIELD_GROUPS)).thenReturn(groups);
        final HstComponentConfigurationService component = new HstComponentConfigurationService();
        component.readJcrFieldGroups(mockedNode);
        assertEquals(2, component.getFieldGroups().size());
        final List<DynamicFieldGroup> fieldGroups = component.getFieldGroups();
        assertEquals("fieldgroup1", fieldGroups.get(0).getTitleKey());
        assertEquals("fieldgroup4", fieldGroups.get(1).getTitleKey());
    }

    @FieldGroupList({
            @FieldGroup(titleKey = "group1", value = {"parameter1"}),
            @FieldGroup(titleKey = "group2", value = {"parameter2"})
    })
    interface ComponentParameters {
        @Parameter(name = "parameter1")
        String getParameter1();

        @Parameter(name = "parameter2")
        String getParameter2();
    }

    @ParametersInfo(type=ComponentParameters.class)
    static class Component {
    }

    @Test
    public void testAnnotationAndJcrGroupMerging() {
        final Map<String, List<String>> jcrFieldGroupsDefs = Maps.of(
                "fieldgroup1", Lists.newArrayList("g1param1", "", "g1param2", "g1param3"),
                "fieldgroup2", Lists.newArrayList("g3param1", "g1param1", "parameter2"),
                "group1", Lists.newArrayList("parameter1", "g1param1")
        );

        final HstNode mockedNode = setupComponentNode(jcrFieldGroupsDefs);
        final HstComponentConfigurationService component = new HstComponentConfigurationService();
        component.readJcrFieldGroups(mockedNode);

        final ParametersInfo parameterInfo = Component.class.getAnnotation(ParametersInfo.class);
        component.populateFieldGroups(parameterInfo);

        final List<DynamicFieldGroup> fieldGroups = component.getFieldGroups();

        assertEquals(4, fieldGroups.size());
        assertEquals(ImmutableList.of("group2", "fieldgroup1", "fieldgroup2", "group1"),
                fieldGroups.stream().map(DynamicFieldGroup::getTitleKey).collect(toList()));
        assertEquals(ImmutableList.of("parameter1"), fieldGroups.get(3).getParameters());
    }

    @NotNull
    private HstNode setupComponentNode(Map<String, List<String>> fieldGroupsDefs) {
        String[] groupsArr = fieldGroupsDefs.keySet().toArray(new String[fieldGroupsDefs.keySet().size()]);

        final HstNode mockedNode = mock(HstNode.class);
        final ValueProvider mockedValueProvider = mock(ValueProvider.class);
        when(mockedNode.getValueProvider()).thenReturn(mockedValueProvider);
        when(mockedValueProvider.getStrings(COMPONENT_PROPERTY_FIELD_GROUPS)).thenReturn(groupsArr);

        fieldGroupsDefs.keySet().forEach(key -> {
            final List<String> paramNames = fieldGroupsDefs.get(key);
            when(mockedValueProvider.getStrings(COMPONENT_PROPERTY_FIELD_GROUPS + "." + key))
                    .thenReturn(paramNames.toArray(new String[paramNames.size()]));
        });
        return mockedNode;
    }
}