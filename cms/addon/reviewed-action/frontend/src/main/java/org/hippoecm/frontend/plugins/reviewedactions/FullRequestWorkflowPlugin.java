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

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowAction;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.reviewedactions.FullRequestWorkflow;

public class FullRequestWorkflowPlugin extends AbstractWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private String state = "UNKNOWN";

    public FullRequestWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        onModelChanged();

        IModel acceptModel = new StringResourceModel("accept-request", this, null);
        addWorkflowDialog("acceptRequest-dialog", acceptModel, acceptModel,
                new StringResourceModel("reject-message", this, null), new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return !(state.equals("rejected"));
            }
        }, new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void execute(Workflow wf) throws Exception {
                FullRequestWorkflow workflow = (FullRequestWorkflow) wf;
                workflow.acceptRequest();
            }
        });

        IModel rejectModel = new StringResourceModel("reject-request", this, null);
        addWorkflowDialog("rejectRequest-dialog", rejectModel, rejectModel,
                new StringResourceModel("reject-message", this, null), new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return !(state.equals("rejected"));
            }
        }, new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void execute(Workflow wf) throws Exception {
                FullRequestWorkflow workflow = (FullRequestWorkflow) wf;
                workflow.rejectRequest(""); // FIXME
            }
        });
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();

        WorkflowsModel model = (WorkflowsModel) getModel();
        try {
            JcrNodeModel nodeModel = model.getNodeModel();
            Node node = nodeModel.getNode();
            Node child = null;
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                for (NodeIterator iter = node.getNodes(node.getName()); iter.hasNext();) {
                    child = iter.nextNode();
                    if (child.isNodeType(HippoNodeType.NT_REQUEST)) {
                        node = child;
                        if (child.hasProperty("type")) {
                            break;
                        }
                    } else {
                        child = null;
                    }
                }
            } else if(node.isNodeType(HippoNodeType.NT_REQUEST)) {
                child = node;
            }
            if (child != null && child.hasProperty("type")) {
                state = node.getProperty("type").getString();
            }
        } catch (RepositoryException ex) {
            // unknown, maybe there are legit reasons for this, so don't emit a warning
        }
    }
}
