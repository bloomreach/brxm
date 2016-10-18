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

package org.onehippo.cms.channelmanager.content.documenttype;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.util.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.MockResponse;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentTypesServiceImpl implements DocumentTypesService {
    private static final Logger log = LoggerFactory.getLogger(DocumentTypesServiceImpl.class);
    private static final DocumentTypesServiceImpl INSTANCE = new DocumentTypesServiceImpl();
    private static final int MAX_NESTING_LEVEL = 10;

    public static DocumentTypesServiceImpl getInstance() {
        return INSTANCE;
    }

    private DocumentTypesServiceImpl() { }

    @Override
    public DocumentType getDocumentType(final Node handle, final Optional<Locale> locale)
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
    public DocumentType getDocumentType(final String id, final Session userSession, final Optional<Locale> locale)
            throws DocumentTypeNotFoundException {
        if ("ns:testdocument".equals(id)) {
            return MockResponse.createTestDocumentType();
        }

        final ContentTypeContext context = getContentTypeContext(id, userSession, 0, locale);

        validateDocumentType(context, id);

        final DocumentType docType = new DocumentType();
        docType.setId(id);
        LocalizationUtils.determineDocumentDisplayName(id, context.getResourceBundle()).ifPresent(docType::setDisplayName);

        populateFields(docType.getFields(), context, docType);

        return docType;
    }

    @Override
    public void populateFieldsForCompoundType(final String id, final List<FieldType> fields,
                                              final ContentTypeContext parentContext, final DocumentType docType) {
        try {
            final Session userSession = parentContext.getContentTypeRoot().getSession();
            final int level = parentContext.getLevel() + 1;
            final Optional<Locale> locale = parentContext.getLocale();
            final ContentTypeContext context = ContentTypeContext.createDocumentTypeContext(id, userSession, level, locale);

            if (level <= MAX_NESTING_LEVEL) {
                populateFields(fields, context, docType);
            } else {
                log.info("Ignoring fields of {}-level-deep nested compound, nesting maximum reached", level);
            }
        } catch (RepositoryException e) {
            log.warn("Failed to retrieve user session", e);
        } catch (ContentTypeException e) {
            log.debug("Failed to create context for content type '{}'", id, e);
        }
    }

    private static ContentTypeContext getContentTypeContext(final String id, final Session userSession,
                                                            final int level, final Optional<Locale> locale)
            throws DocumentTypeNotFoundException {
        try {
            return ContentTypeContext.createDocumentTypeContext(id, userSession, level, locale);
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

    private static void populateFields(final List<FieldType> fields, final ContentTypeContext contentType,
                                       final DocumentType docType) {

        NamespaceUtils.retrieveFieldSorter(contentType.getContentTypeRoot())
                .ifPresent(sorter -> sorter.sortFields(contentType)
                        .stream()
                        .filter(FieldTypeUtils::isSupportedFieldType)
                        .filter(FieldTypeUtils::usesDefaultFieldPlugin)
                        .forEach(fieldType -> FieldTypeUtils.createAndInitFieldType(fieldType, contentType, docType)
                                .ifPresent(fields::add))
                );
    }
}
