/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ProxyFilter.class);

    private static final String PROXIES_SYSTEM_PROPERTY = "resource.proxies";

    private Map<String, String> proxies;

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
    private static String addIndexHtmlIfNeeded(String resourcePath) {
        if (!resourcePath.endsWith(".") && StringUtils.substringAfterLast(resourcePath, ".").isEmpty()) {
            // The welcome-file-list is configurable, but we assume that index.html is always present.
            return StringUtils.substringBeforeLast(resourcePath, "/") + "/index.html";
        }
        return resourcePath;
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        final String parameterValue = filterConfig.getInitParameter("jarPathPrefix");
        String jarPathPrefix = parameterValue == null ? "META-INFO" : parameterValue;
        final String proxiesAsString = System.getProperty(PROXIES_SYSTEM_PROPERTY);
        proxies = new ProxyConfigReader(jarPathPrefix
                , proxiesAsString).getProxies();

    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        final String resourcePath = StringUtils.substringBefore(httpServletRequest.getPathInfo(), ";");
        if (resourcePath != null) {
            String query = httpServletRequest.getQueryString();
            final String queryParams = StringUtils.isEmpty(query) ? "" : "?" + query;

            final Optional<String> url = proxies.entrySet().stream().filter(e -> resourcePath.startsWith(e.getKey())).map(
                    e -> getUrl(e.getKey(), e.getValue(), resourcePath, queryParams)
            ).findFirst();

            if (url.isPresent()) {
                final URL resource = new URL(addIndexHtmlIfNeeded(url.get()));
                final URLConnection urlConnection = resource.openConnection();
                if (urlConnection != null) {
                    log.debug("Proxying {} to {}", resourcePath, url);
                    proxy(response, urlConnection);
                    return;
                }
            }
        }
        log.debug("Pass through request");
        chain.doFilter(request, response);
    }

    private void proxy(final ServletResponse response, final URLConnection urlConnection) throws IOException {
        try (OutputStream out = response.getOutputStream()) {
            try (InputStream is = urlConnection.getInputStream()) {
                final byte[] buffer = new byte[1024 * 4];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    private String getUrl(String from, String to, String resourcePath, String queryParams) {
        return to + StringUtils.substringAfter(resourcePath, from) + queryParams;
    }

    @Override
    public void destroy() {
        proxies = null;
    }


}
