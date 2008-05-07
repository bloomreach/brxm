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

import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowAction;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.reviewedactions.FullRequestWorkflow;

public class FullRequestWorkflowPlugin extends AbstractWorkflowPlugin {
    private static final long serialVersionUID = 1L;

    public FullRequestWorkflowPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, (WorkflowsModel) model, parentPlugin);

        WorkflowAction acceptRequestAction = new WorkflowAction() {
            private static final long serialVersionUID = 1L;
            public Request execute(Channel channel, Workflow wf) throws Exception {
                FullRequestWorkflow workflow = (FullRequestWorkflow) wf;
                workflow.acceptRequest();
                return null;
            }
        }; 
        addWorkflowAction("acceptRequest-dialog", "Approve and execute request", acceptRequestAction);

        IDialogFactory rejectRequestDialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;
            public AbstractDialog createDialog(DialogWindow dialogWindow) {
                return new RejectRequestDialog(dialogWindow);
            }
        };
        addWorkflowDialog("rejectRequest-dialog", "Reject request (with reason)", rejectRequestDialogFactory);
        
        WorkflowAction cancelRequestAction = new WorkflowAction() {
            private static final long serialVersionUID = 1L;
            public Request execute(Channel channel, Workflow wf) throws Exception {
                FullRequestWorkflow workflow = (FullRequestWorkflow) wf;
                workflow.cancelRequest();
                return null;
            }
        };
        addWorkflowAction("cancelRequest-dialog", "Cancel request", cancelRequestAction);
    }
}
