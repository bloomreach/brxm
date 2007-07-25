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

import javax.jcr.SimpleCredentials;

import org.apache.wicket.Request;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebSession;
import org.hippocms.repository.frontend.update.UpdateManager;
import org.hippocms.repository.jr.embedded.HippoRepository;
import org.hippocms.repository.jr.embedded.HippoRepositoryFactory;

public class BrowserSession extends WebSession {
    private static final long serialVersionUID = 1L;
        
    private javax.jcr.Session jcrSession;
    private UpdateManager updateManager;
    private WebApplication application;

    public BrowserSession(WebApplication application, Request request) {
        super(application, request);
        this.application = application;
        this.updateManager = new UpdateManager();
    }

    public javax.jcr.Session getJcrSession()  {
        if (jcrSession == null || !jcrSession.isLive()) {
            try {
                String address = getRepositoryAddress();
                HippoRepository repository = HippoRepositoryFactory.getHippoRepository(address);
                jcrSession = repository.login(new SimpleCredentials("username", "password".toCharArray()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return jcrSession;
    }
    
    public UpdateManager getUpdateManager() {
        return updateManager;
    }
    
    private String getRepositoryAddress() {
        String address = application.getInitParameter("repository-address");
        if (address == null || address.equals("")) {
            address = application.getServletContext().getInitParameter("repository-address");
        }
        if (address == null || address.equals("")) {
            address = "rmi://localhost:1099/jackrabbit.repository";
        }
        return address;
    }
    
}