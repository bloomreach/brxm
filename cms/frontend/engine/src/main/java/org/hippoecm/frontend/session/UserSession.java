/*
 *  Copyright 2008 Hippo.
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

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryManager;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.FacetSearchObserver;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.InvalidLoginPage;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.NoRepositoryAvailablePage;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Wicket {@link org.apache.wicket.Session} that maintains a reference
 * to a JCR {@link javax.jcr.Session}.  It is available to plugins as a
 * threadlocal variable during request processing.
 * <p>
 * When the Wicket session is no longer referenced, the JCR session model
 * is detached.
 */
public class UserSession extends WebSession {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(UserSession.class);

    public final static ValueMap DEFAULT_CREDENTIALS = new ValueMap("username=,password=");

    static final Map<UserSession, JcrSessionReference> jcrSessions = new WeakHashMap<UserSession, JcrSessionReference>();

    private IValueMap credentials;
    private final IModel<ClassLoader> classLoader;
    private final IModel<WorkflowManager> workflowManager;
    private FacetSearchObserver facetSearchObserver;

    public UserSession(Request request) {
        this(request, new JcrSessionModel(DEFAULT_CREDENTIALS));
    }

    @Deprecated
    public UserSession(Request request, JcrSessionModel sessionModel) {
        this(request, (IModel<Session>) sessionModel);
    }
    
    public UserSession(Request request, IModel<Session> sessionModel) {
        super(request);

        classLoader = new LoadableDetachableModel<ClassLoader>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected ClassLoader load() {
                Session session = getJcrSessionInternal();
                if (session != null) {
                    try {
                        return ((HippoSession) session).getSessionClassLoader();
                    } catch (RepositoryException ex) {
                        log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                    }
                }
                return null;
            }

        };

        workflowManager = new LoadableDetachableModel<WorkflowManager>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected WorkflowManager load() {
                Session jcrSession = getJcrSessionInternal();
                if (jcrSession != null) {
                    try {
                        HippoWorkspace workspace = (HippoWorkspace) jcrSession.getWorkspace();
                        return workspace.getWorkflowManager();
                    } catch (RepositoryException ex) {
                        ex.printStackTrace();
                    }
                }
                return null;
            }

        };

        doLogin(DEFAULT_CREDENTIALS, sessionModel);

        //Calling the dirty() method causes this wicket session to be reset in the http session
        //so that it knows that the wicket session has changed (we've just added the jcr session model etc.)
        dirty();
    }

    protected IModel<Session> getJcrSessionModel() {
        synchronized (jcrSessions) {
            JcrSessionReference ref = jcrSessions.get(this);
            if (ref != null) {
                return ref.jcrSession;
            }
            return null;
        }
    }

    /**
     * Retrieve the JCR {@link javax.jcr.Session} that is bound to the Wicket {@link org.apache.wicket.Session}.
     * This method will throw a RestartResponseException when no JCR session is available.
     */
    public Session getJcrSession() {
        Session session = getJcrSessionInternal();
        if (session != null) {
            return session;
        }
        Main main = (Main) Application.get();
        main.resetConnection();
        throw new RestartResponseException(NoRepositoryAvailablePage.class);
    }

    protected final Session getJcrSessionInternal() {
        IModel<Session> sessionModel = getJcrSessionModel();
        if (sessionModel != null) {
            Session result = getJcrSessionModel().getObject();
            if (result != null && result.isLive()) {
                return result;
            }
        }
        return null;
    }

    /**
     * Release the JCR {@link javax.jcr.Session} that is bound to the Wicket session.  The session
     * model will take care of saving any pending changes.
     */
    public void releaseJcrSession() {
        IModel<Session> sessionModel = getJcrSessionModel();
        if (sessionModel != null) {
            getJcrSessionModel().detach();
        }
        classLoader.detach();
        workflowManager.detach();
        facetSearchObserver = null;
    }

    /**
     * The credentials that were used to login, or DEFAULT_CREDENTIALS if no login has taken place yet.
     * <p>
     * Use of this method is deprecated; use getJcrSession().getUserID() instead to obtain the user name.
     */
    @Deprecated
    public IValueMap getCredentials() {
        return credentials;
    }

    public void login(IValueMap credentials) {
        login(credentials, new JcrSessionModel(credentials));
    }

    public void login(IValueMap credentials, IModel<Session> sessionModel) {
        if (sessionModel.getObject() == null) {
            Main main = (Main) Application.get();
            main.resetConnection();
            throw new RestartResponseException(InvalidLoginPage.class);
        }

        doLogin(credentials, sessionModel);
    }

    void doLogin(IValueMap credentials, IModel<Session> sessionModel) {
        this.credentials = credentials;
        classLoader.detach();
        workflowManager.detach();
        facetSearchObserver = null;
        IModel<Session> oldModel = null;
        synchronized (jcrSessions) {
            JcrSessionReference sessionRef = jcrSessions.get(this);
            if (sessionRef != null) {
                oldModel = sessionRef.jcrSession;
            } else {
                sessionRef = new JcrSessionReference(this);
                jcrSessions.put(this, sessionRef);
            }
            sessionRef.jcrSession = sessionModel;
        }
        if (oldModel != null) {
            oldModel.detach();
        }
    }

    public void logout() {
        classLoader.detach();
        workflowManager.detach();
        facetSearchObserver = null;

        doLogin(DEFAULT_CREDENTIALS, new JcrSessionModel(DEFAULT_CREDENTIALS));

        ((WebRequest) RequestCycle.get().getRequest()).getHttpServletRequest().getSession(false).invalidate();
        dirty();
        throw new RestartResponseException(WebApplication.get().getHomePage());
    }

    /**
     * Retrieve the JCR session classloader, if available, or null when this is not the case.
     */
    public ClassLoader getClassLoader() {
        return classLoader.getObject();
    }

    /**
     * Retrieve the Hippo workflow manager, if one is available.  When none is, null is returned.
     */
    public WorkflowManager getWorkflowManager() {
        return workflowManager.getObject();
    }

    /**
     * Retrieve the JCR query manager, when one is available.  When none is, null is returned.
     */
    public QueryManager getQueryManager() {
        Session jcrSession = getJcrSessionInternal();
        if (jcrSession != null) {
            try {
                return jcrSession.getWorkspace().getQueryManager();
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        return null;
    }

    /**
     * Retrieve the JCR root node.  Null is returned when no session is available or the root
     * node cannot be obtained from it.
     */
    public HippoNode getRootNode() {
        HippoNode result = null;
        try {
            Session jcrSession = getJcrSessionInternal();
            if (jcrSession != null) {
                result = (HippoNode) jcrSession.getRootNode();
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return result;
    }

    @Override
    protected void detach() {
        JcrSessionReference.cleanup();
        super.detach();
    }

    void unbind() {
        JcrSessionReference ref;
        synchronized (jcrSessions) {
            ref = jcrSessions.remove(this);
        }
        if (ref != null) {
            ref.jcrSession.detach();
        }
    }
    
    /**
     * THIS METHOD IS NOT PART OF THE PUBLIC API AND SHOULD NOT BE INVOKED BY PLUGINS
     */
    public FacetSearchObserver getFacetSearchObserver() {
        if (facetSearchObserver == null) {
            facetSearchObserver = new FacetSearchObserver(getJcrSession());
        }
        return facetSearchObserver;
    }

    private Map<String, Integer> pluginComponentCounters = new HashMap<String, Integer>();

    // Do not add the @Override annotation on this
    public Object getMarkupId(Component component) {
        String markupId = null;
        for (Component ancestor = component.getParent(); ancestor != null && markupId == null; ancestor = ancestor
                .getParent()) {
            if (ancestor instanceof IPlugin || ancestor instanceof Home) {
                markupId = ancestor.getMarkupId(true);
                break;
            }
        }
        if (markupId == null) {
            return "root";
        }
        int componentNum = 0;
        if (pluginComponentCounters.containsKey(markupId)) {
            componentNum = pluginComponentCounters.get(markupId).intValue();
        }
        ++componentNum;
        pluginComponentCounters.put(markupId, new Integer(componentNum));
        return markupId + "_" + componentNum;
    }
}
