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

import org.apache.commons.scxml2.ActionExecutionContext;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.io.SCXMLReader;
import org.apache.commons.scxml2.model.Action;
import org.apache.commons.scxml2.model.ModelException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.testutils.ExecuteOnLogLevel;
import org.onehippo.repository.testutils.slf4j.LogRecord;
import org.onehippo.repository.testutils.slf4j.LoggerRecordingWrapper;

/**
 * RepositorySCXMLRegistryTest
 */
public class RepositorySCXMLRegistryTest {

    private static final String SCXML_HELLO = 
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" version=\"1.0\" initial=\"hello\">\n" +
            "  <state id=\"hello\">\n" +
            "    <onentry>\n" +
            "      <log expr=\"'Hello, World'\"/>\n" +
            "    </onentry>\n" +
            "  </state>\n" +
            "</scxml>";

    private static final String SCXML_HELLO2 = 
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" version=\"1.0\" initial=\"hello2\">\n" +
            "  <state id=\"hello2\">\n" +
            "    <onentry>\n" +
            "      <log expr=\"'Hello, World 2'\"/>\n" +
            "    </onentry>\n" +
            "  </state>\n" +
            "</scxml>";

    private static final String SCXML_HELLO_NO_INITIAL = // no initial attribute
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" version=\"1.0\">\n" +
            "  <state id=\"hello-noinit\">\n" +
            "    <onentry>\n" +
            "      <log expr=\"'Hello, Invalid World'\"/>\n" +
            "    </onentry>\n" +
            "  </state>\n" +
            "</scxml>";

    private static final String SCXML_HELLO_NONEXISTING_INITIAL = // nonexisting initial attribute
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" version=\"1.0\" initial=\"nonexisting\">\n" +
            "  <state id=\"hello-invalid\">\n" +
            "    <onentry>\n" +
            "      <log expr=\"'Hello, Invalid World'\"/>\n" +
            "    </onentry>\n" +
            "  </state>\n" +
            "</scxml>";

    private static final String SCXML_HELLO_WRONG_EXECUTION_IN_STATE = // execution without onentry
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" version=\"1.0\" initial=\"hello-invalid\">\n" +
            "  <state id=\"hello-invalid\">\n" +
            "    <log expr=\"'Hello, Invalid World'\"/>\n" +
            "  </state>\n" +
            "</scxml>";

    private static final String SCXML_HELLO_WITH_UNKNOWN_CUSTOM_ACTIONS =
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" xmlns:hippo=\"http://www.onehippo.org/cms7/repository/scxml\" version=\"1.0\" initial=\"hello\">\n" +
            "  <state id=\"hello\">\n" +
            "    <onentry>\n" +
            "      <hippo:unknown-custom-action/>\n" +
            "    </onentry>\n" +
            "  </state>\n" +
            "</scxml>";

    private static final String SCXML_HELLO_WITH_UNKNOWN_NS_CUSTOM_ACTIONS =
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" xmlns:hippo=\"http://www.onehippo.org/cms7/repository/scxml\" version=\"1.0\" initial=\"hello\">\n" +
            "  <state id=\"hello\">\n" +
            "    <onentry>\n" +
            "      <hippo2:unknown-custom-action/>\n" +
            "    </onentry>\n" +
            "  </state>\n" +
            "</scxml>";

    private static LoggerRecordingWrapper recordingLogger;

    private MockRepositorySCXMLRegistry registry;

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
        registry.setUp(scxmlConfigNode);

        SCXMLDefinition helloScxml = registry.getSCXMLDefinition("hello");
        assertNotNull(helloScxml);
        assertEquals("hello", helloScxml.getSCXML().getInitial());

        SCXMLDefinition hello2Scxml = registry.getSCXMLDefinition("hello2");
        assertNotNull(hello2Scxml);
        assertEquals("hello2", hello2Scxml.getSCXML().getInitial());

        assertNull(registry.getSCXMLDefinition("nonexisting"));
    }

    @Test
    public void testInitializeWithInvalidSCXML() throws Exception {
        final MockNode scxmlConfigNode = registry.createConfigNode();
        registry.addScxmlNode(scxmlConfigNode, "hello", SCXML_HELLO);
        registry.addScxmlNode(scxmlConfigNode, "hello-no-initial", SCXML_HELLO_NO_INITIAL);
        registry.addScxmlNode(scxmlConfigNode, "hello-nonexisting-initial", SCXML_HELLO_NONEXISTING_INITIAL);
        registry.addScxmlNode(scxmlConfigNode, "hello-wrong-execution-in-state", SCXML_HELLO_WRONG_EXECUTION_IN_STATE);
        try {
            ExecuteOnLogLevel.fatal(new ExecuteOnLogLevel.Executable() {
                @Override
                public void execute() throws Exception {
                    registry.setUp(scxmlConfigNode);
                }
            }, RepositorySCXMLRegistry.class.getName(), SCXMLReader.class.getName(), "org.apache.commons.scxml2.io.ModelUpdater");
        }
        catch (ExecuteOnLogLevel.ExecutableException re) {
            if (re.getCause() instanceof SCXMLException) {
                // expected
            }
            else {
                throw re.getCause();
            }
        }

        SCXMLDefinition helloScxml = registry.getSCXMLDefinition("hello");
        assertNotNull(helloScxml);
        assertEquals("hello", helloScxml.getSCXML().getInitial());
        assertNotNull(registry.getSCXMLDefinition("hello-no-initial"));
        assertNull(registry.getSCXMLDefinition("hello-nonexisting-initial"));
        assertNull(registry.getSCXMLDefinition("hello-wrong-execution-in-state"));

        List<LogRecord> logRecords = recordingLogger.getLogRecords();
        assertTrue(containsLogMessage(logRecords, "Invalid SCXML model definition at '/hippo:moduleconfig/hipposcxml:definitions/hello-wrong-execution-in-state'. Ignoring unknown or invalid element <log> in namespace \"http://www.w3.org/2005/07/scxml\" as child of <state> at "));
    }

    /*
     * When local name of custom action is not found from the all the registered custom actions, it should still work with a proper warning.
     */
    @Test
    public void testLoadUnknownLocalCustomAction() throws Exception {
        final MockNode scxmlConfigNode = registry.createConfigNode();
        MockNode scxmlDefNode = registry.addScxmlNode(scxmlConfigNode, "hello-with-unknown-custom-actions", SCXML_HELLO_WITH_UNKNOWN_CUSTOM_ACTIONS);
        registry.addCustomAction(scxmlDefNode, "http://www.onehippo.org/cms7/repository/scxml", "known-custom-action", KnownAction.class.getName());
        try {
            ExecuteOnLogLevel.fatal(new ExecuteOnLogLevel.Executable() {
                @Override
                public void execute() throws Exception {
                    registry.setUp(scxmlConfigNode);
                }
            }, RepositorySCXMLRegistry.class.getName(), SCXMLReader.class.getName());
        }
        catch (ExecuteOnLogLevel.ExecutableException re) {
            if (re.getCause() instanceof SCXMLException) {
                // expected
            }
            else {
                throw re.getCause();
            }
        }

        SCXMLDefinition helloScxml = registry.getSCXMLDefinition("hello-with-unknown-custom-actions");
        assertNull(helloScxml);

        List<LogRecord> logRecords = recordingLogger.getLogRecords();
        assertTrue(containsLogMessage(logRecords, "Invalid SCXML model definition at '/hippo:moduleconfig/hipposcxml:definitions/hello-with-unknown-custom-actions'. Ignoring unknown or invalid element <unknown-custom-action> in namespace \"http://www.onehippo.org/cms7/repository/scxml\" as child of <onentry> at "));
    }

    /*
     * When local name of custom action is not registered and there's no custom actions registered, it should still work with a proper warning.
     */
    @Test
    public void testLoadUnknownLocalCustomActionWhenNoCustomActions() throws Exception {
        final MockNode scxmlConfigNode = registry.createConfigNode();
        registry.addScxmlNode(scxmlConfigNode, "hello-with-unknown-custom-actions", SCXML_HELLO_WITH_UNKNOWN_CUSTOM_ACTIONS);
        try {
            ExecuteOnLogLevel.fatal(new ExecuteOnLogLevel.Executable() {
                @Override
                public void execute() throws Exception {
                    registry.setUp(scxmlConfigNode);
                }
            }, RepositorySCXMLRegistry.class.getName(), SCXMLReader.class.getName());
        }
        catch (ExecuteOnLogLevel.ExecutableException re) {
            if (re.getCause() instanceof SCXMLException) {
                // expected
            }
            else {
                throw re.getCause();
            }
        }

        SCXMLDefinition helloScxml = registry.getSCXMLDefinition("hello-with-unknown-custom-actions");
        assertNull(helloScxml);

        List<LogRecord> logRecords = recordingLogger.getLogRecords();
        assertTrue(containsLogMessage(logRecords, "Invalid SCXML model definition at '/hippo:moduleconfig/hipposcxml:definitions/hello-with-unknown-custom-actions'. Ignoring unknown or invalid element <unknown-custom-action> in namespace \"http://www.onehippo.org/cms7/repository/scxml\" as child of <onentry> at"));
    }

    /*
     * When namespace prefix is not resolved, it fails to load the SCXML. 
     */
    @Test
    public void testLoadUnknownNsCustomAction() throws Exception {
        final MockNode scxmlConfigNode = registry.createConfigNode();
        registry.addScxmlNode(scxmlConfigNode, "hello-with-unknown-ns-custom-actions", SCXML_HELLO_WITH_UNKNOWN_NS_CUSTOM_ACTIONS);
        try {
            ExecuteOnLogLevel.fatal(new ExecuteOnLogLevel.Executable() {
                @Override
                public void execute() throws Exception {
                    registry.setUp(scxmlConfigNode);
                }
            }, RepositorySCXMLRegistry.class.getName());
        }
        catch (ExecuteOnLogLevel.ExecutableException re) {
            if (re.getCause() instanceof SCXMLException) {
                // expected
            }
            else {
                throw re.getCause();
            }
        }

        SCXMLDefinition helloScxml = registry.getSCXMLDefinition("hello-with-unknown-ns-custom-actions");
        assertNull(helloScxml);

        List<LogRecord> logRecords = recordingLogger.getLogRecords();
        assertTrue(containsLogMessage(logRecords, "Failed to read SCXML XML stream at '/hippo:moduleconfig/hipposcxml:definitions/hello-with-unknown-ns-custom-actions'."));
    }

    private static class KnownAction extends Action {
        private static final long serialVersionUID = 1L;
        @Override
        public void execute(ActionExecutionContext exctx) throws ModelException, SCXMLExpressionException {
        }
    }
}
