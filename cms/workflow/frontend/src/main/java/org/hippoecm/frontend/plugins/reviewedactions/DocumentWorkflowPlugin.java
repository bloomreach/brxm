/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.DestinationDialog;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.editor.workflow.CopyNameHelper;
import org.hippoecm.frontend.editor.workflow.dialog.DeleteDialog;
import org.hippoecm.frontend.editor.workflow.dialog.WhereUsedDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.HistoryDialog;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.Localized;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

public class DocumentWorkflowPlugin extends AbstractDocumentWorkflowPlugin {

    private StdWorkflow deleteAction;
    private StdWorkflow requestDeleteAction;
    private StdWorkflow renameAction;
    private StdWorkflow copyAction;
    private StdWorkflow moveAction;
    private StdWorkflow whereUsedAction;
    private StdWorkflow historyAction;

    public DocumentWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

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
                    return new PackageResourceReference(getClass(), "img/rename-16.png");
                } else {
                    return new PackageResourceReference(getClass(), "img/rename-disabled-16.png");
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
            protected IDialogService.Dialog createRequestDialog() {
                try {
                    final HippoNode node = getModelNode();
                    uriName = node.getName();
                    targetName = getLocalizedNameForSession(node);
                    localizedNames = node.getLocalizedNames();
                } catch (RepositoryException ex) {
                    uriName = targetName = "";
                    localizedNames = Collections.emptyMap();
                }
                return new RenameDocumentDialog(this,
                        new StringResourceModel("rename-title", DocumentWorkflowPlugin.this, null),
                        context);
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
                    ((DocumentWorkflow) wf).rename(nodeName);
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
                return new PackageResourceReference(getClass(), "img/copy-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                destination = new NodeModelWrapper(getFolder()) {};
                CopyNameHelper copyNameHelper = new CopyNameHelper(getNodeNameCodec(), new StringResourceModel(
                        "copyof", DocumentWorkflowPlugin.this, null).getString());
                try {
                    name = copyNameHelper.getCopyName(((HippoNode) ((WorkflowDescriptorModel) getDefaultModel())
                            .getNode()).getLocalizedName(), destination.getNodeModel().getNode());
                } catch (RepositoryException ex) {
                    return new ExceptionDialog(ex);
                }
                return new DestinationDialog(
                        new StringResourceModel("copy-title", DocumentWorkflowPlugin.this, null),
                        new StringResourceModel("copy-name", DocumentWorkflowPlugin.this, null),
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
                DocumentWorkflow workflow = (DocumentWorkflow) wf;

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
                    return new PackageResourceReference(getClass(), "img/move-16.png");
                } else {
                    return new PackageResourceReference(getClass(), "img/move-disabled-16.png");
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
            protected IDialogService.Dialog createRequestDialog() {
                destination = new NodeModelWrapper(getFolder()) {
                };
                return new DestinationDialog(new StringResourceModel("move-title",
                        DocumentWorkflowPlugin.this, null), null, null, destination,
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
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
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
                    return new PackageResourceReference(getClass(), "img/delete-16.png");
                } else {
                    return new PackageResourceReference(getClass(), "img/delete-disabled-16.png");
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
            protected IDialogService.Dialog createRequestDialog() {
                IModel<String> message = new StringResourceModel("delete-message",
                        DocumentWorkflowPlugin.this, null, getDocumentName());
                IModel<String> title = new StringResourceModel("delete-title", DocumentWorkflowPlugin.this,
                        null, getDocumentName());
                return new DeleteDialog(title, getModel(), message, this, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
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
                return new PackageResourceReference(getClass(), "img/workflow-requestdelete-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                IModel<String> message = new StringResourceModel("delete-message",
                        DocumentWorkflowPlugin.this, null, getDocumentName());
                IModel<String> title = new StringResourceModel("delete-title", DocumentWorkflowPlugin.this,
                        null, getDocumentName());
                return new DeleteDialog(title, getModel(), message, this, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
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
                return new PackageResourceReference(getClass(), "img/where-used-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
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
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "img/revision-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = getModel();
                return new HistoryDialog(wdm, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                return null;
            }
        });

        Map<String, Serializable> info = getHints();
        hideIfNecessary(info, "requestDelete", requestDeleteAction);
        hideOrDisable(info, "delete", deleteAction);
        hideOrDisable(info, "rename", renameAction);
        hideOrDisable(info, "move", moveAction);

        hideIfNecessary(info, "copy", copyAction);
        hideOrDisable(info, "listVersions", historyAction);
    }
}
