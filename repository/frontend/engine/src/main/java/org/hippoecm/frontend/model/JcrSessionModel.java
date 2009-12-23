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
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Application;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.servlet.AbortWithHttpStatusException;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.Main;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.standardworkflow.EventLoggerWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Session model that uses the Main application to construct a JCR session.  When the model is attached,
 * pending changes are persisted.
 * <p>
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

    private final IValueMap credentials;

    public JcrSessionModel(IValueMap credentials) {
        this.credentials = credentials;
    }

    protected void flush() {
        Session session = getObject();
        if (session != null) {
            log.debug("Flushing session of {}", session.getUserID());
            if (session.isLive()) {
                try {
                    session.save();
                    if (session.getRootNode().hasNode("hippo:log")) {
                        try {
                            Workflow workflow = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager().getWorkflow(
                                    "internal", session.getRootNode().getNode("hippo:log"));
                            if (workflow instanceof EventLoggerWorkflow) {
                                ((EventLoggerWorkflow) workflow).logEvent(session.getUserID(), "Repository", "logout");
                            }
                        } catch (AccessDeniedException e) {
                            log.debug("Access denied when logging logout", e);
                        }
                    }
                } catch (RepositoryException e) {
                    log.error("Error when logging out", e);
                } catch (RemoteException e) {
                    log.error("Remote error when logging out", e);
                }
                session.logout();
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
        log.info("[" + getRemoteAddr() + "] Logout as " + credentials.getStringValue("username") + " from Hippo CMS 7");
        if (isAttached()) {
            flush();
        }
        super.detach();
    }

    @Override
    protected Session load() {
        javax.jcr.Session result = null;
        boolean fatalError = false;
        try {
            Main main = (Main) Application.get();
            HippoRepository repository = main.getRepository();
            String username = credentials.getString("username");
            String password = credentials.getString("password");
            if (repository != null && username != null && password != null) {
                result = repository.login(username, password.toCharArray());
                try {
                    if (result.getRootNode().hasNode(HippoNodeType.LOG_PATH)
                            && result.getRootNode().getNode(HippoNodeType.LOG_PATH).getProperty("hippolog:enabled")
                                    .getBoolean()) {
                        Workflow workflow = ((HippoWorkspace) result.getWorkspace()).getWorkflowManager().getWorkflow(
                                "internal", result.getRootNode().getNode(HippoNodeType.LOG_PATH));
                        if (workflow instanceof EventLoggerWorkflow) {
                            ((EventLoggerWorkflow) workflow).logEvent(result.getUserID(), "Repository", "login");
                        }
                        result.getRootNode().getNode(HippoNodeType.LOG_PATH).refresh(true);
                    }
                } catch (AccessDeniedException e) {
                    log.debug("Unable to log login event (maybe trying as Anonymous?): " + e.getMessage());
                } catch (RepositoryException e) {
                    log.error("RepositoryException while logging login event", e);
                } catch (RemoteException e) {
                    log.error("RemoteException while logging login event", e);
                }
            }
        } catch (LoginException e) {
            log.info("[" + getRemoteAddr() + "] Invalid login as user: " + credentials.getString("username"));
        } catch (RepositoryException e) {
            fatalError = true;
            log.error("Unable to obtain repository instance, aborting.", e);
        }

        if (fatalError) {
            // there's no sense in continuing
            throw new AbortWithHttpStatusException(503, false);
        }
        return result;
    }

    /**
     * Helper method for logging
     * @return ip address of client
     */
    private String getRemoteAddr() {
        return ((WebRequestCycle) RequestCycle.get()).getWebRequest().getHttpServletRequest().getRemoteAddr();
    }
}
