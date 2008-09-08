/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.standardworkflow;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.Session;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.DialogAction;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NewAbstractFolderWorkflowPlugin extends AbstractWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    transient Logger log = LoggerFactory.getLogger(NewAbstractFolderWorkflowPlugin.class);

    protected static final String WORKFLOW_ACTION_LINK_ID = "workflow-action-dialog-link";
    protected static final String DIALOG_LINKS_COMPONENT_ID = "items";
    
    List<WorkflowActionComponent> staticTemplates;
    protected Map<String, WorkflowActionComponent> templates;

    private Label folderName;

    public NewAbstractFolderWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        templates = new LinkedHashMap<String, WorkflowActionComponent>();
        add(folderName = new Label("foldername"));
        
        DialogAction action = new DialogAction(new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService dialogService) {
                // FIXME: fixed (in code) dialog text
                String text = "Are you sure you want to delete ";
                try {
                    text += "folder ";
                    text += ((WorkflowsModel) NewAbstractFolderWorkflowPlugin.this.getModel()).getNodeModel().getNode().getName();
                } catch (RepositoryException ex) {
                    text += "this folder";
                }
                text += " and all of its contents permanently?";
                return new AbstractWorkflowDialog(NewAbstractFolderWorkflowPlugin.this, dialogService, "Delete folder", text) {
                    @Override
                    protected void execute() throws Exception {
                        // FIXME: this assumes that folders are always embedded in other folders
                        // and there is some logic here to look up the parent.  The real solution is
                        // in the visual component to merge two workflows.
                        WorkflowsModel model = (WorkflowsModel) NewAbstractFolderWorkflowPlugin.this.getModel();
                        Node node = model.getNodeModel().getNode();
                        WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("embedded", node.getParent());
                        workflow.delete(node.getName());
                    }
                };
            }
        }, getDialogService());
        
        addWorkflowAction("Delete Folder", "editmodel_ico", null, action);
        add(createDialogLinksComponent());
    }
    
    /**
     * Create the component that acts as a launcher for the workflow-action-components.
     * @return  new component that list the workflow-action-components
     */
    protected Component createDialogLinksComponent() {
        return new AbstractView(DIALOG_LINKS_COMPONENT_ID, getTemplateProvider()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item item) {
                final WorkflowActionComponent wac = (WorkflowActionComponent) item.getModel().getObject();
                item.add(createDialogActionComponent(wac));
            }
        };
    }
    
    /**
     * Create a new component that will trigger 
     * @param component
     * @return
     */
    protected Component createDialogActionComponent(WorkflowActionComponent component) {
        DialogLink link = new DialogLink(component.getId(), new Model(component.label), component.action);
        if (component.icon != null) {
            link.add(new AttributeAppender("class", new Model(component.icon), " "));
        }
        link.setEnabled(component.action.isEnabled());
        return link;
    }

    protected void addWorkflowAction(String label, String icon, Set<String> prototypes, DialogAction action) {
        getStaticTemplates().add(new WorkflowActionComponent(WORKFLOW_ACTION_LINK_ID, label, icon, prototypes, action));
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        log = LoggerFactory.getLogger(NewAbstractFolderWorkflowPlugin.class);
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        WorkflowsModel model = (WorkflowsModel) NewAbstractFolderWorkflowPlugin.this.getModel();
        WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
        try {
            if (model.getNodeModel() != null) {
                if (model.getNodeModel().getNode() != null) {
                    folderName.setModel(new Model((model.getNodeModel().getNode()).getDisplayName()));
                }
            }
        } catch (RepositoryException ex) {
        }
        try {
            Workflow workflow = manager.getWorkflow(model.getWorkflowDescriptor());
            if (workflow instanceof FolderWorkflow) {
                FolderWorkflow folderWorkflow = (FolderWorkflow) workflow;
                initializeTemplates(folderWorkflow.list());
            }
        } catch (MappingException ex) {
        } catch (WorkflowException ex) {
        } catch (RepositoryException ex) {
        } catch (RemoteException ex) {
        }
        redraw();
    }

    @SuppressWarnings("unchecked")
    public void select(JcrNodeModel nodeModel) {
        IBrowseService<JcrNodeModel> browser = getPluginContext().getService(
                getPluginConfig().getString(IBrowseService.BROWSER_ID), IBrowseService.class);
        IEditService<JcrNodeModel> editor = getPluginContext().getService(
                getPluginConfig().getString(IEditService.EDITOR_ID), IEditService.class);
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
                        WorkflowManager workflowManager = ((UserSession) Session.get()).getWorkflowManager();
                        Workflow workflow = workflowManager.getWorkflow("editing", editNodeModelNode);
                        try {
                            if (workflow instanceof EditableWorkflow) {
                                EditableWorkflow editableWorkflow = (EditableWorkflow) workflow;
                                Document editableDocument = editableWorkflow.obtainEditableInstance();
                                if (editableDocument != null) {
                                    editNodeModel = new JcrNodeModel(((UserSession) Session.get()).getJcrSession()
                                            .getNodeByUUID(editableDocument.getIdentity()));
                                } else {
                                    editNodeModel = null;
                                }
                            }
                            if (editNodeModel != null) {
                                editor.edit(editNodeModel);
                            }
                        } catch (WorkflowException ex) {
                            log.error("Cannot auto-edit document", ex);
                        } catch (RemoteException ex) {
                            log.error("Cannot auto-edit document", ex);
                        } catch (RepositoryException ex) {
                            log.error("Cannot auto-edit document", ex);
                        }
                    }
                }
            }
        } catch (RepositoryException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
    
    private void initializeTemplates(Map<String, Set<String>> list) {
        templates.clear();
        if (list != null && list.size() > 0) {
            for (Entry<String, Set<String>> entry : list.entrySet()) {
                templates.put(entry.getKey(), createWorkflowActionComponent(entry.getKey(), entry.getValue()));
            }
        }
        if (staticTemplates != null && staticTemplates.size() > 0) {
            for (WorkflowActionComponent c : staticTemplates) {
                templates.put(c.label, c);
            }
        }
    }
    
    private List<WorkflowActionComponent> getStaticTemplates() {
        if (staticTemplates == null) {
            staticTemplates = new LinkedList<WorkflowActionComponent>();
        }
        return staticTemplates;
    }
    
    protected IDataProvider getTemplateProvider() {
        return new IDataProvider() {
            private static final long serialVersionUID = 1L;

            public IModel model(Object object) {
                return new Model((WorkflowActionComponent) object);
            }

            public int size() {
                return templates != null ? templates.size() : 0;
            }

            public Iterator<WorkflowActionComponent> iterator(int skip, int count) {
                return templates != null ? templates.values().iterator() : new TreeSet<WorkflowActionComponent>()
                        .iterator();
            }

            public void detach() {
            }
        };
    }
    
    protected WorkflowActionComponent createWorkflowActionComponent(final String category, final Set<String> prototypes) {
        DialogAction action = new DialogAction(new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService dialogService) {
                if (category.contains("New Smart Folder")) // FIXME very bad check on name
                    return new NewFolderWorkflowExtendedDialog(NewAbstractFolderWorkflowPlugin.this, dialogService, category);
                else
                    return new NewFolderWorkflowDialog(NewAbstractFolderWorkflowPlugin.this, dialogService, category);
            }
        }, getDialogService());
        
        // FIXME: proper procedure to get an icon
        String icon = null;
        if (category.toLowerCase().contains("folder")) {
            icon = "addfolder_ico";
        } else if (category.toLowerCase().contains("document")) {
            icon = "adddocument_ico";
        } else {
            icon = "addextended_ico";
        }
        return new WorkflowActionComponent(WORKFLOW_ACTION_LINK_ID, category, icon, prototypes, action);
    }


    public class WorkflowActionComponent implements IClusterable {
        private static final long serialVersionUID = 1L;
        
        private String id;
        private String label;
        private String icon;
        private DialogAction action;
        private Set<String> prototypes;

        public WorkflowActionComponent(String id, String label, String icon, Set<String> prototypes, DialogAction action) {
            this.id = id;
            this.label = label;
            this.icon = icon;
            this.prototypes = prototypes != null ? prototypes : Collections.EMPTY_SET;
            this.action = action;
        }

        public String getId() {
            return id;
        }
        
        public String getLabel() {
            return label;
        }

        public String getIcon() {
            return icon;
        }

        public Set<String> getPrototypes() {
            return prototypes;
        }
        
        public DialogAction getAction() {
            return action;
        }
        
    }
}
