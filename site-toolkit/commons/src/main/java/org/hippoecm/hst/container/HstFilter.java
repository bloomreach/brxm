/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HstFilter working in the frontend of HST web application.
 * This is a <code>DelegatingFilter</code> which delegates calls
 * to a component bean named as FQCN of {@link HstFilter} in HST container assembly.
 */
public class HstFilter extends DelegatingFilter {

    private static Logger log = LoggerFactory.getLogger(HstFilter.class);

    private static final String DELEGATEE_NAME = HstFilter.class.getName();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!HstServices.isAvailable()) {
            if (response instanceof HttpServletResponse) {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
            log.error("The HST Container Services are not initialized yet.");
            return;
        }

        super.doFilter(request, response, chain);
    }

    @Override
    protected String getDelegateeName() {
        return DELEGATEE_NAME;
    }
}
