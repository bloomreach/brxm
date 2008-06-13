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
package org.hippoecm.repository.test;

import java.rmi.RemoteException;
import java.util.Vector;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.Workflow;

import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.TestCase;

import org.junit.*;
import static org.junit.Assert.*;

public class WorkflowChainingTest extends TestCase {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        Node node, root = session.getRootNode();

        node = root.getNode("hippo:configuration/hippo:workflows");

        if (!node.hasNode("test"))
            node = node.addNode("test", "hippo:workflowcategory");
        else
            node = node.getNode("test");
        if (!node.hasNode("chaining")) {
            node = node.addNode("chaining", "hippo:workflow");
            node.setProperty("hippo:nodetype", "hippo:document");
            node.setProperty("hippo:display", "Test workflow chaining");
            node.setProperty("hippo:renderer", "");
            node.setProperty("hippo:classname", "org.hippoecm.repository.api.Document");
            node.setProperty("hippo:workflow", "org.hippoecm.repository.test.ChainingImpl");
            Node types = node.getNode("hippo:types");
            node = types.addNode("org.hippoecm.repository.api.Document", "hippo:type");
            node.setProperty("hippo:nodetype", "hippo:document");
            node.setProperty("hippo:display", "Document");
            node.setProperty("hippo:classname", "org.hippoecm.repository.api.Document");
        }

        if (!root.hasNode("test")) {
            root = root.addNode("test");
        } else {
            root = root.getNode("test");
        }

        node = root.addNode("testdocument", "hippo:handle");
        node.addMixin("hippo:hardhandle");
        node = node.addNode("testdocument", "hippo:document");
        node.addMixin("hippo:harddocument");

        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        session.refresh(false);
        if (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        if (session.getRootNode().hasNode("hippo:configuration/hippo:workflows/test")) {
            session.getRootNode().getNode("hippo:configuration/hippo:workflows/test").remove();
        }
        super.tearDown();
    }

    @Test
    public void testChaining() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node node = session.getRootNode().getNode("test/testdocument/testdocument");
        assertNotNull(node);
        synchronized(ChainingImpl.result) {
            ChainingImpl.result.clear();
            Chaining workflow = (Chaining)((HippoWorkspace)session.getWorkspace()).getWorkflowManager().getWorkflow("test",node);
            workflow.test();
            session.save();
            session.refresh(false);
            assertEquals(6, ChainingImpl.result.size());
            for(int i=0; i<6; i++)
                assertEquals(""+(i+1), ChainingImpl.result.get(i));
            ChainingImpl.result.clear();
        }
    }
}
