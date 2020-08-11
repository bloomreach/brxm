/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.plugin.sdk.utils;

import java.io.InputStream;

import org.junit.Test;
import org.onehippo.cms7.essentials.ResourceModifyingTest;
import org.onehippo.cms7.essentials.plugin.sdk.utils.xml.XmlNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * @version "$Id$"
 */
public class XmlUtilsTest extends ResourceModifyingTest {

    @Test
    public void testParsingProperties() throws Exception {
        final InputStream resourceAsStream = getClass().getResourceAsStream("/test_document_type.xml");
        final XmlNode documentNode = XmlUtils.parseXml(resourceAsStream);
        assertNotNull(documentNode);
        assertEquals(4, documentNode.getTemplates().size());
    }
}
