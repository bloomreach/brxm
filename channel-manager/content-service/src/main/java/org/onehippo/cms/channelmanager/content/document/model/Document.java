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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This bean represents a document, stored in the CMS. It can be serialized into JSON to expose it through a REST API.
 */
@JsonInclude(Include.NON_NULL)
public class Document {
    private String id;                // UUID
    private String variantId;
    private String branchId;
    private String displayName;
    private String urlName;
    private String repositoryPath;
    private DocumentInfo info;        // read-only information about (the current state of) the document
    private Map<String, List<FieldValue>> fields;
    private String state;

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

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(final String variantId) {
        this.variantId = variantId;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(final String branchId) {
        this.branchId = branchId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getUrlName() {
        return urlName;
    }

    public void setUrlName(final String urlName) {
        this.urlName = urlName;
    }

    public String getRepositoryPath() {
        return repositoryPath;
    }

    public void setRepositoryPath(final String repositoryPath) {
        this.repositoryPath = repositoryPath;
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

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Document)) {
            return false;
        }
        final Document document = (Document) o;
        return Objects.equals(getId(), document.getId()) &&
                Objects.equals(getVariantId(), document.getVariantId()) &&
                Objects.equals(getBranchId(), document.getBranchId()) &&
                Objects.equals(getDisplayName(), document.getDisplayName()) &&
                Objects.equals(getUrlName(), document.getUrlName()) &&
                Objects.equals(getRepositoryPath(), document.getRepositoryPath()) &&
                Objects.equals(getInfo(), document.getInfo()) &&
                Objects.equals(getFields(), document.getFields()) &&
                Objects.equals(getState(), document.getState());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getVariantId(), getBranchId(), getDisplayName(), getUrlName(), getRepositoryPath(), getInfo(), getFields(), getState());
    }


    @Override
    public String toString() {
        return "Document{" +
                "id='" + id + '\'' +
                ", variantId='" + variantId + '\'' +
                ", branchId='" + branchId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", urlName='" + urlName + '\'' +
                ", repositoryPath='" + repositoryPath + '\'' +
                ", info=" + info +
                ", fields=" + fields +
                ", state='" + state + '\'' +
                '}';
    }
}
