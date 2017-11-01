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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.model.DocumentInfo;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.model.NewDocumentInfo;
import org.onehippo.cms.channelmanager.content.document.util.DisplayNameUtils;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.document.util.FolderUtils;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ConflictException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.MethodNotAllowed;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.cms.channelmanager.content.error.ResetContentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DocumentsServiceImpl implements DocumentsService {
    private static final Logger log = LoggerFactory.getLogger(DocumentsServiceImpl.class);
    private static final String WORKFLOW_CATEGORY_EDIT = "editing";
    private static final String WORKFLOW_CATEGORY_INTERNAL = "internal";
    private static final DocumentsService INSTANCE = new DocumentsServiceImpl();

    static DocumentsService getInstance() {
        return INSTANCE;
    }

    private DocumentsServiceImpl() { }

    @Override
    public Document createDraft(final String uuid, final Session session, final Locale locale)
            throws ErrorWithPayloadException {
        final Node handle = getHandle(uuid, session);
        final EditableWorkflow workflow = getEditableWorkflow(handle);

        if (!EditingUtils.canCreateDraft(workflow)) {
            throw new ForbiddenException(
                    withDisplayName(EditingUtils.determineEditingFailure(workflow, session).orElse(null), handle)
            );
        }

        final DocumentType docType = getDocumentType(handle, locale);
        if (docType.isReadOnlyDueToUnknownValidator()) {
            throw new ForbiddenException(
                    withDisplayName(new ErrorInfo(ErrorInfo.Reason.UNKNOWN_VALIDATOR), handle)
            );
        }

        final Node draft = EditingUtils.createDraft(workflow, session).orElseThrow(ForbiddenException::new);
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
        final EditableWorkflow workflow = getEditableWorkflow(handle);
        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)
                .orElseThrow(NotFoundException::new);

        if (!EditingUtils.canUpdateDraft(workflow)) {
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

        EditingUtils.copyToPreviewAndKeepEditing(workflow, session)
                .orElseThrow(() -> new InternalServerErrorException(errorInfoFromHintsOrNoHolder(workflow, session)));

        FieldTypeUtils.readFieldValues(draft, docType.getFields(), document.getFields());

        document.getInfo().setDirty(false);

        return document;
    }

    @Override
    public void updateDraftField(final String uuid, final FieldPath fieldPath, final List<FieldValue> fieldValues, final Session session, final Locale locale) throws ErrorWithPayloadException {
        final Node handle = getHandle(uuid, session);
        final EditableWorkflow workflow = getEditableWorkflow(handle);
        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)
                .orElseThrow(NotFoundException::new);

        if (!EditingUtils.canUpdateDraft(workflow)) {
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
        final EditableWorkflow workflow = getEditableWorkflow(handle);

        if (!EditingUtils.canDeleteDraft(workflow)) {
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

    @Override
    public Document createDocument(final NewDocumentInfo info, final Session session, final Locale locale) throws ErrorWithPayloadException {
        final String name = checkNotEmpty("name", info.getName());
        final String slug = checkNotEmpty("slug", info.getSlug());
        final String templateQuery = checkNotEmpty("templateQuery", info.getTemplateQuery());
        final String documentTypeId = checkNotEmpty("documentTypeId", info.getDocumentTypeId());
        final String rootPath = checkNotEmpty("rootPath", info.getRootPath());
        final String defaultPath = info.getDefaultPath();

        final Node rootFolder = FolderUtils.getFolder(rootPath, session);
        final Node folder = StringUtils.isEmpty(defaultPath) ? rootFolder : FolderUtils.getOrCreateFolder(rootFolder, defaultPath, session);

        if (FolderUtils.nodeWithDisplayNameExists(folder, name)) {
            throw new ConflictException(new ErrorInfo(ErrorInfo.Reason.NAME_ALREADY_EXISTS));
        }
        if (FolderUtils.nodeExists(folder, slug)) {
            throw new ConflictException(new ErrorInfo(ErrorInfo.Reason.SLUG_ALREADY_EXISTS));
        }

        final FolderWorkflow folderWorkflow = getFolderWorkflow(folder);

        try {
            final String documentPath = folderWorkflow.add(templateQuery, documentTypeId, slug);
            log.debug("Created document {}", documentPath);

            final Node handle = session.getNode(documentPath);

            if (!slug.equals(name)) {
                final String folderLocale = FolderUtils.getLocale(folder);
                final String displayName = DisplayNameUtils.encodeDisplayName(name, folderLocale);
                DisplayNameUtils.setDisplayName(handle, displayName);
            }

            session.save();

            return getDraft(handle, documentTypeId, locale);
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to add document '{}' of type '{}' to folder '{}' using template query '{}'",
                    slug, documentTypeId, info.getRootPath(), templateQuery);
            throw new InternalServerErrorException();
        }
    }

    private String checkNotEmpty(final String propName, final String propValue) throws BadRequestException {
        if (StringUtils.isEmpty(propValue)) {
            final String errorMessage = "Property '" + propName + "' cannot be empty";
            log.warn(errorMessage);
            throw new BadRequestException(new ErrorInfo(ErrorInfo.Reason.INVALID_DATA, "error", errorMessage));
        }
        return propValue;
    }

    private Document getDraft(final Node handle, final String documentTypeId, final Locale locale) throws ErrorWithPayloadException, RepositoryException {
        final DocumentType docType = DocumentTypesService.get().getDocumentType(documentTypeId, handle.getSession(), locale);
        if (docType.isReadOnlyDueToUnknownValidator()) {
            throw new ResetContentException();
        }

        final Document document = assembleDocument(handle.getIdentifier(), handle, docType);
        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)
                .orElseThrow(InternalServerErrorException::new);
        FieldTypeUtils.readFieldValues(draft, docType.getFields(), document.getFields());
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

    private EditableWorkflow getEditableWorkflow(final Node handle) throws MethodNotAllowed {
        return WorkflowUtils.getWorkflow(handle, WORKFLOW_CATEGORY_EDIT, EditableWorkflow.class)
                .orElseThrow(() -> new MethodNotAllowed(
                        withDisplayName(new ErrorInfo(ErrorInfo.Reason.NOT_A_DOCUMENT), handle)
                ));
    }

    private static FolderWorkflow getFolderWorkflow(final Node folderNode) throws MethodNotAllowed {
        return WorkflowUtils.getWorkflow(folderNode, WORKFLOW_CATEGORY_INTERNAL, FolderWorkflow.class)
                .orElseThrow(() -> new MethodNotAllowed(
                        withDisplayName(new ErrorInfo(ErrorInfo.Reason.NOT_A_FOLDER), folderNode)
                ));
    }

    private DocumentType getDocumentType(final Node handle, final Locale locale) throws InternalServerErrorException {
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

    private static ErrorInfo withDisplayName(final ErrorInfo errorInfo, final Node handle) {
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
        return EditingUtils.determineEditingFailure(workflow, session)
                .orElseGet(() -> new ErrorInfo(ErrorInfo.Reason.NO_HOLDER));
    }
}
