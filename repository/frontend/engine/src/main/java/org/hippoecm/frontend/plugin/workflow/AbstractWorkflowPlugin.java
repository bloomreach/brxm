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
package org.hippoecm.frontend.plugin.workflow;

import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.Model;

import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.ChannelFactory;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.session.UserSession;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractWorkflowPlugin extends Plugin {
    static protected Logger log = LoggerFactory.getLogger(AbstractWorkflowPlugin.class);

    public AbstractWorkflowPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, (WorkflowsModel)model, parentPlugin);
    }

    protected void addWorkflowAction(final String dialogName, final String dialogLink, final String dialogTitle, boolean visible,
                                     final WorkflowDialogAction action) {
        if(visible) {
            add(new DialogLink(dialogName, new Model(dialogLink),
                               new IDialogFactory() {
                                   public AbstractDialog createDialog(DialogWindow dialogWindow) {
                                       return new AbstractWorkflowDialog(dialogWindow, dialogTitle) {
                                               protected void execute() throws Exception {
                                                   Channel channel = getTopChannel();
                                                   Request request = action.execute(channel, getWorkflow());
                                                   if(request != null) {
                                                       channel.send(request);
                                                       // request.getContext().apply(target); // FIXME
                                                   }
                                               }
                                           };
                                   }
                               }, (WorkflowsModel) getPluginModel(), getTopChannel(), getPluginManager().getChannelFactory()));
        } else {
            add(new EmptyPanel(dialogName));
        }
    }

    protected void addWorkflowAction(final String linkName, final String linkTitle, boolean visible,
                                     final WorkflowDialogAction action) {
        final WorkflowsModel workflowModel = (WorkflowsModel) getModel();
        if(visible) {
            add(new AjaxLink(linkName, new Model(linkTitle)) {
                    private static final long serialVersionUID = 1L;

                    @Override
                        public void onClick(AjaxRequestTarget target) {
                        try {
                            // before saving (which possibly means deleting), find the handle
                            JcrNodeModel handle = workflowModel.getNodeModel();
                            while (handle.getParentModel() != null && !handle.getNode().isNodeType(HippoNodeType.NT_HANDLE)) {
                                handle = handle.getParentModel();
                            }
                            // save the handle so that the workflow uses the correct content
                            handle.getNode().save();
                            ((UserSession) Session.get()).getJcrSession().refresh(true);

                            try {
                                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                                Workflow workflow = manager.getWorkflow(workflowModel.getWorkflowDescriptor());
                                Channel channel = getTopChannel();
                                Request request = action.execute(channel, workflow);
                                if(request != null) {
                                    channel.send(request);
                                    request.getContext().apply(target);
                                }
                            } catch (MappingException e) {
                                log.error(e.getMessage());
                            } catch (RepositoryException e) {
                                log.error(e.getMessage());
                            } catch (Exception e) {
                                log.error(e.getMessage());
                            }

                        } catch(RepositoryException ex) {
                            log.error("Invalid data to save", ex);
                        }

                    }
                });
        } else {
            add(new EmptyPanel(linkName));
        }
    }

    protected void addWorkflowAction(final String linkName, final String linkTitle, 
                                     final WorkflowDialogAction action) {
        addWorkflowAction(linkName, linkTitle, true, action);
    }
    protected void addWorkflowAction(final String dialogName, final String dialogLink, final String dialogTitle,
                                     final WorkflowDialogAction action) {
        addWorkflowAction(dialogName, dialogTitle, dialogTitle, true, action);
    }
}
