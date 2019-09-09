/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.frontend.Main;

import static java.util.stream.Collectors.joining;

public class NavAppRedirectFilter implements Filter {

    public static final String INITIAL_PATH_QUERY_PARAMETER = "initialPath";

    private static final String HTTP_METHOD_GET = "GET";

    static final List<String> WHITE_LISTED_PATH_PREFIXES = Arrays.asList(
            "/angular",
            "/auth",
            "/ckeditor",
            "/console",
            "/logging",
            "/navapp",
            "/ping",
            "/repository",
            "/site",
            "/skin",
            "/wicket",
            "/ws"
    );


    @Override
    public void init(FilterConfig filterConfig) {
        // This filter is stateless and has no init parameters.
    }

    @Override
    public void destroy() {
        // This filter is stateless, so there is nothing to dispose of.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (isWhiteListed(request)) {
            chain.doFilter(request, response);
        } else {
            final Map<String, String[]> parameterMap = new HashMap<>(request.getParameterMap());
            parameterMap.put(INITIAL_PATH_QUERY_PARAMETER, new String[]{getPathAfterContextPath(request)});
            final String queryParameters = parameterMap.entrySet().stream()
                    .flatMap(entry -> {
                        final String[] values = entry.getValue();
                        final String key = entry.getKey();
                        if (values == null || values.length == 0) {
                            return Stream.of(key);
                        }
                        return Stream.of(entry.getValue()).map(value -> String.join("=", key, value));
                    })
                    .collect(joining("&", "?", ""));
            final String redirectUrl = String.format("./%s", queryParameters);
            response.sendRedirect(redirectUrl);
        }
    }

    private boolean isWhiteListed(HttpServletRequest request) {
        return !HTTP_METHOD_GET.equalsIgnoreCase(request.getMethod())
                || request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER) != null
                || matchesAWhiteListedPrefix(request);
    }

    private boolean matchesAWhiteListedPrefix(HttpServletRequest request) {
        final String path = getPathAfterContextPath(request);
        return WHITE_LISTED_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private String getPathAfterContextPath(HttpServletRequest request) {
        return request.getRequestURI().replaceFirst(request.getContextPath(), "");
    }
}
