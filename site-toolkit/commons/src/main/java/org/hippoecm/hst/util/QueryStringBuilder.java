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
package org.hippoecm.hst.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Utility class to create a query string by appending name/value pairs. Names and values are URL encoded using the
 * given encoding. {@link #toString()} returns an empty string if no pairs were added or a string starting with a ?
 * followed by the encoded pairs.
 */
public class QueryStringBuilder {

    private final StringBuilder builder = new StringBuilder(100);
    private final String encoding;

    public QueryStringBuilder(final String encoding) {
        this.encoding = encoding;
    }

    public void append(final String name, final String value) throws UnsupportedEncodingException {
        builder.append(builder.length() == 0 ? '?' : '&')
                .append(encodeName(name))
                .append('=')
                .append(URLEncoder.encode(value, encoding));
    }

    public String toString() {
        return builder.toString();
    }

    private String encodeName(final String name) throws UnsupportedEncodingException {
        // do not encode 'name' if only composed of ASCII characters, except if 'name' contains characters
        // that have other meaning in URL parameters: % = & and #
        // reason for this exception: we want to keep certain characters decoded (most notably : for readability of
        // component rendering urls)
        boolean encode = false;
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (c > 127 || c == '%' || c == '=' || c == '&' || c == '#') {
                encode = true;
                break;
            }
        }
        if (encode) {
            return URLEncoder.encode(name, encoding);
        } else {
            return name;
        }
    }

}
