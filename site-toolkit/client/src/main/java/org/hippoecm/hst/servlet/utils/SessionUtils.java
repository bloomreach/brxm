/*
 *  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.servlet.utils;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for getting and returning sessions from the hst session pool.
 */
public final class SessionUtils {

    private static final Logger log = LoggerFactory.getLogger(SessionUtils.class);

    /**
     * Hide constructor of utility class
     */
    private SessionUtils() {
    }

    public static Session getBinariesSession(final HttpServletRequest request) throws RepositoryException {
        return getPooledSession(request, "binaries");
    }

    public static Session getPooledSession(final HttpServletRequest request, final String poolName) throws RepositoryException {
        Session session = getSessionFromRequest(request);
        if (session == null && HstServices.isAvailable()) {
            session = getSessionFromHstServices(poolName);
        }
        return session;
    }

    public static void releaseSession(final HttpServletRequest request, final Session session) {
        if (session == null) {
            return;
        }
        try {
            if (session != getSessionFromRequest(request)) {
                session.logout();
            }
        } catch (RepositoryException re) {
            HstRequest hstRequest = HstRequestUtils.getHstRequest(request);
            if (hstRequest == null) {
                session.logout();
            }
        }
    }

    private static Session getSessionFromRequest(final HttpServletRequest request) throws RepositoryException {
        HstRequestContext requestContext = RequestContextProvider.get();
        if (requestContext != null) {
            try {
                final Session session = requestContext.getSession();
                log.debug("Return HstRequestContext#getSession '{}' for Request pathInfo '{}'",
                        session.getUserID(), request.getPathInfo());
                return session;
            } catch (IllegalStateException e) {
                log.warn("HstRequestContext is already disposed for Request pathInfo '{}'. Return null for jcr session " +
                        "from request.", request.getPathInfo());
            }
        }
        return null;
    }

    private static Session getSessionFromHstServices(final String poolName) throws RepositoryException {
        Repository repository = getRepositoryFromHstServices();
        Session session = null;
        if (repository != null) {
            Credentials credentials = getCredentialsFromHstServices(poolName);
            if (credentials != null) {
                session = repository.login(credentials);
            } else {
                session = repository.login();
            }
        }
        return session;
    }
    
    private static Repository getRepositoryFromHstServices() {
        return HstServices.getComponentManager().getComponent(Repository.class.getName());
    }

    private static Credentials getCredentialsFromHstServices(final String type) {
        return HstServices.getComponentManager().getComponent(Credentials.class.getName() + "." + type);
    }

}