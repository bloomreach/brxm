/*
 * Copyright 2019-2022 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.wicket.ThreadContext;
import org.easymock.Capture;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.navigation.NavigationItem;
import org.hippoecm.frontend.navigation.NavigationItemService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.mock.MockSession;
import org.springframework.mock.web.MockFilterConfig;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hippoecm.frontend.filter.NavAppRedirectFilter.CUSTOM_ACCEPTED_PATH_PREFIXES_PARAMETER;
import static org.junit.Assert.assertThat;

@RunWith(EasyMockRunner.class)
public class NavAppRedirectFilterTest {

    private NavAppRedirectFilter filter;

    @Mock
    private FilterChain chain;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private NavigationItemService navigationItemService;

    @Before
    public void setUp() {
        filter = new NavAppRedirectFilter();
        filter.init(new MockFilterConfig());
        HippoServiceRegistry.register(navigationItemService, NavigationItemService.class);
    }

    @After
    public void tearDown() {
        HippoServiceRegistry.unregister(navigationItemService, NavigationItemService.class);
    }

    @Test
    public void urls_with_put_method_dont_redirect() throws IOException, ServletException {

        expect(request.getMethod()).andReturn("PUT");
        replay(request);

        chain.doFilter(request, response);
        expectLastCall();
        replay(chain);

        filter.doFilter(request, response, chain);

        verify(request, chain);
    }

    @Test
    public void urls_with_iframe_query_param_dont_redirect() throws IOException, ServletException {

        expect(request.getMethod()).andReturn("GET");
        expect(request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER)).andReturn("");
        replay(request);

        chain.doFilter(request, response);
        expectLastCall();
        replay(chain);

        filter.doFilter(request, response, chain);

        verify(request, chain);
    }

    @Test
    public void urls_with_allowed_path_dont_redirect() throws IOException, ServletException {

        expect(request.getMethod()).andReturn("GET");
        expect(request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER)).andReturn(null);
        expect(request.getRequestURI()).andReturn("/foo/" + NavAppRedirectFilter.ACCEPTED_PATH_PREFIXES.get(0));
        expect(request.getContextPath()).andReturn("/foo");
        replay(request);

        chain.doFilter(request, response);
        expectLastCall();
        replay(chain);

        filter.doFilter(request, response, chain);

        verify(request, chain);
    }

    @Test
    public void urls_with_unknown_frontend_app_path_dont_redirect() throws IOException, ServletException {
        mockNavigationService("channel-manager");

        expect(request.getMethod()).andReturn("GET");
        expect(request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER)).andReturn(null);
        expect(request.getContextPath()).andReturn("/foo");
        expect(request.getRequestURI()).andReturn("/foo/experience-manager");
        replay(request);

        chain.doFilter(request, response);
        expectLastCall();
        replay(chain);

        filter.doFilter(request, response, chain);

        verify(request, chain, navigationItemService);
    }

    @Test
    public void urls_with_unknown_frontend_app_path_redirect_if_there_are_zero_navigation_items() throws IOException, ServletException {
        mockNavigationService();

        final String path = "/experience-manager";
        expect(request.getMethod()).andReturn("GET");
        expect(request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER)).andReturn(null);
        expect(request.getContextPath()).andStubReturn("/foo");
        expect(request.getRequestURI()).andStubReturn("/foo" + path);
        expect(request.getParameterMap()).andReturn(Collections.emptyMap());
        replay(request);

        final Capture<String> location = Capture.newInstance();
        response.sendRedirect(capture(location));
        expectLastCall();
        replay(response);

        filter.doFilter(request, response, chain);

        final String capturedLocation = location.getValue();
        assertThat(capturedLocation, startsWith("./?"));
        assertThat(capturedLocation, containsString(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER + "=" + path));

        verify(request, response, navigationItemService);
    }

    @Test
    public void redirect_preserves_query_parameters() throws IOException, ServletException {
        mockNavigationService("channel-manager");

        final String path = "/channel-manager";
        final Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("bar", new String[]{"baz"});
        parameterMap.put("qux", new String[]{"0", "1"});

        expect(request.getMethod()).andReturn("GET");
        expect(request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER)).andReturn(null);
        expect(request.getContextPath()).andStubReturn("/foo");
        expect(request.getRequestURI()).andStubReturn("/foo" + path);
        expect(request.getParameterMap()).andReturn(parameterMap);
        replay(request);

        final Capture<String> location = Capture.newInstance();
        response.sendRedirect(capture(location));
        expectLastCall();
        replay(response);

        filter.doFilter(request, response, chain);

        final String capturedLocation = location.getValue();
        assertThat(capturedLocation, startsWith("./?"));
        assertThat(capturedLocation, containsString("bar=baz"));
        assertThat(capturedLocation, containsString("qux=0"));
        assertThat(capturedLocation, containsString("qux=1"));
        assertThat(capturedLocation, containsString(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER + "=" + path));

        verify(request, response, navigationItemService);
    }

    @Test
    public void redirect_can_handle_query_parameters_without_values() throws IOException, ServletException {
        mockNavigationService("content");

        final String path = "/content";
        final Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("bar", new String[0]);
        parameterMap.put("qux", null);

        expect(request.getMethod()).andReturn("GET");
        expect(request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER)).andReturn(null);
        expect(request.getContextPath()).andStubReturn("/foo");
        expect(request.getRequestURI()).andStubReturn("/foo" + path);
        expect(request.getParameterMap()).andReturn(parameterMap);
        replay(request);

        final Capture<String> location = Capture.newInstance();
        response.sendRedirect(capture(location));
        expectLastCall();
        replay(response);

        filter.doFilter(request, response, chain);

        final String capturedLocation = location.getValue();
        assertThat(capturedLocation, startsWith("./?"));
        assertThat(capturedLocation.chars().filter(c -> c == '&').count(), is(2L));
        assertThat(capturedLocation, containsString("bar"));
        assertThat(capturedLocation, containsString("qux"));
        assertThat(capturedLocation, containsString(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER + "=" + path));

        verify(request, response, navigationItemService);
    }

    @Test
    public void redirect_can_handle_sub_paths() throws IOException, ServletException {
        mockNavigationService("content");

        final String path = "/content/documents/news";
        final Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("bar", new String[]{"baz"});
        parameterMap.put("qux", new String[]{"0", "1"});

        expect(request.getMethod()).andReturn("GET");
        expect(request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER)).andReturn(null);
        expect(request.getContextPath()).andStubReturn("/foo");
        expect(request.getRequestURI()).andStubReturn("/foo" + path);
        expect(request.getParameterMap()).andReturn(parameterMap);
        replay(request);

        final Capture<String> location = Capture.newInstance();
        response.sendRedirect(capture(location));
        expectLastCall();
        replay(response);

        filter.doFilter(request, response, chain);

        final String capturedLocation = location.getValue();
        assertThat(capturedLocation, startsWith("./../../?"));
        assertThat(capturedLocation, containsString("bar=baz"));
        assertThat(capturedLocation, containsString("qux=0"));
        assertThat(capturedLocation, containsString("qux=1"));
        assertThat(capturedLocation, containsString(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER + "=" + path));

        verify(request, response, navigationItemService);
    }

    @Test
    public void redirect_can_handle_sub_paths_with_empty_context_path() throws IOException, ServletException {
        mockNavigationService("content");

        final String path = "/content/documents/news";
        final Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("bar", new String[]{"baz"});
        parameterMap.put("qux", new String[]{"0", "1"});

        expect(request.getMethod()).andReturn("GET");
        expect(request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER)).andReturn(null);
        expect(request.getContextPath()).andStubReturn("");
        expect(request.getRequestURI()).andStubReturn(path);
        expect(request.getParameterMap()).andReturn(parameterMap);
        replay(request);

        final Capture<String> location = Capture.newInstance();
        response.sendRedirect(capture(location));
        expectLastCall();
        replay(response);

        filter.doFilter(request, response, chain);

        final String capturedLocation = location.getValue();
        assertThat(capturedLocation, startsWith("./../../?"));
        assertThat(capturedLocation, containsString("bar=baz"));
        assertThat(capturedLocation, containsString("qux=0"));
        assertThat(capturedLocation, containsString("qux=1"));
        assertThat(capturedLocation, containsString(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER + "=" + path));

        verify(request, response, navigationItemService);
    }

    @Test
    public void urls_with_custom_allowed_path_dont_redirect() throws IOException, ServletException {
        MockFilterConfig filterConfig = new MockFilterConfig();
        final String custom2 = "custom2";
        filterConfig.addInitParameter(CUSTOM_ACCEPTED_PATH_PREFIXES_PARAMETER,
                String.format("custom1, %s,    custom3", custom2));
        filter.init(filterConfig);
        expect(request.getMethod()).andReturn("GET");
        expect(request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER)).andReturn(null);
        expect(request.getRequestURI()).andReturn("/foo/" + custom2);
        expect(request.getContextPath()).andReturn("/foo");
        replay(request);

        chain.doFilter(request, response);
        expectLastCall();
        replay(chain);

        filter.doFilter(request, response, chain);

        verify(request, chain);
    }

    private void mockNavigationService(final String... appPaths) {
        final List<NavigationItem> items = Stream.of(appPaths)
                .map(path -> {
                    final NavigationItem item = new NavigationItem();
                    item.setAppPath(path);
                    return item;
                })
                .collect(Collectors.toList());

        final HippoSession jcrSession = new MockSession(null);
        final Locale locale = Locale.ENGLISH;
        final UserSession userSession = createMock(UserSession.class);
        expect(userSession.getJcrSession()).andStubReturn(jcrSession);
        expect(userSession.getLocale()).andStubReturn(locale);
        ThreadContext.setSession(userSession);
        replay(userSession);

        expect(navigationItemService.getNavigationItems(jcrSession, locale)).andReturn(items);
        replay(navigationItemService);
    }
}
