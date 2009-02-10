package org.hippoecm.hst.core.jcr.pool;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.hippoecm.hst.test.AbstractSpringTestCase;

public class TestBasicPoolingRepository extends AbstractSpringTestCase {

    protected BasicPoolingRepository poolingRepository;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.poolingRepository = (BasicPoolingRepository) getComponent(PoolingRepository.class.getName());
    }
    
    public void testBasicPoolingRepository() throws Exception {
        Repository repository = poolingRepository;
        int maxActive = poolingRepository.getMaxActive();

        Session[] sessions = new Session[maxActive];

        for (int i = 0; i < maxActive; i++) {
            sessions[i] = repository.login();
        }

        assertEquals("Active session count is not the same as the maximum available session count.", poolingRepository
                .getMaxActive(), poolingRepository.getNumActive());

        try {
            Session oneMore = repository.login();
            fail("The pool should limit the maximum active session object.");
        } catch (Exception e) {
        }

        for (int i = 0; i < maxActive; i++) {
            sessions[i].logout();
        }

        assertEquals("Active session count is not zero.", 0, poolingRepository.getNumActive());

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
            SimpleCredentials defaultCredentials = poolingRepository.getDefaultCredentials();
            SimpleCredentials testCredentials = new SimpleCredentials(defaultCredentials.getUserID(),
                    defaultCredentials.getPassword());

            // Check if the session pool returns a session by a same credentials info.
            int curNumActive = poolingRepository.getNumActive();
            session = repository.login(testCredentials);
            assertNotNull("session is null.", session);
            assertEquals("Active session count is not " + (curNumActive + 1), curNumActive + 1, poolingRepository
                    .getNumActive());

            session.save();
        } catch (UnsupportedRepositoryOperationException uroe) {
            fail("The session is not writable one: " + session);
        } finally {
            if (session != null)
                session.logout();
        }
    }

    public void testSessionLifeCycleManagementPerThread() throws Exception {

        final Repository repository = poolingRepository;
        int maxActive = poolingRepository.getMaxActive();

        assertEquals("Active session count is not zero.", 0, poolingRepository.getNumActive());

        // Each worker thread will mimic the servlet container's worker thread.
        Thread[] workers = new Thread[maxActive];

        for (int i = 0; i < maxActive; i++) {
            workers[i] = new Thread() {
                // Each thread worker will run 100 times simultaneously.
                public void run() {
                    // Container will invoke this (InitializationValve) initial step:
                    poolingRepository.getPooledSessionLifecycleManagement().setActive(true);

                    try {
                        // a job execution 
                        UncautiousJob job = new UncautiousJob(repository);
                        job.foo();
                    } catch (Exception e) {
                    } finally {
                        // Container will invoke this (CleanUpValve) clean up step:
                        poolingRepository.getPooledSessionLifecycleManagement().disposeAllResources();
                    }
                }
            };
        }

        for (int i = 0; i < maxActive; i++) {
            workers[i].start();
        }

        for (int i = 0; i < maxActive; i++) {
            workers[i].join();
        }

        assertEquals("Active session count is not zero.", 0, poolingRepository.getNumActive());
    }

    private class UncautiousJob {

        private Repository repository;

        public UncautiousJob(Repository repository) {
            this.repository = repository;
        }

        public void foo() {
            try {
                Session session = this.repository.login();
                Node root = session.getRootNode();
                // forgot to invoke logout() to return the session to the pool!
            } catch (Exception e) {
                fail("Cannot borrow session from the pool!");
            }
        }
    }
    
}
