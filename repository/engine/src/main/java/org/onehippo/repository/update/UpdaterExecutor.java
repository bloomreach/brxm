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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.ValueFactory;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.id.NodeId;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.json.JSONObject;

/**
 * Executes an {@link NodeUpdateVisitor}
 */
public class UpdaterExecutor implements EventListener {

    private static final Logger log = LoggerFactory.getLogger(UpdaterExecutor.class);
    private static final int PROPERTY_EVENTS = Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED | Event.PROPERTY_CHANGED;
    private static final long PROGRESS_REPORT_INTERVAL = 500;

    private final Session session;
    private final Session background;
    private final UpdaterInfo updaterInfo;
    private final UpdaterExecutionReport report;
    private volatile boolean cancelled;
    private int lastUpdateCount = 0;

    public UpdaterExecutor(Node updaterNode, final Session session) throws Exception {
        this.session = session;
        this.background = session.impersonate(new SimpleCredentials("system", new char[] {}));
        report = new UpdaterExecutionReport();
        try {
            updaterInfo = new UpdaterInfo(updaterNode);
        } catch (Exception e) {
            // log to report only: client needs to know about this but caller needs to do exception handling
            report.getLogger().error("Cannot run updater: " + e.getClass().getName() + ": " + e.getMessage());
            throw e;
        } finally {
            // we need to get node instance from our own session in order to save changes
            updaterNode = session.getNodeByIdentifier(updaterNode.getIdentifier());
            saveReport(updaterNode);
        }
        session.getWorkspace().getObservationManager().addEventListener(this, PROPERTY_EVENTS, updaterNode.getPath(), false, null, null, true);
    }

    public void execute() {
        String message = "Executing updater " + updaterInfo.getName();
        info(message);
        logEvent(updaterInfo.getMethod(), updaterInfo.getStartedBy(), message);
        final NodeUpdateVisitor updater = updaterInfo.getUpdater();
        try {
            if (updater instanceof BaseNodeUpdateVisitor) {
                ((BaseNodeUpdateVisitor) updater).setLogger(getLogger());
                ((BaseNodeUpdateVisitor) updater).setParametersMap(jsonToParamsMap(updaterInfo.getParameters()));
            }
            updater.initialize(session);
            report.start();
            if (updaterInfo.isRevert()) {
                runRevertVisitor();
            } else {
                runPathVisitor();
                runQueryVisitor();
            }
        } catch (RepositoryException e) {
            error("Unexpected exception while executing updater", e);
        } finally {
            updaterInfo.getUpdater().destroy();
            // log before saving report for last time
            message = "Finished executing updater " + updaterInfo.getName();
            info(message);
            logEvent(updaterInfo.getMethod(), null, message);
            info("Visited " + report.getVisitedCount() + " nodes in total");
            if (report.getVisitedCount() > 0) {
                info("Updated: " + report.getUpdateCount());
                info("Skipped: " + report.getSkippedCount());
                info("Failed: " + report.getFailedCount());
            }
            report.finish();
            try {
                commitBatchIfNeeded();
            } catch (RepositoryException e) {
                // log.error() instead of error() on purpose: report already saved
                log.error(e.getClass().getName() + ": " + e.getMessage(), e);
            }
            report.close();
        }
    }

    public synchronized void cancel() {
        try {
            final Node node = session.getNodeByIdentifier(updaterInfo.getIdentifier());
            final String cancelledBy = JcrUtils.getStringProperty(node, HippoNodeType.HIPPOSYS_CANCELLEDBY, "unknown");
            final String message = "Cancelling execution of updater " + updaterInfo.getName();
            info(message);
            logEvent("cancel", cancelledBy, message);
        } catch (RepositoryException e) {
            error("Failed to log cancel event for " + updaterInfo.getName(), e);
        }
        report.finish();
        cancelled = true;
    }

    public void destroy() {
        try {
            session.getWorkspace().getObservationManager().removeEventListener(this);
        } catch (RepositoryException e) {
            log.error("Failed to remove self as event listener during destroy", e);
        }
        if (background != null) {
            background.logout();
        }
    }

    private void logEvent(String action, String user, String message) {
        final HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus != null) {
            final HippoEvent event = new HippoEvent("repository");
            event.category("updater").user(user).action(action);
            event.timestamp(System.currentTimeMillis()).message(message);
            event.set("query", updaterInfo.getQuery()).set("path", updaterInfo.getPath());
            eventBus.post(event);
        }
    }

    private void runPathVisitor() throws RepositoryException {
        final Node startNode = getStartNode();
        if (startNode != null) {
            final UpdaterPathVisitor visitor = new UpdaterPathVisitor();
            try {
                info("Loading nodes to update");
                startNode.accept(visitor);
                info("Finished loading " + visitor.count() + " nodes to update");
                for (String identifier : visitor.identifiers()) {
                    if (cancelled) {
                        info("Update cancelled");
                        return;
                    }
                    try {
                        Node node = session.getNodeByIdentifier(identifier);
                        executeUpdater(node);
                        commitBatchIfNeeded();
                    } catch (ItemNotFoundException e) {
                        debug("Node no longer exists: " + identifier);
                    }
                }
            } catch (UnsupportedOperationException e) {
                warn("Cannot run updater: not implemented");
            } catch (RepositoryException e) {
                error("Unexpected exception while running updater path visitor", e);
            }
        }
    }

    private Node getStartNode() throws RepositoryException {
        final String startPath = updaterInfo.getPath();
        if (startPath == null) {
            info("No path set. Skipping path visitor.");
            return null;
        }
        if (!session.nodeExists(startPath)) {
            warn("No such start node: " + startPath);
            return null;
        }
        return session.getNode(startPath);
    }

    private void runQueryVisitor() throws RepositoryException {
        for (String identifier : getQueryResult()) {
            if (cancelled) {
                info("Update cancelled");
                return;
            }
            try {
                final Node node = session.getNodeByIdentifier(identifier);
                executeUpdater(node);
                commitBatchIfNeeded();
            } catch (ItemNotFoundException e) {
                debug("Node no longer exists: " + identifier);
            } catch (UnsupportedOperationException e) {
                warn("Cannot run updater: not implemented");
                break;
            }
        }
    }

    private Collection<String> getQueryResult() throws RepositoryException {
        final String query = updaterInfo.getQuery();
        if (query == null) {
            info("No query set. Skipping query visitor.");
            return Collections.emptyList();
        }
        try {
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final Query jcrQuery = queryManager.createQuery(query, updaterInfo.getLanguage());
            final Collection<String> results = new ArrayList<String>();
            info("Loading nodes to update");
            int count = 0;
            for (Node node : new NodeIterable(jcrQuery.execute().getNodes())) {
                results.add(node.getIdentifier());
                if (++count % PROGRESS_REPORT_INTERVAL == 0) {
                    info("Loaded " + count + " nodes");
                }
            }
            info("Finished loading " + count + " nodes to update");
            return results;
        } catch (RepositoryException e) {
            error("Executing query failed: " + e.getClass().getName() + " : " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private void executeUpdater(final Node node) throws RepositoryException {
        final String path = node.getPath();
        boolean updated = false, failed = false;
        if (updaterInfo.isRevert()) {
            try {
                ensureIsCheckedOut(node);
                updated = updaterInfo.getUpdater().undoUpdate(node);
            } catch (UnsupportedOperationException e) {
                throw e;
            } catch (Exception e) {
                error("Reverting " + path + " failed", e);
                failed = true;
                report.failed(path);
            }
        } else {
            try {
                ensureIsCheckedOut(node);
                updated = updaterInfo.getUpdater().doUpdate(node);
            } catch (UnsupportedOperationException e) {
                throw e;
            } catch (Exception e) {
                error("Updating " + path + " failed", e);
                failed = true;
                report.failed(path);
            }
        }
        if (updated) {
            report.updated(path);
        } else if (!failed) {
            report.skipped(path);
        }
    }

    private void runRevertVisitor() throws RepositoryException {
        Iterator<String> updatedNodes = updaterInfo.getUpdatedNodes();
        while (updatedNodes.hasNext()) {
            if (cancelled) {
                info("UndoUpdate cancelled");
                return;
            }
            String path = updatedNodes.next();
            Node node = null;
            try {
                node = session.getNode(path);
            } catch (PathNotFoundException e) {
                debug("Node no longer exists: " + path);
            }
            if (node != null) {
                try {
                    executeUpdater(node);
                } catch (UnsupportedOperationException e) {
                    warn("Cannot run updater: undoUpdate is not implemented");
                    break;
                }
                commitBatchIfNeeded();
            }
        }
    }

    @Override
    public void onEvent(final EventIterator events) {
        while (events.hasNext()) {
            String path = null;
            try {
                path = events.nextEvent().getPath();
                if (path.endsWith(HippoNodeType.HIPPOSYS_CANCELLED)) {
                    handleCancelEvent();
                }
            } catch (RepositoryException e) {
                error("Failed to process event on " + path, e);
            }
        }
    }

    private void handleCancelEvent() {
        try {
            final Node node = session.getNodeByIdentifier(updaterInfo.getIdentifier());
            if (JcrUtils.getBooleanProperty(node, HippoNodeType.HIPPOSYS_CANCELLED, false)) {
                cancel();
            }
        } catch (RepositoryException e) {
            error("Failed to handle cancel event for " + updaterInfo.getName(), e);
        }
    }

    private class UpdaterPathVisitor implements ItemVisitor {

        private final List<NodeId> identifiers = new ArrayList<NodeId>();
        private long count = 0;

        @Override
        public void visit(final Property property) throws RepositoryException {
        }

        @Override
        public void visit(Node node) throws RepositoryException {
            if (cancelled) {
                info("Update cancelled");
                return;
            }
            identifiers.add(new NodeId(node.getIdentifier()));
            visitChildren(node);
            if (++count % PROGRESS_REPORT_INTERVAL == 0) {
                info("Loaded " + count + " nodes");
            }
        }

        private void visitChildren(final Node node) throws RepositoryException {
            NodeIterator children = null;
            try {
                children = node.getNodes();
            } catch (RepositoryException e) {
                if (e.getCause() instanceof ItemNotFoundException) {
                    debug("Node no longer exists: " + node.getIdentifier());
                } else {
                    throw e;
                }
            }
            if (children == null) {
                return;
            }
            for (Node child : new NodeIterable(children)) {
                if (child != null && !isVirtual(child)) {
                    visit(child);
                }
            }
        }

        private long count() {
            return count;
        }

        private Iterable<String> identifiers() {
            return new Iterable<String>() {
                @Override
                public Iterator<String> iterator() {
                    final Iterator<NodeId> iterator = identifiers.iterator();
                    return new Iterator<String>() {
                        @Override
                        public boolean hasNext() {
                            return iterator.hasNext();
                        }

                        @Override
                        public String next() {
                            return iterator.next().toString();
                        }

                        @Override
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
            };
        }

    }

    private void commitBatchIfNeeded() throws RepositoryException {
        final boolean batchCompleted = report.getUpdateCount() != lastUpdateCount && report.getUpdateCount() % updaterInfo.getBatchSize() == 0;
        if (batchCompleted || report.isFinished()) {
            if (updaterInfo.isDryRun()) {
                session.refresh(false);
            } else {
                try {
                    session.save();
                } catch (RepositoryException e) {
                    error("Failed to save session", e);
                    report.batchFailed();
                    session.refresh(false);
                }
            }
            report.startBatch();
            saveReport();
        }
        if (batchCompleted) {
            lastUpdateCount = report.getUpdateCount();
            throttle(updaterInfo.getThrottle());
        }
    }

    private void saveReport() throws RepositoryException {
        saveReport(session.getNodeByIdentifier(updaterInfo.getIdentifier()));
    }

    private void saveReport(final Node node) throws RepositoryException {
        try {
            node.setProperty(HippoNodeType.HIPPOSYS_LOGTAIL, report.getLogTail());
            if (report.isStarted()) {
                node.setProperty(HippoNodeType.HIPPOSYS_STARTTIME, report.getStartTime());
                node.setProperty(HippoNodeType.HIPPOSYS_UPDATEDCOUNT, report.getUpdateCount());
                node.setProperty(HippoNodeType.HIPPOSYS_FAILEDCOUNT, report.getFailedCount());
                node.setProperty(HippoNodeType.HIPPOSYS_SKIPPEDCOUNT, report.getSkippedCount());
            }
            if (report.isFinished()) {
                node.setProperty(HippoNodeType.HIPPOSYS_FINISHTIME, report.getFinishTime());
                setBinaryProperty(node, HippoNodeType.HIPPOSYS_UPDATED, report.getUpdatedFile());
                setBinaryProperty(node, HippoNodeType.HIPPOSYS_FAILED, report.getFailedFile());
                setBinaryProperty(node, HippoNodeType.HIPPOSYS_SKIPPED, report.getSkippedFile());
                setBinaryProperty(node, HippoNodeType.HIPPOSYS_LOG, report.getLogFile());
            }
            session.save();
        } catch (RepositoryException e) {
            log.error("Failed to save report", e);
            session.refresh(false);
        }
    }

    private void setBinaryProperty(Node node, String propertyName, File file) throws RepositoryException {
        final ValueFactory valueFactory = session.getValueFactory();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            final Binary binary = valueFactory.createBinary(fis);
            node.setProperty(propertyName, binary);
        } catch (IOException e) {
            log.error("Failed to save log property", e);
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    /**
     * Checking out of <code>node</code> must be done by a background jcr session as the session doing the upgrade of nodes
     * might already have local changes on the <code>node</code> in which case checking out the node with that session
     * would fail
     */
    private void ensureIsCheckedOut(Node node) throws RepositoryException {
        if (!node.isCheckedOut()) {
            log.debug("Checking out node {}" + node.getPath());
            JcrUtils.ensureIsCheckedOut(background.getNodeByIdentifier(node.getIdentifier()));
        }
    }

    private void throttle(long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException ignore) {}
    }

    private boolean isVirtual(final Node node) throws RepositoryException {
        return node instanceof HippoNode && ((HippoNode) node).isVirtual();
    }

    private void debug(String message) {
        debug(message, null);
    }

    private void debug(String message, Throwable t) {
        if (report.getLogger() != null) {
            report.getLogger().debug(format(message, t));
        }
        if (t != null) {
            log.debug(message, t);
        } else {
            log.debug(message);
        }
    }

    private void info(String message) {
        info(message, null);
    }

    private void info(String message, Throwable t) {
        if (report.getLogger() != null) {
            report.getLogger().info(format(message, t));
        }
        if (t != null) {
            log.info(message, t);
        } else {
            log.info(message);
        }
    }

    private void warn(String message) {
        warn(message, null);
    }

    private void warn(String message, Throwable t) {
        if (report.getLogger() != null) {
            report.getLogger().warn(format(message, t));
        }
        if (t != null) {
            log.warn(message, t);
        } else {
            log.warn(message);
        }
    }

    private void error(String message) {
        error(message, null);
    }

    private void error(String message, Throwable t) {
        if (report.getLogger() != null) {
            report.getLogger().error(format(message, t));
        }
        if (t != null) {
            log.error(message, t);
        } else {
            log.error(message);
        }
    }

    private String format(final String message, final Throwable t) {
        if (t != null) {
            return message + " - " + t.getClass().getName() + ": " + t.getMessage();
        }
        return message;
    }

    private Logger getLogger() {
        if (report.getLogger() != null) {
            return report.getLogger();
        }
        return log;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonToParamsMap(final String paramsInJson) {
        if (StringUtils.isBlank(paramsInJson)) {
            return Collections.emptyMap();
        }
        return JSONObject.fromObject(paramsInJson);
    }
}
