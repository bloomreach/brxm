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
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resets expired locks to 'FREE' if they are in state 'RUNNING' or 'ABORT'
 */
public class DbResetExpiredLocksJanitor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(DbResetExpiredLocksJanitor.class);

    private final DbLockManager dbLockManager;

    public DbResetExpiredLocksJanitor(final DbLockManager dbLockManager) {
        this.dbLockManager = dbLockManager;
    }

    @Override
    public void run() {
        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = dbLockManager.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            final PreparedStatement resetStatement = connection.prepareStatement(dbLockManager.getResetExpiredStatement());
            long currentTime = System.currentTimeMillis();
            resetStatement.setLong(1, currentTime);
            resetStatement.setLong(2, currentTime);
            int updated = resetStatement.executeUpdate();
            log.info("Expired {} locks", updated);
            resetStatement.close();
        } catch (SQLException e) {
            log.error("Error while trying to reset locks", e);
        } finally {
            dbLockManager.close(connection, originalAutoCommit);
        }
    }
}
