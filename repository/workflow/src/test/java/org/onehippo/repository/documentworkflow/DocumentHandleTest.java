/*
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

package org.onehippo.repository.documentworkflow;

import javax.jcr.Node;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.scxml.SCXMLWorkflowContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DocumentHandleTest extends BaseDocumentWorkflowTest {

    private static class DocumentHandleWorkflowContext extends SCXMLWorkflowContext {
        private DocumentHandleWorkflowContext(final String scxmlId, final WorkflowContext workflowContext) {
            super(scxmlId, workflowContext);
        }

        public void initialize() throws WorkflowException {
            super.initialize();
        }
    }

    protected static DocumentVariant getDraft(DocumentHandle dm) {
        return dm.getDocuments().get(HippoStdNodeType.DRAFT);
    }

    protected static DocumentVariant getUnpublished(DocumentHandle dm) {
        return dm.getDocuments().get(HippoStdNodeType.UNPUBLISHED);
    }

    protected static DocumentVariant getPublished(DocumentHandle dm) {
        return dm.getDocuments().get(HippoStdNodeType.PUBLISHED);
    }

    @Test
    public void initDocumentHandle() throws Exception {

        // create handle with publication request
        MockNode handle = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        addRequest(handle, HippoStdPubWfNodeType.PUBLISH, true);
        DocumentHandleWorkflowContext workflowContext = new DocumentHandleWorkflowContext("test", new MockWorkflowContext("testuser"));
        DocumentHandle dh = new DocumentHandle(handle);
        workflowContext.initialize();
        dh.initialize();
        assertTrue(dh.getDocuments().isEmpty());
        assertEquals("testuser", workflowContext.getUser());
        assertEquals(1, dh.getRequests().size());
        assertTrue(dh.isRequestPending());


        // add published, unpublished variants & rejected request
        Node publishedVariant = addVariant(handle, HippoStdNodeType.PUBLISHED);
        Node unpublishedVariant = addVariant(handle, HippoStdNodeType.UNPUBLISHED);
        addRequest(handle, HippoStdPubWfNodeType.REJECTED, true);
        Node rejectedRequest = addRequest(handle, HippoStdPubWfNodeType.REJECTED, true);
        rejectedRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "testuser");
        dh = new DocumentHandle(handle);
        workflowContext.initialize();
        dh.initialize();
        assertNull(getDraft(dh));
        assertNotNull(getUnpublished(dh));
        assertNotNull(getPublished(dh));
        assertEquals(publishedVariant, getPublished(dh).getNode());
        assertEquals(unpublishedVariant, getUnpublished(dh).getNode());
        assertEquals(3, dh.getRequests().size());
        assertTrue(dh.isRequestPending());

        // add draft
        Node draftVariant = addVariant(handle, HippoStdNodeType.DRAFT);
        dh = new DocumentHandle(handle);
        workflowContext.initialize();
        dh.initialize();
        assertNotNull(getDraft(dh));
        assertNotNull(getUnpublished(dh));
        assertNotNull(getPublished(dh));
        assertEquals(draftVariant, getDraft(dh).getNode());
    }

    @Test
    public void testPrivileges() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        session.setPermissions("/test/test", "hippo:admin", false);
        MockWorkflowContext context = new MockWorkflowContext("testuser", session);
        Node handleNode = session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        addVariant((MockNode)handleNode, HippoStdNodeType.DRAFT);
        DocumentHandleWorkflowContext workflowContext = new DocumentHandleWorkflowContext("test", context);
        DocumentHandle dh = new DocumentHandle(handleNode);
        workflowContext.initialize();
        dh.initialize();
        // testing with only hippo:admin being denied
        assertTrue(workflowContext.isGranted(getDraft(dh), "foo"));
        assertTrue(workflowContext.isGranted(getDraft(dh), "foo,bar"));
        assertTrue(workflowContext.isGranted(getDraft(dh), "bar,foo"));
        assertFalse(workflowContext.isGranted(getDraft(dh), "hippo:admin"));

        session.setPermissions("/test/test", "hippo:author,hippo:editor", true);
        dh = new DocumentHandle(handleNode);
        workflowContext.initialize();
        dh.initialize();
        // testing with only hippo:author,hippo:editor being allowed
        assertFalse(workflowContext.isGranted(getDraft(dh), "foo"));
        assertFalse(workflowContext.isGranted(getDraft(dh), "foo,bar"));
        assertFalse(workflowContext.isGranted(getDraft(dh), "bar,foo"));
        assertFalse(workflowContext.isGranted(getDraft(dh), "hippo:author,foo"));
        assertFalse(workflowContext.isGranted(getDraft(dh), "hippo:author,hippo:editor,foo"));
        assertTrue(workflowContext.isGranted(getDraft(dh), "hippo:author"));
        assertTrue(workflowContext.isGranted(getDraft(dh), "hippo:editor"));
        assertTrue(workflowContext.isGranted(getDraft(dh), "hippo:author,hippo:editor"));
        assertTrue(workflowContext.isGranted(getDraft(dh), "hippo:editor,hippo:author"));
        assertFalse(workflowContext.isGranted(getDraft(dh), "hippo:admin"));
        assertFalse(workflowContext.isGranted(getDraft(dh), "hippo:admin,foo"));
        assertFalse(workflowContext.isGranted(getDraft(dh), "hippo:admin,hippo:author"));
        assertFalse(workflowContext.isGranted(getDraft(dh), "hippo:admin,hippo:author,hippo:editor"));
        assertFalse(workflowContext.isGranted(getDraft(dh), "hippo:author,hippo:editor,hippo:admin"));
    }
}
