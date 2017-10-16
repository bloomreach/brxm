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
import java.sql.SQLException;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;

import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.apache.jackrabbit.core.util.db.ConnectionHelperDataSourceAccessor;
import org.apache.jackrabbit.core.util.db.OracleConnectionHelper;

public class DbLockManagerFactory {

    public static DbLockManager create(final ConnectionHelper connectionHelper, final String schemaObjectPrefix,
                        final boolean schemaCheckEnabled, final String clusterNodeId) throws RepositoryException {
        return create(ConnectionHelperDataSourceAccessor.getDataSource(connectionHelper), connectionHelper,
                schemaObjectPrefix, schemaCheckEnabled, clusterNodeId);
    }

    public static DbLockManager create(final DataSource dataSource, final String schemaObjectPrefix,
                        final boolean schemaCheckEnabled, final String clusterNodeId) throws RepositoryException {
        return create(dataSource, null, schemaObjectPrefix, schemaCheckEnabled, clusterNodeId);
    }

    /**
     * Creates the {@link DbLockManager} which can be used for general purpose locking *not* using JCR at all
     * @throws RuntimeException if the lock manager cannot be created, resulting the repository startup to short-circuit
     * @throws RepositoryException if a repository exception happened while creating the lock manager
     */
    private static DbLockManager create(final DataSource dataSource, ConnectionHelper connectionHelper,
                         String schemaObjectPrefix, final boolean schemaCheckEnabled, String clusterNodeId)
            throws RuntimeException, RepositoryException {
        String dbProductName;
        try {
            try (Connection connection = dataSource.getConnection()) {
                dbProductName = connection.getMetaData().getDatabaseProductName();
            }
            if (connectionHelper == null) {
                if ("Oracle".equals(dbProductName)) {
                    connectionHelper = new OracleConnectionHelper(dataSource, false);
                } else {
                    connectionHelper = new ConnectionHelper(dataSource, false);
                }
            }
            schemaObjectPrefix = connectionHelper.prepareDbIdentifier(schemaObjectPrefix == null ? "" : schemaObjectPrefix.trim());
        } catch (SQLException e) {
            throw new RepositoryException("Failed to retrieve SQL Lock Manager database metadata", e);
        }
        clusterNodeId = clusterNodeId == null ? "default" : clusterNodeId;

        switch (dbProductName) {
            case "MySQL":
            case "PostgreSQL":
            case "H2":
                return new DbLockManager(connectionHelper, dataSource, schemaObjectPrefix, schemaCheckEnabled, clusterNodeId);
            case "Oracle":
                return new OracleDbLockManager(connectionHelper, dataSource, schemaObjectPrefix, schemaCheckEnabled, clusterNodeId);
            default:
                throw new RepositoryException("Unsupported Database engine. Product name: " + dbProductName);
        }
    }


}
