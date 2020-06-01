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
package org.hippoecm.hst.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * GenericHttpServletRequestWrapper
 * @version $Id$
 */
public class GenericHttpServletRequestWrapper extends HttpServletRequestWrapper {
    
    protected String contextPath;
    protected String requestURI;
    protected StringBuffer requestURL;
    protected String servletPath;
    protected String pathInfo;
    protected String pathTranslated;

    public GenericHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }
    
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
    
    @Override
    public String getContextPath() {
        return (contextPath != null ? contextPath : super.getContextPath());
    }
    
    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    @Override
    public String getRequestURI() {
        return (requestURI != null ? requestURI : super.getRequestURI());
    }

    public void setRequestURL(StringBuffer requestURL) {
        this.requestURL = requestURL;
    }

    @Override
    public StringBuffer getRequestURL() {
        return (requestURL != null ? requestURL : super.getRequestURL());
    }

    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }
    
    @Override
    public String getServletPath() {
        return (servletPath != null ? servletPath : super.getServletPath());
    }
    
    public void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }
    
    @Override
    public String getPathInfo() {
        return (pathInfo != null ? pathInfo : super.getPathInfo());
    }
    
    public void setPathTranslated(String pathTranslated) {
        this.pathTranslated = pathTranslated;
    }

    @Override
    public String getPathTranslated() {
        return (pathTranslated != null ? pathTranslated : super.getPathTranslated());
    }

}