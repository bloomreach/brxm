/*
 *  Copyright 2011-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.autoexport;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;
import static org.hippoecm.repository.api.HippoNodeType.CONFIGURATION_PATH;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_SEQUENCE;
import static org.hippoecm.repository.api.HippoNodeType.INITIALIZE_PATH;
import static org.hippoecm.repository.api.HippoNodeType.NT_INITIALIZEITEM;
import static org.onehippo.cms7.autoexport.AutoExportModule.log;
import static org.onehippo.cms7.autoexport.Constants.CONFIG_ENABLED_PROPERTY_NAME;
import static org.onehippo.cms7.autoexport.Constants.CONFIG_NODE_PATH;
import static org.onehippo.cms7.autoexport.Constants.NODETYPES_PATH;

public class EventProcessor implements EventListener {

    private static final List<String> ignored = new ArrayList<String>(20);
    private static final int EVENT_TYPES = NODE_ADDED | NODE_REMOVED | PROPERTY_ADDED | PROPERTY_CHANGED | PROPERTY_REMOVED;

    static {
        ignored.add("/jcr:system/jcr:versionStorage");
        ignored.add("/hippo:log");
        ignored.add("/live");
        ignored.add("/preview");
        ignored.add("/content/documents/state");
        ignored.add("/content/documents/tags");
        ignored.add("/content/attic");
        ignored.add("/hst:hst/hst:configuration/hst:default");
        ignored.add(CONFIG_NODE_PATH + "/" + CONFIG_ENABLED_PROPERTY_NAME);
        ignored.add("/hippo:configuration/hippo:modules/brokenlinks");
        ignored.add("/hippo:configuration/hippo:modules/scheduler");
        ignored.add("/hippo:configuration/hippo:temporary");
        ignored.add("/hippo:configuration/hippo:initialize");
        ignored.add("/formdata");
        ignored.add("/initialize");
        ignored.add("/jcr:system/jcr:nodeTypes/hipposys:");
        ignored.add("/jcr:system/jcr:nodeTypes/hippo:");
        ignored.add("/jcr:system/jcr:nodeTypes/rep:");
        ignored.add("/jcr:system/jcr:nodeTypes/hipposysedit:");
        ignored.add("/jcr:system/jcr:nodeTypes/hippofacnav:");
        ignored.add("/hippo:configuration/hippo:update/hippo:queue");
        ignored.add("/hippo:configuration/hippo:update/hippo:history");
        ignored.add("/hippo:configuration/hippo:update/jcr:");
    }

    private final InitializeItemRegistry registry;
    private final Session session;
    private final ScheduledExecutorService executor;
    private final Set<String> uris;
    private final Runnable task = new Runnable() {
        @Override
        public synchronized void run() {
            try {
                processEvents();
                events.clear();
                session.save();
            } catch (Exception e) {
                log.error(e.getClass().getName() + " : " + e.getMessage(), e);
            } finally {
                try {
                    session.refresh(false);
                } catch (RepositoryException ignore) {
                }
            }
        }
    };
    private final Set<ExportEvent> events = new HashSet<ExportEvent>(100);
    private final EventPreProcessor eventPreProcessor;
    private final ObservationManager manager;
    private final Configuration configuration;
    private final List<Module> modules;
    private Module defaultModule;
    private ScheduledFuture<?> future;

    EventProcessor(File baseDir, Session session) throws Exception {
        this.session = session;
        configuration = new Configuration(session);

        log.info("Automatic export is {}", configuration.isExportEnabled() ? "enabled" : "disabled");

        registry = new InitializeItemRegistry();
        modules = new ArrayList<Module>();
        for (Map.Entry<String, Collection<String>> entry : configuration.getModules().entrySet()) {
            String modulePath = entry.getKey();
            Collection<String> repositoryPaths = entry.getValue();
            Module module = new Module(modulePath, repositoryPaths, baseDir, registry, session, configuration);
            if (repositoryPaths.contains("/")) {
                defaultModule = module;
            } else {
                modules.add(module);
            }
        }

        executor = Executors.newSingleThreadScheduledExecutor();
        eventPreProcessor = new EventPreProcessor(session);

        // cache the registered namespace uris so we can detect it when any were added
        String[] uris = session.getWorkspace().getNamespaceRegistry().getURIs();
        this.uris = new HashSet<String>(uris.length + 10);
        Collections.addAll(this.uris, uris);

        manager = session.getWorkspace().getObservationManager();
        manager.addEventListener(this, EVENT_TYPES, "/", true, null, null, false);

    }

    @Override
    public synchronized void onEvent(EventIterator iter) {

        // The following synchronized block will block if task is already running.
        // Once the lock is acquired task.run() itself will block, 
        // thereby making it possible to cancel it here
        synchronized (task) {
            if (future != null) {
                // try to cancel
                future.cancel(true);
            }
            // add events to the set to be processed
            while (iter.hasNext()) {
                Event event = iter.nextEvent();
                String path;
                try {
                    path = event.getPath();
                    if (path.startsWith(CONFIG_NODE_PATH)) {
                        configuration.handleConfigurationEvent(event);
                    }
                    if (isExcluded(path)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Ignoring event on " + path);
                        }
                        continue;
                    }
                    if (!configuration.isExportEnabled()) {
                        if (log.isDebugEnabled()) {
                            log.debug("Lost event: " + event);
                        }
                        continue;
                    }
                } catch (RepositoryException e) {
                    log.error("Error occurred getting path from event.", e);
                }
                addEvent(event);
            }
            if (events.size() > 0) {
                // Schedule events to be processed 2 seconds in the future.
                // If other events arrive during this period the task will
                // be cancelled and a new task is scheduled.
                // This allows us to process events in bigger batches,
                // thus preventing multiple calls to processEvents() where a single
                // call suffices.
                future = executor.schedule(task, 2, TimeUnit.SECONDS);
            }
        }
    }

    private void addEvent(Event event) {
        try {
            events.add(new ExportEvent(event));
        } catch (RepositoryException e) {
            log.error("Unable to add event because unable to compute event path", e);
        }
    }

    private boolean isExcluded(String path) {
        for (String ignore : ignored) {
            if (path.startsWith(ignore)) {
                return true;
            }
        }
        return configuration.getExclusionContext().isExcluded(path);
    }

    private void processEvents() {
        long startTime = System.nanoTime();

        // preprocess the events
        List<ExportEvent> events = eventPreProcessor.preProcessEvents(this.events);

        Collection<InitializeItem> created = new HashSet<InitializeItem>();
        Collection<InitializeItem> updated = new HashSet<InitializeItem>();

        // process the events, passing them on to the designated initialize items
        for (ExportEvent event : events) {
            String path = event.getPath();
            if (log.isDebugEnabled()) {
                log.debug(ExportEvent.valueOf(event.getType()) + " on " + path);
            }

            Module module = getModuleForPath(path);
            Collection<InitializeItem> items = registry.getInitializeItemsByPath(path, event.getType());
            InitializeItem item = ExportUtils.getBestMatchingInitializeItem(items, module);
            if (item == null) {
                item = module.getInitializeItemFactory().createInitializeItem(path, event.getType());
                if (log.isDebugEnabled()) {
                    log.debug("No initialize matching item found for path " + path + ". Created new one: " + item.getName() + " in module " + module.getModulePath());
                }
                registry.addInitializeItem(item);
                created.add(item);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Initialize item found for path " + path + " : " + item.getName() + " in module " + item.getModule().getModulePath());
                }
                if (!created.contains(item)) {
                    updated.add(item);
                }
            }
            item.handleEvent(event);
            if (event.getType() == NODE_REMOVED) {
                for (InitializeItem descendant : registry.getDescendentInitializeItems(path)) {
                    if (!created.contains(descendant)) {
                        updated.add(descendant);
                    }
                    descendant.handleEvent(event);
                }
            }
        }

        // now process the affected initialize items
        Collection<InitializeItem> added = new ArrayList<InitializeItem>();
        Collection<InitializeItem> removed = new ArrayList<InitializeItem>();
        for (InitializeItem item : updated) {
            if (!item.processEvents()) {
                log.info("Initialize item " + item.getName() + " in module " + item.getModule().getModulePath() + " could not be exported");
                continue;
            }
            Module module = item.getModule();
            if (item.isEmpty()) {
                registry.removeInitializeItem(item);
                module.getExporter().scheduleForDeletion(item);
                module.getExtension().initializeItemRemoved(item);
                removed.add(item);
            } else {
                module.getExporter().scheduleForExport(item);
            }
        }
        for (InitializeItem item : created) {
            if (!item.processEvents()) {
                log.info("Initialize item " + item.getName() + " in module " + item.getModule().getModulePath() + " could not be exported");
                continue;
            }
            Module module = item.getModule();
            if (item.isEmpty()) {
                registry.removeInitializeItem(item);
            } else {
                module.getExporter().scheduleForExport(item);
                module.getExtension().initializeItemAdded(item);
                added.add(item);
            }
        }

        // now check if the namespace registry has changed
        try {
            NamespaceRegistry nsRegistry = session.getWorkspace().getNamespaceRegistry();
            // Were any namespaces added?
            for (String uri : nsRegistry.getURIs()) {
                if (!uris.contains(uri)) {
                    log.debug("New namespace detected: {}", uri);
                    String prefix = nsRegistry.getPrefix(uri);
                    Module module = getModuleForNSPrefix(prefix);

                    final InitializeItem item = module.getInitializeItemFactory().createInitializeItem(uri, prefix);
                    log.debug("Adding initialize item: {}", item.getName());
                    registry.addInitializeItem(item);
                    module.getExtension().initializeItemAdded(item);
                    added.add(item);
                    uris.add(uri);
                }
            }
            // No need to check removal of namespaces. Jackrabbit doesn't support that
        } catch (RepositoryException e) {
            log.error("Failed to update namespace instructions.", e);
        }

        export();

        // update the initialize items in the repository
        for (InitializeItem item : added) {
            addInitializeItemNode(item);
        }

        long estimatedTime = System.nanoTime() - startTime;
        log.debug("Processing events took {} ms. ", TimeUnit.MILLISECONDS.convert(estimatedTime, TimeUnit.NANOSECONDS));
    }

    void shutdown() {
        // remove event listener
        try {
            manager.removeEventListener(this);
        } catch (RepositoryException ignore) {
        }
        executor.shutdown();
    }

    private void addInitializeItemNode(InitializeItem item) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Adding node /" + CONFIGURATION_PATH + "/" + INITIALIZE_PATH + "/" + item.getName());
            }
            Node parent = session.getRootNode().getNode(CONFIGURATION_PATH).getNode(INITIALIZE_PATH);
            if (!parent.hasNode(item.getName())) {
                Node node = parent.addNode(item.getName());
                node.setPrimaryType(NT_INITIALIZEITEM);
                node.setProperty(HIPPO_SEQUENCE, item.getSequence());
            }
        } catch (RepositoryException e) {
            log.error("Failed to add initialize item node: " + item.getName(), e);
        }
    }

    private Module getModuleForNSPrefix(String nsPrefix) {
        return getModuleForPath("/hippo:namespaces/" + nsPrefix);
    }

    private Module getModuleForPath(String path) {
        final String nodeTypesPathPrefix = NODETYPES_PATH + "/";
        if (path.startsWith(nodeTypesPathPrefix)) {
            String nodeType = path.substring(nodeTypesPathPrefix.length());
            int offset = nodeType.indexOf(':');
            if (offset != -1) {
                String prefix = nodeType.substring(0, offset);
                return getModuleForNSPrefix(prefix);
            }
        }
        Module result = null;
        for (Module module : modules) {
            if (module.isPathForModule(path)) {
                result = module;
                break;
            }
        }
        if (result == null) {
            result = defaultModule;
        }
        return result;
    }

    private void export() {
        for (Module module : modules) {
            module.getExporter().export();
            module.getExtension().export();
        }
        defaultModule.getExporter().export();
        defaultModule.getExtension().export();
    }

}
