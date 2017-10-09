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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.onehippo.cms7.services.lock.Lock;
import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.cms7.services.lock.LockManagerException;
import org.onehippo.repository.lock.AbstractLockManager;
import org.onehippo.repository.lock.MutableLock;
import org.onehippo.repository.lock.db.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.repository.lock.db.DbHelper.close;

public class DbLockManager extends AbstractLockManager {

    private static final Logger log = LoggerFactory.getLogger(DbLockManager.class);


    public final static String TABLE_NAME_LOCK = "hippo_lock";

    private final static String CREATE_LOCK_TABLE_STATEMENT = "CREATE TABLE %s (" +
            "lockKey VARCHAR(256) NOT NULL, " +
            "lockOwner VARCHAR(256), " +
            "lockThread VARCHAR(256)," +
            "status VARCHAR(256) NOT NULL," +
            "lockTime BIGINT," +
            "expirationTime BIGINT," +
            "lastModified BIGINT" +
            ")";

    public static final String CREATE_STATEMENT = "INSERT INTO " + TABLE_NAME_LOCK + " VALUES(?,?,?,'RUNNING',?,?,?)";
    public static final String SELECT_STATEMENT = "SELECT * FROM " + TABLE_NAME_LOCK + " WHERE lockKey=?";
    public static final String LOCK_STATEMENT = "UPDATE " + TABLE_NAME_LOCK + " SET status='RUNNING', lockTime=?, lastModified=?, expirationTime=?, lockOwner=?, lockThread=? WHERE lockKey=? AND status='FREE'";

    public static final String ALL_LOCKED_STATEMENT = "SELECT * FROM " + TABLE_NAME_LOCK + " WHERE status='RUNNING' OR status='ABORT'";

    public static final String RESET_LOCK_STATEMENT = "UPDATE " + TABLE_NAME_LOCK  + " SET " +
            "lockOwner=NULL, " +
            "lockThread=NULL, " +
            "status='FREE', " +
            "lockTime=0, " +
            "expirationTime=0, " +
            "lastModified=? " +
            "WHERE lockKey=? AND lockOwner=? AND lockThread=?";


    public static final String RESET_EXPIRED_STATEMENT = "UPDATE " + TABLE_NAME_LOCK  + " SET " +
            "lockOwner=NULL, " +
            "lockThread=NULL, " +
            "status='FREE', " +
            "lockTime=0, " +
            "expirationTime=0, " +
            "lastModified=? " +
            "WHERE expirationTime<? AND (status='RUNNING' OR status='ABORT')";

    public static final String REMOVE_OUTDATED_LOCKS = "DELETE FROM " + TABLE_NAME_LOCK + " WHERE lastModified<?";

    public static final String ABORT_STATEMENT = "UPDATE " + TABLE_NAME_LOCK  + " SET status='ABORT', lastModified=? WHERE lockKey=? AND status='RUNNING'";

    // only refreshes its own cluster locks
    public static final String REFRESH_LOCK_STATEMENT = "UPDATE " + TABLE_NAME_LOCK + " SET lastModified=?, expirationTime=expirationTime+"+ REFRESH_RATE_SECONDS * 1000 +
            " WHERE lockOwner=? AND expirationTime<? AND (status='RUNNING' OR status='ABORT')";

    public static final String SELECT_ABORT_STATEMENT = "SELECT * FROM " + TABLE_NAME_LOCK + " WHERE status='ABORT' AND lockOwner=?";

    private final DataSource dataSource;
    private final String clusterNodeId;

    public DbLockManager(final DataSource dataSource, final String clusterNodeId) {
        this.dataSource = dataSource;
        this.clusterNodeId = clusterNodeId;
        org.onehippo.repository.lock.db.DbHelper.createTableIfNeeded(dataSource, getCreateLockTableStatement(), TABLE_NAME_LOCK, "lockKey");

        addJob(new UnlockStoppedThreadJanitor());
        addJob(new DbResetExpiredLocksJanitor(dataSource));
        final int oneDaySeconds = 24 * 60 * 60;
        addJob(new DbLockCleanupJanitor(dataSource), 60, oneDaySeconds);
        addJob(new DbLockRefresher(dataSource, clusterNodeId));
        addJob(new LockThreadInterrupter(dataSource, clusterNodeId, this));
    }

    protected String getCreateLockTableStatement() {
        return CREATE_LOCK_TABLE_STATEMENT;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected synchronized MutableLock createLock(final String key, final String threadName) throws LockException {
        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = dataSource.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);

            final long lockTime = System.currentTimeMillis();
            final long expirationTime = lockTime + REFRESH_RATE_SECONDS * 1000;
            final PreparedStatement lockStatement = connection.prepareStatement(LOCK_STATEMENT);
            lockStatement.setLong(1, lockTime);
            lockStatement.setLong(2, lockTime);
            lockStatement.setLong(3, expirationTime);
            lockStatement.setString(4, clusterNodeId);
            lockStatement.setString(5, threadName);
            lockStatement.setString(6, key);
            int changed = lockStatement.executeUpdate();
            lockStatement.close();

            if (changed == 0) {
                log.debug("Either there is already a row entry for key '{}' which is not free OR the entry is not yet " +
                        "present. Trying to add it now. If that fails, another cluster already contains the lock");
                final PreparedStatement createStatement = connection.prepareStatement(CREATE_STATEMENT);
                createStatement.setString(1, key);
                createStatement.setString(2, clusterNodeId);
                createStatement.setString(3, threadName);
                createStatement.setLong(4, lockTime);
                createStatement.setLong(5, expirationTime);
                createStatement.setLong(6, lockTime);
                try {
                    createStatement.execute();
                    createStatement.close();
                } catch (SQLException e) {
                    throw new LockException(String.format("Lock for '%s' is already taken", key), e);
                }
            }
            log.debug("Obtained a lock for '{}'", key);
            return new MutableLock(key, clusterNodeId, threadName, lockTime, "RUNNING");

        } catch (SQLException e) {
            if (log.isDebugEnabled()) {
                log.info("Cannot lock '{}'. Most likely already locked by another cluster node.", e);
            } else {
                log.info("Cannot lock '{}'. Most likely already locked by another cluster node : {}", e.toString());
            }
            throw new LockException(e);
        } finally {
            close(connection, originalAutoCommit);
        }
    }

    @Override
    protected synchronized void releasePersistedLock(final String key, final String threadName) {
        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = dataSource.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            final PreparedStatement resetLockStatement = connection.prepareStatement(RESET_LOCK_STATEMENT);
            resetLockStatement.setLong(1, System.currentTimeMillis());
            resetLockStatement.setString(2, key);
            resetLockStatement.setString(3, clusterNodeId);
            resetLockStatement.setString(4, threadName);
            int changed = resetLockStatement.executeUpdate();
            resetLockStatement.close();
            if (changed == 0) {
                final PreparedStatement selectStatement = connection.prepareStatement(SELECT_STATEMENT);
                selectStatement.setString(1, key);
                ResultSet resultSet = selectStatement.executeQuery();
                if (!resultSet.next()) {
                    log.error("Database Lock '{}' cannot be released by '{}' and cluster '{}' because lock does not exist",
                            key, threadName, clusterNodeId);
                    selectStatement.close();
                    return;
                } else {
                    log.error("Database Lock '{}' cannot be released for thread '{}' and cluster '{}' because lock is not owned.",
                            key, threadName, clusterNodeId);
                    selectStatement.close();
                    return;
                }
            }
            log.info("Successfully released '{}'", key);
        } catch (SQLException e) {
            final String msg = String.format("Unlocking Database Lock '%s' for thread '%s' and cluster '%s' failed.", key, threadName, clusterNodeId, e);
            log.error(msg);
            // we do not want to throw a checked exception for #unlock because that would mean code flow that in the finally block
            // wants to unlock would always have to catch an exception....with which a developer can't do much
            throw new RuntimeException(msg, e);
        } finally {
            close(connection, originalAutoCommit);
        }
    }

    @Override
    protected synchronized void abortPersistedLock(final String key) throws LockManagerException {
        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = dataSource.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            final PreparedStatement abortStatement = connection.prepareStatement(ABORT_STATEMENT);
            abortStatement.setLong(1, System.currentTimeMillis());
            abortStatement.setString(2, key);
            int changed = abortStatement.executeUpdate();
            abortStatement.close();

            if (changed == 0) {
                // can happen because by another Thread or cluster node already stopped in the meantime
                log.info("Cannot set status to abort for '{}' because no such lock present or already aborted or not runnning.",
                        key);
                return;
            }
            log.info("Successfully changed status to abort for '{}'", key);
        } catch (SQLException e) {
            final String msg = String.format("Aborting Database Lock '%s' failed.", key);
            log.error(msg, e);
            throw new LockManagerException(msg, e);
        } finally {
            close(connection, originalAutoCommit);
        }
    }

    @Override
    protected synchronized boolean containsLock(final String key) throws LockManagerException {
        try (Connection connection = dataSource.getConnection()) {
            final PreparedStatement selectStatement = connection.prepareStatement(SELECT_STATEMENT);
            selectStatement.setString(1, key);
            ResultSet resultSet = selectStatement.executeQuery();
            if (!resultSet.next()) {
                log.debug("No database row found for lockKey '{}'", key);
                selectStatement.close();
                return false;
            }
            final String status = resultSet.getString("status");
            selectStatement.close();

            boolean locked = "RUNNING".equals(status) || "ABORT".equals(status);
            log.debug("Found database row for '{}' with locked = {}", key, locked);
            return locked;
        } catch (SQLException e) {
            final String msg = String.format("Exception while checking lock for '%s'. Return false.", key);
            log.error(msg, e);
            throw new LockManagerException(msg, e);
        }
    }

    @Override
    protected synchronized List<Lock> retrieveLocks() throws LockManagerException {
        try (Connection connection = dataSource.getConnection()) {
            final PreparedStatement selectStatement = connection.prepareStatement(ALL_LOCKED_STATEMENT);
            ResultSet resultSet = selectStatement.executeQuery();
            final List<Lock> locks = new ArrayList<>();
            while (resultSet.next()) {
                final String lockKey = resultSet.getString("lockKey");
                final String lockOwner = resultSet.getString("lockOwner");
                final String lockThread = resultSet.getString("lockThread");
                final long lockTime = resultSet.getLong("lockTime");
                final String status = resultSet.getString("status");
                final Lock lock = new Lock(lockKey, lockOwner, lockThread, lockTime, status);
                log.debug("Adding lock : {}", lock.toString());
                locks.add(lock);
            }
            resultSet.close();

            if (locks.size() == 0) {
                log.debug("No locks found");
            }
            return locks;
        } catch (SQLException e) {
            final String msg = "Exception while retrieving database locks. Return empty list";
            log.error(msg, e);
            throw new LockManagerException(msg, e);
        }
    }
}
