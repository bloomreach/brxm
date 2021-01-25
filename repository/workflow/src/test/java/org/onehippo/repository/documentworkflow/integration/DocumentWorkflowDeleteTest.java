/*
 * Copyright 2021 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.documentworkflow.integration;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.mock.MockNode;

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.HippoStdNodeType.NT_DOCUMENT;
import static org.hippoecm.repository.HippoStdNodeType.NT_RELAXED;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATED_BY;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATION_DATE;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_AVAILABILITY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;

public class DocumentWorkflowDeleteTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void testDeleteDocument_unpublishedOnly() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.delete();
        final boolean isInAttic = workflow.getNode().getPath().contains("attic");
        assertTrue("Document has not been archived", isInAttic);
    }


    @Test
    public void testDeleteDocument_unpublishedandPublished() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        createVariant(HippoNodeType.NT_DOCUMENT, HippoStdNodeType.PUBLISHED);
        session.save();
        workflow.delete();
        final boolean isInAttic = workflow.getNode().getPath().contains("attic");
        assertTrue("Document has not been archived", isInAttic);
    }

    @Test
    public void testDeleteDocument_draftOnly() throws Exception {
        handle.getNode("document").remove();
        createVariant(HippoNodeType.NT_DOCUMENT, HippoStdNodeType.DRAFT);
        session.save();
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.delete();
        final boolean isInAttic = workflow.getNode().getPath().contains("attic");
        assertTrue("Document has not been archived", isInAttic);
    }

    private Node createVariant(String primaryType, String state) throws RepositoryException {
        final Node variant = handle.addNode("document", primaryType);
        variant.addMixin(HIPPOSTDPUBWF_DOCUMENT);
        variant.addMixin(MIX_VERSIONABLE);
        variant.addMixin(NT_RELAXED);
        variant.setProperty(HIPPOSTDPUBWF_CREATION_DATE, Calendar.getInstance());
        variant.setProperty(HIPPOSTDPUBWF_CREATED_BY, TESTUSER);
        variant.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE, Calendar.getInstance());
        variant.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_BY, TESTUSER);
        variant.setProperty(HIPPOSTD_STATE, state);
        variant.setProperty(HIPPO_AVAILABILITY, new String[]{"preview"});
        return variant;
    }
}
