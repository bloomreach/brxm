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

public class ContextFilter implements Filter {
    public static final Logger logger = LoggerFactory.getLogger(ContextFilter.class);
    static String ATTRIBUTE = ContextFilter.class.getName() + ".ATTRIBUTE";

    private String urlbasehome;
    private String urlbasepath;
    private String attributename = "context";

    public void init(FilterConfig filterConfig) throws ServletException {
        String param;

        param = filterConfig.getInitParameter("urlbasehome");
        if (param != null && !param.trim().equals("")) {
            urlbasehome = param;
        } else
            urlbasehome = null;

        param = filterConfig.getInitParameter("urlbasepath");
        if (param != null && !param.trim().equals("")) {
            urlbasepath = param;
        } else
            throw new ServletException("Missing parameter urlbasepath in "+filterConfig.getFilterName());

        param = filterConfig.getInitParameter("attributename");
        if (param != null && !param.trim().equals("")) {
            attributename = param.trim();
            filterConfig.getServletContext().setAttribute(ATTRIBUTE, attributename);
        }
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException,
            ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String pathAfterContext = req.getRequestURI();
        /* This next part is better resolved by using filterConfig.getServletContext().getContextPath(), but
         * this is only available after Servlet API 2.5.
         */
        if (pathAfterContext.startsWith(req.getServletPath())) {
            pathAfterContext = pathAfterContext.substring(req.getServletPath().length());
        }
        if (pathAfterContext.startsWith(req.getContextPath())) {
            pathAfterContext = pathAfterContext.substring(req.getContextPath().length());
        }
        int semicolonIdx = pathAfterContext.indexOf(';');
        if (semicolonIdx != -1) {
            pathAfterContext = pathAfterContext.substring(0, semicolonIdx);
        }
        if (pathAfterContext.equals("") || pathAfterContext.endsWith("/")) {
            if (urlbasehome != null)
                pathAfterContext += urlbasehome;
            else if (pathAfterContext.endsWith("/"))
                pathAfterContext = pathAfterContext.substring(0,pathAfterContext.length()-1);
        }
      
        String documentPath = urlbasepath;
        if (documentPath == null) {
            documentPath = "/";
        }
        if (!documentPath.endsWith("/") && !pathAfterContext.startsWith("/")) {
            documentPath = documentPath + "/" + pathAfterContext;
        } else {
            documentPath = documentPath + pathAfterContext;
        }
        if(logger.isDebugEnabled()) {
            logger.debug("Using document path "+documentPath);
        }

        Session jcrSession = JCRConnector.getJCRSession(req.getSession());
        Context context = new Context(jcrSession, urlbasepath);
        context.setPath(documentPath);
        req.setAttribute(attributename, context);

        if(!doInternal(context, documentPath, req, res)) {
            filterChain.doFilter(req, res);
            return; // no action ALLOWED after this point.
        }
    }

    public boolean doInternal(Context context, String documentPath, HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
        return false;
    }
}
