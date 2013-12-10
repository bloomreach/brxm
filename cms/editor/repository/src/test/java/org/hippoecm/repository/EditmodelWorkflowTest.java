/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.hippoecm.editor.repository.EditmodelWorkflow;
import org.hippoecm.editor.repository.NamespaceWorkflow;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EditmodelWorkflowTest extends RepositoryTestCase {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp(true);
        if (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        session.refresh(false);
        if (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        super.tearDown();
    }

    @Test
    public void editType() throws RepositoryException, WorkflowException, RemoteException {
        Node root = session.getRootNode();

        Node typeNode = root.getNode("hippo:namespaces/editmodel/existing");

        // check initial conditions
        NodeIterator nodes = typeNode.getNode("hipposysedit:nodetype").getNodes("hipposysedit:nodetype");
        assertEquals(1, nodes.getSize());

        WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        Workflow workflow = workflowManager.getWorkflow("test", typeNode);
        assertTrue(workflow instanceof EditmodelWorkflow);

        ((EditmodelWorkflow) workflow).edit();
        session.refresh(false);

        nodes = typeNode.getNode("hipposysedit:nodetype").getNodes("hipposysedit:nodetype");
        assertEquals(2, nodes.getSize());

        int remodelled = 0;
        while (nodes.hasNext()) {
            Node node = nodes.nextNode();
            if (node.isNodeType(HippoNodeType.NT_REMODEL)) {
                remodelled++;
            }
        }
        assertEquals(1, remodelled);

        nodes = typeNode.getNode("hipposysedit:prototypes").getNodes("hipposysedit:prototype");
        assertEquals(2, nodes.getSize());
        assertTrue(nodes.nextNode().isNodeType("editmodel:existing"));
        assertTrue(nodes.nextNode().isNodeType("nt:unstructured"));
    }

    @Test
    public void mixinsAreCopiedToDraft() throws RepositoryException, WorkflowException, RemoteException {
        Node root = session.getRootNode();

        Node typeNode = root.getNode("hippo:namespaces/test/mixinTest");

        WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        Workflow workflow = workflowManager.getWorkflow("test", typeNode);
        assertTrue(workflow instanceof EditmodelWorkflow);

        ((EditmodelWorkflow) workflow).edit();
        session.refresh(false);

        NodeIterator nodes = typeNode.getNode("hipposysedit:prototypes").getNodes("hipposysedit:prototype");
        assertEquals(2, nodes.getSize());

        Node draft = null;
        while (nodes.hasNext()) {
            Node node = nodes.nextNode();
            if (node.isNodeType("nt:unstructured")) {
                draft = node;
            }
        }
        assertNotNull(draft);
        NodeType[] mixins = draft.getMixinNodeTypes();
        assertEquals(1, mixins.length);
        assertEquals("test:mixin", mixins[0].getName());
    }

    @Test
    public void superMixinsAreCopiedToDraft() throws RepositoryException, WorkflowException, RemoteException {
        Node root = session.getRootNode();

        Node typeNode = root.getNode("hippo:namespaces/test/superMixinTest");

        WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        Workflow workflow = workflowManager.getWorkflow("test", typeNode);
        assertTrue(workflow instanceof EditmodelWorkflow);

        ((EditmodelWorkflow) workflow).edit();
        session.refresh(false);

        NodeIterator nodes = typeNode.getNode("hipposysedit:prototypes").getNodes("hipposysedit:prototype");
        assertEquals(2, nodes.getSize());

        Node draft = null;
        while (nodes.hasNext()) {
            Node node = nodes.nextNode();
            if (node.isNodeType("nt:unstructured")) {
                draft = node;
            }
        }
        assertNotNull(draft);
        assertTrue(draft.isNodeType("test:mixin"));
    }

    @Test
    public void mixinsAreCopiedFromDraft() throws RepositoryException, WorkflowException, RemoteException {
        Node root = session.getRootNode();
        Node typeNode = root.getNode("hippo:namespaces/test/mixinTest");

        WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        EditmodelWorkflow workflow = (EditmodelWorkflow) workflowManager.getWorkflow("test", typeNode);
        workflow.edit();
        session.refresh(false);

        NodeIterator nodes = typeNode.getNode("hipposysedit:prototypes").getNodes("hipposysedit:prototype");
        Node draft = null;
        while (nodes.hasNext()) {
            Node node = nodes.nextNode();
            if (node.isNodeType("nt:unstructured")) {
                draft = node;
            }
        }
        assertNotNull(draft);
        draft.addMixin("test:extramixin");

        session.save();
        workflow = (EditmodelWorkflow) workflowManager.getWorkflow("test", typeNode);
        workflow.commit();

        session.refresh(false);
        Node prototype = typeNode.getNode("hipposysedit:prototypes").getNodes("hipposysedit:prototype").nextNode();
        assertTrue(prototype.isNodeType("test:extramixin"));
    }

    @Test
    public void commitType() throws RepositoryException, WorkflowException, RemoteException {
        Node root = session.getRootNode();
        Node typeNode = root.getNode("hippo:namespaces/editmodel/existing");
        WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        Workflow workflow = workflowManager.getWorkflow("test", typeNode);
        assertTrue(workflow instanceof EditmodelWorkflow);

        ((EditmodelWorkflow) workflow).edit();
        session.refresh(false);
        NodeIterator nodes;

        nodes = typeNode.getNode("hipposysedit:nodetype").getNodes("hipposysedit:nodetype");
        assertEquals(2, nodes.getSize());

        workflow = workflowManager.getWorkflow("test", typeNode);
        assertTrue(workflow instanceof EditmodelWorkflow);

        ((EditmodelWorkflow) workflow).commit();
        nodes = typeNode.getNode("hipposysedit:nodetype").getNodes("hipposysedit:nodetype");
        assertEquals(1, nodes.getSize());
    }

    @Test
    public void firstCommitType() throws RepositoryException, WorkflowException, RemoteException {
        Node root = session.getRootNode();

        WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        Workflow workflow = workflowManager.getWorkflow("test", session.getNode("/hippo:namespaces/editmodel"));
        assertNotNull(workflow);
        assertTrue(workflow instanceof NamespaceWorkflow);
        ((NamespaceWorkflow) workflow).addCompoundType("newtype");
        
        Node typeNode = root.getNode("hippo:namespaces/editmodel/newtype");
        NodeIterator nodes = typeNode.getNode("hipposysedit:nodetype").getNodes("hipposysedit:nodetype");
        assertEquals(1, nodes.getSize());
        Node ntNode = nodes.nextNode();
        assertFalse(ntNode.isNodeType(HippoNodeType.NT_REMODEL));

        NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
        assertFalse(ntMgr.hasNodeType("editmodel:newtype"));

        workflow = workflowManager.getWorkflow("test", typeNode);
        assertTrue(workflow instanceof EditmodelWorkflow);

        ((EditmodelWorkflow) workflow).commit();
        nodes = typeNode.getNode("hipposysedit:nodetype").getNodes("hipposysedit:nodetype");
        assertEquals(1, nodes.getSize());

        ntNode = nodes.nextNode();
        assertTrue(ntNode.isNodeType(HippoNodeType.NT_REMODEL));
        assertTrue(ntMgr.hasNodeType("editmodel:newtype"));
    }

    @Test
    public void copyType() throws RepositoryException, WorkflowException, RemoteException {
        Node root = session.getRootNode();

        Node typeNode = root.getNode("hippo:namespaces/editmodel/existing");
        typeNode.addMixin("hippo:translated");

        Node translation = typeNode.addNode("hippo:translation", "hippo:translation");
        translation.setProperty("hippo:language", "");
        translation.setProperty("hippo:message", "Existing");
        session.save();

        // check initial conditions
        NodeIterator nodes = typeNode.getNode("hipposysedit:nodetype").getNodes("hipposysedit:nodetype");
        assertEquals(1, nodes.getSize());

        WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        Workflow workflow = workflowManager.getWorkflow("test", typeNode);
        assertTrue(workflow instanceof EditmodelWorkflow);

        ((EditmodelWorkflow) workflow).copy("copiedtype");
        session.refresh(false);

        typeNode = root.getNode("hippo:namespaces/editmodel/copiedtype");
        assertFalse(typeNode.isNodeType(HippoNodeType.NT_TRANSLATED));

        nodes = typeNode.getNode("hipposysedit:nodetype").getNodes("hipposysedit:nodetype");
        assertEquals(1, nodes.getSize());
        Node ntNode = nodes.nextNode();
        assertFalse(ntNode.isNodeType(HippoNodeType.NT_REMODEL));

        NodeIterator prototypes = typeNode.getNode("hipposysedit:prototypes").getNodes("hipposysedit:prototype");
        assertEquals(1, prototypes.getSize());
        Node prototypeNode = prototypes.nextNode();
        assertEquals("nt:unstructured", prototypeNode.getPrimaryNodeType().getName());
    }

}
