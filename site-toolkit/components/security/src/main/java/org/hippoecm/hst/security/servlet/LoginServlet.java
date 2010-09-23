/*
 *  Copyright 2008 Hippo.
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

import java.io.IOException;
import java.security.Principal;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.util.ServletConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LoginServlet
 * <P>
 * Example servlet configuration:
 * <PRE><XMP>
 * <servlet>
 *   <servlet-name>LoginServlet</servlet-name>
 *   <servlet-class>org.hippoecm.hst.security.servlet.LoginProxyServlet</servlet-class>
 * </servlet>
 * 
 * <servlet-mapping>
 *   <servlet-name>LoginServlet</servlet-name>
 *   <url-pattern>/login/*</url-pattern>
 * </servlet-mapping>
 * 
 * <security-constraint>
 *   <web-resource-collection>
 *     <web-resource-name>Login</web-resource-name>
 *     <url-pattern>/login/redirector</url-pattern>
 *   </web-resource-collection>
 *   <auth-constraint>
 *     <role-name>everybody</role-name>
 *   </auth-constraint>
 * </security-constraint>
 * 
 * <login-config>
 *   <auth-method>FORM</auth-method>
 *   <realm-name>HSTSITE</realm-name>
 *   <form-login-config>
 *     <form-login-page>/login/login</form-login-page>
 *     <form-error-page>/WEB-INF/jsp/login-failure.jsp</form-error-page>
 *   </form-login-config>
 * </login-config>
 * 
 * <security-role>
 *   <description>Default role for every authenticated user</description>
 *   <role-name>everybody</role-name>
 * </security-role>
 * </XMP></PRE>
 * </P>
 * <P>
 * <EM>Note:</EM>
 * <UL>
 * <LI>To invoke login proxy url, use '/login/proxy' for the form action value. (e.g. action='/site/login/proxy')</LI>
 * <LI>To invoke logout, use '/login/logout' for the link. (e.g. href='/site/login/logout')</LI>
 * </P>
 * @version $Id$
 */
public class LoginServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;

    public static final String DESTINATION = "destination";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    
    public static final String DESTINATION_ATTR_NAME = LoginServlet.class.getName() + "." + DESTINATION;
    public static final String USERNAME_ATTR_NAME = LoginServlet.class.getName() + "." + USERNAME;
    public static final String PASSWORD_ATTR_NAME = LoginServlet.class.getName() + "." + PASSWORD;
    
    public static final String DEFAULT_LOGIN_REDIRECTOR_PATH = "/login/redirector";
    public static final String DEFAULT_LOGIN_FORM_PAGE_PATH = "/WEB-INF/jsp/login.jsp";
    
    public static final String MODE_LOGIN_PROXY = "proxy";
    public static final String MODE_LOGIN_LOGIN = "login";
    public static final String MODE_LOGIN_REDIRECT = "redirect";
    public static final String MODE_LOGIN_LOGOUT = "logout";
    
    private static Logger logger = LoggerFactory.getLogger(LoginServlet.class);
    
    private String loginRedirectorPath;
    private String loginFormPagePath;
    
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        loginRedirectorPath = ServletConfigUtils.getInitParameter(servletConfig, null, "loginRedirector", DEFAULT_LOGIN_REDIRECTOR_PATH);
        loginFormPagePath = ServletConfigUtils.getInitParameter(servletConfig, null, "loginFormPage", DEFAULT_LOGIN_FORM_PAGE_PATH);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String mode = getMode(request);
        
        if (MODE_LOGIN_PROXY.equals(mode)) {
            doLoginProxy(request, response);
        } else if (MODE_LOGIN_REDIRECT.equals(mode)) {
            doLoginRedirect(request, response);
        } else if (MODE_LOGIN_LOGOUT.equals(mode)) {
            doLoginLogout(request, response);
        } else {
            doLoginLogin(request, response);
        }
    }

    @Override
    public final void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
        doGet(request, response);
    }
    
    protected String getMode(HttpServletRequest request) {
        String mode = request.getParameter("mode");
        
        if (mode == null) {
            String requestURI = request.getRequestURI();
            mode = requestURI.substring(requestURI.lastIndexOf('/'));
        }
        
        return mode;
    }
    
    protected void doLoginProxy(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(true);
        
        String parameter = request.getParameter(DESTINATION);
        
        if (parameter != null) {
            session.setAttribute(DESTINATION_ATTR_NAME, parameter);
        } else {
            session.removeAttribute(DESTINATION_ATTR_NAME);
        }
        
        String username = request.getParameter(USERNAME);
        
        if (username != null) {
            session.setAttribute(USERNAME_ATTR_NAME, username);
        } else {
            session.removeAttribute(USERNAME_ATTR_NAME);
        }
        
        String password = request.getParameter(PASSWORD);
        
        if (password != null) {
            session.setAttribute(PASSWORD_ATTR_NAME, password);
        } else {
            session.removeAttribute(PASSWORD_ATTR_NAME);
        }
        
        response.sendRedirect(response.encodeURL(request.getContextPath() + loginRedirectorPath));
    }
    
    protected void doLoginLogin(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Principal userPrincipal = request.getUserPrincipal();
        
        if (userPrincipal == null) {
            request.getRequestDispatcher(loginFormPagePath).forward(request, response);
            return;
        }
        
        String destination = null;
        
        HttpSession session = request.getSession(false);
        
        if (session != null) {
            destination = (String) session.getAttribute(DESTINATION_ATTR_NAME);
        }
        
        if (destination == null) {
            destination = request.getContextPath() + "/";
        }

        response.sendRedirect(response.encodeURL(destination));
    }
    
    protected void doLoginRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String destination = null;
        
        HttpSession session = request.getSession(false);
        
        if (session != null) {
            destination = (String) session.getAttribute(DESTINATION_ATTR_NAME);
        }
        
        if (destination == null || destination.equals(request.getContextPath())) {
            destination = request.getContextPath() + "/";
        } else {
            session.removeAttribute(DESTINATION_ATTR_NAME);
        }

        session.removeAttribute(USERNAME_ATTR_NAME);
        session.removeAttribute(PASSWORD_ATTR_NAME);

        response.sendRedirect(response.encodeURL(destination));
    }
    
    protected void doLoginLogout(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String destination = request.getParameter(DESTINATION);
        HttpSession session = request.getSession(false);
        
        if (session != null) {
            session.invalidate();
        }
        
        if (destination == null) {
            destination = request.getContextPath() + "/";
        }
        
        response.sendRedirect(response.encodeURL(destination));

    }
}

