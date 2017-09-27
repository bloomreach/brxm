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
     *      A lock is released when a successful {@link #unlock(String)} is invoked as many times as
     *     {@link #lock(String)}. Alternatively, when the {@link LockManager} implementation detects that the Thread
     *     that held the lock is not live any more, the {@link LockManager} implementation can also release the lock.
     *     Lastly, in a database setup, a lock will be released when it has not been refreshed for 60 seconds,
     *     see {@link #lock(String, int)}. This last case is useful in clustered setups where a cluster node has
     *     an ungraceful shutdown (crash) : A graceful shutdown should namely release all locks, also implying
     *     that every Thread that holds a lock calls #unlock
     * </p>
     * <p>
     *     A persisted {@link Lock} can be marked to be aborted: In this case, the {@link Thread} that holds the lock
     *     gets interrupted ({@link Thread#interrupt()}). Threads that hold a lock should invoke {@link #unlock(String)}
     *     when interrupted (in general by just stopping their work and make sure the finally in the try block is hit
     *     which in general should contain the {@link #unlock(String)} logic.
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
     * @param key the key to unlock where {@code key} is at most 256 chars. If the {@link Thread} that invokes
     *           {@link #unlock(String) unlock(key)} does not hold the {@link Lock}, nothing happens (apart from
     *            the {@link LockManager} implementation most likely logging a warning or error, because it is an
     *            implementation issue if  {@code unlock(key)} is invoked by a thread that does not hold the lock.
     * @throws IllegalArgumentException if the {@code key} exceeds 256 chars
     * @throws RuntimeException in case some error occurs.
     */
    void unlock(String key);

    /**
     * <p>
     *     Returns {@code true} if there is a lock for {@code key}. Note that this method returns {@code true} or {@code false}
     *     regardless whether the {@link Thread} that invokes {@link #isLocked(String)} contains the lock or whether another
     *     {@link Thread} contains the lock
     * </p>
     * @param key the {@code key} to check whether there is a lock for
     * @return {@code true} when locked
     * @throws RuntimeException in case some error occurs.
     */
    boolean isLocked(String key);

    /**
     * @return all the {@link Lock}s that are currently active (including locks that are marked to be aborted but not
     * yet aborted)
     * @throws RuntimeException in case some error occurs.
     */
    List<Lock> getLocks();

    /**
     * <p>
     *     Indicates the {@link LockManager} that the {@link Thread} containing the {@link Lock} for {@code key} should
     *     be interrupted.
     *     This method can be invoked by another thread than the one that holds the  {@link Lock}. In clustered setups
     *     it can be requested by other cluster nodes that do not contain a {@link Thread} that holds the {@link Lock}.
     * </p>
     * <p>
     *     When the {@link LockManager} finds a lock marked to be aborted contained in its own JVM,
     *     it must interrupt the {@link Thread} that holds the {@link Lock}. As a result, the process should stop
     *     and the {@link Thread} to abort should invoke {@link #unlock(String)}
     * </p>
     * <p>
     *     If there is no {@link Lock} for {@code key}, nothing happens and void is returned.
     * </p>
     * @param key the {@code key} to check whether there is a lock for
     * @throws IllegalArgumentException if the {@code key} exceeds 256 chars
     * @throws RuntimeException in case some error occurs.
     */
    void abort(String key);


}
