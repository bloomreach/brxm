/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.lock;

import javax.jcr.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.repository.lock.db.DbLockManager;
import org.onehippo.repository.lock.memory.MemoryLockManager;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LockManagerDestroyTest extends AbstractLockManagerTest {

    @Override
    @After
    public void tearDown() throws Exception {
        // since LockManager#destroy removes destroys the lock manager, we need to recreate the repository per test
        super.tearDown(true);
    }

    @Test(expected = IllegalStateException.class)
    public void after_destroy_the_lock_manager_cannot_lock_any_more() throws Exception {
        lockManager.destroy();
        lockManager.lock("123");
    }

    @Test
    public void destroy_lock_manager_interrupts_running_lock_threads() throws Exception {
        final String key = "123";
        final LockRunnable runnable = new LockRunnable(key, true);
        final Thread lockThread = new Thread(runnable);

        lockThread.start();
        // give lockThread time to lock
        Thread.sleep(100);

        dbRowAssertion(key, "RUNNING");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(MemoryLockManager.class, DbLockManager.class).build()) {
            lockManager.destroy();
            assertEquals(0, interceptor.messages().count());
        }

        assertTrue("Thread containing lock 123 should had been interrupted ", runnable.interrupted);
        assertFalse(lockThread.isAlive());

        // assert db record lock is 'free'
        dbRowAssertion(key, "FREE");
    }

    @Test
    public void if_running_lock_thread_does_not_stop_on_interrupt_it_takes_10_seconds_to_destroy_and_a_warning_is_logged() throws Exception {
        final String key = "123";
        final LockUnstoppableRunnable unstoppable = new LockUnstoppableRunnable(key, true);
        final Thread unstoppableLockThread = new Thread(unstoppable);

        unstoppableLockThread.start();
        // give lockThread time to lock
        Thread.sleep(100);

        dbRowAssertion(key, "RUNNING");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(MemoryLockManager.class, DbLockManager.class).build()) {
            final long start = System.currentTimeMillis();
            lockManager.destroy();
            assertTrue("Lock Manager destroy was expected to take at least 10 seconds to wait for all interrupted threads " +
                    "holding a lock to #unlock", (System.currentTimeMillis() - start ) >= 10_000);
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Lock '123' owned by '"+getClusterNodeId(session)+"' was never unlocked. Removing the lock now.")));
        }

        assertTrue("Thread containing lock 123 should had been interrupted ", unstoppable.interrupted);
        // the thread never died and never did unlock
        assertTrue(unstoppableLockThread.isAlive());

        // however the thread never unlocked the lock, the LockManager did so in #clear
        dbRowAssertion(key, "FREE");

        // stop the thread
        unstoppable.keepAlive = false;
        unstoppableLockThread.join();
    }

    protected class LockRunnable implements Runnable {

        private String key;
        private volatile boolean keepAlive;
        private boolean interrupted;

        LockRunnable(final String key, final boolean keepAlive) {
            this.key = key;
            this.keepAlive = keepAlive;
        }

        @Override
        public void run() {
            try {
                lockManager.lock(key);
                while (keepAlive) {
                    Thread.sleep(25);
                }
            } catch (LockException e) {

            } catch (InterruptedException e) {
                lockManager.unlock(key);
                interrupted = true;
            }
        }
    }

    protected class LockUnstoppableRunnable implements Runnable {

        private String key;
        private volatile boolean keepAlive;
        private boolean interrupted;

        LockUnstoppableRunnable(final String key, final boolean keepAlive) {
            this.key = key;
            this.keepAlive = keepAlive;
        }

        @Override
        public void run() {
            // NOTE on purpose wrong construct since never #unlock is called
            while (keepAlive) {
                try {
                    lockManager.lock(key);
                    Thread.sleep(25);

                } catch (LockException | InterruptedException e) {
                    interrupted = true;
                } catch (IllegalStateException e){
                   // happens after the lock manager has been destroyed
                }

            }
        }
    }
}
