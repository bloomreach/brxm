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
package org.onehippo.repository.lock.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.onehippo.repository.lock.MutableLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Thread that contains a lock marked with 'ABORT' will be interrupted : In turn, that Thread should invoke #unlock itself
 * If there is no Thread for the 'ABORT' marked lock and the lock is for the current cluster node, the lock will be reset to 'FREE'
 */
public class LockThreadInterrupter implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(LockThreadInterrupter.class);

    private final DbLockManager dbLockManager;

    public LockThreadInterrupter(final DbLockManager dbLockManager) {
        this.dbLockManager = dbLockManager;
    }


    public void run() {
        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = dbLockManager.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            final PreparedStatement selectAbortStatement = connection.prepareStatement(dbLockManager.getSelectAbortStatement());
            selectAbortStatement.setString(1, dbLockManager.getClusterNodeId());
            ResultSet resultSet = selectAbortStatement.executeQuery();
            while (resultSet.next()) {
                // interrupt the thread for this lock (if still present). Otherwise ignore.
                final String lockKey = resultSet.getString("lockKey");
                final String lockThread = resultSet.getString("lockThread");
                if (lockThread == null) {
                    log.error("Illegal database row state: cannot abort db entry '{}' for which lockThread is null.", lockKey);
                    continue;
                }
                boolean lockThreadForAbortFound = false;
                for (MutableLock lock : dbLockManager.getLocalLocks().values()) {
                    Thread thread = lock.getThread().get();
                    if (thread == null || !thread.isAlive()) {
                       // ignore since will be picked up by org.onehippo.services.lock.AbstractLockManager.UnlockStoppedThreadJanitor
                    } else if (lockKey.equals(lock.getLockKey())){
                        if (!lockThread.equals(thread.getName())) {
                            log.error("Lock thread in JVM is other one than in database for lock '{}' which is an illegal state.",
                                    lockKey);
                            continue;
                        }
                        // best effort : thread interrupt : As a result, the Thread containing the lock should invoke
                        // #unlock at some point in time
                        // There are no guarantees beyond best-effort attempts to stop
                        //  processing actively executing Thread holding a lock.  For example, typical
                        // implementations will cancel via {@link Thread#interrupt}, so any
                        // task that fails to respond to interrupts may never terminate.
                        try {
                            lockThreadForAbortFound = true;
                            log.info("Found Thread '{}' to be interrupted for Lock '{}'", thread.getName(), lockKey);
                            if (thread.isInterrupted()) {
                                log.info("Thread '{}' has already been interrupted. Not interrupting again.", thread.getName());
                            } else {
                                log.info("Interrupting thread '{}'", thread.getName());
                                thread.interrupt();
                            }
                        } catch (SecurityException e) {
                            String msg = String.format("Thread '%s' is not allowed to be interrupted. Can't abort '%s'",
                                    thread.getName(), lock.getLockKey());
                            log.error(msg);
                        }
                    }
                }
                if (!lockThreadForAbortFound) {
                    log.warn("There has not been found an alive Thread for key '{}' which has status 'ABORT'. After the " +
                            "lock has been expired it will be removed by DbLockResetJanitor", lockKey);
                }
            }
            selectAbortStatement.close();
        } catch (Exception e) {
            log.error("Exception in {} happened:", this.getClass().getName(), e);
        } finally {
            dbLockManager.close(connection, originalAutoCommit);
        }
    }
}
