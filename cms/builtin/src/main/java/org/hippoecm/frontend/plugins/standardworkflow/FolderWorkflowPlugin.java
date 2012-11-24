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
package org.hippoecm.frontend.plugins.standardworkflow;

import java.io.Serializable;
import java.rmi.RemoteException;
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

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.PackageResource;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.i18n.types.SortedTypeChoiceRenderer;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.plugins.standardworkflow.components.LanguageField;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
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

public class FolderWorkflowPlugin extends CompatibilityWorkflowPlugin<FolderWorkflow> {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(FolderWorkflowPlugin.class);

    private WorkflowAction reorderAction;

    public FolderWorkflowPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(new Label("new"));

        add(new WorkflowAction("rename", new StringResourceModel("rename-title", this, null)) {
            public String targetName;
            public String uriName;

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "rename-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                try {
                    uriName =  ((WorkflowDescriptorModel)getDefaultModel()).getNode().getName();
                    targetName = ((HippoNode)((WorkflowDescriptorModel)getDefaultModel()).getNode()).getLocalizedName();
                } catch(RepositoryException ex) {
                    uriName = targetName = "";
                }
                return new RenameDocumentDialog(this, new StringResourceModel("rename-title", FolderWorkflowPlugin.this, null));
            }

            @Override
            protected void execute(WorkflowDescriptorModel model) throws Exception {
                // FIXME: this assumes that folders are always embedded in other folders
                // and there is some logic here to look up the parent.  The real solution is
                // in the visual component to merge two workflows.
                HippoNode node = (HippoNode) model.getNode();
                String nodeName = getNodeNameCodec().encode(uriName);
                String localName = getLocalizeCodec().encode(targetName);
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                FolderWorkflow folderWorkflow = (FolderWorkflow) manager.getWorkflow("embedded", node.getParent());
                if (!((WorkflowDescriptorModel)getDefaultModel()).getNode().getName().equals(nodeName)) {
                    folderWorkflow.rename(node.getName() + (node.getIndex() > 1 ? "[" + node.getIndex() + "]" : ""), nodeName);
                }
                if(!node.getLocalizedName().equals(localName)) {
                    defaultWorkflow.localizeName(localName);
                }
            }
        });

        add(reorderAction = new WorkflowAction("reorder", new StringResourceModel("reorder-folder", this, null)) {
            public List<String> order = new LinkedList<String>();

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "reorder-16.png");
            }
            
            @Override
            protected Dialog createRequestDialog() {
                return new ReorderDialog(this, config, (WorkflowDescriptorModel) FolderWorkflowPlugin.this.getDefaultModel(),
                        order);
            }

            @Override
            protected void execute(WorkflowDescriptorModel model) throws Exception {
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow((WorkflowDescriptor) model.getObject());
                workflow.reorder(order);
            }
        });

        add(new WorkflowAction("delete", new StringResourceModel("delete-title", this, null)) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "delete-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                Node folderNode = null;
                try {
                    folderNode = ((WorkflowDescriptorModel) FolderWorkflowPlugin.this.getDefaultModel()).getNode();
                } catch (RepositoryException e) {
                    log.error("Unable to retrieve node from WorkflowDescriptorModel, folder delete cancelled", e);
                }

                if(folderNode != null) {
                    final IModel folderName = new NodeTranslator(new JcrNodeModel(folderNode)).getNodeName();
                    try {
                        boolean deleteAllowed = true;
                        for (NodeIterator iter = folderNode.getNodes(); iter.hasNext();) {
                            Node child = iter.nextNode();
                            NodeDefinition nd = child.getDefinition();
                            if (nd.getDeclaringNodeType().isMixin()) {
                                continue;
                            }
                            deleteAllowed = false;
                            break;
                        }
                        StringResourceModel messageModel = new StringResourceModel(deleteAllowed ? "delete-message-extended" : "delete-message-denied",
                                FolderWorkflowPlugin.this, null, new Object[]{folderName}) {

                            @Override
                            public void detach() {
                                folderName.detach();
                                super.detach();
                            }
                        };
                        return new DeleteDialog(messageModel, deleteAllowed);

                    } catch (RepositoryException e) {
                        log.error("Unable to retrieve number of child nodes from node, folder delete cancelled", e);
                    }
                }
                return new DeleteDialog(new StringResourceModel("delete-message-error", FolderWorkflowPlugin.this, null), false);
            }

            class DeleteDialog extends WorkflowAction.WorkflowDialog {

                public DeleteDialog(IModel messageModel, boolean deleteAllowed) {
                    super(messageModel);

                    if(deleteAllowed) {
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
                    return DialogConstants.SMALL;
                }
            }

            @Override
            public void execute(WorkflowDescriptorModel model) throws Exception {
                // FIXME: this assumes that folders are always embedded in other folders
                // and there is some logic here to look up the parent.  The real solution is
                // in the visual component to merge two workflows.
                Node node = model.getNode();
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("embedded", node.getParent());
                workflow.delete(node.getName() + (node.getIndex() > 1 ? "[" + node.getIndex() + "]" : ""));
            }
        });

        try {
            IModel model = getDefaultModel();
            if (model instanceof WorkflowDescriptorModel) {
                WorkflowDescriptorModel descriptorModel = (WorkflowDescriptorModel) getDefaultModel();
                List<StdWorkflow> list = new LinkedList<StdWorkflow>();
                WorkflowDescriptor descriptor = (WorkflowDescriptor) model.getObject();
                WorkflowManager manager = ((UserSession) org.apache.wicket.Session.get()).getWorkflowManager();
                Workflow workflow = manager.getWorkflow(descriptor);
                if(workflow instanceof FolderWorkflow) {
                    FolderWorkflow folderWorkflow = (FolderWorkflow) workflow;
                    Map<String, Serializable> hints = folderWorkflow.hints();
    
                    if (hints.containsKey("reorder") && hints.get("reorder") instanceof Boolean) {
                        reorderAction.setVisible(((Boolean) hints.get("reorder")).booleanValue());
                    }
    
                    final Set<String> translated = new TreeSet<String>();
                    if (getPluginConfig().containsKey("workflow.translated")) {
                        for (String translatedPrototype : getPluginConfig().getStringArray("workflow.translated")) {
                            translated.add(translatedPrototype);
                        }
                    }
    
                    final Map<String, Set<String>> prototypes = (Map<String, Set<String>>) hints.get("prototypes");
                    for (final String category : prototypes.keySet()) {
                        String categoryLabel = new StringResourceModel("add-category", this, null,
                                new Object[] { new StringResourceModel(category, this, null) }).getString();
                        ResourceReference iconResource = new ResourceReference(getClass(), category + "-16.png");
                        iconResource.bind(getApplication());
                        if (iconResource.getResource() == null ||
                            (iconResource.getResource() instanceof PackageResource && ((PackageResource)iconResource.getResource()).getResourceStream(false) == null)) {
                            iconResource = new ResourceReference(getClass(), "new-document-16.png");
                            iconResource.bind(getApplication());
                        }
                        list.add(new WorkflowAction("id", categoryLabel, iconResource) {
                            public String prototype;
                            public String targetName;
                            public String uriName;
                            public String language;
    
                            @Override
                            protected Dialog createRequestDialog() {
                                return new AddDocumentDialog(this, new StringResourceModel(category, FolderWorkflowPlugin.this, null),
                                        category, prototypes.get(category), translated.contains(category));
                            }
    
                            @Override
                            protected String execute(FolderWorkflow workflow) throws Exception {
                                if (prototype == null) {
                                    throw new IllegalArgumentException("You need to select a type");
                                }
                                if (targetName == null || "".equals(targetName)) {
                                    throw new IllegalArgumentException("You need to enter a name");
                                }
                                if (uriName == null || "".equals(uriName)) {
                                    throw new IllegalArgumentException("You need to enter a URL name");
                                }
                                if (workflow != null) {
                                    if (!prototypes.get(category).contains(prototype)) {
                                        log.error("unknown folder type " + prototype);
                                        return "Unknown folder type " + prototype;
                                    }
                                    String nodeName = getNodeNameCodec().encode(uriName);
                                    String localName = getLocalizeCodec().encode(targetName);
                                    if ("".equals(nodeName)) {
                                        throw new IllegalArgumentException("You need to enter a name");
                                    }
    
                                    TreeMap<String, String> arguments = new TreeMap<String, String>();
                                    arguments.put("name", nodeName);
                                    if (language != null) {
                                        arguments.put("hippotranslation:locale", language);
                                    }

                                    String path = workflow.add(category, prototype, arguments);
                                    ((UserSession) Session.get()).getJcrSession().refresh(true);
                                    JcrNodeModel nodeModel = new JcrNodeModel(new JcrItemModel(path));
                                    select(nodeModel);
                                    if(!nodeName.equals(localName)) {
                                        WorkflowManager workflowMgr = ((UserSession) org.apache.wicket.Session.get()).getWorkflowManager();
                                        DefaultWorkflow defaultWorkflow = (DefaultWorkflow) workflowMgr.getWorkflow("core", nodeModel.getNode());
                                        defaultWorkflow.localizeName(localName);
                                    }
                                } else {
                                    log.error("no workflow defined on model for selected node");
                                }
                                return null;
                            }
                        });
                    }
                }

                AbstractView add;
                replace(add = new AbstractView("new", createListDataProvider(list)) {

                    @Override
                    protected void populateItem(Item item) {
                        item.add((StdWorkflow) item.getModelObject());
                    }
                });
                add.populate();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage());
        } catch (WorkflowException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage());
        } catch (RemoteException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    protected IDataProvider createListDataProvider(List<StdWorkflow> list) {
        return new ListDataProvider(list);
    }

    @SuppressWarnings("unchecked")
    public void select(JcrNodeModel nodeModel) {
        IBrowseService<JcrNodeModel> browser = getPluginContext().getService(
                getPluginConfig().getString(IBrowseService.BROWSER_ID), IBrowseService.class);
        IEditorManager editorMgr = getPluginContext().getService(getPluginConfig().getString(IEditorManager.EDITOR_ID),
                IEditorManager.class);
        try {
            if (nodeModel.getNode() != null
                    && (nodeModel.getNode().isNodeType(HippoNodeType.NT_DOCUMENT) || nodeModel.getNode().isNodeType(
                            HippoNodeType.NT_HANDLE))) {
                if (browser != null) {
                    browser.browse(nodeModel);
                }
                if (!nodeModel.getNode().isNodeType("hippostd:folder")
                        && !nodeModel.getNode().isNodeType("hippostd:directory")) {
                    if (editorMgr != null) {
                        JcrNodeModel editNodeModel = nodeModel;
                        Node editNodeModelNode = nodeModel.getNode();
                        if (editNodeModelNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                            editNodeModelNode = editNodeModelNode.getNode(editNodeModelNode.getName());
                        }
                        javax.jcr.Session session = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
                        WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace())
                                .getWorkflowManager();
                        Workflow workflow = workflowManager.getWorkflow("editing", editNodeModelNode);
                        try {
                            if (workflow instanceof EditableWorkflow) {
                                EditableWorkflow editableWorkflow = (EditableWorkflow) workflow;
                                Document editableDocument = editableWorkflow.obtainEditableInstance();
                                if (editableDocument != null) {
                                    session.refresh(true);
                                    editNodeModel = new JcrNodeModel(session.getNodeByUUID(editableDocument
                                            .getIdentity()));
                                } else {
                                    editNodeModel = null;
                                }
                            }
                            if (editNodeModel != null) {
                                IEditor editor = editorMgr.getEditor(editNodeModel);
                                if (editor == null) {
                                    editorMgr.openEditor(editNodeModel);
                                }
                            }
                        } catch (WorkflowException ex) {
                            log.error("Cannot auto-edit document", ex);
                        } catch (RemoteException ex) {
                            log.error("Cannot auto-edit document", ex);
                        } catch (RepositoryException ex) {
                            log.error("Cannot auto-edit document", ex);
                        } catch (ServiceException ex) {
                            log.error("Cannot auto-edit document", ex);
                        }
                    }
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

    public class AddDocumentDialog extends WorkflowAction.WorkflowDialog {
        private String category;
        private Set<String> prototypes;
        private IModel title;
        private Label typelabel;
        private TextField nameComponent;
        private TextField uriComponent;
        private boolean uriModified = false;
        private LanguageField languageField;

        public AddDocumentDialog(WorkflowAction action, IModel title, String category, Set<String> prototypes, boolean translated) {
            action.super();
            this.title = title;
            this.category = category;
            this.prototypes = prototypes;

            final PropertyModel<String> nameModel = new PropertyModel<String>(action, "targetName");
            final PropertyModel<String> uriModel = new PropertyModel<String>(action, "uriName");
            final PropertyModel<String> prototypeModel = new PropertyModel<String>(action, "prototype");

            nameComponent = new TextField<String>("name", new IModel<String>() {
                private static final long serialVersionUID = 1L;

                public String getObject() {
                    return nameModel.getObject();
                }

                public void setObject(String object) {
                    nameModel.setObject(object);
                    if (!uriModified) {
                        uriModel.setObject(getNodeNameCodec().encode(nameModel.getObject()));
                    }
                }

                public void detach() {
                    nameModel.detach();
                }
                
            });
            nameComponent.setRequired(true);
            nameComponent.setLabel(new StringResourceModel("name-label", this, null));
            AjaxEventBehavior behavior;
            nameComponent.add(behavior = new OnChangeAjaxBehavior() {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    if (!uriModified) {
                        target.addComponent(uriComponent);
                    }
                }
            });
            behavior.setThrottleDelay(Duration.milliseconds(500));
            nameComponent.setOutputMarkupId(true);
            setFocus(nameComponent);
            add(nameComponent);

            add(typelabel = new Label("typelabel", new StringResourceModel("document-type", FolderWorkflowPlugin.this,
                    null)));

            if (prototypes.size() > 1) {
                final List<String> prototypesList = new LinkedList<String>(prototypes);
                final DropDownChoice folderChoice;
                SortedTypeChoiceRenderer typeChoiceRenderer = new SortedTypeChoiceRenderer(this, prototypesList);
                add(folderChoice = new DropDownChoice("prototype", prototypeModel, typeChoiceRenderer, typeChoiceRenderer) {
                    protected boolean wantOnSelectionChangedNotifications() {
                        return false;
                    }
                });
                folderChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        target.addComponent(folderChoice);
                    }
                });
                folderChoice.setNullValid(false);
                folderChoice.setRequired(true);
                folderChoice.setLabel(new StringResourceModel("document-type", FolderWorkflowPlugin.this, null));
                // while not a prototype chosen, disable ok button
                Component notypes;
                add(notypes = new EmptyPanel("notypes"));
                notypes.setVisible(false);
            } else if (prototypes.size() == 1) {
                Component component;
                add(component = new EmptyPanel("prototype"));
                component.setVisible(false);
                prototypeModel.setObject(prototypes.iterator().next());
                Component notypes;
                add(notypes = new EmptyPanel("notypes"));
                notypes.setVisible(false);
                typelabel.setVisible(false);
            } else {
                // if the folderWorkflowPlugin.templates.get(category).size() = 0 you cannot add this
                // category currently.
                Component component;
                add(component = new EmptyPanel("prototype"));
                component.setVisible(false);
                prototypeModel.setObject(null);
                add(new Label("notypes", "There are no types available for : [" + category
                        + "] First create document types please."));
                nameComponent.setVisible(false);
                typelabel.setVisible(false);
            }

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
            uriComponent.setRequired(true);
            uriComponent.setLabel(new StringResourceModel("url-label", this, null));
            uriComponent.setOutputMarkupId(true);
            
            AjaxLink<Boolean> uriAction = new AjaxLink<Boolean>("uriAction") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    uriModified = !uriModified;
                    if (!uriModified) {
                        uriModel.setObject(Strings.isEmpty(nameModel.getObject()) ? "" : getNodeNameCodec().encode(nameModel.getObject()));
                        uriComponent.modelChanged();
                    } else {
                        target.focusComponent(uriComponent);
                    }
                    target.addComponent(AddDocumentDialog.this);
                }
            };
            uriAction.add(new Label("uriActionLabel", new AbstractReadOnlyModel<String>() {
                @Override
                public String getObject() {
                    return uriModified ? getString("url-reset") : getString("url-edit");
                }
            }));
            add(uriAction);

            languageField = new LanguageField("language", new PropertyModel<String>(action, "language"), getLocaleProvider());
            if (!translated) {
                languageField.setVisible(false);
            } else {
                WorkflowDescriptorModel descriptorModel = (WorkflowDescriptorModel) FolderWorkflowPlugin.this.getDefaultModel();
                try {
                    Node node = descriptorModel.getNode();
                    if (node != null) {
                        while (node.getDepth() > 0) {
                            if (node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                                languageField.setVisible(false);
                                break;
                            }
                            node = node.getParent();
                        }
                    }
                } catch (RepositoryException e) {
                    log.error("Could not determine visibility of language field");
                }
            }
            add(languageField);
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
            nameComponent.setLabel(new StringResourceModel("name-label", FolderWorkflowPlugin.this, null));
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

            uriComponent = new TextField<String>("uriinput", uriModel) {
                @Override
                public boolean isEnabled() {
                    return uriModified;
                }
            };

            uriComponent.setLabel(new StringResourceModel("url-label", FolderWorkflowPlugin.this, null));
            add(uriComponent);

            uriComponent.add(new CssClassAppender(new AbstractReadOnlyModel<String>() {
                @Override
                public String getObject() {
                    return uriModified ? "grayedin" : "grayedout";
                }
            }));
            uriComponent.setOutputMarkupId(true);
            uriComponent.setRequired(true);

            AjaxLink<Boolean> uriAction = new AjaxLink<Boolean>("uriAction") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    uriModified = !uriModified;
                    if (!uriModified) {
                        uriModel.setObject(Strings.isEmpty(nameModel.getObject()) ? "" : getNodeNameCodec().encode(
                                nameModel.getObject()));
                        uriComponent.modelChanged();
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
