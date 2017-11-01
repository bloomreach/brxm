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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.onehippo.cms7.services.lock.Lock;
import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.cms7.services.lock.LockManagerException;
import org.onehippo.cms7.services.lock.LockResource;
import org.onehippo.cms7.services.lock.AlreadyLockedException;
import org.slf4j.Logger;

import static java.util.concurrent.TimeUnit.SECONDS;

public abstract class AbstractLockManager implements InternalLockManager {

    /**
     * This locks object contains only the locks held by the *current* JVM
     */
    private final Map<String, MutableLock> localLocks = new HashMap<>();

    private final ScheduledExecutorService scheduledExecutorService;

    private static final long DEFAULT_SCHEDULED_JOBS_INITIAL_DELAY_SECONDS = 5;
    private static final long DEFAULT_SCHEDULED_JOBS_INTERVAL_SECONDS = 5;
    public static final long REFRESH_RATE_SECONDS = 60;

    private long longestIntervalSeconds = DEFAULT_SCHEDULED_JOBS_INTERVAL_SECONDS;

    private volatile boolean destroyed = false;
    private boolean destroyInProgress = false;

    protected abstract Logger getLogger();

    protected abstract MutableLock createLock(String key, String threadName) throws LockException;

    protected abstract void releasePersistedLock(String key, String threadName);

    protected abstract void abortPersistedLock(String key) throws LockManagerException;

    protected abstract boolean containsLock(String key) throws LockManagerException;

    protected abstract List<Lock> retrieveLocks() throws LockManagerException;

    public AbstractLockManager() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    protected void addJob(final Runnable runnable) {
        addJob(runnable, DEFAULT_SCHEDULED_JOBS_INITIAL_DELAY_SECONDS, DEFAULT_SCHEDULED_JOBS_INTERVAL_SECONDS);
    }

    @Override
    public synchronized LockResource lock(final String key) throws LockException {
        checkLive();
        validateKey(key);
        final MutableLock lock = localLocks.get(key);
        if (lock == null) {
            getLogger().debug("Create lock '{}' for thread '{}'", key, Thread.currentThread().getName());
            MutableLock newLock = createLock(key, Thread.currentThread().getName());
            localLocks.put(key, newLock);
            return new LockResourceImpl(newLock);
        }
        final Thread lockThread = lock.getThread().get();
        if (lockThread == null || !lockThread.isAlive()) {
            getLogger().warn("Thread '{}' that created lock for '{}' has stopped without releasing the lock. Thread '{}' " +
                    "now gets the lock", lock.getLockThread(), key, Thread.currentThread().getName());
            unlock(key);
            MutableLock newLock = createLock(key, Thread.currentThread().getName());
            localLocks.put(key, newLock);
            return new LockResourceImpl(newLock);
        }
        if (lockThread == Thread.currentThread()) {
            getLogger().debug("Thread '{}' already contains lock '{}', increase hold count", Thread.currentThread().getName(), key);
            lock.increment();
            return new LockResourceImpl(lock);
        }
        throw new AlreadyLockedException(String.format("This thread '%s' cannot lock '%s' : already locked by thread '%s'",
                Thread.currentThread().getName(), key, lockThread.getName()));
    }

    private class LockResourceImpl implements LockResource {
        private final MutableLock lock;
        private final boolean newLock;
        private boolean closed = false;

        LockResourceImpl(final MutableLock lock) {
            this.lock = lock;
            this.newLock = lock.getHoldCount() == 1;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                unlock(this);
            }
        }

        @Override
        public MutableLock getLock() {
            return lock;
        }

        @Override
        public boolean isNewLock() {
            return newLock;
        }

        @Override
        public Thread getHolder() {
            return lock.getThread().get();
        }
    }

    @Override
    public synchronized void unlock(final String key) {
        checkLive();
        validateKey(key);
        unlock(key, null);
    }

    protected synchronized void unlock(final LockResource lockResource) {
        checkLive();
        unlock(lockResource.getLock().getLockKey(), lockResource.getLock());
    }

    protected void unlock(final String key, final Lock lock) {
        final MutableLock localLock = localLocks.get(key);
        if (localLock == null) {
            if (lock == null) {
                getLogger().error("Lock '{}' does not exist or this cluster node does not contain the lock hence a thread from " +
                        "this JVM cannot unlock it", key);
            } else {
                final String msg = String.format("Lock '%s' already manually unlocked in thread '%s'. This is a coding error!", key, lock.getLockThread());
                getLogger().error(msg, new IllegalStateException(msg));
            }
            return;
        } else if (lock != null && lock != localLock) {
            final String msg = String.format("Lock '%s' already unlocked before AND locked again. This is a coding error!", key);
            getLogger().error(msg, new IllegalStateException(msg));
            return;
        }
        final Thread lockThread = localLock.getThread().get();
        if (lockThread == null || !lockThread.isAlive()) {
            getLogger().error("Thread '{}' that created lock for '{}' has stopped without releasing the lock. The Thread " +
                            "should have invoked #unlock. Removing lock now",
                    localLock.getLockThread(), key, Thread.currentThread().getName());
            releasePersistedLock(key, localLock.getLockThread());
            localLocks.remove(key);
            return;
        }
        if (lock == null && lockThread != Thread.currentThread()) {
            getLogger().error("Thread '{}' cannot unlock '{}' because lock owned by '{}'. Thread '{}' should never had " +
                            "invoked #unlock({}), This is a coding error!", Thread.currentThread().getName(), key,
                    lockThread.getName(), Thread.currentThread().getName(), key);
            return;
        }
        localLock.decrement();
        if (localLock.getHoldCount() < 0) {
            getLogger().error("Hold count of lock should never be able to be less than 0. Core implementation issue in {}. Remove " +
                            "lock for {} nonetheless.",
                    this.getClass().getName(), key);
            localLocks.remove(key);
            releasePersistedLock(key, lockThread.getName());
        } else if (localLock.getHoldCount() == 0) {
            getLogger().debug("Remove lock '{}'", key);
            localLocks.remove(key);
            releasePersistedLock(key, lockThread.getName());
        } else {
            getLogger().debug("Lock '{}' will not be removed since hold count is '{}'", key, localLock.getHoldCount());
        }
    }

    @Override
    public synchronized void abort(final String key) throws LockManagerException {
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
                throw new IllegalStateException(msg);
            }
        }

        abortPersistedLock(key);
    }

    @Override
    public synchronized boolean isLocked(final String key) throws LockManagerException {
        checkLive();
        validateKey(key);
        final MutableLock lock = localLocks.get(key);
        if (lock != null) {
            final Thread thread = lock.getThread().get();
            if (thread == null || !thread.isAlive()) {
                getLogger().error("Lock '{}' with lockOwner '{}' was present but the Thread that created the lock already stopped. " +
                        "Removing the lock now", key, lock.getLockOwner());
                releasePersistedLock(key, lock.getLockThread());
                localLocks.remove(key);
                return false;
            }
            return true;
        }
        return containsLock(key);
    }

    @Override
    public synchronized List<Lock> getLocks() throws LockManagerException {
        checkLive();
        expungeNeverUnlockedLocksFromStoppedThreads();
        return retrieveLocks();
    }

    /**
     * Destroy MOST not be synchronized because other threads need to be able to #unlock
     */
    @Override
    public void destroy() {

        synchronized (this) {
            if (destroyInProgress) {
                return;
            }
            destroyInProgress = true;
        }

        scheduledExecutorService.shutdown();
        try {
            boolean success = scheduledExecutorService.awaitTermination(longestIntervalSeconds + 5, SECONDS);
            if (!success) {
                getLogger().warn("Not all jobs have been successfully completed.");
            }
        } catch (InterruptedException e) {
            getLogger().error("InterruptedException during shutdown of scheduledExecutorService : ", e);
        }

        final List<Thread> waitForThreads = new ArrayList<>();
        for (MutableLock mutableLock : localLocks.values()) {
            Thread thread = mutableLock.getThread().get();
            if (thread == null || !thread.isAlive() || thread.isInterrupted()) {
                continue;
            }
            thread.interrupt();
            waitForThreads.add(thread);
        }

        // try graceful shutdown for the interrupted threads
        long waitMax = 10_000;
        for (Thread thread : waitForThreads) {
            try {
                long start = System.currentTimeMillis();
                thread.join(waitMax);
                waitMax -= System.currentTimeMillis() - start;
            } catch (InterruptedException e) {
                getLogger().info("Thread '{}' already interrupted");
                thread.interrupt();
            }
        }
        clear();
        destroyed = true;
    }

    @Override
    public synchronized void clear() {
        final Iterator<Map.Entry<String, MutableLock>> iterator = localLocks.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, MutableLock> next = iterator.next();
            getLogger().warn("Lock '{}' owned by cluster '{}' and thread '{}' was never unlocked. Removing the lock now.",
                    next.getKey(), next.getValue().getLockOwner(), next.getValue().getLockThread());
            releasePersistedLock(next.getKey(), next.getValue().getLockThread());
            iterator.remove();
        }
    }

    public synchronized void checkLive() {
        if (destroyed) {
            throw new IllegalStateException("This LockManager has been destroyed.");
        }
    }

    public synchronized void expungeNeverUnlockedLocksFromStoppedThreads() {
        final Iterator<Map.Entry<String, MutableLock>> iterator = localLocks.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, MutableLock> next = iterator.next();
            final MutableLock lock = next.getValue();
            final Thread thread = lock.getThread().get();
            if (thread == null || !thread.isAlive()) {
                getLogger().error("Lock '{}' with lockOwner '{}' was present but the Thread that created the lock already stopped. " +
                        "Removing the lock now", next.getKey(), lock.getLockOwner());
                releasePersistedLock(next.getKey(), next.getValue().getLockThread());
                iterator.remove();
            }
        }
    }

    private void validateKey(final String key) {
        if (key == null || key.length() > LOCK_KEY_MAX_LENGTH) {
            throw new IllegalArgumentException(String.format("Key is not allowed to be null or longer than %s chars", LOCK_KEY_MAX_LENGTH));
        }
    }

    public void addJob(final Runnable runnable, final long initialDelaySeconds, final long periodSeconds) {
        // make the runnable synchronized to avoid concurrency from background jobs with methods in the LockManager
        // modifying 'locks'
        final Runnable exceptionCatchingRunnable = () -> {
            final long start = System.currentTimeMillis();
            getLogger().info("Running '{}' at {}", runnable.getClass().getName(), Calendar.getInstance().getTime());
            try {
                runnable.run();
            } catch (Exception e) {
                getLogger().error("Background job '{}' resulted in exception.", runnable.getClass().getName(), e);
            }
            getLogger().info("Running '{}' finished in '{}' ms.", runnable.getClass().getName(), (System.currentTimeMillis() - start));
        };
        if (periodSeconds > longestIntervalSeconds) {
            longestIntervalSeconds = periodSeconds;
        }
        scheduledExecutorService.scheduleAtFixedRate(exceptionCatchingRunnable, initialDelaySeconds, periodSeconds, SECONDS);
    }

    /**
     * @return a copy of {@code localLocks}
     */
    public synchronized Map<String, MutableLock> getLocalLocks() {
        return new HashMap<>(localLocks);
    }

    public class UnlockStoppedThreadJanitor implements Runnable {
        @Override
        public void run() {
            checkLive();
            expungeNeverUnlockedLocksFromStoppedThreads();
        }
    }

}

