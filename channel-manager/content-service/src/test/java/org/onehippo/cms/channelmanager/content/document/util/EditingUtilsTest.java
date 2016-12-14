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

package org.onehippo.cms.channelmanager.content.document.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.junit.Test;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
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
    public void determineEditingFailureWithException() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Session session = createMock(Session.class);

        expect(workflow.hints()).andThrow(new RepositoryException());
        replay(workflow);

        assertFalse(EditingUtils.determineEditingFailure(workflow, session).isPresent());

        verify(workflow);
    }

    @Test
    public void determineEditingFailureUnknown() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Session session = createMock(Session.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(workflow.hints()).andReturn(hints);
        replay(workflow);

        assertFalse(EditingUtils.determineEditingFailure(workflow, session).isPresent());

        verify(workflow);
    }

    @Test
    public void determineEditingFailureRequestPending() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Session session = createMock(Session.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("requests", Boolean.TRUE);

        expect(workflow.hints()).andReturn(hints);
        replay(workflow);

        final ErrorInfo errorInfo = EditingUtils.determineEditingFailure(workflow, session).get();
        assertThat(errorInfo.getReason(), equalTo(ErrorInfo.Reason.REQUEST_PENDING));
        assertNull(errorInfo.getParams());

        verify(workflow);
    }

    @Test
    public void determineEditingFailureInUseByWithName() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Session session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final SecurityService securityService = createMock(SecurityService.class);
        final User user = createMock(User.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("inUseBy", "admin");

        expect(workflow.hints()).andReturn(hints);
        expect(session.getWorkspace()).andReturn(workspace);
        expect(workspace.getSecurityService()).andReturn(securityService);
        expect(securityService.getUser("admin")).andReturn(user);
        expect(user.getFirstName()).andReturn(" John ");
        expect(user.getLastName()).andReturn(" Doe ");
        replay(workflow, session, workspace, securityService, user);

        final ErrorInfo errorInfo = EditingUtils.determineEditingFailure(workflow, session).get();
        assertThat(errorInfo.getReason(), equalTo(ErrorInfo.Reason.OTHER_HOLDER));
        assertThat(errorInfo.getParams().get("userId"), equalTo("admin"));
        assertThat(errorInfo.getParams().get("userName"), equalTo("John Doe"));

        verify(workflow, session, workspace, securityService, user);
    }

    @Test
    public void determineEditingFailureInUseByWithoutName() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Session session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("inUseBy", "admin");

        expect(workflow.hints()).andReturn(hints);
        expect(session.getWorkspace()).andReturn(workspace);
        expect(workspace.getSecurityService()).andThrow(new RepositoryException());
        replay(workflow, session, workspace);

        final ErrorInfo errorInfo = EditingUtils.determineEditingFailure(workflow, session).get();
        assertThat(errorInfo.getReason(), equalTo(ErrorInfo.Reason.OTHER_HOLDER));
        assertThat(errorInfo.getParams().get("userId"), equalTo("admin"));
        assertNull(errorInfo.getParams().get("userName"));

        verify(workflow, session, workspace);
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

        assertThat(EditingUtils.createDraft(workflow, session).get(), equalTo(draft));

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

    @Test
    public void copyToPreviewAndKeepEditingWithException() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Session session = createMock(Session.class);

        expect(workflow.commitEditableInstance()).andThrow(new WorkflowException("bla"));
        expect(session.getUserID()).andReturn("bla");
        replay(workflow, session);

        assertFalse(EditingUtils.copyToPreviewAndKeepEditing(workflow, session).isPresent());

        verify(workflow, session);
    }

    @Test
    public void copyToPreviewAndKeepEditing() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Session session = createMock(Session.class);
        final Document document = createMock(Document.class);
        final Node draft = createMock(Node.class);

        expect(workflow.commitEditableInstance()).andReturn(null);
        expect(workflow.obtainEditableInstance()).andReturn(document);
        expect(document.getNode(session)).andReturn(draft);
        replay(workflow, document);

        assertThat(EditingUtils.copyToPreviewAndKeepEditing(workflow, session).get(), equalTo(draft));

        verify(workflow, document);
    }
}
