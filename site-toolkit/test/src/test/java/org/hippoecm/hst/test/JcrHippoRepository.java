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
package org.hippoecm.hst.test;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrHippoRepository implements Repository {
    
    private static final Logger log = LoggerFactory.getLogger(JcrHippoRepository.class);

    protected String repositoryURI;
    protected HippoRepository hippoRepository;
    protected boolean vmRepositoryUsed;
    
    public JcrHippoRepository(String repositoryURI) {
        this.repositoryURI = repositoryURI;
        vmRepositoryUsed = (repositoryURI != null && repositoryURI.startsWith("vm:"));
    }
    
    private synchronized void initHippoRepository() throws RepositoryException {
        try {
            if (log.isInfoEnabled()) {
                log.info("Trying to get hippo repository from {}.", repositoryURI);
            }

            if (repositoryURI == null) {
                hippoRepository = HippoRepositoryFactory.getHippoRepository();
            } else {
                hippoRepository = HippoRepositoryFactory.getHippoRepository(repositoryURI);
            }
            
            if (log.isInfoEnabled()) {
                log.info("Has retrieved hippo repository from {}.", repositoryURI);
            }
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }
    
    public String getDescriptor(String key) {
        String descriptor = null;
        
        if (hippoRepository != null) {
            ClassLoader currentClassloader = switchToRepositoryClassloader();
            
            try {
                descriptor = hippoRepository.getRepository().getDescriptor(key);
            } finally {
                if (currentClassloader != null) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }
            }
        }
        
        return descriptor;
    }

    public String[] getDescriptorKeys() {
        String [] descriptorKeys = ArrayUtils.EMPTY_STRING_ARRAY;
        
        if (hippoRepository != null) {
            ClassLoader currentClassloader = switchToRepositoryClassloader();
            
            try {
                descriptorKeys = hippoRepository.getRepository().getDescriptorKeys();
            } finally {
                if (currentClassloader != null) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }
            }
        }
        
        return descriptorKeys;
    }

    public Session login() throws LoginException, RepositoryException {
        if (hippoRepository == null) {
            initHippoRepository();
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
        if (hippoRepository == null) {
            initHippoRepository();
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
        return login();
    }

    public Session login(Credentials credentials, String workspaceName) throws LoginException, NoSuchWorkspaceException,
            RepositoryException {
        return login(credentials);
    }
    
    public void closeHippoRepository() {
        if (hippoRepository != null) {
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
