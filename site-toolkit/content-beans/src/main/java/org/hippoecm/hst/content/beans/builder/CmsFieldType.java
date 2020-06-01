/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.builder;

import java.util.Arrays;

import org.onehippo.cms7.services.contenttype.ContentType;

/**
 * Enum of field types which can be added to document types as a property/node.
 * While generating a dynamic bean, these types get used to map {@link ContentType}
 * fields to content bean getters.
 */
public enum CmsFieldType {

    STRING("String"), //
    HTML("Html"), //
    PASSWORD("Password"), //
    TEXT("Text"), //
    DATE("Date"), //
    BOOLEAN("Boolean"), //
    LONG("Long"), //
    DOUBLE("Double"), //
    DOCBASE("Docbase"), //
    HIPPO_HTML("hippostd:html"), //
    HIPPO_IMAGELINK("hippogallerypicker:imagelink"), //
    HIPPO_MIRROR("hippo:mirror"), //
    HIPPO_IMAGE("hippogallery:image"), //
    HIPPO_RESOURCE("hippo:resource"), //
    HIPPO_COMPOUND("hippo:compound"), //
    CONTENT_BLOCKS("content:blocks"), //
    UNKNOWN("Unknown");

    private String type;

    public String getCmsFieldType() {
        return this.type;
    }

    CmsFieldType(String type) {
        this.type = type;
    }

    public static CmsFieldType getCmsFieldType(String type) {
        return Arrays.stream(CmsFieldType.values())
                .filter(doc -> doc.getCmsFieldType().equals(type))
                .findFirst()
                .orElse(CmsFieldType.UNKNOWN);
    }
}