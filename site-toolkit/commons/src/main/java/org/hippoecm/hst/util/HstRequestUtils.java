/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.net.URI;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

import static org.hippoecm.hst.site.HstServices.getComponentManager;

/**
 * HST Request Utils
 *
 * @version $Id$
 */
public class HstRequestUtils {

    public static final Pattern MATRIX_PARAMS_PATTERN = Pattern.compile(";[^\\/]*");

    public static final String HTTP_METHOD_POST = "POST";

    public static String URI_ENCODING_DEFAULT_CHARSET_KEY = "uriencoding.default.charset";
    public static String URI_ENCODING_DEFAULT_CHARSET_VALUE = "UTF-8";
    public static String URI_ENCODING_USE_BODY_CHARSET_KEY = "uriencoding.use.body.charset";
    public static boolean URI_ENCODING_USE_BODY_CHARSET_VALUE = false;

    /**
     * Default HTTP Forwarded-For header name. <code>X-Forwarded-For</code> by default.
     */
    public static final String DEFAULT_HTTP_FORWARDED_FOR_HEADER = "X-Forwarded-For";

    /**
     * Array of the default HTTP Forwarded-For header name(s). <code>{ "X-Forwarded-For" }</code> by default.
     */
    private static final String[] DEFAULT_HTTP_FORWARDED_FOR_HEADERS = { DEFAULT_HTTP_FORWARDED_FOR_HEADER };

    /**
     * Servlet context init parameter name for custom HTTP Forwarded-For header name.
     * If not set, {@link #DEFAULT_HTTP_FORWARDED_FOR_HEADER} is used by default.
     */
    public static final String HTTP_FORWARDED_FOR_HEADER_PARAM = "http-forwarded-for-header";

    /*
     * Package protected for unit tests.
     */
    static String[] httpForwardedForHeaderNames;

    private HstRequestUtils() {

    }

    /**
     * Returns <CODE>HstRequest</CODE> object found in the servletRequest.
     * @param servletRequest
     * @return
     */
    public static HstRequest getHstRequest(HttpServletRequest servletRequest) {
        HstRequest hstRequest = (HstRequest) servletRequest.getAttribute(ContainerConstants.HST_REQUEST);

        if (hstRequest == null && servletRequest instanceof HstRequest) {
            hstRequest = (HstRequest) servletRequest;
        }

        return hstRequest;
    }

    /**
     * Returns <CODE>HstResponse</CODE> object found in the servletRequest or servletResponse.
     * @param servletRequest
     * @param servletResponse
     * @return
     */
    public static HstResponse getHstResponse(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        HstResponse hstResponse = (HstResponse) servletRequest.getAttribute(ContainerConstants.HST_RESPONSE);

        if (hstResponse == null && servletResponse instanceof HstResponse) {
            hstResponse = (HstResponse) servletResponse;
        }

        return hstResponse;
    }

    /**
     * Returns <CODE>HstRequestContext</CODE> object found in the servletRequest.
     * @param servletRequest
     * @return
     */
    public static HstRequestContext getHstRequestContext(HttpServletRequest servletRequest) {
        return (HstRequestContext)servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
    }

    /**
     * @param request
     * @param excludeMatrixParameters
     * @return
     */
    public static String getRequestURI(HttpServletRequest request, boolean excludeMatrixParameters) {
        String requestURI = request.getRequestURI();

        if (excludeMatrixParameters) {
            return removeAllMatrixParams(requestURI);
        }

        return requestURI;
    }

    /**
     * @param request
     * @return the decoded getRequestURI after the context path but before the matrix parameters or the query string in the request URL
     */
    public static String getRequestPath(HttpServletRequest request)  {
        return getDecodedPath(request, getURIEncoding(request));
    }

    /**
     * @param request
     * @param encoding
     * @return the decoded getRequestURI after the context path but before the matrix parameters or the query string in the request URL
     * @deprecated since CMS 12.0, not used in the Hippo stack, use {@link #getRequestPath(HttpServletRequest)}
     */
    @Deprecated
    public static String getRequestPath(HttpServletRequest request, String encoding) {
        return getDecodedPath(request, encoding);
    }

    private static String getDecodedPath(HttpServletRequest request, String encoding) {
        String requestURI = getRequestURI(request, true);
        String encodePathInfo = requestURI.substring(request.getContextPath().length());

        try {
            return URLDecoder.decode(encodePathInfo, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Invalid character encoding: " + encoding, e);
        }

    }

    /**
     * @deprecated since CMS 7.9.1 (2.28.05) use {@link javax.servlet.http.HttpServletRequest#getPathInfo()} instead
     */
    @Deprecated
    public static String getPathInfo(ResolvedMount mount, HttpServletRequest request) {
        return request.getPathInfo();
    }

    /**
     * @deprecated since CMS 7.9.1 (2.28.05) use {@link javax.servlet.http.HttpServletRequest#getPathInfo()} instead
     */
    @Deprecated
    public static String getPathInfo(ResolvedMount mount, HttpServletRequest request, String characterEncoding) {
        return request.getPathInfo();
    }

    /**
     * Returns HTTP/1.1 compatible 'Host' header value.
     * @param request
     * @param checkRenderHost
     * @return
     */
    public static String [] getRequestHosts(HttpServletRequest request, boolean checkRenderHost) {
        String host = null;

        if (checkRenderHost) {
            host = getRenderingHost(request);
        }

        if (host == null) {
            host = request.getHeader("X-Forwarded-Host");
        }

        if (host != null) {
            String [] hosts = host.split(",");

            for (int i = 0; i < hosts.length; i++) {
                hosts[i] = hosts[i].trim();
            }

            return hosts;
        }

        host = request.getHeader("Host");

        if (host != null && !"".equals(host)) {
            return new String [] { host };
        }

        // fallback to request server name for HTTP/1.0 clients.
        // e.g., HTTP/1.0 based browser clients or load balancer not providing 'Host' header.

        int serverPort = request.getServerPort();

        // in case this utility method is invoked by a component, for some reason, ...
        HstRequest hstRequest = getHstRequest(request);
        if (hstRequest != null) {
            Mount mount = hstRequest.getRequestContext().getResolvedMount().getMount();

            if (mount.isPortInUrl()) {
                serverPort = mount.getPort();
            } else {
                serverPort = 0;
            }
        }

        if (serverPort == 80 || serverPort == 443 || serverPort <= 0) {
            host = request.getServerName();
        } else {
            host = request.getServerName() + ":" + serverPort;
        }

        return new String[] { host };
    }

    /**
     * Returns the rendering host of the current request, i.e. the host at which the request output is rendered.
     * The rendering host can be set as a request parameter, or be present in the HTTP session. The request parameter
     * value has precedence over the value in the HTTP session.
     *
     * @param request the servlet request
     * @return the rendering host for the current request
     */
    public static String getRenderingHost(final HttpServletRequest request) {
        String hostName = getRenderingHostName(request);
        if (hostName == null) {
            return null;
        }
        if (!hostName.contains(":")) {
            // the rendering host does not contain a portnumber. Use the portnumber of the hostname
            // that the request was done with
            String farthestHostName = getFarthestRequestHost(request, false);
            if (farthestHostName.contains(":")) {
                int portNumber = Integer.parseInt(farthestHostName.substring(farthestHostName.indexOf(":")+1));
                if (portNumber != 80 && portNumber != 443) {
                    hostName += ":" + portNumber;
                }
            }
        }
        return hostName;
    }

    private static String getRenderingHostName(final HttpServletRequest request) {
        String forceClientHost = request.getParameter("Force-Client-Host");
        if (forceClientHost == null) {
            forceClientHost = request.getHeader("Force-Client-Host");
        }
        if (Boolean.parseBoolean(forceClientHost) == Boolean.TRUE) {
            return null;
        }

        String requestParam = request.getParameter(ContainerConstants.RENDERING_HOST);
        if (requestParam != null) {
            return requestParam;
        }

        HttpSession session = request.getSession(false);
        if (session != null) {

            // TODO HSTTWO-4374 can we share this information cleaner between platform webapp and site webapps?
            CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(session);
            if (cmsSessionContext != null) {
                return (String) cmsSessionContext.getContextPayload().get(ContainerConstants.RENDERING_HOST);
            }
        }
        return null;
    }

    /**
     * Returns the original host informations requested by the client or the proxies
     * in the Host HTTP request headers.
     * @param request
     * @return
     */
    public static String [] getRequestHosts(HttpServletRequest request) {
        return getRequestHosts(request, true);
    }

    /**
     * Returns the original host information requested by the client.
     * @param request
     * @return
     */
    public static String getFarthestRequestHost(HttpServletRequest request) {
        return getRequestHosts(request)[0];
    }

    /**
     * Returns the original host information requested by the client and do check optional
     * injected render host information only when <code>checkRenderHost</code> is <code>true</code>
     * @param request
     * @param checkRenderHost when <code>true</code> the optional render host is used when present.
     * @return the farthest request host or option render host when <code>checkRenderHost</code> is <code>true</code>
     */
    public static String getFarthestRequestHost(HttpServletRequest request, boolean checkRenderHost) {
        return getRequestHosts(request, checkRenderHost)[0];
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
     * Returns the original host's server name requested by the client.
     * @param request
     * @return
     */
    public static String getRequestServerName(HttpServletRequest request) {
        String requestHost = getFarthestRequestHost(request);

        if (requestHost == null) {
            return request.getServerName();
        }

        int offset = requestHost.indexOf(':');

        if (offset != -1) {
            return requestHost.substring(0, offset);
        } else {
            return requestHost;
        }
    }

    /**
     * Returns the original host' port number requested by the client.
     * @param request
     * @return
     */
    public static int getRequestServerPort(HttpServletRequest request) {
        String requestHost = getFarthestRequestHost(request);

        if (requestHost == null) {
            return request.getServerPort();
        }

        int offset = requestHost.lastIndexOf(':');

        if (offset != -1) {
            return Integer.parseInt(requestHost.substring(offset + 1));
        } else {
            return ("https".equals(request.getScheme()) ? 443 : 80);
        }
    }

    /**
     * Returns the remote host addresses related to this request.
     * If there's any proxy server between the client and the server,
     * then the proxy addresses are contained in the returned array.
     * The lowest indexed element is the farthest downstream client and
     * each successive proxy addresses are the next elements.
     * @param request
     * @return
     */
    public static String [] getRemoteAddrs(HttpServletRequest request) {
        String [] headerNames = getForwardedForHeaderNames(request);

        for (String headerName : headerNames) {
            String headerValue = request.getHeader(headerName);

            if (headerValue != null && headerValue.length() > 0) {
                String [] addrs = headerValue.split(",");

                for (int i = 0; i < addrs.length; i++) {
                    addrs[i] = addrs[i].trim();
                }

                return addrs;
            }
        }

        return new String [] { request.getRemoteAddr() };
    }

    /**
     * Returns the remote client address.
     * @param request
     * @return
     */
    public static String getFarthestRemoteAddr(HttpServletRequest request) {
        return getRemoteAddrs(request)[0];
    }

    /**
     * Returns the name of the character encoding used in the body of the servlet request.
     * This method returns <code>ISO-8859-1</code> instead of null if the request does not specify a character encoding
     * because the Servlet specification requires that an encoding of ISO-8859-1 is used if a character encoding is not specified.
     * @param request
     * @return
     */
    public static String getCharacterEncoding(HttpServletRequest request) {
        return getCharacterEncoding(request, "ISO-8859-1");
    }

    /**
     * Returns the name of the character encoding used in the body of the servlet request.
     * This method returns <code>defaultEncoding</code> instead of null if the request does not specify a character encoding.
     * @param request
     * @param defaultEncoding
     * @return
     */
    public static String getCharacterEncoding(HttpServletRequest request, String defaultEncoding) {
        String encoding = request.getCharacterEncoding();

        if (encoding != null) {
            return encoding;
        }

        return defaultEncoding;
    }

    /**
     * Returns the name of the character encoding used for encoding the requests URI.
     * This method uses the hst configuration parameters {@link #URI_ENCODING_DEFAULT_CHARSET_KEY} and
     * {@link #URI_ENCODING_USE_BODY_CHARSET_KEY} to resolve the character encoding with the following logic:
     * <ul>
     *     <li>if URI_ENCODING_USE_BODY_CHARSET_KEY == false, return URI_ENCODING_DEFAULT_CHARSET_KEY</li>
     *     <li>else, if the request specifies a character encoding, return that character encoding</li>
     *     <li>else, return URI_ENCODING_DEFAULT_CHARSET_KEY</li>
     * </ul>
     * @param request
     * @return
     */
    public static String getURIEncoding(final HttpServletRequest request) {
        final String defaultEncoding;
        final boolean useBodyCharset;

        if (!HstServices.isAvailable()) {
            // we're running in a (simple) test setup
            defaultEncoding = URI_ENCODING_DEFAULT_CHARSET_VALUE;
            useBodyCharset = URI_ENCODING_USE_BODY_CHARSET_VALUE;
        } else {
            defaultEncoding = getComponentManager().getContainerConfiguration().getString(
                    URI_ENCODING_DEFAULT_CHARSET_KEY, URI_ENCODING_DEFAULT_CHARSET_VALUE);
            useBodyCharset = getComponentManager().getContainerConfiguration().getBoolean(
                    URI_ENCODING_USE_BODY_CHARSET_KEY, URI_ENCODING_USE_BODY_CHARSET_VALUE);
        }

        if (useBodyCharset) {
            return getCharacterEncoding(request, defaultEncoding);
        } else {
            return defaultEncoding;
        }
    }

    public static Map<String, String []> parseQueryString(HttpServletRequest request) throws UnsupportedEncodingException {
        return parseQueryString(request.getQueryString(), getURIEncoding(request));
    }

    public static Map<String, String []> parseQueryString(URI uri, String encoding) throws UnsupportedEncodingException {
        return parseQueryString(uri.getRawQuery(), encoding);
    }

    private static Map<String, String []> parseQueryString(String queryString, String encoding) throws UnsupportedEncodingException {
        if (queryString == null) {
            return Collections.emptyMap();
        }

        // keep insertion ordered map to maintain the order of the querystring when re-constructing it from a map
        Map<String, String []> queryParamMap = new LinkedHashMap<>();

        String[] paramPairs = queryString.split("&");
        String paramName;
        String paramValue;
        String [] paramValues;
        String [] tempValues;

        for (String paramPair : paramPairs) {
            String[] paramNameAndValue = paramPair.split("=");

            if (paramNameAndValue.length > 1) {
                paramName = URLDecoder.decode(paramNameAndValue[0], encoding);
                paramValue = URLDecoder.decode(paramNameAndValue[1], encoding);

                paramValues = queryParamMap.get(paramName);

                if (paramValues == null) {
                    queryParamMap.put(paramName, new String[] { paramValue });
                } else {
                    tempValues = new String[paramValues.length + 1];
                    System.arraycopy(paramValues, 0, tempValues, 0, paramValues.length);
                    tempValues[paramValues.length] = paramValue;
                    queryParamMap.put(paramName, tempValues);
                }
            }
        }

        return queryParamMap;
    }

    public static String removeAllMatrixParams(String uri) {
        Matcher matcher = MATRIX_PARAMS_PATTERN.matcher(uri);
        return matcher.replaceAll("");
    }

    /**
     * Creates the same fully qualified URL as the client used to make the request, only force the scheme of the URL to be equal
     * to the <code>scheme</code> parameter instead of the scheme of the <code>request</code>.
     */
    public static String createURLWithExplicitSchemeForRequest(final String scheme, final Mount mount, final HttpServletRequest request) {
        String contextPath = "";
        if (mount.isContextPathInUrl()) {
            if (mount.getContextPath() == null) {
                // contextPath agnostic, use context path from servlet request
                contextPath = request.getContextPath();
            } else {
                contextPath = mount.getContextPath();
            }
        }
        StringBuilder url = new StringBuilder(scheme).append("://").append(HstRequestUtils.getFarthestRequestHost(request, false))
                .append(contextPath).append(request.getRequestURI().substring(request.getContextPath().length()));
        if (request.getQueryString() != null) {
            url.append("?").append(request.getQueryString());
        }
        return url.toString();
    }

    /**
     * Returns a fully qualified String url for the {@link HstURL} <code>hstUrl</code>. As scheme for the created url,
     * always the scheme of the current (farthest) request is taken, as a hstUrl can never have a different scheme than the
     * request that was used to create the hstUrl
     */
    public static String getFullyQualifiedHstURL(HstRequestContext requestContext, HstURL hstUrl, boolean escapeXml) {
        StringBuilder urlBuilder = new StringBuilder(80);
        final String scheme = HstRequestUtils.getFarthestRequestScheme(requestContext.getServletRequest());
        final Mount mount = requestContext.getResolvedMount().getMount();
        // When 0, the Mount is port agnostic. Then take port from current container url
        int port = (mount.getPort() == 0 ? requestContext.getBaseURL().getPortNumber() : mount.getPort());
        if (!mount.isPortInUrl() || ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
            urlBuilder.append(scheme).append("://").append(mount.getVirtualHost().getHostName());
        } else {
            urlBuilder.append(scheme).append("://").append(mount.getVirtualHost().getHostName()).append(':').append(port);
        }
        if (escapeXml) {
            urlBuilder.append(escapeXml(hstUrl.toString()));
        } else {
            urlBuilder.append(hstUrl.toString());
        }

        return urlBuilder.toString();
    }

    /**
     * Replaces in String str the characters &,>,<,",'
     * with their corresponding character entity codes.
     * @param str - the String where to replace
     * @return String
     *
     */
    public static String escapeXml(String str) {
        if((str == null) || (str.length() == 0)) {
            return str;
        }
        str = str.replaceAll("&", "&amp;");
        str = str.replaceAll("<", "&lt;");
        str = str.replaceAll(">", "&gt;");
        str = str.replaceAll("\"", "&#034;");
        str = str.replaceAll("'", "&#039;");
        return str;
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
     * @return <code>true</code> if the request is a preview component rendering request from channel manager in cms
     */
    public static boolean isComponentRenderingPreviewRequest(final HstRequestContext requestContext) {
        return requestContext.isCmsRequest()
                && HTTP_METHOD_POST.equals(requestContext.getServletRequest().getMethod())
                && requestContext.getBaseURL().getComponentRenderingWindowReferenceNamespace() != null;
    }

    /**
     * Returns the request URL as seen by, for example, a browser. The returned URL consists of:
     * <ol>
     *     <li>the base URL
     * as returned by {@link org.hippoecm.hst.configuration.hosting.VirtualHost#getBaseURL(javax.servlet.http.HttpServletRequest)}</li>
     *    <li>the context path,
     * if that is configured in the HST to be visible (as determined by {@link org.hippoecm.hst.configuration.hosting.VirtualHost#isContextPathInUrl()})</li>
     * <li>the path of the URL</li>
     * <li>optionally based on flag {@code includeQueryString}, the queryString is added</li>
     * </ol>
     *
     * @param request            the HTTP servlet request
     * @param includeQueryString whether to include the queryString as seen by the browser
     * @return the external request URL as seen by the browser (without encoding)
     */
    public static String getExternalRequestUrl(final HttpServletRequest request, final boolean includeQueryString) {
        final HstRequestContext context = getHstRequestContext(request);
        if (context == null) {
            // no context (e.g. in unit tests), simply return the request URL
            if (includeQueryString) {
                return request.getRequestURL().toString() + "?" + request.getQueryString();
            } else {
                return request.getRequestURL().toString();
            }
        }

        final VirtualHost virtualHost = context.getVirtualHost();
        final StringBuilder url = new StringBuilder(virtualHost.getBaseURL(request));

        if (virtualHost.isContextPathInUrl()) {
            url.append(request.getContextPath());
        }
        url.append(context.getBaseURL().getRequestPath());
        if (includeQueryString && request.getQueryString() != null) {
            url.append("?").append(request.getQueryString());
        }
        return url.toString();
    }

    /**
     * Return an array containing only <code>X-Forwarded-For</code> HTTP header name by default or custom equivalent
     * HTTP header names if {@link #HTTP_FORWARDED_FOR_HEADER_PARAM} context parameter is defined to use any other
     * comma separated custom HTTP header names instead.
     * @param request servlet request
     * @return an array containing <code>X-Forwarded-For</code> HTTP header name by default or custom equivalent
     * HTTP header names
     */
    private static String[] getForwardedForHeaderNames(final HttpServletRequest request) {
        String[] forwardedForHeaderNames = httpForwardedForHeaderNames;

        if (forwardedForHeaderNames == null) {
            synchronized (HstRequestUtils.class) {
                forwardedForHeaderNames = httpForwardedForHeaderNames;

                if (forwardedForHeaderNames == null) {
                    String param = request.getServletContext().getInitParameter(HTTP_FORWARDED_FOR_HEADER_PARAM);

                    if (param != null && !param.isEmpty()) {
                        forwardedForHeaderNames = param.split(",");

                        for (int i = 0; i < forwardedForHeaderNames.length; i++) {
                            forwardedForHeaderNames[i] = forwardedForHeaderNames[i].trim();
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

}
