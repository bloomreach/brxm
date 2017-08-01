/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.document.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.ValidationErrorInfo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FieldValueTest {

    @Test
    public void equals() {
        FieldValue empty1 = new FieldValue();
        FieldValue empty2 = new FieldValue();
        FieldValue value1 = new FieldValue("value1");
        FieldValue value2 = new FieldValue("value2");

        Map<String, List<FieldValue>> compoundValues1 = new HashMap<>();
        compoundValues1.put("value1", Arrays.asList(value1));
        FieldValue compound1 = new FieldValue(compoundValues1);

        Map<String, List<FieldValue>> compoundValues2 = new HashMap<>();
        compoundValues1.put("value2", Arrays.asList(value2));
        FieldValue compound2 = new FieldValue(compoundValues2);

        FieldValue choice1 = new FieldValue("choice1", value1);
        FieldValue choice2 = new FieldValue("choice2", value2);

        assertTrue(empty1.equals(empty1));
        assertTrue(empty1.equals(empty2));

        assertTrue(value1.equals(value1));
        assertFalse(value1.equals(value2));

        assertTrue(compound1.equals(compound1));
        assertFalse(compound1.equals(compound2));

        assertTrue(choice1.equals(choice1));
        assertFalse(choice1.equals(choice2));

        assertFalse(empty1.equals(value1));
        assertFalse(value1.equals(compound1));
        assertFalse(compound1.equals(choice1));
    }

    @Test
    public void equalsIgnoresIds() {
        FieldValue value1 = new FieldValue("value");
        value1.setId("1");

        FieldValue value2 = new FieldValue("value");
        value2.setId("2");

        assertTrue(value1.equals(value2));
    }

    @Test
    public void equalsIgnoresErrorInfo() {
        FieldValue value1 = new FieldValue("value");

        FieldValue value2 = new FieldValue("value");
        value2.setErrorInfo(new ValidationErrorInfo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));

        assertTrue(value1.equals(value2));
    }
}