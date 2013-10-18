/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.util;

import javax.servlet.http.HttpServletRequest;

/**
 * Utilities for retrieving information from the HttpServletRequest.
 *
 * @version $Id: $
 */
public class HttpRequestUtils {

    private HttpRequestUtils() {
    }

    /**
     * Returns the original host information requested by the client.
     *
     * @param request from a client.
     * @return farthest of all hosts in the request.
     */
    public static String getFarthestRequestHost(HttpServletRequest request) {
        return getRequestHosts(request)[0];
    }

    /**
     * Returns HTTP/1.1 compatible 'Host' header value.
     *
     * @param request from a client.
     * @return list of all request host.
     */
    public static String[] getRequestHosts(HttpServletRequest request) {
        return getRequestHosts(request, getXForwardedHost(request), getRequestServerPort(request));
    }

    public static String[] getRequestHosts(HttpServletRequest request, int serverPort) {
        return getRequestHosts(request, getXForwardedHost(request), serverPort);
    }

    public static String[] getRequestHosts(HttpServletRequest request, String host, int serverPort) {
        if (host != null) {
            String[] hosts = host.split(",");

            for (int i = 0; i < hosts.length; i++) {
                hosts[i] = hosts[i].trim();
            }

            return hosts;
        }

        host = request.getHeader("Host");

        if (host != null && !"".equals(host)) {
            return new String[]{host};
        }

        // fallback to request server name for HTTP/1.0 clients.
        // e.g., HTTP/1.0 based browser clients or load balancer not providing 'Host' header.
        if (serverPort == 80 || serverPort == 443 || serverPort <= 0) {
            host = request.getServerName();
        } else {
            host = request.getServerName() + ":" + serverPort;
        }

        return new String[]{host};

    }

    private static String getXForwardedHost(final HttpServletRequest request) {
        return request.getHeader("X-Forwarded-Host");
    }

    public static int getRequestServerPort(final HttpServletRequest request) {
        return request.getServerPort();
    }

}
