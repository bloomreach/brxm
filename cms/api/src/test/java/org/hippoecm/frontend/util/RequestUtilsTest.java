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

import java.util.Arrays;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class RequestUtilsTest {

    private static final String DEFAULT_REMOTE_ADDR = "100.100.100.100";

    private static final String DEFAULT_X_FORWARDED_FOR_HEADER_VALUE = "100.100.100.101, 100.100.100.10";

    private static final String CUSTOM_X_FORWARDED_FOR_HEADER_NAME = "X-ACM-Forwarded-For";

    private static final String CUSTOM_MULTIPLE_X_FORWARDED_FOR_HEADER_NAMES = "X-DEV-Forwarded-For, X-ACM-Forwarded-For";

    private static final String CUSTOM_X_ACM_FORWARDED_FOR_HEADER_VALUE = "100.100.100.102, 100.100.100.11";

    private static final String CUSTOM_X_DEV_FORWARDED_FOR_HEADER_VALUE = "200.200.200.202, 200.200.200.22";

    @Before
    public void setUp() {
        // Reset the http forwarded header for clean state in each test.
        RequestUtils.httpForwardedForHeaderNames = null;
    }

    @Test
    public void testGetRemoteAddrsByDefault() {
        Request request = createRequest();
        String[] expArr = new String[] { DEFAULT_REMOTE_ADDR };
        String[] actArr = RequestUtils.getRemoteAddrs(request);
        assertArrayEquals(createErrorMessage(expArr, actArr), expArr, actArr);
        String expVal = DEFAULT_REMOTE_ADDR;
        String actVal = RequestUtils.getFarthestRemoteAddr(request);
        assertEquals(createErrorMessage(expVal, actVal), expVal, actVal);
    }

    @Test
    public void testGetRemoteAddrsWithDefaultForwardedForHeader() {
        Request request = createRequest(createServletContext(), DEFAULT_X_FORWARDED_FOR_HEADER_VALUE, null);
        String[] expArr = StringUtils.split(DEFAULT_X_FORWARDED_FOR_HEADER_VALUE, ", ");
        String[] actArr = RequestUtils.getRemoteAddrs(request);
        assertArrayEquals(createErrorMessage(expArr, actArr), expArr, actArr);
        String expVal = StringUtils.split(DEFAULT_X_FORWARDED_FOR_HEADER_VALUE, ", ")[0];
        String actVal = RequestUtils.getFarthestRemoteAddr(request);
        assertEquals(createErrorMessage(expVal, actVal), expVal, actVal);
    }

    @Test
    public void testGetRemoteAddrsWithDefaultForwardedForHeaderSetEmpty() {
        Request request = createRequest(createServletContext(), "", null);
        String[] expArr = new String[] { DEFAULT_REMOTE_ADDR };
        String[] actArr = RequestUtils.getRemoteAddrs(request);
        assertArrayEquals(createErrorMessage(expArr, actArr), expArr, actArr);
        String expVal = DEFAULT_REMOTE_ADDR;
        String actVal = RequestUtils.getFarthestRemoteAddr(request);
        assertEquals(createErrorMessage(expVal, actVal), expVal, actVal);
    }

    @Test
    public void testGetRemoteAddrsWithCustomForwardedForHeader() {
        ServletContext servletContext = createNiceMock(ServletContext.class);
        expect(servletContext.getInitParameter(RequestUtils.HTTP_FORWARDED_FOR_HEADER_PARAM))
                .andReturn(CUSTOM_X_FORWARDED_FOR_HEADER_NAME).anyTimes();
        replay(servletContext);
        Request request = createRequest(servletContext, null, CUSTOM_X_ACM_FORWARDED_FOR_HEADER_VALUE);

        String[] expArr = StringUtils.split(CUSTOM_X_ACM_FORWARDED_FOR_HEADER_VALUE, ", ");
        String[] actArr = RequestUtils.getRemoteAddrs(request);
        assertArrayEquals(createErrorMessage(expArr, actArr), expArr, actArr);
        String expVal = StringUtils.split(CUSTOM_X_ACM_FORWARDED_FOR_HEADER_VALUE, ", ")[0];
        String actVal = RequestUtils.getFarthestRemoteAddr(request);
        assertEquals(createErrorMessage(expVal, actVal), expVal, actVal);
    }

    @Test
    public void testGetRemoteAddrsWithCustomMultipleForwardedForHeaders() {
        ServletContext servletContext = createNiceMock(ServletContext.class);
        expect(servletContext.getInitParameter(RequestUtils.HTTP_FORWARDED_FOR_HEADER_PARAM))
                .andReturn(CUSTOM_MULTIPLE_X_FORWARDED_FOR_HEADER_NAMES).anyTimes();
        replay(servletContext);
        Request request = createRequest(servletContext, null, CUSTOM_X_DEV_FORWARDED_FOR_HEADER_VALUE);

        String[] expArr = StringUtils.split(CUSTOM_X_DEV_FORWARDED_FOR_HEADER_VALUE, ", ");
        String[] actArr = RequestUtils.getRemoteAddrs(request);
        assertArrayEquals(createErrorMessage(expArr, actArr), expArr, actArr);
        String expVal = StringUtils.split(CUSTOM_X_DEV_FORWARDED_FOR_HEADER_VALUE, ", ")[0];
        String actVal = RequestUtils.getFarthestRemoteAddr(request);
        assertEquals(createErrorMessage(expVal, actVal), expVal, actVal);
    }

    @Test
    public void testGetRemoteAddrsWithCustomForwardedForHeaderSetEmpty() {
        ServletContext servletContext = createNiceMock(ServletContext.class);
        expect(servletContext.getInitParameter(RequestUtils.HTTP_FORWARDED_FOR_HEADER_PARAM))
                .andReturn(CUSTOM_X_FORWARDED_FOR_HEADER_NAME).anyTimes();
        replay(servletContext);
        Request request = createRequest(servletContext, null, "");

        String[] expArr = new String[] { DEFAULT_REMOTE_ADDR };
        String[] actArr = RequestUtils.getRemoteAddrs(request);
        assertArrayEquals(createErrorMessage(expArr, actArr), expArr, actArr);
        String expVal = DEFAULT_REMOTE_ADDR;
        String actVal = RequestUtils.getFarthestRemoteAddr(request);
        assertEquals(createErrorMessage(expVal, actVal), expVal, actVal);
    }

    private ServletContext createServletContext() {
        ServletContext servletContext = createNiceMock(ServletContext.class);
        replay(servletContext);
        return servletContext;
    }

    private Request createRequest() {
        return createRequest(createServletContext(), null, null);
    }

    private Request createRequest(final ServletContext servletContext, final String httpForwardedForHeaderValue, final String customHttpForwardedForHeaderValue) {
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

    private String createErrorMessage(String[] expArr, String[] actArr) {
        return "Different array (RequestUtils.httpForwardedForHeader=" + Arrays.toString(RequestUtils.httpForwardedForHeaderNames)
                + "). expected=" + Arrays.toString(expArr) + ", actual=" + Arrays.toString(actArr);
    }

    private String createErrorMessage(String expVal, String actVal) {
        return "Different value (RequestUtils.httpForwardedForHeader=" + Arrays.toString(RequestUtils.httpForwardedForHeaderNames)
                + ").expected=" + expVal + ", actual=" + actVal;
    }
}
