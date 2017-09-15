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

import java.util.List;

import org.onehippo.cms7.services.SingletonService;

@SingletonService
public interface LockManager {

    /**
     * <p>
     *     Tries to create a {@link Lock} for {@code key}. The {@code key} is not allowed to exceed 256 chars. If there
     *     is already a {@code Lock} for {@code key} then in case the current {@link Thread} has the lock, void is
     *     returned, otherwise a {@link LockException} is thrown.
     * </p>
     * <p>
     *     Invoking this method multiple times with the same {@code key} and the same thread results in the hold count
     *     being incremented.
     * </p>
     * <p>
     *     This {@link #lock(String)} method is the same as invoking {@link #lock(String, int) lock(key, 60)}. The
     *     {@code refreshRateSeconds} only has meaning in a clustered setup: if the cluster wide lock
     *     is not being updated by the {@link LockManager} for more than 60 seconds, the lock will be released. This is
     *     a safeguard against a lock being kept by a cluster node that died / has been stopped
     * </p>
     * <p>
     *      A lock is released when {@link #unlock(String)} is invoked as many times as
     *     {@link #lock(String)}. Alternatively, when the {@link LockManager} implementation detects that the Thread
     *     that held the lock is not live any more, the {@link LockManager} implementation can also release the lock.
     *     Lastly, a lock will be released when it has not been refreshed for 60 seconds, see {@link #lock(String, int)}.
     *     This last case is useful in clustered setups where a cluster node has an ungraceful shutdown.
     * </p>
     * @param key the key for the {@link Lock} where {@code key} is now allowed to exceed 256 chars
     * @throws LockException in case there is already a {@link Lock} for {@code key} or the lock could not be created
     * @throws IllegalArgumentException if the {@code key} exceeds 256 chars
     * @see #lock(String, int)
     */
    void lock(String key) throws LockException;

    /**
     * <p>
     *     Same as {@link #lock(String)} only with explicit value for {@code refreshRateSeconds}. The larger {@code refreshRateSeconds}
     *     is set, the less frequently the {@link LockManager} has to refresh this lock. Thus for example for master election long
     *     running processes where it is not important if there is for example no master for, say, 10 minutes, you can set
     *     {@code refreshRateSeconds} to 10 minutes.
     * </p>
     * @param key key the key for the {@link Lock} where {@code key} is now allowed to exceed 256 chars
     * @param refreshRateSeconds If the {@link LockManager} does not refresh the lock for more than {@code refreshRateSeconds}
     *                          the lock gets released. The minimal value for {@code refreshRateSeconds} is 60 seconds. If
     *                          a smaller value is used, the {@link LockManager} will use 60 seconds instead.
     * @throws LockException in case there is already a {@link Lock} for {@code key} or the lock could not be created
     * @throws IllegalArgumentException if the {@code key} exceeds 256 chars
     * @see #lock(String)
     */
    void lock(String key, int refreshRateSeconds) throws LockException;

    /**
     * @param key the key to unlock where {@code key} is at most 256 chars. If there exists no lock for {@code key}, a
     *            {link LockException} is thrown. If there is a {@link Lock} but it cannot be released (for example because not owned) a
     *            {@link LockException} is thrown
     * @throws LockException in case no {@link Lock} exists or in case a {@link Lock} exists for {@code key}
     *                      but could not be released or some other exception happened
     * @throws IllegalArgumentException if the {@code key} exceeds 256 chars
     */
    void unlock(String key) throws LockException;

    /**
     * Returns {@code true} if there is a lock for {@code key}. Note that this method returns {@code true} or {@code false}
     * regardless whether the {@link Thread} that invokes {@link #isLocked(String)} contains the lock or whether another
     * {@link Thread} contains the lock
     * @param key the {@code key} to check whether there is a lock for
     * @return {@code true} when locked
     * @throws LockException
     */
    boolean isLocked(String key) throws LockException;

    List<Lock> getLocks();

    /**
     * Releases all locks held by the current JVM and destroys this {@link LockManager}.
     */
    void destroy();
}
