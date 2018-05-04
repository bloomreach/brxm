/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.DocumentsService;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.cms.channelmanager.content.slug.SlugFactory;
import org.onehippo.cms.channelmanager.content.workflows.WorkflowService;
import org.onehippo.jaxrs.cxf.CXFTest;
import org.onehippo.repository.jaxrs.api.SessionRequestContextProvider;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import static java.util.Collections.emptyMap;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.powermock.api.easymock.PowerMock.replayAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*","javax.net.ssl.*"})
@PrepareForTest({DocumentsService.class, DocumentTypesService.class, SlugFactory.class})
public class ContentResourceTest extends CXFTest {

    private Session userSession;
    private Locale locale;
    private DocumentsService documentsService;
    private WorkflowService workflowService;
    private DocumentTypesService documentTypesService;
    private Function<HttpServletRequest, Map<String, Serializable>> contextPayloadService;

    @Before
    public void setup() {
        locale = new Locale("en");
        userSession = createMock(Session.class);
        documentsService = createMock(DocumentsService.class);
        workflowService = createMock(WorkflowService.class);
        documentTypesService = createMock(DocumentTypesService.class);
        contextPayloadService = createMock(Function.class);

        final SessionRequestContextProvider sessionRequestContextProvider = createMock(SessionRequestContextProvider.class);
        expect(sessionRequestContextProvider.getJcrSession(anyObject())).andReturn(userSession).anyTimes();
        expect(sessionRequestContextProvider.getLocale(anyObject())).andReturn(locale).anyTimes();
        replay(sessionRequestContextProvider);

        expect(contextPayloadService.apply(anyObject())).andStubReturn(emptyMap());
        replay(contextPayloadService);

        PowerMock.mockStaticPartial(DocumentTypesService.class, "get");
        expect(DocumentTypesService.get()).andReturn(documentTypesService).anyTimes();
        replayAll();

        final CXFTest.Config config = new CXFTest.Config();
        config.addServerSingleton(new ContentResource(sessionRequestContextProvider, documentsService, workflowService, contextPayloadService));
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
    public void obtainEditableDocument() throws Exception {
        final String requestedUuid = "requested-uuid";
        final String uuid = "returned-uuid";
        final Document testDocument = createDocument(uuid);

        expect(documentsService.obtainEditableDocument(requestedUuid, userSession, locale, emptyMap())).andReturn(testDocument);
        replay(documentsService);

        final String expectedBody = normalizeJsonResource("/empty-document.json");

        when()
                .get("/documents/" + requestedUuid + "/editable")
        .then()
                .statusCode(200)
                .body(equalTo(expectedBody));
    }

    @Test
    public void obtainEditableDocumentForbidden() throws Exception {
        final String requestedUuid = "requested-uuid";

        expect(documentsService.obtainEditableDocument(requestedUuid, userSession, locale, emptyMap())).andThrow(new ForbiddenException());
        replay(documentsService);

        when()
                .get("/documents/" + requestedUuid + "/editable")
        .then()
                .statusCode(403);
    }

    @Test
    public void obtainEditableDocumentNotFound() throws Exception {
        final String requestedUuid = "requested-uuid";

        expect(documentsService.obtainEditableDocument(requestedUuid, userSession, locale, emptyMap())).andThrow(new NotFoundException());
        replay(documentsService);

        when()
                .get("/documents/" + requestedUuid + "/editable")
        .then()
                .statusCode(404);
    }

    @Test
    public void updateEditableDocument() throws Exception {
        final String requestedUuid = "requested-uuid";
        final String uuid = "returned-uuid";
        final Document testDocument = createDocument(uuid);
        final boolean finishEditing = false;

        expect(documentsService.updateEditableDocument(eq(requestedUuid), isA(Document.class), eq(userSession), eq(locale), eq(emptyMap()))).andReturn(testDocument);
        replay(documentsService);

        final String expectedBody = normalizeJsonResource("/empty-document.json");

        given()
                .body(expectedBody)
                .contentType("application/json")
        .when()
                .put("/documents/" + requestedUuid + "/editable")
        .then()
                .statusCode(200)
                .body(equalTo(expectedBody));
    }

    @Test
    public void discardChanges() throws Exception {
        final String requestedUuid = "requested-uuid";

        documentsService.discardEditableDocument(requestedUuid, userSession, locale, emptyMap());
        expectLastCall();
        replay(documentsService);

        when()
                .delete("/documents/" + requestedUuid + "/editable")
        .then()
                .statusCode(204);
    }

    @Test
    public void discardChangesNotFound() throws Exception {
        final String requestedUuid = "requested-uuid";

        documentsService.discardEditableDocument(requestedUuid, userSession, locale, emptyMap());
        expectLastCall().andThrow(new NotFoundException());
        replay(documentsService);

        when()
                .delete("/documents/" + requestedUuid + "/editable")
        .then()
                .statusCode(404);
    }

    @Test
    public void discardChangesBadRequest() throws Exception {
        final String requestedUuid = "requested-uuid";

        documentsService.discardEditableDocument(requestedUuid, userSession, locale, emptyMap());
        expectLastCall().andThrow(new BadRequestException(new ErrorInfo(ErrorInfo.Reason.ALREADY_DELETED)));
        replay(documentsService);

        when()
                .delete("/documents/" + requestedUuid + "/editable")
        .then()
                .statusCode(400)
                .body(equalTo("{\"reason\":\"ALREADY_DELETED\"}"));
    }

    @Test
    public void discardChangesServerError() throws Exception {
        final String requestedUuid = "requested-uuid";

        documentsService.discardEditableDocument(requestedUuid, userSession, locale, emptyMap());
        expectLastCall().andThrow(new InternalServerErrorException());
        replay(documentsService);

        when()
                .delete("/documents/" + requestedUuid + "/editable")
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

    @Test
    public void createSlugWithoutLocale() {
        PowerMock.mockStaticPartial(SlugFactory.class, "createSlug");

        expect(SlugFactory.createSlug(eq("some content name"), eq(null))).andReturn("some-content-name");
        replayAll();

        given()
                .body("some content name")
                .contentType("application/json")
        .when()
                .post("/slugs")
        .then()
                .statusCode(200)
                .body(equalTo("some-content-name"));
    }

    @Test
    public void createSlugWithLocale() {
        PowerMock.mockStaticPartial(SlugFactory.class, "createSlug");

        expect(SlugFactory.createSlug(eq("some content name"), eq("de"))).andReturn("some-content-name");
        replayAll();

        given()
                .body("some content name")
                .contentType("application/json")
        .when()
                .post("/slugs?locale=de")
        .then()
                .statusCode(200)
                .body(equalTo("some-content-name"));
    }

    @Test
    public void executeDocumentWorkflowAction() throws Exception {
        final String documentId = "uuid";

        workflowService.executeDocumentWorkflowAction(documentId, "publish", userSession, emptyMap());
        expectLastCall();

        replay(workflowService);

        when()
                .post("/workflows/documents/" + documentId + "/publish")
        .then()
                .statusCode(204);
    }

    @Test
    public void executeDocumentWorkflowActionAndDocumentNotFound() throws Exception {
        final String documentId = "uuid";

        workflowService.executeDocumentWorkflowAction(documentId, "publish", userSession, emptyMap());
        expectLastCall().andThrow(new NotFoundException());

        replay(workflowService);

        when()
                .post("/workflows/documents/" + documentId + "/publish")
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
