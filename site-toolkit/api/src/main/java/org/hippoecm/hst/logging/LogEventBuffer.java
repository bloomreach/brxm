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
package org.hippoecm.hst.logging;

import java.util.Iterator;

/**
 * Defines a collection that allows log events to be stored and removed in some well-defined order. 
 * 
 * @version $Id$
 */
public interface LogEventBuffer {
    
    /**
     * Adds the given log event to this buffer. 
     * @param event
     * @return
     */
    boolean add(LogEvent event);
    
    /**
     * Clears this buffer. 
     */
    void clear();
    
    /**
     * Returns the least recently inserted element in this buffer. 
     * @return
     */
    LogEvent get();
    
    /**
     * Returns true if this buffer is empty; false otherwise. 
     * @return
     */
    boolean isEmpty();
    
    /**
     * Returns true if this collection is full and no new elements can be added. 
     * @return
     */
    boolean isFull();
    
    /**
     * Returns an iterator over this buffer's elements. 
     * @return
     */
    Iterator<LogEvent> iterator();
    
    /**
     * Gets the maximum size of the collection (the bound). 
     * @return
     */
    int maxSize();
    
    /**
     * Removes the least recently inserted element from this buffer. 
     * @return
     */
    LogEvent remove();
    
    /**
     * Returns the number of elements stored in the buffer. 
     * @return
     */
    int size();
    
    /**
     * Sets log level to store.
     * @param level
     */
    void setLevel(LogEvent.Level level);
    
    /**
     * Sets log level by its name to store.
     * @param level
     */
    void setLevelByName(String levelName);
    
    /**
     * Returns log level
     * @return
     */
    LogEvent.Level getLevel();

    /**
     * Returns log level name
     * @return
     */
    String getLevelName();
    
}
