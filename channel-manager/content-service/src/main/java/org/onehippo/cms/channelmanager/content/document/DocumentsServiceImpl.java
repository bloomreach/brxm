/*
 * Copyright 2016-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.platform.api.ChannelService;
import org.hippoecm.hst.platform.api.PlatformServices;
import org.hippoecm.hst.platform.api.experiencepages.XPageLayout;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.standardworkflow.JcrTemplateNode;
import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.hippoecm.repository.util.WorkflowUtils.Variant;
import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.model.DocumentInfo;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.model.NewDocumentInfo;
import org.onehippo.cms.channelmanager.content.document.model.PublicationState;
import org.onehippo.cms.channelmanager.content.document.util.BranchingService;
import org.onehippo.cms.channelmanager.content.document.util.DocumentLocaleUtils;
import org.onehippo.cms.channelmanager.content.document.util.DocumentNameUtils;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.document.util.FolderUtils;
import org.onehippo.cms.channelmanager.content.document.util.HintsInspector;
import org.onehippo.cms.channelmanager.content.document.util.HintsUtils;
import org.onehippo.cms.channelmanager.content.document.util.PublicationStateUtils;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;
import org.onehippo.cms.channelmanager.content.documenttype.validation.ValidationUtils;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ConflictException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.branch.BranchHandle;
import org.onehippo.repository.documentworkflow.BranchHandleImpl;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.util.JcrUtils.getNodePathQuietly;
import static org.onehippo.cms.channelmanager.content.document.util.ContentWorkflowUtils.getDocumentWorkflow;
import static org.onehippo.cms.channelmanager.content.document.util.ContentWorkflowUtils.getEditableWorkflow;
import static org.onehippo.cms.channelmanager.content.document.util.ContentWorkflowUtils.getFolderWorkflow;
import static org.onehippo.cms.channelmanager.content.document.util.DocumentHandleUtils.getHandle;
import static org.onehippo.cms.channelmanager.content.document.util.EditingUtils.HINT_PUBLISH;
import static org.onehippo.cms.channelmanager.content.document.util.EditingUtils.HINT_REQUEST_PUBLICATION;
import static org.onehippo.cms.channelmanager.content.document.util.EditingUtils.isHintActionTrue;
import static org.onehippo.cms.channelmanager.content.error.ErrorInfo.withDisplayName;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

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
    private BranchingService branchingService;
    private CompoundService compoundService;

    public void setHintsInspector(final HintsInspector hintsInspector) {
        this.hintsInspector = hintsInspector;
    }

    public void setBranchingService(final BranchingService branchingService) {
        this.branchingService = branchingService;
    }

    public void setCompoundService(final CompoundService compoundService) {
        this.compoundService = compoundService;
    }

    @Override
    public Document getDocument(final String uuid, final String branchId, final UserContext userContext) {
        final Node handle = getHandle(uuid, userContext.getSession());
        final DocumentType docType = getDocumentType(handle, userContext);

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

            final Node draft = branchHandle.getDraft();
            if (draft != null) {
                return createDocument(uuid, handle, docType, draft);
            }

            // there is no unpublished for branch 'branchId' and no published master, in case branchId is not for
            // 'master', check whether there is an unpublished master.
            if (!MASTER_BRANCH_ID.equals(branchId)) {
                final Node unpublishedMaster = new BranchHandleImpl(MASTER_BRANCH_ID, handle).getUnpublished();
                if (unpublishedMaster != null) {
                    return createDocument(uuid, handle, docType, unpublishedMaster);
                }
            }

            throw new NotFoundException(new ErrorInfo(Reason.DOES_NOT_EXIST));

        } catch (WorkflowException e) {
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR, "error", e.getMessage()));
        }
    }

    @Override
    public Document branchDocument(final String uuid, final String branchId, final UserContext userContext) {
        final Node handle = getHandle(uuid, userContext.getSession());
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        final Map<String, Serializable> hints = HintsUtils.getHints(workflow, branchId);
        final Set<String> existingBranches = getExistingBranches(workflow);

        hintsInspector.canBranchDocument(branchId, hints, existingBranches)
                .ifPresent(errorInfo -> {
                    throw new ForbiddenException(withDocumentInfo(errorInfo, handle));
                });

        final DocumentType docType = getDocumentType(handle, userContext);
        if (docType.isReadOnlyDueToUnsupportedValidator()) {
            throw new ForbiddenException(
                    withDisplayName(new ErrorInfo(Reason.CREATE_WITH_UNSUPPORTED_VALIDATOR), handle)
            );
        }

        final Node draftNode = branchingService.branch(workflow, branchId, userContext.getSession());
        final Document document = assembleDocument(uuid, handle, draftNode, docType);
        FieldTypeUtils.readFieldValues(draftNode, docType.getFields(), document.getFields());

        document.getInfo().setDirty(isDocumentDirty(handle, docType, document));
        document.getInfo().setCanPublish(isHintActionTrue(hints, HINT_PUBLISH));
        document.getInfo().setCanRequestPublication(isHintActionTrue(hints, HINT_REQUEST_PUBLICATION));

        return document;
    }

    static boolean isDocumentDirty(final Node handle, final DocumentType docType, final Document document) {
        return WorkflowUtils.getDocumentVariantNode(handle, Variant.UNPUBLISHED)
                .map(unpublished -> {
                    final Map<String, List<FieldValue>> unpublishedFields = new HashMap<>();
                    FieldTypeUtils.readFieldValues(unpublished, docType.getFields(), unpublishedFields);
                    return !document.getFields().equals(unpublishedFields);
                })
                .orElse(false);
    }

    private Set<String> getExistingBranches(final DocumentWorkflow workflow) {
        try {
            return workflow.listBranches();
        } catch (WorkflowException e) {
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }
    }

    @Override
    public Document obtainEditableDocument(final String uuid, final String branchId, final UserContext userContext) {

        SaveDraftDocumentService jcrSaveDraftDocumentService = getJcrSaveDraftDocumentService(uuid, branchId
                , userContext);
        if (jcrSaveDraftDocumentService.canEditDraft()) {
            return jcrSaveDraftDocumentService.editDraft();
        }

        final Session session = userContext.getSession();
        final Node handle = getHandle(uuid, session);
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        Map<String, Serializable> hints = HintsUtils.getHints(workflow, branchId);
        if (!hintsInspector.canObtainEditableDocument(branchId, hints)) {
            throw hintsInspector
                    .determineEditingFailure(branchId, hints, session)
                    .map(errorInfo -> withDocumentInfo(errorInfo, handle))
                    .map(ForbiddenException::new)
                    .orElseGet(() -> new ForbiddenException(new ErrorInfo(Reason.SERVER_ERROR)));
        }

        final DocumentType docType = getDocumentType(handle, userContext);
        if (docType.isReadOnlyDueToUnsupportedValidator()) {
            throw new ForbiddenException(
                    withDisplayName(new ErrorInfo(Reason.CREATE_WITH_UNSUPPORTED_VALIDATOR), handle)
            );
        }

        final Node draftNode = EditingUtils.getEditableDocumentNode(workflow, branchId, session)
                .orElseThrow(() -> new ForbiddenException(new ErrorInfo(Reason.SERVER_ERROR)));
        final Document document = assembleDocument(uuid, handle, draftNode, docType);
        FieldTypeUtils.readFieldValues(draftNode, docType.getFields(), document.getFields());

        // For master documents we must use the hints that were retrieved before the editable instance was obtained
        // from the workflow, see the class level javadoc.
        if (!branchId.equals(MASTER_BRANCH_ID)) {
            hints = HintsUtils.getHints(workflow, branchId);
        }

        document.getInfo().setDirty(isDocumentDirty(handle, docType, document));
        document.getInfo().setCanPublish(isHintActionTrue(hints, HINT_PUBLISH));
        document.getInfo().setCanRequestPublication(isHintActionTrue(hints, HINT_REQUEST_PUBLICATION));
        jcrSaveDraftDocumentService.addDocumentInfo(document);

        return document;
    }

    SaveDraftDocumentService getJcrSaveDraftDocumentService(final String uuid, final String branchId
            , final UserContext userContext) {
        return new JcrSaveDraftDocumentService(uuid, branchId, userContext);
    }

    @Override
    public Document updateEditableDocument(final String uuid, final Document document, final UserContext userContext) {
        final String branchId = document.getBranchId();
        SaveDraftDocumentService jcrSaveDraftDocumentService =
                getJcrSaveDraftDocumentService(uuid, branchId, userContext);
        if (jcrSaveDraftDocumentService.shouldSaveDraft(document)) {
            return jcrSaveDraftDocumentService.saveDraft(document);
        }
        final Session session = userContext.getSession();
        final Node handle = getHandle(uuid, session);
        final EditableWorkflow workflow = getEditableWorkflow(handle);
        final Node draftNode = WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)
                .orElseThrow(() -> new NotFoundException(new ErrorInfo(Reason.DOES_NOT_EXIST)));

        final Map<String, Serializable> hints = HintsUtils.getHints(workflow, document.getBranchId());
        if (!hintsInspector.canUpdateDocument(document.getBranchId(), hints)) {
            throw new ForbiddenException(errorInfoFromHintsOrNoHolder(document.getBranchId(), hints,
                    session));
        }

        final DocumentType docType = getDocumentType(handle, userContext);
        if (docType.isReadOnlyDueToUnsupportedValidator()) {
            throw new ForbiddenException(new ErrorInfo(Reason.SAVE_WITH_UNSUPPORTED_VALIDATOR));
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

        if (!ValidationUtils.validateDocument(document, docType, draftNode, userContext)) {
            throw new BadRequestException(document);
        }

        try {
            workflow.commitEditableInstance();
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            throw new InternalServerErrorException(
                    errorInfoFromHintsOrNoHolder(branchId, HintsUtils.getHints(workflow, branchId),
                            session));
        }

        // Get the workflow hints before obtaining an editable instance again, see the class level javadoc.
        final DocumentWorkflow newWorkflow = getDocumentWorkflow(handle);
        final Map<String, Serializable> newHints = HintsUtils.getHints(newWorkflow, branchId);

        final Node newDraftNode = EditingUtils.getEditableDocumentNode(workflow, branchId, session).orElseThrow(
                () -> new ForbiddenException(new ErrorInfo(Reason.SERVER_ERROR)));

        setDocumentPublicationState(document.getInfo(), newDraftNode);

        FieldTypeUtils.readFieldValues(newDraftNode, docType.getFields(), document.getFields());

        document.getInfo().setDirty(false);
        document.getInfo().setCanPublish(isHintActionTrue(newHints, HINT_PUBLISH));
        document.getInfo().setCanRequestPublication(isHintActionTrue(newHints, HINT_REQUEST_PUBLICATION));

        return document;
    }

    @Override
    public List<FieldValue> updateEditableField(final String uuid, final String branchId, final FieldPath fieldPath,
                                                final List<FieldValue> fieldValues, final UserContext userContext) {

        final Session session = userContext.getSession();
        final Node handle = getHandle(uuid, session);
        final EditableWorkflow workflow = getEditableWorkflow(handle);
        final Node draftNode = WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)
                .orElseThrow(() -> new NotFoundException(new ErrorInfo(Reason.DOES_NOT_EXIST)));

        final Map<String, Serializable> hints = HintsUtils.getHints(workflow, branchId);
        if (!hintsInspector.canUpdateDocument(branchId, hints)) {
            throw new ForbiddenException(errorInfoFromHintsOrNoHolder(branchId, hints, session));
        }

        final DocumentType docType = getDocumentType(handle, userContext);
        if (docType.isReadOnlyDueToUnsupportedValidator()) {
            throw new ForbiddenException(new ErrorInfo(Reason.SAVE_WITH_UNSUPPORTED_VALIDATOR));
        }

        // Write field value to draft node
        final CompoundContext documentContext = new CompoundContext(draftNode, draftNode, userContext.getLocale(),
                userContext.getTimeZone());
        if (FieldTypeUtils.writeFieldValue(fieldPath, fieldValues, docType.getFields(), documentContext)) {
            try {
                session.save();
            } catch (final RepositoryException e) {
                log.warn("Failed to save changes to field '{}' in draft node of document {}", fieldPath, uuid, e);
                throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
            }
        } else {
            throw new BadRequestException(new ErrorInfo(Reason.INVALID_DATA));
        }

        return fieldValues;
    }

    @Override
    public void discardEditableDocument(final String uuid, final String branchId, final UserContext userContext) {
        final Node handle = getHandle(uuid, userContext.getSession());
        final EditableWorkflow workflow = getEditableWorkflow(handle);

        final Map<String, Serializable> hints = HintsUtils.getHints(workflow, branchId);
        if (!hintsInspector.canDisposeEditableDocument(branchId, hints)) {
            throw new ForbiddenException(new ErrorInfo(Reason.ALREADY_DELETED));
        }

        try {
            workflow.disposeEditableInstance();
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to dispose of editable instance", e);
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }
    }

    @Override
    public Document createDocument(final NewDocumentInfo newDocumentInfo, final UserContext userContext) {
        final String name = checkNotBlank("name", newDocumentInfo.getName());
        final String slug = checkNotBlank("slug", newDocumentInfo.getSlug());
        final String documentTemplateQuery = checkNotBlank("documentTemplateQuery",
                newDocumentInfo.getDocumentTemplateQuery());
        final String folderTemplateQuery = newDocumentInfo.getFolderTemplateQuery();
        final String documentTypeId = checkNotBlank("documentTypeId", newDocumentInfo.getDocumentTypeId());
        final String rootPath = checkNotBlank("rootPath", newDocumentInfo.getRootPath());
        final String defaultPath = newDocumentInfo.getDefaultPath();

        final Session session = userContext.getSession();
        final Node rootFolder = FolderUtils.getFolder(rootPath, session);
        final Node folder = StringUtils.isEmpty(defaultPath)
                ? rootFolder
                : FolderUtils.getOrCreateFolder(rootFolder, defaultPath, session, folderTemplateQuery);
        final String folderLocale = FolderUtils.getLocale(folder);

        final String encodedName = DocumentNameUtils.encodeDisplayName(name, folderLocale);
        if (FolderUtils.nodeWithDisplayNameExists(folder, encodedName)) {
            throw new ConflictException(new ErrorInfo(Reason.NAME_ALREADY_EXISTS));
        }

        final String encodedSlug = DocumentNameUtils.encodeUrlName(slug, folderLocale);
        if (FolderUtils.nodeExists(folder, encodedSlug)) {
            throw new ConflictException(new ErrorInfo(Reason.SLUG_ALREADY_EXISTS));
        }

        final DocumentType documentType = getDocumentTypeByNodeTypeIdentifier(userContext, documentTypeId);
        if (documentType.isReadOnlyDueToUnsupportedValidator()) {
            throw new ForbiddenException(new ErrorInfo(Reason.CREATE_WITH_UNSUPPORTED_VALIDATOR));
        }

        final FolderWorkflow folderWorkflow = getFolderWorkflow(folder);

        try {
            final JcrTemplateNode xPageLayoutNode = getXPageLayoutTemplateNode(newDocumentInfo.getLayout(), folder);
            final String documentPath = folderWorkflow.add(documentTemplateQuery, documentTypeId, encodedSlug, xPageLayoutNode);
            log.debug("Created document {}", documentPath);

            final Node document = session.getNode(documentPath);
            final Node handle = document.getParent();

            if (!encodedSlug.equals(encodedName)) {
                DocumentNameUtils.setDisplayName(handle, encodedName);
            }

            session.save();
            return getCreatedDocument(handle, documentType);
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to add document '{}' of type '{}' to folder '{}' using template query '{}'",
                    encodedSlug, documentTypeId, newDocumentInfo.getRootPath(), documentTemplateQuery, e);
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }
    }

    private JcrTemplateNode getXPageLayoutTemplateNode(final String layoutId, final Node folder) throws RepositoryException {
        if (StringUtils.isEmpty(layoutId)) {
            return null;
        }

        final String channelId = JcrUtils.getStringProperty(folder, HippoStdNodeType.HIPPOSTD_CHANNEL_ID, null);
        if (StringUtils.isEmpty(channelId)) {
            log.warn("Failed to retrieve XPageLayout[{}]. Could not read property {} on node {}",
                    layoutId, HippoStdNodeType.HIPPOSTD_CHANNEL_ID, JcrUtils.getNodePathQuietly(folder));
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }

        final ChannelService channelService = HippoServiceRegistry.getService(PlatformServices.class).getChannelService();
        final Map<String, XPageLayout> xPageLayouts = channelService.getXPageLayouts(channelId);
        if (!xPageLayouts.containsKey(layoutId)) {
            log.warn("Failed to retrieve XPageLayout[{}]. Available id's are {}",
                    layoutId, String.join(",", xPageLayouts.keySet()));
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }

        return xPageLayouts.get(layoutId).getJcrTemplateNode();
    }

    private static Document getCreatedDocument(final Node handle, final DocumentType documentType) throws RepositoryException {
        final Node draftNode = WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)
                .orElseThrow(() -> new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR)));
        return createDocument(handle.getIdentifier(), handle, documentType, draftNode);
    }

    private static Document createDocument(final String uuid, final Node handle, final DocumentType docType, final Node unpublished) {

        final Document document = assembleDocument(uuid, handle, unpublished, docType);
        document.getInfo().setCanKeepDraft(canKeepDraft(handle));
        FieldTypeUtils.readFieldValues(unpublished, docType.getFields(), document.getFields());
        return document;
    }

    private static boolean canKeepDraft(final Node handle){
        try {
            final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);
            final Map<String, Serializable> hints = documentWorkflow.hints();
            return Boolean.TRUE.equals(hints.get(AbstractSaveDraftDocumentService.SAVE_DRAFT));
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to determine if save draft is allowed for document: { path : {}}"
                    , JcrUtils.getNodePathQuietly(handle));
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }
    }

    @Override
    public Document updateDocumentNames(final String uuid, final String branchId, final Document document, final UserContext userContext) {
        final String displayName = checkNotBlank("displayName", document.getDisplayName());
        final String urlName = checkNotBlank("urlName", document.getUrlName());

        final Node handle = getHandle(uuid, userContext.getSession());
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
            final Map<String, Serializable> hints = HintsUtils.getHints(getEditableWorkflow(handle), branchId);
            log.info("Changing URL name of '{}' to '{}'", handlePath, newUrlName);
            DocumentNameUtils.setUrlName(handle, newUrlName, hints);
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
    public void deleteDocument(final String uuid, final String branchId, final UserContext userContext) {
        final Node handle = getHandle(uuid, userContext.getSession());
        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);

        final Map<String, Serializable> hints = HintsUtils.getHints(documentWorkflow, branchId);
        // Try to archive the document (i.e. move to the attic) so there's still a pointer into the version history
        if (EditingUtils.canArchiveDocument(hints)) {
            archiveDocument(uuid, documentWorkflow);
            return;
        }

        // Archiving not possible: the document can be published, a request can be pending etc. Only case left to check:
        // is the document a draft that was just created? (in which case it won't have a 'preview' variant yet)
        if (EditingUtils.hasPreview(hints)) {
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

    @Override
    public Map<String, List<FieldValue>> addCompoundField(final String uuid,
                                     final String branchId,
                                     final FieldPath fieldPath,
                                     final UserContext userContext) {
        if (fieldPath.isEmpty()) {
            log.warn("Can not add compound field if fieldPath is empty");
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }

        final Node handle = getHandle(uuid, userContext.getSession());
        final BranchHandle branchHandle;
        try {
            branchHandle = new BranchHandleImpl(branchId, handle);
        } catch (WorkflowException e) {
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR, "error", e.getMessage()));
        }

        final Node draft = branchHandle.getDraft();
        if (draft == null) {
            throw new NotFoundException(new ErrorInfo(Reason.DOES_NOT_EXIST));
        }

        final String documentPath;
        try {
            documentPath = draft.getPath();
        } catch (RepositoryException e) {
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR, "error", e.getMessage()));
        }

        final DocumentType documentType = getDocumentType(handle, userContext);
        compoundService.addCompoundField(documentPath, fieldPath, documentType.getFields());

        final Document document = assembleDocument(uuid, handle, draft, documentType);
        FieldTypeUtils.readFieldValues(draft, documentType.getFields(), document.getFields());

        return findFieldValues(fieldPath, document.getFields());
    }

    private static Map<String, List<FieldValue>> findFieldValues(final FieldPath path,
                                                                 final Map<String, List<FieldValue>> fields) {
        final String fieldName = path.getFirstSegmentName();
        final List<FieldValue> fieldValues = fields.get(fieldName);

        if (fieldValues == null) {
            throw new NotFoundException(new ErrorInfo(Reason.DOES_NOT_EXIST, "field", fieldName));
        }

        final int fieldIndex = path.getFirstSegmentIndex();
        final Map<String, List<FieldValue>> firstSegmentFields = fieldValues.get(fieldIndex - 1).getFields();
        final FieldPath remaining = path.getRemainingSegments();
        return remaining.isEmpty()
                ? firstSegmentFields
                : findFieldValues(remaining, firstSegmentFields);
    }

    private static void archiveDocument(final String uuid, final DocumentWorkflow documentWorkflow) {
        try {
            log.info("Archiving document '{}'", uuid);
            documentWorkflow.delete();
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to archive document '{}'", uuid, e);
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }
    }

    private static void eraseDocument(final String uuid, final FolderWorkflow folderWorkflow, final Item handle) {
        try {
            log.info("Erasing document '{}'", uuid);
            folderWorkflow.delete(handle.getName());
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to erase document '{}'", uuid, e);
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }
    }

    private static String checkNotBlank(final String propName, final String propValue) {
        if (StringUtils.isBlank(propValue)) {
            final String errorMessage = "Property '" + propName + "' cannot be blank or null";
            log.warn(errorMessage);
            throw new BadRequestException(new ErrorInfo(Reason.INVALID_DATA, "error", errorMessage));
        }
        return propValue;
    }

    static DocumentType getDocumentType(final Node handle, final UserContext userContext) {
        final String id = getVariantNodeType(handle).orElseThrow(
                () -> new InternalServerErrorException(new ErrorInfo(Reason.DOES_NOT_EXIST)));

        try {
            return getDocumentTypeByNodeTypeIdentifier(userContext, id);
        } catch (final ErrorWithPayloadException e) {
            log.warn("Failed to retrieve type of document '{}'", getNodePathQuietly(handle), e);
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }
    }

    static DocumentType getDocumentTypeByNodeTypeIdentifier(final UserContext userContext, final String id) {
        return DocumentTypesService.get().getDocumentType(id, userContext);
    }

    static Optional<String> getVariantNodeType(final Node handle) {
        return DocumentUtils.getVariantNodeType(handle);
    }

    static Document assembleDocument(final String uuid, final Node handle, final Node variant, final DocumentType docType) {
        final Document document = new Document();
        document.setId(uuid);

        final DocumentInfo documentInfo = new DocumentInfo();
        documentInfo.setTypeId(docType.getId());
        setDocumentPublicationState(documentInfo, variant);
        setDocumentLocale(documentInfo, variant);
        document.setInfo(documentInfo);

        DocumentUtils.getDisplayName(handle).ifPresent(document::setDisplayName);
        document.setUrlName(JcrUtils.getNodeNameQuietly(handle));

        document.setRepositoryPath(JcrUtils.getNodePathQuietly(handle));

        try {
            final DocumentVariant documentVariant = new DocumentVariant(variant);
            document.setState(documentVariant.getState());
            document.setBranchId(documentVariant.getBranchId());
            document.setVariantId(variant.getIdentifier());
        } catch (RepositoryException e) {
            log.warn("Failed to set state and branchId for document with id '{}'", uuid, e);
        }
        return document;
    }

    private static void setDocumentPublicationState(final DocumentInfo documentInfo, final Node variant) {
        final PublicationState state = PublicationStateUtils.getPublicationStateFromVariant(variant);
        documentInfo.setPublicationState(state);
    }

    private static void setDocumentLocale(final DocumentInfo documentInfo, final Node variant) {
        documentInfo.setLocale(DocumentLocaleUtils.getDocumentLocale(variant));
    }

    static ErrorInfo withDocumentInfo(final ErrorInfo errorInfo, final Node handle) {
        DocumentUtils.getDisplayName(handle).ifPresent(displayName -> errorInfo.addParam("displayName", displayName));

        final PublicationState publicationState = PublicationStateUtils.getPublicationStateFromHandle(handle);
        errorInfo.addParam("publicationState", publicationState);

        return errorInfo;
    }

    private ErrorInfo errorInfoFromHintsOrNoHolder(String branchId, Map<String, Serializable> hints, Session session) {
        return hintsInspector.determineEditingFailure(branchId, hints, session)
                .orElseGet(() -> new ErrorInfo(Reason.NO_HOLDER));
    }

}
