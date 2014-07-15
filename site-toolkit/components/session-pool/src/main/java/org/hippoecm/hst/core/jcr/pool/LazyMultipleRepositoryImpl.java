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
package org.hippoecm.hst.core.jcr.pool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.hippoecm.hst.core.ResourceVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LazyMultipleRepositoryImpl extends MultipleRepositoryImpl {
    
    private static final Logger log = LoggerFactory.getLogger(LazyMultipleRepositoryImpl.class);
    
    private Map<String, Map<String, PoolingRepository>> repositoriesMapByCredsDomain = Collections.synchronizedMap(new HashMap<String, Map<String, PoolingRepository>>());
    
    private BasicPoolingRepositoryFactory poolingRepositoryFactory;
    private Map<String, String> defaultConfigMap;
    private boolean pooledSessionLifecycleManagementActive = true;
    private String credentialsDomainSeparator = String.valueOf('\uFFFF');
    
    private long timeBetweenEvictionRunsMillis;
    private Pattern disposableUserIDPatternObject;
    private InactiveRepositoryDisposer inactiveRepositoryDisposer;
    
    private ResourceLifecycleManagement [] lazyResourceLifecycleManagements;
    
    private ThreadLocal<Set<String>> tlCurrentCredsDomains = new ThreadLocal<Set<String>>();
    
    public LazyMultipleRepositoryImpl(Credentials defaultCredentials, Map<String, String> defaultConfigMap) {
        super(defaultCredentials);
        /*
         *  because properties that have an accidental space behind become very confusing, for example Boolean.toBoolean("true ") is not parsed as <code>true</code> because of the space, 
         *  we first trim leading and trailing whitespaces from the String values of the map
         */
        this.defaultConfigMap = trimWhiteSpaceValues(defaultConfigMap);
    }
    
    public LazyMultipleRepositoryImpl(Map<Credentials, Repository> repoMap, Credentials defaultCredentials, Map<String, String> defaultConfigMap) {
        super(repoMap, defaultCredentials);
        /*
         *  because properties that have an accidental space behind become very confusing, for example Boolean.toBoolean("true ") is not parsed as <code>true</code> because of the space, 
         *  we first trim leading and trailing whitespaces from the String values of the map
         */
        this.defaultConfigMap = trimWhiteSpaceValues(defaultConfigMap);
    }
    
    public synchronized void setPoolingRepositoryFactory(BasicPoolingRepositoryFactory poolingRepositoryFactory) {
        this.poolingRepositoryFactory = poolingRepositoryFactory;
    }
    
    public synchronized void setDefaultConfigMap(Map<String, String> defaultConfigMap) {
        this.defaultConfigMap = trimWhiteSpaceValues(defaultConfigMap);
    }
    
    public void setPooledSessionLifecycleManagementActive(boolean pooledSessionLifecycleManagementActive) {
        this.pooledSessionLifecycleManagementActive = pooledSessionLifecycleManagementActive;
    }
    
    public void setCredentialsDomainSeparator(String credentialsDomainSeparator) {
        this.credentialsDomainSeparator = credentialsDomainSeparator;
    }
    
    public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        if (timeBetweenEvictionRunsMillis < 0) {
            throw new IllegalArgumentException("timeBetweenEvictionRunsMillis cannot be a negative value.");
        }
        
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
    }
    
    public void setDisposableUserIDPattern(String disposableUserIDPattern) {
        this.disposableUserIDPatternObject = Pattern.compile(disposableUserIDPattern);
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
        
        ResourceLifecycleManagement [] cloned = new ResourceLifecycleManagement[lazyResourceLifecycleManagements.length];
        System.arraycopy(lazyResourceLifecycleManagements, 0, cloned, 0, lazyResourceLifecycleManagements.length);
        return cloned;
    }
    
    public Map<String, Map<String, PoolingRepository>> cloneRepositoriesMapByCredsDomain() {
        Map<String, Map<String, PoolingRepository>> clonedRepositoriesMapByCredsDomain = new HashMap<String, Map<String, PoolingRepository>>();
        
        List<String> credsDomains = new LinkedList<String>();
        
        if (repositoriesMapByCredsDomain == null || repositoriesMapByCredsDomain.isEmpty()) {
            return Collections.emptyMap();
        }
        
        synchronized (repositoriesMapByCredsDomain) {
            Iterator<String> it = repositoriesMapByCredsDomain.keySet().iterator();
            while (it.hasNext()) {
                credsDomains.add(it.next());
            }
        }
        
        for (String credsDomain : credsDomains) {
            Map<String, PoolingRepository> repoMap = repositoriesMapByCredsDomain.get(credsDomain);
            
            if (repoMap != null && !repoMap.isEmpty()) {
                List<String> userIDs = new LinkedList<String>();
                
                synchronized (repoMap) {
                    Iterator<String> it = repoMap.keySet().iterator();
                    while (it.hasNext()) {
                        userIDs.add(it.next());
                    }
                }
                
                Map<String, PoolingRepository> clonedRepoMap = new HashMap<String, PoolingRepository>();
                
                for (String userID : userIDs) {
                    PoolingRepository repo = repoMap.get(userID);
                    if (repo != null) {
                        clonedRepoMap.put(userID, repo);
                    }
                }
                
                clonedRepositoriesMapByCredsDomain.put(credsDomain, clonedRepoMap);
            }
        }
        
        return clonedRepositoriesMapByCredsDomain;
    }
    
    @Override
    protected Session login(CredentialsWrapper credentialsWrapper) throws LoginException, RepositoryException {
        Session session = null;
        
        try {
            PoolingRepository repository = (PoolingRepository) repositoryMap.get(credentialsWrapper);
            
            if (repository != null) {
                if (!repository.isDisposableWhenNotInUse()) {
                    // non closable pool, so just go ahead without locking.
                    session = repository.login(credentialsWrapper.getCredentials());
                } else {
                    // closable pool. get session only if the pool is not marked to be closing.
                    if (!repository.isMarkedForDisposal()) {
                        synchronized (this) {
                            if (!repository.isMarkedForDisposal()) {
                                session = repository.login(credentialsWrapper.getCredentials());
                                setCurrentThreadRepository(repository);
                            }
                        }
                    }
                }
            }
            
            if (session == null) {
                session = getSessionFromRepositoryCreatedOnDemand(credentialsWrapper);
            }
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
        
        String credentialsDomain = StringUtils.substringAfter(credentialsWrapper.getUserID(), credentialsDomainSeparator);
        Set<String> credsDomains = tlCurrentCredsDomains.get();
        if (credsDomains == null) {
            credsDomains = new HashSet<String>();
            credsDomains.add(credentialsDomain);
            tlCurrentCredsDomains.set(credsDomains);
        } else {
            credsDomains.add(credentialsDomain);
        }
        return session;
    }

    private synchronized Session getSessionFromRepositoryCreatedOnDemand(CredentialsWrapper credentialsWrapper) throws Exception {
        Session session;
        PoolingRepository repository = (PoolingRepository) repositoryMap.get(credentialsWrapper);
        
        if (repository != null && !repository.isMarkedForDisposal()) {
            session = repository.login(credentialsWrapper.getCredentials());
            setCurrentThreadRepository(repository);
            return session;
        }
        
        Map<String, String> configMap = new HashMap<String, String>(defaultConfigMap);
        String userID = credentialsWrapper.getUserID();
        configMap.put("defaultCredentialsUserID", userID);
        configMap.put("defaultCredentialsPassword", credentialsWrapper.getPassword());
        
        if (poolingRepositoryFactory == null) {
            poolingRepositoryFactory = new BasicPoolingRepositoryFactory();
        }
        
        repository = poolingRepositoryFactory.getObjectInstanceByConfigMap(configMap);
        
        if (disposableUserIDPatternObject != null && disposableUserIDPatternObject.matcher(userID).matches()) {
            ((BasicPoolingRepository) repository).setDisposableWhenNotInUse(true);
        }
        
        if (repository instanceof MultipleRepositoryAware) {
            ((MultipleRepositoryAware) repository).setMultipleRepository(this);
        }
        
        ResourceLifecycleManagement resourceLifecycleManagement = repository.getResourceLifecycleManagement();
        
        if (resourceLifecycleManagement != null) {
            resourceLifecycleManagement.setAlwaysActive(pooledSessionLifecycleManagementActive);
        }

        // create session first in order to increase active count and to not be closeable.
        // before registering this new created repository into the maps.
        session = repository.login(credentialsWrapper.getCredentials());
        setCurrentThreadRepository(repository);

        String credentialsDomain = StringUtils.substringAfter(userID, credentialsDomainSeparator);
        Map<String, PoolingRepository> credsDomainRepos = repositoriesMapByCredsDomain.get(credentialsDomain);
        
        if (credsDomainRepos == null) {
            credsDomainRepos = Collections.synchronizedMap(new HashMap<String, PoolingRepository>());
        }
        
        credsDomainRepos.put(userID, repository);
        
        repositoriesMapByCredsDomain.put(credentialsDomain, credsDomainRepos);
        
        lazyResourceLifecycleManagements = null;
        repositoryMap.put(credentialsWrapper, repository);
        
        if (timeBetweenEvictionRunsMillis > 0L && inactiveRepositoryDisposer == null) {
            inactiveRepositoryDisposer = new InactiveRepositoryDisposer();
            inactiveRepositoryDisposer.start();
        }
        
        return session;
    }

    public synchronized void close() {
        super.close();
        if (inactiveRepositoryDisposer != null) {
            inactiveRepositoryDisposer.interrupt();
        }

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
            String credsDomain = StringUtils.substringAfter(userID, credentialsDomainSeparator);
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
    
    private class InactiveRepositoryDisposer extends Thread {
        
        private boolean stopped;
        
        private InactiveRepositoryDisposer() {
            super("InactiveRepositoryDisposer");
            setDaemon(true);
        }
        
        public void run() {
            
            boolean interrupted = stopped;
            
            synchronized (this) {
                try {
                    wait(timeBetweenEvictionRunsMillis);
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            
            stopped = interrupted;
            
            while (!stopped) {
                Map<String, Map<String, PoolingRepository>> clonedRepositoriesMapByCredsDomain = cloneRepositoriesMapByCredsDomain();
                
                List<PoolingRepository> reposToClose = new ArrayList<PoolingRepository>();
                
                for (Map.Entry<String, Map<String, PoolingRepository>> entry1 : clonedRepositoriesMapByCredsDomain.entrySet()) {
                    String credsDomain = entry1.getKey();
                    Map<String, PoolingRepository> clonedRepoMap = entry1.getValue();
                    
                    for (Map.Entry<String, PoolingRepository> entry2: clonedRepoMap.entrySet()) {
                        String userID = entry2.getKey();
                        BasicPoolingRepository poolingRepo = (BasicPoolingRepository) entry2.getValue();
                        
                        if (!poolingRepo.isDisposableWhenNotInUse()) {
                            continue;
                        }
                        
                        if (poolingRepo.getNumIdle() <= 0 && poolingRepo.getNumActive() <= 0) {
                            synchronized (LazyMultipleRepositoryImpl.this) {
                                if (poolingRepo.getNumIdle() <= 0 && poolingRepo.getNumActive() <= 0) {
                                    poolingRepo.setMarkedForDisposal(true);
                                    removeRepository(poolingRepo.getDefaultCredentials());
                                    reposToClose.add(poolingRepo);
                                    
                                    Map<String, PoolingRepository> repoMap = repositoriesMapByCredsDomain.get(credsDomain);
                                    
                                    if (repoMap != null) {
                                        repoMap.remove(userID);
                                        
                                        if (repoMap.isEmpty()) {
                                            repositoriesMapByCredsDomain.remove(credsDomain);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                for (PoolingRepository repo : reposToClose) {
                    try {
                        repo.close();
                    } catch (Exception e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Failed to close an inactive pooling repository.", e);
                        } else {
                            log.warn("Failed to close an inactive pooling repository. {}", e.toString());
                        }
                    }
                }
                
                synchronized (this) {
                    try {
                        wait(timeBetweenEvictionRunsMillis);
                    } catch (InterruptedException e) {
                        interrupted = true;
                        break;
                    }
                }
                
                stopped = interrupted;
            }
        }
    }
    
    private Map<String, String> trimWhiteSpaceValues(Map<String, String> configMap) {
        if (configMap == null) {
            return Collections.emptyMap();
        }
        Map<String, String> trimmedConfigMap = new HashMap<String, String>(configMap.size());
        for(Entry<String, String> entry : configMap.entrySet()) {
            trimmedConfigMap.put(entry.getKey(), entry.getValue().trim());
        }
        return trimmedConfigMap;
    }
    
}
