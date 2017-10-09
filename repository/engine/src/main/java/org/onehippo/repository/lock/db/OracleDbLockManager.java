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

import javax.sql.DataSource;

public class OracleDbLockManager extends DbLockManager {

    private final static String ORACLE_CREATE_LOCK_TABLE_STATEMENT = "CREATE TABLE %s (" +
            "lockKey VARCHAR(256) NOT NULL, " +
            "lockOwner VARCHAR(256), " +
            "lockThread VARCHAR(256)," +
            "status VARCHAR(256) NOT NULL," +
            "lockTime NUMBER(19)," +
            "expirationTime NUMBER(19)," +
            "lastModified NUMBER(19)" +
            ")";

    public OracleDbLockManager(final DataSource dataSource, final String clusterNodeId) {
        super(dataSource, clusterNodeId);
    }

    @Override
    protected String getCreateLockTableStatement() {
        return ORACLE_CREATE_LOCK_TABLE_STATEMENT;
    }
}
