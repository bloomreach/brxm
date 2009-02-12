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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestMultipleRepository extends AbstractSpringTestCase {
    
    static Log log = LogFactory.getLog(TestMultipleRepository.class);

    protected MultipleRepository multipleRepository;
    protected Repository repository;
    protected Credentials readOnlyCredentials;
    protected Credentials writableCredentials;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        this.multipleRepository = (MultipleRepository) getComponent(Repository.class.getName());
        this.repository = this.multipleRepository;
        this.readOnlyCredentials = (Credentials) getComponent(Credentials.class.getName() + ".readOnly");
        this.writableCredentials = (Credentials) getComponent(Credentials.class.getName() + ".writable");
    }

    @Test
    public void testMultipleRepository() throws LoginException, RepositoryException {
        Repository readOnlyRepository = this.multipleRepository.getRepositoryByCredentials(this.readOnlyCredentials);
        Repository writableRepository = this.multipleRepository.getRepositoryByCredentials(this.writableCredentials);
        
        assertFalse("The readOnly repository must be different one from the writable repository.", readOnlyRepository == writableRepository);
        
        Map<Credentials, Repository> repoMap = this.multipleRepository.getRepositoryMap();
        
        assertTrue("The repository retrieved by credentials is different from the entry of the map.", 
                readOnlyRepository == repoMap.get(this.readOnlyCredentials));
        assertTrue("The repository retrieved by credentials is different from the entry of the map.", 
                writableRepository == repoMap.get(this.writableCredentials));
        
        Session sessionFromReadOnlyRepository = this.repository.login(this.readOnlyCredentials);
        assertTrue("Current session's repository is not the expected repository", 
                readOnlyRepository == ((MultipleRepositoryImpl) this.multipleRepository).getCurrentThreadRepository());
        sessionFromReadOnlyRepository.logout();
        
        Session sessionFromWritableRepository = this.repository.login(this.writableCredentials);
        assertTrue("Current session's repository is not the expected repository", 
                writableRepository == ((MultipleRepositoryImpl) this.multipleRepository).getCurrentThreadRepository());
        sessionFromWritableRepository.logout();
    }
    
    @Test
    public void testSessionLifeCycleManagementPerThread() throws Exception {

        final Repository repository = multipleRepository;
        BasicPoolingRepository readOnlyRepository = (BasicPoolingRepository) this.multipleRepository.getRepositoryByCredentials(this.readOnlyCredentials);
        BasicPoolingRepository writableRepository = (BasicPoolingRepository) this.multipleRepository.getRepositoryByCredentials(this.writableCredentials);
        
        int maxActive = Math.max(readOnlyRepository.getMaxActive(), writableRepository.getMaxActive());
        
        LinkedList<Runnable> jobQueue = new LinkedList<Runnable>();
        
        for (int i = 0; i < 1000 * maxActive; i++) {
            jobQueue.add(new UncautiousJob(repository, readOnlyRepository, writableRepository));
        }
        
        assertTrue("Active session count is not zero.", 0 == readOnlyRepository.getNumActive());
        assertTrue("Active session count is not zero.", 0 == writableRepository.getNumActive());

        Thread[] workers = new Thread[maxActive * 2];

        for (int i = 0; i < maxActive; i++) {
            workers[i] = new Worker(jobQueue, readOnlyRepository, writableRepository);
        }

        for (int i = 0; i < maxActive; i++) {
            workers[i].start();
            workers[i].join();
        }
        
        assertTrue("The job queue is not empty.", jobQueue.isEmpty());
        assertTrue("Active session count is not zero.", 0 == readOnlyRepository.getNumActive());
        assertTrue("Active session count is not zero.", 0 == writableRepository.getNumActive());
    }
    
    @Ignore
    private class Worker extends Thread {
        
        private LinkedList<Runnable> jobQueue;
        private BasicPoolingRepository readOnlyRepository;
        private BasicPoolingRepository writableRepository;
        
        public Worker(LinkedList<Runnable> jobQueue, BasicPoolingRepository readOnlyRepository, BasicPoolingRepository writableRepository) {
            this.jobQueue = jobQueue;
            this.readOnlyRepository = readOnlyRepository;
            this.writableRepository = writableRepository;
        }
        
        public void run() {
            // Container will invoke this (InitializationValve) initial step:
            this.readOnlyRepository.getPooledSessionLifecycleManagement().setActive(true);
            this.writableRepository.getPooledSessionLifecycleManagement().setActive(true);

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
                    this.readOnlyRepository.getPooledSessionLifecycleManagement().disposeAllResources();
                    this.writableRepository.getPooledSessionLifecycleManagement().disposeAllResources();
                }
            }
        }        
    }

    @Ignore
    private class UncautiousJob implements Runnable {

        private Repository repository;
        private BasicPoolingRepository readOnlyRepository;
        private BasicPoolingRepository writableRepository;
        private long maxWaitOfReadOnlyRepository;
        private long maxWaitOfWritableRepository;

        public UncautiousJob(Repository repository, BasicPoolingRepository readOnlyRepository, BasicPoolingRepository writableRepository) {
            this.repository = repository;
            this.readOnlyRepository = readOnlyRepository;
            this.writableRepository = writableRepository;
            this.maxWaitOfReadOnlyRepository = readOnlyRepository.getMaxWait();
            this.maxWaitOfWritableRepository = writableRepository.getMaxWait();
        }

        public void run() {
            long start = System.currentTimeMillis();
            
            try {
                Session readOnlySession = this.repository.login(readOnlyCredentials);
                // forgot to invoke logout() to return the session to the pool by invoking the following:
                //session.logout();
            } catch (NoAvailableSessionException e) {
                long end = System.currentTimeMillis();
                assertTrue("No waiting occurred.", (end - start) >= this.maxWaitOfReadOnlyRepository);
                log.warn("NoAvailableSessionException occurred.");
                log.warn("Current active sessions: " + readOnlyRepository.getNumActive() + " / " + readOnlyRepository.getMaxActive() + ", waiting time: " + (end - start));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            start = System.currentTimeMillis();
            
            try {
                Session readOnlySession = this.repository.login(writableCredentials);
                // forgot to invoke logout() to return the session to the pool by invoking the following:
                //session.logout();
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
