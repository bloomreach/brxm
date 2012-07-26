/*
 *  Copyright 2011 Hippo.
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
package org.onehippo.cms7.repository.wfdropbox;

import java.util.Calendar;
import java.util.Date;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.TestCase;
import org.hippoecm.repository.util.Utilities;
import org.onehippo.cms7.test.TestWorkflow;
import org.onehippo.cms7.test.TestWorkflowImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DropboxTest extends TestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp(true);
        Node node = session.getRootNode().getNode("hippo:configuration/hippo:workflows");
        node = node.addNode("test");
        node = node.addNode("test","hipposys:workflow");
        node.setProperty("hipposys:nodetype", "hippo:document");
        node.setProperty("hipposys:classname", TestWorkflowImpl.class.getName());
        node.setProperty("hipposys:display", "Test workflow");
        while(session.getRootNode().hasNode("test"))
            session.getRootNode().getNode("test").remove();
        session.save();
        TestWorkflowImpl.invocationCountNoArg = 0;
        TestWorkflowImpl.invocationCountDateArg = 0;
    }

    @After
    public void tearDown() throws Exception {
         TestWorkflowImpl.invocationCountNoArg = 0;
         TestWorkflowImpl.invocationCountDateArg = 0;
         Node node;
         node = session.getRootNode().getNode("hippo:configuration/hippo:workflows");
         if(node.hasNode("test")) {
             node.getNode("test").remove();
         }
         node = session.getRootNode();
         if(node.hasNode("test"))
             node.getNode("test").remove();
        session.save();
        super.tearDown();
    }

    @Test(timeout=10000)
    public void test() throws RepositoryException, InterruptedException {
        TestWorkflowImpl.invocationCountDateArg = 0;
        TestWorkflowImpl.invocationCountNoArg = 0;
        Node node = session.getRootNode().addNode("test");
        Node doc = node.addNode("document", "hippo:handle");
        doc.addMixin("hippo:hardhandle");
        doc = doc.addNode("document", "hippo:document");
        doc.addMixin("hippo:harddocument");
        Node call = node.addNode("call", "wfdropbox:node");
        call.setProperty("wfdropbox:alternate", "/test/document/document", PropertyType.PATH);
        call.setProperty("wfdropbox:category", "test");
        call.setProperty("wfdropbox:method", "test");
        Node n = call.addNode("wfdropbox:arguments", "wfdropbox:arguments");
        n = n.addNode("date", "wfdropbox:argument");
        n.setProperty("wfdropbox:formal", Date.class.getName());
        n.setProperty("wfdropbox:actual", session.getValueFactory().createValue(Calendar.getInstance()));
        assertEquals(0, TestWorkflowImpl.invocationCountNoArg);
        assertEquals(0, TestWorkflowImpl.invocationCountDateArg);
        session.save();
        while (TestWorkflowImpl.invocationCountDateArg == 0 || session.getRootNode().getNode("test").hasNode("call")) {
            Thread.sleep(300);
            session.refresh(false);
        }
        assertFalse(session.getRootNode().getNode("test").hasNode("call"));
        assertEquals(0, TestWorkflowImpl.invocationCountNoArg);
        assertEquals(1, TestWorkflowImpl.invocationCountDateArg);
    }
}
