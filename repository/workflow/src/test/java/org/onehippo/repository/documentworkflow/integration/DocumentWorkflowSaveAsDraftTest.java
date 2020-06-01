/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.onehippo.repository.documentworkflow.integration;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.junit.AfterClass;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static junit.framework.Assert.assertTrue;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_HOLDER;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.HippoStdNodeType.NT_RELAXED;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATED_BY;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATION_DATE;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE;
import static org.junit.Assert.assertFalse;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;

/**
 *  Tests the workflow related to the "Save as draft" functionality
 */
public class DocumentWorkflowSaveAsDraftTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void saveAsDraftReturnsDraft() throws Exception {
        final Document document = saveDraft();
        final Node node = document.getNode(session);
        final boolean transferable = node.getProperty(HippoStdNodeType.HIPPOSTD_TRANSFERABLE).getBoolean();
        assertTrue("After saving a draft the transferable property should be set to true", transferable);
    }

    @Test
    public void editAsDraftReturnsDraft() throws Exception {
        saveDraft();
        final Document draft = getDocumentWorkflow(handle).editDraft();
        final Node draftNode = draft.getNode(session);
        final boolean transferable =
                Optional.ofNullable(draftNode.getProperty(HippoStdNodeType.HIPPOSTD_TRANSFERABLE).getBoolean())
                        .orElse(false);
        assertFalse(transferable);
    }

    @Test
    public void obtainEditableInstanceReturnsCopyOfUnpublishedVariant() throws Exception {
        final Document draftBeforeObtainEditableInstance = saveDraft();
        final Node draftBeforeNode = draftBeforeObtainEditableInstance.getNode(session);
        assertTrue(draftBeforeNode.hasProperty("foo"));
        final Document document = getDocumentWorkflow(handle).obtainEditableInstance();
        session.save();
        final Node draftBasedOnUnpublishedVariant = document.getNode(session);
        assertFalse("Draft should not contain property that was only present on 'saved draft'"
                , draftBasedOnUnpublishedVariant.hasProperty("foo"));
    }


    private Document saveDraft() throws RepositoryException, WorkflowException, RemoteException {
        Node draft = handle.addNode(handle.getName(), "hippo:document");
        draft.addMixin(HIPPOSTDPUBWF_DOCUMENT);
        draft.addMixin(MIX_VERSIONABLE);
        draft.addMixin(NT_RELAXED);
        draft.setProperty(HIPPOSTD_STATE, "draft");
        draft.setProperty(HIPPOSTDPUBWF_CREATION_DATE, Calendar.getInstance());
        draft.setProperty(HIPPOSTDPUBWF_CREATED_BY, "testuser");
        draft.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE, Calendar.getInstance());
        draft.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_BY, "testuser");
        draft.setProperty(HIPPOSTD_HOLDER, session.getUserID());
        draft.setProperty("foo", "foo");
        session.save();
        DocumentWorkflow workflow = getDocumentWorkflow(handle);
        final Document document = workflow.saveDraft();
        session.save();
        return document;

    }


}
