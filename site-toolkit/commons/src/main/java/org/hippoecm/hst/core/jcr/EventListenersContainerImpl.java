/*
 *  Copyright 2008 Hippo.
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

import java.util.ArrayList;
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
import org.hippoecm.repository.api.HippoNode;

public class EventListenersContainerImpl implements EventListenersContainer {

    private static final String LOGGER_CATEGORY_NAME = EventListenersContainerImpl.class.getName();

    protected Repository repository;
    protected Credentials credentials;
    protected Session session;
    protected boolean sessionLiveCheck;
    protected long sessionLiveCheckIntervalOnStartup = 3000L;
    protected long sessionLiveCheckInterval = 60000L;
    protected Workspace workspace;
    protected ObservationManager observationManager;
    protected List<EventListenerItem> eventListenerItems = new ArrayList<EventListenerItem>();

    protected boolean firstInitializationDone;
    protected EventListenersContainerSessionChecker eventListenersContainerSessionChecker;
    protected volatile boolean stopped;
    protected LoggerFactory loggerFactory;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public List<EventListenerItem> getEventListenerItems() {
        return this.eventListenerItems;
    }

    public void setEventListenerItems(List<EventListenerItem> eventListenerItems) {
        this.eventListenerItems = eventListenerItems;
    }
    
    public void addEventListenerItem(EventListenerItem eventListenerItem) {
        this.eventListenerItems.add(eventListenerItem);
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
    
    public void start() {
        this.stopped = false;
        
        if (!this.sessionLiveCheck) {
            doInit();
        } else {
            this.eventListenersContainerSessionChecker = new EventListenersContainerSessionChecker();
            this.eventListenersContainerSessionChecker.start();
        }
    }

    protected void doInit() {
        Logger log = getLogger();
        
        if (log.isDebugEnabled())
            log.debug("EventListenersContainer will initialize itself.");

        doDeinit();

        try {
            if (this.credentials == null) {
                session = this.repository.login();
            } else {
                session = this.repository.login(this.credentials);
            }
            this.workspace = session.getWorkspace();
            this.observationManager = this.workspace.getObservationManager();

            for (EventListenerItem item : this.eventListenerItems) {

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

                this.observationManager.addEventListener(eventListener, eventTypes, absolutePath, isDeep, uuids,
                        nodeTypeNames, noLocal);
                
                boolean itemExistsOnAbsolutePath = false;
                
                try {
                    itemExistsOnAbsolutePath = session.itemExists(absolutePath);
                    if(itemExistsOnAbsolutePath) {
                        Item jcrItem = session.getItem(absolutePath);
                        if(!jcrItem.isNode()) {
                            jcrItem = jcrItem.getParent();
                        }
                        Node canonical = ((HippoNode)jcrItem).getCanonicalNode();
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
                            + ", absolutePath=" + absolutePath + ", isDeep=" + isDeep + ", uuids=" + uuids
                            + ", nodeTypeNames=" + nodeTypeNames + ", noLocal=" + noLocal);
                }
            }

            this.firstInitializationDone = true;

            if (log.isInfoEnabled()) {
                log.info("- INFO: EventListenersContainer's initialization done.");
            } else {
                log.warn("- INFO: EventListenersContainer's initialization done.");
            }
        } catch (LoginException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to get a session in EventListenersContainer. The repository might be not available yet or the credentials might be wrong. It will try initialization next time. " + e, e);
            } else {
                log.warn("Failed to get a session in EventListenersContainer. The repository might be not available yet or the credentials might be wrong. It will try initialization next time. " + e);
            }
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("The repository is not available. It will try initialization next time. " + e, e);
            } else {
                log.warn("The repository is not available. {} {}", "It will try initialization next time.", e);
            }
        }
    }

    public void stop() {
        this.stopped = true;
        
        doDeinit();
        
        if (this.eventListenersContainerSessionChecker != null) {
            try {
                this.eventListenersContainerSessionChecker.interrupt();
            } catch (Exception e) {
                Logger log = getLogger();
                
                if (log.isDebugEnabled()) {
                    log.warn("Exception occurred during interrupting eventListenersContainerSessionChecker thread", e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Exception occurred during interrupting eventListenersContainerSessionChecker thread. {}", e.toString());
                }
            }
            this.eventListenersContainerSessionChecker = null;
        }
    }
    
    protected void doDeinit() {
        if (this.observationManager != null && this.eventListenerItems != null) {
            for (EventListenerItem item : this.eventListenerItems) {
                try {
                    this.observationManager.removeEventListener(item.getEventListener());
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
            }
        }

        this.observationManager = null;
        this.workspace = null;
        this.session = null;
    }
    
    private class EventListenersContainerSessionChecker extends Thread {

        private EventListenersContainerSessionChecker() {
            super("EventListenersContainerSessionChecker");
        }

        public void run() {
            while (!EventListenersContainerImpl.this.stopped) {
                if (EventListenersContainerImpl.this.session == null
                        || !EventListenersContainerImpl.this.session.isLive()) {
                    doInit();
                }

                synchronized (this) {
                    try {
                        wait(firstInitializationDone ? EventListenersContainerImpl.this.sessionLiveCheckInterval
                                : EventListenersContainerImpl.this.sessionLiveCheckIntervalOnStartup);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

}