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
package org.hippoecm.hst.core.jcr;

import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;

import org.hippoecm.hst.security.HstSubject;
import org.hippoecm.hst.security.User;

/**
 * SubjectBasedStatefulRepository
 * @version $Id$
 */
public class SubjectBasedStatefulRepository extends DelegatingRepository {
    
    public SubjectBasedStatefulRepository(Repository delegatee) {
        super(delegatee);
    }
    
    public Session login() throws LoginException, RepositoryException {
        Session session = loginBySubject();
        
        if (session != null) {
            return session;
        }
        
        return super.login();
    }

    public Session login(Credentials credentials) throws LoginException, RepositoryException {
        Session session = loginBySubject();
        
        if (session != null) {
            return session;
        }
        
        return super.login(credentials);
    }

    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        Session session = loginBySubject();
        
        if (session != null) {
            return session;
        }
        
        return super.login(workspaceName);
    }

    public Session login(Credentials credentials, String workspaceName) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        Session session = loginBySubject();
        
        if (session != null) {
            return session;
        }
        
        return super.login(credentials, workspaceName);
    }
    
    protected Session loginBySubject() throws LoginException, RepositoryException {
        Subject subject = HstSubject.getSubject(null);
        
        if (subject != null) {
            Set<User> users = subject.getPrincipals(User.class);
            
            if (!users.isEmpty()) {
                String username = users.iterator().next().getName();
                // FIXME: retrieve password credentials more properly and securely.
                //        possibly store private credentials into subject with proper access controls.
                String password = username;
                Credentials creds = new SimpleCredentials(username, password.toCharArray());
                return super.login(creds);
            }
        }
        
        return null;
    }

}
