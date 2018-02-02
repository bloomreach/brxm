/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.filters;

import org.onehippo.cms7.essentials.plugin.sdk.utils.ProjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * Check if essentials can run properly,
 * e.g if all required settings like project.basedir are set properly.
 */
public class RequirementsCheckFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(RequirementsCheckFilter.class);

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain) throws IOException, ServletException {
        try {
            /*
             * IllegalStateException exception will be thrown
             * if there is no project directory set
             */
            final String directory = ProjectUtils.getBaseProjectDirectory();
            final File file = new File(directory);
            if (!file.exists()) {
                sendRedirect(req, res, "Directory: " + directory + " does not exist or is not accessible by java process");
                return;
            }
            if (!file.isDirectory()) {
                sendRedirect(req, res, "File: " + directory + " must be a directory");
                return;
            }
        } catch (IllegalStateException e) {
            if (log.isDebugEnabled()) {
                log.error("Error processing request:", e);
            }
            sendRedirect(req, res, e.getMessage());
            return;
        }
        chain.doFilter(req, res);
    }

    private void sendRedirect(final ServletRequest req, final ServletResponse resp, final String error) throws IOException {
        final HttpServletResponse response = (HttpServletResponse) resp;
        log.error(error);
        req.setAttribute("error", error);
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, error);
    }

    @Override
    public void destroy() {

    }
}
