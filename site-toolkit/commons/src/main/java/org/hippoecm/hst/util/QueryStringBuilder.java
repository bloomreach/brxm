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
        // Do not encode 'name' if only composed of allowed characters -- from https://tools.ietf.org/html/rfc3986:
        //
        // query      = *( pchar / "/" / "?" )
        // pchar      = unreserved / pct-encoded / sub-delims / ":" / "@"
        // unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
        // sub-delims = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
        //
        // Reason for this exception: we want to keep certain characters decoded (most notably ":" for readability of
        // component rendering urls).

        boolean encode = false;
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);

            // The above rules state that "&" and "=" are allowed chars, but do encode the name if they are in the name
            if (isOneOf(c, "&=") || !isQueryChar(c)) {
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

    private boolean isQueryChar(final char ch) {
        return isPchar(ch) || isOneOf(ch, "/?");
    }

    private boolean isPchar(final char ch) {
        return isUnreserved(ch) || isSubDelim(ch) || isOneOf(ch, ":@");
    }

    private boolean isUnreserved(final char ch) {
        return (ch >= 'a' && ch <='z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || isOneOf(ch, "-._~");
    }

    private boolean isSubDelim(final char ch) {
        return isOneOf(ch, "!$&'()*+,;=");
    }

    private boolean isOneOf(final char ch, final String s) {
        return s.indexOf(ch) != -1;
    }

}
