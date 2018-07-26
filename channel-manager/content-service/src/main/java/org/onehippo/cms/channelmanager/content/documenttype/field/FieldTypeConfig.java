/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.documenttype.field;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms.json.Json;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Reads configuration properties of a field type into a JSON object using the 'builder' pattern with a fluid syntax.
 * The names of the properties become the keys of the JSON object.
 * If a 'removePrefix' is set, the keys becomes the name without that prefix in all subsequent properties.
 */
public class FieldTypeConfig {

    private final ObjectNode config;
    private final FieldTypeContext context;
    private String prefix = StringUtils.EMPTY;

    public FieldTypeConfig(final FieldTypeContext context) {
        this.context = context;
        config = Json.object();
    }

    public FieldTypeConfig prefix(final String prefix) {
        this.prefix = prefix;
        return this;
    }

    public FieldTypeConfig booleans(final String... names) {
        for (final String propertyName : names) {
            context.getBooleanConfig(prefix + propertyName).ifPresent((value) -> config.put(propertyName, value));
        }
        return this;
    }

    public FieldTypeConfig strings(final String... names) {
        for (final String propertyName : names) {
            context.getStringConfig(prefix + propertyName).ifPresent((value) -> config.put(propertyName, value));
        }
        return this;
    }

    public FieldTypeConfig multipleStrings(final String... names) {
        for (final String propertyName : names) {
            context.getMultipleStringConfig(prefix + propertyName).ifPresent((values -> {
                final ArrayNode array = config.putArray(propertyName);
                for (final String value : values) {
                    array.add(value);
                }
            }));
        }
        return this;
    }

    public ObjectNode build() {
        return config;
    }
}
