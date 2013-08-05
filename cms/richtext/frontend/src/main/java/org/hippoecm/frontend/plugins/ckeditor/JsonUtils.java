package org.hippoecm.frontend.plugins.ckeditor;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * JSON manipulation utilities
 */
class JsonUtils {

    static JSONObject createJSONObject(String jsonOrBlank) throws JSONException {
        if (StringUtils.isBlank(jsonOrBlank)) {
            return new JSONObject();
        }
        return new JSONObject(jsonOrBlank);
    }

    static void appendToCommaSeparatedString(JSONObject object, String key, String value) throws JSONException {
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

    static void putIfAbsent(JSONObject object, String key, String value) throws JSONException {
        if (!object.has(key)) {
            object.put(key, value);
        }
    }

}
