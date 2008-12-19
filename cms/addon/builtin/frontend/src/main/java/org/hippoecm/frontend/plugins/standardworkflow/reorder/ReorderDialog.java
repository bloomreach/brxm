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

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReorderDialog extends AbstractWorkflowDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ReorderDialog.class);

    private IServiceReference<IJcrService> jcrServiceRef;
    private ReorderPanel panel;

    public ReorderDialog(AbstractWorkflowPlugin plugin, IServiceReference<IJcrService> jcrService) {
        super(plugin);
        jcrServiceRef = jcrService;

        JcrNodeModel folderModel = ((WorkflowsModel) plugin.getModel()).getNodeModel();
        panel = new ReorderPanel("reorder-panel", folderModel, new DocumentListFilter(plugin.getPluginConfig()));
        add(panel);
        String name;
        try {
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

    @Override
    protected void execute() throws Exception {
        FolderWorkflow workflow = (FolderWorkflow) getWorkflow();
        workflow.reorder(panel.getMapping());

        IModel pluginModel = getPlugin().getModel();
        if (pluginModel instanceof JcrNodeModel) {
            jcrServiceRef.getService().flush((JcrNodeModel) pluginModel);
        }
    }

}
