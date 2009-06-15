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
package org.hippoecm.hst.core.jcr.pool;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.core.ResourceLifecycleManagement;

public class MultipleRepositoryImpl implements MultipleRepository {

    private static ThreadLocal<Repository> tlCurrentRepository = new ThreadLocal<Repository>();
    
    protected Map<CredentialsWrapper, Repository> repositoryMap = new HashMap<CredentialsWrapper, Repository>();
    protected ResourceLifecycleManagement [] resourceLifecycleManagements;
    protected CredentialsWrapper defaultCredentialsWrapper;
    
    public MultipleRepositoryImpl(Credentials defaultCredentials) {
        this(null, defaultCredentials);
    }
    
    public MultipleRepositoryImpl(Map<Credentials, Repository> repoMap, Credentials defaultCredentials) {
        this.defaultCredentialsWrapper = new CredentialsWrapper(defaultCredentials);

        for (Map.Entry<Credentials, Repository> entry : repoMap.entrySet()) {
            Credentials cred = entry.getKey();
            Repository repo = entry.getValue();
            addRepository(cred, repo);
        }

        refreshResourceLifecycleManagements();
    }
    
    public void addRepository(Credentials credentials, Repository repository) {
        if (repository instanceof MultipleRepositoryAware) {
            ((MultipleRepositoryAware) repository).setMultipleRepository(this);
        }
        
        this.repositoryMap.put(new CredentialsWrapper(credentials), repository);

        refreshResourceLifecycleManagements();
    }
    
    public Repository getRepositoryByCredentials(Credentials credentials) {
        return this.repositoryMap.get(new CredentialsWrapper(credentials));
    }
    
    public Map<Credentials, Repository> getRepositoryMap() {
        Map<Credentials, Repository> repoMap = new HashMap<Credentials, Repository>();
        
        for (Map.Entry<CredentialsWrapper, Repository> entry : this.repositoryMap.entrySet()) {
            repoMap.put(entry.getKey().getCredentials(), entry.getValue());
        }
        
        return repoMap;
    }

    public String getDescriptor(String arg0) {
        String descriptor = null;
        Repository curRepository = getCurrentThreadRepository();
        
        if (curRepository != null) {
            descriptor = curRepository.getDescriptor(arg0);
        }
        
        return descriptor;
    }

    public String[] getDescriptorKeys() {
        String [] descriptorKeys = null;
        Repository curRepository = getCurrentThreadRepository();
        
        if (curRepository != null) {
            descriptorKeys = curRepository.getDescriptorKeys();
        }
        
        return descriptorKeys;
    }

    public Session login() throws LoginException, RepositoryException {
        return login(this.defaultCredentialsWrapper);
    }

    public Session login(Credentials credentials) throws LoginException, RepositoryException {
        return login(new CredentialsWrapper(credentials));
    }
    
    protected Session login(CredentialsWrapper credentialsWrapper) throws LoginException, RepositoryException {
        Repository repository = this.repositoryMap.get(credentialsWrapper);
        
        if (repository == null) {
            throw new RepositoryException("The repository is not available."); 
        }

        setCurrentThreadRepository(repository);
        
        return repository.login(credentialsWrapper.getCredentials());
    }

    public Session login(String workspace) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login();
    }

    public Session login(Credentials credentials, String workspace) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(credentials);
    }
    
    public ResourceLifecycleManagement [] getResourceLifecycleManagements() {
        return this.resourceLifecycleManagements;
    }
    
    protected Repository getCurrentThreadRepository() {
        return tlCurrentRepository.get();
    }
    
    protected void setCurrentThreadRepository(Repository repository) {
        tlCurrentRepository.set(repository);
    }

    private void refreshResourceLifecycleManagements() {
        Set<ResourceLifecycleManagement> resourceLifecycleManagementSet = new HashSet<ResourceLifecycleManagement>();
        
        for (Repository repo : this.repositoryMap.values()) {
            if (repo instanceof PoolingRepository) {
                ResourceLifecycleManagement rlm = ((PoolingRepository) repo).getResourceLifecycleManagement();
                resourceLifecycleManagementSet.add(rlm);
            }
        }
        
        this.resourceLifecycleManagements = new ResourceLifecycleManagement[resourceLifecycleManagementSet.size()];
        int index = 0;
        
        for (ResourceLifecycleManagement rlm : resourceLifecycleManagementSet) {
            this.resourceLifecycleManagements[index++] = rlm;
        }
    }
    
    private boolean equalsCredentials(Credentials credentials1, Credentials credentials2) {
        if (credentials1 instanceof SimpleCredentials && credentials2 instanceof SimpleCredentials) {
            return (((SimpleCredentials) credentials1).getUserID().equals(((SimpleCredentials) credentials2).getUserID()));
        } else if (credentials1 != null) {
            return credentials1.equals(credentials2);
        }
        
        return false;
    }

    private class CredentialsWrapper implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        private final Credentials credentials;
        private String userID;
        private String password;
        private int hash;

        private CredentialsWrapper(final Credentials credentials) {
            super();
            this.credentials = credentials;
            
            if (this.credentials instanceof SimpleCredentials) {
                SimpleCredentials sc = (SimpleCredentials) this.credentials;
                this.userID = sc.getUserID();
                this.password = new String(sc.getPassword());
                this.hash = new StringBuilder(this.userID).append(':').append(this.password).toString().hashCode();
            } else {
                this.hash = this.credentials.hashCode();
            }
        }
        
        public Credentials getCredentials() {
            return this.credentials;
        }
        
        @Override
        public boolean equals(final Object other) {
            CredentialsWrapper cwOther = (CredentialsWrapper) other;
            return (this.userID.equals(cwOther.userID) && this.password.equals(cwOther.password));
        }
        
        @Override
        public int hashCode() {
            return this.hash;
        }
    }
}
