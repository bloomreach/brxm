/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.ajax.BrLink;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.i18n.types.TypeChoiceRenderer;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standardworkflow.components.LanguageField;
import org.hippoecm.frontend.plugins.standardworkflow.validators.AddDocumentValidator;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.util.CodecUtils;
import org.hippoecm.frontend.widgets.NameUriField;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.repository.security.StandardPermissionNames.HIPPO_AUTHOR;

public class FolderShortcutPlugin extends RenderPlugin {

    private static final Logger log = LoggerFactory.getLogger(FolderShortcutPlugin.class);

    private static final String SLASH = "/";

    private String defaultDropLocation = "/content";

    public FolderShortcutPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final AjaxLink<Void> link = new BrLink<Void>("link") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                final IDialogService dialogService = getDialogService();
                final JcrNodeModel model = (JcrNodeModel) FolderShortcutPlugin.this.getDefaultModel();
                final Node node = model != null ? model.getNode() : null;
                dialogService.show(new FolderShortcutPlugin.AddRootFolderDialog(context, config, node, defaultDropLocation));
            }
        };
        add(link);

        final String path = config.getString("option.location");
        if (path != null && !path.equals("")) {
            defaultDropLocation = path;
        }

        if (!defaultDropLocation.equals("")) {
            try {
                final Session jcrSession = UserSession.get().getJcrSession();
                while (defaultDropLocation.startsWith(SLASH)) {
                    defaultDropLocation = defaultDropLocation.substring(1);
                }
                if (!jcrSession.getRootNode().hasNode(defaultDropLocation)) {
                    defaultDropLocation = null;
                } else {
                    link.setVisible(jcrSession.hasPermission(SLASH + defaultDropLocation, HIPPO_AUTHOR));
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
    public static void select(final JcrNodeModel nodeModel,
                              final IServiceReference<IBrowseService> browseServiceRef,
                              final IServiceReference<IEditorManager> editServiceRef) {
        try {
            final Node node = nodeModel.getNode();
            if (node == null) {
                return;
            }

            if (node.isNodeType(HippoNodeType.NT_DOCUMENT) || node.isNodeType(HippoNodeType.NT_HANDLE)) {
                final IBrowseService browser = (browseServiceRef != null ? browseServiceRef.getService() : null);
                if (browser != null) {
                    browser.browse(nodeModel);
                }
                if (!node.isNodeType(HippoStdNodeType.NT_FOLDER) && !node.isNodeType(HippoStdNodeType.NT_DIRECTORY)) {
                    openEditor(nodeModel, editServiceRef);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }
    }

    private static void openEditor(final JcrNodeModel nodeModel,
                                   final IServiceReference<IEditorManager> editServiceRef) throws RepositoryException {
        final IEditorManager editorMgr = (editServiceRef != null ? editServiceRef.getService() : null);
        if (editorMgr == null) {
            return;
        }

        JcrNodeModel editNodeModel = nodeModel;
        Node editNodeModelNode = nodeModel.getNode();
        if (editNodeModelNode.isNodeType(HippoNodeType.NT_HANDLE)) {
            editNodeModelNode = editNodeModelNode.getNode(editNodeModelNode.getName());
        }

        final WorkflowManager workflowManager = UserSession.get().getWorkflowManager();
        final Workflow workflow = workflowManager.getWorkflow("editing", editNodeModelNode);

        try {
            if (workflow instanceof EditableWorkflow) {
                final EditableWorkflow editableWorkflow = (EditableWorkflow) workflow;
                final Document editableDocument = editableWorkflow.obtainEditableInstance();
                if (editableDocument != null) {
                    final Session jcrSession = UserSession.get().getJcrSession();
                    jcrSession.refresh(true);
                    final String id = editableDocument.getIdentity();
                    editNodeModel = new JcrNodeModel(jcrSession.getNodeByIdentifier(id));
                } else {
                    editNodeModel = null;
                }
            }
            if (editNodeModel != null) {
                final IEditor editor = editorMgr.getEditor(editNodeModel);
                if (editor == null) {
                    editorMgr.openEditor(editNodeModel);
                }
            }
        } catch (WorkflowException | ServiceException | RepositoryException | RemoteException ex) {
            log.error("Cannot auto-edit document", ex);
        }
    }

    protected StringCodec getLocalizeCodec() {
        return CodecUtils.getDisplayNameCodec(getPluginContext());
    }

    protected ILocaleProvider getLocaleProvider() {
        return getPluginContext().getService(
                getPluginConfig().getString(ILocaleProvider.SERVICE_ID, ILocaleProvider.class.getName()),
                ILocaleProvider.class);
    }

    public class AddRootFolderDialog extends Dialog<WorkflowDescriptor> {

        private String templateCategory = null;
        private String prototype = null;
        private String language = null;

        private final IServiceReference<IBrowseService> browseServiceRef;
        private final IServiceReference<IEditorManager> editServiceRef;
        private Map<String, Set<String>> templates;

        private final DropDownChoice<String> prototypeChoice;
        private final DropDownChoice<String> templateChoice;

        private final NameUriField nameUriContainer;
        private final WebMarkupContainer prototypeContainer;
        private final WebMarkupContainer templateContainer;
        private final LanguageField languageContainer;

        public AddRootFolderDialog(IPluginContext context, IPluginConfig config, Node folder, String defaultFolder) {
            super();

            setSize(DialogConstants.MEDIUM_AUTO);
            setCssClass("add-root-folder-dialog");

            boolean optionSelectFirst = false;
            if (config.containsKey("option.first")) {
                optionSelectFirst = config.getBoolean("option.first");
            }

            String optionSelectOnly = null;
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
                    FolderWorkflow folderWorkflow = (FolderWorkflow) workflow;
                    templates = (Map<String, Set<String>>) folderWorkflow.hints().get("prototypes");
                } else {
                    folder = null;
                    templates = Collections.emptyMap();
                }

                if (optionSelectFirst) {
                    if (optionSelectOnly != null) {
                        Map<String, Set<String>> newTemplates = new TreeMap<>();
                        if (templates.containsKey(optionSelectOnly)) {
                            newTemplates.put(optionSelectOnly, templates.get(optionSelectOnly));
                        }
                        templates = newTemplates;
                    } else {
                        Map<String, Set<String>> newTemplates = new TreeMap<>();
                        if (templates.size() > 0) {
                            Map.Entry<String, Set<String>> firstEntry = templates.entrySet().iterator().next();
                            newTemplates.put(firstEntry.getKey(), firstEntry.getValue());
                        }
                        templates = newTemplates;
                    }
                } else if (optionSelectOnly != null) {
                    Map<String, Set<String>> newTemplates = new TreeMap<>();
                    if (templates.containsKey(optionSelectOnly)) {
                        newTemplates.put(optionSelectOnly, templates.get(optionSelectOnly));
                    }
                    templates = newTemplates;
                }
            } catch (RepositoryException | WorkflowException | RemoteException ex) {
                log.warn("failure to initialize shortcut", ex);
                folder = null;
            }

            add(new Label("message", Model.of("")) {
                @Override
                public boolean isVisible() {
                    return StringUtils.isNotBlank(this.getDefaultModelObjectAsString());
                }
            });

            final IModel<StringCodec> codecModel = new LoadableDetachableModel<StringCodec>() {
                @Override
                protected StringCodec load() {
                    //language value can change between load() calls
                    return CodecUtils.getNodeNameCodec(getPluginContext(), language);
                }
            };

            nameUriContainer = new NameUriField("name-url", codecModel);
            add(nameUriContainer);

            // The dialog produces ajax requests in NameUriField and OK/Cancel dialog buttons, which may cause Wicket
            // exceptions when typing very fast. Thus it needs to use a dedicated ajax channel with ACTIVE behavior when
            // some AJAX requests may be sent after dialog is closed.
            final AjaxChannel activeAjaxChannel = new AjaxChannel(getMarkupId(), AjaxChannel.Type.ACTIVE);
            setAjaxChannel(activeAjaxChannel);
            nameUriContainer.setAjaxChannel(activeAjaxChannel);

            List<String> emptyList = new LinkedList<>();
            emptyList.add("");

            prototypeChoice = new DropDownChoice<>("select", new PropertyModel<>(this, "prototype"), emptyList);
            prototypeChoice.setNullValid(false);
            prototypeChoice.setRequired(true);
            prototypeChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    evaluateChoices();
                }
            });

            prototypeContainer = new WebMarkupContainer("prototype");
            prototypeContainer.setOutputMarkupPlaceholderTag(true);
            prototypeContainer.add(prototypeChoice);
            add(prototypeContainer);

            final PropertyModel<String> templateCategoryModel = new PropertyModel<>(this, "templateCategory");
            templateChoice = new DropDownChoice<>("select", templateCategoryModel, emptyList, new IChoiceRenderer<String>() {

                public Object getDisplayValue(String object) {
                    return getString(object);
                }

                public String getIdValue(String object, int index) {
                    return object;
                }

                @Override
                public String getObject(final String id, final IModel<? extends List<? extends String>> choicesModel) {
                    final List<? extends String> choices = choicesModel.getObject();
                    return choices.contains(id) ? id : null;
                }
            });
            templateChoice.setNullValid(false);
            templateChoice.setRequired(true);
            templateChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    prototype = null;
                    evaluateChoices();
                    // Detach the codec model to load the correct codec for the selected template
                    codecModel.detach();
                    nameUriContainer.onCodecModelDetached();
                }
            });

            templateContainer = new WebMarkupContainer("template");
            templateContainer.setOutputMarkupPlaceholderTag(true);
            templateContainer.add(templateChoice);
            templateContainer.add(new Label("typelabel", new StringResourceModel("document-type", this)));
            add(templateContainer);

            languageContainer = new LanguageField("language", new PropertyModel<>(this, "language"), getLocaleProvider()) {
                @Override
                protected void onSelectionChanged() {
                    // Detach the codec model to load the correct codec for the selected language
                    codecModel.detach();
                    nameUriContainer.onCodecModelDetached();
                }
            };
            add(languageContainer);

            setModel(folderWorkflowDescriptorModel);
            setOkEnabled(folder != null);

            if (templates != null && !templates.isEmpty()) {
                // pre-select the first item in the template category
                templateCategory = templates.keySet().iterator().next();
            }
            evaluateChoices();

            add(new AddDocumentValidator(this, nameUriContainer, folderWorkflowDescriptorModel));
        }

        @Override
        protected FeedbackPanel newFeedbackPanel(String id) {
            return new FeedbackPanel(id);
        }

        private void evaluateChoices() {
            if (templates == null || templates.isEmpty()) {
                templateContainer.setVisible(false);
            } else if (templates.keySet().size() == 1) {
                templateChoice.setChoices(new LinkedList<>(templates.keySet()));
                templateContainer.setVisible(false);
            } else {
                templateChoice.setChoices(new LinkedList<>(templates.keySet()));
                templateContainer.setVisible(true);
            }
            boolean languageVisible = false;
            if (templates != null && templateCategory != null) {
                final List<String> prototypesList = new LinkedList<>(templates.get(templateCategory));
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

            Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
            target.ifPresent(ajaxRequestTarget -> {
                ajaxRequestTarget.add(prototypeContainer);
                ajaxRequestTarget.add(templateContainer);

            });

            if (languageVisible != languageContainer.isVisible()) {
                languageContainer.setVisible(languageVisible);
                target.ifPresent(ajaxRequestTarget -> ajaxRequestTarget.add(this));
            }

            // By resetting the language the URI is re-encoded using the correct StringCodec
            // when switching between translated and non-translated root folders
            if (!languageVisible) {
                language = null;
            }
        }

        @Override
        public IModel<String> getTitle() {
            return new StringResourceModel("new-document-label", this);
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
                if (model instanceof WorkflowDescriptorModel) {
                    Session jcrSession = UserSession.get().getJcrSession();
                    WorkflowManager manager = ((HippoWorkspace) (jcrSession.getWorkspace())).getWorkflowManager();
                    FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow(((WorkflowDescriptorModel) model)
                            .getObject());
                    if (prototype == null) {
                        error("You need to select a type");
                        return;
                    }
                    if (workflow != null) {
                        if (!templates.get(templateCategory).contains(prototype)) {
                            log.error("unknown folder type {}", prototype);
                            error("Unknown folder type " + prototype);
                            return;
                        }

                        String localName = getLocalizeCodec().encode(name);
                        String nodeName = CodecUtils.getNodeNameCodec(getPluginContext(), language).encode(url);

                        TreeMap<String, String> arguments = new TreeMap<>();
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
                            defaultWorkflow.setDisplayName(localName);
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
    }
}
