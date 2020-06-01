/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;
import java.util.Locale;

import javax.jcr.Session;

import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.model.NewDocumentInfo;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;

/**
 * DocumentsService exposes an API for reading and manipulating documents
 */
public interface DocumentsService {

    /**
     * Gets a document for viewing by the current CMS user.
     * <p>
     *
     * @param uuid     UUID of the requested document (handle)
     * @param branchId id of the requested document branch
     * @param session  user-authenticated, invocation-scoped JCR session
     * @param locale   Locale of the CMS user
     * @return JSON-serializable representation of the parts supported for exposing
     * @throws ErrorWithPayloadException If reading the document failed
     */
    Document getDocument(String uuid, String branchId, Session session, Locale locale) throws ErrorWithPayloadException;

    /**
     * Branches a document.
     * <p>
     * If all goes well, the document's content is returned.
     *
     * @param uuid     UUID of the requested document (handle)
     * @param session  user-authenticated, invocation-scoped JCR session
     * @param locale   Locale of the CMS user
     * @param branchId id of branch
     * @return JSON-serializable representation of the parts supported for exposing
     * @throws ErrorWithPayloadException If branching the document failed
     */
    Document branchDocument(String uuid, Session session, Locale locale, String branchId) throws ErrorWithPayloadException;


    /**
     * Retrieves an editable version of a document and locks it for editing by the current CMS user.
     * <p>
     * If all goes well, the document's content is returned.
     *
     * @param uuid     UUID of the requested document (handle)
     * @param session  user-authenticated, invocation-scoped JCR session
     * @param locale   Locale of the CMS user
     * @param branchId id of branch
     * @return JSON-serializable representation of the parts supported for exposing
     * @throws ErrorWithPayloadException If obtaining the editable instance failed
     */
    Document obtainEditableDocument(String uuid, Session session, Locale locale, String branchId) throws ErrorWithPayloadException;

    /**
     * Updates the editable version of a document, and keep it locked for further editing.
     * <p>
     * The persisted document may differ from the posted one (e.g. when fields are subject to additional processing
     * before being persisted).
     *
     * @param uuid     UUID of the document to be updated
     * @param document Document containing the to-be-persisted content
     * @param session  user-authenticated, invocation-scoped JCR session. In case of a bad request, changes may be
     *                 pending.
     * @param locale   Locale of the CMS user
     * @return JSON-serializable representation of the persisted document.
     * @throws ErrorWithPayloadException If updating the editable document failed
     */
    Document updateEditableDocument(String uuid, Document document, Session session, Locale locale) throws ErrorWithPayloadException;

    /**
     * Update a single field value in the editable version of a document.
     * <p>
     * The persisted value may differ from the posted one (e.g. when fields are subject to additional processing before
     * being persisted).
     *
     * @param uuid        UUID of the document
     * @param fieldPath   Path to the field in the document
     * @param fieldValues Field values containing the to-be-persisted content
     * @param session     user-authenticated, invocation-scoped JCR session. In case of a bad request, changes may be
     *                    pending.
     * @param locale      Locale of the CMS user
     * @param branchId    id of branch
     * @throws ErrorWithPayloadException If updating the field failed
     */
    void updateEditableField(String uuid, FieldPath fieldPath, List<FieldValue> fieldValues, Session session, Locale locale, final String branchId) throws ErrorWithPayloadException;

    /**
     * Discard the editable version of a document, such that it is available for others to edit. The changes that were
     * saved in fields after obtaining the editable version of the document are discarded.
     * <p/>
     *
     * @param uuid     UUID of the document to be released
     * @param session  user-authenticated, invocation-scoped JCR session
     * @param locale   Locale of the CMS user
     * @param branchId id of branch
     * @throws ErrorWithPayloadException If releasing the editable instance failed
     */
    void discardEditableDocument(String uuid, Session session, Locale locale, final String branchId) throws ErrorWithPayloadException;

    /**
     * Creates a new document
     *
     * @param newDocumentInfo the information about the new document to create
     * @param session         user-authenticated, invocation-scoped JCR session. In case of a bad request, changes may
     *                        be pending.
     * @param locale          Locale of the CMS user
     * @return the created document
     */
    Document createDocument(NewDocumentInfo newDocumentInfo, Session session, Locale locale) throws ErrorWithPayloadException;

    /**
     * Updates the display name and URL name of a document.
     *
     * @param uuid     UUID of the document (handle)
     * @param document Document containing the to-be-persisted display name and URL name. Other fields are ignored.
     * @param session  user-authenticated, invocation-scoped JCR session. In case of a bad request, changes may
     *                 be pending.
     * @throws ErrorWithPayloadException if the display name and/or URL name already exists, or changing the names fails.
     */
    Document updateDocumentNames(String uuid, Document document, Session session, String branchId) throws ErrorWithPayloadException;

    /**
     * Deletes a document
     *
     * @param uuid     UUID of the document (handle) to delete
     * @param session  user-authenticated, invocation-scoped JCR session. In case of a bad request, changes may
     *                 be pending.
     * @param locale   Locale of the CMS user
     * @param branchId id of branch
     * @throws ErrorWithPayloadException If deleting the document failed
     */
    void deleteDocument(String uuid, Session session, Locale locale, String branchId) throws ErrorWithPayloadException;
}
