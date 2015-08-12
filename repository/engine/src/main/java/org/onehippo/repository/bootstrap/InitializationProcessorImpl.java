/*
 *  Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.bootstrap;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.locking.HippoLock;
import org.onehippo.repository.locking.HippoLockManager;
import org.slf4j.Logger;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_LOCK;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_SEQUENCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_STATUS;
import static org.hippoecm.repository.util.RepoUtils.getClusterNodeId;
import static org.onehippo.repository.bootstrap.Extension.EXTENSION_FILE_NAME;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.INIT_FOLDER_PATH;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.INIT_LOCK_PATH;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.log;

public class InitializationProcessorImpl implements InitializationProcessor {

    private static final long LOCK_ATTEMPT_INTERVAL = 1000 * 5;
    private static final long LOCK_TIMEOUT = Long.getLong("repo.bootstrap.lock.timeout", 30);

    private final static String PENDING_INITIALIZE_ITEMS_QUERY = String.format(
            "SELECT * FROM hipposys:initializeitem " +
            "WHERE %s = 'pending' OR %s = 'reload' ORDER BY %s ASC",
            HIPPO_STATUS, HIPPO_STATUS, HIPPO_SEQUENCE);

    private static final Comparator<Node> initializeItemComparator = new Comparator<Node>() {
        @Override
        public int compare(final Node n1, final Node n2) {
            try {
                final Double s1 = JcrUtils.getDoubleProperty(n1, HIPPO_SEQUENCE, -1.0);
                final Double s2 = JcrUtils.getDoubleProperty(n2, HIPPO_SEQUENCE, -1.0);
                final int result = s1.compareTo(s2);
                if (result != 0) {
                    return result;
                }
                return n1.getName().compareTo(n2.getName());
            } catch (RepositoryException e) {
                log.error("Error comparing initialize item nodes", e);
            }
            return 0;
        }
    };

    private HippoLock lock;

    @Override
    public List<Node> loadExtensions(final Session session) throws RepositoryException, IOException {
        return loadExtensions(session, true);
    }

    @Override
    public List<Node> loadExtension(final Session session, final URL url) throws RepositoryException, IOException {
        log.error("loadExtension no longer functional, noop implementation always returning empty list.");
        return Collections.emptyList();
    }

    @Override
    public List<PostStartupTask> processInitializeItems(Session session) {
        try {
            final List<Node> initializeItems = getItemNodesToBeExecuted(session);
            return doProcessInitializeItems(session, initializeItems);
        } catch (RepositoryException e) {
            log.error("Failed to load pending initialize items", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<PostStartupTask> processInitializeItems(Session session, List<Node> initializeItems) {
        return doProcessInitializeItems(session, initializeItems);
    }

    @Override
    public void setLogger(final Logger logger) {
        logger.warn("Setting the logger on the InitializationProcessor is no longer supported");
    }

    @Override
    public boolean lock(final Session session) throws RepositoryException {
        ensureIsLockable(session);
        final HippoLockManager lockManager = (HippoLockManager) session.getWorkspace().getLockManager();
        while (true) {
            log.debug("Attempting to obtain lock");
            try {
                lock = lockManager.lock(INIT_LOCK_PATH, false, false, LOCK_TIMEOUT, getClusterNodeId(session));
                log.debug("Lock successfully obtained");
                try {
                    lock.startKeepAlive();
                } catch (LockException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to start lock keep-alive", e);
                    } else {
                        log.warn("Failed to start lock keep-alive: " + e);
                    }
                    throw new RepositoryException(e);
                }
                return true;
            } catch (LockException e) {
                log.debug("Obtaining lock failed, reattempting in {} ms", LOCK_ATTEMPT_INTERVAL);
                try {
                    Thread.sleep(LOCK_ATTEMPT_INTERVAL);
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

    @Override
    public void unlock(final Session session) throws RepositoryException {
        final LockManager lockManager = session.getWorkspace().getLockManager();
        try {
            log.debug("Attempting to release lock");
            if (lock != null) {
                lock.stopKeepAlive();
            }
            session.refresh(false);
            lockManager.unlock(INIT_LOCK_PATH);
            log.debug("Lock successfully released");
        } catch (LockException e) {
            log.warn("Current session no longer holds a lock");
        } catch (RepositoryException e) {
            log.error("Failed to unlock initialization processor: {}. " +
                    "Lock will time out within {} seconds", e.toString(), LOCK_TIMEOUT);
        }
    }

    public List<Node> loadExtensions(Session session, boolean markMissingItems) throws IOException, RepositoryException {
        final Set<InitializeItem> reloadItems = new HashSet<>();
        final List<Extension> extensions = scanForExtensions(session);
        final List<InitializeItem> initializeItems = new ArrayList<>();
        final Map<String, String> itemNames = new HashMap<>();
        for (final Extension extension : extensions) {
            for (final InitializeItem initializeItem : extension.load(itemNames)) {
                if (initializeItem.isReload()) {
                    reloadItems.add(initializeItem);
                }
                initializeItems.add(initializeItem);
            }
        }
        if (markMissingItems) {
            markMissingInitializeItems(session, itemNames.keySet());
        }
        markReloadDownstreamItems(session, initializeItems, reloadItems);
        return getItemNodesToBeExecuted(initializeItems);
    }

    private List<PostStartupTask> doProcessInitializeItems(final Session session, final List<Node> initializeItems) {
        Collections.sort(initializeItems, initializeItemComparator);
        final List<PostStartupTask> postStartupTasks = new ArrayList<>();
        try {
            session.refresh(false);
            for (Node initializeItemNode : initializeItems) {
                InitializeItem initializeItem = new InitializeItem(initializeItemNode);
                try {
                    postStartupTasks.addAll(initializeItem.process());
                } catch (RepositoryException e) {
                    if (log.isDebugEnabled()) {
                        log.error("Failed to initialize item {}", initializeItem.getName(), e);
                    } else {
                        log.error("Failed to process initialize item {}: {}", initializeItem.getName(), e.toString());
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getClass().getName() + ": " + e.getMessage(), e);
        }
        return postStartupTasks;
    }

    void markReloadDownstreamItems(final Session session, final List<InitializeItem> initializeItems, final Collection<InitializeItem> reloadItems)
            throws RepositoryException {
        for (InitializeItem reloadItem : reloadItems) {
            for (InitializeItem downstreamItem : resolveDownstreamItems(reloadItem, initializeItems)) {
                if (!downstreamItem.isMissing()) {
                    log.info("Marking item {} pending because downstream from {}", downstreamItem.getName(), reloadItem.getName());
                    downstreamItem.markDownstream(reloadItem);
                }
            }
        }
        session.save();
    }

    Collection<InitializeItem> resolveDownstreamItems(final InitializeItem reloadItem, final List<InitializeItem> initializeItems)
            throws RepositoryException {
        final Collection<InitializeItem> downstreamItems = new ArrayList<>();
        for (InitializeItem initializeItem : initializeItems) {
            if (initializeItem.isDownstreamItem(reloadItem)) {
                downstreamItems.add(initializeItem);
            }
        }
        return downstreamItems;
    }

    private List<Node> getItemNodesToBeExecuted(final List<InitializeItem> initializeItems) throws RepositoryException {
        List<Node> pendingItems = new ArrayList<>();
        for (InitializeItem initializeItem : initializeItems) {
            if (initializeItem.isPending() || initializeItem.isReload()) {
                pendingItems.add(initializeItem.getItemNode());
            }
        }
        return pendingItems;
    }

    private List<Node> getItemNodesToBeExecuted(final Session session) throws RepositoryException {
        final List<Node> initializeItems = new ArrayList<>();
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final Query getInitializeItems = queryManager.createQuery(PENDING_INITIALIZE_ITEMS_QUERY, Query.SQL);
        final NodeIterator nodes = getInitializeItems.execute().getNodes();
        while(nodes.hasNext()) {
            initializeItems.add(nodes.nextNode());
        }
        return initializeItems;
    }

    private void markMissingInitializeItems(final Session session, final Set<String> itemNames) throws RepositoryException {
        final Node initializeFolder = session.getNode(INIT_FOLDER_PATH);
        for (Node item : new NodeIterable(initializeFolder.getNodes())) {
            if (!itemNames.contains(item.getName()) && !item.getName().equals(HIPPO_LOCK)) {
                log.info("Marking missing initialize item {}", item.getName());
                InitializeItem.markMissing(item);
            }
        }
        session.save();
    }

    private List<Extension> scanForExtensions(final Session session) throws IOException {
        final List<Extension> extensions = new LinkedList<>();
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final Enumeration<URL> resources = classLoader.getResources(EXTENSION_FILE_NAME);
        while (resources.hasMoreElements()) {
            extensions.add(new Extension(session, resources.nextElement()));
        }
        return extensions;
    }

    private void ensureIsLockable(final Session session) throws RepositoryException {
        if (!session.nodeExists(INIT_LOCK_PATH)) {
            session.getNode(INIT_FOLDER_PATH).addNode(HIPPO_LOCK, HIPPO_LOCK);
            session.save();
        }
    }
}
