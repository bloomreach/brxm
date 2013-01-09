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

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.hippoecm.hst.core.parameters.Color;
import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class ParametersInfoProcessorTest {

    @ParametersInfo(type=NewstyleInterface.class)
    static class NewstyleContainer {
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

}
