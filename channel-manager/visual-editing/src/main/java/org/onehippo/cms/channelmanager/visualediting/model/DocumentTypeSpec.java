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

package org.onehippo.cms.channelmanager.visualediting.model;

import java.util.ArrayList;
import java.util.List;

/**
 * This bean represents a document type, known to the CMS.
 * It can be serialized into JSON to expose it through a REST API.
 */
public class DocumentTypeSpec {
    private String id; // "namespace:typename"
    private String displayName;
    private List<FieldTypeSpec> fields; // ordered list of fields

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

    public List<FieldTypeSpec> getFields() {
        return fields;
    }

    public void addField(final FieldTypeSpec field) {
        if (fields == null) {
            fields = new ArrayList<>();
        }
        fields.add(field);
    }
}
