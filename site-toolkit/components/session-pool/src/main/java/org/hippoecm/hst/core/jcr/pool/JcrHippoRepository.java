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

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.naming.InitialContext;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JCR Repository implementation wrapping HippoRepository.
 * 
 * @version $Id$
 */
public class JcrHippoRepository implements Repository {
    
    private static final Logger log = LoggerFactory.getLogger(JcrHippoRepository.class);

    protected String repositoryURI;
    
    protected HippoRepository hippoRepository;       // repository created via HippoRepositoryFactory
    protected Repository jcrDelegateeRepository;     // repository created from hippo ecm jca support
    
    private boolean vmRepositoryUsed;
    private boolean localRepositoryUsed;
    
    private boolean repositoryInitialized;
    
    public JcrHippoRepository() {
        this((String) null);
    }
    
    public JcrHippoRepository(String repositoryURI) {
        this.repositoryURI = repositoryURI;
        vmRepositoryUsed = (repositoryURI != null && repositoryURI.startsWith("vm:"));
        
        if (StringUtils.isBlank(repositoryURI)) {
            localRepositoryUsed = true;
        } else if (StringUtils.startsWith(repositoryURI, "file:")) {
            localRepositoryUsed = true;
        } else if (StringUtils.startsWith(repositoryURI, "/")) {
            localRepositoryUsed = true;
        }
    }
    
    public JcrHippoRepository(HippoRepository hippoRepository) {
        this.hippoRepository = hippoRepository;
        
        if (hippoRepository != null) {
            repositoryInitialized = true;
            repositoryURI = hippoRepository.getLocation();
            
            vmRepositoryUsed = (repositoryURI != null && repositoryURI.startsWith("vm:"));
            
            if (StringUtils.isBlank(repositoryURI)) {
                localRepositoryUsed = true;
            } else if (StringUtils.startsWith(repositoryURI, "file:")) {
                localRepositoryUsed = true;
            } else if (StringUtils.startsWith(repositoryURI, "/")) {
                localRepositoryUsed = true;
            }
        }
    }
    
    private synchronized void initHippoRepository() throws RepositoryException {
        if (repositoryInitialized) {
            return;
        }
        
        try {
            if (log.isInfoEnabled()) {
                log.info("Trying to get hippo repository from {}.", repositoryURI);
            }

            if (StringUtils.isEmpty(repositoryURI)) {
                hippoRepository = HippoRepositoryFactory.getHippoRepository();
            } else if (repositoryURI.startsWith("java:")) {
                InitialContext ctx = new InitialContext();
                Object repositoryObject = ctx.lookup(repositoryURI);
                
                if (repositoryObject instanceof Repository) {
                    jcrDelegateeRepository = (Repository) repositoryObject;
                } else if (repositoryObject instanceof HippoRepository) {
                    hippoRepository = (HippoRepository) repositoryObject;
                } else {
                    throw new RepositoryException("Unknown repository object from " + repositoryURI + ": " + repositoryObject);
                }
            } else {
                hippoRepository = HippoRepositoryFactory.getHippoRepository(repositoryURI);
            }
            
            if (log.isInfoEnabled()) {
                log.info("Has retrieved hippo repository from {}.", repositoryURI);
            }
        } catch (Exception e) {
            throw new RepositoryException(e);
        } finally {
            repositoryInitialized = (jcrDelegateeRepository != null || hippoRepository != null);
        }
    }
    
    public String getDescriptor(String key) {
        if (jcrDelegateeRepository != null) {
            return jcrDelegateeRepository.getDescriptor(key);
        }
        
        if (hippoRepository != null) {
            ClassLoader currentClassloader = switchToRepositoryClassloader();
            
            try {
                return hippoRepository.getRepository().getDescriptor(key);
            } finally {
                if (currentClassloader != null) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }
            }
        }
        
        return null;
    }

    public String[] getDescriptorKeys() {
        if (jcrDelegateeRepository != null) {
            return jcrDelegateeRepository.getDescriptorKeys();
        }
        
        if (hippoRepository != null) {
            ClassLoader currentClassloader = switchToRepositoryClassloader();
            
            try {
                return hippoRepository.getRepository().getDescriptorKeys();
            } finally {
                if (currentClassloader != null) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }
            }
        }
        
        return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    public Session login() throws LoginException, RepositoryException {
        if (!repositoryInitialized) {
            initHippoRepository();
        }
        
        if (jcrDelegateeRepository != null) {
            return jcrDelegateeRepository.login();
        }
        
        ClassLoader currentClassloader = switchToRepositoryClassloader();
        
        try {
            return hippoRepository.login();
        } finally {
            if (currentClassloader != null) {
                Thread.currentThread().setContextClassLoader(currentClassloader);
            }
        }
    }

    public Session login(Credentials credentials) throws LoginException, RepositoryException {
        if (!repositoryInitialized) {
            initHippoRepository();
        }
        
        if (jcrDelegateeRepository != null) {
            return jcrDelegateeRepository.login(credentials);
        }
        
        ClassLoader currentClassloader = switchToRepositoryClassloader();
        
        try {
            return hippoRepository.login((SimpleCredentials) credentials);
        } finally {
            if (currentClassloader != null) {
                Thread.currentThread().setContextClassLoader(currentClassloader);
            }
        }
    }

    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        if (!repositoryInitialized) {
            initHippoRepository();
        }
        
        if (jcrDelegateeRepository != null) {
            return jcrDelegateeRepository.login(workspaceName);
        }
        
        return login();
    }

    public Session login(Credentials credentials, String workspaceName) throws LoginException, NoSuchWorkspaceException,
            RepositoryException {
        if (!repositoryInitialized) {
            initHippoRepository();
        }
        
        if (jcrDelegateeRepository != null) {
            return jcrDelegateeRepository.login(credentials, workspaceName);
        }
        
        return login(credentials);
    }
    
    public void closeHippoRepository() {
        if (hippoRepository != null && !localRepositoryUsed && !vmRepositoryUsed) {
            hippoRepository.close();
        }
    }
    
    /*
     * Because HippoRepository can be loaded in other classloader which is not the same as the caller's classloader,
     * the context classloader needs to be switched.
     */
    private ClassLoader switchToRepositoryClassloader() {
        if (vmRepositoryUsed) {
            return null;
        }
        
        ClassLoader repositoryClassloader = hippoRepository.getClass().getClassLoader();
        ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();
        
        if (repositoryClassloader != currentClassloader) {
            Thread.currentThread().setContextClassLoader(repositoryClassloader);
            return currentClassloader;
        } else {
            return null;
        }
    }

    public Value getDescriptorValue(String key) {
        if (jcrDelegateeRepository != null) {
            return jcrDelegateeRepository.getDescriptorValue(key);
        }
        
        if (hippoRepository != null) {
            ClassLoader currentClassloader = switchToRepositoryClassloader();
            
            try {
                return hippoRepository.getRepository().getDescriptorValue(key);
            } finally {
                if (currentClassloader != null) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }
            }
        }
        
        return null;
    }

    public Value[] getDescriptorValues(String key) {
        if (jcrDelegateeRepository != null) {
            return jcrDelegateeRepository.getDescriptorValues(key);
        }
        
        if (hippoRepository != null) {
            ClassLoader currentClassloader = switchToRepositoryClassloader();
            
            try {
                return hippoRepository.getRepository().getDescriptorValues(key);
            } finally {
                if (currentClassloader != null) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }
            }
        }
        
        return null;
    }

    public boolean isSingleValueDescriptor(String key) {
        if (jcrDelegateeRepository != null) {
            return jcrDelegateeRepository.isSingleValueDescriptor(key);
        }
        
        if (hippoRepository != null) {
            ClassLoader currentClassloader = switchToRepositoryClassloader();
            
            try {
                return hippoRepository.getRepository().isSingleValueDescriptor(key);
            } finally {
                if (currentClassloader != null) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }
            }
        }
        return false;
    }

    public boolean isStandardDescriptor(String key) {
        if (jcrDelegateeRepository != null) {
            return jcrDelegateeRepository.isStandardDescriptor(key);
        }
        
        if (hippoRepository != null) {
            ClassLoader currentClassloader = switchToRepositoryClassloader();
            
            try {
                return hippoRepository.getRepository().isStandardDescriptor(key);
            } finally {
                if (currentClassloader != null) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }
            }
        }
        return false;
    }
}
