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

import javax.jcr.Item;
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
import org.hippoecm.repository.util.WorkflowUtils.Variant;
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
import org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.MethodNotAllowed;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.cms.channelmanager.content.error.ResetContentException;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.util.JcrUtils.getNodePathQuietly;

class DocumentsServiceImpl implements DocumentsService {
    private static final Logger log = LoggerFactory.getLogger(DocumentsServiceImpl.class);
    private static final String WORKFLOW_CATEGORY_DEFAULT = "default";
    private static final String WORKFLOW_CATEGORY_EDIT = "editing";
    private static final String WORKFLOW_CATEGORY_INTERNAL = "internal";
    private static final DocumentsService INSTANCE = new DocumentsServiceImpl();

    static DocumentsService getInstance() {
        return INSTANCE;
    }

    private DocumentsServiceImpl() {
    }

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
                    withDisplayName(new ErrorInfo(Reason.UNKNOWN_VALIDATOR), handle)
            );
        }

        final Node draft = EditingUtils.createDraft(workflow, session).orElseThrow(ForbiddenException::new);
        final Document document = assembleDocument(uuid, handle, docType);
        FieldTypeUtils.readFieldValues(draft, docType.getFields(), document.getFields());

        final boolean isDirty = WorkflowUtils.getDocumentVariantNode(handle, Variant.UNPUBLISHED)
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
        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)
                .orElseThrow(NotFoundException::new);

        if (!EditingUtils.canUpdateDraft(workflow)) {
            throw new ForbiddenException(errorInfoFromHintsOrNoHolder(workflow, session));
        }

        final DocumentType docType = getDocumentType(handle, locale);
        if (docType.isReadOnlyDueToUnknownValidator()) {
            throw new ForbiddenException("Document type " + docType + " can not be updated.");
        }

        // Push fields onto draft node
        FieldTypeUtils.writeFieldValues(document.getFields(), docType.getFields(), draft);

        // Persist changes to repository
        try {
            session.save();
        } catch (final RepositoryException e) {
            final String warning = "Failed to save changes to draft node of document " + uuid;
            log.warn(warning, e);
            throw new InternalServerErrorException(warning);
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
        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)
                .orElseThrow(NotFoundException::new);

        if (!EditingUtils.canUpdateDraft(workflow)) {
            throw new ForbiddenException(errorInfoFromHintsOrNoHolder(workflow, session));
        }

        final DocumentType docType = getDocumentType(handle, locale);
        if (docType.isReadOnlyDueToUnknownValidator()) {
            throw new ForbiddenException("Document type " + docType + " can not be updated.");
        }

        // Write field value to draft node
        if (FieldTypeUtils.writeFieldValue(fieldPath, fieldValues, docType.getFields(), draft)) {
            // Persist changes to repository
            try {
                session.save();
            } catch (final RepositoryException e) {
                final String warning = "Failed to save changes to field " + fieldPath + " in draft node of document " + uuid;
                log.warn(warning, e);
                throw new InternalServerErrorException(warning);
            }
        }
    }

    @Override
    public void deleteDraft(final String uuid, final Session session, final Locale locale)
            throws ErrorWithPayloadException {
        final Node handle = getHandle(uuid, session);
        final EditableWorkflow workflow = getEditableWorkflow(handle);

        if (!EditingUtils.canDeleteDraft(workflow)) {
            throw new ForbiddenException(new ErrorInfo(Reason.ALREADY_DELETED));
        }

        try {
            workflow.disposeEditableInstance();
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            final String warning = "Failed to dispose of editable instance";
            log.warn(warning, e);
            throw new InternalServerErrorException(warning);
        }
    }

    @Override
    public Document getPublished(final String uuid, final Session session, final Locale locale)
            throws ErrorWithPayloadException {
        final Node handle = getHandle(uuid, session);
        final DocumentType docType = getDocumentType(handle, locale);
        final Document document = assembleDocument(uuid, handle, docType);

        WorkflowUtils.getDocumentVariantNode(handle, Variant.PUBLISHED)
                .ifPresent(node -> FieldTypeUtils.readFieldValues(node, docType.getFields(), document.getFields()));
        return document;
    }

    @Override
    public Document createDocument(final NewDocumentInfo newDocumentInfo, final Session session, final Locale locale) throws ErrorWithPayloadException {
        final String name = checkNotEmpty("name", newDocumentInfo.getName());
        final String slug = checkNotEmpty("slug", newDocumentInfo.getSlug());
        final String templateQuery = checkNotEmpty("templateQuery", newDocumentInfo.getTemplateQuery());
        final String documentTypeId = checkNotEmpty("documentTypeId", newDocumentInfo.getDocumentTypeId());
        final String rootPath = checkNotEmpty("rootPath", newDocumentInfo.getRootPath());
        final String defaultPath = newDocumentInfo.getDefaultPath();

        final Node rootFolder = FolderUtils.getFolder(rootPath, session);
        final Node folder = StringUtils.isEmpty(defaultPath) ? rootFolder : FolderUtils.getOrCreateFolder(rootFolder, defaultPath, session);

        if (FolderUtils.nodeWithDisplayNameExists(folder, name)) {
            throw new ConflictException(new ErrorInfo(Reason.NAME_ALREADY_EXISTS));
        }
        if (FolderUtils.nodeExists(folder, slug)) {
            throw new ConflictException(new ErrorInfo(Reason.SLUG_ALREADY_EXISTS));
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
            final String warning = "Failed to add document " + slug + " of type " + documentTypeId + " to folder " + newDocumentInfo.getRootPath() + " using template query " + templateQuery;
            log.warn(warning, e);
            throw new InternalServerErrorException(warning);
        }
    }

    @Override
    public void deleteDocument(final String uuid, final Session session, final Locale locale) throws ErrorWithPayloadException {
        final Node handle = getHandle(uuid, session);
        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);

        // Try to archive the document (i.e. move to the attic) so there's still a pointer into the version history
        if (EditingUtils.canArchiveDocument(documentWorkflow)) {
            archiveDocument(uuid, documentWorkflow);
            return;
        }

        // Archiving not possible: the document can be published, a request can be pending etc. Only case left to check:
        // is the document a draft that was just created? (in which case it won't have a 'preview' variant yet)
        if (EditingUtils.hasPreview(documentWorkflow)) {
            final String warning = "Forbade to erase document " + uuid + ": it already has a preview variant";
            log.warn(warning);
            throw new ForbiddenException(warning);
        }

        // No preview indeed, so erase the draft document
        final Node folder = FolderUtils.getFolder(handle);
        final FolderWorkflow folderWorkflow = getFolderWorkflow(folder);

        if (EditingUtils.canEraseDocument(folderWorkflow)) {
            eraseDocument(uuid, folderWorkflow, handle);
        } else {
            final String warning = "Forbade to erase document " + JcrUtils.getNodeNameQuietly(handle) + ": not allowed by the workflow of folder " + getNodePathQuietly(folder);
            log.warn(warning);
            throw new ForbiddenException(warning);
        }
    }

    private static void archiveDocument(final String uuid, final DocumentWorkflow documentWorkflow) throws InternalServerErrorException, NotFoundException, MethodNotAllowed {
        try {
            log.info("Archiving document '{}'", uuid);
            documentWorkflow.delete();
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to archive document '{}'", uuid, e);
            throw new InternalServerErrorException(e);
        }
    }

    private static void eraseDocument(final String uuid, final FolderWorkflow folderWorkflow, final Item handle) throws InternalServerErrorException, NotFoundException, MethodNotAllowed {
        try {
            log.info("Erasing document '{}'", uuid);
            folderWorkflow.delete(handle.getName());
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to erase document '{}'", uuid, e);
            throw new InternalServerErrorException(e);
        }
    }

    private static String checkNotEmpty(final String propName, final String propValue) throws BadRequestException {
        if (StringUtils.isEmpty(propValue)) {
            final String errorMessage = "Property '" + propName + "' cannot be empty";
            log.warn(errorMessage);
            throw new BadRequestException(new ErrorInfo(Reason.INVALID_DATA, "error", errorMessage));
        }
        return propValue;
    }

    private static Document getDraft(final Node handle, final String documentTypeId, final Locale locale) throws ErrorWithPayloadException, RepositoryException {
        final DocumentType docType = DocumentTypesService.get().getDocumentType(documentTypeId, handle.getSession(), locale);
        if (docType.isReadOnlyDueToUnknownValidator()) {
            throw new ResetContentException();
        }

        final Document document = assembleDocument(handle.getIdentifier(), handle, docType);
        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)
                .orElseThrow(InternalServerErrorException::new);
        FieldTypeUtils.readFieldValues(draft, docType.getFields(), document.getFields());
        return document;
    }

    private static Node getHandle(final String uuid, final Session session) throws ErrorWithPayloadException {
        return DocumentUtils.getHandle(uuid, session)
                .filter(DocumentsServiceImpl::isValidHandle)
                .orElseThrow(NotFoundException::new);
    }

    private static boolean isValidHandle(final Node handle) {
        return DocumentUtils.getVariantNodeType(handle)
                .filter(type -> !type.equals(HippoNodeType.NT_DELETED))
                .isPresent();
    }

    private static EditableWorkflow getEditableWorkflow(final Node handle) throws MethodNotAllowed {
        return WorkflowUtils.getWorkflow(handle, WORKFLOW_CATEGORY_EDIT, EditableWorkflow.class)
                .orElseThrow(() -> new MethodNotAllowed(
                        withDisplayName(new ErrorInfo(Reason.NOT_A_DOCUMENT), handle)
                ));
    }

    private static DocumentWorkflow getDocumentWorkflow(final Node handle) throws MethodNotAllowed {
        return WorkflowUtils.getWorkflow(handle, WORKFLOW_CATEGORY_DEFAULT, DocumentWorkflow.class)
                .orElseThrow(() -> new MethodNotAllowed(
                        withDisplayName(new ErrorInfo(Reason.NOT_A_DOCUMENT), handle)
                ));
    }

    private static FolderWorkflow getFolderWorkflow(final Node folderNode) throws MethodNotAllowed {
        return WorkflowUtils.getWorkflow(folderNode, WORKFLOW_CATEGORY_INTERNAL, FolderWorkflow.class)
                .orElseThrow(() -> new MethodNotAllowed(
                        withDisplayName(new ErrorInfo(Reason.NOT_A_FOLDER), folderNode)
                ));
    }

    private static DocumentType getDocumentType(final Node handle, final Locale locale) throws InternalServerErrorException {
        final String id = DocumentUtils.getVariantNodeType(handle).orElseThrow(InternalServerErrorException::new);

        try {
            return DocumentTypesService.get().getDocumentType(id, handle.getSession(), locale);
        } catch (final RepositoryException e) {
            final String warning = "Failed to retrieve JCR session for node " + getNodePathQuietly(handle);
            log.warn(warning, e);
            throw new InternalServerErrorException(warning);
        } catch (final ErrorWithPayloadException e) {
            final String warning = "Failed to retrieve type of document " + getNodePathQuietly(handle);
            log.debug(warning, e);
            throw new InternalServerErrorException(warning);
        }
    }

    private static Document assembleDocument(final String uuid, final Node handle, final DocumentType docType) {
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

    private static ErrorInfo errorInfoFromHintsOrNoHolder(final Workflow workflow, final Session session) {
        return EditingUtils.determineEditingFailure(workflow, session)
                .orElseGet(() -> new ErrorInfo(Reason.NO_HOLDER));
    }
}
