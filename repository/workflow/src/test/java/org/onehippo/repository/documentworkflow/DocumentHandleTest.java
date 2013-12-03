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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.reviewedactions.HippoStdPubWfNodeType;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

public class DocumentHandleTest {

    protected MockNode addVariant(MockNode handle, String state) throws RepositoryException {
        MockNode variant = handle.addMockNode(handle.getName(), HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT);
        variant.setProperty(HippoStdNodeType.HIPPOSTD_STATE, state);
        return variant;
    }

    protected MockNode addRequest(MockNode handle, String type) throws RepositoryException {
        MockNode variant = handle.addMockNode(PublicationRequest.HIPPO_REQUEST, PublicationRequest.NT_HIPPOSTDPUBWF_REQUEST);
        variant.setProperty(PublicationRequest.HIPPOSTDPUBWF_TYPE, type);
        return variant;
    }

    @Test
    public void initDocumentHandle() throws Exception {
        DocumentHandle dm = null;

        MockNode handle = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode publishRequest = addRequest(handle, PublicationRequest.PUBLISH);

        dm = new DocumentHandle(new MockWorkflowContext("testuser"), publishRequest);
        assertEquals("", dm.getStates());
        assertNotNull(dm.getRequest());

        MockNode publishedVariant = addVariant(handle, HippoStdNodeType.PUBLISHED);
        MockNode unpublishedVariant = addVariant(handle, HippoStdNodeType.UNPUBLISHED);
        MockNode rejectedRequest1 = addRequest(handle, PublicationRequest.REJECTED);
        MockNode rejectedRequest2 = addRequest(handle, PublicationRequest.REJECTED);
        MockNode draftVariant = null;

        dm = new DocumentHandle(new MockWorkflowContext("testuser"), unpublishedVariant);
        assertEquals("up", dm.getStates());
        assertNull(dm.getDraft());

        draftVariant = addVariant(handle, HippoStdNodeType.DRAFT);
        dm = new DocumentHandle(new MockWorkflowContext("testuser"), unpublishedVariant);
        assertEquals("dup", dm.getStates());
        assertNotNull(dm.getDraft());

        assertEquals(publishedVariant,dm.getPublished().getNode());
        assertEquals(unpublishedVariant,dm.getUnpublished().getNode());
        assertEquals(HippoStdNodeType.UNPUBLISHED, dm.getSubjectState());
        assertEquals("testuser", dm.getUser());
        assertNull(dm.getRejectedRequest());
        assertEquals(publishRequest, dm.getRequest().getNode());

        dm = new DocumentHandle(new MockWorkflowContext("testuser"), publishRequest);
        assertNull(dm.getSubjectState());
        assertNull(dm.getRejectedRequest());
        assertEquals(publishRequest, dm.getRequest().getNode());

        dm = new DocumentHandle(new MockWorkflowContext("testuser"), rejectedRequest2);
        assertEquals(rejectedRequest2, dm.getRejectedRequest().getNode());
        assertEquals(publishRequest, dm.getRequest().getNode());
    }

    @Test
    public void testWorkflowSupportedFeatures() throws Exception {
        MockWorkflowContext context = new MockWorkflowContext("testuser");
        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);

        DocumentHandle dm = new DocumentHandle(context, draftVariant);
        assertEquals(DocumentWorkflow.SupportedFeatures.all, dm.getSupportedFeatures());
        assertTrue(dm.getSupportedFeatures().document());
        assertTrue(dm.getSupportedFeatures().request());

        context.getWorkflowConfiguration().put("workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.document.name());
        dm = new DocumentHandle(context, draftVariant);
        assertEquals(DocumentWorkflow.SupportedFeatures.document, dm.getSupportedFeatures());
        assertTrue(dm.getSupportedFeatures().document());
        assertFalse(dm.getSupportedFeatures().request());

        context.getWorkflowConfiguration().put("workflow.supportedFeatures", "undefined");
        dm = new DocumentHandle(context, draftVariant);
        assertEquals(DocumentWorkflow.SupportedFeatures.all, dm.getSupportedFeatures());
    }

    @Test
    public void testPrivileges() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        session.setPermissions("/test/test", "hippo:author,hippo:editor", true);
        session.setPermissions("/test/test", "hippo:admin", false);
        MockWorkflowContext context = new MockWorkflowContext("testuser", session);
        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        DocumentHandle dm = new DocumentHandle(context, draftVariant);
        assertFalse(dm.hasPermission("foo"));
        assertFalse(dm.hasPermission("foo,bar"));
        assertFalse(dm.hasPermission("bar,foo"));
        assertFalse(dm.hasPermission("hippo:author,foo"));
        assertFalse(dm.hasPermission("hippo:author,hippo:editor,foo"));
        assertTrue(dm.hasPermission("hippo:author"));
        assertTrue(dm.hasPermission("hippo:editor"));
        assertTrue(dm.hasPermission("hippo:author,hippo:editor"));
        assertTrue(dm.hasPermission("hippo:editor,hippo:author"));
        assertFalse(dm.hasPermission("hippo:admin"));
        assertFalse(dm.hasPermission("hippo:admin,foo"));
        assertFalse(dm.hasPermission("hippo:admin,hippo:author,hippo:editor"));
        assertFalse(dm.hasPermission("hippo:author,hippo:editor,hippo:admin"));
    }
}
