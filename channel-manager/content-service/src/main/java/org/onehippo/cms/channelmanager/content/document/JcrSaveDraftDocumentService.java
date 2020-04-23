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
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Session;

import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.cms.channelmanager.content.document.util.HintsInspectorImpl;
import org.onehippo.cms.channelmanager.content.document.util.HintsUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static org.onehippo.cms.channelmanager.content.document.util.ContentWorkflowUtils.getDocumentWorkflow;
import static org.onehippo.cms.channelmanager.content.document.util.DocumentHandleUtils.getHandle;

public final class JcrSaveDraftDocumentService extends AbstractSaveDraftDocumentService {


    private final HintsInspectorImpl hintsInspector;

    public JcrSaveDraftDocumentService() {
        hintsInspector = new HintsInspectorImpl();
    }

    @Override
    protected boolean isDocumentDirty(final String identifier, final UserContext userContext, final Document document) {
        return DocumentsServiceImpl.isDocumentDirty(getHandle(identifier, userContext.getSession()), getDocumentType(identifier, userContext), document);
    }

    @Override
    ErrorInfo withDisplayName(final ErrorInfo errorInfo, final String identifier, final UserContext context) {
        return ErrorInfo.withDisplayName(new ErrorInfo(ErrorInfo.Reason.CREATE_WITH_UNSUPPORTED_VALIDATOR), getHandle(identifier, context.getSession()));
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
        return hintsInspector.determineEditingFailure("master", hints, userContext.getSession());
    }

    @Override
    protected Map<String, Serializable> getHints(final String identifier, final UserContext userContext) {
        return HintsUtils.getHints(getWorkflow(identifier, userContext), "master");
    }

    private DocumentWorkflow getWorkflow(final String identifier, final UserContext userContext) {
        final Session session = userContext.getSession();
        final Node handle = getHandle(identifier, session);
        return getDocumentWorkflow(handle);
    }

    @Override
    Document getDraft(final String identifier, final UserContext userContext) {
        final Node handle = getHandle(identifier, userContext.getSession());
        final Node draftNode = EditingUtils.getDraftNode(getDocumentWorkflow(handle), userContext.getSession())
                .orElseThrow(() -> new ForbiddenException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR)));
        final DocumentType documentType = getDocumentType(identifier, userContext);
        final Document document = DocumentsServiceImpl.assembleDocument(identifier, handle, draftNode, documentType);
        FieldTypeUtils.readFieldValues(draftNode, documentType.getFields(), document.getFields());
        addDocumentInfo(identifier, userContext, document);

        return document;
    }
}
