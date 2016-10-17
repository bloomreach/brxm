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

package org.onehippo.cms.channelmanager.content;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;

/**
 * This class temporarily provides the front-end with mock responses of the visual editing REST API.
 * Once the back-end has been fully implemented, this class should be deleted.
 */
public class MockResponse {

    public static Document createTestDocument(final String id) {
        final String resourcePath = "/MockResponse-document.json";
        try {
            final Document document = readResource(resourcePath, Document.class);
            document.setId(id);
            return document;
        } catch (IOException e) {
            throw new IllegalStateException("Error reading mock document " + resourcePath, e);
        }
    }

    public static DocumentType createTestDocumentType() {
        final String resourcePath = "/MockResponse-documenttype.json";
        try {
            return readResource(resourcePath, DocumentType.class);
        } catch (IOException e) {
            throw new IllegalStateException("Error reading mock document type " + resourcePath, e);
        }
    }

    private static <T> T readResource(String resourcePath, Class<T> c) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        final InputStream resourceStream = MockResponse.class.getResourceAsStream(resourcePath);
        return mapper.readValue(resourceStream, c);
    }
}
