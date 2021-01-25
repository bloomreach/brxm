/*
 * Copyright 2016-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;

import org.onehippo.cms.channelmanager.content.UserContext;
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
     * @param uuid          UUID of the requested document (handle)
     * @param branchId      Id of the requested document branch
     * @param userContext   Properties of the user that executes the request
     *
     * @return JSON-serializable representation of the parts supported for exposing
     *
     * @throws ErrorWithPayloadException If reading the document failed
     */
    Document getDocument(final String uuid, final String branchId, final UserContext userContext);

    /**
     * Branches a document.
     * <p>
     * If all goes well, the document's content is returned.
     *
     * @param uuid          UUID of the requested document (handle)
     * @param branchId      Id of the requested document branch
     * @param userContext   Properties of the user that executes the request
     *
     * @return JSON-serializable representation of the parts supported for exposing
     *
     * @throws ErrorWithPayloadException If branching the document failed
     */
    Document branchDocument(final String uuid, final String branchId, final UserContext userContext);


    /**
     * Retrieves an editable version of a document and locks it for editing by the current CMS user.
     * <p>
     * If all goes well, the document's content is returned.
     *
     * @param uuid          UUID of the requested document (handle)
     * @param branchId      Id of the requested document branch
     * @param userContext   Properties of the user that executes the request
     *
     * @return JSON-serializable representation of the parts supported for exposing
     *
     * @throws ErrorWithPayloadException If obtaining the editable instance failed
     */
    Document obtainEditableDocument(final String uuid, final String branchId, final UserContext userContext);

    /**
     * Updates the editable version of a document, and keep it locked for further editing.
     * <p>
     * The persisted document may differ from the posted one (e.g. when fields are subject to additional processing
     * before being persisted).
     * <p>
     * In case of a bad request, changes may be pending.
     *
     * @param uuid          UUID of the document to be updated
     * @param document      Document containing the to-be-persisted content
     * @param userContext   Properties of the user that executes the request
     *
     * @return JSON-serializable representation of the persisted document.
     *
     * @throws ErrorWithPayloadException If updating the editable document failed
     */
    Document updateEditableDocument(final String uuid, final Document document, final UserContext userContext);

    /**
     * Update a single field value in the editable version of a document.
     * <p>
     * The persisted value may differ from the posted one (e.g. when fields are subject to additional processing before
     * being persisted).
     * <p>
     * In case of a bad request, changes may be pending.
     *
     * @param uuid          UUID of the document
     * @param branchId      Id of the requested document branch
     * @param fieldPath     Path to the field in the document
     * @param fieldValues   Field values containing the to-be-persisted content
     * @param userContext   Properties of the user that executes the request
     *
     * @return the field values; the ones that were deemed invalid will be annotated with an errorInfo object.
     *
     * @throws ErrorWithPayloadException If updating the field failed
     */
    List<FieldValue> updateEditableField(final String uuid, final String branchId, final FieldPath fieldPath,
                                         final List<FieldValue> fieldValues, final UserContext userContext);

    /**
     * Discard the editable version of a document, such that it is available for others to edit. The changes that were
     * saved in fields after obtaining the editable version of the document are discarded.
     * <p/>
     *
     * @param uuid          UUID of the document to be released
     * @param branchId      Id of the requested document branch
     * @param userContext   Properties of the user that executes the request
     *
     * @throws ErrorWithPayloadException If releasing the editable instance failed
     */
    void discardEditableDocument(final String uuid, final String branchId, final UserContext userContext);

    /**
     * Creates a new document. In case of a bad request, changes may be pending.
     *
     * @param newDocumentInfo   The information about the new document to create
     * @param userContext       Properties of the user that executes the request
     *
     * @return the created document
     */
    Document createDocument(final NewDocumentInfo newDocumentInfo, final UserContext userContext);

    /**
     * Updates the display name and URL name of a document. In case of a bad request, changes may be pending.
     *
     * @param uuid          UUID of the document (handle)
     * @param document      Document containing the to-be-persisted display name and URL name. Other fields are ignored.
     * @param userContext   Properties of the user that executes the request
     *
     * @throws ErrorWithPayloadException if the display name and/or URL name already exists, or changing the names fails.
     */
    Document updateDocumentNames(final String uuid, final String branchId, final Document document,
                                 final UserContext userContext);

    /**
     * Deletes a document. In case of a bad request, changes may be pending.
     *
     * @param uuid          UUID of the document (handle) to delete
     * @param branchId      Id of the requested document branch
     * @param userContext   Properties of the user that executes the request
     *
     * @throws ErrorWithPayloadException If deleting the document failed
     */
    void deleteDocument(final String uuid, final String branchId, final UserContext userContext);

    /**
     * Add a new compound field. The position of the new field can be managed by passing an index (1-based) in the
     * {@code fieldPath} argument using the square-bracket notation, e.g. field[2]. By default, the new field is
     * inserted at the first position.
     *
     * @param uuid          UUID of the document (handle)
     * @param branchId      Id of the requested document branch
     * @param fieldPath     Path to the new compound field in the document
     * @param userContext   Properties of the user that executes the request
     *
     * @return JSON-serializable representation of the new compound field.
     *
     * @throws ErrorWithPayloadException If adding a compound field to the document failed
     */
    Map<String, List<FieldValue>> addCompoundField(String uuid, String branchId, FieldPath fieldPath, UserContext userContext);
}
