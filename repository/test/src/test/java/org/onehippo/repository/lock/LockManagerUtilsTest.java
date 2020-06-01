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

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.cms7.services.lock.LockManagerUtils;
import org.onehippo.cms7.services.lock.LockResource;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LockManagerUtilsTest extends AbstractLockManagerTest {

    protected class TimedLockRunnable implements Runnable {

        private final String key;
        private final long lockTime;
        private AtomicBoolean started = new AtomicBoolean(false);

        public TimedLockRunnable(final String key, final long lockTime) {
            this.key = key;
            this.lockTime = lockTime;
        }

        @Override
        public void run() {
            try (LockResource lock = lockManager.lock(key)){
                started.set(true);
                Thread.sleep(lockTime);
            } catch (LockException | InterruptedException ignore) {
                // bad pattern, but OK for this test-case
            }
        }

        void waitForLock(final long waitInterval) {
            while (!started.get()) {
                try {
                    Thread.sleep(waitInterval);
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

    @Test
    public void waitForLockTest() throws Exception {
        long time = System.currentTimeMillis();
        TimedLockRunnable runnable = new TimedLockRunnable("123", 500);
        Thread lockThread = new Thread(runnable);
        lockThread.start();
        runnable.waitForLock(10);
        try {
            lockManager.lock("123");
            // unexpected, but release the lock to prevent (other) warning
            lockManager.unlock("123");
            fail("Lock should not be available yet");
        } catch (LockException ignore) {
            // expected
        }
        try (LockResource ignore = LockManagerUtils.waitForLock(lockManager, "123", 100)) {
            assertTrue(System.currentTimeMillis() > time + 500);
        }
        lockThread.join();
    }

    @Test
    public void waitForLockTimeoutTest() throws Exception {
        TimedLockRunnable runnable = new TimedLockRunnable("123", 500);
        Thread lockThread = new Thread(runnable);
        lockThread.start();
        runnable.waitForLock(10);
        try {
            lockManager.lock("123");
            // unexpected, but release the lock to prevent (other) warning
            lockManager.unlock("123");
            fail("Lock should not be available yet");
        } catch (LockException ignore) {
        }
        try (LockResource ignore = LockManagerUtils.waitForLock(lockManager, "123", 100, 400)) {
            fail("Lock should not be available yet");
        } catch (TimeoutException ignore) {
            // expected
        }
        lockThread.join();
    }
}
