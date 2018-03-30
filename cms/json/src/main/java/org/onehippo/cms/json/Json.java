/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.json;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Utility class for manipulating JSON via the Jackson library.
 */
public class Json {

    private static final Logger log = LoggerFactory.getLogger(Json.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        mapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, false);
    }

    private static final ObjectWriter prettyPrinter = mapper.writerWithDefaultPrettyPrinter();

    private Json() {
    }

    public static ObjectNode object() {
        return mapper.createObjectNode();
    }

    public static ObjectNode object(final String json) throws IOException {
        final String toParse = StringUtils.isBlank(json) ? "{}" : json;
        final JsonNode result = mapper.readTree(toParse);
        if (result.isObject()) {
            return (ObjectNode) result;
        }
        throw new IOException("Not a JSON object: " + toParse);
    }

    public static String prettyString(final JsonNode json) {
        try {
            return prettyPrinter.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            final String uglyJson = json.toString();
            log.info("Could not pretty print JSON: '" + uglyJson + "'. Using ugly version instead.", e);
            return uglyJson;
        }
    }
}
