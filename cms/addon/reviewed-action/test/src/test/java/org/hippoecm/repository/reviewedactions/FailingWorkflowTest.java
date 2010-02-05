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
package org.hippoecm.repository.reviewedactions;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.rmi.RemoteException;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.TestCase;
import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;

public class FailingWorkflowTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    String[] content = {
        "/test/folder", "hippostd:folder",
        "jcr:mixinTypes", "hippo:harddocument",
        "/test/folder/document", "hippo:handle",
        "jcr:mixinTypes", "hippo:hardhandle",
        "/test/folder/document/document", "hippo:testpublishabledocument",
        "hippostdpubwf:createdBy", "admin",
        "hippostdpubwf:creationDate", "2010-02-04T16:32:28.068+02:00</sv:value>",
        "hippostdpubwf:lastModifiedBy", "admin",
        "hippostdpubwf:lastModificationDate", "2010-02-04T16:32:28.068+02:00",
        "jcr:mixinTypes", "hippo:harddocument",
        "hippostd:folder", "admin",
        "hippostd:state", "published"
    };

    @Before
    public void setUp() throws Exception {
        super.setUp(true);
        Node root = session.getRootNode();
        if(root.hasNode("test"))
            root.getNode("test").remove();
        root = root.addNode("test");
        session.save();

        build(session, content);
        session.save();

        Node workflowsNode = session.getRootNode().getNode("hippo:configuration/hippo:workflows/default");
        Node wfNode = workflowsNode.getNode("reviewedactions");
        if(wfNode.hasProperty("hippo:privileges")) {
            wfNode.getProperty("hippo:privileges").remove();
        }

        session.save();
    }

    @After
    public void tearDown() throws Exception {
        Node root = session.getRootNode();
        if(root.hasNode("test"))
            root.getNode("test").remove();
        super.tearDown();
    }

    @Test
    public void testDummy() throws Exception {
    }

    @Test
    public void testFailAfterRename() throws Exception {
        Node root, node;
        WorkflowManager manager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
        Node handle = session.getRootNode().getNode("test/folder/document");
        Node document = handle.getNode(handle.getName());
        // WorkflowDescriptor workflowDescriptor = manager.getWorkflowDescriptor("default", document);
        // Workflow workflowInterface = manager.getWorkflow(workflowDescriptor);
        Workflow workflowInterface = manager.getWorkflow("default", document);
        assertTrue(workflowInterface instanceof FullReviewedActionsWorkflow);
        FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) workflowInterface;
        try {
            workflow.rename("fail");
            fail("rename should have failed");
        } catch(WorkflowException ex) {
            // expected
        }

        document = handle.getNode(handle.getName());
        // workflowDescriptor = manager.getWorkflowDescriptor("default", document);
        // workflowInterface = manager.getWorkflow(workflowDescriptor);
        workflowInterface = manager.getWorkflow("default", document);
        assertTrue(workflowInterface instanceof FullReviewedActionsWorkflow);
        workflow = (FullReviewedActionsWorkflow) workflowInterface;

        workflow.depublish();

        document = handle.getNode(handle.getName());
        // workflowDescriptor = manager.getWorkflowDescriptor("default", document);
        // workflowInterface = manager.getWorkflow(workflowDescriptor);
        workflowInterface = manager.getWorkflow("default", document);
        assertTrue(workflowInterface instanceof FullReviewedActionsWorkflow);
        workflow = (FullReviewedActionsWorkflow) workflowInterface;
        workflow.rename("succeed");
    }
}
