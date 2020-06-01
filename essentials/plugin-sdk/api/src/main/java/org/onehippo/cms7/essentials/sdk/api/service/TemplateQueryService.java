/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.sdk.api.service;

/**
 * TemplateQueryService provides methods to manipulate document type and folder template queries.
 *
 * Document type template queries are used for creating new documents of that type.
 * Folder template queries are used for creating folders with a limited set of supported document types.
 * Currently, this service only supports folder template queries for folders with a single supported document type.
 *
 * Can be @Inject'ed into REST Resources and Instructions.
 */
public interface TemplateQueryService {
    /**
     * Create a template query for a document type.
     *
     * @param jcrDocumentType prefixed JCR name of a document type
     * @return                true if the template query exists upon returning, false otherwise
     */
    boolean createDocumentTypeTemplateQuery(String jcrDocumentType);

    /**
     * Create a template query for a folder, constrained to a document type.
     *
     * @param jcrDocumentType prefixed JCR name of a document type
     * @return                true if the template query exists upon returning, false otherwise
     */
    boolean createFolderTemplateQuery(String jcrDocumentType);

    /**
     * Check if a template query exists for a document type.
     *
     * @param jcrDocumentType prefixed JCR name of a document type
     * @return                true if the template query exists, false otherwise
     */
    boolean documentTypeTemplateQueryExists(String jcrDocumentType);

    /**
     * Check if a template query exists for a folder, constrained to a document type.
     *
     * @param jcrDocumentType prefixed JCR name of a document type
     * @return                true if the template query exists, false otherwise
     */
    boolean folderTemplateQueryExists(String jcrDocumentType);
}
