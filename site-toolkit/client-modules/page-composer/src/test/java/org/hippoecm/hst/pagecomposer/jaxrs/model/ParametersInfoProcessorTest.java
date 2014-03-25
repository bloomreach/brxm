/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import org.hippoecm.hst.core.parameters.Color;
import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class ParametersInfoProcessorTest {

    @ParametersInfo(type=NewstyleInterface.class)
    static class NewstyleContainer {
    }

    @ParametersInfo(type=NewstyleSubInterface.class)
    static class NewstyleSubContainer {
    }

    ParametersInfoProcessor processor = new ParametersInfoProcessor();

    @Test
    public void additionalAnnotationBasedProcessing() {
        final String currentMountCanonicalContentPath = "/content/documents/testchannel";

        ParametersInfo parameterInfo = NewstyleContainer.class.getAnnotation(ParametersInfo.class);
        List<ContainerItemComponentPropertyRepresentation> properties = processor.getProperties(parameterInfo, null, currentMountCanonicalContentPath);
        assertEquals(16, properties.size());

        // sort properties alphabetically by name to ensure a deterministic order
        Collections.sort(properties, new PropertyComparator());

        ContainerItemComponentPropertyRepresentation colorProperty = properties.get(0);
        assertEquals("colorfield", colorProperty.getType());
        assertEquals("blue", colorProperty.getDefaultValue());

        ContainerItemComponentPropertyRepresentation docLocProperty = properties.get(1);
        assertEquals("/content", docLocProperty.getDocLocation());
        assertEquals("documentcombobox", docLocProperty.getType());
        assertEquals("hst:testdocument", docLocProperty.getDocType());

        ContainerItemComponentPropertyRepresentation imageProperty = properties.get(2);
        assertEquals("textfield", imageProperty.getType());
        assertEquals("/content/gallery/default.png", imageProperty.getDefaultValue());

        ContainerItemComponentPropertyRepresentation dateProperty = properties.get(3);
        assertEquals("datefield", dateProperty.getType());

        ContainerItemComponentPropertyRepresentation booleanProperty = properties.get(4);
        assertEquals("checkbox", booleanProperty.getType());

        ContainerItemComponentPropertyRepresentation booleanClassProperty = properties.get(5);
        assertEquals("checkbox", booleanClassProperty.getType());

        ContainerItemComponentPropertyRepresentation intProperty = properties.get(6);
        assertEquals("numberfield", intProperty.getType());

        ContainerItemComponentPropertyRepresentation integerClassProperty = properties.get(7);
        assertEquals("numberfield", integerClassProperty.getType());

        ContainerItemComponentPropertyRepresentation longProperty = properties.get(8);
        assertEquals("numberfield", longProperty.getType());

        ContainerItemComponentPropertyRepresentation longClassProperty = properties.get(9);
        assertEquals("numberfield", longClassProperty.getType());

        ContainerItemComponentPropertyRepresentation shortProperty = properties.get(10);
        assertEquals("numberfield", shortProperty.getType());

        ContainerItemComponentPropertyRepresentation shortClassProperty = properties.get(11);
        assertEquals("numberfield", shortClassProperty.getType());

        ContainerItemComponentPropertyRepresentation jcrPathProperty = properties.get(12);
        assertEquals("linkpicker", jcrPathProperty.getType());
        assertEquals("cms-pickers/documents", jcrPathProperty.getPickerConfiguration());
        assertEquals("/content/documents/subdir/foo", jcrPathProperty.getPickerInitialPath());
        assertEquals(currentMountCanonicalContentPath, jcrPathProperty.getPickerRootPath());

        ContainerItemComponentPropertyRepresentation relativeJcrPathProperty = properties.get(13);
        assertEquals("linkpicker", relativeJcrPathProperty.getType());
        assertEquals("cms-pickers/mycustompicker", relativeJcrPathProperty.getPickerConfiguration());
        assertEquals("subdir/foo", relativeJcrPathProperty.getPickerInitialPath());
        assertEquals(currentMountCanonicalContentPath, relativeJcrPathProperty.getPickerRootPath());

        final ContainerItemComponentPropertyRepresentation dropDownProperty = properties.get(14);
        assertEquals("combo", dropDownProperty.getType());
        final String values[] = dropDownProperty.getDropDownListValues();
        assertEquals(values.length, 3);
        assertEquals("value1", values[0]);
        assertEquals("value2", values[1]);
        assertEquals("value3", values[2]);

        String[] displayValues = dropDownProperty.getDropDownListDisplayValues();
        assertEquals(3, displayValues.length);

        final ContainerItemComponentPropertyRepresentation hideInChannelManagerProperty = properties.get(15);
        assertTrue(hideInChannelManagerProperty.isHiddenInChannelManager());

    }

    @Test
    @Ignore // Hudson doesn't load the NewstyleInterface resource bundle
    public void dropDownResourceBundleProcessing() {
        final String currentMountCanonicalContentPath = "/content/documents/testchannel";

        ParametersInfo parameterInfo = NewstyleContainer.class.getAnnotation(ParametersInfo.class);
        List<ContainerItemComponentPropertyRepresentation> properties = processor.getProperties(parameterInfo, null, currentMountCanonicalContentPath);
        assertEquals(16, properties.size());

        // sort properties alphabetically by name to ensure a deterministic order
        Collections.sort(properties, new PropertyComparator());

        final ContainerItemComponentPropertyRepresentation dropDownProperty = properties.get(14);
        assertEquals("combo", dropDownProperty.getType());

        String[] displayValues = dropDownProperty.getDropDownListDisplayValues();
        assertEquals(3, displayValues.length);
        assertEquals("Value 1", displayValues[0]);
        assertEquals("Value 2", displayValues[1]);
    }


    private static class PropertyComparator implements Comparator<ContainerItemComponentPropertyRepresentation> {

        @Override
        public int compare(final ContainerItemComponentPropertyRepresentation p1, final ContainerItemComponentPropertyRepresentation p2) {
            return p1.getName().compareTo(p2.getName());
        }

    }


    @Test
    public void valuesAreLocalized() {
        final String currentMountCanonicalContentPath = "/content/documents/testchannel";

        ParametersInfo parameterInfo = NewstyleContainer.class.getAnnotation(ParametersInfo.class);

        List<ContainerItemComponentPropertyRepresentation> properties = processor.getProperties(parameterInfo, new Locale("nl"), currentMountCanonicalContentPath);
        assertEquals(16, properties.size());

        // sort properties alphabetically by name to ensure a deterministic order
        Collections.sort(properties, new PropertyComparator());

        final ContainerItemComponentPropertyRepresentation representation = properties.get(14);
        final String[] displayValues = representation.getDropDownListDisplayValues();
        assertEquals(3, displayValues.length);
        assertEquals("Waarde 1", displayValues[0]);
        assertEquals("Waarde 2", displayValues[1]);
        assertEquals("value3", displayValues[2]);
    }


    static interface a extends b, c {
    }

    static interface b extends d, e {
    }

    static interface d {
    }

    static interface e extends h {
    }

    static interface h {
    }

    static interface c extends f, g {
    }

    static interface f {
    }
    static interface g {
    }

    /**
     * above is an interface hierarchy as follows:
     *
     *                a
     *              /   \
     *             b     c
     *           / \    / \
     *          d  e   f  g
     *              \
     *              h
     *
     *  the BREADTH FIRST method should thus return a,b,c,d,e,f,gh
     */
    @Test
    public void assertBreadthFirstInterfaceHierarchy() {
        final ParametersInfoProcessor parametersInfoProcessor = new ParametersInfoProcessor();
        final List<Class<?>> hierarchy = parametersInfoProcessor.getBreadthFirstInterfaceHierarchy(a.class);
        assertEquals(hierarchy.get(0), a.class);
        assertEquals(hierarchy.get(1), b.class);
        assertEquals(hierarchy.get(2), c.class);
        assertEquals(hierarchy.get(3), d.class);
        assertEquals(hierarchy.get(4), e.class);
        assertEquals(hierarchy.get(5), f.class);
        assertEquals(hierarchy.get(6), g.class);
        assertEquals(hierarchy.get(7), h.class);
    }

    @Test
    public void valuesAreInheritedFromSuperTypesAndLocalized() {
        final String currentMountCanonicalContentPath = "/content/documents/testchannel";

        ParametersInfo parameterInfo = NewstyleSubContainer.class.getAnnotation(ParametersInfo.class);

        List<ContainerItemComponentPropertyRepresentation> properties = processor.getProperties(parameterInfo, new Locale("nl"), currentMountCanonicalContentPath);

        // NewstyleSubContainer has 2 properties and NewstyleContainer which is extends has 16 properties, BUT
        // NewstyleSubContainer overrides one property of NewstyleContainer, hence total should be 16 + 1
        assertEquals(17, properties.size());

        // sort properties alphabetically by name to ensure a deterministic order
        Collections.sort(properties, new PropertyComparator());

        final ContainerItemComponentPropertyRepresentation representation1 = properties.get(0);
        // present in NewstyleSubInterface_nl.properties and in NewstyleInterface.properties, however
        // value from NewstyleSubInterface_nl.properties should be shown
        assertEquals(representation1.getLabel(), "zwart sub");

        final ContainerItemComponentPropertyRepresentation representation2 = properties.get(1);
        // missing in NewstyleSubInterface_nl.properties and missing in NewstyleInterface.properties
        // but in NewstyleInterface there is a displayname, namely:
        // @Parameter(name = "01-documentLocation", displayName = "Document Location")
        assertEquals(representation2.getLabel(), "Document Location");


        final ContainerItemComponentPropertyRepresentation representation3 = properties.get(2);
        // missing in NewstyleSubInterface_nl.properties but defined in NewstyleInterface.properties
        // @Parameter(name = "02-image", defaultValue = "/content/gallery/default.png")
        assertEquals(representation3.getLabel(), "Plaatje");


        final ContainerItemComponentPropertyRepresentation representation14 = properties.get(14);
        // NewstyleSubInterface_nl.properties does not have dropdown values but NewstyleInterface.properties
        // has them, so should be taken from there
        final String[] displayValues = representation14.getDropDownListDisplayValues();
        assertEquals(3, displayValues.length);
        assertEquals("Waarde 1", displayValues[0]);
        assertEquals("Waarde 2", displayValues[1]);
        assertEquals("value3", displayValues[2]);


        final ContainerItemComponentPropertyRepresentation representation15 = properties.get(16);
        // Present in NewstyleSubInterface_nl.properties
        // @Parameter(name = "16-subboolean")
        assertEquals(representation15.getLabel(),"Sub Boolean NL");
    }



    /**
     * Below, we have two broken combinations: 
     * @Color annotation is not allowed to return a int
     * @DocumentLink is not allowed to return a Date
     */
    static interface InvalidReturnTypeAnnotationCombinationInterface {
        @Parameter(name="00-color", defaultValue = "blue")
        @Color
        int getColor();

        @Parameter(name="01-documentLocation")
        @DocumentLink(docLocation = "/content", docType = "hst:testdocument")
        Date getDocumentLocation();
    }

    @ParametersInfo(type=InvalidReturnTypeAnnotationCombinationInterface.class)
    static class InvalidReturnTypeAnnotationCombination {
    
    }
    
    @Test
    public void testInvalidReturnTypeAnnotationCombination() {
        ParametersInfo parameterInfo = InvalidReturnTypeAnnotationCombination.class.getAnnotation(ParametersInfo.class);
        // the getProperties below are expected to log some warnings
        List<ContainerItemComponentPropertyRepresentation> properties = processor.getProperties(parameterInfo, null, "");
        assertEquals(2, properties.size());

        // sort properties alphabetically by name to ensure a deterministic order
        Collections.sort(properties, new PropertyComparator());

        // Since the @Color is not compatible with returnType int and @DocumentLink not with returnType Date, 
        // we expext that ParameterType#getType(...) defaults back to getType 'numberfield' for 00-color
        // and to datefield for 01-documentLocation
        ContainerItemComponentPropertyRepresentation colorProperty = properties.get(0);
        assertEquals("numberfield", colorProperty.getType());
        
        ContainerItemComponentPropertyRepresentation docLocProperty = properties.get(1);
        assertEquals("datefield", docLocProperty.getType());

    }

    @ParametersInfo(type=FieldGroupInterface.class)
    static class FieldGroupComponent {
    }

    @Test
    public void fieldGroupListGroupsParameters() {
        ParametersInfo parameterInfo = FieldGroupComponent.class.getAnnotation(ParametersInfo.class);
        List<ContainerItemComponentPropertyRepresentation> properties = processor.getProperties(parameterInfo, null, "");
        assertEquals("number of properties", 3, properties.size());
        assertNameAndGroupLabel(properties.get(0), "three", "Group1");
        assertNameAndGroupLabel(properties.get(1), "one", "Group1");
        assertNameAndGroupLabel(properties.get(2), "two", "Group2");
    }

    @FieldGroupList({})
    static interface EmptyFieldGroupListInterface {
        @Parameter(name = "one")
        String getOne();
    }

    @ParametersInfo(type=EmptyFieldGroupListInterface.class)
    static class EmptyFieldGroupListComponent {
    }

    @Test
    public void emptyFieldGroupListIncludesAllParameters() {
        ParametersInfo parameterInfo = EmptyFieldGroupListComponent.class.getAnnotation(ParametersInfo.class);
        List<ContainerItemComponentPropertyRepresentation> properties = processor.getProperties(parameterInfo, null, "");
        assertEquals("number of properties", 1, properties.size());
        assertNameAndGroupLabel(properties.get(0), "one", null);
    }

    @FieldGroupList({
            @FieldGroup({"two", "one"})
    })
    static interface FieldGroupWithoutTitleInterface {
        @Parameter(name = "one")
        String getOne();

        @Parameter(name = "two")
        String getTwo();
    }

    @ParametersInfo(type=FieldGroupWithoutTitleInterface.class)
    static class FieldGroupWithoutTitleComponent {
    }

    @Test
    public void fieldGroupWithoutTitleUsesEmptyTitle() {
        ParametersInfo parameterInfo = FieldGroupWithoutTitleComponent.class.getAnnotation(ParametersInfo.class);
        List<ContainerItemComponentPropertyRepresentation> properties = processor.getProperties(parameterInfo, null, "");
        assertEquals("number of properties", 2, properties.size());
        assertNameAndGroupLabel(properties.get(0), "two", "");
        assertNameAndGroupLabel(properties.get(1), "one", "");
    }

    @FieldGroupList({
            @FieldGroup(
                    titleKey = "group",
                    value = {"parameter"}
            )
    })
    static interface FieldGroupWithUntranslatedTitleInterface {
        @Parameter(name = "parameter")
        String getParameter();
    }

    @ParametersInfo(type=FieldGroupWithUntranslatedTitleInterface.class)
    static class FieldGroupWithUntranslatedTitleComponent {
    }

    @Test
    public void fieldGroupWithUntranslatedTitleUsesKeyAsTitle() {
        ParametersInfo parameterInfo = FieldGroupWithUntranslatedTitleComponent.class.getAnnotation(ParametersInfo.class);
        List<ContainerItemComponentPropertyRepresentation> properties = processor.getProperties(parameterInfo, null, "");
        assertEquals("number of properties", 1, properties.size());
        assertNameAndGroupLabel(properties.get(0), "parameter", "group");
    }

    @FieldGroupList({
            @FieldGroup({"nosuchparameter"})
    })
    static interface FieldGroupWithUnknownParameterInterface {
        @Parameter(name = "parameter")
        String getParameter();
    }

    @ParametersInfo(type=FieldGroupWithUnknownParameterInterface.class)
    static class FieldGroupWithUnknownParameterComponent {
    }

    @Test
    public void unknownFieldGroupParameterIsIgnored() {
        ParametersInfo parameterInfo = FieldGroupWithUnknownParameterComponent.class.getAnnotation(ParametersInfo.class);
        List<ContainerItemComponentPropertyRepresentation> properties = processor.getProperties(parameterInfo, null, "");
        assertEquals("number of properties", 1, properties.size());
        assertNameAndGroupLabel(properties.get(0), "parameter", null);
    }

    @FieldGroupList({
            @FieldGroup(titleKey = "group1",
                    value = {"parameter"}
            ),
            @FieldGroup(titleKey = "group2",
                    value = {"parameter"}
            )
    })
    static interface FieldGroupWithDuplicateParameterInterface {
        @Parameter(name = "parameter")
        String getParameter();
    }

    @ParametersInfo(type=FieldGroupWithDuplicateParameterInterface.class)
    static class FieldGroupWithDuplicateParameterComponent {
    }

    @Test
    public void duplicateFieldGroupParameterBelongsToFirstGroup() {
        ParametersInfo parameterInfo = FieldGroupWithDuplicateParameterComponent.class.getAnnotation(ParametersInfo.class);
        List<ContainerItemComponentPropertyRepresentation> properties = processor.getProperties(parameterInfo, null, "");
        assertEquals("number of properties", 1, properties.size());
        assertNameAndGroupLabel(properties.get(0), "parameter", "group1");
    }

    @FieldGroupList({
            @FieldGroup(
                    titleKey = "group",
                    value = {"one", "two"}
            )
    })
    static interface FieldGroupWithSubsetOfParametersInterface {
        @Parameter(name = "one")
        String getOne();

        @Parameter(name = "two")
        String getTwo();

        @Parameter(name = "three")
        String getThree();
    }

    @ParametersInfo(type=FieldGroupWithSubsetOfParametersInterface.class)
    static class FieldGroupWithSubsetOfParametersComponent {
    }

    @Test
    public void fieldGroupWithSubsetOfParametersIncludesAllOtherParametersInSeparateLastGroup() {
        ParametersInfo parameterInfo = FieldGroupWithSubsetOfParametersComponent.class.getAnnotation(ParametersInfo.class);
        List<ContainerItemComponentPropertyRepresentation> properties = processor.getProperties(parameterInfo, null, "");
        assertEquals("number of properties", 3, properties.size());
        assertNameAndGroupLabel(properties.get(0), "one", "group");
        assertNameAndGroupLabel(properties.get(1), "two", "group");
        assertNameAndGroupLabel(properties.get(2), "three", null);
    }

    @FieldGroupList({
            @FieldGroup(
                    titleKey = "group-d1",
                    value = {"d1"}
            ),
            @FieldGroup(
                titleKey = "group-d3",
                value = {"d3"}
            )
    })
    static interface FieldGroupInheritedInterfaceD {
        @Parameter(name = "d1")
        String getD1();

        @Parameter(name = "d2")
        String getD2();

        @Parameter(name = "d3")
        String getD3();

        @Parameter(name = "d4")
        String getD4();
    }

    static interface FieldGroupInheritedInterfaceC {
        @Parameter(name = "c1")
        String getC1();

        @Parameter(name = "c2")
        String getC2();
    }

    @FieldGroupList({
            @FieldGroup(
                    titleKey = "group-b1-d2",
                    value = {"b1", "d2"}
            ),
            @FieldGroup(
                    titleKey = "group-b2-d3",
                    value = {"b2", "d3"}
            )
    })
    static interface FieldGroupInheritedInterfaceB extends FieldGroupInheritedInterfaceD {
        @Parameter(name = "b1")
        String getB1();

        @Parameter(name = "b2")
        String getB2();

        @Parameter(name = "b3")
        String getB3();
    }

    @FieldGroupList({
            @FieldGroup(
                    titleKey = "group-a1-c2-b3-d1",
                    value = {"a1", "c2", "b3", "d1"}
            ),
            @FieldGroup(
                    titleKey = "group-a3",
                    value = {"a3"}
            )

    })
    static interface FieldGroupInheritedInterfaceA extends FieldGroupInheritedInterfaceB, FieldGroupInheritedInterfaceC {
        @Parameter(name = "a1")
        String getA1();

        @Parameter(name = "a2")
        String getA2();

        @Parameter(name = "a3")
        String getA3();
    }

    @ParametersInfo(type=FieldGroupInheritedInterfaceA.class)
    static class FieldGroupInheritedComponent {
    }

    @Test
    public void fieldGroupsAreInherited() {
        ParametersInfo parameterInfo = FieldGroupInheritedComponent.class.getAnnotation(ParametersInfo.class);
        List<ContainerItemComponentPropertyRepresentation> properties = processor.getProperties(parameterInfo, null, "");
        assertEquals("number of properties", 12, properties.size());
        assertNameAndGroupLabel(properties.get(0), "a1", "group-a1-c2-b3-d1");
        assertNameAndGroupLabel(properties.get(1), "c2", "group-a1-c2-b3-d1");
        assertNameAndGroupLabel(properties.get(2), "b3", "group-a1-c2-b3-d1");
        assertNameAndGroupLabel(properties.get(3), "d1", "group-a1-c2-b3-d1");
        assertNameAndGroupLabel(properties.get(4), "a3", "group-a3");
        assertNameAndGroupLabel(properties.get(5), "b1", "group-b1-d2");
        assertNameAndGroupLabel(properties.get(6), "d2", "group-b1-d2");
        assertNameAndGroupLabel(properties.get(7), "b2", "group-b2-d3");
        assertNameAndGroupLabel(properties.get(8), "d3", "group-b2-d3");
        assertContains(properties, 9, "a2", "c1", "d4");
    }


    @FieldGroupList({
            @FieldGroup(
                    // same titleKey as FieldGroupInheritedInterfaceB
                    titleKey = "group-b1-d2",
                    value = {"a1"}
            ),
            @FieldGroup(
                    titleKey = "group-b2-d3",
                    value = {"a2", "a3"}
            )
    })
    static interface FieldGroupInheritedMergingGroupInterface extends FieldGroupInheritedInterfaceB, FieldGroupInheritedInterfaceC {
        @Parameter(name = "a1")
        String getA1();

        @Parameter(name = "a2")
        String getA2();

        @Parameter(name = "a3")
        String getA3();
    }

    @ParametersInfo(type=FieldGroupInheritedMergingGroupInterface.class)
    static class InheritedFieldGroupsAreMergedComponent {
    }

    @Test
    public void inheritedFieldGroupsAreMerged() {
        ParametersInfo parameterInfo = InheritedFieldGroupsAreMergedComponent.class.getAnnotation(ParametersInfo.class);
        List<ContainerItemComponentPropertyRepresentation> properties = processor.getProperties(parameterInfo, null, "");
        assertEquals("number of properties", 12, properties.size());
        assertNameAndGroupLabel(properties.get(0), "a1", "group-b1-d2");
        assertNameAndGroupLabel(properties.get(1), "b1", "group-b1-d2");
        assertNameAndGroupLabel(properties.get(2), "d2", "group-b1-d2");
        assertNameAndGroupLabel(properties.get(3), "a2", "group-b2-d3");
        assertNameAndGroupLabel(properties.get(4), "a3", "group-b2-d3");
        assertNameAndGroupLabel(properties.get(5), "b2", "group-b2-d3");
        assertNameAndGroupLabel(properties.get(6), "d3", "group-b2-d3");
        assertNameAndGroupLabel(properties.get(7), "d1", "group-d1");
        assertContains(properties, 8, "b3", "d4", "c1", "c2");
    }

    @FieldGroupList({
            @FieldGroup(
                    // same titleKey as FieldGroupInheritedInterfaceB
                    titleKey = "group-b1-d2",
                    value = {}
            ),
            @FieldGroup(
                    titleKey = "group-b2-d3",
                    value = {"a2", "a3"}
            )
    })
    static interface FieldEmptyGroupInheritedMergingGroupInterface extends FieldGroupInheritedInterfaceB, FieldGroupInheritedInterfaceC {
        @Parameter(name = "a1")
        String getA1();

        @Parameter(name = "a2")
        String getA2();

        @Parameter(name = "a3")
        String getA3();
    }

    @ParametersInfo(type=FieldEmptyGroupInheritedMergingGroupInterface.class)
    static class InheritedEmptyFieldGroupsAreMergedComponent {
    }

    @Test
    public void inheritedEmptyFieldGroupsAreMerged() {
        ParametersInfo parameterInfo = InheritedEmptyFieldGroupsAreMergedComponent.class.getAnnotation(ParametersInfo.class);
        List<ContainerItemComponentPropertyRepresentation> properties = processor.getProperties(parameterInfo, null, "");
        assertEquals("number of properties", 12, properties.size());
        assertNameAndGroupLabel(properties.get(0), "b1", "group-b1-d2");
        assertNameAndGroupLabel(properties.get(1), "d2", "group-b1-d2");
        assertNameAndGroupLabel(properties.get(2), "a2", "group-b2-d3");
        assertNameAndGroupLabel(properties.get(3), "a3", "group-b2-d3");
        assertNameAndGroupLabel(properties.get(4), "b2", "group-b2-d3");
        assertNameAndGroupLabel(properties.get(5), "d3", "group-b2-d3");
        assertNameAndGroupLabel(properties.get(6), "d1", "group-d1");
        assertContains(properties, 7,"a1", "b3", "d4", "c1", "c2");
    }

    private void assertContains(List<ContainerItemComponentPropertyRepresentation> properties, int fromIndex, String... propertyNames) {
        List<String> expectedUngroupedPropertyNames = new ArrayList<String>(Arrays.asList(propertyNames));
        ListIterator<ContainerItemComponentPropertyRepresentation> iterator = properties.listIterator(fromIndex);
        while (iterator.hasNext()) {
            final String propertyName = iterator.next().getName();
            assertTrue("expected ungrouped property '" + propertyName + "'", expectedUngroupedPropertyNames.remove(propertyName));
        }
    }

    private void assertNameAndGroupLabel(ContainerItemComponentPropertyRepresentation property, String name, String groupLabel) {
        assertEquals("name", name, property.getName());
        assertEquals("group label", groupLabel, property.getGroupLabel());
    }

}
