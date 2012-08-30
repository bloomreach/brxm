/*
 *  Copyright 2012 Hippo.
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

import javax.jcr.Binary;
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
import javax.jcr.query.QueryResult;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.commons.iterator.NodeIterable;
import org.codehaus.groovy.control.CompilationFailedException;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes an {@link Updater}
 */
public class UpdaterExecutor implements EventListener {

    private static final Logger log = LoggerFactory.getLogger(UpdaterExecutor.class);
    private static final int PROPERTY_EVENTS = Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED | Event.PROPERTY_CHANGED;

    private final Session session;
    private final UpdaterInfo updaterInfo;
    private final UpdaterExecutionReport report;
    private volatile boolean cancelled;

    public UpdaterExecutor(final Node updaterNode, final Session session) throws RepositoryException, IllegalAccessException, InstantiationException, ClassNotFoundException, IOException, IllegalArgumentException, CompilationFailedException {
        this.session = session;
        report = new UpdaterExecutionReport();
        try {
            updaterInfo = new UpdaterInfo(updaterNode);
        } catch (RepositoryException e) {
            // log to report only: client needs to know about this but caller needs to do exception handling
            report.getLogger().error("Cannot run updater: " + e.getClass().getName() + ": " + e.getMessage());
            throw e;
        } catch (IllegalAccessException e) {
            report.getLogger().error("Cannot run updater: " + e.getClass().getName() + ": " + e.getMessage());
            throw e;
        } catch (InstantiationException e) {
            report.getLogger().error("Cannot run updater: " + e.getClass().getName() + ": " + e.getMessage());
            throw e;
        } catch (ClassNotFoundException e) {
            report.getLogger().error("Cannot run updater: " + e.getClass().getName() + ": " + e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            report.getLogger().error("Cannot run updater: " + e.getClass().getName() + ": " + e.getMessage());
            throw e;
        } catch (CompilationFailedException e) {
            report.getLogger().error("Cannot run updater: " + e.getClass().getName() + ": " + e.getMessage());
            throw e;
        } finally {
            saveReport(updaterNode);
        }
        session.getWorkspace().getObservationManager().addEventListener(this, PROPERTY_EVENTS, updaterNode.getPath(), false, null, null, true);
    }

    public void execute() {
        String message = "Executing updater " + updaterInfo.getName();
        info(message);
        logEvent(updaterInfo.getMethod(), updaterInfo.getStartedBy(), message);
        final Updater updater = updaterInfo.getUpdater();
        try {
            if (updater instanceof BaseUpdater) {
                ((BaseUpdater) updater).setLogger(getLogger());
            }
            updater.initialize(session);
            report.start();
            runPathVisitor();
            runQueryVisitor();
        } catch (RepositoryException e) {
            error("Unexpected exception while executing updater", e);
        } finally {
            // log before saving report for last time
            message = "Finished executing updater " + updaterInfo.getName();
            info(message);
            logEvent(updaterInfo.getMethod(), null, message);
            report.finish();
            try {
                commitBatchIfNeeded();
            } catch (RepositoryException e) {
                // log.error() instead of error() on purpose: report already saved
                log.error(e.getClass().getName() + ": " + e.getMessage(), e);
            }
            updaterInfo.getUpdater().destroy();
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
        if (session != null) {
            try {
                session.getWorkspace().getObservationManager().removeEventListener(this);
            } catch (RepositoryException e) {
                log.error("Failed to remove self as event listener during destroy", e);
            }
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
                startNode.accept(visitor);
            } catch (UnsupportedOperationException e) {
                warn("Cannot run updater: " + updaterInfo.getMethod() + " is not implemented");
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
        final NodeIterator nodes = getQueryResultIterator();
        if (nodes != null) {
            while (nodes.hasNext()) {
                if (cancelled) {
                    info("Update cancelled");
                    return;
                }
                final Node node = nodes.nextNode();
                if (node != null) {
                    try {
                        executeUpdater(node);
                    } catch (UnsupportedOperationException e) {
                        warn("Cannot run updater: " + (updaterInfo.isRevert() ? "revert" : "update") + " is not implemented");
                        break;
                    }
                    commitBatchIfNeeded();
                }
            }
        }
    }

    private NodeIterator getQueryResultIterator() throws RepositoryException {
        final String query = updaterInfo.getQuery();
        if (query == null) {
            info("No query set. Skipping query visitor.");
            return null;
        }
        QueryResult results;
        try {
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final Query jcrQuery = queryManager.createQuery(query, updaterInfo.getLanguage());
            results = jcrQuery.execute();
        } catch (RepositoryException e) {
            error("Executing query failed: " + e.getClass().getName() + " : " + e.getMessage());
            return null;
        }

        return results.getNodes();
    }

    private void executeUpdater(final Node node) throws RepositoryException {
        final String path = node.getPath();
        boolean updated = false, failed = false;
        if (updaterInfo.isRevert()) {
            try {
                updated = updaterInfo.getUpdater().revert(node);
            } catch (UnsupportedOperationException e) {
                throw e;
            } catch (Exception e) {
                error("Reverting " + path + " failed", e);
                failed = true;
                report.failed(path);
            }
        } else {
            try {
                updated = updaterInfo.getUpdater().update(node);
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

        @Override
        public void visit(final Property property) throws RepositoryException {
        }

        @Override
        public void visit(Node node) throws RepositoryException {
            if (cancelled) {
                info("Update cancelled");
                return;
            }
            final String path = node.getPath();
            executeUpdater(node);
            commitBatchIfNeeded();
            visitChildren(path);
        }

        private void visitChildren(final String path) throws RepositoryException {
            Node node;
            try {
                // updater might have removed the node, fetch it anew
                node = session.getNode(path);
            } catch (PathNotFoundException e) {
                return;
            }
            for (Node child : new NodeIterable(node.getNodes())) {
                if (child != null && !isVirtual(child)) {
                    visit(child);
                }
            }
        }
    }

    private void commitBatchIfNeeded() throws RepositoryException {
        final boolean batchCompleted = report.getUpdateCount() % updaterInfo.getBatchSize() == 0;
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
            saveReport(session.getNodeByIdentifier(updaterInfo.getIdentifier()));
        }

        throttle(updaterInfo.getThrottle());
    }

    private void saveReport(final Node node) throws RepositoryException {
        try {
            node.setProperty(HippoNodeType.HIPPOSYS_STARTTIME, report.getStartTime());
            node.setProperty(HippoNodeType.HIPPOSYS_UPDATEDCOUNT, report.getUpdateCount());
            node.setProperty(HippoNodeType.HIPPOSYS_FAILEDCOUNT, report.getFailedCount());
            node.setProperty(HippoNodeType.HIPPOSYS_SKIPPEDCOUNT, report.getSkippedCount());
            node.setProperty(HippoNodeType.HIPPOSYS_LOGTAIL, report.getLogTail());
            if (report.isFinished()) {
                node.setProperty(HippoNodeType.HIPPOSYS_FINISHTIME, report.getFinishTime());
                setBinaryProperty(node, HippoNodeType.HIPPOSYS_UPDATED, report.getUpdatedFile());
                setBinaryProperty(node, HippoNodeType.HIPPOSYS_FAILED, report.getFailedFile());
                setBinaryProperty(node, HippoNodeType.HIPPOSYS_FAILED, report.getSkippedFile());
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
            log.debug(message);
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

}
