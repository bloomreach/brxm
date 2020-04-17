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

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.junit.Assert;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

public class DocumentWorkflowKeepDraftTest extends AbstractDocumentWorkflowIntegrationTest {


    private DocumentWorkflow workflow;

    @Test
    public void keepDraft_document_becomes_transferable() throws RepositoryException, WorkflowException, RemoteException {
        Assert.assertTrue(getDocumentVariantAfterConsecutiveEditAndKeepDraft().isTransferable());
    }

    @Test
    public void keepDraft_document_becomes_retainable() throws RepositoryException, WorkflowException, RemoteException {
        Assert.assertTrue(getDocumentVariantAfterConsecutiveEditAndKeepDraft().isRetainable());
    }

    @Test
    public void editDraft_document_becomes_non_transferable() throws RepositoryException, RemoteException, WorkflowException {
        Assert.assertFalse(getDocumentVariantAfterConsecutiveEditKeepDraftAndEditDraft().isTransferable());
    }

    @Test
    public void editDraft_document_becomes_remains_retainable() throws RepositoryException, RemoteException, WorkflowException {
        Assert.assertTrue(getDocumentVariantAfterConsecutiveEditKeepDraftAndEditDraft().isRetainable());
    }

    @Test
    public void save_document_becomes_non_retainable() throws RepositoryException, RemoteException, WorkflowException {
        Assert.assertFalse(getDocumentVariantAfterConsecutiveEditKeepDraftEditDraftAndSave().isRetainable());
    }

    @Test
    public void save_document_becomes_non_transferable() throws RepositoryException, RemoteException, WorkflowException {
        Assert.assertFalse(getDocumentVariantAfterConsecutiveEditKeepDraftEditDraftAndSave().isTransferable());
    }

    private DocumentVariant getDocumentVariantAfterConsecutiveEditKeepDraftEditDraftAndSave() throws RepositoryException, WorkflowException, RemoteException {
        getDocumentVariantAfterConsecutiveEditKeepDraftAndEditDraft();
        final Document document = workflow.commitEditableInstance();
        return getDocumentVariant(document);
    }

    private DocumentVariant getDocumentVariantAfterConsecutiveEditKeepDraftAndEditDraft() throws RepositoryException, WorkflowException, RemoteException {
        getDocumentVariantAfterConsecutiveEditAndKeepDraft();
        final Document document = workflow.editDraft();
        return getDocumentVariant(document);
    }

    private DocumentVariant getDocumentVariantAfterConsecutiveEditAndKeepDraft() throws RepositoryException, WorkflowException, RemoteException {
        workflow = getDocumentWorkflow(handle);
        workflow.obtainEditableInstance();
        final Document document = workflow.saveDraft();
        return getDocumentVariant(document);
    }

    private DocumentVariant getDocumentVariant(final Document document) throws RepositoryException {
        return new DocumentVariant(document.getNode(session));
    }

}
