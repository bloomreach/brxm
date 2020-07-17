/*
 *  Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toSet;

public class ProxyFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ProxyFilter.class);

    private static final String PROXIES_SYSTEM_PROPERTY = "resource.proxies";
    private static final String PROXY_ENABLED_MESSAGE = "Resource proxy enabled with the following mapping(s)";

    /**
     * See http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html.
     * Location and cookie headers are marked as ignore headers as well because the proxy does not (yet) do anything
     * special with them.
     */
    private static final Set<String> IGNORED_RESPONSE_HEADERS = Stream.of(
            "Connection",
            "Keep-Alive",
            "Proxy-Authenticate",
            "Proxy-Authorization",
            "TE",
            "Trailers",
            "Transfer-Encoding",
            "Upgrade",
            "Location",
            "Set-Cookie",
            "Set-Cookie2"
    ).collect(toSet());

    private static final Set<String> IGNORED_REQUEST_HEADERS = Stream.of(
            "Cookie",
            "Host"
    ).collect(toSet());

    private Set<ProxyConfig> proxies;

    @Override
    public void init(final FilterConfig filterConfig) {
        final String parameterValue = filterConfig.getInitParameter("jarPathPrefix");
        final String jarPathPrefix = parameterValue == null ? "/META-INF" : parameterValue;
        final String proxiesAsString = System.getProperty(PROXIES_SYSTEM_PROPERTY);
        proxies = ProxyConfigReader.getProxies(jarPathPrefix, proxiesAsString);

        if (!proxies.isEmpty()) {
            final List<String> messages = proxies.stream().map(ProxyConfig::getLabel).collect(Collectors.toList());
            messages.add(0, PROXY_ENABLED_MESSAGE);
            logBorderedMessage(messages);
        }
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        final HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        final String resourcePath = StringUtils.substringBefore(httpServletRequest.getPathInfo(), ";");
        if (resourcePath == null) {
            log.debug("Resource path is null, passing through request");
            chain.doFilter(request, response);
            return;
        }

        final ProxyConfig proxy = proxies.stream()
                .filter(proxyConfig -> resourcePath.startsWith(proxyConfig.getFrom()))
                .findFirst()
                .orElse(null);

        if (proxy == null) {
            log.debug("No proxy starts with resource path {}, passing through request", resourcePath);
            chain.doFilter(request, response);
            return;
        }

        final String query = httpServletRequest.getQueryString();
        final String queryParams = StringUtils.isEmpty(query) ? "" : "?" + query;
        final String url = proxy.getUrl(resourcePath, queryParams);
        final URL resource = new URL(addIndexHtmlIfNeeded(url));

        final HttpURLConnection urlConnection;
        try {
            urlConnection = (HttpURLConnection) resource.openConnection();
            copyRequestHeaders(httpServletRequest, urlConnection);
            urlConnection.connect();
        } catch (final IOException e) {
            if (proxy.isConnected()) {
                proxy.setConnected(false);
                log.info("Proxy connection {} lost", proxy.getLabel());
            }

            log.debug("Proxy end-point refused a connection for url {}, passing through request", url);
            chain.doFilter(request, response);
            return;
        }

        if (!proxy.isConnected()) {
            proxy.setConnected(true);
            log.info("Proxy connection {} established", proxy.getLabel());
        }

        log.debug("Proxying {} to {}", resourcePath, url);
        proxy(httpServletRequest, httpServletResponse, urlConnection);
    }

    @Override
    public void destroy() {
        proxies = null;
    }

    /**
     * Due to the pattern matching of resourcePaths, a resource path must end with a resource. A resource consists of a
     * name part, and a suffix (e.g. html or jpeg) separated by a dot. The default convention for java servlets is that
     * requesting a root path it is mapped to a resource configured in the welcome file list. The first resource that is
     * found is then returned. For hippo, we always map it to index.html. So, when a resourcePath does not end  with a
     * dot followed by a suffix we can assume it is a request for index.html and see if it indeed matches with an
     * allowed index.html resource.
     *
     * @param resourcePath a path
     * @return resource path, appended with index.html if needed.
     */
    private static String addIndexHtmlIfNeeded(final String resourcePath) {
        if (!resourcePath.endsWith(".") && StringUtils.substringAfterLast(resourcePath, ".").isEmpty()) {
            // The welcome-file-list is configurable, but we assume that index.html is always present.
            return StringUtils.substringBeforeLast(resourcePath, "/") + "/index.html";
        }
        return resourcePath;
    }

    private void proxy(final HttpServletRequest request, final HttpServletResponse response, final HttpURLConnection urlConnection) throws IOException {
        copyResponseHeaders(response, urlConnection);
        response.setStatus(urlConnection.getResponseCode());

        try (final OutputStream out = response.getOutputStream()) {
            try (final InputStream is = urlConnection.getInputStream()) {
                final byte[] buffer = new byte[1024 * 4];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    private void copyResponseHeaders(final HttpServletResponse httpServletResponse, final HttpURLConnection urlConnection) {
        urlConnection.getHeaderFields().entrySet().stream()
                .filter(e -> !IGNORED_RESPONSE_HEADERS.contains(e.getKey()))
                .forEach(e ->
                        e.getValue().forEach(value -> httpServletResponse.addHeader(e.getKey(), value)));
    }

    private void copyRequestHeaders(final HttpServletRequest httpServletRequest, final HttpURLConnection urlConnection) {
        final Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            final String header = headerNames.nextElement();

            if (!IGNORED_REQUEST_HEADERS.contains(header)) {
                urlConnection.setRequestProperty(header, httpServletRequest.getHeader(header));
            }
        }
    }

    private static void logBorderedMessage(final List<String> messages) {
        final String prefix = "** ";
        final String suffix = " **";
        final int fixLength = prefix.length() + suffix.length();

        // find longest message
        messages.stream().max(Comparator.comparingInt(String::length)).ifPresent(longest -> {
            final int width = longest.length() + fixLength;
            final String border = StringUtils.repeat("*", width);
            log.info(border);
            messages.forEach(msg -> {
                final String rightPadding = StringUtils.repeat(" ", width - fixLength - msg.length());
                log.info(prefix + msg + rightPadding + suffix);
            });
            log.info(border);
        });
    }

}
