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
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DocumentHandleTest {

    protected Node addVariant(Node handle, String state) throws RepositoryException {
        Node variant = handle.addNode(handle.getName(), HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT);
        variant.setProperty(HippoStdNodeType.HIPPOSTD_STATE, state);
        return variant;
    }

    protected Node addRequest(Node handle, String type, boolean workflowRequest) throws RepositoryException {
        Node variant = handle.addNode(WorkflowRequest.HIPPO_REQUEST,
                workflowRequest ? WorkflowRequest.NT_HIPPOSTDPUBWF_REQUEST : ScheduledRequest.NT_HIPPOSCHED_WORKFLOW_JOB);
        variant.setProperty(WorkflowRequest.HIPPOSTDPUBWF_TYPE, type);
        variant.addMixin(HippoNodeType.NT_REQUEST);
        return variant;
    }

    protected static DocumentVariant getDraft(DocumentHandle dm) {
        return dm.getDocumentVariantByState(HippoStdNodeType.DRAFT);
    }

    protected static DocumentVariant getUnpublished(DocumentHandle dm) {
        return dm.getDocumentVariantByState(HippoStdNodeType.UNPUBLISHED);
    }

    protected static DocumentVariant getPublished(DocumentHandle dm) {
        return dm.getDocumentVariantByState(HippoStdNodeType.PUBLISHED);
    }

    @Test
    public void initDocumentHandle() throws Exception {

        // create handle with publication request
        MockNode handle = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        Node publishRequest = addRequest(handle, WorkflowRequest.PUBLISH, true);
        DocumentHandle dm = new DocumentHandle("test", new MockWorkflowContext("testuser"), handle);

        assertTrue(dm.getDocuments().isEmpty());
        assertEquals("testuser", dm.getUser());
        assertEquals(1, dm.getRequests().size());
        assertTrue(dm.isRequestPending());


        // add published, unpublished variants & rejected request
        Node publishedVariant = addVariant(handle, HippoStdNodeType.PUBLISHED);
        Node unpublishedVariant = addVariant(handle, HippoStdNodeType.UNPUBLISHED);
        addRequest(handle, WorkflowRequest.REJECTED, true);
        Node rejectedRequest = addRequest(handle, WorkflowRequest.REJECTED, true);
        rejectedRequest.setProperty(WorkflowRequest.HIPPOSTDPUBWF_USERNAME, "testuser");
        dm = new DocumentHandle("test", new MockWorkflowContext("testuser"), handle);

        assertNull(getDraft(dm));
        assertNotNull(getUnpublished(dm));
        assertNotNull(getPublished(dm));
        assertEquals(publishedVariant, getPublished(dm).getNode());
        assertEquals(unpublishedVariant, getUnpublished(dm).getNode());
        assertEquals(3, dm.getRequests().size());
        assertTrue(dm.isRequestPending());

        // add draft
        Node draftVariant = addVariant(handle, HippoStdNodeType.DRAFT);
        dm = new DocumentHandle("test", new MockWorkflowContext("testuser"), handle);

        assertNotNull(getDraft(dm));
        assertNotNull(getUnpublished(dm));
        assertNotNull(getPublished(dm));
        assertEquals(draftVariant, getDraft(dm).getNode());
    }

    @Test
    public void testPrivileges() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        session.setPermissions("/test/test", "hippo:admin", false);
        MockWorkflowContext context = new MockWorkflowContext("testuser", session);
        Node handleNode = session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        Node draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        DocumentHandle dm = new DocumentHandle("test", context, handleNode);

        // testing with only hippo:admin being denied
        assertTrue(dm.isGranted(getDraft(dm), "foo"));
        assertTrue(dm.isGranted(getDraft(dm), "foo,bar"));
        assertTrue(dm.isGranted(getDraft(dm), "bar,foo"));
        assertFalse(dm.isGranted(getDraft(dm), "hippo:admin"));

        session.setPermissions("/test/test", "hippo:author,hippo:editor", true);
        dm = new DocumentHandle("test", context, handleNode);

        // testing with only hippo:author,hippo:editor being allowed
        assertFalse(dm.isGranted(getDraft(dm), "foo"));
        assertFalse(dm.isGranted(getDraft(dm), "foo,bar"));
        assertFalse(dm.isGranted(getDraft(dm), "bar,foo"));
        assertFalse(dm.isGranted(getDraft(dm), "hippo:author,foo"));
        assertFalse(dm.isGranted(getDraft(dm), "hippo:author,hippo:editor,foo"));
        assertTrue(dm.isGranted(getDraft(dm), "hippo:author"));
        assertTrue(dm.isGranted(getDraft(dm), "hippo:editor"));
        assertTrue(dm.isGranted(getDraft(dm), "hippo:author,hippo:editor"));
        assertTrue(dm.isGranted(getDraft(dm), "hippo:editor,hippo:author"));
        assertFalse(dm.isGranted(getDraft(dm), "hippo:admin"));
        assertFalse(dm.isGranted(getDraft(dm), "hippo:admin,foo"));
        assertFalse(dm.isGranted(getDraft(dm), "hippo:admin,hippo:author"));
        assertFalse(dm.isGranted(getDraft(dm), "hippo:admin,hippo:author,hippo:editor"));
        assertFalse(dm.isGranted(getDraft(dm), "hippo:author,hippo:editor,hippo:admin"));
    }
}
