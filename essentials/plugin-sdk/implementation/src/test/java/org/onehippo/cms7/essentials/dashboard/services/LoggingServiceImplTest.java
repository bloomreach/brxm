/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.services;

import java.io.File;

import org.junit.Test;
import org.onehippo.cms7.essentials.ResourceModifyingTest;
import org.onehippo.cms7.essentials.dashboard.utils.Dom4JUtils;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LoggingServiceImplTest extends ResourceModifyingTest {

    private LoggingServiceImpl service = new LoggingServiceImpl();

    @Test
    public void add_logger_new() throws Exception {
        final File config = createModifiableFile("/services/logging/log4j2.xml", "dummy.xml");

        assertEquals(0, nrOfOccurrences(config, "<Logger name=\"testLogger\" level=\"info\"/>"));
        assertTrue(service.addLoggerToLog4jConfiguration(config, "testLogger", "info"));
        assertEquals(1, nrOfOccurrences(config, "<Logger name=\"testLogger\" level=\"info\"/>"));
    }

    @Test
    public void add_logger_override_level() throws Exception {
        final File config = createModifiableFile("/services/logging/log4j2.xml", "dummy.xml");

        assertEquals(1, nrOfOccurrences(config, "<Logger name=\"org.hippoecm.hst\""));
        assertEquals(1, nrOfOccurrences(config, "<Logger name=\"org.hippoecm.hst\" level=\"warn\"/>"));
        assertTrue(service.addLoggerToLog4jConfiguration(config, "org.hippoecm.hst", "info"));
        assertEquals(1, nrOfOccurrences(config, "<Logger name=\"org.hippoecm.hst\""));
        assertEquals(1, nrOfOccurrences(config, "<Logger name=\"org.hippoecm.hst\" level=\"info\"/>"));
    }

    @Test
    public void add_logger_no_loggers() throws Exception {
        final File config = createModifiableFile("/services/logging/log4j2-no-loggers.xml", "dummy.xml");

        assertTrue(service.addLoggerToLog4jConfiguration(config, "org.hippoecm.hst", "info"));
        assertEquals(1, nrOfOccurrences(config, "<Logger name=\"org.hippoecm.hst\" level=\"info\"/>"));
    }

    @Test
    public void add_logger_invalid_xml() throws Exception {
        final File config = createModifiableFile("/services/logging/log4j2-invalid.xml", "dummy.xml");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(Dom4JUtils.class).build()) {
            assertFalse(service.addLoggerToLog4jConfiguration(config, "org.hippoecm.hst", "info"));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Failed to update XML file")));
        }
    }
}
