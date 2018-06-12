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
package org.hippoecm.repository.impl;

import java.util.Calendar;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
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
public class LockManagerDecorator implements HippoLockManager, LockManager {

    private static final Logger log = LoggerFactory.getLogger(LockManagerDecorator.class);
    private static final Calendar NO_TIMEOUT = Calendar.getInstance();
    static {
        NO_TIMEOUT.setTimeInMillis(Long.MAX_VALUE);
    }

    private final Session session;
    private final LockManager lockManager;

    private final ScheduledExecutorService executor;

    public LockManagerDecorator(final Session session, final LockManager lockManager) {
        this.session = session;
        this.lockManager = lockManager;
        executor = ((InternalHippoSession) SessionDecorator.unwrap(session)).getExecutor();
    }

    public static LockManager unwrap(LockManager lockManager) {
        if (lockManager instanceof LockManagerDecorator) {
            return ((LockManagerDecorator) lockManager).lockManager;
        }
        return lockManager;
    }

    @Override
    public void addLockToken(final String lockToken) throws LockException, RepositoryException {
        lockManager.addLockToken(lockToken);
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
        final Lock lock = lockManager.lock(absPath, isDeep, isSessionScoped, timeoutHint, ownerInfo);
        setTimeout(lock, timeoutHint);
        return new LockDecorator(lock, timeoutHint);
    }

    @Override
    public HippoLock getLock(final String absPath) throws RepositoryException {
        return new LockDecorator(lockManager.getLock(absPath));
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

    private void setTimeout(final Lock lock, final long timeoutHint) throws RepositoryException {
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
    }

    /**
     * Using jcr locking is deprecated, use {@link org.onehippo.cms7.services.lock.LockManager} instead
     */
    @Deprecated
    public class LockDecorator implements HippoLock {

        private Lock lock;
        private volatile ScheduledFuture future;
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
            synchronized (session) {
                return lock.getLockOwner();
            }
        }

        @Override
        public boolean isDeep() {
            synchronized (session) {
                return lock.isDeep();
            }
        }

        @Override
        public Node getNode() {
            synchronized (session) {
                return lock.getNode();
            }
        }

        @Override
        public String getLockToken() {
            synchronized (session) {
                return lock.getLockToken();
            }
        }

        @Override
        public long getSecondsRemaining() throws RepositoryException {
            synchronized (session) {
                return lock.getSecondsRemaining();
            }
        }

        @Override
        public boolean isLive() throws RepositoryException {
            synchronized (session) {
                return lock.isLive();
            }
        }

        @Override
        public boolean isSessionScoped() {
            synchronized (session) {
                return lock.isSessionScoped();
            }
        }

        @Override
        public boolean isLockOwningSession() {
            synchronized (session) {
                return lock.isLockOwningSession();
            }
        }

        @Override
        public void refresh() throws LockException, RepositoryException {
            synchronized (session) {
                lock.refresh();
                setTimeout(lock, getSecondsRemaining());
            }
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
                    synchronized (session) {
                        if (future.isCancelled()) {
                            return;
                        }
                        boolean success = false;
                        try {
                            refresh();
                            log.debug("Refreshed lock {}", lock.getNode().getPath());
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
                                log.error("Failed to refresh lock", e1);
                            }
                        } catch (RepositoryException e) {
                            log.error("Failed to refresh lock", e);
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
