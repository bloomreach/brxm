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

package org.onehippo.cms.channelmanager.content.document;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.cms.channelmanager.content.document.util.HintsUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms.channelmanager.content.document.util.ContentWorkflowUtils.getDocumentWorkflow;
import static org.onehippo.cms.channelmanager.content.document.util.DocumentHandleUtils.getHandle;
import static org.onehippo.cms.channelmanager.content.document.util.EditingUtils.isHintActionFalse;

public final class JcrSaveDraftDocumentService extends AbstractSaveDraftDocumentService {

    private static final Logger log = LoggerFactory.getLogger(JcrSaveDraftDocumentService.class);

    @Override
    protected void updateDraft(final String identifier, final UserContext userContext, final Document document) {
        final Session session = userContext.getSession();
        final DocumentType docType = getDocumentType(identifier, userContext);
        try {
            final Node handle = session.getNodeByIdentifier(identifier);
            final Node draftNode = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)
                    .orElseThrow(() -> new NotFoundException(new ErrorInfo(ErrorInfo.Reason.DOES_NOT_EXIST)));
            FieldTypeUtils.writeFieldValues(document.getFields(), docType.getFields(), draftNode);
        } catch (RepositoryException e) {
            log.warn("Failed to update draft variant of handle: { identifier :  {} }", identifier, e);
            throw new InternalServerErrorException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR));
        }

        try {
            session.save();
        } catch (final RepositoryException e) {
            log.warn("Failed to save changes to draft node of document {}", identifier, e);
            throw new InternalServerErrorException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR));
        }

        final DocumentWorkflow workflow = getWorkflow(identifier, userContext);
        refreshInternalWorkflowSession(workflow);
        try {
            workflow.saveDraft();
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to save draft for user '{}'.", session.getUserID(), e);
            throw new InternalServerErrorException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR));
        }
    }

    @Override
    protected boolean isDocumentRetainable(final String identifier, final UserContext userContext) {
        final Session session = userContext.getSession();
        try {
            final Node handle = session.getNodeByIdentifier(identifier);
            final DocumentHandle documentHandle = new DocumentHandle(handle);
            documentHandle.initialize();
            return documentHandle.isRetainable();
        } catch (WorkflowException | RepositoryException e) {
            log.warn("Failed to determine if document is retainable");
            throw new InternalServerErrorException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR));
        }
    }

    @Override
    protected boolean isDocumentDirty(final String identifier, final UserContext userContext, final Document document) {
        return DocumentsServiceImpl.isDocumentDirty(getHandle(identifier, userContext.getSession()),
                getDocumentType(identifier, userContext), document);
    }

    @Override
    ErrorInfo withDisplayName(final ErrorInfo errorInfo, final String identifier, final UserContext context) {
        return ErrorInfo.withDisplayName(new ErrorInfo(ErrorInfo.Reason.CREATE_WITH_UNSUPPORTED_VALIDATOR),
                getHandle(identifier, context.getSession()));
    }

    @Override
    protected ErrorInfo withDocumentInfo(final ErrorInfo errorInfo, final String identifier, final UserContext userContext) {
        return DocumentsServiceImpl.withDocumentInfo(errorInfo, getHandle(identifier, userContext.getSession()));
    }


    @Override
    Optional<String> getVariantNodeType(final String identifier, final UserContext userContext) {
        return DocumentsServiceImpl.getVariantNodeType(getHandle(identifier, userContext.getSession()));
    }

    @Override
    DocumentType getDocumentTypeByNodeTypeIdentifier(final UserContext context, final String nodeTypeIdentifier) {
        return DocumentsServiceImpl.getDocumentTypeByNodeTypeIdentifier(context, nodeTypeIdentifier);
    }

    @Override
    Optional<ErrorInfo> determineEditingFailure(final Map<String, Serializable> hints, final UserContext userContext) {
        if (isHintActionFalse(hints, EDIT_DRAFT)) {
            return Optional.of(new ErrorInfo(ErrorInfo.Reason.NOT_EDITABLE, null));
        }
        return Optional.empty();
    }

    @Override
    protected Map<String, Serializable> getHints(final String identifier, final UserContext userContext) {
        final DocumentWorkflow workflow = getWorkflow(identifier, userContext);
        refreshInternalWorkflowSession(workflow);
        return HintsUtils.getHints(workflow, null);
    }

    private DocumentWorkflow getWorkflow(final String identifier, final UserContext userContext) {
        final Session session = userContext.getSession();
        final Node handle = getHandle(identifier, session);
        return getDocumentWorkflow(handle);
    }

    private void refreshInternalWorkflowSession(DocumentWorkflow documentWorkflow){
        try {
            final WorkflowContext workflowContext = documentWorkflow.getWorkflowContext();
                workflowContext.getInternalWorkflowSession().refresh(false);
        } catch (RepositoryException e) {
            log.warn("Could not refresh interal workflow session");
            throw new ForbiddenException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR));
        }
    }


    @Override
    Document getDraft(final String identifier, final UserContext userContext) {
        final Node handle = getHandle(identifier, userContext.getSession());
        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);
        refreshInternalWorkflowSession(documentWorkflow);
        final Node draftNode = EditingUtils.getDraftNode(documentWorkflow, userContext.getSession())
                .orElseThrow(() -> new ForbiddenException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR)));
        final DocumentType documentType = getDocumentType(identifier, userContext);
        final Document document = DocumentsServiceImpl.assembleDocument(identifier, handle, draftNode, documentType);
        FieldTypeUtils.readFieldValues(draftNode, documentType.getFields(), document.getFields());

        return document;
    }
}
