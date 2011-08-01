/*
 *  Copyright 2011 Hippo.
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
import org.hippoecm.hst.core.parameters.ImageSetLink;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ComponentWrapper.Property;
import org.hippoecm.hst.pagecomposer.jaxrs.model.utils.ParametersInfoProcessor;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class ParametersInfoProcessorTest {

    static interface NewstyleInterface {
        @Parameter(name="color", defaultValue = "blue")
        @Color
        String getColor();

        @Parameter(name="documentLocation")
        @DocumentLink(docLocation = "/content", docType = "hst:testdocument")
        String getDocumentLocation();

        @Parameter(name="image", defaultValue = "/content/gallery/default.png")
        @ImageSetLink
        String getImage();

        @Parameter(name="start")
        Date getStart();
    }
    @ParametersInfo(type=NewstyleInterface.class)
    static class NewstyleContainer {
    }

    @Test
    public void additionalAnnotationBasedProcessing() {
        ParametersInfo parameterInfo = NewstyleContainer.class.getAnnotation(ParametersInfo.class);
        List<Property> properties = ParametersInfoProcessor.getProperties(parameterInfo);
        assertEquals(4, properties.size());

        // sort properties alphabetically by name to ensure a deterministic order
        Collections.sort(properties, new PropertyComparator());

        Property colorProperty = properties.get(0);
        assertEquals("colorfield", colorProperty.getType());
        assertEquals("blue", colorProperty.getDefaultValue());
        assertEquals("blue", colorProperty.getValue());

        Property docLocProperty = properties.get(1);
        assertEquals("/content", docLocProperty.getDocLocation());
        assertEquals("combo", docLocProperty.getType());
        assertEquals("hst:testdocument", docLocProperty.getDocType());

        Property imageProperty = properties.get(2);
        assertEquals("textfield", imageProperty.getType());
        assertEquals("/content/gallery/default.png", imageProperty.getDefaultValue());
        assertEquals("/content/gallery/default.png", imageProperty.getValue());

        Property startProperty = properties.get(3);
        assertEquals("datefield", startProperty.getType());
    }

    private static class PropertyComparator implements Comparator<Property> {

        @Override
        public int compare(final Property p1, final Property p2) {
            return p1.getName().compareTo(p2.getName());
        }

    }

}
