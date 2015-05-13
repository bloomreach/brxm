/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Calendar;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;

import org.hippoecm.repository.jackrabbit.InternalHippoSession;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.locking.HippoLock;
import org.onehippo.repository.locking.HippoLockManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_LOCKEXPIRATIONTIME;
import static org.hippoecm.repository.api.HippoNodeType.NT_LOCKABLE;

public class LockManagerDecorator extends org.hippoecm.repository.decorating.LockManagerDecorator implements HippoLockManager {

    private static final Logger log = LoggerFactory.getLogger(LockManagerDecorator.class);
    private static final Calendar NO_TIMEOUT = Calendar.getInstance();
    static {
        NO_TIMEOUT.setTimeInMillis(Long.MAX_VALUE);
    }

    private final ScheduledExecutorService executor;

    public LockManagerDecorator(final Session session, final LockManager lockManager) {
        super(session, lockManager);
        executor = ((InternalHippoSession) SessionDecorator.unwrap(session)).getExecutor();
    }

    public static LockManager unwrap(LockManager lockManager) {
        if (lockManager instanceof LockManagerDecorator) {
            return ((LockManagerDecorator) lockManager).lockManager;
        }
        return lockManager;
    }

    @Override
    public boolean expireLock(final String absPath) throws LockException, RepositoryException {
        final Node lockNode = session.getNode(absPath);
        final Calendar timeout = JcrUtils.getDateProperty(lockNode, HIPPO_LOCKEXPIRATIONTIME, NO_TIMEOUT);
        if (System.currentTimeMillis() < timeout.getTimeInMillis()) {
            return false;
        }
        try {
            unlock(absPath);
        } catch (LockException e) {
            return !isLocked(absPath);
        }
        return true;
    }

    @Override
    public HippoLock lock(final String absPath, final boolean isDeep, final boolean isSessionScoped, final long timeoutHint, final String ownerInfo)
            throws RepositoryException {
        if (isLocked(absPath)) {
            if (!expireLock(absPath)) {
                throw new LockException("Already locked: " + absPath);
            }
        }
        final Lock lock = super.lock(absPath, isDeep, isSessionScoped, timeoutHint, ownerInfo);
        setTimeout(lock, timeoutHint);
        return new LockDecorator(lock, timeoutHint);
    }

    @Override
    public HippoLock getLock(final String absPath) throws RepositoryException {
        return new LockDecorator(super.getLock(absPath));
    }

    private void setTimeout(final Lock lock, final long timeoutHint) {
        try {
            final Node lockNode = lock.getNode();
            if (timeoutHint != Long.MAX_VALUE) {
                lockNode.addMixin(NT_LOCKABLE);
                final Calendar timeout = Calendar.getInstance();
                final long timeoutTime = System.currentTimeMillis() + timeoutHint * 1000;
                timeout.setTimeInMillis(timeoutTime);
                lockNode.setProperty(HIPPO_LOCKEXPIRATIONTIME, timeout);
            } else {
                if (lockNode.hasProperty(HIPPO_LOCKEXPIRATIONTIME)) {
                    lockNode.getProperty(HIPPO_LOCKEXPIRATIONTIME).remove();
                }
            }
            lockNode.getSession().save();
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.error("Failed to set hippo:timeout on lock", e);
            } else {
                log.error("Failed to set hippo:timeout on lock: {}", e.toString());
            }
        }
    }

    public class LockDecorator implements HippoLock {

        private Lock lock;
        private volatile ScheduledFuture future;
        private final Object monitor = this; // guards future
        private final long timeout;

        private LockDecorator(final Lock lock, final long timeout) {
            this.lock = lock;
            this.timeout = timeout;
        }

        private LockDecorator(final Lock lock) throws RepositoryException {
            this.lock = lock;
            timeout = -1;
        }

        @Override
        public String getLockOwner() {
            return lock.getLockOwner();
        }

        @Override
        public boolean isDeep() {
            return lock.isDeep();
        }

        @Override
        public Node getNode() {
            return lock.getNode();
        }

        @Override
        public String getLockToken() {
            return lock.getLockToken();
        }

        @Override
        public long getSecondsRemaining() throws RepositoryException {
            return lock.getSecondsRemaining();
        }

        @Override
        public boolean isLive() throws RepositoryException {
            return lock.isLive();
        }

        @Override
        public boolean isSessionScoped() {
            return lock.isSessionScoped();
        }

        @Override
        public boolean isLockOwningSession() {
            return lock.isLockOwningSession();
        }

        @Override
        public void refresh() throws LockException, RepositoryException {
            lock.refresh();
            setTimeout(lock, getSecondsRemaining());
        }

        @Override
        public void startKeepAlive() throws RepositoryException {
            if (!isLive()) {
                throw new LockException("Can't start a keep-alive on an unalive lock");
            }
            if (timeout == Long.MAX_VALUE) {
                throw new LockException("Lock has no timeout");
            }
            if (timeout < 10) {
                throw new LockException("Timeout must be at least 10 seconds to start keep-alive, was: " + timeout);
            }
            final long secondsRemaining = getSecondsRemaining();
            long delay = secondsRemaining - 8;
            if (secondsRemaining < 8) {
                refresh();
                delay = timeout - 8;
            }
            future = executor.schedule(new Runnable() {
                @Override
                public void run() {
                    synchronized (monitor) {
                        if (future.isCancelled()) {
                            return;
                        }
                        boolean success = false;
                        try {
                            refresh();
                            success = true;
                        } catch(LockException e) {
                            if (log.isDebugEnabled()) {
                                log.warn("Failed to refresh lock, this might have occurred due to a hiccup; trying to obtain new lock...", e);
                            } else {
                                log.warn("Failed to refresh lock, this might have occurred due to a hiccup; trying to obtain new lock... ({})", e);
                            }
                            try {
                                lock = lockManager.lock(lock.getNode().getPath(), lock.isDeep(), lock.isSessionScoped(), timeout, lock.getLockOwner());
                                setTimeout(lock, timeout);
                                success = true;
                            } catch (RepositoryException e1) {
                                if (log.isDebugEnabled()) {
                                    log.error("Failed to refresh lock", e1);
                                } else {
                                    log.error("Failed to refresh lock: " + e1);
                                }
                            }
                        } catch (RepositoryException e) {
                            if (log.isDebugEnabled()) {
                                log.error("Failed to refresh lock", e);
                            } else {
                                log.error("Failed to refresh lock: " + e);
                            }
                        }
                        if (success) {
                            try {
                                startKeepAlive();
                            } catch (RepositoryException e) {
                                if (log.isDebugEnabled()) {
                                    log.error("Failed to schedule next keep-alive", e);
                                } else {
                                    log.error("Failed to schedule next keep-alive: " + e);
                                }
                            }
                        }
                    }
                }
            }, delay, TimeUnit.SECONDS);
        }

        @Override
        public synchronized void stopKeepAlive() {
            if (future != null) {
                future.cancel(false);
            }
        }

    }
}
