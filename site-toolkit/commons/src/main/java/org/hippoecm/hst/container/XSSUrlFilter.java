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
package org.hippoecm.hst.container;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple XSS Url attack protection blocking access whenever the request url contains a &lt; or &gt; character.
 * @version $Id: XSSUrlFilter.java 516448 2007-03-09 16:25:47Z ate $
 */
public class XSSUrlFilter implements Filter {

    public void init(FilterConfig config) throws ServletException {
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
            ServletException {
        if (req instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) req;

            if (!isValidRequest(request)) {
                ((HttpServletResponse) res).sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }

        chain.doFilter(req, res);
    }

    private boolean isValidRequest(HttpServletRequest request) {
        String queryString = request.getQueryString();
        
        if (queryString != null && containsMarkups(queryString)) {
            return false;
        }
        
        String requestURI = request.getRequestURI();
        
        if (requestURI != null && containsMarkups(requestURI)) {
            return false;
        }
        
        return true;
    }

    public static boolean containsMarkups(String value) {
        if (value.indexOf('<') != -1 || value.indexOf('>') != -1) {
            return true;
        }
        
        if (value.indexOf("%3C") != -1 || value.indexOf("%3c") != -1) {
            return true;
        }
        
        if (value.indexOf("%3E") != -1 || value.indexOf("%3e") != -1) {
            return true;
        }
        
        return false;
    }

    public void destroy() {
    }
}
