/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Objects;

import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This bean carries information of a document, stored in the CMS.
 * It is part of a document and can be serialized into JSON to expose it through a REST API.
 * Type {@code type} attribute refers to the document's {@link DocumentType} by id.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
// only return the error count and error messages, never let clients set it
@JsonIgnoreProperties(value = {"errorCount", "errorMessages"}, allowGetters = true)
public class DocumentInfo {

    /**
     * enveloped reference to document type: { id: "namespace:typename" }
     */
    private Type type;

    // whether this document has auto-drafted changes that have not been saved to the preview variant yet
    private boolean dirty;

    // the number of validation errors
    private int errorCount;

    // human-readable messages of document-level validation errors
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> errorMessages;

    // maps to hippostd:publishableSummary (new, live, changed, or unknown)
    private PublicationState publicationState;

    private boolean canPublish;
    private boolean canRequestPublication;
    private String locale;
    private boolean canKeepDraft;
    private boolean retainable;

    public boolean isRetainable() {
        return retainable;
    }

    public Type getType() {
        return type;
    }

    public void setTypeId(final String id) {
        type = new Type(id);
    }

    public void setCanKeepDraft(final boolean saveDraft) {
        this.canKeepDraft = saveDraft;
    }

    public void setRetainable(final boolean retainable) {
        this.retainable = retainable;
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

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(final int errorCount) {
        this.errorCount = errorCount;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(final List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public PublicationState getPublicationState() {
        return publicationState;
    }

    public void setPublicationState(final PublicationState publicationState) {
        this.publicationState = publicationState;
    }

    public boolean isCanPublish() {
        return canPublish;
    }

    public boolean isCanKeepDraft(){
        return canKeepDraft;
    }

    public boolean isCanRequestPublication() {
        return canRequestPublication;
    }

    public void setCanPublish(final boolean canPublish) {
        this.canPublish = canPublish;
    }

    public void setCanRequestPublication(final boolean canRequestPublication) {
        this.canRequestPublication = canRequestPublication;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(final String locale) {
        this.locale = locale;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentInfo)) {
            return false;
        }
        final DocumentInfo that = (DocumentInfo) o;
        return isDirty() == that.isDirty() &&
                getErrorCount() == that.getErrorCount() &&
                isCanPublish() == that.isCanPublish() &&
                isCanRequestPublication() == that.isCanRequestPublication() &&
                isCanKeepDraft() == that.isCanKeepDraft() &&
                isRetainable() == that.isRetainable() &&
                Objects.equals(getType(), that.getType()) &&
                Objects.equals(getErrorMessages(), that.getErrorMessages()) &&
                getPublicationState() == that.getPublicationState() &&
                Objects.equals(getLocale(), that.getLocale());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), isDirty(), getErrorCount(), getErrorMessages(), getPublicationState(), isCanPublish(), isCanRequestPublication(), getLocale(), isCanKeepDraft(), isRetainable());
    }


    @Override
    public String toString() {
        return "DocumentInfo{" +
                "type=" + type +
                ", dirty=" + dirty +
                ", errorCount=" + errorCount +
                ", errorMessages=" + errorMessages +
                ", publicationState=" + publicationState +
                ", canPublish=" + canPublish +
                ", canRequestPublication=" + canRequestPublication +
                ", locale='" + locale + '\'' +
                ", canKeepDraft=" + canKeepDraft +
                ", retainable=" + retainable +
                '}';
    }
}
