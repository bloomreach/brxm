/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.session;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Application;
import org.apache.wicket.Request;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.LoginPage;
import org.hippoecm.frontend.Main;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserSession extends WebSession {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(UserSession.class);

    private JcrSessionModel jcrSessionModel;
    private String hippo;

    public UserSession(Request request) {
        super(request);
        jcrSessionModel = new JcrSessionModel();
        //Calling the dirty() method causes this wicket session to be reset in the http session
        //so that it knows that the wicket session has changed (we've just added the jcr session model etc.)
        dirty();
    }

    public Session getJcrSession() {
        return jcrSessionModel.getSession();
    }

    public void setJcrCredentials(ValueMap credentials) {
        jcrSessionModel = new JcrSessionModel(credentials);
    }

    public void logout() {
        jcrSessionModel.logout();
    }

    public ValueMap getCredentials() {
        return jcrSessionModel.getCredentials();
    }

    public ClassLoader getClassLoader() {
        return jcrSessionModel.getClassLoader();
    }

    public WorkflowManager getWorkflowManager() {
        return jcrSessionModel.getWorkflowManager();
    }

    public HippoNode getRootNode() {
        HippoNode result = null;
        try {
            Session jcrSession = jcrSessionModel.getSession();
            if (jcrSession != null) {
                result = (HippoNode) jcrSession.getRootNode();
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return result;
    }

    public void setHippo(String hippo) {
        this.hippo = hippo;
        dirty();
    }

    public String getHippo() {
        return this.hippo;
    }

    private class JcrSessionModel extends LoadableDetachableModel {
        // The jcr session is wrapped in a LoadableDetachableModel because it can't be serialized
        // and therefore cannot be a direct field of the wicket session. Wrapping the jcr session
        // like this has the added bonus of being a very simple reconnect mechanism.
        private static final long serialVersionUID = 1L;

        private ValueMap credentials;
        private transient ClassLoader classLoader = null;
        private transient WorkflowManager workflowManager = null;

        JcrSessionModel() {
            credentials = new ValueMap();
        }

        public JcrSessionModel(ValueMap credentials) {
            this.credentials = credentials;
        }

        void logout() {
            Session session = (Session) getObject();
            if (session != null) {
                session.logout();
                detach();
                credentials = new ValueMap();
            }
            throw new RestartResponseException(LoginPage.class);
        }

        ValueMap getCredentials() {
            return credentials;
        }

        Session getSession() {
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
                log.warn(e.getMessage());
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }

            if (result == null) {
                Main main = (Main) getApplication();
                main.resetConnection();
                throw new RestartResponseException(LoginPage.class);
            }
            return result;
        }

    }
}
