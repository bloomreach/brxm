/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.reviewedactions;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowAction;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.reviewedactions.BasicRequestWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CancelWorkflowPlugin extends AbstractWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(BasicReviewedActionsWorkflowPlugin.class);

    private boolean cancelable = true;

    public CancelWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        onModelChanged();

        addWorkflowAction("cancelRequest-dialog", new StringResourceModel("cancel-request", this, null),
                new Visibility() {
                    private static final long serialVersionUID = 1L;

                    public boolean isVisible() {
                        return cancelable;
                    }
                }, new WorkflowAction() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void execute(Workflow wf) throws Exception {
                        BasicRequestWorkflow workflow = (BasicRequestWorkflow) wf;
                        workflow.cancelRequest();
                    }
                });
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        WorkflowsModel model = (WorkflowsModel) getModel();
        try {
            Node node = model.getNodeModel().getNode();
            Node child = null;
            if (node.isNodeType(HippoNodeType.NT_DOCUMENT) && node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                node = node.getParent();
            }
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                for (NodeIterator iter = node.getNodes(HippoNodeType.NT_REQUEST); iter.hasNext();) {
                    child = iter.nextNode();
                    if (child.isNodeType(HippoNodeType.NT_REQUEST)) {
                        node = child;
                    } else {
                        child = null;
                    }
                }
            }
            if (node == null || !node.isNodeType(HippoNodeType.NT_REQUEST)) {
                node = null;
                cancelable = false;
            }
        } catch (RepositoryException ex) {
            // status unknown, maybe there are legit reasons for this, so don't emit a warning
            log.info(ex.getClass().getName() + ": " + ex.getMessage());
        }
    }
}
