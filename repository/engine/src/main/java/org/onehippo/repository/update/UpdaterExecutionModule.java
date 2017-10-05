/*
 *  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.cms7.services.lock.LockManager;
import org.onehippo.repository.modules.DaemonModule;
import org.onehippo.repository.modules.ProvidesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ProvidesService(types = {NodeUpdaterService.class})
public class UpdaterExecutionModule implements DaemonModule, EventListener {

    private static final Logger log = LoggerFactory.getLogger(UpdaterExecutionModule.class);

    private static final String UPDATE_PATH = "/" + HippoNodeType.CONFIGURATION_PATH + "/hippo:update";
    private static final String UPDATE_QUEUE_PATH = UPDATE_PATH + "/hippo:queue";
    private static final String UPDATE_HISTORY_PATH = UPDATE_PATH + "/hippo:history";

    private final ExecutorService updaterExecutor = Executors.newSingleThreadExecutor();
    private Session session;
    private UpdaterRegistry updaterRegistry;
    private NodeUpdaterService updaterService;
    private LockManager lockManager;
    private volatile ExecuteUpdatersTask task;
    private final Object monitor = new Object();

    @Override
    public void initialize(final Session session) throws RepositoryException {
        this.session = session;
        session.getWorkspace().getObservationManager().addEventListener(this, Event.NODE_ADDED, UPDATE_QUEUE_PATH, false, null, null, false);
        updaterRegistry = new UpdaterRegistry(session.impersonate(new SimpleCredentials("system", new char[]{})));
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

        lockManager = HippoServiceRegistry.getService(LockManager.class);
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

        private UpdaterExecutor updaterExecutor;
        private volatile boolean cancelled;


        private ExecuteUpdatersTask() {
        }

        @Override
        public void run() {
            boolean locked = false;
            try {
                lockManager.lock(UPDATE_PATH);
                locked = true;
                executeUpdatersInQueue();
            } catch (LockException e) {
                log.info("Failed to obtain lock, most likely obtained by other cluster node already", e);
            } finally {
                synchronized (monitor) {
                    task = null;
                }
                if (locked) {
                    lockManager.unlock(UPDATE_PATH);
                }
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
                session = UpdaterExecutionModule.this.session.impersonate(new SimpleCredentials("system", new char[]{}));
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

        public Node getNextUpdaterNodeFromQueue() {
            NodeIterator nodes;
            try {
                nodes = session.getNode(UPDATE_QUEUE_PATH).getNodes();
                if (!nodes.hasNext()) {
                    log.debug("Updater queue is empty. Nothing to execute");
                    return null;
                }
            } catch (PathNotFoundException e) {
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
