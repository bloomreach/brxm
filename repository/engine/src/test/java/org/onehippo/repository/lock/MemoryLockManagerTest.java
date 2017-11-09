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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.cms7.services.lock.LockManagerException;
import org.onehippo.cms7.services.lock.LockResource;
import org.onehippo.repository.lock.memory.MemoryLockManager;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

        assertEquals(2, ((MutableLock)memoryLockManager.getLocks().iterator().next()).getHoldCount());

        memoryLockManager.unlock("123");
        assertEquals(1, memoryLockManager.getLocks().size());
        assertEquals(1, ((MutableLock)memoryLockManager.getLocks().iterator().next()).getHoldCount());


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
    public void other_thread_cannot_unlock_without_lock_resource() throws Exception {
        memoryLockManager.lock("123");
        final ExecutorService executorService = newSingleThreadExecutor();
        final Future<Stream<String>> future = newSingleThreadExecutor().submit(() -> {
            // other thread should not successfully unlock and we expect an error to be logged
            try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(MemoryLockManager.class).build()) {
                memoryLockManager.unlock("123");
                return interceptor.messages();
            }
        });

        try {
            assertTrue(memoryLockManager.isLocked("123"));
        } catch (LockManagerException e) {
            fail("#isLocked check failed");
        }

        final Stream<String> messages = future.get();
        assertTrue(messages.anyMatch(m -> m.contains("should never had invoked #unlock(123)")));
        assertTrue(memoryLockManager.isLocked("123"));

        assertEquals(1, memoryLockManager.getLocks().size());
        memoryLockManager.unlock("123");
        executorService.shutdown();
    }

    @Test
    public void other_thread_can_unlock_with_lock_resource() throws Exception {

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ExecutorService executorService = newSingleThreadExecutor();
        final Future<LockResource> futureLock = newSingleThreadExecutor().submit(() -> {
            try {
                return memoryLockManager.lock("123");
            } finally {
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();

        try {
            assertTrue(memoryLockManager.isLocked("123"));
        } catch (LockManagerException e) {
            fail("#isLocked check failed");
        }

        final LockResource lockResource = futureLock.get();
        assertFalse(Thread.currentThread() == lockResource.getHolder());
        assertTrue(lockResource.getHolder().isAlive());
        lockResource.close();
        assertFalse("Lock should have been released by main thread via the LockResource", memoryLockManager.isLocked("123"));
        executorService.shutdown();
    }

    @Test
    public void when_other_thread_contains_lock_a_lock_exception_is_thrown_on_lock_attempt() throws Exception {
        memoryLockManager.lock("123");
        final ExecutorService executorService = newSingleThreadExecutor();
        try {
            executorService.submit(() -> {
                memoryLockManager.lock("123");
                return true;
            }).get();
            fail("ExecutionException excpected");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof LockException);
        }
        executorService.shutdown();
    }

    @Test
    public void when_other_thread_contains_lock_it_cannot_be_unlocked_by_other_thread() throws Exception {

        final ExecutorService executorService = newSingleThreadExecutor();
        final CountDownLatch countDownLatch1 = new CountDownLatch(1);
        final CountDownLatch countDownLatch2 = new CountDownLatch(1);

        final Future<LockResource> futureLock = executorService.submit(() -> {
            final LockResource lock = memoryLockManager.lock("123");
            countDownLatch1.countDown();
            countDownLatch2.await();
            return lock;
        });

        countDownLatch1.await();

        // other thread should not successfully unlock and we expect an error to be logged
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(MemoryLockManager.class).build()) {
            memoryLockManager.unlock("123");
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Thread 'main' should never had invoked #unlock(123)")));
        }

        // trying again should result in same error
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(MemoryLockManager.class).build()) {
            memoryLockManager.unlock("123");
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Thread 'main' should never had invoked #unlock(123)")));
        }

        // "123" should still be locked
        assertTrue(memoryLockManager.isLocked("123"));
        countDownLatch2.countDown();
        try {
            final LockResource lockResource = futureLock.get();
            lockResource.close();
        } catch (ExecutionException e){
            fail(e.toString());
        }

        executorService.shutdown();

        // "123" should be freed by lockResource.close();
        assertFalse(memoryLockManager.isLocked("123"));

    }

    @Test
    public void when_thread_containing_lock_has_ended_without_unlocking_the_lock_can_be_reclaimed_by_another_thread() throws Exception {

        final ExecutorService executorService = newSingleThreadExecutor();
        final CountDownLatch countDownLatch1 = new CountDownLatch(1);
        final CountDownLatch countDownLatch2 = new CountDownLatch(1);

        final Future<LockResource> futureLock = executorService.submit(() -> {
            final LockResource lock = memoryLockManager.lock("123");
            countDownLatch1.countDown();
            countDownLatch2.await();
            return lock;
        });

        countDownLatch1.await();

        try {
            memoryLockManager.lock("123");
            fail("Other thread should have the lock");
        } catch (LockException e) {
            // expected
        }

        countDownLatch2.countDown();

        try {
            final LockResource lockResource = futureLock.get();
            assertEquals("123", memoryLockManager.getLocks().iterator().next().getLockKey());
            assertEquals(lockResource.getHolder().getName(), memoryLockManager.getLocks().iterator().next().getLockThread());

        } catch (ExecutionException e){
            fail(e.toString());
        }

        executorService.shutdown();
        // The lockThread did not unlock but since the thread is not live any more, the lock
        // should again be eligible for other threads

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(MemoryLockManager.class).build()) {
            assertEquals(0, memoryLockManager.getLocks().size());
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Thread that created the lock already stopped")));
        }

        // main thread can lock again
        memoryLockManager.lock("123");
        assertEquals("123", memoryLockManager.getLocks().iterator().next().getLockKey());
        assertEquals(Thread.currentThread().getName(), memoryLockManager.getLocks().iterator().next().getLockThread());
    }

}
