/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.documenttype.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;


/**
 * This bean represents a document type, known to the CMS.
 * It can be serialized into JSON to expose it through a REST API.
 */
@JsonInclude(Include.NON_DEFAULT)
public class DocumentType {
    private String id; // "namespace:typename"
    private String displayName;
    private boolean readOnlyDueToUnknownValidator;
    private boolean allFieldsIncluded;
    private boolean allRequiredFieldsIncluded;
    private final List<FieldType> fields; // ordered list of fields

    @JsonInclude(Include.NON_EMPTY)
    private Set<String> unsupportedFieldTypes = null; // for reporting purposes

    public DocumentType() {
        fields = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public boolean isReadOnlyDueToUnknownValidator() {
        return readOnlyDueToUnknownValidator;
    }

    public void setReadOnlyDueToUnknownValidator(final boolean readOnlyDueToUnknownValidator) {
        this.readOnlyDueToUnknownValidator = readOnlyDueToUnknownValidator;
    }

    public boolean isAllFieldsIncluded() {
        return allFieldsIncluded;
    }

    public void setAllFieldsIncluded(final boolean allFieldsIncluded) {
        this.allFieldsIncluded = allFieldsIncluded;
    }

    public List<FieldType> getFields() {
        return fields;
    }

    public Set<String> getUnsupportedFieldTypes() {
        return unsupportedFieldTypes;
    }

    public void setUnsupportedFieldTypes(final Set<String> unsupportedFieldTypes) {
        this.unsupportedFieldTypes = unsupportedFieldTypes;
    }

    public boolean isAllRequiredFieldsIncluded() {
        return allRequiredFieldsIncluded;
    }

    public void setAllRequiredFieldsIncluded(final boolean allRequiredFieldsIncluded) {
        this.allRequiredFieldsIncluded = allRequiredFieldsIncluded;
    }
}
