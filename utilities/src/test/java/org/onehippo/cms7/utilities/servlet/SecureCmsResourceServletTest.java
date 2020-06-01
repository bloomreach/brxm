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

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletConfig;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.onehippo.cms7.utilities.servlet.ResourceServletTest.getMockHttpServletRequest;

public class SecureCmsResourceServletTest {

    @Test
    public void testUnauthorizedServlet() throws ServletException, IOException {
        final MockHttpServletRequest request = getMockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final ResourceServlet servlet = initializeServlet();
        servlet.service(request, response);
        assertThat("Unauthorized request should give 404.", response.getStatus(), is(404));
    }

    @Test
    public void testAuthorizedServlet() throws ServletException, IOException {
        final MockHttpServletRequest request = getMockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final ResourceServlet servlet = initializeServlet();
        fakeLoggedinUser(request);
        servlet.service(request, response);
        assertThat("Authorized request should give resource.", response.getStatus(), is(200));
        assertThat("Response should be of type image/gif.", response.getContentType(), is("image/gif"));
    }


    private ResourceServlet initializeServlet() throws ServletException {
        final SecureCmsResourceServlet servlet = new SecureCmsResourceServlet();
        final MockServletConfig servletConfig = new MockServletConfig();
        servletConfig.addInitParameter("jarPathPrefix", "META-INF/test");

        servlet.init(servletConfig);
        return servlet;
    }

    private void fakeLoggedinUser(final MockHttpServletRequest request) {
        final HttpSession mockedSession = new MockHttpSession();
        mockedSession.setAttribute("hippo:username", "admin");
        request.setSession(mockedSession);
    }
}
