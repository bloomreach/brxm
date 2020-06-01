/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.net.URI;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.hippoecm.hst.core.container.ContainerConstants.HST_REQUEST_CONTEXT;
import static org.hippoecm.hst.util.HstRequestUtils.HTTP_FORWARDED_FOR_HEADER_PARAM;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TestHstRequestUtils
 * 
 * @version $Id$
 */
public class TestHstRequestUtils {

    private static final String DEFAULT_REMOTE_ADDR = "100.100.100.100";

    private static final String DEFAULT_X_FORWARDED_FOR_HEADER_VALUE = "100.100.100.101, 100.100.100.10";

    private static final String CUSTOM_X_FORWARDED_FOR_HEADER_NAME = "X-ACM-Forwarded-For";

    private static final String CUSTOM_MULTIPLE_X_FORWARDED_FOR_HEADER_NAMES = "X-DEV-Forwarded-For, X-ACM-Forwarded-For";

    private static final String CUSTOM_X_ACM_FORWARDED_FOR_HEADER_VALUE = "100.100.100.102, 100.100.100.11";

    private static final String CUSTOM_X_DEV_FORWARDED_FOR_HEADER_VALUE = "200.200.200.202, 200.200.200.22";

    private ServletContext servletContext;

    private String httpForwardedForHeaderValue;

    private String customHttpForwardedForHeaderValue;

    private String customHttpForwardedForHeaderValue2;

    @Before
    public void setUp() throws Exception {
        servletContext = createNiceMock(ServletContext.class);
        replay(servletContext);
    }

    @After
    public void tearDown() {
        HstRequestUtils.httpForwardedForHeaderNames = null;
    }

    @Test
    public void testRequestPath() throws Exception {
        String contextPath = "/site";
        String pathInfo = "/news/headlines";
        String matrixParams = ";JSESSIONID=abc;foo=bar";
        String requestURI = contextPath + pathInfo;

        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getRequestURI()).andReturn(requestURI).anyTimes();
        expect(request.getContextPath()).andReturn(contextPath).anyTimes();
        replay(request);

        String requestPath = HstRequestUtils.getRequestPath(request);

        assertEquals(pathInfo, requestPath);

        requestURI = contextPath + pathInfo + ";" + matrixParams;

        reset(request);
        expect(request.getRequestURI()).andReturn(requestURI).anyTimes();
        expect(request.getContextPath()).andReturn(contextPath).anyTimes();
        replay(request);

        requestPath = HstRequestUtils.getRequestPath(request);

        assertEquals(pathInfo, requestPath);
    }

    @Test
    public void testParseQueryStringFromRequest() throws Exception {
        String queryString = "foo=bar&lux=bar&foo=foo";
        String[] fooValues = { "bar", "foo" };
        String[] luxValues = { "bar" };

        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getQueryString()).andReturn(queryString).anyTimes();
        expect(request.getParameterValues("foo")).andReturn(fooValues).anyTimes();
        expect(request.getParameterValues("lux")).andReturn(luxValues).anyTimes();

        replay(request);

        Map<String, String[]> parsedQueryStringMap = HstRequestUtils.parseQueryString(request);

        assertTrue("parsedQueryStringMap must contain foo.", parsedQueryStringMap.containsKey("foo"));
        assertTrue("parsedQueryStringMap must have 2 values for foo.", parsedQueryStringMap.get("foo").length == 2);
        assertTrue("parsedQueryStringMap must contain lux.", parsedQueryStringMap.containsKey("lux"));
        assertTrue("parsedQueryStringMap must have 1 value for lux.", parsedQueryStringMap.get("lux").length == 1);
    }

    @Test
    public void testParseQueryStringFromRequestContainingChinese() throws Exception {
        String queryString = "key-%E4%BA%BA=value-%E4%BA%BA"; // %E4%BA%BA == 人
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getQueryString()).andReturn(queryString).anyTimes();
        replay(request);

        Map<String, String[]> parsedQueryStringMap = HstRequestUtils.parseQueryString(request);

        assertTrue("parsedQueryStringMap must contain 'key-人'.", parsedQueryStringMap.containsKey("key-人"));
        assertTrue("parsedQueryStringMap must have 1 value for 'key-人'.", parsedQueryStringMap.get("key-人").length == 1);
        assertEquals("value-人", parsedQueryStringMap.get("key-人")[0]);
    }

    @Test
    public void testParseQueryStringFromURI() throws Exception {
        URI uri = URI.create("http://www.example.com/?foo=bar&lux=bar&foo=foo+bar");
        Map<String, String[]> parsedQueryStringMap = HstRequestUtils.parseQueryString(uri, "UTF-8");

        assertTrue("parsedQueryStringMap must contain foo.", parsedQueryStringMap.containsKey("foo"));
        assertTrue("parsedQueryStringMap must have 2 values for foo.", parsedQueryStringMap.get("foo").length == 2);
        assertEquals("bar", parsedQueryStringMap.get("foo")[0]);
        assertEquals("foo bar", parsedQueryStringMap.get("foo")[1]);
        assertTrue("parsedQueryStringMap must contain lux.", parsedQueryStringMap.containsKey("lux"));
        assertTrue("parsedQueryStringMap must have 1 value for lux.", parsedQueryStringMap.get("lux").length == 1);
        assertEquals("bar", parsedQueryStringMap.get("lux")[0]);
    }

    @Test
    public void testParseQueryStringFromURIContainingChinese() throws Exception {
        URI uri = URI.create("http://www.example.com/?key-%E4%BA%BA=value-%E4%BA%BA"); // %E4%BA%BA == 人
        Map<String, String[]> parsedQueryStringMap = HstRequestUtils.parseQueryString(uri, "UTF-8");

        assertTrue("parsedQueryStringMap must contain 'key-人'.", parsedQueryStringMap.containsKey("key-人"));
        assertTrue("parsedQueryStringMap must have 1 value for 'key-人'.", parsedQueryStringMap.get("key-人").length == 1);
        assertEquals("value-人", parsedQueryStringMap.get("key-人")[0]);
    }

    @Test
    public void testParseQueryStringFromURIUsingISO8859dash1() throws Exception {
        URI uri = URI.create("http://www.example.com/?key-%E4=value-%E4"); // %E4 == ä
        Map<String, String[]> parsedQueryStringMap = HstRequestUtils.parseQueryString(uri, "ISO-8859-1");

        assertTrue("parsedQueryStringMap must contain 'key-ä'.", parsedQueryStringMap.containsKey("key-ä"));
        assertTrue("parsedQueryStringMap must have 1 value for 'key-ä'.", parsedQueryStringMap.get("key-ä").length == 1);
        assertEquals("value-ä", parsedQueryStringMap.get("key-ä")[0]);
    }

    @Test
    public void forcedRenderHostWithoutPortUsesPortFromForwardedHostHeader() {
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getHeader("X-Forwarded-Host")).andReturn("www.example.org:8080");
        expect(request.getParameter("Force-Client-Host")).andReturn("false");
        expect(request.getParameter(ContainerConstants.RENDERING_HOST)).andReturn("localhost");
        replay(request);
        final String renderHost = HstRequestUtils.getFarthestRequestHost(request, true);
        assertEquals("renderHost should take port from forwarded host", "localhost:8080", renderHost);
    }

    @Test
    public void testForwardedProto() throws Exception {
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getHeader("X-Forwarded-Proto")).andReturn("https");
        replay(request);
        assertEquals("https", HstRequestUtils.getFarthestRequestScheme(request));

        request = createNiceMock(HttpServletRequest.class);
        expect(request.getHeader("X-Forwarded-Proto")).andReturn("https,http");
        replay(request);
        assertEquals("https", HstRequestUtils.getFarthestRequestScheme(request));
    }

    @Test
    public void testForwardedScheme() throws Exception {
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getHeader("X-Forwarded-Scheme")).andReturn("https");
        replay(request);
        assertEquals("https", HstRequestUtils.getFarthestRequestScheme(request));

        request = createNiceMock(HttpServletRequest.class);
        expect(request.getHeader("X-Forwarded-Scheme")).andReturn("https,http");
        replay(request);
        assertEquals("https", HstRequestUtils.getFarthestRequestScheme(request));
    }

    @Test
    public void testSSLEnabled() throws Exception {
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getHeader("X-SSL-Enabled")).andReturn("On");
        replay(request);
        assertEquals("https", HstRequestUtils.getFarthestRequestScheme(request));

        request = createNiceMock(HttpServletRequest.class);
        expect(request.getHeader("X-SSL-Enabled")).andReturn("On,Off");
        replay(request);
        assertEquals("https", HstRequestUtils.getFarthestRequestScheme(request));
    }

    @Test
    public void testXForwardedProtoInCapitals() throws Exception {
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getHeader("X-Forwarded-Proto")).andReturn("HTTP").anyTimes();
        replay(request);
        String farthestRequestScheme = HstRequestUtils.getFarthestRequestScheme(request);
        assertEquals("http", farthestRequestScheme);
    }

    @Test
    public void testNoContextExternalRequestUrlWithoutQueryString() {
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getRequestURL()).andReturn(new StringBuffer("http://localhost/site/foo/bar")).anyTimes();
        expect(request.getQueryString()).andReturn("foo=bar&lux=bar").anyTimes();
        replay(request);
        final String externalRequestUrl = HstRequestUtils.getExternalRequestUrl(request, false);
        assertEquals("http://localhost/site/foo/bar", externalRequestUrl);
    }

    @Test
    public void testNoContextExternalRequestUrlWithQueryString() {
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getRequestURL()).andReturn(new StringBuffer("http://localhost/site/foo/bar")).anyTimes();
        expect(request.getQueryString()).andReturn("foo=bar&lux=bar").anyTimes();
        replay(request);
        final String externalRequestUrl = HstRequestUtils.getExternalRequestUrl(request, true);
        assertEquals("http://localhost/site/foo/bar?foo=bar&lux=bar", externalRequestUrl);
    }

    @Test
    public void testExternalRequestUrlWithoutQueryString() {
        HttpServletRequest request = setupMocks();
        final String externalRequestUrl = HstRequestUtils.getExternalRequestUrl(request, false);
        assertEquals("http://www.example.com/foo/bar", externalRequestUrl);
    }

    @Test
    public void testExternalRequestUrlWitQueryString() {
        HttpServletRequest request = setupMocks();
        final String externalRequestUrl = HstRequestUtils.getExternalRequestUrl(request, true);
        assertEquals("http://www.example.com/foo/bar?foo=bar&lux=bar", externalRequestUrl);
    }

    @Test
    public void testGetRemoteAddrsByDefault() {
        HttpServletRequest request = setupMocks();
        assertArrayEquals(new String[] { DEFAULT_REMOTE_ADDR }, HstRequestUtils.getRemoteAddrs(request));
        assertEquals(DEFAULT_REMOTE_ADDR, HstRequestUtils.getFarthestRemoteAddr(request));
    }

    @Test
    public void testGetRemoteAddrsWithDefaultForwardedForHeader() {
        httpForwardedForHeaderValue = DEFAULT_X_FORWARDED_FOR_HEADER_VALUE;
        HttpServletRequest request = setupMocks();
        String[] split = StringUtils.split(DEFAULT_X_FORWARDED_FOR_HEADER_VALUE, ", ");
        String[] remoteAddrs = HstRequestUtils.getRemoteAddrs(request);
        assertArrayEquals(String.format("Arrays '%s' and '%s' are not equal. Used 'http-forwarded-for-header' is '%s'",
                ArrayUtils.toString(split), ArrayUtils.toString(remoteAddrs), servletContext.getInitParameter(HTTP_FORWARDED_FOR_HEADER_PARAM)),
                split, remoteAddrs);
        assertEquals(split[0], HstRequestUtils.getFarthestRemoteAddr(request));
    }

    @Test
    public void testGetRemoteAddrsWithDefaultForwardedForHeaderSetEmpty() {
        httpForwardedForHeaderValue = "";
        HttpServletRequest request = setupMocks();
        assertArrayEquals(new String[] { DEFAULT_REMOTE_ADDR }, HstRequestUtils.getRemoteAddrs(request));
        assertEquals(DEFAULT_REMOTE_ADDR, HstRequestUtils.getFarthestRemoteAddr(request));
    }

    @Test
    public void testGetRemoteAddrsWithCustomForwardedForHeader() {
        servletContext = createNiceMock(ServletContext.class);
        expect(servletContext.getInitParameter(HTTP_FORWARDED_FOR_HEADER_PARAM))
                .andReturn(CUSTOM_X_FORWARDED_FOR_HEADER_NAME).anyTimes();
        replay(servletContext);
        HstRequestUtils.httpForwardedForHeaderNames = null;
        customHttpForwardedForHeaderValue = CUSTOM_X_ACM_FORWARDED_FOR_HEADER_VALUE;
        HttpServletRequest request = setupMocks();

        assertArrayEquals(StringUtils.split(CUSTOM_X_ACM_FORWARDED_FOR_HEADER_VALUE, ", "),
                HstRequestUtils.getRemoteAddrs(request));
        assertEquals(StringUtils.split(CUSTOM_X_ACM_FORWARDED_FOR_HEADER_VALUE, ", ")[0], HstRequestUtils.getFarthestRemoteAddr(request));
    }

    @Test
    public void testGetRemoteAddrsWithCustomMultipleForwardedForHeaders() {
        servletContext = createNiceMock(ServletContext.class);
        expect(servletContext.getInitParameter(HTTP_FORWARDED_FOR_HEADER_PARAM))
                .andReturn(CUSTOM_MULTIPLE_X_FORWARDED_FOR_HEADER_NAMES).anyTimes();
        replay(servletContext);
        HstRequestUtils.httpForwardedForHeaderNames = null;
        customHttpForwardedForHeaderValue = CUSTOM_X_DEV_FORWARDED_FOR_HEADER_VALUE;
        HttpServletRequest request = setupMocks();

        assertArrayEquals(StringUtils.split(CUSTOM_X_DEV_FORWARDED_FOR_HEADER_VALUE, ", "),
                HstRequestUtils.getRemoteAddrs(request));
        assertEquals(StringUtils.split(CUSTOM_X_DEV_FORWARDED_FOR_HEADER_VALUE, ", ")[0], HstRequestUtils.getFarthestRemoteAddr(request));
    }

    @Test
    public void testGetRemoteAddrsWithCustomForwardedForHeaderSetEmpty() {
        servletContext = createNiceMock(ServletContext.class);
        expect(servletContext.getInitParameter(HTTP_FORWARDED_FOR_HEADER_PARAM))
                .andReturn(CUSTOM_X_FORWARDED_FOR_HEADER_NAME).anyTimes();
        replay(servletContext);
        // Set httpForwardedForHeaders to null to clean the cache in HstRequestUtils.
        HstRequestUtils.httpForwardedForHeaderNames = null;
        customHttpForwardedForHeaderValue = "";
        HttpServletRequest request = setupMocks();
        assertArrayEquals(new String[] { DEFAULT_REMOTE_ADDR }, HstRequestUtils.getRemoteAddrs(request));
        assertEquals(DEFAULT_REMOTE_ADDR, HstRequestUtils.getFarthestRemoteAddr(request));
    }

    private HttpServletRequest setupMocks() {
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        HstRequestContext context = createNiceMock(HstRequestContext.class);
        VirtualHost virtualHost = createNiceMock(VirtualHost.class);
        HstContainerURL baseURL = createNiceMock(HstContainerURL.class);

        expect(baseURL.getRequestPath()).andReturn("/foo/bar");
        expect(context.getBaseURL()).andReturn(baseURL);
        expect(context.getVirtualHost()).andReturn(virtualHost).anyTimes();
        expect(virtualHost.getBaseURL(eq(request))).andReturn("http://www.example.com").anyTimes();
        expect(virtualHost.isContextPathInUrl()).andReturn(false).anyTimes();

        expect(request.getServletContext()).andReturn(servletContext).anyTimes();
        // some ip address which is however not the matched host but the internal server that runs the load balancer for example
        expect(request.getRequestURL()).andReturn(new StringBuffer("10.10.100.84/foo/bar")).anyTimes();
        expect(request.getQueryString()).andReturn("foo=bar&lux=bar").anyTimes();
        expect(request.getAttribute(eq(HST_REQUEST_CONTEXT))).andReturn(context);
        expect(request.getRemoteAddr()).andReturn(DEFAULT_REMOTE_ADDR).anyTimes();
        expect(request.getHeader(HstRequestUtils.DEFAULT_HTTP_FORWARDED_FOR_HEADER))
                .andReturn(httpForwardedForHeaderValue).anyTimes();
        expect(request.getHeader(CUSTOM_X_FORWARDED_FOR_HEADER_NAME)).andReturn(customHttpForwardedForHeaderValue)
                .anyTimes();

        replay(request, baseURL, context, virtualHost);

        return request;
    }
}
