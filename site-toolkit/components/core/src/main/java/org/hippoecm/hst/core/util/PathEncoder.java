/*
 *  Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.core.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang3.StringUtils;

public class PathEncoder {

    private PathEncoder() {}

    public static String encode(final String pathInfo,
                                final String characterEncoding) throws UnsupportedEncodingException {
        return encode(pathInfo, characterEncoding, null);
    }

    public static String encode(final String pathInfo,
                                final String characterEncoding,
                                final String[] ignorePrefixes) throws UnsupportedEncodingException {
        if (pathInfo == null) {
            return null;
        }
        final StringBuilder builder = new StringBuilder();
        String pathInfoToEncode = pathInfo;
        if (ignorePrefixes != null) {
            for (String ignorePrefix : ignorePrefixes) {
                if (pathInfo.startsWith(ignorePrefix)) {
                    builder.append(ignorePrefix);
                    pathInfoToEncode = pathInfoToEncode.substring(ignorePrefix.length());
                    break;
                }
            }
        }
        final String[] unencodedPaths = StringUtils.splitPreserveAllTokens(pathInfoToEncode, '/');
        for (int i = 0; i < unencodedPaths.length; i++) {
            String path = unencodedPaths[i];
            // check if we have an anchor link and encode everything behind it, but leave first part as it is:
            if (path.indexOf('#') != -1) {
                String[] hashParts = StringUtils.splitPreserveAllTokens(path, '#');

                String afterHash = "";
                if (hashParts.length > 2) {
                    for (int j = 1; j < hashParts.length; j++) {
                        afterHash += hashParts[j];
                        if (j < hashParts.length - 1) {
                            afterHash += "#";
                        }
                    }
                } else {
                    afterHash = hashParts[1];
                }
                // check if preceded with query
                if (hashParts[0].indexOf('?') != -1) {
                    String[] parameterParts = StringUtils.splitPreserveAllTokens(hashParts[0], '?');
                    builder.append(URLEncoder.encode(parameterParts[0], characterEncoding))
                            .append('?').append(parameterParts[1])
                            .append('#').append(URLEncoder.encode(afterHash, characterEncoding));
                } else {
                    builder.append(URLEncoder.encode(hashParts[0], characterEncoding)).append('#').append(URLEncoder.encode(afterHash, characterEncoding));
                }
            }
            // check query parameters:
            else if (path.indexOf('?') != -1) {
                String[] parameterParts = StringUtils.splitPreserveAllTokens(path, '?');
                builder.append(URLEncoder.encode(parameterParts[0], characterEncoding)).append('?').append(parameterParts[1]);
            } else {
                builder.append(URLEncoder.encode(path, characterEncoding));
            }

            if (i != unencodedPaths.length - 1) {
                // check in if above is to avoid trailing slash being added
                builder.append('/');
            }
        }
        return builder.toString();
    }
}
