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
package org.hippoecm.frontend.plugins.standardworkflow.reorder;

import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReorderDialog extends CompatibilityWorkflowPlugin.WorkflowAction.WorkflowDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ReorderDialog.class);

    private ReorderPanel panel;
    WorkflowDescriptorModel model;

    public ReorderDialog(CompatibilityWorkflowPlugin.WorkflowAction action, IPluginConfig pluginConfig, WorkflowDescriptorModel model) {
        action . super();
        this.model = model;

        String name;
        try {
            JcrNodeModel folderModel = new JcrNodeModel(model.getNode());
            panel = new ReorderPanel("reorder-panel", folderModel, new DocumentListFilter(pluginConfig));
            add(panel);
            name = folderModel.getNode().getName();
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            name = "";
        }
        add(new Label("message", new StringResourceModel("reorder-message", this, null, new Object[] { name })));
    }
    
    public IModel getTitle() {
        return new StringResourceModel("reorder", this, null);
    }

    protected String execute2() {
        try {
            WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
            FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow((WorkflowDescriptor) model.getObject());
            workflow.reorder(panel.getMapping());
            return null;
        } catch(Exception ex) {
            return ex.getClass().getName()+": "+ex.getMessage();
        }
    }
}
