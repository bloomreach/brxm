/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.repository.upgrade;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;

import org.onehippo.repository.modules.ConfigurableDaemonModule;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HandleMigrationModule implements ConfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(HandleMigrationModule.class);

    private String moduleConfigPath;
    private HandleMigrator migrator;
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private Future migrationFuture;
    private Future keepAliveFuture;

    @Override
    public void configure(final Node moduleConfig) throws RepositoryException {
        moduleConfigPath = moduleConfig.getPath();
    }

    @Override
    public void initialize(final Session session) throws RepositoryException {
        final Session lockSession = session.impersonate(new SimpleCredentials("system", new char[] {}));
        migrationFuture = executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (lock(lockSession, moduleConfigPath)) {
                        startLockKeepAlive(lockSession);
                        migrator = new HandleMigrator(session);
                        migrator.migrate();
                        migrator.shutdown();
                        migrator = null;
                    }
                } finally {
                    stopLockKeepAlive();
                    unlock(lockSession, moduleConfigPath);
                }
            }
        });
    }

    @Override
    public void shutdown() {
        if (migrator != null) {
            migrator.cancel();
        }
        if (migrationFuture != null && !migrationFuture.isDone()) {
            migrationFuture.cancel(true);
        }
        executorService.shutdown();
    }

    private boolean lock(Session session, String nodePath) {
        log.debug("Trying to obtain lock on " + nodePath);
        try {
            final LockManager lockManager = session.getWorkspace().getLockManager();
            if (!lockManager.isLocked(nodePath)) {
                try {
                    ensureIsLockable(session, nodePath);
                    lockManager.lock(nodePath, false, false, 60*2, getClusterNodeId(session));
                    log.debug("Lock successfully obtained on " + nodePath);
                    return true;
                } catch (LockException e) {
                    // happens when other cluster node beat us to it
                    log.debug("Failed to set lock on "  + nodePath +  ": " + e.getMessage());
                }
            } else {
                log.debug("Already locked " + nodePath);
            }
        } catch (RepositoryException e) {
            log.error("Failed to set lock on " + nodePath);
        }
        return false;
    }

    private void unlock(Session session, String nodePath) {
        log.debug("Trying to release lock on " + nodePath);
        try {
            final LockManager lockManager = session.getWorkspace().getLockManager();
            if (lockManager.isLocked(nodePath)) {
                final Lock lock = lockManager.getLock(nodePath);
                if (lock.isLockOwningSession()) {
                    lockManager.unlock(nodePath);
                    log.debug("Lock successfully released on " + nodePath);
                } else {
                    log.debug("We don't own the lock on " + nodePath);
                }
            } else {
                log.debug("Not locked " + nodePath);
            }
        } catch (RepositoryException e) {
            log.error("Failed to release lock on " + nodePath, e);
        }
    }

    private static void ensureIsLockable(Session session, String nodePath) throws RepositoryException {
        final Node node = session.getNode(nodePath);
        if (!node.isNodeType(JcrConstants.MIX_LOCKABLE)) {
            node.addMixin(JcrConstants.MIX_LOCKABLE);
        }
        session.save();
    }

    private static String getClusterNodeId(Session session) {
        String clusteNodeId = session.getRepository().getDescriptor("jackrabbit.cluster.id");
        if (clusteNodeId == null) {
            clusteNodeId = "default";
        }
        return clusteNodeId;
    }

    private void refreshLock(final Session session) {
        try {
            final LockManager lockManager = session.getWorkspace().getLockManager();
            final Lock lock = lockManager.getLock(moduleConfigPath);
            lock.refresh();
            log.debug("Lock successfully refreshed");
        } catch (RepositoryException e) {
            log.error("Failed to refresh lock", e);
        }
     }

    private void startLockKeepAlive(final Session session) {
        keepAliveFuture = executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                refreshLock(session);
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    private void stopLockKeepAlive() {
        if (keepAliveFuture != null) {
            keepAliveFuture.cancel(true);
        }
    }

}
