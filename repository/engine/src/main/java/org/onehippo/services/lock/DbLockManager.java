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
package org.onehippo.services.lock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.cms7.services.lock.LockManager;
import org.onehippo.services.lock.db.DbHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbLockManager extends AbstractLockManager implements LockManager {

    private static final Logger log = LoggerFactory.getLogger(DbLockManager.class);

    public final static String TABLE_NAME_LOCK = "hippolock";
    public final static String TABLE_NAME_LOCK_OVERVIEW = "hippolockoverview";

    final static String CREATE_LOCK_TABLE_STATEMENT = "CREATE TABLE %s (lockKey VARCHAR(256) NOT NULL)";
    // TODO for oracle it must be NUMBER instead of BIGINT
    final static String CREATE_LOCK_OVERVIEW_TABLE_STATEMENT = "CREATE TABLE %s (lockKey VARCHAR(256) NOT NULL, lockTime BIGINT NOT NULL)";

    public static final String LOCK_STATEMENT = "SELECT * FROM " + TABLE_NAME_LOCK + " WHERE lockKey=? FOR UPDATE NOWAIT";
    public static final String INSERT_STATEMENT = "INSERT INTO " + TABLE_NAME_LOCK + " VALUES(?)";
    public static final String DELETE_STATEMENT = "DELETE " + TABLE_NAME_LOCK + " WHERE lockKey=?";

    private DataSource dataSource;
    private String clusterNodeId;

    public DbLockManager(final DataSource dataSource, final String clusterNodeId) {
        this.dataSource = dataSource;
        this.clusterNodeId = clusterNodeId;
        DbHelper.createTableIfNeeded(dataSource, CREATE_LOCK_TABLE_STATEMENT, TABLE_NAME_LOCK);
        DbHelper.createTableIfNeeded(dataSource, CREATE_LOCK_OVERVIEW_TABLE_STATEMENT, TABLE_NAME_LOCK_OVERVIEW);
    }

    @Override
    Logger getLogger() {
        return log;
    }

    @Override
    AbstractLock createLock(final String key) throws LockException {
        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = dataSource.getConnection();
            originalAutoCommit = connection.getAutoCommit();

            connection.setAutoCommit(false);
            final PreparedStatement preparedLockStatement = connection.prepareStatement(LOCK_STATEMENT);
            preparedLockStatement.setString(1, key);
            preparedLockStatement.setQueryTimeout(10);

            // the lockResultSet must no be in the autoclosable 'try' because we need to keep it open
            ResultSet lockResultSet = preparedLockStatement.executeQuery();
            if (!lockResultSet.next()) {
                // entry did not yet exist, we need to add an entry first
                lockResultSet.close();
                final PreparedStatement preparedInsertstatement = connection.prepareStatement(INSERT_STATEMENT);
                preparedInsertstatement.setString(1, key);

                try {
                    preparedInsertstatement.execute();
                    connection.commit();
                } catch (SQLException e) {
                    log.debug("'{}' : Cannot created new row for key '{}' because most likely concurrently created by another " +
                            "cluster node. Can try to lock the row now{} ", e.toString(), key);
                }
                lockResultSet = preparedLockStatement.executeQuery();
                if (!lockResultSet.next()) {
                    String msg = String.format("Illegal state : A row for '%s' was expected and if it could not be locked, " +
                            "an SQL Exception was expected instead of empty result set.", key);
                    log.error(msg);
                    throw new LockException(msg);
                }
            }

            // push the connection and lockResultSet in a lock object such that it cannot be GC-ed and thus not closed as a result of being GC-ed
            return new DbLock(key, clusterNodeId, connection, originalAutoCommit, lockResultSet);

        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.setAutoCommit(originalAutoCommit);
                    connection.close();
                } catch (SQLException e1) {
                    log.error("Failed to close connection.", e);
                    throw new LockException(e);
                }
            }
            if (log.isDebugEnabled()) {
                log.info("Cannot lock '{}'. Most likely already locked by another cluster node.", e);
            } else {
                log.info("Cannot lock '{}'. Most likely already locked by another cluster node : {}", e.toString());
            }
            throw new LockException(e);
        }
    }

}
