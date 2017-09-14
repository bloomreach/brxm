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
package org.onehippo.repository.locking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import javax.jcr.Repository;
import javax.sql.DataSource;

import org.apache.jackrabbit.core.util.db.ConnectionHelperDataSourceAccessor;
import org.hippoecm.repository.impl.RepositoryDecorator;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.cms7.services.lock.LockManager;
import org.onehippo.repository.journal.JournalConnectionHelperAccessor;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.services.lock.AbstractLock;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.services.lock.DbLockManager.LOCK_STATEMENT;
import static org.onehippo.services.lock.DbLockManager.TABLE_NAME_LOCK;

public class LockManagerTest extends RepositoryTestCase {

    private LockManager lockManager;
    // dataSource is not null in case of cluster Db test
    private DataSource dataSource;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        lockManager = HippoServiceRegistry.getService(LockManager.class);
        //if (server.getRepository() instanceof  Decora)
        Repository repository = server.getRepository();
        if (repository instanceof RepositoryDecorator) {
            repository = RepositoryDecorator.unwrap(repository);
        }
        if (repository instanceof RepositoryImpl) {
            JournalConnectionHelperAccessor journalConnectionHelperAccessor = ((RepositoryImpl)repository).getJournalConnectionHelperAccessor();
            if (journalConnectionHelperAccessor != null) {
                // running a cluster db test
                dataSource = ConnectionHelperDataSourceAccessor.getDataSource(journalConnectionHelperAccessor.getConnectionHelper());
            }
        }
    }

    @Override
    @After
    public void tearDown() {
        lockManager.destroy();
        assertTrue(lockManager.getLocks().isEmpty());
        // TODO assert db rows empty

    }

    @Test
    public void same_thread_can_lock_same_key_multiple_times() throws Exception {
        final String KEY = "123";
        lockManager.lock(KEY);

        assertDbRowLocked(KEY);

        lockManager.lock(KEY);

        assertDbRowLocked(KEY);

        assertEquals(1, lockManager.getLocks().size());
        assertEquals(KEY, lockManager.getLocks().iterator().next().getLockKey());
        assertEquals(Thread.currentThread().getName(), lockManager.getLocks().iterator().next().getLockThread());

        assertEquals(2, ((AbstractLock)lockManager.getLocks().iterator().next()).getHoldCount());

        lockManager.unlock(KEY);

        assertEquals(1, lockManager.getLocks().size());
        assertEquals(1, ((AbstractLock)lockManager.getLocks().iterator().next()).getHoldCount());

        assertDbRowLocked(KEY);

        lockManager.unlock(KEY);
        assertEquals(0, lockManager.getLocks().size());

        assertDbRowNotLocked(KEY);
        assertDbRowDoesNotExist(KEY);

    }

    private void assertDbRowLocked(final String key) {
        assertDbRowLocked(key, true);
    }

    private void assertDbRowLocked(final String key, final boolean expectedLock) {
        if (dataSource == null) {
            // not a clustered db test
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
             final PreparedStatement preparedLockStatement = connection.prepareStatement(LOCK_STATEMENT);
             preparedLockStatement.setString(1, key);
             preparedLockStatement.setQueryTimeout(10);
             preparedLockStatement.executeQuery();
             if (expectedLock) {
                 fail(String.format("Database row for key '%s% should be locked", key));
             }
        } catch (SQLException e) {
            if (!expectedLock) {
                fail(String.format("Database row for key '%s% should not be locked", key));
            }
        }
    }


    private void assertDbRowNotLocked(final String key) {
        assertDbRowLocked(key, false);
    }


    private void assertDbRowDoesNotExist(final String key) {
        if (dataSource == null) {
            // not a clustered db test
            return;
        }
        final String selectStatement = "SELECT * FROM " + TABLE_NAME_LOCK + " WHERE lockKey=?";
        try (Connection connection = dataSource.getConnection()) {
            final PreparedStatement preparedSelectStatement = connection.prepareStatement(selectStatement);
            preparedSelectStatement.setString(1, key);
            preparedSelectStatement.setQueryTimeout(10);
            ResultSet resultSet = preparedSelectStatement.executeQuery();
            assertFalse(String.format("There should be no database row for ", key),resultSet.next());
        } catch (SQLException e) {
             fail(String.format("Database row for key '%s% should not be locked", key));
        }
    }

    @Test
    public void same_thread_can_unlock_() throws Exception {
        lockManager.lock("123");
        assertDbRowLocked("123");
        lockManager.unlock("123");
        assertDbRowNotLocked("123");
        assertDbRowDoesNotExist("123");
        assertEquals(0, lockManager.getLocks().size());
    }

    @Test
    public void other_thread_cannot_unlock_() throws Exception {
        lockManager.lock("123");
        assertDbRowLocked("123");
        Thread lockThread = new Thread(() -> {
            try {
                lockManager.unlock("123");
            } catch (LockException e) {
                // expected
                assertDbRowLocked("123");
            }
        });

        lockThread.start();
        lockThread.join();
        assertEquals(1, lockManager.getLocks().size());
    }

    @Test
    public void when_other_thread_contains_lock_a_lock_exception_is_thrown_on_lock_attempt() throws Exception {
        lockManager.lock("123");
        assertDbRowLocked("123");
        try {
            newSingleThreadExecutor().submit(() -> {
                lockManager.lock("123");
                return true;
            }).get();
            fail("ExecutionException excpected");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof LockException);
            assertDbRowLocked("123");
        }
    }

    @Test
    public void when_other_thread_contains_lock_it_cannot_be_unlocked_by_other_thread() throws Exception {
        Thread lockThread = new Thread(() -> {
            try {
                lockManager.lock("123");
            } catch (LockException e) {
                fail(e.toString());
            }
        });

        lockThread.start();
        lockThread.join();

        try {
            lockManager.unlock("123");
            assertDbRowLocked("123");
            fail("Main thread should not be able to unlock");
        } catch (LockException e) {
            // expected
        }
    }

    @Test
    public void when_thread_containing_lock_has_ended_without_unlocking_the_lock_can_be_reclaimed_by_another_thread() throws Exception {
        Thread lockThread = new Thread(() -> {
            try {
                lockManager.lock("123");
            } catch (LockException e) {
                e.printStackTrace();
            }
        });

        lockThread.start();
        lockThread.join();

        try {
            lockManager.lock("123");
            fail("Other thread should have the lock");
        } catch (LockException e) {
            assertDbRowLocked("123");
        }

        assertEquals("123", lockManager.getLocks().iterator().next().getLockKey());
        assertEquals(lockThread.getName(), lockManager.getLocks().iterator().next().getLockThread());

        // set the lock thread to null and make it eligible for GC ....however since the lock has not been unlocked, we
        // do expect a warning
        lockThread = null;

        long l = System.currentTimeMillis();
        while (tryFor10Seconds(l)) {
            System.gc();
            if (lockManager.getLocks().size() == 0) {
                break;
            }
        }

        assertEquals(0, lockManager.getLocks().size());
        assertDbRowNotLocked("123");
        // main thread can lock again
        lockManager.lock("123");
        assertEquals("123", lockManager.getLocks().iterator().next().getLockKey());
        assertEquals(Thread.currentThread().getName(), lockManager.getLocks().iterator().next().getLockThread());
    }

    private boolean tryFor10Seconds(final long l) {
        return System.currentTimeMillis() - l < 10000;
    }

    @Test
    public void assert_database_lock_manager_destroy_only_removes_rows_for_which_it_is_the_owner() throws Exception {

    }
}
