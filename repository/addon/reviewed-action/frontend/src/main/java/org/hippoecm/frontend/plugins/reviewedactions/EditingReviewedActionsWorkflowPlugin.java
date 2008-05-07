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
package org.hippoecm.frontend.plugins.reviewedactions;

import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowAction;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditingReviewedActionsWorkflowPlugin extends AbstractWorkflowPlugin {
    private static final long serialVersionUID = 1L;

    static protected Logger log = LoggerFactory.getLogger(BasicReviewedActionsWorkflowPlugin.class);

    public EditingReviewedActionsWorkflowPlugin(PluginDescriptor pluginDescriptor, final IPluginModel model,
            Plugin parentPlugin) {
        super(pluginDescriptor, (WorkflowsModel) model, parentPlugin);

        WorkflowAction saveAction = new WorkflowAction() {
            private static final long serialVersionUID = 1L;
            public Request execute(Channel channel, Workflow wf) throws Exception {
                Request request = null;
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.commitEditableInstance();
                if (channel != null) {
                    request = channel.createRequest("close", new JcrNodeModel(model));
                    channel.send(request);
                }
                return request;
            }
        };
        addWorkflowAction("save", "Save", saveAction);
        
        WorkflowAction revertAction = new WorkflowAction() {
            private static final long serialVersionUID = 1L;
            public Request execute(Channel channel, Workflow wf) throws Exception {
                Request request = null;
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.disposeEditableInstance();
                if (channel != null) {
                    request = channel.createRequest("close", new JcrNodeModel(model));
                    channel.send(request);
                }
                return request;
            }
        };
        addWorkflowAction("revert", "Revert", revertAction);
    }
}
