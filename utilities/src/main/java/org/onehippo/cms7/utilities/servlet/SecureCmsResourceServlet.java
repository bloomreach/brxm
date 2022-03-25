/*
 *  Copyright 2017-2022 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.utilities.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * By default, this {@link SecureCmsResourceServlet} serves resources with extensions allowed from either web
 * application or classpath, just like {@link ResourceServlet} does. Optionally, this {@link SecureCmsResourceServlet}
 * can be configured to only serve the resources for requests which have logged in user if this is required for
 * some reason. To configure secured access, configure in the web.xml
 *
 * <pre>
 *     &lt;servlet&gt;
 *        &lt;servlet-name&gt;ServletName&lt;/servlet-name&gt;
 *        &lt;servlet-class&gt;org.onehippo.cms7.utilities.servlet.SecureCmsResourceServlet&lt;/servlet-class&gt;
 *        &lt;init-param&gt;
 *          &lt;param-name&gt;cmsSecure&lt;/param-name&gt;
 *          &lt;param-value&gt;true&lt;/param-value&gt;
 *        &lt;/init-param&gt;
 *        &lt;snip&gt;other init params&lt;/snip&gt;
 *      &lt;/servlet&gt;
 * </pre>
 * You can also opt for setting CORS headers for
 * <ol>
 *     <li>Access-Control-Allow-Origin = ${origin}</li>
 *     <li>Access-Control-Allow-Credentials = true</li>
 *     <li>Vary = Origin</li>
 * </ol>
 * By default these headers will be set unless you add
 * <pre>
 *        &lt;init-param&gt;
 *          &lt;param-name&gt;supportCors&lt;/param-name&gt;
 *          &lt;param-value&gt;false&lt;/param-value&gt;
 *        &lt;/init-param&gt;
 * </pre>
 * </p>
 *
 */
public class SecureCmsResourceServlet extends ResourceServlet {

    private static final Logger log = LoggerFactory.getLogger(SecureCmsResourceServlet.class);

    /**
     *  by default cmsSecure is false meaning you do not have to be authenticated to load files allowed via the allowlist (see
     *  ResourceServlet). if authenticated is needed, set init-param 'cmsSecure = true' in the web.xml
     */
    private boolean cmsSecure;
    private boolean supportCors;

    @Override
    public void init() throws ServletException {
        super.init();
        cmsSecure = Boolean.parseBoolean(getInitParameter("cmsSecure", "false"));
        supportCors = Boolean.parseBoolean(getInitParameter("supportCors", "true"));
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        if (supportCors) {
            final String origin = RequestUtils.getOrigin(req);
            if (origin == null) {
                log.info("Cannot set origin header for response since no origin found for request");
            } else {
                resp.setHeader("Access-Control-Allow-Origin", origin);
                resp.setHeader("Access-Control-Allow-Credentials", "true");
            }
            // Below set 'Vary' is really important because without it, the same resource is cached in the browser
            // regardless the Origin
            resp.setHeader("Vary", "Origin");
        }
        super.doGet(req, resp);
    }

    protected boolean isAllowed(final String resourcePath, final HttpServletRequest servletRequest) {
        if (cmsSecure && !isUserLoggedIn(servletRequest)) {
            return false;
        }

        return super.isAllowed(resourcePath, servletRequest);
    }

    private static boolean isUserLoggedIn(final HttpServletRequest servletRequest) {
        final HttpSession httpSession = servletRequest.getSession(false);
        return httpSession != null && httpSession.getAttribute("hippo:username") != null;
    }


}
