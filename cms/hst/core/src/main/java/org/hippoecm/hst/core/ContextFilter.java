/*
 * Copyright 2007-2008 Hippo.
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
package org.hippoecm.hst.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.jcr.Session;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.jcr.JCRConnector;
import org.hippoecm.hst.util.LocaleFactory;
import org.hippoecm.hst.util.RepositoryLocaleFactory;

//import org.slf4j.LoggerFactory;
//import org.slf4j.Logger;

/**
 * Filter that creates a context available for expression language.  
 */
public class ContextFilter implements Filter {

    // private static final Logger logger = LoggerFactory.getLogger(ContextFilter.class);
    
    private static final String KEY_LOCALE_FACTORY_CLASS = "locale.factory.class";
    private static final Class DEFAULT_LOCALE_FACTORY_CLASS = RepositoryLocaleFactory.class;
    
    private String attributeName = "context";
    String repositoryBaseLocation = "/";

    private String[] ignoreExtensions = new String[] { "ico", "gif", "jpg", "jpeg", "svg", "png", "css", "js" };
    private final List<String> ignoreExtensionsList = new ArrayList<String>();
    
    private LocaleFactory localeFactory;

    /**
     * Constructor
     */
    public ContextFilter() {
        super();
    }

    // from interface
    public void init(FilterConfig filterConfig) throws ServletException {

        // get attributeName
        String param = filterConfig.getInitParameter("attributeName");
        if (param != null && !param.trim().equals("")) {
            this.attributeName = param.trim();
        }

        // get repositoryBaseLocation
        param = filterConfig.getInitParameter("repositoryBaseLocation");
        if (param != null && !param.trim().equals("")) {
            this.repositoryBaseLocation = param;
        }

        // get ignoreExtensions
        param = filterConfig.getInitParameter("ignoreExtensions");
        if (param != null && !param.trim().equals("")) {
            this.ignoreExtensions = param.split(",");
        }

        // convert extensions to list for easy 'contains' access
        for (int i = 0; i < ignoreExtensions.length; i++) {
            String extension = ignoreExtensions[i].trim();
            if (!extension.startsWith(".")) {
                extension = "." + extension;
            }
            ignoreExtensionsList.add(extension);
        }
    }

    // from interface
    public void destroy() {
    }

    // from interface
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException,
            ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // check skipped extensions, images may be retrieved with the BinariesServlet
        String servletPath = req.getServletPath();

        if (servletPath.lastIndexOf(".") >= 0) {
            String extension = servletPath.substring(servletPath.lastIndexOf("."));

            if (ignoreExtensionsList.contains(extension)) {
                filterChain.doFilter(req, res);
                return;
            }
        }

        // create context and set in request
        Session jcrSession = JCRConnector.getJCRSession(req.getSession());
        Context context = createContext(jcrSession, req);
        req.setAttribute(attributeName, context);
        context.setLocale(getLocale(req, attributeName));

        if (shouldCallFilterChain(context, req, res)) {
            filterChain.doFilter(req, res);
        }
    }

    /**
     * A hook for subclasses to forward or redirect and not calling filterChain.doFilter. 
     */
    boolean shouldCallFilterChain(final Context context, final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException, IOException {
        return true;
    }

    /**
     * Create a (initial) context object. 
     */
    Context createContext(Session jcrSession, HttpServletRequest request) {
        return new Context(jcrSession, request.getContextPath(), request.getRequestURI(), this.repositoryBaseLocation);
    }

    private Locale getLocale(HttpServletRequest request, String contextName) {
        
        if (localeFactory == null) {

            // class name by configuration
            String localeFactoryClassName = HSTConfiguration.get(request.getSession().getServletContext(), 
                KEY_LOCALE_FACTORY_CLASS, false/*not required*/);
            
            // instantiate
            try {
                Class localeFactoryClass = DEFAULT_LOCALE_FACTORY_CLASS;
                
                if (localeFactoryClassName != null) {
                    localeFactoryClass = Class.forName(localeFactoryClassName);
                }
                
                this.localeFactory = (LocaleFactory) localeFactoryClass.newInstance();
            } 
            catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        
        return localeFactory.getLocale(request, contextName);
    }
}
