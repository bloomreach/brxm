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
package org.hippoecm.hst.core.jcr;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 * DelegatingRepository
 * @version $Id$
 */
public class DelegatingRepository implements Repository {
    
    private Repository delegatee;
    
    public DelegatingRepository(Repository delegatee) {
        this.delegatee = delegatee;
    }

    public String getDescriptor(String key) {
        return delegatee.getDescriptor(key);
    }

    public String[] getDescriptorKeys() {
        return delegatee.getDescriptorKeys();
    }


    public Value getDescriptorValue(String key) {
        return delegatee.getDescriptorValue(key);
    }

    public Value[] getDescriptorValues(String key) {
        return delegatee.getDescriptorValues(key);
    }

    public boolean isSingleValueDescriptor(String key) {
        return delegatee.isSingleValueDescriptor(key);
    }

    public boolean isStandardDescriptor(String key) {
        return delegatee.isStandardDescriptor(key);
    }
    
    public Session login() throws LoginException, RepositoryException {
        return delegatee.login();
    }

    public Session login(Credentials credentials) throws LoginException, RepositoryException {
        return delegatee.login(credentials);
    }

    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return delegatee.login(workspaceName);
    }

    public Session login(Credentials credentials, String workspaceName) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        return delegatee.login(credentials, workspaceName);
    }
    
    protected Repository getDelegatee() {
        return delegatee;
    }

}
