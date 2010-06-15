/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend;

import java.io.IOException;
import java.util.Random;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet filter that prefixes resources with an additional path element.
 * The prefix can be random (default) or configured explicitly.  The prefix is available for
 * lower filters and the servlet in a request parameter, to be used when generating resource
 * URLs.
 * <p>
 */
public class ResourceRewriteFilter implements Filter {

    static final Logger log = LoggerFactory.getLogger(ResourceRewriteFilter.class);
    
    /**
     * request wrapper that moves the prefix from the servlet path
     * to the context path.
     */
    class FilteredRequest extends HttpServletRequestWrapper {

        public FilteredRequest(HttpServletRequest request) {
            super(request);

            setAttribute(PREFIX_REQUEST_ATTRIBUTE, prefix.substring(1));
        }

        @Override
        public String getPathTranslated() {
            String superPath = super.getPathTranslated();
            String stripped = stripPrefix(superPath);
            if (log.isDebugEnabled()) {
                if (!stripped.equals(superPath)) {
                    log.debug("TranslatedTranslated Path from " + superPath + " to " + stripped);
                }
            }
            return stripped;
        }

        @Override
        public String getServletPath() {
            String superPath = super.getServletPath();
            String stripped = stripPrefix(superPath);
            if (log.isDebugEnabled()) {
                if (!stripped.equals(superPath)) {
                    log.debug("Translated ServletPath from " + superPath + " to " + stripped);
                }
            }
            return stripped;
        }

        @Override
        public String getContextPath() {
            if (super.getServletPath().startsWith(prefix)) {
                return super.getContextPath() + prefix;
            } else {
                return super.getContextPath();
            }
        }
        
        protected String stripPrefix(String path) {
            if (path == null) {
                return null;
            }
            if (path.startsWith(prefix)) {
                return path.substring(prefix.length());
            }
            return path;
        }
    }

    public static final String PREFIX_PARAMETER = "prefix";
    public static final String PREFIX_REQUEST_ATTRIBUTE = "org.hippoecm.frontend.prefix";

    private String prefix;

    public void init(FilterConfig config) throws ServletException {
        if (config.getInitParameter(PREFIX_PARAMETER) != null) {
            prefix = "/" + config.getInitParameter(PREFIX_PARAMETER);
        } else {
            Random rand = new Random();
            int value = rand.nextInt();
            StringBuilder sb = new StringBuilder("/");
            for (int i = 0; i < 8; i++) {
                int j = value & 0xf;
                value = value >> 4;
                if (j < 10) {
                    sb.append(j);
                } else {
                    sb.append((char) ('a' + (j - 10)));
                }
            }
            prefix = sb.toString();
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        chain.doFilter(new FilteredRequest((HttpServletRequest) request), response);
    }

    public void destroy() {
    }

}
