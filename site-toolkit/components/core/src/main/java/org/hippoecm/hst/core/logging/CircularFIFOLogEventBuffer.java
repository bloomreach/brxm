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
package org.hippoecm.hst.core.logging;

import java.util.Iterator;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.hippoecm.hst.logging.LogEvent;
import org.hippoecm.hst.logging.LogEventBuffer;

public class CircularFIFOLogEventBuffer implements LogEventBuffer {
    
    protected CircularFifoBuffer buffer;
    protected LogEvent.Level logLevel = LogEvent.Level.DEBUG;
    
    public CircularFIFOLogEventBuffer(int size) {
        buffer = new CircularFifoBuffer(size);
    }

    public boolean add(LogEvent event) {
        boolean appendable = true;
        LogEvent.Level level = event.getLevel();
        
        if (logLevel == LogEvent.Level.INFO && level == LogEvent.Level.DEBUG) {
            appendable = false;
        } else if (logLevel == LogEvent.Level.WARN && (level == LogEvent.Level.DEBUG || level == LogEvent.Level.INFO)) {
            appendable = false;
        } else if (logLevel == LogEvent.Level.ERROR && (level == LogEvent.Level.DEBUG || level == LogEvent.Level.INFO || level == LogEvent.Level.WARN)) {
            appendable = false;
        }
        
        if (appendable) {
            synchronized (this) {
                return buffer.add(event);
            }
        } else {
            return false;
        }
    }

    public void clear() {
        buffer.clear();
    }

    public LogEvent get() {
        return (LogEvent) buffer.get();
    }

    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    public boolean isFull() {
        return buffer.isFull();
    }

    public Iterator<LogEvent> iterator() {
        return (Iterator<LogEvent>) buffer.iterator();
    }

    public int maxSize() {
        return buffer.maxSize();
    }

    public LogEvent remove() {
        return (LogEvent) buffer.remove();
    }

    public int size() {
        return buffer.size();
    }

    public void setLevel(LogEvent.Level logLevel) {
        this.logLevel = logLevel;
    }
    
    public void setLevelByName(String logLevelName) {
        if ("DEBUG".equals(logLevelName)) {
            this.logLevel = LogEvent.Level.DEBUG;
        } else if ("INFO".equals(logLevelName)) {
            this.logLevel = LogEvent.Level.INFO;
        } else if ("WARN".equals(logLevelName)) {
            this.logLevel = LogEvent.Level.WARN;
        } else if ("ERROR".equals(logLevelName)) {
            this.logLevel = LogEvent.Level.ERROR;
        }
    }
    
    public LogEvent.Level getLevel() {
        return this.logLevel;
    }

    public String getLevelName() {
        return this.logLevel.toString();
    }
    
}
