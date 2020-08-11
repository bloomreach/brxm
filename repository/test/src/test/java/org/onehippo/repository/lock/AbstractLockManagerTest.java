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
package org.onehippo.repository.lock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.jcr.Session;

import org.junit.After;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.cms7.services.lock.LockManager;
import org.onehippo.cms7.services.lock.LockManagerException;
import org.onehippo.repository.lock.db.DbLockManager;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.onehippo.repository.lock.AbstractLockManager.REFRESH_RATE_SECONDS;

public abstract class AbstractLockManagerTest extends RepositoryTestCase {

    protected final String CLUSTER_NODE_ID = "node1";

    protected InternalLockManager lockManager;
    protected DbLockManager dbLockManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        lockManager = (InternalLockManager)HippoServiceRegistry.getService(LockManager.class);
        if (lockManager instanceof DbLockManager) {
            dbLockManager = (DbLockManager)lockManager;
        }
    }

    @Override
    @After
    public void tearDown() throws Exception {
        tearDown(false);
    }

    @Override
    public void tearDown(boolean clearRepository) throws Exception {
        lockManager.clear();
        // DELETE ALL ROWS if there are any present
        if (dbLockManager != null) {
            try (Connection connection = dbLockManager.getConnection() ){
                final PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM "+dbLockManager.getTableName());
                deleteStatement.execute();

            } catch (SQLException e) {
                fail("Failed to delete rows : " + e.toString());
            }
        }
        super.tearDown(clearRepository);
    }

    protected void dbRowAssertion(final String key, final String expectedStatus) throws SQLException {
        dbRowAssertion(key, expectedStatus, null, null);
    }

    protected void dbRowAssertion(final String key, final String expectedStatus, final String lockOwnerExpectation, final String lockThreadExpectation) throws SQLException {
        if (dbLockManager == null) {
            // not a clustered db test
            return;
        }

        try (Connection connection = dbLockManager.getConnection()) {
            final PreparedStatement selectStatement = connection.prepareStatement(dbLockManager.getSelectStatement());
            selectStatement.setString(1, key);
            ResultSet resultSet = selectStatement.executeQuery();
            if (resultSet.next()) {
                String status = resultSet.getString("status");
                assertEquals(String.format("Unexpected status '%s' found", status), expectedStatus, status);
                if (lockOwnerExpectation != null) {
                    String lockOwner = resultSet.getString("lockOwner");
                    assertEquals(String.format("Unexpected lockOwner '%s' found", lockOwner), lockOwnerExpectation, lockOwner);
                }
                if (lockThreadExpectation != null) {
                    String lockThread = resultSet.getString("lockThread");
                    assertEquals(String.format("Unexpected lockThread '%s' found", lockThread), lockThreadExpectation, lockThread);
                }
            } else {
                fail(String.format("A row with lockKey '%s' should exist", key));
            }
        }
    }

    protected void assertKeyMissing(final String key) throws SQLException {
        if (dbLockManager == null) {
            // not a clustered db test
            return;
        }

        try (Connection connection = dbLockManager.getConnection()) {
            final PreparedStatement selectStatement = connection.prepareStatement(dbLockManager.getSelectStatement());
            selectStatement.setString(1, key);
            ResultSet resultSet = selectStatement.executeQuery();
            if (resultSet.next()) {
                fail(String.format("Key '%s' not expected to be present in database", key));
            }
        }
    }

    protected void addManualLockToDatabase(final String key, final String clusterNodeId,
                                           final String threadName) throws LockException {
        addManualLockToDatabase(key, clusterNodeId, threadName, 0L, 0L,  0L);
    }

    protected void addManualLockToDatabase(final String key, final String clusterNodeId,
                                           final String threadName, long lockTime, long expirationTime, long lastModified) throws LockException {
        if (dbLockManager != null) {
            try (Connection connection = dbLockManager.getConnection()) {

                final PreparedStatement createStatement = connection.prepareStatement(dbLockManager.getSelectStatement());
                createStatement.setString(1, key);
                createStatement.setString(2, clusterNodeId);
                createStatement.setString(3, threadName);
                lockTime = (lockTime == 0L) ? System.currentTimeMillis() : lockTime;
                createStatement.setLong(4, lockTime);
                expirationTime = (expirationTime ==0) ? lockTime + REFRESH_RATE_SECONDS * 1000 : expirationTime;
                createStatement.setLong(5, expirationTime);
                lastModified = (lastModified == 0L) ? lockTime : lastModified;
                createStatement.setLong(6, lastModified);
                try {
                    createStatement.execute();
                } catch (SQLException e) {
                    throw new LockManagerException(String.format("Cannot create lock row for '{}'", key), e);
                }
            } catch (SQLException e) {
                fail("Failed to delete rows : " + e.toString());
            }
        }
    }


    protected void insertDataRowLock(final String key, final String clusterId, final String threadName,
                                   final long expirationTime) throws SQLException {
        final long lockTime = System.currentTimeMillis();
        try (Connection connection = dbLockManager.getConnection()){
            final PreparedStatement createStatement = connection.prepareStatement(dbLockManager.getSelectStatement());
            createStatement.setString(1, key);
            createStatement.setString(2, clusterId);
            createStatement.setString(3, threadName);
            createStatement.setLong(4, lockTime);
            createStatement.setLong(5, expirationTime);
            createStatement.setLong(6, lockTime);
            createStatement.execute();
        }
    }

    protected String getClusterNodeId(final Session session) {
        String clusterNodeId = session.getRepository().getDescriptor("jackrabbit.cluster.id");
        if (clusterNodeId == null) {
            clusterNodeId = "default";
        }
        return clusterNodeId;
    }
}
