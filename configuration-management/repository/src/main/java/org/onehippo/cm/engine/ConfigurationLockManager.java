/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cm.engine;

import java.util.concurrent.locks.ReentrantLock;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.lock.LockException;

import org.onehippo.repository.locking.HippoLock;
import org.onehippo.repository.locking.HippoLockManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_LOCK;
import static org.hippoecm.repository.util.RepoUtils.getClusterNodeId;
import static org.onehippo.cm.engine.Constants.HCM_LOCK_PATH;
import static org.onehippo.cm.engine.Constants.HCM_ROOT_PATH;

/**
 * The ConfigurationLockManager is used to create and release a persistent {@Link HippoLock} for accessing
 * and modifying the ConfigurationModel in the repository, on lock path {@link Constants#HCM_LOCK_PATH}.
 * <p>
 * Note the persistent lock management will be done with a separate/dedicated Session, impersonated from the Session
 * passed into the constructor, which otherwise will not be used any further.</p>
 * <p>
 * A {@link HippoLock} will be created with a default timeout of 30 seconds, and be kept alive until the lock is
 * explicitly released through {@link #unlock}.</p>
 * <p>
 * The default timeout can be overridden through system parameter <b><code>hcm.lock.timeout</code></b> (seconds).</p>
 * <p>
 * Note that {@link HippoLock} requires a minimum timeout of 10 seconds to be able to keep it alive!</p>
 * <p>
 * When acquiring a {@link HippoLock} fails because of a LockException it will be re-attempted every 500 ms (indefinitely)
 * until it succeeds.</p>
 * </p>
 * <p>
 * The {@link #lock} and {@link #unlock()} methods can be thread reentrant and use an internal {@link ReentrantLock}
 * for the actual creation and releasing of the {@link HippoLock}.</p>
 * <p>
 * Therefore, like with {@link ReentrantLock} the typical usage-pattern is/should be:</p>
 * <pre><code>
 *     lockManager.lock();
 *     try {
 *         get or modify the ConfigurationModel
 *     } finally {
 *         lockManager.unlock();
 *     }
 * </code></pre>
 */
public class ConfigurationLockManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationLockManager.class);

    private static final long LOCK_ATTEMPT_INTERVAL = 500;
    private static final long LOCK_TIMEOUT = Long.getLong("hcm.lock.timeout", 30);

    private final ReentrantLock reentrantLock = new ReentrantLock();
    private final Session lockSession;
    private final String lockOwnerId;
    private final HippoLockManager hippoLockManager;

    private HippoLock hippoLock;

    public ConfigurationLockManager(final Session configurationSession) throws RepositoryException {
        final SimpleCredentials credentials = new SimpleCredentials(configurationSession.getUserID(), new char[]{});
        lockSession = configurationSession.impersonate(credentials);
        lockOwnerId = getClusterNodeId(lockSession);
        hippoLockManager = (HippoLockManager) lockSession.getWorkspace().getLockManager();
    }

    private void ensureIsLockable() throws RepositoryException {
        if (!lockSession.nodeExists(HCM_LOCK_PATH)) {
            lockSession.getNode(HCM_ROOT_PATH).addNode(HIPPO_LOCK, HIPPO_LOCK);
            lockSession.save();
        }
    }

    private boolean unlockHippoLock(final HippoLock lock) throws RepositoryException {
        if (lock != null) {
            try {
                log.debug("Attempting to release lock");
                lock.stopKeepAlive();
                lockSession.refresh(false);
                hippoLockManager.unlock(HCM_LOCK_PATH);
                log.debug("Lock successfully released");
            } catch (LockException e) {
                log.warn("Current session no longer holds a lock");
            } catch (RepositoryException e) {
                log.error("Failed to unlock initialization processor: {}. " +
                        "Lock will time out within {} seconds", e.toString(), LOCK_TIMEOUT);
                return false;
            }
        }
        return true;
    }

    public void lock() throws RepositoryException {
        boolean locked = false;
        reentrantLock.lock();
        try {
            if (hippoLock == null || !hippoLock.isLive()) {
                ensureIsLockable();
                while (true) {
                    log.debug("Attempting to obtain lock");
                    try {
                        hippoLock = hippoLockManager.lock(HCM_LOCK_PATH, false, false, LOCK_TIMEOUT, lockOwnerId);
                        log.debug("Lock successfully obtained");
                        try {
                            hippoLock.startKeepAlive();
                            break;
                        } catch (LockException e) {
                            if (log.isDebugEnabled()) {
                                log.warn("Failed to start lock keep-alive", e);
                            } else {
                                log.warn("Failed to start lock keep-alive: " + e);
                            }
                            throw new RepositoryException(e);
                        }
                    } catch (LockException e) {
                        log.debug("Obtaining lock failed, reattempting in {} ms", LOCK_ATTEMPT_INTERVAL);
                        try {
                            Thread.sleep(LOCK_ATTEMPT_INTERVAL);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
            } else {
                if (!hippoLock.isLive()) {
                    throw new LockException("Lock no longer alive");
                }
            }
            locked = true;
        } finally {
            if (!locked) {
                // In Exception context: don't hold onto the reentrantLock
                reentrantLock.unlock();
            }
        }
    }

    public void unlock() throws RepositoryException {
        reentrantLock.lock();
        try {
            if (reentrantLock.getHoldCount() < 3) {
                if (reentrantLock.getHoldCount() == 2) {
                    if (unlockHippoLock(hippoLock)) {
                        hippoLock = null;
                    }
                } else {
                    // Error: unlock call without balanced lock call
                    // second unlock() call below will result in IllegalMonitorStateException!
                }
            }
        } finally {
            reentrantLock.unlock();
            reentrantLock.unlock();
        }
    }

    public void shutdown() {
        try {
            if (lockSession != null && lockSession.isLive()) {
                unlockHippoLock(hippoLock);
                hippoLock = null;
                lockSession.logout();
            }
        } catch (RepositoryException e) {
            log.warn("Failed to unlock or logout during shutdown", e);
        }
    }
}
