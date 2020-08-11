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

package org.hippoecm.repository.util;

import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.value.StringValue;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.mock.MockNode;

import org.junit.Test;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class WorkflowUtilsTest {
    @Test
    public void getVariantFromHandle() throws Exception {
        final Node root = MockNode.root();
        final Node handle = root.addNode("test", HippoNodeType.NT_HANDLE);
        handle.addNode("someOtherNode", HippoNodeType.NT_FIELD);
        final Node published = handle.addNode("test", "bla:whatever");
        published.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "published");
        final Node unpublished = handle.addNode("test", "bla:whatever");
        unpublished.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "unpublished");
        final Node draft = handle.addNode("test", "bla:whatever");
        draft.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "draft");

        assertThat(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get(), equalTo(draft));
        assertThat(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get(), equalTo(unpublished));
        assertThat(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get(), equalTo(published));
    }

    @Test
    public void getVariantFromVariant() throws Exception {
        final Node root = MockNode.root();
        final Node handle = root.addNode("test", HippoNodeType.NT_HANDLE);
        final Node someOtherNode = handle.addNode("someOtherNode", HippoNodeType.NT_FIELD);
        final Node published = handle.addNode("test", "bla:whatever");
        published.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "published");
        final Node unpublished = handle.addNode("test", "bla:whatever");
        unpublished.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "unpublished");
        final Node draft = handle.addNode("test", "bla:whatever");
        draft.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "draft");

        assertThat(WorkflowUtils.getDocumentVariantNode(draft, WorkflowUtils.Variant.DRAFT).get(), equalTo(draft));
        assertThat(WorkflowUtils.getDocumentVariantNode(unpublished, WorkflowUtils.Variant.DRAFT).get(), equalTo(draft));
        assertThat(WorkflowUtils.getDocumentVariantNode(published, WorkflowUtils.Variant.DRAFT).get(), equalTo(draft));
        assertThat(WorkflowUtils.getDocumentVariantNode(someOtherNode, WorkflowUtils.Variant.DRAFT).get(), equalTo(draft));
    }

    @Test(expected = NoSuchElementException.class)
    public void getVariantFromRootNode() {
        final Node root = MockNode.root();

        Log4jInterceptor.onWarn().deny(WorkflowUtils.class).run( () -> {
            WorkflowUtils.getDocumentVariantNode(root, WorkflowUtils.Variant.DRAFT).get();
        });
    }

    @Test(expected = NoSuchElementException.class)
    public void getVariantNoHandle() throws Exception {
        final Node root = MockNode.root();
        final Node parent = root.addNode("parent", "bla:whatever");
        final Node child = parent.addNode("child", "bla:whatever");

        WorkflowUtils.getDocumentVariantNode(child, WorkflowUtils.Variant.DRAFT).get();
    }

    @Test(expected = NoSuchElementException.class)
    public void variantDoesntExist() throws Exception {
        final Node root = MockNode.root();
        final Node handle = root.addNode("test", HippoNodeType.NT_HANDLE);
        final Node published = handle.addNode("test", "bla:whatever");
        published.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "published");
        final Node unpublished = handle.addNode("test", "bla:whatever");
        unpublished.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "unpublished");

        WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();
    }

    @Test(expected = NoSuchElementException.class)
    public void variantHasNoState() throws Exception {
        final Node root = MockNode.root();
        final Node handle = root.addNode("test", HippoNodeType.NT_HANDLE);
        handle.addNode("test", "bla:whatever");

        WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();
    }

    @Test
    public void getWorkflow() throws Exception {
        final Node node = createMock(Node.class);
        final Session session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final WorkflowManager wfm = createMock(WorkflowManager.class);
        final Workflow workflow = createMock(EditableWorkflow.class);

        expect(node.getSession()).andReturn(session).anyTimes();
        expect(node.getPath()).andReturn("/bla");
        expect(session.getWorkspace()).andReturn(workspace).anyTimes();
        expect(workspace.getWorkflowManager()).andReturn(wfm).anyTimes();
        expect(wfm.getWorkflow("category", node)).andReturn(workflow).anyTimes();
        replay(node, session, workspace, wfm);

        assertThat(WorkflowUtils.getWorkflow(node, "category", EditableWorkflow.class).get(), equalTo(workflow));
        assertThat(WorkflowUtils.getWorkflow(node, "category", Workflow.class).get(), equalTo(workflow));
        assertThat(WorkflowUtils.getWorkflow(node, "category", DocumentWorkflow.class).isPresent(), equalTo(false));
    }

    @Test
    public void getWorkflowNotFound() throws Exception {
        final Node node = createMock(Node.class);
        final Session session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final WorkflowManager wfm = createMock(WorkflowManager.class);

        expect(node.getSession()).andReturn(session).anyTimes();
        expect(node.getPath()).andReturn("/bla");
        expect(session.getWorkspace()).andReturn(workspace).anyTimes();
        expect(workspace.getWorkflowManager()).andReturn(wfm).anyTimes();
        expect(wfm.getWorkflow("category", node)).andReturn(null);
        replay(node, session, workspace, wfm);

        assertFalse(WorkflowUtils.getWorkflow(node, "category", EditableWorkflow.class).isPresent());
    }

    @Test(expected = NoSuchElementException.class)
    public void getWorkflowWithRepositoryException() throws Exception {
        final Node node = createMock(Node.class);

        expect(node.getSession()).andThrow(new RepositoryException());
        replay(node);

        Log4jInterceptor.onWarn().deny(WorkflowUtils.class).run( () -> {
            WorkflowUtils.getWorkflow(node, "category", EditableWorkflow.class).get();
        });
    }

    @Test
    public void hasAvailability() throws RepositoryException {
        final Node root = MockNode.root();
        final Node variant = root.addNode("test", HippoNodeType.NT_FIELD);

        Value[] values = new Value[]{new StringValue("")};
        variant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, values);
        assertFalse(WorkflowUtils.hasAvailability(variant, "live"));

        values = new Value[]{new StringValue("live")};
        variant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, values);
        assertTrue(WorkflowUtils.hasAvailability(variant, "live"));

        values = new Value[]{new StringValue("live"), new StringValue("preview")};
        variant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, values);
        assertTrue(WorkflowUtils.hasAvailability(variant, "live"));

        values = new Value[]{new StringValue("foo"), new StringValue("bar")};
        variant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, values);
        assertFalse(WorkflowUtils.hasAvailability(variant, "live"));
    }

}
