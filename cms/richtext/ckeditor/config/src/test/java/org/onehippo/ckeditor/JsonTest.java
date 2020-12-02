/*
 * Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.ckeditor;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonTest {

    private ObjectNode object;

    @Before
    public void setUp() {
        object = Json.object();
    }

    @Test
    public void createEmptyObject() {
        assertTrue(object.isObject());
        assertEquals(0, object.size());
    }

    @Test
    public void createObjectFromValidJson() throws IOException {
        ObjectNode object = Json.object("{ a: 1 } ");
        assertTrue(object.isObject());
        assertEquals(1, object.get("a").asInt());
    }

    @Test(expected = IOException.class)
    public void createObjectFromWrongJson() throws IOException {
        Json.object("[ a: 1 ] ");
    }

    @Test(expected = IOException.class)
    public void createObjectFromInvalidJson() throws IOException {
        Json.object("{ invalid json");
    }

    @Test
    public void createObjectFromEmptyString() throws IOException {
        ObjectNode object = Json.object("");
        assertTrue(object.isObject());
        assertEquals(0, object.size());
    }

    @Test
    public void createObjectFromBlankString() throws IOException {
        ObjectNode object = Json.object("    ");
        assertTrue(object.isObject());
        assertEquals(0, object.size());
    }

    @Test
    public void createEmptyArray() {
        ArrayNode array = Json.array();
        assertTrue(array.isArray());
        assertEquals(0, array.size());
    }

    @Test
    public void createArrayFromValidJson() throws IOException {
        ArrayNode array = Json.array("[ 1, 2, 3 ]");
        assertTrue(array.isArray());
        assertEquals(3, array.size());
        assertEquals(1, array.get(0).intValue());
        assertEquals(2, array.get(1).intValue());
        assertEquals(3, array.get(2).intValue());
    }

    @Test(expected = IOException.class)
    public void createArrayFromWrongJson() throws IOException {
        Json.array("{ object: true }");
    }

    @Test(expected = IOException.class)
    public void createArrayFromInvalidJson() throws IOException {
        Json.array("[ not an array");
    }

    @Test
    public void createArrayFromEmptyString() throws IOException {
        ArrayNode array = Json.array("");
        assertTrue(array.isArray());
        assertEquals(0, array.size());
    }

    @Test
    public void createArrayFromBlankString() throws IOException {
        ArrayNode array = Json.array("   ");
        assertTrue(array.isArray());
        assertEquals(0, array.size());
    }

    @Test
    public void overlayNullDoesNothing() throws IOException {
        Json.overlay(object, null);
        assertEquals(0, object.size());
    }

    @Test
    public void overlayEmptyObjectDoesNothing() throws IOException {
        Json.overlay(object, Json.object());
        assertEquals(0, object.size());
    }

    @Test
    public void overlayNewStringIsAdded() throws IOException {
        object.put("key1", "value1");
        Json.overlay(object, Json.object("{ key2: 'value2' }"));
        assertEquals(2, object.size());
        assertEquals("value1", object.get("key1").asText());
        assertEquals("value2", object.get("key2").asText());
    }

    @Test
    public void overlayNewObjectIsAdded() throws IOException {
        object.put("key1", "value1");
        Json.overlay(object, Json.object("{ key2: { childKey: 'childValue' } }"));
        assertEquals(2, object.size());
        assertEquals("value1", object.get("key1").asText());

        JsonNode child = object.get("key2");
        assertEquals("childValue", child.get("childKey").asText());
    }

    @Test
    public void overlayExistingStringReplacesExistingValue() throws IOException {
        object.put("key", "value");
        Json.overlay(object, Json.object("{ key: 'newValue' }"));
        assertEquals(1, object.size());
        assertEquals("newValue", object.get("key").asText());
    }

    @Test
    public void overlayExistingBooleanReplacesExistingValue() throws IOException {
        object.put("key", true);
        Json.overlay(object, Json.object("{ key: false }"));
        assertEquals(1, object.size());
        assertFalse(object.get("key").asBoolean());
    }

    @Test
    public void overlayObjectOverwritesKeysInExistingObject() throws IOException {
        object.set("key", Json.object("{ childKey: 'childValue' }"));
        Json.overlay(object, Json.object("{ key: { childKey: 'newChildValue' } }"));

        assertEquals(1, object.size());

        JsonNode child = object.get("key");
        assertEquals(1, child.size());
        assertEquals("newChildValue", child.get("childKey").asText());
    }

    @Test
    public void overlayObjectOverwritesKeysInExistingGrandchildren() throws IOException {
        object.set("key", Json.object("{ childKey: { grandchildKey: 'foo' } }"));
        Json.overlay(object, Json.object("{ key: { childKey: { grandchildKey: 'bar' } } }"));

        assertEquals(1, object.size());

        JsonNode child = object.get("key");
        assertEquals(1, child.size());

        JsonNode grandchild = child.get("childKey");
        assertEquals(1, grandchild.size());
        assertEquals("bar", grandchild.get("grandchildKey").asText());
    }

    @Test
    public void appendNullDoesNothing() {
        Json.append(object, null);
        assertEquals(0, object.size());
    }

    @Test
    public void appendEmptyObjectDoesNothing() throws IOException {
        Json.append(object, Json.object());
        assertEquals(0, object.size());
    }

    @Test
    public void appendNewStringIsAdded() throws IOException {
        object.put("key1", "value1");
        Json.append(object, Json.object("{ key2: 'value2' }"));
        assertEquals(2, object.size());
        assertEquals("value1", object.get("key1").asText());
        assertEquals("value2", object.get("key2").asText());
    }

    @Test
    public void appendNewObjectIsAdded() throws IOException {
        object.put("key1", "value1");
        Json.append(object, Json.object("{ key2: { childKey: 'childValue' } }"));
        assertEquals(2, object.size());
        assertEquals("value1", object.get("key1").asText());

        JsonNode child = object.get("key2");
        assertEquals("childValue", child.get("childKey").asText());
    }

    @Test
    public void appendStringToStringCreatesCommaSeparatedString() throws IOException {
        object.put("key", "first");
        Json.append(object, Json.object("{ key: 'second' }"));
        assertEquals(1, object.size());
        assertEquals("first,second", object.get("key").asText());
    }

    @Test
    public void appendStringToArrayAppendsToArray() throws IOException {
        object.set("key", Json.array("['one','two']"));
        Json.append(object, Json.object("{ key: 'three' }"));
        assertEquals(1, object.size());
        JsonNode array = object.get("key");
        assertEquals(3, array.size());
        assertEquals("one", array.get(0).asText());
        assertEquals("two", array.get(1).asText());
        assertEquals("three", array.get(2).asText());
    }

    @Test
    public void appendArrayToArrayConcatenatesTheArrays() throws IOException {
        object.set("key", Json.array("['one','two']"));
        Json.append(object, Json.object("{ key: ['three', 'four' ] }"));
        assertEquals(1, object.size());
        JsonNode array = object.get("key");
        assertEquals(4, array.size());
        assertEquals("one", array.get(0).asText());
        assertEquals("two", array.get(1).asText());
        assertEquals("three", array.get(2).asText());
        assertEquals("four", array.get(3).asText());
    }

    @Test
    public void appendBooleanToStringIsIgnored() throws IOException {
        object.put("key", "first");
        Json.append(object, Json.object("{ key: true }"));
        assertEquals("first", object.get("key").asText());
    }

    @Test
    public void appendObjectToObjectAppendsRecursively() throws IOException {
        object.set("key", Json.object("{ childKey: 'first' }"));
        Json.append(object, Json.object("{ key: { childKey: 'second' } }"));

        assertEquals(1, object.size());

        JsonNode child = object.get("key");
        assertEquals(1, child.size());
        assertEquals("first,second", child.get("childKey").asText());
    }

    @Test
    public void appendToNewCommaSeparatedString() {
        Json.appendToCommaSeparatedString(object, "test", "foo");
        assertEquals("foo", object.get("test").asText());
    }

    @Test
    public void appendToEmptyCommaSeparatedString() {
        assertEquals("foo", appendToCommaSeparatedString("", "foo"));
    }

    @Test
    public void appendToCommaSeparatedString() {
        assertEquals("one,two,three", appendToCommaSeparatedString("one,two", "three"));
    }

    @Test
    public void appendToCommaSeparatedStringEndingWithComma() {
        assertEquals("one,two,three", appendToCommaSeparatedString("one,two,", "three"));
    }

    private String appendToCommaSeparatedString(String initialValue, String appendedValue) {
        object.put("test", initialValue);
        Json.appendToCommaSeparatedString(object, "test", appendedValue);
        return object.get("test").asText();
    }

    @Test
    public void prettyStringForEmptyObject() throws JsonProcessingException {
        assertEquals("{ }", Json.prettyString(object));
    }

    @Test
    public void prettyStringForNullThrowsException() throws JsonProcessingException {
        assertEquals("null", Json.prettyString(null));
    }

    @Test
    public void prettyStringForObject() throws IOException {
        assertEquals("{\n"
                        + "  value : 1,\n"
                        + "  child : {\n"
                        + "    nestedValue1 : \"text\",\n"
                        + "    nestedValue2 : true\n"
                        + "  }\n"
                        + "}",
                Json.prettyString(Json.object("{ value: 1, child: { nestedValue1: 'text', nestedValue2: true } }"))
        );
    }

}