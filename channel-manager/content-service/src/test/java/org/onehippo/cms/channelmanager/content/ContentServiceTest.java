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

package org.onehippo.cms.channelmanager.content;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.NotFoundException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.channelmanager.content.exception.DocumentNotFoundException;
import org.onehippo.cms.channelmanager.content.model.Document;
import org.onehippo.cms.channelmanager.content.model.EditingInfo;
import org.onehippo.cms.channelmanager.content.model.UserInfo;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ContentServiceTest {
    private Node rootNode;
    private Session session;
    private ContentService contentService = new ContentService();

    @Before
    public void setup() throws RepositoryException {
        rootNode = MockNode.root();
        session = rootNode.getSession();
    }

    @Test(expected = DocumentNotFoundException.class)
    public void returnNotFoundWhenDocumentHandleNotFound() throws Exception {
        contentService.getDocument(session, "unknown-uuid");
    }

    @Test(expected = DocumentNotFoundException.class)
    public void returnNotFoundWhenDocumentHandleTypeIsInvalid() throws Exception {
        final Node handle = rootNode.addNode("testDocument", "invalid-type");
        final String id = handle.getIdentifier();
        contentService.getDocument(session, id);
    }

    @Test(expected = DocumentNotFoundException.class)
    public void returnNotFoundWhenDocumentHandleHasNoVariantNode() throws Exception {
        final Node handle = rootNode.addNode("testDocument", "hippo:handle");
        final String id = handle.getIdentifier();

        handle.addNode("otherName", "ns:doctype");

        contentService.getDocument(session, id);
    }

    @Test
    public void fillIdDisplayNameAndType() throws Exception {
        final Node handle = rootNode.addNode("testDocument", "hippo:handle");
        final String id = handle.getIdentifier();

        handle.addNode("testDocument", "ns:doctype");
        handle.setProperty(HippoNodeType.HIPPO_NAME, "Test Document");
        contentService = createMockBuilder(ContentService.class).addMockedMethod("determineEditingInfo").createMock();
        expect(contentService.determineEditingInfo(session, handle)).andReturn(null);
        replay(contentService);

        final Document document = contentService.getDocument(session, id);

        assertThat(document.getId(), equalTo(id));
        assertThat(document.getDisplayName(), equalTo("Test Document"));
        assertThat(document.getInfo().getType().getId(), equalTo("ns:doctype"));
    }

    @Test
    public void readUnavailableEditingInfoInCaseOfException() throws Exception {
        final Node handle = rootNode.addNode("testDocument", "hippo:handle");
        session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);

        expect(workspace.getWorkflowManager()).andThrow(new RepositoryException());
        expect(session.getWorkspace()).andReturn(workspace);
        replay(session, workspace);

        final EditingInfo info = contentService.determineEditingInfo(session, handle);

        verify(session, workspace);
        assertThat(info.getState(), equalTo(EditingInfo.State.UNAVAILABLE));
        assertThat(info.getHolder(), equalTo(null));
    }

    @Test
    public void readAvailableEditingInfo() throws Exception {
        final Node handle = rootNode.addNode("testDocument", "hippo:handle");
        session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final WorkflowManager wfm = createMock(WorkflowManager.class);
        final Workflow wf = createMock(Workflow.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("obtainEditableInstance", true);

        expect(wf.hints()).andReturn(hints);
        expect(wfm.getWorkflow("editing", handle)).andReturn(wf);
        expect(workspace.getWorkflowManager()).andReturn(wfm);
        expect(session.getWorkspace()).andReturn(workspace);
        replay(session, workspace, wfm, wf);

        final EditingInfo info = contentService.determineEditingInfo(session, handle);

        verify(session, workspace, wfm, wf);
        assertThat(info.getState(), equalTo(EditingInfo.State.AVAILABLE));
        assertThat(info.getHolder(), equalTo(null));
    }

    @Test
    public void readUnavailableHeldByOtherUser() throws Exception {
        final Node handle = rootNode.addNode("testDocument", "hippo:handle");
        contentService = createMockBuilder(ContentService.class).addMockedMethod("determineHolder").createMock();
        session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final WorkflowManager wfm = createMock(WorkflowManager.class);
        final Workflow wf = createMock(Workflow.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("obtainEditableInstance", false);
        hints.put("inUseBy", "otherUser");
        hints.put("requests", "dummy");

        expect(wf.hints()).andReturn(hints);
        expect(wfm.getWorkflow("editing", handle)).andReturn(wf);
        expect(workspace.getWorkflowManager()).andReturn(wfm);
        expect(session.getWorkspace()).andReturn(workspace);
        expect(contentService.determineHolder("otherUser", workspace)).andReturn(null);
        replay(contentService, session, workspace, wfm, wf);

        final EditingInfo info = contentService.determineEditingInfo(session, handle);

        verify(contentService, session, workspace, wfm, wf);
        assertThat(info.getState(), equalTo(EditingInfo.State.UNAVAILABLE_HELD_BY_OTHER_USER));
        assertThat(info.getHolder(), equalTo(null));
    }

    @Test
    public void readUnavailableRequestPending() throws Exception {
        final Node handle = rootNode.addNode("testDocument", "hippo:handle");
        session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final WorkflowManager wfm = createMock(WorkflowManager.class);
        final Workflow wf = createMock(Workflow.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("obtainEditableInstance", false);
        hints.put("requests", "dummy");

        expect(wf.hints()).andReturn(hints);
        expect(wfm.getWorkflow("editing", handle)).andReturn(wf);
        expect(workspace.getWorkflowManager()).andReturn(wfm);
        expect(session.getWorkspace()).andReturn(workspace);
        replay(session, workspace, wfm, wf);

        final EditingInfo info = contentService.determineEditingInfo(session, handle);

        verify(session, workspace, wfm, wf);
        assertThat(info.getState(), equalTo(EditingInfo.State.UNAVAILABLE_REQUEST_PENDING));
        assertThat(info.getHolder(), equalTo(null));
    }

    @Test
    public void readHolderWithRepositoryException() throws Exception {
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        expect(workspace.getSecurityService()).andThrow(new RepositoryException());
        replay(workspace);

        UserInfo info = contentService.determineHolder("otherUser", workspace);

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

        UserInfo info = contentService.determineHolder("otherUser", workspace);

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

        UserInfo info = contentService.determineHolder("otherUser", workspace);

        verify(workspace, ss, user);
        assertThat(info.getId(), equalTo("otherUser"));
        assertThat(info.getDisplayName(), equalTo("John"));
    }
}
