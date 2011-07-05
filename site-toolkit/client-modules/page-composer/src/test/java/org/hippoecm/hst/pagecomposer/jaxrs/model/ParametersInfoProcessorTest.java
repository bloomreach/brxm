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

import java.util.List;

import org.hippoecm.hst.configuration.components.Color;
import org.hippoecm.hst.configuration.components.DocumentLink;
import org.hippoecm.hst.configuration.components.Parameter;
import org.hippoecm.hst.configuration.components.ParameterType;
import org.hippoecm.hst.configuration.components.ParametersInfo;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class ParametersInfoProcessorTest {

    static interface OldstyleInterface {
        @Parameter(name="documentLocation", typeHint = ParameterType.DOCUMENT, docLocation = "/content", docType = "hst:testdocument")
        String getDocumentLocation();

        @Parameter(name="color", typeHint = ParameterType.COLOR)
        String getColor();
    }
    @ParametersInfo(type=OldstyleInterface.class)
    static class OldstyleContainer {
    }

    @Test
    public void backwardsCompatibleProcessing() {
        ParametersInfo parameterInfo = OldstyleContainer.class.getAnnotation(ParametersInfo.class);
        List<Property> properties = ParametersInfoProcessor.getProperties(parameterInfo);
        assertEquals(2, properties.size());

        Property docLocProperty = properties.get(0);
        assertEquals("/content", docLocProperty.getDocLocation());
        assertEquals("combo", docLocProperty.getType());
        assertEquals("hst:testdocument", docLocProperty.getDocType());

        Property colorProperty = properties.get(1);
        assertEquals("colorfield", colorProperty.getType());
    }

    static interface NewstyleInterface {
        @Parameter(name="documentLocation")
        @DocumentLink(docLocation = "/content", docType = "hst:testdocument")
        String getDocumentLocation();

        @Parameter(name="color")
        @Color
        String getColor();
    }
    @ParametersInfo(type=NewstyleInterface.class)
    static class NewstyleContainer {
    }

    @Test
    public void additionalAnnotationBasedProcessing() {
        ParametersInfo parameterInfo = NewstyleContainer.class.getAnnotation(ParametersInfo.class);
        List<Property> properties = ParametersInfoProcessor.getProperties(parameterInfo);
        assertEquals(2, properties.size());

        Property docLocProperty = properties.get(0);
        assertEquals("/content", docLocProperty.getDocLocation());
        assertEquals("combo", docLocProperty.getType());
        assertEquals("hst:testdocument", docLocProperty.getDocType());

        Property colorProperty = properties.get(1);
        assertEquals("colorfield", colorProperty.getType());
    }

}
