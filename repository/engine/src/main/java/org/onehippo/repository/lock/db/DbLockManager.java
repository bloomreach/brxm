/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.onehippo.cms7.services.lock.Lock;
import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.cms7.services.lock.LockManagerException;
import org.onehippo.cms7.services.lock.AlreadyLockedException;
import org.onehippo.repository.lock.AbstractLockManager;
import org.onehippo.repository.lock.MutableLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbLockManager extends AbstractLockManager {

    private static final Logger log = LoggerFactory.getLogger(DbLockManager.class);


    private final static String TABLE_NAME_LOCK = "HIPPO_LOCK";

    private final static String CREATE_LOCK_TABLE_STATEMENT = "CREATE TABLE %s (" +
            "lockKey VARCHAR(190) NOT NULL, " +
            "lockOwner VARCHAR(256), " +
            "lockThread VARCHAR(256)," +
            "status VARCHAR(256) NOT NULL," +
            "lockTime BIGINT," +
            "expirationTime BIGINT," +
            "lastModified BIGINT" +
            ")";

    private static final String CREATE_STATEMENT = "INSERT INTO %s VALUES(?,?,?,'RUNNING',?,?,?)";
    private static final String SELECT_STATEMENT = "SELECT * FROM %s WHERE lockKey=?";
    private static final String LOCK_STATEMENT = "UPDATE %s SET status='RUNNING', lockTime=?, lastModified=?, expirationTime=?, lockOwner=?, lockThread=? WHERE lockKey=? AND status='FREE'";

    private static final String ALL_LOCKED_STATEMENT = "SELECT * FROM %s WHERE status='RUNNING' OR status='ABORT'";

    private static final String RESET_LOCK_STATEMENT = "UPDATE %s SET " +
            "lockOwner=NULL, " +
            "lockThread=NULL, " +
            "status='FREE', " +
            "lockTime=0, " +
            "expirationTime=0, " +
            "lastModified=? " +
            "WHERE lockKey=? AND lockOwner=? AND lockThread=?";

    private static final String RESET_INVALID_LIVE_LOCKS_STATEMENT = "UPDATE %s SET " +
            "lockOwner=NULL, " +
            "lockThread=NULL, " +
            "status='FREE', " +
            "lockTime=0, " +
            "expirationTime=0, " +
            "lastModified=? " +
            "WHERE lockOwner=? AND (status='RUNNING' OR status='ABORT')";


    private static final String RESET_EXPIRED_STATEMENT = "UPDATE %s SET " +
            "lockOwner=NULL, " +
            "lockThread=NULL, " +
            "status='FREE', " +
            "lockTime=0, " +
            "expirationTime=0, " +
            "lastModified=? " +
            "WHERE expirationTime<? AND (status='RUNNING' OR status='ABORT')";

    private static final String REMOVE_OUTDATED_LOCKS = "DELETE FROM %s WHERE lastModified<?";

    private static final String ABORT_STATEMENT = "UPDATE %s SET status='ABORT', lastModified=? WHERE lockKey=? AND status='RUNNING'";

    // only refreshes its own cluster locks
    private static final String REFRESH_LOCK_STATEMENT = "UPDATE %s SET lastModified=?, expirationTime=expirationTime+"+ REFRESH_RATE_SECONDS * 1000 +
            " WHERE lockOwner=? AND expirationTime<? AND (status='RUNNING' OR status='ABORT')";

    private static final String SELECT_ABORT_STATEMENT = "SELECT * FROM %s WHERE status='ABORT' AND lockOwner=?";

    private final DataSource dataSource;
    private final String clusterNodeId;
    private final String tableName;
    private final String createStatement;
    private final String selectStatement;
    private final String lockStatement;
    private final String allLockedStatement;
    private final String resetLockStatement;
    private final String resetExpiredStatement;
    private final String resetInvalidLiveLocksStatement;
    private final String removeOutdatedStatement;
    private final String abortStatement;
    private final String refreshLockStatement;
    private final String selectAbortStatement;

    protected DbLockManager(final ConnectionHelper connectionHelper, final DataSource dataSource,
                         final String schemaObjectPrefix, final boolean schemaCheckEnabled, final String clusterNodeId) {
        this.dataSource = dataSource;
        this.clusterNodeId = clusterNodeId;
        this.tableName = schemaObjectPrefix.toUpperCase() + TABLE_NAME_LOCK;
        this.createStatement = String.format(CREATE_STATEMENT, tableName);
        this.selectStatement = String.format(SELECT_STATEMENT, tableName);
        this.lockStatement = String.format(LOCK_STATEMENT, tableName);
        this.allLockedStatement = String.format(ALL_LOCKED_STATEMENT, tableName);
        this.resetLockStatement = String.format(RESET_LOCK_STATEMENT, tableName);
        this.resetExpiredStatement = String.format(RESET_EXPIRED_STATEMENT, tableName);
        this.resetInvalidLiveLocksStatement = String.format(RESET_INVALID_LIVE_LOCKS_STATEMENT, tableName);
        this.removeOutdatedStatement = String.format(REMOVE_OUTDATED_LOCKS, tableName);
        this.abortStatement = String.format(ABORT_STATEMENT, tableName);
        this.refreshLockStatement = String.format(REFRESH_LOCK_STATEMENT, tableName);
        this.selectAbortStatement = String.format(SELECT_ABORT_STATEMENT, tableName);

        createTableIfNeeded(dataSource, connectionHelper, getCreateLockTableStatement(), tableName, schemaCheckEnabled, "lockKey");

        resetInvalidLiveLocks();

        addJob(new UnlockStoppedThreadJanitor());
        addJob(new DbResetExpiredLocksJanitor(this));
        final int oneDaySeconds = 24 * 60 * 60;
        addJob(new DbLockCleanupJanitor(this), 60, oneDaySeconds);
        addJob(new DbLockRefresher(this));
        addJob(new LockThreadInterrupter(this));
    }

    /**
     * Creates the table {@code tableName} and throws a {@link RuntimeException} if it does not succeed in it. Note that
     * if in the meantime another cluster node has created the table, this method does not throw an exception but just
     * returns.
     * @param dataSource
     * @param tableName
     */
    public void createTableIfNeeded(final DataSource dataSource,
                                    final ConnectionHelper connectionHelper,
                                    final String createTableStatement,
                                    final String tableName,
                                    final boolean schemaCheckEnabled,
                                    final String ... uniqueIndexes) throws RuntimeException {
        try {
            if (schemaCheckEnabled && !connectionHelper.tableExists(tableName)) {
                createTable(dataSource, connectionHelper, createTableStatement, tableName, uniqueIndexes);
            }
        } catch (SQLException e) {
            final RuntimeException re = new RuntimeException("Could not get a connection or could not (check to) create table", e);
            log.error(re.getMessage());
            throw re;
        }
    }

    protected void createTable(final DataSource dataSource, final ConnectionHelper connectionHelper, final String createTableStatement, final String tableName, final String[] uniqueIndexes) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            log.info("Creating table {} ", tableName);
            try (Statement statement = connection.createStatement()) {
                statement.addBatch(String.format(createTableStatement, tableName));
                int index = 1;
                for (String uniqueIndex : uniqueIndexes) {
                    statement.addBatch("CREATE UNIQUE INDEX " + tableName + "_idx_"+index+" on " + tableName + "("+uniqueIndex+")");
                    index++;
                }
                statement.setQueryTimeout(10);
                statement.executeBatch();
            } catch (SQLException e) {
                if (connectionHelper.tableExists(tableName)) {
                    log.debug("Table {} already created by another cluster node", tableName);
                } else {
                    final String errm = "Failed to create table "+ tableName;
                    log.error(errm + ": {}", e.getMessage());
                    throw new RuntimeException(errm, e);
                }
            }
        }
    }

    private void resetInvalidLiveLocks() {
        // stop any lockKey for the current cluster node ID that is in state RUNNING (or ABORT): This can happen when a cluster node
        // has an ungraceful shutdown (or graceful but some jobs did not finish not clearing the locks) AND restarts within
        // 1 minute since then the DbResetExpiredLocksJanitor did not yet clean up the abandoned locks
        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            try (final PreparedStatement resetObsoleteLocksStatement = connection.prepareStatement(resetInvalidLiveLocksStatement)) {
                long currentTime = System.currentTimeMillis();
                resetObsoleteLocksStatement.setLong(1, currentTime);
                resetObsoleteLocksStatement.setString(2, getClusterNodeId());
                int updated = resetObsoleteLocksStatement.executeUpdate();
                log.info("Reset {} locks", updated);
            }
        } catch (SQLException e) {
            log.error("Error while trying to reset locks", e);
        } finally {
            close(connection, originalAutoCommit);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public String getClusterNodeId() {
        return clusterNodeId;
    }

    protected String getCreateLockTableStatement() {
        return CREATE_LOCK_TABLE_STATEMENT;
    }

    public String getTableName() {
        return tableName;
    }

    public String getCreateStatement() {
        return createStatement;
    }

    public String getSelectStatement() {
        return selectStatement;
    }

    public String getLockStatement() {
        return lockStatement;
    }

    public String getAllLockedStatement() {
        return allLockedStatement;
    }

    public String getResetLockStatement() {
        return resetLockStatement;
    }

    public String getResetExpiredStatement() {
        return resetExpiredStatement;
    }

    public String getRemoveOutdatedStatement() {
        return removeOutdatedStatement;
    }

    public String getAbortStatement() {
        return abortStatement;
    }

    public String getRefreshLockStatement() {
        return refreshLockStatement;
    }

    public String getSelectAbortStatement() {
        return selectAbortStatement;
    }

    public void close(final Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            log.error("Failed to close connection.", e);
        }
    }

    public void close(final Connection connection, final boolean originalAutoCommit)  {
        if (connection == null) {
            return;
        }
        try {
            connection.setAutoCommit(originalAutoCommit);
            connection.close();
        } catch (SQLException e) {
            log.error("Failed to close connection.", e);
        }
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
            try (final PreparedStatement lockStatement = connection.prepareStatement(getLockStatement())) {
                lockStatement.setLong(1, lockTime);
                lockStatement.setLong(2, lockTime);
                lockStatement.setLong(3, expirationTime);
                lockStatement.setString(4, clusterNodeId);
                lockStatement.setString(5, threadName);
                lockStatement.setString(6, key);
                int changed = lockStatement.executeUpdate();
                // explicit early close to release resources
                lockStatement.close();

                if (changed == 0) {
                    log.debug("Either there is already a row entry for key '{}' which is not free OR the entry is not yet " +
                            "present. Trying to add it now. If that fails, another cluster already contains the lock", key);
                    try (final PreparedStatement createStatement = connection.prepareStatement(getCreateStatement())) {
                        createStatement.setString(1, key);
                        createStatement.setString(2, clusterNodeId);
                        createStatement.setString(3, threadName);
                        createStatement.setLong(4, lockTime);
                        createStatement.setLong(5, expirationTime);
                        createStatement.setLong(6, lockTime);
                        try {
                            createStatement.execute();
                        } catch (SQLException e) {
                            throw new AlreadyLockedException(String.format("Lock for '%s' is already taken", key), e);
                        }
                    }
                }
            }
            log.debug("Obtained a lock for '{}'", key);
            return new MutableLock(key, clusterNodeId, threadName, lockTime, "RUNNING");

        } catch (SQLException e) {
            if (log.isDebugEnabled()) {
                log.info("Cannot lock '{}'.", e);
            } else {
                log.info("Cannot lock '{}'.", e.toString());
            }
            throw new LockManagerException(e);
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
            try (final PreparedStatement resetLockStatement = connection.prepareStatement(getResetLockStatement())) {
                resetLockStatement.setLong(1, System.currentTimeMillis());
                resetLockStatement.setString(2, key);
                resetLockStatement.setString(3, clusterNodeId);
                resetLockStatement.setString(4, threadName);
                int changed = resetLockStatement.executeUpdate();
                // explicit early close to release resources
                resetLockStatement.close();
                if (changed == 0) {
                    try (final PreparedStatement selectStatement = connection.prepareStatement(getSelectStatement())) {
                        selectStatement.setString(1, key);
                        try (final ResultSet resultSet = selectStatement.executeQuery()) {
                            if (!resultSet.next()) {
                                log.error("Database Lock '{}' cannot be released by '{}' and cluster '{}' because lock does not exist",
                                        key, threadName, clusterNodeId);
                            } else {
                                log.error("Database Lock '{}' cannot be released for thread '{}' and cluster '{}' because lock is not owned.",
                                        key, threadName, clusterNodeId);
                            }
                        }
                    }
                } else {
                    log.info("Successfully released '{}'", key);
                }
            }
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
            try (final PreparedStatement abortStatement = connection.prepareStatement(getAbortStatement())) {
                abortStatement.setLong(1, System.currentTimeMillis());
                abortStatement.setString(2, key);
                int changed = abortStatement.executeUpdate();
                if (changed == 0) {
                    // can happen because by another Thread or cluster node already stopped in the meantime
                    log.info("Cannot set status to abort for '{}' because no such lock present or already aborted or not runnning.",
                            key);
                } else {
                    log.info("Successfully changed status to abort for '{}'", key);
                }
            }
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
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement selectStatement = connection.prepareStatement(getSelectStatement())) {
            selectStatement.setString(1, key);
            try (final ResultSet resultSet = selectStatement.executeQuery()) {
                if (!resultSet.next()) {
                    log.debug("No database row found for lockKey '{}'", key);
                    return false;
                }
                final String status = resultSet.getString("status");

                boolean locked = "RUNNING".equals(status) || "ABORT".equals(status);
                log.debug("Found database row for '{}' with locked = {}", key, locked);
                return locked;
            }
        } catch (SQLException e) {
            final String msg = String.format("Exception while checking lock for '%s'. Return false.", key);
            log.error(msg, e);
            throw new LockManagerException(msg, e);
        }
    }

    @Override
    protected synchronized List<Lock> retrieveLocks() throws LockManagerException {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement selectStatement = connection.prepareStatement(getAllLockedStatement());
             final ResultSet resultSet = selectStatement.executeQuery()) {
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
            final String msg = "Exception while retrieving database locks. Return empty list";
            log.error(msg, e);
            throw new LockManagerException(msg, e);
        }
    }
}
