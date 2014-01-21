/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;

/**
 * If your action executes a method on FullReviewedActionsWorkflow then extend this class
 */
public abstract class AbstractAssetActionsWorkflowAction extends AbstractWorkflowAction {

    private static final String WORKFLOW_CATEGORY = "default";
    private static final Class<DefaultWorkflow> WORKFLOW_CLASS = DefaultWorkflow.class;

    public AbstractAssetActionsWorkflowAction(ActionContext context) {
        super(context);
    }

    @Override
    protected final String getWorkflowCategory() {
        return WORKFLOW_CATEGORY;
    }

    @Override
    protected boolean isApplicableDocumentType(Node node) throws RepositoryException {
        return node.isNodeType("hippogallery:exampleAssetSet");
    }
    
    @Override
    protected final Class<? extends Workflow> getWorkflowClass() {
        return WORKFLOW_CLASS;
    }

    protected final DefaultWorkflow getWorkflow(Node node) throws RepositoryException {
        Workflow wf = getWorkflowManager(node.getSession()).getWorkflow(getWorkflowCategory(), node);
        assert wf instanceof DefaultWorkflow;
        return (DefaultWorkflow) wf;
    }
    
}
