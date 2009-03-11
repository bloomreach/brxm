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
package org.hippoecm.frontend.plugins.standardworkflow.dialogs;

import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standardworkflow.EditmodelWorkflowPlugin;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.standardworkflow.EditmodelWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyModelDialog extends AbstractWorkflowDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(CopyModelDialog.class);

    private String name;

    public CopyModelDialog(EditmodelWorkflowPlugin plugin) {
        super(plugin);

        WorkflowsModel wflModel = (WorkflowsModel) getPlugin().getModel();
        if (wflModel.getNodeModel().getNode() == null) {
            ok.setEnabled(false);
        }

        try {
            name = wflModel.getNodeModel().getNode().getName();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }

        add(new TextFieldWidget("name", new PropertyModel(this, "name")));
    }

    @Override
    protected void execute() throws Exception {
        EditmodelWorkflow workflow = (EditmodelWorkflow) getWorkflow();
        if (workflow != null) {
            String path = workflow.copy(name);
            ((UserSession) Session.get()).getJcrSession().refresh(true);

            JcrNodeModel nodeModel = new JcrNodeModel(new JcrItemModel(path));
            if (path != null) {
                IPluginContext context = getPlugin().getPluginContext();
                IPluginConfig config = getPlugin().getPluginConfig();

                IEditorManager editService = context.getService(config.getString(IEditorManager.EDITOR_ID),
                        IEditorManager.class);
                IEditor editor = editService.openEditor(nodeModel);
                IRenderService renderer = context.getService(context.getReference(editor).getServiceId(),
                        IRenderService.class);
                if (renderer != null) {
                    renderer.focus(null);
                }
            } else {
                log.error("no model found to edit");
            }
        } else {
            log.error("no workflow defined on model for selected node");
        }
    }
    
    public IModel getTitle() {
        return new StringResourceModel("copy-model", this, null);
    }

}
