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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This bean represents a document, stored in the CMS.
 * It can be serialized into JSON to expose it through a REST API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Document {
    private String id;                // UUID
    private String displayName;
    private DocumentInfo info;        // read-only information about (the current state of) the document
    private Map<String, List<FieldValue>> fields;

    public Document() {
        setInfo(new DocumentInfo());
        setFields(new HashMap<>());
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

    public DocumentInfo getInfo() {
        return info;
    }

    public void setInfo(final DocumentInfo info) {
        this.info = info;
    }

    public Map<String, List<FieldValue>> getFields() {
        return fields;
    }

    public void setFields(final Map<String, List<FieldValue>> fields) {
        this.fields = fields;
    }
}
