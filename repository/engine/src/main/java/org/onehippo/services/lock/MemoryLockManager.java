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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.onehippo.cms7.services.lock.Lock;
import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.cms7.services.lock.LockManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryLockManager implements LockManager {

    private static final Logger log = LoggerFactory.getLogger(MemoryLockManager.class);

    private final Map<String, MemoryLock> locks = new HashMap();

    @Override
    public synchronized void lock(final String key) throws LockException {
        final MemoryLock memoryLock = locks.get(key);
        if (memoryLock == null) {
            log.debug("Create lock '{}' for thread '{}'", key, Thread.currentThread().getName());
            locks.put(key, new MemoryLock(key));
            return;
        }
        final Thread lockThread = memoryLock.thread.get();
        if (lockThread == null) {
            log.warn("Thread '{}' that created lock for '{}' has stopped without releasing the lock. Thread '{}' " +
                    "now gets the lock", memoryLock.getLockOwner(),  key, Thread.currentThread().getName());
            memoryLock.thread = new WeakReference<>(Thread.currentThread());
            return;
        }
        if (lockThread == Thread.currentThread()) {
            log.debug("Thread '{}' already contains lock '{}', increase hold count", Thread.currentThread().getName(), key);
            memoryLock.increment();
            return;
        }
        throw new LockException(String.format("This thread '%s' cannot lock '%s' : already locked by thread '%s'",
                Thread.currentThread().getName(), key, lockThread.getName()));
    }

    @Override
    public synchronized void unlock(final String key) throws LockException {
        final MemoryLock memoryLock = locks.get(key);
        if (memoryLock == null) {
            log.debug("No lock present for '{}'", key);
            return;
        }
        final Thread lockThread = memoryLock.thread.get();
        if (lockThread == null) {
            log.warn("Thread '{}' that created lock for '{}' has stopped without releasing the lock. Removing lock now",
                    memoryLock.getLockOwner(), key, Thread.currentThread().getName());
            locks.remove(key);
        }
        if (lockThread != Thread.currentThread()) {
            throw new LockException(String.format("Thread '%s' cannot unlock '%s' because lock owned by '%s'", Thread.currentThread().getName(), key,
                    lockThread.getName()));
        }
        memoryLock.decrement();
        if (memoryLock.holdCount < 0) {
            log.error("Hold count of lock should never be able to be less than 0. Core implementation issue in {}. Remove " +
                            "lock for {} nonetheless.",
                    this.getClass().getName(), key);
            locks.remove(key);
        } else if (memoryLock.holdCount == 0) {
            log.debug("Remove lock '{}'", key);
            locks.remove(key);
        } else {
            log.debug("Lock '{}' will not be removed since hold count is '{}'", key, memoryLock.holdCount);
        }

    }

    @Override
    public synchronized boolean isLocked(final String key) throws LockException {
        expungeNeverUnlockedLocksFromGCedThreads();
        return locks.containsKey(key);
    }

    @Override
    public synchronized List<Lock> getLocks() {
        expungeNeverUnlockedLocksFromGCedThreads();
        return new ArrayList<>(locks.values());
    }

    @Override
    public void destroy() {
        Iterator<Map.Entry<String, MemoryLock>> iterator = locks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, MemoryLock> next = iterator.next();
            log.warn("Lock '{}' owned by '{}' was never unlocked. Removing the lock now.", next.getKey(), next.getValue().getLockOwner());
        }
    }

    private void expungeNeverUnlockedLocksFromGCedThreads() {
        Iterator<Map.Entry<String, MemoryLock>> iterator = locks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, MemoryLock> next = iterator.next();
            if (next.getValue().thread.get() == null) {
                log.warn("Lock '{}' with lockOwner '{}' was present but the Thread that created the lock does not exist any more. " +
                        "Removing the lock now", next.getKey(), next.getValue().getLockOwner());
                iterator.remove();
            }
        }
    }

    class MemoryLock extends Lock {

        private WeakReference<Thread> thread;
        int holdCount;

        public MemoryLock(final String lockKey) {
            super(lockKey, Thread.currentThread().getName(), System.currentTimeMillis());
            thread = new WeakReference<>(Thread.currentThread());
            holdCount = 1;
        }

        public void increment() {
            holdCount++;
        }
        public void decrement() {
            holdCount--;
        }
    }
}
