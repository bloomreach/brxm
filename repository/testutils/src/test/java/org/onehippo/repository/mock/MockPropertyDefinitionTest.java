/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.jcr.Property;
import javax.jcr.PropertyType;

import org.junit.Test;

public class MockPropertyDefinitionTest {

    @Test
    public void testMandatoryProperties() throws Exception {
        Property jcrBaseVersionProp = new MockProperty("jcr:baseVersion", PropertyType.REFERENCE);
        assertTrue(jcrBaseVersionProp.getDefinition().isMandatory());

        Property jcrDataProp = new MockProperty("jcr:data", PropertyType.BINARY);
        assertTrue(jcrDataProp.getDefinition().isMandatory());

        Property titleProp = new MockProperty("ns1:title", PropertyType.STRING);
        assertFalse(titleProp.getDefinition().isMandatory());
        ((MockPropertyDefinition) titleProp.getDefinition()).setMandatory(true);
        assertTrue(titleProp.getDefinition().isMandatory());
    }

    @Test
    public void testAutoCreatedProperties() throws Exception {
        Property jcrCreatedProp = new MockProperty("jcr:created", PropertyType.DATE);
        assertTrue(jcrCreatedProp.getDefinition().isAutoCreated());

        Property jcrIsCheckedOutProp = new MockProperty("jcr:isCheckedOut", PropertyType.BOOLEAN);
        assertTrue(jcrIsCheckedOutProp.getDefinition().isAutoCreated());

        Property titleProp = new MockProperty("ns1:title", PropertyType.STRING);
        assertFalse(titleProp.getDefinition().isAutoCreated());
        ((MockPropertyDefinition) titleProp.getDefinition()).setAutoCreated(true);
        assertTrue(titleProp.getDefinition().isAutoCreated());
    }

    @Test
    public void testProtectedProperties() throws Exception {
        Property jcrUuidProp = new MockProperty("jcr:uuid", PropertyType.STRING);
        assertTrue(jcrUuidProp.getDefinition().isProtected());

        Property jcrPrimaryTypeProp = new MockProperty("jcr:primaryType", PropertyType.STRING);
        assertTrue(jcrPrimaryTypeProp.getDefinition().isProtected());

        Property titleProp = new MockProperty("ns1:title", PropertyType.STRING);
        assertFalse(titleProp.getDefinition().isProtected());
        ((MockPropertyDefinition) titleProp.getDefinition()).setProtected(true);
        assertTrue(titleProp.getDefinition().isProtected());
    }

    @Test
    public void testGetRequiredType() throws Exception {
        Property jcrBaseVersionProp = new MockProperty("jcr:baseVersion", PropertyType.REFERENCE);
        assertEquals(PropertyType.REFERENCE, jcrBaseVersionProp.getDefinition().getRequiredType());

        Property jcrDataProp = new MockProperty("jcr:data", PropertyType.BINARY);
        assertEquals(PropertyType.BINARY, jcrDataProp.getDefinition().getRequiredType());

        Property jcrCreatedProp = new MockProperty("jcr:created", PropertyType.DATE);
        assertEquals(PropertyType.DATE, jcrCreatedProp.getDefinition().getRequiredType());

        Property jcrIsCheckedOutProp = new MockProperty("jcr:isCheckedOut", PropertyType.BOOLEAN);
        assertEquals(PropertyType.BOOLEAN, jcrIsCheckedOutProp.getDefinition().getRequiredType());

        Property jcrUuidProp = new MockProperty("jcr:uuid", PropertyType.STRING);
        assertEquals(PropertyType.STRING, jcrUuidProp.getDefinition().getRequiredType());

        Property titleProp = new MockProperty("ns1:title", PropertyType.STRING);
        assertEquals(PropertyType.STRING, titleProp.getDefinition().getRequiredType());
    }

}
