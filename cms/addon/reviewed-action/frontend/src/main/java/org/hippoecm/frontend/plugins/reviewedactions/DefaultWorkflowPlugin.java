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
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(BasicReviewedActionsWorkflowPlugin.class);

    private IModel caption = new StringResourceModel("unknown", this, null);

    private WorkflowAction editAction;
    private WorkflowAction deleteAction;
    private WorkflowAction renameAction;
    private WorkflowAction copyAction;
    private WorkflowAction moveAction;

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

        add(deleteAction = new WorkflowAction("delete",
                new StringResourceModel("delete-label", this, null).getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "delete-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                return new WorkflowAction.WorkflowDialog(new StringResourceModel("delete-text",
                        DefaultWorkflowPlugin.this, null, new Object[] { caption })) {

                    @Override
                    public IModel getTitle() {
                        return new StringResourceModel("delete-title", DefaultWorkflowPlugin.this, null);
                    }

                    @Override
                    protected void init() {
                        setFocusOnCancel();
                    }

                    @Override
                    public IValueMap getProperties() {
                        return SMALL;
                    }
                };
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                ((DefaultWorkflow) wf).delete();
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
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                if (!((WorkflowDescriptorModel)getDefaultModel()).getNode().getName().equals(nodeName)) {
                    ((DefaultWorkflow) wf).rename(nodeName);
                }
                if (!node.getLocalizedName().equals(localName)) {
                    defaultWorkflow.localizeName(localName);
                }
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
                try {
                    name = ((HippoNode) ((WorkflowDescriptorModel) getDefaultModel()).getNode()).getLocalizedName();
                } catch (RepositoryException ex) {
                    name = "";
                }
                return new WorkflowAction.DestinationDialog(new StringResourceModel("move-title",
                        DefaultWorkflowPlugin.this, null), new StringResourceModel("move-text",
                        DefaultWorkflowPlugin.this, null), new PropertyModel(this, "name"), destination);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                if (name == null || name.trim().equals("")) {
                    throw new WorkflowException("No name for destination given");
                }
                DefaultWorkflow workflow = (DefaultWorkflow) wf;
                workflow.move(new Document(destination.getNodeModel().getNode().getUUID()), NodeNameCodec.encode(name,
                        true));
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
                name = "";
                return new WorkflowAction.DestinationDialog(new StringResourceModel("copy-title",
                        DefaultWorkflowPlugin.this, null), new StringResourceModel("copy-text",
                        DefaultWorkflowPlugin.this, null), new PropertyModel(this, "name"), destination);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                if (name == null || name.trim().equals("")) {
                    throw new WorkflowException("No name for destination given");
                }
                DefaultWorkflow workflow = (DefaultWorkflow) wf;
                workflow.copy(new Document(destination.getNodeModel().getNode().getUUID()), NodeNameCodec.encode(name,
                        true));
                return null;
            }
        });
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

    @Override
    public void onModelChanged() {
        try {
            super.onModelChanged();
            WorkflowDescriptorModel model = (WorkflowDescriptorModel) getDefaultModel();
            if (model != null) {
                Node documentNode = model.getNode();
                if (documentNode != null) {
                    caption = new NodeTranslator(new JcrNodeModel(documentNode)).getNodeName();
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
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
            return MEDIUM;
        }
    }
}
