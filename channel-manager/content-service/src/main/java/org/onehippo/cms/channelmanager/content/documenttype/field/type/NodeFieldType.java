/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;

/**
 * A field that stores its value(s) in a node.
 */
public interface NodeFieldType extends FieldType {

    /**
     * Reads a single field value from a node.
     * @param node the node to read from
     * @return the value read
     */
    FieldValue readValue(final Node node);

    /**
     * Writes a single field value to a node.
     * @param node the node to write to
     * @param fieldValue the value to write
     * @throws ErrorWithPayloadException when the field value is wrong
     * @throws RepositoryException when the write failed
     */
    void writeValue(final Node node, final FieldValue fieldValue) throws ErrorWithPayloadException, RepositoryException;

    /**
     * Writes a field value to the field specified by the field path. Can be this field or a child field in case of
     * compound or compound-like fields.
     *
     * @param node the node to write to
     * @param fieldPath the path to the field
     * @param values the values to write
     * @return true if the value has been written, false otherwise.
     * @throws ErrorWithPayloadException when the field path or field value is wrong
     * @throws RepositoryException when the write failed
     */
    boolean writeFieldValue(final Node node, FieldPath fieldPath, final List<FieldValue> values) throws ErrorWithPayloadException, RepositoryException;

    /**
     * Validates the value.
     * @param value value to validate
     * @return true of the value is valid, false otherwise.
     */
    boolean validateValue(final FieldValue value);

}
