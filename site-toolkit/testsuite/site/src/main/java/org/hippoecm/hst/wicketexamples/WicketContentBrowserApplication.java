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
package org.hippoecm.hst.wicketexamples;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.wicket.protocol.http.WebApplication;
import org.hippoecm.hst.site.HstServices;

public class WicketContentBrowserApplication extends WebApplication {
    
    private Repository repository;
    private Credentials credentials;
    private boolean credentialsConfigured = true;
    
    @Override
    public Class getHomePage() {
        return WicketContentBrowserPage.class;
    }
    
    public Repository getDefaultRepository() throws Exception {
        if (repository == null) {
            String repositoryResourceReferenceName = getInitParameter("repository-res-ref-name");
            
            if (repositoryResourceReferenceName != null) {
                Context initCtx = new InitialContext();
                Context envCtx = (Context) initCtx.lookup("java:comp/env");
                repository = (Repository) envCtx.lookup(repositoryResourceReferenceName);
            } else {
                repository = HstServices.getComponentManager().getComponent("javax.jcr.Repository");
            }
        }
        
        return repository;
    }
    
    public Credentials getDefaultCredentials() {
        if (credentials == null && credentialsConfigured) {
            String user = getInitParameter("repository-user");
            String password = getInitParameter("repository-password");
            
            if (user != null && password != null) {
                credentials = new SimpleCredentials(user, password.toCharArray());
            } else {
                credentialsConfigured = false;
            }
        }
        
        return credentials;
    }
    
}
