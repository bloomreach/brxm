/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

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
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.model.NewDocumentInfo;
import org.onehippo.cms.channelmanager.content.document.util.DisplayNameUtils;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.document.util.FolderUtils;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ConflictException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.MethodNotAllowed;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({WorkflowUtils.class, DocumentUtils.class, DocumentTypesService.class,
        JcrUtils.class, EditingUtils.class, FieldTypeUtils.class, FolderUtils.class, DisplayNameUtils.class})
public class DocumentsServiceImplTest {

    private Session session;
    private Locale locale;
    private final DocumentsServiceImpl documentsService = (DocumentsServiceImpl) DocumentsService.get();
    private NewDocumentInfo info;

    @Before
    public void setup() throws RepositoryException {
        session = createMock(Session.class);
        locale = new Locale("en");

        PowerMock.mockStatic(DisplayNameUtils.class);
        PowerMock.mockStatic(DocumentTypesService.class);
        PowerMock.mockStatic(DocumentUtils.class);
        PowerMock.mockStatic(EditingUtils.class);
        PowerMock.mockStatic(FieldTypeUtils.class);
        PowerMock.mockStatic(FolderUtils.class);
        PowerMock.mockStatic(JcrUtils.class);
        PowerMock.mockStatic(WorkflowUtils.class);

        info = new NewDocumentInfo();
        info.setName("Breaking News");
        info.setSlug("breaking-news");
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
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertNull(e.getPayload());
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
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertNull(e.getPayload());
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
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertNull(e.getPayload());
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
            documentsService.createDraft(uuid, session, locale);
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
        expect(EditingUtils.canCreateDraft(workflow)).andReturn(false);
        expect(EditingUtils.determineEditingFailure(workflow, session)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertNull(e.getPayload());
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
        expect(EditingUtils.canCreateDraft(workflow)).andReturn(false);
        expect(EditingUtils.determineEditingFailure(workflow, session)).andReturn(Optional.of(errorInfo));

        replayAll();

        try {
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertThat(e.getPayload(), equalTo(errorInfo));
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
        expect(EditingUtils.canCreateDraft(workflow)).andReturn(true);

        replayAll();

        try {
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertNull(e.getPayload());
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
        expect(EditingUtils.canCreateDraft(workflow)).andReturn(true);
        expect(DocumentTypesService.get()).andReturn(documentTypesService);
        expect(JcrUtils.getNodePathQuietly(handle)).andReturn("/bla");

        expect(handle.getSession()).andThrow(new RepositoryException());

        replayAll(handle);

        try {
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertNull(e.getPayload());
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
        expect(EditingUtils.canCreateDraft(workflow)).andReturn(true);
        expect(DocumentTypesService.get()).andReturn(documentTypesService);
        expect(JcrUtils.getNodePathQuietly(handle)).andReturn("/bla");

        expect(handle.getSession()).andReturn(session);
        expect(documentTypesService.getDocumentType(variantType, session, locale)).andThrow(new NotFoundException());

        replayAll(handle, documentTypesService);

        try {
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertNull(e.getPayload());
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
        expect(EditingUtils.canCreateDraft(workflow)).andReturn(true);

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(true);

        replayAll(docType);

        try {
            documentsService.createDraft(uuid, session, locale);
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
        expect(EditingUtils.canCreateDraft(workflow)).andReturn(true);
        expect(EditingUtils.createDraft(workflow, session)).andReturn(Optional.empty());

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);

        replayAll(docType);

        try {
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertNull(e.getPayload());
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
        expect(EditingUtils.canCreateDraft(workflow)).andReturn(true);
        expect(EditingUtils.createDraft(workflow, session)).andReturn(Optional.of(draft));
        FieldTypeUtils.readFieldValues(eq(draft), eq(fields), isA(Map.class));
        expectLastCall();

        expect(docType.getId()).andReturn("document:type");
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(fields).anyTimes();

        expect(WorkflowUtils.getDocumentVariantNode(eq(handle), eq(Variant.UNPUBLISHED))).andReturn(Optional.of(unpublished));
        FieldTypeUtils.readFieldValues(eq(unpublished), eq(fields), isA(Map.class));
        expectLastCall();

        replayAll(docType);

        final Document document = documentsService.createDraft(uuid, session, locale);
        assertThat(document.getId(), equalTo("uuid"));
        assertThat(document.getDisplayName(), equalTo("Display Name"));
        assertThat(document.getInfo().getType().getId(), equalTo("document:type"));
        assertThat(document.getInfo().isDirty(), equalTo(false));

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
        expect(EditingUtils.canCreateDraft(workflow)).andReturn(true);
        expect(EditingUtils.createDraft(workflow, session)).andReturn(Optional.of(draft));
        FieldTypeUtils.readFieldValues(eq(draft), eq(fields), isA(Map.class));
        expectLastCall();

        expect(docType.getId()).andReturn("document:type");
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(fields);

        expect(WorkflowUtils.getDocumentVariantNode(eq(handle), eq(Variant.UNPUBLISHED))).andReturn(Optional.of(unpublished));
        expect(docType.getFields()).andReturn(fields);
        FieldTypeUtils.readFieldValues(eq(unpublished), eq(fields), isA(Map.class));
        expectLastCall().andAnswer(() -> ((Map)getCurrentArguments()[2]).put("extraField", new FieldValue("value")));

        replayAll(docType);

        final Document document = documentsService.createDraft(uuid, session, locale);
        assertThat(document.getId(), equalTo("uuid"));
        assertThat(document.getDisplayName(), equalTo("Display Name"));
        assertThat(document.getInfo().getType().getId(), equalTo("document:type"));
        assertThat(document.getInfo().isDirty(), equalTo(true));

        verifyAll();
    }

    @Test
    public void updateDraftNotAHandle() throws Exception {
        final Document document = new Document();
        final String uuid = "uuid";

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.updateDraft(uuid, document, session, locale);
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertNull(e.getPayload());
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
            documentsService.updateDraft(uuid, document, session, locale);
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
            documentsService.updateDraft(uuid, document, session, locale);
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertNull(e.getPayload());
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
        expect(EditingUtils.canUpdateDraft(workflow)).andReturn(false);
        expect(EditingUtils.determineEditingFailure(workflow, session)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.updateDraft(uuid, document, session, locale);
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
        expect(EditingUtils.canUpdateDraft(workflow)).andReturn(false);
        expect(EditingUtils.determineEditingFailure(workflow, session)).andReturn(Optional.of(errorInfo));

        replayAll();

        try {
            documentsService.updateDraft(uuid, document, session, locale);
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
        expect(EditingUtils.canUpdateDraft(workflow)).andReturn(true);

        replayAll();

        try {
            documentsService.updateDraft(uuid, document, session, locale);
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertNull(e.getPayload());
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
        expect(EditingUtils.canUpdateDraft(workflow)).andReturn(true);

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(true);

        replayAll(docType);

        try {
            documentsService.updateDraft(uuid, document, session, locale);
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertNull(e.getPayload());
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
        expect(EditingUtils.canUpdateDraft(workflow)).andReturn(true);
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall().andThrow(badRequest);

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList());

        replayAll(docType);

        try {
            documentsService.updateDraft(uuid, document, session, locale);
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
        expect(EditingUtils.canUpdateDraft(workflow)).andReturn(true);
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList());
        session.save();
        expectLastCall().andThrow(new RepositoryException());

        replayAll(docType, session);

        try {
            documentsService.updateDraft(uuid, document, session, locale);
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertNull(e.getPayload());
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
        expect(EditingUtils.canUpdateDraft(workflow)).andReturn(true);
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();
        expect(FieldTypeUtils.validateFieldValues(document.getFields(), Collections.emptyList())).andReturn(false);

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList()).anyTimes();
        session.save();
        expectLastCall();

        replayAll(docType, session);

        try {
            documentsService.updateDraft(uuid, document, session, locale);
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
        expect(EditingUtils.canUpdateDraft(workflow)).andReturn(true);
        expect(EditingUtils.copyToPreviewAndKeepEditing(workflow, session)).andReturn(Optional.empty());
        expect(EditingUtils.determineEditingFailure(workflow, session)).andReturn(Optional.empty());
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();
        expect(FieldTypeUtils.validateFieldValues(document.getFields(), Collections.emptyList())).andReturn(true);

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList()).anyTimes();
        session.save();
        expectLastCall();

        replayAll(docType, session);

        try {
            documentsService.updateDraft(uuid, document, session, locale);
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
        expect(EditingUtils.canUpdateDraft(workflow)).andReturn(true);
        expect(EditingUtils.copyToPreviewAndKeepEditing(workflow, session)).andReturn(Optional.of(draft));
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();
        expect(FieldTypeUtils.validateFieldValues(document.getFields(), Collections.emptyList())).andReturn(true);

        expect(docType.getFields()).andReturn(Collections.emptyList());
        FieldTypeUtils.readFieldValues(draft, Collections.emptyList(), document.getFields());
        expectLastCall();

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList()).anyTimes();
        session.save();
        expectLastCall();

        replayAll(docType, session);

        final Document persistedDocument = documentsService.updateDraft(uuid, document, session, locale);

        assertThat(persistedDocument, equalTo(document));
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
        expect(EditingUtils.canUpdateDraft(workflow)).andReturn(true);
        expect(EditingUtils.copyToPreviewAndKeepEditing(workflow, session)).andReturn(Optional.of(draft));
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();
        expect(FieldTypeUtils.validateFieldValues(document.getFields(), Collections.emptyList())).andReturn(true);

        expect(docType.getFields()).andReturn(Collections.emptyList());
        FieldTypeUtils.readFieldValues(draft, Collections.emptyList(), document.getFields());
        expectLastCall();

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList()).anyTimes();
        session.save();
        expectLastCall();

        replayAll(docType, session);

        final Document persistedDocument = documentsService.updateDraft(uuid, document, session, locale);

        assertThat(persistedDocument.getId(), equalTo(document.getId()));
        assertThat(persistedDocument.getDisplayName(), equalTo(document.getDisplayName()));
        assertThat(persistedDocument.getFields(), equalTo(document.getFields()));
        assertThat(persistedDocument.getInfo().isDirty(), equalTo(false));
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
            documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale);
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertNull(e.getPayload());
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
            documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale);
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
            documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale);
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertNull(e.getPayload());
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
        expect(EditingUtils.canUpdateDraft(workflow)).andReturn(false);
        expect(EditingUtils.determineEditingFailure(workflow, session)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale);
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
        expect(EditingUtils.canUpdateDraft(workflow)).andReturn(false);
        expect(EditingUtils.determineEditingFailure(workflow, session)).andReturn(Optional.of(errorInfo));

        replayAll();

        try {
            documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale);
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
        expect(EditingUtils.canUpdateDraft(workflow)).andReturn(true);

        replayAll();

        try {
            documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale);
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertNull(e.getPayload());
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
        expect(EditingUtils.canUpdateDraft(workflow)).andReturn(true);

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(true);

        replayAll(docType);

        try {
            documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale);
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertNull(e.getPayload());
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
        expect(EditingUtils.canUpdateDraft(workflow)).andReturn(true);
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList());

        expect(FieldTypeUtils.writeFieldValue(fieldPath, fieldValues, fields, draft)).andThrow(badRequest);

        replayAll(docType);

        try {
            documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale);
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
        expect(EditingUtils.canUpdateDraft(workflow)).andReturn(true);
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(fields);
        expect(FieldTypeUtils.writeFieldValue(fieldPath, fieldValues, fields, draft)).andReturn(false);

        replayAll(docType, session);

        documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale);

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
        expect(EditingUtils.canUpdateDraft(workflow)).andReturn(true);
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(fields);
        expect(FieldTypeUtils.writeFieldValue(fieldPath, fieldValues, fields, draft)).andReturn(true);

        session.save();
        expectLastCall();

        replayAll(docType, session);

        documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale);

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
        expect(EditingUtils.canUpdateDraft(workflow)).andReturn(true);
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList());
        expect(FieldTypeUtils.writeFieldValue(fieldPath, fieldValues, fields, draft)).andReturn(true);

        session.save();
        expectLastCall().andThrow(new RepositoryException());

        replayAll(docType, session);

        try {
            documentsService.updateDraftField(uuid, fieldPath, fieldValues, session, locale);
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertNull(e.getPayload());
        }

        verifyAll();
    }

    @Test
    public void deleteDraftNotAHandle() throws Exception {
        final String uuid = "uuid";

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.deleteDraft(uuid, session, locale);
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertNull(e.getPayload());
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
            documentsService.deleteDraft(uuid, session, locale);
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
        expect(EditingUtils.canDeleteDraft(workflow)).andReturn(false);

        replayAll();

        try {
            documentsService.deleteDraft(uuid, session, locale);
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
        expect(EditingUtils.canDeleteDraft(workflow)).andReturn(true);

        expect(workflow.disposeEditableInstance()).andThrow(new WorkflowException("bla"));

        replayAll(workflow);

        try {
            documentsService.deleteDraft(uuid, session, locale);
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertNull(e.getPayload());
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
        expect(EditingUtils.canDeleteDraft(workflow)).andReturn(true);

        expect(workflow.disposeEditableInstance()).andReturn(null);

        replayAll(workflow);

        documentsService.deleteDraft(uuid, session, locale);

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
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.PUBLISHED)).andReturn(Optional.of(published));
        FieldTypeUtils.readFieldValues(eq(published), eq(Collections.emptyList()), isA(Map.class));
        expectLastCall();

        expect(docType.getId()).andReturn("document:type");
        expect(docType.getFields()).andReturn(Collections.emptyList());

        replayAll(docType);

        final Document document = documentsService.getPublished(uuid, session, locale);

        assertThat(document.getDisplayName(), equalTo("Document Display Name"));

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
        expect(FolderUtils.getFolder(eq("/content/documents/channel/news"), eq(session))).andReturn(folderNode);
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News"))).andReturn(true);
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
        expect(FolderUtils.getFolder(eq("/content/documents/channel/news"), eq(session))).andReturn(folderNode);
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News"))).andReturn(false);
        expect(FolderUtils.nodeExists(eq(folderNode), eq("breaking-news"))).andReturn(true);
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
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News")))
                .andReturn(false);
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
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News")))
                .andReturn(false);
        expect(FolderUtils.nodeExists(eq(folderNode), eq("breaking-news")))
                .andReturn(false);
        expect(FolderUtils.getLocale(folderNode))
                .andReturn("en_GB");
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
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News")))
                .andReturn(false);
        expect(FolderUtils.nodeExists(eq(folderNode), eq("breaking-news")))
                .andReturn(false);
        expect(FolderUtils.getLocale(folderNode))
                .andReturn("en_GB");
        expect(WorkflowUtils.getWorkflow(eq(folderNode), eq("internal"), eq(FolderWorkflow.class)))
                .andReturn(Optional.of(folderWorkflow));
        expect(folderWorkflow.add(eq("new-news-document"), eq("project:newsdocument"), eq("breaking-news")))
                .andReturn("/content/documents/channel/news/breaking-news");
        expect(session.getNode(eq("/content/documents/channel/news/breaking-news")))
                .andReturn(documentHandle);
        expect(DisplayNameUtils.encodeDisplayName(eq("Breaking News"), eq("en_GB")))
                .andReturn("Breaking News (encoded)");

        DisplayNameUtils.setDisplayName(eq(documentHandle), eq("Breaking News (encoded)"));
        expectLastCall();

        expect(WorkflowUtils.getDocumentVariantNode(eq(documentHandle), eq(Variant.DRAFT)))
                .andReturn(Optional.of(documentDraft));

        expect(documentHandle.getIdentifier()).andReturn("uuid");

        final DocumentType docType = provideDocumentType(documentHandle);
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getId()).andReturn("project:newsdocument");
        expect(DocumentUtils.getDisplayName(documentHandle)).andReturn(Optional.of("Breaking News (encoded)"));

        session.save();
        expectLastCall();

        final List<FieldType> fields = Collections.emptyList();
        expect(docType.getFields()).andReturn(fields);
        FieldTypeUtils.readFieldValues(eq(documentDraft), eq(fields), isA(Map.class));
        expectLastCall();

        replayAll(folderNode, documentDraft, folderWorkflow, docType, session);

        final Document document = documentsService.createDocument(info, session, locale);

        assertThat(document.getId(), equalTo("uuid"));
        assertThat(document.getDisplayName(), equalTo("Breaking News (encoded)"));
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
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News")))
                .andReturn(false);
        expect(FolderUtils.nodeExists(eq(folderNode), eq("breaking-news")))
                .andReturn(false);
        expect(FolderUtils.getLocale(folderNode))
                .andReturn("en_GB");
        expect(WorkflowUtils.getWorkflow(eq(folderNode), eq("internal"), eq(FolderWorkflow.class)))
                .andReturn(Optional.of(folderWorkflow));
        expect(folderWorkflow.add(eq("new-news-document"), eq("project:newsdocument"), eq("breaking-news")))
                .andReturn("/content/documents/channel/news/breaking-news");
        expect(session.getNode(eq("/content/documents/channel/news/breaking-news")))
                .andReturn(documentHandle);
        expect(DisplayNameUtils.encodeDisplayName(eq("Breaking News"), eq("en_GB")))
                .andReturn("Breaking News (encoded)");

        DisplayNameUtils.setDisplayName(eq(documentHandle), eq("Breaking News (encoded)"));
        expectLastCall();

        expect(WorkflowUtils.getDocumentVariantNode(eq(documentHandle), eq(Variant.DRAFT)))
                .andReturn(Optional.of(documentDraft));

        expect(documentHandle.getIdentifier()).andReturn("uuid");

        final DocumentType docType = provideDocumentType(documentHandle);
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getId()).andReturn("project:newsdocument");
        expect(DocumentUtils.getDisplayName(documentHandle)).andReturn(Optional.of("Breaking News (encoded)"));

        session.save();
        expectLastCall();

        final List<FieldType> fields = Collections.emptyList();
        expect(docType.getFields()).andReturn(fields);
        FieldTypeUtils.readFieldValues(eq(documentDraft), eq(fields), isA(Map.class));
        expectLastCall();

        replayAll(folderNode, documentDraft, folderWorkflow, docType, session);

        final Document document = documentsService.createDocument(info, session, locale);

        assertThat(document.getId(), equalTo("uuid"));
        assertThat(document.getDisplayName(), equalTo("Breaking News (encoded)"));
        assertThat(document.getFields().size(), equalTo(0));

        verifyAll();
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
}
