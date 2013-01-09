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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.hippoecm.hst.core.ResourceVisitor;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestBasicPoolingRepository extends AbstractSessionPoolSpringTestCase {
    
    private BasicPoolingRepository poolingRepository;
    private BasicPoolingRepository readOnlyPoolingRepository;
    
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
    public void testSessionsRefreshPendingAfter() throws Exception {
        Repository repository = poolingRepository;

        Session session = null;

        try {
            session = repository.login();
            assertTrue("session is not a PooledSession.", session instanceof PooledSession);
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
    public void testSessionRefreshOnPassivate() throws Exception {
        Repository repository = poolingRepository;
        
        poolingRepository.setRefreshOnPassivate(true);
        poolingRepository.setMaxRefreshIntervalOnPassivate(0);
        
        PooledSession session = null;
        long lastRefreshed = 0L;
        
        try {
            session = (PooledSession) repository.login();
            lastRefreshed = session.lastRefreshed();
        } finally {
            if (session != null) {
                Thread.sleep(10);
                session.logout();
                assertTrue("The session is not refreshed. session.lastRefreshed(): " + session.lastRefreshed() + ", lastRefreshed: " + lastRefreshed, session.lastRefreshed() > lastRefreshed);
            }
        }
    }
    
    @Test
    public void testSessionRefreshOnPassivateWithMaxInterval() throws Exception {
        Repository repository = poolingRepository;
        
        poolingRepository.setRefreshOnPassivate(true);
        poolingRepository.setMaxRefreshIntervalOnPassivate(Long.MAX_VALUE);
        
        PooledSession session = null;
        long lastRefreshed = 0L;
        
        try {
            session = (PooledSession) repository.login();
            lastRefreshed = session.lastRefreshed();
        } finally {
            if (session != null) {
                session.logout();
                assertTrue("The session is unexpectedly refreshed.", session.lastRefreshed() == lastRefreshed);
            }
        }
        
        poolingRepository.setMaxRefreshIntervalOnPassivate(100L);
        
        try {
            session = (PooledSession) repository.login();
            lastRefreshed = session.lastRefreshed();
        } finally {
            if (session != null) {
                Thread.sleep(200L);
                session.logout();
                assertTrue("The session is not refreshed.", session.lastRefreshed() > lastRefreshed);
            }
        }
    }
    
    @Test
    public void testSessionLifeCycleManagementPerThread() throws Exception {

        final Repository repository = poolingRepository;
        int jobCount = 100;
        int workerCount = 20;
        
        // temporary fix for concurrent first login when repository is just started: From ECM 2.10.00 and higher this is fixed
        Session session = this.poolingRepository.login();
        session.logout();
        
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
    
    @Test
    public void testImpersonatedNonPooledSessionLifeCycleManagement() throws Exception {
        PooledSessionResourceManagement pooledSessionLifecycleManagement = new PooledSessionResourceManagement();
        poolingRepository.setResourceLifecycleManagement(pooledSessionLifecycleManagement);
        poolingRepository.getResourceLifecycleManagement().setActive(true);
        
        // retrieve a pooled session from the pool
        
        Session session = poolingRepository.login();
        assertTrue("The session was not a pooled session.", session instanceof PooledSession);

        // now the managed session count must be 1.
        final List<Session> managedSessions = new ArrayList<Session>();
        pooledSessionLifecycleManagement.visitResources(new ResourceVisitor() {
            public Object resource(Object resource) {
                managedSessions.add((Session) resource);
                return null;
            }
        });
        assertEquals("The managed session count is wrong.", 1, managedSessions.size());
        
        // Now retrieve an impersonated session which is not from the pool.
        
        Session impersonatedNonPooledSession = session.impersonate(new SimpleCredentials("editor", "editor".toCharArray()));
        assertNotNull("The impersonated session is null", impersonatedNonPooledSession);
        assertFalse("The session was not a non-pooled session.", impersonatedNonPooledSession instanceof PooledSession);
        
        // now the managed session count must be 2.
        managedSessions.clear();
        pooledSessionLifecycleManagement.visitResources(new ResourceVisitor() {
            public Object resource(Object resource) {
                managedSessions.add((Session) resource);
                return null;
            }
        });
        assertEquals("The managed session count is wrong.", 2, managedSessions.size());
        
        // also, the impersonated non pooled session must be live
        assertTrue("The impersonated session is not live.", impersonatedNonPooledSession.isLive());
        
        pooledSessionLifecycleManagement.disposeAllResources();
        
        // now the managed session count must be 0.
        managedSessions.clear();
        pooledSessionLifecycleManagement.visitResources(new ResourceVisitor() {
            public Object resource(Object resource) {
                managedSessions.add((Session) resource);
                return null;
            }
        });
        assertEquals("The managed session count is wrong.", 0, managedSessions.size());
        
        // also, the impersonated non pooled session must be not live now.
        assertFalse("The impersonated session is still live.", impersonatedNonPooledSession.isLive());
    }
    
    @Test
    public void testBasicPoolingRepositoryCounters() throws Exception {
        Repository repository = poolingRepository;
        PoolingCounter counter = new DefaultPoolingCounter(true);
        poolingRepository.setPoolingCounter(counter);
        int maxActive = poolingRepository.getMaxActive();
        int maxIdle = poolingRepository.getMaxIdle();
        
        Session[] sessions = new Session[maxActive];

        for (int i = 0; i < maxActive; i++) {
            sessions[i] = repository.login();
        }
        
        assertEquals(maxActive, counter.getNumSessionsCreated());
        assertEquals(maxActive, counter.getNumSessionsActivated());
        assertEquals(maxActive, counter.getNumSessionsObtained());
        
        for (int i = 0; i < maxActive; i++) {
            sessions[i].logout();
        }
        
        assertEquals(maxActive, counter.getNumSessionsReturned());
        assertEquals(maxActive, counter.getNumSessionsPassivated());
        assertEquals(maxActive - maxIdle, counter.getNumSessionsDestroyed());
        
        for (int i = 0; i < maxActive; i++) {
            sessions[i] = repository.login();
        }
        
        assertEquals(2 * maxActive - maxIdle, counter.getNumSessionsCreated());
        assertEquals(2 * maxActive, counter.getNumSessionsActivated());
        assertEquals(2 * maxActive, counter.getNumSessionsObtained());
        
        for (int i = 0; i < maxActive; i++) {
            sessions[i].logout();
        }
        
        assertEquals(2 * maxActive, counter.getNumSessionsReturned());
        assertEquals(2 * maxActive, counter.getNumSessionsPassivated());
        assertEquals(2 * (maxActive - maxIdle), counter.getNumSessionsDestroyed());
        
        counter.reset();
        
        assertEquals(0L, counter.getNumSessionsCreated());
        assertEquals(0L, counter.getNumSessionsActivated());
        assertEquals(0L, counter.getNumSessionsObtained());
        assertEquals(0L, counter.getNumSessionsReturned());
        assertEquals(0L, counter.getNumSessionsPassivated());
        assertEquals(0L, counter.getNumSessionsDestroyed());
    }
    
    @Ignore
    public void testSessionValidity() throws Exception {
        Session session = poolingRepository.login();
        session.logout();
        
        try {
            Node root = session.getRootNode();
            fail("Must fail because the session has been already logged out.");
        } catch (IllegalStateException e) {
            log.debug("Proper illegal state: " + e);
        } catch (Exception e) {
            fail("Unexpected exception on invalid session: " + e);
        }
        
        session = poolingRepository.login();
        
        poolingRepository.close();
        
        try {
            Node root = session.getRootNode();
            fail("Must fail because the repository has been already closed.");
        } catch (IllegalStateException e) {
            log.debug("Proper illegal state: " + e);
        } catch (RepositoryException e) {
            fail("Unexpected repository exception on invalid session from closed repository: " + e);
            e.printStackTrace();
        } catch (Exception e) {
            fail("Unexpected exception on invalid session from closed repository: " + e);
            e.printStackTrace();
        }
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
                // session.logout();
            } catch (Exception e) {
                long end = System.currentTimeMillis();
                assertTrue("No waiting occurred.", (end - start) >= this.maxWait);
                log.warn("NoAvailableSessionException occurred.");
                log.warn("Current active sessions: " + poolingRepository.getNumActive() + " / " + poolingRepository.getMaxActive() + ", waiting time: " + (end - start));
            }
        }
    }
    
}
