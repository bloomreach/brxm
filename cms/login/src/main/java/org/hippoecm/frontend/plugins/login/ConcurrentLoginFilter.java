/*
 *  Copyright 2009-2020 Hippo B.V. (http://www.onehippo.com)
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
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;


/**
 * Backward compatibility code. To be completely removed in next major version
 * @deprecated since 14.2.0
 */
@Deprecated
public class ConcurrentLoginFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws java.io.IOException, ServletException {
        //Backward compatibility code. To be removed completely in next major version
        chain.doFilter(request, response);
    }

    public static void validateSession(HttpSession session, String user, boolean allowConcurrent) {
        //Backward compatibility code. To be completely removed in next major version
    }

    static void destroySession(HttpSession session) {
        //Backward compatibility code. To be completely removed in next major version
    }
}

