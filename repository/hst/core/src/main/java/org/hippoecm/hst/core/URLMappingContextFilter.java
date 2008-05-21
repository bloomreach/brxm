/*
 * Copyright 2008 Hippo.
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
import java.net.URLDecoder;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Filter that creates a context available for expression language and that  
 * supports url mapping.  
 */
public class URLMappingContextFilter extends ContextFilter {

    public static final String ENCODING_SCHEME = "UTF-8";

    public static final String URL_BASE_PATH = URLMappingContextFilter.class.getName() + ".CURRENT_URL_BASE_PATH";
    public static final String REPOSITORY_BASE_LOCATION = ContextFilter.class.getName() + ".CURRENT_REPOSITORY_BASE_LOCATION";

    private String urlBasePath = "/";
    private String urlMappingLocation = null;
    private static final String DEFAULT_URL_MAPPING_LOCATION = "/urlmapping";

    public URLMappingContextFilter() {
        super();
    }

    // from interface
    public void init(FilterConfig filterConfig) throws ServletException {

        super.init(filterConfig);

        // get urlBasePath
        String param = filterConfig.getInitParameter("urlBasePath");
        if (param != null && !param.trim().equals("")) {
            this.urlBasePath = param;
        }

        // if repositoryBaseLocation not there, default to urlBasePath
        param = filterConfig.getInitParameter("repositoryBaseLocation");
        if (param == null) {
            this.repositoryBaseLocation = urlBasePath;
        }
    }

    // javadoc from super
    boolean shouldCallFilterChain(final Context context, final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        storeSessionAttributes(request.getSession());
        
        // start URL mapping
        String relativeURL = request.getRequestURI();

        // remove session-related part of url
        int semicolonIdx = relativeURL.indexOf(';');
        if (semicolonIdx != -1) {
            relativeURL = relativeURL.substring(0, semicolonIdx);
        }

        // URL decode
        relativeURL = URLDecoder.decode(relativeURL, ENCODING_SCHEME);

        // remove contextPath before setting the relative location as the context has 
        // no idea of that part of the path
        if (relativeURL.startsWith(request.getContextPath())) {
            relativeURL = relativeURL.substring(request.getContextPath().length());
        }

        URLPathTranslator urlPathTranslator = new URLPathTranslator(request.getContextPath(), context.getURLBasePath(),
                context.getBaseLocation());
        String documentPath = urlPathTranslator.urlToDocumentPath(relativeURL);

        // set translated location 
        context.setRelativeLocation(documentPath);

        try {
            URLMappingResponseWrapper responseWrapper = new URLMappingResponseWrapper(context, 
                    urlPathTranslator, request, response);
            String mappedPage = responseWrapper.mapRepositoryDocument(context.getLocation(), 
                    getURLMappingLocation(request));

            // mapping allowed to fail, use-case is a /images url with this filter in root
            if (mappedPage != null) {

                // forward the request to that page
                RequestDispatcher dispatcher = request.getRequestDispatcher(mappedPage);

                if (dispatcher == null) {
                    throw new ServletException("No dispatcher could be obtained for mapped page " + mappedPage);
                }

                dispatcher.forward(request, responseWrapper);

                // no further filter chaining when forwarding
                return false;
            }
        } catch (RepositoryException re) {
            throw new ServletException(re);
        }
        
        return true;
    }

    // javadoc from super
    Context createContext(Session jcrSession, HttpServletRequest request) {
        return new Context(jcrSession, request.getContextPath(), this.urlBasePath, this.repositoryBaseLocation);
    }

    private String getURLMappingLocation(HttpServletRequest request) {

        // lazy
        if (this.urlMappingLocation == null) {
        
            // by configuration
            String urlMappingLocation = HSTConfiguration.get(request.getSession().getServletContext(), 
                HSTConfiguration.KEY_REPOSITORY_URLMAPPING_LOCATION, false/*not required*/);
            
            // by default
            if (urlMappingLocation == null) {
                urlMappingLocation = DEFAULT_URL_MAPPING_LOCATION;
            }

            this.urlMappingLocation = urlMappingLocation;
        }

        return this.urlMappingLocation;
    }
    
    /*
     * Save some data in session for use by other objects, e.g. BinariesServlet;
     * don't do it lazily as the user might switch filters, e.g. from 'live' to 
     * 'preview' in one session
     */   
    void storeSessionAttributes(HttpSession session) {
        session.setAttribute(REPOSITORY_BASE_LOCATION, this.repositoryBaseLocation);
        session.setAttribute(URL_BASE_PATH, this.urlBasePath);
    }
}
