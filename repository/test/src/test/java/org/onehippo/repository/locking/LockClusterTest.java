/*
 * Copyright 2015-2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.locking;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;

import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.clustering.ClusterTest;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockClusterTest extends ClusterTest {

    private static final Logger log = LoggerFactory.getLogger(LockClusterTest.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Node test = session1.getRootNode().addNode("test");
        session1.getRootNode().addNode("test");
        test.addMixin(JcrConstants.MIX_LOCKABLE);
        session1.save();
    }

    @Ignore
    @Test
    public void testLockingConcurrency() throws Exception {
        LockThread lockThread1 = new LockThread(session1.impersonate(new SimpleCredentials("admin", new char[] {})));
        LockThread lockThread2 = new LockThread(session2.impersonate(new SimpleCredentials("admin", new char[] {})));
        WriteThread writeThread1 = new WriteThread(session1.impersonate(new SimpleCredentials("admin", new char[] {})));
        lockThread1.start();
        lockThread2.start();
        writeThread1.start();
        Thread.sleep(10000l);
        lockThread1.shutdown();
        lockThread2.shutdown();
        writeThread1.shutdown();
        Thread.sleep(50l);
    }

    private static class LockThread extends Thread {
        private final Session session;
        private volatile boolean cancelled = false;

        private LockThread(final Session session) {
            this.session = session;
        }

        @Override
        public synchronized void run() {
            while (!cancelled) {
                try {
                    ensureIsLockable();
                    if (locked()) {
                        unlock();
                    } else {
                        lock();
                    }
                    try {
                        Thread.sleep(5l);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                } catch (Exception e) {
                    log.error("======> failure", e);
                }
            }
        }

        private void ensureIsLockable() {
            try {
                session.getNode("/test").addMixin("mix:lockable");
                session.save();
            } catch (LockException e) {
                //ignore
            } catch (RepositoryException e) {
                log.warn("failed to set mixin: {}", e);
            }
        }

        private boolean locked() {
            try {
                final LockManager lockManager = session.getWorkspace().getLockManager();
                return lockManager.isLocked("/test");
            } catch (RepositoryException e) {
                log.warn("failed to determine locked status: {}", e);
            }
            return false;
        }

        private void lock() {
            try {
                final LockManager lockManager = session.getWorkspace().getLockManager();
                lockManager.lock("/test", false, false, 10l, null);
            } catch (LockException e) {
                //ignore
            } catch (RepositoryException e) {
                log.warn("failed to lock: {}", e);
            }
        }

        private void unlock() {
            try {
                final LockManager lockManager = session.getWorkspace().getLockManager();
                if (lockManager.getLock("/test").isLockOwningSession()) {
                    lockManager.unlock("/test");
                }
            } catch (LockException e) {
                //ignore
            } catch (RepositoryException e) {
                log.warn("failed to unlock: {}", e);
            }
        }

        private void shutdown() {
            cancelled = true;
            synchronized (this) {
                session.logout();
            }
        }
    }

    private static class WriteThread extends Thread {
        private final Session session;
        private volatile boolean cancelled = false;

        private WriteThread(final Session session) {
            this.session = session;
        }

        @Override
        public synchronized void run() {
            try {
                while (!cancelled) {
                    if (hasTestProperty()) {
                        removeTestProperty();
                    } else {
                        setTestProperty();
                    }
                    try {
                        Thread.sleep(5l);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                }
            } catch (Exception e) {
                log.error("========> failure", e);
            }
        }

        private boolean hasTestProperty() {
            try {
                return session.propertyExists("/test[2]/test");
            } catch (RepositoryException e) {
                log.warn("failed to test property existence: " + e);
            }
            return false;
        }

        private void removeTestProperty() {
            try {
                session.getProperty("/test[2]/test").remove();
                session.save();
            } catch (RepositoryException e) {
                log.warn("failed to remove property: " + e);
            }
        }

        private void setTestProperty() {
            try {
                session.getNode("/test[2]").setProperty("test", "test");
                session.save();
            } catch (RepositoryException e) {
                log.warn("failed to set property: " + e);
            }
        }

        private void shutdown() {
            cancelled = true;
            synchronized (this) {
                session.logout();
            }
        }
    }

}
