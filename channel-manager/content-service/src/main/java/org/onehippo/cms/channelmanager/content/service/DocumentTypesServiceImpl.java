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

package org.onehippo.cms.channelmanager.content.service;

import java.util.List;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.exception.ContentTypeException;
import org.onehippo.cms.channelmanager.content.exception.DocumentTypeNotFoundException;
import org.onehippo.cms.channelmanager.content.model.documenttype.DocumentType;
import org.onehippo.cms.channelmanager.content.util.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.util.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.util.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.util.FieldValidators;
import org.onehippo.cms.channelmanager.content.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.util.MockResponse;
import org.onehippo.cms.channelmanager.content.util.NamespaceUtils;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentTypesServiceImpl implements DocumentTypesService {
    private static final Logger log = LoggerFactory.getLogger(DocumentTypesServiceImpl.class);
    private static final DocumentTypesServiceImpl INSTANCE = new DocumentTypesServiceImpl();

    public static DocumentTypesServiceImpl getInstance() {
        return INSTANCE;
    }

    private DocumentTypesServiceImpl() { }

    @Override
    public DocumentType getDocumentType(final Node handle, final Locale locale)
            throws DocumentTypeNotFoundException {
        try {
            final String id = DocumentUtils.getVariantNodeType(handle).orElseThrow(DocumentTypeNotFoundException::new);

            return getDocumentType(id, handle.getSession(), locale);
        } catch (RepositoryException e) {
            log.warn("Failed to retrieve document type from node '{}'", JcrUtils.getNodePathQuietly(handle), e);
        }
        throw new DocumentTypeNotFoundException();
    }

    @Override
    public DocumentType getDocumentType(final String id, final Session userSession, final Locale locale)
            throws DocumentTypeNotFoundException {
        if ("ns:testdocument".equals(id)) {
            return MockResponse.createTestDocumentType();
        }

        final ContentTypeContext context = getContentTypeContext(id, userSession, locale);

        validateDocumentType(context, id);

        final DocumentType docType = new DocumentType();

        docType.setId(id);
        LocalizationUtils.determineDocumentDisplayName(id, context.getResourceBundle()).ifPresent(docType::setDisplayName);
        populateFields(docType, context);

        return docType;
    }

    private static ContentTypeContext getContentTypeContext(final String id, final Session userSession,
                                                            final Locale locale) throws DocumentTypeNotFoundException {
        try {
            return ContentTypeContext.createDocumentTypeContext(id, userSession, locale);
        } catch (ContentTypeException e) {
            log.warn("Failed to retrieve context for content type '{}'", id, e);
            throw new DocumentTypeNotFoundException();
        }
    }

    private static void validateDocumentType(final ContentTypeContext context, final String id)
            throws DocumentTypeNotFoundException {
        if (!context.getContentType().isDocumentType()) {
            log.debug("Requested type '{}' is not document type", id);
            throw new DocumentTypeNotFoundException();
        }
    }

    private static void populateFields(final DocumentType docType, final ContentTypeContext context) {
        NamespaceUtils.retrieveFieldSorter(context.getContentTypeRoot())
                .ifPresent(sorter -> sorter.sortFields(context)
                        .stream()
                        .filter(FieldTypeUtils::isSupportedFieldType)
                        .filter(FieldTypeUtils::usesDefaultFieldPlugin)
                        .forEach(fieldContext -> addPropertyField(docType, context, fieldContext))
                );
    }

    private static void addPropertyField(final DocumentType docType,
                                         final ContentTypeContext context,
                                         final FieldTypeContext fieldContext) {
        final ContentTypeItem item = fieldContext.getContentTypeItem();
        FieldTypeUtils.createFieldType(item).ifPresent((fieldType) -> {
            final String fieldId = item.getName();

            fieldType.setId(fieldId);

            LocalizationUtils.determineFieldDisplayName(fieldId, context.getResourceBundle(), fieldContext.getEditorConfigNode())
                    .ifPresent(fieldType::setDisplayName);
            LocalizationUtils.determineFieldHint(fieldId, context.getResourceBundle(), fieldContext.getEditorConfigNode())
                    .ifPresent(fieldType::setHint);
            fieldType.setStoredAsMultiValueProperty(item.isMultiple());

            if (item.isMultiple() || item.getValidators().contains(FieldValidators.OPTIONAL)) {
                fieldType.setMultiple(true);
            }

            FieldTypeUtils.determineValidators(fieldType, docType, item.getValidators());

            docType.addField(fieldType);
        });
    }
}
