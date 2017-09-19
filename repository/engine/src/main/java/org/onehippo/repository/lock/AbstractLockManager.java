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
package org.onehippo.repository.lock;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.onehippo.cms7.services.lock.Lock;
import org.onehippo.cms7.services.lock.LockException;
import org.slf4j.Logger;

import static java.util.concurrent.TimeUnit.SECONDS;

public abstract class AbstractLockManager implements InternalLockManager {

    /**
     * This locks object contains only the locks held by the *current* JVM
     */
    protected final Map<String, MutableLock> localLocks = new HashMap<>();

    private final ScheduledExecutorService scheduledExecutorService;

    private static final long DEFAULT_SCHEDULED_JOBS_INITIAL_DELAY_SECONDS = 5;
    private static final long DEFAULT_SCHEDULED_JOBS_INTERVAL_SECONDS = 5;

    private long longestIntervalSeconds = DEFAULT_SCHEDULED_JOBS_INTERVAL_SECONDS;

    private boolean destroyed = false;

    protected abstract Logger getLogger();

    protected abstract MutableLock createLock(String key, String threadName, int refreshRateSeconds) throws LockException;

    protected abstract void releasePersistedLock(String key, String threadName) throws LockException;

    protected abstract void abortPersistedLock(String key) throws LockException;

    protected abstract boolean containsLock(String key) throws LockException;

    protected abstract List<Lock> retrieveLocks() throws LockException;

    public AbstractLockManager() {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    protected void addJob(final Runnable runnable) {
        addJob(runnable, DEFAULT_SCHEDULED_JOBS_INITIAL_DELAY_SECONDS, DEFAULT_SCHEDULED_JOBS_INTERVAL_SECONDS);
    }

    @Override
    public synchronized void lock(final String key) throws LockException {
        lock(key, 60);
    }

    @Override
    public synchronized void lock(final String key, int refreshRateSeconds) throws LockException {
        checkLive();
        validateKey(key);
        if (refreshRateSeconds < 60) {
            getLogger().warn("refreshRateSeconds not allowed to be '{}'. Must be at least 60 or higher. Using 60 now.",
                    refreshRateSeconds);
            refreshRateSeconds = 60;
        }
        final MutableLock lock = localLocks.get(key);
        if (lock == null) {
            getLogger().debug("Create lock '{}' for thread '{}'", key, Thread.currentThread().getName());
            localLocks.put(key, createLock(key, Thread.currentThread().getName(), refreshRateSeconds));
            return;
        }
        final Thread lockThread = lock.getThread().get();
        if (lockThread == null || !lockThread.isAlive()) {
            getLogger().warn("Thread '{}' that created lock for '{}' has stopped without releasing the lock. Thread '{}' " +
                    "now gets the lock", lock.getLockOwner(), key, Thread.currentThread().getName());
            unlock(key);
            localLocks.put(key, createLock(key, Thread.currentThread().getName(), refreshRateSeconds));
            return;
        }
        if (lockThread == Thread.currentThread()) {
            getLogger().debug("Thread '{}' already contains lock '{}', increase hold count", Thread.currentThread().getName(), key);
            lock.increment();
            return;
        }
        throw new LockException(String.format("This thread '%s' cannot lock '%s' : already locked by thread '%s'",
                Thread.currentThread().getName(), key, lockThread.getName()));
    }

    @Override
    public synchronized void unlock(final String key) throws LockException {
        checkLive();
        validateKey(key);
        final MutableLock lock = localLocks.get(key);
        if (lock == null) {
            // note there can still be a database lock : The lock might be owned by a different cluster node
            throw new LockException(String.format("No lock present for '{}'", key));
        }
        final Thread lockThread = lock.getThread().get();
        if (lockThread == null || !lockThread.isAlive()) {
            getLogger().warn("Thread '{}' that created lock for '{}' has stopped without releasing the lock. Removing lock now",
                    lock.getLockOwner(), key, Thread.currentThread().getName());
            releasePersistedLock(key, lockThread.getName());
            localLocks.remove(key);
            return;
        }
        if (lockThread != Thread.currentThread()) {
            throw new LockException(String.format("Thread '%s' cannot unlock '%s' because lock owned by '%s'", Thread.currentThread().getName(), key,
                    lockThread.getName()));
        }
        lock.decrement();
        if (lock.holdCount < 0) {
            getLogger().error("Hold count of lock should never be able to be less than 0. Core implementation issue in {}. Remove " +
                            "lock for {} nonetheless.",
                    this.getClass().getName(), key);
            localLocks.remove(key);
            releasePersistedLock(key, lockThread.getName());
        } else if (lock.holdCount == 0) {
            getLogger().debug("Remove lock '{}'", key);
            localLocks.remove(key);
            releasePersistedLock(key, lockThread.getName());
        } else {
            getLogger().debug("Lock '{}' will not be removed since hold count is '{}'", key, lock.holdCount);
        }

    }

    @Override
    public synchronized void abort(final String key) throws LockException {
        checkLive();
        validateKey(key);
        final MutableLock localLock = localLocks.get(key);
        if (localLock != null) {
            // The cluster node that invoked #abort(key) happens to also contain the Thread that holds the lock, hence
            // inform the Thread. If it is another cluster node that holds the lock, that cluster node will find out
            // by polling that it should abort
            final Thread thread = localLock.getThread().get();
            if (thread == null || !thread.isAlive()) {
                getLogger().info("Thread '{}' already stopped for lock '{}'.", localLock.getLockThread(), key);
                return;
            }
            // signal the running thread that it should abort : This thread should then in turn call #unlock itself : That
            // won't be done here
            try {
                thread.interrupt();
            } catch (SecurityException e) {
                String msg = String.format("Thread '%s' is not allowed to be interrupted. Can't abort '%s'", thread.getName(), key);
                getLogger().warn(msg);
                throw new LockException(msg);
            }
        }
        abortPersistedLock(key);
    }

    @Override
    public synchronized boolean isLocked(final String key) throws LockException {
        checkLive();
        validateKey(key);
        expungeNeverUnlockedLocksFromStoppedThreads();
        return containsLock(key);
    }

    @Override
    public synchronized List<Lock> getLocks() throws LockException {
        checkLive();
        expungeNeverUnlockedLocksFromStoppedThreads();
        return retrieveLocks();
    }

    @Override
    public synchronized void destroy() {
        checkLive();
        destroyed = true;
        scheduledExecutorService.shutdown();
        try {
            scheduledExecutorService.awaitTermination(longestIntervalSeconds + 5, SECONDS);
        } catch (InterruptedException e) {
            getLogger().error("InterruptedException during shutdown of scheduledExecutorService : ", e);
        }
        clear();
    }

    @Override
    public synchronized void clear() {
        final Iterator<Map.Entry<String, MutableLock>> iterator = localLocks.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, MutableLock> next = iterator.next();
            getLogger().warn("Lock '{}' owned by '{}' was never unlocked. Removing the lock now.", next.getKey(), next.getValue().getLockOwner());
            tryReleaseLock(next.getKey(), next.getValue().getLockThread());
            iterator.remove();
        }
    }

    public synchronized void checkLive() {
        if (destroyed) {
            throw new IllegalStateException("This LockManager has been destroyed.");
        }
    }

    private synchronized  void tryReleaseLock(final String key, final String lockThread) {
        try {
            releasePersistedLock(key, lockThread);
        } catch (LockException e) {
            if (getLogger().isDebugEnabled()) {
                getLogger().warn("Could not release '{}'", key, e);
            } else {
                getLogger().warn("Could not release '{}' : {}", key, e.toString());
            }
        }
    }

    public synchronized void expungeNeverUnlockedLocksFromStoppedThreads() {
        final Iterator<Map.Entry<String, MutableLock>> iterator = localLocks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, MutableLock> next = iterator.next();
            MutableLock lock = next.getValue();
            if (lock.getThread().get() == null || !lock.getThread().get().isAlive()) {
                getLogger().warn("Lock '{}' with lockOwner '{}' was present but the Thread that created the lock already stopped. " +
                        "Removing the lock now", next.getKey(), lock.getLockOwner());
                tryReleaseLock(next.getKey(), next.getValue().getLockThread());
                iterator.remove();
            }
        }
    }

    private void validateKey(final String key) {
        if (key == null || key.length() > 256) {
            throw new IllegalArgumentException("Key is not allowed to be null or longer than 256 chars");
        }
    }

    @Override
    public void addJob(final Runnable runnable, final long initialDelaySeconds, final long periodSeconds) {
        // make the runnable synchronized to avoid concurrency from background jobs with methods in the LockManager
        // modifying 'locks'
        final Runnable synchronizedRunnable = () -> {
            synchronized (AbstractLockManager.this) {
                final long start = System.currentTimeMillis();
                getLogger().info("Running '{}' at {}", runnable.getClass().getName(), Calendar.getInstance().getTime());
                try {
                    runnable.run();
                } catch (Exception e) {
                    getLogger().error("Background job '{}' resulted in exception.", runnable.getClass().getName(), e);
                }
                getLogger().info("Running '{}' finished in '{}' ms.", runnable.getClass().getName(), (System.currentTimeMillis() - start));
            }
        };
        if (periodSeconds > longestIntervalSeconds) {
            longestIntervalSeconds = periodSeconds;
        }
        scheduledExecutorService.scheduleAtFixedRate(synchronizedRunnable, initialDelaySeconds, periodSeconds, SECONDS);
    }

    public class UnlockStoppedThreadJanitor implements Runnable {
        @Override
        public void run() {
            checkLive();
            expungeNeverUnlockedLocksFromStoppedThreads();
        }
    }

}

