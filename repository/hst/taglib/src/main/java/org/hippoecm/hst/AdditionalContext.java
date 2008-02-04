/*
 * Copyright 2007 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdditionalContext implements Filter {
    public static final Logger logger = LoggerFactory.getLogger(AdditionalContext.class);

    private String location;
    private String attributename;

    public void init(FilterConfig filterConfig) throws ServletException {
        String param;

        param = filterConfig.getInitParameter("location");
        if (param != null && !param.trim().equals("")) {
            location = param;
        } else
            throw new ServletException("Missing parameter location in "+filterConfig.getFilterName());

        param = filterConfig.getInitParameter("attributename");
        if (param != null && !param.trim().equals("")) {
            attributename = param.trim();
        } else
            throw new ServletException("Missing parameter attributename in "+filterConfig.getFilterName());
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException,
            ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        Session jcrSession = JCRConnector.getJCRSession(req.getSession());
        Context context = new Context(jcrSession, null);
        context.setPath(location);
        req.setAttribute(attributename, context);

        filterChain.doFilter(req, res);
    }
}
