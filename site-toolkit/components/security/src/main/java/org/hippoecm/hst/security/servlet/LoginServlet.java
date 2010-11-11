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
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.security.PolicyContextWrapper;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.ServletConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LoginServlet
 * <P>
 * The LoginServlet enables form-based JAAS login. 
 * The LoginServlet is able to processes form-based at the four different stage:
 * <UL>
 * <LI><EM>Login::Proxy</EM> - An html form submits to this servlet with login info, 
 * and then this servlet redirects to a secured resource, Login::Resource, which is configured in web.xml as security-constraint.
 * As the Login::Resource is requested, the servlet container will invoke the configured form-based login servlet path,
 * which is also configured in web.xml as login-config.
 * In this stage, this servlet stores the user's login information to be used later. 
 * </LI>
 * <LI><EM>Login::Login</EM> - Because the Login::Proxy mode redirects to the Login::Resource mode url in the previous stage,
 * the servlet container invokes this Login::Login mode servlet url which is configured in web.xml as login-config.
 * In this stage, this servlet forwards to a view page to write a hidden html form filled with the stored login information.
 * The hidden form will be submitted automatically to 'j_security_check', as soon as the page loaded.
 * </LI>
 * <LI><EM>Login::Resource</EM> - After authentication succeeds, the servlet container allows the Login::Resource url to the authenticated user.
 * However, because the Login::Resource url was used for internal purpose only, it should redirect to somewhere.
 * If 'destination' parameter was used at the Login::Proxy stage, then the destination url will be used to redirect.
 * Otherwise, it will redirect to the root servlet context path.
 * </LI>
 * <LI><EM>Login::Logout</EM> - A web site can provide a logout link which invoked this mode.
 * If 'destination' parameter was used for this url, then the destination url will be used to redirect after logout.
 * Otherwise, it will redirect to the root servlet context path after logout.
 * </LI>
 * </UL> 
 * </P>
 * <P>
 * Example servlet configuration:
 * <PRE><XMP>
 * <servlet>
 *   <servlet-name>LoginServlet</servlet-name>
 *   <servlet-class>org.hippoecm.hst.security.servlet.LoginServlet</servlet-class>
 * </servlet>
 * 
 * <servlet-mapping>
 *   <servlet-name>LoginServlet</servlet-name>
 *   <url-pattern>/login/*</url-pattern>
 * </servlet-mapping>
 * 
 * <security-constraint>
 *   <web-resource-collection>
 *     <web-resource-name>Login Resource</web-resource-name>
 *     <url-pattern>/login/resource</url-pattern>
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
    
    public static final String DESTINATION_ATTR_NAME = LoginServlet.class.getPackage().getName() + "." + DESTINATION;
    public static final String USERNAME_ATTR_NAME = LoginServlet.class.getPackage().getName() + "." + USERNAME;
    public static final String PASSWORD_ATTR_NAME = LoginServlet.class.getPackage().getName() + "." + PASSWORD;
    
    public static final String DEFAULT_LOGIN_RESOURCE_PATH = "/login/resource";
    
    public static final String MODE_LOGIN_FORM = "form";
    public static final String MODE_LOGIN_PROXY = "proxy";
    public static final String MODE_LOGIN_LOGIN = "login";
    public static final String MODE_LOGIN_RESOURCE = "resource";
    public static final String MODE_LOGIN_LOGOUT = "logout";
    public static final String MODE_LOGIN_ERROR = "error";
    
    private static final String RESOURCE_BUNDLE_BASE_NAME = LoginServlet.class.getName();
    
    private static Logger log = LoggerFactory.getLogger(LoginServlet.class);
    
    protected String requestCharacterEncoding;
    protected String loginFormPagePath;
    protected String loginResourcePath;
    protected String loginSecurityCheckFormPagePath;
    protected String loginErrorPagePath;
    
    private Map<String, String> templateContentMap = Collections.synchronizedMap(new HashMap<String, String>());
    
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        requestCharacterEncoding = ServletConfigUtils.getInitParameter(servletConfig, null, "requestCharacterEncoding", null);
        loginFormPagePath = ServletConfigUtils.getInitParameter(servletConfig, null, "loginFormPagePath", null);
        loginResourcePath = ServletConfigUtils.getInitParameter(servletConfig, null, "loginResource", DEFAULT_LOGIN_RESOURCE_PATH);
        loginSecurityCheckFormPagePath = ServletConfigUtils.getInitParameter(servletConfig, null, "loginSecurityCheckFormPagePath", null);
        loginErrorPagePath = ServletConfigUtils.getInitParameter(servletConfig, null, "loginErrorPage", null);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (requestCharacterEncoding != null) {
            request.setCharacterEncoding(requestCharacterEncoding);
        }
        
        String mode = getMode(request);
        
        if (MODE_LOGIN_FORM.equals(mode)) {
            doLoginForm(request, response);
        } else if (MODE_LOGIN_PROXY.equals(mode)) {
            doLoginProxy(request, response);
        } else if (MODE_LOGIN_RESOURCE.equals(mode)) {
            doLoginResource(request, response);
        } else if (MODE_LOGIN_LOGOUT.equals(mode)) {
            doLoginLogout(request, response);
        } else if (MODE_LOGIN_ERROR.equals(mode)) {
            doLoginError(request, response);
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
            String requestURI = HstRequestUtils.getRequestURI(request, true);
            mode = requestURI.substring(requestURI.lastIndexOf('/') + 1);
        }
        
        return mode;
    }
    
    protected void doLoginForm(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (loginFormPagePath != null) {
            request.getRequestDispatcher(loginFormPagePath).forward(request, response);
        } else {
            renderLoginFormPage(request, response);
        }
    }
    
    protected void doLoginProxy(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
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
        
        response.sendRedirect(response.encodeURL(request.getContextPath() + loginResourcePath));
    }
    
    protected void doLoginLogin(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Principal userPrincipal = request.getUserPrincipal();
        
        if (userPrincipal == null) {
            if (loginSecurityCheckFormPagePath != null) {
                request.getRequestDispatcher(loginSecurityCheckFormPagePath).forward(request, response);
            } else {
                renderAutoLoginPage(request, response);
            }
            
            return;
        }
        
        String destination = null;
        
        HttpSession session = request.getSession(false);
        
        if (session != null) {
            destination = (String) session.getAttribute(DESTINATION_ATTR_NAME);
        }
        
        destination = normalizeDestination(destination, request);

        response.sendRedirect(response.encodeURL(destination));
    }
    
    protected void doLoginResource(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String destination = null;
        
        HttpSession session = request.getSession(false);
        
        if (session != null) {
            if (!PolicyContextWrapper.isAvailable()) {
                Credentials repoCreds = createSubjectRepositoryCredentials(request);
                
                if (repoCreds != null) {
                    session.setAttribute(ContainerConstants.SUBJECT_REPO_CREDS_ATTR_NAME, repoCreds);
                }
            }
            
            session.removeAttribute(USERNAME_ATTR_NAME);
            session.removeAttribute(PASSWORD_ATTR_NAME);
            
            destination = (String) session.getAttribute(DESTINATION_ATTR_NAME);
            
            if (destination != null) {
                session.removeAttribute(DESTINATION_ATTR_NAME);
            }
        }
        
        destination = normalizeDestination(destination, request);
        
        response.sendRedirect(response.encodeURL(destination));
    }
    
    protected void doLoginLogout(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String destination = normalizeDestination(request.getParameter(DESTINATION), request);
        HttpSession session = request.getSession(false);
        
        if (session != null) {
            session.invalidate();
        }
        
        response.sendRedirect(response.encodeURL(destination));
    }
    
    protected void doLoginError(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (loginErrorPagePath != null) {
            request.getRequestDispatcher(loginErrorPagePath).forward(request, response);
        } else {
            renderLoginErrorPage(request, response);
        }
    }
    
    protected String normalizeDestination(String destination, HttpServletRequest request) {
        if (destination == null || "".equals(destination.trim())) {
            destination = request.getContextPath() + "/";
        }
        
        return destination;
    }
    
    /**
     * Creates repository credentials for the subject.
     * <P>
     * This method is invoked to store a repository credentials for the subject.
     * By default, this method creates a repository credentials with the same user/password credentials
     * used during authentication.
     * </P>
     * <P>
     * A child class can override this method to behave differently.
     * </P>
     * @param request
     * @return
     */
    protected Credentials createSubjectRepositoryCredentials(HttpServletRequest request) {
        String username = (String) request.getSession().getAttribute(USERNAME_ATTR_NAME);
        String password = (String) request.getSession().getAttribute(PASSWORD_ATTR_NAME);
        
        if (username != null && password != null) {
            return new SimpleCredentials(username, password.toCharArray());
        } else {
            log.warn("Invalid username or password: " + username);
        }
        
        return null;
    }
    
    protected void renderLoginFormPage(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String username = StringUtils.defaultString(request.getParameter(USERNAME));
        String destination = StringUtils.defaultString(request.getParameter(DESTINATION));
        
        HttpSession httpSession = request.getSession(false);
        
        if (httpSession != null) {
            if (StringUtils.isBlank(username)) {
                username = StringUtils.defaultString((String) httpSession.getAttribute(USERNAME_ATTR_NAME));
            }
            
            if (StringUtils.isBlank(destination)) {
                destination = normalizeDestination((String) httpSession.getAttribute(DESTINATION_ATTR_NAME), request);
            }
            
            if (BooleanUtils.toBoolean(request.getParameter("invalidate"))) {
                httpSession.invalidate();
            }
        }
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("j_username", username);
        params.put("destination", response.encodeURL(destination));
        
        renderTemplatePage(request, response, "login_form.vm", params);
    }
    
    protected void renderAutoLoginPage(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String jSecurityCheck = response.encodeURL("j_security_check");
        String username = "";
        String password = "";
        
        HttpSession httpSession = request.getSession(false);
        
        if (httpSession != null) {
            username = StringUtils.defaultString((String) httpSession.getAttribute(USERNAME_ATTR_NAME));
            password = StringUtils.defaultString((String) httpSession.getAttribute(PASSWORD_ATTR_NAME));
        }
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("j_security_check", jSecurityCheck);
        params.put("j_username", username);
        params.put("j_password", password);
        
        renderTemplatePage(request, response, "login_security_check.vm", params);
    }
    
    protected void renderLoginErrorPage(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String username = "";
        String destination = "";
        
        HttpSession httpSession = request.getSession(false);
        
        if (httpSession != null) {
            username = StringUtils.defaultString((String) httpSession.getAttribute(USERNAME_ATTR_NAME));
            destination = normalizeDestination((String) httpSession.getAttribute(DESTINATION_ATTR_NAME), request);
        }
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("j_username", username);
        params.put("destination", response.encodeURL(destination));
        
        renderTemplatePage(request, response, "login_failure.vm", params);
    }
    
    protected void renderTemplatePage(HttpServletRequest request, HttpServletResponse response, String templateResourcePath, Map<String, Object> params) throws IOException, ServletException {
        if (!templateContentMap.containsKey(templateResourcePath)) {
            InputStream input = null;
            
            try {
                input = getClass().getResourceAsStream(templateResourcePath);
                templateContentMap.put(templateResourcePath, IOUtils.toString(input, "UTF-8"));
            } finally {
                IOUtils.closeQuietly(input);
            }
        }
        
        String templateContent = templateContentMap.get(templateResourcePath);
        
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        Context context = new VelocityContext();
        
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                context.put(entry.getKey(), entry.getValue());
            }
        }
        
        try {
            Locale requestLocale = request.getLocale();
            ResourceBundle bundle = null;
            
            if (requestLocale != null) {
                bundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_BASE_NAME, request.getLocale());
            } else {
                bundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_BASE_NAME);
            }
            
            context.put("messages", bundle);
        } catch (Exception e) {
            log.warn("Cannot find resource bundle. " + RESOURCE_BUNDLE_BASE_NAME);
        }
        
        context.put("request", request);
        
        Velocity.evaluate(context, out, templateResourcePath, templateContent);
        
        out.flush();
    }
}

