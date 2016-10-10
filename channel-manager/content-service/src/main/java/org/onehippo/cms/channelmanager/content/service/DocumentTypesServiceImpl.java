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

import java.util.Locale;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.exception.DocumentTypeNotFoundException;
import org.onehippo.cms.channelmanager.content.model.documenttype.DocumentType;
import org.onehippo.cms.channelmanager.content.util.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.util.FieldValidators;
import org.onehippo.cms.channelmanager.content.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.util.MockResponse;
import org.onehippo.cms.channelmanager.content.util.NamespaceUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.repository.l10n.ResourceBundle;
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

        final ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        final ContentType contentType = getContentType(id, service);
        validateDocumentType(contentType, id);

        final ScanningContext context = createScanningContext(id, contentType, userSession, locale);
        final DocumentType docType = new DocumentType();

        docType.setId(id);
        LocalizationUtils.determineDocumentDisplayName(id, context.resourceBundle).ifPresent(docType::setDisplayName);
        populateFields(docType, context);

        return docType;
    }

    private static ContentType getContentType(final String id, final ContentTypeService service) throws DocumentTypeNotFoundException {
        try {
            return service.getContentTypes().getType(id);
        } catch (RepositoryException e) {
            log.warn("Failed to retrieve content type '{}'", id, e);
            throw new DocumentTypeNotFoundException();
        }
    }

    private static void validateDocumentType(final ContentType contentType, final String id)
            throws DocumentTypeNotFoundException {
        if (contentType == null) {
            log.debug("No content type found for '{}'", id);
            throw new DocumentTypeNotFoundException();
        }
        if (!contentType.isDocumentType()) {
            log.debug("Requested type '{}' is not document type", id);
            throw new DocumentTypeNotFoundException();
        }
    }

    private static ScanningContext createScanningContext(final String id, final ContentType contentType,
                                                    final Session userSession, final Locale locale)
            throws DocumentTypeNotFoundException {

        final Node documentTypeRoot = NamespaceUtils.getDocumentTypeRootNode(id, userSession)
                                                    .orElseThrow(DocumentTypeNotFoundException::new);
        final Optional<ResourceBundle> resourceBundle = LocalizationUtils.getResourceBundleForDocument(id, locale);

        return new ScanningContext(contentType, resourceBundle, documentTypeRoot);
    }

    private static void populateFields(final DocumentType docType, final ScanningContext context) {
        context.contentType.getProperties()
                .values()
                .stream()
                .filter(FieldTypeUtils::isProjectProperty)
                .filter(FieldTypeUtils::isSupportedFieldType)
                .filter(field -> FieldTypeUtils.usesDefaultFieldPlugin(field, context.documentTypeRoot))
                .forEach(field -> addPropertyField(docType, field, context));
    }

    private static void addPropertyField(final DocumentType docType,
                                    final ContentTypeProperty property,
                                    final ScanningContext context) {
        FieldTypeUtils.createFieldType(property).ifPresent((fieldType) -> {
            final String fieldId = property.getName();

            fieldType.setId(fieldId);

            LocalizationUtils.determineFieldDisplayName(fieldId, context.resourceBundle, context.documentTypeRoot)
                    .ifPresent(fieldType::setDisplayName);
            LocalizationUtils.determineFieldHint(fieldId, context.resourceBundle, context.documentTypeRoot)
                    .ifPresent(fieldType::setHint);
            fieldType.setStoredAsMultiValueProperty(property.isMultiple());

            if (property.isMultiple() || property.getValidators().contains(FieldValidators.OPTIONAL)) {
                fieldType.setMultiple(true);
            }

            FieldTypeUtils.determineValidators(fieldType, docType, property.getValidators());

            docType.addField(fieldType);
        });
    }

    /**
     * DocumentTypeScanningContext reduces the need to pass long parameter lists of any of these resources through
     * nested method invocation.
     */
    private static class ScanningContext {
        private final ContentType contentType;
        private final Optional<ResourceBundle> resourceBundle;
        private final Node documentTypeRoot;

        private ScanningContext(final ContentType contentType,
                               final Optional<ResourceBundle> resourceBundle,
                               final Node documentTypeRoot) {
            this.contentType = contentType;
            this.resourceBundle = resourceBundle;
            this.documentTypeRoot = documentTypeRoot;
        }
    }
}
