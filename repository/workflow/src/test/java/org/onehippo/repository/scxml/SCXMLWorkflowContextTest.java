/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;

import org.apache.jackrabbit.JcrConstants;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SCXMLWorkflowContextTest {

    @Test
    public void testPrivileges() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        session.setPermissions("/test", "hippo:admin", false);
        MockWorkflowContext context = new MockWorkflowContext("testuser", session);
        Node mockNode = session.getRootNode().addNode("test", JcrConstants.NT_BASE);

        SCXMLWorkflowContext workflowContext = new SCXMLWorkflowContext("test", context);
        // testing with only hippo:admin being denied
        assertTrue(workflowContext.isGranted(mockNode, "foo"));
        assertTrue(workflowContext.isGranted(mockNode, "foo,bar"));
        assertTrue(workflowContext.isGranted(mockNode, "bar,foo"));
        assertFalse(workflowContext.isGranted(mockNode, "hippo:admin"));

        session.setPermissions("/test", "hippo:author,hippo:editor", true);
        workflowContext = new SCXMLWorkflowContext("test", context);

        // testing with only hippo:author,hippo:editor being allowed
        assertFalse(workflowContext.isGranted(mockNode, "foo"));
        assertFalse(workflowContext.isGranted(mockNode, "foo,bar"));
        assertFalse(workflowContext.isGranted(mockNode, "bar,foo"));
        assertFalse(workflowContext.isGranted(mockNode, "hippo:author,foo"));
        assertFalse(workflowContext.isGranted(mockNode, "hippo:author,hippo:editor,foo"));
        assertTrue(workflowContext.isGranted(mockNode, "hippo:author"));
        assertTrue(workflowContext.isGranted(mockNode, "hippo:editor"));
        assertTrue(workflowContext.isGranted(mockNode, "hippo:author,hippo:editor"));
        assertTrue(workflowContext.isGranted(mockNode, "hippo:editor,hippo:author"));
        assertFalse(workflowContext.isGranted(mockNode, "hippo:admin"));
        assertFalse(workflowContext.isGranted(mockNode, "hippo:admin,foo"));
        assertFalse(workflowContext.isGranted(mockNode, "hippo:admin,hippo:author"));
        assertFalse(workflowContext.isGranted(mockNode, "hippo:admin,hippo:author,hippo:editor"));
        assertFalse(workflowContext.isGranted(mockNode, "hippo:author,hippo:editor,hippo:admin"));
    }
}
