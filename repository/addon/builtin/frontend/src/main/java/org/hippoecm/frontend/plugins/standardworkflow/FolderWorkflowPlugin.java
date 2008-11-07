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
package org.hippoecm.frontend.plugins.standardworkflow;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.model.StringResourceModel;

import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.AbstractNameDialog;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.DialogAction;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;

public class FolderWorkflowPlugin extends AbstractFolderWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    transient Logger log = LoggerFactory.getLogger(FolderWorkflowPlugin.class);

    public FolderWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        DialogAction deleteAction = new DialogAction(new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService dialogService) {
                StringResourceModel text;
                try {
                    Object[] params = new Object[] { ((WorkflowsModel) FolderWorkflowPlugin.this.getModel()).getNodeModel().getNode()
                            .getName() };
                    text = new StringResourceModel("delete-message-extended", null, null, params);
                } catch (RepositoryException ex) {
                    text = new StringResourceModel("delete-message", (Component) null, null);
                }
                return new AbstractWorkflowDialog(FolderWorkflowPlugin.this, dialogService, new StringResourceModel("delete-title", (Component) null, null), text) {
                    @Override
                    protected void execute() throws Exception {
                        // FIXME: this assumes that folders are always embedded in other folders
                        // and there is some logic here to look up the parent.  The real solution is
                        // in the visual component to merge two workflows.
                        WorkflowsModel model = (WorkflowsModel) FolderWorkflowPlugin.this.getModel();
                        Node node = model.getNodeModel().getNode();
                        WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("embedded", node.getParent());
                        workflow.delete(node.getName());
                    }
                };
            }
        }, getDialogService());
        addWorkflowAction(new StringResourceModel("delete-title", this, null), "editmodel_ico", null, deleteAction);

        final StringResourceModel renameTitle = new StringResourceModel("rename-title", this, null);
        final StringResourceModel renameText = new StringResourceModel("rename-text", this, null);
        DialogAction renameAction = new DialogAction(new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService dialogService) {
                StringResourceModel text;
                try {
                    Object[] params = new Object[] { ((WorkflowsModel) FolderWorkflowPlugin.this.getModel()).getNodeModel().getNode()
                            .getName() };
                    text = new StringResourceModel("delete-message-extended", null, null, params);
                } catch (RepositoryException ex) {
                    text = new StringResourceModel("delete-message", (Component) null, null);
                }

                return new AbstractNameDialog(FolderWorkflowPlugin.this, dialogService, renameTitle, renameText, "") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void execute() throws Exception {
                        // FIXME: this assumes that folders are always embedded in other folders
                        // and there is some logic here to look up the parent.  The real solution is
                        // in the visual component to merge two workflows.
                        WorkflowsModel model = (WorkflowsModel) FolderWorkflowPlugin.this.getModel();
                        Node node = model.getNodeModel().getNode();
                        WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("embedded", node.getParent());
                        workflow.rename(node.getName(), name);
                    }
                };
            }
        }, getDialogService());
        addWorkflowAction(new StringResourceModel("rename-title", this, null), "editmodel_ico", null, renameAction);
}

    @Override
    protected Component createDialogLinksComponent() {
        return new WorkflowActionComponentDropDownChoice(DIALOG_LINKS_COMPONENT_ID, templates);
    }
}
