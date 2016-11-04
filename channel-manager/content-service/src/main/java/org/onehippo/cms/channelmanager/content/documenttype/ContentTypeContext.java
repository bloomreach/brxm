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

    private final ContentType contentType;
    private final Node contentTypeRoot;
    private final int level;
    private final Optional<Locale> locale;
    private final Optional<ResourceBundle> resourceBundle;

    /**
     * Create a new {@link ContentTypeContext} for the identified content type and the current CMS session.
     *
     * @param id             identifies the requested content type, e.g. "myhippoproject:newsdocument"
     * @param userSession    JCR session using the privileges of the requesting user
     * @param level          the nesting level of this content type in a document type. Top-level content types have
     *                       level 0, fields in top-level compounds have level 1, fields in nested compounds
     *                       have level 2, etc.
     * @param optionalLocale locale of the current CMS session
     * @return               {@link ContentTypeContext} for creating a {@link DocumentType}, wrapped in an Optional
     */
    public static Optional<ContentTypeContext> createDocumentTypeContext(final String id,
                                                                         final Session userSession,
                                                                         final int level,
                                                                         final Optional<Locale> optionalLocale) {
        final Optional<ContentType> optionalContentType = getContentType(id);
        if (!optionalContentType.isPresent()) {
            return Optional.empty();
        }

        final Optional<Node> optionalDocumentTypeRoot = NamespaceUtils.getDocumentTypeRootNode(id, userSession);
        if (!optionalDocumentTypeRoot.isPresent()) {
            return Optional.empty();
        }

        return Optional.of(new ContentTypeContext(optionalContentType.get(),
                                                  optionalDocumentTypeRoot.get(),
                                                  level, optionalLocale));
    }

    private static Optional<ContentType> getContentType(final String id) {
        final ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        try {
            return Optional.ofNullable(service.getContentTypes().getType(id));
        } catch (RepositoryException e) {
            log.warn("Failed to retrieve content type '{}'", id, e);
        }
        return Optional.empty();
    }

    private ContentTypeContext(final ContentType contentType,
                               final Node documentTypeRoot,
                               final int level,
                               final Optional<Locale> optionalLocale) {
        this.contentType = contentType;
        this.contentTypeRoot = documentTypeRoot;
        this.level = level;
        this.locale = optionalLocale;
        this.resourceBundle = optionalLocale
                .flatMap(locale -> LocalizationUtils.getResourceBundleForDocument(contentType.getName(), locale));
    }

    public ContentType getContentType() {
        return contentType;
    }

    public Node getContentTypeRoot() {
        return contentTypeRoot;
    }

    public int getLevel() {
        return level;
    }

    public Optional<Locale> getLocale() {
        return locale;
    }

    public Optional<ResourceBundle> getResourceBundle() {
        return resourceBundle;
    }
}
