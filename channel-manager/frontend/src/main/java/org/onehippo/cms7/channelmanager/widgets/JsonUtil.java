/**
 * Copyright 2011 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.channelmanager.widgets;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON utility methods.
 */
class JsonUtil {

    private static Logger log = LoggerFactory.getLogger(JsonUtil.class);

    private JsonUtil() {
        // prevent instantiation
    }

    static String getStringParameter(final Map<String, JSONArray> parameters, String parameterName) {
        if (parameters.containsKey(parameterName)) {
            JSONArray array = parameters.get(parameterName);
            if (array.length() > 0) {
                try {
                    return array.get(0).toString();
                } catch (JSONException e) {
                    log.error("Could not parse JSON string parameter '" + parameterName + "'", e);
                }
            }
        }
        return null;
    }

    static JSONObject getJsonObject(final Map<String, JSONArray> parameters, String parameterName) {
        if (parameters.containsKey(parameterName)) {
            JSONArray array = parameters.get(parameterName);
            if (array.length() > 0) {
                try {
                    Object o = array.getJSONObject(0);
                    return (JSONObject)o;
                } catch (JSONException e) {
                    log.error("Could not parse JSON object parameter '" + parameterName + "'", e);
                }
            }
        }
        return null;
    }

    static String[] getStringArray(JSONObject json, String name, String[] defaultValue) throws JSONException {
        JSONArray jsonArray = json.optJSONArray(name);
        if (jsonArray == null) {
            return defaultValue;
        }

        String[] result = new String[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            result[i] = jsonArray.getString(i);
        }
        return result;
    }

}
