/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.ckeditor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

/**
 * Tests {@link CKEditorStringUtils}.
 */
public class JsonUtilsTest {

    private JSONObject object;

    @Before
    public void setUp() throws Exception {
        object = new JSONObject();
    }

    @Test
    public void appendToNewCommaSeparatedString() throws JSONException {
        JsonUtils.appendToCommaSeparatedString(object, "test", "foo");
        assertEquals("foo", object.get("test"));
    }

    @Test
    public void appendToEmptyCommaSeparatedString() throws JSONException {
        assertEquals("foo", appendToCommaSeparatedString("", "foo"));
    }

    @Test
    public void appendToCommaSeparatedString() throws JSONException {
        assertEquals("one,two,three", appendToCommaSeparatedString("one,two", "three"));
    }

    @Test
    public void appendToCommaSeparatedStringEndingWithComma() throws JSONException {
        assertEquals("one,two,three", appendToCommaSeparatedString("one,two,", "three"));
    }

    private String appendToCommaSeparatedString(String initialValue, String appendedValue) throws JSONException {
        object.put("test", initialValue);
        JsonUtils.appendToCommaSeparatedString(object, "test", appendedValue);
        return object.getString("test");
    }

    @Test
    public void putIfAbsentWhenAbsent() throws JSONException {
        JsonUtils.putIfAbsent(object, "test", "value");
        assertEquals("value", object.getString("test"));
    }

    @Test
    public void putIfAbsentWhenPresent() throws JSONException {
        object.put("test", "value");
        JsonUtils.putIfAbsent(object, "test", "newValue");
        assertEquals("value", object.getString("test"));
    }

    @Test
    public void getOrCreateNewChildObject() throws JSONException {
        JSONObject child = JsonUtils.getOrCreateChildObject(object, "test");
        assertEquals("new child object is empty", 0, child.length());
    }

    @Test
    public void getOrCreateExistingChildObject() throws JSONException {
        JSONObject child = new JSONObject();
        object.put("test", child);
        assertSame(child, JsonUtils.getOrCreateChildObject(object, "test"));
    }

    @Test
    public void overlayNullDoesNothing() throws JSONException {
        JsonUtils.overlay(object, null);
        assertEquals(0, object.length());
    }

    @Test
    public void overlayEmptyObjectDoesNothing() throws JSONException {
        JsonUtils.overlay(object, new JSONObject());
        assertEquals(0, object.length());
    }

    @Test
    public void overlayNewStringIsAdded() throws JSONException {
        object.put("key1", "value1");
        JsonUtils.overlay(object, new JSONObject("{ key2: 'value2' }"));
        assertEquals(2, object.length());
        assertEquals("value1", object.getString("key1"));
        assertEquals("value2", object.getString("key2"));
    }

    @Test
    public void overlayNewObjectIsAdded() throws JSONException {
        object.put("key1", "value1");
        JsonUtils.overlay(object, new JSONObject("{ key2: { childKey: 'childValue' } }"));
        assertEquals(2, object.length());
        assertEquals("value1", object.getString("key1"));

        JSONObject child = object.getJSONObject("key2");
        assertEquals("childValue", child.getString("childKey"));
    }

    @Test
    public void overlayExistingStringReplacesExistingValue() throws JSONException {
        object.put("key", "value");
        JsonUtils.overlay(object, new JSONObject("{ key: 'newValue' }"));
        assertEquals(1, object.length());
        assertEquals("newValue", object.getString("key"));
    }

    @Test
    public void overlayExistingBooleanReplacesExistingValue() throws JSONException {
        object.put("key", true);
        JsonUtils.overlay(object, new JSONObject("{ key: false }"));
        assertEquals(1, object.length());
        assertFalse(object.getBoolean("key"));
    }

    @Test
    public void overlayObjectOverwritesKeysInExistingObject() throws JSONException {
        object.put("key", new JSONObject("{ childKey: 'childValue' }"));
        JsonUtils.overlay(object, new JSONObject("{ key: { childKey: 'newChildValue' } }"));

        assertEquals(1, object.length());

        JSONObject child = object.getJSONObject("key");
        assertEquals(1, child.length());
        assertEquals("newChildValue", child.getString("childKey"));
    }

    @Test
    public void overlayObjectOverwritesKeysInExistingGrandchildren() throws JSONException {
        object.put("key", new JSONObject("{ childKey: { grandchildKey: 'foo' } }"));
        JsonUtils.overlay(object, new JSONObject("{ key: { childKey: { grandchildKey: 'bar' } } }"));

        assertEquals(1, object.length());

        JSONObject child = object.getJSONObject("key");
        assertEquals(1, child.length());

        JSONObject grandchild = child.getJSONObject("childKey");
        assertEquals(1, grandchild.length());
        assertEquals("bar", grandchild.getString("grandchildKey"));
    }

    @Test
    public void appendNullDoesNothing() throws JSONException {
        JsonUtils.append(object, null);
        assertEquals(0, object.length());
    }

    @Test
    public void appendEmptyObjectDoesNothing() throws JSONException {
        JsonUtils.append(object, new JSONObject());
        assertEquals(0, object.length());
    }

    @Test
    public void appendNewStringIsAdded() throws JSONException {
        object.put("key1", "value1");
        JsonUtils.append(object, new JSONObject("{ key2: 'value2' }"));
        assertEquals(2, object.length());
        assertEquals("value1", object.getString("key1"));
        assertEquals("value2", object.getString("key2"));
    }

    @Test
    public void appendNewObjectIsAdded() throws JSONException {
        object.put("key1", "value1");
        JsonUtils.append(object, new JSONObject("{ key2: { childKey: 'childValue' } }"));
        assertEquals(2, object.length());
        assertEquals("value1", object.getString("key1"));

        JSONObject child = object.getJSONObject("key2");
        assertEquals("childValue", child.getString("childKey"));
    }

    @Test
    public void appendStringToStringCreatesCommaSeparatedString() throws JSONException {
        object.put("key", "first");
        JsonUtils.append(object, new JSONObject("{ key: 'second' }"));
        assertEquals(1, object.length());
        assertEquals("first,second", object.getString("key"));
    }

    @Test
    public void appendStringToArrayAppendsToArray() throws JSONException {
        object.put("key", new JSONArray("['one','two']"));
        JsonUtils.append(object, new JSONObject("{ key: 'three' }"));
        assertEquals(1, object.length());
        assertEquals("\"one\",\"two\",\"three\"", object.getJSONArray("key").join(","));
    }

    @Test
    public void appendArrayToArrayConcatenatesTheArrays() throws JSONException {
        object.put("key", new JSONArray("['one','two']"));
        JsonUtils.append(object, new JSONObject("{ key: ['three', 'four' ] }"));
        assertEquals(1, object.length());
        assertEquals("\"one\",\"two\",\"three\",\"four\"", object.getJSONArray("key").join(","));
    }

    @Test(expected = JSONException.class)
    public void appendBooleanToStringThrowsException() throws JSONException {
        object.put("key", "first");
        JsonUtils.append(object, new JSONObject("{ key: true }"));
    }

    @Test
    public void appendObjectToObjectAppendsRecursively() throws JSONException {
        object.put("key", new JSONObject("{ childKey: 'first' }"));
        JsonUtils.append(object, new JSONObject("{ key: { childKey: 'second' } }"));

        assertEquals(1, object.length());

        JSONObject child = object.getJSONObject("key");
        assertEquals(1, child.length());
        assertEquals("first,second", child.getString("childKey"));
    }

}
