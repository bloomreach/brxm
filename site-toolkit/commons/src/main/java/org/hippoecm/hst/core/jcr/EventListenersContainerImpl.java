/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.jcr;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.hippoecm.hst.logging.Logger;
import org.hippoecm.hst.logging.LoggerFactory;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.NodeUtils;

/**
 * EventListenersContainerImpl
 * 
 * @version $Id$
 */
public class EventListenersContainerImpl implements EventListenersContainer {

    private static final String LOGGER_CATEGORY_NAME = EventListenersContainerImpl.class.getName();
    
    private static int eventListenersContainerSessionCheckerIndex;
    
    protected String name;
    protected Repository repository;
    protected Credentials credentials;
    protected Session session;
    protected boolean sessionLiveCheck;
    protected long sessionLiveCheckIntervalOnStartup = 3000L;
    protected long sessionLiveCheckInterval = 60000L;
    protected Workspace workspace;
    protected ObservationManager observationManager;
    protected List<EventListenerItem> eventListenerItems = Collections.synchronizedList(new LinkedList<EventListenerItem>());

    protected boolean firstInitializationDone;
    protected EventListenersContainerSessionChecker eventListenersContainerSessionChecker;
    protected volatile boolean stopped;
    protected LoggerFactory loggerFactory;
    
    public EventListenersContainerImpl() {
        this(null);
    }
    
    public EventListenersContainerImpl(String name) {
        this.name = name;
    }
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public List<EventListenerItem> getEventListenerItems() {
        if (eventListenerItems == null) {
            return Collections.emptyList();
        }
        
        List<EventListenerItem> copiedEventListenerItems = null;
        
        synchronized (eventListenerItems) {
            copiedEventListenerItems = new LinkedList<EventListenerItem>(eventListenerItems);
        }
        
        return copiedEventListenerItems;
    }

    public synchronized void setEventListenerItems(List<EventListenerItem> eventListenerItems) {
        this.eventListenerItems = eventListenerItems;
    }
    
    public synchronized void addEventListenerItem(EventListenerItem eventListenerItem) {
        if (eventListenerItems == null) {
            eventListenerItems = Collections.synchronizedList(new LinkedList<EventListenerItem>());
        }
        
        eventListenerItems.add(eventListenerItem);
    }
    
    public boolean removeEventListenerItem(EventListenerItem eventListenerItem) {
        if (eventListenerItems == null) {
            return false;
        }
        
        return eventListenerItems.remove(eventListenerItem);
    }
    
    public void setSessionLiveCheck(boolean sessionLiveCheck) {
        this.sessionLiveCheck = sessionLiveCheck;
    }

    public void setSessionLiveCheckIntervalOnStartup(long sessionLiveCheckIntervalOnStartup) {
        this.sessionLiveCheckIntervalOnStartup = sessionLiveCheckIntervalOnStartup;
    }

    public void setSessionLiveCheckInterval(long sessionLiveCheckInterval) {
        this.sessionLiveCheckInterval = sessionLiveCheckInterval;
    }
    
    public void setLoggerFactory(LoggerFactory loggerFactory) {
        this.loggerFactory = loggerFactory;
    }
    
    protected Logger getLogger() {
        if (loggerFactory != null) {
            return loggerFactory.getLogger(LOGGER_CATEGORY_NAME);
        } else {
            return HstServices.getLogger(LOGGER_CATEGORY_NAME);
        }
    }
    
    public synchronized void start() {
        if (!this.sessionLiveCheck) {
            this.stopped = false;
            doDeinit();
            doInit();
        } else {
            this.stopped = true;
            if (eventListenersContainerSessionChecker != null) {
                if (eventListenersContainerSessionChecker.isAlive()) {
                    try {
                        this.eventListenersContainerSessionChecker.interrupt();
                        this.eventListenersContainerSessionChecker.join(10000L);
                        getLogger().debug("EventListenersContainerSessionChecker is interrupted on start: {}", this.eventListenersContainerSessionChecker);
                    } catch (Exception e) {
                        Logger log = getLogger();
                        
                        if (log.isDebugEnabled()) {
                            log.warn("Exception occurred during interrupting eventListenersContainerSessionChecker thread.", e);
                        } else if (log.isWarnEnabled()) {
                            log.warn("Exception occurred during interrupting eventListenersContainerSessionChecker thread. {}", e.toString());
                        }
                    }
                }
                eventListenersContainerSessionChecker = null;
            }
            doDeinit();
            this.stopped = false;
            eventListenersContainerSessionChecker = new EventListenersContainerSessionChecker();
            getLogger().debug("EventListenersContainerSessionChecker is started: {}", this);
            eventListenersContainerSessionChecker.start();
        }
    }

    protected void doInit() {
        Logger log = getLogger();
        
        if (log.isDebugEnabled())
            log.debug("EventListenersContainer will initialize itself.");

        try {
            if (this.credentials == null) {
                session = this.repository.login();
            } else {
                session = this.repository.login(this.credentials);
            }
            this.workspace = session.getWorkspace();
            this.observationManager = this.workspace.getObservationManager();
            
            for (EventListenerItem item : getEventListenerItems()) {

                EventListener eventListener = item.getEventListener();
                int eventTypes = item.getEventTypes();
                String absolutePath = item.getAbsolutePath();
                boolean isDeep = item.isDeep();
                String[] uuids = item.getUuids();
                String[] nodeTypeNames = item.getNodeTypeNames();
                boolean noLocal = item.isNoLocal();

                if (eventListener == null) {
                    if (log.isWarnEnabled())
                        log.warn("event listener object is null. Just ignored.");
                    continue;
                }
                
                if (eventTypes <= 0) {
                    if (log.isWarnEnabled())
                        log.warn("event listener's event types is invalid: {}. Just ignored.", eventTypes);
                    continue;
                }

                try {
                    this.observationManager.addEventListener(eventListener, eventTypes, absolutePath, isDeep, uuids,
                            nodeTypeNames, noLocal);
                } catch (RepositoryException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to register event listener '" + eventListener + "': " + e, e);
                    } else {
                        log.warn("Failed to register event listener '" + eventListener + "': " + e);
                    };
                    continue;
                }
                
                boolean itemExistsOnAbsolutePath = false;
                
                try {
                    itemExistsOnAbsolutePath = session.itemExists(absolutePath);
                    if(itemExistsOnAbsolutePath) {
                        Item jcrItem = session.getItem(absolutePath);
                        if(!jcrItem.isNode()) {
                            jcrItem = jcrItem.getParent();
                        }
                        Node canonical = NodeUtils.getCanonicalNode((Node) jcrItem);
                        if(canonical == null || !canonical.isSame(jcrItem)) {
                            log.warn("An event handler will be registered for a virtual node. Virtual nodes never have events. You should take the canonical location most likely. Virtual path = " + absolutePath);
                        }
                        
                    }
                } catch (Exception anyEx) {
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to check if item exists on " + absolutePath + ": " + anyEx, anyEx);
                    } else {
                        log.warn("Failed to check if item exists on " + absolutePath + ": " + anyEx);
                    }
                }
                
                if (!itemExistsOnAbsolutePath) {
                    log.warn("An event handler will be registered for a path where no node currently is available: " + absolutePath);
                }
                
                if (log.isInfoEnabled()) {
                    log.info("An event listener registered: listener=" + eventListener + ", eventTypes=" + eventTypes
                            + ", absolutePath=" + absolutePath + ", isDeep=" + isDeep + ", uuids=" + Arrays.toString(uuids)
                            + ", nodeTypeNames=" + Arrays.toString(nodeTypeNames) + ", noLocal=" + noLocal);
                }
            }
            
            if (!firstInitializationDone) {
                firstInitializationDone = true;
                
                for (EventListenerItem item : getEventListenerItems()) {
                    EventListener eventListener = item.getEventListener();
                    
                    if (eventListener instanceof EventListenersContainerListener) {
                        try {
                            ((EventListenersContainerListener) eventListener).onEventListenersContainerStarted();
                        } catch (Exception elcle) {
                            if (log.isDebugEnabled()) {
                                log.warn("Failed to fire started event. " + elcle, elcle);
                            } else {
                                log.warn("Failed to fire started event. " + elcle);
                            }
                        }
                    }
                }
                
                log.info("EventListenersContainer's initialization done.");
            } else {
                for (EventListenerItem item : getEventListenerItems()) {
                    EventListener eventListener = item.getEventListener();
                    
                    if (eventListener instanceof EventListenersContainerListener) {
                        try {
                            ((EventListenersContainerListener) eventListener).onEventListenersContainerRefreshed();
                        } catch (Exception elcle) {
                            if (log.isDebugEnabled()) {
                                log.warn("Failed to fire refreshed event. " + elcle, elcle);
                            } else {
                                log.warn("Failed to fire refreshed event. " + elcle);
                            }
                        }
                    }
                }
                
                log.info("EventListenersContainer's initialization done again.");
            }
            
        } catch (LoginException e) {
            if(firstInitializationDone) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to get a session in EventListenersContainer. The repository might be not available yet or the credentials might be wrong. It will try initialization next time. " + e, e);
                } else {
                    log.warn("Failed to get a session in EventListenersContainer. The repository might be not available yet or the credentials might be wrong. It will try initialization next time. " + e);
                }
            } else {
                log.info("Could not yet get a session in the EventListenersContainer. The repository still needs to be started. Will try again in '{}' ms.", String.valueOf(sessionLiveCheckIntervalOnStartup));
            }
        } catch (RepositoryException e) {
            if(firstInitializationDone) {
                if (log.isDebugEnabled()) {
                    log.warn("The repository is not available. It will try initialization next time. " + e, e);
                } else {
                    log.warn("The repository is not available. {} {}", "It will try initialization next time.", e);
                }
            } else {
                 log.info("The repository is not yet available. The repository still needs to be started. Will try again in '{}' ms.", String.valueOf(sessionLiveCheckIntervalOnStartup));
            }
        }
    }

    public synchronized void stop() {
        this.stopped = true;
        
        doDeinit();
        
        if (eventListenersContainerSessionChecker != null) {
            if (eventListenersContainerSessionChecker.isAlive()) {
                try {
                    this.eventListenersContainerSessionChecker.interrupt();
                    this.eventListenersContainerSessionChecker.join(10000L);
                    getLogger().debug("EventListenersContainerSessionChecker is interrupted on stop: {}", this.eventListenersContainerSessionChecker);
                } catch (Exception e) {
                    Logger log = getLogger();
                    
                    if (log.isDebugEnabled()) {
                        log.warn("Exception occurred during interrupting eventListenersContainerSessionChecker thread", e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Exception occurred during interrupting eventListenersContainerSessionChecker thread. {}", e.toString());
                    }
                }
            }
            eventListenersContainerSessionChecker = null;
        }
        
        for (EventListenerItem item : getEventListenerItems()) {
            EventListener eventListener = item.getEventListener();
            
            if (eventListener instanceof EventListenersContainerListener) {
                try {
                    ((EventListenersContainerListener) eventListener).onEventListenersContainerStopped();
                } catch (Exception elcle) {
                    Logger log = getLogger();
                    
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to fire stopped event. " + elcle, elcle);
                    } else {
                        log.warn("Failed to fire stopped event. " + elcle);
                    }
                }
            }
        }
    }
    
    protected void doDeinit() {
        if (this.observationManager != null) {
            for (EventListenerItem item : getEventListenerItems()) {
                try {
                    EventListener eventListener = item.getEventListener();
                    if (eventListener == null) {
                        Logger log = getLogger();
                        if (log.isWarnEnabled()) {
                            log.warn("event listener object is null. Just ignored.");
                        }
                    }
                    else {
                        this.observationManager.removeEventListener(eventListener);
                    }
                } catch (Exception e) {
                    Logger log = getLogger();
                    if (log.isWarnEnabled()) {
                        log.warn("Cannot remove event listener. {}", e.toString());
                    }
                }
            }
        }

        if (this.session != null) {
            try {
                this.session.logout();
            } catch (Exception ce) {
                getLogger().debug("Exception while logging out jcr session: {}", ce.toString());
            }
        }

        this.observationManager = null;
        this.workspace = null;
        this.session = null;
    }
    
    protected class EventListenersContainerSessionChecker extends Thread {

        protected EventListenersContainerSessionChecker() {
            super((name != null ? name + "::" : "") + "EventListenersContainerSessionChecker-" + (++eventListenersContainerSessionCheckerIndex));
            setDaemon(true);
        }

        public void run() {
            
            getLogger().debug("EventListenersContainerSessionChecker starts running: {}", this);
            
            while (!EventListenersContainerImpl.this.stopped) {
                boolean isSessionLive = false;
                
                try {
                    if(EventListenersContainerImpl.this.session != null){
                        isSessionLive = EventListenersContainerImpl.this.session.isLive();
                    }
                } catch (Exception e) {
                    getLogger().debug("Exception while checking jcr session: {}", e.toString());
                }
                
                if (EventListenersContainerImpl.this.stopped) {
                    break;
                }
                
                if (EventListenersContainerImpl.this.session == null || !isSessionLive) {
                    if (!EventListenersContainerImpl.this.stopped) {
                        doDeinit();
                        if (EventListenersContainerImpl.this.stopped) {
                            break;
                        }
                        doInit();
                        if (EventListenersContainerImpl.this.stopped) {
                            break;
                        }
                    }
                }

                synchronized (this) {
                    try {
                        wait(firstInitializationDone ? EventListenersContainerImpl.this.sessionLiveCheckInterval
                                : EventListenersContainerImpl.this.sessionLiveCheckIntervalOnStartup);
                    } catch (InterruptedException e) {
                        if (EventListenersContainerImpl.this.stopped) {
                            break;
                        }
                    }
                }
            }
            
            getLogger().debug("EventListenersContainerSessionChecker stops running: {}", this);
        }
    }

}