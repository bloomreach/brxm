/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.utils;

import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

public class TagUtils {

    private static final char DOUBLE_QUOTE = '"';

    /**
     * Returns the given map as a JSON map, with the keys and values in double quotes. Keys and values will be converted
     * to strings by calling {@link Object#toString()}.
     *
     * @param map a map (<code>null</code> or empty are allowed)
     * @return string representation of a JSON map.
     */
    public static String toJSONMap(Map<?, ?> map) {
        if (MapUtils.isEmpty(map)) {
            return "{}";
        }
        final StringBuilder builder = new StringBuilder("{");
        for (Map.Entry<?, ?> each : map.entrySet()) {
            doubleQuote(builder, each.getKey()).append(':');
            doubleQuote(builder, each.getValue()).append(',');
        }
        final int length = builder.length();
        return builder.replace(length - 1, length, "}").toString();
    }

    /**
     * Returns the string enclosed in HTML comments. Before enclosing the string will be whitespace trimmed.
     *
     * @param string a string (<code>null</code> is allowed)
     * @return trimmed string enclosed in HTML comments.
     */
    public static String encloseInHTMLComment(String string) {
        return String.format("<!-- %s -->", StringUtils.stripToEmpty(string));
    }

    private static StringBuilder doubleQuote(StringBuilder builder, Object value) {
        return builder.append(DOUBLE_QUOTE).append(value).append(DOUBLE_QUOTE);
    }
}
