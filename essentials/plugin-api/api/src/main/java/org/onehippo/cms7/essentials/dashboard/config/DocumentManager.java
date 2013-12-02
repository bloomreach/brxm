/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.config;

/**
 * @version "$Id$"
 */
public interface DocumentManager {


    /**
     * Fetches a JCR repository item for specific content type (provided by clazz)
     *
     * @param path  repository path (absolute)
     * @param clazz Class of content implementation
     * @return null if no item was found, instance of a document otherwise
     */
    <T extends Document> T fetchDocument(String path, Class<T> clazz);

    /**
     * Save document to JCR repository
     *
     * @param document document instance to save
     * @return true on success, false otherwise
     */
    boolean saveDocument(Document document);

    /**
     * Fetches a JCR repository item
     *
     * @param path repository path (absolute)
     * @return null if no item was found, instance of a document otherwise
     */
    <T extends Document> T fetchDocument(String path);

    /**
     * Fetches a JCR repository item
     *
     * @param path        repository path (absolute)
     * @param documentType DocumentType value as annotated on the Document implementation class
     * @return null if no item was found, instance of a content otherwise
     */
    <T extends Document> T fetchDocument(String path, String documentType);
}
