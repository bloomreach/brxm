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

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.hippoecm.hst.core.parameters.Color;
import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.ImageSetPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class ParametersInfoProcessorTest {

    static interface NewstyleInterface {
        @Parameter(name="00-color", defaultValue = "blue")
        @Color
        String getColor();

        @Parameter(name="01-documentLocation")
        @DocumentLink(docLocation = "/content", docType = "hst:testdocument")
        String getDocumentLocation();

        @Parameter(name="02-image", defaultValue = "/content/gallery/default.png")
        @ImageSetPath
        String getImage();

        @Parameter(name="03-date")
        Date getDate();

        @Parameter(name="04-boolean")
        boolean isBoolean();

        @Parameter(name="05-booleanClass")
        Boolean isBooleanClass();

        @Parameter(name="06-int")
        int getInt();

        @Parameter(name="07-integerClass")
        Integer getIntegerClass();

        @Parameter(name="08-long")
        long getLong();

        @Parameter(name="09-longClass")
        Long getLongClass();

        @Parameter(name="10-short")
        short getShort();

        @Parameter(name="11-shortClass")
        Short getShortClass();
    }
    @ParametersInfo(type=NewstyleInterface.class)
    static class NewstyleContainer {
    }

    @Test
    public void additionalAnnotationBasedProcessing() {
        ParametersInfo parameterInfo = NewstyleContainer.class.getAnnotation(ParametersInfo.class);
        List<ContainerItemComponentPropertyRepresentation> properties = ContainerItemComponentRepresentation.getProperties(parameterInfo, null);
        assertEquals(12, properties.size());

        // sort properties alphabetically by name to ensure a deterministic order
        Collections.sort(properties, new PropertyComparator());

        ContainerItemComponentPropertyRepresentation colorProperty = properties.get(0);
        assertEquals("colorfield", colorProperty.getType());
        assertEquals("blue", colorProperty.getDefaultValue());

        ContainerItemComponentPropertyRepresentation docLocProperty = properties.get(1);
        assertEquals("/content", docLocProperty.getDocLocation());
        assertEquals("combo", docLocProperty.getType());
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
    }

    private static class PropertyComparator implements Comparator<ContainerItemComponentPropertyRepresentation> {

        @Override
        public int compare(final ContainerItemComponentPropertyRepresentation p1, final ContainerItemComponentPropertyRepresentation p2) {
            return p1.getName().compareTo(p2.getName());
        }

    }

}
