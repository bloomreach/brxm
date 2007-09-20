/*
  THIS CODE IS UNDER CONSTRUCTION, please leave as is until
  work has proceeded to a stable level, at which time this comment
  will be removed.  -- Berry
  HOWEVER: you are very much invited to discuss this code with me.
*/

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
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowMappingException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.api.HippoWorkspace;

public class ReviewedActionsWorkflowTest extends TestCase
{
    private final static String SVN_ID = "$Id$";

    private static final String SYSTEMUSER_ID = "systemuser";
    private static final char[] SYSTEMUSER_PASSWORD = "systempass".toCharArray();

    private HippoRepository server;
    private Session session;
    private WorkflowManager workflowMgr = null;

    public void setUp() throws Exception {
        server = HippoRepositoryFactory.getHippoRepository();

        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        Node node, root = session.getRootNode();

        // set up the workflow specification as a node "/configuration/workflows/default/reviewedactions"
        node = root.getNode("configuration");
        node = node.addNode("hippo:workflows","hippo:workflowfolder");
        node.addMixin("mix:referenceable");
        node = node.addNode("default","hippo:workflowcategory");
        node = node.addNode("myworkflow","hippo:workflow");
        node.setProperty("hippo:nodetype","hippo:document");
        node.setProperty("hippo:display","Reviewed actions workflow");
        node.setProperty("hippo:renderer","org.hippoecm.repository.reviewedactions.ReviewedActionsRenderer");
        node.setProperty("hippo:classname","org.hippoecm.repository.reviewedactions.ReviewedActionsWorkflowImpl");
        Node types = node.getNode("hippo:types");
        node = types.addNode("org.hippoecm.repository.reviewedactions.PublishableDocument","hippo:type");
        node.setProperty("hippo:nodetype","hippo:document");
        node.setProperty("hippo:display","PublishableDocument");
        node.setProperty("hippo:classname","org.hippoecm.repository.reviewedactions.PublishableDocument");
        node = types.addNode("org.hippoecm.repository.reviewedactions.PublicationRequest","hippo:type");
        node.setProperty("hippo:nodetype","nt:unstructured");
        node.setProperty("hippo:display","PublicationRequest");
        node.setProperty("hippo:classname","org.hippoecm.repository.reviewedactions.PublicationRequest");
        session.save();

        node = root.addNode("documents");
        node = node.addNode("myarticle", "hippo:handle");
        node = node.addNode("myarticle", "hippo:document");
        node.setProperty("content", "Lorem ipsum dolor sit amet, consectetaur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum");

        session.save();
    }

    public void tearDown() throws Exception {
        session.getRootNode().getNode("configuration/hippo:workflows").remove();
        session.getRootNode().getNode("documents").remove();
        server.close();
    }

    private Workflow getWorkflow(Node node, String category) throws RepositoryException {
        if(workflowMgr == null) {
            HippoWorkspace wsp = (HippoWorkspace) node.getSession().getWorkspace();
            workflowMgr = wsp.getWorkflowManager();
        }
        return workflowMgr.getWorkflow(category, node);
    }

    public void testReviewedAction()
        throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException
    {
        Node node, root = session.getRootNode();
        Utilities.dump(root.getNode("configuration"));
        Utilities.dump(root.getNode("documents"));

        // steps taken by an author
        {
            node = root.getNode("documents/myarticle");
            ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) getWorkflow(node, "default");
            if(workflow == null)
                return;
            workflow.obtainEditableInstance();
            node = root.getNode("documents/myarticle/myarticle[@state='editing']");
            Property prop = node.getProperty("content");
            prop.setValue(prop.getString() + ",");

            ReviewedActionsWorkflow reviewedWorkflow = (ReviewedActionsWorkflow) getWorkflow(node, "default");
            reviewedWorkflow.requestPublication();
        }

        // These steps would be taken by editor:
        {
            node = root.getNode("documents/myarticle/request");
            RequestWorkflow workflow = (RequestWorkflow) getWorkflow(node, "default");
            workflow.rejectRequest("comma should be a point");
        }

        // steps taken by an author
        {
            node = root.getNode("documents/myarticle/request");
            RequestWorkflow workflow = (RequestWorkflow) getWorkflow(node, "default");
            workflow.cancelRequest();
        }

        // steps taken by an author
        {
            node = root.getNode("documents/myarticle");
            ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) getWorkflow(node, "default");
            workflow.obtainEditableInstance();
            node = root.getNode("documents/myarticle/myarticle[@state='editing']");
            Property prop = node.getProperty("content");
            prop.setValue(prop.getString().substring(0,prop.getString().length()-1) + "!");

            ReviewedActionsWorkflow reviewedWorkflow = (ReviewedActionsWorkflow) getWorkflow(node, "default");
            reviewedWorkflow.requestPublication();
        }

        // These steps would be taken by editor:
        {
            node = root.getNode("documents/myarticle/request");
            RequestWorkflow workflow = (RequestWorkflow) getWorkflow(node, "default");
            workflow.acceptRequest();
        }

        // These steps would be taken by editor:
        {
            ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) getWorkflow(node, "default");
            workflow.obtainEditableInstance();
            node = root.getNode("documents/myarticle/myarticle[@state='editing']");
            Property prop = node.getProperty("content");
            prop.setValue(prop.getString().substring(0,prop.getString().length()-1) + ".");

            ReviewedActionsWorkflow reviewedWorkflow = (ReviewedActionsWorkflow) getWorkflow(node, "default");
            reviewedWorkflow.publish();
        }

        // These steps would be taken by author
        {
            node = root.getNode("documents/myarticle");
            ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) getWorkflow(node, "default");
            workflow.requestDeletion();
        }
    }
}
