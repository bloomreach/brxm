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

import org.apache.wicket.model.Model;

import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.ChannelFactory;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowDialogAction;

import org.hippoecm.repository.reviewedactions.FullRequestWorkflow;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.Document;

public class FullRequestWorkflowPlugin extends AbstractWorkflowPlugin {
    private static final long serialVersionUID = 1L;

    public FullRequestWorkflowPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, (WorkflowsModel)model, parentPlugin);

        addWorkflowAction("acceptRequest-dialog", "Approve and execute request", new WorkflowDialogAction() {
                public Request execute(Channel channel, Workflow wf) throws Exception {
                    FullRequestWorkflow workflow = (FullRequestWorkflow) wf;
                    workflow.acceptRequest();
                    return null;
                }
            });
        addWorkflowAction("rejectRequest-dialog", "Reject request (with reason)", "Reject request (with reason)", new WorkflowDialogAction() {
                public Request execute(Channel channel, Workflow wf) throws Exception {
                    FullRequestWorkflow workflow = (FullRequestWorkflow) wf;
                    workflow.rejectRequest(""); // FIXME
                    return null;
                }
            });
        addWorkflowAction("cancelRequest-dialog", "Cancel request", new WorkflowDialogAction() {
                public Request execute(Channel channel, Workflow wf) throws Exception {
                    FullRequestWorkflow workflow = (FullRequestWorkflow) wf;
                    workflow.cancelRequest();
                    return null;
                }
            });
    }
}
