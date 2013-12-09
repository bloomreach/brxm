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
package org.hippoecm.repository.test;

import java.util.Date;

import javax.jcr.Node;

import org.hippoecm.repository.api.HippoWorkspace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WorkflowChainingTest extends RepositoryTestCase {


    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        final Node root = session.getRootNode();
        final Node workflows = session.getNode("/hippo:configuration/hippo:workflows");

        final Node category = workflows.addNode("test", "hipposys:workflowcategory");
        final Node workflow = category.addNode("chaining", "hipposys:workflow");
        workflow.setProperty("hipposys:nodetype", "hippo:document");
        workflow.setProperty("hipposys:display", "Test workflow chaining");
        workflow.setProperty("hipposys:classname", "org.hippoecm.repository.test.ChainingImpl");

        final Node test = root.addNode("test");
        final Node handle = test.addNode("testdocument", "hippo:handle");
        handle.addMixin("hippo:hardhandle");
        final Node document = handle.addNode("testdocument", "hippo:document");
        document.addMixin("mix:versionable");

        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        while (session.nodeExists("/hippo:configuration/hippo:workflows/test")) {
            session.getNode("/hippo:configuration/hippo:workflows/test").remove();
        }
        session.save();
        super.tearDown();
    }

    @Test
    public void testChaining() throws Exception {
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
    public void testScheduled() throws Exception {
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
