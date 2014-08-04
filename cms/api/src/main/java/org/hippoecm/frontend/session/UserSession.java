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
package org.hippoecm.frontend.session;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;

import org.apache.wicket.ThreadContext;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.hippoecm.frontend.observation.IFacetRootsObserver;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.WorkflowManager;

/**
 * A Wicket {@link org.apache.wicket.Session} that maintains a reference
 * to a JCR {@link javax.jcr.Session}.  It is available to plugins as a
 * threadlocal variable during request processing.
 * <p>
 * When the Wicket session is no longer referenced, the JCR session model
 * is detached.
 */
public abstract class UserSession extends WebSession {
    private static final long serialVersionUID = 8123464713164870284L;

    /**
     * Returns the current session, or if there is no current session, creates one.
     */
    public static UserSession get() {
        return (UserSession) org.apache.wicket.Session.get();
    }

    public UserSession(Request request) {
        super(request);
    }

    public abstract ClassLoader getClassLoader();

    public abstract QueryManager getQueryManager();

    public abstract WorkflowManager getWorkflowManager();

    public abstract ObservationManager getObservationManager();

    public abstract IFacetRootsObserver getFacetRootsObserver();

    public abstract HippoNode getRootNode();

    public abstract void login(String username, String password) throws LoginException;

    public abstract void logout();

    public abstract Session getJcrSession();
    
    public abstract void releaseJcrSession();

    public abstract String getApplicationName();

    public abstract HippoRepository getHippoRepository() throws RepositoryException;

}
