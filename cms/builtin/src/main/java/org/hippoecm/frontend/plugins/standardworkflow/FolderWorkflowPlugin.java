/*
 *  Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.addon.workflow.WorkflowDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.l10n.ResourceBundleModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeNameModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.icon.HippoIconStack;
import org.hippoecm.frontend.plugins.standards.icon.HippoIconStack.Position;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.CmsIcon;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.util.CodecUtils;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentWorkflowAction;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.api.WorkflowTransition;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderWorkflowPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    private static final String HIPPO_TEMPLATES_BUNDLE_NAME = "hippo:templates";

    private static Logger log = LoggerFactory.getLogger(FolderWorkflowPlugin.class);

    public FolderWorkflowPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(new Label("new"));

        try {
            WorkflowDescriptorModel model = getModel();
            List<StdWorkflow> list = new LinkedList<>();
            WorkflowDescriptor descriptor = model.getObject();
            WorkflowManager manager = UserSession.get().getWorkflowManager();
            Workflow workflow = manager.getWorkflow(descriptor);
            final Map<String, Serializable> hints = workflow.hints();

            if (isActionAvailable("rename", hints)) {
                add(new StdWorkflow("rename", new StringResourceModel("rename-title", this, null), context, getModel()) {
                    private RenameDocumentArguments renameDocumentArguments = new RenameDocumentArguments();

                    @Override
                    protected Component getIcon(final String id) {
                        return HippoIcon.fromSprite(id, Icon.TYPE);
                    }

                    @Override
                    protected Dialog createRequestDialog() {

                        try {
                            HippoNode node = (HippoNode) ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                            renameDocumentArguments.setUriName(node.getName());
                            renameDocumentArguments.setTargetName(node.getDisplayName());
                            renameDocumentArguments.setNodeType(node.getPrimaryNodeType().getName());
                        } catch (RepositoryException ex) {
                            log.error("Could not retrieve workflow document", ex);
                            renameDocumentArguments.setUriName(StringUtils.EMPTY);
                            renameDocumentArguments.setTargetName(StringUtils.EMPTY);
                            renameDocumentArguments.setNodeType(null);
                        }

                        return newRenameDocumentDialog(renameDocumentArguments, this);
                    }

                    @Override
                    protected void execute(WorkflowDescriptorModel model) throws Exception {
                        // FIXME: this assumes that folders are always embedded in other folders
                        // and there is some logic here to look up the parent.  The real solution is
                        // in the visual component to merge two workflows.
                        HippoNode node = (HippoNode) model.getNode();
                        String nodeName = getNodeNameCodec(node).encode(renameDocumentArguments.getUriName());
                        String localName = getLocalizeCodec().encode(renameDocumentArguments.getTargetName());
                        WorkflowManager manager = UserSession.get().getWorkflowManager();
                        DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                        FolderWorkflow folderWorkflow = (FolderWorkflow) manager.getWorkflow("embedded", node.getParent());
                        if (!((WorkflowDescriptorModel) getDefaultModel()).getNode().getName().equals(nodeName)) {
                            folderWorkflow.rename(node.getName() + (node.getIndex() > 1 ? "[" + node.getIndex() + "]" : ""), nodeName);
                        }
                        if (!node.getDisplayName().equals(localName)) {
                            defaultWorkflow.setDisplayName(localName);
                        }
                    }
                });
            }

            if (isActionAvailable("reorder", hints)) {
                add(new StdWorkflow("reorder", new StringResourceModel("reorder-folder", this, null), context, getModel()) {
                    public List<String> order = new LinkedList<>();

                    @Override
                    protected Component getIcon(final String id) {
                        return HippoIcon.fromSprite(id, Icon.SORT);
                    }

                    @Override
                    protected Dialog createRequestDialog() {
                        return new ReorderDialog(this, config, getModel(), order);
                    }

                    @Override
                    protected void execute(WorkflowDescriptorModel model) throws Exception {
                        WorkflowManager manager = UserSession.get().getWorkflowManager();
                        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow(model.getObject());
                        workflow.reorder(order);
                    }
                });
            }

            if (isActionAvailable("delete", hints)) {
                add(new StdWorkflow("delete", new StringResourceModel("delete-title", this, null), context, getModel()) {

                    @Override
                    protected Component getIcon(final String id) {
                        return HippoIcon.fromSprite(id, Icon.TIMES);
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
                            try {
                                final IModel<String> folderName = new NodeNameModel(new JcrNodeModel(folderNode));
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
                                return new DeleteDialog(this, messageModel, deleteAllowed);

                            } catch (RepositoryException e) {
                                log.error("Unable to retrieve number of child nodes from node, folder delete cancelled", e);
                            }
                        }

                        final StringResourceModel notificationModel = new StringResourceModel("delete-message-error",
                                FolderWorkflowPlugin.this, null);
                        return new DeleteDialog(this, notificationModel, false);
                    }

                    class DeleteDialog extends WorkflowDialog<Void> {

                        public DeleteDialog(IWorkflowInvoker invoker, IModel<String> notification, boolean deleteAllowed) {
                            super(invoker);

                            setSize(DialogConstants.SMALL_AUTO);
                            setNotification(notification);

                            if (deleteAllowed) {
                                setFocusOnOk();
                            } else {
                                setOkEnabled(false);
                                setFocusOnCancel();
                            }
                        }

                        @Override
                        public IModel<String> getTitle() {
                            return new StringResourceModel("delete-title", FolderWorkflowPlugin.this, null);
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

            final Set<String> translated = new TreeSet<>();
            if (getPluginConfig().containsKey("workflow.translated")) {
                Collections.addAll(translated, getPluginConfig().getStringArray("workflow.translated"));
            }

            if (isActionAvailable("add", hints) && hints.containsKey("prototypes")) {
                final Map<String, Set<String>> prototypes = (Map<String, Set<String>>) hints.get("prototypes");
                for (final String category : prototypes.keySet()) {
                    IModel<String> categoryLabel = new StringResourceModel("add-category", this, null,
                            new ResourceBundleModel(HIPPO_TEMPLATES_BUNDLE_NAME, category));

                    final StdWorkflow<FolderWorkflow> stdWorkflow = new StdWorkflow<FolderWorkflow>("id", categoryLabel, getPluginContext(), model) {

                        AddDocumentArguments addDocumentModel = new AddDocumentArguments();

                        @Override
                        protected Dialog createRequestDialog() {
                            return createAddDocumentDialog(
                                    addDocumentModel,
                                    category,
                                    prototypes.get(category),
                                    translated.contains(category),
                                    this
                            );
                        }

                        @Override
                        protected Component getIcon(final String id) {
                            final String pngName = category + "-16";
                            final String svgName = category + "-" + IconSize.M.name().toLowerCase();

                            // Override using inline SVG icon
                            ResourceReference iconResource = new PackageResourceReference(getClass(), svgName + ".svg");
                            if (!resourceExists(iconResource)) {
                                // Override using PNG icon
                                iconResource = new PackageResourceReference(getClass(), pngName + ".png");
                            }

                            if (resourceExists(iconResource)) {
                                // Return override icon
                                return HippoIcon.fromResource(id, iconResource);
                            }

                            // Try to match svgName with a built-in Icon
                            final String iconName = StringUtils.replace(svgName, "-", "_").toUpperCase();
                            for (Icon icon : Icon.values()) {
                                if (icon.name().equals(iconName)) {
                                    return HippoIcon.fromSprite(id, icon);
                                }
                            }

                            // Fallback to default icon
                            HippoIconStack defaultIcon = new HippoIconStack(id, IconSize.M);
                            defaultIcon.addFromSprite(category.endsWith("-folder") ? Icon.FOLDER : Icon.FILE);
                            defaultIcon.addFromCms(CmsIcon.OVERLAY_PLUS, IconSize.M, Position.TOP_LEFT);
                            return defaultIcon;
                        }

                        private boolean resourceExists(final ResourceReference reference) {
                            return !(reference.getResource() == null ||
                                    (reference.getResource() instanceof PackageResource && ((PackageResource) reference.getResource()).getResourceStream() == null));
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
                                String nodeName = getNodeNameCodec(getModel().getNode()).encode(addDocumentModel.getUriName());
                                String localName = getLocalizeCodec().encode(addDocumentModel.getTargetName());
                                if ("".equals(nodeName)) {
                                    throw new IllegalArgumentException("You need to enter a name");
                                }

                                TreeMap<String, String> arguments = new TreeMap<>();
                                arguments.put("name", nodeName);
                                arguments.put("localName", localName);
                                if (StringUtils.isNotBlank(addDocumentModel.getLanguage())) {
                                    arguments.put(HippoTranslationNodeType.LOCALE, addDocumentModel.getLanguage());
                                }
                                String path = workflow.add(category, addDocumentModel.getPrototype(), arguments);
                                onWorkflowAdded(path);
                                JcrNodeModel nodeModel = new JcrNodeModel(path);
                                if (!nodeName.equals(localName)) {
                                    WorkflowManager workflowMgr = UserSession.get().getWorkflowManager();
                                    DefaultWorkflow defaultWorkflow = (DefaultWorkflow) workflowMgr.getWorkflow("core", nodeModel.getNode());
                                    defaultWorkflow.setDisplayName(localName);
                                }
                                select(nodeModel);
                            } else {
                                log.error("no workflow defined on model for selected node");
                            }
                            return null;
                        }
                    };
                    list.add(stdWorkflow);
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

    protected void onWorkflowAdded(String path) { }

    private boolean isActionAvailable(final String action, final Map<String, Serializable> hints) {
        return hints.containsKey(action) && hints.get(action) instanceof Boolean && (Boolean) hints.get(action);
    }

    @Override
    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) super.getModel();
    }

    protected Dialog newRenameDocumentDialog(RenameDocumentArguments renameDocumentArguments, IWorkflowInvoker invoker) {

        String locale = getCodecLocale();
        IModel<StringCodec> codecModel = CodecUtils.getNodeNameCodecModel(getPluginContext(), locale);

        return new RenameDocumentDialog(
                renameDocumentArguments,
                new StringResourceModel("rename-title", this, null),
                invoker,
                codecModel,
                this.getModel()
        );
    }

    protected boolean isLanguageKnown() {
        WorkflowDescriptorModel descriptorModel = (WorkflowDescriptorModel) getDefaultModel();
        try {
            Node node = descriptorModel.getNode();
            while (node != null && node.getDepth() > 0) {
                if (node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                    return true;
                }
                node = node.getParent();
            }
        } catch (RepositoryException e) {
            log.error("Could not determine whether language is known");
        }
        return false;
    }

    protected AddDocumentDialog createAddDocumentDialog(AddDocumentArguments addDocumentModel,
                                                        String category, Set<String> prototypes, boolean translated,
                                                        IWorkflowInvoker invoker) {
        String locale = getCodecLocale();
        IModel<StringCodec> codecModel = CodecUtils.getNodeNameCodecModel(getPluginContext(), locale);

        AddDocumentDialog dialog = new AddDocumentDialog(
                addDocumentModel,
                new ResourceBundleModel(HIPPO_TEMPLATES_BUNDLE_NAME, category),
                category,
                prototypes,
                translated && !isLanguageKnown(),
                invoker,
                codecModel,
                getLocaleProvider(),
                this.getModel()
        );

        if (locale != null) {
            dialog.getLanguageField().setVisible(false);
        }

        return dialog;
    }

    protected IDataProvider<StdWorkflow> createListDataProvider(List<StdWorkflow> list) {
        return new ListDataProvider<>(list);
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
                        final WorkflowTransition transition = new WorkflowTransition.Builder()
                                .action(DocumentWorkflowAction.OBTAIN_EDITABLE_INSTANCE)
                                .contextPayload(InitializationPayload.get())
                                .build();
                        Document editableDocument = (Document) workflow.transition(transition);
                        if (editableDocument != null) {
                            session.refresh(true);
                            final Node docNode = session.getNodeByIdentifier(editableDocument.getIdentity());
                            editNodeModel = new JcrNodeModel(docNode);
                        }
                    } catch (WorkflowException | RepositoryException ex) {
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
        return CodecUtils.getDisplayNameCodec(getPluginContext());
    }

    protected StringCodec getNodeNameCodec(Node node) {
        return CodecUtils.getNodeNameCodec(getPluginContext(), node);
    }

    protected ILocaleProvider getLocaleProvider() {
        return getPluginContext().getService(
                getPluginConfig().getString(ILocaleProvider.SERVICE_ID, ILocaleProvider.class.getName()),
                ILocaleProvider.class);
    }

    private String getCodecLocale() {
        try {
            return CodecUtils.getLocaleFromNode(getModel().getNode());
        } catch (RepositoryException e) {
            return null;
        }
    }

}
