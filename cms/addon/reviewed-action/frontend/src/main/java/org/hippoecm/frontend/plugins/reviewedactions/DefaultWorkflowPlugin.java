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

import java.rmi.RemoteException;
import javax.jcr.Node;

import javax.jcr.RepositoryException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.WorkflowAction;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(BasicReviewedActionsWorkflowPlugin.class);

    private IModel caption = new StringResourceModel("unknown", this, null);

    public DefaultWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);}/*

        add(new Label("caption", caption));

        onModelChanged();

        addWorkflowAction("edit-dialog", new StringResourceModel("edit", this, null), new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void execute(Workflow wf) throws Exception {
                Node handleNode = ((WorkflowDescriptorModel)DefaultWorkflowPlugin.this.getModel()).getNode();
                Node docNode = handleNode.getNodes(handleNode.getName()).nextNode();
                IEditorManager viewer = getPluginContext().getService(getPluginConfig().getString(IEditorManager.EDITOR_ID), IEditorManager.class);
                if (viewer != null) {
                    viewer.openEditor(new JcrNodeModel(docNode));
                } else {
                    log.warn("No editor found to edit {}", handleNode.getPath());
                }
            }
        });

        IModel deleteLabel = new StringResourceModel("delete-label", this, null);
        addWorkflowDialog("delete-dialog", deleteLabel, deleteLabel, 
                          new StringResourceModel("delete-message", this, null, new Object[] { caption }), new WorkflowAction() {
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

            public AbstractDialog createDialog() {

                return new NameDialog(renameTitle, renameText, "") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String execute() {
                        try {
                        DefaultWorkflow workflow = (DefaultWorkflow) getWorkflow();
                        workflow.rename(NodeNameCodec.encode(name, true));
                        return null;
                        } catch(MappingException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        } catch(RepositoryException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        } catch(WorkflowException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        } catch(RemoteException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        }
                    }
                };
            }
        });
    }

    @Override
    public void onModelChanged() {
        try {
        super.onModelChanged();
        WorkflowDescriptorModel model = (WorkflowDescriptorModel) getModel();
        caption = new NodeTranslator(new JcrNodeModel(model.getNode())).getNodeName();
        } catch(RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }
*/
}
