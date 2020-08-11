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

import java.io.Serializable;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

/**
 * If your action operates on a workflow then extend this class
 */
public abstract class AbstractWorkflowAction extends Action {

    public AbstractWorkflowAction(ActionContext context) {
        super(context);
    }

    @Override
    public boolean canOperateOnNode(Node node) throws Exception {
        return isApplicableDocumentType(node) && isApplicableMethod(node);
    }

    protected boolean isApplicableMethod(Node node) throws Exception {
        WorkflowDescriptor descriptor = getWorkflowDescriptor(node);
        if (descriptor == null) {
            return false;
        }
        for (Class<Workflow> c : descriptor.getInterfaces()) {
            if (c.equals(getWorkflowClass())) {
                Map<String, Serializable> hints = descriptor.hints();
                if (hints == null) {
                    return true;
                }
                Serializable info = hints.get(getWorkflowMethodName());
                if (info instanceof Boolean) {
                    return (Boolean) info;
                } else {
                    // don't understand, try it
                    return true;
                }
            }
        }
        return false;
    }

    protected WorkflowDescriptor getWorkflowDescriptor(Node node) throws Exception {
        return getWorkflowManager(node.getSession()).getWorkflowDescriptor(getWorkflowCategory(), node);
    }

    @Override
    public boolean isWriteAction() {
        return true;
    }
    
    /**
     * The workflow we are looking for is in this category
     */
    protected abstract String getWorkflowCategory();
    
    /**
     * Our doExecute calls this workflow method
     */
    protected abstract String getWorkflowMethodName();

    
    /**
     * Given the category of getWorkflowCategory() we expect the workflow manager to give us a workflow that
     * implements this class for a given node
     */
    protected abstract Class<? extends Workflow> getWorkflowClass();
    
    /**
     * Determines whether the actions can apply on a document of this type
     */
    protected abstract boolean isApplicableDocumentType(Node node) throws RepositoryException;
    
    protected final WorkflowManager getWorkflowManager(Session session) throws RepositoryException {
        return ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
    }

}
