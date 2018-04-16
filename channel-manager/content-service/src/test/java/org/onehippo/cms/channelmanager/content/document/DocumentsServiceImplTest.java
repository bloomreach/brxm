/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.document;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.easymock.EasyMock;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.hippoecm.repository.util.WorkflowUtils.Variant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.model.PublicationState;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.model.NewDocumentInfo;
import org.onehippo.cms.channelmanager.content.document.util.DocumentNameUtils;
import org.onehippo.cms.channelmanager.content.document.util.PublicationStateUtils;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.document.util.FolderUtils;
import org.onehippo.cms.channelmanager.content.document.util.HintsInspector;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ConflictException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.MethodNotAllowed;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static java.util.Collections.emptyMap;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({
        DocumentNameUtils.class,
        PublicationStateUtils.class,
        DocumentTypesService.class,
        DocumentUtils.class,
        EditingUtils.class,
        FieldTypeUtils.class,
        FolderUtils.class,
        JcrUtils.class,
        WorkflowUtils.class
})
public class DocumentsServiceImplTest {

    private Session session;
    private Locale locale;
    private NewDocumentInfo info;
    private DocumentsServiceImpl documentsService;
    private HintsInspector hintsInspector;

    @Before
    public void setup() {
        session = createMock(Session.class);
        locale = new Locale("en");
        hintsInspector = createMock(HintsInspector.class);
        documentsService = new DocumentsServiceImpl();
        documentsService.setHintsInspector(hintsInspector);

        PowerMock.mockStatic(DocumentNameUtils.class);
        PowerMock.mockStatic(PublicationStateUtils.class);
        PowerMock.mockStatic(DocumentTypesService.class);
        PowerMock.mockStatic(DocumentUtils.class);
        PowerMock.mockStatic(EditingUtils.class);
        PowerMock.mockStatic(FieldTypeUtils.class);
        PowerMock.mockStatic(FolderUtils.class);
        PowerMock.mockStatic(JcrUtils.class);
        PowerMock.mockStatic(WorkflowUtils.class);

        info = new NewDocumentInfo();
        info.setName("Breaking News"); // the name needs to be display-name-encoded by the backend
        info.setSlug("breaking news"); // the slug needs to be URI-encoded by the backend, e.g. to "breaking-news"
        info.setTemplateQuery("new-news-document");
        info.setDocumentTypeId("project:newsdocument");
        info.setRootPath("/content/documents/channel/news");
    }

    @Test
    public void createDraftNotAHandle() throws Exception {
        final String uuid = "uuid";
        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.empty());
        replayAll();

        try {
            documentsService.createDraft(uuid, session, locale, emptyMap());
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.DOES_NOT_EXIST));
        }

        verifyAll();
    }

    @Test
    public void createDraftNoNodeType() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.createDraft(uuid, session, locale, emptyMap());
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.DOES_NOT_EXIST));
        }

        verifyAll();
    }

    @Test
    public void createDraftDeleted() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("hippo:deleted"));

        replayAll();

        try {
            documentsService.createDraft(uuid, session, locale, emptyMap());
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.DOES_NOT_EXIST));
        }

        verifyAll();
    }

    @Test
    public void createDraftNoWorkflow() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:nodetype"));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.of("Display Name"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.createDraft(uuid, session, locale, emptyMap());
            fail("No Exception");
        } catch (final MethodNotAllowed e) {
            assertTrue(e.getPayload() instanceof ErrorInfo);
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(Reason.NOT_A_DOCUMENT));
            assertThat(errorInfo.getParams().get("displayName"), equalTo("Display Name"));
        }

        verifyAll();
    }

    @Test
    public void createDraftNotEditable() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:nodetype"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canCreateDraft(emptyMap())).andReturn(false);
        expect(hintsInspector.determineEditingFailure(emptyMap(), session)).andReturn(Optional.empty());

        replayAll();
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.createDraft(uuid, session, locale, emptyMap());
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.SERVER_ERROR));
        }

        verifyAll();
    }

    @Test
    public void createDraftOtherHolder() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final ErrorInfo errorInfo = new ErrorInfo(Reason.OTHER_HOLDER);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:nodetype"));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.empty());
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andStubReturn(emptyMap());
        expect(hintsInspector.canCreateDraft(emptyMap())).andReturn(false);
        expect(hintsInspector.determineEditingFailure(emptyMap(), session)).andReturn(Optional.of(errorInfo));

        replayAll();
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.createDraft(uuid, session, locale, emptyMap());
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertThat(e.getPayload(), is(errorInfo));
        }

        verifyAll();
    }

    @Test
    public void createDraftNoDocumentNodeType() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:nodetype"));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.empty());
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap());
        expect(hintsInspector.canCreateDraft(emptyMap())).andReturn(true);

        replayAll();
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.createDraft(uuid, session, locale, emptyMap());
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.DOES_NOT_EXIST));
        }

        verifyAll();
    }

    @Test
    public void createDraftNoSession() throws Exception {
        final String uuid = "uuid";
        final String variantType = "project:newsdocument";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentTypesService documentTypesService = createMock(DocumentTypesService.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of(variantType)).anyTimes();
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(DocumentTypesService.get()).andReturn(documentTypesService);
        expect(JcrUtils.getNodePathQuietly(handle)).andReturn("/bla");
        expect(workflow.hints()).andReturn(emptyMap());
        expect(hintsInspector.canCreateDraft(emptyMap())).andReturn(true);

        expect(handle.getSession()).andThrow(new RepositoryException());

        replayAll(handle);
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.createDraft(uuid, session, locale, emptyMap());
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.SERVER_ERROR));
        }

        verifyAll();
    }

    @Test
    public void createDraftNoDocumentType() throws Exception {
        final String uuid = "uuid";
        final String variantType = "project:newsdocument";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentTypesService documentTypesService = createMock(DocumentTypesService.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of(variantType)).anyTimes();
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap());
        expect(hintsInspector.canCreateDraft(emptyMap())).andReturn(true);
        expect(DocumentTypesService.get()).andReturn(documentTypesService);
        expect(JcrUtils.getNodePathQuietly(handle)).andReturn("/bla");

        expect(handle.getSession()).andReturn(session);
        expect(documentTypesService.getDocumentType(variantType, session, locale)).andThrow(new NotFoundException());

        replayAll(handle, documentTypesService);
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.createDraft(uuid, session, locale, emptyMap());
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.SERVER_ERROR));
        }

        verifyAll();
    }

    @Test
    public void createDraftUnknownValidator() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.of("Display Name"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap());
        expect(hintsInspector.canCreateDraft(emptyMap())).andReturn(true);

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(true);

        replayAll(docType);
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.createDraft(uuid, session, locale, emptyMap());
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertTrue(e.getPayload() instanceof ErrorInfo);
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(Reason.UNKNOWN_VALIDATOR));
            assertThat(errorInfo.getParams().get("displayName"), equalTo("Display Name"));
        }

        verifyAll();
    }

    @Test
    public void createDraftFailed() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canCreateDraft(emptyMap())).andReturn(true);
        expect(EditingUtils.createDraft(workflow, session)).andReturn(Optional.empty());

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);

        replayAll(docType);
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.createDraft(uuid, session, locale, emptyMap());
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.SERVER_ERROR));
        }

        verifyAll();
    }

    @Test
    public void createDraftSuccess() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final Node unpublished = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final List<FieldType> fields = Collections.emptyList();

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.of("Display Name"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.createDraft(workflow, session)).andReturn(Optional.of(draft));
        expect(PublicationStateUtils.getPublicationState(draft)).andReturn(PublicationState.NEW);
        expect(workflow.hints()).andReturn(emptyMap());
        expect(hintsInspector.canCreateDraft(emptyMap())).andReturn(true);
        FieldTypeUtils.readFieldValues(eq(draft), eq(fields), isA(Map.class));
        expectLastCall();

        expect(docType.getId()).andReturn("document:type");
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(fields).anyTimes();

        expect(WorkflowUtils.getDocumentVariantNode(eq(handle), eq(Variant.UNPUBLISHED))).andReturn(Optional.of(unpublished));
        expect(JcrUtils.getNodeNameQuietly(eq(handle))).andReturn("url-name");
        expect(JcrUtils.getNodePathQuietly(eq(handle))).andReturn("/content/documents/test/url-name");
        FieldTypeUtils.readFieldValues(eq(unpublished), eq(fields), isA(Map.class));
        expectLastCall();

        replayAll(docType);
        EasyMock.replay(hintsInspector, workflow);

        final Document document = documentsService.createDraft(uuid, session, locale, emptyMap());
        assertThat(document.getId(), equalTo("uuid"));
        assertThat(document.getUrlName(), equalTo("url-name"));
        assertThat(document.getDisplayName(), equalTo("Display Name"));
        assertThat(document.getRepositoryPath(), equalTo("/content/documents/test/url-name"));
        assertThat(document.getInfo().getType().getId(), equalTo("document:type"));
        assertThat(document.getInfo().isDirty(), equalTo(false));
        assertThat(document.getInfo().getPublicationState(), equalTo(PublicationState.NEW));

        verifyAll();
    }

    @Test
    public void createDirtyDraft() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final Node unpublished = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final List<FieldType> fields = Collections.emptyList();

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));

        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.of("Display Name"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.createDraft(workflow, session)).andReturn(Optional.of(draft));
        expect(PublicationStateUtils.getPublicationState(draft)).andReturn(PublicationState.NEW);
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canCreateDraft(emptyMap())).andReturn(true);
        FieldTypeUtils.readFieldValues(eq(draft), eq(fields), isA(Map.class));
        expectLastCall();

        expect(docType.getId()).andReturn("document:type");
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(fields);

        expect(WorkflowUtils.getDocumentVariantNode(eq(handle), eq(Variant.UNPUBLISHED))).andReturn(Optional.of(unpublished));
        expect(JcrUtils.getNodeNameQuietly(eq(handle))).andReturn("url-name");
        expect(JcrUtils.getNodePathQuietly(eq(handle))).andReturn("/content/documents/test/url-name");
        expect(docType.getFields()).andReturn(fields);
        FieldTypeUtils.readFieldValues(eq(unpublished), eq(fields), isA(Map.class));
        expectLastCall().andAnswer(() -> ((Map) getCurrentArguments()[2]).put("extraField", new FieldValue("value")));

        replayAll(docType);
        EasyMock.replay(hintsInspector, workflow);

        final Document document = documentsService.createDraft(uuid, session, locale, emptyMap());
        assertThat(document.getId(), equalTo("uuid"));
        assertThat(document.getUrlName(), equalTo("url-name"));
        assertThat(document.getDisplayName(), equalTo("Display Name"));
        assertThat(document.getRepositoryPath(), equalTo("/content/documents/test/url-name"));
        assertThat(document.getInfo().getType().getId(), equalTo("document:type"));
        assertThat(document.getInfo().isDirty(), equalTo(true));
        assertThat(document.getInfo().getPublicationState(), equalTo(PublicationState.NEW));

        verifyAll();
    }

    @Test
    public void updateDraftNotAHandle() throws Exception {
        final Document document = new Document();
        final String uuid = "uuid";

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.updateDraft(uuid, document, session, locale, emptyMap());
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.DOES_NOT_EXIST));
        }

        verifyAll();
    }

    @Test
    public void updateDraftNoWorkflow() throws Exception {
        final Document document = new Document();
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.empty());
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.updateDraft(uuid, document, session, locale, emptyMap());
            fail("No Exception");
        } catch (final MethodNotAllowed e) {
            assertTrue(e.getPayload() instanceof ErrorInfo);
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(Reason.NOT_A_DOCUMENT));
        }

        verifyAll();
    }

    @Test
    public void updateDraftVariantNotFound() throws Exception {
        final Document document = new Document();
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.updateDraft(uuid, document, session, locale, emptyMap());
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.DOES_NOT_EXIST));
        }

        verifyAll();
    }

    @Test
    public void updateDraftNotEditing() throws Exception {
        final Document document = new Document();
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDraft(emptyMap())).andReturn(false);
        expect(hintsInspector.determineEditingFailure(emptyMap(), session)).andReturn(Optional.empty());

        replayAll();
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.updateDraft(uuid, document, session, locale, emptyMap());
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertTrue(e.getPayload() instanceof ErrorInfo);
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(Reason.NO_HOLDER));
            assertNull(errorInfo.getParams());
        }

        verifyAll();
    }

    @Test
    public void updateDraftOtherHolder() throws Exception {
        final Document document = new Document();
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final ErrorInfo errorInfo = new ErrorInfo(Reason.OTHER_HOLDER);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDraft(emptyMap())).andReturn(false);
        expect(hintsInspector.determineEditingFailure(emptyMap(), session)).andReturn(Optional.of(errorInfo));

        replayAll();
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.updateDraft(uuid, document, session, locale, emptyMap());
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertThat(e.getPayload(), equalTo(errorInfo));
        }

        verifyAll();
    }

    @Test
    public void updateDraftNoDocumentType() throws Exception {
        final Document document = new Document();
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.empty());
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap());
        expect(hintsInspector.canUpdateDraft(emptyMap())).andReturn(true);

        replayAll();
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.updateDraft(uuid, document, session, locale, emptyMap());
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.DOES_NOT_EXIST));
        }

        verifyAll();
    }

    @Test
    public void updateDraftUnknownValidator() throws Exception {
        final Document document = new Document();
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDraft(emptyMap())).andReturn(true);

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(true);

        replayAll(docType);
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.updateDraft(uuid, document, session, locale, emptyMap());
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.UNKNOWN_VALIDATOR));
        }

        verifyAll();
    }

    @Test
    public void updateDraftWriteFailure() throws Exception {
        final Document document = new Document();
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final BadRequestException badRequest = new BadRequestException();

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDraft(emptyMap())).andReturn(true);
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall().andThrow(badRequest);

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);

        expect(docType.getFields()).andReturn(Collections.emptyList());

        replayAll(docType);
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.updateDraft(uuid, document, session, locale, emptyMap());
            fail("No Exception");
        } catch (final BadRequestException e) {
            assertThat(e, equalTo(badRequest));
        }

        verifyAll();
    }

    @Test
    public void updateDraftSaveFailure() throws Exception {
        final Document document = new Document();
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDraft(emptyMap())).andReturn(true);
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList());
        session.save();
        expectLastCall().andThrow(new RepositoryException());

        replayAll(docType, session);
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.updateDraft(uuid, document, session, locale, emptyMap());
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.SERVER_ERROR));
        }

        verifyAll();
    }

    @Test
    public void updateDraftValidationFailure() throws Exception {
        final Document document = new Document();
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();
        expect(FieldTypeUtils.validateFieldValues(document.getFields(), Collections.emptyList())).andReturn(false);
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDraft(emptyMap())).andReturn(true);

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList()).anyTimes();
        session.save();
        expectLastCall();

        replayAll(docType, session);
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.updateDraft(uuid, document, session, locale, emptyMap());
            fail("No Exception");
        } catch (final BadRequestException e) {
            assertThat(e.getPayload(), equalTo(document));
        }

        verifyAll();
    }

    @Test
    public void updateDraftCopyToPreviewFailure() throws Exception {
        final Document document = new Document();
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.copyToPreviewAndKeepEditing(workflow, session)).andReturn(Optional.empty());
        expect(hintsInspector.canUpdateDraft(emptyMap())).andReturn(true);
        expect(hintsInspector.determineEditingFailure(emptyMap(), session)).andReturn(Optional.empty());
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();
        expect(FieldTypeUtils.validateFieldValues(document.getFields(), Collections.emptyList())).andReturn(true);
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList()).anyTimes();
        session.save();
        expectLastCall();

        replayAll(docType, session);
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.updateDraft(uuid, document, session, locale, emptyMap());
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertTrue(e.getPayload() instanceof ErrorInfo);
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(Reason.NO_HOLDER));
            assertNull(errorInfo.getParams());
        }

        verifyAll();
    }

    @Test
    public void updateDraftSuccess() throws Exception {
        final Document document = new Document();
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDraft(emptyMap())).andReturn(true);
        expect(EditingUtils.copyToPreviewAndKeepEditing(workflow, session)).andReturn(Optional.of(draft));
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();
        expect(FieldTypeUtils.validateFieldValues(document.getFields(), Collections.emptyList())).andReturn(true);
        expect(PublicationStateUtils.getPublicationState(draft)).andReturn(PublicationState.CHANGED);
        expect(docType.getFields()).andReturn(Collections.emptyList());
        FieldTypeUtils.readFieldValues(draft, Collections.emptyList(), document.getFields());
        expectLastCall();

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList()).anyTimes();
        session.save();
        expectLastCall();

        replayAll(docType, session);
        EasyMock.replay(hintsInspector, workflow);

        final Document persistedDocument = documentsService.updateDraft(uuid, document, session, locale, emptyMap());

        assertThat(persistedDocument, equalTo(document));
        assertThat(persistedDocument.getInfo().getPublicationState(), equalTo(PublicationState.CHANGED));
        verifyAll();
    }

    @Test
    public void updateDirtyDraftSuccess() throws Exception {
        final Document document = new Document();
        document.getInfo().setDirty(true);

        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDraft(emptyMap())).andReturn(true);
        expect(EditingUtils.copyToPreviewAndKeepEditing(workflow, session)).andReturn(Optional.of(draft));
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();
        expect(FieldTypeUtils.validateFieldValues(document.getFields(), Collections.emptyList())).andReturn(true);

        expect(PublicationStateUtils.getPublicationState(draft)).andReturn(PublicationState.CHANGED);
        expect(docType.getFields()).andReturn(Collections.emptyList());
        FieldTypeUtils.readFieldValues(draft, Collections.emptyList(), document.getFields());
        expectLastCall();

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList()).anyTimes();
        session.save();
        expectLastCall();

        replayAll(docType, session);
        EasyMock.replay(hintsInspector, workflow);

        final Document persistedDocument = documentsService.updateDraft(uuid, document, session, locale, emptyMap());

        assertThat(persistedDocument.getId(), equalTo(document.getId()));
        assertThat(persistedDocument.getDisplayName(), equalTo(document.getDisplayName()));
        assertThat(persistedDocument.getFields(), equalTo(document.getFields()));
        assertThat(persistedDocument.getInfo().isDirty(), equalTo(false));
        assertThat(persistedDocument.getInfo().getPublicationState(), equalTo(PublicationState.CHANGED));
        verifyAll();
    }

    @Test
    public void updateDraftFieldNotAHandle() throws Exception {
        final String uuid = "uuid";
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale, emptyMap());
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.DOES_NOT_EXIST));
        }

        verifyAll();
    }

    @Test
    public void updateDraftFieldNoWorkflow() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.empty());
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale, emptyMap());
            fail("No Exception");
        } catch (final MethodNotAllowed e) {
            assertTrue(e.getPayload() instanceof ErrorInfo);
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(Reason.NOT_A_DOCUMENT));
        }

        verifyAll();
    }

    @Test
    public void updateDraftFieldVariantNotFound() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale, emptyMap());
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.DOES_NOT_EXIST));
        }

        verifyAll();
    }

    @Test
    public void updateDraftFieldNotEditing() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDraft(emptyMap())).andReturn(false);
        expect(hintsInspector.determineEditingFailure(emptyMap(), session)).andReturn(Optional.empty());

        replayAll();
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale, emptyMap());
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertTrue(e.getPayload() instanceof ErrorInfo);
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(Reason.NO_HOLDER));
            assertNull(errorInfo.getParams());
        }

        verifyAll();
    }

    @Test
    public void updateDraftFieldOtherHolder() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final ErrorInfo errorInfo = new ErrorInfo(Reason.OTHER_HOLDER);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDraft(emptyMap())).andReturn(false);
        expect(hintsInspector.determineEditingFailure(emptyMap(), session)).andReturn(Optional.of(errorInfo));

        replayAll();
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale, emptyMap());
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertThat(e.getPayload(), equalTo(errorInfo));
        }

        verifyAll();
    }

    @Test
    public void updateDraftFieldNoDocumentType() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.empty());
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDraft(emptyMap())).andReturn(true);

        replayAll();
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale, emptyMap());
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.DOES_NOT_EXIST));
        }

        verifyAll();
    }

    @Test
    public void updateDraftFieldUnknownValidator() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDraft(emptyMap())).andReturn(true);

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(true);

        replayAll(docType);
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale, emptyMap());
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.UNKNOWN_VALIDATOR));
        }

        verifyAll();
    }

    @Test
    public void updateDraftFieldWriteFailure() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldType> fields = Collections.emptyList();
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));
        final BadRequestException badRequest = new BadRequestException();

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDraft(emptyMap())).andReturn(true);
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList());

        expect(FieldTypeUtils.writeFieldValue(fieldPath, fieldValues, fields, draft)).andThrow(badRequest);

        replayAll(docType);
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale, emptyMap());
            fail("No Exception");
        } catch (final BadRequestException e) {
            assertThat(e, equalTo(badRequest));
        }

        verifyAll();
    }

    @Test
    public void updateDraftFieldNotSavedWhenUnknown() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));
        final List<FieldType> fields = Collections.emptyList();

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap());
        expect(hintsInspector.canUpdateDraft(emptyMap())).andReturn(true);
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(fields);
        expect(FieldTypeUtils.writeFieldValue(fieldPath, fieldValues, fields, draft)).andReturn(false);

        replayAll(docType, session);
        EasyMock.replay(hintsInspector, workflow);

        documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale, emptyMap());

        verifyAll();
    }

    @Test
    public void updateDraftFieldSuccess() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));
        final List<FieldType> fields = Collections.emptyList();

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDraft(emptyMap())).andReturn(true);
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(fields);
        expect(FieldTypeUtils.writeFieldValue(fieldPath, fieldValues, fields, draft)).andReturn(true);

        session.save();
        expectLastCall();

        replayAll(docType, session);
        EasyMock.replay(hintsInspector, workflow);

        documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale, emptyMap());

        verifyAll();
    }

    @Test
    public void updateDraftFieldSaveFailure() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));
        final List<FieldType> fields = Collections.emptyList();

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDraft(emptyMap())).andReturn(true);
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList());
        expect(FieldTypeUtils.writeFieldValue(fieldPath, fieldValues, fields, draft)).andReturn(true);

        session.save();
        expectLastCall().andThrow(new RepositoryException());

        replayAll(docType, session);
        EasyMock.replay(hintsInspector, workflow);

        try {
            documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale, emptyMap());
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.SERVER_ERROR));
        }

        verifyAll();
    }

    @Test
    public void deleteDraftNotAHandle() throws Exception {
        final String uuid = "uuid";

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.deleteDraft(uuid, session, locale, emptyMap());
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.DOES_NOT_EXIST));
        }

        verifyAll();
    }

    @Test
    public void deleteDraftNoWorkflow() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.empty());
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.deleteDraft(uuid, session, locale, emptyMap());
            fail("No Exception");
        } catch (final MethodNotAllowed e) {
            assertTrue(e.getPayload() instanceof ErrorInfo);
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(Reason.NOT_A_DOCUMENT));
        }

        verifyAll();
    }

    @Test
    public void deleteDraftNotDeletable() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap());
        expect(hintsInspector.canDeleteDraft(emptyMap())).andReturn(false);

        replayAll();
        replay(hintsInspector, workflow);

        try {
            documentsService.deleteDraft(uuid, session, locale, emptyMap());
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertTrue(e.getPayload() instanceof ErrorInfo);
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(Reason.ALREADY_DELETED));
        }

        verifyAll();
    }

    @Test
    public void deleteDraftDisposeFailure() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canDeleteDraft(emptyMap())).andReturn(true);

        expect(workflow.disposeEditableInstance()).andThrow(new WorkflowException("bla"));

        replayAll(workflow);
        EasyMock.replay(hintsInspector);

        try {
            documentsService.deleteDraft(uuid, session, locale, emptyMap());
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(Reason.SERVER_ERROR));
        }

        verifyAll();
    }

    @Test
    public void deleteDraftSuccess() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canDeleteDraft(emptyMap())).andReturn(true);

        expect(workflow.disposeEditableInstance()).andReturn(null);

        replayAll();
        EasyMock.replay(hintsInspector, workflow);

        documentsService.deleteDraft(uuid, session, locale, emptyMap());

        verifyAll();
    }


    @Test
    public void getPublished() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node published = createMock(Node.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.of("Document Display Name"));
        expect(JcrUtils.getNodeNameQuietly(eq(handle))).andReturn("document-url-name");
        expect(JcrUtils.getNodePathQuietly(eq(handle))).andReturn("/content/documents/test/url-name");
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.PUBLISHED)).andReturn(Optional.of(published));
        expect(PublicationStateUtils.getPublicationState(published)).andReturn(PublicationState.LIVE);
        FieldTypeUtils.readFieldValues(eq(published), eq(Collections.emptyList()), isA(Map.class));
        expectLastCall();

        expect(docType.getId()).andReturn("document:type");
        expect(docType.getFields()).andReturn(Collections.emptyList());

        replayAll(docType);

        final Document document = documentsService.getPublished(uuid, session, locale);

        assertThat(document.getUrlName(), equalTo("document-url-name"));
        assertThat(document.getDisplayName(), equalTo("Document Display Name"));
        assertThat(document.getRepositoryPath(), equalTo("/content/documents/test/url-name"));
        assertThat(document.getInfo().getPublicationState(), equalTo(PublicationState.LIVE));

        verifyAll();
    }

    @Test(expected = BadRequestException.class)
    public void createDocumentWithoutName() throws Exception {
        info.setName("");
        documentsService.createDocument(info, session, locale);
    }

    @Test(expected = BadRequestException.class)
    public void createDocumentWithoutSlug() throws Exception {
        info.setSlug("");
        documentsService.createDocument(info, session, locale);
    }

    @Test(expected = BadRequestException.class)
    public void createDocumentWithoutTemplateQuery() throws Exception {
        info.setTemplateQuery("");
        documentsService.createDocument(info, session, locale);
    }

    @Test(expected = BadRequestException.class)
    public void createDocumentWithoutDocumentTypeId() throws Exception {
        info.setDocumentTypeId("");
        documentsService.createDocument(info, session, locale);
    }

    @Test(expected = BadRequestException.class)
    public void createDocumentWithoutRootPath() throws Exception {
        info.setRootPath("");
        documentsService.createDocument(info, session, locale);
    }

    @Test
    public void createDocumentWithExistingName() throws Exception {
        final Node folderNode = createMock(Node.class);
        expect(FolderUtils.getFolder(eq("/content/documents/channel/news"), eq(session)))
                .andReturn(folderNode);
        expect(FolderUtils.getLocale(folderNode))
                .andReturn("en_GB");
        expect(DocumentNameUtils.encodeDisplayName(eq("Breaking News"), eq("en_GB")))
                .andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News (encoded)")))
                .andReturn(true);
        replayAll(folderNode);

        try {
            documentsService.createDocument(info, session, locale);
            fail("No Exception");
        } catch (final ConflictException e) {
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(Reason.NAME_ALREADY_EXISTS));
        }

        verifyAll();
    }

    @Test
    public void createDocumentWithExistingSlug() throws Exception {
        final Node folderNode = createMock(Node.class);
        expect(FolderUtils.getFolder(eq("/content/documents/channel/news"), eq(session)))
                .andReturn(folderNode);
        expect(FolderUtils.getLocale(folderNode))
                .andReturn("en_GB");
        expect(DocumentNameUtils.encodeDisplayName(eq("Breaking News"), eq("en_GB")))
                .andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News (encoded)")))
                .andReturn(false);
        expect(DocumentNameUtils.encodeUrlName(eq("breaking news"), eq("en_GB")))
                .andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folderNode), eq("breaking-news")))
                .andReturn(true);
        replayAll(folderNode);

        try {
            documentsService.createDocument(info, session, locale);
            fail("No Exception");
        } catch (final ConflictException e) {
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(Reason.SLUG_ALREADY_EXISTS));
        }

        verifyAll();
    }

    @Test
    public void createDocumentNoWorkflow() throws Exception {
        final Node folderNode = createMock(Node.class);
        expect(FolderUtils.getFolder(eq("/content/documents/channel/news"), eq(session)))
                .andReturn(folderNode);
        expect(FolderUtils.getLocale(folderNode))
                .andReturn("en_GB");
        expect(DocumentNameUtils.encodeDisplayName(eq("Breaking News"), eq("en_GB")))
                .andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News (encoded)")))
                .andReturn(false);
        expect(DocumentNameUtils.encodeUrlName(eq("breaking news"), eq("en_GB")))
                .andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folderNode), eq("breaking-news")))
                .andReturn(false);
        expect(WorkflowUtils.getWorkflow(eq(folderNode), eq("internal"), eq(FolderWorkflow.class)))
                .andReturn(Optional.empty());
        expect(DocumentUtils.getDisplayName(folderNode))
                .andReturn(Optional.of("News"));
        replayAll(folderNode);

        try {
            documentsService.createDocument(info, session, locale);
            fail("No Exception");
        } catch (final MethodNotAllowed e) {
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(Reason.NOT_A_FOLDER));
            assertThat(errorInfo.getParams().get("displayName"), equalTo("News"));
        }

        verifyAll();
    }

    @Test(expected = InternalServerErrorException.class)
    public void createDocumentWorkflowThrowsException() throws Exception {
        final Node folderNode = createMock("folder", Node.class);
        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);

        expect(FolderUtils.getFolder(eq("/content/documents/channel/news"), eq(session)))
                .andReturn(folderNode);
        expect(FolderUtils.getLocale(folderNode))
                .andReturn("en_GB");
        expect(DocumentNameUtils.encodeDisplayName(eq("Breaking News"), eq("en_GB")))
                .andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News (encoded)")))
                .andReturn(false);
        expect(DocumentNameUtils.encodeUrlName(eq("breaking news"), eq("en_GB")))
                .andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folderNode), eq("breaking-news")))
                .andReturn(false);
        expect(WorkflowUtils.getWorkflow(eq(folderNode), eq("internal"), eq(FolderWorkflow.class)))
                .andReturn(Optional.of(folderWorkflow));
        expect(folderWorkflow.add(eq("new-news-document"), eq("project:newsdocument"), eq("breaking-news")))
                .andThrow(new RepositoryException());
        expect(JcrUtils.getNodePathQuietly(folderNode)).andReturn("/content/documents/channel/news");

        replayAll(folderNode, folderWorkflow);

        documentsService.createDocument(info, session, locale);
    }

    @Test
    public void createDocumentInRootPath() throws Exception {
        final Node folderNode = createMock("folder", Node.class);
        final Node documentHandle = createMock("documentHandle", Node.class);
        final Node documentDraft = createMock("documentDraft", Node.class);
        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);

        expect(FolderUtils.getFolder(eq("/content/documents/channel/news"), eq(session)))
                .andReturn(folderNode);
        expect(FolderUtils.getLocale(folderNode))
                .andReturn("en_GB");
        expect(DocumentNameUtils.encodeDisplayName(eq("Breaking News"), eq("en_GB")))
                .andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News (encoded)")))
                .andReturn(false);
        expect(DocumentNameUtils.encodeUrlName(eq("breaking news"), eq("en_GB")))
                .andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folderNode), eq("breaking-news")))
                .andReturn(false);
        expect(WorkflowUtils.getWorkflow(eq(folderNode), eq("internal"), eq(FolderWorkflow.class)))
                .andReturn(Optional.of(folderWorkflow));
        expect(folderWorkflow.add(eq("new-news-document"), eq("project:newsdocument"), eq("breaking-news")))
                .andReturn("/content/documents/channel/news/breaking-news/breaking-news");
        expect(session.getNode(eq("/content/documents/channel/news/breaking-news/breaking-news")))
                .andReturn(documentDraft);
        expect(documentDraft.getParent()).andReturn(documentHandle);

        DocumentNameUtils.setDisplayName(eq(documentHandle), eq("Breaking News (encoded)"));
        expectLastCall();

        expect(WorkflowUtils.getDocumentVariantNode(eq(documentHandle), eq(Variant.DRAFT)))
                .andReturn(Optional.of(documentDraft));

        expect(documentHandle.getIdentifier()).andReturn("uuid");

        final DocumentType docType = provideDocumentType(documentHandle);
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getId()).andReturn("project:newsdocument");
        expect(DocumentUtils.getDisplayName(documentHandle)).andReturn(Optional.of("Breaking News (encoded)"));
        expect(JcrUtils.getNodeNameQuietly(eq(documentHandle))).andReturn("breaking-news");
        expect(JcrUtils.getNodePathQuietly(eq(documentHandle))).andReturn("/content/documents/news/breaking-news");
        expect(PublicationStateUtils.getPublicationState(documentDraft)).andReturn(PublicationState.LIVE);

        session.save();
        expectLastCall();

        final List<FieldType> fields = Collections.emptyList();
        expect(docType.getFields()).andReturn(fields);
        FieldTypeUtils.readFieldValues(eq(documentDraft), eq(fields), isA(Map.class));
        expectLastCall();

        replayAll(folderNode, documentDraft, folderWorkflow, docType, session);

        final Document document = documentsService.createDocument(info, session, locale);

        assertThat(document.getId(), equalTo("uuid"));
        assertThat(document.getUrlName(), equalTo("breaking-news"));
        assertThat(document.getDisplayName(), equalTo("Breaking News (encoded)"));
        assertThat(document.getRepositoryPath(), equalTo("/content/documents/news/breaking-news"));
        assertThat(document.getInfo().getPublicationState(), equalTo(PublicationState.LIVE));
        assertThat(document.getFields().size(), equalTo(0));

        verifyAll();
    }

    @Test
    public void createDocumentInDefaultPath() throws Exception {
        final Node rootFolderNode = createMock("rootFolder", Node.class);
        final Node folderNode = createMock("folder", Node.class);
        final Node documentHandle = createMock("documentHandle", Node.class);
        final Node documentDraft = createMock("documentDraft", Node.class);
        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);

        info.setDefaultPath("2017/11");

        expect(FolderUtils.getFolder(eq("/content/documents/channel/news"), eq(session)))
                .andReturn(rootFolderNode);
        expect(FolderUtils.getOrCreateFolder(eq(rootFolderNode), eq("2017/11"), eq(session)))
                .andReturn(folderNode);
        expect(FolderUtils.getLocale(folderNode))
                .andReturn("en_GB");
        expect(DocumentNameUtils.encodeDisplayName(eq("Breaking News"), eq("en_GB")))
                .andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News (encoded)")))
                .andReturn(false);
        expect(DocumentNameUtils.encodeUrlName(eq("breaking news"), eq("en_GB")))
                .andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folderNode), eq("breaking-news")))
                .andReturn(false);
        expect(WorkflowUtils.getWorkflow(eq(folderNode), eq("internal"), eq(FolderWorkflow.class)))
                .andReturn(Optional.of(folderWorkflow));
        expect(folderWorkflow.add(eq("new-news-document"), eq("project:newsdocument"), eq("breaking-news")))
                .andReturn("/content/documents/channel/news/breaking-news/breaking-news");
        expect(session.getNode(eq("/content/documents/channel/news/breaking-news/breaking-news")))
                .andReturn(documentDraft);
        expect(documentDraft.getParent()).andReturn(documentHandle);

        DocumentNameUtils.setDisplayName(eq(documentHandle), eq("Breaking News (encoded)"));
        expectLastCall();

        expect(WorkflowUtils.getDocumentVariantNode(eq(documentHandle), eq(Variant.DRAFT)))
                .andReturn(Optional.of(documentDraft));
        expect(documentHandle.getName()).andReturn("breaking-news");
        expect(documentHandle.getIdentifier()).andReturn("uuid");

        final DocumentType docType = provideDocumentType(documentHandle);
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getId()).andReturn("project:newsdocument");
        expect(DocumentUtils.getDisplayName(documentHandle)).andReturn(Optional.of("Breaking News (encoded)"));
        expect(JcrUtils.getNodeNameQuietly(eq(documentHandle))).andReturn("breaking-news");
        expect(JcrUtils.getNodePathQuietly(eq(documentHandle))).andReturn("/content/documents/news/breaking-news");
        expect(PublicationStateUtils.getPublicationState(documentDraft)).andReturn(PublicationState.NEW);

        session.save();
        expectLastCall();

        final List<FieldType> fields = Collections.emptyList();
        expect(docType.getFields()).andReturn(fields);
        FieldTypeUtils.readFieldValues(eq(documentDraft), eq(fields), isA(Map.class));
        expectLastCall();

        replayAll(folderNode, documentDraft, folderWorkflow, docType, session);

        final Document document = documentsService.createDocument(info, session, locale);

        assertThat(document.getId(), equalTo("uuid"));
        assertThat(document.getUrlName(), equalTo("breaking-news"));
        assertThat(document.getDisplayName(), equalTo("Breaking News (encoded)"));
        assertThat(document.getRepositoryPath(), equalTo("/content/documents/news/breaking-news"));
        assertThat(document.getFields().size(), equalTo(0));
        assertThat(document.getInfo().getPublicationState(), equalTo(PublicationState.NEW));

        verifyAll();
    }

    @Test(expected = BadRequestException.class)
    public void updateDocumentNamesWithoutDisplayName() throws Exception {
        final Document document = new Document();
        documentsService.updateDocumentNames("uuid", document, session);
    }

    @Test(expected = BadRequestException.class)
    public void updateDocumentNamesWithoutUrlName() throws Exception {
        final Document document = new Document();
        document.setDisplayName("Breaking News");
        documentsService.updateDocumentNames("uuid", document, session);
    }

    @Test(expected = BadRequestException.class)
    public void updateDocumentNamesNotAHandle() throws Exception {
        final String uuid = "uuid";
        final Document document = new Document();

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.empty());
        replayAll();

        documentsService.updateDocumentNames(uuid, document, session);
    }

    @Test
    public void updateDocumentNamesUrlNameClashes() throws Exception {
        final String uuid = "uuid";
        final String urlName = "new name";
        final String encodedUrlName = "new-name";
        final Document document = new Document();
        document.setDisplayName("New Name");
        document.setUrlName(urlName);
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final String folderLocale = "en";

        expect(DocumentUtils.getHandle(eq(uuid), eq(session))).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("project:newsdocument"));
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(FolderUtils.getLocale(eq(folder))).andReturn(folderLocale);
        expect(JcrUtils.getNodePathQuietly(eq(handle))).andReturn("/content/documents/channel/news/breaking-news");
        expect(DocumentNameUtils.encodeUrlName(eq(urlName), eq(folderLocale))).andReturn(encodedUrlName);
        expect(DocumentNameUtils.getUrlName(eq(handle))).andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folder), eq(encodedUrlName))).andReturn(true);
        replayAll();

        assertUpdateDocumentNamesFails(uuid, document, Reason.SLUG_ALREADY_EXISTS);
    }

    @Test
    public void updateDocumentNamesUrlNameOnly() throws Exception {
        final String uuid = "uuid";
        final String displayName = "Breaking News";
        final String encodedDisplayName = "Breaking News (encoded)";
        final String urlName = "new name";
        final String encodedUrlName = "new-name";
        final Document document = new Document();
        document.setDisplayName(displayName);
        document.setUrlName(urlName);
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final String folderLocale = "en";

        expect(DocumentUtils.getHandle(eq(uuid), eq(session))).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("project:newsdocument"));
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(FolderUtils.getLocale(eq(folder))).andReturn(folderLocale);
        expect(JcrUtils.getNodePathQuietly(eq(handle))).andReturn("/content/documents/channel/news/breaking-news");

        expect(DocumentNameUtils.encodeUrlName(eq(urlName), eq(folderLocale))).andReturn(encodedUrlName);
        expect(DocumentNameUtils.getUrlName(eq(handle))).andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folder), eq(encodedUrlName))).andReturn(false);

        expect(DocumentNameUtils.encodeDisplayName(eq(displayName), eq(folderLocale))).andReturn(encodedDisplayName);
        expect(DocumentNameUtils.getDisplayName(eq(handle))).andReturn(encodedDisplayName);

        DocumentNameUtils.setUrlName(eq(handle), eq(encodedUrlName));
        expectLastCall();

        replayAll();

        final Document result = documentsService.updateDocumentNames(uuid, document, session);
        assertThat(result.getDisplayName(), equalTo(displayName));
        assertThat(result.getUrlName(), equalTo(encodedUrlName));
    }

    @Test
    public void updateDocumentNamesDisplayNameOnlyAndClashes() throws Exception {
        final String uuid = "uuid";
        final String displayName = "New Name";
        final String encodedDisplayName = "New Name (encoded)";
        final String urlName = "breaking news";
        final String encodedUrlName = "breaking-news";
        final Document document = new Document();
        document.setDisplayName(displayName);
        document.setUrlName(urlName);
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final String folderLocale = "en";

        expect(DocumentUtils.getHandle(eq(uuid), eq(session))).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("project:newsdocument"));
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(FolderUtils.getLocale(eq(folder))).andReturn(folderLocale);
        expect(JcrUtils.getNodePathQuietly(eq(handle))).andReturn("/content/documents/channel/news/breaking-news");

        expect(DocumentNameUtils.encodeUrlName(eq(urlName), eq(folderLocale))).andReturn(encodedUrlName);
        expect(DocumentNameUtils.getUrlName(eq(handle))).andReturn(encodedUrlName);

        expect(DocumentNameUtils.encodeDisplayName(eq(displayName), eq(folderLocale))).andReturn(encodedDisplayName);
        expect(DocumentNameUtils.getDisplayName(eq(handle))).andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folder), eq(encodedDisplayName))).andReturn(true);

        replayAll();

        assertUpdateDocumentNamesFails(uuid, document, Reason.NAME_ALREADY_EXISTS);
    }

    @Test
    public void updateDocumentNamesDisplayNameOnly() throws Exception {
        final String uuid = "uuid";
        final String displayName = "New Name";
        final String encodedDisplayName = "New Name (encoded)";
        final String urlName = "breaking news";
        final String encodedUrlName = "breaking-news";
        final Document document = new Document();
        document.setDisplayName(displayName);
        document.setUrlName(urlName);
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final String folderLocale = "en";

        expect(DocumentUtils.getHandle(eq(uuid), eq(session))).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("project:newsdocument"));
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(FolderUtils.getLocale(eq(folder))).andReturn(folderLocale);
        expect(JcrUtils.getNodePathQuietly(eq(handle))).andReturn("/content/documents/channel/news/breaking-news");

        expect(DocumentNameUtils.encodeUrlName(eq(urlName), eq(folderLocale))).andReturn(encodedUrlName);
        expect(DocumentNameUtils.getUrlName(eq(handle))).andReturn(encodedUrlName);

        expect(DocumentNameUtils.encodeDisplayName(eq(displayName), eq(folderLocale))).andReturn(encodedDisplayName);
        expect(DocumentNameUtils.getDisplayName(eq(handle))).andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folder), eq(encodedDisplayName))).andReturn(false);

        DocumentNameUtils.setDisplayName(eq(handle), eq(encodedDisplayName));
        expectLastCall();

        replayAll();

        final Document result = documentsService.updateDocumentNames(uuid, document, session);
        assertThat(result.getDisplayName(), equalTo(encodedDisplayName));
        assertThat(result.getUrlName(), equalTo(urlName));
    }

    @Test
    public void updateDocumentNamesBoth() throws Exception {
        final String uuid = "uuid";
        final String displayName = "New Name";
        final String encodedDisplayName = "New Name (encoded)";
        final String urlName = "new name";
        final String encodedUrlName = "new-name";
        final Document document = new Document();
        document.setDisplayName(displayName);
        document.setUrlName(urlName);
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final String folderLocale = "en";

        expect(DocumentUtils.getHandle(eq(uuid), eq(session))).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("project:newsdocument"));
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(FolderUtils.getLocale(eq(folder))).andReturn(folderLocale);
        expect(JcrUtils.getNodePathQuietly(eq(handle))).andReturn("/content/documents/channel/news/breaking-news");

        expect(DocumentNameUtils.encodeUrlName(eq(urlName), eq(folderLocale))).andReturn(encodedUrlName);
        expect(DocumentNameUtils.getUrlName(eq(handle))).andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folder), eq(encodedUrlName))).andReturn(false);

        expect(DocumentNameUtils.encodeDisplayName(eq(displayName), eq(folderLocale))).andReturn(encodedDisplayName);
        expect(DocumentNameUtils.getDisplayName(eq(handle))).andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folder), eq(encodedDisplayName))).andReturn(false);

        DocumentNameUtils.setUrlName(eq(handle), eq(encodedUrlName));
        expectLastCall();

        DocumentNameUtils.setDisplayName(eq(handle), eq(encodedDisplayName));
        expectLastCall();

        replayAll();

        final Document result = documentsService.updateDocumentNames(uuid, document, session);
        assertThat(result.getDisplayName(), equalTo(encodedDisplayName));
        assertThat(result.getUrlName(), equalTo(encodedUrlName));
    }

    @Test
    public void updateDocumentNamesBothDisplayNameClashes() throws Exception {
        final String uuid = "uuid";
        final String displayName = "New Name";
        final String encodedDisplayName = "New Name (encoded)";
        final String urlName = "new name";
        final String encodedUrlName = "new-name";
        final Document document = new Document();
        document.setDisplayName(displayName);
        document.setUrlName(urlName);
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final String folderLocale = "en";

        expect(DocumentUtils.getHandle(eq(uuid), eq(session))).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("project:newsdocument"));
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(FolderUtils.getLocale(eq(folder))).andReturn(folderLocale);
        expect(JcrUtils.getNodePathQuietly(eq(handle))).andReturn("/content/documents/channel/news/breaking-news");

        expect(DocumentNameUtils.encodeUrlName(eq(urlName), eq(folderLocale))).andReturn(encodedUrlName);
        expect(DocumentNameUtils.getUrlName(eq(handle))).andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folder), eq(encodedUrlName))).andReturn(false);

        expect(DocumentNameUtils.encodeDisplayName(eq(displayName), eq(folderLocale))).andReturn(encodedDisplayName);
        expect(DocumentNameUtils.getDisplayName(eq(handle))).andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folder), eq(encodedDisplayName))).andReturn(true);

        replayAll();

        assertUpdateDocumentNamesFails(uuid, document, Reason.NAME_ALREADY_EXISTS);
    }

    @Test(expected = NotFoundException.class)
    public void deleteDocumentNotAHandle() throws Exception {
        final String uuid = "uuid";

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.empty());
        replayAll();

        documentsService.deleteDocument(uuid, session, locale);
    }

    @Test
    public void deleteDocumentNoWorkflow() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.empty());
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.deleteDocument(uuid, session, locale);
            fail("No Exception");
        } catch (final MethodNotAllowed e) {
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(Reason.NOT_A_DOCUMENT));
        }

        verifyAll();
    }

    @Test(expected = InternalServerErrorException.class)
    public void deleteArchivableDocumentWorkflowFailure() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(workflow));
        expect(EditingUtils.canArchiveDocument(eq(workflow))).andReturn(true);

        workflow.delete();
        expectLastCall().andThrow(new WorkflowException("meh"));

        replayAll(workflow);

        try {
            documentsService.deleteDocument(uuid, session, locale);
        } finally {
            verifyAll();
        }
    }

    @Test
    public void deleteArchivableDocumentSuccess() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(workflow));
        expect(EditingUtils.canArchiveDocument(eq(workflow))).andReturn(true);

        workflow.delete();
        expectLastCall();

        replayAll(workflow);

        documentsService.deleteDocument(uuid, session, locale);

        verifyAll();
    }

    @Test(expected = ForbiddenException.class)
    public void deleteExistingDocumentWithPreviewVariant() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(workflow));
        expect(EditingUtils.canArchiveDocument(eq(workflow))).andReturn(false);
        expect(EditingUtils.hasPreview(eq(workflow))).andReturn(true);

        replayAll(workflow);

        documentsService.deleteDocument(uuid, session, locale);

        verifyAll();
    }

    @Test
    public void deleteNewDocumentNoFolderWorkflow() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(workflow));
        expect(EditingUtils.canArchiveDocument(eq(workflow))).andReturn(false);
        expect(EditingUtils.hasPreview(eq(workflow))).andReturn(false);
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(WorkflowUtils.getWorkflow(eq(folder), eq("internal"), eq(FolderWorkflow.class))).andReturn(Optional.empty());
        expect(DocumentUtils.getDisplayName(eq(folder))).andReturn(Optional.empty());

        replayAll(workflow);

        try {
            documentsService.deleteDocument(uuid, session, locale);
            fail("No Exception");
        } catch (final MethodNotAllowed e) {
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(Reason.NOT_A_FOLDER));
        }

        verifyAll();
    }

    @Test(expected = ForbiddenException.class)
    public void deleteNewDocumentNotAllowedByFolderWorkflow() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(documentWorkflow));
        expect(EditingUtils.canArchiveDocument(eq(documentWorkflow))).andReturn(false);
        expect(EditingUtils.hasPreview(eq(documentWorkflow))).andReturn(false);
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(WorkflowUtils.getWorkflow(eq(folder), eq("internal"), eq(FolderWorkflow.class))).andReturn(Optional.of(folderWorkflow));
        expect(EditingUtils.canEraseDocument(eq(folderWorkflow))).andReturn(false);
        expect(JcrUtils.getNodeNameQuietly(handle)).andReturn("document");
        expect(JcrUtils.getNodePathQuietly(folder)).andReturn("/path/to/folder");

        replayAll(documentWorkflow, folderWorkflow);

        try {
            documentsService.deleteDocument(uuid, session, locale);
        } finally {
            verifyAll();
        }
    }

    @Test(expected = InternalServerErrorException.class)
    public void deleteNewDocumentFolderWorkflowFailure() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(documentWorkflow));
        expect(EditingUtils.canArchiveDocument(eq(documentWorkflow))).andReturn(false);
        expect(EditingUtils.hasPreview(eq(documentWorkflow))).andReturn(false);
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(WorkflowUtils.getWorkflow(eq(folder), eq("internal"), eq(FolderWorkflow.class))).andReturn(Optional.of(folderWorkflow));
        expect(EditingUtils.canEraseDocument(eq(folderWorkflow))).andReturn(true);

        final String handleName = "document";
        expect(handle.getName()).andReturn(handleName);

        folderWorkflow.delete(eq(handleName));
        expectLastCall().andThrow(new WorkflowException("meh"));

        replayAll(documentWorkflow, handle, folderWorkflow);

        try {
            documentsService.deleteDocument(uuid, session, locale);
        } finally {
            verifyAll();
        }
    }

    @Test
    public void deleteNewDocumentSuccess() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(documentWorkflow));
        expect(EditingUtils.canArchiveDocument(eq(documentWorkflow))).andReturn(false);
        expect(EditingUtils.hasPreview(eq(documentWorkflow))).andReturn(false);
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(WorkflowUtils.getWorkflow(eq(folder), eq("internal"), eq(FolderWorkflow.class))).andReturn(Optional.of(folderWorkflow));
        expect(EditingUtils.canEraseDocument(eq(folderWorkflow))).andReturn(true);

        final String handleName = "document";
        expect(handle.getName()).andReturn(handleName);

        folderWorkflow.delete(eq(handleName));
        expectLastCall();

        replayAll(documentWorkflow, handle, folderWorkflow);

        documentsService.deleteDocument(uuid, session, locale);

        verifyAll();
    }

    @Test
    public void cannotCreateDraftNoHintsFromWorkflow() throws ErrorWithPayloadException, RepositoryException, RemoteException, WorkflowException {

        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.of("Display Name"));

        final EditableWorkflow workflow = createMock(DocumentWorkflow.class);
        expect(WorkflowUtils.getWorkflow(anyObject(), anyObject(), eq(EditableWorkflow.class))).andReturn(Optional.of(workflow));
        expect(workflow.hints()).andReturn(emptyMap());

        final Map<String, Serializable> contextPayload = new HashMap<>();
        contextPayload.put("some-key", "some value");
        expect(hintsInspector.canCreateDraft(contextPayload)).andReturn(false);

        final Optional<ErrorInfo> errorInfo = Optional.of(new ErrorInfo(Reason.INVALID_DATA, contextPayload));
        expect(hintsInspector.determineEditingFailure(contextPayload, session)).andReturn(errorInfo);

        replayAll(handle, workflow, hintsInspector);

        try {
            documentsService.createDraft(uuid, session, locale, contextPayload);
        } catch (ForbiddenException e) {
            final ErrorInfo payload = (ErrorInfo) e.getPayload();
            assertThat(payload.getReason(), is(Reason.INVALID_DATA));
            assertThat(payload.getParams(), is(contextPayload));
        }
    }

    private DocumentType provideDocumentType(final Node handle) throws Exception {
        final String variantType = "project:newsdocument";
        final DocumentType docType = createMock(DocumentType.class);
        final DocumentTypesService documentTypesService = createMock(DocumentTypesService.class);

        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of(variantType)).anyTimes();
        expect(DocumentTypesService.get()).andReturn(documentTypesService);
        expect(documentTypesService.getDocumentType(variantType, session, locale)).andReturn(docType);
        expect(handle.getSession()).andReturn(session);

        replay(documentTypesService, handle);

        return docType;
    }

    private void assertUpdateDocumentNamesFails(final String uuid, final Document document, final Reason reason) throws ErrorWithPayloadException {
        try {
            documentsService.updateDocumentNames(uuid, document, session);
            fail("No Exception");
        } catch (final ConflictException e) {
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(reason));
        }
    }
}
