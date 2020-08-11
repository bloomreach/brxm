/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.v10;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.Assert.assertFalse;

public class JsonValidationUtil {

    public static void validateReferences(final JsonNode jsonNodeRoot, final JsonNode current) throws IOException {
        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = current.fields();

        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JsonNode> field = fieldsIterator.next();
            final JsonNode value = field.getValue();
            if (value.isContainerNode()) {
                validateReferences(jsonNodeRoot, value); // RECURSIVE CALL
            } else {
                final String key = field.getKey();
                if ("$ref".equals(key)) {
                    // assert that the ref pointer exists

                    final JsonPointer jsonPointer = JsonPointer.compile(value.asText());
                    assertFalse(String.format("Missing reference '%s'", value.asText()),
                            jsonNodeRoot.at(jsonPointer).isMissingNode());

                }
            }
        }
    }
}
