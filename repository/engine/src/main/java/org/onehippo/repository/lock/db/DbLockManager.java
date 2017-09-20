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
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.onehippo.cms7.services.lock.Lock;
import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.repository.lock.AbstractLockManager;
import org.onehippo.repository.lock.MutableLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.repository.lock.db.DbHelper.close;

public class DbLockManager extends AbstractLockManager {

    private static final Logger log = LoggerFactory.getLogger(DbLockManager.class);


    public final static String TABLE_NAME_LOCK = "hippo_lock";

    // TODO for oracle it must be NUMBER instead of BIGINT
    final static String CREATE_LOCK_TABLE_STATEMENT = "CREATE TABLE %s (" +
            "lockKey VARCHAR(256) NOT NULL, " +
            "lockOwner VARCHAR(256), " +
            "lockThread VARCHAR(256)," +
            "status VARCHAR(256) NOT NULL," +
            "lockTime BIGINT," +
            "refreshRateSeconds SMALLINT," +
            "expirationTime BIGINT" +
            ")";

    public static final String CREATE_STATEMENT = "INSERT INTO " + TABLE_NAME_LOCK + " VALUES(?,?,?,'RUNNING',?,?,?)";
    public static final String SELECT_STATEMENT = "SELECT * FROM " + TABLE_NAME_LOCK + " WHERE lockKey=?";
    public static final String LOCK_STATEMENT = "UPDATE " + TABLE_NAME_LOCK + " SET status='RUNNING', lockTime=?, expirationTime=?, lockOwner=?, lockThread=? WHERE lockKey=? AND status='FREE'";

    public static final String EXPIRED_BLOCKING_STATEMENT = "SELECT * FROM " + TABLE_NAME_LOCK + " WHERE expirationTime<? AND (status='RUNNING' OR status='ABORT') FOR UPDATE";

    public static final String ALL_LOCKED_STATEMENT = "SELECT * FROM " + TABLE_NAME_LOCK + " WHERE status='RUNNING' OR status='ABORT'";

    public static final String RESET_LOCK_STATEMENT = "UPDATE " + TABLE_NAME_LOCK  + " SET " +
            "lockOwner=NULL, " +
            "lockThread=NULL, " +
            "status='FREE', " +
            "lockTime=0, " +
            "refreshRateSeconds=0, " +
            "expirationTime=0 " +
            "WHERE lockKey=? AND lockOwner=? AND lockThread=?";


    public static final String RESET_LOCK_STATEMENT_BY_KEY_ONLY = "UPDATE " + TABLE_NAME_LOCK  + " SET " +
            "lockOwner=NULL, " +
            "lockThread=NULL, " +
            "status='FREE', " +
            "lockTime=0, " +
            "refreshRateSeconds=0, " +
            "expirationTime=0 " +
            "WHERE lockKey=?";

    public static final String ABORT_STATEMENT = "UPDATE " + TABLE_NAME_LOCK  + " SET status='ABORT' WHERE lockKey=?";

    // only refreshes its own cluster locks
    public static final String LOCKS_TO_REFRESH_BLOCKING_STATEMENT = "SELECT * FROM " + TABLE_NAME_LOCK + " WHERE lockOwner=? AND expirationTime<? AND status='RUNNING' FOR UPDATE";
    public static final String REFRESH_LOCK_STATEMENT = "UPDATE " + TABLE_NAME_LOCK + " SET expirationTime=? WHERE lockKey=?";


    private final DataSource dataSource;
    private final String clusterNodeId;

    public DbLockManager(final DataSource dataSource, final String clusterNodeId) {
        this.dataSource = dataSource;
        this.clusterNodeId = clusterNodeId;
        DbHelper.createTableIfNeeded(dataSource, CREATE_LOCK_TABLE_STATEMENT, TABLE_NAME_LOCK, "lockKey");

        addJob(new UnlockStoppedThreadJanitor());
        addJob(new DbResetExpiredLocksJanitor(dataSource));
        addJob(new DbLockRefresher(dataSource, clusterNodeId));
        addJob(new LockThreadInterrupter(dataSource, clusterNodeId, this));
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected synchronized MutableLock createLock(final String key, final String threadName, final int refreshRateSeconds) throws LockException {
        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = dataSource.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);

            final long lockTime = System.currentTimeMillis();
            final long expirationTime = lockTime + refreshRateSeconds * 1000;
            final PreparedStatement lockStatement = connection.prepareStatement(LOCK_STATEMENT);
            lockStatement.setLong(1, lockTime);
            lockStatement.setLong(2, expirationTime);
            lockStatement.setString(3, clusterNodeId);
            lockStatement.setString(4, threadName);
            lockStatement.setString(5, key);
            int changed = lockStatement.executeUpdate();

            if (changed == 0) {
                log.debug("Either there is already a row entry for key '{}' which is not free OR the entry is not yet " +
                        "present. Trying to add it now. If that fails, another cluster already contains the lock");
                final PreparedStatement createStatement = connection.prepareStatement(CREATE_STATEMENT);
                createStatement.setString(1, key);
                createStatement.setString(2, clusterNodeId);
                createStatement.setString(3, threadName);
                createStatement.setLong(4, lockTime);
                createStatement.setInt(5, refreshRateSeconds);
                createStatement.setLong(6, expirationTime);
                try {
                    createStatement.execute();
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
    protected synchronized void releasePersistedLock(final String key, final String threadName) throws LockException {
        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = dataSource.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            final PreparedStatement resetLockStatement = connection.prepareStatement(RESET_LOCK_STATEMENT);
            resetLockStatement.setString(1, key);
            resetLockStatement.setString(2, clusterNodeId);
            resetLockStatement.setString(3, threadName);
            int changed = resetLockStatement.executeUpdate();
            if (changed == 0) {
                final PreparedStatement selectStatement = connection.prepareStatement(SELECT_STATEMENT);
                selectStatement.setString(1, key);
                ResultSet resultSet = selectStatement.executeQuery();
                if (!resultSet.next()) {
                    String msg = String.format("Lock '%s' cannot be released by '%s' because lock does not exist", key, threadName);
                    log.warn(msg);
                    throw new LockException(msg);
                } else {
                    String msg = String.format("Lock '%s' cannot be released for thread '%s' because lock is not owned.", key, threadName);
                    log.warn(msg);
                    throw new LockException(msg);
                }
            }
            log.info("Successfully released '{}'", key);
        } catch (SQLException e) {
            String msg = String.format("Cannot unlock '%s'.", key);
            if (log.isDebugEnabled()) {
                log.info(msg, e);
            } else {
                log.info(msg + " : {}", e.toString());

            }
            throw new LockException(msg, e);
        } finally {
            close(connection, originalAutoCommit);
        }
    }

    @Override
    protected synchronized void abortPersistedLock(final String key) throws LockException {
        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = dataSource.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            final PreparedStatement abortStatement = connection.prepareStatement(ABORT_STATEMENT);
            abortStatement.setString(1, key);
            int changed = abortStatement.executeUpdate();
            if (changed == 0) {
                // can happen because by another Thread or cluster node already stopped in the meantime
                log.info("Cannot set status to abort for '{}' because no such lock present.", key);
                return;
            }
            log.info("Successfully changed status to abort for '{}'", key);
        } catch (SQLException e) {
            String msg = String.format("Cannot abort '%s'.", key);
            if (log.isDebugEnabled()) {
                log.info(msg, e);
            } else {
                log.info(msg + " : {}", e.toString());

            }
            throw new LockException(msg, e);
        } finally {
            close(connection, originalAutoCommit);
        }
    }

    @Override
    protected synchronized boolean containsLock(final String key) throws LockException {
        try (Connection connection = dataSource.getConnection()) {
            final PreparedStatement selectStatement = connection.prepareStatement(SELECT_STATEMENT);
            selectStatement.setString(1, key);
            ResultSet resultSet = selectStatement.executeQuery();
            if (!resultSet.next()) {
                log.debug("No database row found for lockKey '{}'", key);
                return false;
            }
            final String status = resultSet.getString("status");
            boolean locked = "RUNNING".equals(status) || "ABORT".equals(status);
            log.debug("Found database row for '{}' with locked = {}", key, locked);
            return locked;
        } catch (SQLException e) {
            String msg = String.format("Could not query for '%s'.", key);
            if (log.isDebugEnabled()) {
                log.info(msg, e);
            } else {
                log.info(msg + " : {}", e.toString());

            }
            throw new LockException(msg, e);
        }
    }

    @Override
    protected synchronized List<Lock> retrieveLocks() throws LockException {
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
            if (locks.size() == 0) {
                log.debug("No locks found");
            }
            return locks;
        } catch (SQLException e) {
            String msg = String.format("Could retrieve locks");
            if (log.isDebugEnabled()) {
                log.info(msg, e);
            } else {
                log.info(msg + " : {}", e.toString());

            }
            throw new LockException(msg, e);
        }
    }
}
