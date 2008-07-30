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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowAction;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.service.IFactoryService;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.repository.api.Workflow;

public class TemplateEditingWorkflowPlugin extends AbstractWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static protected Logger log = LoggerFactory.getLogger(TemplateEditingWorkflowPlugin.class);

    public TemplateEditingWorkflowPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        addWorkflowAction("save", new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow workflow) throws Exception {
                WorkflowsModel model = (WorkflowsModel) getModel();
                JcrNodeModel nodeModel = model.getNodeModel();
                if (nodeModel.getNode() != null) {
                    nodeModel.getNode().save();

                    IJcrService jcrService = context.getService(IJcrService.class.getName(), IJcrService.class);
                    if (jcrService != null) {
                        jcrService.flush(nodeModel);
                    }
                } else {
                    log.error("Node does not exist");
                }
                close();
            }

        });
        add(new AjaxLink("revert") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                WorkflowsModel model = (WorkflowsModel) TemplateEditingWorkflowPlugin.this.getModel();
                JcrNodeModel nodeModel = model.getNodeModel();
                if (nodeModel.getNode() != null) {
                    try {
                        nodeModel.getNode().refresh(false);
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                    IJcrService jcrService = context.getService(IJcrService.class.getName(), IJcrService.class);
                    if (jcrService != null) {
                        jcrService.flush(nodeModel);
                    }
                } else {
                    log.error("Node does not exist");
                }
                close();
            }
        });
    }

    private void close() {
        IPluginContext context = getPluginContext();
        IEditService viewer = context.getService(getPluginConfig().getString(IEditService.EDITOR_ID),
                IEditService.class);
        if (viewer != null) {
            String serviceId = context.getReference(viewer).getServiceId();
            IFactoryService factory = context.getService(serviceId, IFactoryService.class);
            if (factory != null) {
                factory.delete(viewer);
            }
        } else {
            log.warn("No editor service found");
        }
    }
}
