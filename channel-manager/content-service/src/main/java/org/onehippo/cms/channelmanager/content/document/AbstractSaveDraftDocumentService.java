/*
 * Copyright 2020-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.model.DocumentInfo;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines the business logic related to the "Keep draft" functionality. The class intends to be persistence layer
 * agnostic to unit test without the need for mocks or backing repository.
 * The class is not thread safe, so for each request a new instance should be created.
 */
public abstract class AbstractSaveDraftDocumentService implements SaveDraftDocumentService {

    private static final Logger log = LoggerFactory.getLogger(AbstractSaveDraftDocumentService.class);

    static final String EDIT_DRAFT = "editDraft";
    static final String SAVE_DRAFT = "saveDraft";

    private final String identifier;
    private final String branchId;

    public String getIdentifier() {
        return identifier;
    }

    public UserContext getUserContext() {
        return userContext;
    }

    public String getBranchId() {
        return branchId;
    }

    private final UserContext userContext;


    public AbstractSaveDraftDocumentService(final String identifier, final String branchId
            , final UserContext userContext) {
        this.identifier = identifier;
        this.branchId = branchId;
        this.userContext = userContext;
    }

    public final Document editDraft() {
        validateEditDraft();
        final Document draft = getDraft();
        addDocumentInfo(draft);
        return draft;
    }

    public Document saveDraft(final Document document) {
        validateSaveDraft();
        updateDraft(document);
        return document;
    }

    protected abstract void updateDraft(final Document document);

    public final DocumentInfo addDocumentInfo(final Document document) {
        final DocumentInfo documentInfo = document.getInfo();
        documentInfo.setDirty(isDocumentDirty(document));
        documentInfo.setRetainable(isDocumentRetainable());
        documentInfo.setCanKeepDraft(isHintActionTrue(SAVE_DRAFT));
        return documentInfo;
    }

    protected abstract boolean isDocumentRetainable();

    public final DocumentType getDocumentType() {
        final Optional<String> variantNodeType = getVariantNodeType();
        if (variantNodeType.isPresent()) {
            try {
                return getDocumentTypeByNodeTypeIdentifier(variantNodeType.get());
            } catch (ErrorWithPayloadException e) {
                log.error("Failed to retrieve type of document '{identifier: {} }'", identifier, e);
                throw new InternalServerErrorException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR));
            }
        }
        log.error("Could not determine document type for handle : { identifier : {} }", getIdentifier());
        throw new InternalServerErrorException(new ErrorInfo(ErrorInfo.Reason.DOES_NOT_EXIST));

    }

    public final boolean canEditDraft() {
        return isHintActionTrue(EDIT_DRAFT);
    }

    public final boolean canSaveDraft() {
        return isHintActionTrue(SAVE_DRAFT);
    }

    @Override
    public boolean shouldSaveDraft(final Document document) {
        return document.getInfo().isRetainable();
    }

    private boolean isHintActionTrue(final String key) {
        Map<String, Serializable> hints = getHints();
        return EditingUtils.isHintActionTrue(hints == null ? Collections.emptyMap() : hints, key);
    }

    abstract boolean isDocumentDirty(Document document);

    abstract ErrorInfo withDisplayName(ErrorInfo errorInfo);

    abstract ErrorInfo withDocumentInfo(ErrorInfo errorInfo);

    abstract Optional<String> getVariantNodeType();

    abstract DocumentType getDocumentTypeByNodeTypeIdentifier(String nodeTypeIdentifier);

    abstract Optional<ErrorInfo> determineEditingFailure();

    abstract Map<String, Serializable> getHints();

    private void validateEditDraft() {
        if (!canEditDraft()) {
            throwException();
        }
        validateDocType();
    }

    private void throwException() {
        throw determineEditingFailure()
                .map(this::withDocumentInfo)
                .map(ForbiddenException::new)
                .orElseGet(() -> new ForbiddenException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR)));
    }

    private void validateDocType() {
        final DocumentType docType = getDocumentType();
        if (docType.isReadOnlyDueToUnsupportedValidator()) {
            final ErrorInfo errorInfo = new ErrorInfo(ErrorInfo.Reason.CREATE_WITH_UNSUPPORTED_VALIDATOR);
            throw new ForbiddenException(withDisplayName(errorInfo));
        }
    }

    private void validateSaveDraft() {
        if (!canSaveDraft()) {
            throwException();
        }
        validateDocType();
    }

    abstract Document getDraft();
}
