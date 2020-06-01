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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.repository.l10n.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ContentTypeContext groups and wraps sources of information about a content type.
 */
public class ContentTypeContext {
    private static final Logger log = LoggerFactory.getLogger(ContentTypeContext.class);
    private static final int MAX_NESTING_LEVEL = 10;

    private final ContentType contentType;
    private final Session session;
    private final Node contentTypeRoot;
    private final Locale locale;
    private final DocumentType documentType;
    private final int level;
    private final ResourceBundle resourceBundle;
    private final List<FieldScanningContext> fieldScanningContexts;

    /**
     * Retrieve the data for a specific content type.
     *
     * @param id ID of the content type, e.g. "myhippoproject:addresscompound"
     * @return   {@link ContentType} corresponding to the requested ID, or nothing, wrapped in an Optional
     */
    public static Optional<ContentType> getContentType(final String id) {
        final ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        try {
            return Optional.ofNullable(service.getContentTypes().getType(id));
        } catch (RepositoryException e) {
            log.warn("Failed to retrieve content type '{}'", id, e);
        }
        return Optional.empty();
    }

    /**
     * Create a new {@link ContentTypeContext} for the identified content type and the current CMS session.
     *
     * @param id      identifies the requested content type, e.g. "myhippoproject:newsdocument"
     * @param session JCR session using the privileges of the requesting user
     * @param Locale  locale of the current CMS session
     * @param docType {@link DocumentType} being assembled
     * @return        {@link ContentTypeContext} for creating a {@link DocumentType}, wrapped in an Optional
     */
    public static Optional<ContentTypeContext> createForDocumentType(final String id,
                                                                     final Session session,
                                                                     final Locale Locale,
                                                                     final DocumentType docType) {
        return create(id, session, Locale, docType, 0);
    }

    /**
     * Create a new {@link ContentTypeContext} for the identified content type, given a parent context.
     *
     * @param id            identifies the requested content type, e.g. "myhippoproject:newsdocument"
     * @param parentContext content type context supplying the JCR session and locale to use.
     * @return              {@link ContentTypeContext} for creating a {@link DocumentType}, wrapped in an Optional
     */
    public static Optional<ContentTypeContext> createFromParent(final String id, final ContentTypeContext parentContext) {
        final int level = parentContext.getLevel() + 1;
        if (level < MAX_NESTING_LEVEL) {
            return create(id, parentContext.getSession(), parentContext.getLocale(), parentContext.getDocumentType(), level);
        } else {
            log.info("Ignoring fields of {}-level-deep nested compound, nesting maximum reached", level);
        }
        return Optional.empty();
    }

    /**
     * Upon successful retrieval of the contentType and the contentTypeRoot Node,
     * create a new {@link ContentTypeContext} instance.
     */
    private static Optional<ContentTypeContext> create(final String id,
                                                       final Session session,
                                                       final Locale locale,
                                                       final DocumentType docType,
                                                       final int level) {
        return getContentType(id)
                .flatMap(contentType -> NamespaceUtils.getContentTypeRootNode(id, session)
                        .map(contentTypeRoot -> new ContentTypeContext(id, contentType, session, contentTypeRoot,
                                                                       locale, docType, level)));
    }

    private ContentTypeContext(final String id,
                               final ContentType contentType,
                               final Session session,
                               final Node documentTypeRoot,
                               final Locale locale,
                               final DocumentType documentType,
                               final int level) {
        this.contentType = contentType;
        this.session = session;
        this.contentTypeRoot = documentTypeRoot;
        this.locale = locale;
        this.documentType = documentType;
        this.level = level;

        this.resourceBundle = LocalizationUtils.getResourceBundleForDocument(id, locale).orElse(null);
        this.fieldScanningContexts = new ArrayList<>();

        populateTypesForFields(id);
    }

    public ContentType getContentType() {
        return contentType;
    }

    public Session getSession() {
        return session;
    }

    public Node getContentTypeRoot() {
        return contentTypeRoot;
    }

    public Locale getLocale() {
        return locale;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public int getLevel() {
        return level;
    }

    public Optional<ResourceBundle> getResourceBundle() {
        return Optional.ofNullable(resourceBundle);
    }

    public List<FieldScanningContext> getFieldScanningContexts() {
        return fieldScanningContexts;
    }

    /**
     * Prepare the list of {@link FieldScanningContext}s representing the content types applicable for field scanning.
     *
     * On top of the main/primary content type of this content type context, the content type's supertypes
     * are also considered applicable, if they have a corresponding nodeTypeNode with child nodes.
     */
    private void populateTypesForFields(final String id) {
        makeSlimContentTypeContext(id, contentType, true).ifPresent(fieldScanningContexts::add);

        contentType.getSuperTypes().stream()
                .forEach(superTypeName -> getContentType(superTypeName)
                        .flatMap(superType -> makeSlimContentTypeContext(superTypeName, superType, false))
                        .ifPresent(fieldScanningContexts::add));
    }

    private Optional<FieldScanningContext> makeSlimContentTypeContext(final String id,
                                                                      final ContentType contentType,
                                                                      final boolean allowChildless) {
        return NamespaceUtils.getContentTypeRootNode(id, session)
                .flatMap(rootNode -> NamespaceUtils.getNodeTypeNode(rootNode, allowChildless)
                        .map(nodeTypeNode -> new FieldScanningContext(contentType, nodeTypeNode)));
    }
}
