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

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.ObservationManager;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.RevisionEvent;
import org.hippoecm.repository.api.RevisionEventJournal;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cm.engine.ConfigurationServiceImpl;
import org.onehippo.cm.engine.JcrResourceInputProvider;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.definition.NamespaceDefinitionImpl;
import org.onehippo.cm.model.impl.source.ConfigSourceImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.parser.ParserException;
import org.onehippo.cm.model.parser.PathConfigurationReader;
import org.onehippo.cm.model.serializer.ModuleContext;
import org.onehippo.cm.model.serializer.SourceSerializer;
import org.onehippo.cm.model.tree.ConfigurationItemCategory;
import org.onehippo.cm.model.tree.ValueType;
import org.onehippo.cms7.utilities.exceptions.ExceptionLoopDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.engine.Constants.HCM_ROOT;
import static org.onehippo.cm.engine.Constants.SYSTEM_PARAMETER_AUTOEXPORT_LOOP_PROTECTION;
import static org.onehippo.cm.engine.ValueProcessor.isKnownDerivedPropertyName;
import static org.onehippo.cm.model.util.FilePathUtils.nativePath;

public class EventJournalProcessor {

    static final Logger log = LoggerFactory.getLogger(EventJournalProcessor.class);

    private static final String[] builtinIgnoredEventPaths = new String[]{
            "/hippo:log",
            "/content/attic",
            "/formdata",
            "/webfiles",
            "/hippo:configuration/hippo:update/hippo:queue",
            "/hippo:configuration/hippo:update/hippo:history",
            "/hippo:configuration/hippo:update/jcr:",
            "/hippo:configuration/hippo:temporary",
            "/hippo:configuration/hippo:modules/brokenlinks",
            "/" + HCM_ROOT,
    };

    private static final int MAX_REPEAT_PROCESS_EVENTS = 3;
    private static final long TIME_TO_LIVE = 2000L;
    private static final int EXCEPTION_THRESHOLD = 3;

    private final ExceptionLoopDetector exceptionLoopDetector;
    private final boolean exceptionLoopPreventionEnabled;
    private final AutoExportConfig autoExportConfig;
    private final ConfigurationServiceImpl configurationService;
    private final Session eventProcessorSession;
    private final String nodeTypeRegistryLastModifiedPropertyPath;
    private final String lastRevisionPropertyPath;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final long minChangeLogAge = 250;
    private long lastRevision = -1;
    private Node autoExportConfigNode = null;
    private Long lastRevisionPropertyValue = null;
    private EventChanges pendingChanges;
    private EventChanges currentChanges;
    private RevisionEventJournal eventJournal;
    private ConfigurationModelImpl currentModel;

    // critical segment flag -- if this is true, a simple rollback recovery cannot be performed safely
    private boolean fileWritesInProgress = false;

    private ScheduledFuture<?> future;
    private final AtomicBoolean taskFailed = new AtomicBoolean(false);
    private final AtomicBoolean runningOnce = new AtomicBoolean(false);
    private final Runnable task = () -> {
        if (!taskFailed.get()) {
            try {
                tryProcessEvents();
            } catch (Exception e) {
                taskFailed.set(true);
                AutoExportServiceImpl.log.error(e.getClass().getName() + " : " + e.getMessage(), e);
                if (future != null && !future.isDone()) {
                    future.cancel(false);
                }
            }
        }
    };

    public EventJournalProcessor(final ConfigurationServiceImpl configurationService,
                                 final AutoExportConfig autoExportConfig, final Set<String> extraIgnoredEventPaths)
            throws RepositoryException {

        exceptionLoopPreventionEnabled = Boolean.getBoolean(SYSTEM_PARAMETER_AUTOEXPORT_LOOP_PROTECTION);
        this.exceptionLoopDetector = new ExceptionLoopDetector(TIME_TO_LIVE, EXCEPTION_THRESHOLD);
        this.configurationService = configurationService;
        this.autoExportConfig = autoExportConfig;
        nodeTypeRegistryLastModifiedPropertyPath = autoExportConfig.getConfigPath()
                + "/" + AutoExportConstants.CONFIG_NTR_LAST_MODIFIED_PROPERTY_NAME;
        lastRevisionPropertyPath = autoExportConfig.getConfigPath()
                + "/" + AutoExportConstants.CONFIG_LAST_REVISION_PROPERTY_NAME;
        PathsMap ignoredEventPaths = new PathsMap(builtinIgnoredEventPaths);
        ignoredEventPaths.addAll(extraIgnoredEventPaths);
        ignoredEventPaths.add(nodeTypeRegistryLastModifiedPropertyPath);
        ignoredEventPaths.add(lastRevisionPropertyPath);
        autoExportConfig.addIgnoredPaths(ignoredEventPaths);

        eventProcessorSession = autoExportConfig.createImpersonatedSession();
        autoExportConfigNode = eventProcessorSession.getNode(autoExportConfig.getConfigPath());
        prepareEventJournalAndLastRevision();
    }

    public void start() {
        synchronized (executor) {
            taskFailed.set(false);
            if (future == null || future.isDone()) {
                future = executor.scheduleWithFixedDelay(task, 0, minChangeLogAge, TimeUnit.MILLISECONDS);
            }
        }
    }

    public void stop() {
        stop(false);
        runOnce();
        reset();
    }

    public void abort() {
        stop(false);
        reset();
    }

    private void stop(final boolean mayInterruptIfRunning) {
        synchronized (executor) {
            if (future != null && !future.isDone()) {
                future.cancel(mayInterruptIfRunning);
                try {
                    future.get();
                } catch (InterruptedException|ExecutionException|CancellationException ignore) {
                }
            }
            future = null;
        }
    }

    /**
     * Executes the core auto-export loop exactly once in synchronous blocking fashion, which is primarily used to
     * flush any pending changes after autoexport is {@link #stop() stopped}.
     * It is also used for tests.
     * Note: this method also serializes calls from different threads, such that only a single runOnce() call can
     * be active simultaneously.
     */
    public void runOnce() {
        // only a single thread can call runOnce simultaneously!
        synchronized (runningOnce) {
            // only a single thread can manipulate the executor simultaneously!
            synchronized (executor) {
                if (!taskFailed.get()) {
                    try {
                        runningOnce.set(true);
                        future = executor.schedule(task, 0, TimeUnit.MILLISECONDS);
                    } catch(final Exception ignore) {
                    }
                }
            }

            // don't attempt the future.get() inside the synchronized block for executor, because that will deadlock
            // calls to stop() from within processEvents(); yet, we want to protect the value of runningOnce, so it must
            // be within the synchronized block for runningOnce!
            try {
                if (runningOnce.get() && future != null) {
                    future.get();
                }
            } catch (InterruptedException|ExecutionException|CancellationException ignore) {
            } finally {
                runningOnce.set(false);
            }
        }
    }

    private void reset() {
        synchronized (executor) {
            if (future != null) {
                stop(false);
            }
            eventJournal = null;
            lastRevision = -1;
            currentChanges = null;
            pendingChanges = null;
            currentModel = null;
            // reset lastRevisionPropertyValue as well, allowing an actual 'reset' to -1 (or delete) of the property
            // by a developer, thereby 'skipping' next autoexport when enabled to the then current 'head' revision
            lastRevisionPropertyValue = null;
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
            } else {
                // processEvents unsuccessful: new events arrived before it could export already collected changes
                log.debug("Incoming events during processEvents() -- retrying!");
            }
        }
    }

    private void prepareEventJournalAndLastRevision() throws RepositoryException {
        // ensure eventJournal will be 'up-to-date' and that there are no pending session changes
        eventProcessorSession.refresh(false);

        if (eventJournal == null) {
            final ObservationManager observationManager = eventProcessorSession.getWorkspace().getObservationManager();
            eventJournal = (RevisionEventJournal) observationManager.getEventJournal();
            lastRevision = getLastRevision();
        }
        if (lastRevision == -1) {
            // first skip to almost now (minus 1 minute), so we likely can capture the current last revision fast
            // this is useful when enabling autoexport on an existing (production copy?) repository with a large journal
            eventJournal.skipTo(System.currentTimeMillis()-1000*60);
            RevisionEvent lastEvent = null;
            while (eventJournal.hasNext()) {
                lastEvent = eventJournal.nextEvent();
            }
            if (lastEvent != null) {
                log.info("Skipping to initial eventjournal head revision: {} ", lastEvent.getRevision());
                lastRevision = lastEvent.getRevision();
                setLastRevision(lastRevision);
            }
        } else {
            eventJournal.skipToRevision(lastRevision);
        }
    }

    private boolean processEvents() throws RepositoryException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // update our local reference to the runtime model immediately before using it
        this.currentModel = configurationService.getRuntimeConfigurationModel();

        try {
            prepareEventJournalAndLastRevision();
            int count = 0;
            while (eventJournal.hasNext()) {
                RevisionEvent event = eventJournal.nextEvent();
                boolean storeLastRevision = lastRevision == -1;
                lastRevision = event.getRevision();
                if (storeLastRevision) {
                    // still haven't yet stored the current last revision: do so now
                    setLastRevision(lastRevision);
                }
                if (event.getType() == Event.PERSIST) {
                    continue;
                }
                if (event.getPath().equals(lastRevisionPropertyPath)) {
                    continue;
                }
                // any other event can require processing or as a minimum result in updating the lastRevision
                if (currentChanges == null) {
                    currentChanges =
                            new EventChanges(autoExportConfig, currentModel);
                }
                count++;
                if (HCM_ROOT.equals(event.getUserData())) {
                    continue;
                }
                processEvent(event);
            }
            if (count > 0) {
                AutoExportServiceImpl.log.debug("Read {} events up to {}", count, lastRevision);
            }
            if (currentChanges != null) {
                if (!currentChanges.isEmpty()) {
                    if (pendingChanges != null) {
                        pendingChanges.mergeCurrentChanges(currentChanges);
                        AutoExportServiceImpl.log.debug("Adding new changes to pending changes");
                    } else if (runningOnce.get() || isReadyForProcessing(currentChanges)) {
                        pendingChanges = currentChanges;
                    }
                } else {
                    // all events are skipped
                    currentChanges = null;
                    if (pendingChanges == null) {
                        // no pending changes either: bump lastRevision to skip these igorable events in the future
                        setLastRevision(lastRevision);
                    }
                }
            }
            if (count > 0) {
                stopWatch.split();
                AutoExportServiceImpl.log.debug("Events processed in {}", stopWatch.toSplitString());
            }
            if (pendingChanges != null) {
                currentChanges = null;

                // create cloned PathsMaps for added/deleted content as these might get 'enhanced' during the next
                // stage, while if we detect later overlapping events came in *before* writing them out we
                // need to rewind, without these 'enhancements'.
                final PathsMap addedContent = new PathsMap(pendingChanges.getAddedContent());
                final PathsMap deletedContent = new PathsMap(pendingChanges.getDeletedContent());
                ModuleImpl changesModule = createChangesModule(addedContent, deletedContent);
                if (eventJournal.hasNext()) {
                    stopWatch.stop();
                    log.info("Diff processing abandoned after {}", stopWatch.toString());

                    // new events arrived before we could export the pending changes!
                    // rewind and let tryProcessEvents() repeat until success
                    return false;
                } else {
                    try {
                        exportChangesModule(changesModule, addedContent, deletedContent);
                        pendingChanges = null;
                        if (exceptionLoopPreventionEnabled) {
                            exceptionLoopDetector.purge();
                        }
                    } catch (Exception ex) {
                        //stop autoexport
                        if (fileWritesInProgress) {
                            try {
                                log.error("Failure writing files during auto-export -- files on disk may not be valid!");
                                abort();
                                disableAutoExportJcrProperty();
                            } finally {
                                fileWritesInProgress = false;
                            }
                        }

                        if (exceptionLoopPreventionEnabled && exceptionLoopDetector.loopDetected(ex)) {
                            log.info("Looping detected, disabling autoexport");
                            abort();
                            disableAutoExportJcrProperty();
                        }
                        throw ex;
                    }
                }
            }
        } catch (Exception e) {
            stopWatch.stop();
            log.info("Events processing failed after {}", stopWatch.toString());

            // catch all exceptions, not just RepositoryException
            AutoExportServiceImpl.log.error("Processing events failed: ", e);
        }
        return true;
    }

    /**
     * Set the autoexport:enabled property to false in the repository. This method should only be called in an exception
     * handler, as it assumes the current session state is garbage and aggressively clears it.
     * @throws RepositoryException
     */
    private void disableAutoExportJcrProperty() throws RepositoryException {
        // this method is almost certainly being called while current session state is somehow unreliable
        // therefore, before attempting to work with the repo, we should attempt to reset to a good state
        eventProcessorSession.refresh(false);

        final Property autoExportEnableProperty = autoExportConfigNode.getProperty(AutoExportConstants.CONFIG_ENABLED_PROPERTY_NAME);
        autoExportEnableProperty.setValue(false);
        eventProcessorSession.save();
    }

    private boolean isReadyForProcessing(final EventChanges changedNodes) {
        return System.currentTimeMillis() - changedNodes.getCreationTime() > minChangeLogAge;
    }

    private void processEvent(final RevisionEvent event) {
        try {
            final String eventPath = event.getPath();
            switch (event.getType()) {
                case Event.PROPERTY_ADDED:
                case Event.PROPERTY_CHANGED:
                case Event.PROPERTY_REMOVED:
                    if (isKnownDerivedPropertyName(StringUtils.substringAfterLast(eventPath, "/"))) {
                        return;
                    }
                    if (nodeTypeRegistryLastModifiedPropertyPath.equals(eventPath)) {
                        if (event.getUserData() != null) {
                            String[] changedNamespacePrefixes = event.getUserData().split("\\|");
                            for (String changedNamespacePrefix : changedNamespacePrefixes) {
                                currentChanges.recordChangedNsPrefix(changedNamespacePrefix);
                            }
                            if (log.isDebugEnabled()) {
                                AutoExportServiceImpl.log.debug(String.format("event %d: namespace prefixes %s updated",
                                        event.getRevision(), Arrays.toString(changedNamespacePrefixes)));
                            }
                        }
                    } else {
                        currentChanges.recordEvent(event, eventPath, false, false, true);
                    }
                    break;
                case Event.NODE_ADDED:
                    currentChanges.recordEvent(event, eventPath, true, false, false);
                    break;
                case Event.NODE_REMOVED:
                    currentChanges.recordEvent(event, eventPath, false, true, false);
                    break;
                case Event.NODE_MOVED:
                    final String srcAbsPath = (String) event.getInfo().get("srcAbsPath");
                    if (srcAbsPath != null) {
                        // not an order-before
                        currentChanges.recordEvent(event, eventPath, true, false, false);
                        currentChanges.recordEvent(event, srcAbsPath, false, true, false);
                    } else {
                        currentChanges.recordEvent(event, eventPath, false, false, false);
                    }
                    break;
            }
        } catch (RepositoryException e) {
            // ignore: return empty set
        }
    }

    // scan down the JCR tree and return for each added content node its path and those of its decendant node children
    // protect against stale journal event processing where an added path might already have been deleted since
    // TODO: do this only for node move and not for all node-add, which will already have separate events
    // TODO: if add, then move, this could try to find children for a non-existing node
    // TODO: -- catch this case and continue processing
    public Set<String> getAllAddedContentPaths(final Set<String> addedContentRoots) throws RepositoryException {
        final Set<String> contentPaths = Collections.newSetFromMap(new PatriciaTrie<>());
        final ItemVisitor nodePathsCollector = new ItemVisitor() {
            @Override
            public void visit(final Node node) throws RepositoryException {
                if (!((HippoNode)node).isVirtual()) {
                    final String childPath = node.getPath();
                    if (!autoExportConfig.isExcludedPath(childPath) &&
                            ConfigurationItemCategory.SYSTEM != autoExportConfig.getCategoryForItem(childPath, false, currentModel)) {
                        contentPaths.add(node.getPath());
                        for (Node child : new NodeIterable(node.getNodes())) {
                            child.accept(this);
                        }
                    }
                }
            }
            @Override
            public void visit(final Property ignore) throws RepositoryException {
            }
        };

        for (String addedContentRoot : addedContentRoots) {
            if (eventProcessorSession.nodeExists(addedContentRoot)) {
                contentPaths.add(addedContentRoot);
                for (Node child : new NodeIterable(eventProcessorSession.getNode(addedContentRoot).getNodes())) {
                    child.accept(nodePathsCollector);
                }
            }
        }
        return contentPaths;
    };

    protected ModuleImpl createChangesModule(final PathsMap addedContent, final PathsMap deletedContent)
            throws RepositoryException, IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final ModuleImpl module = new ModuleImpl("autoexport-module",
                new ProjectImpl("autoexport-project",
                        new GroupImpl("autoexport-group")));
        final JcrResourceInputProvider jcrResourceInputProvider = new JcrResourceInputProvider(eventProcessorSession);
        module.setConfigResourceInputProvider(jcrResourceInputProvider);
        module.setContentResourceInputProvider(jcrResourceInputProvider);
        final ConfigSourceImpl configSource = module.addConfigSource("autoexport.yaml");

        if (!pendingChanges.getChangedNsPrefixes().isEmpty()) {
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
                ValueImpl cndPath = new ValueImpl(newNsPrefix + ".cnd", ValueType.STRING, true, false);
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

        final AutoExportConfigExporter autoExportConfigExporter =
                new AutoExportConfigExporter(currentModel, autoExportConfig, addedContent);
        for (String path : pendingChanges.getChangedConfig()) {
            log.info("Computing diff for path: \n\t{}", path);
            autoExportConfigExporter.exportConfigNode(eventProcessorSession, path, configSource);
        }

        if (log.isInfoEnabled()) {
            final SourceSerializer sourceSerializer = new SourceSerializer(null, configSource, false);
            final StringWriter writer = new StringWriter();
            sourceSerializer.serializeNode(writer, sourceSerializer.representSource());
            log.info("Computed diff: \n{}", writer.toString());
            log.info("added content: \n\t{}", String.join("\n\t", addedContent));
            log.info("changed content: \n\t{}", String.join("\n\t", pendingChanges.getChangedContent()));
            log.info("deleted content: \n\t{}", String.join("\n\t", deletedContent));
        }

        module.build();

        stopWatch.stop();
        log.info("Diff computed in {}", stopWatch.toString());

        return module;
    }

    protected void exportChangesModule(final ModuleImpl changesModule, final PathsMap addedContent, final PathsMap deletedContent)
            throws RepositoryException, IOException, ParserException {
        if (changesModule.isEmpty()
                && addedContent.isEmpty()
                && pendingChanges.getChangedContent().isEmpty()
                && deletedContent.isEmpty()) {
            log.info("No changes detected");
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            // save this fact immediately and do nothing else
            setLastRevision(lastRevision);

            stopWatch.stop();
            log.info("Diff export (revision update only) in {}", stopWatch.toString());
        } else {
            AutoExportServiceImpl.log.info("autoexport is processing changes...");

            final Set<String> addedContentPaths = getAllAddedContentPaths(addedContent.getPaths());

            final DefinitionMergeService mergeService =
                    new DefinitionMergeService(autoExportConfig, currentModel, eventProcessorSession);
            final Collection<ModuleImpl> mergedModules =
                    mergeService.mergeChangesToModules(changesModule,
                            addedContentPaths, pendingChanges.getChangedContent(), deletedContent.getPaths());
            final List<ModuleImpl> reloadedModules = new ArrayList<>();

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            // 1) export result to filesystem
            // convert the project basedir to a Path, so we can resolve modules against it
            final String projectDir = System.getProperty(org.onehippo.cm.model.Constants.PROJECT_BASEDIR_PROPERTY);
            final Path projectPath = Paths.get(projectDir);

            // write each module to the file system
            final JcrResourceInputProvider valueInputProvider = new JcrResourceInputProvider(eventProcessorSession);
            final AutoExportModuleWriter writer = new AutoExportModuleWriter(valueInputProvider);
            for (ModuleImpl module : mergedModules) {
                final Path moduleDescriptorPath = projectPath.resolve(nativePath(
                        module.getMvnPath() + org.onehippo.cm.engine.Constants.MAVEN_MODULE_DESCRIPTOR));
                final ModuleContext ctx = new AutoExportModuleContext(module, moduleDescriptorPath, valueInputProvider);
                ctx.createOutputProviders(moduleDescriptorPath);

                // set a critical segment flag -- if we pass this point but don't reach the full successful completion,
                // we cannot perform an automated recovery to the previous safe state
                fileWritesInProgress = true;
                writer.writeModule(module, ctx);

                // then reload the modules, so we get a nice, clean, purely-File-based view of the sources
                // TODO: share this logic with ClasspathConfigurationModelReader somehow
                // TODO: better yet, avoid this step via proper in-place resource updating on write
                final PathConfigurationReader.ReadResult result =
                        new PathConfigurationReader(false, true).read(moduleDescriptorPath);

                final ModuleImpl loadedModule = result.getModuleContext().getModule();
                // store mvnPath again for later use
                loadedModule.setMvnPath(module.getMvnPath());

                // pass along change indicators for sources and resources, so baseline can perform incremental update
                // TODO: use this simpler code when we have time to test it thoroughly
//                module.getRemovedConfigResources().forEach(loadedModule::addConfigResourceToRemove);
//                module.getRemovedContentResources().forEach(loadedModule::addContentResourceToRemove);
//                module.getConfigSources().stream().filter(SourceImpl::hasChangedSinceLoad).forEach(source -> {
//                    loadedModule.getConfigSource(source.getPath()).ifPresent(SourceImpl::markChanged);
//                });
//                module.getContentSources().stream().filter(SourceImpl::hasChangedSinceLoad).forEach(source -> {
//                    loadedModule.getContentSource(source.getPath()).ifPresent(SourceImpl::markChanged);
//                });

                module.getRemovedConfigResources().forEach(loadedModule::addConfigResourceToRemove);
                module.getRemovedContentResources().forEach(loadedModule::addContentResourceToRemove);
                module.getConfigSources().forEach(source -> {
                    loadedModule.getConfigSource(source.getPath())
                            .ifPresent(s -> {
                                if (source.hasChangedSinceLoad()) {
                                    s.markChanged();
                                }
                            });
                });
                module.getContentSources().forEach(source -> {
                    loadedModule.getContentSource(source.getPath())
                            .ifPresent(s -> {
                                if (source.hasChangedSinceLoad()) {
                                    s.markChanged();
                                }
                            });
                });

                reloadedModules.add(loadedModule);
            }

            stopWatch.stop();
            log.info("Diff export (writing modules) in {}", stopWatch.toString());

            // 2) save result to baseline (which should do Session.save()
            // 3) update or reload ConfigurationService.currentRuntimeModel
            configurationService.updateBaselineForAutoExport(reloadedModules);

            // 4) now also update and save the lastRevision
            setLastRevision(lastRevision);

            // we've reached a new safe state, so a rollback recovery to this state is again possible
            fileWritesInProgress = false;

            AutoExportServiceImpl.log.info("autoexport update complete");
        }
    }

    private long getLastRevision() throws RepositoryException {
        if (lastRevisionPropertyValue == null) {
            lastRevisionPropertyValue = JcrUtils.getLongProperty(autoExportConfigNode, AutoExportConstants.CONFIG_LAST_REVISION_PROPERTY_NAME, -1l);
        }
        return lastRevisionPropertyValue;
    }

    /**
     * Sets and saves the lastRevision property
     * @param lastRevision the new value of the lastRevision property
     * @throws RepositoryException
     */
    private void setLastRevision(final long lastRevision) throws RepositoryException {
        autoExportConfigNode.setProperty(AutoExportConstants.CONFIG_LAST_REVISION_PROPERTY_NAME, lastRevision);
        eventProcessorSession.save();
        this.lastRevisionPropertyValue = lastRevision;
    }
}
