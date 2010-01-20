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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.hippoecm.hst.core.ResourceVisitor;

public class LazyMultipleRepositoryImpl extends MultipleRepositoryImpl {
    
    private Map<String, Map<String, PoolingRepository>> repositoriesMapByCredsDomain = Collections.synchronizedMap(new HashMap<String, Map<String, PoolingRepository>>());
    
    private BasicPoolingRepositoryFactory poolingRepositoryFactory;
    private Map<String, String> defaultConfigMap;
    private boolean pooledSessionLifecycleManagementActive = true;
    
    private ResourceLifecycleManagement [] lazyResourceLifecycleManagements;
    
    private ThreadLocal<Set<String>> tlCurrentCredsDomains = new ThreadLocal<Set<String>>();
    
    public LazyMultipleRepositoryImpl(Credentials defaultCredentials, Map<String, String> defaultConfigMap) {
        super(defaultCredentials);
        this.defaultConfigMap = defaultConfigMap;
    }
    
    public LazyMultipleRepositoryImpl(Map<Credentials, Repository> repoMap, Credentials defaultCredentials, Map<String, String> defaultConfigMap) {
        super(repoMap, defaultCredentials);
        this.defaultConfigMap = defaultConfigMap;
    }
    
    public void setPoolingRepositoryFactory(BasicPoolingRepositoryFactory poolingRepositoryFactory) {
        this.poolingRepositoryFactory = poolingRepositoryFactory;
    }
    
    public void setDefaultConfigMap(Map<String, String> defaultConfigMap) {
        this.defaultConfigMap = defaultConfigMap;
    }
    
    public void setPooledSessionLifecycleManagementActive(boolean pooledSessionLifecycleManagementActive) {
        this.pooledSessionLifecycleManagementActive = pooledSessionLifecycleManagementActive;
    }
    
    @Override
    public ResourceLifecycleManagement [] getResourceLifecycleManagements() {
        int size = (resourceLifecycleManagements != null ? resourceLifecycleManagements.length : 0);
        
        if (lazyResourceLifecycleManagements == null || size != lazyResourceLifecycleManagements.length - 1) {
            ResourceLifecycleManagement [] tempResourceLifecycleManagements = new ResourceLifecycleManagement[size  + 1];
            
            for (int i = 0; i < size; i++) {
                tempResourceLifecycleManagements[i] = resourceLifecycleManagements[i];
            }
            
            tempResourceLifecycleManagements[size] = new DelegatingResourceLifecycleManagements();
            lazyResourceLifecycleManagements = tempResourceLifecycleManagements;
        }
        
        return lazyResourceLifecycleManagements;
    }
    
    @Override
    protected Session login(CredentialsWrapper credentialsWrapper) throws LoginException, RepositoryException {
        if (!repositoryMap.containsKey(credentialsWrapper)) {
            try {
                createRepositoryOnDemand(credentialsWrapper);
            } catch (Exception e) {
                throw new RepositoryException(e);
            }
        }
        
        String credentialsDomain = StringUtils.substringAfter(credentialsWrapper.getUserID(), "@");
        Set<String> credsDomains = tlCurrentCredsDomains.get();
        if (credsDomains == null) {
            credsDomains = new HashSet<String>();
            credsDomains.add(credentialsDomain);
            tlCurrentCredsDomains.set(credsDomains);
        } else {
            credsDomains.add(credentialsDomain);
        }
        
        return super.login(credentialsWrapper);
    }
    
    protected synchronized void createRepositoryOnDemand(CredentialsWrapper credentialsWrapper) throws Exception {
        if (repositoryMap.containsKey(credentialsWrapper)) {
            return;
        }
        
        Map<String, String> configMap = new HashMap<String, String>(defaultConfigMap);
        String userID = credentialsWrapper.getUserID();
        configMap.put("defaultCredentialsUserID", userID);
        configMap.put("defaultCredentialsPassword", credentialsWrapper.getPassword());
        
        if (poolingRepositoryFactory == null) {
            poolingRepositoryFactory = new BasicPoolingRepositoryFactory();
        }
        
        PoolingRepository repository = poolingRepositoryFactory.getObjectInstanceByConfigMap(configMap);
        
        if (repository instanceof MultipleRepositoryAware) {
            ((MultipleRepositoryAware) repository).setMultipleRepository(this);
        }
        
        ResourceLifecycleManagement resourceLifecycleManagement = repository.getResourceLifecycleManagement();
        
        if (resourceLifecycleManagement != null) {
            resourceLifecycleManagement.setAlwaysActive(pooledSessionLifecycleManagementActive);
        }

        String credentialsDomain = StringUtils.substringAfter(userID, "@");
        Map<String, PoolingRepository> credsDomainRepos = repositoriesMapByCredsDomain.get(credentialsDomain);
        
        if (credsDomainRepos == null) {
            credsDomainRepos = Collections.synchronizedMap(new HashMap<String, PoolingRepository>());
        }
        
        credsDomainRepos.put(userID, repository);
        
        repositoriesMapByCredsDomain.put(credentialsDomain, credsDomainRepos);
        
        lazyResourceLifecycleManagements = null;
        repositoryMap.put(credentialsWrapper, repository);
    }
    
    private class DelegatingResourceLifecycleManagements implements ResourceLifecycleManagement {

        public boolean isActive() {
            return pooledSessionLifecycleManagementActive;
        }
        
        public void setActive(boolean active) {
        }
        
        public boolean isAlwaysActive() {
            return pooledSessionLifecycleManagementActive;
        }
        
        public void setAlwaysActive(boolean alwaysActive) {
        }
        
        public void registerResource(Object resource) {
            PooledSession session = (PooledSession) resource;
            ResourceLifecycleManagement resourceLifecycleManagement = getResourceLifecycleManagementBySession(session);
            
            if (resourceLifecycleManagement != null) {
                resourceLifecycleManagement.registerResource(resource);
            }
        }
        
        public void unregisterResource(Object resource) {
            PooledSession session = (PooledSession) resource;
            ResourceLifecycleManagement resourceLifecycleManagement = getResourceLifecycleManagementBySession(session);
            
            if (resourceLifecycleManagement != null) {
                resourceLifecycleManagement.unregisterResource(resource);
            }
        }
        
        public void disposeResource(Object resource) {
            PooledSession session = (PooledSession) resource;
            ResourceLifecycleManagement resourceLifecycleManagement = getResourceLifecycleManagementBySession(session);
            
            if (resourceLifecycleManagement != null) {
                resourceLifecycleManagement.disposeResource(resource);
            }
        }
        
        public void disposeAllResources() {
            for (ResourceLifecycleManagement resourceLifecycleManagement : getCurrentResourceLifecycleManagements()) {
                resourceLifecycleManagement.disposeAllResources();
            }
            
            Set<String> credsDomains = tlCurrentCredsDomains.get();
            if (credsDomains != null) {
                credsDomains.clear();
            }
        }
        
        public Object visitResources(ResourceVisitor visitor) {
            for (ResourceLifecycleManagement resourceLifecycleManagement : getCurrentResourceLifecycleManagements()) {
                Object ret = resourceLifecycleManagement.visitResources(visitor);
                
                if (ret != ResourceVisitor.CONTINUE_TRAVERSAL) {
                    return ret;
                }
            }
            
            return null;
        }
        
        private ResourceLifecycleManagement getResourceLifecycleManagementBySession(PooledSession session) {
            String userID = session.getUserID();
            String credsDomain = StringUtils.substringAfter(userID, "@");
            Map<String, PoolingRepository> repoMap = repositoriesMapByCredsDomain.get(credsDomain);
            
            if (repoMap != null) {
                PoolingRepository repository = repoMap.get(userID);
                
                if (repository != null) {
                    return repository.getResourceLifecycleManagement();
                }
            }
            
            return null;
        }
        
        private List<ResourceLifecycleManagement> getCurrentResourceLifecycleManagements() {
            List<ResourceLifecycleManagement> resourceLifecycleManagements = new ArrayList<ResourceLifecycleManagement>();
            
            Set<String> credsDomains = tlCurrentCredsDomains.get();
            
            if (credsDomains != null) {
                for (String credsDomain : credsDomains) {
                    Map<String, PoolingRepository> repoMap = repositoriesMapByCredsDomain.get(credsDomain);
                    
                    if (repoMap == null) {
                        continue;
                    }
                    
                    synchronized (repoMap) {
                        for (PoolingRepository repository : repoMap.values()) {
                            ResourceLifecycleManagement resourceLifecycleManagement = repository.getResourceLifecycleManagement();
                            
                            if (resourceLifecycleManagement != null) {
                                resourceLifecycleManagements.add(resourceLifecycleManagement);
                            }
                        }
                    }
                }
            }
            
            return resourceLifecycleManagements;
        }
    }
    
}
