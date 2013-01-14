/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;

import static junit.framework.Assert.assertEquals;

public class ResourceServletTest {
    
    private ResourceServlet defaultServlet;
    private ResourceServlet moreServingServlet;
    
    @Before
    public void setUp() throws Exception {
        defaultServlet = new ResourceServlet();
        MockServletConfig servletConfig = new MockServletConfig();
        servletConfig.addInitParameter("jarPathPrefix", "META-INF/test");
        defaultServlet.init(servletConfig);

        moreServingServlet = new ResourceServlet();
        servletConfig = new MockServletConfig();
        servletConfig.addInitParameter("jarPathPrefix", "META-INF/test");
        String customAllowedResourcePaths = 
            "^/.*\\.js, \n" + 
            "^/.*\\.css, \n" + 
            "^/.*\\.png, \n" + 
            "^/.*\\.gif, \n" + 
            "^/.*\\.ico, \n" + 
            "^/.*\\.jpg, \n" + 
            "^/.*\\.jpeg, \n" + 
            "^/.*\\.swf, \n" +
            "^/.*\\.properties\n";
        servletConfig.addInitParameter("allowedResourcePaths", customAllowedResourcePaths);
        String mimeTypes =
            ".css = text/css,\n" +
            ".js = text/javascript,\n" +
            ".gif = image/gif,\n" +
            ".png = image/png,\n" +
            ".ico = image/vnd.microsoft.icon,\n" +
            ".jpg = image/jpeg,\n" +
            ".jpeg = image/jpeg,\n" +
            ".swf = application/x-shockwave-flash,\n" +
            ".properties = text/plain\n";
        
        servletConfig.addInitParameter("mimeTypes", mimeTypes);
        moreServingServlet.init(servletConfig);
    }
    
    @Test
    public void testDefaultServlet() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        request.addHeader("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,video/x-mng,image/png,image/jpeg,image/gif;q=0.2,*/*;q=0.1");
        request.addHeader("Accept-Encoding", "gzip,deflate");
        request.setMethod("GET");
        request.setServletPath("/resources");
        request.setPathInfo("/onehippo.gif");
        MockHttpServletResponse response = new MockHttpServletResponse();
        defaultServlet.service(request, response);
        assertEquals(200, response.getStatus());
        assertEquals("image/gif", response.getContentType());
        
        request = new MockHttpServletRequest();
        request.addHeader("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,video/x-mng,image/png,image/jpeg,image/gif;q=0.2,*/*;q=0.1");
        request.addHeader("Accept-Encoding", "gzip,deflate");
        request.setMethod("GET");
        request.setServletPath("/resources");
        request.setPathInfo("/internal.properties");
        response = new MockHttpServletResponse();
        defaultServlet.service(request, response);
        assertEquals(404, response.getStatus());
    }
    
    @Test
    public void testMoreServingServlet() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,video/x-mng,image/png,image/jpeg,image/gif;q=0.2,*/*;q=0.1");
        request.addHeader("Accept-Encoding", "gzip,deflate");
        request.setMethod("GET");
        request.setServletPath("/resources");
        request.setPathInfo("/onehippo.gif");
        MockHttpServletResponse response = new MockHttpServletResponse();
        moreServingServlet.service(request, response);
        assertEquals(200, response.getStatus());
        assertEquals("image/gif", response.getContentType());
        
        request = new MockHttpServletRequest();
        request.addHeader("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,video/x-mng,image/png,image/jpeg,image/gif;q=0.2,*/*;q=0.1");
        request.addHeader("Accept-Encoding", "gzip,deflate");
        request.setMethod("GET");
        request.setServletPath("/resources");
        request.setPathInfo("/internal.properties");
        response = new MockHttpServletResponse();
        moreServingServlet.service(request, response);
        assertEquals(200, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertEquals("gzip", response.getHeader("Content-Encoding"));
    }
}
