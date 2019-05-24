/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.List;
import java.util.Optional;

import javax.jcr.Node;

import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.CompoundContext;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;

/**
 * Base implementation requirements for all field types.
 */
public interface BaseFieldType extends FieldType {

    /**
     * Reads a field's value(s) from a JCR node
     *
     * @param node JCR node to read the value(s) from
     * @return     the field's values; can be an empty list when the field does not have any values.
     */
    List<FieldValue> readValues(final Node node);

    /**
     * Writes the optional value(s) of this field to the provided JCR node.
     *
     * We purposefully pass in the value as an optional, because the validation of the cardinality constraint
     * can happen during this call. If we would not do the call if the field has no value, then the validation
     * against the field's cardinality constraints or against the field's current number of values would not
     * take place.
     *
     * @param node          JCR node to store the value on
     * @param optionalValues value to write, or nothing, wrapped in an Optional
     * @param checkCardinality whether to validate the cardinality constraint.
     * @throws ErrorWithPayloadException
     *                      indicates that writing the provided value ran into an unrecoverable error
     */
    void writeValues(final Node node, final Optional<List<FieldValue>> optionalValues, boolean checkCardinality);

    /**
     * Validates a single value.
     * @param value value to validate
     * @param context the context for this field
     * @return the number of violations found
     */
    int validateValue(final FieldValue value, final CompoundContext context) throws ErrorWithPayloadException;

    /**
     * Converts the value of this field to the Java object passed to the validators of this field.
     * @param value the value of this field
     * @param context the context for this field
     * @return the value passed to the validators of this field.
     */
    Object getValidatedValue(FieldValue value, final CompoundContext context);

}
