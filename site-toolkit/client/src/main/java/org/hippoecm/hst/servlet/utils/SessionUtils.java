/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.HstRequestUtils;

/**
 * Utility class for getting and returning sessions from the hst session pool.
 */
public final class SessionUtils {


    /**
     * Hide constructor of utility class
     */
    private SessionUtils() {
    }

    public static Session getBinariesSession(HttpServletRequest request) throws RepositoryException {
        return getPooledSession(request, "binaries");
    }

    public static Session getPooledSession(HttpServletRequest request, String poolName) throws RepositoryException {
        Session session = getSessionFromRequest(request);
        if (session == null && HstServices.isAvailable()) {
            session = getSessionFromHstServices(poolName);
        }
        return session;
    }

    public static void releaseSession(HttpServletRequest request, Session session) {
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

    private static Session getSessionFromRequest(HttpServletRequest request) throws RepositoryException {
        HstRequest hstRequest = HstRequestUtils.getHstRequest(request);
        if (hstRequest != null) {
            Session session = hstRequest.getRequestContext().getSession();
            if (session != null && session.isLive()) {
                return session;
            }
        }
        return null;
    }

    private static Session getSessionFromHstServices(String poolName) throws RepositoryException {
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

    private static Credentials getCredentialsFromHstServices(String type) {
        return HstServices.getComponentManager().getComponent(Credentials.class.getName() + "." + type);
    }

}