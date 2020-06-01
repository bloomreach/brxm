/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.webfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.onehippo.repository.testutils.slf4j.LogRecord;
import org.onehippo.repository.testutils.slf4j.LoggerRecordingWrapper;
import org.slf4j.Logger;

import static org.junit.Assert.assertArrayEquals;

public class LogRecorder {

    private final LoggerRecordingWrapper recorder;

    public LogRecorder(final Logger log) {
        recorder = new LoggerRecordingWrapper(log);
    }

    public Logger getLogger() {
        return recorder;
    }

    public void clear() {
        recorder.clearLogRecords();
    }

    public void assertNoWarningsOrErrorsLogged() {
        assertLogged(Level.WARN);
        assertLogged(Level.ERROR);
    }

    public void assertLogged(final Level level, final String... expectedMessages) {
        List<String> loggedOfLevel = new ArrayList<>();
        for (LogRecord record : recorder.getLogRecords()) {
            if (record.getLevel() == level.intLevel()) {
                loggedOfLevel.add(record.getFormattedMessage());
            }
        }
        assertArrayEquals("expected logged " + level.name() + " messages:\n" +
                        Arrays.toString(expectedMessages) + "\nbut got:\n" + Arrays.toString(loggedOfLevel.toArray()) + "\n",
                expectedMessages, loggedOfLevel.toArray());
    }

}
