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
import org.hippocms.repository.frontend.plugin.Plugin;
import org.hippocms.repository.jr.embedded.HippoRepository;
import org.hippocms.repository.jr.embedded.HippoRepositoryFactory;

public class UserSession extends WebSession {
    private static final long serialVersionUID = 1L;

    private List plugins;
    private JcrSessionModel jcrSessionModel;

    public UserSession(WebApplication application, Request request) {
        super(application, request);
        plugins = new ArrayList();
        jcrSessionModel = new JcrSessionModel();
        //Calling the dirty() method causes this wicket session to be reset in the http session
        //so that it knows that the wicket session has changed (we've just added the jcr session model etc.)
        dirty();
    }

    public Session getJcrSession() {
        return jcrSessionModel.getSession();
    }

    private class JcrSessionModel extends LoadableDetachableModel {
        // The jcr session is wrapped in a LoadableDetachableModel because it can't be serialized
        // and therefore cannot be a direct field of the wicket session. Wrapping the jcr session
        // like this has the added bonus of being a very simple reconnect mechanism.  
        private static final long serialVersionUID = 1L;

        private final static String repositoryAddressConfigParam = "repository-address";
        private final static String defaultRepositoryAddress = "rmi://localhost:1099/jackrabbit.repository";
        
        private String repositoryAdress;

        JcrSessionModel() {
            Main main = (Main) Application.get();
            repositoryAdress = main.getConfigurationParameter(repositoryAddressConfigParam, defaultRepositoryAddress);
        }

        Session getSession() {
            // detach if anything is wrong with the jcr session
            try {
                if (!((Session) getObject()).isLive()) {
                    detach();
                }
            } catch (Exception e) {
                detach();
            }
            // this will call load() only if detached 
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

    //TODO: Look into IInitializer and wicket.properties for this 

    public void registerPlugin(Plugin plugin) {
        //should we call dirty() here? (see comment in constructor)
        plugins.add(plugin);
    }

    public void notifyPlugins(AjaxRequestTarget target, JcrNodeModel model) {
        Iterator it = plugins.iterator();
        while (it.hasNext()) {
            ((Plugin) it.next()).update(target, model);
        }
    }

}