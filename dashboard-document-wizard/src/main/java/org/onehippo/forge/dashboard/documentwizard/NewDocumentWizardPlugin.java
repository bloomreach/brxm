/**
 * Copyright 2001-2013 Hippo B.V. (http://www.onehippo.com)
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

/**
 * Copyright 2001-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.validation.IErrorMessageSource;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.IValidator;
import org.hippoecm.frontend.CmsHeaderItem;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.AjaxDateTimeField;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.onehippo.forge.selection.frontend.model.ValueList;
import org.onehippo.forge.selection.frontend.provider.IValueListProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewDocumentWizardPlugin extends RenderPlugin<Object> implements IHeaderContributor {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(NewDocumentWizardPlugin.class);

    private static final String DEFAULT_LANGUAGE = "en";

    private static final ResourceReference WIZARD_CSS = new CssResourceReference(NewDocumentWizardPlugin.class, "NewDocumentWizardPlugin.css");

    private static final String PARAM_CLASSIFICATION_TYPE = "classificationType";
    private static final String PARAM_BASE_FOLDER = "baseFolder";
    private static final String PARAM_DOCUMENT_TYPE = "documentType";
    private static final String PARAM_QUERY = "query";
    private static final String PARAM_VALUELIST_PATH = "valueListPath";
    private static final String PARAM_SHORTCUT_LINK_LABEL = "shortcut-link-label";

    private static final String DEFAULT_QUERY = "new-document";
    private static final String DEFAULT_BASE_FOLDER = "/content/documents";
    private static final String DEFAULT_SERVICE_VALUELIST = "service.valuelist.default";

    private enum ClassificationType {DATE, LIST, LISTDATE}

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
            String labelText = localeConfig.getString(PARAM_SHORTCUT_LINK_LABEL, "Warning: label not found: " + PARAM_SHORTCUT_LINK_LABEL);
            labelComponent = new Label(PARAM_SHORTCUT_LINK_LABEL, new Model<String>(labelText));
        } else {
            labelComponent = new Label(PARAM_SHORTCUT_LINK_LABEL, new StringResourceModel(PARAM_SHORTCUT_LINK_LABEL, this, null));
        }

        link.add(labelComponent);
    }

    /**
     * Adds the dialog css to its html header.
     */
    @Override
    public void renderHead(final IHeaderResponse response) {
        response.render(new CssReferenceHeaderItem(WIZARD_CSS, null, null, null) {
            @Override
            public Iterable<? extends HeaderItem> getDependencies() {
                return Collections.singleton(CmsHeaderItem.get());
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
        private static final long serialVersionUID = 1L;

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
            parent.getDialogService().show(new Dialog(context, config, parent));
        }
    }

    /**
     * The dialog that opens after the user has clicked the dashboard link.
     */
    protected class Dialog extends AbstractDialog<Object> {

        private static final long serialVersionUID = 1L;
        private static final String DIALOG_NAME_LABEL = "name-label";
        private static final String DIALOG_LIST_LABEL = "list-label";
        private static final String DIALOG_DATE_LABEL = "date-label";
        private static final String DIALOG_HOURS_LABEL = "hours-label";
        private static final String DIALOG_MINUTES_LABEL = "minutes-label";

        private final IPluginContext context;
        private final IPluginConfig config;
        private final Component parent;
        private final String documentType;
        private final String query;
        private final String baseFolder;
        private ClassificationType classificationType;

        private String documentName;
        private String list;
        private Date date;

        /**
         * @param context plugin context
         * @param config  plugin config
         * @param parent  parent component
         */
        public Dialog(final IPluginContext context, final IPluginConfig config, Component parent) {
            this.context = context;
            this.config = config;
            this.parent = parent;

            // get values from the shortcut configuration
            documentType = config.getString(PARAM_DOCUMENT_TYPE);
            if (StringUtils.isBlank(documentType)) {
                throw new IllegalArgumentException("Missing configuration parameter: " + PARAM_DOCUMENT_TYPE);
            }
            query = config.getString(PARAM_QUERY, DEFAULT_QUERY);
            baseFolder = config.getString(PARAM_BASE_FOLDER, DEFAULT_BASE_FOLDER);
            final String classification = config.getString(PARAM_CLASSIFICATION_TYPE);
            try {
                classificationType = ClassificationType.valueOf(StringUtils.upperCase(classification));
            } catch (Exception iae) {
                classificationType = ClassificationType.DATE;
            }

            feedback = new FeedbackPanel("feedback");
            replace(feedback);
            feedback.setOutputMarkupId(true);

            documentName = "";
            list = "";
            date = new Date();

            // get list value list
            IValueListProvider provider = getValueListProvider();
            ValueList categories;
            String valuelistPath = config.getString(PARAM_VALUELIST_PATH);
            try {
                categories = provider.getValueList(valuelistPath);
            } catch (IllegalStateException ise) {
                if (classificationType.equals(ClassificationType.LIST) || classificationType.equals(ClassificationType.LISTDATE)) {
                    log.warn("ValueList not found for parameter " + PARAM_VALUELIST_PATH + " with value " + valuelistPath);
                }
                categories = new ValueList();
            }

            // add name text field
            final Label nameLabel = getLabel(DIALOG_NAME_LABEL, config);
            add(nameLabel);
            IModel<String> nameModel = new PropertyModel<String>(this, "documentName");
            TextField<String> nameField = new TextField<String>("name", nameModel);
            nameField.setRequired(true);
            final StringResourceModel errorMsgModel = new StringResourceModel("invalid.name", this, null);
            nameField.add(new IValidator<String>() {
                public void validate(final IValidatable<String> strValue) {
                    String value = strValue.getValue();
                    if (!isValidName(value)) {
                        strValue.error(new IValidationError() {
                            public String getErrorMessage(final IErrorMessageSource messageSource) {                                
                                return errorMsgModel.getString();
                            }
                        });
                    }
                }
            });
            nameField.setLabel(new StringResourceModel(DIALOG_NAME_LABEL, this, null));
            add(nameField);

            // add list dropdown field
            Label listLabel = getLabel(DIALOG_LIST_LABEL, config);
            add(listLabel);

            final PropertyModel<Object> propModel = new PropertyModel<Object>(this, "list");
            final IChoiceRenderer<Object> choiceRenderer = new ListChoiceRenderer(categories);
            DropDownChoice<Object> listField = new DropDownChoice<Object>("list", propModel, categories, choiceRenderer);
            listField.setRequired(true);
            listField.setLabel(new StringResourceModel(DIALOG_LIST_LABEL, this, null));
            add(listField);

            if (!classificationType.equals(ClassificationType.LIST) && !classificationType.equals(ClassificationType.LISTDATE)) {
                listLabel.setVisible(false);
                listField.setVisible(false);
            }

            // add date field
            final Label dateLabel = getLabel(DIALOG_DATE_LABEL, config);
            AjaxDateTimeField dateField = new AjaxDateTimeField("date", new PropertyModel<Date>(this, "date"), true);
            dateField.setRequired(true);
            final StringResourceModel dateLabelModel = new StringResourceModel(DIALOG_DATE_LABEL, this, null);
            dateField.setLabel(dateLabelModel);
            ((DateTextField)dateField.get("date")).setLabel(dateLabelModel);
            ((TextField)dateField.get("hours")).setLabel(new StringResourceModel(DIALOG_HOURS_LABEL, this, null));
            ((TextField)dateField.get("minutes")).setLabel(new StringResourceModel(DIALOG_MINUTES_LABEL, this, null));
            add(dateLabel);
            add(dateField);
            if (!classificationType.equals(ClassificationType.DATE) && !classificationType.equals(ClassificationType.LISTDATE)) {
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
        private Label getLabel(final String labelKey, final IPluginConfig config) {
            final IPluginConfig localeConfig = getLocalizedPluginConfig(config);
            if (localeConfig != null) {
                final String label = localeConfig.getString(labelKey);
                if (StringUtils.isNotBlank(label)) {
                    return new Label(labelKey, label);
                }
            }
            return new Label(labelKey, new StringResourceModel(labelKey, this, null));
        }

        /**
         * Gets the dialog title from the config.
         *
         * @return the label, or a warning if not found.
         */
        public IModel<String> getTitle() {
            final IPluginConfig localeConfig = getLocalizedPluginConfig(config);
            if (localeConfig != null) {
                String label = localeConfig.getString(PARAM_SHORTCUT_LINK_LABEL);
                if (StringUtils.isNotBlank(label)) {
                    return new Model<String>(label);
                }
            }
            return new StringResourceModel(PARAM_SHORTCUT_LINK_LABEL, this, null);
        }

        @Override
        public IValueMap getProperties() {
            return MEDIUM;
        }

        @Override
        protected void onOk() {
            Session session = ((UserSession) getSession()).getJcrSession();
            HippoWorkspace workspace = (HippoWorkspace) session.getWorkspace();
            try {
                WorkflowManager workflowMgr = workspace.getWorkflowManager();

                // get the folder node
                HippoNode folderNode = (HippoNode) session.getItem(baseFolder);

                if (classificationType.equals(ClassificationType.LIST) || classificationType.equals(ClassificationType.LISTDATE)) {
                    folderNode = listFolder(folderNode, list);
                }

                if (classificationType.equals(ClassificationType.DATE) || classificationType.equals(ClassificationType.LISTDATE)) {
                    folderNode = createDateFolders(folderNode, date);
                }

                // get the folder node's workflow
                Workflow workflow = workflowMgr.getWorkflow("internal", folderNode);

                if (workflow instanceof FolderWorkflow) {
                    FolderWorkflow fw = (FolderWorkflow) workflow;

                    // create the new document
                    String encodedDocumentName = getNodeNameCodec().encode(documentName);
                    Map<String, String> arguments = new TreeMap<String, String>();
                    arguments.put("name", encodedDocumentName);
                    if (classificationType.equals(ClassificationType.LIST) || classificationType.equals(ClassificationType.LISTDATE)) {
                        arguments.put("list", list);
                        log.debug("Create document using for $list: " + list);
                    }
                    if (classificationType.equals(ClassificationType.DATE) || classificationType.equals(ClassificationType.LISTDATE)) {
                        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd'T'kk:mm:ss.SSSZZ");
                        arguments.put("date", fmt.print(date.getTime()));
                        log.debug("Create document using for $date: " + fmt.print(date.getTime()));
                    }
                    log.debug("Query used: " + query);
                    String path = fw.add(query, documentType, arguments);

                    Node document = folderNode.getNode(encodedDocumentName);
                    JcrNodeModel nodeModel = new JcrNodeModel(new JcrItemModel(path));
                    select(nodeModel);

                    // add the not-encoded document name as translation
                    if (!documentName.equals(encodedDocumentName)) {
                        DefaultWorkflow defaultWorkflow = (DefaultWorkflow) workflowMgr.getWorkflow("core", nodeModel.getNode());
                        if (defaultWorkflow != null) {
                            defaultWorkflow.localizeName(documentName);
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.error("Error occurred while creating new document: "
                        + e.getClass().getName() + ": " + e.getMessage());
            } catch (RemoteException e) {
                log.error("Error occurred while creating new document: "
                        + e.getClass().getName() + ": " + e.getMessage());
            } catch (WorkflowException e) {
                log.error("Error occurred while creating new document: "
                        + e.getClass().getName() + ": " + e.getMessage());
            }

        }

        @Override
        protected void onValidate() {
            super.onValidate();
        }

        private IPluginContext getPluginContext() {
            return context;
        }

        private IValueListProvider getValueListProvider() {
            return getPluginContext().getService(DEFAULT_SERVICE_VALUELIST, IValueListProvider.class);
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

    }

    /**
     * Determine whether the a document name is valid.
     *
     * @param value the document name
     * @return whether the name is a valid JCR nodename
     */
    protected static boolean isValidName(final String value) {
        if (!value.trim().equals(value)) {
            return false;
        }
        if (".".equals(value) || "..".equals(value)) {
            return false;
        }
        return value.matches("[^\\[\\]\\|/:\\}\\{]+");
    }

    protected StringCodec getNodeNameCodec() {
        return new StringCodecFactory.UriEncoding();
    }


    /**
     * Get or create folder for classificationType.LIST.
     *
     * @param parentNode
     * @param list
     * @return
     * @throws java.rmi.RemoteException
     * @throws javax.jcr.RepositoryException
     * @throws org.hippoecm.repository.api.WorkflowException
     *
     */
    protected HippoNode listFolder(HippoNode parentNode, String list) throws RemoteException, RepositoryException, WorkflowException {
        String listEncoded = getNodeNameCodec().encode(list);
        HippoNode resultParentNode = parentNode;
        if (resultParentNode.hasNode(listEncoded)) {
            resultParentNode = (HippoNode) resultParentNode.getNode(listEncoded);
        } else {
            final String listEncodedLowerCase = listEncoded.toLowerCase(getLocale());
            if (resultParentNode.hasNode(listEncodedLowerCase)) {
                resultParentNode = (HippoNode) resultParentNode.getNode(listEncodedLowerCase);
            } else {
                resultParentNode = createFolder(resultParentNode, listEncoded);
            }
        }
        return resultParentNode;
    }

    /**
     * Get or create folder(s) for classificationType.DATE.
     *
     * @param parentNode
     * @param date
     * @return
     * @throws java.rmi.RemoteException
     * @throws javax.jcr.RepositoryException
     * @throws org.hippoecm.repository.api.WorkflowException
     *
     */
    protected HippoNode createDateFolders(HippoNode parentNode, Date date) throws RemoteException, RepositoryException, WorkflowException {
        String year = new SimpleDateFormat("yyyy").format(date);
        HippoNode resultParentNode = parentNode;
        if (resultParentNode.hasNode(year)) {
            resultParentNode = (HippoNode) resultParentNode.getNode(year);
        } else {
            resultParentNode = createFolder(resultParentNode, year);
        }

        String month = new SimpleDateFormat("MM").format(date);
        if (resultParentNode.hasNode(month)) {
            resultParentNode = (HippoNode) resultParentNode.getNode(month);
        } else {
            resultParentNode = createFolder(resultParentNode, month);
        }

        return resultParentNode;
    }

    protected HippoNode createFolder(HippoNode parentNode, String name) throws RepositoryException, RemoteException, WorkflowException {
        Session session = ((UserSession) getSession()).getJcrSession();
        HippoWorkspace workspace = (HippoWorkspace) session.getWorkspace();
        WorkflowManager workflowMgr = workspace.getWorkflowManager();

        // get the folder node's workflow
        Workflow workflow = workflowMgr.getWorkflow("internal", parentNode);

        if (workflow instanceof FolderWorkflow) {
            FolderWorkflow fw = (FolderWorkflow) workflow;

            // create the new folder
            String category = "new-folder";
            NodeType[] mixinNodeTypes = parentNode.getMixinNodeTypes();
            for (int i = 0; i < mixinNodeTypes.length; i++) {
                NodeType mixinNodeType = mixinNodeTypes[i];
                if (mixinNodeType.getName().equals("hippotranslation:translated")) {
                    category = "new-translated-folder";
                    break;
                }
            }
            fw.add(category, "hippostd:folder", name);

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

    protected void reorderFolder(final FolderWorkflow folderWorkflow, final HippoNode parentNode) {
        // intentional stub
    }

    private static class ListChoiceRenderer implements IChoiceRenderer<Object> {
        private static final long serialVersionUID = 1L;
        private final ValueList list;

        public ListChoiceRenderer(ValueList list) {
            this.list = list;
        }

        public Object getDisplayValue(Object object) {
            return list.getLabel(object);
        }

        public String getIdValue(Object object, int index) {
            return list.getKey(object);
        }

    }

}