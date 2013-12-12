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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.scxml2.SCXMLExecutor;
import org.apache.commons.scxml2.model.OnEntry;
import org.apache.commons.scxml2.model.OnExit;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

/**
 * SCXMLExecutorWrapperTest
 */
public class SCXMLExecutorWrapperTest {

    private static final String SCXML_HELLO_WITH_ERROR_JEXL_SCRIPTS =
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" initial=\"hello\">\n" +
            "  <state id=\"hello\">\n" +
            "    <initial>\n" +
            "      <transition target=\"world\" />\n" +
            "    </initial>\n" +
            "    <state id=\"world\">\n" +
            "      <onentry>\n" +
            "        <script>\n" +
            "          unknownObject.invoke();\n" +
            "        </script>\n" +
            "      </onentry>\n" +
            "      <transition target=\"world2\" />\n" +
            "      <onexit>\n" +
            "        <script>\n" +
            "          unknownObject2.invoke();\n" +
            "        </script>\n" +
            "      </onexit>\n" +
            "    </state>\n" +
            "    <state id=\"world2\">\n" +
            "      <onentry>\n" +
            "        <script>\n" +
            "          unknownObject3.invoke();\n" +
            "        </script>\n" +
            "      </onentry>\n" +
            "    </state>\n" +
            "  </state>\n" +
            "</scxml>";

    private MockRepositorySCXMLRegistry registry;
    private RepositorySCXMLExecutorFactory execFactory;

    @Before
    public void before() throws Exception {
        registry = new MockRepositorySCXMLRegistry();

        execFactory = new RepositorySCXMLExecutorFactory();
        execFactory.initialize();
    }

    @Test
    public void testLoadWithErrorJexlScripts() throws Exception {
        MockNode scxmlConfigNode = registry.createConfigNode();
        registry.addScxmlNode(scxmlConfigNode, "hello-with-error-jexl-scripts", SCXML_HELLO_WITH_ERROR_JEXL_SCRIPTS);
        registry.setUp(scxmlConfigNode);

        SCXMLDefinition helloScxml = registry.getSCXMLDefinition("hello-with-error-jexl-scripts");

        SCXMLExecutor helloExec = execFactory.createSCXMLExecutor(helloScxml);
        HippoScxmlErrorReporter errorReporter = new HippoScxmlErrorReporter(helloScxml);
        helloExec.setErrorReporter(errorReporter);

        SCXMLExecutorWrapper helloExecWrapper = new SCXMLExecutorWrapper(helloExec);
        boolean tryRes = helloExecWrapper.tryGo();

        assertFalse(tryRes);
        assertNull(helloExecWrapper.getModelException());
        assertEquals(3, helloExecWrapper.getSCXMLExecutionErrors().size());

        SCXMLExecutionError error = helloExecWrapper.getSCXMLExecutionError();
        assertNotNull(error);
        assertEquals(error, helloExecWrapper.getSCXMLExecutionErrors().get(0));
        assertEquals("EXPRESSION_ERROR", error.getErrorCode());
        assertTrue(error.getErrorDetail().contains("'unknownObject.invoke();'"));
        assertTrue(error.getErrorContext() instanceof OnEntry);

        error = helloExecWrapper.getSCXMLExecutionErrors().get(1);
        assertNotNull(error);
        assertEquals("EXPRESSION_ERROR", error.getErrorCode());
        assertTrue(error.getErrorDetail().contains("'unknownObject2.invoke();'"));
        assertTrue(error.getErrorContext() instanceof OnExit);

        error = helloExecWrapper.getSCXMLExecutionErrors().get(2);
        assertNotNull(error);
        assertEquals("EXPRESSION_ERROR", error.getErrorCode());
        assertTrue(error.getErrorDetail().contains("'unknownObject3.invoke();'"));
        assertTrue(error.getErrorContext() instanceof OnEntry);
    }
}
