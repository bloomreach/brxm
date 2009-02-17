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

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.MappingException;

import org.hippoecm.repository.Utilities;

import org.junit.*;
import static org.junit.Assert.*;

public class ReviewedActionsWorkflowTest extends ReviewedActionsWorkflowAbstractTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        Node node, root = session.getRootNode();
        while(root.hasNode("test")) {
            root.getNode("test").remove();
            root.save();
            root.refresh(false);
        }
        node = root.addNode("test");
        node = node.addNode("myarticle", "hippo:handle");
        node.addMixin("mix:referenceable");
        node = node.addNode("myarticle", "hippo:testdocument");
        node.addMixin("hippo:harddocument");
        node.setProperty("content", LOREM);
        node.setProperty("hippostd:holder", "admin");
        node.setProperty("hippostd:state", "draft");

        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testReviewedAction() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node node, root = session.getRootNode();
        {
            assertTrue(session.getRootNode().hasNode("test"));
            assertTrue(session.getRootNode().getNode("test").hasNode("myarticle"));
            assertTrue(session.getRootNode().getNode("test").getNode("myarticle").hasNode("myarticle"));
            assertTrue(session.getRootNode().getNode("test/myarticle").hasNode("myarticle"));
            assertTrue(session.getRootNode().getNode("test/myarticle").hasNode("myarticle"));
            node = getNode("test/myarticle/myarticle");
            FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
            Document document = workflow.obtainEditableInstance();
            session.save();
            session.refresh(true);
            node = getNode("test/myarticle/myarticle[@hippostd:state='draft']");
         }

        // steps taken by an author
        {
            node = getNode("test/myarticle/myarticle");
            BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            assertNotNull("No applicable workflow where there should be one", workflow);
            Document document = workflow.obtainEditableInstance();
            session.save();
            session.refresh(true);
            node = getNode("test/myarticle/myarticle[@hippostd:state='draft']");
            assertTrue(node.getUUID().equals(document.getIdentity()));
            Property prop = node.getProperty("content");
            prop.setValue(prop.getString() + ",");
            session.save();
            session.refresh(true);

            node = getNode("test/myarticle/myarticle[@hippostd:state='draft']");
            BasicReviewedActionsWorkflow reviewedWorkflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            reviewedWorkflow.commitEditableInstance();
            session.save();
            session.refresh(true);

            reviewedWorkflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            assertNotNull("No applicable workflow where there should be one", reviewedWorkflow);
            reviewedWorkflow.requestPublication();
            session.save();
            session.refresh(true);
        }

        // These steps would be taken by editor:
        {
            node = getNode("test/myarticle/hippo:request");
            FullRequestWorkflow workflow = (FullRequestWorkflow) getWorkflow(node, "default");
            assertNotNull("No applicable workflow where there should be one", workflow);
            workflow.rejectRequest("comma should be a point");
            session.save();
            session.refresh(true);
            assertTrue(getNode("test/myarticle/hippo:request").getProperty("reason").getString().equals("comma should be a point"));
        }

        // steps taken by an author
        {
            node = getNode("test/myarticle/hippo:request[@type='rejected']");
            BasicRequestWorkflow workflow = (BasicRequestWorkflow) getWorkflow(node, "default");
            assertNotNull("No applicable workflow where there should be one", workflow);
            workflow.cancelRequest();
            session.save();
            session.refresh(true);
        }

        // steps taken by an author
        {
            node = getNode("test/myarticle/myarticle[@hippostd:state='unpublished']");
            BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            workflow.obtainEditableInstance();
            session.save();
            session.refresh(true);
            node = getNode("test/myarticle/myarticle[@hippostd:state='draft']");
            Property prop = node.getProperty("content");
            prop.setValue(prop.getString().substring(0, prop.getString().length() - 1) + "!");

            node = getNode("test/myarticle/myarticle[@hippostd:state='draft']");
            BasicReviewedActionsWorkflow reviewedWorkflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            reviewedWorkflow.commitEditableInstance();
            session.save();
            session.refresh(true);

            reviewedWorkflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            assertNotNull("No applicable workflow where there should be one", reviewedWorkflow);
            reviewedWorkflow.requestPublication();
            session.save();
            session.refresh(true);
        }

        // These steps would be taken by editor:
        {
            node = getNode("test/myarticle/hippo:request[@type='publish']");
            FullRequestWorkflow workflow = (FullRequestWorkflow) getWorkflow(node, "default");
            workflow.acceptRequest();
            session.save();
            session.refresh(true);
        }

        // These steps would be taken by editor:
        {
            node = getNode("test/myarticle/myarticle");
            BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            assertNotNull("No applicable workflow where there should be one", workflow);
            Document document = workflow.obtainEditableInstance();
            session.save();
            session.refresh(true);
            node = getNode("test/myarticle/myarticle[@hippostd:state='draft']");
            assertTrue(node.getUUID().equals(document.getIdentity()));
            Property prop = node.getProperty("content");
            prop.setValue(prop.getString().substring(0, prop.getString().length() - 1) + ".");
            session.save();
            session.refresh(true);

            node = getNode("test/myarticle/myarticle[@hippostd:state='draft']");
            FullReviewedActionsWorkflow reviewedWorkflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
            reviewedWorkflow.commitEditableInstance();
            session.save();
            session.refresh(true);

            reviewedWorkflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
            assertNotNull("No applicable workflow where there should be one", reviewedWorkflow);
            reviewedWorkflow.publish();
            session.save();
            session.refresh(true);
        }

        // These steps would be taken by author
        {
            node = getNode("test/myarticle/myarticle[@hippostd:state='published']");
            BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            // cannot delete published document when request is present
            try {
                workflow.requestDeletion();
            } catch (WorkflowException e) {
                assertTrue("cannot request deletion when there is already a request", true );
            }
        }


        // Test regarding Issue HREPTWO-688
        {
            Node node2 = getNode("test/myarticle/hippo:request[@type='delete']");
            FullRequestWorkflow requestWorkflow = (FullRequestWorkflow) getWorkflow(node2, "default");
            requestWorkflow.cancelRequest();
            session.save();
            session.refresh(false);

            // now it should be possible
            node = getNode("test/myarticle/myarticle[@hippostd:state='published']");
            BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");

            try {
                workflow.requestDeletion();
            } catch (WorkflowException e) {
                fail("Issue HREPTWO-688 has resurfaced");
            }
            session.save();
            session.refresh(true);
        }
    }
}
