/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.model.DocumentInfo;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.MethodNotAllowed;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentsServiceImpl implements DocumentsService {

    private static final Logger log = LoggerFactory.getLogger(DocumentsServiceImpl.class);
    private static final String WORKFLOW_CATEGORY_EDIT = "editing";

    private EditingUtils editingUtils;

    public void setEditingUtils(EditingUtils editingUtils) {
        this.editingUtils = editingUtils;
    }

    @Override
    public Document createDraft(final String uuid, final Session session, final Locale locale)
            throws ErrorWithPayloadException {
        final Node handle = getHandle(uuid, session);
        final EditableWorkflow workflow = getWorkflow(handle);

        if (!editingUtils.canCreateDraft(workflow)) {
            throw new ForbiddenException(
                    withDocumentName(editingUtils.determineEditingFailure(workflow, session).orElse(null), handle)
            );
        }

        final DocumentType docType = getDocumentType(handle, locale);
        if (docType.isReadOnlyDueToUnknownValidator()) {
            throw new ForbiddenException(
                    withDocumentName(new ErrorInfo(ErrorInfo.Reason.UNKNOWN_VALIDATOR), handle)
            );
        }

        final Node draft = editingUtils.createDraft(workflow, session).orElseThrow(ForbiddenException::new);
        final Document document = assembleDocument(uuid, handle, docType);
        FieldTypeUtils.readFieldValues(draft, docType.getFields(), document.getFields());

        boolean isDirty = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED)
                .map(unpublished -> {
                    final Map<String, List<FieldValue>> unpublishedFields = new HashMap<>();
                    FieldTypeUtils.readFieldValues(unpublished, docType.getFields(), unpublishedFields);
                    return !document.getFields().equals(unpublishedFields);
                })
                .orElse(false);

        document.getInfo().setDirty(isDirty);

        return document;
    }

    @Override
    public Document updateDraft(final String uuid, final Document document, final Session session, final Locale locale)
            throws ErrorWithPayloadException {
        final Node handle = getHandle(uuid, session);
        final EditableWorkflow workflow = getWorkflow(handle);
        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)
                .orElseThrow(NotFoundException::new);

        if (!editingUtils.canUpdateDraft(workflow)) {
            throw new ForbiddenException(errorInfoFromHintsOrNoHolder(workflow, session));
        }

        final DocumentType docType = getDocumentType(handle, locale);
        if (docType.isReadOnlyDueToUnknownValidator()) {
            throw new ForbiddenException();
        }

        // Push fields onto draft node
        FieldTypeUtils.writeFieldValues(document.getFields(), docType.getFields(), draft);

        // Persist changes to repository
        try {
            session.save();
        } catch (RepositoryException e) {
            log.warn("Failed to save changes to draft node of document {}", uuid, e);
            throw new InternalServerErrorException();
        }

        if (!FieldTypeUtils.validateFieldValues(document.getFields(), docType.getFields())) {
            throw new BadRequestException(document);
        }

        editingUtils.copyToPreviewAndKeepEditing(workflow, session)
                .orElseThrow(() -> new InternalServerErrorException(errorInfoFromHintsOrNoHolder(workflow, session)));

        FieldTypeUtils.readFieldValues(draft, docType.getFields(), document.getFields());

        document.getInfo().setDirty(false);

        return document;
    }

    @Override
    public void updateDraftField(final String uuid, final FieldPath fieldPath, final List<FieldValue> fieldValues, final Session session, final Locale locale) throws ErrorWithPayloadException {
        final Node handle = getHandle(uuid, session);
        final EditableWorkflow workflow = getWorkflow(handle);
        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)
                .orElseThrow(NotFoundException::new);

        if (!editingUtils.canUpdateDraft(workflow)) {
            throw new ForbiddenException(errorInfoFromHintsOrNoHolder(workflow, session));
        }

        final DocumentType docType = getDocumentType(handle, locale);
        if (docType.isReadOnlyDueToUnknownValidator()) {
            throw new ForbiddenException();
        }

        // Write field value to draft node
        if (FieldTypeUtils.writeFieldValue(fieldPath, fieldValues, docType.getFields(), draft)) {
            // Persist changes to repository
            try {
                session.save();
            } catch (RepositoryException e) {
                log.warn("Failed to save changes to field '{}' in draft node of document {}", fieldPath, uuid, e);
                throw new InternalServerErrorException();
            }
        }
    }

    @Override
    public void deleteDraft(final String uuid, final Session session, final Locale locale)
            throws ErrorWithPayloadException {
        final Node handle = getHandle(uuid, session);
        final EditableWorkflow workflow = getWorkflow(handle);

        if (!editingUtils.canDeleteDraft(workflow)) {
            throw new ForbiddenException(new ErrorInfo(ErrorInfo.Reason.ALREADY_DELETED));
        }

        try {
            workflow.disposeEditableInstance();
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to dispose of editable instance", e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public Document getPublished(final String uuid, final Session session, final Locale locale)
            throws ErrorWithPayloadException {
        final Node handle = getHandle(uuid, session);
        final DocumentType docType = getDocumentType(handle, locale);
        final Document document = assembleDocument(uuid, handle, docType);

        WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED)
                .ifPresent(node -> FieldTypeUtils.readFieldValues(node, docType.getFields(), document.getFields()));
        return document;
    }

    private Node getHandle(final String uuid, final Session session) throws ErrorWithPayloadException {
        return DocumentUtils.getHandle(uuid, session)
                .filter(this::isValidHandle)
                .orElseThrow(NotFoundException::new);
    }

    private boolean isValidHandle(final Node handle) {
        return DocumentUtils.getVariantNodeType(handle)
                .filter(type -> !type.equals(HippoNodeType.NT_DELETED))
                .isPresent();
    }

    private EditableWorkflow getWorkflow(final Node handle) throws ErrorWithPayloadException {
        return WorkflowUtils.getWorkflow(handle, WORKFLOW_CATEGORY_EDIT, EditableWorkflow.class)
                .orElseThrow(() -> new MethodNotAllowed(
                        withDocumentName(new ErrorInfo(ErrorInfo.Reason.NOT_A_DOCUMENT), handle)
                ));
    }

    private DocumentType getDocumentType(final Node handle, final Locale locale)
            throws ErrorWithPayloadException {
        final String id = DocumentUtils.getVariantNodeType(handle).orElseThrow(InternalServerErrorException::new);

        try {
            return DocumentTypesService.get().getDocumentType(id, handle.getSession(), locale);
        } catch (RepositoryException e) {
            log.warn("Failed to retrieve JCR session for node '{}'", JcrUtils.getNodePathQuietly(handle), e);
            throw new InternalServerErrorException();
        } catch (ErrorWithPayloadException e) {
            log.debug("Failed to retrieve type of document '{}'", JcrUtils.getNodePathQuietly(handle), e);
            throw new InternalServerErrorException();
        }
    }

    private Document assembleDocument(final String uuid, final Node handle, final DocumentType docType) {
        final Document document = new Document();
        document.setId(uuid);

        final DocumentInfo documentInfo = new DocumentInfo();
        documentInfo.setTypeId(docType.getId());
        document.setInfo(documentInfo);

        DocumentUtils.getDisplayName(handle).ifPresent(document::setDisplayName);

        return document;
    }

    private ErrorInfo withDocumentName(final ErrorInfo errorInfo, final Node handle) {
        if (errorInfo != null) {
            DocumentUtils.getDisplayName(handle).ifPresent(displayName -> {
                if (errorInfo.getParams() == null) {
                    errorInfo.setParams(new HashMap<>());
                }
                errorInfo.getParams().put("displayName", displayName);
            });
        }
        return errorInfo;
    }

    private ErrorInfo errorInfoFromHintsOrNoHolder(final Workflow workflow, final Session session) {
        return editingUtils.determineEditingFailure(workflow, session)
                .orElseGet(() -> new ErrorInfo(ErrorInfo.Reason.NO_HOLDER));
    }
}
