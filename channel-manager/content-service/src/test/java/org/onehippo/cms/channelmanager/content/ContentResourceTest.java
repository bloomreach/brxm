/*
 * Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.DocumentVersionService;
import org.onehippo.cms.channelmanager.content.document.DocumentsService;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.hippoecm.hst.core.internal.BranchSelectionService;
import org.onehippo.cms.channelmanager.content.document.model.DocumentVersionInfo;
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
import org.onehippo.repository.branch.BranchConstants;
import org.onehippo.repository.jaxrs.api.SessionRequestContextProvider;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
        "com.sun.org.apache.xalan.*",
        "com.sun.org.apache.xerces.*",
        "javax.activation.*",
        "javax.management.*",
        "javax.net.ssl.*",
        "javax.xml.*",
        "org.apache.logging.log4j.*",
        "org.w3c.dom.*",
        "org.xml.*",
})
@PrepareForTest({DocumentsService.class, DocumentTypesService.class, SlugFactory.class, ContentResource.class})
public class ContentResourceTest extends CXFTest {

    private UserContext userContext;
    private DocumentsService documentsService;
    private WorkflowService workflowService;
    private DocumentTypesService documentTypesService;
    private Function<HttpServletRequest, Map<String, Serializable>> contextPayloadService;
    private BranchSelectionService branchSelectionService;
    private DocumentVersionService documentVersionService;

    @Before
    public void setup() throws Exception {
        documentsService = createMock(DocumentsService.class);
        workflowService = createMock(WorkflowService.class);
        documentTypesService = createMock(DocumentTypesService.class);
        contextPayloadService = createMock(Function.class);
        branchSelectionService = createMock(BranchSelectionService.class);
        documentVersionService = createMock(DocumentVersionService.class);

        userContext = new TestUserContext();
        final Locale locale = userContext.getLocale();
        final TimeZone timeZone = userContext.getTimeZone();
        final Session userSession = userContext.getSession();

        final SessionRequestContextProvider sessionRequestContextProvider = createMock(SessionRequestContextProvider.class);
        expect(sessionRequestContextProvider.getJcrSession(anyObject())).andReturn(userSession).anyTimes();
        expect(sessionRequestContextProvider.getLocale(anyObject())).andReturn(locale).anyTimes();
        expect(sessionRequestContextProvider.getTimeZone(anyObject())).andReturn(timeZone).anyTimes();
        expectNew(UserContext.class, userSession, locale, timeZone).andReturn(userContext);

        expect(contextPayloadService.apply(anyObject())).andStubReturn(emptyMap());
        expect(branchSelectionService.getSelectedBranchId(anyObject())).andStubReturn("master");

        PowerMock.mockStaticPartial(DocumentTypesService.class, "get");
        expect(DocumentTypesService.get()).andReturn(documentTypesService).anyTimes();

        final CXFTest.Config config = new CXFTest.Config();
        config.addServerSingleton(new ContentResource(
                sessionRequestContextProvider,
                documentsService,
                workflowService,
                contextPayloadService,
                branchSelectionService,
                documentVersionService
        ));
        config.addServerSingleton(new JacksonJsonProvider());

        setup(config);
    }

    @Test
    public void getPublishedMasterDocument() throws Exception {
        final String requestedUuid = "requested-uuid";
        final String uuid = "returned-uuid";
        final Document testDocument = createDocument(uuid);

        expect(documentsService.getDocument(eq(requestedUuid), eq("master"), eq(userContext)))
                .andReturn(testDocument);
        replayAll();

        final String expectedBody = normalizeJsonResource("/empty-document.json");
        when()
            .get("/documents/" + requestedUuid + "/master")
        .then()
            .statusCode(200)
            .body(equalTo(expectedBody));

        verifyAll();
    }

    @Test
    public void getPublishedBranchDocument() throws Exception {
        final String requestedUuid = "requested-uuid";
        final String uuid = "returned-uuid";

        final Document branchDocument = createDocument(uuid);
        final String branchId = "branchId";
        branchDocument.setBranchId(branchId);

        expect(documentsService.getDocument(eq(requestedUuid), eq(branchId), eq(userContext)))
                .andReturn(branchDocument);
        replayAll();

        final Document expected =
                when()
                    .get("/documents/" + requestedUuid + "/" + branchId)
                .then()
                    .statusCode(200)
                    .and()
                    .extract()
                    .body()
                    .as(Document.class);

        assertThat(expected.getBranchId(), is(branchId));

        verifyAll();
    }

    @Test
    public void getPublishedDocumentNotFound() throws Exception {
        final String requestedUuid = "requested-uuid";

        expect(documentsService.getDocument(eq(requestedUuid), eq("master"), eq(userContext)))
                .andThrow(new NotFoundException());
        replayAll();

        when()
                .get("/documents/" + requestedUuid + "/master")
        .then()
                .statusCode(404);

        verifyAll();
    }

    @Test
    public void obtainEditableDocument() throws Exception {
        final String requestedUuid = "requested-uuid";
        final String uuid = "returned-uuid";
        final Document testDocument = createDocument(uuid);

        expect(documentsService.obtainEditableDocument(requestedUuid, "master", userContext)).andReturn(testDocument);
        replayAll();

        final String expectedBody = normalizeJsonResource("/empty-document.json");

        when()
                .post("/documents/" + requestedUuid + "/editable")
        .then()
                .statusCode(200)
                .body(equalTo(expectedBody));

        verifyAll();
    }

    @Test
    public void obtainEditableDocumentForbidden() throws Exception {
        final String requestedUuid = "requested-uuid";

        expect(documentsService.obtainEditableDocument(requestedUuid, "master", userContext)).andThrow(new ForbiddenException());
        replayAll();

        when()
                .post("/documents/" + requestedUuid + "/editable")
        .then()
                .statusCode(403);

        verifyAll();
    }

    @Test
    public void obtainEditableDocumentNotFound() throws Exception {
        final String requestedUuid = "requested-uuid";

        expect(documentsService.obtainEditableDocument(requestedUuid, "master", userContext)).andThrow(new NotFoundException());
        replayAll();

        when()
                .post("/documents/" + requestedUuid + "/editable")
        .then()
                .statusCode(404);

        verifyAll();
    }

    @Test
    public void updateEditableDocument() throws Exception {
        final String requestedUuid = "requested-uuid";
        final String uuid = "returned-uuid";
        final Document testDocument = createDocument(uuid);

        expect(documentsService.updateEditableDocument(eq(requestedUuid), isA(Document.class), eq(userContext))).andReturn(testDocument);
        replayAll();

        final String expectedBody = normalizeJsonResource("/empty-document.json");

        given()
                .body(expectedBody)
                .contentType("application/json")
        .when()
                .put("/documents/" + requestedUuid + "/editable")
        .then()
                .statusCode(200)
                .body(equalTo(expectedBody));

        verifyAll();
    }

    @Test
    public void discardChanges() throws Exception {
        final String requestedUuid = "requested-uuid";

        documentsService.discardEditableDocument(requestedUuid, "master", userContext);
        expectLastCall();
        replayAll();

        when()
                .delete("/documents/" + requestedUuid + "/editable")
        .then()
                .statusCode(204);

        verifyAll();
    }

    @Test
    public void discardChangesNotFound() throws Exception {
        final String requestedUuid = "requested-uuid";

        documentsService.discardEditableDocument(requestedUuid, "master", userContext);
        expectLastCall().andThrow(new NotFoundException());
        replayAll();

        when()
                .delete("/documents/" + requestedUuid + "/editable")
        .then()
                .statusCode(404);

        verifyAll();
    }

    @Test
    public void discardChangesBadRequest() throws Exception {
        final String requestedUuid = "requested-uuid";

        documentsService.discardEditableDocument(requestedUuid, "master", userContext);
        expectLastCall().andThrow(new BadRequestException(new ErrorInfo(ErrorInfo.Reason.ALREADY_DELETED)));
        replayAll();

        when()
                .delete("/documents/" + requestedUuid + "/editable")
        .then()
                .statusCode(400)
                .body(equalTo("{\"reason\":\"ALREADY_DELETED\"}"));

        verifyAll();
    }

    @Test
    public void discardChangesServerError() throws Exception {
        final String requestedUuid = "requested-uuid";

        documentsService.discardEditableDocument(requestedUuid, "master", userContext);
        expectLastCall().andThrow(new InternalServerErrorException());
        replayAll();

        when()
                .delete("/documents/" + requestedUuid + "/editable")
        .then()
                .statusCode(500)
                .body(equalTo("")); // no additional ErrorInfo.

        verifyAll();
    }

    @Test
    public void retrieveDocumentType() throws Exception {
        final String requestedId = "ns:testdocument";
        final String returnedId = "ns:otherdocument";
        final DocumentType docType = new DocumentType();
        docType.setId(returnedId);

        expect(documentTypesService.getDocumentType(requestedId, userContext)).andReturn(docType);
        replayAll();

        final String expectedBody = normalizeJsonResource("/empty-documenttype.json");

        when()
                .get("/documenttypes/" + requestedId)
        .then()
                .statusCode(200)
                .header("Cache-Control", Matchers.containsString("no-cache"))
                .body(equalTo(expectedBody));

        verifyAll();
    }

    @Test
    public void documentTypeNotFound() throws Exception {
        final String requestedId = "ns:testdocument";

        expect(documentTypesService.getDocumentType(requestedId, userContext))
                .andThrow(new NotFoundException());
        replayAll();

        when()
                .get("/documenttypes/" + requestedId)
                .then()
                .statusCode(404);

        verifyAll();
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

        workflowService.executeDocumentWorkflowAction(documentId, "publish", userContext.getSession(), "master");
        expectLastCall();

        replayAll();

        when()
                .post("/workflows/documents/" + documentId + "/publish")
        .then()
                .statusCode(204);

        verifyAll();
    }

    @Test
    public void executeDocumentWorkflowActionAndDocumentNotFound() throws Exception {
        final String documentId = "uuid";

        workflowService.executeDocumentWorkflowAction(documentId, "publish", userContext.getSession(), "master");
        expectLastCall().andThrow(new NotFoundException());

        replayAll();

        when()
                .post("/workflows/documents/" + documentId + "/publish")
        .then()
                .statusCode(404);

        verifyAll();
    }

    @Test
    public void getVersions() {
        final String handleId = "uuid";
        final String branchId = BranchConstants.MASTER_BRANCH_ID;

        expect(documentVersionService.getVersionInfo(eq(handleId), eq(branchId), anyObject(), anyBoolean()))
                .andReturn(new DocumentVersionInfo(emptyList(), false, true, false, false));
        replayAll();

        when()
                .get("documents/{handleId}/{branchId}/versions", handleId, branchId)
        .then()
                .statusCode(HttpStatus.SC_OK);
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

