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

public abstract class AbstractSaveDraftDocumentService implements SaveDraftDocumentService {

    private static final Logger log = LoggerFactory.getLogger(AbstractSaveDraftDocumentService.class);
    public static final String SAVE_DRAFT = "saveDraft";

    @Override
    public final Document editDraft(final String identifier, final UserContext userContext) {
        validateEditDraft(identifier, userContext);
        final Document draft = getDraft(identifier, userContext);
        addDocumentInfo(identifier, userContext, draft);
        return draft;
    }

    @Override
    public Document saveDraft(final String identifier, final UserContext userContext, final Document document) {
        validateSaveDraft(identifier, userContext);
        updateDraft(identifier, userContext, document);
        final Document updatedDocument = editDraft(identifier, userContext);
        addDocumentInfo(identifier, userContext, updatedDocument);
        return updatedDocument;
    }

    protected abstract void updateDraft(final String identifier, final UserContext userContext, final Document document);

    @Override
    public final boolean canEditDraft(final String identifier, final UserContext userContext) {
        return canEditDraft(getHints(identifier, userContext));
    }

    @Override
    public boolean canSaveDraft(final String identifier, final UserContext userContext, final Document document) {
        return false;
    }

    public final DocumentInfo addDocumentInfo(final String identifier, final UserContext userContext, final Document document) {
        final Map<String, Serializable> hints = getHints(identifier, userContext);
        final DocumentInfo documentInfo = document.getInfo();
        documentInfo.setDirty(isDocumentDirty(identifier, userContext, document));
        documentInfo.setRetainable(isDocumentRetainable(identifier, userContext));
        documentInfo.setCanKeepDraft(isHintActionTrue(hints, SAVE_DRAFT));
        return documentInfo;
    }

    protected abstract boolean isDocumentRetainable(final String identifier, final UserContext userContext);

    public final DocumentType getDocumentType(final String identifier, final UserContext userContext) {
        final String id = getVariantNodeType(identifier, userContext).orElseThrow(
                () -> new InternalServerErrorException(new ErrorInfo(ErrorInfo.Reason.DOES_NOT_EXIST)));

        try {
            return getDocumentTypeByNodeTypeIdentifier(userContext, id);
        } catch (ErrorWithPayloadException e) {
            log.warn("Failed to retrieve type of document '{identifier: {} }'", identifier, e);
            throw new InternalServerErrorException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR));
        }
    }

    public final boolean canEditDraft(final Map<String, Serializable> hints) {
        return isHintActionTrue(hints, "editDraft");
    }

    public final boolean canSaveDraft(final Map<String, Serializable> hints) {
        return isHintActionTrue(hints, SAVE_DRAFT);
    }

    @Override
    public boolean shouldSaveDraft(final Document document) {
        return document.getInfo().isRetainable();
    }

    private boolean isHintActionTrue(final Map<String, Serializable> hints, final String key) {
        return EditingUtils.isHintActionTrue(hints == null ? Collections.emptyMap() : hints, key);
    }

    abstract boolean isDocumentDirty(String identifier, UserContext userContext, Document document);

    abstract ErrorInfo withDisplayName(ErrorInfo errorInfo, String identifier, UserContext context);

    abstract ErrorInfo withDocumentInfo(ErrorInfo errorInfo, String identifier, UserContext userContext);

    abstract Optional<String> getVariantNodeType(String identifier, UserContext userContext);

    abstract DocumentType getDocumentTypeByNodeTypeIdentifier(UserContext context, String nodeTypeIdentifier);

    abstract Optional<ErrorInfo> determineEditingFailure(Map<String, Serializable> hints, UserContext userContext);

    abstract Map<String, Serializable> getHints(String identifier, UserContext userContext);

    private void validateEditDraft(final String identifier, final UserContext userContext) {
        final Map<String, Serializable> hints = getHints(identifier, userContext);
        if (!canEditDraft(hints)) {
            throwException(identifier, userContext, hints);
        }
        validateDocType(identifier, userContext);
    }

    private void throwException(final String identifier, final UserContext userContext
            , final Map<String, Serializable> hints) {
        throw determineEditingFailure(hints, userContext)
                .map(errorInfo -> withDocumentInfo(errorInfo, identifier, userContext))
                .map(ForbiddenException::new)
                .orElseGet(() -> new ForbiddenException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR)));
    }

    private void validateDocType(final String identifier, final UserContext userContext) {
        DocumentType docType = getDocumentType(identifier, userContext);
        if (docType.isReadOnlyDueToUnsupportedValidator()) {
            throw new ForbiddenException(
                    withDisplayName(new ErrorInfo(ErrorInfo.Reason.CREATE_WITH_UNSUPPORTED_VALIDATOR)
                            , identifier, userContext)
            );
        }
    }

    private void validateSaveDraft(final String identifier, final UserContext userContext) {
        Map<String, Serializable> hints = getHints(identifier, userContext);
        if (!canSaveDraft(hints)) {
            throwException(identifier, userContext, hints);
        }
        validateDocType(identifier, userContext);
    }

    abstract Document getDraft(String identifier, UserContext userContext);
}
