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

        public TimedLockRunnable(final String key, final long lockTime) {
            this.key = key;
            this.lockTime = lockTime;
        }

        @Override
        public void run() {
            try (LockResource lock = lockManager.lock(key)){
                Thread.sleep(lockTime);
            } catch (LockException | InterruptedException ignore) {
                // bad pattern, but OK for this test-case
            }
            System.out.println("lock should be cleared now");
        }
    }

    @Test
    public void waitForLockTest() throws Exception {
        long time = System.currentTimeMillis();
        Thread lockThread = new Thread(new TimedLockRunnable("123", 500));
        lockThread.start();
        // give lockThread time to start
        Thread.sleep(15);
        try {
            lockManager.lock("123");
            fail("Lock should not be available yet");
        } catch (LockException ignore) {
        }
        try (LockResource lock = LockManagerUtils.waitForLock(lockManager, "123", 100)) {
            assertTrue(System.currentTimeMillis() > time + 500);
            System.out.println("time: "+(System.currentTimeMillis() - time));
        }
    }

    @Test
    public void waitForLockTimeoutTest() throws Exception {
        long time = System.currentTimeMillis();
        Thread lockThread = new Thread(new TimedLockRunnable("123", 500));
        lockThread.start();
        // give lockThread time to start
        Thread.sleep(15);
        try {
            lockManager.lock("123");
            fail("Lock should not be available yet");
        } catch (LockException ignore) {
        }
        try (LockResource lock = LockManagerUtils.waitForLock(lockManager, "123", 100, 400)) {
            fail("Lock should not be available yet");
        } catch (TimeoutException ignore) {
            // expected
        }
    }
}
