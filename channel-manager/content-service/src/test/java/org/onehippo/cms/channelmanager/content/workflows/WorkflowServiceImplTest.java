/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.workflows;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockSession;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.apache.jackrabbit.JcrConstants.MIX_VERSIONABLE;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({
        DocumentUtils.class,
        EditingUtils.class,
        WorkflowUtils.class
})
public class WorkflowServiceImplTest {


    private WorkflowServiceImpl workflowService;
    private Session session;

    @Before
    public void setup() {
        workflowService = new WorkflowServiceImpl();
        session = createMock(Session.class);

        PowerMock.mockStatic(DocumentUtils.class);
        PowerMock.mockStatic(WorkflowUtils.class);
        PowerMock.mockStatic(EditingUtils.class);
    }

    @Test
    public void executeDocumentWorkflowErrorForUnimplementedAction() throws Exception {
        final String uuid = "uuid";
        final String action = "actionNotYetImplemented";

        final Node handle = createMock(Node.class);
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(documentWorkflow.hints("master")).andStubReturn(hints);
        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:nodetype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(documentWorkflow));
        expect(EditingUtils.isActionAvailable(eq(action), eq(hints))).andReturn(true);

        replayAll();

        try {
            workflowService.executeDocumentWorkflowAction(uuid, action, session, "master");
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(ErrorInfo.Reason.WORKFLOW_ACTION_NOT_IMPLEMENTED));
        }

        verifyAll();
    }

    @Test
    public void executeDocumentWorkflowErrorForUnavailableAction() throws Exception {
        final String uuid = "uuid";
        final String action = "publish";

        final Node handle = createMock(Node.class);
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(documentWorkflow.hints("master")).andStubReturn(hints);
        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:nodetype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(documentWorkflow));
        expect(EditingUtils.isActionAvailable(eq(action), eq(hints))).andReturn(false);

        replayAll();

        try {
            workflowService.executeDocumentWorkflowAction(uuid, action, session, "master");
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(ErrorInfo.Reason.WORKFLOW_ACTION_NOT_AVAILABLE));
        }

        verifyAll();
    }

    @Test
    public void executeDocumentWorkflowPublishAction() throws Exception {
        final String uuid = "uuid";
        final String action = "publish";

        final Node handle = createMock(Node.class);
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(documentWorkflow.hints("master")).andStubReturn(hints);
        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:nodetype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(documentWorkflow));
        expect(EditingUtils.isActionAvailable(eq(action), eq(hints))).andReturn(true);

        documentWorkflow.publish();

        replayAll();

        workflowService.executeDocumentWorkflowAction(uuid, action, session, "master");

        verifyAll();
    }

    @Test
    public void executeDocumentWorkflowRequestPublicationAction() throws Exception {
        final String uuid = "uuid";
        final String action = "requestPublication";

        final Node handle = createMock(Node.class);
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(documentWorkflow.hints("master")).andStubReturn(hints);
        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:nodetype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(documentWorkflow));
        expect(EditingUtils.isActionAvailable(eq(action), eq(hints))).andReturn(true);

        documentWorkflow.requestPublication();

        replayAll();

        workflowService.executeDocumentWorkflowAction(uuid, action, session, "master");

        verifyAll();
    }

    @Test
    public void executeDocumentWorkflowErrorForCancelRequestWithoutPendingRequest() throws Exception {
        final String uuid = "uuid";
        final String action = "cancelRequest";

        final Node handle = createMock(Node.class);
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        final NodeIterator nodeIterator = createMock(NodeIterator.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(documentWorkflow.hints("master")).andStubReturn(hints);
        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:nodetype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(documentWorkflow));
        expect(handle.getNodes("hippo:request")).andReturn(nodeIterator);
        expect(nodeIterator.hasNext()).andReturn(false);

        replayAll();

        try {
            workflowService.executeDocumentWorkflowAction(uuid, action, session, "master");
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(ErrorInfo.Reason.NO_REQUEST_PENDING));
        }

        verifyAll();
    }

    @Test
    public void executeDocumentWorkflowErrorForCancelRequestWithMultipleRequestsPending() throws Exception {
        final String uuid = "uuid";
        final String action = "cancelRequest";

        final Node handle = createMock(Node.class);
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        final NodeIterator nodeIterator = createMock(NodeIterator.class);
        final Node requestNode = createMock(Node.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(documentWorkflow.hints("master")).andStubReturn(hints);
        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:nodetype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(documentWorkflow));
        expect(handle.getIdentifier()).andReturn(uuid);
        expect(handle.getNodes("hippo:request")).andReturn(nodeIterator);
        expect(nodeIterator.hasNext()).andReturn(true).times(2);
        expect(nodeIterator.nextNode()).andReturn(requestNode);

        replayAll();

        try {
            workflowService.executeDocumentWorkflowAction(uuid, action, session, "master");
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(ErrorInfo.Reason.MULTIPLE_REQUESTS));
        }

        verifyAll();
    }

    @Test
    public void executeDocumentWorkflowCancelRequestAction() throws Exception {
        final String uuid = "uuid";
        final String action = "cancelRequest";
        final String requestUUID = "requestUUID";

        final Node handle = createMock(Node.class);
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        final NodeIterator nodeIterator = createMock(NodeIterator.class);
        final Node requestNode = createMock(Node.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(documentWorkflow.hints("master")).andStubReturn(hints);
        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:nodetype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(documentWorkflow));
        expect(EditingUtils.isRequestActionAvailable(eq(action), eq(requestUUID), eq(hints))).andReturn(true);
        expect(requestNode.getIdentifier()).andReturn(requestUUID).times(2);
        expect(handle.getNodes("hippo:request")).andReturn(nodeIterator);
        expect(nodeIterator.hasNext()).andReturn(true);
        expect(nodeIterator.nextNode()).andReturn(requestNode);
        expect(nodeIterator.hasNext()).andReturn(false);

        documentWorkflow.cancelRequest(eq(requestUUID));

        replayAll();

        workflowService.executeDocumentWorkflowAction(uuid, action, session, "master");

        verifyAll();
    }

    @Test
    public void executeDocumentWorkflowVersionAction() throws Exception {
        final String uuid = "uuid";
        final String action = "version";

        final Node handle = createMock(Node.class);
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(documentWorkflow.hints("master")).andStubReturn(hints);
        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:nodetype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(documentWorkflow));
        expect(EditingUtils.isActionAvailable(eq(action), eq(hints))).andReturn(true);

        final Document document = new Document();

        expect(documentWorkflow.version()).andStubReturn(document);
        expect(documentWorkflow.listBranches()).andStubReturn(Collections.singleton("master"));

        replayAll();

        workflowService.executeDocumentWorkflowAction(uuid, action, session, "master");

        verifyAll();
    }

    @Test
    public void restoreDocumentWorkflowAction() throws Exception {

        final MockNode handle = new MockNode("myDoc", "hippo:handle");
        final MockNode unpublished = handle.addNode("myDoc", "myproject:news");

        unpublished.addMixin(MIX_VERSIONABLE);

        final MockSession session = handle.getSession();
        session.setUserId("john");

        VersionManager versionManager = session.getWorkspace().getVersionManager();

        final Version version = versionManager.checkin(unpublished.getPath());
        final Node frozenUnpublished = version.getFrozenNode();

        versionManager.checkout(unpublished.getPath());

        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(documentWorkflow.hints("master")).andStubReturn(hints);

        expect(DocumentUtils.getHandle(handle.getIdentifier(), session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("hippo:handle"));

        expect(EditingUtils.isActionAvailable(eq("restoreVersionToBranch"), eq(hints))).andReturn(true);

        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(documentWorkflow));

        final Document document = new Document();

        expect(documentWorkflow.restoreVersionToBranch(eq(version), eq("master"))).andStubReturn(document);

        replayAll();

        workflowService.restoreDocumentWorkflowAction(handle.getIdentifier(), frozenUnpublished.getIdentifier(), session, "master");

        verifyAll();
    }
}
