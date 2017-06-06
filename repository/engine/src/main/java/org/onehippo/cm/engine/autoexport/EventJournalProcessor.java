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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.Event;
import javax.jcr.observation.ObservationManager;

import org.hippoecm.repository.api.RevisionEvent;
import org.hippoecm.repository.api.RevisionEventJournal;
import org.onehippo.cm.ConfigurationService;
import org.onehippo.cm.engine.ConfigurationServiceImpl;
import org.onehippo.cm.engine.JcrContentProcessingService;
import org.onehippo.cm.engine.JcrResourceInputProvider;
import org.onehippo.cm.engine.ValueProcessor;
import org.onehippo.cm.model.ConfigurationItemCategory;
import org.onehippo.cm.model.ValueType;
import org.onehippo.cm.model.impl.ConfigSourceImpl;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.NamespaceDefinitionImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.ValueImpl;
import org.onehippo.cm.model.util.ConfigurationModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.engine.Constants.HCM_ROOT;

public class EventJournalProcessor {

    static final Logger log = LoggerFactory.getLogger(EventJournalProcessor.class);

    private static final String[] builtinIgnoredEventPaths = new String[] {
            "/hippo:log",
            "/content/attic",
            "/formdata",
            "/hippo:configuration/hippo:update/hippo:queue",
            "/hippo:configuration/hippo:update/hippo:history",
            "/hippo:configuration/hippo:update/jcr:",
            "/hippo:configuration/hippo:temporary",
            "/hippo:configuration/hippo:modules/brokenlinks",
            "/" + HCM_ROOT
    };

    private static final int MAX_REPEAT_PROCESS_EVENTS = 3;

    /**
     * Track changed jcr paths of *parent* nodes of jcr events
     */
    protected static class Changes {
        private Set<String> changedNsPrefixes = new HashSet<>();
        private TreeSet<String> addedConfig = new TreeSet<>();
        private TreeSet<String> changedConfig = new TreeSet<>();
        private TreeSet<String> addedContent = new TreeSet<>();
        private TreeSet<String> changedContent = new TreeSet<>();
        private TreeSet<String> deletedContent = new TreeSet<>();
        private long creationTime;

        private Changes() {
            this.creationTime = System.currentTimeMillis();
        }

        private boolean isEmpty() {
            return changedNsPrefixes.isEmpty() && changedConfig.isEmpty() && changedContent.isEmpty() && deletedContent.isEmpty();
        }

        protected Set<String> getChangedNsPrefixes() {
            return changedNsPrefixes;
        }

        protected TreeSet<String> getAddedConfig() {
            return addedConfig;
        }

        protected TreeSet<String> getChangedConfig() {
            return changedConfig;
        }

        protected TreeSet<String> getAddedContent() {
            return addedContent;
        }

        protected TreeSet<String> getChangedContent() {
            return changedContent;
        }

        protected TreeSet<String> getDeletedContent() {
            return deletedContent;
        }

        protected long getCreationTime() {
            return creationTime;
        }

        protected void addCurrentChanges(Changes currentChanges) {
            changedNsPrefixes.addAll(currentChanges.getChangedNsPrefixes());
            changedConfig.addAll(currentChanges.getChangedConfig());
            changedContent.addAll(currentChanges.getChangedContent());
            deletedContent.addAll(currentChanges.getDeletedContent());
        }
    }

    private final Configuration configuration;
    private final TreeSet<String> ignoredEventPaths;
    private final ConfigurationService configurationService;
    private final Session eventProcessorSession;
    private final String nodeTypeRegistryLastModifiedPropertyPath;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final long minChangeLogAge = 250;
    private long lastRevision = -1;
    private Changes pendingChanges;
    private Changes currentChanges;
    private RevisionEventJournal eventJournal;
    private ConfigurationModelImpl currentModel;
    private PatternSet exclusionContext;

    private ScheduledFuture<?> future;
    private volatile boolean taskFailed;
    private final Runnable task = () -> {
        if (!taskFailed) {
            try {
                tryProcessEvents();
            } catch (Exception e) {
                taskFailed = true;
                AutoExportServiceImpl.log.error(e.getClass().getName() + " : " + e.getMessage(), e);
                if (future != null && (!future.isCancelled() || !future.isDone())) {
                    future.cancel(true);
                }
            }
        }
    };

    public EventJournalProcessor(final ConfigurationServiceImpl configurationService,
                                 final Configuration configuration, final Set<String> extraIgnoredEventPaths)
            throws RepositoryException {
        this.configurationService = configurationService;
        this.configuration = configuration;
        this.currentModel = configurationService.getRuntimeConfigurationModel();
        ignoredEventPaths = new TreeSet<>(Arrays.asList(builtinIgnoredEventPaths));
        ignoredEventPaths.addAll(extraIgnoredEventPaths);

        final Session moduleSession = configuration.getModuleSession();
        eventProcessorSession =
                moduleSession.impersonate(new SimpleCredentials(moduleSession.getUserID(), "".toCharArray()));
        nodeTypeRegistryLastModifiedPropertyPath = configuration.getModuleConfigPath()
                + "/" + Constants.CONFIG_NTR_LAST_MODIFIED_PROPERTY_NAME;
    }

    public void start() {
        exclusionContext = configuration.getExclusionContext();
        // TODO: should we 'refresh' the currentModel here as well?
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

    private void tryProcessEvents() throws RepositoryException {
        // try processEvents max MAX_REPEAT_PROCESS_EVENTS in a row until success (for one task run)
        for (int i = 0; i < MAX_REPEAT_PROCESS_EVENTS; i++) {
            if (processEvents()) {
                break;
            }
            // processEvents unsuccessful: new events arrived before it could export already collected changes
        }
    }

    private boolean processEvents() throws RepositoryException {
        if (eventJournal == null) {
            final ObservationManager observationManager = eventProcessorSession.getWorkspace().getObservationManager();
            eventJournal = (RevisionEventJournal)observationManager.getEventJournal();
            lastRevision = configuration.getLastRevision();
        }
        try {
            eventJournal.skipToRevision(lastRevision);
            int count = 0;
            while (eventJournal.hasNext()) {
                if (currentChanges == null) {
                    currentChanges = new Changes();
                }
                count++;
                RevisionEvent event = eventJournal.nextEvent();
                lastRevision = event.getRevision();
                if (event.getType() == Event.PERSIST) {
                    continue;
                }
                processEvent(event);
            }
            if (count > 0) {
                AutoExportServiceImpl.log.debug("Read {} events up to {}", count, lastRevision);
            }
            if (currentChanges != null && !currentChanges.isEmpty()) {
                if (pendingChanges != null) {
                    pendingChanges.addCurrentChanges(currentChanges);
                    AutoExportServiceImpl.log.debug("Adding new changes to pending changes");
                } else if (isReadyForProcessing(currentChanges)) {
                    pendingChanges = currentChanges;
                }
            }
            if (pendingChanges != null) {
                currentChanges = null;
                ModuleImpl changesModule = createChangesModule();
                if (eventJournal.hasNext()) {
                    // new events arrived before we could export the pending changes!
                    // rewind and let tryProcessEvents() repeat until success
                    return false;
                } else {
                    exportChangesModule(changesModule);
                    pendingChanges = null;
                }
            }
        } catch (Exception e) {
            // catch all exceptions, not just RepositoryException
            AutoExportServiceImpl.log.error("Processing events failed: ", e);
        }
        return true;
    }

    private boolean isReadyForProcessing(final Changes changedNodes) {
        return System.currentTimeMillis() - changedNodes.getCreationTime() > minChangeLogAge;
    }

    private void processEvent(final RevisionEvent event) {
        try {
            switch (event.getType()) {
                case Event.PROPERTY_ADDED:
                case Event.PROPERTY_CHANGED:
                case Event.PROPERTY_REMOVED:
                    if (nodeTypeRegistryLastModifiedPropertyPath.equals(event.getPath())) {
                        if (event.getUserData() != null) {
                            String[] changedNamespacePrefixes = event.getUserData().split("\\|");
                            for (String changedNamespacePrefix : changedNamespacePrefixes) {
                                currentChanges.getChangedNsPrefixes().add(changedNamespacePrefix);
                            }
                            if (log.isDebugEnabled()) {
                                AutoExportServiceImpl.log.debug(String.format("event %d: namespace prefixes %s updated",
                                        event.getRevision(), Arrays.toString(changedNamespacePrefixes)));
                            }
                        }
                    } else {
                        checkAddEventPath(event, event.getPath(), false, false, true);
                    }
                    break;
                case Event.NODE_ADDED:
                    checkAddEventPath(event, event.getPath(), true, false, false);
                    break;
                case Event.NODE_REMOVED:
                    checkAddEventPath(event, event.getPath(), false, true, false);
                    break;
                case Event.NODE_MOVED:
                    final String srcAbsPath = (String)event.getInfo().get("srcAbsPath");
                    if (srcAbsPath != null) {
                        // not an order-before
                        checkAddEventPath(event, event.getPath(), true, false, false);
                        checkAddEventPath(event, srcAbsPath, false, true, false);
                    } else {
                        checkAddEventPath(event, event.getPath(), false, false, false);
                    }
                    break;
            }
        } catch (RepositoryException e) {
            // ignore: return empty set
        }
    }

    private void checkAddEventPath(final RevisionEvent event, final String eventPath, final boolean addedNode,
                                   final boolean deletedNode, final boolean propertyPath) throws RepositoryException {
        if (!overlaps(eventPath, ignoredEventPaths) && !exclusionContext.matches(eventPath)) {

            final ConfigurationItemCategory category =
                    ConfigurationModelUtils.getCategoryForItem(eventPath, propertyPath, currentModel);

            if (category == ConfigurationItemCategory.CONFIG && !overlaps(eventPath, currentChanges.getAddedConfig())) {
                if (addedNode) {
                    boolean childPathAddedBefore = removeChildPaths(eventPath, currentChanges.getAddedConfig());
                    currentChanges.getAddedConfig().add(eventPath);
                    if (childPathAddedBefore) {
                        removeChildPaths(eventPath, currentChanges.getChangedConfig());
                    }
                    currentChanges.getChangedConfig().remove(eventPath);
                } else if (deletedNode) {
                    removeChildPaths(eventPath, currentChanges.getAddedConfig());
                    currentChanges.getAddedConfig().remove(eventPath);
                    removeChildPaths(eventPath, currentChanges.getChangedConfig());
                    currentChanges.getChangedConfig().remove(eventPath);
                }
                String parentPath = getParentPath(eventPath);
                if (currentChanges.getChangedConfig().add(parentPath)) {
                    logEvent(event, parentPath);
                }
            }
            else if (category == ConfigurationItemCategory.CONTENT && !overlaps(eventPath, currentChanges.getAddedContent())) {
                if (addedNode) {
                    boolean childPathAddedBefore = removeChildPaths(eventPath, currentChanges.getAddedContent());
                    currentChanges.getAddedContent().add(eventPath);
                    if (childPathAddedBefore) {
                        removeChildPaths(eventPath, currentChanges.getChangedContent());
                    }
                    currentChanges.getChangedContent().remove(eventPath);
                } else if (deletedNode) {
                    removeChildPaths(eventPath, currentChanges.getAddedContent());
                    currentChanges.getAddedContent().remove(eventPath);
                    removeChildPaths(eventPath, currentChanges.getChangedContent());
                    currentChanges.getChangedContent().remove(eventPath);
                    removeChildPaths(eventPath, currentChanges.getDeletedContent());
                    currentChanges.getDeletedContent().add(eventPath);
                } else {
                    final String changedPath = propertyPath ? getParentPath(eventPath) : eventPath;
                    if (currentChanges.getChangedContent().add(changedPath)) {
                        logEvent(event, changedPath);
                    }
                }
            }
        }
    }

    private void logEvent(final RevisionEvent event, final String path) throws RepositoryException {
        if (log.isDebugEnabled()) {
            final String eventPath = event.getPath();
            AutoExportServiceImpl.log.debug(String.format("event %d: %s under parent: [%s] at: [%s] for user: [%s]",
                    event.getRevision(), Constants.getJCREventTypeName(event.getType()), path,
                    eventPath.startsWith(path) ? eventPath.substring(path.length()) : eventPath,
                    event.getUserID()));
        }
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
    private ModuleImpl createChangesModule() throws RepositoryException {
        final ModuleImpl module = new ModuleImpl("autoexport-module",
                new ProjectImpl("autoexport-project",
                        new GroupImpl("autoexport-group")));
        final JcrResourceInputProvider jcrResourceInputProvider = new JcrResourceInputProvider(eventProcessorSession);
        module.setConfigResourceInputProvider(jcrResourceInputProvider);
        module.setContentResourceInputProvider(jcrResourceInputProvider);
        final ConfigSourceImpl configSource = module.addConfigSource("autoexport.yaml");

        if (!pendingChanges.changedNsPrefixes.isEmpty()) {
            Set<String> nsPrefixes = new HashSet<>(pendingChanges.getChangedNsPrefixes());
            List<NamespaceDefinitionImpl> modifiedNsDefs = currentModel.getNamespaceDefinitions().stream()
                    .filter(d -> nsPrefixes.remove(d.getPrefix()))
                    .collect(Collectors.toList());
            for (NamespaceDefinitionImpl nsDef : modifiedNsDefs) {
                ValueImpl cndPath = new ValueImpl(nsDef.getCndPath().getString(), ValueType.STRING, true, false);
                cndPath.setInternalResourcePath(jcrResourceInputProvider.createCndResourcePath(nsDef.getPrefix()));
                cndPath.setDefinition(configSource.addNamespaceDefinition(nsDef.getPrefix(), nsDef.getURI(), cndPath));
            }
            for (String newNsPrefix : nsPrefixes) {
                ValueImpl cndPath = new ValueImpl(newNsPrefix+".cnd", ValueType.STRING, true, false);
                cndPath.setNewResource(true);
                cndPath.setInternalResourcePath(jcrResourceInputProvider.createCndResourcePath(newNsPrefix));
                try {
                    cndPath.setDefinition(configSource.addNamespaceDefinition(newNsPrefix,
                            new URI(eventProcessorSession.getNamespaceURI(newNsPrefix)), cndPath));
                } catch (URISyntaxException e) {
                    // not going to happen
                }
            }
        }
        JcrContentProcessingService jcrInputProcessor = new JcrContentProcessingService(new ValueProcessor());

        // TODO: use Configuration.filterUuidPaths during delta computation (suppressing export of jcr:uuid)

        for (String path : pendingChanges.getChangedConfig()) {
            // TODO
            // jcrInputProcessor.exportConfigNodeDelta(eventProcessorSession, path, configSource, currentModel);
        }
        for (String path : pendingChanges.getChangedContent()) {
            // TODO
        }
        for (String path : pendingChanges.getDeletedContent()) {
            // TODO
        }

        return module;
    }

    private void exportChangesModule(ModuleImpl changesModule) throws RepositoryException {
        // TODO: move this to internal implementation/handling within DefinitionMergeService
        Set<ModuleImpl> exportModules = new HashSet<>();
        for (GroupImpl g : currentModel.getSortedGroups()) {
            for (ProjectImpl p : g.getProjects()) {
                for (ModuleImpl m : p.getModules()) {
                    if (m.getMvnPath() != null) {
                        exportModules.add(m);
                    }
                }
            }
        }

        DefinitionMergeService mergeService = new DefinitionMergeService(configuration);
        Collection<ModuleImpl> result =
                mergeService.mergeChangesToModules(changesModule, exportModules, currentModel);

        // TODO: 1) export result to filesystem
        // TODO  2) configuration.setLastRevision(lastRevision)
        // TODO  3) save result to baseline (which should do Session.save() thereby also saving the lastRevision update
        // TODO  4) update or reload ConfigurationService.currentRuntimeModel
        // TODO  NOTE: the above might need to be owned/implemented by ConfigurationServiceImpl
        // TODO  5) update/set EventJournalProcessor.currentModel
    }
}
