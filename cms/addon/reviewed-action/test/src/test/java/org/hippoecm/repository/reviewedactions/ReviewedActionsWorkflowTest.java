/*
 *  Copyright 2008-2009 Hippo.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.Utilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
        node = node.addNode("myarticle", "hippostdpubwf:test");
        node.addMixin("hippo:harddocument");
        node.setProperty("hippostdpubwf:content", LOREM);
        node.setProperty("hippostd:holder", "admin");
        node.setProperty("hippostd:state", "unpublished");
        node.setProperty("hippo:availability", new String[] { "preview" });
        node.setProperty("hippostdpubwf:createdBy", "admin");
        node.setProperty("hippostdpubwf:creationDate", "2010-02-04T16:32:28.068+02:00");
        node.setProperty("hippostdpubwf:lastModifiedBy", "admin");
        node.setProperty("hippostdpubwf:lastModificationDate", "2010-02-04T16:32:28.068+02:00");

        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        Node root = session.getRootNode();
        while(root.hasNode("test")) {
            root.getNode("test").remove();
        }
        root.save();
        super.tearDown();
    }

    @Test
    public void testBasic() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node node, root = session.getRootNode();
        node = getNode("test/myarticle/myarticle");
        FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
	workflow.publish();
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
            Property prop = node.getProperty("hippostdpubwf:content");
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
            assertTrue(getNode("test/myarticle/hippo:request").getProperty("hippostdpubwf:reason").getString().equals("comma should be a point"));
        }

        // steps taken by an author
        {
            node = getNode("test/myarticle/hippo:request[@hippostdpubwf:type='rejected']");
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
            Property prop = node.getProperty("hippostdpubwf:content");
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
            node = getNode("test/myarticle/hippo:request[@hippostdpubwf:type='publish']");
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
            Property prop = node.getProperty("hippostdpubwf:content");
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
    }

    @Test
    public void testHREPTWO688() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        testReviewedAction();

        // These steps would be taken by author
        {
            Node node = getNode("test/myarticle/myarticle[@hippostd:state='published']");
            BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            // cannot delete published document when request is present
            try {
                workflow.requestDeletion();
            } catch (WorkflowException e) {
                assertTrue("cannot request deletion when there is already a request", true );
            }
        }

        {
            Node node = getNode("test/myarticle/hippo:request[@hippostdpubwf:type='delete']");
            FullRequestWorkflow requestWorkflow = (FullRequestWorkflow) getWorkflow(node, "default");
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

    @Test
    public void testHREPTWO2318() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        testReviewedAction();

        {
            // preparation: create draft
            Node node = getNode("test/myarticle/myarticle");
            BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            assertNotNull("No applicable workflow where there should be one", workflow);
            Document document = workflow.obtainEditableInstance();
            session.save();
            session.refresh(true);

            // "save" as commit/edit combo
            node = getNode("test/myarticle/myarticle[@hippostd:state='draft']");
            workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            workflow.commitEditableInstance();
            document = workflow.obtainEditableInstance();
            session.save();
            session.refresh(true);

            // "revert" as dispose/edit combo
            node = getNode("test/myarticle/myarticle[@hippostd:state='draft']");
            workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            workflow.disposeEditableInstance();
            session.refresh(false);
            node = getNode("test/myarticle/myarticle");
            workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            workflow.obtainEditableInstance();
            session.save();
            session.refresh(true);

            // cleanup
            node = getNode("test/myarticle/myarticle");
            workflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
            workflow.disposeEditableInstance();
        }
    }

    @Test
    public void testEditing() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Document document;
        Node node, root = session.getRootNode();
        {
            assertTrue(session.getRootNode().hasNode("test"));
            assertTrue(session.getRootNode().getNode("test").hasNode("myarticle"));
            assertTrue(session.getRootNode().getNode("test").getNode("myarticle").hasNode("myarticle"));
            assertTrue(session.getRootNode().getNode("test/myarticle").hasNode("myarticle"));
            assertTrue(session.getRootNode().getNode("test/myarticle").hasNode("myarticle"));
            node = getNode("test/myarticle/myarticle");
            node.setProperty("hippostd:state", "unpublished");
            node.save();

            FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
            workflow.publish();

            node = getNode("test/myarticle/myarticle");
            workflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
            document = workflow.obtainEditableInstance();
            assertNotNull(document);
            node = session.getNodeByUUID(document.getIdentity());
            assertNotNull(node);
            assertEquals("draft", node.getProperty("hippostd:state").getString());

            workflow = (FullReviewedActionsWorkflow) getWorkflow(document, "default");
            document = workflow.disposeEditableInstance();
            assertNotNull(document);
            node = session.getNodeByUUID(document.getIdentity());
            assertNotNull(node);
            assertEquals("published", node.getProperty("hippostd:state").getString());

            workflow = (FullReviewedActionsWorkflow) getWorkflow(document, "default");
            document = workflow.obtainEditableInstance();
            assertNotNull(document);
            node = session.getNodeByUUID(document.getIdentity());
            assertNotNull(node);
            assertEquals("draft", node.getProperty("hippostd:state").getString());

            workflow = (FullReviewedActionsWorkflow) getWorkflow(document, "default");
            document = workflow.commitEditableInstance();
            assertNotNull(document);
            node = session.getNodeByUUID(document.getIdentity());
            assertNotNull(node);
            assertEquals("unpublished", node.getProperty("hippostd:state").getString());

            session.refresh(true);
         }
    }

    @Test
    public void testCopy() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node root = session.getRootNode();

        // set up the workflow specification as a node "/hippo:configuration/hippo:workflows/default/reviewedactions"
        Node workflows = root.getNode("hippo:configuration/hippo:workflows");
        {
            Node internalWfc = workflows.addNode("internal", "hipposys:workflowcategory");
            Node folderWf = internalWfc.addNode("folder", "hipposys:workflow");
            folderWf.setProperty("hipposys:nodetype", "hippostd:folder");
            folderWf.setProperty("hipposys:display", "Folder workflow");
            folderWf.setProperty("hipposys:classname", "org.hippoecm.repository.standardworkflow.FolderWorkflowImpl");
        }
        {
            Node embeddedWfc = workflows.addNode("embedded", "hipposys:workflowcategory");
            Node embedWf = embeddedWfc.addNode("extended", "hipposys:workflow");
            embedWf.setProperty("hipposys:nodetype", "hippostd:folder");
            embedWf.setProperty("hipposys:display", "extended folder workflow");
            embedWf.setProperty("hipposys:classname", "org.hippoecm.repository.standardworkflow.FolderWorkflowImpl");
        }
        session.save();
        
        Node folder = root.getNode("test").addNode("origin", "hippostd:folder");
        folder.addMixin("hippo:harddocument");
        Node target = root.getNode("test").addNode("target", "hippostd:folder");
        target.addMixin("hippo:harddocument");

        ((HippoSession)session).copy(getNode("test/myarticle"), "/test/origin/myarticle");
        Node doc = getNode("test/origin/myarticle/myarticle");
        doc.setProperty("hippostd:state", "published");
        session.save();

        FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) getWorkflow(doc, "default");
        workflow.obtainEditableInstance();
        session.refresh(false);

        // test with draft present
        {
            workflow = (FullReviewedActionsWorkflow) getWorkflow(doc, "default");
            workflow.copy(new Document(target.getUUID()), "article");
            
            Node copy = getNode("test/target/article");
            assertEquals(1, copy.getNodes().getSize());
            assertEquals("unpublished", copy.getNode("article").getProperty("hippostd:state").getString());

            copy.remove();
            session.save();
        }

        // test with unpublished
        {
            workflow = (FullReviewedActionsWorkflow) getWorkflow(doc, "default");
            workflow.commitEditableInstance();
            session.refresh(false);

            workflow = (FullReviewedActionsWorkflow) getWorkflow(doc, "default");
            workflow.copy(new Document(target.getUUID()), "article");

            Node copy = getNode("test/target/article");
            assertEquals(1, copy.getNodes().getSize());
            assertEquals("unpublished", copy.getNode("article").getProperty("hippostd:state").getString());

            copy.remove();
            session.save();
        }

        // test to same folder
        {
            workflow = (FullReviewedActionsWorkflow) getWorkflow(doc, "default");
            workflow.copy(new Document(folder.getUUID()), "article");

            Node copy = getNode("test/origin/article");
            assertEquals(1, copy.getNodes().getSize());
            assertEquals("unpublished", copy.getNode("article").getProperty("hippostd:state").getString());

            copy.remove();
            session.save();
        }

        // test as unpublished
        {
            doc.remove();
            doc = root.getNode("test/origin/myarticle/myarticle");
            assertEquals("unpublished", doc.getProperty("hippostd:state").getString());
            session.save();

            workflow = (FullReviewedActionsWorkflow) getWorkflow(doc, "default");
            workflow.copy(new Document(folder.getUUID()), "article");

            Node copy = getNode("test/origin/article");
            assertEquals(1, copy.getNodes().getSize());
            assertEquals("unpublished", copy.getNode("article").getProperty("hippostd:state").getString());

            copy.remove();
            session.save();
        }
    }
}
