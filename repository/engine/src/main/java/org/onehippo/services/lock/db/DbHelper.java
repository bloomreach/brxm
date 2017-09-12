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
package org.onehippo.services.lock.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbHelper {

    private static final Logger log = LoggerFactory.getLogger(DbHelper.class);

    /**
     * Creates the table {@code tableName} and throws a {@link RuntimeException} if it does not succeed in it. Note that
     * if in the meantime another cluster node has created the table, this method does not throw an exception but just
     * returns.
     * @param dataSource
     * @param tableName
     */
    public static void createTableIfNeeded(final DataSource dataSource, final String createTableStatement, final String tableName) throws RuntimeException {
        try {
            try (Connection connection = dataSource.getConnection()) {
                final boolean tableExists = tableExists(connection, tableName);
                if (!tableExists) {
                    log.info("Creating table {} ", tableName);
                    try (Statement statement = connection.createStatement()) {
                        statement.addBatch(String.format(createTableStatement, tableName));
                        statement.addBatch("CREATE UNIQUE INDEX " + tableName + "_idx_1 on " + tableName + "(lockkey)");
                        statement.setQueryTimeout(10);
                        statement.executeBatch();
                    } catch (SQLException e) {
                        if (tableExists(connection, tableName)) {
                            log.debug("Table {} already created by another cluster node", tableName);
                        } else {
                            log.error("Failed to create table {}: {}", tableName, e.getMessage());
                            throw e;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Could not get a connection or could not create table");
            throw new RuntimeException("Could not get a connection not create table", e);
        }
    }

    public static boolean tableExists(final Connection connection, final String tableName) throws SQLException {
        final ResultSet resultSet = connection.getMetaData().getTables(null,
                null,
                connection.getMetaData().storesUpperCaseIdentifiers() ? tableName.toUpperCase() : tableName, null);
        return resultSet.next();
    }

}
