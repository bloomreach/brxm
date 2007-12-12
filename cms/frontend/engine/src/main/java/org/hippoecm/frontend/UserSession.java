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
package org.hippoecm.frontend;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Application;
import org.apache.wicket.Request;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.api.HippoNode;

public class UserSession extends WebSession {
    private static final long serialVersionUID = 1L;

    private JcrSessionModel jcrSessionModel;
    private String application;

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
        jcrSessionModel.logout();
        jcrSessionModel = new JcrSessionModel(credentials);
    }

    public void logout() {
        jcrSessionModel.logout();
    }

    public ValueMap getCredentials() {
        return jcrSessionModel.getCredentials();
    }

    public HippoNode getRootNode() {
        HippoNode result = null;
        try {
            Session jcrSession = jcrSessionModel.getSession();
            if (jcrSession != null) {
                result = (HippoNode) jcrSession.getRootNode();
            }
        } catch (RepositoryException e) {
            //
        }
        return result;
    }

    public void setFrontendApp(String application) {
        this.application = application;
    }

    public String getFrontendApp() {
        return this.application;
    }

    private class JcrSessionModel extends LoadableDetachableModel {
        // The jcr session is wrapped in a LoadableDetachableModel because it can't be serialized
        // and therefore cannot be a direct field of the wicket session. Wrapping the jcr session
        // like this has the added bonus of being a very simple reconnect mechanism.  
        private static final long serialVersionUID = 1L;

        private ValueMap credentials;

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

        @Override
        protected Object load() {
            javax.jcr.Session result = null;
            try {
                Main main = (Main) Application.get();
                HippoRepository repository = main.getRepository();

                String username = credentials.getString("username");
                String password = credentials.getString("password");

                if (username != null && password != null) {
                    result = repository.login(username, password.toCharArray());
                }
            } catch (RepositoryException e) {
                System.err.println("Failed to connect to repository.");
            }
            return result;
        }

    }
}
