/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.brokenlinks;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.math.NumberUtils;

/**
 * URI Utils for Link Checker
 */
public class LinkURIUtils {

    private static final Pattern HTTP_URI_PARSE_PATTERN =
            Pattern.compile("^(https?)://([^\\/]+)(/[^\\?]+)?(\\?([^#]+))?(#(.+))?$", Pattern.CASE_INSENSITIVE);

    private LinkURIUtils() {
    }

    /**
     * Tries to create a URI instance from the given url string by simply invoking {@link URI#create(String)} method.
     * However, this can give an IllegalArgumentException for a malformed URL, so this utility method tries one more time
     * by parsing the URL string and creates an encoded URI by invoking the <code>java.net.URI</code> constructor directly.
     * <P>
     * <EM>Note: This utility method is useful when you're not sure if the input url string is always valid.</EM>
     * </P>
     *
     * @param url
     * @return
     * @throws IllegalArgumentException
     * @throws URISyntaxException
     */
    public static URI createHttpURIFromString(String url) throws IllegalArgumentException {
        URI uri = null;

        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            Matcher m = HTTP_URI_PARSE_PATTERN.matcher(url);

            if (m.matches()) {
                String scheme = m.group(1);
                String host = m.group(2);
                int port = -1;
                int offset = host.indexOf(':');
                if (offset != -1) {
                    port = NumberUtils.toInt(host.substring(offset + 1), 0);
                    host = host.substring(0, offset);
                }
                String path = m.group(3);
                String query = m.group(5);
                String fragment = m.group(7);

                try {
                    uri = new URI(scheme, null, host, port, path, query, fragment);
                } catch (URISyntaxException se) {
                    throw new IllegalArgumentException(se);
                }
            } else {
                throw e;
            }
        }

        return uri;
    }

}
