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
import org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockSession;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

public class FolderUtilsTest {

    private MockNode root;
    private MockSession session;

    @Before
    public void setUp() throws RepositoryException {
        root = MockNode.root();
        session = root.getSession();
    }

    @Test(expected = IllegalAccessException.class)
    public void cannotCreateInstance() throws Exception {
        FolderUtils.class.newInstance();
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
        replayAll();
        FolderUtils.nodeExists(mockNode, "test");
    }

    @Test
    public void nodeWithDisplayNameExists() throws Exception {
        root.addNode("a", "nt:unstructured");
        root.addNode("b", "nt:unstructured").setProperty("hippo:name", "Not a folder");
        root.addNode("c", "hippostd:folder");
        root.addNode("d", "hippostd:folder").setProperty("hippo:name", "Folder");
        root.addNode("e", "hippo:handle").setProperty("hippo:name", "Document");

        // wrong type
        assertFalse(FolderUtils.nodeWithDisplayNameExists(root, "a"));
        assertFalse(FolderUtils.nodeWithDisplayNameExists(root, "b"));
        assertFalse(FolderUtils.nodeWithDisplayNameExists(root, "Not a folder"));

        // display name falls back to node name
        assertTrue(FolderUtils.nodeWithDisplayNameExists(root, "c"));
        assertFalse(FolderUtils.nodeWithDisplayNameExists(root, "d"));
        assertFalse(FolderUtils.nodeWithDisplayNameExists(root, "e"));

        // checks hippo:name property?
        assertTrue(FolderUtils.nodeWithDisplayNameExists(root, "Folder"));
        assertTrue(FolderUtils.nodeWithDisplayNameExists(root, "Document"));

        // weird input
        assertFalse(FolderUtils.nodeWithDisplayNameExists(root, ""));
        assertFalse(FolderUtils.nodeWithDisplayNameExists(root, null));
    }

    @Test(expected = InternalServerErrorException.class)
    public void nodeWithDisplayNameExistsThrowsException() throws Exception {
        final Node mockNode = createMock(Node.class);
        expect(mockNode.getNodes()).andThrow(new RepositoryException());
        expect(mockNode.getPath()).andThrow(new RepositoryException());
        replayAll();
        FolderUtils.nodeWithDisplayNameExists(mockNode, "test");
    }

    @Test
    public void getLocaleOfTranslatedFolder() throws Exception {
        root.addMixin("hippotranslation:translated");
        root.setProperty("hippotranslation:locale", "en_GB");
        assertThat(FolderUtils.getLocale(root), equalTo("en_GB"));
    }

    @Test
    public void getLocaleOfNonTranslatedFolder() {
        assertThat(FolderUtils.getLocale(root), equalTo(null));
    }

    @Test
    public void getLocaleThrowsException() throws Exception {
        final Node brokenNode = createMock(Node.class);
        expect(brokenNode.isNodeType(eq("hippotranslation:translated"))).andThrow(new RepositoryException());
        expect(brokenNode.getPath()).andThrow(new RepositoryException());
        replayAll();
        assertThat(FolderUtils.getLocale(brokenNode), equalTo(null));
    }

    @Test
    public void getExistingFolder() throws Exception {
        final Node test = root.addNode("test", "hippostd:folder");
        final Node child = test.addNode("child", "hippostd:folder");
        assertThat(FolderUtils.getFolder("/test", session), equalTo(test));
        assertThat(FolderUtils.getFolder("/test/child", session), equalTo(child));
    }

    @Test(expected = NotFoundException.class)
    public void getMissingFolder() throws Exception {
        FolderUtils.getFolder("/test", session);
    }

    @Test
    public void getNonFolder() throws Exception {
        root.addNode("test", "hippo:document");
        try {
            FolderUtils.getFolder("/test", session);
            fail("No Exception");
        } catch (final BadRequestException e) {
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(Reason.NOT_A_FOLDER));
            assertThat(errorInfo.getParams(), equalTo(Collections.singletonMap("path", "/test")));
        }
    }

    @Test(expected = InternalServerErrorException.class)
    public void getFolderThrowsException() throws Exception {
        final Session mockSession = createMock(Session.class);
        expect(mockSession.nodeExists("/test")).andThrow(new RepositoryException());
        replayAll();

        FolderUtils.getFolder("/test", mockSession);
    }

    @Test
    public void getOrCreateExistingFolder() throws Exception {
        final Node test = root.addNode("test", "hippostd:folder");
        final Node foo = test.addNode("foo", "hippostd:folder");

        assertThat(FolderUtils.getOrCreateFolder(root, "test", session), equalTo(test));
        assertThat(FolderUtils.getOrCreateFolder(root, "test/foo", session), equalTo(foo));
        assertThat(FolderUtils.getOrCreateFolder(test, "foo", session), equalTo(foo));
    }

    @Test
    public void getOrCreateNonFolder() throws Exception {
        root.addNode("test", "hippo:document");
        try {
            FolderUtils.getOrCreateFolder(root, "test", session);
            fail("No Exception");
        } catch (final BadRequestException e) {
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(Reason.NOT_A_FOLDER));
            assertThat(errorInfo.getParams(), equalTo(Collections.singletonMap("path", "/test")));
        }
    }

    @Test(expected = InternalServerErrorException.class)
    public void getOrCreateFolderThrowsException() throws Exception {
        final Node mockNode = createMock(Node.class);
        expect(mockNode.hasNode("test")).andThrow(new RepositoryException());
        expect(mockNode.getPath()).andThrow(new RepositoryException());
        replayAll();

        FolderUtils.getOrCreateFolder(mockNode, "test", session);
    }

    @Test(expected = InternalServerErrorException.class)
    public void createNewFolderWithoutFolderWorkflowOnParent() throws Exception {
        final WorkflowManager workflowManager = createMock(WorkflowManager.class);
        root.getSession().getWorkspace().setWorkflowManager(workflowManager);

        final Workflow wrongWorkflow = createMock(Workflow.class);
        expect(workflowManager.getWorkflow(eq("internal"), eq(root))).andReturn(wrongWorkflow);

        replayAll();

        FolderUtils.getOrCreateFolder(root, "test", session);
    }

    @Test(expected = InternalServerErrorException.class)
    public void createNewFolderAndWorkflowFails() throws Exception {
        root.setPrimaryType("hippostd:folder");

        final WorkflowManager workflowManager = createMock(WorkflowManager.class);
        root.getSession().getWorkspace().setWorkflowManager(workflowManager);

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(workflowManager.getWorkflow(eq("internal"), eq(root))).andReturn(folderWorkflow);
        expect(folderWorkflow.add("new-folder", "hippostd:folder", "test")).andThrow(new WorkflowException("eek"));

        replayAll();

        FolderUtils.getOrCreateFolder(root, "test", session);
    }

    @Test
    public void createNewFolder() throws Exception {
        root.setPrimaryType("hippostd:folder");
        root.setProperty("hippostd:foldertype", new String[]{"new-folder"});

        final WorkflowManager workflowManager = createMock(WorkflowManager.class);
        session.getWorkspace().setWorkflowManager(workflowManager);

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(workflowManager.getWorkflow(eq("internal"), eq(root))).andReturn(folderWorkflow);
        expect(folderWorkflow.add("new-folder", "hippostd:folder", "test")).andAnswer(
                () -> root.addNode("test", "hippostd:folder").getPath()
        );

        replayAll();

        final Node test = FolderUtils.getOrCreateFolder(root, "test", session);

        verifyAll();
        assertSingleChild(root);
        assertThat(test, equalTo(test));
        assertFolderTypes(test, "new-folder");
    }

    @Test
    public void createNewDirectory() throws Exception {
        root.setPrimaryType("hippostd:directory");
        root.setProperty("hippostd:foldertype", new String[]{"new-folder"});

        final WorkflowManager workflowManager = createMock(WorkflowManager.class);
        session.getWorkspace().setWorkflowManager(workflowManager);

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(workflowManager.getWorkflow(eq("internal"), eq(root))).andReturn(folderWorkflow);
        expect(folderWorkflow.add("new-folder", "hippostd:directory", "test")).andAnswer(
                () -> root.addNode("test", "hippostd:directory").getPath()
        );

        replayAll();

        final Node test = FolderUtils.getOrCreateFolder(root, "test", session);

        verifyAll();
        assertSingleChild(root);
        assertThat(test, equalTo(test));
        assertFolderTypes(test, "new-folder");
    }

    @Test
    public void createNewFolderWithMultipleFolderTypes() throws Exception {
        root.setPrimaryType("hippostd:folder");
        root.setProperty("hippostd:foldertype", new String[]{"new-folder", "new-other-folder"});

        final WorkflowManager workflowManager = createMock(WorkflowManager.class);
        root.getSession().getWorkspace().setWorkflowManager(workflowManager);

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(workflowManager.getWorkflow(eq("internal"), eq(root))).andReturn(folderWorkflow);
        expect(folderWorkflow.add("new-folder", "hippostd:folder", "test")).andAnswer(
                () -> root.addNode("test", "hippostd:folder").getPath()
        );

        replayAll();

        final Node returnedNode = FolderUtils.getOrCreateFolder(root, "test", session);

        verifyAll();
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
        translatedNode.setProperty("hippostd:foldertype", new String[]{"new-translated-folder"});

        final WorkflowManager workflowManager = createMock(WorkflowManager.class);
        root.getSession().getWorkspace().setWorkflowManager(workflowManager);

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(workflowManager.getWorkflow(eq("internal"), eq(translatedNode))).andReturn(folderWorkflow);
        expect(folderWorkflow.add("new-translated-folder", "hippostd:folder", "test")).andAnswer(
                () -> translatedNode.addNode("test", "hippostd:folder").getPath()
        );

        replayAll();

        final Node returnedNode = FolderUtils.getOrCreateFolder(root, "translated/test", session);

        verifyAll();
        assertSingleChild(translatedNode);
        assertThat(returnedNode, equalTo(root.getNode("translated/test")));
        assertFolderTypes(returnedNode, "new-translated-folder");
    }

    @Test
    public void createAllNewFolders() throws Exception {
        final String[] folderTypes = {"new-folder"};
        root.setPrimaryType("hippostd:folder");
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

        replayAll();

        final Node createdNode = FolderUtils.getOrCreateFolder(root, "one/two", session);

        verifyAll();

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

        replayAll();

        final Node createdNode = FolderUtils.getOrCreateFolder(root, "one/two/three", session);

        verifyAll();

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