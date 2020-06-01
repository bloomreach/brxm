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
import static org.junit.Assert.assertTrue;

import org.apache.commons.scxml2.SCXMLExecutor;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

/**
 * ActionAndResultActionTest
 */
public class ActionAndResultActionTest {

    private static final String SCXML_HELLO_ACTION_AND_RESULT =
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" xmlns:hippo=\"http://www.onehippo.org/cms7/repository/scxml\" version=\"1.0\" initial=\"hello\">\n" +
                    "  <state id=\"hello\">\n" +
                    "    <initial>\n" +
                    "      <transition target=\"world\" />\n" +
                    "    </initial>\n" +
                    "    <state id=\"world\">\n" +
                    "      <onentry>\n" +
                    "        <hippo:action action=\"hello\" enabledExpr=\"world\"/>\n" +
                    "        <hippo:result value=\"workflowContext\"/>\n" +
                    "      </onentry>\n" +
                    "    </state>\n" +
                    "  </state>\n" +
                    "</scxml>";

    @Test
    public void testActionAndResultAction() throws Exception {
        MockRepositorySCXMLRegistry registry = new MockRepositorySCXMLRegistry();

        RepositorySCXMLExecutorFactory execFactory = new RepositorySCXMLExecutorFactory();
        execFactory.initialize();

        MockNode scxmlConfigNode = registry.createConfigNode();
        MockNode scxmlDefNode = registry.addScxmlNode(scxmlConfigNode, "helloScxml", SCXML_HELLO_ACTION_AND_RESULT);
        registry.addCustomAction(scxmlDefNode, "http://www.onehippo.org/cms7/repository/scxml", "action", ActionAction.class.getName());
        registry.addCustomAction(scxmlDefNode, "http://www.onehippo.org/cms7/repository/scxml", "result", ResultAction.class.getName());
        registry.setUp(scxmlConfigNode);

        SCXMLDefinition helloScxml = registry.getSCXMLDefinition("helloScxml");
        assertNotNull(helloScxml);

        SCXMLExecutor helloExec = execFactory.createSCXMLExecutor(helloScxml);

        SCXMLWorkflowContext workflowContext = new SCXMLWorkflowContext(helloScxml.getId(), new MockWorkflowContext("testuser"));
        helloExec.getRootContext().set(SCXMLWorkflowContext.SCXML_CONTEXT_KEY, workflowContext);
        helloExec.getRootContext().set("world", Boolean.TRUE);
        helloExec.go();

        assertTrue(workflowContext.getActions().get("hello"));
        assertEquals(workflowContext, workflowContext.getResult());
    }
}
