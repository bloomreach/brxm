/*
 *  Copyright 2011-2012 Hippo.
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

import static junit.framework.Assert.assertEquals;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.hippoecm.hst.core.parameters.*;
import org.junit.Test;

public class ParametersInfoProcessorTest {

    @ParametersInfo(type=NewstyleInterface.class)
    static class NewstyleContainer {
    }

    @Test
    public void additionalAnnotationBasedProcessing() {
        final String currentMountCanonicalContentPath = "/content/documents/testchannel";

        ParametersInfo parameterInfo = NewstyleContainer.class.getAnnotation(ParametersInfo.class);
        List<ContainerItemComponentPropertyRepresentation> properties = ContainerItemComponentRepresentation.getProperties(parameterInfo, null, currentMountCanonicalContentPath);
        assertEquals(15, properties.size());

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
        assertEquals("Value 1", displayValues[0]);
        assertEquals("Value 2", displayValues[1]);
        // value 3 does not have a translation, so the display value should be the same as the underlying value
        assertEquals("value3", displayValues[2]);
    }

    private static class PropertyComparator implements Comparator<ContainerItemComponentPropertyRepresentation> {

        @Override
        public int compare(final ContainerItemComponentPropertyRepresentation p1, final ContainerItemComponentPropertyRepresentation p2) {
            return p1.getName().compareTo(p2.getName());
        }

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
        List<ContainerItemComponentPropertyRepresentation> properties = ContainerItemComponentRepresentation.getProperties(parameterInfo, null, "");
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
