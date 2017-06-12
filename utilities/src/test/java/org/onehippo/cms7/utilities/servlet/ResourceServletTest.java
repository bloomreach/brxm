/*
 *  Copyright 2011-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletConfig;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ResourceServletTest {

    private static final String GIF_RESOURCE_PATH = "^/.*\\.gif\n";
    private static final String PROPERTIES_RESOURCE_PATH = "^/.*\\.properties\n";
    private static final String GIF_RESOURCE_MIME = ".gif = image/gif\n";
    private static final String PROPERTIES_RESOURCE_MIME = ".properties = text/plain\n";

    @Test
    public void testDefaultServlet() throws ServletException, IOException {
        final MockHttpServletRequest servletRequest = getMockHttpServletRequest();
        final MockHttpServletResponse validResponse = new MockHttpServletResponse();
        final ResourceServlet servlet = initializeServlet();
        servlet.service(servletRequest, validResponse);
        assertThat("Default servlet should allow any request.", validResponse.getStatus(), is(200));
        assertThat("Response should be of type image/gif.", validResponse.getContentType(), is("image/gif"));
    }

    @Test
    public void testDefaultServletWithInvalidRequest() throws ServletException, IOException {
        final MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,video/x-mng,image/png,image/jpeg,image/gif;q=0.2,*/*;q=0.1");
        servletRequest.addHeader("Accept-Encoding", "gzip,deflate");
        servletRequest.setMethod("GET");
        servletRequest.setServletPath("/resources");
        servletRequest.setPathInfo("/internal.properties");
        final MockHttpServletResponse invalidResponse = new MockHttpServletResponse();
        final ResourceServlet resourceServlet = initializeServlet();
        resourceServlet.service(servletRequest, invalidResponse);
        assertThat("Should give 404 since internal.properties is not accepted.", invalidResponse.getStatus(), is(404));
    }

    @Test
    public void testResourcePaths() throws ServletException, IOException {
        final MockHttpServletRequest servletRequest = getMockHttpServletRequest();
        servletRequest.addHeader("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,video/x-mng,image/png,image/jpeg,image/gif;q=0.2,*/*;q=0.1");
        servletRequest.addHeader("Accept-Encoding", "gzip,deflate");
        servletRequest.setMethod("GET");
        servletRequest.setServletPath("/resources");
        servletRequest.setPathInfo("/onehippo.gif");
        final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        final ResourceServlet servlet = initializeServlet(GIF_RESOURCE_PATH, GIF_RESOURCE_MIME);
        servlet.service(servletRequest, servletResponse);
        assertThat("Servlet should allow requests for image/gif.", servletResponse.getStatus(), is(200));
        assertThat("Response should be of type image/gif.", servletResponse.getContentType(), is("image/gif"));
    }

    @Test
    public void testCustomAllowedResourcePaths() throws ServletException, IOException {
        final MockHttpServletRequest request = getMockHttpServletRequest();
        request.addHeader("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,video/x-mng,image/png,image/jpeg,image/gif;q=0.2,*/*;q=0.1");
        request.addHeader("Accept-Encoding", "gzip,deflate");
        request.setMethod("GET");
        request.setServletPath("/resources");
        request.setPathInfo("/internal.properties");
        final MockHttpServletResponse validResponse = new MockHttpServletResponse();
        final ResourceServlet servlet = initializeServlet(PROPERTIES_RESOURCE_PATH, PROPERTIES_RESOURCE_MIME);
        servlet.service(request, validResponse);
        assertThat("Servlet should allow requests for text/plain.", validResponse.getStatus(), is(200));
        assertThat("Response should be of type text/plain.", validResponse.getContentType(), is("text/plain"));
        assertThat("Response content should be encoded.", validResponse.getHeader("Content-Encoding"), is("gzip"));
    }

    @Test
    public void testSkinResourcesExplicitlyWithoutAuthentication() throws ServletException, IOException {
        final MockHttpServletRequest request = getMockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final ResourceServlet servlet = initializeSkinServlet();
        servlet.service(request, response);
        assertThat("Authorized request should give resource.", response.getStatus(), is(200));
        assertThat("Response should be of type image/gif.", response.getContentType(), is("image/gif"));
    }

    private ResourceServlet initializeServlet() throws ServletException {
        return initializeServlet(null, null);
    }

    private ResourceServlet initializeServlet(final String allowedResourcePaths, final String mimeTypes) throws ServletException {
        final ResourceServlet servlet = new ResourceServlet();
        final MockServletConfig servletConfig = new MockServletConfig();
        servletConfig.addInitParameter("jarPathPrefix", "META-INF/test");
        if (!StringUtils.isEmpty(allowedResourcePaths)) {
            servletConfig.addInitParameter("allowedResourcePaths", allowedResourcePaths);
        }

        if (!StringUtils.isEmpty(mimeTypes)) {
            servletConfig.addInitParameter("mimeTypes", mimeTypes);
        }

        servlet.init(servletConfig);
        return servlet;
    }

    private ResourceServlet initializeSkinServlet() throws ServletException {
        final ResourceServlet skinServlet = new ResourceServlet();
        final MockServletConfig servletConfig = new MockServletConfig();

        servletConfig.addInitParameter("jarPathPrefix", "/skin");
        servletConfig.addInitParameter("allowedResourcePaths", "^/.*\\..*");

        skinServlet.init(servletConfig);

        return skinServlet;
    }

    private void fakeLoggedinUser(final MockHttpServletRequest request) {
        final HttpSession mockedSession = new MockHttpSession();
        mockedSession.setAttribute("hippo:username", "admin");
        request.setSession(mockedSession);
    }

    protected static MockHttpServletRequest getMockHttpServletRequest() {
        final MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,video/x-mng,image/png,image/jpeg,image/gif;q=0.2,*/*;q=0.1");
        request.addHeader("Accept-Encoding", "gzip,deflate");
        request.setMethod("GET");
        request.setServletPath("/resources");
        request.setPathInfo("/onehippo.gif");
        return request;
    }

}
