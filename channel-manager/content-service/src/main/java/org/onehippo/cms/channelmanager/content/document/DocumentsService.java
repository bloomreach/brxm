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

package org.onehippo.cms.channelmanager.content.document;

import java.util.Locale;

import javax.jcr.Session;

import org.onehippo.cms.channelmanager.content.document.model.Document;

/**
 * DocumentsService exposes an API for reading and manipulating documents
 */
public interface DocumentsService {

    static DocumentsService get() {
        return DocumentsServiceImpl.getInstance();
    }

    /**
     * Creates a draft version of a document and locks it for editing by the current CMS user.
     *
     * If all goes well, the document's content is returned.
     *
     * @param uuid    UUID of the requested document (handle)
     * @param session user-authenticated JCR session for reading from the repository
     * @param locale  locale of the current CMS session
     * @return        JSON-serializable representation of the parts supported for exposing
     * @throws DocumentNotFoundException
     *                If the requested UUID was not found or is not a document
     */
    Document createDraft(final String uuid, final Session session, final Locale locale)
            throws DocumentNotFoundException;

    /**
     * Read the published variant of a document
     *
     * @param uuid    UUID of the requested document (handle)
     * @param session user-authenticated JCR session for reading from the repository
     * @param locale  locale of the current CMS session
     * @return        JSON-serializable representation of the parts supported for exposing
     * @throws DocumentNotFoundException
     *                If the requested UUID was not found or is not a document
     */
    Document getPublished(final String uuid, final Session session, final Locale locale)
            throws DocumentNotFoundException;
}
