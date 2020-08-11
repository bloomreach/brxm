/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.essentials.templatequery.rest;

import org.onehippo.cms7.essentials.sdk.api.model.rest.ContentType;

public class TemplateQuery {

    private ContentType contentType;
    private boolean documentQueryExists;
    private boolean folderQueryExists;


    public boolean isDocumentQueryExists() {
        return documentQueryExists;
    }

    public void setDocumentQueryExists(final boolean documentQueryExists) {
        this.documentQueryExists = documentQueryExists;
    }

    public boolean isFolderQueryExists() {
        return folderQueryExists;
    }

    public void setFolderQueryExists(final boolean folderQueryExists) {
        this.folderQueryExists = folderQueryExists;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(final ContentType contentType) {
        this.contentType = contentType;
    }
}
