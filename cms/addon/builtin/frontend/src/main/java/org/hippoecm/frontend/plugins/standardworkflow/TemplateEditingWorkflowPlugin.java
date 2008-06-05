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
package org.hippoecm.frontend.plugins.standardworkflow;

import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowAction;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.EditmodelWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class TemplateEditingWorkflowPlugin extends AbstractWorkflowPlugin {
    private static final long serialVersionUID = 1L;

    static protected Logger log = LoggerFactory.getLogger(TemplateEditingWorkflowPlugin.class);

    public TemplateEditingWorkflowPlugin(PluginDescriptor pluginDescriptor, final IPluginModel model,
            Plugin parentPlugin) {
        super(pluginDescriptor, (WorkflowsModel) model, parentPlugin);

        addWorkflowAction("save", "Save", true, new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public Request execute(Channel channel, Workflow wf) throws Exception {
                WorkflowsModel model = (WorkflowsModel) getModel();

                Request request = null;
                if (channel != null) {
                    request = channel.createRequest("save", new JcrNodeModel(model));
                    channel.send(request);
                }
                return request; // FIXME: can only return one request, shows problem with model
            }
        });
    }

    @Override
    public void receive(Notification notification) {
        if ("save".equals(notification.getOperation())) {
            WorkflowsModel workflowModel = (WorkflowsModel) getModel();
            if (workflowModel.getNodeModel().equals(notification.getModel())) {
                try {
                    WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                    EditmodelWorkflow workflow = (EditmodelWorkflow) manager.getWorkflow(workflowModel
                            .getWorkflowDescriptor());
                    workflow.save();
                } catch (MappingException e) {
                    log.error(e.getMessage());
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }
        super.receive(notification);
    }
}
