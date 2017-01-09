/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.component.support.forms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

public class TestFormField {

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNull() {
        new FormField(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithEmptyString() {
        new FormField("");
    }

    @Test
    public void testName() throws Exception {
        FormField formField = new FormField("test name");
        assertEquals("Getter does not return name from constructor", "test name", formField.getName());
        formField.setName("test name 2");
        assertEquals("Getter does not return name from setter", "test name 2", formField.getName());
    }

    @Test
    public void testLabel() throws Exception {
        FormField formField = new FormField("t");
        formField.setLabel("test label");
        assertEquals("Getter does not return label set by setter", "test label", formField.getLabel());
    }

    @Test
    public void testMessages() throws Exception {
        FormField formField = new FormField("t");
        assertNotNull("Getter may not return null", formField.getMessages());
        assertEquals("Size of messages list must be 0", 0, formField.getMessages().size());

        formField.addMessage("m 1");
        assertEquals("First message not equal to added message", "m 1", formField.getMessages().get(0));
        assertEquals("Size of messages list must be 1", 1, formField.getMessages().size());

        formField.addMessage("m 2");
        assertEquals("First message not equal to added message", "m 1", formField.getMessages().get(0));
        assertEquals("Second message not equal to added message", "m 2", formField.getMessages().get(1));
        assertEquals("Size of messages list must be 2", 2, formField.getMessages().size());

        formField.addMessage(null);
        assertEquals("First message not equal to added message", "m 1", formField.getMessages().get(0));
        assertEquals("Second message not equal to added message", "m 2", formField.getMessages().get(1));
        assertNull("Third message must be null", formField.getMessages().get(2));
        assertEquals("Size of messages list must be 3", 3, formField.getMessages().size());

        formField.setMessages(null);
        assertNotNull("Getter may not return null", formField.getMessages());
        assertEquals("Size of messages list must be 0", 0, formField.getMessages().size());

        List<String> newList = new ArrayList<>();
        newList.add("n 1");
        newList.add("n 2");
        formField.setMessages(newList);

        assertEquals("First message not equal to set message", "n 1", formField.getMessages().get(0));
        assertEquals("Second message not equal to set message", "n 2", formField.getMessages().get(1));
        assertEquals("Size of set messages list must be 2", 2, formField.getMessages().size());
    }

    @Test
    public void testValues() throws Exception {
        FormField formField = new FormField("t");
        assertNotNull("Getter may not return null", formField.getValueList());
        assertEquals("Size of value list must be 0", 0, formField.getValueList().size());
        assertEquals("Get value must return empty String", "", formField.getValue());

        formField.addValue("v 1");
        assertEquals("First value not equal to added value", "v 1", formField.getValueList().get(0));
        assertEquals("Size of value list must be 1", 1, formField.getValueList().size());
        assertEquals("Get value must return first value list entry", "v 1", formField.getValue());

        formField.addValue("v 2");
        assertEquals("First value not equal to added value", "v 1", formField.getValueList().get(0));
        assertEquals("Second value not equal to added value", "v 2", formField.getValueList().get(1));
        assertEquals("Size of value list must be 2", 2, formField.getValueList().size());
        assertEquals("Get value must return first value list entry", "v 1", formField.getValue());

        formField.addValue(null);
        assertEquals("First value not equal to added value", "v 1", formField.getValueList().get(0));
        assertEquals("Second value not equal to added value", "v 2", formField.getValueList().get(1));
        assertEquals("Size of value list must still be 2", 2, formField.getValueList().size());
        assertEquals("Get value must return first value list entry", "v 1", formField.getValue());

        formField.setValueList(null);
        assertNotNull("Getter may not return null", formField.getValueList());
        assertEquals("Size of value list must be 0", 0, formField.getValueList().size());
        assertEquals("Get value must return empty String", "", formField.getValue());

        List<String> newList = new ArrayList<>();
        newList.add("w 1");
        newList.add("w 2");
        newList.add(null);
        formField.setValueList(newList);

        assertEquals("First value not equal to set value", "w 1", formField.getValueList().get(0));
        assertEquals("Second value not equal to set value", "w 2", formField.getValueList().get(1));
        assertNull("Third value must be null", formField.getValueList().get(2));
        assertEquals("Size of set value list must be 3", 3, formField.getValueList().size());
        assertEquals("Get value must return first value list entry", "w 1", formField.getValue());
    }

    @Test
    public void testDeprectatedSetValues() throws Exception {
        FormField formField = new FormField("t");

        formField.setValues(null);
        assertNotNull("Getter may not return null", formField.getValueList());
        assertEquals("Size of value list must be 0", 0, formField.getValueList().size());
        assertEquals("Get value must return empty String", "", formField.getValue());

        Map<String, String> valuesMap = new LinkedHashMap<>();
        valuesMap.put("w 1", "w 1");
        valuesMap.put("w 2", "w 2");
        valuesMap.put(null, null);
        formField.setValues(valuesMap);

        assertEquals("First value not equal to set value", "w 1", formField.getValueList().get(0));
        assertEquals("Second value not equal to set value", "w 2", formField.getValueList().get(1));
        assertNull("Third value must be null", formField.getValueList().get(2));
        assertEquals("Size of set value list must be 3", 3, formField.getValueList().size());
        assertEquals("Get value must return first value list entry", "w 1", formField.getValue());
    }

    @Test
    public void testDeprecatedGetValues() throws Exception {
        FormField formField = new FormField("t");
        assertNotNull("Getter may not return null", formField.getValues());
        assertEquals("Size of values map must be 0", 0, formField.getValues().size());

        formField.addValue("v 1");
        assertEquals("First value not equal to added value", "v 1", formField.getValues().get("v 1"));
        assertEquals("Size of values map must be 1", 1, formField.getValues().size());

        formField.addValue("v 2");
        assertEquals("First value not equal to added value", "v 1", formField.getValues().get("v 1"));
        assertEquals("Second value not equal to added value", "v 2", formField.getValues().get("v 2"));
        assertEquals("Size of values map must be 2", 2, formField.getValues().size());

        formField.addValue(null);
        assertEquals("First value not equal to added value", "v 1", formField.getValues().get("v 1"));
        assertEquals("Second value not equal to added value", "v 2", formField.getValues().get("v 2"));
        assertEquals("Size of values map must still be 2", 2, formField.getValues().size());

        formField.setValues(null);
        assertNotNull("Getter may not return null", formField.getValues());
        assertEquals("Size of values map must be 0", 0, formField.getValues().size());

        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("w 1", "w 1");
        valuesMap.put("w 2", "w 2");
        valuesMap.put(null, null);
        formField.setValues(valuesMap);

        assertEquals("First value not equal to set value", "w 1", formField.getValues().get("w 1"));
        assertEquals("Second value not equal to set value", "w 2", formField.getValues().get("w 2"));
        assertEquals("Size of set values map must be 3", 3, formField.getValues().size());
    }

    @Test
    public void testEquals() throws Exception {
        FormField formField = new FormField("t 1");
        FormField formField1 = new FormField("t 1");
        assertEquals("FormFields with the same name must be seen as equal objects.", formField, formField1);
        FormField formField2 = new FormField("t 2");
        assertNotSame("FormFields with a different name must be seen as unequal objects.", formField, formField2);
    }

    @Test
    public void testHash() throws Exception {
        String name = "field 1";
        FormField formField = new FormField(name);
        assertEquals("Hashcode of FormField must equal hashcode of its name", name.hashCode(), formField.hashCode());
    }
}