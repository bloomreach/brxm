package org.hippoecm.frontend.plugins.ckeditor;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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

}
