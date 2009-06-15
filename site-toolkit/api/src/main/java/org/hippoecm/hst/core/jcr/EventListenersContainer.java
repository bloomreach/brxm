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

/**
 * The container interface for {@link EventListenerItem} instances.
 * This is responsible for registering all the event listener item.
 * 
 * @version $Id$
 */
public interface EventListenersContainer {

    /**
     * Sets event listener items.
     * 
     * @param eventListenerItems
     */
    void setEventListenerItems(List<EventListenerItem> eventListenerItems);
    
    /**
     * Adds an event listener item.
     * 
     * @param eventListenerItem
     */
    void addEventListenerItem(EventListenerItem eventListenerItem);
    
    /**
     * Returns the event listener items.
     * @return
     */
    List<EventListenerItem> getEventListenerItems();
    
    /**
     * Registers all event listener items and
     * starts the event listener container.
     */
    void start();
    
    /**
     * Removes all event listener items and
     * stop the event listener container.
     */
    void stop();
    
}
