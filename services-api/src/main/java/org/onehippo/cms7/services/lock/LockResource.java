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


public interface LockResource extends AutoCloseable {

    /**
     * @return true if this {@link LockResource} has been closed
     */
    boolean isClosed();

    /**
     * Close the {@link LockResource} and unlocks (removes) the lock.
     * <p>Note: unlike {@link LockManager#unlock(String)} this may be invoked by another thread, allowing
     * delegation of unlocking the lock to another thread</p>
     * <p>Warning: while the LockResource may be closed by another thread, the lock itself remains tied to the thread
     * creating it!<br/>
     * Therefore the thread creating the lock must <em>NOT</em> be terminated before the other thread completes the
     * process requiring the lock, as the lock then <em>may</em> expire prematurely!</p>
     */
    @Override
    void close();

    /**
     * @return the {@link Lock} for ths {@link LockResource}
     */
    Lock getLock();

    /**
     * @return true if this {@link #getLock()} was created together with this {@link LockResource} instance;
     * false if this {@link #getLock()} already was created earlier by the same thread creating this {@link LockResource}.
     */
    boolean isNewLock();

    /**
     * @return the {@link Thread} that holds this {@link LockResource} or {@code null} in case the {@link Thread}
     * that created this lock has already stopped and been GC-ed
     */
    Thread getHolder();
}
