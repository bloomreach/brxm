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

    private static final String SCXML_HELLO_INVALID = // no initial attribute
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\">\n" +
            "  <state id=\"hello-invalid\">\n" +
            "    <onentry>\n" +
            "      <log expr=\"'Hello, Invalid World'\"/>\n" +
            "    </onentry>\n" +
            "  </state>\n" +
            "</scxml>";

    private static final String SCXML_HELLO_INVALID2 = // nonexisting initial attribute
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" initial=\"nonexisting\">\n" +
            "  <state id=\"hello-invalid\">\n" +
            "    <onentry>\n" +
            "      <log expr=\"'Hello, Invalid World'\"/>\n" +
            "    </onentry>\n" +
            "  </state>\n" +
            "</scxml>";

    private static final String SCXML_HELLO_INVALID3 = // execution without onentry
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" initial=\"hello-invalid2\">\n" +
            "  <state id=\"hello-invalid2\">\n" +
            "    <log expr=\"'Hello, Invalid World'\"/>\n" +
            "  </state>\n" +
            "</scxml>";

    private static LoggerRecordingWrapper recordingLogger;

    private RepositorySCXMLRegistry registry;

    @BeforeClass
    public static void beforeClass() throws Exception {
        recordingLogger = new LoggerRecordingWrapper(RepositorySCXMLRegistry.log);
        RepositorySCXMLRegistry.log = recordingLogger;
    }

    @Before
    public void before() throws Exception {
        recordingLogger.clearLogRecords();
        registry = new RepositorySCXMLRegistry();
    }

    @Test
    public void testInitialize() throws Exception {
        MockNode root = MockNode.root();
        MockNode scxmlConfigNode = root.addMockNode("hippo:moduleconfig", "nt:unstructured");
        MockNode scxmlDefsNode = scxmlConfigNode.addMockNode("hipposcxml:definitions", "hipposcxml:definitions");
        MockNode scxmlDefNode = scxmlDefsNode.addMockNode("hello", "hipposcxml:scxml");
        scxmlDefNode.setProperty("hipposcxml:source", SCXML_HELLO);
        scxmlDefNode = scxmlDefsNode.addMockNode("hello2", "hipposcxml:scxml");
        scxmlDefNode.setProperty("hipposcxml:source", SCXML_HELLO2);
        registry.reconfigure(scxmlConfigNode);
        registry.initialize();

        SCXML helloScxml = registry.getSCXML("hello");
        assertNotNull(helloScxml);
        assertEquals("hello", helloScxml.getInitial());

        SCXML hello2Scxml = registry.getSCXML("hello2");
        assertNotNull(hello2Scxml);
        assertEquals("hello2", hello2Scxml.getInitial());

        assertNull(registry.getSCXML("nonexisting"));
    }

    @Test
    public void testInitializeWithInvalidSCXML() throws Exception {
        MockNode root = MockNode.root();
        MockNode scxmlConfigNode = root.addMockNode("hippo:moduleconfig", "nt:unstructured");
        MockNode scxmlDefsNode = scxmlConfigNode.addMockNode("hipposcxml:definitions", "hipposcxml:definitions");
        MockNode scxmlDefNode = scxmlDefsNode.addMockNode("hello", "hipposcxml:scxml");
        scxmlDefNode.setProperty("hipposcxml:source", SCXML_HELLO);
        scxmlDefNode = scxmlDefsNode.addMockNode("hello-invalid", "hipposcxml:scxml");
        scxmlDefNode.setProperty("hipposcxml:source", SCXML_HELLO_INVALID);
        scxmlDefNode = scxmlDefsNode.addMockNode("hello-invalid2", "hipposcxml:scxml");
        scxmlDefNode.setProperty("hipposcxml:source", SCXML_HELLO_INVALID2);
        scxmlDefNode = scxmlDefsNode.addMockNode("hello-invalid3", "hipposcxml:scxml");
        scxmlDefNode.setProperty("hipposcxml:source", SCXML_HELLO_INVALID3);
        registry.reconfigure(scxmlConfigNode);
        registry.initialize();

        SCXML helloScxml = registry.getSCXML("hello");
        assertNotNull(helloScxml);
        assertEquals("hello", helloScxml.getInitial());

        assertNull(registry.getSCXML("hello-invalid"));
        List<LogRecord> logRecords = recordingLogger.getLogRecords();
        assertTrue(containsLogMessage(logRecords, "Invalid SCXML model definition at '/hippo:moduleconfig/hipposcxml:definitions/hello-invalid'."));
        assertTrue(containsLogMessage(logRecords, "No SCXML child state with ID \"null\" found; illegal initialstate for SCXML document"));
        assertTrue(containsLogMessage(logRecords, "No SCXML child state with ID \"nonexisting\" found; illegal initialstate for SCXML document"));
        assertTrue(containsLogMessage(logRecords, "SCXML model error in /hippo:moduleconfig/hipposcxml:definitions/hello-invalid3 (L3:C41): [COMMONS_SCXML] Ignoring element <log> in namespace \"http://www.w3.org/2005/07/scxml\" as child  of <state>"));
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
