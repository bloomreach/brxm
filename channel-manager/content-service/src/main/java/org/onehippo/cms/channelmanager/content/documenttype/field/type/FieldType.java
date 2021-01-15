/*
 * Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Optional;

import javax.jcr.Node;

import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;

/**
 * A field type in a {@link DocumentType}.
 */
public interface FieldType {

    enum Type {
        STRING,
        DOUBLE,
        LONG,
        BOOLEAN,
        MULTILINE_STRING,
        DATE_AND_TIME,
        DATE_ONLY,
        RADIO_GROUP,
        BOOLEAN_RADIO_GROUP,
        STATIC_DROPDOWN,
        DYNAMIC_DROPDOWN,
        HTML,
        CHOICE, // "content blocks"
        COMPOUND,
        IMAGE_LINK,
        NODE_LINK,
        OPEN_UI
    }

    String getId();

    void setId(final String id);

    Type getType();

    String getJcrType();

    String getDisplayName();

    void setDisplayName(final String displayName);

    String getHint();

    void setHint(final String hint);

    int getMinValues();

    void setMinValues(final int minValues);

    int getMaxValues();

    void setMaxValues(final int maxValues);

    boolean isMultiple();

    void setMultiple(final boolean isMultiple);

    boolean isOrderable();

    void setOrderable(final boolean orderable);

    /**
     * Represents the "required" validator, which indicates that a field must have content. What exactly that
     * means depends on the field type. The "required" validator is *not* meant to indicate that at least one
     * instance of a multiple field must be present.
     *
     * @return true or false
     */
    boolean isRequired();

    /**
     * Checks if an initialized field is supported, i.e. should be present in a document type.
     *
     * @return true or false
     */
    boolean isSupported();

    /**
     * Initializes a {@link FieldType}, given a field context.
     *
     * @param fieldContext  information about the field (as part of a parent content type)
     * @return information about the initialized fields.
     */
    FieldsInformation init(final FieldTypeContext fieldContext);

    /**
     * Reads a field's value(s) from a JCR node
     *
     * @param node JCR node to read the value(s) from
     * @return     a list of at least one value, or nothing, wrapped in an Optional
     */
    Optional<List<FieldValue>> readFrom(final Node node);

    /**
     * Writes the optional value(s) of this field to the provided JCR node.
     *
     * We purposefully pass in the value as an optional, because the validation of the cardinality constraint
     * happens during this call. If we would not do the call if the field has no value, then the validation
     * against the field's cardinality constraints or against the field's current number of values would not
     * take place.
     *
     * @param node          JCR node to store the value on
     * @param optionalValues value to write, or nothing, wrapped in an Optional
     * @throws ErrorWithPayloadException
     *                      indicates that writing the provided value ran into an unrecoverable error
     */
    void writeTo(final Node node, final Optional<List<FieldValue>> optionalValues) throws ErrorWithPayloadException;

    /**
     * Writes value(s) to the field indicated by the field path. Can be this field, or a child field in case of
     * compound or compound-like types.
     *
     * @param fieldPath the path to the field to write
     * @param values the values to write
     * @param context the context to use during validation
     * @return true if the values have been written, false otherwise.
     * @throws ErrorWithPayloadException
     *                      indicates that writing the provided values ran into an unrecoverable error
     */
    boolean writeField(final FieldPath fieldPath,
                       final List<FieldValue> values,
                       final CompoundContext context) throws ErrorWithPayloadException;

    /**
     * Validates the current value of this field (possible multiple) against all applicable (and supported) validators.
     * A field value with a violation will get its error info set.
     *
     * Note that the "required" validator is implemented as a sanity check in
     * {@link #writeField(FieldPath, List, CompoundContext) <FieldValue>)} since that is
     * supposed to be checked by the front-end.
     *
     * @param valueList list of field value(s) to validate
     * @param context context of this field
     * @return          the number of violations found
     */
    int validate(final List<FieldValue> valueList, final CompoundContext context) throws ErrorWithPayloadException;

    /**
     * Adds the name of a validator for this field.
     *
     * @param validatorName the name of the validator
     */
    void addValidatorName(final String validatorName);

    /**
     * Marks this field as having a validator known by the system
     * but not supported in this part of the code base.
     */
    void setUnsupportedValidator(boolean hasUnsupportedValidators);

    /**
     * @return whether this field has a validator known by the system
     * but not supported in this part of the code base.
     */
    boolean hasUnsupportedValidator();
}
