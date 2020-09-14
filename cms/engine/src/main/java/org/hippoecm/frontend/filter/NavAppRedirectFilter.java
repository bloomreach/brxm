/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
 */
package org.hippoecm.frontend.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
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

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.navigation.NavigationItem;
import org.hippoecm.frontend.navigation.NavigationItemService;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class NavAppRedirectFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(NavAppRedirectFilter.class);

    public static final String INITIAL_PATH_QUERY_PARAMETER = "initialPath";

    public static final String CUSTOM_ACCEPTED_PATH_PREFIXES_PARAMETER = "customAcceptedPathPrefixes";

    private static final String HTTP_METHOD_GET = "GET";

    static final List<String> ACCEPTED_PATH_PREFIXES = Arrays.asList(
            "angular",
            "auth",
            "binaries",
            "ckeditor",
            "console",
            "logging",
            "navapp-assets",
            "oidc",
            "ping",
            "repository",
            "site",
            "skin",
            "wicket",
            "ws"
    );

    private static List<String> acceptedPathPrefixes;

    @Override
    public void init(FilterConfig filterConfig) {
        final String value = filterConfig.getInitParameter(CUSTOM_ACCEPTED_PATH_PREFIXES_PARAMETER);
        if (value == null) {
            log.debug("Init parameter {} is not present, using hard coded path prefixes."
                    , CUSTOM_ACCEPTED_PATH_PREFIXES_PARAMETER);
            acceptedPathPrefixes = ACCEPTED_PATH_PREFIXES;
        } else {
            acceptedPathPrefixes = new ArrayList<>(ACCEPTED_PATH_PREFIXES);
            final List<String> customWhiteListedPathPrefixes =
                    Stream.of(value.split(",")).map(String::trim).collect(Collectors.toList());
            log.debug("Adding custom path prefixes: {}", customWhiteListedPathPrefixes);
            acceptedPathPrefixes.addAll(customWhiteListedPathPrefixes);
        }
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
        if (shouldRedirectToCMS(request)) {
            final String relativePath = getRelativePath(request);
            final String queryParameters = getQueryParameterString(request);
            final String redirectUrl = "./" + relativePath + queryParameters;

            if (log.isDebugEnabled()) {
                log.debug("Redirecting '{}' to '{}'", getRequestAsString(request), redirectUrl);
            }
            response.sendRedirect(redirectUrl);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Chaining '{}' to the next filter", getRequestAsString(request));
            }
            chain.doFilter(request, response);
        }
    }

    private boolean shouldRedirectToCMS(final HttpServletRequest request) {
        if (!HTTP_METHOD_GET.equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        if (request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER) != null) {
            return false;
        }

        final String segment = getSegmentAfterContextPath(request);
        if (acceptedPathPrefixes.stream().anyMatch(segment::equals)) {
            return false;
        }

        final List<NavigationItem> navigationItems = getNavigationItems();
        if (navigationItems.isEmpty()) { // not logged in yet
            return true;
        }

        return navigationItems.stream()
                .map(NavigationItem::getAppPath)
                .anyMatch(segment::equals);

    }

    private String getSegmentAfterContextPath(final HttpServletRequest request) {
        final String pathAfterContextPath = getPathAfterContextPath(request);
        if (StringUtils.isEmpty(pathAfterContextPath)) {
            return StringUtils.EMPTY;
        }

        final String[] segments = pathAfterContextPath.split("/");
        return segments.length < 2
                ? StringUtils.EMPTY
                : segments[1];
    }

    private static String getRequestAsString(final HttpServletRequest request) {
        final StringBuffer url = request.getRequestURL();
        if (isNotBlank(request.getQueryString())) {
            url.append("?").append(request.getQueryString());
        }
        return url.toString();
    }

    private String getRelativePath(HttpServletRequest request) {
        final String[] segments = getPathAfterContextPath(request).split("/");
        final int dirsUp = Math.max(segments.length - 2, 0);
        final StringJoiner stringJoiner = new StringJoiner("/", "", "/");
        stringJoiner.setEmptyValue("");
        nCopies(dirsUp, "..").forEach(stringJoiner::add);
        return stringJoiner.toString();
    }

    private String getQueryParameterString(HttpServletRequest request) {
        final Map<String, String[]> parameterMap = new HashMap<>(request.getParameterMap());
        parameterMap.put(INITIAL_PATH_QUERY_PARAMETER, new String[]{getPathAfterContextPath(request)});
        return parameterMap.entrySet().stream()
                .flatMap(entry -> {
                    final String[] values = entry.getValue();
                    final String key = entry.getKey();
                    if (values == null || values.length == 0) {
                        return Stream.of(key);
                    }
                    return Stream.of(entry.getValue()).map(value -> String.join("=", key, value));
                })
                .collect(joining("&", "?", ""));
    }

    private String getPathAfterContextPath(HttpServletRequest request) {
        return request.getRequestURI().replaceFirst(request.getContextPath(), "");
    }

    private static List<NavigationItem> getNavigationItems() {
        if (!UserSession.exists()) {
            return emptyList();
        }

        final UserSession userSession = UserSession.get();
        final NavigationItemService navigationItemService = HippoServiceRegistry.getService(NavigationItemService.class);
        return navigationItemService.getNavigationItems(userSession.getJcrSession(), userSession.getLocale());
    }
}
