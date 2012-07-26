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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.rmi.RemoteException;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.TestCase;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WorkflowChainingTest extends TestCase {


    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        Node node, root = session.getRootNode();

        node = root.getNode("hippo:configuration/hippo:workflows");

        if (!node.hasNode("test"))
            node = node.addNode("test", "hipposys:workflowcategory");
        else
            node = node.getNode("test");
        if (!node.hasNode("chaining")) {
            node = node.addNode("chaining", "hipposys:workflow");
            node.setProperty("hipposys:nodetype", "hippo:document");
            node.setProperty("hipposys:display", "Test workflow chaining");
            node.setProperty("hipposys:classname", "org.hippoecm.repository.test.ChainingImpl");
            Node types = node.getNode("hipposys:types");
            node = types.addNode("org.hippoecm.repository.api.Document", "hipposys:type");
            node.setProperty("hipposys:nodetype", "hippo:document");
            node.setProperty("hipposys:display", "Document");
            node.setProperty("hipposys:classname", "org.hippoecm.repository.api.Document");
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

    @Test
    public void testScheduled() throws WorkflowException, MappingException, RepositoryException, RemoteException, InterruptedException {
        Node node = session.getRootNode().getNode("test/testdocument/testdocument");
        assertNotNull(node);
        ChainingImpl.result.clear();
        Chaining workflow = (Chaining)((HippoWorkspace)session.getWorkspace()).getWorkflowManager().getWorkflow("test",node);
        Date schedule = new Date();
        final long delay = 10L;
        schedule.setTime(schedule.getTime()+delay*1000L);
        assertEquals(0, ChainingImpl.result.size());
        workflow.schedule(schedule);
        session.save();
        session.refresh(false);
        Thread.sleep(delay*1000L/2);
        assertEquals(0, ChainingImpl.result.size());
        Thread.sleep((delay+10)*1000L);
        for(int i=0; i<120; i++) {
            if(ChainingImpl.result.size() > 0)
                break;
            Thread.sleep(1000L);
        }
        assertEquals(1, ChainingImpl.result.size());
    }
}
