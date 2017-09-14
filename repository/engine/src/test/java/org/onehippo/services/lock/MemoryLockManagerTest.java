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

import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.lock.LockException;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MemoryLockManagerTest {

    private MemoryLockManager memoryLockManager;

    @Before
    public void setUp() {
        memoryLockManager = new MemoryLockManager();
    }

    @Test
    public void same_thread_can_lock_same_key_multiple_times() throws Exception {
        memoryLockManager.lock("123");
        memoryLockManager.lock("123");
        assertEquals(1, memoryLockManager.getLocks().size());
        assertEquals("123", memoryLockManager.getLocks().iterator().next().getLockKey());
        assertEquals(Thread.currentThread().getName(), memoryLockManager.getLocks().iterator().next().getLockThread());

        assertEquals(2, ((MemoryLock)memoryLockManager.getLocks().iterator().next()).holdCount);

        memoryLockManager.unlock("123");
        assertEquals(1, memoryLockManager.getLocks().size());
        assertEquals(1, ((MemoryLock)memoryLockManager.getLocks().iterator().next()).holdCount);


        memoryLockManager.unlock("123");
        assertEquals(0, memoryLockManager.getLocks().size());

    }

    @Test
    public void same_thread_can_unlock_() throws Exception {
        memoryLockManager.lock("123");
        memoryLockManager.unlock("123");
        assertEquals(0, memoryLockManager.getLocks().size());
    }

    @Test
    public void other_thread_cannot_unlock_() throws Exception {
        memoryLockManager.lock("123");
        Thread lockThread = new Thread(() -> {
            try {
                memoryLockManager.unlock("123");
            } catch (LockException e) {
                // expected
            }
        });

        lockThread.start();
        lockThread.join();
        assertEquals(1, memoryLockManager.getLocks().size());
    }

    @Test
    public void when_other_thread_contains_lock_a_lock_exception_is_thrown_on_lock_attempt() throws Exception {
        memoryLockManager.lock("123");
        try {
            newSingleThreadExecutor().submit(() -> {
                memoryLockManager.lock("123");
                return true;
            }).get();
            fail("ExecutionException excpected");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof LockException);
        }
    }

    @Test
    public void when_other_thread_contains_lock_it_cannot_be_unlocked_by_other_thread() throws Exception {
        Thread lockThread = new Thread(() -> {
            try {
                memoryLockManager.lock("123");
            } catch (LockException e) {
                e.printStackTrace();
            }
        });

        lockThread.start();
        lockThread.join();

        try {
            memoryLockManager.unlock("123");
            fail("Main thread should not be able to unlock");
        } catch (LockException e) {
            // expected
        }
    }

    @Test
    public void when_thread_containing_lock_has_ended_without_unlocking_the_lock_can_be_reclaimed_by_another_thread() throws Exception {
        Thread lockThread = new Thread(() -> {
            try {
                memoryLockManager.lock("123");
            } catch (LockException e) {
                e.printStackTrace();
            }
        });

        lockThread.start();
        lockThread.join();

        try {
            memoryLockManager.lock("123");
            fail("Other thread should have the lock");
        } catch (LockException e) {
            // expected
        }

        assertEquals("123", memoryLockManager.getLocks().iterator().next().getLockKey());
        assertEquals(lockThread.getName(), memoryLockManager.getLocks().iterator().next().getLockThread());

        // set the lock thread to null and make it eligible for GC ....however since the lock has not been unlocked, we
        // do expect a warning
        lockThread = null;

        long l = System.currentTimeMillis();
        while (tryFor10Seconds(l)) {
            System.gc();
            if (memoryLockManager.getLocks().size() == 0) {
                break;
            }
        }

        assertEquals(0, memoryLockManager.getLocks().size());
        // main thread can lock again
        memoryLockManager.lock("123");
        assertEquals("123", memoryLockManager.getLocks().iterator().next().getLockKey());
        assertEquals(Thread.currentThread().getName(), memoryLockManager.getLocks().iterator().next().getLockThread());
    }

    private boolean tryFor10Seconds(final long l) {
        return System.currentTimeMillis() - l < 10000;
    }
}
