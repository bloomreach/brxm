/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.util;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP Header Utils.
 */
public class HttpHeaderUtils {

    private static Logger log = LoggerFactory.getLogger(HttpHeaderUtils.class);

    private HttpHeaderUtils() {
    }

    /**
     * Parse HTTP Header line in the format: <code>&lt;name&gt; ":" &lt;value&gt;</code>. Or null if the input is
     * null or the input is in an invalid form.
     * e.g, "Access-Control-Allow-Origin: *" will be parsed into [ "Access-Control-Allow-Origin", "*" ].
     * @param headerLine single http header line (name and value pair separated by ':')
     * @return parsed key value pair
     */
    public static KeyValue<String, String> parseHeaderLine(final String headerLine) {
        if (headerLine == null) {
            return null;
        }

        final String[] pair = StringUtils.split(headerLine, ":", 2);

        if (pair.length != 2) {
            log.warn("Header line in an invalid form: '{}'.", headerLine);
            return null;
        }

        return new DefaultKeyValue<>(StringUtils.trim(pair[0]), StringUtils.trim(pair[1]));
    }

    /**
     * Parse HTTP Header lines in the format: <code>&lt;name&gt; ":" &lt;value&gt;</code>. Or null if the {@code headerLines}
     * is null.
     * <p>
     * Each item in the {@code headerLines} must in the valid form, like "Access-Control-Allow-Origin: *".
     * @param headerLines http header lines, each of which is a single http header line (name and value pair separated by ':')
     * @return map of parsed key value pairs
     */
    public static Map<String, String> parseHeaderLines(final String[] headerLines) {
        if (headerLines == null) {
            return null;
        }

        final Map<String, String> headerMap = new LinkedHashMap<>();

        for (String headerLine : headerLines) {
            final KeyValue<String, String> kv = parseHeaderLine(headerLine);

            if (kv != null) {
                headerMap.put(kv.getKey(), kv.getValue());
            }
        }

        return headerMap;
    }

}
