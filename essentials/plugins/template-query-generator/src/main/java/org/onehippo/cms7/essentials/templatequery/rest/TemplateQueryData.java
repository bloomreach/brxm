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

import java.util.List;

import org.onehippo.cms7.essentials.sdk.api.model.rest.ContentType;

public class TemplateQueryData {

    private List<Scope> scopes;
    private List<ContentType> contentTypes;

    public List<Scope> getScopes() {
        return scopes;
    }

    public void setScopes(final List<Scope> scopes) {
        this.scopes = scopes;
    }

    public List<ContentType> getContentTypes() {
        return contentTypes;
    }

    public void setContentTypes(final List<ContentType> contentTypes) {
        this.contentTypes = contentTypes;
    }

    public enum Scope {
        DOCUMENT, FOLDER
    }
}
