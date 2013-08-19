package org.hippoecm.frontend.plugins.ckeditor;

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

}
