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

import java.sql.SQLException;

import org.junit.Test;
import org.onehippo.cms7.services.lock.LockException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        if (runnable.msgExceptionPair != null) {
            fail(runnable.msgExceptionPair.msg + " : " + runnable.msgExceptionPair.e.toString());
        }

        // AFTER the interrupt, the database record should be in state 'FREE' since LockRunnable invoked #unlock
        dbRowAssertion(key, "FREE");
    }

    @Test
    public void fake_other_cluster_node_has_set_abort() throws Exception {
        // by manually in the database setting status 'ABORT' is the same as if it is done on a different cluster node
    }


    private class LockRunnable implements Runnable {

        private String key;
        private volatile boolean keepAlive;
        private boolean interrupted = false;
        private MsgExceptionPair msgExceptionPair;

        LockRunnable(final String key , final boolean keepAlive) {
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
                msgExceptionPair = new MsgExceptionPair("LockException", e);
            } catch (InterruptedException e) {
                try {
                    // The DB entry is either in status 'ABORT' or in status 'RUNNING'
                    boolean locked = lockManager.isLocked(key);
                    if (!locked) {
                        msgExceptionPair = new MsgExceptionPair("Key should be in state ABORT or RUNNING", new IllegalStateException());
                    }
                    // because of the call above to lockManager.isLocked which is synchronized, the #abort invocation by the main
                    // thread must have finished (because synchronized on same object) and thus the database record must now
                    // be 'ABORT'
                    try {
                        dbRowAssertion(key, "ABORT");
                    } catch (SQLException e1) {
                        msgExceptionPair = new MsgExceptionPair("SQL Exception", e);
                    }
                    interrupted = true;

                    lockManager.unlock(key);

                    try {
                        dbRowAssertion(key, "FREE");
                    } catch (SQLException e1) {
                        msgExceptionPair = new MsgExceptionPair("SQL Exception", e);
                    }
                } catch (LockException e1) {
                    msgExceptionPair = new MsgExceptionPair("After interruption, the current Thread holding the lock should be able to unlock", e1);
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
