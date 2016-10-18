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
    private final Optional<ResourceBundle> resourceBundle;
    private final Node contentTypeRoot;
    private final Locale locale;
    private final int level;

    /**
     * Create a new {@link ContentTypeContext} for the identified content type and the current CMS session.
     *
     * @param id          identifies the requested content type, e.g. "myhippoproject:newsdocument"
     * @param userSession JCR session using the privileges of the requesting user
     * @param locale      locale of the current CMS session
     * @param level       the nesting level of this content type in a document type. Top-level content types have
     *                    level 0, fields in top-level compounds have level 1, fields in nested compounds
     *                    have level 2, etc.
     * @return            {@link ContentTypeContext} for creating a {@link DocumentType}
     * @throws ContentTypeException
     *                    if creation of a ContentTypeContext failed.
     */
    public static ContentTypeContext createDocumentTypeContext(final String id, final Session userSession,
                                                               final Locale locale, final int level)
            throws ContentTypeException {

        final ContentType contentType = getContentType(id).orElseThrow(ContentTypeException::new);
        final Optional<ResourceBundle> resourceBundle = LocalizationUtils.getResourceBundleForDocument(id, locale);
        final Node documentTypeRoot = NamespaceUtils.getDocumentTypeRootNode(id, userSession)
                .orElseThrow(ContentTypeException::new);

        return new ContentTypeContext(contentType, resourceBundle, documentTypeRoot, locale, level);
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
                               final Optional<ResourceBundle> resourceBundle,
                               final Node documentTypeRoot,
                               final Locale locale,
                               final int level) {
        this.contentType = contentType;
        this.resourceBundle = resourceBundle;
        this.contentTypeRoot = documentTypeRoot;
        this.locale = locale;
        this.level = level;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public Optional<ResourceBundle> getResourceBundle() {
        return resourceBundle;
    }

    public Node getContentTypeRoot() {
        return contentTypeRoot;
    }

    public Locale getLocale() {
        return locale;
    }

    public int getLevel() {
        return level;
    }
}
