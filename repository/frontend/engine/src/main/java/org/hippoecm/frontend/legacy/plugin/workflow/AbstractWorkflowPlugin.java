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
package org.hippoecm.frontend.legacy.plugin.workflow;


import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.legacy.dialog.DialogLink;
import org.hippoecm.frontend.legacy.dialog.IDialogFactory;
import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.plugin.channel.Channel;
import org.hippoecm.frontend.legacy.plugin.channel.ChannelFactory;
import org.hippoecm.frontend.legacy.plugin.channel.Request;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated use org.hippoecm.frontend.sa.* instead
 */
@Deprecated
public class AbstractWorkflowPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    protected static Logger log = LoggerFactory.getLogger(AbstractWorkflowPlugin.class);

    public AbstractWorkflowPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, (WorkflowsModel) model, parentPlugin);
    }

    protected void addWorkflowAction(String id, String title, WorkflowAction action) {
        addWorkflowAction(id, title, true, action);
    }

    protected void addWorkflowAction(String id, String title, boolean visible, final WorkflowAction action) {
        if (visible) {
            final WorkflowsModel workflowModel = (WorkflowsModel) getModel();
            add(new AjaxLink(id, new Model(title)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    try {
                        Request request = executeWorkflow(workflowModel, action);
                        request.getContext().apply(target);
                    } catch (RepositoryException ex) {
                        log.error("Workflow action failed", ex);
                    }
                }
            });
        } else {
            add(new EmptyPanel(id));
        }
    }
    
    protected void addWorkflowDialog(String id, String title, IDialogFactory dialogFactory) {
        addWorkflowDialog(id, title, true, dialogFactory);
    }

    protected void addWorkflowDialog(final String id, String title, boolean visible, IDialogFactory dialogFactory) {
        if (visible) {
            Channel channel = getTopChannel();
            ChannelFactory channelFactory = getPluginManager().getChannelFactory();
            IPluginModel pluginModel = getPluginModel();
            add(new DialogLink(id, new Model(title), dialogFactory, pluginModel, channel, channelFactory));
        } else {
            add(new EmptyPanel(id));
        }
    }
    
    public Workflow getWorkflow() {
        Workflow workflow = null;
        try {
            WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
            WorkflowsModel workflowModel = (WorkflowsModel) getModel();
            workflow = manager.getWorkflow(workflowModel.getWorkflowDescriptor());
        } catch (MappingException e) {
            log.error(e.getMessage());
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return workflow;
    }
       
    private Request executeWorkflow(WorkflowsModel workflowModel, WorkflowAction action) throws RepositoryException {
        JcrNodeModel handle = workflowModel.getNodeModel();
        while (handle.getParentModel() != null && !handle.getNode().isNodeType(HippoNodeType.NT_HANDLE)) {
            handle = handle.getParentModel();
        }
        handle.getNode().save();
        ((UserSession) Session.get()).getJcrSession().refresh(true);

        Channel channel = getTopChannel();
        Request request = null;
        try {
            request = action.execute(channel, getWorkflow());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        ((UserSession) Session.get()).getJcrSession().refresh(true);

        if (request != null) {
            channel.send(request);
        } else {
            request = channel.createRequest("flush", new JcrNodeModel(workflowModel).getParentModel());
            channel.send(request);
            request = channel.createRequest("select", new JcrNodeModel(workflowModel));
            channel.send(request);
        }
        return request;
    }

}
