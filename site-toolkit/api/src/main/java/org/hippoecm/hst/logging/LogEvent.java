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

import java.io.Serializable;

/**
 * The representation of logging events. When an affirmative
 * decision is made to log then a <code>LogEvent</code> instance
 * is created.
 * 
 * @version $Id$
 */
public interface LogEvent extends Serializable {
    
    /**
     * Defines the minimum set of levels recognized by the system, that is
     * <code>ERROR</code>, <code>WARN</code>, <code>INFO</code, <code>DEBUG</code>.
     */
    enum Level {
        ERROR,
        WARN,
        INFO,
        DEBUG
    }
    
    /**
     * Gets the logger of the event.
     */
    String getLoggerName();
    
    /**
     * Return the level of this event.  
     */
    Level getLevel();
    
    /**
     * Return the thread name of this event.  
     */
    String getThreadName();
    
    /**
     * Getter for the event's time stamp. The time stamp is calculated starting
     * from 1970-01-01 GMT.
     */
    long getTimestamp();
    
    /**
     * Return the message for this logging event.
     */
    String getMessage();
    
}
