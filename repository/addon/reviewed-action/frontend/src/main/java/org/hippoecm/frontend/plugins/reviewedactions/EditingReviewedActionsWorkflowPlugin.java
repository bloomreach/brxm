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

import java.rmi.RemoteException;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.ChannelFactory;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowDialogAction;
import org.hippoecm.frontend.session.UserSession;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;

import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditingReviewedActionsWorkflowPlugin extends AbstractWorkflowPlugin {
    private static final long serialVersionUID = 1L;

    static protected Logger log = LoggerFactory.getLogger(BasicReviewedActionsWorkflowPlugin.class);

    public EditingReviewedActionsWorkflowPlugin(PluginDescriptor pluginDescriptor, final IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, (WorkflowsModel)model, parentPlugin);

        addWorkflowAction("save", "Save", true,
            new WorkflowDialogAction() {
                public Request execute(Channel channel, Workflow wf) throws Exception {
                    Request request = null;
                    BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                    workflow.commitEditableInstance();
                    if (channel != null) {
                        request = channel.createRequest("save", new JcrNodeModel(model));
                        channel.send(request);
                        request = channel.createRequest("flush", new JcrNodeModel(model));
                        channel.send(request);
                    }
                    return request; // FIXME: can only return one request, shows problem with model
                }
            });
        addWorkflowAction("revert", "Revert", true,
            new WorkflowDialogAction() {
                public Request execute(Channel channel, Workflow wf) throws Exception {
                    Request request = null;
                    BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                    workflow.disposeEditableInstance();
                    if (channel != null) {
                        request = channel.createRequest("save", new JcrNodeModel(model));
                        channel.send(request);
                        request = channel.createRequest("flush", new JcrNodeModel(model));
                        channel.send(request);
                    }
                    return request; // FIXME: can only return one request, shows problem with model
                }
            });
    }
}
