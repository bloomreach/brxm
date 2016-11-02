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
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.EditingInfo;
import org.onehippo.cms.channelmanager.content.document.model.UserInfo;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EditingUtils.class, WorkflowUtils.class, JcrUtils.class})
public class EditingUtilsTest {

    @Test
    public void determineEditingInfoAvailable() throws Exception {
        final Node handle = createMock(Node.class);
        final Session session = createMock(Session.class);
        final Workflow workflow = createMock(Workflow.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("obtainEditableInstance", true);

        expect(handle.getSession()).andReturn(session);
        expect(workflow.hints()).andReturn(hints);
        replay(handle, workflow);

        final EditingInfo info = EditingUtils.determineEditingInfo(workflow, handle);

        assertThat(info.getState(), equalTo(EditingInfo.State.AVAILABLE));
        assertThat(info.getHolder(), equalTo(null));
    }

    @Test
    public void determineEditingInfoUnavailableHeldByOtherUser() throws Exception {
        final Node handle = createMock(Node.class);
        final Session session = createMock(Session.class);
        final Workflow workflow = createMock(Workflow.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("obtainEditableInstance", false);
        hints.put("inUseBy", "otherUser");
        hints.put("requests", "dummy");

        PowerMock.mockStaticPartial(EditingUtils.class, "determineHolder");

        expect(handle.getSession()).andReturn(session);
        expect(session.getUserID()).andReturn("tester");
        expect(workflow.hints()).andReturn(hints);
        expect(EditingUtils.determineHolder("otherUser", session)).andReturn(null);
        replay(handle, session, workflow);
        PowerMock.replayAll();

        final EditingInfo info = EditingUtils.determineEditingInfo(workflow, handle);

        assertThat(info.getState(), equalTo(EditingInfo.State.UNAVAILABLE_HELD_BY_OTHER_USER));
        assertThat(info.getHolder(), equalTo(null));
    }

    @Test
    public void determineEditingInfoUnavailableRequestPending() throws Exception {
        final Node handle = createMock(Node.class);
        final Session session = createMock(Session.class);
        final Workflow workflow = createMock(Workflow.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("obtainEditableInstance", false);
        hints.put("requests", "dummy");

        expect(handle.getSession()).andReturn(session);
        expect(workflow.hints()).andReturn(hints);
        replay(handle, workflow);

        final EditingInfo info = EditingUtils.determineEditingInfo(workflow, handle);

        assertThat(info.getState(), equalTo(EditingInfo.State.UNAVAILABLE_REQUEST_PENDING));
        assertThat(info.getHolder(), equalTo(null));
    }

    @Test
    public void determineEditingInfoUnavailableDueToException() throws Exception {
        final Node handle = createMock(Node.class);
        final Workflow workflow = createMock(Workflow.class);

        PowerMock.mockStaticPartial(JcrUtils.class, "getNodePathQuietly");

        expect(handle.getSession()).andThrow(new RepositoryException());
        expect(JcrUtils.getNodePathQuietly(handle)).andReturn("bla");
        replay(handle);
        PowerMock.replayAll();

        final EditingInfo info = EditingUtils.determineEditingInfo(workflow, handle);

        assertThat(info.getState(), equalTo(EditingInfo.State.UNAVAILABLE));
    }

    @Test
    public void canUpdateDocument() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(workflow.hints()).andReturn(hints).anyTimes();
        replay(workflow);

        assertThat(EditingUtils.canUpdateDocument(workflow), equalTo(false));

        hints.put("commitEditableInstance", Boolean.FALSE);
        assertThat(EditingUtils.canUpdateDocument(workflow), equalTo(false));

        hints.put("commitEditableInstance", Boolean.TRUE);
        assertThat(EditingUtils.canUpdateDocument(workflow), equalTo(true));
    }

    @Test
    public void canUpdateDocumentWithException() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(workflow.hints()).andThrow(new WorkflowException("bla"));
        replay(workflow);

        assertThat(EditingUtils.canUpdateDocument(workflow), equalTo(false));
    }

    @Test
    public void canDeleteDraft() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("disposeEditableInstance", Boolean.TRUE);

        expect(workflow.hints()).andReturn(hints);
        replay(workflow);

        assertThat(EditingUtils.canDeleteDraft(workflow), equalTo(true));
    }

    @Test
    public void determineHolderWithRepositoryException() throws Exception {
        final Session session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);

        expect(session.getWorkspace()).andReturn(workspace);
        expect(workspace.getSecurityService()).andThrow(new RepositoryException());
        replay(session, workspace);

        UserInfo info = EditingUtils.determineHolder("otherUser", session);

        assertThat(info.getId(), equalTo("otherUser"));
        assertThat(info.getDisplayName(), equalTo(null));
    }

    @Test
    public void determineHolder() throws Exception {
        final Session session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final SecurityService ss = createMock(SecurityService.class);
        final User user = createMock(User.class);

        expect(session.getWorkspace()).andReturn(workspace);
        expect(workspace.getSecurityService()).andReturn(ss);
        expect(ss.getUser("otherUser")).andReturn(user);
        expect(user.getFirstName()).andReturn(" John ");
        expect(user.getLastName()).andReturn(" Doe ");
        replay(session, workspace, ss, user);

        UserInfo info = EditingUtils.determineHolder("otherUser", session);

        assertThat(info.getId(), equalTo("otherUser"));
        assertThat(info.getDisplayName(), equalTo("John Doe"));
    }

    @Test
    public void determineHolderWithFirstNameOnly() throws Exception {
        final Session session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final SecurityService ss = createMock(SecurityService.class);
        final User user = createMock(User.class);

        expect(session.getWorkspace()).andReturn(workspace);
        expect(workspace.getSecurityService()).andReturn(ss);
        expect(ss.getUser("otherUser")).andReturn(user);
        expect(user.getFirstName()).andReturn(" John ");
        expect(user.getLastName()).andReturn("");
        replay(session, workspace, ss, user);

        UserInfo info = EditingUtils.determineHolder("otherUser", session);

        assertThat(info.getId(), equalTo("otherUser"));
        assertThat(info.getDisplayName(), equalTo("John"));
    }

    @Test
    public void determineHolderWithoutDisplayName() throws Exception {
        final Session session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final SecurityService ss = createMock(SecurityService.class);
        final User user = createMock(User.class);

        expect(session.getWorkspace()).andReturn(workspace);
        expect(workspace.getSecurityService()).andReturn(ss);
        expect(ss.getUser("otherUser")).andReturn(user);
        expect(user.getFirstName()).andReturn(null);
        expect(user.getLastName()).andReturn(null);
        replay(session, workspace, ss, user);

        UserInfo info = EditingUtils.determineHolder("otherUser", session);

        assertThat(info.getId(), equalTo("otherUser"));
        assertThat(info.getDisplayName(), equalTo(""));
    }

    @Test
    public void createDraft() throws Exception {
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final Session session = createMock(Session.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Document document = createMock(Document.class);

        expect(workflow.obtainEditableInstance()).andReturn(document);
        expect(document.getNode(session)).andReturn(draft);
        expect(handle.getSession()).andReturn(session);
        replay(workflow, document, handle);

        assertThat(EditingUtils.createDraft(workflow, handle).get(), equalTo(draft));
    }

    @Test(expected = NoSuchElementException.class)
    public void createDraftWithWorkflowException() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(workflow.obtainEditableInstance()).andThrow(new WorkflowException("bla"));
        replay(workflow);

        EditingUtils.createDraft(workflow, null).get();
    }
}
