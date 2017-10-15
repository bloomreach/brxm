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
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes all locks that are free for longer than a day
 */
public class DbLockCleanupJanitor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(DbLockCleanupJanitor.class);

    private final DbLockManager dbLockManager;

    public DbLockCleanupJanitor(final DbLockManager dbLockManager) {
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

            final PreparedStatement removeStatement = connection.prepareStatement(dbLockManager.getRemoveOutdatedStatement());
            long dayAgoTime = System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);
            removeStatement.setLong(1, dayAgoTime);
            int updated = removeStatement.executeUpdate();
            log.info("Removed {} outdated locks", updated);
            removeStatement.close();
        } catch (SQLException e) {
            log.error("Error while trying remove outdated locks", e);
        } finally {
            dbLockManager.close(connection, originalAutoCommit);
        }
    }
}
