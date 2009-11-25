/*
 *  Copyright 2009 Hippo.
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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.DeleteDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.DepublishDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.HistoryDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.WhereUsedDialog;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullReviewedActionsWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(FullReviewedActionsWorkflowPlugin.class);

    public String stateSummary = "UNKNOWN";

    public String inUseBy = "";

    StdWorkflow infoAction;
    StdWorkflow infoEditAction;
    WorkflowAction editAction;
    WorkflowAction publishAction;
    WorkflowAction depublishAction;
    WorkflowAction deleteAction;
    WorkflowAction renameAction;
    WorkflowAction copyAction;
    WorkflowAction moveAction;
    WorkflowAction schedulePublishAction;
    WorkflowAction scheduleDepublishAction;
    WorkflowAction whereUsedAction;
    WorkflowAction historyAction;

    public FullReviewedActionsWorkflowPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        final TypeTranslator translator = new TypeTranslator(new JcrNodeTypeModel("hippostd:publishableSummary"));
        add(infoAction = new StdWorkflow("info", "info") {
            @Override
            protected IModel getTitle() {
                return translator.getValueName("hippostd:stateSummary", new PropertyModel(
                        FullReviewedActionsWorkflowPlugin.this, "stateSummary"));
            }

            @Override
            protected void invoke() {
            }
        });

        add(infoEditAction = new StdWorkflow("infoEdit", "infoEdit") {
            @Override
            protected IModel getTitle() {
                return new StringResourceModel("in-use-by", this, null, new Object[] { new PropertyModel(
                        FullReviewedActionsWorkflowPlugin.this, "inUseBy") });
            }

            @Override
            protected void invoke() {
            }
        });

        add(editAction = new WorkflowAction("edit", new StringResourceModel("edit-label", this, null).getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "edit-16.png");
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                Document docRef = workflow.obtainEditableInstance();
                ((UserSession) getSession()).getJcrSession().refresh(true);
                Node docNode = ((UserSession) getSession()).getJcrSession().getNodeByUUID(docRef.getIdentity());
                IEditorManager editorMgr = getPluginContext().getService(
                        getPluginConfig().getString(IEditorManager.EDITOR_ID), IEditorManager.class);
                if (editorMgr != null) {
                    JcrNodeModel docModel = new JcrNodeModel(docNode);
                    IEditor editor = editorMgr.getEditor(docModel);
                    if (editor == null) {
                        editorMgr.openEditor(docModel);
                    }
                } else {
                    log.warn("No editor found to edit {}", docNode.getPath());
                }
                return null;
            }
        });

        add(publishAction = new WorkflowAction("publish", new StringResourceModel("publish-label", this, null)
                .getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "publish-16.png");
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.publish();
                return null;
            }
        });

        add(depublishAction = new WorkflowAction("depublish", new StringResourceModel("depublish-label", this, null)
                .getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "unplublish-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                final IModel docName = getDocumentName();
                // FIXME: no longer necessary in Wicket-1.4.x; see WICKET-2381
                IModel title = new StringResourceModel("depublish-title", FullReviewedActionsWorkflowPlugin.this,
                        null, new Object[] { docName }) {
                    @Override
                    public void detach() {
                        docName.detach();
                        super.detach();
                    }
                };
                // FIXME: no longer necessary in Wicket-1.4.x; see WICKET-2381
                IModel message = new StringResourceModel("depublish-message", FullReviewedActionsWorkflowPlugin.this,
                        null, new Object[] { docName }) {
                    @Override
                    public void detach() {
                        docName.detach();
                        super.detach();
                    }
                };
                return new DepublishDialog(title, message, this, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.depublish();
                return null;
            }
        });

        add(schedulePublishAction = new WorkflowAction("schedulePublish", new StringResourceModel(
                "schedule-publish-label", this, null).getString(), null) {
            public Date date = new Date();

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "publish-schedule-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                return new WorkflowAction.DateDialog(new StringResourceModel("schedule-publish-text",
                        FullReviewedActionsWorkflowPlugin.this, null), new PropertyModel(this, "date")) {
                    @Override
                    public IModel getTitle() {
                        return new StringResourceModel("schedule-publish-title",
                                FullReviewedActionsWorkflowPlugin.this, null);
                    }
                };
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                if (date != null) {
                    workflow.publish(date);
                } else {
                    workflow.publish();
                }
                return null;
            }
        });

        add(scheduleDepublishAction = new WorkflowAction("scheduleDepublish", new StringResourceModel(
                "schedule-depublish-label", this, null).getString(), null) {
            public Date date = new Date();

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "unpublish-scheduled-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                return new WorkflowAction.DateDialog(new StringResourceModel("schedule-depublish-text",
                        FullReviewedActionsWorkflowPlugin.this, null), new PropertyModel(this, "date")) {
                    @Override
                    public IModel getTitle() {
                        return new StringResourceModel("schedule-depublish-title",
                                FullReviewedActionsWorkflowPlugin.this, null);
                    }
                };
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                if (date != null) {
                    workflow.depublish(date);
                } else {
                    workflow.depublish();
                }
                return null;
            }
        });

        add(renameAction = new WorkflowAction("rename", new StringResourceModel("rename-label", this, null)) {
            public String name;

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "rename-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                name = getInputNodeName();
                return new WorkflowAction.NameDialog(new StringResourceModel("rename-title",
                        FullReviewedActionsWorkflowPlugin.this, null), new StringResourceModel("rename-text",
                        FullReviewedActionsWorkflowPlugin.this, null), new PropertyModel(this, "name"));
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                if (name == null || name.trim().equals("")) {
                    throw new WorkflowException("No name for destination given");
                }
                if (name.equals(getInputNodeName())) {
                    // shortcut, the node was not actually renamed
                    return null;
                }
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                String nodeName = NodeNameCodec.encode(name, true);
                workflow.rename(nodeName);
                return null;
            }
        });

        add(copyAction = new WorkflowAction("copy", new StringResourceModel("copy-label", this, null)) {
            public String name;
            public NodeModelWrapper destination = new NodeModelWrapper(new JcrNodeModel("/")) {
            };

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "copy-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                name = getInputNodeName();
                return new WorkflowAction.DestinationDialog(new StringResourceModel("copy-title",
                        FullReviewedActionsWorkflowPlugin.this, null), new StringResourceModel("copy-text",
                        FullReviewedActionsWorkflowPlugin.this, null), new PropertyModel(this, "name"), destination);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                if (name == null || name.trim().equals("")) {
                    throw new WorkflowException("No name for destination given");
                }
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.copy(new Document(destination.getNodeModel().getNode().getUUID()), NodeNameCodec.encode(name,
                        true));
                return null;
            }
        });

        add(moveAction = new WorkflowAction("move", new StringResourceModel("move-label", this, null)) {
            public String name;
            public NodeModelWrapper destination = new NodeModelWrapper(new JcrNodeModel("/")) {
            };

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "move-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                name = getInputNodeName();
                return new WorkflowAction.DestinationDialog(new StringResourceModel("move-title",
                        FullReviewedActionsWorkflowPlugin.this, null), new StringResourceModel("move-text",
                        FullReviewedActionsWorkflowPlugin.this, null), new PropertyModel(this, "name"), destination);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                if (name == null || name.trim().equals("")) {
                    throw new WorkflowException("No name for destination given");
                }
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                String nodeName = NodeNameCodec.encode(name, true);
                workflow.move(new Document(destination.getNodeModel().getNode().getUUID()), nodeName);
                browseTo(new JcrNodeModel(destination.getNodeModel().getItemModel().getPath() + "/" + nodeName));
                return null;
            }
        });

        add(deleteAction = new WorkflowAction("delete",
                new StringResourceModel("delete-label", this, null).getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "delete-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                final IModel docName = getDocumentName();
                // FIXME: no longer necessary in Wicket-1.4.x; see WICKET-2381
                IModel message = new StringResourceModel("delete-message", FullReviewedActionsWorkflowPlugin.this,
                        null, new Object[] { getDocumentName() }) {
                    @Override
                    public void detach() {
                        docName.detach();
                        super.detach();
                    }
                };
                // FIXME: no longer necessary in Wicket-1.4.x; see WICKET-2381
                IModel title = new StringResourceModel("delete-title", FullReviewedActionsWorkflowPlugin.this,
                        null, new Object[] { getDocumentName() }) {
                    @Override
                    public void detach() {
                        docName.detach();
                        super.detach();
                    }
                };
                return new DeleteDialog(title, message, this, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.delete();
                return null;
            }
        });

        add(whereUsedAction = new WorkflowAction("where-used", new StringResourceModel("where-used-label", this, null)
                .getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "where-used-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
                return new WhereUsedDialog(wdm, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                return null;
            }
        });

        add(historyAction = new WorkflowAction("history", new StringResourceModel("history-label", this, null)
                .getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "revision-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
                return new HistoryDialog(wdm, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                return null;
            }
        });

        onModelChanged();
    }

    private IEditorManager getEditorManager() {
        return getPluginContext().getService(getPluginConfig().getString("editor.id"), IEditorManager.class);
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        try {
            WorkflowManager manager = ((UserSession) org.apache.wicket.Session.get()).getWorkflowManager();
            WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) getDefaultModel();
            WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) getDefaultModelObject();
            if (workflowDescriptor != null) {
                Node documentNode = workflowDescriptorModel.getNode();
                if (documentNode != null && documentNode.hasProperty("hippostd:stateSummary")) {
                    stateSummary = documentNode.getProperty("hippostd:stateSummary").getString();
                }
                Workflow workflow = manager.getWorkflow(workflowDescriptor);
                Map<String, Serializable> info = workflow.hints();
                if (info.containsKey("obtainEditableInstanceobtainEditableInstance")
                        && info.get("obtainEditableInstanceobtainEditableInstance") instanceof Boolean
                        && !((Boolean) info.get("obtainEditableInstanceobtainEditableInstance")).booleanValue()) {
                    editAction.setVisible(false);
                }
                if (info.containsKey("publish") && info.get("publish") instanceof Boolean
                        && !((Boolean) info.get("publish")).booleanValue()) {
                    publishAction.setVisible(false);
                    schedulePublishAction.setVisible(false);
                }
                if (info.containsKey("depublish") && info.get("depublish") instanceof Boolean
                        && !((Boolean) info.get("depublish")).booleanValue()) {
                    depublishAction.setVisible(false);
                    scheduleDepublishAction.setVisible(false);
                }
                if (info.containsKey("delete") && info.get("delete") instanceof Boolean
                        && !((Boolean) info.get("delete")).booleanValue()) {
                    deleteAction.setVisible(false);
                }
                if (info.containsKey("rename") && info.get("rename") instanceof Boolean
                        && !((Boolean) info.get("rename")).booleanValue()) {
                    renameAction.setVisible(false);
                }
                if (info.containsKey("move") && info.get("move") instanceof Boolean
                        && !((Boolean) info.get("move")).booleanValue()) {
                    moveAction.setVisible(false);
                }
                if (info.containsKey("copy") && info.get("copy") instanceof Boolean
                        && !((Boolean) info.get("copy")).booleanValue()) {
                    copyAction.setVisible(false);
                }
                if (info.containsKey("status") && info.get("status") instanceof Boolean
                        && !((Boolean) info.get("status")).booleanValue()) {
                    infoAction.setVisible(false);
                    whereUsedAction.setVisible(false);
                    historyAction.setVisible(false);
                }
                if (info.containsKey("inUseBy") && info.get("inUseBy") instanceof String) {
                    inUseBy = (String) info.get("inUseBy");
                    infoEditAction.setVisible(true);
                } else {
                    infoEditAction.setVisible(false);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        } catch (WorkflowException ex) {
            log.error(ex.getMessage(), ex);
        } catch (RemoteException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    /**
     * Get the name of the node this workflow operates on
     * 
     * @return The name of the node that the workflow operates on or an empty String if an error occurs
     * @throws RepositoryException
     */
    //    private String getInputNodeName() {
    //        WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel)getModel();
    //        try {
    //            return new NodeTranslator(new JcrNodeModel(workflowDescriptorModel.getNode())).getNodeName().getObject().toString();
    //        } catch (RepositoryException e) {
    //            log.error("Error translating node name", e);
    //        }
    //        return "";
    //    }
    /**
     * Use the IBrowseService to select the node referenced by parameter path
     * 
     * @param nodeModel Absolute path of node to browse to
     * @throws RepositoryException
     */
    private void browseTo(JcrNodeModel nodeModel) throws RepositoryException {
        //refresh session before IBrowseService.browse is called
        ((UserSession) org.apache.wicket.Session.get()).getJcrSession().refresh(false);

        getPluginContext().getService(getPluginConfig().getString(IBrowseService.BROWSER_ID), IBrowseService.class)
                .browse(nodeModel);
    }

    IModel getDocumentName() {
        try {
            return (new NodeTranslator(new JcrNodeModel(((WorkflowDescriptorModel) getDefaultModel()).getNode())))
                    .getNodeName();
        } catch (RepositoryException ex) {
            try {
                return new Model(((WorkflowDescriptorModel) getDefaultModel()).getNode().getName());
            } catch (RepositoryException e) {
                return new StringResourceModel("unknown", this, null);
            }
        }
    }

}
