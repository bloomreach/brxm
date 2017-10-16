/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.locking;

import javax.jcr.RepositoryException;
import javax.jcr.lock.LockManager;

/**
 * JCR locking is deprecated, use {@link org.onehippo.cms7.services.lock.LockManager} instead. Creating a (cluster wide)
 * lock with {@link org.onehippo.cms7.services.lock.LockManager} can be achieved as follows:
 * <code>
 *     <pre>
 *        final LockManager lockManager = HippoServiceRegistry.getService(LockManager.class);
 *        try {
 *            lockManager.lock(key);
 *            // do locked work
 *        } catch (LockException e) {
 *            log.info("{} already locked", key);
 *        } finally {
 *            lockManager.unlock(key);
 *        }
 *     </pre>
 * </code>
 * @deprecated since 5.0.3
 */
@Deprecated
public interface HippoLockManager extends LockManager {

    /**
     * Try to unlock the node at {@code absPath} due to expiration of the lock.
     *
     * @param absPath  the path to the node to expire
     * @return whether the lock was successfully expired
     * @throws RepositoryException if an error occurs
     */
    boolean expireLock(String absPath) throws RepositoryException;

    /**
     * @inheritDoc
     */
    @Override
    HippoLock getLock(String absPath) throws RepositoryException;

    /**
     * @inheritDoc
     */
    @Override
    HippoLock lock(String absPath, boolean isDeep, boolean isSessionScoped, long timeoutHint, String ownerInfo)
            throws RepositoryException;

}
