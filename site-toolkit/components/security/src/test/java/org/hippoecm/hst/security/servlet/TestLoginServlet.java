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
package org.hippoecm.hst.security.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockRequestDispatcher;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

/**
 * TestLoginServlet
 * @version $Id$
 */
public class TestLoginServlet {

    private static Logger log = LoggerFactory.getLogger(TestLoginServlet.class);
    
    private LoginServlet loginServlet;
    private MockServletConfig servletConfig;
    
    @Before
    public void setUp() throws Exception {
        loginServlet = new LoginServlet();
        MockServletContext context = new MockServletContext() {
            @Override
            public RequestDispatcher getRequestDispatcher(String url) {
                return new MockRequestDispatcher(url);
            }
        };
        servletConfig = new MockServletConfig(context);
        loginServlet.init(servletConfig);
    }

    @Test
    public void testInit() throws ServletException {
        servletConfig.addInitParameter("requestCharacterEncoding", "UTF-8");
        servletConfig.addInitParameter("loginResource", "/login/resource2");
        servletConfig.addInitParameter("loginSecurityCheckFormPagePath", "/WEB-INF/jsp/login_security_check2.jsp");
        loginServlet.init(servletConfig);
        assertEquals("UTF-8", loginServlet.requestCharacterEncoding);
        assertEquals("/login/resource2", loginServlet.defaultLoginResourcePath);
        assertEquals("/WEB-INF/jsp/login_security_check2.jsp", loginServlet.defaultLoginSecurityCheckFormPagePath);
    }
    
    @Test
    public void testGetMode() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        request.setRequestURI("/site/login/proxy");
        String mode = loginServlet.getMode(request);
        assertEquals("proxy", mode);
        
        request.addParameter("mode", "logout");
        mode = loginServlet.getMode(request);
        assertEquals("logout", mode);
        request.removeAllParameters();
        
        request.setRequestURI("/site/login/resource");
        mode = loginServlet.getMode(request);
        assertEquals("resource", mode);
        
        request.setRequestURI("/site/login/login");
        mode = loginServlet.getMode(request);
        assertEquals("login", mode);
        
        request.setRequestURI("/site/login/logout");
        mode = loginServlet.getMode(request);
        assertEquals("logout", mode);
    }
    
    @Test
    public void testDoLoginForm() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        request.setContextPath("/site");
        request.setServletPath("/login");
        request.setPathInfo("/form");
        request.setRequestURI("/site/login/form");
        
        loginServlet.doLoginForm(request, response);

        assertEquals("Response status was not 200 but "+response.getStatus()+"", 200, response.getStatus());
    }
    
    @Test
    public void testDoLoginError() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        request.setContextPath("/site");
        request.setServletPath("/login");
        request.setPathInfo("/form");
        request.setRequestURI("/site/login/form");
        
        loginServlet.doLoginError(request, response);

        assertEquals("Response status was not 200 but "+response.getStatus()+"", 200, response.getStatus());
    }
    
    @Test
    public void testDoLoginProxy() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        request.setContextPath("/site");
        request.setRequestURI("/site/login/proxy");
        request.addParameter("destination", "/site/welcome.html");
        request.addParameter("username", "charley");
        request.addParameter("password", "brown");
        
        loginServlet.doLoginProxy(request, response);
        assertNotNull(request.getSession());
        assertEquals("/site/welcome.html", request.getSession().getAttribute(LoginServlet.DESTINATION_ATTR_NAME));
        assertEquals("charley", request.getSession().getAttribute(LoginServlet.USERNAME_ATTR_NAME));
        assertEquals("brown", request.getSession().getAttribute(LoginServlet.PASSWORD_ATTR_NAME));
        assertEquals("http://localhost/site/login/resource", response.getRedirectedUrl());
    }
    
    @Test
    public void testDoLoginLogin() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        loginServlet.doLoginLogin(request, response);
        
        log.info("Response:\n" + response.getContentAsString() + "\n");
        
        assertTrue(response.getContentAsString().contains("onload"));
        assertTrue(response.getContentAsString().contains(".submit"));
        assertTrue(response.getContentAsString().contains("j_security_check"));
        assertTrue(response.getContentAsString().contains("j_username"));
        assertTrue(response.getContentAsString().contains("j_password"));
        
        servletConfig.addInitParameter("loginSecurityCheckFormPagePath", "/WEB-INF/jsp/login_security_check.jsp");
        loginServlet.init(servletConfig);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        
        loginServlet.doLoginLogin(request, response);
        assertEquals("/WEB-INF/jsp/login_security_check.jsp", response.getForwardedUrl());
    }
    
    @Test
    public void testDoLoginResource() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        request.getSession().setAttribute(LoginServlet.DESTINATION_ATTR_NAME, "/site/welcome.html");
        request.getSession().setAttribute(LoginServlet.USERNAME_ATTR_NAME, "charley");
        request.getSession().setAttribute(LoginServlet.PASSWORD_ATTR_NAME, "brown");
        
        loginServlet.doLoginResource(request, response);
        assertNull(request.getSession().getAttribute(LoginServlet.DESTINATION_ATTR_NAME));
        assertNull(request.getSession().getAttribute(LoginServlet.USERNAME_ATTR_NAME));
        assertNull(request.getSession().getAttribute(LoginServlet.PASSWORD_ATTR_NAME));
        assertEquals("http://localhost/site/welcome.html", response.getRedirectedUrl());
    }
    
    @Test
    public void testDoLoginLogout() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        request.getSession().setAttribute("foo", "bar");
        request.addParameter("destination", "/site/welcome.html");
        
        loginServlet.doLoginLogout(request, response);
        assertNull(request.getSession().getAttribute("foo"));
        assertEquals("http://localhost/site/welcome.html", response.getRedirectedUrl());
    }
}
