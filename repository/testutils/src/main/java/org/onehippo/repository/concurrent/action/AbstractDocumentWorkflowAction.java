/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.concurrent.action;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

/**
 * If your action executes a method on DocumentWorkflow then extend this class
 */
public abstract class AbstractDocumentWorkflowAction extends AbstractWorkflowAction {

    private static final String[] REQUIRED_NODE_TYPES = new String[] {"hippostdpubwf:document"};
    private static final String WORKFLOW_CATEGORY = "default";
    private static final Class<DocumentWorkflow> WORKFLOW_CLASS = DocumentWorkflow.class;
    
    public AbstractDocumentWorkflowAction(ActionContext context) {
        super(context);
    }

    @Override
    protected final String getWorkflowCategory() {
        return WORKFLOW_CATEGORY;
    }

    @Override
    protected WorkflowDescriptor getWorkflowDescriptor(Node node) throws Exception {
        Node handle = node.getParent();
        return getWorkflowManager(node.getSession()).getWorkflowDescriptor(getWorkflowCategory(), handle);
    }

    @Override
    protected boolean isApplicableDocumentType(Node node) throws RepositoryException {
        for (String nodeType : REQUIRED_NODE_TYPES) {
            if (node.isNodeType(nodeType)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    protected final Class<? extends Workflow> getWorkflowClass() {
        return WORKFLOW_CLASS;
    }

    protected final DocumentWorkflow getDocumentWorkflow(Node handle) throws RepositoryException, WorkflowException {
        Workflow wf = getWorkflowManager(handle.getSession()).getWorkflow(getWorkflowCategory(), handle);
        if (wf == null) {
            context.getLog().error("FIXME: find out what is going wrong");
            throw new WorkflowException("Workflow is null");
        }

        return (DocumentWorkflow) wf;
    }
    
}
