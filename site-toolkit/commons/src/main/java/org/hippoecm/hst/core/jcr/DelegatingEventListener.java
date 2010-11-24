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

import java.util.List;

import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.hippoecm.hst.logging.Logger;
import org.hippoecm.hst.site.HstServices;

/**
 * DelegatingEventListener
 * <P>
 * An implementation of <CODE>javax.jcr.observation.EventListener</CODE>, delegating to
 * multiple listeners.
 * </P>
 * <P>
 * If there is only one delegatee event listener, then it just invoke the delegatee simply.
 * However, if there are more than one delegatee event listeners, this creates 
 * <CODE>org.hippoecm.core.jcr.RewindableEventIterator</CODE> to pass the event iterator
 * to each delegatee event listener.
 * </P>
 * 
 * @version $Id$
 */
public class DelegatingEventListener implements EventListener, EventListenersContainerListener {
    
    private static final String LOGGER_CATEGORY_NAME = DelegatingEventListener.class.getName();

    protected List<EventListener> delegatees;
    protected String [] skipPaths;
    protected int maxEvents;
    
    public DelegatingEventListener(List<EventListener> delegatees) {
        this.delegatees = delegatees;
    }
    
    public String [] getSkipPaths() {
        if (skipPaths == null) {
            return null;
        }
        
        String [] cloned = new String[skipPaths.length];
        System.arraycopy(skipPaths, 0, cloned, 0, skipPaths.length);
        return cloned;
    }
    
    public void setSkipPaths(String [] skipPaths) {
        if (skipPaths == null) {
            this.skipPaths = null;
        } else {
            this.skipPaths = new String[skipPaths.length];
            System.arraycopy(skipPaths, 0, this.skipPaths, 0, skipPaths.length);
        }
    }
    
    public int getMaxEvents() {
        return maxEvents;
    }
    
    public void setMaxEvents(int maxEvents) {
        this.maxEvents = maxEvents;
    }
    
    public void onEvent(EventIterator events) {
        if (delegatees == null || delegatees.isEmpty()) {
            return;
        }
        
        if (delegatees.size() == 1) {
            delegatees.get(0).onEvent(events);
        } else {
            RewindableEventIterator rewindableEvents = new RewindableEventIterator(events, maxEvents, skipPaths);
            Logger log = null;
            
            for (EventListener delegatee : delegatees) {
                try {
                    delegatee.onEvent(rewindableEvents);
                } catch (Exception e) {
                    if (log == null) {
                        log = HstServices.getLogger(LOGGER_CATEGORY_NAME);
                    }
                    if (log.isDebugEnabled()) {
                        log.warn("Exception during invoking delegatee event listener.", e);
                    } else {
                        log.warn("Exception during invoking delegatee event listener. {}", e.toString());
                    }
                } finally {
                    rewindableEvents.rewind();
                }
            }
        }
    }
    
    public void onEventListenersContainerStarted() {
        if (delegatees == null || delegatees.isEmpty()) {
            return;
        }
        
        Logger log = null;
        
        for (EventListener delegatee : delegatees) {
            if (delegatee instanceof EventListenersContainerListener) {
                try {
                    ((EventListenersContainerListener) delegatee).onEventListenersContainerStarted();
                } catch (Exception e) {
                    if (log == null) {
                        log = HstServices.getLogger(LOGGER_CATEGORY_NAME);
                    }
                    if (log.isDebugEnabled()) {
                        log.warn("Exception during invoking delegatee event listener (onEventListenersContainerStarted).", e);
                    } else {
                        log.warn("Exception during invoking delegatee event listener (onEventListenersContainerStarted). {}", e.toString());
                    }
                }
            }
        }
    }
    
    public void onEventListenersContainerRefreshed() {
        if (delegatees == null || delegatees.isEmpty()) {
            return;
        }
        
        Logger log = null;
        
        for (EventListener delegatee : delegatees) {
            if (delegatee instanceof EventListenersContainerListener) {
                try {
                    ((EventListenersContainerListener) delegatee).onEventListenersContainerRefreshed();
                } catch (Exception e) {
                    if (log == null) {
                        log = HstServices.getLogger(LOGGER_CATEGORY_NAME);
                    }
                    if (log.isDebugEnabled()) {
                        log.warn("Exception during invoking delegatee event listener (onEventListenersContainerRefreshed).", e);
                    } else {
                        log.warn("Exception during invoking delegatee event listener (onEventListenersContainerRefreshed). {}", e.toString());
                    }
                }
            }
        }
    }
    
    public void onEventListenersContainerStopped() {
        if (delegatees == null || delegatees.isEmpty()) {
            return;
        }
        
        Logger log = null;
        
        for (EventListener delegatee : delegatees) {
            if (delegatee instanceof EventListenersContainerListener) {
                try {
                    ((EventListenersContainerListener) delegatee).onEventListenersContainerStopped();
                } catch (Exception e) {
                    if (log == null) {
                        log = HstServices.getLogger(LOGGER_CATEGORY_NAME);
                    }
                    if (log.isDebugEnabled()) {
                        log.warn("Exception during invoking delegatee event listener (onEventListenersContainerStopped).", e);
                    } else {
                        log.warn("Exception during invoking delegatee event listener (onEventListenersContainerStopped). {}", e.toString());
                    }
                }
            }
        }
    }
}
