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

package org.onehippo.cms.channelmanager.content.documenttype.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;

/**
 * This bean represents a document type, known to the CMS.
 * It can be serialized into JSON to expose it through a REST API.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DocumentType {
    private String id; // "namespace:typename"
    private String displayName;
    private boolean readOnlyDueToUnknownValidator;
    private final List<FieldType> fields; // ordered list of fields

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

    public List<FieldType> getFields() {
        return fields;
    }

    public void addField(final FieldType field) {
        fields.add(field);
    }
}
