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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.Test;
import org.onehippo.cms7.services.lock.LockException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.lock.db.DbLockManager.ABORT_STATEMENT;

public class LockManagerAbortTest extends AbstractLockManagerTest {


    @Test
    public void any_thread_can_request_abort_which_results_in_an_interrupt_for_thread_holding_lock() throws Exception {
        final String key = "123";
        final LockRunnable runnable = new LockRunnable(key, true);
        final Thread lockThread = new Thread(runnable);

        lockThread.start();
        // give lockThread time to lock
        Thread.sleep(100);

        // now abort with main thread : This should signal directly an interrupt to the Thread that has the lock because
        // the Lock is kept in the same JVM
        lockManager.abort("123");

        // although keepalive still true, the Thread should still have stopped because of the interrupt
        lockThread.join();

        assertTrue("lockThread should be interrupted", runnable.interrupted);

        // AFTER the interrupt, the database record should be in state 'FREE' since LockRunnable invoked #unlock
        dbRowAssertion(key, "FREE");
    }



    private class LockRunnable implements Runnable {

        private String key;
        private volatile boolean keepAlive;
        private boolean interrupted = false;

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
                fail("Unexpected lock exception :" + e.toString());
            } catch (InterruptedException e) {
                // The DB entry should be in status 'ABORT' and thus still locked
                boolean locked = false;
                try {
                    locked = lockManager.isLocked(key);
                } catch (LockException e1) {
                    fail(e1.toString());
                }
                if (!locked) {
                    fail(String.format("Lock for '%s' should be in state ABORT or RUNNNING", key));
                }
                // because of the call above to lockManager.isLocked which is synchronized, the #abort invocation by the main
                // thread must have finished (because synchronized on same object) and thus the database record must now
                // be 'ABORT' : This subtle thing is because 'dbRowAssertion' check the database directly without taking
                // synchronization in the LockManager into account
                try {
                    dbRowAssertion(key, "ABORT");
                } catch (SQLException e1) {
                    fail(String.format("SQL Exception : " + e.toString()));
                }

                interrupted = true;
                try {
                    lockManager.unlock(key);
                } catch (LockException e1) {
                    fail("After interruption, the current Thread holding the lock should be able to unlock :" + e1.toString());
                }
                try {
                    dbRowAssertion(key, "FREE");
                } catch (SQLException e1) {
                    fail(String.format("SQL Exception : " + e.toString()));
                }
            }
        }
    }

    private static class MsgExceptionPair {
        private String msg;
        private Exception e;

        public MsgExceptionPair(final String msg, final Exception e) {
            this.msg = msg;
            this.e = e;
        }
    }
}
