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
 * Document types to wrap {@link ContentType} types to getter fields in Dynamic Beans. 
 */
public enum DocumentType {
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

    public String getDocumentType() {
        return this.type;
    }

    DocumentType(String type) {
        this.type = type;
    }

    public static DocumentType getDocumentType(String type) {
        return Arrays.stream(DocumentType.values())
                .filter(doc -> doc.getDocumentType().equals(type))
                .findFirst()
                .orElse(DocumentType.UNKNOWN);
    }
}