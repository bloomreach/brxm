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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestBasicPoolingRepository extends AbstractSpringTestCase {
    
    protected BasicPoolingRepository poolingRepository;
    protected BasicPoolingRepository readOnlyPoolingRepository;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.poolingRepository = (BasicPoolingRepository) getComponent(PoolingRepository.class.getName());
        this.readOnlyPoolingRepository = (BasicPoolingRepository) getComponent(PoolingRepository.class.getName() + ".readOnly");

        assertTrue("The maxActive configuration must be greater than zero for test.", this.poolingRepository.getMaxActive() > 0);
        assertTrue("The maxActive configuration must not be smaller than maxIdle configuration for test.", 
                this.poolingRepository.getMaxActive() >= this.poolingRepository.getMaxIdle());
        assertEquals("The whenExhaustedAction must be 'block' for test", PoolingRepository.WHEN_EXHAUSTED_BLOCK, this.poolingRepository.getWhenExhaustedAction());
        assertTrue("The maxWait configuration must be greater than zero for test.", this.poolingRepository.getMaxWait() > 0);
    }
    
    @Test
    public void testBasicPoolingRepository() throws Exception {
        Repository repository = poolingRepository;
        int maxActive = poolingRepository.getMaxActive();

        Session[] sessions = new Session[maxActive];

        for (int i = 0; i < maxActive; i++) {
            sessions[i] = repository.login();
        }

        assertTrue("Active session count is not the same as the maximum available session count.", 
                poolingRepository.getMaxActive() == poolingRepository.getNumActive());

        long start = System.currentTimeMillis();
        
        try {
            Session oneMore = repository.login();
            fail("The session must not be borrowed here because of the active session limit.");
        } catch (NoAvailableSessionException e) {
            long end = System.currentTimeMillis();
            assertTrue("The waiting time is smaller than the maxWait configuration.", 
                    (end - start) >= this.poolingRepository.getMaxWait());
        }

        for (int i = 0; i < maxActive; i++) {
            sessions[i].logout();
        }

        assertTrue("Active session count is not zero.", 0 == poolingRepository.getNumActive());

        Session session = null;

        try {
            session = repository.login();
            Node node = session.getRootNode();
            assertNotNull("The root node is null.", node);

            try {
                session.save();
            } catch (UnsupportedRepositoryOperationException uroe) {
                fail("The session from the pool is not able to save.");
            }
        } finally {
            if (session != null)
                session.logout();
        }

        session = null;

        try {
            SimpleCredentials defaultCredentials = (SimpleCredentials) poolingRepository.getDefaultCredentials();
            SimpleCredentials testCredentials = new SimpleCredentials(defaultCredentials.getUserID(),
                    defaultCredentials.getPassword());

            // Check if the session pool returns a session by a same credentials info.
            int curNumActive = poolingRepository.getNumActive();
            session = repository.login(testCredentials);
            assertNotNull("session is null.", session);
            assertTrue("Active session count is not " + (curNumActive + 1), 
                    (curNumActive + 1) == poolingRepository.getNumActive());

            session.save();
        } catch (UnsupportedRepositoryOperationException uroe) {
            fail("The session is not writable one: " + session);
        } finally {
            if (session != null)
                session.logout();
        }
    }
    
    @Test
    public void testSessionsRefreshed() throws Exception {
        Repository repository = poolingRepository;

        Session session = null;

        try {
            session = repository.login();
            assertTrue("session is not a PooledSession.", session instanceof PooledSession);
            assertFalse("The session's lastRefreshed should be non-positive number by default.", 
                        ((PooledSession) session).lastRefreshed() > 0);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
        
        session = null;

        try {
            long sessionsRefreshPendingTimeMillis = System.currentTimeMillis();
            poolingRepository.setSessionsRefreshPendingAfter(sessionsRefreshPendingTimeMillis);
            
            session = repository.login();

            long lastRefreshed = ((PooledSession) session).lastRefreshed();
            assertTrue("The session does not seem to be refreshed. sessionsRefreshPendingTimeMillis: " + sessionsRefreshPendingTimeMillis + ", lastRefreshed: " + lastRefreshed, 
                        lastRefreshed >= sessionsRefreshPendingTimeMillis);
        } finally {
            poolingRepository.setSessionsRefreshPendingAfter(0);
            if (session != null) {
                session.logout();
            }
        }
    }
    
    @Test
    public void testSessionLifeCycleManagementPerThread() throws Exception {

        final Repository repository = poolingRepository;
        int jobCount = 100;
        int workerCount = 20;
        
        LinkedList<Runnable> jobQueue = new LinkedList<Runnable>();
        
        for (int i = 0; i < jobCount; i++) {
            jobQueue.add(new UncautiousJob(repository));
        }
        
        assertTrue("Active session count is not zero.", 0 == poolingRepository.getNumActive());

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
        assertTrue("Active session count is not zero.", 0 == poolingRepository.getNumActive());
    }
    
    @Ignore
    private class Worker extends Thread {
        
        private LinkedList<Runnable> jobQueue;
        
        public Worker(LinkedList<Runnable> jobQueue) {
            this.jobQueue = jobQueue;
        }
        
        public void run() {
            // Container will invoke this (InitializationValve) initial step:
            poolingRepository.getResourceLifecycleManagement().setActive(true);

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
                    poolingRepository.getResourceLifecycleManagement().disposeAllResources();
                }
            }
        }        
    }

    @Ignore
    private class UncautiousJob implements Runnable {

        private Repository repository;
        private long maxWait;

        public UncautiousJob(Repository repository) {
            this.repository = repository;
            this.maxWait = poolingRepository.getMaxWait();
        }

        public void run() {
            long start = System.currentTimeMillis();
            
            try {
                Session session = this.repository.login();
                // forgot to invoke logout() to return the session to the pool by invoking the following:
                //session.logout();
            } catch (NoAvailableSessionException e) {
                long end = System.currentTimeMillis();
                assertTrue("No waiting occurred.", (end - start) >= this.maxWait);
                log.warn("NoAvailableSessionException occurred.");
                log.warn("Current active sessions: " + poolingRepository.getNumActive() + " / " + poolingRepository.getMaxActive() + ", waiting time: " + (end - start));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    
}
