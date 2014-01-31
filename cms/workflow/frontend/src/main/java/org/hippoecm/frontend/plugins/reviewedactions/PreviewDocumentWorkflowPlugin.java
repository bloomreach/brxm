/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.AbstractWorkflowDialog;
import org.hippoecm.addon.workflow.DestinationDialog;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.editor.workflow.CopyNameHelper;
import org.hippoecm.frontend.editor.workflow.dialog.DeleteDialog;
import org.hippoecm.frontend.editor.workflow.dialog.WhereUsedDialog;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
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
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.plugins.standardworkflow.RenameMessage;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Localized;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.hippoecm.repository.reviewedactions.UnlockWorkflow;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewDocumentWorkflowPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PreviewDocumentWorkflowPlugin.class);

    @SuppressWarnings("unused") // used by a PropertyModel
    private String inUseBy = StringUtils.EMPTY;

    private StdWorkflow infoAction;
    private StdWorkflow infoEditAction;
    private StdWorkflow editAction;
    private StdWorkflow unlockAction;
    private StdWorkflow publishAction;
    private StdWorkflow depublishAction;
    private StdWorkflow requestPublishAction;
    private StdWorkflow requestDepublishAction;
    private StdWorkflow deleteAction;
    private StdWorkflow requestDeleteAction;
    private StdWorkflow renameAction;
    private StdWorkflow copyAction;
    private StdWorkflow moveAction;
    private StdWorkflow schedulePublishAction;
    private StdWorkflow scheduleDepublishAction;
    private StdWorkflow whereUsedAction;
    private StdWorkflow historyAction;

    public PreviewDocumentWorkflowPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        final TypeTranslator translator = new TypeTranslator(new JcrNodeTypeModel(HippoStdNodeType.NT_PUBLISHABLESUMMARY));
        add(infoAction = new StdWorkflow("info", "info") {

            @Override
            public String getSubMenu() {
                return "info";
            }

            @Override
            protected IModel getTitle() {
                return translator.getValueName(HippoStdNodeType.HIPPOSTD_STATESUMMARY,
                        new PropertyModel<String>(PreviewDocumentWorkflowPlugin.this, "stateSummary"));
            }

            @Override
            protected void invoke() {
            }
        });

        add(infoEditAction = new StdWorkflow("infoEdit", "infoEdit") {

            @Override
            public String getSubMenu() {
                return "info";
            }

            @Override
            protected IModel getTitle() {
                return new StringResourceModel("in-use-by", this, null,
                        new PropertyModel(PreviewDocumentWorkflowPlugin.this, "inUseBy"));
            }

            @Override
            protected void invoke() {
            }
        });

        add(editAction = new StdWorkflow("edit", new StringResourceModel("edit-label", this, null), getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "edit-16.png");
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                Document docRef = workflow.obtainEditableInstance();
                Session session = UserSession.get().getJcrSession();
                session.refresh(true);
                Node docNode = session.getNodeByUUID(docRef.getIdentity());
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

        add(unlockAction = new StdWorkflow<UnlockWorkflow>("unlock", new StringResourceModel("unlock", this, null),
                null, context, getModel()) {

            @Override
            public String getSubMenu() {
                return "admin";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "unlock-16.png");
            }

            @Override
            protected String execute(UnlockWorkflow workflow) throws Exception {
                workflow.unlock();
                return null;
            }
        });

        add(publishAction = new StdWorkflow("publish", new StringResourceModel("publish-label", this, null), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "publication";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "publish-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                HippoNode node;
                try {
                    node = (HippoNode) ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                    final UnpublishedReferenceProvider referenced = new UnpublishedReferenceProvider(
                            new ReferenceProvider(new JcrNodeModel(node)));
                    if (referenced.size() > 0) {
                        return new UnpublishedReferencesDialog(publishAction, new UnpublishedReferenceNodeProvider(
                                referenced), getEditorManager());
                    }
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                }
                return null;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.publish();
                return null;
            }
        });

        add(depublishAction = new StdWorkflow("depublish", new StringResourceModel("depublish-label", this, null), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "publication";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "unpublish-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                final IModel docName = getDocumentName();
                IModel title = new StringResourceModel("depublish-title", PreviewDocumentWorkflowPlugin.this, null, docName);
                IModel message = new StringResourceModel("depublish-message", PreviewDocumentWorkflowPlugin.this, null, docName);
                return new DepublishDialog(title, message, getModel(), this, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.depublish();
                return null;
            }
        });

        add(requestPublishAction = new StdWorkflow("requestPublication", new StringResourceModel("request-publication", this, null), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "publication";
            }

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

        add(requestDepublishAction = new StdWorkflow("requestDepublication", new StringResourceModel("request-depublication", this, null), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "publication";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "workflow-requestunpublish-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                final IModel docName = getDocumentName();
                IModel<String> title = new StringResourceModel("depublish-title", PreviewDocumentWorkflowPlugin.this, null, docName);
                IModel<String> message = new StringResourceModel("depublish-message", PreviewDocumentWorkflowPlugin.this, null, docName);
                return new DepublishDialog(title, message, getModel(), this, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.requestDepublication();
                return null;
            }
        });

        add(schedulePublishAction = new StdWorkflow("schedulePublish", new StringResourceModel(
                "schedule-publish-label", this, null), context, getModel()) {
            public Date date = new Date();

            @Override
            public String getSubMenu() {
                return "publication";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "publish-schedule-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
                try {
                    return new SchedulePublishDialog(this, new JcrNodeModel(wdm.getNode()), new PropertyModel<Date>(this, "date"), getEditorManager());
                } catch (RepositoryException ex) {
                    log.warn("could not retrieve node for scheduling publish", ex);
                }
                return null;
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

        add(scheduleDepublishAction = new StdWorkflow("scheduleDepublish", new StringResourceModel(
                "schedule-depublish-label", this, null), context, getModel()) {
            public Date date = new Date();

            @Override
            public String getSubMenu() {
                return "publication";
            }

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
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                if (date != null) {
                    workflow.depublish(date);
                } else {
                    workflow.depublish();
                }
                return null;
            }
        });

        add(renameAction = new StdWorkflow("rename", new StringResourceModel("rename-label", this, null), context, getModel()) {
            public String targetName;
            public String uriName;
            public Map<Localized, String> localizedNames;

            @Override
            public String getSubMenu() {
                return "document";
            }

            @Override
            protected ResourceReference getIcon() {
                if (isEnabled()) {
                    return new PackageResourceReference(getClass(), "rename-16.png");
                } else {
                    return new PackageResourceReference(getClass(), "rename-disabled-16.png");
                }
            }

            @Override
            protected IModel getTooltip() {
                if (isEnabled()) {
                    return super.getTooltip();
                } else {
                    return new StringResourceModel("unavailable-tip", this, null);
                }
            }

            @Override
            protected Dialog createRequestDialog() {
                try {
                    final HippoNode node = getModelNode();
                    uriName = node.getName();
                    targetName = getLocalizedNameForSession(node);
                    localizedNames = node.getLocalizedNames();
                } catch (RepositoryException ex) {
                    uriName = targetName = "";
                    localizedNames = Collections.emptyMap();
                }
                return new RenameDocumentDialog(this, new StringResourceModel("rename-title",
                        PreviewDocumentWorkflowPlugin.this, null));
            }

            private HippoNode getModelNode() throws RepositoryException {
                final WorkflowDescriptorModel model = (WorkflowDescriptorModel) getDefaultModel();
                return (HippoNode) model.getNode();
            }

            private String getLocalizedNameForSession(final HippoNode node) throws RepositoryException {
                final Locale cmsLocale = UserSession.get().getLocale();
                final Localized cmsLocalized = Localized.getInstance(cmsLocale);
                return node.getLocalizedName(cmsLocalized);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                if (targetName == null || targetName.trim().equals("")) {
                    throw new WorkflowException("No name for destination given");
                }
                final HippoNode node = getModelNode();
                String nodeName = getNodeNameCodec().encode(uriName);
                String localName = getLocalizeCodec().encode(targetName);
                if ("".equals(nodeName)) {
                    throw new IllegalArgumentException("You need to enter a name");
                }
                WorkflowManager manager = UserSession.get().getWorkflowManager();
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                if (!((WorkflowDescriptorModel) getDefaultModel()).getNode().getName().equals(nodeName)) {
                    ((FullReviewedActionsWorkflow) wf).rename(nodeName);
                }
                if (!getLocalizedNameForSession(node).equals(localName)) {
                    defaultWorkflow.replaceAllLocalizedNames(localName);
                }
                return null;
            }
        });

        add(copyAction = new StdWorkflow("copy", new StringResourceModel("copy-label", this, null), context, getModel()) {
            NodeModelWrapper destination = null;
            String name = null;

            @Override
            public String getSubMenu() {
                return "document";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "copy-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                destination = new NodeModelWrapper(getFolder()) {};
                CopyNameHelper copyNameHelper = new CopyNameHelper(getNodeNameCodec(), new StringResourceModel(
                        "copyof", PreviewDocumentWorkflowPlugin.this, null).getString());
                try {
                    name = copyNameHelper.getCopyName(((HippoNode) ((WorkflowDescriptorModel) getDefaultModel())
                            .getNode()).getLocalizedName(), destination.getNodeModel().getNode());
                } catch (RepositoryException ex) {
                    return new ExceptionDialog(ex);
                }
                return new DestinationDialog(
                        new StringResourceModel("copy-title", PreviewDocumentWorkflowPlugin.this, null),
                        new StringResourceModel("copy-name", PreviewDocumentWorkflowPlugin.this, null),
                        new PropertyModel<String>(this, "name"),
                        destination, getPluginContext(), getPluginConfig()) {
                    {
                        setOkEnabled(true);
                    }

                    @Override
                    public void invokeWorkflow() throws Exception {
                        copyAction.invokeWorkflow();
                    }
                };
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                JcrNodeModel folderModel;
                if (destination != null) {
                    folderModel = destination.getNodeModel();
                } else {
                    folderModel = new JcrNodeModel("/");
                }
                StringCodec codec = getNodeNameCodec();
                String nodeName = codec.encode(name);
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;

                workflow.copy(new Document(folderModel.getNode()), nodeName);
                JcrNodeModel resultModel = new JcrNodeModel(folderModel.getItemModel().getPath() + "/" + nodeName);
                Node result = resultModel.getNode();

                WorkflowManager manager = UserSession.get().getWorkflowManager();
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", result.getNode(nodeName));
                defaultWorkflow.localizeName(getLocalizeCodec().encode(name));

                browseTo(resultModel);
                return null;
            }
        });

        add(moveAction = new StdWorkflow("move", new StringResourceModel("move-label", this, null), context, getModel()) {
            NodeModelWrapper destination;

            @Override
            public String getSubMenu() {
                return "document";
            }

            @Override
            protected ResourceReference getIcon() {
                if (isEnabled()) {
                    return new PackageResourceReference(getClass(), "move-16.png");
                } else {
                    return new PackageResourceReference(getClass(), "move-disabled-16.png");
                }
            }

            @Override
            protected IModel getTooltip() {
                if (isEnabled()) {
                    return super.getTooltip();
                } else {
                    return new StringResourceModel("unavailable-tip", this, null);
                }
            }

            @Override
            protected Dialog createRequestDialog() {
                destination = new NodeModelWrapper(getFolder()) {
                };
                return new DestinationDialog(new StringResourceModel("move-title",
                        PreviewDocumentWorkflowPlugin.this, null), null, null, destination,
                                             getPluginContext(), getPluginConfig()) {
                    @Override
                    public void invokeWorkflow() throws Exception {
                        moveAction.invokeWorkflow();
                    }
                };
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                JcrNodeModel folderModel = new JcrNodeModel("/");
                if (destination != null) {
                    folderModel = destination.getNodeModel();
                }
                String nodeName = getModel().getNode().getName();
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.move(new Document(folderModel.getNode()), nodeName);
                browseTo(new JcrNodeModel(folderModel.getItemModel().getPath() + "/" + nodeName));
                return null;
            }
        });

        add(deleteAction = new StdWorkflow("delete",
                new StringResourceModel("delete-label", this, null), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "document";
            }

            @Override
            protected ResourceReference getIcon() {
                if (isEnabled()) {
                    return new PackageResourceReference(getClass(), "delete-16.png");
                } else {
                    return new PackageResourceReference(getClass(), "delete-disabled-16.png");
                }
            }

            @Override
            protected IModel getTooltip() {
                if (isEnabled()) {
                    return super.getTooltip();
                } else {
                    return new StringResourceModel("unavailable-tip", this, null);
                }
            }

            @Override
            protected Dialog createRequestDialog() {
                IModel<String> message = new StringResourceModel("delete-message",
                        PreviewDocumentWorkflowPlugin.this, null, getDocumentName());
                IModel<String> title = new StringResourceModel("delete-title", PreviewDocumentWorkflowPlugin.this,
                        null, getDocumentName());
                return new DeleteDialog(title, getModel(), message, this, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.delete();
                return null;
            }
        });

        add(requestDeleteAction = new StdWorkflow("requestDelete", new StringResourceModel("request-delete", this, null), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "document";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "workflow-requestdelete-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                IModel<String> message = new StringResourceModel("delete-message",
                        PreviewDocumentWorkflowPlugin.this, null, getDocumentName());
                IModel<String> title = new StringResourceModel("delete-title", PreviewDocumentWorkflowPlugin.this,
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

        add(whereUsedAction = new StdWorkflow("where-used", new StringResourceModel("where-used-label", this, null), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "document";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "where-used-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = getModel();
                return new WhereUsedDialog(wdm, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                return null;
            }
        });

        add(historyAction = new StdWorkflow("history", new StringResourceModel("history-label", this, null), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "document";
            }

            @Override
            public boolean isEnabled() {
                try {
                    final Node node = getModel().getNode();
                    final Node parent = node.getParent();
                    if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                        return !parent.isNodeType(JcrConstants.MIX_VERSIONABLE);
                    }
                } catch (RepositoryException e) {
                    log.warn("Unable to determine whether version history is available", e);
                }
                return false;
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "revision-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = getModel();
                return new HistoryDialog(wdm, getEditorManager());
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

    private JcrNodeModel getFolder() {
        JcrNodeModel folderModel = new JcrNodeModel("/");
        try {
            WorkflowDescriptorModel wdm = getModel();
            if (wdm != null) {
                Node node = wdm.getNode();
                if (node != null) {
                    folderModel = new JcrNodeModel(node.getParent().getParent());
                }
            }
        } catch (RepositoryException ex) {
            log.warn("Could not determine folder path", ex);
        }
        return folderModel;
    }

    private IEditorManager getEditorManager() {
        return getPluginContext().getService(getPluginConfig().getString("editor.id"), IEditorManager.class);
    }

    protected StringCodec getLocalizeCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID,
                ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.display");
    }

    protected StringCodec getNodeNameCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID,
                ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.node");
    }

    @SuppressWarnings("unused")  // used by a PropertyModel
    public String getStateSummary() {
        try {
            WorkflowDescriptorModel wdm = getModel();
            Node handleNode = wdm.getNode();
            for (Node child : new NodeIterable(handleNode.getNodes())) {
                if (child.hasProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY)) {
                    return child.getProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY).getString();
                }
            }
        } catch (RepositoryException ex) {
            log.warn("Unable to ascertain state summary", ex);
        }
        return "";
    }

    private void hideInvalidActions() {
        try {
            WorkflowManager manager = UserSession.get().getWorkflowManager();
            WorkflowDescriptorModel wdm = getModel();
            WorkflowDescriptor workflowDescriptor = wdm.getObject();
            if (workflowDescriptor != null) {
                Workflow workflow = manager.getWorkflow(workflowDescriptor);
                Map<String, Serializable> info = workflow.hints();

                hideIfNecessary(info, "obtainEditableInstance", editAction);
                hideIfNecessary(info, "unlock", unlockAction);

                hideIfNecessary(info, "publish", publishAction);
                hideIfNecessary(info, "schedulePublish", schedulePublishAction);
                hideIfNecessary(info, "depublish", depublishAction);
                hideIfNecessary(info, "scheduleDepublish", scheduleDepublishAction);

                hideIfNecessary(info, "requestDelete", requestDeleteAction);
                hideIfNecessary(info, "requestPublish", requestPublishAction);
                hideIfNecessary(info, "requestDepublish", requestDepublishAction);

                hideOrDisable(deleteAction, info, "delete");
                hideOrDisable(renameAction, info, "rename");
                hideOrDisable(moveAction, info, "move");

                hideIfNecessary(info, "copy", copyAction);
                hideIfNecessary(info, "status", infoAction, whereUsedAction, historyAction);

                if (info.containsKey("inUseBy") && info.get("inUseBy") instanceof String) {
                    inUseBy = (String) info.get("inUseBy");
                    infoEditAction.setVisible(true);
                } else {
                    infoEditAction.setVisible(false);
                }
            }
        } catch (RepositoryException | WorkflowException | RemoteException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    void hideIfNecessary(Map<String, Serializable> info, String key, StdWorkflow... actions) {
        if (info.containsKey(key) && info.get(key) instanceof Boolean && !(Boolean) info.get(key)) {
            for (StdWorkflow action : actions) {
                action.setVisible(false);
            }
        }
    }

    void hideOrDisable(StdWorkflow action, Map<String, Serializable> info, String key) {
        if (info.containsKey(key)) {
            if (info.get(key) instanceof Boolean && !(Boolean) info.get(key)) {
                action.setEnabled(false);
            }
        } else {
            action.setVisible(false);
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
        UserSession.get().getJcrSession().refresh(false);

        getPluginContext().getService(getPluginConfig().getString(IBrowseService.BROWSER_ID), IBrowseService.class)
                .browse(nodeModel);
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

    public class RenameDocumentDialog extends AbstractWorkflowDialog {
        private IModel title;
        private TextField nameComponent;
        private TextField uriComponent;
        private boolean uriModified;

        public RenameDocumentDialog(StdWorkflow action, IModel title) {
            super(null, action);

            this.title = title;

            final PropertyModel<String> nameModel = new PropertyModel<String>(action, "targetName");
            final PropertyModel<String> uriModel = new PropertyModel<String>(action, "uriName");
            final PropertyModel<Map<Localized, String>> localizedNamesModel = new PropertyModel<Map<Localized, String>>(action, "localizedNames");

            String s1 = nameModel.getObject();
            String s2 = uriModel.getObject();
            uriModified = !s1.equals(s2);

            nameComponent = new TextField<String>("name", nameModel);
            nameComponent.setRequired(true);
            nameComponent.setLabel(new StringResourceModel("name-label", this, null));
            nameComponent.add(new OnChangeAjaxBehavior() {

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    if (!uriModified) {
                        uriModel.setObject(getNodeNameCodec().encode(nameModel.getObject()));
                        target.add(uriComponent);
                    }
                }

                @Override
                protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                    super.updateAjaxAttributes(attributes);
                    attributes.setThrottlingSettings(new ThrottlingSettings(nameComponent.getPath(), Duration.milliseconds(500)));
                }
            });
            nameComponent.setOutputMarkupId(true);
            setFocus(nameComponent);
            add(nameComponent);

            add(uriComponent = new TextField<String>("uriinput", uriModel) {
                @Override
                public boolean isEnabled() {
                    return uriModified;
                }
            });

            uriComponent.add(new CssClassAppender(new AbstractReadOnlyModel<String>() {
                @Override
                public String getObject() {
                    return uriModified ? "grayedin" : "grayedout";
                }
            }));
            uriComponent.setOutputMarkupId(true);

            AjaxLink<Boolean> uriAction = new AjaxLink<Boolean>("uriAction") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    uriModified = !uriModified;
                    if (!uriModified) {
                        uriModel.setObject(Strings.isEmpty(nameModel.getObject()) ? "" : getNodeNameCodec().encode(
                                nameModel.getObject()));
                    } else {
                        target.focusComponent(uriComponent);
                    }
                    target.add(RenameDocumentDialog.this);
                }
            };
            uriAction.add(new Label("uriActionLabel", new AbstractReadOnlyModel<String>() {
                @Override
                public String getObject() {
                    return uriModified ? getString("url-reset") : getString("url-edit");
                }
            }));
            add(uriAction);

            final Locale cmsLocale = UserSession.get().getLocale();
            final RenameMessage message = new RenameMessage(cmsLocale, localizedNamesModel.getObject());
            if (message.shouldShow()) {
                warn(message.forDocument());
            };
        }

        @Override
        public IModel getTitle() {
            return title;
        }

        @Override
        public IValueMap getProperties() {
            return MEDIUM;
        }
    }
}
