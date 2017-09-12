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

public abstract class AbstractLockManager implements LockManager {

    private final Map<String, AbstractLock> locks = new HashMap();

    abstract Logger getLogger();

    abstract AbstractLock createLock(final String key) throws LockException;

    @Override
    public synchronized void lock(final String key) throws LockException {
        final AbstractLock abstractLock = locks.get(key);
        if (abstractLock == null) {
            getLogger().debug("Create lock '{}' for thread '{}'", key, Thread.currentThread().getName());
            locks.put(key, createLock(key));
            return;
        }
        final Thread lockThread = abstractLock.getThread().get();
        if (lockThread == null) {
            getLogger().warn("Thread '{}' that created lock for '{}' has stopped without releasing the lock. Thread '{}' " +
                    "now gets the lock", abstractLock.getLockOwner(), key, Thread.currentThread().getName());
            abstractLock.setThread(new WeakReference<>(Thread.currentThread()));
            return;
        }
        if (lockThread == Thread.currentThread()) {
            getLogger().debug("Thread '{}' already contains lock '{}', increase hold count", Thread.currentThread().getName(), key);
            abstractLock.increment();
            return;
        }
        throw new LockException(String.format("This thread '%s' cannot lock '%s' : already locked by thread '%s'",
                Thread.currentThread().getName(), key, lockThread.getName()));
    }

    @Override
    public synchronized void unlock(final String key) throws LockException {
        final AbstractLock abstractLock = locks.get(key);
        if (abstractLock == null) {
            getLogger().debug("No lock present for '{}'", key);
            return;
        }
        final Thread lockThread = abstractLock.getThread().get();
        if (lockThread == null) {
            getLogger().warn("Thread '{}' that created lock for '{}' has stopped without releasing the lock. Removing lock now",
                    abstractLock.getLockOwner(), key, Thread.currentThread().getName());
            locks.remove(key);
        }
        if (lockThread != Thread.currentThread()) {
            throw new LockException(String.format("Thread '%s' cannot unlock '%s' because lock owned by '%s'", Thread.currentThread().getName(), key,
                    lockThread.getName()));
        }
        abstractLock.decrement();
        if (abstractLock.holdCount < 0) {
            getLogger().error("Hold count of lock should never be able to be less than 0. Core implementation issue in {}. Remove " +
                            "lock for {} nonetheless.",
                    this.getClass().getName(), key);
            locks.remove(key);
        } else if (abstractLock.holdCount == 0) {
            getLogger().debug("Remove lock '{}'", key);
            locks.remove(key);
        } else {
            getLogger().debug("Lock '{}' will not be removed since hold count is '{}'", key, abstractLock.holdCount);
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
        Iterator<Map.Entry<String, AbstractLock>> iterator = locks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, AbstractLock> next = iterator.next();
            getLogger().warn("Lock '{}' owned by '{}' was never unlocked. Removing the lock now.", next.getKey(), next.getValue().getLockOwner());
            next.getValue().destroy();
            iterator.remove();
        }
    }

    private void expungeNeverUnlockedLocksFromGCedThreads() {
        Iterator<Map.Entry<String, AbstractLock>> iterator = locks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, AbstractLock> next = iterator.next();
            if (next.getValue().getThread().get() == null) {
                getLogger().warn("Lock '{}' with lockOwner '{}' was present but the Thread that created the lock does not exist any more. " +
                        "Removing the lock now", next.getKey(), next.getValue().getLockOwner());
                next.getValue().destroy();
                iterator.remove();
            }
        }
    }
}

