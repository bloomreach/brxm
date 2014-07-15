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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.beanutils.PropertyUtils;
import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestLazyMultiplePoolingRepository {
    
    static Logger log = LoggerFactory.getLogger(TestLazyMultiplePoolingRepository.class);
    
    private Map<String, String> basicPoolConfigMap = new HashMap<String, String>();
    private SimpleCredentials nonExistingUserCreds = new SimpleCredentials("non-existing-user", "non-existing-user-password".toCharArray());
    private SimpleCredentials defaultCreds = new SimpleCredentials("admin@onehippo.org", "admin".toCharArray());
    private SimpleCredentials wikiCreds = new SimpleCredentials("admin@wiki.onehippo.org", "admin".toCharArray());
    private SimpleCredentials disposableWikiCreds = new SimpleCredentials("admin@wiki.onehippo.org;disposable", "admin".toCharArray());
    private SimpleCredentials disposableWiki2Creds = new SimpleCredentials("admin@wiki2.onehippo.org;disposable", "admin".toCharArray());
    private MultipleRepository multipleRepository;
    
    @Before
    public void setUp() {
        basicPoolConfigMap.put("repositoryAddress", "");
        basicPoolConfigMap.put("maxActive", "4");
        basicPoolConfigMap.put("maxIdle", "0");
        basicPoolConfigMap.put("minIdle", "0");
        basicPoolConfigMap.put("initialSize", "1");
        basicPoolConfigMap.put("maxWait", "10000");
        basicPoolConfigMap.put("testOnBorrow", "true");
        basicPoolConfigMap.put("testOnReturn", "false");
        basicPoolConfigMap.put("testWhileIdle", "false");
        basicPoolConfigMap.put("timeBetweenEvictionRunsMillis", "1000");
        basicPoolConfigMap.put("numTestsPerEvictionRun", "1");
        basicPoolConfigMap.put("minEvictableIdleTimeMillis", "0");
        basicPoolConfigMap.put("refreshOnPassivate", "true");
        basicPoolConfigMap.put("defaultCredentialsUserIDSeparator", "@");
        
        multipleRepository = new LazyMultipleRepositoryImpl(defaultCreds, basicPoolConfigMap);
        ((LazyMultipleRepositoryImpl) multipleRepository).setCredentialsDomainSeparator("@");
        ((LazyMultipleRepositoryImpl) multipleRepository).setDisposableUserIDPattern(".*;disposable");
        
        assertEquals(0, multipleRepository.getRepositoryMap().size());
    }

    @After
    public void tearDown() {
        ((LazyMultipleRepositoryImpl) multipleRepository).close();
    }
    
    @Test
    public void testLazyMultiplePoolingRepository() throws Exception {
        assertNull(multipleRepository.getRepositoryByCredentials(defaultCreds));
        Session session = multipleRepository.login(defaultCreds);
        assertNotNull(session);
        assertEquals(defaultCreds.getUserID(), session.getUserID());
        assertEquals(1, multipleRepository.getRepositoryMap().size());
        Repository defaultRepo = multipleRepository.getRepositoryByCredentials(defaultCreds);
        assertNotNull(defaultRepo);
        assertPoolProperties(basicPoolConfigMap, defaultRepo);
        session.logout();
        
        assertNull(multipleRepository.getRepositoryByCredentials(wikiCreds));
        session = multipleRepository.login(wikiCreds);
        assertNotNull(session);
        assertEquals(wikiCreds.getUserID(), session.getUserID());
        assertEquals(2, multipleRepository.getRepositoryMap().size());
        Repository wikiRepo = multipleRepository.getRepositoryByCredentials(wikiCreds);
        assertNotNull(wikiRepo);
        assertPoolProperties(basicPoolConfigMap, wikiRepo);
        session.logout();
        
        try {
            multipleRepository.login(nonExistingUserCreds);
            fail("It should throw repository exception here.");
        } catch (RepositoryException ignore) {
        }
    }
    
    @Test
    public void testSessionLifeCycleManagementPerThread() throws Exception {
        final Repository repository = multipleRepository;
        
        ResourceLifecycleManagement [] rlms = multipleRepository.getResourceLifecycleManagements();
        assertNotNull(rlms);
        assertEquals(2, rlms.length);
        
        Session session = multipleRepository.login(defaultCreds);
        assertNotNull(session);
        session.logout();
        session = multipleRepository.login(wikiCreds);
        assertNotNull(session);
        session.logout();
        
        rlms = multipleRepository.getResourceLifecycleManagements();
        assertNotNull(rlms);
        assertEquals(2, rlms.length);
        
        BasicPoolingRepository defaultRepository = (BasicPoolingRepository) multipleRepository.getRepositoryByCredentials(defaultCreds);
        BasicPoolingRepository wikiRepository = (BasicPoolingRepository) multipleRepository.getRepositoryByCredentials(wikiCreds);
        
        int jobCount = 40;
        int workerCount = 8;
        
        LinkedList<Runnable> jobQueue = new LinkedList<Runnable>();
        
        for (int i = 0; i < jobCount; i++) {
            jobQueue.add(new UncautiousJob(repository, (i % 2 == 0 ? defaultCreds : wikiCreds)));
        }
        
        assertTrue("Active session count is not zero.", 0 == defaultRepository.getNumActive());
        assertTrue("Active session count is not zero.", 0 == wikiRepository.getNumActive());

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
        assertEquals("Active session count is not zero.", 0, defaultRepository.getNumActive());
        assertEquals("Active session count is not zero.", 0, wikiRepository.getNumActive());
    }

    @Test
    public void testAutomaticDisposing() throws Exception {
        ((LazyMultipleRepositoryImpl) multipleRepository).setTimeBetweenEvictionRunsMillis(5000);
        ((LazyMultipleRepositoryImpl) multipleRepository).setDisposableUserIDPattern(".*;disposable");

        Session session = multipleRepository.login(wikiCreds);
        assertNotNull(session);
        Repository wikiRepo = multipleRepository.getRepositoryByCredentials(wikiCreds);
        assertNotNull(wikiRepo);
        session.logout();

        session = multipleRepository.login(disposableWikiCreds);
        assertNotNull(session);
        Repository disposableWikiRepo = multipleRepository.getRepositoryByCredentials(disposableWikiCreds);
        assertNotNull(disposableWikiRepo);
        session.logout();

        Thread.sleep(15000L);

        wikiRepo = multipleRepository.getRepositoryByCredentials(wikiCreds);
        assertNotNull(wikiRepo);

        disposableWikiRepo = multipleRepository.getRepositoryByCredentials(disposableWikiCreds);
        assertNull(disposableWikiRepo);
    }

    @Test
    public void testAutomaticDisposingVeryShortEvictionRun() throws Exception {
        ((LazyMultipleRepositoryImpl) multipleRepository).setTimeBetweenEvictionRunsMillis(1);
        ((LazyMultipleRepositoryImpl) multipleRepository).setDisposableUserIDPattern(".*;disposable");

        Session session = multipleRepository.login(wikiCreds);
        assertNotNull(session);
        Repository wikiRepo = multipleRepository.getRepositoryByCredentials(wikiCreds);
        assertNotNull(wikiRepo);
        session.logout();

        session = multipleRepository.login(disposableWikiCreds);
        assertNotNull(session);
        Repository disposableWikiRepo = multipleRepository.getRepositoryByCredentials(disposableWikiCreds);
        assertNotNull(disposableWikiRepo);
        session.logout();

        Thread.sleep(15000L);

        wikiRepo = multipleRepository.getRepositoryByCredentials(wikiCreds);
        assertNotNull(wikiRepo);

        disposableWikiRepo = multipleRepository.getRepositoryByCredentials(disposableWikiCreds);
        assertNull(disposableWikiRepo);
    }
    
    @Test
    public void testClosingForAutomaticallyDisposedPoolingRepository() throws Exception {
        ((LazyMultipleRepositoryImpl) multipleRepository).setTimeBetweenEvictionRunsMillis(1000);
        ((LazyMultipleRepositoryImpl) multipleRepository).setDisposableUserIDPattern(".*;disposable");
        
        Repository disposableWikiRepo = multipleRepository.getRepositoryByCredentials(disposableWiki2Creds);
        assertNull(disposableWikiRepo);
        
        Session session = multipleRepository.login(disposableWiki2Creds);
        assertNotNull(session);
        PoolingRepository disposableWikiPoolingRepo = (PoolingRepository) multipleRepository.getRepositoryByCredentials(disposableWiki2Creds);
        session.logout();
        assertNotNull(disposableWikiPoolingRepo);
        assertTrue(disposableWikiPoolingRepo.isActive());

        System.gc();
        Thread.sleep(3000);
        System.gc();
        
        disposableWikiRepo = multipleRepository.getRepositoryByCredentials(disposableWiki2Creds);
        assertNull(disposableWikiRepo);
        assertFalse(disposableWikiPoolingRepo.isActive());
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

    private void assertPoolProperties(Map<String, String> expectedPropMap, Object bean) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        for (Map.Entry<String, String> entry : expectedPropMap.entrySet()) {
            String propName = entry.getKey();
            String propValue = entry.getValue();
            Object beanPropValue = PropertyUtils.getProperty(bean, entry.getKey());
            assertNotNull("Cannot find a property from the bean: " + bean + ", " + propName, beanPropValue);
            
            if (beanPropValue instanceof char []) {
                assertEquals("The property has a different value: " + propName, propValue, new String((char []) beanPropValue));
            } else {
                assertEquals("The property has a different value: " + propName, propValue, beanPropValue.toString());
            }
        }
    }
}
