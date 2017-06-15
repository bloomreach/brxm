/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.core.resource.jackson;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public class JacksonUtils {

    private JacksonUtils() {
    }

    public static Object getJsonScalaValue(final JsonNode jsonNode) {
        if (jsonNode.isContainerNode()) {
            throw new IllegalArgumentException("jsonNode is a container node.");
        }

        Object value = null;

        switch (jsonNode.getNodeType()) {
        case STRING:
            value = jsonNode.asText();
            break;
        case NUMBER:
            if (jsonNode.isLong()) {
                value = jsonNode.asLong();
            } else if (jsonNode.isInt() || jsonNode.isShort()) {
                value = jsonNode.asInt();
            } else if (jsonNode.isDouble() || jsonNode.isFloat()) {
                value = jsonNode.asDouble();
            } else if (jsonNode.isBigInteger()) {
                value = new BigInteger(jsonNode.asText());
            } else if (jsonNode.isBigDecimal()) {
                value = new BigDecimal(jsonNode.asText());
            }
            break;
        case BOOLEAN:
            value = jsonNode.asBoolean();
            break;
        }

        return value;
    }

    public static boolean hasAnyContainerNodeField(final JsonNode base) {
        JsonNode field;

        for (Iterator<Map.Entry<String, JsonNode>> it = base.fields(); it.hasNext(); ) {
            field = it.next().getValue();

            if (field.isContainerNode()) {
                return true;
            }
        }

        return false;
    }

    public static List<String> getFieldNames(final JsonNode jsonNode) {
        List<String> fieldNames = new LinkedList<>();

        for (Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields(); it.hasNext(); ) {
            fieldNames.add(it.next().getKey());
        }

        return fieldNames;
    }
}
