/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.workflow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.addon.workflow.DestinationDialog;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.addon.workflow.WorkflowDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.editor.workflow.dialog.DeleteDialog;
import org.hippoecm.frontend.editor.workflow.dialog.WhereUsedDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standardworkflow.RenameDocumentArguments;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.util.CodecUtils;
import org.hippoecm.frontend.util.DocumentUtils;
import org.hippoecm.frontend.widgets.NameUriField;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.io.FilenameUtils.EXTENSION_SEPARATOR_STR;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;

public class DefaultWorkflowPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(DefaultWorkflowPlugin.class);

    private final StdWorkflow editAction;
    private final StdWorkflow deleteAction;
    private final StdWorkflow renameAction;
    private final StdWorkflow copyAction;
    private final StdWorkflow moveAction;
    private final StdWorkflow whereUsedAction;

    private static final String NUMBER_EXPRESSION = "[0-9]*";

    public DefaultWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        onModelChanged();

        add(editAction = new StdWorkflow("edit", Model.of(getString("edit")), getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.PENCIL_SQUARE);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                Node docNode = getModel().getNode();
                IEditorManager editorMgr = getPluginContext().getService(
                        getPluginConfig().getString(IEditorManager.EDITOR_ID), IEditorManager.class);
                if (editorMgr != null) {
                    JcrNodeModel docModel = new JcrNodeModel(docNode);
                    IEditor editor = editorMgr.getEditor(docModel);
                    if (editor == null) {
                        editorMgr.openEditor(docModel);
                    } else {
                        editor.setMode(Mode.EDIT);
                    }
                } else {
                    log.warn("No editor found to edit {}", docNode.getPath());
                }
                return null;
            }
        });

        add(renameAction = new StdWorkflow("rename", Model.of(getString("rename-label")), context, getModel()) {
            private RenameDocumentArguments renameDocumentArguments;

            @Override
            public String getSubMenu() {
                return "document";
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.TYPE);
            }

            @Override
            protected Dialog createRequestDialog() {
                try {
                    final Node node = getModel().getNode();
                    renameDocumentArguments = new RenameDocumentArguments(
                            getDisplayName().getObject(),
                            node.getName());
                } catch (RepositoryException ex) {
                    renameDocumentArguments = new RenameDocumentArguments();
                }

                return new org.hippoecm.frontend.plugins.standardworkflow.RenameDocumentDialog(renameDocumentArguments,
                        new StringResourceModel("rename-title", DefaultWorkflowPlugin.this),
                        this,
                        getStringCodecModel(),
                        this.getModel()
                );
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                final String targetName = renameDocumentArguments.getTargetName();
                final String uriName = renameDocumentArguments.getUriName();

                if (Strings.isEmpty(targetName)) {
                    throw new WorkflowException("No name for destination given");
                }
                HippoNode node = (HippoNode) getModel().getNode();
                String nodeName = getNodeNameCodec(node).encode(uriName);
                String localName = getLocalizeCodec().encode(targetName);
                if ("".equals(nodeName)) {
                    throw new IllegalArgumentException("You need to enter a name");
                }
                WorkflowManager manager = obtainUserSession().getWorkflowManager();
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                if (!node.getName().equals(nodeName)) {
                    ((DefaultWorkflow) wf).rename(nodeName);
                }
                if (!getDisplayName().getObject().equals(localName)) {
                    defaultWorkflow.setDisplayName(localName);
                }
                return null;
            }
        });

        add(copyAction = new StdWorkflow("copy", Model.of(getString("copy-label")), context, getModel()) {
            NodeModelWrapper<Node> destination = null;
            String name = null;

            @Override
            public String getSubMenu() {
                return "document";
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.FILES);
            }

            @Override
            protected Dialog createRequestDialog() {
                destination = new NodeModelWrapper<Node>(getFolder()) {
                };
                try {
                    String nodeName = getDisplayName().getObject().toLowerCase();

                    if (getModel().getNode().isNodeType("hippogallery:imageset")
                            || getModel().getNode().isNodeType("hippogallery:exampleAssetSet")) {
                        createNewFileNodeNameWithBaseNameSuffix(nodeName);
                    } else {
                        String locale = CodecUtils.getLocaleFromNode(getFolder().getNode());
                        IModel<StringCodec> codec = CodecUtils.getNodeNameCodecModel(context, locale);
                        String copyof = new StringResourceModel("copyof", DefaultWorkflowPlugin.this).getString();
                        CopyNameHelper copyNameHelper = new CopyNameHelper(codec, copyof);

                        name = copyNameHelper.getCopyName(nodeName, destination.getChainedModel().getObject());
                    }
                } catch (RepositoryException ex) {
                    return new ExceptionDialog(ex);
                }
                return new DestinationDialog(
                        Model.of(getString("copy-title")),
                        Model.of(getString("copy-name")),
                        new PropertyModel<>(this, "name"), destination, context, config) {
                    {
                        setOkEnabled(true);
                    }

                    @Override
                    public void invokeWorkflow() throws Exception {
                        copyAction.invokeWorkflow();
                    }
                };
            }

            /**
             * Creates a new node name, based on the given nodeName. It will add a suffix (-1) at the end of the base
             * name (node name without the extension) of the node name. If this new name already exists, it will
             * increment the suffix (-2, -3,...) until a unique name has been found.
             *
             * @param nodeName The node name
             * @throws RepositoryException Thrown when it cannot retrieve the node from the repository
             */
            private void createNewFileNodeNameWithBaseNameSuffix(final String nodeName) throws RepositoryException {
                name = nodeName;
                Node gallery = destination.getChainedModel().getObject();
                if (gallery.hasNode(name)) {
                    name = addOrIncrementFileNodeNameBaseNameSuffix(name);
                    createNewFileNodeNameWithBaseNameSuffix(name);
                }
            }

            /**
             * Adds a suffix (-1) at the end of the base name. If it is has an existing suffix (-n) it will increment
             * this suffix to n+1.
             *
             * <p>Returns test-1.jpg when the input is test.jpg. <br/>
             * Returns test-2.jpg when the input is test-1.jpg. <br/>
             * Returns test-c-1.jpg when the input is test-c.jpg.
             * </p>
             *
             * @param nodeName The node name.
             * @return A node name with a '-1' as suffix attached to the base name or a node name which already has
             * such a suffix (-n) with an incremented (n+1) number of this suffix.
             */
            private String addOrIncrementFileNodeNameBaseNameSuffix(final String nodeName) {
                final String baseName = getBaseName(nodeName);
                final String extension = getExtension(nodeName);

                int separatorIndex = baseName.lastIndexOf("-");
                if (separatorIndex != -1 && separatorIndex != baseName.length() - 1) {
                    String copyNumberAsString = baseName.substring(separatorIndex + 1);
                    if (copyNumberAsString.matches(NUMBER_EXPRESSION)) {
                        int copyNumber = Integer.parseInt(copyNumberAsString);
                        copyNumber++;
                        return baseName.substring(0, separatorIndex + 1) + copyNumber + EXTENSION_SEPARATOR_STR +
                                extension;
                    }
                }
                return baseName + "-1" + EXTENSION_SEPARATOR_STR + extension;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                final Node folder = destination != null ?
                        destination.getChainedModel().getObject() : UserSession.get().getJcrSession().getRootNode();

                final String locale = CodecUtils.getLocaleFromDocumentOrFolder(getModel().getNode(), folder);
                final StringCodec codec = CodecUtils.getNodeNameCodec(getPluginContext(), locale);
                String nodeName = codec.encode(name);

                DefaultWorkflow workflow = (DefaultWorkflow) wf;
                workflow.copy(new Document(folder), nodeName);

                JcrNodeModel copyModel = new JcrNodeModel(folder.getPath() + "/" + nodeName);
                HippoNode node = (HippoNode) copyModel.getNode().getNode(nodeName);

                String localName = getLocalizeCodec().encode(name);
                if (!getDisplayName().getObject().equals(localName)) {
                    WorkflowManager manager = UserSession.get().getWorkflowManager();
                    DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                    defaultWorkflow.setDisplayName(localName);
                }
                browseTo(copyModel);
                return null;
            }
        });

        add(moveAction = new StdWorkflow("move", Model.of(getString("move-label")), context, getModel()) {
            public NodeModelWrapper<Node> destination = null;

            @Override
            public String getSubMenu() {
                return "document";
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.MOVE_INTO);
            }

            @Override
            protected Dialog createRequestDialog() {
                destination = new NodeModelWrapper<Node>(getFolder()) {
                };
                return new DestinationDialog(Model.of(getString("move-title")), null, null, destination, context, config) {
                    @Override
                    public void invokeWorkflow() throws Exception {
                        moveAction.invokeWorkflow();
                    }
                };
            }

            @Override
            protected String execute(Workflow wf) throws Exception {

                final Node src = getModel().getNode();
                final Node dest = destination != null ?
                        destination.getChainedModel().getObject() : UserSession.get().getJcrSession().getRootNode();

                final String srcLocale = CodecUtils.getLocaleFromNodeAndAncestors(src);
                final String destLocale = CodecUtils.getLocaleFromNodeAndAncestors(dest);

                StringCodec codec = null;
                if (srcLocale == null) {
                    if (destLocale != null) {
                        codec = CodecUtils.getNodeNameCodec(getPluginContext(), destLocale);
                    }
                } else if (!srcLocale.equals(destLocale)) {
                    codec = CodecUtils.getNodeNameCodec(getPluginContext(), destLocale);
                }

                String nodeName;
                if (codec != null) {
                    // we are moving between locales so re-encode the display name
                    nodeName = codec.encode(getDisplayName().getObject());
                } else {
                    // use original node name
                    nodeName = src.getName();
                }

                DefaultWorkflow workflow = (DefaultWorkflow) wf;
                workflow.move(new Document(dest), nodeName);
                return null;
            }

        });

        add(deleteAction = new StdWorkflow("delete", Model.of(getString("delete-label")), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "document";
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.TIMES);
            }

            @Override
            protected Dialog createRequestDialog() {
                final IModel<String> docName = getDisplayName();
                final IModel<String> message = new StringResourceModel("delete-message", DefaultWorkflowPlugin.this)
                        .setParameters(docName);
                final IModel<String> title = new StringResourceModel("delete-title", DefaultWorkflowPlugin.this)
                        .setParameters(docName);
                return new DeleteDialog(title, getModel(), message, this, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                ((DefaultWorkflow) wf).delete();
                return null;
            }
        });

        add(whereUsedAction = new StdWorkflow("where-used", Model.of(getString("where-used-label")), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "document";
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.LINK);
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

        WorkflowDescriptorModel model = getModel();
        if (model != null) {
            Map<String, Serializable> info = obtainWorkflowHints(model);
            if (info != null) {
                updateWorkflowVisibilities(info);
            }
        }
    }

    private IModel<StringCodec> getStringCodecModel() {
        String locale = null;
        try {
            locale = CodecUtils.getLocaleFromNodeAndAncestors(getModel().getNode());
        } catch (RepositoryException e) {
            //ignore
        }
        return CodecUtils.getNodeNameCodecModel(getPluginContext(), locale);
    }

    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) getDefaultModel();
    }

    private JcrNodeModel getFolder() {
        JcrNodeModel folderModel = new JcrNodeModel("/");
        try {
            WorkflowDescriptorModel wdm = getModel();
            if (wdm != null) {
                HippoNode node = (HippoNode) wdm.getNode();
                if (node != null) {
                    folderModel = new JcrNodeModel(node.getParent().getParent());
                }
            }
        } catch (RepositoryException ex) {
            log.warn("Could not determine folder path", ex);
        }
        return folderModel;
    }

    protected StringCodec getLocalizeCodec() {
        return CodecUtils.getDisplayNameCodec(getPluginContext());
    }

    protected StringCodec getNodeNameCodec(final Node node) {
        return CodecUtils.getNodeNameCodec(getPluginContext(), node);
    }

    private void browseTo(JcrNodeModel nodeModel) throws RepositoryException {
        //refresh session before IBrowseService.browse is called
        obtainUserSession().getJcrSession().refresh(false);

        @SuppressWarnings("unchecked")
        IBrowseService<IModel<Node>> service = getPluginContext().getService(getPluginConfig().getString(IBrowseService.BROWSER_ID),
                IBrowseService.class);
        if (service != null) {
            service.browse(nodeModel);
        } else {
            log.warn("No browser service found, cannot open document");
        }
    }

    private IModel<String> getDisplayName() {
        try {
            final IModel<String> model = DocumentUtils.getDocumentNameModel(getModel().getNode());
            if (model != null) {
                return model;
            }
        } catch (RepositoryException ignored) {
        }
        return Model.of(getString("unknown"));
    }

    IEditorManager getEditorManager() {
        return getPluginContext().getService(getPluginConfig().getString("editor.id"), IEditorManager.class);
    }

    private Map<String, Serializable> obtainWorkflowHints(WorkflowDescriptorModel model) {
        Map<String, Serializable> info = Collections.emptyMap();
        try {
            WorkflowDescriptor workflowDescriptor = model.getObject();
            if (workflowDescriptor != null) {
                WorkflowManager manager = obtainUserSession().getWorkflowManager();
                Workflow workflow = manager.getWorkflow(workflowDescriptor);
                info = workflow.hints();
            }
        } catch (RepositoryException | WorkflowException | RemoteException ex) {
            log.error(ex.getMessage());
        }
        return info;
    }

    private static UserSession obtainUserSession() {
        return UserSession.get();
    }

    private void updateWorkflowVisibilities(Map<String, Serializable> workflowHints) {
        editAction.setVisible(!Boolean.FALSE.equals(workflowHints.get("edit")));
        deleteAction.setVisible(!Boolean.FALSE.equals(workflowHints.get("delete")));
        renameAction.setVisible(!Boolean.FALSE.equals(workflowHints.get("rename")));
        moveAction.setVisible(!Boolean.FALSE.equals(workflowHints.get("move")));
        copyAction.setVisible(!Boolean.FALSE.equals(workflowHints.get("copy")));
        whereUsedAction.setVisible(!Boolean.FALSE.equals(workflowHints.get("status")));
    }


    /**
     * @deprecated replaced by {@link org.hippoecm.frontend.plugins.standardworkflow.RenameDocumentDialog} since version 3.2.0.
     */
    @Deprecated
    public class RenameDocumentDialog extends WorkflowDialog<Void> {

        private final IModel<String> nameModel;
        private final IModel<String> uriModel;
        private final NameUriField nameUriField;

        public RenameDocumentDialog(StdWorkflow action, IModel<String> title) {
            super(action, null, title);

            setSize(DialogConstants.MEDIUM_AUTO);

            nameModel = PropertyModel.of(action, "targetName");
            uriModel = PropertyModel.of(action, "uriName");

            final IModel<StringCodec> codecModel = getStringCodecModel();

            final String originalTargetName = nameModel.getObject();
            final String originalUriName = uriModel.getObject();
            add(nameUriField = new NameUriField("name-url", codecModel, originalUriName, originalTargetName, true));
        }

        @Override
        protected void onOk() {
            nameModel.setObject(nameUriField.getName());
            uriModel.setObject(nameUriField.getUrl());
            super.onOk();
        }
    }
}
