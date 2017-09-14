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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.services.lock.DbLockManager.DELETE_STATEMENT;

class DbLock extends AbstractLock {

    private static final Logger log = LoggerFactory.getLogger(DbLock.class);

    private final Connection connection;
    private final boolean originalAutoCommit;
    private ResultSet dbLockResult;

    DbLock(final String lockKey, final String clusterNodeId, final Connection connection, final boolean originalAutoCommit,  final ResultSet dbLockSet) {
        super(lockKey, clusterNodeId, Thread.currentThread().getName(), System.currentTimeMillis());
        this.connection = connection;
        this.originalAutoCommit = originalAutoCommit;
        this.dbLockResult = dbLockSet;
    }

    @Override
    void destroy() {
        try {

            // remove the row from the database (which can be done safely because we contain the lock)
            final PreparedStatement preparedSelectStatement = connection.prepareStatement(DELETE_STATEMENT);
            preparedSelectStatement.setString(1, getLockKey());
            preparedSelectStatement.setQueryTimeout(10);
            preparedSelectStatement.execute();

            connection.commit();

            dbLockResult.close();
            // The connection pool does not provide you with the actual Connection instance from the driver,
            // but returns a wrapper. When you call 'close()' on a Connection instance from the pool,
            // it will not close the driver's Connection, but instead just return the open connection to the pool
            // so that it can be re-used. Hence we first have to restore the original auto commit value before closing
            connection.setAutoCommit(originalAutoCommit);
            connection.close();
        } catch (SQLException e) {
            log.error("Error while destroying DbLock", e);
        }
    }
}
