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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.jcr.Session;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.channelmanager.content.exception.DocumentNotFoundException;
import org.onehippo.cms.channelmanager.content.exception.DocumentTypeNotFoundException;
import org.onehippo.cms.channelmanager.content.model.Document;
import org.onehippo.cms.channelmanager.content.model.DocumentTypeSpec;
import org.onehippo.jaxrs.cxf.CXFTest;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.hamcrest.core.IsEqual.equalTo;

public class ContentResourceTest extends CXFTest {

    private Session userSession;
    private Locale locale;
    private ContentService contentService;
    private DocumentTypeFactory documentTypeFactory;

    @Before
    public void setup() {
        locale = new Locale("en");
        userSession = createMock(Session.class);
        contentService = createMock(ContentService.class);
        documentTypeFactory = createMock(DocumentTypeFactory.class);

        final SessionDataProvider sessionDataProvider = createMock(SessionDataProvider.class);
        expect(sessionDataProvider.getJcrSession(anyObject())).andReturn(userSession).anyTimes();
        expect(sessionDataProvider.getLocale(anyObject())).andReturn(locale).anyTimes();
        replay(sessionDataProvider);

        final CXFTest.Config config = new CXFTest.Config();
        config.addServerSingleton(new ContentResource(sessionDataProvider, contentService, documentTypeFactory));
        config.addServerSingleton(new JacksonJsonProvider());

        setup(config);
    }

    @Test
    public void retrieveDocument() throws Exception {
        final String requestedUuid = "requested-uuid";
        final String returnedUuid = "returned-uuid";
        final Document testDocument = new Document();
        testDocument.setId(returnedUuid);
        expect(contentService.getDocument(userSession, requestedUuid)).andReturn(testDocument);
        replay(contentService);

        final String expectedBody = normalizeJsonResource("/empty-document.json");

        when()
                .get("/documents/" + requestedUuid)
        .then()
                .statusCode(200)
                .body(equalTo(expectedBody));
    }

    @Test
    public void documentNotFound() throws Exception {
        final String requestedUuid = "requested-uuid";

        expect(contentService.getDocument(userSession, requestedUuid)).andThrow(new DocumentNotFoundException());
        replay(contentService);

        when()
                .get("/documents/" + requestedUuid)
        .then()
                .statusCode(404);
    }

    @Test
    public void retrieveDocumentType() throws Exception {
        final String requestedId = "ns:testdocument";
        final String returnedId = "ns:otherdocument";
        final DocumentTypeSpec docType = new DocumentTypeSpec();
        docType.setId(returnedId);

        expect(documentTypeFactory.getDocumentTypeSpec(requestedId, locale)).andReturn(docType);
        replay(documentTypeFactory);

        final String expectedBody = normalizeJsonResource("/empty-documenttype.json");

        when()
                .get("/documenttypes/" + requestedId)
        .then()
                .statusCode(200)
                .body(equalTo(expectedBody));
    }

    @Test
    public void documentTypeNotFound() throws Exception {
        final String requestedId = "ns:testdocument";

        expect(documentTypeFactory.getDocumentTypeSpec(requestedId, locale)).andThrow(new DocumentTypeNotFoundException());
        replay(documentTypeFactory);

        when()
                .get("/documenttypes/" + requestedId)
        .then()
                .statusCode(404);
    }

    private String normalizeJsonResource(final String resourcePath) {
        final InputStream resourceStream = ContentResourceTest.class.getResourceAsStream(resourcePath);
        return new BufferedReader(new InputStreamReader(resourceStream))
                .lines()
                .map(String::trim)
                .collect(Collectors.joining(""));
    }
}
