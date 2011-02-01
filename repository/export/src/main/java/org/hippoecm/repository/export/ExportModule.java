/*
 *  Copyright 2011 Hippo (www.hippo.nl).
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
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jcr.NamespaceRegistry;
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
 * {@code hippo.config.dir}. This property specifies the directory where configuration
 * files should be written to. If no configuration has yet been defined in this directory
 * new hippoecm-extension.xml and related files will be created as necessary. 
 * Otherwise changes to the repository will be merged into the existing files and new files
 * will be created when necessary.
 * <p>
 */
public class ExportModule implements DaemonModule {

	
	// ---------- Static variables
	
    private static final Logger log = LoggerFactory.getLogger("org.hippoecm.repository.export");

    
    // ---------- Member variables
    
    private Session m_session;
    private Extension m_extension;
    private Future<?> m_future;
    
    
    // ---------- Constructor
    
    public ExportModule() {}

    
    // ---------- DaemonModule implementation

    @Override
    public void initialize(Session session) throws RepositoryException {
    	
    	m_session = session;
    	
    	// this module is enabled if system property 'hippo.config.dir' is set
    	String configDir = System.getProperty("hippo.config.dir");
        File configDirectory = null;
        if (configDir != null) {
            configDirectory = new File(configDir);
        } else {
            log.info("No config directory set. Automatic export will not be available.");
            return;
        }
        
        // initialize config directory
        if (!configDirectory.exists()) {
            configDirectory.mkdirs();
        }
        
        log.info("Automatically exporting changes to directory " + configDirectory.getPath());

        // create export project
        File extension = new File(configDirectory, "hippoecm-extension.xml");
        try {
            m_extension = new Extension(extension);
        } catch (IOException ex) {
            log.error("Cannot create project for export. "
                    + "Automatic export will not be available.", ex);
            return;
        } catch (DocumentException ex) {
            log.error("Cannot create project for export. "
                    + "Automatic export will not be available", ex);
            return;
        }

        // install event listener
        ExportEventListener listener = new ExportEventListener(m_extension, session);
        int eventTypes = Event.NODE_ADDED | Event.NODE_MOVED | Event.NODE_REMOVED
        		| Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;
        try {
        	ObservationManager manager = session.getWorkspace().getObservationManager();
            manager.addEventListener(listener, eventTypes, "/", true, null, null, false);
        } catch (RepositoryException ex) {
            log.error("Failed to set up export. Automatic export will not be available.", ex);
        }

        // schedule export task
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Runnable task = new Runnable() { @Override public void run() { m_extension.export(m_session); } };
        m_future = executor.scheduleAtFixedRate(task , 1, 1, TimeUnit.SECONDS);

    }

    @Override
    public void shutdown() {
    	if (m_future != null) {
        	m_future.cancel(false);
    	}
    }

    
    // ---------- Implementation
    
    /**
     * JCR EventListener. Listens to jcr change events on relevant nodes; checks if an appropriate instruction
     * exists for the event, creates one if none was found; decides what to do in order to persist the changes.
     */
    private static class ExportEventListener implements EventListener {

        private static final List<String> ignored = new ArrayList<String>(10);

        // TODO: add more ignored paths
        static {
            ignored.add("/jcr:system/jcr:versionStorage");
            ignored.add("/hippo:log");
            ignored.add("/live");
            ignored.add("/preview");
            ignored.add("/content/documents/state");
            ignored.add("/content/documents/tags");
        }
        
        private final Extension m_extension;
        private final Session m_session;
        private final Set<String> m_uris;

        private ExportEventListener(Extension extension, Session session) throws RepositoryException {
            m_extension = extension;
            m_session = session;
            String[] uris = m_session.getWorkspace().getNamespaceRegistry().getURIs();
            m_uris = new HashSet<String>(uris.length+10);
            Collections.addAll(m_uris, uris);
        }

        @Override
        public void onEvent(EventIterator iter) {
        	List<Event> events = sortEvents(iter);
            for(Event event : events) {
                try {
                    String path = event.getPath();
                    if (ignore(path)) {
                    	log.debug("Ignoring event on " + path);
                    	continue;
                    }
                    ResourceInstruction instruction = m_extension.findResourceInstruction(path);
                    switch (event.getType()) {
                    case Event.NODE_ADDED : {
                        log.debug("Node added on " + path);
                        if (instruction != null) {
                        	log.debug("Found instruction " + instruction);
                        	instruction.nodeAdded(path);
                        }
                        else {
                        	instruction = m_extension.createResourceInstruction(path);
                            log.debug("Adding instruction " + instruction);
                        	m_extension.addInstruction(instruction);
                        }
                        break;
                    }
                    case Event.NODE_REMOVED : {
                    	log.debug("Node removed on " + path);
                    	if (instruction != null) {
                    		log.debug("Found instruction " + instruction);
                    		if (instruction.nodeRemoved(path)) {
                				// the context node was removed, remove the instruction
                				m_extension.removeInstruction(instruction);
                    		}
                    	}
                    	else {
                    		// this use case seems not to be covered by the import functionality 
                    		// anymore now that contentdelete is deprecated
                    		log.debug("No instruction to update. This change will be lost.");
                    	}
                    	break;
                    }
                    case Event.NODE_MOVED : {
                    	String srcPath = m_session.getNodeByIdentifier(event.getIdentifier()).getPath();
                    	log.debug("Node moved from " + srcPath + " to " + path);
                    	// path was added
                    	if (instruction != null) {
                    		log.debug("Found instruction " + instruction);
                    		instruction.nodeAdded(path);
                    	}
                    	else {
                        	instruction = m_extension.createResourceInstruction(path);
                            log.debug("Adding instruction " + instruction);
                        	m_extension.addInstruction(instruction);
                    	}
                    	
                    	// srcPath was removed
                    	instruction = m_extension.findResourceInstruction(srcPath);
                    	if (instruction != null) {
                    		log.debug("Found instruction " + instruction);
                    		if (instruction.nodeRemoved(srcPath)) {
                    			// the context node was removed, remove instruction
                				m_extension.removeInstruction(instruction);
                    		}
                    	}
                    	else {
                    		log.debug("No instruction to update. This change will be lost.");
                    	}
                    	break;
                    }
                    case Event.PROPERTY_ADDED : {
                    	log.debug("Property added on " + path);
                        if (instruction != null) {
                        	log.debug("Found instruction " + instruction);
                        	instruction.propertyAdded(path);
                        }
                        break;
                    }
                    case Event.PROPERTY_CHANGED : {
                    	log.debug("Property changed on " + path);
                        if (instruction != null) {
                        	log.debug("Found instruction " + instruction);
                        	instruction.propertyChanged(path);
                        }
                        break;
                    }
                    case Event.PROPERTY_REMOVED : {
                    	log.debug("Property removed on " + path);
                        if (instruction != null) {
                        	log.debug("Found instruction " + instruction);
                        	instruction.propertyRemoved(path);
                        }
                        break;
                    }
                    }
                } catch (RepositoryException ex) {
                    log.error("Failed to process repository event.", ex);
                }
            }
            
            // now check if the namespace registry has changed
            try {
            	Set<String> uris = new HashSet<String>(m_uris.size()+2);
            	NamespaceRegistry registry = m_session.getWorkspace().getNamespaceRegistry();
				Collections.addAll(uris, registry.getURIs());
				// Were any namespaces added?
				for (String uri : uris) {
					if (!m_uris.contains(uri)) {
						log.debug("New namespace detected.");
						NamespaceInstruction instruction = m_extension.findNamespaceInstruction(uri);
						if (instruction != null) {
							// remove the outdated namespace instruction
							log.debug("Removing instruction " + instruction);
							m_extension.removeInstruction(instruction);
						}
						instruction = m_extension.createNamespaceInstruction(uri, registry.getPrefix(uri));
                        log.debug("Adding instruction " + instruction);
                    	m_extension.addInstruction(instruction);
						m_uris.add(uri);
					}
				}
				// NOTE: no need for checking removal of namespaces.
				// Jackrabbit doesn't support that
			} catch (RepositoryException e) {
				log.error("Failed to update namespace instructions.", e);
			} catch (IOException e) {
				log.error("Failed to update namespace instructions.", e);
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
                
        private List<Event> sortEvents(EventIterator events) {
        	List<Event> list = new ArrayList<Event>(20);
        	while (events.hasNext()) {
        		list.add(events.nextEvent());
        	}
        	Collections.sort(list, new EventComparator());
        	return list;
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
				}
				else {
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
					|| event.getType() == Event.NODE_MOVED 
					|| event.getType() == Event.NODE_REMOVED;
			}
        }
    }

}
