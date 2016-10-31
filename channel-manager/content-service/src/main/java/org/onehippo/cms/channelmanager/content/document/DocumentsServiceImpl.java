/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.content.document;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.model.DocumentInfo;
import org.onehippo.cms.channelmanager.content.document.model.EditingInfo;
import org.onehippo.cms.channelmanager.content.document.model.ErrorInfo;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypeNotFoundException;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.cms.channelmanager.content.MockResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentsServiceImpl implements DocumentsService {
    private static final Logger log = LoggerFactory.getLogger(DocumentsServiceImpl.class);
    private static final String WORKFLOW_CATEGORY_EDIT = "editing";
    private static final DocumentsService INSTANCE = new DocumentsServiceImpl();

    static DocumentsService getInstance() {
        return INSTANCE;
    }

    private DocumentsServiceImpl() { }

    @Override
    public Document createDraft(final String uuid, final Session session)
            throws DocumentNotFoundException {
        final Node handle = DocumentUtils.getHandle(uuid, session).orElseThrow(DocumentNotFoundException::new);
        final EditableWorkflow workflow = WorkflowUtils.getWorkflow(handle, WORKFLOW_CATEGORY_EDIT, EditableWorkflow.class)
                                                       .orElseThrow(DocumentNotFoundException::new);
        final DocumentType docType = getDocumentType(handle);
        final Document document = assembleDocument(uuid, handle, workflow, docType);
        final EditingInfo editingInfo = document.getInfo().getEditingInfo();

        if (editingInfo.getState() == EditingInfo.State.AVAILABLE) {
            final Optional<Node> optionalDraft = EditingUtils.createDraft(workflow, handle);
            if (optionalDraft.isPresent()) {
                loadFields(document, optionalDraft.get(), docType);
            } else {
                editingInfo.setState(EditingInfo.State.UNAVAILABLE);
            }
        }
        return document;
    }

    @Override
    public void updateDraft(final String uuid, final Document document, final Session session)
            throws DocumentNotFoundException, OperationFailedException {
        final Node handle = DocumentUtils.getHandle(uuid, session).orElseThrow(DocumentNotFoundException::new);
        final EditableWorkflow workflow = WorkflowUtils.getWorkflow(handle, WORKFLOW_CATEGORY_EDIT, EditableWorkflow.class)
                .orElseThrow(DocumentNotFoundException::new);
        final DocumentType docType = getDocumentType(handle);

        if (!EditingUtils.canUpdateDocument(workflow)) {
            throw new OperationFailedException(new ErrorInfo(ErrorInfo.Reason.NOT_HOLDER));
        }
        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)
                .orElseThrow(DocumentNotFoundException::new);

        if (writeFields(document, draft, docType)) {
            persistChangesAndKeepEditing(session, workflow);
        } else {
            cancelPendingChanges(session);
            throw new OperationFailedException(); // TODO: report per-field errors?
        }
    }

    @Override
    public void deleteDraft(final String uuid, final Session session) throws DocumentNotFoundException, OperationFailedException {
        final Node handle = DocumentUtils.getHandle(uuid, session).orElseThrow(DocumentNotFoundException::new);
        final EditableWorkflow workflow = WorkflowUtils.getWorkflow(handle, WORKFLOW_CATEGORY_EDIT, EditableWorkflow.class)
                .orElseThrow(DocumentNotFoundException::new);

        if (!EditingUtils.canDeleteDraft(workflow)) {
            throw new OperationFailedException(new ErrorInfo(ErrorInfo.Reason.ALREADY_DELETED));
        }

        try {
            workflow.disposeEditableInstance();
            session.refresh(false); // TODO: should we use 'true' instead?
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to dispose of editable instance", e);
            throw new OperationFailedException();
        }
    }

    @Override
    public Document getPublished(final String uuid, final Session session)
            throws DocumentNotFoundException {
        if ("test".equals(uuid)) {
            return MockResponse.createTestDocument(uuid);
        }

        final Node handle = DocumentUtils.getHandle(uuid, session).orElseThrow(DocumentNotFoundException::new);
        final EditableWorkflow workflow = WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)
                                                       .orElseThrow(DocumentNotFoundException::new);
        final DocumentType docType = getDocumentType(handle);
        final Document document = assembleDocument(uuid, handle, workflow, docType);

        WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED)
                .ifPresent(unpublished -> loadFields(document, unpublished, docType));
        return document;
    }

    private DocumentType getDocumentType(final Node handle)
            throws DocumentNotFoundException {
        try {
            return DocumentTypesService.get().getDocumentType(handle, Optional.empty());
        } catch (DocumentTypeNotFoundException e) {
            final String handlePath = JcrUtils.getNodePathQuietly(handle);
            throw new DocumentNotFoundException("Failed to retrieve type of document '" + handlePath + "'", e);
        }
    }

    private Document assembleDocument(final String uuid, final Node handle,
                                      final EditableWorkflow workflow, final DocumentType docType) {
        final Document document = new Document();
        document.setId(uuid);

        final DocumentInfo documentInfo = new DocumentInfo();
        documentInfo.setTypeId(docType.getId());
        document.setInfo(documentInfo);

        DocumentUtils.getDisplayName(handle).ifPresent(document::setDisplayName);

        final EditingInfo editingInfo = EditingUtils.determineEditingInfo(workflow, handle);
        documentInfo.setEditingInfo(editingInfo);

        return document;
    }

    private void loadFields(final Document document, final Node variant, final DocumentType docType) {
        for (FieldType field : docType.getFields()) {
            field.readFrom(variant).ifPresent(value -> document.addField(field.getId(), value));
        }
    }

    // TODO: how to communicate about write errors...?
    private boolean writeFields(final Document document, final Node variant, final DocumentType docType) {
        int errors = 0;
        final Map<String, Object> valueMap = document.getFields();
        for (FieldType fieldType : docType.getFields()) {
            errors += fieldType.writeTo(variant, Optional.ofNullable(valueMap.get(fieldType.getId())));
        }
        return errors == 0;
    }

    private void cancelPendingChanges(final Session session) {
        try {
            session.refresh(false);
        } catch (RepositoryException e) {
            log.warn("Problem cancelling pending changes", e);
        }
    }

    private void persistChangesAndKeepEditing(final Session session, final EditableWorkflow workflow)
            throws OperationFailedException {
        try {
            session.save();
            workflow.commitEditableInstance();
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to persist changes", e);
            throw new OperationFailedException();
        }

        try {
            session.refresh(true); // TODO: copied from CMS, assume that this makes the changes to the unpublished
                                   // variant visible in this session, discuss the need to do this.
            workflow.obtainEditableInstance();
        } catch (WorkflowException e) {
            log.warn("User '{}' failed to re-obtain ownership of document", session.getUserID(), e);
            throw new OperationFailedException(new ErrorInfo(ErrorInfo.Reason.HOLDERSHIP_LOST));
        } catch (RepositoryException | RemoteException e) {
            log.warn("User '{}' failed to re-obtain ownership of document", session.getUserID(), e);
            throw new OperationFailedException();
        }
    }
}
