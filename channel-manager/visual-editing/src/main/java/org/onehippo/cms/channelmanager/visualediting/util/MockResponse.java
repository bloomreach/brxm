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

package org.onehippo.cms.channelmanager.visualediting.util;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.onehippo.cms.channelmanager.visualediting.model.Document;
import org.onehippo.cms.channelmanager.visualediting.model.DocumentTypeSpec;

/**
 * This class temporarily provides the front-end with mock responses of the visual editing REST API.
 * Once the back-end has been fully implemented, this class should be deleted.
 */
public class MockResponse {

    public static Document createTestDocument(final String id) throws IOException {
        final Document document = readResource("/MockResponse-document.json", Document.class);
        document.setId(id);
        return document;
    }

    public static DocumentTypeSpec createTestDocumentType() throws IOException {
        return readResource("/MockResponse-documenttype.json", DocumentTypeSpec.class);
    }

    private static <T> T readResource(String resourcePath, Class<T> c) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        final InputStream resourceStream = MockResponse.class.getResourceAsStream(resourcePath);
        return mapper.readValue(resourceStream, c);
    }
}
