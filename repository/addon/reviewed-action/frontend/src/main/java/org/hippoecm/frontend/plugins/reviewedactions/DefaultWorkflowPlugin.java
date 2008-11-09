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
package org.hippoecm.frontend.plugins.reviewedactions;

import javax.jcr.Node;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.AbstractNameDialog;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowAction;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultWorkflowPlugin extends AbstractWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(BasicReviewedActionsWorkflowPlugin.class);

    private IModel caption = new StringResourceModel("unknown", this, null);

    public DefaultWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new Label("caption", caption));

        onModelChanged();

        addWorkflowAction("edit-dialog", new StringResourceModel("edit", this, null), new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void execute(Workflow wf) throws Exception {
                Node handleNode = ((WorkflowsModel)DefaultWorkflowPlugin.this.getModel()).getNodeModel().getNode();
                Node docNode = handleNode.getNodes(handleNode.getName()).nextNode();
                IEditService viewer = getPluginContext().getService(getPluginConfig().getString(IEditService.EDITOR_ID), IEditService.class);
                if (viewer != null) {
                    viewer.edit(new JcrNodeModel(docNode));
                } else {
                    log.warn("No editor found to edit {}", handleNode.getPath());
                }
            }
        });

        addWorkflowAction("delete-dialog", new StringResourceModel("delete", this, null), new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void execute(Workflow wf) throws Exception {
                ((DefaultWorkflow)wf).delete();
            }
        });

        IModel renameLabel = new StringResourceModel("rename-label", this, null);
        final StringResourceModel renameTitle = new StringResourceModel("rename-title", this, null);
        final StringResourceModel renameText = new StringResourceModel("rename-text", this, null);
        addWorkflowDialog("rename-dialog", renameLabel, new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return true;
            }}, new IDialogFactory() {
                    private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService dialogService) {

                return new AbstractNameDialog(DefaultWorkflowPlugin.this, dialogService, renameTitle, renameText, "") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void execute() throws Exception {
                        DefaultWorkflow workflow = (DefaultWorkflow) getWorkflow();
                        workflow.rename(name);
                    }
                };
            }
        });
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        WorkflowsModel model = (WorkflowsModel) getModel();
        caption = new NodeTranslator(model.getNodeModel()).getNodeName();
    }
}
