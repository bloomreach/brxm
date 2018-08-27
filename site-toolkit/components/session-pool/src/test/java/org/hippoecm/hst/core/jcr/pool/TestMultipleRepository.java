/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.NoSuchElementException;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestMultipleRepository extends AbstractSessionPoolSpringTestCase {
    
    protected MultipleRepository multipleRepository;
    protected Repository repository;
    protected Credentials defaultCredentials;
    protected Credentials writableCredentials;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        this.multipleRepository = (MultipleRepository) getComponent(Repository.class.getName());
        this.repository = this.multipleRepository;
        this.defaultCredentials = (Credentials) getComponent(Credentials.class.getName() + ".default");
        this.writableCredentials = (Credentials) getComponent(Credentials.class.getName() + ".writable");
    }

    @Test
    public void testMultipleRepository() throws LoginException, RepositoryException {
        Repository defaultRepository = this.multipleRepository.getRepositoryByCredentials(this.defaultCredentials);
        Repository writableRepository = this.multipleRepository.getRepositoryByCredentials(this.writableCredentials);
        
        Session sessionFromDefaultRepository = this.repository.login(this.defaultCredentials);
        assertTrue("Current session's repository is not the expected repository", 
                defaultRepository == ((MultipleRepositoryImpl) this.multipleRepository).getCurrentThreadRepository());
        sessionFromDefaultRepository.logout();
        
        Session sessionFromWritableRepository = this.repository.login(this.writableCredentials);
        assertTrue("Current session's repository is not the expected repository", 
                writableRepository == ((MultipleRepositoryImpl) this.multipleRepository).getCurrentThreadRepository());
        sessionFromWritableRepository.logout();
    }
    
    @Test
    public void testNumActives() throws LoginException, RepositoryException {
        PoolingRepository defaultRepository = (PoolingRepository) this.multipleRepository.getRepositoryByCredentials(this.defaultCredentials);
        PoolingRepository writableRepository = (PoolingRepository) this.multipleRepository.getRepositoryByCredentials(this.writableCredentials);
        
        assertTrue("Active sessions of default repository is negative! " + defaultRepository.getNumActive(), 
                defaultRepository.getNumActive() >= 0);
        Session sessionFromDefaultRepository = this.repository.login(this.defaultCredentials);
        assertTrue("The active sessions of default repository should be greater than zero. " + defaultRepository.getNumActive(), 
                defaultRepository.getNumActive() > 0);

        // try to return session (by logout() method of session) many times to check if the numActive shrinks to negative value...
        boolean alreadyReturned = false;
        try {
            for (int i = 0; i < 2; i++) { 
                sessionFromDefaultRepository.logout();
            }
        } catch (IllegalStateException e) {
            alreadyReturned = true;
            // just ignore on pooled session which is already returned to the pool.
        }
        assertTrue("The pooled session should be already returned", alreadyReturned);
        
        assertTrue("Active sessions of default repository is negative! " + defaultRepository.getNumActive(), 
                defaultRepository.getNumActive() >= 0);
        
        sessionFromDefaultRepository = this.repository.login(this.defaultCredentials);
        Session sessionFromWritableRepository = sessionFromDefaultRepository.impersonate(this.writableCredentials);
        
        assertTrue("The active sessions of default repository should be greater than zero. " + writableRepository.getNumActive(), 
                writableRepository.getNumActive() > 0);

        // try to return session (by logout() method of session) many times to check if the numActive shrinks to negative value...
        alreadyReturned = false;
        try {
            for (int i = 0; i < 2; i++) {
                sessionFromWritableRepository.logout();
            }
        } catch (IllegalStateException e) {
            alreadyReturned = true;
            // just ignore on pooled session which is already returned to the pool.
        }
        assertTrue("The pooled session should be already returned", alreadyReturned);
        
        assertTrue("Active sessions of default repository is negative! " + writableRepository.getNumActive(), 
                writableRepository.getNumActive() >= 0);
        
        sessionFromDefaultRepository.logout();
    }
    
    @Test
    public void testSessionLifeCycleManagementPerThread() throws Exception {

        final Repository repository = multipleRepository;
        BasicPoolingRepository defaultRepository = (BasicPoolingRepository) this.multipleRepository.getRepositoryByCredentials(this.defaultCredentials);
        BasicPoolingRepository writableRepository = (BasicPoolingRepository) this.multipleRepository.getRepositoryByCredentials(this.writableCredentials);
        
        int jobCount = 100;
        int workerCount = 20;
        
        // temporary fix for concurrent first login when repository is just started: From ECM 2.10.00 and higher this is fixed
        Session session = this.multipleRepository.login();
        session.logout();
        
        LinkedList<Runnable> jobQueue = new LinkedList<Runnable>();
        
        for (int i = 0; i < jobCount; i++) {
            jobQueue.add(new UncautiousJob(repository, (i % 2 == 0 ? this.defaultCredentials : this.writableCredentials)));
        }
        
        assertTrue("Active session count is not zero.", 0 == defaultRepository.getNumActive());
        assertTrue("Active session count is not zero.", 0 == writableRepository.getNumActive());

        Thread [] workers = new Thread[workerCount];

        for (int i = 0; i < workerCount; i++) {
            workers[i] = new Worker(jobQueue);
        }

        for (Thread worker : workers) {
            worker.start();
        }
        
        for (Thread worker : workers) {
            worker.join();
        }
        
        assertTrue("The job queue is not empty.", jobQueue.isEmpty());
        assertTrue("Active session count is not zero.", 0 == defaultRepository.getNumActive());
        assertTrue("Active session count is not zero.", 0 == writableRepository.getNumActive());
    }
    
    @Test
    public void testCurrentRepositoryLifeCycleManagementPerThread() throws Exception {
        ResourceLifecycleManagement [] rlms = multipleRepository.getResourceLifecycleManagements();
        for (ResourceLifecycleManagement rlm : rlms) {
            rlm.setActive(true);
        }
        
        Repository defaultRepository = multipleRepository.getRepositoryByCredentials(this.defaultCredentials);
        Session sessionFromDefaultRepository = multipleRepository.login(this.defaultCredentials);
        assertTrue("Current session's repository is not the expected repository", 
                defaultRepository == ((MultipleRepositoryImpl) multipleRepository).getCurrentThreadRepository());
        sessionFromDefaultRepository.logout();
        assertTrue("Current session's repository is not the expected repository", 
                defaultRepository == ((MultipleRepositoryImpl) multipleRepository).getCurrentThreadRepository());
        
        for (ResourceLifecycleManagement rlm : rlms) {
            rlm.disposeResourcesAndReset();
        }
        
        assertNull("Current session's repository should have been removed after resource disposal.", 
                ((MultipleRepositoryImpl) multipleRepository).getCurrentThreadRepository());
    }
    
    @Ignore
    private class Worker extends Thread {
        
        private LinkedList<Runnable> jobQueue;
        
        public Worker(LinkedList<Runnable> jobQueue) {
            this.jobQueue = jobQueue;
        }
        
        public void run() {
            // Container will invoke this (InitializationValve) initial step:
            ResourceLifecycleManagement [] rlms = multipleRepository.getResourceLifecycleManagements();
            for (ResourceLifecycleManagement rlm : rlms) {
                rlm.setActive(true);
            }

            while (true) {
                Runnable job = null;
                
                synchronized (this.jobQueue) {
                    try {
                        job = this.jobQueue.removeFirst();
                    } catch (NoSuchElementException e) {
                        // job queue is empty, so stop here.
                        break;
                    }
                }
                    
                try {
                    job.run();
                } finally {
                    // Container will invoke this (CleanUpValve) clean up step:
                    for (ResourceLifecycleManagement rlm : rlms) {
                        try {
                            rlm.disposeResourcesAndReset();
                        } catch (Exception e) {
                            log.error("Failed to disposeAll: " + Thread.currentThread() + ", " + rlm + ", " + rlms, e);
                        }
                    }
                }
            }
        }        
    }

    @Ignore
    private class UncautiousJob implements Runnable {

        private Repository repository;
        private Credentials credentials;

        public UncautiousJob(Repository repository, Credentials credentials) {
            this.repository = repository;
            this.credentials = credentials;
        }

        public void run() {
            try {
                Session session = this.repository.login(this.credentials);
                // forgot to invoke logout() to return the session to the pool by invoking the following:
                //session.logout();
            } catch (Exception ignore) {
            }
        }
    }

}
