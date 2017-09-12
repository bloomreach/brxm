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
package org.onehippo.services.lock;

import java.util.List;

import org.onehippo.cms7.services.lock.Lock;
import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.cms7.services.lock.LockManager;

public class AssertingLockManager implements LockManager {

    private LockManager delegatee;

    public AssertingLockManager(final LockManager delegatee) {
        this.delegatee = delegatee;
    }

    @Override
    public void lock(final String key) throws LockException {
        if (key == null || key.length() > 256) {
            throw new IllegalArgumentException("Key is not allowed to be null or longer than 256 chars");
        }
        delegatee.lock(key);
    }

    @Override
    public void unlock(final String key) throws LockException {
        if (key == null || key.length() > 256) {
            throw new IllegalArgumentException("Key is not allowed to be null or longer than 256 chars");
        }
        delegatee.unlock(key);
    }

    @Override
    public boolean isLocked(final String key) throws LockException {
        if (key == null || key.length() > 256) {
            throw new IllegalArgumentException("Key is not allowed to be null or longer than 256 chars");
        }
        return delegatee.isLocked(key);
    }

    @Override
    public List<Lock> getLocks() {
        return delegatee.getLocks();
    }

    @Override
    public void destroy() {
        delegatee.destroy();
    }
}
