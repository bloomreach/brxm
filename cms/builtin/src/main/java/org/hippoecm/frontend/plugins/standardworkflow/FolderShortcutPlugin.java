/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
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
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.i18n.types.TypeChoiceRenderer;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standardworkflow.components.LanguageField;
import org.hippoecm.frontend.plugins.standardworkflow.components.NameUriField;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderShortcutPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    static Logger log = LoggerFactory.getLogger(FolderShortcutPlugin.class);
    private static final String SLASH = "/";

    private String defaultDropLocation = "/content";

    public FolderShortcutPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        AjaxLink link = new AjaxLink("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                IDialogService dialogService = getDialogService();
                JcrNodeModel model = (JcrNodeModel) FolderShortcutPlugin.this.getDefaultModel();
                dialogService.show(new FolderShortcutPlugin.Dialog(context, config, (model != null ? model.getNode()
                        : null), defaultDropLocation));
            }
        };
        add(link);

        String path = config.getString("option.location");
        if (path != null && !path.equals("")) {
            defaultDropLocation = path;
        }

        if (!defaultDropLocation.equals("")) {
            try {
                Session jcrSession = UserSession.get().getJcrSession();
                while (defaultDropLocation.startsWith(SLASH)) {
                    defaultDropLocation = defaultDropLocation.substring(1);
                }
                if (!jcrSession.getRootNode().hasNode(defaultDropLocation)) {
                    defaultDropLocation = null;
                } else {
                    link.setVisible(jcrSession.hasPermission(SLASH + defaultDropLocation, Session.ACTION_ADD_NODE));
                }
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

    @Override
    public boolean isEnabled() {
        return getPluginConfig().getAsBoolean("workflow.enabled", true);
    }

    @Override
    public boolean isVisible() {
        return getPluginConfig().getAsBoolean("workflow.enabled", true);
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
                if (!nodeModel.getNode().isNodeType("hippostd:folder")
                        && !nodeModel.getNode().isNodeType("hippostd:directory")) {
                    if (editorMgr != null) {
                        JcrNodeModel editNodeModel = nodeModel;
                        Node editNodeModelNode = nodeModel.getNode();
                        if (editNodeModelNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                            editNodeModelNode = editNodeModelNode.getNode(editNodeModelNode.getName());
                        }
                        WorkflowManager workflowManager = UserSession.get().getWorkflowManager();
                        Workflow workflow = workflowManager.getWorkflow("editing", editNodeModelNode);
                        try {
                            if (workflow instanceof EditableWorkflow) {
                                EditableWorkflow editableWorkflow = (EditableWorkflow) workflow;
                                Document editableDocument = editableWorkflow.obtainEditableInstance();
                                if (editableDocument != null) {
                                    Session jcrSession = UserSession.get().getJcrSession();
                                    jcrSession.refresh(true);
                                    final String id = editableDocument.getIdentity();
                                    editNodeModel = new JcrNodeModel(jcrSession.getNodeByIdentifier(id));
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

    protected ILocaleProvider getLocaleProvider() {
        return getPluginContext().getService(
                getPluginConfig().getString(ILocaleProvider.SERVICE_ID, ILocaleProvider.class.getName()),
                ILocaleProvider.class);
    }

    public class Dialog extends AbstractDialog {
        private static final long serialVersionUID = 1L;

        private String templateCategory = null;
        private String prototype = null;
        private String language = null;

        private final IServiceReference<IBrowseService> browseServiceRef;
        private final IServiceReference<IEditorManager> editServiceRef;
        private Map<String, Set<String>> templates;

        private final DropDownChoice prototypeChoice;
        private final DropDownChoice templateChoice;

        private String optionSelectOnly = null;
        private boolean optionSelectFirst = false;

        private NameUriField nameUriContainer;
        private WebMarkupContainer prototypeContainer;
        private WebMarkupContainer templateContainer;
        private LanguageField languageContainer;

        public Dialog(IPluginContext context, IPluginConfig config, Node folder, String defaultFolder) {

            if (config.containsKey("option.first")) {
                optionSelectFirst = config.getBoolean("option.first");
            }
            if (config.containsKey("option.only")) {
                optionSelectOnly = config.getString("option.only");
            }

            browseServiceRef = context.getReference(context.getService(config.getString(IBrowseService.BROWSER_ID),
                    IBrowseService.class));
            editServiceRef = context.getReference(context.getService(config.getString(IEditorManager.EDITOR_ID),
                    IEditorManager.class));

            String workflowCategory = config.getString("workflow.categories");
            Session jcrSession = UserSession.get().getJcrSession();
            WorkflowDescriptorModel folderWorkflowDescriptorModel = null;
            try {
                if (folder == null) {
                    folder = jcrSession.getRootNode().getNode(
                            defaultFolder.startsWith(SLASH) ? defaultFolder.substring(1) : defaultFolder);
                }

                folderWorkflowDescriptorModel = new WorkflowDescriptorModel(workflowCategory, folder);
                Workflow workflow = folderWorkflowDescriptorModel.getWorkflow();
                if (workflow instanceof FolderWorkflow) {
                    templates = ((FolderWorkflow) workflow).list();
                } else {
                    folder = null;
                    templates = Collections.emptyMap();
                }

                if (optionSelectFirst) {
                    if (optionSelectOnly != null) {
                        Map<String, Set<String>> newTemplates = new TreeMap<String, Set<String>>();
                        if (templates.containsKey(optionSelectOnly)) {
                            newTemplates.put(optionSelectOnly, templates.get(optionSelectOnly));
                        }
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
                    if (templates.containsKey(optionSelectOnly)) {
                        newTemplates.put(optionSelectOnly, templates.get(optionSelectOnly));
                    }
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

            add(new Label("message", new Model("")));

            add(nameUriContainer = new NameUriField("name-url", new LoadableDetachableModel<StringCodec>() {
                private static final long serialVersionUID = 1L;

                @Override
                protected StringCodec load() {
                    return getNodeNameCodec();
                }
            }));

            List<String> emptyList = new LinkedList<String>();
            emptyList.add("");

            prototypeContainer = new WebMarkupContainer("prototype");
            prototypeContainer.add(prototypeChoice = new DropDownChoice("select", new PropertyModel(this, "prototype"),
                    emptyList));
            prototypeChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    evaluateChoices();
                }
            });
            prototypeChoice.setNullValid(false);
            prototypeChoice.setRequired(true);
            prototypeContainer.setOutputMarkupPlaceholderTag(true);
            add(prototypeContainer);

            templateContainer = new WebMarkupContainer("template");
            templateContainer.add(templateChoice = new DropDownChoice<String>("select", new PropertyModel<String>(this,
                    "templateCategory"), emptyList, new IChoiceRenderer<String>() {
                private static final long serialVersionUID = 1L;

                public Object getDisplayValue(String object) {
                    return new StringResourceModel(object, Dialog.this, null).getObject();
                }

                public String getIdValue(String object, int index) {
                    return object;
                }
            }));
            templateChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    prototype = null;
                    evaluateChoices();
                }
            });
            templateChoice.setNullValid(false);
            templateChoice.setRequired(true);
            templateContainer.setOutputMarkupPlaceholderTag(true);
            templateContainer.add(new Label("typelabel", new StringResourceModel("document-type", this, null)));
            add(templateContainer);

            languageContainer = new LanguageField("language", new PropertyModel<String>(this, "language"), getLocaleProvider());
            add(languageContainer);

            setOkEnabled(false);

            if (folder != null && folderWorkflowDescriptorModel != null) {
                setModel(folderWorkflowDescriptorModel);
                setOkEnabled(true);
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
                templateChoice.setChoices(new LinkedList<String>(templates.keySet()));
                templateContainer.setVisible(false);
            } else if (templates.keySet().size() > 1) {
                templateChoice.setChoices(new LinkedList<String>(templates.keySet()));
                templateContainer.setVisible(true);
            } else {
                templateContainer.setVisible(false);
            }
            boolean languageVisible = false;
            if (templateCategory != null) {
                final List<String> prototypesList = new LinkedList<String>(templates.get(templateCategory));
                prototypeChoice.setChoices(prototypesList);
                prototypeChoice.setChoiceRenderer(new TypeChoiceRenderer(this));
                if (prototypesList.size() > 1) {
                    prototypeContainer.setVisible(true);
                    prototypeChoice.setNullValid(false);
                    prototypeChoice.setRequired(true);
                    if (!prototypesList.contains(prototype)) {
                        prototype = null;
                    }
                } else if (prototypesList.size() == 1) {
                    prototype = prototypesList.iterator().next();
                    prototypeContainer.setVisible(false);
                } else {
                    prototypeContainer.setVisible(false);
                    prototype = null;
                }
                if (prototype != null) {
                    String[] translated = getPluginConfig().getStringArray("workflow.translated");
                    if (translated != null) {
                        for (String translatedPrototype : translated) {
                            if (translatedPrototype.equals(templateCategory)) {
                                languageVisible = true;
                                break;
                            }
                        }
                    }
                }
            } else {
                prototypeContainer.setVisible(false);
                prototype = null;
            }
            setOkEnabled(prototype != null);

            AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
            if (target != null) {
                target.add(prototypeContainer);
                target.add(templateContainer);
            }

            if (languageVisible != languageContainer.isVisible()) {
                languageContainer.setVisible(languageVisible);
                if (target != null) {
                    target.add(this);
                }
            }
        }

        public IModel getTitle() {
            return new StringResourceModel("new-document-label", this, null);
        }

        @Override
        protected void onOk() {
            String name = nameUriContainer.getName();
            String url = nameUriContainer.getUrl();
            if (name == null || "".equals(name)) {
                error("You need to enter a name");
            }
            try {
                IModel model = getModel();
                if (model != null && model instanceof WorkflowDescriptorModel) {
                    Session jcrSession = UserSession.get().getJcrSession();
                    WorkflowManager manager = ((HippoWorkspace) (jcrSession.getWorkspace())).getWorkflowManager();
                    FolderWorkflow workflow = (FolderWorkflow) manager
                            .getWorkflow((WorkflowDescriptor) ((WorkflowDescriptorModel) model).getObject());
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

                        String localName = getLocalizeCodec().encode(name);
                        String nodeName = getNodeNameCodec().encode(url);

                        TreeMap<String, String> arguments = new TreeMap<String, String>();
                        arguments.put("name", nodeName);
                        if (language != null) {
                            arguments.put("hippotranslation:locale", language);
                        }

                        String path = workflow.add(templateCategory, prototype, arguments);
                        Node node = jcrSession.getNode(path);
                        if (!nodeName.equals(localName)) {
                            WorkflowManager workflowMgr = UserSession.get()
                                    .getWorkflowManager();
                            DefaultWorkflow defaultWorkflow = (DefaultWorkflow) workflowMgr.getWorkflow("core", node);
                            defaultWorkflow.localizeName(localName);
                        }
                        jcrSession.refresh(true);

                        JcrNodeModel nodeModel = new JcrNodeModel(path);
                        select(nodeModel, browseServiceRef, editServiceRef);
                    } else {
                        log.error("no workflow defined on model for selected node");
                    }
                }
            } catch (Exception ex) {
                error(ex.getMessage());
            }
        }

        @Override
        public IValueMap getProperties() {
            return DialogConstants.MEDIUM;
        }
    }
}
