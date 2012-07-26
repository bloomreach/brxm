/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.plugins.login;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class ConcurrentLoginFilter implements Filter {

    final private static String ATTRIBUTE_SESSIONMATCH = ConcurrentLoginFilter.class.getName() + ".match";

    final private static String ATTRIBUTE_SESSIONUSER = ConcurrentLoginFilter.class.getName() + ".user";

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws java.io.IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)request;
        HttpSession session = req.getSession(false);

        if (session != null && session.getAttribute(ATTRIBUTE_SESSIONUSER) != null) {
            ServletContext context = session.getServletContext();
            String user = (String)session.getAttribute(ATTRIBUTE_SESSIONUSER);
            String current = (String)session.getAttribute(ATTRIBUTE_SESSIONMATCH);
            String match = (String)context.getAttribute(ATTRIBUTE_SESSIONMATCH + "." + user);
            if (current == null || (!current.equals("*") && !current.equals(match))) {
                session.invalidate();
            }
        }

        chain.doFilter(req, response);
    }

    static boolean isConcurrentSession(HttpSession session, String user) {
        ServletContext context = session.getServletContext();
        String match = (String)context.getAttribute(ATTRIBUTE_SESSIONMATCH + "." + user);
        String current = (String)session.getAttribute(ATTRIBUTE_SESSIONMATCH);
        if (match != null) {
            if (current == null || !(current.equals("*") || current.equals(match))) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    static void validateSession(HttpSession session, String user, boolean allowConcurrent) {
        ServletContext context = session.getServletContext();
        String match = (String)context.getAttribute(ATTRIBUTE_SESSIONMATCH + "." + user);
        if (allowConcurrent && match != null) {
            session.setAttribute(ATTRIBUTE_SESSIONMATCH, "*");
        } else {
            String id = session.getId();
            session.setAttribute(ATTRIBUTE_SESSIONMATCH, id);
            context.setAttribute(ATTRIBUTE_SESSIONMATCH + "." + user, id);
        }
        session.setAttribute(ATTRIBUTE_SESSIONUSER, user);
    }

    static void destroySession(HttpSession session) {
        ServletContext context = session.getServletContext();
        String user = (String)session.getAttribute(ATTRIBUTE_SESSIONUSER);
        if (user != null) {
            String current = (String)session.getAttribute(ATTRIBUTE_SESSIONMATCH);
            String match = (String)context.getAttribute(ATTRIBUTE_SESSIONMATCH + "." + user);
            if (current != null && current.equals(match)) {
                context.removeAttribute(ATTRIBUTE_SESSIONMATCH + "." + user);
            }
        }
        session.removeAttribute(ATTRIBUTE_SESSIONUSER);
        session.removeAttribute(ATTRIBUTE_SESSIONMATCH);
    }
}
