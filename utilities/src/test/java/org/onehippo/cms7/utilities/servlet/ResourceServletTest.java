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
import org.junit.Assert;
import org.junit.Test;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletConfig;

import static org.hamcrest.core.Is.is;

public class ResourceServletTest {

    private static final String GIF_RESOURCE_PATH = "^/.*\\.gif\n";
    private static final String PROPERTIES_RESOURCE_PATH = "^/.*\\.properties\n";
    private static final String GIF_RESOURCE_MIME = ".gif = image/gif\n";
    private static final String PROPERTIES_RESOURCE_MIME = ".properties = text/plain\n";

    @Test
    public void testDefaultServlet() throws ServletException, IOException {
        final MockHttpServletRequest servletRequest = getMockHttpServletRequest();
        final MockHttpServletResponse validResponse = new MockHttpServletResponse();
        final ResourceServlet servlet = initializeServlet(false, null, null);
        servlet.service(servletRequest, validResponse);
        Assert.assertThat("Default servlet should allow any request.", validResponse.getStatus(), is(200));
        Assert.assertThat("Response should be of type image/gif.", validResponse.getContentType(), is("image/gif"));
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
        final ResourceServlet resourceServlet = initializeServlet(false, null, null);
        resourceServlet.service(servletRequest, invalidResponse);
        Assert.assertThat("Should give 404 since internal.properties is not accepted.", invalidResponse.getStatus(), is(404));
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
        final ResourceServlet servlet = initializeServlet(false, GIF_RESOURCE_PATH, GIF_RESOURCE_MIME);
        servlet.service(servletRequest, servletResponse);
        Assert.assertThat("Servlet should allow requests for image/gif.", servletResponse.getStatus(), is(200));
        Assert.assertThat("Response should be of type image/gif.", servletResponse.getContentType(), is("image/gif"));
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
        final ResourceServlet servlet = initializeServlet(false, PROPERTIES_RESOURCE_PATH, PROPERTIES_RESOURCE_MIME);
        servlet.service(request, validResponse);
        Assert.assertThat("Servlet should allow requests for text/plain.", validResponse.getStatus(), is(200));
        Assert.assertThat("Response should be of type text/plain.", validResponse.getContentType(), is("text/plain"));
        Assert.assertThat("Response content should be encoded.", validResponse.getHeader("Content-Encoding"), is("gzip"));
    }

    @Test
    public void testUnauthorizedServlet() throws ServletException, IOException {
        final MockHttpServletRequest request = getMockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final ResourceServlet servlet = initializeServlet(true, GIF_RESOURCE_PATH, GIF_RESOURCE_MIME);
        servlet.service(request, response);
        Assert.assertThat("Unauthorized request should give 404.", response.getStatus(), is(404));
    }

    @Test
    public void testAuthorizedServlet() throws ServletException, IOException {
        final MockHttpServletRequest request = getMockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final ResourceServlet servlet = initializeServlet(true);
        fakeLoggedinUser(request);
        servlet.service(request, response);
        Assert.assertThat("Authorized request should give resource.", response.getStatus(), is(200));
        Assert.assertThat("Response should be of type image/gif.", response.getContentType(), is("image/gif"));
    }

    @Test
    public void testWhitelistedServlet() throws ServletException, IOException {
        final MockHttpServletRequest request = getMockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final ResourceServlet servlet = initializeServlet(true, GIF_RESOURCE_PATH, GIF_RESOURCE_MIME, "/onehippo.gif");
        servlet.service(request, response);
        Assert.assertThat("Whitelisted request should give resource.", response.getStatus(), is(200));
        Assert.assertThat("Response should be of type image/gif.", response.getContentType(), is("image/gif"));
    }

    @Test
    public void testMisconfiguredWhitelistedServlet() throws ServletException, IOException {
        final MockHttpServletRequest request = getMockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final ResourceServlet servlet = initializeServlet(true, PROPERTIES_RESOURCE_PATH, PROPERTIES_RESOURCE_MIME, "/onehippo.gif");
        servlet.service(request, response);
        Assert.assertThat("Misconfigured request should give 404.", response.getStatus(), is(404));
    }

    private ResourceServlet initializeServlet(final boolean requireAuthentication) throws ServletException {
        return initializeServlet(requireAuthentication, null, null);
    }

    private ResourceServlet initializeServlet(final boolean requireAuthentication, final String allowedResourcePaths, final String mimeTypes) throws ServletException {
        return initializeServlet(requireAuthentication, allowedResourcePaths, mimeTypes, null);
    }

    private ResourceServlet initializeServlet(final boolean requireAuthentication, final String allowedResourcePaths, final String mimeTypes, final String whitelistedResourcePaths) throws ServletException {
        final ResourceServlet servlet = new ResourceServlet();
        final MockServletConfig servletConfig = new MockServletConfig();
        servletConfig.addInitParameter("jarPathPrefix", "META-INF/test");
        servletConfig.addInitParameter("requireAuthentication", requireAuthentication ? "true" : "false");
        if (!StringUtils.isEmpty(allowedResourcePaths)) {
            servletConfig.addInitParameter("allowedResourcePaths", allowedResourcePaths);
        }

        if (!StringUtils.isEmpty(mimeTypes)) {
            servletConfig.addInitParameter("mimeTypes", mimeTypes);
        }

        if (!StringUtils.isEmpty(whitelistedResourcePaths)) {
            servletConfig.addInitParameter("whitelistedResourcePaths", whitelistedResourcePaths);
        }
        servlet.init(servletConfig);
        return servlet;
    }

    private void fakeLoggedinUser(final MockHttpServletRequest request) {
        final HttpSession mockedSession = new MockHttpSession();
        mockedSession.setAttribute(CmsSessionContext.class.getName(), new MockSessionContext());
        request.setSession(mockedSession);
    }

    class MockSessionContext implements CmsSessionContext {

        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getCmsContextServiceId() {
            return null;
        }

        @Override
        public Object get(final String s) {
            return null;
        }
    }

    private static MockHttpServletRequest getMockHttpServletRequest() {
        final MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,video/x-mng,image/png,image/jpeg,image/gif;q=0.2,*/*;q=0.1");
        request.addHeader("Accept-Encoding", "gzip,deflate");
        request.setMethod("GET");
        request.setServletPath("/resources");
        request.setPathInfo("/onehippo.gif");
        return request;
    }

}
