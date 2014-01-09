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
package org.hippoecm.repository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicAuth {


    private static final String WWW_AUTHENTICATE = "WWW-Authenticate";
    private static final String BASIC_AUTH_PREFIX = "Basic "; // intentional trailing space
    private static final int BASIC_AUTH_PREFIX_LENGTH = BASIC_AUTH_PREFIX.length();

    private static final Logger log = LoggerFactory.getLogger(BasicAuth.class);

    public static boolean hasAuthorizationHeader(HttpServletRequest request) {
        return (getAuthorizationHeader(request) != null);
    }

    public static SimpleCredentials parseAuthorizationHeader(HttpServletRequest request) {
        String authHeader = getAuthorizationHeader(request);
        String decoded = base64DecodeAuthHeader(authHeader);
        String[] creds = getUsernamePasswordFromAuth(decoded);
        return new SimpleCredentials(creds[0], creds[1].toCharArray());
    }

    public static String getAuthorizationHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.length() < BASIC_AUTH_PREFIX_LENGTH) {
            log.info("Authorization header not found.");
            return null;
        }
        return authHeader;
    }

    public static void setRequestAuthorizationHeaders(HttpServletResponse response, String realm) throws IOException {
        response.setHeader(WWW_AUTHENTICATE, "Basic realm=\"" + realm + '"');
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    public static String base64DecodeAuthHeader(String authHeader) {
        if (authHeader == null || authHeader.length() == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Base64.decode(authHeader.substring(BASIC_AUTH_PREFIX_LENGTH), out);
            return new String(out.toByteArray(), "UTF-8");
        } catch (IOException e) {
            log.warn("Unable to decode auth header '" + authHeader + "' : " + e.getMessage());
            log.debug("Decode error:", e);
        }
        return null;
    }

    public static String[] getUsernamePasswordFromAuth(String decoded) {
        int split = decoded.indexOf(':');
        if (split < 1) {
            log.warn("Invalid authorization header found '{}'.", decoded);
            return null;
        }
        return new String[] { decoded.substring(0, split), decoded.substring(split + 1) };
    }
}
