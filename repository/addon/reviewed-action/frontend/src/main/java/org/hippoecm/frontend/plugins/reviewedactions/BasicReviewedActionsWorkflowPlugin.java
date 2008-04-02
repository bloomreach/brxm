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
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;

import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;

public class BasicReviewedActionsWorkflowPlugin extends AbstractWorkflowPlugin {
    private static final long serialVersionUID = 1L;

    public BasicReviewedActionsWorkflowPlugin(PluginDescriptor pluginDescriptor, final IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, (WorkflowsModel)model, parentPlugin);

        addWorkflowAction("edit-dialog", "Edit document", new WorkflowDialogAction() {
                public Request execute(Channel channel, Workflow wf) throws Exception {
                    BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                    Document docRef = workflow.obtainEditableInstance();
                    Node docNode = ((UserSession)getSession()).getJcrSession().getNodeByUUID(docRef.getIdentity());
                    if (channel != null) {
                        Request request = channel.createRequest("edit", new JcrNodeModel(docNode));
                        return request;
                    } else {
                        return null;
                    }
                }
            });
        addWorkflowAction("requestPublication-dialog", "Request publication", new WorkflowDialogAction() {
                public Request execute(Channel channel, Workflow wf) throws Exception {
                    BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                    workflow.requestPublication();
                    return null;
                }
            });
        addWorkflowAction("requestDePublication-dialog", "Request unpublication", new WorkflowDialogAction() {
                public Request execute(Channel channel, Workflow wf) throws Exception {
                    BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                    workflow.requestDepublication();
                    return null;
                }
            });
        addWorkflowAction("requestDeletion-dialog", "Request delete", new WorkflowDialogAction() {
                public Request execute(Channel channel, Workflow wf) throws Exception {
                    BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                    workflow.requestDeletion();
                    return null;
                }
            });
    }

    @Override
    public void receive(Notification notification) {
        if ("save".equals(notification.getOperation())) {
            try {
                WorkflowsModel workflowModel = (WorkflowsModel) getPluginModel();
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) manager.getWorkflow(workflowModel.getWorkflowDescriptor());
                workflow.commitEditableInstance();
            } catch (RemoteException e) {
                log.error(e.getMessage());
            } catch (WorkflowException e) {
                log.error(e.getMessage());
            } catch (MappingException e) {
                log.error(e.getMessage());
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        } else if ("close".equals(notification.getOperation())) {
            // FIXME: possibly dispose editable instance?
        }
        super.receive(notification);
    }
}
