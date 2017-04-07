/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.processor.html.serialize;

import org.htmlcleaner.CleanerProperties;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HtmlSerializerFactoryTest {

    @Test
    public void testAllSerializersNormalizeCharacterReferences() {
        final CleanerProperties cleanerProperties = createCleanerProperties();
        final String input = "' \" &apos; &quot;";
        final String expected = "' \" ' \"";

        assertEquals(expected, HtmlSerializerFactory.create(HtmlSerializer.SIMPLE,
                                                       cleanerProperties).getAsString(input));

        assertEquals(expected, HtmlSerializerFactory.create(HtmlSerializer.COMPACT,
                                                       cleanerProperties).getAsString(input));

        assertEquals(expected, HtmlSerializerFactory.create(HtmlSerializer.PRETTY,
                                                       cleanerProperties).getAsString(input));

    }

    private CleanerProperties createCleanerProperties() {
        final CleanerProperties properties = new CleanerProperties();
        properties.setOmitHtmlEnvelope(true);
        properties.setOmitXmlDeclaration(true);
        return properties;
    }
}
