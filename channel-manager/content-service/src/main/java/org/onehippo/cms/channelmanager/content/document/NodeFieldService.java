/*
 * Copyright 2021-2023 Bloomreach
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

import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;

public interface NodeFieldService {

    /**
     * Add a new node field to the document.
     *
     * @param documentPath  The path of the document
     * @param fieldPath     Path of the new node field
     * @param fields        The fields of the document
     * @param type          The (jcr) type of the new node
     */
    void addNodeField(final String documentPath, final FieldPath fieldPath, final List<FieldType> fields,
                      final String type);

    /**
     * Re-orders a node field in the document.
     *
     * @param documentPath  The path of the document
     * @param fieldPath     Path of the node field
     * @param position      The new position of the field
     */
    void reorderNodeField(String documentPath, FieldPath fieldPath, int position);

    /**
     * Remove a node field from the document.
     *
     * @param documentPath  The path of the document
     * @param fieldPath     Path of the node field
     * @param fields        The fields of the document
     */
    void removeNodeField(final String documentPath, final FieldPath fieldPath, final List<FieldType> fields);
}
