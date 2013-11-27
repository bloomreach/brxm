/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.testutils.slf4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LoggerRecordingWrapperTest
 */
public class LoggerRecordingWrapperTest {

    private static LoggerRecordingWrapper recordingLogger;

    @BeforeClass
    public static void beforeClass() throws Exception {
        recordingLogger = new LoggerRecordingWrapper(Hello.log);
        Hello.log = recordingLogger;
    }

    @Before
    public void before() throws Exception {
        recordingLogger.clearLogRecords();
    }

    @Test
    public void testSimpleLoggerRecording() throws Exception {
        Hello.leaveLogs();

        List<LogRecord> logRecords = recordingLogger.getLogRecords();

        assertEquals(3, logRecords.size());

        assertEquals(Level.INFO_INT, logRecords.get(0).getLevel());
        assertEquals("Hello, World!", logRecords.get(0).getFormattedMessage());

        assertEquals(Level.ERROR_INT, logRecords.get(1).getLevel());
        assertEquals("Hello, Error!", logRecords.get(1).getFormattedMessage());

        assertEquals(Level.ERROR_INT, logRecords.get(2).getLevel());
        assertEquals("Hello, Runtime Error!", logRecords.get(2).getFormattedMessage());
        assertTrue(logRecords.get(2).getStackTrace().contains("java.lang.RuntimeException: A RuntimeException from Hello"));
    }

    private static class Hello {

        static Logger log = LoggerFactory.getLogger(Hello.class);

        public static void leaveLogs() {
            log.info("Hello, {}!", "World");
            log.error("Hello, {}!", "Error");

            try {
                throw new RuntimeException("A RuntimeException from Hello");
            } catch (Exception e) {
                log.error("Hello, Runtime Error!", e);
            }
        }
    }

}
