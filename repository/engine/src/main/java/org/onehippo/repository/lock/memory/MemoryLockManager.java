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
package org.onehippo.repository.lock.memory;

import java.util.ArrayList;
import java.util.List;

import org.onehippo.cms7.services.lock.Lock;
import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.cms7.services.lock.LockManagerException;
import org.onehippo.repository.lock.AbstractLockManager;
import org.onehippo.repository.lock.MutableLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryLockManager extends AbstractLockManager {

    private static final Logger log = LoggerFactory.getLogger(MemoryLockManager.class);

    public MemoryLockManager() {
        addJob(new UnlockStoppedThreadJanitor());
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected MutableLock createLock(final String key, final String threadName) throws LockException {
        return new MutableLock(key, "default", threadName, System.currentTimeMillis(), "RUNNING");
    }

    @Override
    protected void releasePersistedLock(final String key, final String threadName) {
        // no persistent lock needs to be removed so nothing to do
    }

    @Override
    protected void abortPersistedLock(final String key) throws LockManagerException {
        // no persistent lock needs to be aborted so nothing else is needed
    }

    @Override
    protected synchronized boolean containsLock(final String key) throws LockManagerException {
        return getLocalLocks().containsKey(key);
    }

    @Override
    protected synchronized List<Lock> retrieveLocks() throws LockManagerException {
        return new ArrayList<>(getLocalLocks().values());
    }
}
