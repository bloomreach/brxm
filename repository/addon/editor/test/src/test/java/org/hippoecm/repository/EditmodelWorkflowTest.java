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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.editor.cnd.CndSerializer;
import org.hippoecm.editor.repository.EditmodelWorkflow;
import org.hippoecm.editor.repository.NamespaceWorkflow;
import org.hippoecm.editor.repository.TemplateEditorWorkflow;
import org.hippoecm.frontend.editor.workflow.RemodelWorkflowPlugin;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.Change;
import org.hippoecm.repository.standardworkflow.ChangeType;
import org.hippoecm.repository.standardworkflow.RepositoryWorkflow;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class EditmodelWorkflowTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.save();
    }

    @Override
    public void tearDown() throws Exception {
        session.refresh(false);
        if (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        super.tearDown();
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

        ((EditmodelWorkflow) workflow).copy("newtype");
        session.refresh(false);

        typeNode = root.getNode("hippo:namespaces/editmodel/newtype");
        assertFalse(typeNode.isNodeType(HippoNodeType.NT_TRANSLATED));

        nodes = typeNode.getNode("hipposysedit:nodetype").getNodes("hipposysedit:nodetype");
        assertEquals(1, nodes.getSize());
        Node ntNode = nodes.nextNode();
        assertFalse(ntNode.isNodeType(HippoNodeType.NT_REMODEL));
    }

}
