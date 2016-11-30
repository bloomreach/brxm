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
import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.model.EditingInfo;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.MethodNotAllowed;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(PowerMockRunner.class)
@PrepareForTest({WorkflowUtils.class, DocumentUtils.class, DocumentTypesService.class,
        JcrUtils.class, EditingUtils.class, FieldTypeUtils.class})
public class DocumentsServiceImplTest {
    private Session session;
    private Locale locale;
    private DocumentsServiceImpl documentsService = (DocumentsServiceImpl) DocumentsService.get();

    @Before
    public void setup() throws RepositoryException {
        session = createMock(Session.class);
        locale = new Locale("en");

        PowerMock.mockStatic(DocumentTypesService.class);
        PowerMock.mockStatic(DocumentUtils.class);
        PowerMock.mockStatic(EditingUtils.class);
        PowerMock.mockStatic(FieldTypeUtils.class);
        PowerMock.mockStatic(JcrUtils.class);
        PowerMock.mockStatic(WorkflowUtils.class);
    }

    @Test
    public void createDraftNotAHandle() throws Exception {
        final String uuid = "uuid";
        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.empty());
        PowerMock.replayAll();

        try {
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (NotFoundException e) {
            assertNull(e.getPayload());
        }

        PowerMock.verifyAll();
    }

    @Test
    public void createDraftNoNodeType() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.empty());

        PowerMock.replayAll();

        try {
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (NotFoundException e) {
            assertNull(e.getPayload());
        }

        PowerMock.verifyAll();
    }

    @Test
    public void createDraftDeleted() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("hippo:deleted"));

        PowerMock.replayAll();

        try {
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (NotFoundException e) {
            assertNull(e.getPayload());
        }

        PowerMock.verifyAll();
    }

    @Test
    public void createDraftNoWorkflow() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:nodetype"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.empty());

        PowerMock.replayAll();

        try {
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (MethodNotAllowed e) {
            assertTrue(e.getPayload() instanceof ErrorInfo);
            ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(ErrorInfo.Reason.NOT_A_DOCUMENT));
        }

        PowerMock.verifyAll();
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

        PowerMock.replayAll();

        try {
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (InternalServerErrorException e) {
            assertNull(e.getPayload());
        }

        PowerMock.verifyAll();
    }

    @Test
    public void createDraftNoSession() throws Exception {
        final String uuid = "uuid";
        final String variantType = "variant:type";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentTypesService documentTypesService = createMock(DocumentTypesService.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of(variantType)).anyTimes();
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(DocumentTypesService.get()).andReturn(documentTypesService);
        expect(JcrUtils.getNodePathQuietly(handle)).andReturn("/bla");

        expect(handle.getSession()).andThrow(new RepositoryException());

        PowerMock.replayAll();
        replay(handle);

        try {
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (InternalServerErrorException e) {
            assertNull(e.getPayload());
        }

        verify(handle);
        PowerMock.verifyAll();
    }

    @Test
    public void createDraftNoDocumentType() throws Exception {
        final String uuid = "uuid";
        final String variantType = "variant:type";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentTypesService documentTypesService = createMock(DocumentTypesService.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of(variantType)).anyTimes();
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(DocumentTypesService.get()).andReturn(documentTypesService);
        expect(JcrUtils.getNodePathQuietly(handle)).andReturn("/bla");

        expect(handle.getSession()).andReturn(session);
        expect(documentTypesService.getDocumentType(variantType, session, locale)).andThrow(new NotFoundException());

        PowerMock.replayAll();
        replay(handle, documentTypesService);

        try {
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (InternalServerErrorException e) {
            assertNull(e.getPayload());
        }

        verify(handle, documentTypesService);
        PowerMock.verifyAll();
    }

    @Test
    public void createDraftNotAvailable() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final EditingInfo editingInfo = new EditingInfo();

        editingInfo.setState(EditingInfo.State.UNAVAILABLE);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.of("Document Display Name"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.determineEditingInfo(workflow, handle)).andReturn(editingInfo);

        expect(docType.getId()).andReturn("document:type");

        PowerMock.replayAll();
        replay(docType);

        try {
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (ForbiddenException e) {
            assertTrue(e.getPayload() instanceof Document);
            final Document document = (Document)e.getPayload();
            assertThat(document.getInfo().getEditingInfo().getState(), equalTo(EditingInfo.State.UNAVAILABLE));
            assertThat(document.getDisplayName(), equalTo("Document Display Name"));
        }

        verify(docType);
        PowerMock.verifyAll();
    }

    @Test
    public void createDraftUnknownValidator() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final EditingInfo editingInfo = new EditingInfo();

        editingInfo.setState(EditingInfo.State.AVAILABLE);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.empty());
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.determineEditingInfo(workflow, handle)).andReturn(editingInfo);

        expect(docType.getId()).andReturn("document:type");
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(true);

        PowerMock.replayAll();
        replay(docType);

        try {
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (ForbiddenException e) {
            assertTrue(e.getPayload() instanceof Document);
            final Document document = (Document)e.getPayload();
            assertThat(document.getInfo().getEditingInfo().getState(), equalTo(EditingInfo.State.UNAVAILABLE_CUSTOM_VALIDATION_PRESENT));
            assertNull(document.getDisplayName());
        }

        verify(docType);
        PowerMock.verifyAll();
    }

    @Test
    public void createDraftFailed() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final EditingInfo editingInfo = new EditingInfo();

        editingInfo.setState(EditingInfo.State.AVAILABLE);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.empty());
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.determineEditingInfo(workflow, handle)).andReturn(editingInfo);
        expect(EditingUtils.createDraft(workflow, handle)).andReturn(Optional.empty());

        expect(docType.getId()).andReturn("document:type");
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);

        PowerMock.replayAll();
        replay(docType);

        try {
            documentsService.createDraft(uuid, session, locale);
            fail("No Exception");
        } catch (ForbiddenException e) {
            assertTrue(e.getPayload() instanceof Document);
            final Document document = (Document)e.getPayload();
            assertThat(document.getInfo().getEditingInfo().getState(), equalTo(EditingInfo.State.UNAVAILABLE));
        }

        verify(docType);
        PowerMock.verifyAll();
    }

    @Test
    public void createDraftSuccess() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final EditingInfo editingInfo = new EditingInfo();
        final List<FieldType> fields = Collections.emptyList();

        editingInfo.setState(EditingInfo.State.AVAILABLE);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.empty());
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.determineEditingInfo(workflow, handle)).andReturn(editingInfo);
        expect(EditingUtils.createDraft(workflow, handle)).andReturn(Optional.of(draft));
        FieldTypeUtils.readFieldValues(eq(draft), eq(fields), isA(Map.class));
        expectLastCall();

        expect(docType.getId()).andReturn("document:type");
        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(fields);

        PowerMock.replayAll();
        replay(docType);

        Document document = documentsService.createDraft(uuid, session, locale);
        assertThat(document.getInfo().getEditingInfo().getState(), equalTo(EditingInfo.State.AVAILABLE));

        verify(docType);
        PowerMock.verifyAll();
    }

    @Test
    public void updateDraftNotAHandle() throws Exception {
        final Document document = new Document();
        final String uuid = "uuid";

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.empty());

        PowerMock.replayAll();

        try {
            documentsService.updateDraft(uuid, document, session, locale);
            fail("No Exception");
        } catch (NotFoundException e) {
            assertNull(e.getPayload());
        }

        PowerMock.verifyAll();
    }

    @Test
    public void updateDraftNoWorkflow() throws Exception {
        final Document document = new Document();
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.empty());

        PowerMock.replayAll();

        try {
            documentsService.updateDraft(uuid, document, session, locale);
            fail("No Exception");
        } catch (MethodNotAllowed e) {
            assertTrue(e.getPayload() instanceof ErrorInfo);
            ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(ErrorInfo.Reason.NOT_A_DOCUMENT));
        }

        PowerMock.verifyAll();
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
        expect(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)).andReturn(Optional.empty());

        PowerMock.replayAll();

        try {
            documentsService.updateDraft(uuid, document, session, locale);
            fail("No Exception");
        } catch (NotFoundException e) {
            assertNull(e.getPayload());
        }

        PowerMock.verifyAll();
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
        expect(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.canUpdateDocument(workflow)).andReturn(false);

        PowerMock.replayAll();

        try {
            documentsService.updateDraft(uuid, document, session, locale);
            fail("No Exception");
        } catch (ForbiddenException e) {
            assertTrue(e.getPayload() instanceof ErrorInfo);
            ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(ErrorInfo.Reason.NOT_HOLDER));
        }

        PowerMock.verifyAll();
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
        expect(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.canUpdateDocument(workflow)).andReturn(true);

        PowerMock.replayAll();

        try {
            documentsService.updateDraft(uuid, document, session, locale);
            fail("No Exception");
        } catch (InternalServerErrorException e) {
            assertNull(e.getPayload());
        }

        PowerMock.verifyAll();
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
        expect(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.canUpdateDocument(workflow)).andReturn(true);

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(true);

        PowerMock.replayAll();
        replay(docType);

        try {
            documentsService.updateDraft(uuid, document, session, locale);
            fail("No Exception");
        } catch (ForbiddenException e) {
            assertNull(e.getPayload());
        }

        verify(docType);
        PowerMock.verifyAll();
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
        expect(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.canUpdateDocument(workflow)).andReturn(true);
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall().andThrow(badRequest);

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList());

        PowerMock.replayAll();
        replay(docType);

        try {
            documentsService.updateDraft(uuid, document, session, locale);
            fail("No Exception");
        } catch (BadRequestException e) {
            assertThat(e, equalTo(badRequest));
        }

        verify(docType);
        PowerMock.verifyAll();
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
        expect(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.canUpdateDocument(workflow)).andReturn(true);
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList());
        session.save();
        expectLastCall().andThrow(new RepositoryException());

        PowerMock.replayAll();
        replay(docType, session);

        try {
            documentsService.updateDraft(uuid, document, session, locale);
            fail("No Exception");
        } catch (InternalServerErrorException e) {
            assertNull(e.getPayload());
        }

        verify(docType, session);
        PowerMock.verifyAll();
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
        expect(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.canUpdateDocument(workflow)).andReturn(true);
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();
        expect(FieldTypeUtils.validateFieldValues(document.getFields(), Collections.emptyList())).andReturn(false);

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList()).anyTimes();
        session.save();
        expectLastCall();

        PowerMock.replayAll();
        replay(docType, session);

        try {
            documentsService.updateDraft(uuid, document, session, locale);
            fail("No Exception");
        } catch (BadRequestException e) {
            assertThat(e.getPayload(), equalTo(document));
        }

        verify(docType, session);
        PowerMock.verifyAll();
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
        expect(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.canUpdateDocument(workflow)).andReturn(true);
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();
        expect(FieldTypeUtils.validateFieldValues(document.getFields(), Collections.emptyList())).andReturn(true);

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList()).anyTimes();
        session.save();
        expectLastCall();
        expect(workflow.commitEditableInstance()).andThrow(new WorkflowException("bla"));

        PowerMock.replayAll();
        replay(docType, session, workflow);

        try {
            documentsService.updateDraft(uuid, document, session, locale);
            fail("No Exception");
        } catch (InternalServerErrorException e) {
            assertNull(e.getPayload());
        }

        verify(docType, session, workflow);
        PowerMock.verifyAll();
    }

    @Test
    public void updateDraftRestartEditingFailure() throws Exception {
        final Document document = new Document();
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.canUpdateDocument(workflow)).andReturn(true);
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();
        expect(FieldTypeUtils.validateFieldValues(document.getFields(), Collections.emptyList())).andReturn(true);

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList()).anyTimes();
        session.save();
        expectLastCall();
        expect(session.getUserID()).andReturn("admin");
        expect(workflow.commitEditableInstance()).andReturn(null);
        expect(workflow.obtainEditableInstance()).andThrow(new WorkflowException("bla"));

        PowerMock.replayAll();
        replay(docType, session, workflow);

        try {
            documentsService.updateDraft(uuid, document, session, locale);
            fail("No Exception");
        } catch (InternalServerErrorException e) {
            assertTrue(e.getPayload() instanceof ErrorInfo);
            ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(ErrorInfo.Reason.HOLDERSHIP_LOST));
        }

        verify(docType, session, workflow);
        PowerMock.verifyAll();
    }

    @Test
    public void updateDraftRestartEditingOtherFailure() throws Exception {
        final Document document = new Document();
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.canUpdateDocument(workflow)).andReturn(true);
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();
        expect(FieldTypeUtils.validateFieldValues(document.getFields(), Collections.emptyList())).andReturn(true);

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList()).anyTimes();
        session.save();
        expectLastCall();
        expect(session.getUserID()).andReturn("admin");
        expect(workflow.commitEditableInstance()).andReturn(null);
        expect(workflow.obtainEditableInstance()).andThrow(new RepositoryException());

        PowerMock.replayAll();
        replay(docType, session, workflow);

        try {
            documentsService.updateDraft(uuid, document, session, locale);
            fail("No Exception");
        } catch (InternalServerErrorException e) {
            assertNull(e.getPayload());
        }

        verify(docType, session, workflow);
        PowerMock.verifyAll();
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
        expect(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.canUpdateDocument(workflow)).andReturn(true);
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();
        expect(FieldTypeUtils.validateFieldValues(document.getFields(), Collections.emptyList())).andReturn(true);

        expect(docType.isReadOnlyDueToUnknownValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList()).anyTimes();
        session.save();
        expectLastCall();
        expect(workflow.commitEditableInstance()).andReturn(null);
        expect(workflow.obtainEditableInstance()).andReturn(null);

        PowerMock.replayAll();
        replay(docType, session, workflow);

        documentsService.updateDraft(uuid, document, session, locale);

        verify(docType, session, workflow);
        PowerMock.verifyAll();
    }

    @Test
    public void deleteDraftNotAHandle() throws Exception {
        final String uuid = "uuid";

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.empty());

        PowerMock.replayAll();

        try {
            documentsService.deleteDraft(uuid, session, locale);
            fail("No Exception");
        } catch (NotFoundException e) {
            assertNull(e.getPayload());
        }

        PowerMock.verifyAll();
    }

    @Test
    public void deleteDraftNoWorkflow() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.empty());

        PowerMock.replayAll();

        try {
            documentsService.deleteDraft(uuid, session, locale);
            fail("No Exception");
        } catch (MethodNotAllowed e) {
            assertTrue(e.getPayload() instanceof ErrorInfo);
            ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(ErrorInfo.Reason.NOT_A_DOCUMENT));
        }

        PowerMock.verifyAll();
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

        PowerMock.replayAll();

        try {
            documentsService.deleteDraft(uuid, session, locale);
            fail("No Exception");
        } catch (ForbiddenException e) {
            assertTrue(e.getPayload() instanceof ErrorInfo);
            ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(ErrorInfo.Reason.ALREADY_DELETED));
        }

        PowerMock.verifyAll();
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

        PowerMock.replayAll();
        replay(workflow);

        try {
            documentsService.deleteDraft(uuid, session, locale);
            fail("No Exception");
        } catch (InternalServerErrorException e) {
            assertNull(e.getPayload());
        }

        verify(workflow);
        PowerMock.verifyAll();
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

        PowerMock.replayAll();
        replay(workflow);

        documentsService.deleteDraft(uuid, session, locale);

        verify(workflow);
        PowerMock.verifyAll();
    }


    @Test
    public void getPublished() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node published = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final EditingInfo editingInfo = new EditingInfo();

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.of("Document Display Name"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED)).andReturn(Optional.of(published));
        expect(EditingUtils.determineEditingInfo(workflow, handle)).andReturn(editingInfo);
        FieldTypeUtils.readFieldValues(eq(published), eq(Collections.emptyList()), isA(Map.class));
        expectLastCall();

        expect(docType.getId()).andReturn("document:type");
        expect(docType.getFields()).andReturn(Collections.emptyList());

        PowerMock.replayAll();
        replay(docType);

        Document document = documentsService.getPublished(uuid, session, locale);

        assertThat(document.getDisplayName(), equalTo("Document Display Name"));

        verify(docType);
        PowerMock.verifyAll();
    }


    private DocumentType provideDocumentType(final Node handle) throws Exception {
        final String variantType = "variant:type";
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
