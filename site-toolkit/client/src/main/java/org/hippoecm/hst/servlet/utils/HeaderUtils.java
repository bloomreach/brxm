/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.servlet.utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for checking and setting http headers for BinaryPage binaries.
 * TODO: Default max expires
 */
public final class HeaderUtils {

    private static final Logger log = LoggerFactory.getLogger(HeaderUtils.class);

    private static final long MILLIS_IN_SEC = 1000L;
    /**
     * Hide constructor of utility class
     */
    private HeaderUtils() {
    }

    public static boolean isForcedCheck(HttpServletRequest request) {
        String cacheControl = request.getHeader("Cache-Control");
        if (cacheControl != null && "no-cache".equals(cacheControl)) {
            return true;
        }
        String pragma = request.getHeader("Pragma");
        if (pragma != null && "no-cache".equals(pragma)) {
            return true;
        }
        return false;
    }

    public static boolean hasMatchingEtag(HttpServletRequest request, BinaryPage page) {
        String match = request.getHeader("If-None-Match");
        if (match != null && match.equals(page.getETag())) {
            return true;
        }
        return false;
    }

    public static boolean isModifiedSince(HttpServletRequest request, BinaryPage page) {
        long ifModifiedSince = -1L;

        try {
            ifModifiedSince = request.getDateHeader("If-Modified-Since");
        } catch (IllegalArgumentException ignore) {
            if (log.isWarnEnabled()) {
                log.warn("The header, If-Modified-Since, contains invalid value: "
                        + request.getHeader("If-Modified-Since"));
            }
        }
        if (ifModifiedSince != -1L && ifModifiedSince >= page.getLastModified()) {
            return false;
        }
        return true;
    }

    public static void setLastModifiedHeaders(HttpServletResponse response, BinaryPage page) {
        final long lastModifiedDate = page.getLastModified();
        if (lastModifiedDate > 0) {
            response.setDateHeader("Last-Modified", lastModifiedDate);
            final long age = (System.currentTimeMillis() - page.getCreationTime()) / MILLIS_IN_SEC;
            if (age > 0) {
                response.setHeader("Age", String.valueOf(age));
            }
        }
    }

    public static void setExpiresHeaders(HttpServletResponse response, BinaryPage page) {
        final long lastModifiedDate = page.getLastModified();
        if (lastModifiedDate > 0) {
            final long expires = System.currentTimeMillis() - lastModifiedDate;
            if (expires > 0) {
                response.setDateHeader("Expires", expires + System.currentTimeMillis());
                response.setHeader("Cache-Control", "max-age=" + (expires / MILLIS_IN_SEC));
            }
        }
    }

    public static void setContentLengthHeader(HttpServletResponse response, BinaryPage page) {
        response.setHeader("Content-Length", Long.toString(page.getLength())); 
    }

}