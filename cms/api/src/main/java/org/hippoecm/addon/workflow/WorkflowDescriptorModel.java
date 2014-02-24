/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.addon.workflow;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;

import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.repository.util.JcrConstants;

public class WorkflowDescriptorModel extends LoadableDetachableModel<WorkflowDescriptor> {

    private String id;
    private String category;
    private transient Workflow workflow;

    /**
     * deprecated: use the alternative constructor instead
     */
    @Deprecated
    public WorkflowDescriptorModel(WorkflowDescriptor descriptor, String category, Node subject) throws RepositoryException {
        super(descriptor);
        init(category, subject);
    }

    public WorkflowDescriptorModel(String category, Node subject) throws RepositoryException {
        init(category, subject);
    }

    private void init(String category, Node subject) throws RepositoryException {
        this.category = category;
        this.id = subject.getIdentifier();
    }

    protected WorkflowDescriptor load() {
        try {
            Session session = UserSession.get().getJcrSession();
            WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
            Node node = getNode(session);
            if (node.isNodeType(JcrConstants.NT_FROZEN_NODE)) {
                Version version = (Version) node.getParent();
                String docId = version.getContainingHistory().getVersionableIdentifier();
                Node docNode = version.getSession().getNodeByIdentifier(docId);
                if (docNode.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                    Node handle = docNode.getParent();
                    return workflowManager.getWorkflowDescriptor(category, handle);
                } else {
                    return workflowManager.getWorkflowDescriptor(category, docNode);
                }
            } else {
                return workflowManager.getWorkflowDescriptor(category, node);
            }
        } catch (RepositoryException ex) {
            return null;
        }
    }

    public Node getNode() throws RepositoryException {
        Session session = UserSession.get().getJcrSession();
        return getNode(session);
    }

    private Node getNode(Session session) throws RepositoryException {
        return session.getNodeByIdentifier(id);
    }

    public <T extends Workflow> T getWorkflow() {
        if (workflow != null) {
            return (T) workflow;
        }

        WorkflowDescriptor descriptor = getObject();
        if (descriptor == null) {
            return null;
        }

        try {
            Session session = UserSession.get().getJcrSession();
            WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
            workflow = workflowManager.getWorkflow(descriptor);
            return (T) workflow;
        } catch (RepositoryException e) {
            return null;
        }
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        workflow = null;
    }
}
