/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.model;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Application;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.session.SessionClassLoader;
import org.hippoecm.frontend.session.WorkflowManagerDecorator;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrSessionModel extends LoadableDetachableModel {

    // The jcr session is wrapped in a LoadableDetachableModel because it can't be serialized
    // and therefore cannot be a direct field of the wicket session. Wrapping the jcr session
    // like this has the added bonus of being a very simple reconnect mechanism.
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrSessionModel.class);

    private ValueMap credentials;
    private transient ClassLoader classLoader = null;
    private transient WorkflowManager workflowManager = null;

    public JcrSessionModel(ValueMap credentials) {
        this.credentials = credentials;
    }

    public void logout() {
        log.info("[" + getRemoteAddr() + "] Logout as " + credentials.getStringValue("username") + " from Hippo CMS 7");
        Session session = (Session) getObject();
        if (session != null) {
            session.logout();
            detach();
        }
        credentials = new ValueMap();
        //goto home instead of loginpage
        //throw new RestartResponseException(Home.class);
    }

    public void flush() {
        Session session = (Session) getObject();
        if (session != null) {
            session.logout();
            detach();
        }
        classLoader = null;
        workflowManager = null;
    }

    public ValueMap getCredentials() {
        return credentials;
    }
    
    public Session getSession() {
        try {
            Session session = (Session) getObject();
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
        return (Session) getObject();
    }

    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            Session session = getSession();
            if (session != null) {
                classLoader = new SessionClassLoader(session);
            }
        }
        return classLoader;
    }

    public WorkflowManager getWorkflowManager() {
        if (workflowManager == null) {
            try {
                HippoWorkspace workspace = (HippoWorkspace) getSession().getWorkspace();
                workflowManager = new WorkflowManagerDecorator(workspace.getWorkflowManager(), getClassLoader());
            } catch (RepositoryException ex) {
                ex.printStackTrace();
                workflowManager = null;
            }
        }
        return workflowManager;
    }

    @Override
    protected Object load() {
        javax.jcr.Session result = null;
        try {
            Main main = (Main) Application.get();
            HippoRepository repository = main.getRepository();

            String username = credentials.getString("username");
            String password = credentials.getString("password");

            if (repository != null && username != null && password != null) {
                result = repository.login(username, password.toCharArray());
            }
        } catch (LoginException e) {
            log.info("[" + getRemoteAddr() + "] Invalid login as user: " + credentials.getString("username"));
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }

        if (result == null) {
            Main main = (Main) Application.get();
            main.resetConnection();
            throw new RestartResponseException(Home.class);
        }
        return result;
    }
    
    /**
     * Helper method for logging
     * @return ip address of client
     */
    private String getRemoteAddr() {
        return ((WebRequestCycle)RequestCycle.get()).getWebRequest().getHttpServletRequest().getRemoteAddr();
    }
}
