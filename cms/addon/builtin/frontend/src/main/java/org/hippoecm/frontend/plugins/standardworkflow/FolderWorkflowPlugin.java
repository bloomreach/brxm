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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.CustomizableDialogLink;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugins.standardworkflow.reorder.ReorderDialog;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.service.IJcrService;
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

public class FolderWorkflowPlugin extends AbstractWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    transient Logger log = LoggerFactory.getLogger(FolderWorkflowPlugin.class);

    Map<String, Set<String>> templates;
    private Label folderName;
    private DialogLink deleteLink;

    public FolderWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(folderName = new Label("foldername"));

        deleteLink = new DialogLink("delete-dialog", new Model("Delete folder"), new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService dialogService) {
                // FIXME: fixed (in code) dialog text
                String text = "Are you sure you want to delete ";
                try {
                    text += "folder ";
                    text += ((WorkflowsModel) FolderWorkflowPlugin.this.getModel()).getNodeModel().getNode().getName();
                } catch (RepositoryException ex) {
                    text += "this folder";
                }
                text += " and all of its contents permanently?";
                return new AbstractWorkflowDialog(FolderWorkflowPlugin.this, dialogService, "Delete folder", text) {
                    @Override
                    protected void execute() throws Exception {
                        // FIXME: this assumes that folders are always embedded in other folders
                        // and there is some logic here to look up the parent.  The real solution is
                        // in the visual component to merge two workflows.
                        WorkflowsModel model = (WorkflowsModel) FolderWorkflowPlugin.this.getModel();
                        Node node = model.getNodeModel().getNode();
                        WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("embedded", node.getParent());
                        workflow.delete(node.getName());
                    }
                };
            }
        }, getDialogService());
        add(deleteLink);

        final IDataProvider provider = new IDataProvider() {
            private static final long serialVersionUID = 1L;

            public IModel model(Object object) {
                return new Model((String) object);
            }

            public int size() {
                return templates != null ? templates.size() : 0;
            }

            public Iterator<String> iterator(int skip, int count) {
                return templates != null ? templates.keySet().iterator() : new TreeSet<String>().iterator();
            }

            public void detach() {
            }
        };
        add(new AbstractView("items", provider) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item item) {
                final IModel model = item.getModel();
                // final String dialogTitle = "Add " + ((String) model.getObject());
                final String dialogTitle = ((String) model.getObject());
                CustomizableDialogLink link;
                link = new CustomizableDialogLink("add-dialog", new Model(dialogTitle), new IDialogFactory() {
                    private static final long serialVersionUID = 1L;

                    public AbstractDialog createDialog(IDialogService dialogService) {
                        if (dialogTitle.contains("New Smart Folder")) // FIXME very bad check on name
                            return new FolderWorkflowExtendedDialog(FolderWorkflowPlugin.this, dialogService,
                                    ((String) model.getObject()));
                        else
                            return new FolderWorkflowDialog(FolderWorkflowPlugin.this, dialogService, ((String) model
                                    .getObject()));
                    }
                }, getDialogService());

                // FIXME: proper procedure to get an icon
                if (dialogTitle.contains("folder") || dialogTitle.contains("Folder")) {
                    link.setIcon("addfolder_ico");
                } else if (dialogTitle.contains("document") || dialogTitle.contains("Document")) {
                    link.setIcon("adddocument_ico");
                } else {
                    link.setIcon("addextended_ico");
                }

                item.add(link);
            }
        });
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        log = LoggerFactory.getLogger(FolderWorkflowPlugin.class);
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        WorkflowsModel model = (WorkflowsModel) FolderWorkflowPlugin.this.getModel();
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
                templates = folderWorkflow.list();
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
        IBrowseService browser = getPluginContext().getService(getPluginConfig().getString(IBrowseService.BROWSER_ID),
                IBrowseService.class);
        IEditService editor = getPluginContext().getService(getPluginConfig().getString(IEditService.EDITOR_ID),
                IEditService.class);
        try {
            if (nodeModel.getNode() != null
                    && (nodeModel.getNode().isNodeType(HippoNodeType.NT_DOCUMENT) || nodeModel.getNode().isNodeType(
                            HippoNodeType.NT_HANDLE))) {
                if (browser != null) {
                    browser.browse(nodeModel);
                }
                if (!nodeModel.getNode().isNodeType("hippostd:folder") && !nodeModel.getNode().isNodeType("hippostd:directory")) {
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
}
