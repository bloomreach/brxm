/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.reviewedactions;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junit.framework.TestCase;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.Utilities;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.api.HippoWorkspace;

public class ReviewedActionsWorkflowTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    private static final String LOREM = "Lorem ipsum dolor sit amet, consectetaur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum";

    private HippoRepository server;
    private Session session;
    private WorkflowManager workflowMgr = null;

    public void setUp() throws Exception {
        server = HippoRepositoryFactory.getHippoRepository();

        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        Node node, root = session.getRootNode();

        // set up the workflow specification as a node "/hippo:configuration/hippo:workflows/default/reviewedactions"
        node = root.getNode("hippo:configuration");
        if (node.hasNode("hippo:workflows"))
            node.getNode("hippo:workflows").remove();
        node = node.addNode("hippo:workflows", "hippo:workflowfolder");
        node.addMixin("mix:referenceable");
        Node wfs = node.addNode("default", "hippo:workflowcategory");

        node = wfs.addNode("reviewedactions", "hippo:workflow");
        node.setProperty("hippo:nodetype", "hippo:document");
        node.setProperty("hippo:display", "Reviewed actions workflow");
        node.setProperty("hippo:renderer", "org.hippoecm.frontend.reviewedactions.ReviewedActionsRenderer");
        node.setProperty("hippo:classname", "org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflowImpl");
        Node types = node.getNode("hippo:types");
        node = types.addNode("org.hippoecm.repository.reviewedactions.PublishableDocument", "hippo:type");
        node.setProperty("hippo:nodetype", "hippo:document");
        node.setProperty("hippo:display", "PublishableDocument");
        node.setProperty("hippo:classname", "org.hippoecm.repository.reviewedactions.PublishableDocument");
        node = types.addNode("org.hippoecm.repository.reviewedactions.PublicationRequest", "hippo:type");
        node.setProperty("hippo:nodetype", "hippo:request");
        node.setProperty("hippo:display", "PublicationRequest");
        node.setProperty("hippo:classname", "org.hippoecm.repository.reviewedactions.PublicationRequest");

        node = wfs.addNode("reviewedrequests", "hippo:workflow");
        node.setProperty("hippo:nodetype", "hippo:request");
        node.setProperty("hippo:display", "Reviewed requests workflow");
        node.setProperty("hippo:renderer", "org.hippoecm.frontend.reviewedactions.RequestWorkflowRenderer");
        node.setProperty("hippo:classname", "org.hippoecm.repository.reviewedactions.FullRequestWorkflowImpl");
        types = node.getNode("hippo:types");
        node = types.addNode("org.hippoecm.repository.reviewedactions.PublishableDocument", "hippo:type");
        node.setProperty("hippo:nodetype", "hippo:document");
        node.setProperty("hippo:display", "PublishableDocument");
        node.setProperty("hippo:classname", "org.hippoecm.repository.reviewedactions.PublishableDocument");
        node = types.addNode("org.hippoecm.repository.reviewedactions.PublicationRequest", "hippo:type");
        node.setProperty("hippo:nodetype", "hippo:request");
        node.setProperty("hippo:display", "PublicationRequest");
        node.setProperty("hippo:classname", "org.hippoecm.repository.reviewedactions.PublicationRequest");

        session.save();

        if (root.hasNode("documents"))
            root.getNode("documents").remove();
        node = root.addNode("documents");
        node = node.addNode("myarticle", "hippo:handle");
        node = node.addNode("myarticle", "hippo:document");
        node.setProperty("content", LOREM);
        node.setProperty("state", "unpublished");

        session.save();
    }

    public void tearDown() throws Exception {
        session.getRootNode().getNode("documents").remove();
        session.save();
        server.close();
    }

    private Workflow getWorkflow(Node node, String category) throws RepositoryException {
        if (workflowMgr == null) {
            HippoWorkspace wsp = (HippoWorkspace) node.getSession().getWorkspace();
            workflowMgr = wsp.getWorkflowManager();
        }
        Node canonicalNode = ((HippoNode) node).getCanonicalNode();
        return workflowMgr.getWorkflow(category, canonicalNode);
    }

    public void testReviewedAction() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node node, root = session.getRootNode();

        // steps taken by an author
        {
            node = Utilities.getNode(root, "documents/myarticle/myarticle");
            BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            assertNotNull("No applicable workflow where there should be one", workflow);
            Document document = workflow.obtainEditableInstance();
            session.save();
            session.refresh(true);
            node = Utilities.getNode(root, "documents/myarticle/myarticle[state='draft']");
            assertTrue(node.getUUID().equals(document.getIdentity()));
            Property prop = node.getProperty("content");
            prop.setValue(prop.getString() + ",");
            session.save();
            session.refresh(true);
            BasicReviewedActionsWorkflow reviewedWorkflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            assertNotNull("No applicable workflow where there should be one", reviewedWorkflow);
            reviewedWorkflow.requestPublication();
            session.save();
            session.refresh(true);
            //Utilities.dump(root.getNode("documents"));
        }

        // These steps would be taken by editor:
        {
            node = Utilities.getNode(root, "documents/myarticle/request");
            FullRequestWorkflow workflow = (FullRequestWorkflow) getWorkflow(node, "default");
            assertNotNull("No applicable workflow where there should be one", workflow);
            workflow.rejectRequest("comma should be a point");
            session.save();
            session.refresh(true);
            assertTrue(Utilities.getNode(root, "documents/myarticle/request").getProperty("reason").getString().equals("comma should be a point"));
            //Utilities.dump(root.getNode("documents"));
        }

        // steps taken by an author
        {
            node = Utilities.getNode(root, "documents/myarticle/request[type='rejected']");
            BasicRequestWorkflow workflow = (BasicRequestWorkflow) getWorkflow(node, "default");
            assertNotNull("No applicable workflow where there should be one", workflow);
            workflow.cancelRequest();
            session.save();
            session.refresh(true);
            //Utilities.dump(root.getNode("documents"));
        }

        // steps taken by an author
        {
            node = Utilities.getNode(root, "documents/myarticle/myarticle[state='unpublished']");
            BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            workflow.obtainEditableInstance();
            session.save();
            session.refresh(true);
            //Utilities.dump(root.getNode("documents"));
            node = Utilities.getNode(root, "documents/myarticle/myarticle[state='draft']");
            Property prop = node.getProperty("content");
            prop.setValue(prop.getString().substring(0, prop.getString().length() - 1) + "!");
            BasicReviewedActionsWorkflow reviewedWorkflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            assertNotNull("No applicable workflow where there should be one", reviewedWorkflow);
            reviewedWorkflow.requestPublication();
            session.save();
            session.refresh(true);
            //Utilities.dump(root.getNode("documents"));
        }

        // These steps would be taken by editor:
        {
            node = Utilities.getNode(root, "documents/myarticle/request[type='publish']");
            FullRequestWorkflow workflow = (FullRequestWorkflow) getWorkflow(node, "default");
            workflow.acceptRequest();
            session.save();
            session.refresh(true);
            //Utilities.dump(root.getNode("documents"));
        }

        // These steps would be taken by editor:
        {
            node = Utilities.getNode(root, "documents/myarticle/myarticle[state='unpublished']");
            Property prop = node.getProperty("content");
            prop.setValue(prop.getString().substring(0, prop.getString().length() - 1) + ".");
            session.save();
            session.refresh(true);
            FullReviewedActionsWorkflow reviewedWorkflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
            assertNotNull("No applicable workflow where there should be one", reviewedWorkflow);
            reviewedWorkflow.publish();
            session.save();
            session.refresh(true);
            //Utilities.dump(root.getNode("documents"));
        }

        // These steps would be taken by author
        {
            node = Utilities.getNode(root, "documents/myarticle/myarticle[state='published']");
            BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            workflow.requestDeletion();
            session.save();
            session.refresh(true);
            //Utilities.dump(root.getNode("documents"));
        }
    }
}
