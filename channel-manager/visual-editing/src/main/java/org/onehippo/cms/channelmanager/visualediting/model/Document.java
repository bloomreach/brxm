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

import java.util.HashMap;
import java.util.Map;

/**
 * This bean represents a document, stored in the CMS.
 * It can be serialized into JSON to expose it through a REST API.
 * Its {@code type} attribute refers to the document's {@link DocumentTypeSpec} by id.
 */
public class Document {
    private String id;                // UUID
    private Type type; // enveloped reference to document type: { id: "namespace:typename" }
    private String displayName;
    private Map<String, Object> fields;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public void setTypeId(final String id) {
        type = new Type(id);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void addField(final String id, final Object field) {
        if (fields == null) {
            fields = new HashMap<>();
        }
        fields.put(id, field);
    }

    private class Type {
        private String id;

        public Type(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
