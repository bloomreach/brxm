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
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.cms7.services.lock.LockManager;
import org.onehippo.services.lock.db.DbHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbLockManager extends AbstractLockManager implements LockManager {

    private static final Logger log = LoggerFactory.getLogger(DbLockManager.class);

    final static String TABLE_NAME_LOCK = "hippolock";
    final static String TABLE_NAME_LOCK_OVERVIEW = "hippolockoverview";

    final static String CREATE_LOCK_TABLE_STATEMENT = "CREATE TABLE %s (lockKey VARCHAR(256) NOT NULL)";
    // TODO for oracle it must be NUMBER instead of BIGINT
    final static String CREATE_LOCK_OVERVIEW_TABLE_STATEMENT = "CREATE TABLE %s (lockKey VARCHAR(256) NOT NULL, lockTime BIGINT NOT NULL)";

    private DataSource dataSource;
    private final Map<String, DbLock> locks = new HashMap();



    public DbLockManager(final DataSource dataSource) {
        this.dataSource = dataSource;
        DbHelper.createTableIfNeeded(dataSource, CREATE_LOCK_TABLE_STATEMENT, TABLE_NAME_LOCK);
        DbHelper.createTableIfNeeded(dataSource, CREATE_LOCK_OVERVIEW_TABLE_STATEMENT, TABLE_NAME_LOCK_OVERVIEW);
        try {
            lock("foo");
        } catch (LockException e) {
            e.printStackTrace();
        }
    }

    @Override
    Logger getLogger() {
        return log;
    }

    @Override
    AbstractLock createLock(final String key) throws LockException {
        final String lockStatement = "SELECT * FROM " + TABLE_NAME_LOCK + " WHERE lockKey=? FOR UPDATE NOWAIT";
        final String insertStatement = "INSERT INTO " + TABLE_NAME_LOCK + " VALUES(?)";
        try (Connection connection = dataSource.getConnection()) {
            final PreparedStatement preparedLockStatement = connection.prepareStatement(lockStatement);
            preparedLockStatement.setString(1, key);
            preparedLockStatement.setQueryTimeout(10);

            // the lockResultSet must no be in the autoclosable 'try' because we need to keep it open
            ResultSet lockResultSet = preparedLockStatement.executeQuery();
            if (!lockResultSet.next()) {
                // entry did not yet exist, we need to add an entry first
                lockResultSet.close();
                final PreparedStatement preparedInsertstatement = connection.prepareStatement(insertStatement);
                preparedInsertstatement.setString(1, key);

                try {
                    preparedInsertstatement.execute();
                    connection.commit();
                } catch (SQLException e) {
                    // entry can already be created concurrently by other cluster node. We can still try to get the lock
                    // now
                    System.out.println(e);
                }
                lockResultSet = preparedLockStatement.executeQuery();
                if (!lockResultSet.next()) {
                    String msg = String.format("Unexpected : A row for '%s' was expected and if it could not be locked, an SQL Exception was " +
                            "expected.", key);
                    log.error(msg);
                    throw new LockException(msg);
                }
            }

            // push the lockResultSet in a cache such that it cannot be GC-ed and thus not closed as a result of being GC-ed
            return new DbLock(key, lockResultSet);

        } catch (SQLException e) {
            // TODO
            log.error(e.toString());
            throw new LockException(e);
        }
    }

}
