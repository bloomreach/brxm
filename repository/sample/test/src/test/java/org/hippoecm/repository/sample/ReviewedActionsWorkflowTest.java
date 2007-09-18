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
package org.hippoecm.repository.sample;

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
    private Workflow getWorkflow(Node node, String category) throws RepositoryException {
        if(workflowMgr == null) {
            HippoWorkspace wsp = (HippoWorkspace) node.getSession().getWorkspace();
            workflowMgr = wsp.getWorkflowManager();
        }
        return workflowMgr.getWorkflow(category, node);
    }

    public void testReviewedAction() throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException {
        server = HippoRepositoryFactory.getHippoRepository();
        assertNotNull(server);

        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        Node root = session.getRootNode();

        Node node = root;
        node = node.addNode("documents");
        node = node.addNode("myarticle", "hippo:handle");
        node = node.addNode("myarticle", "hippo:document");
        node.setProperty("content", "Lorem ipsum dolor sit amet, consectetaur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum");

        session.save();
        Utilities.dump(root.getNode("documents"));

        // steps taken by an author
        {
            node = root.getNode("documents/myarticle");
            EditableDocumentWorkflow editWorkflow = (EditableDocumentWorkflow) getWorkflow(node, "default");
            node = editWorkflow.obtainEditableInstance();
            Property prop = node.getProperty("content");
            prop.setValue(prop.getString() + ",");

            ReviewedActionsWorkflow reviewedWorkflow = (ReviewedActionsWorkflow) getWorkflow(node, "default");
            node = reviewedWorkflow.requestPublication();
        }

        // These steps would be taken by editor:
        {
            RequestWorkflow requestWorkflow = (RequestWorkflow) getWorkflow(node, "default");
            requestWorkflow.rejectRequest("comma should be a point");
        }

        // steps taken by an author
        {
            RequestWorkflow requestWorkflow = (RequestWorkflow) getWorkflow(node, "default");
            requestWorkflow.cancelRequest();
        }

        // steps taken by an author
        {
            node = root.getNode("documents/myarticle");
            EditableDocumentWorkflow editWorkflow = (EditableDocumentWorkflow) getWorkflow(node, "default");
            node = editWorkflow.obtainEditableInstance();
            Property prop = node.getProperty("content");
            prop.setValue(prop.getString().substring(0,prop.getString().length()-1) + "!");

            ReviewedActionsWorkflow reviewedWorkflow = (ReviewedActionsWorkflow) getWorkflow(node, "default");
            node = reviewedWorkflow.requestPublication();
        }

        // These steps would be taken by editor:
        {
            RequestWorkflow requestWorkflow = (RequestWorkflow) getWorkflow(node, "default");
            requestWorkflow.acceptRequest();
        }

        // These steps would be taken by editor:
        {
            EditableDocumentWorkflow editWorkflow = (EditableDocumentWorkflow) getWorkflow(node, "default");
            node = editWorkflow.obtainEditableInstance();
            Property prop = node.getProperty("content");
            prop.setValue(prop.getString().substring(0,prop.getString().length()-1) + ".");

            ReviewedActionsWorkflow reviewedWorkflow = (ReviewedActionsWorkflow) getWorkflow(node, "default");
            reviewedWorkflow.publish();
        }

        // These steps would be taken by author
        {
            node = root.getNode("documents/myarticle");
            EditableDocumentWorkflow editWorkflow = (EditableDocumentWorkflow) getWorkflow(node, "default");
            editWorkflow.requestDeletion();
        }
    }
}
