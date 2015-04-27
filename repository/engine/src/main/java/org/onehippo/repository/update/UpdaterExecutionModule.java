/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.repository.update;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.locking.HippoLockManager;
import org.onehippo.repository.modules.DaemonModule;
import org.onehippo.repository.modules.ProvidesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ProvidesService(types = { NodeUpdaterService.class })
public class UpdaterExecutionModule implements DaemonModule, EventListener {

    private static final Logger log = LoggerFactory.getLogger(UpdaterExecutionModule.class);

    private static final String UPDATE_PATH = "/" + HippoNodeType.CONFIGURATION_PATH + "/hippo:update";
    private static final String UPDATE_QUEUE_PATH = UPDATE_PATH + "/hippo:queue";
    private static final String UPDATE_HISTORY_PATH = UPDATE_PATH + "/hippo:history";
    private static final long TWO_MINUTES = 60 * 2;
    private static final String DEFAULT_CLUSTER_NODE_ID = "default";

    private final ExecutorService updaterExecutor = Executors.newSingleThreadExecutor();
    private Session session;
    private UpdaterRegistry updaterRegistry;
    private NodeUpdaterService updaterService;
    private volatile ExecuteUpdatersTask task;
    private final Object monitor = new Object();

    @Override
    public void initialize(final Session session) throws RepositoryException {
        this.session = session;
        session.getWorkspace().getObservationManager().addEventListener(this, Event.NODE_ADDED, UPDATE_QUEUE_PATH, false, null, null, false);
        updaterRegistry = new UpdaterRegistry(session.impersonate(new SimpleCredentials("system", new char[] {})));
        updaterRegistry.start();
        updaterService = new NodeUpdaterService() {

            @Override
            public NodeUpdaterResult updateNode(final Node node) {
                String uuid = null;
                try {
                    uuid = node.getIdentifier();
                    final List<NodeUpdateVisitor> updaters = updaterRegistry.getUpdaters(node);
                    if (updaters.size() == 0) {
                        return NodeUpdaterResult.NO_UPDATE_NEEDED;
                    }
                    for (NodeUpdateVisitor updater : updaters) {
                        try {
                            updater.doUpdate(node);
                        } finally {
                            updater.destroy();
                        }
                    }
                    return NodeUpdaterResult.UPDATE_SUCCEEDED;
                } catch (RepositoryException ex) {
                    log.error("Unable to update node " + uuid, ex);
                    return NodeUpdaterResult.UPDATE_FAILED;
                }
            }
        };
        HippoServiceRegistry.registerService(updaterService, NodeUpdaterService.class);
        // check if any updaters are queued and execute them on startup
        runExecuteUpdatersTask();
    }

    @Override
    public void shutdown() {
        try {
            session.getWorkspace().getObservationManager().removeEventListener(this);
        } catch (RepositoryException e) {
            log.error(e.getClass().getName() + ": " + e.getMessage(), e);
        }
        synchronized (monitor) {
            if (task != null) {
                task.cancel();
            }
            updaterExecutor.shutdown();
        }
        try {
            updaterExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
        }
        if (updaterRegistry != null) {
            HippoServiceRegistry.unregisterService(updaterService, NodeUpdaterService.class);
            updaterRegistry.stop();
        }
    }

    @Override
    public void onEvent(final EventIterator events) {
        runExecuteUpdatersTask();
    }

    private void runExecuteUpdatersTask() {
        synchronized (monitor) {
            if (task == null) {
                updaterExecutor.execute(task = new ExecuteUpdatersTask());
            }
        }
    }

    private final class ExecuteUpdatersTask implements Runnable {

        private Lock lock;
        private ScheduledExecutorService keepAliveExecutor;
        private UpdaterExecutor updaterExecutor;
        private volatile boolean cancelled;

        private ExecuteUpdatersTask() {}

        @Override
        public void run() {
            try {
                if (lock()) {
                    startLockKeepAlive();
                    executeUpdatersInQueue();
                }
            } catch (RepositoryException e) {
                log.error("Failed to obtain lock", e);
            } finally {
                synchronized (monitor) {
                    task = null;
                }
                stopLockKeepAlive();
                unlock();
            }
        }

        private void cancel() {
            cancelled = true;
            synchronized (monitor) {
                if (updaterExecutor != null) {
                    updaterExecutor.cancel();
                }
            }
        }

        private boolean lock() throws RepositoryException {
            log.debug("Trying to obtain lock");
            final HippoLockManager lockManager = (HippoLockManager) session.getWorkspace().getLockManager();
            if (!lockManager.isLocked(UPDATE_PATH) || lockManager.expireLock(UPDATE_PATH)) {
                try {
                    lock = lockManager.lock(UPDATE_PATH, false, false, TWO_MINUTES, getClusterNodeId());
                    log.debug("Lock successfully obtained");
                    return true;
                } catch (LockException e) {
                    // happens when other cluster node beat us to it
                    log.debug("Failed to set lock: " + e.getMessage());
                }
            } else {
                log.debug("Already locked");
            }
            return false;
        }

        private void executeUpdatersInQueue() {
            Node updaterNode;
            while (!cancelled && (updaterNode = getNextUpdaterNodeFromQueue()) != null) {
                executeUpdater(updaterNode);
                moveToHistory(updaterNode);
            }
        }

        private void executeUpdater(final Node updaterNode) {
            Session session = null;
            try {
                session = UpdaterExecutionModule.this.session.impersonate(new SimpleCredentials("system", new char[] {}));
                updaterExecutor = new UpdaterExecutor(updaterNode, session);
                updaterExecutor.execute();
            } catch (IOException e) {
                log.error("Could not execute updater: log initialization failed", e);
            } catch (Exception e) {
                log.error(e.getClass().getName() + ": " + e.getMessage(), e);
            } finally {
                if (updaterExecutor != null) {
                    synchronized (monitor) {
                        updaterExecutor.destroy();
                        updaterExecutor = null;
                    }
                }
                if (session != null) {
                    session.logout();
                }
            }
        }

        private void unlock() {
            log.debug("Trying to release lock");
            try {
                final LockManager lockManager = session.getWorkspace().getLockManager();
                if (lockManager.isLocked(UPDATE_PATH)) {
                    final Lock lock = lockManager.getLock(UPDATE_PATH);
                    if (lock.isLockOwningSession()) {
                        lockManager.unlock(UPDATE_PATH);
                        log.debug("Lock successfully released");
                    } else {
                        log.debug("We don't own the lock");
                    }
                } else {
                    log.debug("Not locked");
                }
            } catch (RepositoryException e) {
                log.error("Failed to release lock", e);
            }
        }

        private void moveToHistory(final Node node) {
            try {
                // the updater node was modified externally by the executor
                session.refresh(false);

                final String srcPath = node.getPath();
                final Node history = session.getNode(UPDATE_HISTORY_PATH);
                String name = node.getName();
                int count = 2;
                while (history.hasNode(name)) {
                    name = node.getName() + "-" + count++;
                }
                final String destPath = UPDATE_HISTORY_PATH + "/" + name;
                session.move(srcPath, destPath);
                session.save();
            } catch (RepositoryException e) {
                log.error("Failed to remove updater from queue", e);
            }
        }

        private String getClusterNodeId() {
            String clusteNodeId = session.getRepository().getDescriptor("jackrabbit.cluster.id");
            if (clusteNodeId == null) {
                clusteNodeId = DEFAULT_CLUSTER_NODE_ID;
            }
            return clusteNodeId;
        }

        private void startLockKeepAlive() {
            keepAliveExecutor = Executors.newSingleThreadScheduledExecutor();
            keepAliveExecutor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    refreshLock();
                }
            }, 1l, 1l, TimeUnit.MINUTES);
        }

        private void stopLockKeepAlive() {
            if (keepAliveExecutor != null) {
                keepAliveExecutor.shutdown();
                keepAliveExecutor = null;
            }
        }

        private void refreshLock() {
            try {
                lock.refresh();
                log.debug("Lock successfully refreshed");
            } catch(LockException e) {
                log.error("Failed to refresh lock", e);
                updaterExecutor.cancel();
            } catch (RepositoryException e) {
                log.error("Failed to refresh lock", e);
            }
        }

        public Node getNextUpdaterNodeFromQueue() {
            NodeIterator nodes;
            try {
                nodes = session.getNode(UPDATE_QUEUE_PATH).getNodes();
                if (!nodes.hasNext()) {
                    log.debug("Updater queue is empty. Nothing to execute");
                    return null;
                }
            } catch(PathNotFoundException e) {
                log.debug("No updater queue");
                return null;
            } catch (RepositoryException e) {
                log.error("Failed to get nodes in the updater queue", e);
                return null;
            }
            Node result = null;
            while (nodes.hasNext() && result == null) {
                result = nodes.nextNode();
            }
            return result;
        }
    }

}
