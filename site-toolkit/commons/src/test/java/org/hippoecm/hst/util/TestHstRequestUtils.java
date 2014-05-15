/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.container.ContainerConstants;
import org.junit.Test;

/**
 * TestHstRequestUtils
 * 
 * @version $Id$
 */
public class TestHstRequestUtils {
    
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
    public void testParseQueryString() throws Exception {
        String queryString = "foo=bar&lux=bar&foo=foo";
        String[] fooValues = {"bar", "foo"};
        String[] luxValues = {"bar"};
        
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getQueryString()).andReturn(queryString).anyTimes();
        expect(request.getParameterValues("foo")).andReturn(fooValues).anyTimes();
        expect(request.getParameterValues("lux")).andReturn(luxValues).anyTimes();
        
        replay(request);
       
        
        Map<String, String[] > parsedQueryStringMap =  HstRequestUtils.parseQueryString(request);

        assertTrue("parsedQueryStringMap must contain foo.", parsedQueryStringMap.containsKey("foo"));
        assertTrue("parsedQueryStringMap must have 2 values for foo.", parsedQueryStringMap.get("foo").length == 2);
        assertTrue("parsedQueryStringMap must contain lux.", parsedQueryStringMap.containsKey("lux"));
        assertTrue("parsedQueryStringMap must have 1 value for lux.", parsedQueryStringMap.get("lux").length == 1);
    }

    @Test
    public void forcedRenderHostWithoutPortUsesPortFromForwardedHostHeader() {
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getHeader("X-Forwarded-Host")).andReturn("www.example.org:8080");
        expect(request.getParameter("FORCE_CLIENT_HOST")).andReturn("false");
        expect(request.getParameter(ContainerConstants.RENDERING_HOST)).andReturn("localhost");
        replay(request);
        final String renderHost = HstRequestUtils.getFarthestRequestHost(request, true);
        assertEquals("renderHost should take port from forwarded host","localhost:8080", renderHost);
    }

    @Test
    public void testForwardedProto()  throws Exception {
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
    public void testForwardedScheme()  throws Exception {
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
    public void testSSLEnabled()  throws Exception {
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getHeader("X-SSL-Enabled")).andReturn("On");
        replay(request);
        assertEquals("https", HstRequestUtils.getFarthestRequestScheme(request));

        request = createNiceMock(HttpServletRequest.class);
        expect(request.getHeader("X-SSL-Enabled")).andReturn("On,Off");
        replay(request);
        assertEquals("https", HstRequestUtils.getFarthestRequestScheme(request));
    }

}
