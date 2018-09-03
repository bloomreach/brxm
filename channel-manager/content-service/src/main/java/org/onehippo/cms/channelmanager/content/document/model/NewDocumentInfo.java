/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

public class NewDocumentInfo {

    private String name;
    private String slug;
    private String documentTemplateQuery;
    private String documentTypeId;
    private String rootPath;
    private String defaultPath;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(final String slug) {
        this.slug = slug;
    }

    public String getDocumentTemplateQuery() {
        return documentTemplateQuery;
    }

    public void setDocumentTemplateQuery(final String documentTemplateQuery) {
        this.documentTemplateQuery = documentTemplateQuery;
    }

    public String getDocumentTypeId() {
        return documentTypeId;
    }

    public void setDocumentTypeId(final String documentTypeId) {
        this.documentTypeId = documentTypeId;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(final String rootPath) {
        this.rootPath = rootPath;
    }

    public String getDefaultPath() {
        return defaultPath;
    }

    public void setDefaultPath(final String defaultPath) {
        this.defaultPath = defaultPath;
    }
}
