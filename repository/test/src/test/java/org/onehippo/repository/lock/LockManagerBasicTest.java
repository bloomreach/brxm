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
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.onehippo.cms7.services.lock.Lock;
import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.cms7.services.lock.LockManagerException;
import org.onehippo.cms7.services.lock.LockResource;
import org.onehippo.repository.lock.db.DbLockManager;
import org.onehippo.repository.lock.memory.MemoryLockManager;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LockManagerBasicTest extends AbstractLockManagerTest {


    @Test
    public void general_single_threaded_lock_interaction() throws Exception {
        final String key = "123";
        lockManager.lock(key);

        // for 'node1', see h2-repository.xml or mysql-repository.xml the <cluster> node id
        dbRowAssertion(key, "RUNNING", "node1", Thread.currentThread().getName());

        lockManager.lock(key);

        dbRowAssertion(key, "RUNNING");

        assertEquals(1, lockManager.getLocks().size());
        assertEquals(key, lockManager.getLocks().iterator().next().getLockKey());
        assertEquals(Thread.currentThread().getName(), lockManager.getLocks().iterator().next().getLockThread());

        lockManager.unlock(key);

        assertEquals(1, lockManager.getLocks().size());

        dbRowAssertion(key, "RUNNING");

        lockManager.unlock(key);
        assertEquals(0, lockManager.getLocks().size());
        
        assertDbRowDoesExist(key);
        dbRowAssertion(key, "FREE");

        // now we should be able to lock again
        lockManager.lock(key);
        dbRowAssertion(key, "RUNNING");
        lockManager.unlock(key);
    }

    @Test
    public void lock_try_with_resource_construct() throws Exception {
        String key = "123";
        try (LockResource lock = lockManager.lock(key)) {
            dbRowAssertion(key, "RUNNING");
        }
        assertDbRowDoesExist(key);
        dbRowAssertion(key, "FREE");
    }

    @Test
    public void lock_nested_try_with_resource_construct() throws Exception {
        String key = "123";
        try (LockResource lock = lockManager.lock(key)) {
            try (LockResource lock2 = lockManager.lock(key)) {
                dbRowAssertion(key, "RUNNING");
                assertFalse(lock == lock2);
                assertTrue(lock.getLock() == lock2.getLock());
                assertTrue(lock.getHolder() == lock2.getHolder());
            }
            dbRowAssertion(key, "RUNNING");
        }
        assertDbRowDoesExist(key);
        dbRowAssertion(key, "FREE");
    }

    @Test
    public void lock_nested_try_on_unavailable_lock() throws Exception {
        String key = "123";
        final LockRunnable runnable = new LockRunnable(key, true);
        final Thread lockThread = new Thread(runnable);

        lockThread.start();
        // give lockThread time to lock
        Thread.sleep(100);

        try (LockResource lock = lockManager.lock(key)) {
            fail("should not be able to lock");
        } catch (LockException e) {
            // expected
        }
        assertDbRowDoesExist(key);
        dbRowAssertion(key, "RUNNING");

        runnable.keepAlive = false;

        // after the thread is finished, the lock manager should have no locks any more
        lockThread.join();

        if (runnable.e != null) {
            fail(runnable.e.toString());
        }
    }

    private void assertDbRowDoesExist(final String key) throws SQLException {
        if (dbLockManager == null) {
            // not a clustered db test
            return;
        }
        final String selectStatement = "SELECT * FROM " + dbLockManager.getTableName() + " WHERE lockKey=?";
        try (Connection connection = dbLockManager.getConnection()) {
            final PreparedStatement preparedSelectStatement = connection.prepareStatement(selectStatement);
            preparedSelectStatement.setString(1, key);
            preparedSelectStatement.setQueryTimeout(10);
            ResultSet resultSet = preparedSelectStatement.executeQuery();
            assertTrue(String.format("There should be a database row for ", key), resultSet.next());
        }
    }

    @Test
    public void same_thread_can_unlock_() throws Exception {
        final String key = "123";
        lockManager.lock(key);
        dbRowAssertion(key, "RUNNING");
        lockManager.unlock(key);
        dbRowAssertion(key, "FREE");
        assertDbRowDoesExist(key);
        assertEquals(0, lockManager.getLocks().size());
    }

    @Test
    public void unlock_non_existing_lock() throws Exception {
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(MemoryLockManager.class, DbLockManager.class).build()) {
            lockManager.unlock("123");
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Lock '123' does not exist or this cluster node does not contain the lock")));
        }
    }

    @Test
    public void other_thread_cannot_unlock_() throws Exception {
        final String key = "123";
        lockManager.lock(key);
        dbRowAssertion(key, "RUNNING");
        Thread lockThread = new Thread(() -> {
            // unlock should fail with error logging because not owned
            try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(MemoryLockManager.class, DbLockManager.class).build()) {
                lockManager.unlock(key);
                assertTrue(interceptor.messages().anyMatch(m -> m.contains("Thread '"+Thread.currentThread().getName()+"' should never had invoked #unlock(123)")));
            }

            // second time again
            try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(MemoryLockManager.class, DbLockManager.class).build()) {
                lockManager.unlock(key);
                assertTrue(interceptor.messages().anyMatch(m -> m.contains("Thread '"+Thread.currentThread().getName()+"' should never had invoked #unlock(123)")));
            }

            try {
                assertTrue(lockManager.isLocked(key));
            } catch (LockManagerException e) {
                fail("#isLocked should not fail : " +  e.toString());
            }
            try {
                dbRowAssertion(key, "RUNNING");
            } catch (SQLException e1) {
                fail(e1.toString());
            }
        });

        lockThread.start();
        lockThread.join();
        assertEquals(1, lockManager.getLocks().size());
        dbRowAssertion(key, "RUNNING");
        lockManager.unlock(key);
        dbRowAssertion(key, "FREE");
    }

    @Test
    public void when_other_thread_contains_lock_a_lock_exception_is_thrown_on_lock_attempt() throws Exception {
        final String key = "123";
        lockManager.lock(key);
        dbRowAssertion(key, "RUNNING");
        try {
            newSingleThreadExecutor().submit(() -> {
                lockManager.lock(key);
                return true;
            }).get();
            fail("ExecutionException excpected");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof LockException);
            dbRowAssertion(key, "RUNNING");
            lockManager.unlock(key);
            dbRowAssertion(key, "FREE");
        }
    }

    protected class LockRunnable implements Runnable {

        private String key;
        private volatile boolean keepAlive;
        private Exception e;

        LockRunnable(final String key , final boolean keepAlive) {
            this.key = key;
            this.keepAlive = keepAlive;
        }

        @Override
        public void run() {
            try (LockResource lock = lockManager.lock(key)){
                while (keepAlive) {
                    Thread.sleep(25);
                }
            } catch (LockException | InterruptedException e) {
                this.e = e;
            }
        }
    }

    @Test
    public void when_other_thread_contains_lock_it_cannot_be_unlocked_by_other_thread() throws Exception {
        final String key = "123";
        final LockRunnable runnable = new LockRunnable(key, true);
        final Thread lockThread = new Thread(runnable);

        lockThread.start();
        // give lockThread time to lock
        Thread.sleep(100);

        dbRowAssertion(key, "RUNNING", CLUSTER_NODE_ID, lockThread.getName());

        // main thread should not be able to unlock, error log expected
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(MemoryLockManager.class, DbLockManager.class).build()) {
            lockManager.unlock("123");
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Thread 'main' should never had invoked #unlock(123)")));
        }

        // trying again should again result in error log
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(MemoryLockManager.class, DbLockManager.class).build()) {
            lockManager.unlock("123");
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Thread 'main' should never had invoked #unlock(123)")));
        }

        assertTrue(lockManager.isLocked(key));
        dbRowAssertion(key, "RUNNING");
        // expected

        runnable.keepAlive = false;

        // after the thread is finished, the lock manager should have no locks any more
        lockThread.join();

        if (runnable.e != null) {
            fail(runnable.e.toString());
        }

    }

    @Test
    public void when_thread_containing_lock_has_ended_without_unlocking_the_lock_can_be_reclaimed_by_another_thread() throws Exception {
        final String key = "123";
        final LockRunnable runnable = new LockRunnable(key, true);
        final Thread lockThread = new Thread(runnable);

        lockThread.start();
        // give lockThread time to lock
        Thread.sleep(100);

        try {
            lockManager.lock(key);
            fail("Other thread should have the lock");
        } catch (LockException e) {
            dbRowAssertion(key, "RUNNING");
        }

        assertEquals(key, lockManager.getLocks().iterator().next().getLockKey());
        assertEquals(lockThread.getName(), lockManager.getLocks().iterator().next().getLockThread());

        runnable.keepAlive = false;
        lockThread.join();

        if (runnable.e != null) {
            fail(runnable.e.toString());
        }

        assertEquals(0, lockManager.getLocks().size());
        dbRowAssertion(key, "FREE");
        // main thread can lock again
        lockManager.lock(key);
        assertEquals(key, lockManager.getLocks().iterator().next().getLockKey());
        assertEquals(Thread.currentThread().getName(), lockManager.getLocks().iterator().next().getLockThread());
        lockManager.unlock(key);
    }


    @Test
    public void assert_clear_database_lock_manager_only_frees_locked_rows_for_which_it_is_the_owner() throws Exception {
        lockManager.lock("a");
        lockManager.lock("b");
        // insert manually non-owned rows

        addManualLockToDatabase("c", "otherNode", "otherThreadName");
        addManualLockToDatabase("d", "otherNode", "otherThreadName");

        dbRowAssertion("a", "RUNNING", "node1", Thread.currentThread().getName());
        dbRowAssertion("b", "RUNNING");
        dbRowAssertion("c", "RUNNING");
        dbRowAssertion("d", "RUNNING");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(MemoryLockManager.class, DbLockManager.class).build()) {
            lockManager.clear();
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Lock 'a' owned by cluster '"+getClusterNodeId(session)+
                    "' and thread '"+Thread.currentThread().getName()+"' was never")));
        }

        dbRowAssertion("a", "FREE");
        dbRowAssertion("b", "FREE");
        dbRowAssertion("c", "RUNNING");
        dbRowAssertion("d", "RUNNING");

        if (dbLockManager != null) {
            // rows are kept by clear (and destroy) but fields are made empty
            try (Connection connection = dbLockManager.getConnection()) {
                final PreparedStatement selectStatement = connection.prepareStatement(dbLockManager.getSelectStatement());
                selectStatement.setString(1, "a");
                ResultSet resultSet = selectStatement.executeQuery();
                if (resultSet.next()) {
                    assertEquals("FREE", resultSet.getString("status"));
                    assertNull(resultSet.getString("lockOwner"));
                    assertNull(resultSet.getString("lockThread"));
                    assertEquals(0L, resultSet.getLong("lockTime"));
                    assertEquals(0L, resultSet.getLong("expirationTime"));
                } else {
                    fail(String.format("A row with lockKey '%s' should exist", "a"));
                }
            }
        }
    }

    @Test
    public void get_locks_returns_also_locks_owned_by_other_cluster_node_in_case_of_clustered_setup() throws Exception {
        lockManager.lock("a");
        lockManager.lock("b");
        // insert manually non-owned rows

        addManualLockToDatabase("c", "otherNode", "otherThreadName");
        addManualLockToDatabase("d", "otherNode", "otherThreadName");

        List<Lock> locks = lockManager.getLocks();
        if (dbLockManager == null) {
            // in memory manager
            assertEquals(2, locks.size());
        } else {
            dbRowAssertion("a", "RUNNING", "node1", Thread.currentThread().getName());
            dbRowAssertion("c", "RUNNING", "otherNode", "otherThreadName");

            assertEquals(4, locks.size());
        }
        lockManager.unlock("a");
        lockManager.unlock("b");
    }

    @Test
    public void is_lock_also_checks_locks_owned_by_other_cluster_nodes_in_case_of_clustered_setup() throws Exception {
        lockManager.lock("a");
        lockManager.lock("b");
        // insert manually non-owned rows

        assertTrue(lockManager.isLocked("a"));
        assertTrue(lockManager.isLocked("b"));
        if (dbLockManager != null) {
            addManualLockToDatabase("c", "otherNode", "otherThreadName");
            addManualLockToDatabase("d", "otherNode", "otherThreadName");
            assertTrue(lockManager.isLocked("c"));
            assertTrue(lockManager.isLocked("d"));
        }
        lockManager.unlock("a");
        lockManager.unlock("b");
    }
}
