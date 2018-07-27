/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
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
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DocumentVariant;
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
import org.onehippo.cms.channelmanager.content.document.model.PublicationState;
import org.onehippo.cms.channelmanager.content.document.util.DocumentNameUtils;
import org.onehippo.cms.channelmanager.content.document.util.EditingService;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.document.util.FolderUtils;
import org.onehippo.cms.channelmanager.content.document.util.HintsInspector;
import org.onehippo.cms.channelmanager.content.document.util.PublicationStateUtils;
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
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.cms.channelmanager.content.error.ResetContentException;
import org.onehippo.repository.branch.BranchHandle;
import org.onehippo.repository.documentworkflow.BranchHandleImpl;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_ID;
import static org.hippoecm.repository.util.JcrUtils.getNodePathQuietly;
import static org.onehippo.cms.channelmanager.content.document.ContextPayloadUtils.getBranchId;
import static org.onehippo.cms.channelmanager.content.document.util.ContentWorkflowUtils.getDocumentWorkflow;
import static org.onehippo.cms.channelmanager.content.document.util.ContentWorkflowUtils.getEditableWorkflow;
import static org.onehippo.cms.channelmanager.content.document.util.ContentWorkflowUtils.getFolderWorkflow;
import static org.onehippo.cms.channelmanager.content.document.util.DocumentHandleUtils.getHandle;
import static org.onehippo.cms.channelmanager.content.document.util.EditingUtils.HINT_PUBLISH;
import static org.onehippo.cms.channelmanager.content.document.util.EditingUtils.HINT_REQUEST_PUBLICATION;
import static org.onehippo.cms.channelmanager.content.document.util.EditingUtils.isHintActionTrue;
import static org.onehippo.cms.channelmanager.content.error.ErrorInfo.withDisplayName;

/**
 * Implementation class for retrieving and storing Document information.
 * <p/>
 * The repository Workflow actions are designed to support the situation that when a document is being edited, hardly
 * any other actions are available, e.g. publication. This Service supports a different case: while a
 * document is presented as editable, we also want to present a Publish button. Checking the workflow actions on the
 * editable instance of a document will not give the correct available workflow actions. This implementation takes
 * that situation into account by retrieving the workflow actions from a Document that is not yet in edit mode.
 */
public class DocumentsServiceImpl implements DocumentsService {

    private static final Logger log = LoggerFactory.getLogger(DocumentsServiceImpl.class);

    private HintsInspector hintsInspector;
    private EditingService editingService;

    public void setHintsInspector(final HintsInspector hintsInspector) {
        this.hintsInspector = hintsInspector;
    }

    public void setEditingService(final EditingService editingService) {
        this.editingService = editingService;
    }

    @Override
    public Document getDocument(final String uuid, final String branchId, final Session session, final Locale locale) throws ErrorWithPayloadException {
        final Node handle = getHandle(uuid, session);
        final DocumentType docType = getDocumentType(handle, locale);

        try {
            final BranchHandle branchHandle = new BranchHandleImpl(branchId, handle);

            final Node unpublished = branchHandle.getUnpublished();
            if (unpublished != null) {
                return createDocument(uuid, handle, docType, unpublished);
            }

            final Node published = branchHandle.getPublished();
            if (published != null) {
                return createDocument(uuid, handle, docType, published);
            }

            final Node publishedMaster = branchHandle.getPublishedMaster();
            if (publishedMaster != null) {
                return createDocument(uuid, handle, docType, publishedMaster);
            }

            throw new NotFoundException(new ErrorInfo(ErrorInfo.Reason.DOES_NOT_EXIST));

        } catch (WorkflowException e) {
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR, "error", e.getMessage()));
        }
    }

    @Override
    public Document obtainEditableDocument(final String uuid, final Session session, final Locale locale, final Map<String, Serializable> contextPayload)
            throws ErrorWithPayloadException {
        final Node handle = getHandle(uuid, session);
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        Map<String, Serializable> hints = getHints(workflow, contextPayload);
        if (!hintsInspector.canObtainEditableDocument(hints)) {
            throw hintsInspector
                    .determineEditingFailure(hints, session)
                    .map(errorInfo -> withDocumentInfo(errorInfo, handle))
                    .map(ForbiddenException::new)
                    .orElseGet(() -> new ForbiddenException(new ErrorInfo(Reason.SERVER_ERROR)));
        }

        final DocumentType docType = getDocumentType(handle, locale);
        if (docType.isReadOnlyDueToUnknownValidator()) {
            throw new ForbiddenException(
                    withDisplayName(new ErrorInfo(Reason.UNKNOWN_VALIDATOR), handle)
            );
        }

        final Node draftNode = editingService.getEditableDocumentNode(workflow, hints, session).orElseThrow(() -> new ForbiddenException(new ErrorInfo(Reason.SERVER_ERROR)));
        final Document document = assembleDocument(uuid, handle, draftNode, docType);
        FieldTypeUtils.readFieldValues(draftNode, docType.getFields(), document.getFields());

        final boolean isDirty = WorkflowUtils.getDocumentVariantNode(handle, Variant.UNPUBLISHED)
                .map(unpublished -> {
                    final Map<String, List<FieldValue>> unpublishedFields = new HashMap<>();
                    FieldTypeUtils.readFieldValues(unpublished, docType.getFields(), unpublishedFields);
                    return !document.getFields().equals(unpublishedFields);
                })
                .orElse(false);

        document.getInfo().setDirty(isDirty);

        // For master documents we must use the hints that were retrieved before the editable instance was obtained
        // from the workflow, see the class level javadoc.
        if (!getBranchId(contextPayload).equals(MASTER_BRANCH_ID)) {
            hints = getHints(workflow, contextPayload);
        }
        document.getInfo().setCanPublish(isHintActionTrue(hints, HINT_PUBLISH));
        document.getInfo().setCanRequestPublication(isHintActionTrue(hints, HINT_REQUEST_PUBLICATION));

        return document;
    }

    @Override
    public Document updateEditableDocument(final String uuid, final Document document, final Session session, final Locale locale, final Map<String, Serializable> contextPayload)
            throws ErrorWithPayloadException {
        final Node handle = getHandle(uuid, session);
        final EditableWorkflow workflow = getEditableWorkflow(handle);
        final Node draftNode = WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)
                .orElseThrow(() -> new NotFoundException(new ErrorInfo(Reason.DOES_NOT_EXIST)));

        final Map<String, Serializable> hints = getHints(workflow, contextPayload);
        if (!hintsInspector.canUpdateDocument(hints)) {
            throw new ForbiddenException(errorInfoFromHintsOrNoHolder(hints, session));
        }

        final DocumentType docType = getDocumentType(handle, locale);
        if (docType.isReadOnlyDueToUnknownValidator()) {
            throw new ForbiddenException(new ErrorInfo(Reason.UNKNOWN_VALIDATOR));
        }

        // Push fields onto draft node
        FieldTypeUtils.writeFieldValues(document.getFields(), docType.getFields(), draftNode);

        // Persist changes to repository
        try {
            session.save();
        } catch (final RepositoryException e) {
            log.warn("Failed to save changes to draft node of document {}", uuid, e);
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }

        if (!FieldTypeUtils.validateFieldValues(document.getFields(), docType.getFields())) {
            throw new BadRequestException(document);
        }

        try {
            workflow.commitEditableInstance();
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            throw new InternalServerErrorException(errorInfoFromHintsOrNoHolder(getHints(workflow, contextPayload), session));
        }

        // Get the workflow hints before obtaining an editable instance again, see the class level javadoc.
        final DocumentWorkflow newWorkflow = getDocumentWorkflow(handle);
        final Map<String, Serializable> newHints = getHints(newWorkflow, contextPayload);

        final Node newDraftNode = editingService.getEditableDocumentNode(newWorkflow, hints, session).orElseThrow(() -> new ForbiddenException(new ErrorInfo(Reason.SERVER_ERROR)));

        setDocumentPublicationState(document.getInfo(), newDraftNode);

        FieldTypeUtils.readFieldValues(newDraftNode, docType.getFields(), document.getFields());

        document.getInfo().setDirty(false);
        document.getInfo().setCanPublish(isHintActionTrue(newHints, HINT_PUBLISH));
        document.getInfo().setCanRequestPublication(isHintActionTrue(newHints, HINT_REQUEST_PUBLICATION));

        return document;
    }

    @Override
    public void updateEditableField(final String uuid, final FieldPath fieldPath, final List<FieldValue> fieldValues, final Session session, final Locale locale, final Map<String, Serializable> contextPayload) throws ErrorWithPayloadException {
        final Node handle = getHandle(uuid, session);
        final EditableWorkflow workflow = getEditableWorkflow(handle);
        final Node draftNode = WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)
                .orElseThrow(() -> new NotFoundException(new ErrorInfo(Reason.DOES_NOT_EXIST)));

        final Map<String, Serializable> hints = getHints(workflow, contextPayload);
        if (!hintsInspector.canUpdateDocument(hints)) {
            throw new ForbiddenException(errorInfoFromHintsOrNoHolder(hints, session));
        }

        final DocumentType docType = getDocumentType(handle, locale);
        if (docType.isReadOnlyDueToUnknownValidator()) {
            throw new ForbiddenException(new ErrorInfo(Reason.UNKNOWN_VALIDATOR));
        }

        // Write field value to draft node
        if (FieldTypeUtils.writeFieldValue(fieldPath, fieldValues, docType.getFields(), draftNode)) {
            // Persist changes to repository
            try {
                session.save();
            } catch (final RepositoryException e) {
                log.warn("Failed to save changes to field '{}' in draft node of document {}", fieldPath, uuid, e);
                throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
            }
        }
    }

    @Override
    public void discardEditableDocument(final String uuid, final Session session, final Locale locale, final Map<String, Serializable> contextPayload)
            throws ErrorWithPayloadException {
        final Node handle = getHandle(uuid, session);
        final EditableWorkflow workflow = getEditableWorkflow(handle);

        final Map<String, Serializable> hints = getHints(workflow, contextPayload);
        if (!hintsInspector.canDisposeEditableDocument(hints)) {
            throw new ForbiddenException(new ErrorInfo(ErrorInfo.Reason.ALREADY_DELETED));
        }

        try {
            workflow.disposeEditableInstance();
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to dispose of editable instance", e);
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }
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
        final String folderLocale = FolderUtils.getLocale(folder);

        final String encodedName = DocumentNameUtils.encodeDisplayName(name, folderLocale);
        if (FolderUtils.nodeWithDisplayNameExists(folder, encodedName)) {
            throw new ConflictException(new ErrorInfo(Reason.NAME_ALREADY_EXISTS));
        }

        final String encodedSlug = DocumentNameUtils.encodeUrlName(slug, folderLocale);
        if (FolderUtils.nodeExists(folder, encodedSlug)) {
            throw new ConflictException(new ErrorInfo(Reason.SLUG_ALREADY_EXISTS));
        }

        final FolderWorkflow folderWorkflow = getFolderWorkflow(folder);

        try {
            final String documentPath = folderWorkflow.add(templateQuery, documentTypeId, encodedSlug);
            log.debug("Created document {}", documentPath);

            final Node document = session.getNode(documentPath);
            final Node handle = document.getParent();

            if (!encodedSlug.equals(encodedName)) {
                DocumentNameUtils.setDisplayName(handle, encodedName);
            }

            session.save();
            return getCreatedDocument(handle, documentTypeId, locale);
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to add document '{}' of type '{}' to folder '{}' using template query '{}'",
                    encodedSlug, documentTypeId, newDocumentInfo.getRootPath(), templateQuery, e);
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }
    }

    private static Document getCreatedDocument(final Node handle, final String documentTypeId, final Locale locale) throws ErrorWithPayloadException, RepositoryException {
        final DocumentType docType = DocumentTypesService.get().getDocumentType(documentTypeId, handle.getSession(), locale);
        if (docType.isReadOnlyDueToUnknownValidator()) {
            throw new ResetContentException();
        }

        final Node draftNode = WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)
                .orElseThrow(() -> new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR)));
        return createDocument(handle.getIdentifier(), handle, docType, draftNode);
    }

    private static Document createDocument(final String uuid, final Node handle, final DocumentType docType, final Node unpublished) {
        final Document document = assembleDocument(uuid, handle, unpublished, docType);
        FieldTypeUtils.readFieldValues(unpublished, docType.getFields(), document.getFields());
        return document;
    }

    @Override
    public Document updateDocumentNames(final String uuid, final Document document, final Session session) throws ErrorWithPayloadException {
        final String displayName = checkNotEmpty("displayName", document.getDisplayName());
        final String urlName = checkNotEmpty("urlName", document.getUrlName());

        final Node handle = getHandle(uuid, session);
        final Node folder = FolderUtils.getFolder(handle);
        final String folderLocale = FolderUtils.getLocale(folder);
        final String handlePath = getNodePathQuietly(handle);

        final String newUrlName = DocumentNameUtils.encodeUrlName(urlName, folderLocale);
        final String oldUrlName = DocumentNameUtils.getUrlName(handle);
        final boolean changeUrlName = !newUrlName.equals(oldUrlName);

        if (changeUrlName && FolderUtils.nodeExists(folder, newUrlName)) {
            throw new ConflictException(new ErrorInfo(Reason.SLUG_ALREADY_EXISTS));
        }

        final String newDisplayName = DocumentNameUtils.encodeDisplayName(displayName, folderLocale);
        final String oldDisplayName = DocumentNameUtils.getDisplayName(handle);
        final boolean changeDisplayName = !newDisplayName.equals(oldDisplayName);

        if (changeDisplayName && FolderUtils.nodeWithDisplayNameExists(folder, newDisplayName)) {
            throw new ConflictException(new ErrorInfo(Reason.NAME_ALREADY_EXISTS));
        }

        if (changeUrlName) {
            log.info("Changing URL name of '{}' to '{}'", handlePath, newUrlName);
            DocumentNameUtils.setUrlName(handle, newUrlName);
            document.setUrlName(newUrlName);
        }

        if (changeDisplayName) {
            log.info("Changing display name of '{}' to '{}'", handlePath, newDisplayName);
            DocumentNameUtils.setDisplayName(handle, newDisplayName);
            document.setDisplayName(newDisplayName);
        }

        return document;
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
            log.warn("Forbidden to erase document '{}': it already has a preview variant", uuid);
            throw new ForbiddenException(new ErrorInfo(Reason.WORKFLOW_ERROR));
        }

        // No preview indeed, so erase the draft document
        final Node folder = FolderUtils.getFolder(handle);
        final FolderWorkflow folderWorkflow = getFolderWorkflow(folder);

        if (EditingUtils.canEraseDocument(folderWorkflow)) {
            eraseDocument(uuid, folderWorkflow, handle);
        } else {
            log.warn("Forbidden to erase document '{}': not allowed by the workflow of folder '{}'",
                    JcrUtils.getNodeNameQuietly(handle), getNodePathQuietly(folder));
            throw new ForbiddenException(new ErrorInfo(Reason.WORKFLOW_ERROR));
        }
    }

    private static void archiveDocument(final String uuid, final DocumentWorkflow documentWorkflow) throws InternalServerErrorException {
        try {
            log.info("Archiving document '{}'", uuid);
            documentWorkflow.delete();
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to archive document '{}'", uuid, e);
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }
    }

    private static void eraseDocument(final String uuid, final FolderWorkflow folderWorkflow, final Item handle) throws InternalServerErrorException {
        try {
            log.info("Erasing document '{}'", uuid);
            folderWorkflow.delete(handle.getName());
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to erase document '{}'", uuid, e);
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
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

    private static DocumentType getDocumentType(final Node handle, final Locale locale) throws InternalServerErrorException {
        final String id = DocumentUtils.getVariantNodeType(handle).orElseThrow(() -> new InternalServerErrorException(new ErrorInfo(Reason.DOES_NOT_EXIST)));

        try {
            return DocumentTypesService.get().getDocumentType(id, handle.getSession(), locale);
        } catch (final RepositoryException e) {
            log.warn("Failed to retrieve JCR session for node '{}'", getNodePathQuietly(handle), e);
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        } catch (final ErrorWithPayloadException e) {
            log.warn("Failed to retrieve type of document '{}'", getNodePathQuietly(handle), e);
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }
    }

    private static Document assembleDocument(final String uuid, final Node handle, final Node variant, final DocumentType docType) {
        final Document document = new Document();
        document.setId(uuid);

        final DocumentInfo documentInfo = new DocumentInfo();
        documentInfo.setTypeId(docType.getId());
        setDocumentPublicationState(documentInfo, variant);
        document.setInfo(documentInfo);

        DocumentUtils.getDisplayName(handle).ifPresent(document::setDisplayName);
        document.setUrlName(JcrUtils.getNodeNameQuietly(handle));

        document.setRepositoryPath(JcrUtils.getNodePathQuietly(handle));

        try {
            final DocumentVariant documentVariant = new DocumentVariant(variant);
            document.setState(documentVariant.getState());
            document.setBranchId(documentVariant.getBranchId());
        } catch (RepositoryException e) {
            log.warn("Failed to set state and branchId for document with id '{}'", uuid, e);
        }
        return document;
    }

    private static void setDocumentPublicationState(final DocumentInfo documentInfo, final Node variant) {
        final PublicationState state = PublicationStateUtils.getPublicationStateFromVariant(variant);
        documentInfo.setPublicationState(state);
    }

    private static ErrorInfo withDocumentInfo(final ErrorInfo errorInfo, final Node handle) {
        DocumentUtils.getDisplayName(handle).ifPresent(displayName -> errorInfo.addParam("displayName", displayName));

        final PublicationState publicationState = PublicationStateUtils.getPublicationStateFromHandle(handle);
        errorInfo.addParam("publicationState", publicationState);

        return errorInfo;
    }

    private ErrorInfo errorInfoFromHintsOrNoHolder(Map<String, Serializable> hints, Session session) {
        return hintsInspector.determineEditingFailure(hints, session)
                .orElseGet(() -> new ErrorInfo(ErrorInfo.Reason.NO_HOLDER));
    }

    private Map<String, Serializable> getHints(EditableWorkflow workflow, Map<String, Serializable> contextPayload) {
        final Map<String, Serializable> hints = new HashMap<>();
        if (contextPayload != null) {
            hints.putAll(contextPayload);
        }
        final String branchId = getBranchId(contextPayload);
        try {
            hints.putAll(workflow.hints(branchId));
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed reading hints from workflow", e);
        }
        return hints;
    }
}
