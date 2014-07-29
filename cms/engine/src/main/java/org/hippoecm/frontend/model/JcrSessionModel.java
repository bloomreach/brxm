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
package org.hippoecm.frontend.model;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.wicket.Application;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException;
import org.hippoecm.frontend.Main;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.event.HippoEventConstants;
import org.onehippo.cms7.event.HippoSecurityEvent;
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

    // The jcr session is wrapped in a LoadableDetachableModel because it can't be serialized
    // and therefore cannot be a direct field of the wicket session. Wrapping the jcr session
    // like this has the added bonus of being a very simple reconnect mechanism.
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrSessionModel.class);

    private UserCredentials credentials;
    private String remoteAddress;
    private boolean saveOnExit = true;
    private LoginException lastThrownLoginException;

    // although only written to, this instance variable is important : It serves to make sure
    // the transientModelObject jcr session gets logged out during serialization
    private TransientJCrSessionWrapper transientJcrSessionWrapper;

    public JcrSessionModel(UserCredentials credentials) {
        this.credentials = credentials;
        this.remoteAddress = ((ServletWebRequest) RequestCycle.get().getRequest()).getContainerRequest().getRemoteAddr();
    }

    protected void flush() {
        Session session = getObject();
        if (session != null) {
            if (session.isLive()) {
                if (saveOnExit) {
                    try {
                        session.save();
                    } catch (RepositoryException e) {
                        log.error("Failed to save session before logging out", e);
                    }
                }
                session.logout();
                logHippoEvent(false, session.getUserID(), "logout", true);
            }
            transientJcrSessionWrapper = null;
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
        if (isAttached()) {
            flush();
        }
        super.detach();
    }

    @Override
    protected Session load() {
        Session session = null;
        boolean fatalError = false;
        try {
            if (credentials == null) {
                return null;
            }
            session = login(credentials);
            if (isSystemUser(session)) {
                // TODO when does this happen and when does this session gets logged out?
                logHippoEvent(true, credentials.getUsername(), "system user", false);
                return null;
            }
            logHippoEvent(true, credentials.getUsername(), "login successful", true);
        } catch (LoginException e) {
            logHippoEvent(true, credentials.getUsername(), "invalid credentials", false);
            lastThrownLoginException = e;
        } catch (RepositoryException e) {
            fatalError = true;
            log.error("Unable to obtain repository instance, aborting.", e);
        }

        if (fatalError) {
            // there's no sense in continuing
            throw new AbortWithHttpErrorCodeException(503, "Unable to load session");
        }
        transientJcrSessionWrapper = new TransientJCrSessionWrapper(session);
        return session;
    }

    public Session getSessionObject() throws LoginException {
        final Session session = getObject();

        try {
            if (lastThrownLoginException != null) {
                throw lastThrownLoginException;
            }
        } finally {
            // Clear the login exception for subsequent calls
            lastThrownLoginException = null;
        }

        return session;
    }

    public static Session login(UserCredentials credentials) throws LoginException, RepositoryException {
        Main main = (Main) Application.get();
        HippoRepository repository = main.getRepository();
        return repository.getRepository().login(credentials.getJcrCredentials());
    }

    private void logHippoEvent(final boolean login, final String user, final String message, boolean success) {
        final HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus != null) {
            final String action = login ? "login" : "logout";
            final HippoEvent event = new HippoSecurityEvent("cms").success(success).action(action)
                    .category(HippoEventConstants.CATEGORY_SECURITY).user(user).set("remoteAddress", getRemoteAddr())
                    .message(message);
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
            log.warn("Unable to determine if user is a system user: {}", e.getMessage());
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
