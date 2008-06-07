/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.sa.plugins.standardworkflow.dialogs;

import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.sa.plugins.standardworkflow.PrototypeWorkflowPlugin;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.standardworkflow.PrototypeWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderDialog extends AbstractWorkflowDialog {
    private static final long serialVersionUID = 1L;

    private transient static final Logger log = LoggerFactory.getLogger(PrototypeDialog.class);

    private String name;
    private IServiceReference<IJcrService> jcrServiceRef;

    public FolderDialog(PrototypeWorkflowPlugin plugin, IDialogService dialogWindow) {
        super(plugin, dialogWindow, "Add folder");

        IPluginContext context = plugin.getPluginContext();
        IJcrService service = context.getService(IJcrService.class.getName(), IJcrService.class);
        jcrServiceRef = context.getReference(service);

        WorkflowsModel model = (WorkflowsModel) plugin.getModel();

        name = "New folder";
        if (model.getNodeModel().getNode() == null) {
            ok.setEnabled(false);
        }
        add(new TextFieldWidget("name", new PropertyModel(this, "name")));
    }

    @Override
    protected void execute() throws Exception {
        PrototypeWorkflow workflow = (PrototypeWorkflow) getWorkflow();
        if (workflow != null) {
            String path = workflow.addFolder(name);
            JcrNodeModel nodeModel = new JcrNodeModel(new JcrItemModel(path));

            IJcrService jcrService = jcrServiceRef.getService();
            if (jcrService != null) {
                jcrService.flush(nodeModel.getParentModel());
            }

            PrototypeWorkflowPlugin plugin = (PrototypeWorkflowPlugin) getPlugin();
            plugin.select(nodeModel);
        } else {
            log.error("no workflow defined on model for selected node");
        }
    }

    @Override
    public void cancel() {
    }
}
