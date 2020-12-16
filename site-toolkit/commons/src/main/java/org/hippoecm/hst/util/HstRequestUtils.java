/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.jackrabbit.JcrConstants;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.container.security.AccessToken;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.internal.BranchSelectionService;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.onehippo.repository.branch.BranchConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.hippoecm.hst.core.container.ContainerConstants.BR_VERSION_UUID_REQUEST_PARAMETER;
import static org.hippoecm.hst.core.container.ContainerConstants.FORCE_USE_PREFER_RENDER_ATTR_NAME;
import static org.hippoecm.hst.core.container.ContainerConstants.PREFER_RENDER_BRANCH_ID;
import static org.hippoecm.hst.core.container.ContainerConstants.PREVIEW_ACCESS_TOKEN_REQUEST_ATTRIBUTE;
import static org.hippoecm.hst.core.container.ContainerConstants.RENDER_BRANCH_ID;
import static org.hippoecm.hst.site.HstServices.getComponentManager;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

/**
 * HST Request Utils
 *
 * @version $Id$
 */
public class HstRequestUtils {

    private final static Logger log = LoggerFactory.getLogger(HstRequestUtils.class);

    public static final Pattern MATRIX_PARAMS_PATTERN = Pattern.compile(";[^\\/]*");

    public static final String HTTP_METHOD_POST = "POST";
    public static final String ORIGIN = "Origin";

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
     * <p>
     *     Returns the rendering host of the current request, i.e. the host at which the request output is rendered.
     *     The rendering host can be set as a request parameter, or be present in the HTTP session. The request parameter
     *     value has precedence over the value in the HTTP session.
     * </p>
     * <p>
     *     Note that the rendering host for webapps not of type {@link HippoWebappContext.Type#SITE} will always return
     *     {@code null} since the 'rendering host' is only meant for requests that hit an HST Site webapp over the host
     *     of the CMS : These are requests that render a channel in the CMS. Requests hitting the webapp
     *     {@link HippoWebappContext.Type#CMS} always need to get the host information from the {@link HttpServletRequest}
     *     and never from the rendering host name stored in the cross webapp shared {@link CmsSessionContext}
     * </p>
     *
     * @param request the servlet request
     * @return the rendering host for the current request
     */
    public static String getRenderingHost(final HttpServletRequest request) {
        final HippoWebappContext context = HippoWebappContextRegistry.get().getContext(request.getContextPath());
        if (context.getType() != HippoWebappContext.Type.SITE) {
            // only for SITE webapps we support the 'rendering host' to be different than the actual request from the
            // http servlet request
            return null;
        }
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
        String requestParam = request.getParameter(ContainerConstants.RENDERING_HOST);
        if (requestParam != null) {
            return requestParam;
        }

        HttpSession session = request.getSession(false);
        if (session != null) {

            final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(session);
            if (cmsSessionContext != null) {
                final Map<String, Serializable> contextPayload = cmsSessionContext.getContextPayload();
                if (contextPayload == null) {
                    return null;
                }
                return (String) contextPayload.get(ContainerConstants.RENDERING_HOST);
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
            contextPath = mount.getContextPath();
        }
        StringBuilder url = new StringBuilder(scheme).append("://").append(HstRequestUtils.getFarthestRequestHost(request, false))
                .append(contextPath).append(request.getRequestURI().substring(request.getContextPath().length()));
        if (request.getQueryString() != null) {
            url.append("?").append(request.getQueryString());
        }
        return url.toString();
    }

    /**
     * Creates a fully qualified URL with the same scheme and host as the client used, and pathInfo equal to the
     * matched mount : note that this pathInfo for the matched mount does not include the _cmsinternal for preview matched
     * mounts
     */
    public static String createURLForMountPath(final String scheme, final Mount mount, final HttpServletRequest request) {
        String contextPath = "";
        if (mount.isContextPathInUrl()) {
            contextPath = mount.getContextPath();
        }
        return new StringBuilder(scheme).append("://").append(HstRequestUtils.getFarthestRequestHost(request, false))
                .append(contextPath).append(mount.getMountPath()).toString();

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
        return requestContext.isChannelManagerPreviewRequest()
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

    /**
     * <p>
     *     Returns the cms base URL, for example https://cms.example.org or https://cms.example.org/cms. The
     *     {@code cmsHostServletRequest} can be a request to <strong>any</strong> webapp (/site, /intranet, /cms) as long
     *     as the request is using the host name for the cms, eg https://cms.example.org/site. This utility method then
     *     finds out via the hst:platform model whether the context path '/cms' should be present in the base URL or not
     * </p>
     *
     * @param cmsHostServletRequest a request that uses the cms host in the client. Note the request can still be for
     *                              a site webapp to load a site in the channel mgr, however, as long as the request is
     *                              using the hostname that the cms runs on
     * @return
     */
    public static String getCmsBaseURL(final HttpServletRequest cmsHostServletRequest) {
        final String farthestRequestScheme = HstRequestUtils.getFarthestRequestScheme(cmsHostServletRequest);
        final String farthestRequestHost = HstRequestUtils.getFarthestRequestHost(cmsHostServletRequest, false);

        final HstModel platformHstModel = getPlatformHstModel();

        final ResolvedVirtualHost resolvedCmsHost = platformHstModel.getVirtualHosts().matchVirtualHost(farthestRequestHost);
        if (resolvedCmsHost == null) {
            throw new IllegalStateException(String.format("Could not match cms host '%s' in platform hst model", farthestRequestHost));
        }

        final String cmsLocation;
        final VirtualHost cmsVHost = resolvedCmsHost.getVirtualHost();
        if (cmsVHost.isContextPathInUrl() && isNotEmpty(cmsVHost.getContextPath())) {
            cmsLocation = farthestRequestScheme + "://" + farthestRequestHost + cmsVHost.getContextPath();
        } else {
            cmsLocation =  farthestRequestScheme + "://" + farthestRequestHost;
        }
        return cmsLocation;
    }

    public static HstModel getPlatformHstModel() {

        HstModelRegistry hstModelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
        for (HstModel hstModel : hstModelRegistry.getHstModels()) {
            final String contextPath = hstModel.getVirtualHosts().getContextPath();
            final HippoWebappContext context = HippoWebappContextRegistry.get().getContext(contextPath);
            if (context.getType() == HippoWebappContext.Type.CMS || context.getType() == HippoWebappContext.Type.PLATFORM) {
                return hstModel;
            }

        }
        throw new IllegalStateException("Platform hst model is not available");
    }


    public static String getBranchIdFromContext(final HstRequestContext requestContext) {

        if (Boolean.TRUE.equals(requestContext.getAttribute(FORCE_USE_PREFER_RENDER_ATTR_NAME))
                && requestContext.getAttribute(PREFER_RENDER_BRANCH_ID) != null) {
            return (String) requestContext.getAttribute(PREFER_RENDER_BRANCH_ID);
        }

        final Map<HstSite, HstSite> renderMap = (Map<HstSite, HstSite>)requestContext.getAttribute(RENDER_BRANCH_ID);
        if (renderMap == null) {
            return MASTER_BRANCH_ID;
        }

        final HstSite hstSite = requestContext.getResolvedMount().getMount().getHstSite();
        final HstSite renderSite = renderMap.get(hstSite);
        if (renderSite == null || renderSite.getChannel() == null || renderSite.getChannel().getBranchId() == null) {
            return MASTER_BRANCH_ID;
        }
        return renderSite.getChannel().getBranchId();
    }

    /**
     * <p>
     *     This method will almost always return {@code null} except when all below conditions are met: in that case, it
     *     will return the uuid of the frozen node to render instead of the {@code workspaceNode}. Conditions:
     *     <ol>
     *         <li>The request is a channel manager preview (possibly PMA) request</li>
     *         <li>The servlet request contains request parameter {@link ContainerConstants#BR_VERSION_UUID_REQUEST_PARAMETER}</li>
     *         <li>The request parameter value is a UUID pointing to a frozen JCR Node</li>
     *         <li>The JCR Session belonging to {@code workspaceNode} is allowed to read the frozen JCR Node</li>
     *         <li>The branchId of the frozen Node is equal to the branchId of the current {@code requestContext}</li>
     *         <li>The workspace Node identifier for the frozen Node is equal to the identifier of {@code workspaceNode}</li>
     *     </ol>
     * </p>
     * <p>
     *     This method can be used to trigger rendering a versioned document instead of {@code workspaceNode}
     * </p>
     * @param requestContext the request context for the current request
     * @param node the workspaceNode for which instead we might want to render a frozen node from version history
     *                      in case the request parameter {@link ContainerConstants#BR_VERSION_UUID_REQUEST_PARAMETER}
     *                      contains a UUID from a versioned node for which the workspace node is equal to
     *                      {@code workspaceNode}
     * @param contextBranchId The branchId for the {@code requestContext} to render
     * @return the uuid of the frozen node to render instead of {@code workspaceNode} or null if just the workspaceNode
     *         should be used
     */
    public static String getRenderFrozenNodeId(final HstRequestContext requestContext, final Node node,
                                               final String contextBranchId) {

        if (!requestContext.isChannelManagerPreviewRequest()) {
            return null;
        }

        final String br_version_uuid = requestContext.getServletRequest().getParameter(BR_VERSION_UUID_REQUEST_PARAMETER);
        if (br_version_uuid == null) {
            return null;
        }

        try {

            if (node.getIdentifier().equals(br_version_uuid)) {
                return br_version_uuid;
            }

            final Node frozenNode = node.getSession().getNodeByIdentifier(br_version_uuid);
            if (!frozenNode.isNodeType(JcrConstants.NT_FROZENNODE)) {
                return null;
            }

            final String branchIdOfNode = JcrUtils.getStringProperty(frozenNode, HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID);
            if (!branchIdOfNode.equals(contextBranchId)) {
                return null;
            }

            final Node version = frozenNode.getParent();

            if (!(version instanceof Version)) {
                return null;
            }

            final String workspaceIdentifier = ((Version) version).getContainingHistory().getVersionableIdentifier();

            if (node.getIdentifier().equals(workspaceIdentifier)) {
                return br_version_uuid;
            }

            return null;
        } catch (ItemNotFoundException e) {
            log.debug("Node '{}' for request param is not found '{}'", br_version_uuid, BR_VERSION_UUID_REQUEST_PARAMETER);
            return null;
        } catch (RepositoryException e) {
            log.debug("Repository Exception while checking which version history to render", e);
            return null;
        }

    }

    /**
     * <p>
     *     If present for this request, returns the cluster node affinity id for cookie
     *     {@code clusterNodeAffinityCookieName}, which is typically 'serverid'. The {@code clusterNodeAffinityCookieName}
     *     lookup is done from a cookie and if not found, the header clusterNodeAffinityHeaderName will be checked.
     *     The following logic is applied
     *     <ul>
     *         <li>if cookie present with case-insensitive name {@code clusterNodeAffinityCookieName}, return the cookie value</li>
     *         <li>else if the (case insensitive by spec) header '{@code clusterNodeAffinityHeaderName} exists, return the
     *         value of the header</li>
     *         <li>else return {@code null}</li>
     *     </ul>
     * </p>
     * @param request the {@link HttpServletRequest} to find the server id from
     */
    public static String getClusterNodeAffinityId(final HttpServletRequest request, final String clusterNodeAffinityCookieName,
                                                  final String clusterNodeAffinityHeaderName) {
        final Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<String> serverId = Arrays.stream(cookies).filter(cookie -> clusterNodeAffinityCookieName.equalsIgnoreCase(cookie.getName()))
                    .map(cookie -> cookie.getValue()).findFirst();
            if (serverId.isPresent()) {
                return serverId.get();
            }
        }
        return request.getHeader(clusterNodeAffinityHeaderName);

    }

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

        // fallback to request host
        final String farthestRequestHost = HstRequestUtils.getFarthestRequestHost(servletRequest, false);
        final String farthestRequestScheme = HstRequestUtils.getFarthestRequestScheme(servletRequest);
        return farthestRequestScheme + "://" + farthestRequestHost;

    }

    /**
     * <p>
     *     If the {@code request} is tied to a {@link CmsSessionContext}, this method returns that {@link CmsSessionContext}.
     *     Note that the {@code request} can be tied to in two different ways to a {@link CmsSessionContext}:
     *     <ol>
     *         <li>If the request has a PREVIEW_ACCESS_TOKEN_REQUEST_ATTRIBUTE attribute, then the
     *            {@link CmsSessionContext} from the token is returned
     *         </li>
     *         <li>
     *             If the request has an {@link HttpSession}, then the {@link CmsSessionContext} linked to that http
     *             session is returned of present
     *         </li>
     *     </ol>
     * </p>
     * @param request the {@link HttpServletRequest} to return the {@link CmsSessionContext} for if present
     * @return the {@link CmsSessionContext} for the {@code request} if present and otherwise returns {@code null}
     */
    public static CmsSessionContext getCmsSessionContext(final HttpServletRequest request) {
        Object token = request.getAttribute(PREVIEW_ACCESS_TOKEN_REQUEST_ATTRIBUTE);
        if (token == null) {
            final HttpSession session = request.getSession(false);
            if (session == null) {
                return null;
            }

            return CmsSessionContext.getContext(session);
        } else {
            // token based rendering for preview
            if (token instanceof AccessToken) {
                return ((AccessToken) token).getCmsSessionContext();
            } else {
                throw new IllegalStateException(String.format("For attribute '%s' only an object of type '%s' " +
                        "is allowed", PREVIEW_ACCESS_TOKEN_REQUEST_ATTRIBUTE, AccessToken.class.getName()));
            }
        }
    }

    /**
     * <p>
     *     If for {@code request} there is a CmsSessionContext (tied to the http Session for {@code request}, the stored
     *     branchId on the cms session context payload is returned, and if no specific branchId info stored,
     *     {@link BranchConstants#MASTER_BRANCH_ID} is returned
     * </p>
     * @param request the current http servlet request
     * @return the selected branch Id on the CmsSessionContext and when not found returns {@link BranchConstants#MASTER_BRANCH_ID}
     */
    public static String getCmsSessionActiveBranchId(final HttpServletRequest request) {
        final CmsSessionContext cmsSessionContext = getCmsSessionContext(request);
        if (cmsSessionContext == null) {
            return BranchConstants.MASTER_BRANCH_ID;
        }
        final BranchSelectionService branchSelectionService = HippoServiceRegistry.getService(BranchSelectionService.class);
        if (branchSelectionService == null) {
            // there is no branchSelectionService registered which can happen if there is no page-composer jar deployed
            // in case of hst + repo deployments without cms (and no need for page-composer)
            return BranchConstants.MASTER_BRANCH_ID;
        }
        final String branchId = branchSelectionService.getSelectedBranchId(cmsSessionContext.getContextPayload());
        if (branchId == null) {
            return BranchConstants.MASTER_BRANCH_ID;
        }
        return branchId;
    }
}
