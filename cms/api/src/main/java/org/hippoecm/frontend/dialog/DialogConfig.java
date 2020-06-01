/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.dialog;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.onehippo.cms.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

class DialogConfig implements IClusterable {

    private static final Logger log = LoggerFactory.getLogger(DialogConfig.class);

    private final IPluginConfig defaultConfig;

    DialogConfig(final IPluginConfig defaultConfig) {
        this.defaultConfig = defaultConfig != null
            ? defaultConfig
            : new JavaPluginConfig();
    }

    IPluginConfig getMerged(final String jsonConfig) {
        if (StringUtils.isEmpty(jsonConfig)) {
            return defaultConfig;
        }

        final JavaPluginConfig config = new JavaPluginConfig();
        config.putAll(defaultConfig);

        try {
            final ObjectNode dialogConfig = Json.object(jsonConfig);
            addJsonToConfig(dialogConfig, config);
        } catch (IOException e) {
            log.warn("Could not parse dialog configuration '{}'. Using default configuration.",
                    jsonConfig, e);
        }
        return config;
    }

    private void addJsonToConfig(final ObjectNode json, final JavaPluginConfig config) {
        final Iterator<String> jsonFields = json.fieldNames();
        while (jsonFields.hasNext()) {
            final String jsonField = jsonFields.next();
            final JsonNode jsonValue = json.get(jsonField);
            config.put(jsonField, asJavaObject(jsonValue));
        }
    }

    private Object asJavaObject(final JsonNode jsonValue) {
        if (jsonValue.isTextual()) {
            return jsonValue.asText();
        } else if (jsonValue.isBoolean()) {
            return jsonValue.asBoolean();
        } else if (jsonValue.isArray()) {
            // always assume an array of strings
            final ArrayNode array = (ArrayNode) jsonValue;
            final String[] values = new String[array.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = array.get(i).asText();
            }
            return values;
        }
        log.warn("Skipped JSON value of unknown type: '{}'", jsonValue.toString());
        return null;
    }
}
