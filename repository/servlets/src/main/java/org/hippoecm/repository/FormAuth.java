/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class FormAuth {

    private static final Logger log = LoggerFactory.getLogger(FormAuth.class);
    private static final String AUTHORIZED = "cms-user-authorized";
    private static final String AUTHORIZATION_CREDENTIALS = "cms-user-credentials";

    private static Template formAuthRenderingTemplate;

    public static final Session authorize(HttpServletRequest request, HippoRepository repository) throws ServletException, IOException {

        HttpSession httpSession = request.getSession();
        if(httpSession.getAttribute(AUTHORIZED) != null){
            return login(request, (SimpleCredentials) httpSession.getAttribute(AUTHORIZATION_CREDENTIALS), repository);
        }

        SimpleCredentials credentials = parseAuthorizationForm(request);
        if(credentials != null){
            Session hippoSession = login(request, credentials, repository);
            if(hippoSession != null){
                httpSession.setAttribute(AUTHORIZED, true);
                httpSession.setAttribute(AUTHORIZATION_CREDENTIALS, credentials);
                return hippoSession;
            }
        }

        return null;
    }

    public static Session login(HttpServletRequest request, SimpleCredentials credentials, HippoRepository repository){
        Session hippoSession;
        try {
            if (credentials.getUserID() == null || credentials.getUserID().length() == 0) {
                hippoSession = repository.login();
            } else {
                hippoSession = repository.login(credentials);
            }
            if (((HippoSession) hippoSession).getUser().isSystemUser()) {
                final InetAddress address = InetAddress.getByName(request.getRemoteHost());
                if (!address.isAnyLocalAddress() && !address.isLoopbackAddress()) {
                    throw new LoginException();
                }
            }
            return hippoSession;
        } catch (Exception e){
            return null;
        }
    }

    public static void logout(HttpServletRequest request){
        request.getSession().removeAttribute(AUTHORIZED);
        request.getSession().removeAttribute(AUTHORIZATION_CREDENTIALS);
    }

    public static synchronized final void showLoginPage(HttpServletRequest request, HttpServletResponse response, String message) throws ServletException, IOException {
        PrintWriter out = null;
        try {
            out = response.getWriter();
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("request", request);
            context.put("response", response);
            context.put("message", message);
            if( formAuthRenderingTemplate == null) {
                formAuthRenderingTemplate = getRenderTemplate(request);
            }
            formAuthRenderingTemplate.process(context, out);
            out.flush();
        } catch (TemplateException e) {
            log.warn("Failed to render freemarker template.", e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static SimpleCredentials parseAuthorizationForm(HttpServletRequest request) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(password)){
            return null;
        }
        return new SimpleCredentials(username, password.toCharArray());
    }

    private static Template getRenderTemplate(final HttpServletRequest request) throws IOException, TemplateException {
        Configuration cfg = new Configuration();

        cfg.setObjectWrapper(new DefaultObjectWrapper());
        cfg.setTemplateLoader(new ClassTemplateLoader(FormAuth.class, ""));

        InputStream propsInput = null;
        final String propsResName = FormAuth.class.getSimpleName() + "-ftl.properties";

        try {
            propsInput = FormAuth.class.getResourceAsStream(propsResName);
            cfg.setSettings(propsInput);
            return cfg.getTemplate(FormAuth.class.getSimpleName() + "-html.ftl");
        } finally {
            IOUtils.closeQuietly(propsInput);
        }
    }
}
