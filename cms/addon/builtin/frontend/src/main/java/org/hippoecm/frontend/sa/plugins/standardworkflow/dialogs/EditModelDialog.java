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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.sa.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.sa.dialog.IDialogService;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugins.standardworkflow.EditmodelWorkflowPlugin;
import org.hippoecm.frontend.sa.service.IJcrService;
import org.hippoecm.frontend.sa.service.IViewService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.standardworkflow.EditmodelWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditModelDialog extends AbstractWorkflowDialog {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(EditModelDialog.class);

    public EditModelDialog(EditmodelWorkflowPlugin plugin, IDialogService dialogWindow) {
        super(plugin, dialogWindow, "Edit model");

        WorkflowsModel wflModel = (WorkflowsModel) plugin.getModel();
        if (wflModel.getNodeModel().getNode() == null) {
            ok.setVisible(false);
        }
    }

    @Override
    protected void execute() throws Exception {
        EditmodelWorkflow workflow = (EditmodelWorkflow) getWorkflow();
        if (workflow != null) {
            String path = workflow.edit();
            try {
                Node node = ((UserSession) Session.get()).getJcrSession().getRootNode().getNode(path.substring(1));
                JcrItemModel itemModel = new JcrItemModel(node);
                if (path != null) {
                    IPluginContext context = getPlugin().getPluginContext();

                    IJcrService jcrService = context.getService(IJcrService.class.getName(), IJcrService.class);
                    jcrService.flush(new JcrNodeModel(itemModel));

                    IPluginConfig config = getPlugin().getPluginConfig();
                    IViewService viewService = context.getService(config.getString(IViewService.VIEWER_ID),
                            IViewService.class);
                    viewService.view(new JcrNodeModel(itemModel));
                } else {
                    log.error("no model found to edit");
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.error("no workflow defined on model for selected node");
        }
    }

    @Override
    public void cancel() {
    }

}
