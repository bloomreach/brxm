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
import java.util.Map;

import javax.sql.DataSource;

import org.onehippo.repository.lock.MutableLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.repository.lock.db.DbHelper.close;
import static org.onehippo.repository.lock.db.DbLockManager.TABLE_NAME_LOCK;

/**
 * A Thread that contains a lock marked with 'ABORT' will be interrupted : In turn, that Thread should invoke #unlock itself
 * If there is no Thread for the 'ABORT' marked lock, the lock will be reset to 'FREE'
 */
public class LockThreadInterrupter implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(LockThreadInterrupter.class);

    private final DataSource dataSource;
    private final String clusterNodeId;
    private final Map<String, MutableLock> locks;


    public static final String SELECT_ABORT_STATEMENT = "SELECT * FROM " + TABLE_NAME_LOCK + " WHERE status='ABORT' AND lockOwner=?";

    public LockThreadInterrupter(final DataSource dataSource, final String clusterNodeId, final Map<String, MutableLock> locks) {
        this.dataSource = dataSource;
        this.clusterNodeId = clusterNodeId;
        this.locks = locks;
    }

    public void run() {
        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = dataSource.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            final PreparedStatement selectAbortStatement = connection.prepareStatement(SELECT_ABORT_STATEMENT);
            selectAbortStatement.setString(1, clusterNodeId);
            ResultSet resultSet = selectAbortStatement.executeQuery();
            while (resultSet.next()) {
                // interrupt the thread for this lock (if still present). Otherwise ignore.
                String lockKey = resultSet.getString("lockKey");
                final String lockThread = resultSet.getString("lockThread");
                if (lockThread == null) {
                    log.error("Cannot abort db entry '{}' for which lockThread is null.", lockKey);
                    continue;
                }
                boolean lockThreadForAbortFound = false;
                for (MutableLock lock : locks.values()) {
                    Thread thread = lock.getThread().get();
                    if (thread == null || !thread.isAlive()) {
                       // ignore since will be picked up by org.onehippo.services.lock.AbstractLockManager.UnlockStoppedThreadJanitor
                    } else if (lockThread.equals(thread.getName()) && lockKey.equals(lock.getLockKey())){
                        // best effort : thread interrupt : As a result, the Thread containing the lock should invoke
                        // #unlock at some point in time
                        // There are no guarantees beyond best-effort attempts to stop
                        //  processing actively executing Thread holding a lock.  For example, typical
                        // implementations will cancel via {@link Thread#interrupt}, so any
                        // task that fails to respond to interrupts may never terminate.
                        try {
                            lockThreadForAbortFound = true;
                            log.info("Found Thread '{}' to be interrupted for Lock '{}'", thread.getName(), lockKey);
                            thread.interrupt();
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
        } catch (Exception e) {
            log.error("Exception in {} happened:", this.getClass().getName(), e);
        } finally {
            close(connection, originalAutoCommit);
        }
    }
}
