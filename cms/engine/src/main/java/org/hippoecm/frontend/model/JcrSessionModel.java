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
package org.hippoecm.frontend.model;

import java.rmi.RemoteException;

import javax.jcr.AccessDeniedException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.wicket.Application;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.servlet.AbortWithHttpStatusException;
import org.hippoecm.frontend.Main;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.standardworkflow.EventLoggerWorkflow;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Session model that uses the Main application to construct a JCR session.  When the model is attached, pending
 * changes are persisted.
 * <p/>
 * Plugins can subclass this model to refine the way the session is obtained.
 */
public class JcrSessionModel extends LoadableDetachableModel<Session> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    // The jcr session is wrapped in a LoadableDetachableModel because it can't be serialized
    // and therefore cannot be a direct field of the wicket session. Wrapping the jcr session
    // like this has the added bonus of being a very simple reconnect mechanism.
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrSessionModel.class);

    private UserCredentials credentials;
    private String remoteAddress;
    private boolean saveOnExit = true;

    public JcrSessionModel(UserCredentials credentials) {
        this.credentials = credentials;
        this.remoteAddress = ((WebRequestCycle) RequestCycle.get()).getWebRequest().getHttpServletRequest().getRemoteAddr();
    }

    protected void flush() {
        Session session = getObject();
        if (session != null) {
            log.debug("Flushing session of {}", session.getUserID());
            if (session.isLive()) {
                if (saveOnExit) {
                    try {
                        session.refresh(true);
                        session.save();
                    } catch (RepositoryException e) {
                        log.error("Error when logging out", e);
                    }
                }
                session.logout();
                final HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
                if (eventBus != null) {
                    final HippoEvent event = new HippoEvent("cms").user(session.getUserID()).action("logout");
                    event.sealEvent();
                    eventBus.post(event);
                }
            }
            super.detach();
        }
    }

    public Session getSession() {
        try {
            Session session = getObject();
            if (session == null) {
                return null;
            }
            if (!session.isLive()) {
                detach();
            }
        } catch (Exception e) {
            detach();
        }
        // this will call load() only if detached
        return getObject();
    }

    @Override
    public void detach() {
        if (log.isInfoEnabled()) {
            String username = (credentials != null ? credentials.getUsername() : null);
            if (username != null && username.length() > 0) {
                // don't log logouts from anonymous (eg login screen)
                log.info("[" + getRemoteAddr() + "] Logout as " + username + " from Hippo CMS 7");
            }
        }
        if (isAttached()) {
            flush();
        }
        super.detach();
    }

    @Override
    protected Session load() {
        javax.jcr.Session session = null;
        boolean fatalError = false;
        String username = (credentials != null ? credentials.getUsername() : null);
        try {
            session = login(credentials);
            if (session == null) {
                return null;
            }
            try {
                logLogin(session);
                if (isSystemUser(session)) {
                    session = null;
                    log.warn("[" + getRemoteAddr() + "] Login not allowed for system user: " + username);
                } else {
                    log.info("[" + getRemoteAddr() + "] Login by: " + username);
                }
            } catch (AccessDeniedException e) {
                log.debug("Unable to log login event (maybe trying as Anonymous?): " + e.getMessage());
            } catch (RepositoryException e) {
                log.error("RepositoryException while logging login event", e);
            } catch (RemoteException e) {
                log.error("RemoteException while logging login event", e);
            }
        } catch (LoginException e) {
            log.info("[" + getRemoteAddr() + "] Invalid login as user: " + username);
        } catch (RepositoryException e) {
            fatalError = true;
            log.error("Unable to obtain repository instance, aborting.", e);
        }

        if (fatalError) {
            // there's no sense in continuing
            throw new AbortWithHttpStatusException(503, false);
        }
        return session;
    }

    public static Session login(UserCredentials credentials) throws LoginException, RepositoryException {
        Main main = (Main) Application.get();
        HippoRepository repository = main.getRepository();
        if (credentials != null) {
            return repository.getRepository().login(credentials.getJcrCredentials());
        } else {
            return null;
        }
    }

    private void logLogin(Session session) throws RepositoryException, RemoteException {
        final HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus != null) {
            final HippoEvent event = new HippoEvent("cms").user(session.getUserID()).action("login").set("remoteAddress", getRemoteAddr());
            event.sealEvent();
            eventBus.post(event);
        }
    }


    protected boolean isSystemUser(Session session) {
        try {
            Node userNode = getUserNode(session);
            if (userNode != null && userNode.hasProperty("hipposys:system")) {
                return userNode.getProperty("hipposys:system").getBoolean();
            }
        } catch (RepositoryException e) {
            log.warn("Unable to determine is user is a system user: {}", e.getMessage());
            log.debug("Error while determining system user status:", e);
        }
        return false;
    }

    protected Node getUserNode(Session session) throws RepositoryException {
        String userId = session.getUserID();
        StringBuilder statement = new StringBuilder();
        statement.append("//element");
        statement.append("(*, ").append(HippoNodeType.NT_USER).append(")");
        statement.append('[').append("fn:name() = ").append("'").append(NodeNameCodec.encode(userId, true)).append(
                "'").append(']');
        Query q;
        q = session.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.XPATH);
        QueryResult result = q.execute();
        if (result.getNodes().hasNext()) {
            return result.getNodes().nextNode();
        }
        return null;
    }


    /**
     * Helper method for logging
     *
     * @return ip address of client
     */
    private String getRemoteAddr() {
        return remoteAddress;
    }

    public void setSaveOnExit(final boolean saveOnExit) {
        this.saveOnExit = saveOnExit;
    }
}
