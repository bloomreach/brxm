package org.hippoecm.frontend.plugins.ckeditor;

import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * JSON manipulation utilities
 */
public class JsonUtils {

    public static JSONObject createJSONObject(String jsonOrBlank) throws JSONException {
        if (StringUtils.isBlank(jsonOrBlank)) {
            return new JSONObject();
        }
        return new JSONObject(jsonOrBlank);
    }

    public static JSONObject getOrCreateChildObject(JSONObject object, String key) throws JSONException {
        if (object.has(key)) {
            return object.getJSONObject(key);
        } else {
            JSONObject child = new JSONObject();
            object.put(key, child);
            return child;
        }
    }

    public static void appendToCommaSeparatedString(JSONObject object, String key, String value) throws JSONException {
        if (object.has(key)) {
            final String commaSeparated = object.getString(key);
            if (StringUtils.isBlank(commaSeparated)) {
                object.put(key, value);
            } else if (StringUtils.trim(commaSeparated).endsWith(",")) {
                object.put(key, commaSeparated + value);
            } else {
                object.put(key, commaSeparated + ',' + value);
            }
        } else {
            object.put(key, value);
        }
    }

    public static void putIfAbsent(JSONObject object, String key, String value) throws JSONException {
        if (!object.has(key)) {
            object.put(key, value);
        }
    }

    /**
     * Overlays a JSON object on top of another one. New values will be copied to the existing object.
     * Existing primitive values and arrays will replace existing ones. Existing objects will be merged
     * recursively.
     *
     * For example, if the existing object is:
     * {
     *     a: 'aaa',
     *     b: true
     *     c: {
     *         d: 'ddd',
     *         e: {
     *             f: 'fff'
     *         },
     *         g: {
     *             h: 'hhh'
     *         }
     *     }
     * }
     *
     * And the overlayed values are:
     * {
     *     a: 'AAA',
     *     c: {
     *         d: 'DDD',
     *         e: {
     *             y: 'yyy'
     *         },
     *         g: false
     *     },
     *     x: false
     * }
     *
     * The resulting object is:
     * {
     *     a: 'AAA',
     *     b: true,
     *     c: {
     *         d: 'DDD',
     *         e: {
     *             f: 'fff',
     *             y: 'yyy'
     *         },
     *         g: false
     *     },
     *     x: false
     * }
     *
     * @param object the object to add new values to and replace existing value in
     * @param values the values to add or replace existing ones with
     * @throws JSONException when reading or writing from/to the provided JSON objects fails
     */
    public static void overlay(JSONObject object, JSONObject values) throws JSONException {
        if (values == null) {
            return;
        }
        for (Iterator<String> newKeyIt = values.keys(); newKeyIt.hasNext(); ) {
            final String newKey = newKeyIt.next();
            final Object newValue = values.get(newKey);

            if (!object.has(newKey) || object.isNull(newKey)) {
                object.put(newKey, newValue);
            } else {
                final Object oldValue = object.get(newKey);
                if (oldValue instanceof JSONObject && newValue instanceof JSONObject) {
                    overlay((JSONObject)oldValue, (JSONObject)newValue);
                } else {
                    object.put(newKey, newValue);
                }
            }
        }
    }

    /**
     * Appends a JSON object to an existing one. New values will be copied to the existing object.
     * Existing strings will be appended to existing ones using a comma as separator. Existing arrays to get
     * the new values appended. Existing objects will be appended recursively.
     *
     * For example, if the existing object is:
     * {
     *     a: 'a1',
     *     b: {
     *         c: [ 'c1', 'c2' ]
     *     }
     * }
     *
     * And the appended object is:
     * {
     *     a: 'a2',
     *     b: {
     *         c: 'c3',
     *         d: false
     *     }
     * }
     *
     * The resulting object will be:
     * {
     *     a: 'a1,a2',
     *     b: {
     *         c: [ 'c1', 'c2', 'c3' ],
     *         d: false
     *     }
     * }
     *
     * @param object the object to append values to
     * @param values the values to append
     * @throws JSONException
     */
    public static void append(JSONObject object, JSONObject values) throws JSONException {
        if (values == null) {
            return;
        }
        for (Iterator<String> newKeyIt = values.keys(); newKeyIt.hasNext(); ) {
            final String newKey = newKeyIt.next();
            final Object newValue = values.get(newKey);

            if (!object.has(newKey) || object.isNull(newKey)) {
                object.put(newKey, newValue);
            } else {
                final Object oldValue = object.get(newKey);
                if (oldValue instanceof JSONObject && newValue instanceof JSONObject) {
                    append((JSONObject)oldValue, (JSONObject)newValue);
                } else if (oldValue instanceof String && newValue instanceof String) {
                    appendToCommaSeparatedString(object, newKey, (String)newValue);
                } else {
                    object.append(newKey, newValue);
                }
            }
        }
    }

}
