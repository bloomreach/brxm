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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.Capture;
import org.hippoecm.frontend.Main;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@PowerMockIgnore("javax.management.*")
@PrepareForTest({HippoServiceRegistry.class})
@RunWith(PowerMockRunner.class)
public class NavAppRedirectFilterTest {

    private NavAppRedirectFilter filter;
    private FilterChain chain;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @Before
    public void setUp() {
        filter = new NavAppRedirectFilter();

        chain = createNiceMock(FilterChain.class);
        request = createNiceMock(HttpServletRequest.class);
        response = createNiceMock(HttpServletResponse.class);
    }

    @Test
    public void urls_with_put_method_dont_redirect() throws IOException, ServletException {

        expect(request.getMethod()).andReturn("PUT");

        chain.doFilter(request, response);
        expectLastCall();
        replayAll();

        filter.doFilter(request, response, chain);

        verifyAll();
    }


    @Test
    public void urls_with_iframe_query_param_dont_redirect() throws IOException, ServletException {

        expect(request.getMethod()).andReturn("GET");
        expect(request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER)).andReturn("");

        chain.doFilter(request, response);
        expectLastCall();
        replayAll();

        filter.doFilter(request, response, chain);

        verifyAll();
    }


    @Test
    public void urls_with_whitelisted_path_dont_redirect() throws IOException, ServletException {

        expect(request.getMethod()).andReturn("GET");
        expect(request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER)).andReturn(null);
        expect(request.getRequestURI()).andReturn("/foo/" + NavAppRedirectFilter.WHITE_LISTED_PATH_PREFIXES.get(0));
        expect(request.getContextPath()).andReturn("/foo");

        chain.doFilter(request, response);
        expectLastCall();
        replayAll();

        filter.doFilter(request, response, chain);

        verifyAll();
    }

    @Test
    public void redirect_preserves_query_parameters() throws IOException, ServletException {

        final String path = "/channelmanager";
        final Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("bar", new String[]{"baz"});
        parameterMap.put("qux", new String[]{"0", "1"});

        expect(request.getMethod()).andReturn("GET");
        expect(request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER)).andReturn(null);
        expect(request.getContextPath()).andStubReturn("/foo");
        expect(request.getRequestURI()).andStubReturn("/foo" + path);
        expect(request.getParameterMap()).andReturn(parameterMap);

        final Capture<String> location = Capture.newInstance();
        response.sendRedirect(capture(location));
        expectLastCall();
        replayAll();

        filter.doFilter(request, response, chain);

        final String capturedLocation = location.getValue();
        assertThat(capturedLocation, startsWith("./?"));
        assertThat(capturedLocation, containsString("bar=baz"));
        assertThat(capturedLocation, containsString("qux=0"));
        assertThat(capturedLocation, containsString("qux=1"));
        assertThat(capturedLocation, containsString(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER + "=" + path));

        verifyAll();
    }


    @Test
    public void redirect_can_handle_query_parameters_without_values() throws IOException, ServletException {

        final String path = "/path";
        final Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("bar", new String[0]);
        parameterMap.put("qux", null);

        expect(request.getMethod()).andReturn("GET");
        expect(request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER)).andReturn(null);
        expect(request.getContextPath()).andStubReturn("/foo");
        expect(request.getRequestURI()).andStubReturn("/foo" + path);
        expect(request.getParameterMap()).andReturn(parameterMap);

        final Capture<String> location = Capture.newInstance();
        response.sendRedirect(capture(location));
        expectLastCall();
        replayAll();

        filter.doFilter(request, response, chain);

        final String capturedLocation = location.getValue();
        assertThat(capturedLocation, startsWith("./?"));
        assertThat(capturedLocation.chars().filter(c -> c == '&').count(), is(2L));
        assertThat(capturedLocation, containsString("bar"));
        assertThat(capturedLocation, containsString("qux"));
        assertThat(capturedLocation, containsString(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER + "=" + path));

        verifyAll();
    }

    @Test
    public void redirect_can_handle_sub_paths() throws IOException, ServletException {

        final String path = "/x/y/z";
        final Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("bar", new String[]{"baz"});
        parameterMap.put("qux", new String[]{"0", "1"});

        expect(request.getMethod()).andReturn("GET");
        expect(request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER)).andReturn(null);
        expect(request.getContextPath()).andStubReturn("/foo");
        expect(request.getRequestURI()).andStubReturn("/foo" + path);
        expect(request.getParameterMap()).andReturn(parameterMap);

        final Capture<String> location = Capture.newInstance();
        response.sendRedirect(capture(location));
        expectLastCall();
        replayAll();

        filter.doFilter(request, response, chain);

        final String capturedLocation = location.getValue();
        assertThat(capturedLocation, startsWith("./../../?"));
        assertThat(capturedLocation, containsString("bar=baz"));
        assertThat(capturedLocation, containsString("qux=0"));
        assertThat(capturedLocation, containsString("qux=1"));
        assertThat(capturedLocation, containsString(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER + "=" + path));

        verifyAll();
    }

    @Test
    public void redirect_can_handle_sub_paths_with_empty_context_path() throws IOException, ServletException {

        final String path = "/x/y/z";
        final Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("bar", new String[]{"baz"});
        parameterMap.put("qux", new String[]{"0", "1"});

        expect(request.getMethod()).andReturn("GET");
        expect(request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER)).andReturn(null);
        expect(request.getContextPath()).andStubReturn("");
        expect(request.getRequestURI()).andStubReturn(path);
        expect(request.getParameterMap()).andReturn(parameterMap);

        final Capture<String> location = Capture.newInstance();
        response.sendRedirect(capture(location));
        expectLastCall();
        replayAll();

        filter.doFilter(request, response, chain);

        final String capturedLocation = location.getValue();
        assertThat(capturedLocation, startsWith("./../../?"));
        assertThat(capturedLocation, containsString("bar=baz"));
        assertThat(capturedLocation, containsString("qux=0"));
        assertThat(capturedLocation, containsString("qux=1"));
        assertThat(capturedLocation, containsString(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER + "=" + path));

        verifyAll();
    }

}
