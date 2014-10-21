/*
 *  Copyright 2012-2014 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTDELETE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTPROPDELETE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTPROPADD;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTPROPSET;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTRESOURCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTROOT;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTEXTPATHS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_SEQUENCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_STATUS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_TIMESTAMP;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_UPSTREAMITEMS;
import static org.hippoecm.repository.util.RepoUtils.getClusterNodeId;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.INIT_FOLDER_PATH;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.log;
import static org.onehippo.repository.util.JcrConstants.MIX_LOCKABLE;

public class InitializationProcessorImpl implements InitializationProcessor {

    private static final long LOCK_TIMEOUT = Long.getLong("repo.bootstrap.lock.timeout", 60 * 5);
    private static final long LOCK_ATTEMPT_INTERVAL = 1000 * 2;

    private final static String PENDING_INITIALIZE_ITEMS_QUERY = String.format(
            "SELECT * FROM hipposys:initializeitem " +
            "WHERE %s = 'pending' ORDER BY %s ASC", HIPPO_STATUS, HIPPO_SEQUENCE);

    private final static String MISSING_INITIALIZE_ITEMS_QUERY = String.format(
            "SELECT * FROM hipposys:initializeitem " +
            "WHERE %s IS NULL OR %s < %%s", HIPPO_TIMESTAMP, HIPPO_TIMESTAMP);

    private static final String DOWNSTREAM_CONTENT_RESOURCE_QUERY = String.format(
            "SELECT * FROM hipposys:initializeitem " +
                    "WHERE (%s LIKE '$contextPath/%%' OR %s = '$contextPath') AND %s IS NOT NULL AND %s <> 'missing'",
            HIPPO_CONTEXTPATHS, HIPPO_CONTEXTPATHS, HIPPO_CONTENTRESOURCE, HIPPO_STATUS);

    private static final String DOWNSTREAM_CONTENT_PROPSET_PROPADD_QUERY = String.format(
            "SELECT * FROM hipposys:initializeitem WHERE " +
             "(%s LIKE '$contextPath/%%' OR %s = '$contextPath') AND (%s IS NOT NULL OR %s IS NOT NULL) AND %s <> 'missing'",
            HIPPO_CONTENTROOT, HIPPO_CONTENTROOT, HIPPO_CONTENTPROPSET, HIPPO_CONTENTPROPADD, HIPPO_STATUS);

    private static final String DOWNSTREAM_CONTENTDELETE_CONTENTPROPDELETE_QUERY = String.format(
            "SELECT * FROM hipposys:initializeitem WHERE " +
             "(%s LIKE '$contextPath/%%' OR %s ='$contextPath' OR %s LIKE '$contextPath/%%') AND %s <> 'missing'",
            HIPPO_CONTENTDELETE, HIPPO_CONTENTDELETE, HIPPO_CONTENTPROPDELETE, HIPPO_STATUS
    );

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


    public InitializationProcessorImpl() {}

    @Override
    public List<Node> loadExtensions(final Session session) throws RepositoryException, IOException {
        return loadExtensions(session, true);
    }

    public List<Node> loadExtensions(Session session, boolean markMissingItems) throws IOException, RepositoryException {
        final Set<String> reloadItems = new HashSet<>();
        final long now = System.currentTimeMillis();
        final List<Extension> extensions = scanForExtensions(session);
        final List<Node> initializeItems = new ArrayList<>();
        for (final Extension extension : extensions) {
            extension.load();
            for (final InitializeItem initializeItem : extension.getInitializeItems()) {
                if (initializeItem.isReload()) {
                    reloadItems.add(initializeItem.getItemNode().getIdentifier());
                }
                initializeItems.add(initializeItem.getItemNode());
            }
        }
        if (markMissingItems) {
            markMissingInitializeItems(session, now);
        }
        initializeItems.addAll(markReloadDownstreamItems(session, reloadItems));
        return initializeItems;
    }

    @Override
    public List<Node> loadExtension(final Session session, final URL url) throws RepositoryException, IOException {
        log.error("loadExtension no longer functional, noop implementation always returning empty list.");
        return Collections.emptyList();
    }

    @Override
    public List<PostStartupTask> processInitializeItems(Session session) {
        try {
            final List<Node> initializeItems = getPendingInitializeItemNodes(session);
            return doProcessInitializeItems(session, initializeItems);
        } catch (RepositoryException e) {
            log.error("Failed to load pending initialize items", e);
            return Collections.emptyList();
        }
    }

    private List<Node> getPendingInitializeItemNodes(final Session session) throws RepositoryException {
        final List<Node> initializeItems = new ArrayList<>();
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final Query getInitializeItems = queryManager.createQuery(PENDING_INITIALIZE_ITEMS_QUERY, Query.SQL);
        final NodeIterator nodes = getInitializeItems.execute().getNodes();
        while(nodes.hasNext()) {
            initializeItems.add(nodes.nextNode());
        }
        return initializeItems;
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
        ensureIsLockable(session, INIT_FOLDER_PATH);
        final LockManager lockManager = session.getWorkspace().getLockManager();
        final long t1 = System.currentTimeMillis();
        while (true) {
            log.debug("Attempting to obtain lock");
            try {
                lockManager.lock(INIT_FOLDER_PATH, false, false, LOCK_TIMEOUT, getClusterNodeId(session));
                log.debug("Lock successfully obtained");
                return true;
            } catch (LockException e) {
                if (System.currentTimeMillis() - t1 < LOCK_TIMEOUT * 1000) {
                    log.debug("Obtaining lock failed, reattempting in {} ms", LOCK_ATTEMPT_INTERVAL);
                    try {
                        Thread.sleep(LOCK_ATTEMPT_INTERVAL);
                    } catch (InterruptedException ignore) {
                    }
                } else {
                    return false;
                }
            }
        }
    }

    @Override
    public void unlock(final Session session) throws RepositoryException {
        final LockManager lockManager = session.getWorkspace().getLockManager();
        try {
            log.debug("Attempting to release lock");
            session.refresh(false);
            lockManager.unlock(INIT_FOLDER_PATH);
            log.debug("Lock successfully released");
        } catch (LockException e) {
            log.warn("Current session no longer holds a lock, please set a longer repo.bootstrap.lock.timeout");
        }
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

    List<Node> markReloadDownstreamItems(final Session session, final Set<String> reloadItems) throws RepositoryException {
        List<Node> initializeItems = new ArrayList<>();
        for (String reloadItem : reloadItems) {
            final Node initItemNode = session.getNodeByIdentifier(reloadItem);
            for (Node downStreamItem : resolveDownstreamItems(session, initItemNode)) {
                log.info("Marking item {} pending because downstream from {}", new Object[] { downStreamItem.getName(), initItemNode.getName() });
                downStreamItem.setProperty(HIPPO_STATUS, "pending");
                Value[] upstreamItems;
                if (downStreamItem.hasProperty(HIPPO_UPSTREAMITEMS)) {
                    List<Value> values = new ArrayList<>(Arrays.asList(downStreamItem.getProperty(HIPPO_UPSTREAMITEMS).getValues()));
                    values.add(session.getValueFactory().createValue(reloadItem));
                    upstreamItems = values.toArray(new Value[values.size()]);
                } else {
                    upstreamItems = new Value[] { session.getValueFactory().createValue(reloadItem) };
                }
                downStreamItem.setProperty(HIPPO_UPSTREAMITEMS, upstreamItems);
                initializeItems.add(downStreamItem);
            }
        }
        session.save();
        return initializeItems;
    }

    private void markMissingInitializeItems(final Session session, final long markBefore) throws RepositoryException {
        try {
            final String statement = String.format(MISSING_INITIALIZE_ITEMS_QUERY, String.valueOf(markBefore));
            final Query query = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL);
            for (Node node : new NodeIterable(query.execute().getNodes())) {
                if (node != null) {
                    log.info("Marking missing initialize item {}", node.getName());
                    node.setProperty(HIPPO_STATUS, "missing");
                }
            }
            session.save();
        } catch (RepositoryException e) {
            log.error("Exception occurred while marking missing initialize items", e);
            session.refresh(false);
        }
    }

    private List<Extension> scanForExtensions(final Session session) throws IOException {
        final List<Extension> extensions = new LinkedList<>();
        final Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources("hippoecm-extension.xml");
        while (resources.hasMoreElements()) {
            extensions.add(new Extension(session, resources.nextElement()));
        }
        return extensions;
    }

    List<Node> resolveDownstreamItems(final Session session, final Node upstreamItem) throws RepositoryException {
        final List<Node> downStreamItems = new ArrayList<>();
        final String[] contextPaths = JcrUtils.getMultipleStringProperty(upstreamItem, HIPPO_CONTEXTPATHS, null);
        if (contextPaths != null && contextPaths.length > 0) {
            // First contextPath is the 'root' content path of the content resource, possible additional contextPaths
            // (in case of multiple delta imports) are always children so do no need to be checked for downstream items.
            final String contextPath = contextPaths[0];
            downStreamItems.addAll(resolveDownstreamItems(session, contextPath, upstreamItem, DOWNSTREAM_CONTENT_RESOURCE_QUERY));
            downStreamItems.addAll(resolveDownstreamItems(session, contextPath, upstreamItem, DOWNSTREAM_CONTENT_PROPSET_PROPADD_QUERY));
            downStreamItems.addAll(resolveDownstreamItems(session, contextPath, upstreamItem, DOWNSTREAM_CONTENTDELETE_CONTENTPROPDELETE_QUERY));
        }
        return downStreamItems;
    }

    private List<Node> resolveDownstreamItems(final Session session, final String contextPath, final Node upstreamItem, final String statement) throws RepositoryException {
        final List<Node> downStreamItems = new ArrayList<>();
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final Query query = queryManager.createQuery(statement, Query.SQL);
        query.bindValue("contextPath", session.getValueFactory().createValue(contextPath));
        final QueryResult result = query.execute();
        for (Node item : new NodeIterable(result.getNodes())) {
            if (!upstreamItem.isSame(item)) {
                downStreamItems.add(item);
            }
        }
        return downStreamItems;
    }


    private void ensureIsLockable(final Session session, final String absPath) throws RepositoryException {
        final Node node = session.getNode(absPath);
        if (!node.isNodeType(MIX_LOCKABLE)) {
            node.addMixin(MIX_LOCKABLE);
            session.save();
        }
    }
}
