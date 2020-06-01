/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.documenttype;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldsInformation;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

class DocumentTypesServiceImpl implements DocumentTypesService {
    private static final Logger log = LoggerFactory.getLogger(DocumentTypesServiceImpl.class);

    private static final DocumentTypesServiceImpl INSTANCE = new DocumentTypesServiceImpl();
    private static final Cache<String, DocumentType> DOCUMENT_TYPES = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build();

    public static DocumentTypesServiceImpl getInstance() {
        return INSTANCE;
    }

    private DocumentTypesServiceImpl() {

    }

    @Override
    public DocumentType getDocumentType(final String id, final UserContext userContext)
            throws ErrorWithPayloadException {

        final Locale locale = userContext.getLocale();
        final String cacheKey = id + "-" + locale.toString();
        try {
            return DOCUMENT_TYPES.get(cacheKey, () -> createDocumentType(id, userContext));
        } catch (final ExecutionException ignore) {
            throw new NotFoundException();
        }
    }

    @Override
    public void invalidateCache() {
        DOCUMENT_TYPES.invalidateAll();
    }

    private DocumentType createDocumentType(final String id, final UserContext userContext) throws NotFoundException {
        final DocumentType docType = new DocumentType();
        final ContentTypeContext context = ContentTypeContext.createForDocumentType(id, userContext, docType)
                .orElseThrow(NotFoundException::new);

        final ContentType contentType = context.getContentType();
        if (!contentType.isDocumentType()) {
            log.debug("Requested type '{}' is not document type", id);
            throw new NotFoundException();
        }

        docType.setId(id);
        LocalizationUtils.determineDocumentDisplayName(id, context.getResourceBundle()).ifPresent(docType::setDisplayName);

        final FieldsInformation fieldsInformation = FieldTypeUtils.populateFields(docType.getFields(), context);
        FieldTypeUtils.checkPluginsWithoutFieldDefinition(fieldsInformation, context);
        docType.setAllFieldsIncluded(fieldsInformation.isAllFieldsIncluded());
        docType.setCanCreateAllRequiredFields(fieldsInformation.getCanCreateAllRequiredFields());
        docType.setUnsupportedFieldTypes(fieldsInformation.getUnsupportedFieldTypes());
        docType.setUnsupportedRequiredFieldTypes(fieldsInformation.getUnsupportedRequiredFieldTypes());

        contentType.getValidators()
                .forEach(docType::addValidatorName);

        return docType;
    }
}
