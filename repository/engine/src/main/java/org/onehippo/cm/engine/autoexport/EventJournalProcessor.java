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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.Event;
import javax.jcr.observation.ObservationManager;
import javax.jcr.util.TraversingItemVisitor;

import org.apache.commons.lang3.time.StopWatch;
import org.hippoecm.repository.api.RevisionEvent;
import org.hippoecm.repository.api.RevisionEventJournal;
import org.onehippo.cm.engine.ConfigurationServiceImpl;
import org.onehippo.cm.engine.JcrContentProcessor;
import org.onehippo.cm.engine.JcrResourceInputProvider;
import org.onehippo.cm.model.ConfigurationItemCategory;
import org.onehippo.cm.model.FileConfigurationWriter;
import org.onehippo.cm.model.ModuleContext;
import org.onehippo.cm.model.PathConfigurationReader;
import org.onehippo.cm.model.ValueType;
import org.onehippo.cm.model.impl.ConfigSourceImpl;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.NamespaceDefinitionImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.ValueImpl;
import org.onehippo.cm.model.parser.ParserException;
import org.onehippo.cm.model.serializer.SourceSerializer;
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
            "/" + HCM_ROOT,
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

        protected Changes() {
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
    private final ConfigurationServiceImpl configurationService;
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
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // try processEvents max MAX_REPEAT_PROCESS_EVENTS in a row until success (for one task run)
        for (int i = 0; i < MAX_REPEAT_PROCESS_EVENTS; i++) {
            if (processEvents()) {
                break;
            }
            else {
                // processEvents unsuccessful: new events arrived before it could export already collected changes
                log.debug("Incoming events during processEvents() -- retrying!");
            }

            stopWatch.stop();
            if (stopWatch.getTime(TimeUnit.MILLISECONDS) > 0) {
                log.debug("Full auto-export cycle in {}", stopWatch.toString());
            }
        }
    }

    private boolean processEvents() throws RepositoryException {
        // update our local reference to the runtime model immediately before using it
        this.currentModel = configurationService.getRuntimeConfigurationModel();

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
                    ConfigurationModelUtils.getCategoryForItem(eventPath, propertyPath,
                            configurationService.getRuntimeConfigurationModel());

            // for config, we want to store the paths of the parents of changed nodes or properties
            // (that way, we always have a node to scan for detailed changes)
            // also, remove descendants from the list, since we will be scanning them anyway
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
            // for content, we want to store the actual paths of changed nodes (not properties)
            // for add or change events, keep descendants, since they may indicate a need to export a separate source file
            else if (category == ConfigurationItemCategory.CONTENT && !overlaps(eventPath, currentChanges.getAddedContent())) {
                if (addedNode) {
                    currentChanges.getAddedContent().add(eventPath);

                    // we must scan down the JCR tree and record an add for each descendant node path
                    // protect against race conditions with add and then immediate delete
                    if (eventProcessorSession.nodeExists(eventPath)) {
                        eventProcessorSession.getNode(eventPath).accept(new TraversingItemVisitor.Default() {
                            @Override
                            protected void entering(final Node node, final int level) throws RepositoryException {
                                currentChanges.getAddedContent().add(node.getPath());
                            }
                        });
                    }

                    // cleanup a previously-encountered delete
                    removeChildPaths(eventPath, currentChanges.getDeletedContent());
                    currentChanges.getDeletedContent().remove(eventPath);
                } else if (deletedNode) {
                    // clean up previously-recorded events for descendants, which are now redundant,
                    // since this delete will clear out all descendants anyway
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

    private ModuleImpl createChangesModule() throws RepositoryException, IOException {
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

        JcrContentProcessor jcrInputProcessor = new JcrContentProcessor(currentModel, configuration);
        for (String path : pendingChanges.getChangedConfig()) {
            log.info("Computing diff for path: \n\t{}", path);
            jcrInputProcessor.exportConfigNode(eventProcessorSession, path, configSource);
        }

        // empty defs rarely happen when a new node ends up having only excluded properties -- clean them up
        configSource.cleanEmptyDefinitions();

        if (log.isInfoEnabled()) {
            final SourceSerializer sourceSerializer = new SourceSerializer(null, configSource, false);
            final StringWriter writer = new StringWriter();
            sourceSerializer.serializeNode(writer,sourceSerializer.representSource(new ArrayList<>()::add));
            log.info("Computed diff: \n{}", writer.toString());
        }

        return module.build();
    }

    private void exportChangesModule(ModuleImpl changesModule) throws RepositoryException, IOException, ParserException {
        if (changesModule.isEmpty() && pendingChanges.getAddedContent().isEmpty()
                && pendingChanges.getChangedContent().isEmpty()) {
            AutoExportServiceImpl.log.info("No changes detected");

            // save this fact immediately and do nothing else
            configuration.setLastRevision(lastRevision);
            eventProcessorSession.save();
        }
        else {
            final DefinitionMergeService mergeService = new DefinitionMergeService(configuration);
            final Collection<ModuleImpl> mergedModules =
                    mergeService.mergeChangesToModules(changesModule, pendingChanges, currentModel);
            final List<ModuleImpl> reloadedModules = new ArrayList<>();

            // 1) export result to filesystem
            // convert the project basedir to a Path, so we can resolve modules against it
            final String projectDir = System.getProperty(org.onehippo.cm.model.Constants.PROJECT_BASEDIR_PROPERTY);
            final Path projectPath = Paths.get(projectDir);

            // write each module to the file system
            FileConfigurationWriter writer = new FileConfigurationWriter();
            for (ModuleImpl module : mergedModules) {
                final Path moduleDescriptorPath = projectPath.resolve(module.getMvnPath())
                        .resolve(org.onehippo.cm.model.Constants.MAVEN_MODULE_DESCRIPTOR);
                final ModuleContext ctx = new ModuleContext(module, moduleDescriptorPath, false);
                ctx.createOutputProviders(moduleDescriptorPath);

                // TODO: confirm that this will work properly with in-place overwrite
                writer.writeModule(module, ctx);

                // then reload the modules, so we get a nice, clean, purely-File-based view of the sources
                // TODO: share this logic with ClasspathConfigurationModelReader somehow
                final PathConfigurationReader.ReadResult result =
                        new PathConfigurationReader().read(moduleDescriptorPath);
                result.getGroups().stream()
                        .flatMap(g -> g.getProjects().stream())
                        .flatMap(p -> p.getModules().stream())
                        .forEach(m -> {
                            // store mvnPath again for later use
                            m.setMvnPath(module.getMvnPath());
                            reloadedModules.add(m);
                        });
            }

            // 2) configuration.setLastRevision(lastRevision) (should NOT save the JCR session!)
            configuration.setLastRevision(lastRevision);

            // 3) save result to baseline (which should do Session.save() thereby also saving the lastRevision update
            // 4) update or reload ConfigurationService.currentRuntimeModel
            configurationService.updateBaselineForAutoExport(reloadedModules);
        }
    }
}
