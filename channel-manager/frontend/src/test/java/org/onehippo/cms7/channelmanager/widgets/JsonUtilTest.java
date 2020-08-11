/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.widgets;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests {@link JsonUtil}.
 */
public class JsonUtilTest {

    private Map<String, JSONArray> parameters;

    @Before
    public void setUp() {
         parameters = new HashMap<String, JSONArray>();
    }

    /**
     * Tests getStringParameter(final Map<String, JSONArray> parameters, String parameterName)
     */
    @Test
    public void getStringParameter() {
        parameters.put("test", new JSONArray(Arrays.asList("one", "two")));
        assertEquals("one", JsonUtil.getStringParameter(parameters, "test"));
        assertNull(JsonUtil.getStringParameter(parameters, "nosuchparam"));

        parameters.put("empty", new JSONArray(Arrays.asList()));
        assertEquals(null, JsonUtil.getStringParameter(parameters, "empty"));
    }

    /**
     * Tests getJsonObject(final Map<String, JSONArray> parameters, String parameterName)
     */
    @Test
    public void getJsonObject() throws JSONException {
        String json = "{\"one\": 1}";
        JSONObject jsonObject = new JSONObject(json);
        parameters.put("test", new JSONArray(Arrays.asList(jsonObject)));
        assertEquals(jsonObject, JsonUtil.getJsonObject(parameters, "test"));

        parameters.put("notjson", new JSONArray(Arrays.asList("not a JSON object")));
        assertNull(JsonUtil.getJsonObject(parameters, "notjson"));
    }

}
