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

package org.onehippo.cms.channelmanager.content;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.jcr.Session;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.document.DocumentsService;
import org.onehippo.jaxrs.cxf.CXFTest;
import org.onehippo.repository.jaxrs.api.SessionDataProvider;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({DocumentsService.class, DocumentTypesService.class})
public class ContentResourceTest extends CXFTest {

    private Session userSession;
    private Locale locale;
    private DocumentsService documentsService;
    private DocumentTypesService documentTypesService;

    @Before
    public void setup() {
        locale = new Locale("en");
        userSession = createMock(Session.class);
        documentsService = createMock(DocumentsService.class);
        documentTypesService = createMock(DocumentTypesService.class);

        final SessionDataProvider sessionDataProvider = createMock(SessionDataProvider.class);
        expect(sessionDataProvider.getJcrSession(anyObject())).andReturn(userSession).anyTimes();
        expect(sessionDataProvider.getLocale(anyObject())).andReturn(locale).anyTimes();
        replay(sessionDataProvider);

        PowerMock.mockStaticPartial(DocumentsService.class, "get");
        expect(DocumentsService.get()).andReturn(documentsService).anyTimes();
        PowerMock.mockStaticPartial(DocumentTypesService.class, "get");
        expect(DocumentTypesService.get()).andReturn(documentTypesService).anyTimes();
        PowerMock.replayAll();

        final CXFTest.Config config = new CXFTest.Config();
        config.addServerSingleton(new ContentResource(sessionDataProvider));
        config.addServerSingleton(new JacksonJsonProvider());

        setup(config);
    }

    @Test
    public void getPublishedDocument() throws Exception {
        final String requestedUuid = "requested-uuid";
        final String uuid = "returned-uuid";
        final Document testDocument = createDocument(uuid);

        expect(documentsService.getPublished(requestedUuid, userSession, locale)).andReturn(testDocument);
        replay(documentsService);

        final String expectedBody = normalizeJsonResource("/empty-document.json");

        when()
                .get("/documents/" + requestedUuid)
        .then()
                .statusCode(200)
                .body(equalTo(expectedBody));
    }

    @Test
    public void getPublishedDocumentNotFound() throws Exception {
        final String requestedUuid = "requested-uuid";

        expect(documentsService.getPublished(requestedUuid, userSession, locale)).andThrow(new NotFoundException());
        replay(documentsService);

        when()
                .get("/documents/" + requestedUuid)
        .then()
                .statusCode(404);
    }

    @Test
    public void createDraftDocument() throws Exception {
        final String requestedUuid = "requested-uuid";
        final String uuid = "returned-uuid";
        final Document testDocument = createDocument(uuid);

        expect(documentsService.createDraft(requestedUuid, userSession, locale)).andReturn(testDocument);
        replay(documentsService);

        final String expectedBody = normalizeJsonResource("/empty-document.json");

        when()
                .post("/documents/" + requestedUuid + "/draft")
        .then()
                .statusCode(201)
                .body(equalTo(expectedBody));
    }

    @Test
    public void createDraftDocumentForbidden() throws Exception {
        final String requestedUuid = "requested-uuid";
        final String uuid = "returned-uuid";
        final Document testDocument = createDocument(uuid);

        expect(documentsService.createDraft(requestedUuid, userSession, locale)).andThrow(new ForbiddenException(testDocument));
        replay(documentsService);

        final String expectedBody = normalizeJsonResource("/empty-document.json");

        when()
                .post("/documents/" + requestedUuid + "/draft")
        .then()
                .statusCode(403)
                .body(equalTo(expectedBody));
    }

    @Test
    public void createDraftDocumentNotFound() throws Exception {
        final String requestedUuid = "requested-uuid";

        expect(documentsService.createDraft(requestedUuid, userSession, locale)).andThrow(new NotFoundException());
        replay(documentsService);

        when()
                .post("/documents/" + requestedUuid + "/draft")
        .then()
                .statusCode(404);
    }

    @Test
    public void deleteDraft() throws Exception {
        final String requestedUuid = "requested-uuid";

        documentsService.deleteDraft(requestedUuid, userSession, locale);
        expectLastCall();
        replay(documentsService);

        when()
                .delete("/documents/" + requestedUuid + "/draft")
        .then()
                .statusCode(200);
    }

    @Test
    public void deleteDraftNotFound() throws Exception {
        final String requestedUuid = "requested-uuid";

        documentsService.deleteDraft(requestedUuid, userSession, locale);
        expectLastCall().andThrow(new NotFoundException());
        replay(documentsService);

        when()
                .delete("/documents/" + requestedUuid + "/draft")
        .then()
                .statusCode(404);
    }

    @Test
    public void deleteDraftBadRequest() throws Exception {
        final String requestedUuid = "requested-uuid";

        documentsService.deleteDraft(requestedUuid, userSession, locale);
        expectLastCall().andThrow(new BadRequestException(new ErrorInfo(ErrorInfo.Reason.ALREADY_DELETED)));
        replay(documentsService);

        when()
                .delete("/documents/" + requestedUuid + "/draft")
        .then()
                .statusCode(400)
                .body(equalTo("{\"reason\":\"ALREADY_DELETED\"}"));
    }

    @Test
    public void deleteDraftServerError() throws Exception {
        final String requestedUuid = "requested-uuid";

        documentsService.deleteDraft(requestedUuid, userSession, locale);
        expectLastCall().andThrow(new InternalServerErrorException());
        replay(documentsService);

        when()
                .delete("/documents/" + requestedUuid + "/draft")
        .then()
                .statusCode(500)
                .body(equalTo("")); // no additional ErrorInfo.
    }

    @Test
    public void retrieveDocumentType() throws Exception {
        final String requestedId = "ns:testdocument";
        final String returnedId = "ns:otherdocument";
        final DocumentType docType = new DocumentType();
        docType.setId(returnedId);

        expect(documentTypesService.getDocumentType(requestedId, userSession, locale)).andReturn(docType);
        replay(documentTypesService);

        final String expectedBody = normalizeJsonResource("/empty-documenttype.json");

        when()
                .get("/documenttypes/" + requestedId)
        .then()
                .statusCode(200)
                .header("Cache-Control", Matchers.containsString("no-cache"))
                .body(equalTo(expectedBody));
    }

    @Test
    public void documentTypeNotFound() throws Exception {
        final String requestedId = "ns:testdocument";

        expect(documentTypesService.getDocumentType(requestedId, userSession, locale))
                .andThrow(new NotFoundException());
        replay(documentTypesService);

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

    private Document createDocument(final String uuid) {
        final Document document = new Document();
        document.setId(uuid);
        return document;
    }
}
