/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.editor.repository.NamespaceWorkflow;
import org.hippoecm.editor.repository.TemplateEditorWorkflow;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.RepositoryWorkflow;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TemplateEditorWorkflowTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    String cnd1 =
        "<rep='internal'>\n" +
        "<jcr='http://www.jcp.org/jcr/1.0'>\n" +
        "<nt='http://www.jcp.org/jcr/nt/1.0'>\n" +
        "<mix='http://www.jcp.org/jcr/mix/1.0'>\n" +
        "<hippo='http://www.onehippo.org/jcr/hippo/nt/2.0'>\n" +
        "<hippotest3='http://www.hippoecm.org/test2/1.0'>\n" +
        "\n" +
        "[hippotest3:test] > hippo:document\n" +
        "- hippotest3:first (string) mandatory\n" +
        "+ hippotest3:node\n";
    String cnd2 =
        "<rep='internal'>\n" +
        "<jcr='http://www.jcp.org/jcr/1.0'>\n" +
        "<nt='http://www.jcp.org/jcr/nt/1.0'>\n" +
        "<mix='http://www.jcp.org/jcr/mix/1.0'>\n" +
        "<hippo='http://www.onehippo.org/jcr/hippo/nt/2.0'>\n" +
        "<hippotest3='http://www.hippoecm.org/test2/1.1'>\n" +
        "\n" +
        "[hippotest3:test] > hippo:document\n" +
        "- hippotest3:second (string)\n" +
        "+ hippotest3:node\n";

    @Before
    public void setUp() throws Exception {
        super.setUp(true);
        session.getRootNode().addNode("test", "nt:unstructured");
        session.save();
    }

    @After
    public void tearDown() throws Exception {
        if (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
            session.save();
        }
        super.tearDown(true);
    }

    @Test
    public void testBareNamespace() throws RepositoryException, WorkflowException, RemoteException {
        Node root = session.getRootNode();
        Node node = root.getNode("hippo:namespaces");

        WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        Workflow workflow = workflowManager.getWorkflow("internal", root);
        assertNotNull(workflow);

        ((RepositoryWorkflow) workflow).createNamespace("hippotest1", "http://www.hippoecm.org/test/1.0");
        session.refresh(false);
        assertFalse(node.hasNode("hippo:namespaces/hippotest1"));
        try {
            session.getRootNode().getNode("test").setProperty("hippotest1:test", "testing");
            session.save();
        } catch (RepositoryException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
        }
        assertTrue(session.getRootNode().getNode("test").hasProperty("hippotest1:test"));
    }

    @Ignore
    public void testTemplateEditorNamespace() throws RepositoryException, WorkflowException, RemoteException {
        Node root = session.getRootNode();
        Node node = root.getNode("hippo:namespaces");

        WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        Workflow workflow = workflowManager.getWorkflow("test", node);
        assertNotNull(workflow);

        ((TemplateEditorWorkflow) workflow).createNamespace("hippotest2", "http://www.hippoecm.org/test/1.1");
        session.refresh(false);
        assertTrue(session.getRootNode().hasNode("hippo:namespaces"));
        assertTrue(session.getRootNode().hasNode("hippo:namespaces/hippotest2"));
        try {
            session.getRootNode().getNode("test").setProperty("hippotest2:test", "testing");
            session.save();
        } catch (RepositoryException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
        }
        assertTrue(session.getRootNode().getNode("test").hasProperty("hippotest2:test"));
    }

    @Ignore
    public void testTemplateEditorType() throws RepositoryException, WorkflowException, RemoteException {
        Node root = session.getRootNode();
        Node node = root.getNode("hippo:namespaces/hippostd");

        WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        Workflow workflow = workflowManager.getWorkflow("test", node);
        assertTrue(workflow instanceof NamespaceWorkflow);
        ((NamespaceWorkflow) workflow).addDocumentType("testtype");
        assertTrue(session.getRootNode().hasNode("hippo:namespaces/hippostd/testtype"));
    }

    @Test
    public void testBareUpdate() throws RepositoryException, WorkflowException, RemoteException {
        {
            WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
            Workflow workflow = workflowManager.getWorkflow("internal", session.getRootNode());
            assertNotNull(workflow);
            assertTrue(workflow instanceof RepositoryWorkflow);

            ((RepositoryWorkflow) workflow).createNamespace("hippotest3", "http://www.hippoecm.org/test/1.0");
            session.refresh(false);

            ((RepositoryWorkflow) workflow).updateModel("hippotest3", cnd1);
        }

        session.logout();
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);

        {
            Node handle = session.getRootNode().getNode("test").addNode("testing", "hippo:handle");
            handle.addMixin("hippo:hardhandle");
            Node node = handle.addNode("testing", "hippotest3:test");
            node.addMixin("hippo:harddocument");
            node.setProperty("hippotest3:first", "foobar");
            node.addNode("hippotest3:node", "nt:unstructured");
            session.save();
            node.checkin();
            handle.checkin();

            node = session.getRootNode().getNode("test").getNode("testing").getNode("testing");
            assertEquals("hippotest3:test", node.getPrimaryNodeType().getName());
        }

        {
            WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
            Workflow workflow = workflowManager.getWorkflow("internal", session.getRootNode());
            ((RepositoryWorkflow) workflow).updateModel("hippotest3", cnd2);
        }

        session.logout();
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);

        {
            Node node = session.getRootNode().getNode("test").getNode("testing").getNode("testing");
            assertEquals("hippotest3:test", node.getPrimaryNodeType().getName());
            //assertFalse(node.hasProperty("hippotest3:first"));
            //assertFalse(node.hasProperty("hippotest3:second"));
            node.setProperty("hippotest3:second", "bla");
        }
    }

}
