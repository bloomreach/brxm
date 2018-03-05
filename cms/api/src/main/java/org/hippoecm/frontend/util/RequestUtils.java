/*
 *  Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.util;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.http.WebRequest;

/**
 * Wicket {@link Request} related utilities.
 */
public class RequestUtils {

    /**
     * Default HTTP Forwarded-For header name. <code>X-Forwarded-For</code> by default.
     */
    public static final String DEFAULT_HTTP_FORWARDED_FOR_HEADER = "X-Forwarded-For";

    /**
     * Array of the default HTTP Forwarded-For header name(s). <code>{ "X-Forwarded-For" }</code> by default.
     */
    private static final String[] DEFAULT_HTTP_FORWARDED_FOR_HEADERS = { DEFAULT_HTTP_FORWARDED_FOR_HEADER };

    /**
     * Servlet context init parameter name for custom HTTP Forwarded-For header name(s).
     * If not set, {@link DEFAULT_HTTP_FORWARDED_FOR_HEADER} is used by default.
     */
    public static final String HTTP_FORWARDED_FOR_HEADER_PARAM = "http-forwarded-for-header";

    /*
     * Package protected for unit tests.
     */
    static String[] httpForwardedForHeaderNames;

    private RequestUtils() {
    }

    /**
     * Returns the remote client address or null if remote client address information is unavailable.
     * @param request wicket request
     * @return the remote client address or null if remote client address information is unavailable
     */
    public static String getFarthestRemoteAddr(final Request request) {
        String [] remoteAddrs = getRemoteAddrs(request);

        if (ArrayUtils.isNotEmpty(remoteAddrs)) {
            return remoteAddrs[0];
        }

        return null;
    }

    /**
     * Returns the remote host addresses related to this request.
     * If there's any proxy server between the client and the server,
     * then the proxy addresses are contained in the returned array.
     * The lowest indexed element is the farthest downstream client and
     * each successive proxy addresses are the next elements.
     * @param request wicket request
     * @return remote host addresses as non-null string array
     */
    public static String [] getRemoteAddrs(final Request request) {
        if (request instanceof WebRequest) {
            WebRequest webRequest = (WebRequest) request;

            String[] headerNames = getForwardedForHeaderName(webRequest);

            for (String headerName : headerNames) {
                String headerValue = webRequest.getHeader(headerName);

                if (headerValue != null && headerValue.length() > 0) {
                    String [] addrs = headerValue.split(",");

                    for (int i = 0; i < addrs.length; i++) {
                        addrs[i] = addrs[i].trim();
                    }

                    return addrs;
                }
            }

            if (webRequest.getContainerRequest() instanceof ServletRequest) {
                final ServletRequest servletRequest = (ServletRequest) webRequest.getContainerRequest();
                return new String [] { servletRequest.getRemoteAddr() };
            }
        }

        return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    public static String getFarthestRequestScheme(HttpServletRequest request) {
        String [] schemes = getCommaSeparatedMultipleHeaderValues(request, "X-Forwarded-Proto");

        if (schemes != null && schemes.length != 0) {
            return schemes[0].toLowerCase();
        }

        schemes = getCommaSeparatedMultipleHeaderValues(request, "X-Forwarded-Scheme");

        if (schemes != null && schemes.length != 0) {
            return schemes[0].toLowerCase();
        }

        String [] sslEnabledArray = getCommaSeparatedMultipleHeaderValues(request, "X-SSL-Enabled");

        if (sslEnabledArray == null) {
            sslEnabledArray = getCommaSeparatedMultipleHeaderValues(request, "Front-End-Https");
        }

        if (sslEnabledArray != null && sslEnabledArray.length != 0) {
            String sslEnabled = sslEnabledArray[0];

            if (sslEnabled.equalsIgnoreCase("on") || sslEnabled.equalsIgnoreCase("yes") || sslEnabled.equals("1")) {
                return "https";
            }
        }

        return request.getScheme();
    }



    /**
     * Parse comma separated multiple header value and return an array if the header exists.
     * If the header doesn't exist, it returns null.
     * @param request
     * @param headerName
     * @return null if the header doesn't exist or an array parsed from the comma separated string header value.
     */
    private static String [] getCommaSeparatedMultipleHeaderValues(final HttpServletRequest request, final String headerName) {
        String value = request.getHeader(headerName);

        if (value == null) {
            return null;
        }

        String [] tokens = value.split(",");

        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].trim();
        }

        return tokens;
    }

    /**
     * Return an array containing only <code>X-Forwarded-For</code> HTTP header name by default or custom equivalent
     * HTTP header names if {@link #HTTP_FORWARDED_FOR_HEADER_PARAM} context parameter is defined to use any other
     * comma separated custom HTTP header names instead.
     * @param request servlet request
     * @return an array containing <code>X-Forwarded-For</code> HTTP header name by default or custom equivalent
     * HTTP header names
     */
    private static String[] getForwardedForHeaderName(final WebRequest request) {
        String[] forwardedForHeaderNames = httpForwardedForHeaderNames;

        if (forwardedForHeaderNames == null) {
            synchronized (RequestUtils.class) {
                forwardedForHeaderNames = httpForwardedForHeaderNames;

                if (forwardedForHeaderNames == null) {
                    if (request.getContainerRequest() instanceof ServletRequest) {
                        String param = ((ServletRequest) request.getContainerRequest()).getServletContext().getInitParameter(HTTP_FORWARDED_FOR_HEADER_PARAM);

                        if (param != null && !param.isEmpty()) {
                            forwardedForHeaderNames = param.split(",");

                            for (int i = 0; i < forwardedForHeaderNames.length; i++) {
                                forwardedForHeaderNames[i] = forwardedForHeaderNames[i].trim();
                            }
                        }
                    }

                    if (forwardedForHeaderNames == null) {
                        forwardedForHeaderNames = DEFAULT_HTTP_FORWARDED_FOR_HEADERS;
                    }

                    httpForwardedForHeaderNames = forwardedForHeaderNames;
                }
            }
        }

        return forwardedForHeaderNames;
    }

    /**
     * Returns the original host information requested by the client
     * @param request
     * @return the farthest request host
     */
    public static String getFarthestRequestHost(HttpServletRequest request) {
        String host = request.getHeader("X-Forwarded-Host");

        if (host != null) {
            String [] hosts = host.split(",");
            return hosts[0].trim();
        }

        host = request.getHeader("Host");

        if (host != null && !"".equals(host)) {
            return host;
        }

        // fallback to request server name for HTTP/1.0 clients.
        // e.g., HTTP/1.0 based browser clients or load balancer not providing 'Host' header.

        int serverPort = request.getServerPort();

        if (serverPort == 80 || serverPort == 443 || serverPort <= 0) {
            host = request.getServerName();
        } else {
            host = request.getServerName() + ":" + serverPort;
        }

        return host;
    }

    public static String getFarthestUrlPrefix(final Request  request) {
        return getFarthestUrlPrefix(((ServletWebRequest)request).getContainerRequest());
    }

    public static String getFarthestUrlPrefix(final HttpServletRequest httpServletRequest) {
        return getFarthestRequestScheme(httpServletRequest) + "://" + getFarthestRequestHost(httpServletRequest);
    }
}
