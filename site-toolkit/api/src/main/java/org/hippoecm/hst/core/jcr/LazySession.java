/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.jcr;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoSession;

/**
 * LazySession interface.
 * 
 * @version $Id$
 */
public interface LazySession extends HippoSession {
    
    /**
     * Invokes logout() of the underlying session.
     */
    void logoutSession() throws RepositoryException;

    /**
     * <p>
     *     Returns a {@link Session} exactly the same as via {@link Session#impersonate(Credentials)}, however keeps
     *     a reference to the impersonated session in this {@link LazySession} such that when invoking
     *     {@link #logoutCoupledImpersonations()} all the sessions created by this method get logged out.
     * </p>
     * @param credentials the credentials to impersonate from
     * @return new impersonated {@link Session}
     */
    Session coupledImpersonate(Credentials credentials);

    /**
     * Logs out any impersonated session impersonated via {@link #coupledImpersonate} from this {@link LazySession}
     */
    void logoutCoupledImpersonations();
    
    /**
     * Returns the last refreshed time millis.
     */
    long lastRefreshed();
    
    /**
     * Returns the last logged in time mills
     */
    long lastLoggedIn();
    
    /**
     * Returns the pending time millis after when the session should be refreshed.
     */
    long getRefreshPendingAfter();

    
}
