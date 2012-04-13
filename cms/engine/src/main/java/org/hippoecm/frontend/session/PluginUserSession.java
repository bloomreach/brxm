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

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.Request;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.FrontendNodeType;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.NoRepositoryAvailablePage;
import org.hippoecm.frontend.WebApplicationHelper;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.model.UserCredentials;
import org.hippoecm.frontend.observation.FacetRootsObserver;
import org.hippoecm.frontend.observation.JcrObservationManager;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Wicket {@link org.apache.wicket.Session} that maintains a reference to a JCR {@link javax.jcr.Session}.  It is
 * available to plugins as a threadlocal variable during request processing.
 * <p/>
 * When the Wicket session is no longer referenced, the JCR session model is detached.
 */
public class PluginUserSession extends UserSession {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    private static final String FRONTEND_APPLICATION_ABSOLUTE_PATH = "/hippo:configuration/hippo:frontend/";

    static final Logger log = LoggerFactory.getLogger(UserSession.class);

    private static Session fallbackSession = null;
    private static final Map<UserSession, JcrSessionReference> jcrSessions = new WeakHashMap<UserSession, JcrSessionReference>();

    private final IModel<ClassLoader> classLoader;
    private final IModel<WorkflowManager> workflowManager;
    private transient FacetRootsObserver facetRootsObserver;
    private UserCredentials credentials;

    public UserCredentials getUserCredentials() {
        return credentials;
    }

    public static void setCredentials(UserCredentials credentials) throws RepositoryException {
        fallbackSession = JcrSessionModel.login(credentials);
    }

    public PluginUserSession(Request request) {
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

        //Calling the dirty() method causes this wicket session to be reset in the http session
        //so that it knows that the wicket session has changed (we've just added the jcr session model etc.)
        dirty();
    }

    @Deprecated
    public PluginUserSession(Request request, LoadableDetachableModel<Session> jcrSessionModel) {
        super(request);
        classLoader = new LoadableDetachableModel<ClassLoader>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected ClassLoader load() {
                return null;
            }
        };
        workflowManager = new LoadableDetachableModel<WorkflowManager>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected WorkflowManager load() {
                return null;
            }
        };
        login((UserCredentials) null, jcrSessionModel);
        //Calling the dirty() method causes this wicket session to be reset in the http session
        //so that it knows that the wicket session has changed (we've just added the jcr session model etc.)
        dirty();
    }

    private IModel<Session> getJcrSessionModel() {
        synchronized (jcrSessions) {
            JcrSessionReference ref = jcrSessions.get(this);
            if (ref != null) {
                return ref.jcrSession;
            }
            return null;
        }
    }

    /**
     * Retrieve the JCR {@link javax.jcr.Session} that is bound to the Wicket {@link org.apache.wicket.Session}. This
     * method will throw a RestartResponseException when no JCR session is available.
     */
    public Session getJcrSession() {
        Session session = getJcrSessionInternal();
        if (session == null) {
            Main main = (Main) Application.get();
            if (fallbackSession == null) {
                try {
                    main.getRepository(); // side effect of reinitializing fallback session
                } catch (RepositoryException ex) {
                }
            }
            session = fallbackSession;
            if (session == null) {
                main.resetConnection();
                throw new RestartResponseException(NoRepositoryAvailablePage.class);
            }
        }
        return session;
    }

    private Session getJcrSessionInternal() {
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
     * Release the JCR {@link javax.jcr.Session} that is bound to the Wicket session.  The session model will take care
     * of saving any pending changes.  Event listeners will remain registered and will reregister with a new session.
     */
    public void releaseJcrSession() {
        IModel<Session> sessionModel = getJcrSessionModel();
        if (sessionModel != null) {
            getJcrSessionModel().detach();
        }
        classLoader.detach();
        workflowManager.detach();
        facetRootsObserver = null;
    }

    @Deprecated
    public boolean login(ValueMap credentials, LoadableDetachableModel<Session> jcrSessionModel) {
        return login(new UserCredentials(credentials.getString("username"), credentials.getString("password")),
                     jcrSessionModel);
    }

    @Deprecated
    public boolean login(ValueMap credentials) {
        return login(credentials, null);
    }

    public void login() {
        login((UserCredentials) null, null);
    }

    public void login(UserCredentials credentials) throws LoginException {
        boolean success = login(credentials, null);

        if (!success) {
            throw new LoginException(LoginException.CAUSE.INCORRECT_CREDENTIALS);
        }

        try {
            final Node applicationNode = getJcrSession().getNode(
                    FRONTEND_APPLICATION_ABSOLUTE_PATH + getApplicationName("cms"));
            if (applicationNode.hasProperty(FrontendNodeType.FRONTEND_SAVEONEXIT) && !applicationNode.getProperty(
                    FrontendNodeType.FRONTEND_SAVEONEXIT).getBoolean()) {
                IModel<Session> sessionModel = getJcrSessionModel();
                if (sessionModel instanceof JcrSessionModel) {
                    ((JcrSessionModel) sessionModel).setSaveOnExit(false);
                }
            }
        } catch (PathNotFoundException pne) {
            login();
            throw new LoginException(LoginException.CAUSE.ACCESS_DENIED, pne);
        } catch (RepositoryException re) {
            if (log.isDebugEnabled()) {
                log.warn("Error while accessing repository", re);
            } else {
                log.warn(String.format("Error while accessing repository {}"), re.toString());
            }
            login();
            throw new LoginException(LoginException.CAUSE.REPOSITORY_ERROR, re);
        }
    }

    @Deprecated
    public boolean login(UserCredentials credentials, LoadableDetachableModel<Session> sessionModel) {
        if (sessionModel == null) {
            sessionModel = new JcrSessionModel(credentials);
        }
        classLoader.detach();
        workflowManager.detach();
        facetRootsObserver = null;
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

        this.credentials = credentials;
        if (oldModel != null) {
            oldModel.detach();
        }
        if (sessionModel.getObject() == null) {
            return false;
        } else {
            return true;
        }
    }

    public void logout() {
        classLoader.detach();
        workflowManager.detach();
        facetRootsObserver = null;

        IModel<Session> oldModel = null;
        synchronized (jcrSessions) {
            JcrSessionReference sessionRef = jcrSessions.get(this);
            if (sessionRef != null) {
                oldModel = sessionRef.jcrSession;
                jcrSessions.remove(this);
            }
        }
        if (oldModel != null) {
            oldModel.detach();
        }
        JcrObservationManager.getInstance().cleanupListeners(this);

        invalidate();
        dirty();
        if (WebApplication.exists()) {
            throw new RestartResponseException(WebApplication.get().getHomePage());
        }
    }

    public Credentials getCredentials() {
        return credentials.getJcrCredentials();
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

    @Override
    public ObservationManager getObservationManager() {
        return JcrObservationManager.getInstance();
    }

    /**
     * Retrieve the JCR root node.  Null is returned when no session is available or the root node cannot be obtained
     * from it.
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

    public void flush() {
        JcrObservationManager.getInstance().cleanupListeners(this);
        ((UnbindingHttpSessionStore) getSessionStore()).setClearPageMaps(sessionId);
    }

    @SuppressWarnings("unused")
    private boolean bound = false;
    private String sessionId;

    void onBind(String sessionId) {
        this.bound = true;
        this.sessionId = sessionId;
    }

    void unbind() {
        releaseJcrSession();

        JcrObservationManager.getInstance().cleanupListeners(this);
        JcrSessionReference.cleanup();

        bound = false;
    }

    /**
     * THIS METHOD IS NOT PART OF THE PUBLIC API AND SHOULD NOT BE INVOKED BY PLUGINS
     */
    public FacetRootsObserver getFacetRootsObserver() {
        if (facetRootsObserver == null) {
            facetRootsObserver = new FacetRootsObserver(getJcrSession());
        }
        return facetRootsObserver;
    }

    private Map<String, Integer> pluginComponentCounters = new HashMap<String, Integer>();

    // Do not add the @Override annotation on this
    public Object getMarkupId(Component component) {
        String markupId = null;
        for (Component ancestor = component.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
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
        pluginComponentCounters.put(markupId, Integer.valueOf(componentNum));
        return markupId + "_" + componentNum;
    }

    public String getApplicationName(String defaultAppName) {
        String applicationName = getApplicationName();
        return StringUtils.isNotBlank(applicationName) ? applicationName : defaultAppName;
    }

    public String getApplicationName() {
        String applicationName;
        Session session = getJcrSession();
        String userID = session.getUserID();

        if (userID == null || userID.equals("") || userID.equalsIgnoreCase("anonymous")) {
            applicationName = "login";
        } else {
            applicationName = WebApplicationHelper.getConfigurationParameter((WebApplication) Application.get(),
                                                                             Main.PLUGIN_APPLICATION_NAME, null);
        }

        return applicationName;
    }

}
