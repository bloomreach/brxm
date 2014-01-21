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

import java.util.Random;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.gallery.GalleryWorkflow;

public abstract class AbstractGalleryWorkflowAction extends AbstractWorkflowAction {

    private static final String WORKFLOW_CATEGORY = "gallery";
    private static final String[] REQUIRED_NODE_TYPES = new String[] { "hippogallery:stdAssetGallery", "hippogallery:stdImageGallery" };
    private static final Class<? extends Workflow> WORKFLOW_CLASS = GalleryWorkflow.class;
    
    protected final Random random = new Random(System.currentTimeMillis());
    
    public AbstractGalleryWorkflowAction(ActionContext context) {
        super(context);
    }

    @Override
    protected String getWorkflowCategory() {
        return WORKFLOW_CATEGORY;
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
    protected Class<? extends Workflow> getWorkflowClass() {
        return WORKFLOW_CLASS;
    }

    protected final GalleryWorkflow getGalleryWorkflow(Node node) throws RepositoryException {
        Workflow wf = getWorkflowManager(node.getSession()).getWorkflow(getWorkflowCategory(), node);
        assert wf instanceof GalleryWorkflow;
        return (GalleryWorkflow) wf;
    }
}
