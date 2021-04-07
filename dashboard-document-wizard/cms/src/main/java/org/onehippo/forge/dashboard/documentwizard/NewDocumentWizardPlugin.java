/*
 * Copyright 2001-2021 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package org.onehippo.forge.dashboard.documentwizard;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.FencedFeedbackPanel;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.CmsHeaderItem;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeNameModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.yui.datetime.YuiDateTimeField;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.util.CodecUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.onehippo.forge.selection.frontend.model.ValueList;
import org.onehippo.forge.selection.frontend.provider.IValueListProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.forge.dashboard.documentwizard.NewDocumentWizardPlugin.ClassificationType.DATE;
import static org.onehippo.forge.dashboard.documentwizard.NewDocumentWizardPlugin.ClassificationType.LIST;
import static org.onehippo.forge.dashboard.documentwizard.NewDocumentWizardPlugin.ClassificationType.LISTDATE;

public class NewDocumentWizardPlugin extends RenderPlugin<Object> implements IHeaderContributor {

    private static final Logger log = LoggerFactory.getLogger(NewDocumentWizardPlugin.class);

    private static final String DEFAULT_LANGUAGE = "en";

    private static final ResourceReference WIZARD_CSS = new CssResourceReference(NewDocumentWizardPlugin.class,
                                                                                 "NewDocumentWizardPlugin.css");

    private static final String PARAM_CLASSIFICATION_TYPE = "classificationType";
    private static final String PARAM_BASE_FOLDER = "baseFolder";
    private static final String PARAM_DOCUMENT_TYPE = "documentType";
    private static final String PARAM_QUERY = "query";
    private static final String PARAM_VALUE_LIST_PATH = "valueListPath";
    private static final String PARAM_SHORTCUT_LINK_LABEL = "shortcut-link-label";

    private static final String DEFAULT_QUERY = "new-document";
    private static final String DEFAULT_BASE_FOLDER = "/content/documents";
    private static final String DEFAULT_SERVICE_VALUE_LIST = "service.valuelist.default";

    protected enum ClassificationType {DATE, LIST, LISTDATE}

    /**
     * This class creates a link on the dashboard. The link opens a dialog that allow the user to quickly create a
     * document. The location of the document can be configured.
     */
    public NewDocumentWizardPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        AjaxLink<Object> link = new Link("link", context, config, this);
        add(link);

        final IPluginConfig localeConfig = getLocalizedPluginConfig(config);
        Label labelComponent;

        if (localeConfig != null) {
            String labelText = localeConfig.getString(PARAM_SHORTCUT_LINK_LABEL,
                                                      "Warning: label not found: " + PARAM_SHORTCUT_LINK_LABEL);
            labelComponent = new Label(PARAM_SHORTCUT_LINK_LABEL, Model.of(labelText));
        } else {
            labelComponent = new Label(PARAM_SHORTCUT_LINK_LABEL,
                    new StringResourceModel(PARAM_SHORTCUT_LINK_LABEL, this));
        }

        link.add(labelComponent);

        final HippoIcon icon = HippoIcon.fromSprite("shortcut-icon", Icon.PLUS);
        link.add(icon);
    }

    /**
     * Adds the dialog css to its html header.
     */
    @Override
    public void renderHead(final IHeaderResponse response) {
        response.render(new CssReferenceHeaderItem(WIZARD_CSS, null, null, null) {
            @Override
            public List<HeaderItem> getDependencies() {
                return Collections.singletonList(CmsHeaderItem.get());
            }
        });
    }

    private IPluginConfig getLocalizedPluginConfig(final IPluginConfig config) {
        Locale locale = getSession().getLocale();
        String localeString = getSession().getLocale().toString();
        IPluginConfig localeConfig = config.getPluginConfig(localeString);

        // just in case the locale contains others than language code, try to find it by language code again
        if (localeConfig == null && !StringUtils.equals(locale.getLanguage(), localeString)) {
            localeConfig = config.getPluginConfig(locale.getLanguage());
        }

        // if still not found, then try to find it by the default language again.
        if (localeConfig == null && !StringUtils.equals(DEFAULT_LANGUAGE, locale.getLanguage())) {
            localeConfig = config.getPluginConfig(DEFAULT_LANGUAGE);
        }

        return localeConfig;
    }

    /**
     * The link that opens a dialog window.
     */
    private class Link extends AjaxLink<Object> {

        private final IPluginContext context;
        private final IPluginConfig config;
        private final NewDocumentWizardPlugin parent;

        Link(final String id, final IPluginContext context, final IPluginConfig config, NewDocumentWizardPlugin parent) {
            super(id);
            this.context = context;
            this.config = config;
            this.parent = parent;
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            parent.getDialogService().show(getDialog(context, config, parent));
        }
    }

    protected Dialog getDialog(final IPluginContext context, final IPluginConfig config, NewDocumentWizardPlugin parent) {
        return new Dialog(context, config, parent);
    }

    /**
     * The dialog that opens after the user has clicked the dashboard link.
     */
    protected class Dialog extends org.hippoecm.frontend.dialog.Dialog<Object> {

        protected static final String DIALOG_NAME_LABEL = "name-label";
        protected static final String DIALOG_LIST_LABEL = "list-label";
        protected static final String DIALOG_DATE_LABEL = "date-label";

        private final IPluginContext context;
        private final IPluginConfig config;
        private final String documentType;
        private final String query;
        protected final String baseFolder;
        protected ClassificationType classificationType;

        protected static final String ERROR_SNS_NODE_EXISTS = "error-sns-node-exists";
        protected static final String ERROR_DISPLAY_NAME_EXISTS = "error-display-name-exists";

        protected String documentName;
        protected String list;
        protected Date date;

        /**
         * @param context plugin context
         * @param config  plugin config
         * @param parent  parent component (no longer used)
         */
        public Dialog(final IPluginContext context, final IPluginConfig config, @SuppressWarnings("unused") Component parent) {
            this.context = context;
            this.config = config;

            setSize(DialogConstants.MEDIUM_AUTO);
            setCssClass("new-document-wizard");

            // get values from the configuration
            documentType = config.getString(PARAM_DOCUMENT_TYPE);
            if (StringUtils.isBlank(getDocumentType())) {
                throw new IllegalArgumentException("Missing configuration parameter: " + PARAM_DOCUMENT_TYPE);
            }

            query = config.getString(PARAM_QUERY, DEFAULT_QUERY);

            final String baseFolderConfig = config.getString(PARAM_BASE_FOLDER, DEFAULT_BASE_FOLDER);
            if (baseFolderConfig.endsWith("/")) {
                baseFolder = baseFolderConfig;
            } else {
                baseFolder = baseFolderConfig + "/";
            }

            final String classification = config.getString(PARAM_CLASSIFICATION_TYPE);
            try {
                classificationType = ClassificationType.valueOf(StringUtils.upperCase(classification));
            } catch (Exception iae) {
                classificationType = DATE;
            }

            // build UI
            feedback = new FencedFeedbackPanel("feedback", this);
            replace(feedback);
            feedback.setOutputMarkupId(true);

            documentName = "";
            list = "";
            date = new Date();

            // get list value list
            IValueListProvider provider = getValueListProvider();
            ValueList categories;
            String valuelistPath = config.getString(PARAM_VALUE_LIST_PATH);
            try {
                categories = provider.getValueList(valuelistPath, null);
            } catch (IllegalStateException ise) {
                if (classificationType.equals(LIST) || classificationType.equals(LISTDATE)) {
                    log.warn("ValueList not found for parameter {} with value {}", PARAM_VALUE_LIST_PATH,
                            valuelistPath);
                }
                categories = new ValueList();
            }

            // add name text field
            final Label nameLabel = getLabel(DIALOG_NAME_LABEL, config);
            add(nameLabel);
            IModel<String> nameModel = new PropertyModel<>(this, "documentName");
            TextField<String> nameField = new TextField<>("name", nameModel);
            nameField.setRequired(true);
            nameField.setLabel(new StringResourceModel(DIALOG_NAME_LABEL, this));
            add(nameField);

            // add list dropdown field
            Label listLabel = getLabel(DIALOG_LIST_LABEL, config);
            add(listLabel);

            final PropertyModel<Object> propModel = new PropertyModel<>(this, "list");
            final IChoiceRenderer<Object> choiceRenderer = new ListChoiceRenderer(categories);
            DropDownChoice<Object> listField = new DropDownChoice<>("list", propModel, categories, choiceRenderer);
            listField.setRequired(true);
            listField.setLabel(new StringResourceModel(DIALOG_LIST_LABEL, this));
            add(listField);

            if (!classificationType.equals(LIST) && !classificationType.equals(LISTDATE)) {
                listLabel.setVisible(false);
                listField.setVisible(false);
            }

            // add date field
            final Label dateLabel = getLabel(DIALOG_DATE_LABEL, config);
            add(dateLabel);

            final YuiDateTimeField dateField = new YuiDateTimeField("date", new PropertyModel<>(this, "date"));
            dateField.setRequired(true);
            add(dateField);
            if (!classificationType.equals(DATE) && !classificationType.equals(LISTDATE)) {
                dateLabel.setVisible(false);
                dateField.setVisible(false);
            }
        }

        /**
         * Get a label from the plugin config, or from the Dialog properties file.
         *
         * @param labelKey the key under which the label is stored
         * @param config   the config of the plugin
         * @return a wicket Label
         */
        protected Label getLabel(final String labelKey, final IPluginConfig config) {
            final IPluginConfig localeConfig = getLocalizedPluginConfig(config);
            if (localeConfig != null) {
                final String label = localeConfig.getString(labelKey);
                if (StringUtils.isNotBlank(label)) {
                    return new Label(labelKey, label);
                }
            }
            return new Label(labelKey, new StringResourceModel(labelKey, this).setModel(null).setParameters(
            ));
        }

        /**
         * Gets the dialog title from the config.
         *
         * @return the label, or a warning if not found.
         */
        @Override
        public IModel<String> getTitle() {
            final IPluginConfig localeConfig = getLocalizedPluginConfig(config);
            if (localeConfig != null) {
                String label = localeConfig.getString(PARAM_SHORTCUT_LINK_LABEL);
                if (StringUtils.isNotBlank(label)) {
                    return Model.of(label);
                }
            }
            return new StringResourceModel(PARAM_SHORTCUT_LINK_LABEL, this);
        }

        @Override
        protected void onOk() {
            try {
                // get or create the folder node
                final HippoNode folder = getFolder();
                if (folder == null) {
                    error("Error occurred when trying to find or create the target folder.");
                    return;
                }

                // check if the to be created document not already exists
                final String existenceError = checkDocumentExistence(folder);
                if (existenceError != null) {
                    error(existenceError);
                    return;
                }

                final Session session = getSession().getJcrSession();
                final HippoWorkspace workspace = (HippoWorkspace) session.getWorkspace();
                final WorkflowManager workflowMgr = workspace.getWorkflowManager();

                // get the folder node's workflow
                final Workflow workflow = workflowMgr.getWorkflow("internal", folder);

                if (workflow instanceof FolderWorkflow) {
                    final FolderWorkflow fw = (FolderWorkflow) workflow;

                    final String encodedDocumentName = getNodeNameCodec().encode(documentName);

                    // create the new document
                    final Map<String, String> arguments = new TreeMap<>();
                    arguments.put("name", encodedDocumentName);

                    if (classificationType.equals(LIST) || classificationType.equals(LISTDATE)) {
                        arguments.put("list", list);
                        log.debug("Create document using for $list: {}", list);
                    }

                    if (classificationType.equals(DATE) || classificationType.equals(LISTDATE)) {
                        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd'T'kk:mm:ss.SSSZZ");
                        arguments.put("date", fmt.print(date.getTime()));
                        log.debug("Create document using for $date: {}", fmt.print(date.getTime()));
                    }

                    log.debug("Using query '{}', documentType '{}' and arguments '{}' to add document to folder {}",
                            getQuery(), getDocumentType(), arguments, folder.getPath());
                    final String path = fw.add(getQuery(), getDocumentType(), arguments);
                    final JcrNodeModel nodeModel = new JcrNodeModel(path);
                    final Node documentNode = processNewDocumentNode(nodeModel.getNode());

                    // add the not-encoded document name as display name
                    if (!documentName.equals(encodedDocumentName)) {

                        final DefaultWorkflow defaultWorkflow = (DefaultWorkflow) workflowMgr.getWorkflow("core",
                                documentNode);
                        if (defaultWorkflow != null) {
                            defaultWorkflow.setDisplayName(documentName);
                        }
                    }

                    // browse to new document
                    select(nodeModel);
                }
            } catch (RepositoryException | RemoteException | WorkflowException e) {
                log.error(e.getClass().getSimpleName() + " occurred while creating new document", e);
            }
        }

        protected String checkDocumentExistence(final HippoNode folderNode) throws RepositoryException {
            final String newNodeName = getNodeNameCodec().encode(documentName);
            final String newDisplayName = documentName;

            if (existsDisplayName(folderNode, newDisplayName)) {
                return new StringResourceModel(ERROR_DISPLAY_NAME_EXISTS, this)
                        .setParameters(Model.of(newDisplayName))
                        .getObject();
            } else if (existsNodeName(folderNode, newNodeName)) {
                return new StringResourceModel(ERROR_SNS_NODE_EXISTS, this)
                        .setParameters(Model.of(newNodeName))
                        .getObject();
            }
            return null;
        }

        protected boolean existsDisplayName(final HippoNode folderNode, final String displayName) throws RepositoryException {
            final NodeIterator children = folderNode.getNodes();
            while (children.hasNext()) {
                Node child = children.nextNode();
                if (child.isNodeType(HippoStdNodeType.NT_FOLDER) || child.isNodeType(HippoNodeType.NT_HANDLE)) {
                    String childDisplayName = new NodeNameModel(new JcrNodeModel(child)).getObject();
                    if (StringUtils.equals(childDisplayName, displayName)) {
                        return true;
                    }
                }
            }
            return false;
        }

        protected boolean existsNodeName(final HippoNode folderNode, final String nodeName) throws RepositoryException {
            return folderNode.hasNode(nodeName);
        }

        protected String getFolderPath() {
            String folderPath = null;
            if (classificationType.equals(LIST) || classificationType.equals(LISTDATE)) {
                folderPath = getListFolderPath();
            }
            if (classificationType.equals(DATE)) {
                folderPath = getDateFolderPath();
            }
            return folderPath;
        }

        protected String getListFolderPath() {
            if (list == null) {
                return null;
            }
            final String listEncoded = getNodeNameCodec().encode(list);
            return baseFolder + listEncoded;
        }

        protected String getDateFolderPath() {
            if (date == null) {
                return null;
            }
            final String yearMonth = new SimpleDateFormat("yyyy/MM").format(date);
            return baseFolder + yearMonth;
        }

        protected HippoNode getFolder() throws RepositoryException, RemoteException, WorkflowException {
            final String folderPath = getFolderPath();
            if (folderPath == null) {
                log.error("Could not create path to target folder.");
                return null;
            }
            return getOrCreateFolder(folderPath);
        }

        private HippoNode getOrCreateFolder(final String folderPath) throws RepositoryException, RemoteException, WorkflowException {
            final Session session = getSession().getJcrSession();
            final List<String> notExistingFolders = new ArrayList<>();
            String checkPath = folderPath;
            Node folderNode = null;

            // navigate up the tree to find the lowest already existing node in the path
            while (folderNode == null && StringUtils.isNotBlank(checkPath)) {
                if (session.nodeExists(checkPath)) {
                    folderNode = session.getNode(checkPath);
                } else {
                    notExistingFolders.add(StringUtils.substringAfterLast(checkPath, "/"));
                    checkPath = StringUtils.substringBeforeLast(checkPath, "/");
                }
            }

            // add any folder nodes that do not yet exist
            HippoNode folderHippoNode = (HippoNode) folderNode;
            Collections.reverse(notExistingFolders);
            for (String newFolder : notExistingFolders) {
                folderHippoNode = createFolder(folderHippoNode, newFolder);
            }

            return folderHippoNode;
        }

        protected IValueListProvider getValueListProvider() {
            return context.getService(DEFAULT_SERVICE_VALUE_LIST, IValueListProvider.class);
        }

        protected void select(JcrNodeModel nodeModel) {
            String browserId = config.getString(IBrowseService.BROWSER_ID);
            @SuppressWarnings("unchecked")
            IBrowseService<JcrNodeModel> browser = getPluginContext().getService(browserId, IBrowseService.class);
            if (browser != null) {
                browser.browse(nodeModel);
            } else {
                log.warn("no browser service found");
            }
        }

        protected String getQuery() {
            return query;
        }

        protected String getDocumentType() {
            return this.documentType;
        }

        /**
         * Hook to be able to modify a newly created document in a subclass, e.g. to set custom properties based on
         * custom UI parts.
         */
        protected Node processNewDocumentNode(Node documentNode) throws RepositoryException {
            return documentNode;
        }
    }

    protected StringCodec getNodeNameCodec() {
        final String locale = getSession().getLocale().toString();
        return CodecUtils.getNodeNameCodecModel(getPluginContext(), locale).getObject();
    }

    protected HippoNode createFolder(HippoNode parentNode, String name) throws RepositoryException, RemoteException, WorkflowException {
        Session session = getSession().getJcrSession();
        HippoWorkspace workspace = (HippoWorkspace) session.getWorkspace();
        WorkflowManager workflowMgr = workspace.getWorkflowManager();

        // get the folder node's workflow
        Workflow workflow = workflowMgr.getWorkflow("internal", parentNode);

        if (workflow instanceof FolderWorkflow) {
            FolderWorkflow fw = (FolderWorkflow) workflow;

            // create the new folder
            String category = "new-folder";
            NodeType[] mixinNodeTypes = parentNode.getMixinNodeTypes();
            for (NodeType mixinNodeType : mixinNodeTypes) {
                if (mixinNodeType.getName().equals(HippoTranslationNodeType.NT_TRANSLATED)) {
                    category = "new-translated-folder";
                    break;
                }
            }
            fw.add(category, HippoStdNodeType.NT_FOLDER, name);

            HippoNode newFolder = (HippoNode) parentNode.getNode(name);

            // give the new folder the same folder types as its parent
            Property parentFolderType = parentNode.getProperty("hippostd:foldertype");
            newFolder.setProperty("hippostd:foldertype", parentFolderType.getValues());

            // try to reorder the folder
            reorderFolder(fw, parentNode);

            return newFolder;
        } else {
            throw new WorkflowException("Workflow is not an instance of FolderWorkflow");
        }

    }

    @SuppressWarnings("unused")
    protected void reorderFolder(final FolderWorkflow folderWorkflow, final HippoNode parentNode) {
        // intentional stub
    }

    private static class ListChoiceRenderer implements IChoiceRenderer<Object> {
        private final ValueList list;

        public ListChoiceRenderer(ValueList list) {
            this.list = list;
        }

        @Override
        public Object getDisplayValue(Object object) {
            return list.getLabel(object);
        }

        @Override
        public String getIdValue(Object object, int index) {
            return list.getKey(object);
        }

        @Override
        public Object getObject(final String id, final IModel<? extends List<?>> choices) {
            final int index = list.indexOf(id);
            if (index >= 0) {
                return list.get(index);
            }
            return null;
        }
    }


}
