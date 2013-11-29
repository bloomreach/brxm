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
package org.onehippo.repository.scxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.scxml2.model.SCXML;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.testutils.slf4j.LogRecord;
import org.onehippo.repository.testutils.slf4j.LoggerRecordingWrapper;

/**
 * RepositorySCXMLRegistryTest
 */
public class RepositorySCXMLRegistryTest {

    private static final String SCXML_HELLO = 
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" initial=\"hello\">\n" +
            "  <state id=\"hello\">\n" +
            "    <onentry>\n" +
            "      <log expr=\"'Hello, World'\"/>\n" +
            "    </onentry>\n" +
            "  </state>\n" +
            "</scxml>";

    private static final String SCXML_HELLO2 = 
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" initial=\"hello2\">\n" +
            "  <state id=\"hello2\">\n" +
            "    <onentry>\n" +
            "      <log expr=\"'Hello, World 2'\"/>\n" +
            "    </onentry>\n" +
            "  </state>\n" +
            "</scxml>";

    private static final String SCXML_HELLO_NO_INITIAL = // no initial attribute
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\">\n" +
            "  <state id=\"hello-noinit\">\n" +
            "    <onentry>\n" +
            "      <log expr=\"'Hello, Invalid World'\"/>\n" +
            "    </onentry>\n" +
            "  </state>\n" +
            "</scxml>";

    private static final String SCXML_HELLO_NONEXISTING_INITIAL = // nonexisting initial attribute
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" initial=\"nonexisting\">\n" +
            "  <state id=\"hello-invalid\">\n" +
            "    <onentry>\n" +
            "      <log expr=\"'Hello, Invalid World'\"/>\n" +
            "    </onentry>\n" +
            "  </state>\n" +
            "</scxml>";

    private static final String SCXML_HELLO_WRONG_EXECUTION_IN_STATE = // execution without onentry
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" initial=\"hello-invalid\">\n" +
            "  <state id=\"hello-invalid\">\n" +
            "    <log expr=\"'Hello, Invalid World'\"/>\n" +
            "  </state>\n" +
            "</scxml>";

    private static LoggerRecordingWrapper recordingLogger;

    private MockRepositorySCXMLRegistry registry;

    @BeforeClass
    public static void beforeClass() throws Exception {
        recordingLogger = new LoggerRecordingWrapper(RepositorySCXMLRegistry.log);
        RepositorySCXMLRegistry.log = recordingLogger;
    }

    @Before
    public void before() throws Exception {
        recordingLogger.clearLogRecords();
        registry = new MockRepositorySCXMLRegistry();
    }

    @Test
    public void testInitialize() throws Exception {
        MockNode scxmlConfigNode = registry.createConfigNode();
        registry.addScxmlNode(scxmlConfigNode, "hello", SCXML_HELLO);
        registry.addScxmlNode(scxmlConfigNode, "hello2", SCXML_HELLO2);
        registry.setup(scxmlConfigNode);

        SCXML helloScxml = registry.getSCXML("hello");
        assertNotNull(helloScxml);
        assertEquals("hello", helloScxml.getInitial());

        SCXML hello2Scxml = registry.getSCXML("hello2");
        assertNotNull(hello2Scxml);
        assertEquals("hello2", hello2Scxml.getInitial());

        assertNull(registry.getSCXML("nonexisting"));
    }

    @Test
    public void testLoadCustomActions() throws Exception {
        // TODO
    }

    @Test
    public void testInitializeWithInvalidSCXML() throws Exception {
        MockNode scxmlConfigNode = registry.createConfigNode();
        registry.addScxmlNode(scxmlConfigNode, "hello", SCXML_HELLO);
        registry.addScxmlNode(scxmlConfigNode, "hello-no-initial", SCXML_HELLO_NO_INITIAL);
        registry.addScxmlNode(scxmlConfigNode, "hello-nonexisting-initial", SCXML_HELLO_NONEXISTING_INITIAL);
        registry.addScxmlNode(scxmlConfigNode, "hello-wrong-execution-in-state", SCXML_HELLO_WRONG_EXECUTION_IN_STATE);
        registry.setup(scxmlConfigNode);

        SCXML helloScxml = registry.getSCXML("hello");
        assertNotNull(helloScxml);
        assertEquals("hello", helloScxml.getInitial());
        assertNotNull(registry.getSCXML("hello-no-initial"));
        assertNull(registry.getSCXML("hello-nonexisting-initial"));
        assertNotNull(registry.getSCXML("hello-wrong-execution-in-state"));

        List<LogRecord> logRecords = recordingLogger.getLogRecords();
        assertTrue(containsLogMessage(logRecords, "Invalid SCXML model definition at '/hippo:moduleconfig/hipposcxml:definitions/hello-nonexisting-initial'."));
        assertTrue(containsLogMessage(logRecords, "No SCXML child state with ID \"nonexisting\" found; illegal initial state for SCXML document"));
        assertTrue(containsLogMessage(logRecords, "SCXML model error in /hippo:moduleconfig/hipposcxml:definitions/hello-wrong-execution-in-state (L3:C41): [COMMONS_SCXML] Ignoring element <log> in namespace \"http://www.w3.org/2005/07/scxml\" as child  of <state>"));
    }

    private boolean containsLogMessage(final List<LogRecord> logRecords, String message) {
        if (logRecords == null) {
            return false;
        }

        for (LogRecord logRecord : logRecords) {
            if (logRecord.toString().contains(message)) {
                return true;
            }
        }

        return false;
    }
}
