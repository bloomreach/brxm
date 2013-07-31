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
package org.hippoecm.hst.core.jcr;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * LazySession interface.
 * 
 * @version $Id$
 */
public interface LazySession extends Session {
    
    /**
     * Invokes logout() of the underlying session.
     */
    void logoutSession() throws RepositoryException;
    
    /**
     * Returns the last refreshed time millis.
     * @return
     */
    long lastRefreshed();
    
    /**
     * Returns the last logged in time mills
     * @return
     */
    long lastLoggedIn();
    
    /**
     * Returns the pending time millis after when the session should be refreshed.
     * @return
     */
    long getRefreshPendingAfter();

    /**
     * Does a localRefresh that does not get propagated in clustered environments to the database
     */
    void localRefresh();
    
}
