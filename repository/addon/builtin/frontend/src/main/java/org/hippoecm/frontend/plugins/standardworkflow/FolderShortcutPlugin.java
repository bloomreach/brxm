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
package org.hippoecm.frontend.plugins.standardworkflow;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.i18n.types.TypeChoiceRenderer;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;

public class FolderShortcutPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static Logger log = LoggerFactory.getLogger(FolderShortcutPlugin.class);

    private String defaultDropLocation = "/content";

    public FolderShortcutPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        AjaxLink link = new AjaxLink("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                IDialogService dialogService = getDialogService();
                JcrNodeModel model = (JcrNodeModel) FolderShortcutPlugin.this.getModel();
                dialogService.show(new FolderShortcutPlugin.Dialog(context, config,
                        (model != null ? model.getNode() : null), defaultDropLocation));
            }
        };
        add(link);

        String path = config.getString("option.location");
        if (path != null && !path.equals("")) {
            defaultDropLocation = path;
        }

        if (!defaultDropLocation.equals("")) {
            try {
                Session jcrSession = ((UserSession)org.apache.wicket.Session.get()).getJcrSession();
                while (defaultDropLocation.startsWith("/")) {
                    defaultDropLocation = defaultDropLocation.substring(1);
                }
                if (!jcrSession.getRootNode().hasNode(defaultDropLocation))
                    defaultDropLocation = null;
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

    // FIXME: pure duplication of logic in FolderWorkflowPlugin
    @SuppressWarnings("unchecked")
    public static void select(JcrNodeModel nodeModel, IServiceReference<IBrowseService> browseServiceRef,
            IServiceReference<IEditorManager> editServiceRef) {
        IBrowseService browser = (browseServiceRef != null ? browseServiceRef.getService() : null);
        IEditorManager editorMgr = (editServiceRef != null ? editServiceRef.getService() : null);
        try {
            if (nodeModel.getNode() != null
                    && (nodeModel.getNode().isNodeType(HippoNodeType.NT_DOCUMENT) || nodeModel.getNode().isNodeType(
                            HippoNodeType.NT_HANDLE))) {
                if (browser != null) {
                    browser.browse(nodeModel);
                }
                if (!nodeModel.getNode().isNodeType("hippostd:folder") && !nodeModel.getNode().isNodeType("hippostd:directory")) {
                    if (editorMgr != null) {
                        JcrNodeModel editNodeModel = nodeModel;
                        Node editNodeModelNode = nodeModel.getNode();
                        if (editNodeModelNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                            editNodeModelNode = editNodeModelNode.getNode(editNodeModelNode.getName());
                        }
                        WorkflowManager workflowManager = ((UserSession) org.apache.wicket.Session.get())
                                .getWorkflowManager();
                        Workflow workflow = workflowManager.getWorkflow("editing", editNodeModelNode);
                        try {
                            if (workflow instanceof EditableWorkflow) {
                                EditableWorkflow editableWorkflow = (EditableWorkflow) workflow;
                                Document editableDocument = editableWorkflow.obtainEditableInstance();
                                if (editableDocument != null) {
                                    Session jcrSession = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
                                    jcrSession.refresh(true);
                                    editNodeModel = new JcrNodeModel(jcrSession.getNodeByUUID(editableDocument.getIdentity()));
                                } else {
                                    editNodeModel = null;
                                }
                            }
                            if (editNodeModel != null) {
                                editorMgr.openEditor(editNodeModel);
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

    public class Dialog extends AbstractDialog {
        @SuppressWarnings("unused")
        private final static String SVN_ID = "$Id$";

        private static final long serialVersionUID = 1L;

        private String templateCategory = null;
        private String prototype = null;
        private String name;
        protected IServiceReference<IBrowseService> browseServiceRef;
        protected IServiceReference<IEditorManager> editServiceRef;
        private Map<String, Set<String>> templates;
        protected final DropDownChoice folderChoice;
        protected final DropDownChoice categoryChoice;
        protected final Label typelabel;
        private String optionSelectOnly = null;
        private boolean optionSelectFirst = false;

        public Dialog(IPluginContext context, IPluginConfig config, Node folder, String defaultFolder) {
            ok.setModel(new Model("Create"));

            if (config.containsKey("option.first"))
                optionSelectFirst = config.getBoolean("option.first");
            if (config.containsKey("option.only"))
                optionSelectOnly = config.getString("option.only");

            browseServiceRef = context.getReference(context.getService(config.getString(IBrowseService.BROWSER_ID),
                    IBrowseService.class));
            editServiceRef = context.getReference(context.getService(config.getString(IEditorManager.EDITOR_ID),
                    IEditorManager.class));

            add(typelabel = new Label("typelabel", new StringResourceModel("document-type", FolderShortcutPlugin.this, null)));

            String workflowCategory = config.getString("workflow.categories");
            Session jcrSession = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
            WorkflowDescriptor folderWorkflowDescriptor = null;
            try {
                WorkflowManager manager = ((HippoWorkspace) (jcrSession.getWorkspace())).getWorkflowManager();
                folderWorkflowDescriptor = (folder != null ? manager.getWorkflowDescriptor(workflowCategory, folder) : null);
                Workflow workflow = (folderWorkflowDescriptor != null ? manager.getWorkflow(folderWorkflowDescriptor) : null);
                if (workflow instanceof FolderWorkflow) {
                    templates = ((FolderWorkflow) workflow).list();
                } else {
                    folder = jcrSession.getRootNode().getNode(
                            defaultFolder.startsWith("/") ? defaultFolder.substring(1) : defaultFolder);
                    workflow = manager.getWorkflow(workflowCategory, folder);
                    if (workflow instanceof FolderWorkflow) {
                        FolderWorkflow folderWorkflow = (FolderWorkflow) workflow;
                        templates = folderWorkflow.list();
                    } else {
                        folder = null;
                    }
                }

                if (optionSelectFirst) {
                    if (optionSelectOnly != null) {
                        Map<String, Set<String>> newTemplates = new TreeMap<String, Set<String>>();
                        if (templates.containsKey(optionSelectOnly))
                            newTemplates.put(optionSelectOnly, templates.get(optionSelectOnly));
                        templates = newTemplates;
                    } else {
                        Map<String, Set<String>> newTemplates = new TreeMap<String, Set<String>>();
                        if (templates.size() > 0) {
                            Map.Entry<String, Set<String>> firstEntry = templates.entrySet().iterator().next();
                            newTemplates.put(firstEntry.getKey(), firstEntry.getValue());
                        }
                        templates = newTemplates;
                    }
                } else if (optionSelectOnly != null) {
                    Map<String, Set<String>> newTemplates = new TreeMap<String, Set<String>>();
                    if (templates.containsKey(optionSelectOnly))
                        newTemplates.put(optionSelectOnly, templates.get(optionSelectOnly));
                    templates = newTemplates;
                }
            } catch (MappingException ex) {
                log.warn("failure to initialize shortcut", ex);
                folder = null;
            } catch (WorkflowException ex) {
                log.warn("failure to initialize shortcut", ex);
                folder = null;
            } catch (RepositoryException ex) {
                log.warn("failure to initialize shortcut", ex);
                folder = null;
            } catch (RemoteException ex) {
                log.warn("failure to initialize shortcut", ex);
                folder = null;
            }

            add(new TextFieldWidget("name", new PropertyModel(this, "name")) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    evaluateChoices();
                }
            });

            add(new Label("message", new Model("")));

            List<String> emptyList = new LinkedList<String>();
            emptyList.add("");

            add(folderChoice = new DropDownChoice("prototype", new PropertyModel(this, "prototype"), emptyList));
            folderChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    evaluateChoices();
                }
            });
            folderChoice.setNullValid(false);
            folderChoice.setRequired(true);
            folderChoice.setOutputMarkupId(true);

            add(categoryChoice = new DropDownChoice("template", new PropertyModel(this, "templateCategory"), emptyList));
            categoryChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    prototype = null;
                    evaluateChoices();
                }
            });
            categoryChoice.setNullValid(false);
            categoryChoice.setRequired(true);
            categoryChoice.setOutputMarkupId(true);

            ok.setEnabled(false);
            if (folder != null && folderWorkflowDescriptor != null) {
                try {
                    setModel(new WorkflowDescriptorModel(folderWorkflowDescriptor, workflowCategory, folder));
                    ok.setEnabled(true);
                } catch (RepositoryException ex) {
                    setModel(null);
                }
            } else {
                setModel(null);
            }
            if (templates.keySet().size() >= 1) {
                // pre-select the first item in the template category
                templateCategory = templates.keySet().iterator().next();
            }
            evaluateChoices();
        }

        private void evaluateChoices() {
            if (templates.keySet().size() == 1) {
                categoryChoice.setChoices(new LinkedList<String>(templates.keySet()));
                categoryChoice.setVisible(false);
                typelabel.setVisible(false);
            } else if (templates.keySet().size() > 1) {
                categoryChoice.setChoices(new LinkedList<String>(templates.keySet()));
                categoryChoice.setVisible(true);
                typelabel.setVisible(true);
            } else {
                categoryChoice.setVisible(false);
                typelabel.setVisible(false);
            }
            if (templateCategory != null) {
                final List<String> prototypesList = new LinkedList<String>(templates.get(templateCategory));
                folderChoice.setChoices(prototypesList);
                folderChoice.setChoiceRenderer(new TypeChoiceRenderer(this));
                if (prototypesList.size() > 1) {
                    folderChoice.setVisible(true);
                    folderChoice.setNullValid(false);
                    folderChoice.setRequired(true);
                    if (!prototypesList.contains(prototype)) {
                        prototype = null;
                    }
                } else if (prototypesList.size() == 1) {
                    prototype = prototypesList.iterator().next();
                    folderChoice.setVisible(false);
                } else {
                    folderChoice.setVisible(false);
                    prototype = null;
                }
            } else {
                folderChoice.setVisible(false);
                prototype = null;
            }
            ok.setEnabled(prototype != null);
            AjaxRequestTarget target = AjaxRequestTarget.get();
            if (target != null) {
                target.addComponent(ok);
                target.addComponent(folderChoice);
                target.addComponent(categoryChoice);
            }
        }

        public IModel getTitle() {
            return new StringResourceModel("new-document-label", this, null);
        }

        @Override
        protected void onOk() {
            if (name == null || "".equals(name)) {
                error("You need to enter a name");
            }
            try {
                IModel model = getModel();
                if (model != null && model instanceof WorkflowDescriptorModel) {
                    Session jcrSession = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
                    WorkflowManager manager = ((HippoWorkspace) (jcrSession.getWorkspace())).getWorkflowManager();
                    FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow((WorkflowDescriptor)((WorkflowDescriptorModel) model).getObject());
                    if (prototype == null) {
                        error("You need to select a type");
                        return;
                    }
                    if (workflow != null) {
                        if (!templates.get(templateCategory).contains(prototype)) {
                            log.error("unknown folder type " + prototype);
                            error("Unknown folder type " + prototype);
                            return;
                        }
                        String path = workflow.add(templateCategory, prototype, NodeNameCodec.encode(name, true));
                        jcrSession.refresh(true);

                        JcrNodeModel nodeModel = new JcrNodeModel(new JcrItemModel(path));
                        select(nodeModel, browseServiceRef, editServiceRef);
                    } else {
                        log.error("no workflow defined on model for selected node");
                    }
                }
            } catch (Exception ex) {
                error(ex.getMessage());
            }
        }
    }
}
