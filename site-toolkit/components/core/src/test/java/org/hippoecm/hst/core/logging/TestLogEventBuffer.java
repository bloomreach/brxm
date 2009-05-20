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
package org.hippoecm.hst.core.logging;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.hippoecm.hst.logging.LogEvent;
import org.junit.Test;

public class TestLogEventBuffer {

    private static final int LOG_EVENT_BUFFER_SIZE = 10;
    
    @Test
    public void testCircularFIFOLogEventBuffer() {
        
        CircularFIFOLogEventBuffer logEventBuffer = new CircularFIFOLogEventBuffer(LOG_EVENT_BUFFER_SIZE);
        assertTrue("The log event buffer does not look empty.", logEventBuffer.isEmpty());
        assertFalse("The log event buffer looks full.", logEventBuffer.isFull());
        assertTrue("The maxSize is not " + LOG_EVENT_BUFFER_SIZE, logEventBuffer.maxSize() == LOG_EVENT_BUFFER_SIZE);
        assertTrue("The size is not zero", logEventBuffer.size() == 0);

        LogEvent logEvent = new LogEventImpl("testlog", LogEvent.Level.DEBUG, "test message");
        logEventBuffer.add(logEvent);
        assertFalse("The log event buffer looks empty.", logEventBuffer.isEmpty());
        assertFalse("The log event buffer looks full.", logEventBuffer.isFull());
        assertTrue("The maxSize is not " + LOG_EVENT_BUFFER_SIZE, logEventBuffer.maxSize() == LOG_EVENT_BUFFER_SIZE);
        assertTrue("The size is not one", logEventBuffer.size() == 1);
        
        logEventBuffer.clear();
        assertTrue("The log event buffer does not look empty.", logEventBuffer.isEmpty());
        assertFalse("The log event buffer looks full.", logEventBuffer.isFull());
        assertTrue("The maxSize is not " + LOG_EVENT_BUFFER_SIZE, logEventBuffer.maxSize() == LOG_EVENT_BUFFER_SIZE);
        assertTrue("The size is not zero", logEventBuffer.size() == 0);
        
        logEventBuffer.add(logEvent);
        LogEvent retrieved = logEventBuffer.get();
        assertTrue("The retrieved log event is not the same.", logEvent == retrieved);
        LogEvent removed = logEventBuffer.remove();
        assertTrue("The removed log event is not the same.", logEvent == removed);
        assertTrue("The maxSize is not " + LOG_EVENT_BUFFER_SIZE, logEventBuffer.maxSize() == LOG_EVENT_BUFFER_SIZE);
        assertTrue("The size is not zero", logEventBuffer.size() == 0);
        
        LogEvent [] logEvents = new LogEvent[LOG_EVENT_BUFFER_SIZE];
        for (int i = 0; i < LOG_EVENT_BUFFER_SIZE; i++) {
            logEvents[i] = new LogEventImpl("testlog", LogEvent.Level.DEBUG, "test message " + i);
            logEventBuffer.add(logEvents[i]);
            assertTrue("The size is not " + (i + 1), logEventBuffer.size() == i + 1);
        }
        assertTrue("The log event buffer does not look full.", logEventBuffer.isFull());
        assertTrue("The maxSize is not " + LOG_EVENT_BUFFER_SIZE, logEventBuffer.maxSize() == LOG_EVENT_BUFFER_SIZE);
        assertTrue("The size is not not " + LOG_EVENT_BUFFER_SIZE, logEventBuffer.size() == LOG_EVENT_BUFFER_SIZE);

        int index = 0;
        for (Iterator<LogEvent> it = logEventBuffer.iterator(); it.hasNext(); index++) {
            LogEvent item = it.next();
            assertTrue("The insertion order is not kept.", item == logEvents[index]);
        }
        
        logEvent = new LogEventImpl("testlog", LogEvent.Level.DEBUG, "test message");
        logEventBuffer.add(logEvent);
        assertTrue("The maxSize is not " + LOG_EVENT_BUFFER_SIZE, logEventBuffer.maxSize() == LOG_EVENT_BUFFER_SIZE);
        assertTrue("The size is not not " + LOG_EVENT_BUFFER_SIZE, logEventBuffer.size() == LOG_EVENT_BUFFER_SIZE);
        index = 0;
        for (Iterator<LogEvent> it = logEventBuffer.iterator(); it.hasNext(); index++) {
            LogEvent item = it.next();
            if (index < LOG_EVENT_BUFFER_SIZE - 1) {
                assertTrue("The rolling over doesn't work.", item == logEvents[index + 1]);
            } else {
                assertTrue("The last is not the same as the added one.", item == logEvent);
            }
        }
        
    }
    
}
