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

import javax.jcr.RepositoryException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.io.IOUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.reviewedactions.HippoStdPubWfNodeType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.scxml.MockRepositorySCXMLRegistry;
import org.onehippo.repository.scxml.RepositorySCXMLExecutorFactory;
import org.onehippo.repository.scxml.SCXMLExecutorFactory;
import org.onehippo.repository.scxml.SCXMLRegistry;

public class TestDocumentWorkflowImpl {

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

    @BeforeClass
    public static void beforeClass() throws Exception {
        MockRepositorySCXMLRegistry scxmlRegistry = new MockRepositorySCXMLRegistry();
        String scxml = IOUtils.toString(TestDocumentWorkflowImpl.class.getResourceAsStream("test-document-workflow.scxml"));
        scxmlRegistry.setup("document-workflow", scxml);
        HippoServiceRegistry.registerService(scxmlRegistry, SCXMLRegistry.class);
        HippoServiceRegistry.registerService(new RepositorySCXMLExecutorFactory(), SCXMLExecutorFactory.class);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        HippoServiceRegistry.unregisterService(HippoServiceRegistry.getService(SCXMLExecutorFactory.class), SCXMLExecutorFactory.class);
        HippoServiceRegistry.unregisterService(HippoServiceRegistry.getService(SCXMLRegistry.class), SCXMLRegistry.class);
    }

    @Test
    public void testInitializeWorkflow() throws Exception {

        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(new MockWorkflowContext("testuser"));

        MockNode handle = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode publishedVariant = addVariant(handle, HippoStdNodeType.PUBLISHED);
        MockNode unpublishedVariant = addVariant(handle, HippoStdNodeType.UNPUBLISHED);
        MockNode rejectedRequest1 = addRequest(handle, PublicationRequest.REJECTED);
        MockNode publishRequest = addRequest(handle, PublicationRequest.PUBLISH);
        MockNode rejectedRequest2 = addRequest(handle, PublicationRequest.REJECTED);

        wf.setNode(publishedVariant);
        assertTrue(wf.hints().isEmpty());
    }
}
