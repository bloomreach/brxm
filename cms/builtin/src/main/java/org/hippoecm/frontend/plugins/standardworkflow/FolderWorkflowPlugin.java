/*
 *  Copyright 2009-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standardworkflow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.AbstractWorkflowDialog;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Localized;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderWorkflowPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(FolderWorkflowPlugin.class);

    public FolderWorkflowPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(new Label("new"));

        try {
            WorkflowDescriptorModel model = getModel();
            List<StdWorkflow> list = new LinkedList<StdWorkflow>();
            WorkflowDescriptor descriptor = model.getObject();
            WorkflowManager manager = UserSession.get().getWorkflowManager();
            Workflow workflow = manager.getWorkflow(descriptor);
            final Map<String, Serializable> hints = workflow.hints();

            if (isActionAvailable("rename", hints)) {
                add(new StdWorkflow("rename", new StringResourceModel("rename-title", this, null), context, getModel()) {

                    RenameDocumentArguments renameDocumentModel = new RenameDocumentArguments();

                    @Override
                    protected ResourceReference getIcon() {
                        return new PackageResourceReference(getClass(), "rename-16.png");
                    }

                    @Override
                    protected Dialog createRequestDialog() {

                        try {
                            HippoNode node = (HippoNode) ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                            renameDocumentModel.setUriName(node.getName());
                            renameDocumentModel.setTargetName(getLocalizedNameForSession(node));
                            renameDocumentModel.setNodeType(node.getPrimaryNodeType().getName());
                            renameDocumentModel.setLocalizedNames(node.getLocalizedNames());
                        } catch (RepositoryException ex) {
                            log.error("Could not retrieve workflow document", ex);
                            renameDocumentModel.setUriName("");
                            renameDocumentModel.setTargetName("");
                            renameDocumentModel.setNodeType(null);
                            renameDocumentModel.setLocalizedNames(null);
                        }

                        return newRenameDocumentDialog(renameDocumentModel, this);
                    }

                    private String getLocalizedNameForSession(final HippoNode node) throws RepositoryException {
                        final Locale cmsLocale = UserSession.get().getLocale();
                        final Localized cmsLocalized = Localized.getInstance(cmsLocale);
                        return node.getLocalizedName(cmsLocalized);
                    }

                    @Override
                    protected void execute(WorkflowDescriptorModel model) throws Exception {
                        // FIXME: this assumes that folders are always embedded in other folders
                        // and there is some logic here to look up the parent.  The real solution is
                        // in the visual component to merge two workflows.
                        HippoNode node = (HippoNode) model.getNode();
                        String nodeName = getNodeNameCodec().encode(renameDocumentModel.getUriName());
                        String localName = getLocalizeCodec().encode(renameDocumentModel.getTargetName());
                        WorkflowManager manager = UserSession.get().getWorkflowManager();
                        DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                        FolderWorkflow folderWorkflow = (FolderWorkflow) manager.getWorkflow("embedded", node.getParent());
                        if (!((WorkflowDescriptorModel) getDefaultModel()).getNode().getName().equals(nodeName)) {
                            folderWorkflow.rename(node.getName() + (node.getIndex() > 1 ? "[" + node.getIndex() + "]" : ""), nodeName);
                        }
                        if (!getLocalizedNameForSession(node).equals(localName)) {
                            defaultWorkflow.replaceAllLocalizedNames(localName);
                        }
                    }
                });
            }

            if (isActionAvailable("reorder", hints)) {
                add(new StdWorkflow("reorder", new StringResourceModel("reorder-folder", this, null), context, getModel()) {
                    public List<String> order = new LinkedList<String>();

                    @Override
                    protected ResourceReference getIcon() {
                        return new PackageResourceReference(getClass(), "reorder-16.png");
                    }

                    @Override
                    protected Dialog createRequestDialog() {
                        return new ReorderDialog(this, config, getModel(), order);
                    }

                    @Override
                    protected void execute(WorkflowDescriptorModel model) throws Exception {
                        WorkflowManager manager = UserSession.get().getWorkflowManager();
                        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow((WorkflowDescriptor) model.getObject());
                        workflow.reorder(order);
                    }
                });
            }

            if (isActionAvailable("delete", hints)) {
                add(new StdWorkflow("delete", new StringResourceModel("delete-title", this, null), context, getModel()) {

                    @Override
                    protected ResourceReference getIcon() {
                        return new PackageResourceReference(getClass(), "delete-16.png");
                    }

                    @Override
                    protected Dialog createRequestDialog() {
                        Node folderNode = null;
                        try {
                            folderNode = getModel().getNode();
                        } catch (RepositoryException e) {
                            log.error("Unable to retrieve node from WorkflowDescriptorModel, folder delete cancelled", e);
                        }

                        if (folderNode != null) {
                            final IModel folderName = new NodeTranslator(new JcrNodeModel(folderNode)).getNodeName();
                            try {
                                boolean deleteAllowed = true;
                                for (NodeIterator iter = folderNode.getNodes(); iter.hasNext(); ) {
                                    Node child = iter.nextNode();
                                    NodeDefinition nd = child.getDefinition();
                                    if (nd.getDeclaringNodeType().isMixin()) {
                                        continue;
                                    }
                                    deleteAllowed = false;
                                    break;
                                }
                                StringResourceModel messageModel = new StringResourceModel(deleteAllowed ? "delete-message-extended" : "delete-message-denied",
                                        FolderWorkflowPlugin.this, null, folderName);
                                return new DeleteDialog(messageModel, this, deleteAllowed);

                            } catch (RepositoryException e) {
                                log.error("Unable to retrieve number of child nodes from node, folder delete cancelled", e);
                            }
                        }
                        return new DeleteDialog(new StringResourceModel("delete-message-error", FolderWorkflowPlugin.this, null), this, false);
                    }

                    class DeleteDialog extends AbstractWorkflowDialog {

                        public DeleteDialog(IModel messageModel, IWorkflowInvoker invoker, boolean deleteAllowed) {
                            super(null, messageModel, invoker);

                            if (deleteAllowed) {
                                setFocusOnOk();
                            } else {
                                setOkEnabled(false);
                                setFocusOnCancel();
                            }
                        }

                        @Override
                        public IModel getTitle() {
                            return new StringResourceModel("delete-title", FolderWorkflowPlugin.this, null);
                        }

                        @Override
                        public IValueMap getProperties() {
                            return SMALL;
                        }
                    }

                    @Override
                    public void execute(WorkflowDescriptorModel model) throws Exception {
                        // FIXME: this assumes that folders are always embedded in other folders
                        // and there is some logic here to look up the parent.  The real solution is
                        // in the visual component to merge two workflows.
                        Node node = model.getNode();
                        WorkflowManager manager = UserSession.get().getWorkflowManager();
                        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("embedded", node.getParent());
                        workflow.delete(node.getName() + (node.getIndex() > 1 ? "[" + node.getIndex() + "]" : ""));
                    }
                });
            }

            final Set<String> translated = new TreeSet<String>();
            if (getPluginConfig().containsKey("workflow.translated")) {
                Collections.addAll(translated, getPluginConfig().getStringArray("workflow.translated"));
            }

            if (isActionAvailable("add", hints) && hints.containsKey("prototypes")) {
                final Map<String, Set<String>> prototypes = (Map<String, Set<String>>) hints.get("prototypes");
                for (final String category : prototypes.keySet()) {
                    IModel<String> categoryLabel = new StringResourceModel("add-category", this, null,
                            new StringResourceModel(category, this, null));
                    ResourceReference iconResource = new PackageResourceReference(getClass(), category + "-16.png");
                    if (iconResource.getResource() == null ||
                            (iconResource.getResource() instanceof PackageResource && ((PackageResource) iconResource.getResource()).getResourceStream() == null)) {
                        iconResource = new PackageResourceReference(getClass(), "new-document-16.png");
                    }
                    list.add(new StdWorkflow<FolderWorkflow>("id", categoryLabel, iconResource, getPluginContext(), model) {

                        AddDocumentArguments addDocumentModel = new AddDocumentArguments();

                        @Override
                        protected Dialog createRequestDialog() {
                            return newAddDocumentDialog(
                                    addDocumentModel,
                                    category,
                                    prototypes.get(category),
                                    translated.contains(category),
                                    this
                            );
                        }

                        @Override
                        protected String execute(FolderWorkflow workflow) throws Exception {
                            if (addDocumentModel.getPrototype() == null) {
                                throw new IllegalArgumentException("You need to select a type");
                            }
                            if (addDocumentModel.getTargetName() == null || "".equals(addDocumentModel.getTargetName())) {
                                throw new IllegalArgumentException("You need to enter a name");
                            }
                            if (addDocumentModel.getUriName() == null || "".equals(addDocumentModel.getUriName())) {
                                throw new IllegalArgumentException("You need to enter a URL name");
                            }
                            if (workflow != null) {
                                if (!prototypes.get(category).contains(addDocumentModel.getPrototype())) {
                                    log.error("unknown folder type " + addDocumentModel.getPrototype());
                                    return "Unknown folder type " + addDocumentModel.getPrototype();
                                }
                                String nodeName = getNodeNameCodec().encode(addDocumentModel.getUriName());
                                String localName = getLocalizeCodec().encode(addDocumentModel.getTargetName());
                                if ("".equals(nodeName)) {
                                    throw new IllegalArgumentException("You need to enter a name");
                                }

                                TreeMap<String, String> arguments = new TreeMap<String, String>();
                                arguments.put("name", nodeName);

                                String path = workflow.add(category, addDocumentModel.getPrototype(), arguments);
                                UserSession.get().getJcrSession().refresh(true);
                                JcrNodeModel nodeModel = new JcrNodeModel(path);
                                if (!nodeName.equals(localName)) {
                                    WorkflowManager workflowMgr = UserSession.get().getWorkflowManager();
                                    DefaultWorkflow defaultWorkflow = (DefaultWorkflow) workflowMgr.getWorkflow("core", nodeModel.getNode());
                                    defaultWorkflow.localizeName(localName);
                                }
                                select(nodeModel);
                            } else {
                                log.error("no workflow defined on model for selected node");
                            }
                            return null;
                        }
                    });
                }
            }
            AbstractView add;
            replace(add = new AbstractView<StdWorkflow>("new", createListDataProvider(list)) {

                @Override
                protected void populateItem(Item<StdWorkflow> item) {
                    item.add(item.getModelObject());
                }
            });
            add.populate();
        } catch (RepositoryException | RemoteException | WorkflowException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    private boolean isActionAvailable(final String action, final Map<String, Serializable> hints) {
        return hints.containsKey(action) && hints.get(action) instanceof Boolean && (Boolean) hints.get(action);
    }

    @Override
    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) super.getModel();
    }

    protected Dialog newRenameDocumentDialog(RenameDocumentArguments renameDocumentModel, IWorkflowInvoker invoker) {
        return new RenameDocumentDialog(
                renameDocumentModel,
                new StringResourceModel("rename-title", this, null),
                invoker,
                new LoadableDetachableModel<StringCodec>() {
                    @Override
                    protected StringCodec load() {
                        return getNodeNameCodec();
                    }
                });
    }

    protected Dialog newAddDocumentDialog(AddDocumentArguments addDocumentModel, String category, Set<String> prototypes, boolean translated, IWorkflowInvoker invoker) {

        AddDocumentDialog dialog = new AddDocumentDialog(
                addDocumentModel,
                new StringResourceModel(category, this, null),
                category,
                prototypes,
                translated,
                invoker,
                new LoadableDetachableModel<StringCodec>() {
                    @Override
                    protected StringCodec load() {
                        return getNodeNameCodec();
                    }
                },
                getLocaleProvider());

        WorkflowDescriptorModel descriptorModel = (WorkflowDescriptorModel) getDefaultModel();
        try {
            Node node = descriptorModel.getNode();
            if (node != null) {
                while (node.getDepth() > 0) {
                    if (node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                        dialog.getLanguageField().setVisible(false);
                        break;
                    }
                    node = node.getParent();
                }
            }
        } catch (RepositoryException e) {
            log.error("Could not determine visibility of language field");
        }

        return dialog;
    }

    protected IDataProvider<StdWorkflow> createListDataProvider(List<StdWorkflow> list) {
        return new ListDataProvider<StdWorkflow>(list);
    }

    @SuppressWarnings("unchecked")
    public void select(JcrNodeModel nodeModel) {
        IBrowseService<JcrNodeModel> browser = getPluginContext().getService(
                getPluginConfig().getString(IBrowseService.BROWSER_ID), IBrowseService.class);
        IEditorManager editorMgr = getPluginContext().getService(getPluginConfig().getString(IEditorManager.EDITOR_ID),
                IEditorManager.class);
        try {
            final Node node = nodeModel.getNode();
            if (node != null
                    && (node.isNodeType(HippoNodeType.NT_DOCUMENT) || node.isNodeType(HippoNodeType.NT_HANDLE))) {
                if (browser != null) {
                    browser.browse(nodeModel);
                }
                if (editorMgr == null) {
                    return;
                }
                if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    log.warn("Folder workflow returned node of type handle; expected a variant");
                    return;
                }
                if (!node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                    return;
                }

                Node editNode = node.getParent();

                JcrNodeModel editNodeModel = nodeModel;
                javax.jcr.Session session = UserSession.get().getJcrSession();
                WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace())
                        .getWorkflowManager();
                Workflow workflow = workflowManager.getWorkflow("editing", editNode);
                if (workflow == null) {
                    workflow = workflowManager.getWorkflow("editing", node);
                }
                if (workflow instanceof EditableWorkflow) {
                    try {
                        EditableWorkflow editableWorkflow = (EditableWorkflow) workflow;
                        Document editableDocument = editableWorkflow.obtainEditableInstance();
                        if (editableDocument != null) {
                            session.refresh(true);
                            final Node docNode = session.getNodeByIdentifier(editableDocument.getIdentity());
                            editNodeModel = new JcrNodeModel(docNode);
                        }
                    } catch (WorkflowException | RemoteException | RepositoryException ex) {
                        log.error("Cannot auto-edit document", ex);
                    }
                }

                try {
                    IEditor editor = editorMgr.getEditor(editNodeModel);
                    if (editor == null) {
                        editorMgr.openEditor(editNodeModel);
                    } else if (editor.getMode() == IEditor.Mode.VIEW) {
                        editor.setMode(IEditor.Mode.EDIT);
                    }
                } catch (EditorException | ServiceException ex) {
                    log.error("Cannot auto-edit document", ex);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }
    }

    protected StringCodec getLocalizeCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID, ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.display");
    }

    protected StringCodec getNodeNameCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID, ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.node");
    }

    protected ILocaleProvider getLocaleProvider() {
        return getPluginContext().getService(
                getPluginConfig().getString(ILocaleProvider.SERVICE_ID, ILocaleProvider.class.getName()),
                ILocaleProvider.class);
    }

}
