/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.utilities.servlet;

import javax.servlet.http.HttpServletRequest;

import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringBefore;

public class RequestUtils {

    public static final String ORIGIN = "Origin";

    private RequestUtils(){

    }

    /**
     * @param servletRequest
     * @return the origin for {@code servletRequest} or {@code null} in case the {@code servletRequest} does not contain
     * an 'origin' and neither a 'referer' header
     */
    public static String getOrigin(final HttpServletRequest servletRequest) {
        String requestOrigin = servletRequest.getHeader(ORIGIN);
        if (requestOrigin != null) {
            return requestOrigin;
        }

        // if so check the Origin HTTP header and if the Origin header is missing check the referer (Origin misses for
        // CORS or POST requests from firefox, see CMS-12155)
        final String referer = servletRequest.getHeader("Referer");
        if (referer != null) {
            final String scheme = substringBefore(referer, "://");
            // host possibly including port
            final String host = substringBefore(substringAfter(referer,scheme + "://"), "/");
            return scheme + "://" + host;
        }

        return null;
    }
}
