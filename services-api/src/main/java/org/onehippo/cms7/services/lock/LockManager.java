/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 *     This LockManager is a manager to obtain cluster wide locks. To obtain the {@link LockManager} you can use
 *     <code>
 *         <pre>
 *            LockManager lockManager = HippoServiceRegistry.getService(LockManager.class))
 *         </pre>
 *     </code>
 *     When a {@link Lock} is obtained, that lock is tied to the {@link Thread} that obtained the
 *     {@link Lock} and can only be unlocked by the same {@link Thread}. The number of invocations on {@link #lock(String)}
 *     must be balanced with {@link #unlock(String)} since calling {@link Lock} multiple times increases the hold count: Only
 *     when the hold count is 0, the lock is really freed.
 * </p>
 * <p>
 *     An example usage is as follows:
 *     <code>
 *         <pre>
 *             public void run() {
 *                try (LockResource ignore = lockManager.lock(key)){
 *                   // session.refresh(true|false) if JCR nodes are involved
 *                   // Do work
 *                } catch (AlreadyLockedException e) {
 *                   log.info("'{}' is already locked", key, e);
 *                } catch (LockException e) {
 *                   log.error("Exception while trying to obtain lock, e);
 *                }
 *              }
 *         </pre>
 *     </code>
 *     or without using {@link AutoCloseable} concept of {@link LockResource}:
 *     <code>
 *         <pre>
 *             public void run() {
 *                boolean locked = false;
 *                try {
 *                   lockManager.lock(key);
 *                   locked = true;
 *                   // session.refresh(true|false) if JCR nodes are involved
 *                   // Do work
 *                } catch (AlreadyLockedException e) {
 *                   log.info("'{}' is already locked", key, e);
 *                } catch (LockException e) {
 *                   log.error("Exception while trying to obtain lock, e);
 *                }finally {
 *                   if (locked) {
 *                     lockManager.unlock(key);
 *                   }
 *                }
 *              }
 *         </pre>
 *     </code>
 * </p>
 * <p>
 *     Note that when {@code key} is already locked by another {@link Thread} or other cluster node,
 *     the invocation of {@link #lock(String) #lock(key)} directly results in an {@link AlreadyLockedException} :
 *     This is thus <strong>different</strong> than {@link ReentrantLock#lock()} behavior (which blocks until the lock is acquired).
 *     If you need similar behavior to {@link ReentrantLock#lock()} but then <strong>cluster wide</strong>, you can use
 *     {@link LockManagerUtils#waitForLock(LockManager, String, long)} and if you need the cluster wide equivalent
 *     of {@link java.util.concurrent.locks.ReentrantLock#tryLock(long, TimeUnit)} you can use
 *     {@link LockManagerUtils#waitForLock(LockManager, String, long, long)}.
 * </p>
 * <p>
 *     <strong>Usage in combination with JCR:</strong>
 *     <br/>
 *     When you use this {@link LockManager} to obtain a cluster wide lock after which the code is doing JCR node manipulation,
 *     eg updating the last modification timestamp on a JCR node, then make sure to always invoke
 *     <code>
 *         <pre>
 *             session.refresh(true|false);
 *         </pre>
 *     </code>
 *     after obtaining the {@link LockResource}. The reason for this is that in the cluster wide 'synchronized' part of
 *     the code, you want to make sure that all JCR nodes the code is going to touch are in sync with the latest cluster
 *     state and that the code is not chatting with local stale JCR nodes. Thus make sure to always invoke
 *     {@code session.refresh(true|false);} when dealing with JCR nodes in a cluster wide synchronized code block.
 * </p>
 *
 */
public interface LockManager {

    /**
     * Maximum number of chars for a {@link #lock(String)} key.
     */
    int LOCK_KEY_MAX_LENGTH = 256;

    /**
     * <p>
     *     Tries to create a {@link Lock} for {@code key}. The {@code key} is not allowed to exceed 256 chars. If there
     *     is already a {@code Lock} for {@code key} then in case the current {@link Thread} has the lock, void is
     *     returned, otherwise a {@link LockException} is thrown.
     * </p>
     * <p>
     *     Invoking this method multiple times with the same {@code key} and the same {@link Thread} results in the hold count
     *     being incremented. To unlock the lock, {@link #unlock(String)} or {@link LockResource#close()} must be invoked
     *     an equal amount of times as {@link #lock(String)} was invoked and the unlock must be invoked with the
     *     same {@link Thread} as the one that obtained the {@link Lock}. Note that the {@link LockResource#close()} may
     *     be invoked by a different {@link Thread}!
     * </p>
     * <p>
     *      A lock is released when a successful {@link #unlock(String)} or {@link LockResource#close()} is invoked as
     *      many times as {@link #lock(String)}. Alternatively, when the {@link LockManager} implementation detects that
     *      the Thread that held the lock is not live any more, the {@link LockManager} implementation can also release the lock.
     * </p>
     * <p>
     *     In a clustered setup, a lock will be released (in the database) when it has not been refreshed for more than
     *     60 seconds.
     *     This is a safeguard in case of a clustered setup where a cluster node has an ungraceful shutdown (crash) : In that
     *     case some database lock might still be present for the crashed node.
     *     A graceful shutdown should release all locks, implying that every Thread that holds a lock calls {@link #unlock}
     * </p>
     * <p>
     *     A persisted {@link Lock} can be marked to be aborted: In this case, the {@link Thread} that holds the lock
     *     gets interrupted ({@link Thread#interrupt()}). Threads that hold a lock should invoke {@link #unlock(String)}
     *     when interrupted (in general by just stopping their work and make sure the finally in the try block is hit
     *     which in general should contain the {@link #unlock(String)} logic.
     * </p>
     * @param key the key for the {@link Lock} where {@code key} is now allowed to exceed 256 chars
     * @return {@link LockResource} such that this {@link #lock(String)} method can be used in a try-with-resources statement
     *         where the {@link LockResource#close()} results in the lock being freed.
     * @throws LockException in case there is already a {@link Lock} for {@code key} (throwing a {@link AlreadyLockedException})
     *         or the lock could not be created due to some other exception (resulting in a {@link LockManagerException})
     * @throws IllegalArgumentException if the {@code key} exceeds 256 chars
     */
    LockResource lock(String key) throws LockException;

    /**
     * @param key the key to unlock where {@code key} is at most 256 chars. If the {@link Thread} that invokes
     *           {@link #unlock(String) unlock(key)} does not hold the {@link Lock}, nothing happens (apart from
     *            the {@link LockManager} implementation most likely logging a warning or error, because it is an
     *            implementation issue if  {@code unlock(key)} is invoked by a thread that does not hold the lock.
     * @throws IllegalArgumentException if the {@code key} exceeds 256 chars
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
     * @throws IllegalArgumentException if the {@code key} exceeds 256 chars
     * @throws LockManagerException if some irrecoverable error occurs, for example a database request timeout
     */
    boolean isLocked(String key) throws LockManagerException;

    /**
     * @return all the {@link Lock}s that are currently active (including locks that are marked to be aborted but not
     * yet aborted)
     * @throws LockManagerException if some irrecoverable error occurs, for example a database request timeout
     */
    List<Lock> getLocks() throws LockManagerException;

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
     * @throws LockManagerException if some irrecoverable error occurs, for example a database request timeout
     */
    void abort(String key) throws LockManagerException;


}
