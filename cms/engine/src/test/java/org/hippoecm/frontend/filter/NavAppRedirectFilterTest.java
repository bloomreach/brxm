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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.Capture;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.hippoecm.frontend.Main;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(EasyMockRunner.class)
public class NavAppRedirectFilterTest {


    private NavAppRedirectFilter filter;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @Before
    public void setUp() {
        filter = new NavAppRedirectFilter();
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
    public void urls_with_whitelisted_path_dont_redirect() throws IOException, ServletException {

        expect(request.getMethod()).andReturn("GET");
        expect(request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER)).andReturn(null);
        expect(request.getRequestURI()).andReturn("/foo" + NavAppRedirectFilter.WHITE_LISTED_PATH_PREFIXES.get(0));
        expect(request.getContextPath()).andReturn("/foo");
        replay(request);

        chain.doFilter(request, response);
        expectLastCall();
        replay(chain);

        filter.doFilter(request, response, chain);

        verify(request, chain);
    }

    @Test
    public void redirect_preserves_query_parameters() throws IOException, ServletException {

        final String path = "/channelmanager";
        final Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("bar", new String[]{"baz"});
        parameterMap.put("qux", new String[]{"0", "1"});

        expect(request.getMethod()).andReturn("GET");
        expect(request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER)).andReturn(null);
        expect(request.getContextPath()).andReturn("/foo").times(2);
        expect(request.getRequestURI()).andReturn("/foo" + path).times(2);
        expect(request.getParameterMap()).andReturn(parameterMap);
        replay(request);

        final Capture<String> location = Capture.newInstance();
        response.sendRedirect(capture(location));
        expectLastCall();
        replay(response);

        filter.doFilter(request, response, chain);

        final String capturedLocation = location.getValue();
        assertThat(capturedLocation, containsString("bar=baz"));
        assertThat(capturedLocation, containsString("qux=0"));
        assertThat(capturedLocation, containsString("qux=1"));
        assertThat(capturedLocation, containsString(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER + "=" + path));
    }


    @Test
    public void redirect_can_handle_query_parameters_without_values() throws IOException, ServletException {

        final String path = "/path";
        final Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("bar", new String[0]);
        parameterMap.put("qux", null);

        expect(request.getMethod()).andReturn("GET");
        expect(request.getParameter(Main.CMS_AS_IFRAME_QUERY_PARAMETER)).andReturn(null);
        expect(request.getContextPath()).andReturn("/foo").times(2);
        expect(request.getRequestURI()).andReturn("/foo" + path).times(2);
        expect(request.getParameterMap()).andReturn(parameterMap);
        replay(request);

        final Capture<String> location = Capture.newInstance();
        response.sendRedirect(capture(location));
        expectLastCall();
        replay(response);

        filter.doFilter(request, response, chain);

        final String capturedLocation = location.getValue();
        assertThat(capturedLocation.chars().filter(c -> c == '&').count(), is(2L));
        assertThat(capturedLocation, containsString("bar"));
        assertThat(capturedLocation, containsString("qux"));
        assertThat(capturedLocation, containsString(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER + "=" + path));
    }
}
