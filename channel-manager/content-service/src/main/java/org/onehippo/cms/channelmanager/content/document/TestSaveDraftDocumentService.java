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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;

public class TestSaveDraftDocumentService extends AbstractSaveDraftDocumentService {


    public static final String DISPLAY_NAME = "displayName";
    private Document document;
    private String displayName;
    private String publicationState;
    private Map<String, DocumentType> documentTypeMap = new HashMap<>();
    private Map<String, String> documentIdentifierToNodeTypeIdentifier = new HashMap<>();
    private ErrorInfo editingFailure;
    private Map<String, Serializable> hints;

    public TestSaveDraftDocumentService(String identifier, String branchId, UserContext userContext) {
        super(identifier, branchId, userContext);
    }

    public void setDraft(final Document document) {
        this.document = document;
    }

    public void associateDocumentWithNodeType(final String documentIdentifier, final String nodeTypeIdentifier) {
        this.documentIdentifierToNodeTypeIdentifier.put(documentIdentifier, nodeTypeIdentifier);
    }

    public ErrorInfo setDisplayName(ErrorInfo errorInfo, final String displayName) {
        errorInfo.addParam(DISPLAY_NAME, displayName);
        return errorInfo;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public void setPublicationState(final String publicationState) {
        this.publicationState = publicationState;
    }

    public void addDocumentType(final String identifier, final DocumentType documentType) {
        this.documentTypeMap.put(identifier, documentType);
    }


    public void setEditingFailure(final ErrorInfo editingFailure) {
        this.editingFailure = editingFailure;
    }

    public void setHints(final Map<String, Serializable> hints) {
        this.hints = hints;
    }

    @Override
    protected void updateDraft(final Document document) {
        this.document =  document;
        this.document.getInfo().setRetainable(true);

    }

    @Override
    protected boolean isDocumentRetainable() {
        return document.getInfo().isRetainable();
    }

    @Override
    protected boolean isDocumentDirty(final Document updatedDocument) {
        final Map<String, List<FieldValue>> updatedFields = updatedDocument.getFields();
        final Map<String, List<FieldValue>> fields = document.getFields();
        return !updatedFields.entrySet().stream()
                .allMatch(e -> e.getValue().equals(fields.get(e.getKey())));
    }

    @Override
    ErrorInfo withDisplayName(final ErrorInfo errorInfo) {
        errorInfo.addParam(DISPLAY_NAME, displayName);
        return errorInfo;
    }

    @Override
    protected ErrorInfo withDocumentInfo(final ErrorInfo errorInfo) {
        errorInfo.addParam(DISPLAY_NAME, displayName);
        errorInfo.addParam("publicationState", publicationState);
        return errorInfo;
    }

    @Override
    Optional<String> getVariantNodeType() {
        // Each document it's own document type for test purposes
        return Optional.ofNullable(this.documentIdentifierToNodeTypeIdentifier.get(getIdentifier()));
    }

    @Override
    DocumentType getDocumentTypeByNodeTypeIdentifier(final String nodeTypeIdentifier) {
        if (this.documentTypeMap.containsKey(nodeTypeIdentifier)) {
            return this.documentTypeMap.get(nodeTypeIdentifier);
        }
        throw new InternalServerErrorException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR));
    }

    @Override
    Optional<ErrorInfo> determineEditingFailure() {
        return Optional.ofNullable(editingFailure);
    }

    @Override
    protected Map<String, Serializable> getHints() {
        return hints;
    }

    @Override
    Document getDraft() {
        if (document == null) {
            throw new ForbiddenException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR));
        }
        return document;
    }
}
