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

package org.onehippo.cms.channelmanager.content.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.exception.DocumentNotFoundException;
import org.onehippo.cms.channelmanager.content.exception.DocumentTypeNotFoundException;
import org.onehippo.cms.channelmanager.content.model.Document;
import org.onehippo.cms.channelmanager.content.model.DocumentTypeSpec;
import org.onehippo.cms.channelmanager.content.model.EditingInfo;
import org.onehippo.cms.channelmanager.content.model.FieldTypeSpec;
import org.onehippo.cms.channelmanager.content.model.UserInfo;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DocumentsServiceImpl.class, WorkflowUtils.class})
public class DocumentsServiceImplTest {
    private Node rootNode;
    private Session session;
    private DocumentsServiceImpl documentsService = (DocumentsServiceImpl)DocumentsService.get();

    @Before
    public void setup() throws RepositoryException {
        rootNode = MockNode.root();
        session = rootNode.getSession();
    }

    @Test(expected = DocumentNotFoundException.class)
    public void nodeNotFound() throws Exception {
        documentsService.getDocument("unknown-uuid", session, null);
    }

    @Test(expected = DocumentNotFoundException.class)
    public void nodeNotHandle() throws Exception {
        final Node handle = rootNode.addNode("testDocument", "invalid-type");
        final String id = handle.getIdentifier();
        documentsService.getDocument(id, session, null);
    }

    @Test(expected = DocumentNotFoundException.class)
    public void returnNotFoundWhenDocumentHandleHasNoVariantNode() throws Exception {
        final Node handle = rootNode.addNode("testDocument", "hippo:handle");
        final String id = handle.getIdentifier();

        handle.addNode("otherName", "ns:doctype");

        documentsService.getDocument(id, session, null);
    }

    @Test
    public void successfulButStubbedDocumentRetrieval() throws Exception {
        final Node handle = rootNode.addNode("testDocument", "hippo:handle");
        final String id = handle.getIdentifier();
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final EditingInfo info = new EditingInfo();
        final Document document = new Document();
        final Locale locale = new Locale("en");

        PowerMock.createMock(Document.class);
        PowerMock.expectNew(Document.class).andReturn(document);
        PowerMock.replayAll();

        handle.addNode("testDocument", "ns:doctype");
        handle.setProperty(HippoNodeType.HIPPO_NAME, "Test Document");
        documentsService = createMockBuilder(DocumentsServiceImpl.class)
                .addMockedMethod("retrieveWorkflow")
                .addMockedMethod("determineEditingInfo")
                .addMockedMethod("determineDocumentFields")
                .createMock();
        expect(documentsService.retrieveWorkflow(handle)).andReturn(workflow);
        expect(documentsService.determineEditingInfo(session, workflow)).andReturn(info);
        documentsService.determineDocumentFields(document, handle, workflow, locale);
        expectLastCall();
        replay(documentsService);

        assertThat(documentsService.getDocument(id, session, locale), equalTo(document));

        verify(documentsService);

        assertThat(document.getId(), equalTo(id));
        assertThat(document.getDisplayName(), equalTo("Test Document"));
        assertThat(document.getInfo().getType().getId(), equalTo("ns:doctype"));
    }

    @Test
    public void readAvailableEditingInfo() throws Exception {
        session = createMock(Session.class);
        final Workflow workflow = createMock(Workflow.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("obtainEditableInstance", true);

        expect(workflow.hints()).andReturn(hints);
        replay(workflow);

        final EditingInfo info = documentsService.determineEditingInfo(session, workflow);

        verify(workflow);
        assertThat(info.getState(), equalTo(EditingInfo.State.AVAILABLE));
        assertThat(info.getHolder(), equalTo(null));
    }

    @Test
    public void readAvailableEditingInfoWhileEditing() throws Exception {
        session = createMock(Session.class);
        final Workflow workflow = createMock(Workflow.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("obtainEditableInstance", false);
        hints.put("inUseBy", "tester");

        expect(workflow.hints()).andReturn(hints);
        expect(session.getUserID()).andReturn("tester");
        replay(session, workflow);

        final EditingInfo info = documentsService.determineEditingInfo(session, workflow);

        verify(session, workflow);
        assertThat(info.getState(), equalTo(EditingInfo.State.AVAILABLE));
        assertThat(info.getHolder(), equalTo(null));
    }

    @Test
    public void readUnavailableHeldByOtherUser() throws Exception {
        documentsService = createMockBuilder(DocumentsServiceImpl.class).addMockedMethod("determineHolder").createMock();
        session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final Workflow workflow = createMock(Workflow.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("obtainEditableInstance", false);
        hints.put("inUseBy", "otherUser");
        hints.put("requests", "dummy");

        expect(workflow.hints()).andReturn(hints);
        expect(session.getWorkspace()).andReturn(workspace);
        expect(session.getUserID()).andReturn("tester");
        expect(documentsService.determineHolder("otherUser", workspace)).andReturn(null);
        replay(documentsService, session, workspace, workflow);

        final EditingInfo info = documentsService.determineEditingInfo(session, workflow);

        verify(documentsService, session, workspace, workflow);
        assertThat(info.getState(), equalTo(EditingInfo.State.UNAVAILABLE_HELD_BY_OTHER_USER));
        assertThat(info.getHolder(), equalTo(null));
    }

    @Test
    public void readUnavailableRequestPending() throws Exception {
        session = createMock(Session.class);
        final Workflow workflow = createMock(Workflow.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("obtainEditableInstance", false);
        hints.put("requests", "dummy");

        expect(workflow.hints()).andReturn(hints);
        replay(workflow);

        final EditingInfo info = documentsService.determineEditingInfo(session, workflow);

        verify(workflow);
        assertThat(info.getState(), equalTo(EditingInfo.State.UNAVAILABLE_REQUEST_PENDING));
        assertThat(info.getHolder(), equalTo(null));
    }

    @Test
    public void readHolderWithRepositoryException() throws Exception {
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        expect(workspace.getSecurityService()).andThrow(new RepositoryException());
        replay(workspace);

        UserInfo info = documentsService.determineHolder("otherUser", workspace);

        verify(workspace);
        assertThat(info.getId(), equalTo("otherUser"));
        assertThat(info.getDisplayName(), equalTo(null));
    }

    @Test
    public void readRegularHolder() throws Exception {
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final SecurityService ss = createMock(SecurityService.class);
        final User user = createMock(User.class);

        expect(user.getFirstName()).andReturn(" John ");
        expect(user.getLastName()).andReturn(" Doe ");
        expect(ss.getUser("otherUser")).andReturn(user);
        expect(workspace.getSecurityService()).andReturn(ss);
        replay(workspace, ss, user);

        UserInfo info = documentsService.determineHolder("otherUser", workspace);

        verify(workspace, ss, user);
        assertThat(info.getId(), equalTo("otherUser"));
        assertThat(info.getDisplayName(), equalTo("John Doe"));
    }

    @Test
    public void readHolderWithFirstNameOnly() throws Exception {
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final SecurityService ss = createMock(SecurityService.class);
        final User user = createMock(User.class);

        expect(user.getFirstName()).andReturn(" John ");
        expect(user.getLastName()).andReturn("");
        expect(ss.getUser("otherUser")).andReturn(user);
        expect(workspace.getSecurityService()).andReturn(ss);
        replay(workspace, ss, user);

        UserInfo info = documentsService.determineHolder("otherUser", workspace);

        verify(workspace, ss, user);
        assertThat(info.getId(), equalTo("otherUser"));
        assertThat(info.getDisplayName(), equalTo("John"));
    }

    @Test
    public void readHolderWithoutDisplayName() throws Exception {
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final SecurityService ss = createMock(SecurityService.class);
        final User user = createMock(User.class);

        expect(user.getFirstName()).andReturn(null);
        expect(user.getLastName()).andReturn(null);
        expect(ss.getUser("otherUser")).andReturn(user);
        expect(workspace.getSecurityService()).andReturn(ss);
        replay(workspace, ss, user);

        UserInfo info = documentsService.determineHolder("otherUser", workspace);

        verify(workspace, ss, user);
        assertThat(info.getId(), equalTo("otherUser"));
        assertThat(info.getDisplayName(), equalTo(""));
    }

    @Test(expected = DocumentNotFoundException.class)
    public void failToRetrieveWorkflow() throws Exception {
        final Node handle = createMock(Node.class);

        expect(handle.getSession()).andThrow(new RepositoryException());
        replay(handle);

        documentsService.retrieveWorkflow(handle);
    }

    @Test(expected = DocumentNotFoundException.class)
    public void unsupportedTypeOfWorkflow() throws Exception {
        final Node handle = createMock(Node.class);
        final Session session = createMock(Session.class);
        final HippoWorkspace ws = createMock(HippoWorkspace.class);
        final WorkflowManager wfm = createMock(WorkflowManager.class);
        final Workflow wf = createMock(Workflow.class); // not EditableWorkflow

        expect(handle.getSession()).andReturn(session);
        expect(session.getWorkspace()).andReturn(ws);
        expect(ws.getWorkflowManager()).andReturn(wfm);
        expect(wfm.getWorkflow("editing", handle)).andReturn(wf);
        replay(handle, session, ws, wfm);

        try {
            documentsService.retrieveWorkflow(handle);
        } finally {
            verify(handle, session, ws, wfm);
        }
    }

    @Test
    public void getWorkflow() throws Exception {
        final Node handle = createMock(Node.class);
        final Session session = createMock(Session.class);
        final HippoWorkspace ws = createMock(HippoWorkspace.class);
        final WorkflowManager wfm = createMock(WorkflowManager.class);
        final Workflow wf = createMock(EditableWorkflow.class);

        expect(handle.getSession()).andReturn(session);
        expect(session.getWorkspace()).andReturn(ws);
        expect(ws.getWorkflowManager()).andReturn(wfm);
        expect(wfm.getWorkflow("editing", handle)).andReturn(wf);
        replay(handle, session, ws, wfm);

        assertThat(documentsService.retrieveWorkflow(handle), equalTo(wf));
        verify(handle, session, ws, wfm);
    }

    @Test(expected = DocumentNotFoundException.class)
    public void failToRetrieveDraftNode() throws Exception {
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        documentsService = createMockBuilder(DocumentsServiceImpl.class)
                .addMockedMethod("getOrMakeDraftNode")
                .createMock();
        expect(documentsService.getOrMakeDraftNode(workflow, handle)).andThrow(new DocumentNotFoundException());
        replay(documentsService);

        documentsService.determineDocumentFields(null, handle, workflow, null);
    }

    @Test(expected = DocumentNotFoundException.class)
    public void failToLoadDocumentType() throws Exception {
        final Document document = new Document();
        final Locale locale = new Locale("en");
        final Node handle = createMock(Node.class);
        final Session session = createMock(Session.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        documentsService = createMockBuilder(DocumentsServiceImpl.class)
                .addMockedMethod("getOrMakeDraftNode")
                .addMockedMethod("getDocumentTypeSpec")
                .createMock();
        expect(documentsService.getOrMakeDraftNode(workflow, handle)).andReturn(null);
        expect(documentsService.getDocumentTypeSpec(document, session, locale)).andThrow(new DocumentTypeNotFoundException());
        expect(handle.getSession()).andReturn(session);
        replay(documentsService, handle);

        documentsService.determineDocumentFields(document, handle, workflow, locale);
    }

    @Test(expected = DocumentNotFoundException.class)
    public void failFieldRetrievalWithRepositoryException() throws Exception {
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        documentsService = createMockBuilder(DocumentsServiceImpl.class)
                .addMockedMethod("getOrMakeDraftNode")
                .createMock();
        expect(documentsService.getOrMakeDraftNode(workflow, handle)).andReturn(null);
        expect(handle.getSession()).andThrow(new RepositoryException());
        replay(documentsService, handle);

        documentsService.determineDocumentFields(null, handle, workflow, null);
    }

    @Test
    public void loadBasicFields() throws Exception {
        final Document document = new Document();
        final Locale locale = new Locale("en");
        final DocumentTypeSpec docType = new DocumentTypeSpec();
        final Node handle = createMock(Node.class);
        final Node draft = rootNode.addNode("test", "test");
        final Session session = createMock(Session.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        FieldTypeSpec field = new FieldTypeSpec();
        field.setId("present-string-field");
        field.setType(FieldTypeSpec.Type.STRING);
        docType.addField(field);

        field = new FieldTypeSpec();
        field.setId("absent-string-field");
        field.setType(FieldTypeSpec.Type.STRING);
        docType.addField(field);

        field = new FieldTypeSpec();
        field.setId("present-multiline-string-field");
        field.setType(FieldTypeSpec.Type.MULTILINE_STRING);
        docType.addField(field);

        field = new FieldTypeSpec();
        field.setId("present-multiple-string-field");
        field.setType(FieldTypeSpec.Type.STRING);
        field.setStoredAsMultiValueProperty(true);
        docType.addField(field);

        field = new FieldTypeSpec();
        field.setId("empty-multiple-string-field");
        field.setType(FieldTypeSpec.Type.STRING);
        field.setStoredAsMultiValueProperty(true);
        docType.addField(field);

        field = new FieldTypeSpec();
        field.setId("absent-multiple-string-field");
        field.setType(FieldTypeSpec.Type.STRING);
        field.setStoredAsMultiValueProperty(true);
        docType.addField(field);

        draft.setProperty("present-string-field", "Present String Field");
        draft.setProperty("present-multiline-string-field", "Present Multiline Sting Field");
        draft.setProperty("present-multiple-string-field", new String[] { "one", "two", "three" });
        draft.setProperty("empty-multiple-string-field", new String[] { });

        documentsService = createMockBuilder(DocumentsServiceImpl.class)
                .addMockedMethod("getOrMakeDraftNode")
                .addMockedMethod("getDocumentTypeSpec")
                .createMock();
        expect(documentsService.getOrMakeDraftNode(workflow, handle)).andReturn(draft);
        expect(documentsService.getDocumentTypeSpec(document, session, locale)).andReturn(docType);
        expect(handle.getSession()).andReturn(session);
        replay(documentsService, handle);

        documentsService.determineDocumentFields(document, handle, workflow, locale);

        final Map<String, Object> fields = document.getFields();
        assertThat(fields.get("present-string-field"), equalTo("Present String Field"));
        assertThat("absent string field is not present", !fields.containsKey("absent-string-field"));
        assertThat(fields.get("present-multiline-string-field"), equalTo("Present Multiline Sting Field"));
        assertThat(((List<String>)fields.get("present-multiple-string-field")).size(), equalTo(3));
        assertThat(((List<String>)fields.get("present-multiple-string-field")).get(0), equalTo("one"));
        assertThat(((List<String>)fields.get("present-multiple-string-field")).get(1), equalTo("two"));
        assertThat("empty multiple string field is not present", !fields.containsKey("empty-multiple-string-field"));
        assertThat("absent multiple string field is not present", !fields.containsKey("absent-multiple-string-field"));
    }

    @Test(expected = DocumentNotFoundException.class)
    public void failToDeriveHints() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(workflow.hints()).andThrow(new WorkflowException("bla"));
        replay(workflow);

        documentsService.getOrMakeDraftNode(workflow, null);
    }

    @Test
    public void obtainEditableInstance() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final Session session = createMock(Session.class);
        final org.hippoecm.repository.api.Document document = createMock(org.hippoecm.repository.api.Document.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("obtainEditableInstance", Boolean.TRUE);

        expect(workflow.hints()).andReturn(hints);
        expect(workflow.obtainEditableInstance()).andReturn(document);
        expect(handle.getSession()).andReturn(session);
        expect(document.getNode(session)).andReturn(draft);
        replay(workflow, handle, document);

        assertThat(documentsService.getOrMakeDraftNode(workflow, handle), equalTo(draft));
    }

    @Test(expected = DocumentNotFoundException.class)
    public void failToJustGetDraft() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Node handle = createMock(Node.class);
        final Session session = createMock(Session.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("obtainEditableInstance", Boolean.FALSE);

        PowerMock.mockStatic(WorkflowUtils.class);
        expect(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)).andReturn(Optional.empty());
        PowerMock.replayAll();

        expect(workflow.hints()).andReturn(hints);
        expect(handle.getSession()).andReturn(session);
        replay(workflow, handle);

        documentsService.getOrMakeDraftNode(workflow, handle);
    }

    @Test
    public void justGetDraft() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final Session session = createMock(Session.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("obtainEditableInstance", Boolean.FALSE);

        PowerMock.mockStatic(WorkflowUtils.class);
        expect(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT)).andReturn(Optional.of(draft));
        PowerMock.replayAll();

        expect(workflow.hints()).andReturn(hints);
        expect(handle.getSession()).andReturn(session);
        replay(workflow, handle);

        assertThat(documentsService.getOrMakeDraftNode(workflow, handle), equalTo(draft));
    }
}
