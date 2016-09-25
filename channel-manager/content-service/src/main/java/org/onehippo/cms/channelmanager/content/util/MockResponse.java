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

package org.onehippo.cms.channelmanager.content.util;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.onehippo.cms.channelmanager.content.model.Document;
import org.onehippo.cms.channelmanager.content.model.DocumentTypeSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class temporarily provides the front-end with mock responses of the visual editing REST API.
 * Once the back-end has been fully implemented, this class should be deleted.
 */
public class MockResponse {
    private static final Logger log = LoggerFactory.getLogger(MockResponse.class);

    public static Document createTestDocument(final String id) {
        final String resourcePath = "/MockResponse-document.json";
        Document document = null;
        try {
            document = readResource(resourcePath, Document.class);
            document.setId(id);
        } catch (IOException e) {
            log.error("Error reading mock document {}", resourcePath, e);
        }
        return document;
    }

    public static DocumentTypeSpec createTestDocumentType() {
        final String resourcePath = "/MockResponse-documenttype.json";
        DocumentTypeSpec docType = null;
        try {
            docType = readResource(resourcePath, DocumentTypeSpec.class);
        } catch (IOException e) {
            log.error("Error reading mock document type {}", resourcePath, e);
        }
        return docType;
    }

    private static <T> T readResource(String resourcePath, Class<T> c) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        final InputStream resourceStream = MockResponse.class.getResourceAsStream(resourcePath);
        return mapper.readValue(resourceStream, c);
    }
}
