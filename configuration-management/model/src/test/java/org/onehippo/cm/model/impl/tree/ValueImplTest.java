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
package org.onehippo.cm.model.impl.tree;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.onehippo.cm.model.tree.ValueFormatException;
import org.onehippo.cm.model.tree.ValueType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ValueImplTest {

    private final String resourcePath = "/path/to/resource";

    @Test
    public void makeStringResourceValueTest() throws IOException {

        final String strValue = "stringValue";
        final ValueImpl stringValue = new ValueImpl(strValue);

        stringValue.makeStringResourceValue(resourcePath);

        assertTrue(stringValue.isResource());
        assertEquals(resourcePath, stringValue.getString());
        assertEquals(strValue, IOUtils.toString(stringValue.getResourceInputStream(), StandardCharsets.UTF_8));
        assertNull(stringValue.getInternalResourcePath());
        try {
            stringValue.setInternalResourcePath("/new/path/to/resource");
        } catch (ValueFormatException ignore) { }

    }

    @Test
    public void makeStringResourceValueNegativeTest() throws IOException {

        final ValueImpl boolValue = new ValueImpl(true);

        try {
            boolValue.makeStringResourceValue(resourcePath);
            fail("Converting non string value to resource should fail");
        } catch (ValueFormatException ignore) {}

        final ValueImpl resourceStringValue = new ValueImpl("value", ValueType.STRING, true, false);
        try {
            resourceStringValue.makeStringResourceValue(resourcePath);
            fail("Converting string resource value to resource should fail");
        } catch (ValueFormatException ignore) {}
    }
}