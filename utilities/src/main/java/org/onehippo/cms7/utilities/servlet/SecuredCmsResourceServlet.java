/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Serves resources from either web application or classpath.
 *
 * Can be used in cms only and checks whether the requester is logged in before serving the resource.
 */
public class SecuredCmsResourceServlet extends ResourceServlet {

    protected boolean isAllowed(final String resourcePath, final HttpServletRequest servletRequest) {
        if (!isUserLoggedIn(servletRequest)) {
            return false;
        }

        return super.isAllowed(resourcePath, servletRequest);
    }

    private static boolean isUserLoggedIn(final HttpServletRequest servletRequest) {
        final HttpSession httpSession = servletRequest.getSession(false);
        return httpSession != null && httpSession.getAttribute("hippo:username") != null;
    }


}
