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
package org.hippoecm.repository.reviewedactions;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.repository.testutils.RepositoryTestCase;

public abstract class ReviewedActionsWorkflowAbstractTest extends RepositoryTestCase {

    protected static final String LOREM = "Lorem ipsum dolor sit amet";

    protected WorkflowManager workflowMgr = null;

    protected Workflow getWorkflow(Node node, String category) throws RepositoryException {
        if (workflowMgr == null) {
            HippoWorkspace wsp = (HippoWorkspace) node.getSession().getWorkspace();
            workflowMgr = wsp.getWorkflowManager();
        }
        Node canonicalNode = ((HippoNode) node).getCanonicalNode();
        return workflowMgr.getWorkflow(category, canonicalNode);
    }

    protected Workflow getWorkflow(Document document, String category) throws RepositoryException {
        if (workflowMgr == null) {
            HippoWorkspace wsp = (HippoWorkspace) session.getWorkspace();
            workflowMgr = wsp.getWorkflowManager();
        }
        return workflowMgr.getWorkflow(category, document);
    }

    protected Node getNode(String path) throws RepositoryException {
        return ((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getNode(session.getRootNode(), path);
    }
}
