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
import static org.junit.Assert.assertNull;

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
        MockNode handle = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode publishedVariant = addVariant(handle, HippoStdNodeType.PUBLISHED);
        MockNode unpublishedVariant = addVariant(handle, HippoStdNodeType.UNPUBLISHED);
        MockNode rejectedRequest1 = addRequest(handle, PublicationRequest.REJECTED);
        MockNode publishRequest = addRequest(handle, PublicationRequest.PUBLISH);
        MockNode rejectedRequest2 = addRequest(handle, PublicationRequest.REJECTED);

        DocumentHandle dm = new DocumentHandle(new MockWorkflowContext("testuser"), unpublishedVariant);
        assertNull(dm.getDraft());
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
}
