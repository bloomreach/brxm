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
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.repository.lock.db.DbHelper.close;
import static org.onehippo.repository.lock.db.DbLockManager.EXPIRED_BLOCKING_STATEMENT;
import static org.onehippo.repository.lock.db.DbLockManager.RESET_LOCK_STATEMENT_BY_KEY_ONLY;

/**
 * Resets expired locks to 'FREE' if they are in state 'RUNNING' or 'ABORT'
 */
public class DbResetExpiredLocksJanitor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(DbResetExpiredLocksJanitor.class);

    private final DataSource dataSource;

    public DbResetExpiredLocksJanitor(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run() {
        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = dataSource.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            // since the 'expired blocking statement' can modify the same rows as another cluster node, we use
            // autocommit 'false' and use a blocking statement
            final PreparedStatement expiredBlockingStatement = connection.prepareStatement(EXPIRED_BLOCKING_STATEMENT);
            expiredBlockingStatement.setLong(1, System.currentTimeMillis());
            ResultSet resultSet = expiredBlockingStatement.executeQuery();
            while (resultSet.next()) {
                // found expired lock. Reset it
                String lockKey = resultSet.getString("lockKey");
                log.info("Resetting row with lockKey '{}' because expired", lockKey);
                final PreparedStatement resetLockStatement = connection.prepareStatement(RESET_LOCK_STATEMENT_BY_KEY_ONLY);
                resetLockStatement.setString(1, lockKey);
                resetLockStatement.execute();
            }
            connection.commit();
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.info("Exception in {} happened. Possibly another cluster node did already reset some lock rows:", this.getClass().getName(), e);
            } else {
                log.info("Exception in {} happened.  Possibly another cluster node did already reset some lock rows: {}", this.getClass().getName(), e.toString());
            }
        } finally {
            close(connection, originalAutoCommit);
        }
    }
}
