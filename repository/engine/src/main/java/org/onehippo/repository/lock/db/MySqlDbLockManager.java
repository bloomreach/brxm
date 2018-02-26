/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MySqlDbLockManager extends DbLockManager {

    private static final Logger log = LoggerFactory.getLogger(DbLockManager.class);


    protected MySqlDbLockManager(final ConnectionHelper connectionHelper, final DataSource dataSource,
                                 final String schemaObjectPrefix, final boolean schemaCheckEnabled, final String clusterNodeId) {
        super(connectionHelper, dataSource, schemaObjectPrefix, schemaCheckEnabled, clusterNodeId);
    }

    public void createTableIfNeeded(final DataSource dataSource,
                                    final ConnectionHelper connectionHelper,
                                    final String createTableStatement,
                                    final String tableName,
                                    final boolean schemaCheckEnabled,
                                    final String ... uniqueIndexes) throws RuntimeException {
        try {
            if (schemaCheckEnabled && !connectionHelper.tableExists(tableName)) {
                createTable(dataSource, connectionHelper, createTableStatement, tableName, uniqueIndexes);
            } else {
                try (final Connection connection = dataSource.getConnection();
                     final Statement indicesStatement = connection.createStatement();
                     final ResultSet resultSet = indicesStatement.executeQuery(String.format("SHOW INDEX FROM %s", tableName))) {
                    boolean lockKeyIndexExists = false;
                    while (resultSet.next()) {
                        final String columnName = resultSet.getString("Column_name");
                        if ("lockKey".equals(columnName)) {
                            lockKeyIndexExists = true;
                            log.debug("Found correct database table scheme for '{}'", tableName);
                            break;
                        }
                    }
                    // explicit early close to release resources
                    resultSet.close();
                    if (!lockKeyIndexExists) {
                        // Incorrectly the lockKey does not have an index! This is a bug typically manifesting itself with
                        // MySQL 5.6 because we used varchar(255) instead of varchar(190) where 255 is by default too big to be an index
                        log.info("Found incorrect database table scheme for '{}'. Correcting the table now.", tableName);
                        correctTableScheme(tableName, connection, 5);
                    }
                } catch (SQLException e) {
                    final RuntimeException re = new RuntimeException(String.format("Could not validate the %s table or " +
                            "could not correct it (perhaps alter table is not allowed by the application). " +
                            "Cannot start up the repository, correct the table %s manually.", tableName, tableName), e);
                    log.error(re.getMessage());
                    throw re;
                }

            }
        } catch (SQLException e) {
            final RuntimeException re = new RuntimeException("Could not get a connection or could not (check to) create table", e);
            log.error(re.getMessage());
            throw re;
        }
    }

    private void correctTableScheme(final String tableName, final Connection connection, final int retries) throws SQLException {
        Statement correctLockTable = null;
        try {
            correctLockTable = connection.createStatement();
            correctLockTable.addBatch(String.format("TRUNCATE %s", tableName));
            correctLockTable.addBatch(String.format("ALTER TABLE %s CHANGE lockKey lockKey varchar(190)", tableName));
            correctLockTable.addBatch(String.format("ALTER TABLE %s ADD UNIQUE INDEX %s_idx_1 (lockKey)", tableName, tableName));
            correctLockTable.executeBatch();
            correctLockTable.close();
        } catch (SQLException e) {
            if (correctLockTable != null) {
                correctLockTable.close();
                if (retries == 0) {
                    log.error("Tried 5 times to alter table '{}' but does not succeed. Stop the entire cluster and retry to start" +
                            " this cluster node", tableName);
                    throw  e;
                }
                final String lowerCaseMsg = e.getCause().getMessage().toLowerCase();
                if (lowerCaseMsg.contains("duplicate entry") || lowerCaseMsg.contains("data truncated")) {
                    log.info("After truncate but before table alteration entries have been inserted");
                    correctTableScheme(tableName, connection, retries - 1);
                } else {
                    throw e;
                }
            }
        }
    }

}
