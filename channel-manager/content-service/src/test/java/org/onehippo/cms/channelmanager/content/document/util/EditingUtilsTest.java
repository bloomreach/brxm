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

package org.onehippo.cms.channelmanager.content.document.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EditingUtilsTest {

    @Test
    public void canCreateDraft() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(workflow.hints()).andReturn(hints).anyTimes();
        replay(workflow);

        assertFalse(EditingUtils.canCreateDraft(workflow));

        hints.put("obtainEditableInstance", Boolean.FALSE);
        assertFalse(EditingUtils.canCreateDraft(workflow));

        hints.put("obtainEditableInstance", Boolean.TRUE);
        assertTrue(EditingUtils.canCreateDraft(workflow));
    }

    @Test
    public void canUpdateDocument() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(workflow.hints()).andReturn(hints).anyTimes();
        replay(workflow);

        assertFalse(EditingUtils.canUpdateDraft(workflow));

        hints.put("commitEditableInstance", Boolean.FALSE);
        assertFalse(EditingUtils.canUpdateDraft(workflow));

        hints.put("commitEditableInstance", Boolean.TRUE);
        assertTrue(EditingUtils.canUpdateDraft(workflow));
    }

    @Test
    public void canUpdateDocumentWithException() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(workflow.hints()).andThrow(new WorkflowException("bla"));
        replay(workflow);

        assertFalse(EditingUtils.canUpdateDraft(workflow));
    }

    @Test
    public void canDeleteDraft() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(workflow.hints()).andReturn(hints).anyTimes();
        replay(workflow);

        assertFalse(EditingUtils.canDeleteDraft(workflow));

        hints.put("disposeEditableInstance", Boolean.FALSE);
        assertFalse(EditingUtils.canDeleteDraft(workflow));

        hints.put("disposeEditableInstance", Boolean.TRUE);
        assertTrue(EditingUtils.canDeleteDraft(workflow));
    }

    @Test
    public void canArchiveDocument() throws Exception {
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(workflow.hints()).andReturn(hints).anyTimes();
        replay(workflow);

        assertFalse(EditingUtils.canArchiveDocument(workflow));

        hints.put("delete", Boolean.FALSE);
        assertFalse(EditingUtils.canArchiveDocument(workflow));

        hints.put("delete", Boolean.TRUE);
        assertTrue(EditingUtils.canArchiveDocument(workflow));
    }

    @Test
    public void canEraseDocument() throws Exception {
        final FolderWorkflow workflow = createMock(FolderWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(workflow.hints()).andReturn(hints).anyTimes();
        replay(workflow);

        assertFalse(EditingUtils.canEraseDocument(workflow));

        hints.put("delete", Boolean.FALSE);
        assertFalse(EditingUtils.canEraseDocument(workflow));

        hints.put("delete", Boolean.TRUE);
        assertTrue(EditingUtils.canEraseDocument(workflow));
    }

    @Test
    public void canRenameDocument() throws Exception {
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(workflow.hints()).andReturn(hints).anyTimes();
        replay(workflow);

        assertFalse(EditingUtils.canRenameDocument(workflow));

        hints.put("rename", Boolean.FALSE);
        assertFalse(EditingUtils.canRenameDocument(workflow));

        hints.put("rename", Boolean.TRUE);
        assertTrue(EditingUtils.canRenameDocument(workflow));
    }

    @Test
    public void isActionAvailable() throws Exception {
        final Workflow workflow = createMock(Workflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(workflow.hints()).andReturn(hints).anyTimes();
        replay(workflow);

        assertFalse(EditingUtils.isActionAvailable(workflow, "action"));

        hints.put("action", Boolean.FALSE);
        assertFalse(EditingUtils.isActionAvailable(workflow, "action"));

        hints.put("action", Boolean.TRUE);
        assertTrue(EditingUtils.isActionAvailable(workflow, "action"));

        hints.put("action", "aap");
        assertFalse(EditingUtils.isActionAvailable(workflow, "action"));
    }

    @Test
    public void isRequestActionAvailable() throws Exception {
        final Workflow workflow = createMock(Workflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(workflow.hints()).andReturn(hints).anyTimes();
        replay(workflow);

        assertFalse(EditingUtils.isRequestActionAvailable(workflow, "action", "uuid"));

        HashMap requests = new HashMap();
        hints.put("requests", requests);
        assertFalse(EditingUtils.isRequestActionAvailable(workflow, "action", "uuid"));

        HashMap request = new HashMap();
        requests.put("uuid", request);
        assertFalse(EditingUtils.isRequestActionAvailable(workflow, "action", "uuid"));

        request.put("action", Boolean.FALSE);
        assertFalse(EditingUtils.isRequestActionAvailable(workflow, "action", "uuid"));

        request.put("action", Boolean.TRUE);
        assertTrue(EditingUtils.isRequestActionAvailable(workflow, "action", "uuid"));

        request.put("action", "aap");
        assertFalse(EditingUtils.isRequestActionAvailable(workflow, "action", "uuid"));
    }

    @Test
    public void isHintActionAvailable() {
        final Map<String, Serializable> hints = new HashMap<>();
        assertFalse(EditingUtils.isHintActionTrue(hints, "action"));

        hints.put("action", Boolean.FALSE);
        assertFalse(EditingUtils.isHintActionTrue(hints, "action"));

        hints.put("action", Boolean.TRUE);
        assertTrue(EditingUtils.isHintActionTrue(hints, "action"));

        hints.put("action", "no-boolean");
        assertFalse(EditingUtils.isHintActionTrue(hints, "action"));
    }

    @Test
    public void hasPreview() throws Exception {
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(workflow.hints()).andReturn(hints).anyTimes();
        replay(workflow);

        assertFalse(EditingUtils.hasPreview(workflow));

        hints.put("previewAvailable", Boolean.FALSE);
        assertFalse(EditingUtils.hasPreview(workflow));

        hints.put("previewAvailable", Boolean.TRUE);
        assertTrue(EditingUtils.hasPreview(workflow));
    }

    @Test
    public void getUserNameNoFirstName() throws Exception {
        final Session session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final SecurityService securityService = createMock(SecurityService.class);
        final User user = createMock(User.class);

        expect(session.getWorkspace()).andReturn(workspace);
        expect(workspace.getSecurityService()).andReturn(securityService);
        expect(securityService.getUser("admin")).andReturn(user);
        expect(user.getFirstName()).andReturn(null);
        expect(user.getLastName()).andReturn(" Doe ");
        replay(session, workspace, securityService, user);

        assertThat(EditingUtils.getUserName("admin", session).get(), equalTo("Doe"));

        verify(session, workspace, securityService, user);
    }

    @Test
    public void getUserNameNoLastName() throws Exception {
        final Session session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final SecurityService securityService = createMock(SecurityService.class);
        final User user = createMock(User.class);

        expect(session.getWorkspace()).andReturn(workspace);
        expect(workspace.getSecurityService()).andReturn(securityService);
        expect(securityService.getUser("admin")).andReturn(user);
        expect(user.getFirstName()).andReturn(" John ");
        expect(user.getLastName()).andReturn(null);
        replay(session, workspace, securityService, user);

        assertThat(EditingUtils.getUserName("admin", session).get(), equalTo("John"));

        verify(session, workspace, securityService, user);
    }

    @Test
    public void createDraft() throws Exception {
        final Node draft = createMock(Node.class);
        final Session session = createMock(Session.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Document document = createMock(Document.class);

        expect(workflow.obtainEditableInstance()).andReturn(document);
        expect(document.getNode(session)).andReturn(draft);
        replay(workflow, document);

        final Optional<Node> draftOptional = EditingUtils.createDraft(workflow, session);
        assertThat("There should be a draft", draftOptional.isPresent());
        if (draftOptional.isPresent()) {
            assertThat(draftOptional.get(), equalTo(draft));
        }

        verify(workflow, document);
    }

    @Test
    public void createDraftWithWorkflowException() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Session session = createMock(Session.class);

        expect(workflow.obtainEditableInstance()).andThrow(new WorkflowException("bla"));
        expect(session.getUserID()).andReturn("bla");
        replay(workflow, session);

        assertFalse(EditingUtils.createDraft(workflow, session).isPresent());

        verify(workflow, session);
    }
}
