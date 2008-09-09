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
import java.lang.String;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map;
import java.util.Set;

import java.util.TreeMap;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.NamespaceFriendlyChoiceRenderer;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;

public class FolderShortcutPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    transient Logger log = LoggerFactory.getLogger(FolderShortcutPlugin.class);

    private String defaultDropLocation = "/content";
    private String optionSelectOnly = null;
    private boolean optionSelectFirst = false;

    public FolderShortcutPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        AjaxLink link = new AjaxLink("link") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                IDialogService dialogService = getDialogService();
                dialogService.show(FolderShortcutPlugin.this.new Dialog(dialogService));
            }
        };
        add(link);

        if(config.containsKey("option.first"))
            optionSelectFirst = config.getBoolean("option.first");
        if(config.containsKey("option.only"))
            optionSelectOnly = config.getString("option.only");
        
        String path = config.getString("gallery.path");
        if (path != null) {
            defaultDropLocation = path;
            try {
                while (defaultDropLocation.startsWith("/")) {
                    defaultDropLocation = defaultDropLocation.substring(1);
                }
                Session session = ((UserSession)org.apache.wicket.Session.get()).getJcrSession();
                setModel(new JcrNodeModel(session.getRootNode().getNode(defaultDropLocation)));
                // HREPTWO-1218 getModel returns null, which causes problems for the WizardDialog
            } catch (PathNotFoundException ex) {
                log.warn("No default drop location present");
                defaultDropLocation = null; // force adding empty panel
            } catch (RepositoryException ex) {
                log.warn("Error while accessing default drop location");
                defaultDropLocation = null; // force adding empty panel
            }
        }

        if (defaultDropLocation == null) {
            link.setVisible(false);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        log = LoggerFactory.getLogger(FolderShortcutPlugin.class);
    }

    // FIXME: pure duplication of logic in FolderWorkflowPlugin
    @SuppressWarnings("unchecked")
    public void select(JcrNodeModel nodeModel) {
        IBrowseService browser = getPluginContext().getService(getPluginConfig().getString(IBrowseService.BROWSER_ID),
                                                               IBrowseService.class);
        IEditService editor = getPluginContext().getService(getPluginConfig().getString(IEditService.EDITOR_ID),
                                                            IEditService.class);
        try {
            if (nodeModel.getNode() != null && (nodeModel.getNode().isNodeType(HippoNodeType.NT_DOCUMENT) ||
                                                nodeModel.getNode().isNodeType(HippoNodeType.NT_HANDLE))) {
                if (browser != null) {
                    browser.browse(nodeModel);
                }
                if (!nodeModel.getNode().isNodeType("hippostd:folder") &&
                    !nodeModel.getNode().isNodeType("hippostd:directory")) {
                    if (editor != null) {
                        JcrNodeModel editNodeModel = nodeModel;
                        Node editNodeModelNode = nodeModel.getNode();
                        if (editNodeModelNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                            editNodeModelNode = editNodeModelNode.getNode(editNodeModelNode.getName());
                        }
                        WorkflowManager workflowManager = ((UserSession)org.apache.wicket.Session.get()).getWorkflowManager();
                        Workflow workflow = workflowManager.getWorkflow("editing", editNodeModelNode);
                        try {
                            if (workflow instanceof EditableWorkflow) {
                                EditableWorkflow editableWorkflow = (EditableWorkflow)workflow;
                                Document editableDocument = editableWorkflow.obtainEditableInstance();
                                if (editableDocument != null) {
                                    Session jcrSession = ((UserSession)org.apache.wicket.Session.get()).getJcrSession();
                                    editNodeModel = new JcrNodeModel(jcrSession.getNodeByUUID(editableDocument.getIdentity()));
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

    public class Dialog extends AbstractDialog {
        @SuppressWarnings("unused")
        private final static String SVN_ID = "$Id$";

        private static final long serialVersionUID = 1L;

        private String templateCategory = null;
        private String prototype = null;
        private String name;
        private IServiceReference<IJcrService> jcrServiceRef;
        private Map<String, Set<String>> templates;
        final DropDownChoice folderChoice;
        final DropDownChoice categoryChoice;

        public Dialog(IDialogService dialogWindow) {
            super(FolderShortcutPlugin.this.getPluginContext(), dialogWindow);
            ok.setModel(new Model("Create"));
            
            IPluginContext context = FolderShortcutPlugin.this.getPluginContext();
            IPluginConfig config = FolderShortcutPlugin.this.getPluginConfig();
            
            IJcrService service = context.getService(IJcrService.class.getName(), IJcrService.class);
            jcrServiceRef = context.getReference(service);

            String workflowCategory = config.getString("gallery.workflow");
            Session jcrSession = ((UserSession)org.apache.wicket.Session.get()).getJcrSession();
            Node folder = ((JcrNodeModel)FolderShortcutPlugin.this.getModel()).getNode();
            try {
                boolean isDefaultFolder = false;
                WorkflowManager manager = ((HippoWorkspace)(jcrSession.getWorkspace())).getWorkflowManager();
                Workflow workflow = manager.getWorkflow(workflowCategory, folder);
                if (workflow instanceof FolderWorkflow) {
                    FolderWorkflow folderWorkflow = (FolderWorkflow)workflow;
                    templates = folderWorkflow.list();
                } else {
                    folder = jcrSession.getRootNode().getNode(defaultDropLocation.startsWith("/") ?
                                                              defaultDropLocation.substring(1) : defaultDropLocation);
                    workflow = manager.getWorkflow(workflowCategory, folder);
                    if (workflow instanceof FolderWorkflow) {
                        FolderWorkflow folderWorkflow = (FolderWorkflow)workflow;
                        templates = folderWorkflow.list();
                        isDefaultFolder = true;
                    } else {
                        folder = null;
                    }
                }
                
                if(optionSelectFirst) {
                    if(optionSelectOnly != null) {
                        Map<String, Set<String>> newTemplates = new TreeMap<String, Set<String>>();
                        if(templates.containsKey(optionSelectOnly))
                            newTemplates.put(optionSelectOnly, templates.get(optionSelectOnly));
                    } else {
                        Map<String, Set<String>> newTemplates = new TreeMap<String, Set<String>>();
                        if(templates.size() > 0) {
                            Map.Entry<String,Set<String>> firstEntry = templates.entrySet().iterator().next();
                            newTemplates.put(firstEntry.getKey(), firstEntry.getValue());
                        }
                    }
                } else if(optionSelectOnly != null&& isDefaultFolder) {
                    Map<String, Set<String>> newTemplates = new TreeMap<String, Set<String>>();
                    if(templates.containsKey(optionSelectOnly))
                        newTemplates.put(optionSelectOnly, templates.get(optionSelectOnly));
                }
            } catch(MappingException ex) {
                log.warn("failure to initialize shortcut", ex);
                folder = null;
            } catch(WorkflowException ex) {
                log.warn("failure to initialize shortcut", ex);
                folder = null;
            } catch(RepositoryException ex) {
                log.warn("failure to initialize shortcut", ex);
                folder = null;
            } catch(RemoteException ex) {
                log.warn("failure to initialize shortcut", ex);
                folder = null;
            }

            add(new TextFieldWidget("name", new PropertyModel(this, "name")));

            add(new Label("message", new Model("")));

            List emptyList = new LinkedList();
            emptyList.add("");
            
            add(folderChoice = new DropDownChoice("prototype", new PropertyModel(this, "prototype"), emptyList) {
                    protected boolean wantOnSelectionChangedNotifications() {
                        return true;
                    }
                    protected void onSelectionChanged(Object newSelection) {
                        super.onSelectionChanged(newSelection);
                        evaluateChoices();
                    }
                });
            folderChoice.setNullValid(false);
            folderChoice.setRequired(true);

            add(categoryChoice = new DropDownChoice("template", new PropertyModel(this, "templateCategory"), emptyList) {
                    protected boolean wantOnSelectionChangedNotifications() {
                        return true;
                    }
                    protected void onSelectionChanged(Object newSelection) {
                        super.onSelectionChanged(newSelection);
                        prototype = null;
                        evaluateChoices();
                    }
                });
            folderChoice.setNullValid(false);
            folderChoice.setRequired(true);

            ok.setEnabled(false);
            if(folder != null) {
                try {
                    List<String> categories = new LinkedList<String>();
                    categories.add(workflowCategory);
                    setModel(new WorkflowsModel(new JcrNodeModel(folder), categories));
                    ok.setEnabled(true);
                } catch(RepositoryException ex) {
                    setModel(null);
                }
            } else {
                setModel(null);
            }
            evaluateChoices();
        }

        private void evaluateChoices() {
            if(templates.keySet().size() == 1) {
                categoryChoice.setChoices(new LinkedList(templates.keySet()));
                categoryChoice.setVisible(false);
                templateCategory = templates.keySet().iterator().next();
            } else if(templates.keySet().size() > 1) {
                categoryChoice.setChoices(new LinkedList(templates.keySet()));
                categoryChoice.setVisible(true);
            } else {
                categoryChoice.setVisible(false);
            }
            if(templateCategory != null) {
                final List<String> prototypesList = new LinkedList<String>(templates.get(templateCategory));
                folderChoice.setChoices(prototypesList);
                folderChoice.setChoiceRenderer(new NamespaceFriendlyChoiceRenderer(prototypesList));
                if(templates.get(templateCategory).size() > 1) {
                    folderChoice.setVisible(true);
                    folderChoice.setNullValid(false);
                    folderChoice.setRequired(true);
                    ok.setEnabled(false);
                } else if(templates.get(templateCategory).size() == 1) {
                    prototype = templates.get(templateCategory).iterator().next();
                    folderChoice.setVisible(false);
                    ok.setEnabled(true);
                } else {
                    folderChoice.setVisible(false);
                    ok.setEnabled(false);
                }
            } else {
                folderChoice.setVisible(false);
                ok.setEnabled(false);
            }
            if(prototype != null) {
                ok.setEnabled(true);
            }
        }

        @Override
        public void onDetach() {
            if (jcrServiceRef != null) {
                jcrServiceRef.detach();
            }
            super.onDetach();
        }

        public String getTitle() {
             return "New document";
        }

        protected void ok() throws Exception {
            IModel model = getModel();
            if(model != null && model instanceof WorkflowsModel) {
                Session jcrSession = ((UserSession)org.apache.wicket.Session.get()).getJcrSession();
                WorkflowManager manager = ((HippoWorkspace)(jcrSession.getWorkspace())).getWorkflowManager();
                FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow(((WorkflowsModel)model).getWorkflowDescriptor());
                if (prototype == null) {
                    throw new WorkflowException("You need to select a type");
                }
                if (workflow != null) {
                    if (!templates.get(templateCategory).contains(prototype)) {
                        log.error("unknown folder type " + prototype);
                        throw new WorkflowException("Unknown folder type " + prototype);
                    }
                    String path = workflow.add(templateCategory, prototype, name);
                    JcrNodeModel nodeModel = new JcrNodeModel(new JcrItemModel(path));
                    select(nodeModel);

                    IJcrService jcrService = jcrServiceRef.getService();
                    jcrService.flush((JcrNodeModel)FolderShortcutPlugin.this.getModel());

                } else {
                    log.error("no workflow defined on model for selected node");
                }
            }
        }

        protected void cancel() {
        }
    }
}

