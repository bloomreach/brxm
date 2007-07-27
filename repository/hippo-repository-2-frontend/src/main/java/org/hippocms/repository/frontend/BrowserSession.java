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
package org.hippocms.repository.frontend;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.wicket.Application;
import org.apache.wicket.Request;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebSession;
import org.hippocms.repository.frontend.model.JcrNodeModel;
import org.hippocms.repository.jr.embedded.HippoRepository;
import org.hippocms.repository.jr.embedded.HippoRepositoryFactory;

public class BrowserSession extends WebSession {
    private static final long serialVersionUID = 1L;

    private List updatables;
    private JcrSessionModel jcrSessionModel;

    public BrowserSession(WebApplication application, Request request) {
        super(application, request);
        updatables = new ArrayList();
        jcrSessionModel = new JcrSessionModel();
        dirty();
    }
    
    public Session getJcrSession() {
        return jcrSessionModel.getSession();
    }
    
    
    private class JcrSessionModel extends LoadableDetachableModel {
        private static final long serialVersionUID = 1L;

        String repositoryAdress;
        
        JcrSessionModel() {
            Main main = (Main)Application.get();
            this.repositoryAdress = main.getRepositoryAdress();
        }

        Session getSession() {
            return (Session) getObject();
        }

        protected Object load() {
            javax.jcr.Session result = null;
            try {
                HippoRepository repository = HippoRepositoryFactory.getHippoRepository(repositoryAdress);
                result = repository.login(new SimpleCredentials("username", "password".toCharArray()));
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return result;
        }
    }
    

    public void addUpdatable(IUpdatable updatable) {
        updatables.add(updatable);
    }

    public void updateAll(AjaxRequestTarget target, JcrNodeModel model) {
        Iterator it = updatables.iterator();
        while (it.hasNext()) {
            ((IUpdatable) it.next()).update(target, model);
        }
    }

}