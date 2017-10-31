/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockSession;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FolderUtilsTest {

    private MockNode root;
    private MockSession session;

    @Before
    public void setUp() throws RepositoryException {
        root = MockNode.root();
        session = root.getSession();
    }

    @Test
    public void nodeExists() throws Exception {
        root.addNode("test", "nt:unstructured");
        assertTrue(FolderUtils.nodeExists(root, "test"));
        assertFalse(FolderUtils.nodeExists(root, "noSuchNode"));
        assertFalse(FolderUtils.nodeExists(root, ""));
        assertFalse(FolderUtils.nodeExists(root, null));
    }

    @Test(expected = InternalServerErrorException.class)
    public void nodeExistsThrowsException() throws Exception {
        final Node mockNode = createMock(Node.class);
        expect(mockNode.hasNode("test")).andThrow(new RepositoryException());
        expect(mockNode.getPath()).andThrow(new RepositoryException());
        replay(mockNode);
        FolderUtils.nodeExists(mockNode, "test");
    }

    @Test
    public void getExistingFolder() throws Exception {
        final Node test = root.addNode("test", "hippostd:folder");
        final Node foo = test.addNode("foo", "hippostd:folder");

        assertThat(FolderUtils.getOrCreateFolder("/test", session), equalTo(test));
        assertThat(FolderUtils.getOrCreateFolder("/test/foo", session), equalTo(foo));
    }

    @Test
    public void getNotFolder() throws Exception {
        root.addNode("test", "hippo:document");
        try {
            FolderUtils.getOrCreateFolder("/test", session);
            fail("No Exception");
        } catch (BadRequestException e) {
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(ErrorInfo.Reason.NOT_A_FOLDER));
            assertThat(errorInfo.getParams(), equalTo(Collections.singletonMap("path", "/test")));
        }
    }

    @Test(expected = InternalServerErrorException.class)
    public void repositoryFails() throws Exception {
        final Session mockSession = createMock(Session.class);
        expect(mockSession.nodeExists("/test")).andThrow(new RepositoryException());
        replay(mockSession);

        FolderUtils.getOrCreateFolder("/test", mockSession);
    }

    @Test(expected = InternalServerErrorException.class)
    public void createNewFolderWithoutFolderWorkflowOnParent() throws Exception {
        final WorkflowManager workflowManager = createMock(WorkflowManager.class);
        root.getSession().getWorkspace().setWorkflowManager(workflowManager);

        final Workflow wrongWorkflow = createMock(Workflow.class);
        expect(workflowManager.getWorkflow(eq("internal"), eq(root))).andReturn(wrongWorkflow);

        final Object[] mocks = { workflowManager, wrongWorkflow };
        replay(mocks);

        FolderUtils.getOrCreateFolder("/test", session);
    }

    @Test(expected = InternalServerErrorException.class)
    public void createNewFolderAndWorkflowFails() throws Exception {
        final WorkflowManager workflowManager = createMock(WorkflowManager.class);
        root.getSession().getWorkspace().setWorkflowManager(workflowManager);

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(workflowManager.getWorkflow(eq("internal"), eq(root))).andReturn(folderWorkflow);
        expect(folderWorkflow.add("new-folder", "hippostd:folder", "test")).andThrow(new WorkflowException("eek"));

        final Object[] mocks = { workflowManager, folderWorkflow };
        replay(mocks);

        FolderUtils.getOrCreateFolder("/test", session);
    }

    @Test
    public void createNewFolder() throws Exception {
        root.setProperty("hippostd:foldertype", new String[] { "new-folder" });

        final WorkflowManager workflowManager = createMock(WorkflowManager.class);
        root.getSession().getWorkspace().setWorkflowManager(workflowManager);

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(workflowManager.getWorkflow(eq("internal"), eq(root))).andReturn(folderWorkflow);
        expect(folderWorkflow.add("new-folder", "hippostd:folder", "test")).andAnswer(
                () -> root.addNode("test", "hippostd:folder").getPath()
        );

        final Object[] mocks = { workflowManager, folderWorkflow };
        replay(mocks);

        final Node test = FolderUtils.getOrCreateFolder("/test", session);

        verify(mocks);
        assertSingleChild(root);
        assertThat(test, equalTo(test));
        assertFolderTypes(test, "new-folder");
    }

    @Test
    public void createNewFolderWithMultipleFolderTypes() throws Exception {
        root.setProperty("hippostd:foldertype", new String[] { "new-folder", "new-other-folder" });

        final WorkflowManager workflowManager = createMock(WorkflowManager.class);
        root.getSession().getWorkspace().setWorkflowManager(workflowManager);

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(workflowManager.getWorkflow(eq("internal"), eq(root))).andReturn(folderWorkflow);
        expect(folderWorkflow.add("new-folder", "hippostd:folder", "test")).andAnswer(
                () -> root.addNode("test", "hippostd:folder").getPath()
        );

        final Object[] mocks = { workflowManager, folderWorkflow };
        replay(mocks);

        final Node returnedNode = FolderUtils.getOrCreateFolder("/test", session);

        verify(mocks);
        assertSingleChild(root);
        assertThat(returnedNode, equalTo(root.getNode("test")));

        final Value[] folderTypes = returnedNode.getProperty("hippostd:foldertype").getValues();
        assertThat(folderTypes.length, equalTo(2));
        assertThat(folderTypes[0].getString(), equalTo("new-folder"));
        assertThat(folderTypes[1].getString(), equalTo("new-other-folder"));
    }

    @Test
    public void createNewTranslatedFolder() throws Exception {
        final MockNode translatedNode = root.addNode("translated", "hippostd:folder");
        translatedNode.addMixin("hippotranslation:translated");
        translatedNode.setProperty("hippostd:foldertype", new String[] { "new-translated-folder" });

        final WorkflowManager workflowManager = createMock(WorkflowManager.class);
        root.getSession().getWorkspace().setWorkflowManager(workflowManager);

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(workflowManager.getWorkflow(eq("internal"), eq(translatedNode))).andReturn(folderWorkflow);
        expect(folderWorkflow.add("new-translated-folder", "hippostd:folder", "test")).andAnswer(
                () -> translatedNode.addNode("test", "hippostd:folder").getPath()
        );

        final Object[] mocks = { workflowManager, folderWorkflow };
        replay(mocks);

        final Node returnedNode = FolderUtils.getOrCreateFolder("/translated/test", session);

        verify(mocks);
        assertSingleChild(translatedNode);
        assertThat(returnedNode, equalTo(root.getNode("translated/test")));
        assertFolderTypes(returnedNode, "new-translated-folder");
    }

    @Test
    public void createAllNewFolders() throws Exception {
        final String[] folderTypes = {"new-folder"};
        root.setProperty("hippostd:foldertype", folderTypes);

        final WorkflowManager workflowManager = createMock(WorkflowManager.class);
        root.getSession().getWorkspace().setWorkflowManager(workflowManager);

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);

        expect(workflowManager.getWorkflow(eq("internal"), eq(root))).andReturn(folderWorkflow);
        expect(folderWorkflow.add("new-folder", "hippostd:folder", "one")).andAnswer(
                () -> root.addNode("one", "hippostd:folder").getPath()
        );

        expect(workflowManager.getWorkflow(eq("internal"), anyObject(Node.class))).andReturn(folderWorkflow);
        expect(folderWorkflow.add("new-folder", "hippostd:folder", "two")).andAnswer(
                () -> root.getNode("one").addNode("two", "hippostd:folder").getPath()
        );

        final Object[] mocks = { workflowManager, folderWorkflow };
        replay(mocks);

        final Node createdNode = FolderUtils.getOrCreateFolder("/one/two", session);

        verify(mocks);

        assertSingleChild(root);
        final Node one = root.getNode("one");
        assertFolderTypes(one, folderTypes);

        assertSingleChild(one);
        final Node two = one.getNode("two");
        assertThat(two, equalTo(createdNode));
        assertFolderTypes(two, folderTypes);
    }

    @Test
    public void createSomeNewFolders() throws Exception {
        final MockNode one = root.addNode("one", "hippostd:folder");
        final String[] folderTypes = {"new-folder"};
        one.setProperty("hippostd:foldertype", folderTypes);

        final WorkflowManager workflowManager = createMock(WorkflowManager.class);
        root.getSession().getWorkspace().setWorkflowManager(workflowManager);

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(workflowManager.getWorkflow(eq("internal"), anyObject(Node.class))).andReturn(folderWorkflow).times(2);

        expect(folderWorkflow.add("new-folder", "hippostd:folder", "two")).andAnswer(
                () -> root.getNode("one").addNode("two", "hippostd:folder").getPath()
        );
        expect(folderWorkflow.add("new-folder", "hippostd:folder", "three")).andAnswer(
                () -> root.getNode("one/two").addNode("three", "hippostd:folder").getPath()
        );

        final Object[] mocks = { workflowManager, folderWorkflow };
        replay(mocks);

        final Node createdNode = FolderUtils.getOrCreateFolder("/one/two/three", session);

        verify(mocks);

        assertSingleChild(one);
        final Node two = one.getNode("two");
        assertFolderTypes(two, folderTypes);

        assertSingleChild(two);
        final Node three = two.getNode("three");
        assertThat(three, equalTo(createdNode));
        assertFolderTypes(three, folderTypes);
    }

    private static void assertSingleChild(final Node one) throws RepositoryException {
        assertThat(one.getNodes().getSize(), equalTo(1L));
    }

    private static void assertFolderTypes(final Node node, final String... folderTypes) throws RepositoryException {
        final Value[] values = node.getProperty("hippostd:foldertype").getValues();
        assertThat(values.length, equalTo(folderTypes.length));
        for (int i = 0; i < values.length; i++) {
            assertThat(values[i].getString(), equalTo(folderTypes[i]));
        }
    }

}