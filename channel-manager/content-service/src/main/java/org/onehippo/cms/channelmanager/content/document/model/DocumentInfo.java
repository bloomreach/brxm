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

package org.onehippo.cms.channelmanager.content.document.model;

import java.io.Serializable;
import java.util.Map;

import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This bean carries information of a document, stored in the CMS.
 * It is part of a document and can be serialized into JSON to expose it through a REST API.
 * Type {@code type} attribute refers to the document's {@link DocumentType} by id.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DocumentInfo {

    // enveloped reference to document type: { id: "namespace:typename" }
    private Type type;

    // whether this document has auto-drafted changes that have not been saved to the preview variant yet
    private boolean dirty;

    // maps to hippostd:publishableSummary (new, live, changed, or unknown)
    private PublicationState publicationState;

    private boolean canBePublished;
    private boolean canBeRequestedToPublish;

    public Type getType() {
        return type;
    }

    public void setTypeId(final String id) {
        type = new Type(id);
    }

    public static class Type {

        private final String id;

        @JsonCreator
        public Type(@JsonProperty("id") String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(final boolean dirty) {
        this.dirty = dirty;
    }

    public PublicationState getPublicationState() {
        return publicationState;
    }

    public void setPublicationState(final PublicationState publicationState) {
        this.publicationState = publicationState;
    }

    public boolean isCanBePublished() {
        return canBePublished;
    }

    public boolean isCanBeRequestedToPublish() {
        return canBeRequestedToPublish;
    }

    public void setAvailablePublicationActions(final Map<String, Serializable> hints) {
        canBePublished = hints.containsKey("publish") && ((Boolean) hints.get("publish"));
        canBeRequestedToPublish = hints.containsKey("requestPublication") && ((Boolean) hints.get("requestPublication"));
    }
}
