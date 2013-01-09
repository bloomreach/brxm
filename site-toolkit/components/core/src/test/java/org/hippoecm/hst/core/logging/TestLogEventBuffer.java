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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.hippoecm.hst.logging.LogEvent;
import org.junit.Ignore;
import org.junit.Test;


public class TestLogEventBuffer {

    private static final int LOG_EVENT_BUFFER_SIZE = 10;
    
    private List<Exception> exceptions = Collections.synchronizedList(new LinkedList<Exception>());
    
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
    
    @Test
    public void testCircularFIFOLogEventBufferThreadSafety() throws Exception {
        final CircularFIFOLogEventBuffer logEventBuffer = new CircularFIFOLogEventBuffer(LOG_EVENT_BUFFER_SIZE);
        
        for (int i = 0; i < LOG_EVENT_BUFFER_SIZE; i++) {
            logEventBuffer.add(new LogEventImpl("testlog", LogEvent.Level.DEBUG, "test message"));
        }
        
        
        Thread [] workers = new Thread[100];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(new Runnable() {
                public void run() {
                    try {
                        for (int j = 0; j < 100; j++) {
                            logEventBuffer.add(new LogEventImpl("testlog", LogEvent.Level.DEBUG, "test message"));
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                }
            });
        }
        
        for (int i = 0; i < workers.length; i++) {
            workers[i].start();
        }
        
        try {
            for (int i = 0; i < workers.length; i++) {
                workers[i].join();
            }
        } catch (InterruptedException e) {
        }
        
        if (!exceptions.isEmpty()) {
            StringBuilder exInfo = new StringBuilder();
            for (Exception ex : exceptions) {
                exInfo.append("    " + ex.toString() + " " + ex.getMessage() + "\n");
            }
            fail("Failed to add buffer for some reasons:\n" + exInfo + "\n");
        }
    }
    
}
