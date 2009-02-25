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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestMultipleRepository extends AbstractSpringTestCase {
    
   
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
        
        assertFalse("The default repository must be different one from the writable repository.", defaultRepository == writableRepository);
        
        Map<Credentials, Repository> repoMap = this.multipleRepository.getRepositoryMap();
        
        assertTrue("The repository retrieved by credentials is different from the entry of the map.", 
                defaultRepository == repoMap.get(this.defaultCredentials));
        assertTrue("The repository retrieved by credentials is different from the entry of the map.", 
                writableRepository == repoMap.get(this.writableCredentials));
        
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
    public void testSessionLifeCycleManagementPerThread() throws Exception {

        final Repository repository = multipleRepository;
        BasicPoolingRepository defaultRepository = (BasicPoolingRepository) this.multipleRepository.getRepositoryByCredentials(this.defaultCredentials);
        BasicPoolingRepository writableRepository = (BasicPoolingRepository) this.multipleRepository.getRepositoryByCredentials(this.writableCredentials);
        
        int maxActive = Math.max(defaultRepository.getMaxActive(), writableRepository.getMaxActive());
        
        LinkedList<Runnable> jobQueue = new LinkedList<Runnable>();
        
        for (int i = 0; i < 1000 * maxActive; i++) {
            jobQueue.add(new UncautiousJob(repository, defaultRepository, writableRepository));
        }
        
        assertTrue("Active session count is not zero.", 0 == defaultRepository.getNumActive());
        assertTrue("Active session count is not zero.", 0 == writableRepository.getNumActive());

        Thread[] workers = new Thread[maxActive * 2];

        for (int i = 0; i < maxActive; i++) {
            workers[i] = new Worker(jobQueue);
        }

        for (int i = 0; i < maxActive; i++) {
            workers[i].start();
            workers[i].join();
        }
        
        assertTrue("The job queue is not empty.", jobQueue.isEmpty());
        assertTrue("Active session count is not zero.", 0 == defaultRepository.getNumActive());
        assertTrue("Active session count is not zero.", 0 == writableRepository.getNumActive());
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
                            rlm.disposeAllResources();
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
        private BasicPoolingRepository defaultRepository;
        private BasicPoolingRepository writableRepository;
        private long maxWaitOfDefaultRepository;
        private long maxWaitOfWritableRepository;

        public UncautiousJob(Repository repository, BasicPoolingRepository defaultRepository, BasicPoolingRepository writableRepository) {
            this.repository = repository;
            this.defaultRepository = defaultRepository;
            this.writableRepository = writableRepository;
            this.maxWaitOfDefaultRepository = defaultRepository.getMaxWait();
            this.maxWaitOfWritableRepository = writableRepository.getMaxWait();
        }

        public void run() {
            long start = System.currentTimeMillis();
            
            try {
                Session defaultSession = this.repository.login(defaultCredentials);
                // forgot to invoke logout() to return the session to the pool by invoking the following:
                //defaultSession.logout();
            } catch (NoAvailableSessionException e) {
                long end = System.currentTimeMillis();
                assertTrue("No waiting occurred.", (end - start) >= this.maxWaitOfDefaultRepository);
                log.warn("NoAvailableSessionException occurred.");
                log.warn("Current active sessions: " + defaultRepository.getNumActive() + " / " + defaultRepository.getMaxActive() + ", waiting time: " + (end - start));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            start = System.currentTimeMillis();
            
            try {
                Session writableSession = this.repository.login(writableCredentials);
                // forgot to invoke logout() to return the session to the pool by invoking the following:
                //writableSession.logout();
            } catch (NoAvailableSessionException e) {
                long end = System.currentTimeMillis();
                assertTrue("No waiting occurred.", (end - start) >= this.maxWaitOfWritableRepository);
                log.warn("NoAvailableSessionException occurred.");
                log.warn("Current active sessions: " + writableRepository.getNumActive() + " / " + writableRepository.getMaxActive() + ", waiting time: " + (end - start));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
