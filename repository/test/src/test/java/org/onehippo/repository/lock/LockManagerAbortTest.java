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
import org.onehippo.cms7.services.lock.LockManagerException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.lock.db.DbLockManager.ABORT_STATEMENT;

public class LockManagerAbortTest extends AbstractLockManagerTest {


    @Test
    public void any_thread_can_request_abort_which_results_in_an_interrupt_for_thread_holding_lock() throws Exception {
        final String key = "123";
        final LockRunnable runnable = new LockRunnable(key, true, false);
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


    @Test
    public void other_cluster_node_has_set_abort() throws Exception {
        if (dataSource == null) {
            // in memory test
            return;
        }
        // by manually in the database setting status 'ABORT' is the same as if it is done on a different cluster node
        // it might only take up to 5 seconds before the LockThreadInterrupter background thread triggers an interrupt
        final String key = "123";
        final LockRunnable runnable = new LockRunnable(key, true, false);
        final Thread lockThread = new Thread(runnable);

        lockThread.start();
        // give lockThread time to lock
        Thread.sleep(100);

        // now abort via database : this is as if another cluster node did it
        abortDataRowLock(key);

        // although keepalive still true, the lockThread should via the LockThreadInterrupter get an interrupt
        lockThread.join();

        assertTrue("lockThread should be interrupted", runnable.interrupted);

        // AFTER the interrupt, the database record should be in state 'FREE' since LockRunnable invoked #unlock
        dbRowAssertion(key, "FREE");
    }

    private void abortDataRowLock(final String key) throws SQLException {
        try (Connection connection = dataSource.getConnection()){
            final PreparedStatement abortStatement = connection.prepareStatement(ABORT_STATEMENT);
            abortStatement.setLong(1, System.currentTimeMillis());
            abortStatement.setString(2, key);
            int changed = abortStatement.executeUpdate();
            assertEquals("Abort should had modified 1 row", 1, changed);
        }
    }

    @Test
    public void a_lock_set_to_abort_is_still_set_to_free_on_lockmanager_clear_but_not_being_interrupted_any_more() throws Exception {
        if (dataSource == null) {
            // in memory test
            return;
        }
        final String key = "123";
        final LockRunnable runnable = new LockRunnable(key, true, true);
        final Thread lockThread = new Thread(runnable);

        lockThread.start();
        // give lockThread time to lock
        Thread.sleep(100);

        // now abort via database : this is as if another cluster node did it
        abortDataRowLock(key);

        lockManager.clear();

        lockThread.join();

        assertFalse("Since the lock manager has been cleared (destroyed) after the ABORT was set, " +
                        "the interrupt should never be invoked." ,runnable.interrupted);
    }


    private class LockRunnable implements Runnable {

        private String key;
        private volatile boolean keepAlive;
        private boolean stopAfter10Seconds;
        private boolean interrupted = false;

        LockRunnable(final String key, final boolean keepAlive, final boolean stopAfter10Seconds) {
            this.key = key;
            this.keepAlive = keepAlive;
            this.stopAfter10Seconds = stopAfter10Seconds;
        }

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            try {
                lockManager.lock(key);
                while (keepAlive) {
                    Thread.sleep(25);
                    if ((System.currentTimeMillis() - start) > 10000) {
                        if (stopAfter10Seconds) {
                            break;
                        }
                        fail("Within 10 seconds this thread should had been interrupted");
                    }
                }
            } catch (LockException e) {
                fail("Unexpected lock exception :" + e.toString());
            } catch (InterruptedException e) {
                // The DB entry should be in status 'ABORT' and thus still locked
                boolean locked = false;
                try {
                    locked = lockManager.isLocked(key);
                } catch (LockManagerException e1) {
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
                lockManager.unlock(key);
                try {
                    if (lockManager.isLocked(key)) {
                        fail("After interruption, the current Thread holding the lock should had been able to unlock");
                    }
                } catch (LockManagerException e1) {
                    fail("#isLocked test should not fail : " + e.toString());
                }
                try {
                    dbRowAssertion(key, "FREE");
                } catch (SQLException e1) {
                    fail(String.format("SQL Exception : " + e.toString()));
                }
            }
        }
    }

}
