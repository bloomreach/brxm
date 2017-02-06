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
package org.hippoecm.frontend.util;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.junit.Before;
import org.junit.Test;

public class RequestUtilsTest {

    private static final String DEFAULT_REMOTE_ADDR = "100.100.100.100";

    private static final String DEFAULT_X_FORWARDED_FOR_HEADER_VALUE = "100.100.100.101, 100.100.100.10";

    private static final String CUSTOM_X_FORWARDED_FOR_HEADER_NAME = "X-ACM-Forwarded-For";

    private static final String CUSTOM_X_FORWARDED_FOR_HEADER_VALUE = "100.100.100.102, 100.100.100.11";

    private ServletContext servletContext;

    private String httpForwardedForHeaderValue;

    private String customHttpForwardedForHeaderValue;

    @Before
    public void setUp() throws Exception {
        servletContext = createNiceMock(ServletContext.class);
        replay(servletContext);
    }

    @Test
    public void testGetRemoteAddrsByDefault() {
        Request request = createRequest();
        assertArrayEquals(new String[] { DEFAULT_REMOTE_ADDR }, RequestUtils.getRemoteAddrs(request));
        assertEquals(DEFAULT_REMOTE_ADDR, RequestUtils.getFarthestRemoteAddr(request));
    }

    @Test
    public void testGetRemoteAddrsWithDefaultForwardedForHeader() {
        httpForwardedForHeaderValue = DEFAULT_X_FORWARDED_FOR_HEADER_VALUE;
        Request request = createRequest();
        assertArrayEquals(StringUtils.split(DEFAULT_X_FORWARDED_FOR_HEADER_VALUE, ", "),
                RequestUtils.getRemoteAddrs(request));
        assertEquals(StringUtils.split(DEFAULT_X_FORWARDED_FOR_HEADER_VALUE, ", ")[0],
                RequestUtils.getFarthestRemoteAddr(request));
    }

    @Test
    public void testGetRemoteAddrsWithDefaultForwardedForHeaderSetEmpty() {
        httpForwardedForHeaderValue = "";
        Request request = createRequest();
        assertArrayEquals(new String[] { DEFAULT_REMOTE_ADDR }, RequestUtils.getRemoteAddrs(request));
        assertEquals(DEFAULT_REMOTE_ADDR, RequestUtils.getFarthestRemoteAddr(request));
    }

    @Test
    public void testGetRemoteAddrsWithCustomForwardedForHeader() {
        servletContext = createNiceMock(ServletContext.class);
        expect(servletContext.getInitParameter(RequestUtils.HTTP_FORWARDED_FOR_HEADER_PARAM))
                .andReturn(CUSTOM_X_FORWARDED_FOR_HEADER_NAME).anyTimes();
        replay(servletContext);
        RequestUtils.httpForwardedForHeader = null;
        customHttpForwardedForHeaderValue = CUSTOM_X_FORWARDED_FOR_HEADER_VALUE;
        Request request = createRequest();

        assertArrayEquals(StringUtils.split(CUSTOM_X_FORWARDED_FOR_HEADER_VALUE, ", "),
                RequestUtils.getRemoteAddrs(request));
        assertEquals(StringUtils.split(CUSTOM_X_FORWARDED_FOR_HEADER_VALUE, ", ")[0],
                RequestUtils.getFarthestRemoteAddr(request));
    }

    @Test
    public void testGetRemoteAddrsWithCustomForwardedForHeaderSetEmpty() {
        servletContext = createNiceMock(ServletContext.class);
        expect(servletContext.getInitParameter(RequestUtils.HTTP_FORWARDED_FOR_HEADER_PARAM))
                .andReturn(CUSTOM_X_FORWARDED_FOR_HEADER_NAME).anyTimes();
        replay(servletContext);
        // Set httpForwardedForHeaders to null to clean the cache in HstRequestUtils.
        RequestUtils.httpForwardedForHeader = null;
        customHttpForwardedForHeaderValue = "";
        Request request = createRequest();
        assertArrayEquals(new String[] { DEFAULT_REMOTE_ADDR }, RequestUtils.getRemoteAddrs(request));
        assertEquals(DEFAULT_REMOTE_ADDR, RequestUtils.getFarthestRemoteAddr(request));
    }

    private Request createRequest() {
        HttpServletRequest servletRequest = createNiceMock(HttpServletRequest.class);
        expect(servletRequest.getServletContext()).andReturn(servletContext).anyTimes();

        expect(servletRequest.getRequestURI()).andReturn("/").anyTimes();
        expect(servletRequest.getRemoteAddr()).andReturn(DEFAULT_REMOTE_ADDR).anyTimes();
        expect(servletRequest.getHeader(RequestUtils.DEFAULT_HTTP_FORWARDED_FOR_HEADER))
                .andReturn(httpForwardedForHeaderValue).anyTimes();
        expect(servletRequest.getHeader(CUSTOM_X_FORWARDED_FOR_HEADER_NAME))
                .andReturn(customHttpForwardedForHeaderValue).anyTimes();

        replay(servletRequest);

        ServletWebRequest request = new ServletWebRequest(servletRequest, "", Url.parse("http://localhost:8080/cms/"));

        return request;
    }

}
