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

package org.onehippo.cms.channelmanager.content.document.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.onehippo.cms.channelmanager.content.documenttype.field.validation.ValidationErrorInfo;

/**
 * FieldValue encapsulates the value of a field.
 *
 * If the field is primitive, its value is stored in the "value" field. If it is a compound, the child
 * values are stored in the "fields" map. If a field value has validation errors, they can be associated
 * with the field value by storing them inside the errorInfo. The ID field is for future use: when the
 * client would like to reorder fields, each field value must have an ID to recognize a changed value order.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldValue {
    private String value;                         // Stringified value for primitive fields
    private Map<String, List<FieldValue>> fields; // Subfields of compound
    private String chosenId;                      // ID of a choice in case of a choice field
    private FieldValue chosenValue;               // Singular value of a choice field

    // private String id;    // ID for reordering, unique within a level (list) of field values
    private ValidationErrorInfo errorInfo;

    public FieldValue() { }

    public FieldValue(final String value) {
        this.value = value;
    }

    public FieldValue(final Map<String, List<FieldValue>> fields) {
        this.fields = fields;
    }

    public FieldValue(final String choiceId, final FieldValue choiceValue) {
        this.chosenId = choiceId;
        this.chosenValue = choiceValue;
    }

    public Optional<String> findValue() {
        return Optional.ofNullable(value);
    }

    // use for serialization and testing only, to avoid NPE.
    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public Optional<Map<String, List<FieldValue>>> findFields() {
        return Optional.ofNullable(fields);
    }

    // use for serialization and testing only, to avoid NPE.
    public Map<String, List<FieldValue>> getFields() {
        return fields;
    }

    public void setFields(final Map<String, List<FieldValue>> fields) {
        this.fields = fields;
    }

    public Optional<String> findChosenId() {
        return Optional.ofNullable(chosenId);
    }

    // use for serialization and testing only, to avoid NPE.
    public String getChosenId() {
        return chosenId;
    }

    public void setChosenId(final String chosenId) {
        this.chosenId = chosenId;
    }

    public Optional<FieldValue> findChosenValue() {
        return Optional.ofNullable(chosenValue);
    }

    // use for serialization and testing only, to avoid NPE.
    public FieldValue getChosenValue() {
        return chosenValue;
    }

    public void setChosenValue(final FieldValue chosenValue) {
        this.chosenValue = chosenValue;
    }

    public ValidationErrorInfo getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(final ValidationErrorInfo errorInfo) {
        this.errorInfo = errorInfo;
    }

    public boolean hasErrorInfo() {
        return errorInfo != null;
    }
}
