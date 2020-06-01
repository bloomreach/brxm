/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.impl;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;

public class LockManagerDecorator extends SessionBoundDecorator implements LockManager {

    private final LockManager lockManager;

    public static LockManager unwrap(final LockManager lockManager) {
        if (lockManager instanceof LockManagerDecorator) {
            return ((LockManagerDecorator) lockManager).lockManager;
        }
        return lockManager;
    }

    public LockManagerDecorator(final SessionDecorator session, final LockManager lockManager) {
        super(session);
        this.lockManager = unwrap(lockManager);
    }

    @Override
    public void addLockToken(final String lockToken) throws LockException, RepositoryException {
        lockManager.addLockToken(lockToken);
    }

    @Override
    public Lock lock(final String absPath, final boolean isDeep, final boolean isSessionScoped, final long timeoutHint, final String ownerInfo)
            throws RepositoryException {
        return new LockDecorator(session, lockManager.lock(absPath, isDeep, isSessionScoped, timeoutHint, ownerInfo));
    }

    @Override
    public Lock getLock(final String absPath) throws RepositoryException {
        return new LockDecorator(session, lockManager.getLock(absPath));
    }

    @Override
    public String[] getLockTokens() throws RepositoryException {
        return lockManager.getLockTokens();
    }

    @Override
    public boolean holdsLock(final String absPath) throws PathNotFoundException, RepositoryException {
        return lockManager.holdsLock(absPath);
    }

    @Override
    public boolean isLocked(final String absPath) throws PathNotFoundException, RepositoryException {
        return lockManager.isLocked(absPath);
    }

    @Override
    public void removeLockToken(final String lockToken) throws LockException, RepositoryException {
        lockManager.removeLockToken(lockToken);
    }

    @Override
    public void unlock(final String absPath) throws PathNotFoundException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        lockManager.unlock(absPath);
    }
}
