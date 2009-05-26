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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.i18n.types.TypeChoiceRenderer;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderWorkflowPlugin extends CompatibilityWorkflowPlugin<FolderWorkflow> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(FolderWorkflowPlugin.class);

    private WorkflowAction reorderAction;

    public FolderWorkflowPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(new Label("new"));

        add(new WorkflowAction("delete", new StringResourceModel("delete-title", this, null)) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "delete-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                StringResourceModel messageModel;
                try {
                    IModel folderName = new NodeTranslator(new JcrNodeModel(
                            ((WorkflowDescriptorModel) FolderWorkflowPlugin.this.getModel()).getNode())).getNodeName();
                    messageModel = new StringResourceModel("delete-message-extended", FolderWorkflowPlugin.this, null,
                            new Object[] { folderName });
                } catch (RepositoryException ex) {
                    messageModel = new StringResourceModel("delete-message", FolderWorkflowPlugin.this, null);
                }
                return new WorkflowAction.WorkflowDialog(messageModel) {

                    @Override
                    public IModel getTitle() {
                        return new StringResourceModel("delete-title", FolderWorkflowPlugin.this, null);
                    }

                    @Override
                    public IValueMap getProperties() {
                        return SMALL;
                    }

                    @Override
                    protected void init() {
                        setFocusOnCancel();
                    }

                };
            }

            @Override
            public void execute(WorkflowDescriptorModel model) throws Exception {
                // FIXME: this assumes that folders are always embedded in other folders
                // and there is some logic here to look up the parent.  The real solution is
                // in the visual component to merge two workflows.
                Node node = model.getNode();
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("embedded", node.getParent());
                workflow.delete(node.getName() + "[" + node.getIndex() + "]");
            }
        });

        add(new WorkflowAction("rename", new StringResourceModel("rename-title", this, null)) {
            public String name;

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "rename-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                name = getInputNodeName();
                return new WorkflowAction.NameDialog(new StringResourceModel("rename-title", FolderWorkflowPlugin.this,
                        null), new StringResourceModel("rename-text", FolderWorkflowPlugin.this, null),
                        new PropertyModel(this, "name"));
            }

            @Override
            protected void execute(WorkflowDescriptorModel model) throws Exception {
                // FIXME: this assumes that folders are always embedded in other folders
                // and there is some logic here to look up the parent.  The real solution is
                // in the visual component to merge two workflows.
                Node node = model.getNode();
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("embedded", node.getParent());
                workflow.rename(node.getName() + "[" + node.getIndex() + "]", NodeNameCodec.encode(name, true));
            }
        });

        add(reorderAction = new WorkflowAction("reorder", new StringResourceModel("reorder-folder", this, null)) {
            public List<String> order = new LinkedList<String>();

            @Override
            protected Dialog createRequestDialog() {
                return new ReorderDialog(this, config, (WorkflowDescriptorModel) FolderWorkflowPlugin.this.getModel(),
                        order);
            }

            @Override
            protected void execute(WorkflowDescriptorModel model) throws Exception {
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow((WorkflowDescriptor) model.getObject());
                workflow.reorder(order);
            }
        });

        onModelChanged();
    }

    @Override
    public void onModelChanged() {
        try {
            IModel model = getModel();
            if (model instanceof WorkflowDescriptorModel) {
                WorkflowDescriptorModel descriptorModel = (WorkflowDescriptorModel) getModel();
                Node folderNode = descriptorModel.getNode();
                List<StdWorkflow> list = new LinkedList<StdWorkflow>();
                WorkflowDescriptor descriptor = (WorkflowDescriptor) model.getObject();
                WorkflowManager manager = ((UserSession) org.apache.wicket.Session.get()).getWorkflowManager();
                Workflow workflow = manager.getWorkflow(descriptor);
                FolderWorkflow folderWorkflow = (FolderWorkflow) workflow;
                Map<String, Serializable> hints = folderWorkflow.hints();

                if (hints.containsKey("reorder") && hints.get("reorder") instanceof Boolean) {
                    reorderAction.setVisible(((Boolean) hints.get("reorder")).booleanValue());
                }

                final Map<String, Set<String>> prototypes = (Map<String, Set<String>>) hints.get("prototypes");
                for (final String category : prototypes.keySet()) {
                    String categoryLabel = new StringResourceModel("add-category", this, null,
                            new Object[] { new StringResourceModel(category, this, null) }).getString();
                    list.add(new WorkflowAction("id", categoryLabel, new ResourceReference(getClass(),
                            "document-new-16.png")) {
                        public String prototype;
                        public String targetName;

                        @Override
                        protected Dialog createRequestDialog() {
                            return new FolderWorkflowDialog(this, new StringResourceModel(category, FolderWorkflowPlugin.this, null), category, prototypes
                                    .get(category));
                        }

                        @Override
                        protected String execute(FolderWorkflow wf) throws Exception {
                            FolderWorkflow workflow = wf;
                            if (prototype == null) {
                                throw new IllegalArgumentException("You need to select a type");
                            }
                            if (targetName == null || "".equals(targetName)) {
                                throw new IllegalArgumentException("You need to enter a name");
                            }
                            if (workflow != null) {
                                if (!prototypes.get(category).contains(prototype)) {
                                    log.error("unknown folder type " + prototype);
                                    return "Unknown folder type " + prototype;
                                }
                                String path = workflow.add(category, prototype, NodeNameCodec.encode(targetName, true));

                                ((UserSession) Session.get()).getJcrSession().refresh(true);

                                JcrNodeModel nodeModel = new JcrNodeModel(new JcrItemModel(path));
                                select(nodeModel);
                            } else {
                                log.error("no workflow defined on model for selected node");
                            }
                            return null;
                        }
                    });
                }

                AbstractView add;
                replace(add = new AbstractView("new", new ListDataProvider(list)) {

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

    @SuppressWarnings("unchecked")
    public void select(JcrNodeModel nodeModel) {
        IBrowseService<JcrNodeModel> browser = getPluginContext().getService(
                getPluginConfig().getString(IBrowseService.BROWSER_ID), IBrowseService.class);
        IEditorManager editor = getPluginContext().getService(getPluginConfig().getString(IEditorManager.EDITOR_ID),
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
                    if (editor != null) {
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
                                editor.openEditor(editNodeModel);
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

    public class FolderWorkflowDialog extends WorkflowAction.WorkflowDialog {
        private String category;
        private Set<String> prototypes;
        private IModel title;
        private Label typelabel;

        public FolderWorkflowDialog(WorkflowAction action, IModel title, String category, Set<String> prototypes) {
            action.super();
            this.title = title;
            this.category = category;
            this.prototypes = prototypes;

            PropertyModel nameModel = new PropertyModel(action, "targetName");
            PropertyModel prototypeModel = new PropertyModel(action, "prototype");

            TextField text = new TextField("name", nameModel);
            text.setRequired(true);
            text.setLabel(new StringResourceModel("name-label", FolderWorkflowPlugin.this, null));
            setFocus(text);
            add(text);

            add(typelabel = new Label("typelabel", new StringResourceModel("document-type", FolderWorkflowPlugin.this,
                    null)));

            if (prototypes.size() > 1) {
                final List<String> prototypesList = new LinkedList<String>(prototypes);
                DropDownChoice folderChoice;
                add(folderChoice = new DropDownChoice("prototype", prototypeModel, prototypesList,
                        new TypeChoiceRenderer(this)));
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
                text.setVisible(false);
                typelabel.setVisible(false);
            }
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
