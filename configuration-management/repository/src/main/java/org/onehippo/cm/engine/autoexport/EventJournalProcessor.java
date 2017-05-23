/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.autoexport;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.Event;
import javax.jcr.observation.ObservationManager;

import org.hippoecm.repository.api.RevisionEvent;
import org.hippoecm.repository.api.RevisionEventJournal;
import org.onehippo.cm.ConfigurationService;
import org.onehippo.cm.model.ConfigurationItemCategory;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.ConfigurationNode;
import org.onehippo.cm.model.SnsUtils;

import static org.onehippo.cm.model.Constants.HCM_ROOT_NODE;

public class EventJournalProcessor {

    private static final String[] builtinIgnoredEventPaths = new String[] {
            Constants.SYSTEM_NODETYPES_PATH,
            "/hippo:log",
            "/content/attic",
            "/formdata",
            "/hippo:configuration/hippo:update/hippo:queue",
            "/hippo:configuration/hippo:update/hippo:history",
            "/hippo:configuration/hippo:update/jcr:",
            "/hippo:configuration/hippo:temporary",
            "/hippo:configuration/hippo:modules/brokenlinks",
            "/" + HCM_ROOT_NODE
    };

    private final Configuration configuration;
    private final TreeSet<String> ignoredEventPaths;
    private final ConfigurationService configurationService;
    private final Session eventProcessorSession;
    private final String nodeTypeRegistryLastModifiedPropertyPath;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final long minChangeLogAge = 250;
    private long lastRevision = -1;
    private ChangeLog pendingChangeLog;
    private ChangeLog currentChangeLog;
    private RevisionEventJournal eventJournal;
    private ConfigurationModel currentModel;

    private ScheduledFuture<?> future;
    private volatile boolean taskFailed;
    private final Runnable task = () -> {
        if (!taskFailed) {
            try {
                processEvents();
            } catch (Exception e) {
                taskFailed = true;
                AutoExportModule.log.error(e.getClass().getName() + " : " + e.getMessage(), e);
                if (future != null && (!future.isCancelled() || !future.isDone())) {
                    future.cancel(true);
                }
            }
        }
    };

    public EventJournalProcessor(final ConfigurationService configurationService,
                                 final Configuration configuration, final Set<String> extraIgnoredEventPaths)
            throws RepositoryException {
        this.configurationService = configurationService;
        this.configuration = configuration;

        try {
            this.currentModel = configurationService.loadBaseline();
        } catch (Exception e) {
            // TODO needs better ConfigurationService exception management
            if (e instanceof RepositoryException) {
                throw (RepositoryException)e;
            }
            throw new RepositoryException(e.getMessage(), e);
        }

        ignoredEventPaths = new TreeSet<>(Arrays.asList(builtinIgnoredEventPaths));
        ignoredEventPaths.addAll(extraIgnoredEventPaths);

        final Session moduleSession = configuration.getModuleSession();
        eventProcessorSession =
                moduleSession.impersonate(new SimpleCredentials(moduleSession.getUserID(), "".toCharArray()));
        nodeTypeRegistryLastModifiedPropertyPath = configuration.getModuleConfigPath()
                + "/" + Constants.CONFIG_NTR_LAST_MODIFIED_PROPERTY_NAME;
    }

    public void start() {
        synchronized (executor) {
            if (future == null || future.isCancelled() || future.isDone()) {
                future = executor.scheduleWithFixedDelay(task, 0, minChangeLogAge, TimeUnit.MILLISECONDS);
            }
        }
    }

    public void stop() {
        stop(false);
    }

    private void stop(final boolean mayInterruptIfRunning) {
        synchronized (executor) {
            if (future != null && !future.isCancelled()) {
                future.cancel(mayInterruptIfRunning);
            }
        }
    }

    public void runOnce() {
        synchronized (executor) {
            if (!taskFailed) {
                executor.submit(task);
            }
        }
    }

    public void shutdown() {
        stop(true);
        eventProcessorSession.logout();
    }

    private void processEvents() throws RepositoryException {
        if (eventJournal == null) {
            final ObservationManager observationManager = eventProcessorSession.getWorkspace().getObservationManager();
            eventJournal = (RevisionEventJournal)observationManager.getEventJournal();
            lastRevision = configuration.getLastRevision();
        }
        try {
            eventJournal.skipToRevision(lastRevision);
            int count = 0;
            while (eventJournal.hasNext()) {
                if (currentChangeLog == null) {
                    currentChangeLog = new ChangeLog();
                }
                count++;
                RevisionEvent event = eventJournal.nextEvent();
                lastRevision = event.getRevision();
                if (event.getType() == Event.PERSIST) {
                    continue;
                }
                for (String path : processEvent(event)) {
                    if (AutoExportModule.log.isDebugEnabled()) {
                        final String eventPath = event.getPath();
                        AutoExportModule.log.debug(String.format("event %d: %s under parent: [%s] at: [%s] for user: [%s]",
                                event.getRevision(), Constants.getJCREventTypeName(event.getType()), path,
                                eventPath.startsWith(path) ? eventPath.substring(path.length()) : eventPath,
                                event.getUserID()));
                    }
                }
            }
            if (count > 0) {
                AutoExportModule.log.debug("Read {} events up to {}", count, lastRevision);
            }
            if (currentChangeLog != null && currentChangeLog.hasChanges()) {
                if (pendingChangeLog != null) {
                    pendingChangeLog.getChangedNodePaths().addAll(currentChangeLog.getChangedNodePaths());
                    AutoExportModule.log.debug("Adding new changes to pending changes");
                } else if (isReadyForProcessing(currentChangeLog)) {
                    pendingChangeLog = currentChangeLog;
                }
            }
            if (pendingChangeLog != null) {
                currentChangeLog = null;
                // TODO: dummy
                Object configurationDelta = createConfigurationDelta();
                if (eventJournal.hasNext()) {
                    // rewind
                    processEvents();
                } else {
                    // TODO
                    // configurationService.mergeDelta(configurationDelta);
                    // configuration.setLastRevision(lastRevision);
                }
            }
        } catch (Exception e) {
            // catch all exceptions, not just RepositoryException
            AutoExportModule.log.error("Processing events failed: ", e);
        }
    }

    private boolean isReadyForProcessing(final ChangeLog changeLog) {
        return System.currentTimeMillis() - changeLog.getCreationTime() > minChangeLogAge;
    }

    private Set<String> processEvent(final Event event) {
        HashSet<String> changedNodePaths = new HashSet<>();
        try {
            if (!event.getPath().startsWith(Constants.SYSTEM_NODETYPES_PATH)) {
                switch (event.getType()) {
                    case Event.PROPERTY_ADDED:
                    case Event.PROPERTY_CHANGED:
                    case Event.PROPERTY_REMOVED:
                        if (nodeTypeRegistryLastModifiedPropertyPath.equals(event.getPath())) {
                            if (event.getUserData() != null) {
                                String[] changedNamespacePrefixes = event.getUserData().split("\\|");
                                AutoExportModule.log.debug("detected change event for nodetype prefix(es) [{}]", event.getUserData());
                                for (String changedNamespacePrefix : changedNamespacePrefixes) {
                                    final String path = Constants.SYSTEM_NODETYPES_PATH+changedNamespacePrefix;
                                    currentChangeLog.getChangedNodePaths().add(path);
                                    changedNodePaths.add(path);
                                }
                            }
                        } else {
                            checkAddEventChangedNodePath(event.getPath(), false, false, true, changedNodePaths);
                        }
                        break;
                    case Event.NODE_ADDED:
                        checkAddEventChangedNodePath(event.getPath(), true, false, false, changedNodePaths);
                        break;
                    case Event.NODE_REMOVED:
                        checkAddEventChangedNodePath(event.getPath(), false, true, false, changedNodePaths);
                        break;
                    case Event.NODE_MOVED:
                        final String srcAbsPath = (String)event.getInfo().get("srcAbsPath");
                        if (srcAbsPath != null) {
                            // not an order-before
                            checkAddEventChangedNodePath(event.getPath(), true, false, false, changedNodePaths);
                            checkAddEventChangedNodePath(srcAbsPath, false, true, false, changedNodePaths);
                        } else {
                            checkAddEventChangedNodePath(event.getPath(), false, false, false, changedNodePaths);
                        }
                        break;
                }
            }
        } catch (RepositoryException e) {
            // ignore: return empty set
        }
        return changedNodePaths;
    }

    private void checkAddEventChangedNodePath(final String eventPath, final boolean addedNode, final boolean deletedNode,
                                              final boolean propertyPath, final Set<String> changedNodePaths) {
        if (!overlaps(eventPath, ignoredEventPaths) && !isExcludedInModel(eventPath, propertyPath) &&
                !overlaps(eventPath, currentChangeLog.getAddedNodePaths())) {
            if (addedNode) {
                boolean childPathAddedBefore = removeChildPaths(eventPath, currentChangeLog.getAddedNodePaths());
                currentChangeLog.getAddedNodePaths().add(eventPath);
                if (childPathAddedBefore) {
                    removeChildPaths(eventPath, currentChangeLog.getChangedNodePaths());
                }
                currentChangeLog.getChangedNodePaths().remove(eventPath);
            } else if (deletedNode) {
                removeChildPaths(eventPath, currentChangeLog.getAddedNodePaths());
                currentChangeLog.getAddedNodePaths().remove(eventPath);
                removeChildPaths(eventPath, currentChangeLog.getChangedNodePaths());
                currentChangeLog.getChangedNodePaths().remove(eventPath);
            }
            String parentPath = getParentPath(eventPath);
            if (currentChangeLog.getChangedNodePaths().add(parentPath)) {
                changedNodePaths.add(parentPath);
            }
        }
    }

    private boolean isExcludedInModel(final String eventPath, final boolean propertyPath) {
        String[] segments;
        if ("/".equals(eventPath)) {
            segments = new String[0];
        } else {
            segments = eventPath.substring(1).split("/");
        }
        ConfigurationNode node = currentModel.getConfigurationRootNode();
        for (int i = 0; i < segments.length; i++) {
            if (propertyPath && i == segments.length-1) {
                if (ConfigurationItemCategory.RUNTIME == node.getChildPropertyCategory(segments[i])) {
                    return true;
                } else {
                    return false;
                }
            } else {
                String indexedName = SnsUtils.createIndexedName(segments[i]);
                if (ConfigurationItemCategory.RUNTIME == node.getChildNodeCategory(indexedName)) {
                    return true;
                }
                node = node.getNodes().get(indexedName);
                if (node == null) {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean overlaps(final String path, final SortedSet<String> collectedPaths) {
        for (String collectedPath : collectedPaths) {
            final String collectedChildPath = collectedPath+"/";
            if (path.equals(collectedPath) || path.startsWith(collectedChildPath)) {
                return true;
            } else if ((collectedChildPath).compareTo(path) > 0) {
                return false;
            }
        }
        return false;
    }

    private boolean removeChildPaths(final String path, final SortedSet<String> collectedPaths) {
        final String childPath = path + "/";
        boolean childPathRemoved = false;
        for (Iterator<String> iter = collectedPaths.iterator(); iter.hasNext(); ) {
            final String collectedPath = iter.next();
            if (collectedPath.startsWith(childPath)) {
                childPathRemoved = true;
                iter.remove();
            } else if (collectedPath.compareTo(childPath) > 0) {
                break;
            }
        }
        return childPathRemoved;
    }

    private String getParentPath(String absPath) {
        int end = absPath.lastIndexOf('/');
        return absPath.substring(0, end == 0 ? 1 : end);
    }

    // TODO
    private Object createConfigurationDelta() {
        return null;
    }
}
