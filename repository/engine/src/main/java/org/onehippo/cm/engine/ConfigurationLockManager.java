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

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.lock.LockManager;
import org.onehippo.cms7.services.lock.LockManagerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.engine.Constants.HCM_ROOT_PATH;

/**
 * The ConfigurationLockManager is used to lock and unlock for accessing
 * and modifying the ConfigurationModel in the repository, on LOCK_PATH.
 * <p>
 * The {@link #lock} and {@link #unlock()} methods can be thread reentrant</p>
 * <p>
 * Therefore the typical usage-pattern is/should be:</p>
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
    public static final String LOCK_PATH = HCM_ROOT_PATH;

    private final ReentrantLock reentrantLock = new ReentrantLock();
    private final LockManager lockManager;
    private boolean locked;

    public ConfigurationLockManager() {
        lockManager = HippoServiceRegistry.getService(LockManager.class);
    }

    public void lock() throws RepositoryException {
        reentrantLock.lock();
        if (!locked) {
            try {
                LockManagerUtils.waitForLock(lockManager, LOCK_PATH, LOCK_ATTEMPT_INTERVAL);
                locked = true;
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new RepositoryException(e);
            } finally {
                if (!locked) {
                    // In Exception context: don't hold on to the reentrantLock
                    reentrantLock.unlock();
                }
            }
        }
    }

    public void unlock() throws RepositoryException {
        reentrantLock.lock();
        try {
            if (reentrantLock.getHoldCount() < 3) {
                if (reentrantLock.getHoldCount() == 2) {
                    lockManager.unlock(LOCK_PATH);
                    locked = false;
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

    public void stop() {
        // gracefully wait for possible running locked thread to finish
        reentrantLock.lock();
        reentrantLock.unlock();
    }
}
