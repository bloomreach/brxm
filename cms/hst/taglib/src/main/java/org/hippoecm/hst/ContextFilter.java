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
package org.hippoecm.hst;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

//import org.slf4j.LoggerFactory;
//import org.slf4j.Logger;

/**
 * Filter that creates a context available for expression language.  
 */
public class ContextFilter implements Filter {

    //    private static final Logger logger = LoggerFactory.getLogger(ContextFilter.class);
    public static final String ENCODING_SCHEME = "UTF-8";

    public static final String ATTRIBUTE_NAME = ContextFilter.class.getName() + ".ATTRIBUTE_NAME";
    public static final String URL_BASE_PATH = ContextFilter.class.getName() + ".URL_BASE_PATH";
    public static final String REPOSITORY_BASE_LOCATION = ContextFilter.class.getName() + ".REPOSITORY_BASE_LOCATION";
    public static final String URL_MAPPING_LOCATION = ContextFilter.class.getName() + ".URL_MAPPING_LOCATION";

    private String attributeName = "context";
    private String urlBasePath = "";
    private String repositoryBaseLocation;
    private String urlMappingLocation = "/urlMapping";
    private String[] skippedExtensions = 
        new String[] { "ico", "gif", "jpg", "jpeg", "svg", "png", "css", "js" };
    private final List<String> skippedExtensionsList = new ArrayList<String>();
    boolean urlMappingActive = false;

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

        // get urlBasePath
        param = filterConfig.getInitParameter("urlBasePath");
        if (param != null && !param.trim().equals("")) {
            this.urlBasePath = param;
        }

        // get repositoryBaseLocation
        param = filterConfig.getInitParameter("repositoryBaseLocation");
        if (param != null && !param.trim().equals("")) {
            this.repositoryBaseLocation = param;
        } else {
            this.repositoryBaseLocation = urlBasePath;
        }

        // get urlMappingLocation
        param = filterConfig.getInitParameter("urlMappingLocation");
        if (param != null && !param.trim().equals("")) {
            this.urlMappingLocation = param;
        }

        // get skippedExtensions
        param = filterConfig.getInitParameter("skippedExtensions");
        if (param != null && !param.trim().equals("")) {
            this.skippedExtensions = param.split(",");
        }

        // convert extensions to list for easy 'contains' access
        for (int i = 0; i < skippedExtensions.length; i++) {
            String extension = skippedExtensions[i];
            if (!extension.startsWith(".")) {
                extension = "." + extension;
            }
            skippedExtensionsList.add(extension);
        }
    }

    // from interface
    public void destroy() {
    }

    // from interface
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) 
		    			throws IOException, ServletException {

        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        storeSessionAttributes(req.getSession());

        String servletPath = req.getServletPath();

        if (servletPath.lastIndexOf(".") >= 0) {
			// images are to be retrieved with the DirectAccessServlet        
            String extension = servletPath.substring(servletPath.lastIndexOf("."));

            if (skippedExtensionsList.contains(extension)) {
                filterChain.doFilter(req, res);
                return;
            }
        }

        String relativeURL = req.getRequestURI();

        // remove session-related part of url
        int semicolonIdx = relativeURL.indexOf(';');
        if (semicolonIdx != -1) {
            relativeURL = relativeURL.substring(0, semicolonIdx);
        }

        // URL decode
        relativeURL = URLDecoder.decode(relativeURL, ENCODING_SCHEME);

        Session jcrSession = JCRConnector.getJCRSession(req.getSession());
        Context context = new Context(jcrSession, urlBasePath, repositoryBaseLocation);

        // remove contextPath before setting the relative location as the context has 
        // no idea of that part of the path
        if (relativeURL.startsWith(req.getContextPath())) {
            relativeURL = relativeURL.substring(req.getContextPath().length());
        }
        
        context.setRelativeLocation(relativeURL);

        req.setAttribute(attributeName, context);

        // check url mapping
        if (urlMappingActive) {

            URLPathTranslator urlPathTranslator = new URLPathTranslator(req.getContextPath(), 
                                    context.getURLBasePath(), context.getBaseLocation());
            String documentPath = urlPathTranslator.urlToDocumentPath(relativeURL);

            // set translated location 
            context.setRelativeLocation(documentPath);

            try {
                URLMappingResponseWrapper responseWrapper = new URLMappingResponseWrapper(context, urlPathTranslator, req, res);
                String mappedPage = responseWrapper.mapRepositoryDocument(context.getLocation(), urlMappingLocation);

                // mapping allowed to fail, use-case is a /images url with this filter in root
                if (mappedPage != null) {

                    // forward the request to that page
                    RequestDispatcher dispatcher = request.getRequestDispatcher(mappedPage);

                    if (dispatcher == null) {
                        throw new ServletException("No dispatcher could be obtained for mapped page " + mappedPage);
                    }

                    dispatcher.forward(request, responseWrapper);

                    // no further filter chaining when forwarding
                    return;
                }   
            } catch (RepositoryException re) {
                throw new ServletException(re);
            }
        }

        // normally call rest of the filter
        filterChain.doFilter(req, res);
    }

    private void storeSessionAttributes(HttpSession session) {

        // save some data in session for use by tags or DirectAccessServlet;
        // don't do it lazily as the user might switch from 'live' to 'preview' in one session  
        session.setAttribute(ATTRIBUTE_NAME, this.attributeName);
        session.setAttribute(URL_BASE_PATH, this.urlBasePath);
        session.setAttribute(REPOSITORY_BASE_LOCATION, this.repositoryBaseLocation);
        session.setAttribute(URL_MAPPING_LOCATION, this.attributeName);
    }
}
