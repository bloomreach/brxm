/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.Session;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.editor.workflow.dialog.DeleteDialog;
import org.hippoecm.frontend.editor.workflow.dialog.DocumentMetadataDialog;
import org.hippoecm.frontend.editor.workflow.dialog.WhereUsedDialog;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.DepublishDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.HistoryDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.ScheduleDepublishDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.SchedulePublishDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.UnpublishedReferencesDialog;
import org.hippoecm.frontend.plugins.reviewedactions.model.ReferenceProvider;
import org.hippoecm.frontend.plugins.reviewedactions.model.UnpublishedReferenceProvider;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicReviewedActionsWorkflowPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(BasicReviewedActionsWorkflowPlugin.class);

    public String stateSummary = "";

    public String inUseBy = "";

    private StdWorkflow infoAction;
    private StdWorkflow infoEditAction;
    private StdWorkflow editAction;
    private StdWorkflow publishAction;
    private StdWorkflow depublishAction;
    private StdWorkflow deleteAction;
    private StdWorkflow schedulePublishAction;
    private StdWorkflow scheduleDepublishAction;
    private StdWorkflow whereUsedAction;
    private StdWorkflow historyAction;
    private StdWorkflow docMetaDataAction;

    public BasicReviewedActionsWorkflowPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        final TypeTranslator translator = new TypeTranslator(new JcrNodeTypeModel(HippoStdNodeType.NT_PUBLISHABLESUMMARY));
        add(infoAction = new StdWorkflow("info", "info") {
            @Override
            protected IModel getTitle() {
                return translator.getValueName(HippoStdNodeType.HIPPOSTD_STATESUMMARY, new PropertyModel<String>(BasicReviewedActionsWorkflowPlugin.this, "stateSummary"));
            }
            @Override
            protected void invoke() {
            }
        });

        add(infoEditAction = new StdWorkflow("infoEdit", "infoEdit") {
            @Override
            protected IModel getTitle() {
                return new StringResourceModel("in-use-by", this, null, new PropertyModel(BasicReviewedActionsWorkflowPlugin.this, "inUseBy"));
            }
            @Override
            protected void invoke() {
            }
        });

        add(editAction = new StdWorkflow<BasicReviewedActionsWorkflow>("edit", new StringResourceModel("edit", this, null), getModel()) {

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "edit-16.png");
            }

            @Override
            protected String execute(BasicReviewedActionsWorkflow workflow) throws Exception {
                Document docRef = workflow.obtainEditableInstance();
                Session session = UserSession.get().getJcrSession();
                session.refresh(true);
                Node docNode = session.getNodeByIdentifier(docRef.getIdentity());
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

        add(publishAction = new StdWorkflow("requestPublication", new StringResourceModel("request-publication", this, null), context, getModel()) {
            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "workflow-requestpublish-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                HippoNode node;
                try {
                    node = (HippoNode) ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                    final UnpublishedReferenceProvider referenced = new UnpublishedReferenceProvider(new ReferenceProvider(
                            new JcrNodeModel(node)));
                    if (referenced.size() > 0) {
                        return new UnpublishedReferencesDialog(publishAction, new UnpublishedReferenceNodeProvider(referenced), getEditorManager());
                    }
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                }
                return null;
            }
            
            @Override
            protected String execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.requestPublication();
                return null;
            }
        });

        add(depublishAction = new StdWorkflow("requestDepublication", new StringResourceModel("request-depublication", this, null), context, getModel()) {

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "workflow-requestunpublish-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                final IModel docName = getDocumentName();
                IModel<String> title = new StringResourceModel("depublish-title", BasicReviewedActionsWorkflowPlugin.this, null, docName);
                IModel<String> message = new StringResourceModel("depublish-message", BasicReviewedActionsWorkflowPlugin.this, null, docName);
                return new DepublishDialog(title, message, getModel(), this, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.requestDepublication();
                return null;
            }
        });

        add(deleteAction = new StdWorkflow("delete", new StringResourceModel("request-delete", this, null), context, getModel()) {

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "workflow-requestdelete-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                IModel<String> message = new StringResourceModel("delete-message",
                        BasicReviewedActionsWorkflowPlugin.this, null, getDocumentName());
                IModel<String> title = new StringResourceModel("delete-title", BasicReviewedActionsWorkflowPlugin.this,
                        null, getDocumentName());
                return new DeleteDialog(title, getModel(), message, this, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.requestDeletion();
                return null;
            }
        });

        add(schedulePublishAction = new StdWorkflow("schedulePublish", new StringResourceModel("schedule-publish-label", this, null), context, getModel()) {
            public Date date = new Date();

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "publish-schedule-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = getModel();
                try {
                    return new SchedulePublishDialog(this, new JcrNodeModel(wdm.getNode()), new PropertyModel<Date>(this, "date"), getEditorManager());
                } catch (RepositoryException ex) {
                    log.warn("could not retrieve node for scheduling publish", ex);
                }
                return null;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow)wf;
                if (date != null) {
                    workflow.requestPublication(date);
                } else {
                    workflow.requestPublication();
                }
                return null;
            }
        });

        add(scheduleDepublishAction = new StdWorkflow("scheduleDepublish", new StringResourceModel("schedule-depublish-label", this, null), context, getModel()) {
            public Date date = new Date();

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "unpublish-scheduled-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
                try {
                    return new ScheduleDepublishDialog(this, new JcrNodeModel(wdm.getNode()), new PropertyModel<Date>(this, "date"), getEditorManager());
                } catch (RepositoryException e) {
                    log.warn("could not retrieve node for scheduling depublish", e);
                }
                return null;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow)wf;
                if (date != null) {
                    workflow.requestDepublication(date);
                } else {
                    workflow.requestDepublication();
                }
                return null;
            }
        });

        add(whereUsedAction = new StdWorkflow("where-used", new StringResourceModel("where-used-label", this, null), context, getModel()) {

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "where-used-16.png");
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

        add(historyAction = new StdWorkflow("history", new StringResourceModel("history-label", this, null), context, getModel()) {

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "revision-16.png");
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

        add(docMetaDataAction = new StdWorkflow("docMetaData", new StringResourceModel("docmetadata-label", this, null), context, getModel()) {

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "docmetadata-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = getModel();
                return new DocumentMetadataDialog(wdm, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                return null;
            }
        });

        hideInvalidActions();
    }

    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) super.getDefaultModel();
    }

    private IEditorManager getEditorManager() {
        return getPluginContext().getService(getPluginConfig().getString("editor.id"), IEditorManager.class);
    }

    protected void hideOrDisable(StdWorkflow action, Map<String, Serializable> info, String key) {
        if (info.containsKey(key)) {
            if (info.get(key) instanceof Boolean && !(Boolean) info.get(key)) {
                action.setEnabled(false);
            }
        } else {
            action.setVisible(false);
        }
    }

    IModel<String> getDocumentName() {
        try {
            return (new NodeTranslator(new JcrNodeModel(((WorkflowDescriptorModel) getDefaultModel()).getNode())))
                    .getNodeName();
        } catch (RepositoryException ex) {
            try {
                return new Model<String>(((WorkflowDescriptorModel) getDefaultModel()).getNode().getName());
            } catch (RepositoryException e) {
                return new StringResourceModel("unknown", this, null);
            }
        }
    }

    private void hideInvalidActions() {
        try {
            WorkflowManager manager = getSession().getWorkflowManager();
            WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel)getDefaultModel();
            WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor)getDefaultModelObject();
            if(workflowDescriptor != null) {
                Node documentNode = workflowDescriptorModel.getNode();
                if(documentNode != null && documentNode.hasProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY)) {
                    stateSummary = documentNode.getProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY).getString();
                }
                Workflow workflow = manager.getWorkflow(workflowDescriptor);
                Map<String, Serializable> info = workflow.hints();
                if ((documentNode != null && !documentNode.hasProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY))
                        || (info.containsKey("obtainEditableInstance") && info.get("obtainEditableInstance") instanceof Boolean
                        && !(Boolean) info.get("obtainEditableInstance"))) {
                    editAction.setVisible(false);
                }
                if (info.containsKey("publish") && info.get("publish") instanceof Boolean && !(Boolean)info.get("publish")) {
                   publishAction.setVisible(false);
                    schedulePublishAction.setVisible(false);
                }
                if (info.containsKey("depublish") && info.get("depublish") instanceof Boolean && !(Boolean)info.get("depublish")) {
                    depublishAction.setVisible(false);
                    scheduleDepublishAction.setVisible(false);
                }
                hideOrDisable(deleteAction, info, "delete");
                if (info.containsKey("status") && info.get("status") instanceof Boolean && !(Boolean)info.get("status")) {
                    infoAction.setVisible(false);
                    whereUsedAction.setVisible(false);
                    historyAction.setVisible(false);
                    docMetaDataAction.setVisible(false);
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
}
