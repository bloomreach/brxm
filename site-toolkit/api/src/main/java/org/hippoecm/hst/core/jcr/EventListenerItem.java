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

import javax.jcr.observation.EventListener;

/**
 * Holds {@link EventListener} and its configuration information.
 * 
 * @version $Id$
 */
public interface EventListenerItem {

    /**
     * Returns the {@link EventListener} instance.
     * 
     * @return
     */
    EventListener getEventListener();
    
    /**
     * Returns the event type for the event listener.
     * @see {@link javax.jcr.observation.Event}
     * @see {@link javax.jcr.observation.ObservationManager#addEventListener(EventListener, int, String, boolean, String[], String[], boolean)}
     * @return
     */
    int getEventTypes();
    
    /**
     * Returns the absolute path for the event listener.
     * 
     * @see {@link javax.jcr.observation.ObservationManager#addEventListener(EventListener, int, String, boolean, String[], String[], boolean)}
     * @return
     */
    String getAbsolutePath();
    
    /**
     * Flag to check the event listener be registered with <CODE>deep</CODE> option. 
     *
     * @see {@link javax.jcr.observation.ObservationManager#addEventListener(EventListener, int, String, boolean, String[], String[], boolean)}
     * @return
     */
    boolean isDeep();

    /**
     * Returns the UUID array for the event listener.
     * 
     * @see {@link javax.jcr.observation.ObservationManager#addEventListener(EventListener, int, String, boolean, String[], String[], boolean)}
     * @return
     */
    String [] getUuids();

    /**
     * Returns the node type name array for the event listener.
     * 
     * @see {@link javax.jcr.observation.ObservationManager#addEventListener(EventListener, int, String, boolean, String[], String[], boolean)}
     * @return
     */
    String [] getNodeTypeNames();
    
    /**
     * Flag to check the event listener be registered with <CODE>noLocal</CODE> option.
     * 
     * @see {@link javax.jcr.observation.ObservationManager#addEventListener(EventListener, int, String, boolean, String[], String[], boolean)}
     * @return
     */
    boolean isNoLocal();
    
}
