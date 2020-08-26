/*
 *  Copyright 2011-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.mock.configuration.components.MockHstComponentConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PropertyRepresentationFactory;
import org.hippoecm.hst.pagecomposer.jaxrs.property.SwitchTemplatePropertyRepresentationFactory;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.util.AbstractHstComponentParameters;
import org.hippoecm.hst.platform.configuration.components.DynamicComponentParameter;
import org.hippoecm.hst.platform.configuration.components.HstComponentConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.hippoecm.hst.core.container.ContainerConstants.DEFAULT_PARAMETER_PREFIX;
import static org.hippoecm.hst.pagecomposer.jaxrs.model.ParametersInfoProcessor.getPopulatedProperties;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ParametersInfoProcessorTest {

    public static class TestHstComponentConfigurationService extends HstComponentConfigurationService {

        public TestHstComponentConfigurationService(String id) {
            this.id = id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void populateFieldGroups(ClassLoader websiteClassLoader) {
            super.populateFieldGroups(websiteClassLoader);
        }

        public void setComponentClassName(String componentClassName) {
            this.componentClassName = componentClassName;
        }

        public void setCanonicalStoredLocation(String location) {
            this.canonicalStoredLocation = location;
        }

        public void setDynamicComponentParameters(List<DynamicParameter> dynamicParameters) {
            this.hstDynamicComponentParameters = dynamicParameters;
        }
    }


    @ParametersInfo(type=NewstyleInterface.class)
    static class NewstyleContainer {
    }

    @ParametersInfo(type=NewstyleSubInterface.class)
    static class NewstyleSubContainer {
    }

    protected MockNode containerItemNode;
    protected MockHstComponentConfiguration mockHstComponentConfiguration;
    protected ContainerItemHelper helper;

    protected List<PropertyRepresentationFactory> propertyPresentationFactories= new ArrayList<>();
    {
        propertyPresentationFactories.add(new SwitchTemplatePropertyRepresentationFactory());
    }

    @Before
    public void setUp() throws Exception {
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

    protected HstComponentConfiguration createComponentReference(Class<?> componentClass) {

        final TestHstComponentConfigurationService componentReference = new TestHstComponentConfigurationService("id");
        componentReference.setComponentClassName(componentClass.getName());
        componentReference.setCanonicalStoredLocation("/");
        final ParametersInfo parameterInfo = componentClass.getAnnotation(ParametersInfo.class);
        componentReference.populateFieldGroups(this.getClass().getClassLoader());

        final Stream<Method> stream = Arrays.stream(parameterInfo.type().getMethods());
        final Map<Parameter, Method> paramMap = stream.collect(Collectors.toMap(x -> x.getAnnotation(Parameter.class), a -> a));
        final List<DynamicParameter> dynamicParameters = getDynamicParameters(paramMap);
        componentReference.setDynamicComponentParameters(dynamicParameters);

        return componentReference;
    }

    private List<DynamicParameter> getDynamicParameters(final Map<Parameter, Method> parameters) {
        return parameters.entrySet().stream()
                .map(e -> new DynamicComponentParameter(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    @Test
    public void additionalAnnotationBasedProcessing() throws RepositoryException {
        final String currentMountCanonicalContentPath = "/content/documents/testchannel";

        ParametersInfo parameterInfo = NewstyleContainer.class.getAnnotation(ParametersInfo.class);
        final HstComponentConfiguration component = createComponentReference(NewstyleContainer.class);
        List<ContainerItemComponentPropertyRepresentation> properties = getPopulatedProperties(parameterInfo.type(),
                null,
                currentMountCanonicalContentPath,
                DEFAULT_PARAMETER_PREFIX, containerItemNode, component, helper, propertyPresentationFactories);

        assertEquals(14, properties.size());

        // sort properties alphabetically by name to ensure a deterministic order
        Collections.sort(properties, new PropertyComparator());

        int counter = 0;
        ContainerItemComponentPropertyRepresentation imageProperty = properties.get(counter);
        assertEquals("textfield", imageProperty.getType());
        assertEquals("/content/gallery/default.png", imageProperty.getDefaultValue());

        ContainerItemComponentPropertyRepresentation dateProperty = properties.get(++counter);
        assertEquals("datefield", dateProperty.getType());

        ContainerItemComponentPropertyRepresentation booleanProperty = properties.get(++counter);
        assertEquals("checkbox", booleanProperty.getType());

        ContainerItemComponentPropertyRepresentation booleanClassProperty = properties.get(++counter);
        assertEquals("checkbox", booleanClassProperty.getType());

        ContainerItemComponentPropertyRepresentation intProperty = properties.get(++counter);
        assertEquals("numberfield", intProperty.getType());

        ContainerItemComponentPropertyRepresentation integerClassProperty = properties.get(++counter);
        assertEquals("numberfield", integerClassProperty.getType());

        ContainerItemComponentPropertyRepresentation longProperty = properties.get(++counter);
        assertEquals("numberfield", longProperty.getType());

        ContainerItemComponentPropertyRepresentation longClassProperty = properties.get(++counter);
        assertEquals("numberfield", longClassProperty.getType());

        ContainerItemComponentPropertyRepresentation shortProperty = properties.get(++counter);
        assertEquals("numberfield", shortProperty.getType());

        ContainerItemComponentPropertyRepresentation shortClassProperty = properties.get(++counter);
        assertEquals("numberfield", shortClassProperty.getType());

        ContainerItemComponentPropertyRepresentation jcrPathProperty = properties.get(++counter);
        assertEquals("linkpicker", jcrPathProperty.getType());
        assertEquals("cms-pickers/documents", jcrPathProperty.getPickerConfiguration());
        assertEquals("/content/documents/subdir/foo", jcrPathProperty.getPickerInitialPath());
        assertEquals(currentMountCanonicalContentPath, jcrPathProperty.getPickerRootPath());

        ContainerItemComponentPropertyRepresentation relativeJcrPathProperty = properties.get(++counter);
        assertEquals("linkpicker", relativeJcrPathProperty.getType());
        assertEquals("cms-pickers/mycustompicker", relativeJcrPathProperty.getPickerConfiguration());
        assertEquals("subdir/foo", relativeJcrPathProperty.getPickerInitialPath());
        assertEquals(currentMountCanonicalContentPath, relativeJcrPathProperty.getPickerRootPath());

        final ContainerItemComponentPropertyRepresentation dropDownProperty = properties.get(++counter);
        assertEquals("combo", dropDownProperty.getType());
        final String values[] = dropDownProperty.getDropDownListValues();
        assertEquals(values.length, 3);
        assertEquals("value1", values[0]);
        assertEquals("value2", values[1]);
        assertEquals("value3", values[2]);

        String[] displayValues = dropDownProperty.getDropDownListDisplayValues();
        assertEquals(3, displayValues.length);

        final ContainerItemComponentPropertyRepresentation hideInChannelManagerProperty = properties.get(++counter);
        assertTrue(hideInChannelManagerProperty.isHiddenInChannelManager());

    }

    List<ContainerItemComponentPropertyRepresentation> getProperties(final Class<?> componentClass, final Locale locale,
                                                                     final String contentPath) {
        try {
            final HstComponentConfiguration component = createComponentReference(componentClass);
            final ParametersInfo parametersInfo = componentClass.getAnnotation(ParametersInfo.class);
            return getPopulatedProperties(parametersInfo.type(),
                    locale,
                    contentPath,
                    DEFAULT_PARAMETER_PREFIX, containerItemNode, component, helper, propertyPresentationFactories);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void dropDownResourceBundleProcessing() throws RepositoryException {
        final String currentMountCanonicalContentPath = "/content/documents/testchannel";

        ParametersInfo parameterInfo = NewstyleContainer.class.getAnnotation(ParametersInfo.class);
        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(NewstyleContainer.class, null, currentMountCanonicalContentPath);
        assertEquals(14, properties.size());

        // sort properties alphabetically by name to ensure a deterministic order
        Collections.sort(properties, new PropertyComparator());

        final ContainerItemComponentPropertyRepresentation dropDownProperty = properties.get(12);
        assertEquals("combo", dropDownProperty.getType());

        String[] displayValues = dropDownProperty.getDropDownListDisplayValues();
        assertEquals(3, displayValues.length);
        assertEquals("Value 1", displayValues[0]);
        assertEquals("Value 2", displayValues[1]);
    }

    @ParametersInfo(type= TestValueListProviderInterface.class)
    private static class DropDownComponent {
    }

    @Test
    public void dropDownValueListProviderProcessing() {
        final String currentMountCanonicalContentPath = "/content/documents/testchannel";

        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(DropDownComponent.class, null,
                currentMountCanonicalContentPath);

        assertEquals(1, properties.size());
        final ContainerItemComponentPropertyRepresentation dropDownProperty = properties.get(0);

        String[] displayValues = dropDownProperty.getDropDownListDisplayValues();
        assertEquals(2, displayValues.length);
        assertEquals("Value One", displayValues[0]);
        assertEquals("Value Two", displayValues[1]);
    }
    
    @Test
    public void dropDownValueListProviderLocalizedProcessing() {
        final String currentMountCanonicalContentPath = "/content/documents/testchannel";

        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(DropDownComponent.class, Locale.FRENCH,
                currentMountCanonicalContentPath);

        assertEquals(1, properties.size());
        final ContainerItemComponentPropertyRepresentation dropDownProperty = properties.get(0);

        String[] displayValues = dropDownProperty.getDropDownListDisplayValues();
        assertEquals(2, displayValues.length);
        assertEquals("Valeur un", displayValues[0]);
        assertEquals("Valeur deux", displayValues[1]);
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

        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(NewstyleContainer.class, new Locale("nl"), currentMountCanonicalContentPath);
        assertEquals(14, properties.size());

        // sort properties alphabetically by name to ensure a deterministic order
        Collections.sort(properties, new PropertyComparator());

        final ContainerItemComponentPropertyRepresentation representation = properties.get(12);
        final String[] displayValues = representation.getDropDownListDisplayValues();
        assertEquals(3, displayValues.length);
        assertEquals("Waarde 1", displayValues[0]);
        assertEquals("Waarde 2", displayValues[1]);
        assertEquals("value3", displayValues[2]);
    }


    interface a extends b, c {
    }

    interface b extends d, e {
    }

    interface d {
    }

    interface e extends h {
    }

    interface h {
    }

    interface c extends f, g {
    }

    interface f {
    }
    interface g {
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
        final List<Class<?>> hierarchy = ParametersInfoProcessor.getBreadthFirstInterfaceHierarchy(a.class);
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

        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(NewstyleSubContainer.class, new Locale("nl"), currentMountCanonicalContentPath);

        // NewstyleSubContainer has 2 properties and NewstyleContainer which it extends has 14 properties, BUT
        // NewstyleSubContainer overrides one property of NewstyleContainer, hence total should be 14 + 1
        assertEquals(15, properties.size());

        // sort properties alphabetically by name to ensure a deterministic order
        Collections.sort(properties, new PropertyComparator());

        final ContainerItemComponentPropertyRepresentation representation0 = properties.get(0);
        // present in NewstyleSubInterface_nl.properties and in NewstyleInterface.properties, however
        // value from NewstyleSubInterface_nl.properties should be shown
        assertEquals(representation0.getLabel(), "Plaatje sub");

        final ContainerItemComponentPropertyRepresentation representation1 = properties.get(1);
        // missing in NewstyleSubInterface_nl.properties but defined in NewstyleInterface.properties
        // @Parameter(name = "02-date")
        assertEquals(representation1.getLabel(), "Date");

        final ContainerItemComponentPropertyRepresentation representation12 = properties.get(12);
        // NewstyleSubInterface_nl.properties does not have dropdown values but NewstyleInterface.properties
        // has them, so should be taken from there
        final String[] displayValues = representation12.getDropDownListDisplayValues();
        assertEquals(3, displayValues.length);
        assertEquals("Waarde 1", displayValues[0]);
        assertEquals("Waarde 2", displayValues[1]);
        assertEquals("value3", displayValues[2]);

        final ContainerItemComponentPropertyRepresentation representation14 = properties.get(14);
        // Present in NewstyleSubInterface_nl.properties
        // @Parameter(name = "15-subboolean")
        assertEquals(representation14.getLabel(),"Sub Boolean NL");
    }
    
    /**
     * Below, we have a broken situation: 
     * @DropDownList annotation is not allowed to return a int
     */
    interface InvalidReturnTypeAnnotationInterface {
        @Parameter(name="dropdown", defaultValue = "value1")
        @DropDownList(value = {"value1", "value2", "value3"})
        int getDropDown();
    }

    @ParametersInfo(type= InvalidReturnTypeAnnotationInterface.class)
    static class InvalidReturnTypeAnnotation {

    }

    @Test
    public void testInvalidReturnTypeAnnotation() {
        // the getProperties below are expected to log some warnings
        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(InvalidReturnTypeAnnotation.class, null, "");
        assertEquals(1, properties.size());
        // Since the @DropDownList is not compatible with returnType int
        // we expect that ParameterType#getType(...) defaults back to getType 'numberfield' for dropdown
        ContainerItemComponentPropertyRepresentation dropDownProperty = properties.get(0);
        assertEquals("numberfield", dropDownProperty.getType());
    }

    @ParametersInfo(type=FieldGroupInterface.class)
    static class FieldGroupComponent {
    }

    @Test
    public void fieldGroupListGroupsParameters() {
        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(FieldGroupComponent.class, null, "");
        assertEquals("number of properties", 3, properties.size());
        assertNameAndGroupLabel(properties.get(0), "three", "Group1");
        assertNameAndGroupLabel(properties.get(1), "one", "Group1");
        assertNameAndGroupLabel(properties.get(2), "two", "Group2");
    }

    @FieldGroupList({})
    interface EmptyFieldGroupListInterface {
        @Parameter(name = "one")
        String getOne();
    }

    @ParametersInfo(type=EmptyFieldGroupListInterface.class)
    static class EmptyFieldGroupListComponent {
    }

    @Test
    public void emptyFieldGroupListIncludesAllParameters() {
        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(EmptyFieldGroupListComponent.class, null, "");
        assertEquals("number of properties", 1, properties.size());
        assertNameAndGroupLabel(properties.get(0), "one", null);
    }

    @FieldGroupList({
            @FieldGroup({"two", "one"})
    })
    interface FieldGroupWithoutTitleInterface {
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
        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(FieldGroupWithoutTitleComponent.class, null, "");
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
    interface FieldGroupWithUntranslatedTitleInterface {
        @Parameter(name = "parameter")
        String getParameter();
    }

    @ParametersInfo(type=FieldGroupWithUntranslatedTitleInterface.class)
    static class FieldGroupWithUntranslatedTitleComponent {
    }

    @Test
    public void fieldGroupWithUntranslatedTitleUsesKeyAsTitle() {
        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(FieldGroupWithUntranslatedTitleComponent.class, null, "");
        assertEquals("number of properties", 1, properties.size());
        assertNameAndGroupLabel(properties.get(0), "parameter", "group");
    }

    @FieldGroupList({
            @FieldGroup({"nosuchparameter"})
    })
    interface FieldGroupWithUnknownParameterInterface {
        @Parameter(name = "parameter")
        String getParameter();
    }

    @ParametersInfo(type=FieldGroupWithUnknownParameterInterface.class)
    static class FieldGroupWithUnknownParameterComponent {
    }

    @Test
    public void unknownFieldGroupParameterIsIgnored() {
        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(FieldGroupWithUnknownParameterComponent.class, null, "");
        assertEquals("number of properties", 1, properties.size());
        assertNameAndGroupLabel(properties.get(0), "parameter", null);
    }

    @FieldGroupList({
            @FieldGroup(titleKey = "group1",
                    value = {"parameter", "parameter"}
            ),
            @FieldGroup(titleKey = "group2",
                    value = {"parameter"}
            )
    })
    interface FieldGroupWithDuplicateParameterInterface {
        @Parameter(name = "parameter")
        String getParameter();
    }

    @ParametersInfo(type=FieldGroupWithDuplicateParameterInterface.class)
    static class FieldGroupWithDuplicateParameterComponent {
    }

    @Test
    public void duplicateFieldGroupParameterBelongsToFirstGroup() {
        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(FieldGroupWithDuplicateParameterComponent.class, null, "");
        assertEquals("number of properties", 1, properties.size());
        assertNameAndGroupLabel(properties.get(0), "parameter", "group1");
    }

    @FieldGroupList({
            @FieldGroup(
                    titleKey = "group",
                    value = {"one", "two"}
            )
    })
    interface FieldGroupWithSubsetOfParametersInterface {
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
        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(FieldGroupWithSubsetOfParametersComponent.class, null, "");
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
    interface FieldGroupInheritedInterfaceD {
        @Parameter(name = "d1")
        String getD1();

        @Parameter(name = "d2")
        String getD2();

        @Parameter(name = "d3")
        String getD3();

        @Parameter(name = "d4")
        String getD4();
    }

    interface FieldGroupInheritedInterfaceC {
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
    interface FieldGroupInheritedInterfaceB extends FieldGroupInheritedInterfaceD {
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
    interface FieldGroupInheritedInterfaceA extends FieldGroupInheritedInterfaceB, FieldGroupInheritedInterfaceC {
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
        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(FieldGroupInheritedComponent.class, null, "");
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
    interface FieldGroupInheritedMergingGroupInterface extends FieldGroupInheritedInterfaceB, FieldGroupInheritedInterfaceC {
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
        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(InheritedFieldGroupsAreMergedComponent.class, null, "");
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
    interface FieldEmptyGroupInheritedMergingGroupInterface extends FieldGroupInheritedInterfaceB, FieldGroupInheritedInterfaceC {
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
        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(InheritedEmptyFieldGroupsAreMergedComponent.class, null, "");
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

    @Test
    public void assert_methods_setValueForX_are_not_removed_since_in_use_by_downstream_projects() {
        try {
            ParametersInfoProcessor.class.getMethod("setValueForProperties", List.class, String.class, AbstractHstComponentParameters.class);
            ParametersInfoProcessor.class.getMethod("setValueForProperty", ContainerItemComponentPropertyRepresentation.class, String.class, AbstractHstComponentParameters.class);
        } catch (NoSuchMethodException e) {
            fail(String.format("Although the method is not used in HST, it is used by downstream projects and is not allowed to be " +
                    "removed : %s", e.toString()));
        }
    }

}
