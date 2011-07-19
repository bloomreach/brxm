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
import java.util.List;

import org.hippoecm.hst.configuration.components.Parameter;
import org.hippoecm.hst.configuration.components.ParametersInfo;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class OldParametersInfoProcessorTest {

    static interface OldstyleInterface {
        @Parameter(name="color", typeHint = ComponentWrapper.ParameterType.COLOR)
        String getColor();

        @Parameter(name="documentLocation", typeHint = ComponentWrapper.ParameterType.DOCUMENT, docLocation = "/content", docType = "hst:testdocument")
        String getDocumentLocation();

        @Parameter(name="start", typeHint = ComponentWrapper.ParameterType.DATE)
        String getDate();
    }
    @ParametersInfo(type=OldstyleInterface.class)
    static class OldstyleContainer {
    }

    @Test
    public void backwardsCompatibleProcessing() {
        ParametersInfo parameterInfo = OldstyleContainer.class.getAnnotation(ParametersInfo.class);
        List<Property> properties = OldParametersInfoProcessor.getProperties(parameterInfo);
        assertEquals(3, properties.size());

        // sort properties alphabetically by name to ensure a deterministic order
        Collections.sort(properties, new PropertyComparator());

        Property colorProperty = properties.get(0);
        assertEquals("colorfield", colorProperty.getType());

        Property docLocProperty = properties.get(1);
        assertEquals("/content", docLocProperty.getDocLocation());
        assertEquals("combo", docLocProperty.getType());
        assertEquals("hst:testdocument", docLocProperty.getDocType());

        Property startProperty = properties.get(2);
        assertEquals("datefield", startProperty.getType());
    }

    private static class PropertyComparator implements Comparator<Property> {

        @Override
        public int compare(final Property p1, final Property p2) {
            return p1.getName().compareTo(p2.getName());
        }

    }

}
