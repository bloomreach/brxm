/*
 *  Copyright 2008-2012 Hippo.
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.editor.workflow.dialog.DeleteDialog;
import org.hippoecm.frontend.editor.workflow.dialog.WhereUsedDialog;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
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
import static org.apache.commons.io.FilenameUtils.isExtension;

public class DefaultWorkflowPlugin extends CompatibilityWorkflowPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(DefaultWorkflowPlugin.class);

    private IModel caption = new StringResourceModel("unknown", this, null);

    private StdWorkflow editAction;
    private StdWorkflow deleteAction;
    private StdWorkflow renameAction;
    private StdWorkflow copyAction;
    private StdWorkflow moveAction;
    private StdWorkflow whereUsedAction;

    private static final List<String> KNOWN_IMAGE_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "gif", "png"
    );
    private static final String NUMBER_EXPRESSION = "[0-9]*";

    public DefaultWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow("info", "info") {
            @Override
            protected IModel getTitle() {
                return caption;
            }

            @Override
            protected void invoke() {
            }
        });

        onModelChanged();

        add(editAction = new WorkflowAction("edit", new StringResourceModel("edit", this, null).getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "edit-16.png");
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                Node docNode = ((WorkflowDescriptorModel) DefaultWorkflowPlugin.this.getDefaultModel()).getNode();
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

        add(renameAction = new WorkflowAction("rename", new StringResourceModel("rename-label", this, null)) {
            public String targetName;
            public String uriName;

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "rename-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                try {
                    uriName = ((WorkflowDescriptorModel) getDefaultModel()).getNode().getName();
                    targetName = ((HippoNode) ((WorkflowDescriptorModel) getDefaultModel()).getNode())
                            .getLocalizedName();
                } catch (RepositoryException ex) {
                    uriName = targetName = "";
                }
                return new RenameDocumentDialog(this, new StringResourceModel("rename-title",
                        DefaultWorkflowPlugin.this, null));
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                if (targetName == null || targetName.trim().equals("")) {
                    throw new WorkflowException("No name for destination given");
                }
                HippoNode node = (HippoNode) ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                String nodeName = getNodeNameCodec().encode(uriName);
                String localName = getLocalizeCodec().encode(targetName);
                if ("".equals(nodeName)) {
                    throw new IllegalArgumentException("You need to enter a name");
                }
                WorkflowManager manager = obtainUserSession().getWorkflowManager();
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                if (!((WorkflowDescriptorModel) getDefaultModel()).getNode().getName().equals(nodeName)) {
                    ((DefaultWorkflow) wf).rename(nodeName);
                }
                if (!node.getLocalizedName().equals(localName)) {
                    defaultWorkflow.localizeName(localName);
                }
                return null;
            }
        });

        add(copyAction = new WorkflowAction("copy", new StringResourceModel("copy-label", this, null)) {
            NodeModelWrapper destination = null;
            String name = null;

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "copy-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                destination = new NodeModelWrapper(getFolder()) {
                };
                try {
                    HippoNode node = (HippoNode) ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                    String nodeName = node.getLocalizedName().toLowerCase();

                    if (isExtension(nodeName, KNOWN_IMAGE_EXTENSIONS)) {
                        createNewNodeNameForImage(nodeName);
                    } else {
                        String copyof = new StringResourceModel("copyof", DefaultWorkflowPlugin.this, null).getString();
                        CopyNameHelper copyNameHelper = new CopyNameHelper(getNodeNameCodec(), copyof);
                        name = copyNameHelper.getCopyName(nodeName, destination.getNodeModel().getNode());
                    }
                } catch (RepositoryException ex) {
                    return new ExceptionDialog(ex);
                }
                return new WorkflowAction.DestinationDialog(
                        new StringResourceModel("copy-title", DefaultWorkflowPlugin.this, null),
                        new StringResourceModel("copy-name", DefaultWorkflowPlugin.this, null),
                        new PropertyModel(this, "name"), destination) {
                    {
                        setOkEnabled(true);
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
            private void createNewNodeNameForImage(final String nodeName) throws RepositoryException {
                name = nodeName;
                Node gallery = destination.getNodeModel().getNode();
                if (gallery.hasNode(name)) {
                    name = addOrIncrementNodeNameSuffixForImage(name);
                    createNewNodeNameForImage(name);
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
            private String addOrIncrementNodeNameSuffixForImage(final String nodeName) {
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
                JcrNodeModel folderModel = new JcrNodeModel("/");
                if (destination != null) {
                    folderModel = destination.getNodeModel();
                }
                StringCodec codec = getNodeNameCodec();
                String nodeName = codec.encode(name);

                DefaultWorkflow workflow = (DefaultWorkflow) wf;
                workflow.copy(new Document(folderModel.getNode().getUUID()), nodeName);
                JcrNodeModel copyMode = new JcrNodeModel(folderModel.getItemModel().getPath() + "/" + nodeName);
                HippoNode node = (HippoNode) copyMode.getNode().getNode(nodeName);

                String localName = getLocalizeCodec().encode(name);
                if (!node.getLocalizedName().equals(localName)) {
                    WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                    DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                    defaultWorkflow.localizeName(localName);
                }
                browseTo(copyMode);
                return null;
            }
        });

        add(moveAction = new WorkflowAction("move", new StringResourceModel("move-label", this, null)) {
            public NodeModelWrapper destination = null;

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "move-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                destination = new NodeModelWrapper(getFolder()) {
                };
                return new WorkflowAction.DestinationDialog(new StringResourceModel("move-title",
                        DefaultWorkflowPlugin.this, null), null, null, destination);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                JcrNodeModel folderModel = new JcrNodeModel("/");
                if (destination != null) {
                    folderModel = destination.getNodeModel();
                }
                String nodeName = ((WorkflowDescriptorModel) getDefaultModel()).getNode().getName();
                DefaultWorkflow workflow = (DefaultWorkflow) wf;
                workflow.move(new Document(folderModel.getNode().getUUID()), nodeName);
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
                final IModel<String> docName = getDocumentName();
                IModel<String> message = new StringResourceModel("delete-message", DefaultWorkflowPlugin.this, null,
                        new Object[]{docName});
                IModel<String> title = new StringResourceModel("delete-title", DefaultWorkflowPlugin.this, null,
                        new Object[]{docName});
                return new DeleteDialog(title, message, this, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                ((DefaultWorkflow) wf).delete();
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

        WorkflowDescriptorModel model = (WorkflowDescriptorModel) getDefaultModel();
        if (model != null) {
            Map<String, Serializable> info = obtainWorkflowHints(model);
            if (info != null) {
                updateWorkflowVisibilities(info);
            }
        }
    }

    private JcrNodeModel getFolder() {
        JcrNodeModel folderModel = new JcrNodeModel("/");
        try {
            WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
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

    private void browseTo(JcrNodeModel nodeModel) throws RepositoryException {
        //refresh session before IBrowseService.browse is called
        obtainUserSession().getJcrSession().refresh(false);

        IBrowseService service = getPluginContext().getService(getPluginConfig().getString(IBrowseService.BROWSER_ID),
                IBrowseService.class);
        if (service != null) {
            service.browse(nodeModel);
        } else {
            log.warn("No browser service found, cannot open document");
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

    IEditorManager getEditorManager() {
        return getPluginContext().getService(getPluginConfig().getString("editor.id"), IEditorManager.class);
    }

    private Map<String, Serializable> obtainWorkflowHints(WorkflowDescriptorModel model) {
        Map<String, Serializable> info = null;
        try {
            Node documentNode = model.getNode();
            if (documentNode != null) {
                caption = new NodeTranslator(new JcrNodeModel(documentNode)).getNodeName();
            }
            WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) model.getObject();
            if (workflowDescriptor != null) {
                WorkflowManager manager = obtainUserSession().getWorkflowManager();
                Workflow workflow = manager.getWorkflow(workflowDescriptor);
                info = workflow.hints();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        } catch (RemoteException e) {
            log.error(e.getMessage());
        } catch (WorkflowException e) {
            log.error(e.getMessage());
        }
        return info;
    }

    private static UserSession obtainUserSession() {
        return ((UserSession) Session.get());
    }

    private void updateWorkflowVisibilities(Map<String, Serializable> workflowHints) {
        editAction.setVisible(Boolean.TRUE.equals(workflowHints.get("edit")));
        deleteAction.setVisible(Boolean.TRUE.equals(workflowHints.get("delete")));
        renameAction.setVisible(Boolean.TRUE.equals(workflowHints.get("rename")));
        moveAction.setVisible(Boolean.TRUE.equals(workflowHints.get("move")));
        copyAction.setVisible(Boolean.TRUE.equals(workflowHints.get("copy")));
        whereUsedAction.setVisible(Boolean.TRUE.equals(workflowHints.get("status")));
    }

    public class RenameDocumentDialog extends WorkflowAction.WorkflowDialog {
        private IModel title;
        private TextField nameComponent;
        private TextField uriComponent;
        private boolean uriModified;

        public RenameDocumentDialog(WorkflowAction action, IModel title) {
            action.super();
            this.title = title;

            final PropertyModel<String> nameModel = new PropertyModel<String>(action, "targetName");
            final PropertyModel<String> uriModel = new PropertyModel<String>(action, "uriName");

            String s1 = nameModel.getObject();
            String s2 = uriModel.getObject();
            uriModified = !s1.equals(s2);

            nameComponent = new TextField<String>("name", nameModel);
            nameComponent.setRequired(true);
            nameComponent.setLabel(new StringResourceModel("name-label", DefaultWorkflowPlugin.this, null));
            nameComponent.add(new OnChangeAjaxBehavior() {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    if (!uriModified) {
                        uriModel.setObject(getNodeNameCodec().encode(nameModel.getObject()));
                        target.addComponent(uriComponent);
                    }
                }
            }.setThrottleDelay(Duration.milliseconds(500)));
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
                    target.addComponent(RenameDocumentDialog.this);
                }
            };
            uriAction.add(new Label("uriActionLabel", new AbstractReadOnlyModel<String>() {
                @Override
                public String getObject() {
                    return uriModified ? getString("url-reset") : getString("url-edit");
                }
            }));
            add(uriAction);
        }

        @Override
        public IModel getTitle() {
            return title;
        }

        @Override
        public IValueMap getProperties() {
            return DialogConstants.MEDIUM;
        }
    }
}
