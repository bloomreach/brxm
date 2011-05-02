/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.repository.export;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.dom4j.DocumentException;
import org.hippoecm.repository.ext.DaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This module implements automatic export of repository content.
 * In order to use this functionality you need to set the system property
 * {@code hippoecm.export.dir}. This property specifies the directory where configuration
 * files are to be written to. If no configuration has yet been defined in this directory
 * new hippoecm-extension.xml and related files will be created as necessary. 
 * Otherwise changes to the repository will be merged into the existing files and new files
 * will be created when necessary.
 * <p>
 */
public final class ExportModule implements DaemonModule {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";

    // ---------- Static variables
    private static final Logger log = LoggerFactory.getLogger("org.hippoecm.repository.export");
    private static final String CONFIG_NODE_PATH = "/hippo:configuration/hippo:modules/autoexport/hippo:moduleconfig";
    // ---------- Member variables
    private Session session;
    // we keep the listener, the manager and the executor here as members
    // so we can shutdown properly
    private EventListener listener;
    private ObservationManager manager;
    private ScheduledExecutorService executor;

    // ---------- Constructor
    public ExportModule() {
    }

    // ---------- DaemonModule implementation
    @Override
    public void initialize(Session session) throws RepositoryException {

        this.session = session;

        // read 'hippoecm.export.dir' system property
        String configDir = System.getProperty("hippoecm.export.dir");
        File configDirectory = null;
        if (configDir != null && !configDir.isEmpty()) {
            configDirectory = new File(configDir);
        } else {
            log.info("No config directory set. Automatic export will not be available.");
            return;
        }

        // initialize config directory
        if (!configDirectory.exists()) {
            configDirectory.mkdirs();
        }

        // set export location property in the repository
        String path = null;
        try {
            path = configDirectory.getCanonicalPath();
            Node node = session.getNode(CONFIG_NODE_PATH);
            node.setProperty("hipposys:location", path);
            session.save();
        } catch (RepositoryException e) {
            log.warn("Cannot set export location property: " + e.getMessage());
        } catch (IOException e) {
            log.warn("Cannot set export location property: " + e.getMessage());
        }

        log.info("Automatically exporting changes to directory " + path);

        // create extension
        Extension extension = null;
        File file = new File(configDirectory, "hippoecm-extension.xml");
        try {
            extension = new Extension(file);
        } catch (IOException ex) {
            log.error("Failed to initialize export. "
                    + "Automatic export will not be available.", ex);
            return;
        } catch (DocumentException ex) {
            log.error("Failed to initialize export. "
                    + "Automatic export will not be available", ex);
            return;
        }

        executor = Executors.newSingleThreadScheduledExecutor();

        // install event listener
        listener = new ExportEventListener(extension, session, executor);
        int eventTypes = Event.NODE_ADDED | Event.NODE_REMOVED
                | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;
        try {
            manager = session.getWorkspace().getObservationManager();
            manager.addEventListener(listener, eventTypes, "/", true, null, null, false);
        } catch (RepositoryException e) {
            log.error("Failed to initialize export. Automatic export will not be available.", e);
        }

    }

    @Override
    public void shutdown() {
        // remove event listener
        if (manager != null) {
            try {
                manager.removeEventListener(listener);
            } catch (RepositoryException e) {
            }
        }
        // shutdown executor
        if (executor != null) {
            executor.shutdown();
        }
        // remove location property
        try {
            session.getNode(CONFIG_NODE_PATH).getProperty("hipposys:location").remove();
            session.save();
        } catch (PathNotFoundException e) {
            log.debug("No such item: " + CONFIG_NODE_PATH + "/hipposys:location");
        } catch (RepositoryException e) {
            log.error("Error removing location property from repository. ", e);
        }
    }

    // ---------- Implementation
    /**
     * JCR EventListener. Listens to jcr change events on relevant nodes; checks if an appropriate instruction
     * exists for the event, creates one if none was found; decides what to do in order to persist the changes.
     */
    private static class ExportEventListener implements EventListener {
        private static final List<String> ignored = new ArrayList<String>(10);

        static {
            ignored.add("/jcr:system/jcr:versionStorage");
            ignored.add("/hippo:log");
            ignored.add("/live");
            ignored.add("/preview");
            ignored.add("/content/documents/state");
            ignored.add("/content/documents/tags");
            ignored.add("/content/attic");
            ignored.add("/hst:hst/hst:configuration/hst:default");
            ignored.add("/hippo:configuration/hippo:modules/autoexport");
            ignored.add("/hippo:configuration/hippo:temporary");
            ignored.add("/hippo:configuration/hippo:initialize");
            ignored.add("/formdata");
        }
        private final Extension extension;
        private final Session session;
        private final ScheduledExecutorService executor;
        private final Set<String> uris;
        private final Runnable task = new Runnable() {
            @Override
            public synchronized void run() {
                processEvents();
                events.clear();
            }
        };
        private final Set<Event> events = new HashSet<Event>(100);
        private ScheduledFuture<?> future;

        private ExportEventListener(Extension extension, Session session, ScheduledExecutorService executor) throws RepositoryException {
            this.extension = extension;
            this.session = session;
            this.executor = executor;

            // cache the registered namespace uris so we can detect when any were added 
            String[] _uris = session.getWorkspace().getNamespaceRegistry().getURIs();
            this.uris = new HashSet<String>(_uris.length + 10);
            Collections.addAll(uris, _uris);
        }

        @Override
        public synchronized void onEvent(EventIterator iter) {

            if (!isExportEnabled()) {
                log.info("Automatic export disabled from Console. Changes will be lost.");
                return;
            }

            // The following synchronized block will block if m_task is already running.
            // Once the lock is acquired m_task.run() itself will block, 
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
                        if (ignore(path)) {
                            if (log.isDebugEnabled()) {
                                log.debug("Ignoring event on " + path);
                            }
                            continue;
                        }
                    } catch (RepositoryException e) {
                        log.error("Error occurred getting path from event.", e);
                    }
                    events.add(event);
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

        private void processEvents() {
            long startTime = System.nanoTime();

            // sort the events (see EventComparator)
            List<Event> _events = new ArrayList<Event>(events);
            Collections.sort(_events, new EventComparator());

            // process the events
            for (Event event : _events) {
                try {
                    String path = event.getPath();
                    if (log.isDebugEnabled()) {
                        log.debug(eventString(event) + " on " + path);
                    }
                    boolean isNode = EventComparator.isNodeEventType(event);
                    ResourceInstruction instruction = extension.findResourceInstruction(path, isNode);
                    if (instruction != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Found instruction " + instruction);
                        }
                    }
                    switch (event.getType()) {
                        case Event.NODE_ADDED:
                            if (instruction != null) {
                                instruction.nodeAdded(path);
                            } else {
                                instruction = extension.createResourceInstruction(path, true);
                                if (instruction != null) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Adding instruction " + instruction);
                                    }
                                    extension.addInstruction(instruction);
                                } else {
                                    log.warn("Unable to create instruction. This change will be lost");
                                }
                            }
                            break;
                        case Event.NODE_REMOVED:
                            if (instruction != null) {
                                if (instruction.nodeRemoved(path)) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Removing instruction " + instruction);
                                    }
                                    // the root node of this instruction was removed, remove the instruction
                                    extension.removeInstruction(instruction);
                                }
                            } else {
                                log.warn("Change not handled by export. "
                                        + "Node removed on " + path + ". "
                                        + "You need to do this manually.");
                            }
                            break;
                        case Event.PROPERTY_ADDED:
                            if (instruction != null) {
                                instruction.propertyAdded(path);
                            } else {
                                log.warn("Change not handled by export. "
                                        + "Property added on " + path + ". "
                                        + "You need to do this manually.");
                            }
                            break;
                        case Event.PROPERTY_CHANGED:
                            if (instruction != null) {
                                instruction.propertyChanged(path);
                            } else {
                                log.warn("Change not handled by export. "
                                        + "Property changed on " + path + ". "
                                        + "You need to do this manually.");
                            }
                            break;
                        case Event.PROPERTY_REMOVED:
                            if (instruction != null) {
                                instruction.propertyRemoved(path);
                            } else {
                                log.warn("Change not handled by export. "
                                        + "Property removed on " + path + ". "
                                        + "You need to do this manually.");
                            }
                            break;
                    }
                } catch (RepositoryException ex) {
                    log.error("Failed to process repository event.", ex);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            // now check if the namespace registry has changed
            try {
                NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();
                // Were any namespaces added?
                for (String uri : registry.getURIs()) {
                    if (!uris.contains(uri)) {
                        if (log.isDebugEnabled()) {
                            log.debug("New namespace detected: " + uri);
                        }
                        NamespaceInstruction instruction = extension.findNamespaceInstruction(uri);
                        if (instruction != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("Updating instruction " + instruction);
                            }
                            instruction.updateNamespace(uri);
                            extension.setChanged();
                        } else {
                            instruction = extension.createNamespaceInstruction(uri, registry.getPrefix(uri));
                            if (log.isDebugEnabled()) {
                                log.debug("Adding instruction " + instruction);
                            }
                            extension.addInstruction(instruction);
                        }
                        uris.add(uri);
                    }
                }
                // No need to check removal of namespaces. Jackrabbit doesn't support that
            } catch (RepositoryException e) {
                log.error("Failed to update namespace instructions.", e);
            } catch (IOException e) {
                log.error("Failed to update namespace instructions.", e);
            }

            // export
            extension.export(session);

            if (log.isDebugEnabled()) {
                long estimatedTime = System.nanoTime() - startTime;
                log.debug("onEvent took " + TimeUnit.MILLISECONDS.convert(estimatedTime, TimeUnit.NANOSECONDS) + " ms.");
            }

        }

        private boolean ignore(String path) {
            for (String ignore : ignored) {
                if (path.startsWith(ignore)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isExportEnabled() {
            boolean enabled = true;
            try {
                Node node = session.getNode(CONFIG_NODE_PATH);
                enabled = node.getProperty("hipposys:enabled").getBoolean();
            } catch (PathNotFoundException e) {
                log.debug("No such item: " + CONFIG_NODE_PATH + "/hipposys:enabled");
            } catch (RepositoryException e) {
                log.error("Exception while reading export enabled flag.", e);
            }
            return enabled;
        }

        private static String eventString(Event event) {
            switch (event.getType()) {
                case Event.NODE_ADDED:
                    return "Node added";
                case Event.NODE_REMOVED:
                    return "Node removed";
                case Event.PROPERTY_ADDED:
                    return "Property added";
                case Event.PROPERTY_CHANGED:
                    return "Property changed";
                case Event.PROPERTY_REMOVED:
                    return "Property removed";
            }
            return null;
        }

        /**
         * Sorts Events according to the following rules:
         * - Events on nodes are ordered before events on properties
         * - Shorter paths are ordered before longer paths
         */
        private static class EventComparator implements Comparator<Event> {
            @Override
            public int compare(Event e1, Event e2) {
                int compareType = compareType(e1, e2);
                return compareType == 0 ? comparePath(e1, e2) : compareType;
            }

            private static int compareType(Event e1, Event e2) {
                if (isNodeEventType(e1)) {
                    return isNodeEventType(e2) ? 0 : -1;
                } else {
                    return isNodeEventType(e2) ? 1 : 0;
                }
            }

            private static int comparePath(Event e1, Event e2) {
                try {
                    return e1.getPath().length() - e2.getPath().length();
                } catch (RepositoryException e) {
                    e.printStackTrace();
                }
                return 0;
            }

            private static boolean isNodeEventType(Event event) {
                return event.getType() == Event.NODE_ADDED
                        || event.getType() == Event.NODE_REMOVED;
            }
        }
    }
}
