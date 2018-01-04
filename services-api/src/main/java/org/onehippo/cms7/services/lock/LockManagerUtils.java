/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.services.lock;

import java.util.concurrent.TimeoutException;

public class LockManagerUtils {

    /**
     * <p>
     *   Utility method to create and if needed wait indefinitely (unless interrupted) for a {@link LockManager#lock(String)}.
     * </p>
     * <p>
     *   Make sure that after obtaining the cluster-wide lock, that in you are doing JCR node invocations, you first invoke
     *   <code>
     *       <pre>
     *           session.refresh(true|false)
     *       </pre>
     *   </code>
     *   to make sure the latest global JCR cluster changes are retrieved locally.
     * </p>
     *
     * @param lockManager lockManager
     * @param key the key for the {@link Lock} where {@code key} is now allowed to exceed 256 chars
     * @param waitInterval time in milliseconds to wait before retrying creating the lock
     * @return {@link LockResource} such that this {@link ##waitForLock(LockManager, String, long)} method can be used
     *         in a try-with-resources statement where the {@link LockResource#close()} results in the lock being freed.
     * @throws LockException if the lock could not be created (other then {@link AlreadyLockedException})
     * @throws InterruptedException when the thread is interrupted while waiting before retrying to create the lock
     */
    public static LockResource waitForLock(final LockManager lockManager, final String key, final long waitInterval)
            throws LockException, InterruptedException {
        try {
            return waitForLock(lockManager, key, waitInterval, 0);
        } catch (TimeoutException ignore) {
            throw new IllegalStateException("LockManagerUtils implementation error");
        }
    }

    /**
     * <p>
     *    Utility method to create and if needed wait for a maximum amount of time (unless interrupted) for a {@link LockManager#lock(String)} *
     * </p>
     * <p>
     *   Make sure that after obtaining the cluster-wide lock, that in you are doing JCR node invocations, you first invoke
     *   <code>
     *       <pre>
     *           session.refresh(true|false)
     *       </pre>
     *   </code>
     *   to make sure the latest global JCR cluster changes are retrieved locally.
     * </p>
     * @param lockManager lockManager
     * @param key the key for the {@link Lock} where {@code key} is now allowed to exceed 256 chars
     * @param waitInterval time in milliseconds to wait before retrying creating the lock
     * @param maxWait maximum time in milliseconds for trying to create the lock, will throw TimeoutException when exceeded.
     * @return {@link LockResource} such that this {@link ##waitForLock(LockManager, String, long)} method can be used
     *         in a try-with-resources statement where the {@link LockResource#close()} results in the lock being freed.
     * @throws LockException if the lock could not be created (other then {@link AlreadyLockedException})
     * @throws TimeoutException when the maxWait time has exceeded while trying to create the lock
     * @throws InterruptedException when the thread is interrupted while waiting before retrying to create the lock
     */
    public static LockResource waitForLock(final LockManager lockManager, final String key, final long waitInterval, final long maxWait)
            throws LockException, TimeoutException, InterruptedException {
        final long timeoutTime = maxWait > 0 ? System.currentTimeMillis() + maxWait : 0;
        while (true) {
            try {
                return lockManager.lock(key);
            } catch (AlreadyLockedException e) {
                if (timeoutTime > 0 &&
                        (System.currentTimeMillis() > timeoutTime ||
                                (System.currentTimeMillis() + waitInterval) > timeoutTime)) {
                    throw new TimeoutException();
                }
                Thread.sleep(waitInterval);
            }
        }
    }
}
