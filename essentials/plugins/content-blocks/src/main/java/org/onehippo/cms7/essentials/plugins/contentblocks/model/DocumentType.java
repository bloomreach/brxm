/*
 * Copyright 2014-2023 Bloomreach
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

package org.onehippo.cms7.essentials.plugins.contentblocks.model;

import java.util.List;

public class DocumentType {
    private String id;
    private String name;
    private List<ContentBlocksField> contentBlocksFields;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<ContentBlocksField> getContentBlocksFields() {
        return contentBlocksFields;
    }

    public void setContentBlocksFields(final List<ContentBlocksField> contentBlocksFields) {
        this.contentBlocksFields = contentBlocksFields;
    }
}
