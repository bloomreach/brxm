/*
 *  Copyright 2010-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.translation.workflow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.MenuDescription;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.addon.workflow.WorkflowSNSException;
import org.hippoecm.editor.type.JcrTypeStore;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIconStack;
import org.hippoecm.frontend.plugins.standards.image.CachingImage;
import org.hippoecm.frontend.plugins.standardworkflow.validators.SameNameSiblingsUtil;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.CmsIcon;
import org.hippoecm.frontend.translation.DocumentTranslationProvider;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;
import org.hippoecm.frontend.translation.ILocaleProvider.LocaleState;
import org.hippoecm.frontend.translation.TranslationUtil;
import org.hippoecm.frontend.translation.components.document.FolderTranslation;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.util.CodecUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.translation.HippoTranslatedNode;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.hippoecm.repository.translation.TranslationWorkflow;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.translate.TranslateWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

public final class TranslationWorkflowPlugin extends RenderPlugin<WorkflowDescriptor> {

    private static final Logger log = LoggerFactory.getLogger(TranslationWorkflowPlugin.class);

    private static final String TRANSLATE = "translate";
    private static final String COULD_NOT_CREATE_FOLDERS = "could-not-create-folders";

    /** Model that collects the workflow instances of the variants of the document and unites the information. */
    private TranslationsModel translationsModel;
    private DocumentTranslationProvider translationProvider;

    public TranslationWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final IModel<String> languageModel = new LanguageModel();
        final ILocaleProvider localeProvider = getLocaleProvider();

        Node documentNode = null;
        try {
            documentNode = getDocumentNode();
            translationProvider = new DocumentTranslationProvider(new JcrNodeModel(documentNode),
                    localeProvider);
        } catch (RepositoryException e) {
            log.warn("Unable to find document node");
        }
        if (documentNode == null) {
            return;
        }

        try {
            final String language = languageModel.getObject();
            final boolean hasTranslatableFolder = TranslationUtil.isNtTranslated(documentNode.getParent().getParent());
            final boolean isTranslatableVariant = TranslationUtil.isNtTranslated(documentNode);
            final boolean isKnownLanguage = localeProvider.isKnown(language);
            if (!hasTranslatableFolder && (!isTranslatableVariant || !isKnownLanguage)) {
                return;
            }
        } catch (RepositoryException e) {
            log.warn("Could not determine translations status of document", e);
        }

        final EmptyPanel emptyPanel = new EmptyPanel("content");
        add(emptyPanel);

        try {
            final Node handleNode = documentNode.getParent();
            final String identifier = handleNode.getIdentifier();
            loadTranslationsModel(identifier);
            translationsModel.addWorkflowDescriptorModel((WorkflowDescriptorModel) getDefaultModel());
            addMenuDescription(localeProvider, languageModel, identifier);
        } catch (RepositoryException e) {
            log.warn("Could not determine identifier of handle for node : { path : {} }"
                    , JcrUtils.getNodePathQuietly(documentNode));
        }
    }

    private static void collectFields(final String relPath, final String nodeType, final Set<String> plainTextFields
            , final Set<String> richTextFields) {
        try {
            final JcrTypeStore jcrTypeStore = new JcrTypeStore();
            final ITypeDescriptor type = jcrTypeStore.load(nodeType);
            for (Map.Entry<String, IFieldDescriptor> field : type.getFields().entrySet()) {
                final IFieldDescriptor fieldDescriptor = field.getValue();
                if ("*".equals(fieldDescriptor.getPath())) {
                    continue;
                }
                final ITypeDescriptor fieldType = fieldDescriptor.getTypeDescriptor();
                final String fieldPath = fieldPath(relPath, fieldDescriptor);
                if (fieldType.getType().equals(HippoStdNodeType.NT_HTML)) {
                    final String propertyPath = fieldPath + '/' + HippoStdNodeType.HIPPOSTD_CONTENT;
                    richTextFields.add(propertyPath);
                } else if (fieldType.getName().equals("Text")) {
                    plainTextFields.add(fieldPath);
                } else if (fieldType.getName().equals("Html")) {
                    richTextFields.add(fieldPath);
                } else if (fieldType.isNode()) {
                    collectFields(fieldPath, fieldType.getType(), plainTextFields, richTextFields);
                }
            }
        } catch (StoreException ex) {
            // ignore nt:base
        }
    }

    private static String fieldPath(final String basePath, final IFieldDescriptor fieldDescriptor) {
        return (basePath != null ? basePath + '/' : "") + fieldDescriptor.getPath();
    }

    private void loadTranslationsModel(final String identifier) {
        IPluginContext context = getPluginContext();
        String referenceModelIdentifier = TranslationsModel.class.getName() + "." + identifier + "."
                + UserSession.get().getId();
        final ModelReference<Translations> service = context.getService(referenceModelIdentifier, ModelReference.class);
        if (service == null) {
            translationsModel = new TranslationsModel();
            ModelReference<Translations> translationsModelReference =
                    new ModelReference<>(referenceModelIdentifier, translationsModel);
            translationsModelReference.init(context);
        } else {
            translationsModel = (TranslationsModel) context.getReference(service).getService().getModel();
        }
    }

    private void addMenuDescription(final ILocaleProvider localeProvider, final IModel<String> languageModel
            , final String identifier) {
        IPluginContext context = getPluginContext();
        String referenceModelIdentifier = TranslationMenuDescription.class.getName() + "." + identifier  + "."
                + UserSession.get().getId();
        final ModelReference service = context.getService(referenceModelIdentifier, ModelReference.class);
        if (service == null) {
            ModelReference<Boolean> translationsModelReference = new ModelReference<>(referenceModelIdentifier
                    , new Model(Boolean.TRUE));
            translationsModelReference.init(context);
            TranslationMenuDescription translationMenuDescription = new TranslationMenuDescription(localeProvider
                    , languageModel);
            add(translationMenuDescription);
        }
    }

    public boolean hasLocale(final String locale) {
        return translationProvider != null && translationProvider.contains(locale);
    }

    private Node getDocumentNode() throws RepositoryException {
        WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
        if (wdm != null) {
            return wdm.getNode();
        }
        return null;
    }

    @Override
    public WorkflowDescriptor getModelObject() {
        WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
        if (wdm != null) {
            return wdm.getObject();
        }
        return null;
    }

    protected ILocaleProvider getLocaleProvider() {
        return getPluginContext().getService(
                getPluginConfig().getString(ILocaleProvider.SERVICE_ID, ILocaleProvider.class.getName()),
                ILocaleProvider.class);
    }

    @SuppressWarnings("unchecked")
    protected IBrowseService<IModel<Node>> getBrowserService() {
        final String serviceId = getPluginConfig().getString(IBrowseService.BROWSER_ID, "service.browse");
        return getPluginContext().getService(serviceId, IBrowseService.class);
    }

    protected StringCodec getLocalizeCodec() {
        return CodecUtils.getDisplayNameCodec(getPluginContext());
    }

    @Override
    protected void onDetach() {
        if (translationProvider != null) {
            translationProvider.detach();
        }
        super.onDetach();
    }

    private static class TranslatedFolder {
        private final Node node;

        TranslatedFolder(Node node) {
            this.node = node;
        }

        TranslatedFolder getParent() throws RepositoryException {
            Node ancestor = node;
            do {
                ancestor = ancestor.getParent();
                if ("/content/documents".equals(ancestor.getPath())) {
                    return null;
                }
            } while (!ancestor.isNodeType(HippoTranslationNodeType.NT_TRANSLATED));
            return new TranslatedFolder(ancestor);
        }

        TranslatedFolder getSibling(String locale) throws RepositoryException {
            HippoTranslatedNode translatedNode = new HippoTranslatedNode(node);
            try {
                return new TranslatedFolder(translatedNode.getTranslation(locale));
            } catch (ItemNotFoundException e) {
                return null;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TranslatedFolder)) {
                return false;
            }
            try {
                return ((TranslatedFolder) obj).node.isSame(node);
            } catch (RepositoryException e) {
                throw new IllegalStateException("could not determine whether nodes are equivalent", e);
            }
        }

        @Override
        public int hashCode() {
            try {
                return node.getPath().hashCode();
            } catch (RepositoryException e) {
                throw new IllegalStateException("could not determine path of node", e);
            }
        }
    }

    private final class LanguageModel extends LoadableDetachableModel<String> {
        @Override
        protected String load() {
            WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) TranslationWorkflowPlugin.this.getDefaultModel();
            if (wdm != null) {
                try {
                    Node documentNode = wdm.getNode();
                    return documentNode.getProperty(HippoTranslationNodeType.LOCALE).getString();
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
            return "unknown";
        }
    }

    private final class AvailableLocaleProvider implements IDataProvider<HippoLocale> {
        private final ILocaleProvider localeProvider;
        private transient List<HippoLocale> availableLocales;

        private AvailableLocaleProvider(ILocaleProvider localeProvider) {
            this.localeProvider = localeProvider;
        }

        private void load() {
            availableLocales = new LinkedList<>();
            final Locale locale = getLocale();
            final Translations translations = translationsModel.getObject();
            availableLocales = translations.getAvailableTranslations().stream()
                    .map(localeProvider::getLocale)
                    .sorted(Comparator.comparing(o -> o.getDisplayName(locale)))
                    .collect(Collectors.toList());
        }

        @Override
        public Iterator<? extends HippoLocale> iterator(long first, long count) {
            if (availableLocales == null) {
                load();
            }
            return availableLocales.subList((int) first, (int) (first + count)).iterator();
        }

        @Override
        public IModel<HippoLocale> model(HippoLocale object) {
            final String id = object.getName();
            return new LoadableDetachableModel<HippoLocale>() {

                @Override
                protected HippoLocale load() {
                    return localeProvider.getLocale(id);
                }

            };
        }

        @Override
        public long size() {
            if (availableLocales == null) {
                load();
            }
            return availableLocales.size();
        }

        public void detach() {
            availableLocales = null;
        }

        @SuppressWarnings("unchecked")
        private Set<String> getAvailableLanguages() {
            WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) TranslationWorkflowPlugin.this.getDefaultModel();
            if (wdm != null) {
                WorkflowDescriptor descriptor = wdm.getObject();
                WorkflowManager manager = UserSession.get().getWorkflowManager();
                try {
                    TranslationWorkflow translationWorkflow = (TranslationWorkflow) manager.getWorkflow(descriptor);
                    return (Set<String>) translationWorkflow.hints().get("available");
                } catch (RepositoryException | RemoteException | WorkflowException ex) {
                    log.error("Failed to retrieve available languages", ex);
                }
            }
            return Collections.emptySet();
        }
    }

    private final class TranslationAction extends StdWorkflow<TranslationWorkflow> {

        private final String language;
        private final IModel<String> languageModel;
        private final IModel<HippoLocale> localeModel;
        private final IModel<String> title;
        private boolean autoTranslateContent;
        private String name;
        private String url;
        private List<FolderTranslation> folders;

        private TranslationAction(final String id, final IModel<String> name, final IModel<HippoLocale> localeModel
                , final String language, final IModel<String> languageModel) {
            super(id, name, getPluginContext(), (WorkflowDescriptorModel) TranslationWorkflowPlugin.this.getModel());
            this.language = language;
            this.title = name;
            this.languageModel = languageModel;
            this.localeModel = localeModel;
        }

        @Override
        protected Component getIcon(final String id) {
            final HippoLocale hippoLocale = localeModel.getObject();
            final HippoIconStack nodeIcon = new HippoIconStack(id, IconSize.M);

            final ResourceReference flagIcon = hippoLocale.getIcon(IconSize.M, LocaleState.EXISTS);
            nodeIcon.addFromResource(flagIcon);

            if (!hasLocale(hippoLocale.getName())) {
                nodeIcon.addFromCms(CmsIcon.OVERLAY_PLUS, IconSize.M, HippoIconStack.Position.TOP_LEFT);
            }

            return nodeIcon;
        }

        @Override
        protected IModel<String> getTitle() {
            return title;
        }

        @Override
        protected String execute(TranslationWorkflow wf) throws Exception {
            if (hasLocale(language)) {
                return executeAvailableTransaction();
            } else {
                return executeNonAvailableTranslation(wf);
            }
        }

        private String executeAvailableTransaction() {
            final IBrowseService<IModel<Node>> browser = getBrowserService();
            if (browser != null) {
                final WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) TranslationWorkflowPlugin.this.getDefaultModel();
                if (wdm != null) {
                    Node node;
                    try {
                        node = wdm.getNode();
                        if (node != null) {
                            HippoTranslatedNode translatedNode = new HippoTranslatedNode(node);
                            Node translation = translatedNode.getTranslation(language);
                            browser.browse(new JcrNodeModel(translation.getParent()));
                        } else {
                            log.error("No node found for document");
                        }
                    } catch (RepositoryException e) {
                        log.error("Error retrieving translation node", e);
                    }
                } else {
                    log.error("No workflow descriptor model for document");
                }
            } else {
                log.warn("Cannot navigate to translation - configured browser.id '{}' is invalid." ,getPluginConfig().getString( "browser.id"));
            }
            return null;
        }

        protected String executeNonAvailableTranslation(TranslationWorkflow workflow)
                throws WorkflowException, RepositoryException, RemoteException {
            javax.jcr.Session session = UserSession.get().getJcrSession();

            // Find the index of the deepest translated folder.
            // The caller is to guarantee that at least the root node is translated (hence starting i at 1),
            // and that there is a document handle node at the end of the list (representing the to-be-translated document).
            final int indexOfDeepestFolder = folders.size() - 1;
            int i = 1;
            while (i < indexOfDeepestFolder && !folders.get(i).isEditable()) {
                i++;
            }

            int indexOfDeepestTranslatedFolder = i - 1;
            avoidSameNameSiblings(session, indexOfDeepestTranslatedFolder);

            // Try to create new target folders for all not yet translated source folders
            for (; i < indexOfDeepestFolder; i++) {
                if (!saveFolder(folders.get(i), session)) {
                    return COULD_NOT_CREATE_FOLDERS;
                }
            }

            FolderTranslation docTranslation = folders.get(folders.size() - 1);
            this.name = docTranslation.getNamefr();
            this.url = docTranslation.getUrlfr();

            Document translatedDocument = workflow.addTranslation(language, url);
            Document translatedVariant = getTranslatedVariant(translatedDocument);

            try {
                WorkflowManager manager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                if (autoTranslateContent && translatedVariant != null) {
                    Workflow translateWorkflow = manager.getWorkflow(TRANSLATE, translatedVariant);
                    if (translateWorkflow instanceof TranslateWorkflow) {
                        Set<String> plainTextFields = new TreeSet<>();
                        Set<String> richTextFields = new TreeSet<>();
                        Set<String> allTextFields = new TreeSet<>();
                        String primaryNodeTypeName = session.getNodeByIdentifier(
                                translatedVariant.getIdentity()).getPrimaryNodeType().getName();
                        collectFields(null, primaryNodeTypeName, plainTextFields, richTextFields);
                        allTextFields.addAll(plainTextFields);
                        allTextFields.addAll(richTextFields);
                        ((TranslateWorkflow) translateWorkflow).translate(languageModel.getObject(), language, allTextFields);
                    }
                }
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", translatedDocument);
                if (name != null && !url.equals(name)) {
                    String displayName = getLocalizeCodec().encode(name);
                    defaultWorkflow.setDisplayName(displayName);
                }
            } finally {
                final IBrowseService<IModel<Node>> browser = getBrowserService();
                if (browser != null) {
                    browser.browse(new JcrNodeModel(session.getNodeByIdentifier(translatedDocument.getIdentity())));
                } else {
                    log.warn("Cannot open newly created document - configured browser.id '{}' is invalid."
                            , getPluginConfig().getString( "browser.id"));
                }
            }
            return null;
        }

        /**
         * Prevent the creation of same-name-sibling (SNS) folders when translating a document (or folder?).
         * This affects
         * <p>
         * 1) the case where the deepest existing folder already has a child node with the same (node-)name
         * 2) the case where the deepest existing folder already has a child node with the same localized name
         * <p>
         * An exception of type {@link WorkflowSNSException} will be thrown if there is an SNS issue.
         */
        private void avoidSameNameSiblings(final Session session, final int indexOfDeepestTranslatedFolder)
                throws WorkflowSNSException, RepositoryException {

            final FolderTranslation deepestTranslatedFolder = folders.get(indexOfDeepestTranslatedFolder);
            final Node deepestTranslatedSourceNode = session.getNodeByIdentifier(deepestTranslatedFolder.getId());
            final Node deepestTranslatedTargetNode = new HippoTranslatedNode(deepestTranslatedSourceNode).getTranslation(language);

            if (deepestTranslatedTargetNode == null) {
                // this means that there's a programmatic problem in the construction ot the "folders" list.
                log.error("Invalid deepestTranslatedNode parameter. Target translation node for '{}' could not be found.",
                        deepestTranslatedFolder.getName());
                return;
            }
            // highest untranslated item can be folder OR document
            final FolderTranslation highestUntranslatedItem = folders.get(indexOfDeepestTranslatedFolder + 1);
            String targetUrlName = highestUntranslatedItem.getUrlfr();
            String targetLocalizedName = highestUntranslatedItem.getNamefr();
            if (deepestTranslatedTargetNode.hasNode(targetUrlName)) {
                throw new WorkflowSNSException("A folder or document with name '" + targetUrlName + "' already exists", targetUrlName);
            }
            // check for duplicated localized name
            if (SameNameSiblingsUtil.hasChildWithDisplayName(deepestTranslatedTargetNode, targetLocalizedName)) {
                throw new WorkflowSNSException("A folder or document with localized name '" + targetLocalizedName + "' already exists", targetLocalizedName);
            }
            // No SNS issue!
        }

        private Document getTranslatedVariant(final Document translatedDocument) throws RepositoryException {
            final Node translatedNode = translatedDocument.getNode(UserSession.get().getJcrSession());
            if (translatedNode.isNodeType(NT_HANDLE)) {
                Node variant = null;
                for (Node node : new NodeIterable(translatedNode.getNodes(translatedNode.getName()))) {
                    variant = node;
                    final String state = JcrUtils.getStringProperty(translatedNode, HIPPOSTD_STATE, null);
                    if (UNPUBLISHED.equals(state)) {
                        break;
                    }
                }
                return variant != null ? new Document(variant) : null;
            }
            return translatedDocument;
        }

        @Override
        protected Dialog createRequestDialog() {
            if (hasLocale(language)) {
                return null;
            }
            try {
                Node docNode = ((WorkflowDescriptorModel) TranslationWorkflowPlugin.this.getDefaultModel()).getNode();
                url = docNode.getName();
                name = url;
                if (docNode instanceof HippoNode) {
                    name = ((HippoNode) docNode).getDisplayName();
                }
                folders = new LinkedList<>();
                Node handle = docNode.getParent();
                FolderTranslation docTranslation = JcrFolderTranslationFactory.createFolderTranslation(handle, null);
                folders.add(docTranslation);

                populateFolders(handle);

                IModel<Boolean> autoTranslateModel = null;
                autoTranslateContent = false;
                WorkflowManager manager = ((HippoWorkspace) docNode.getSession().getWorkspace()).getWorkflowManager();
                WorkflowDescriptor workflow = manager.getWorkflowDescriptor(TRANSLATE, docNode);
                if (workflow != null) {
                    final Serializable available = workflow.hints().get(TRANSLATE);
                    if (available != null && (Boolean) available) {
                        autoTranslateModel = new PropertyModel<>(TranslationAction.this, "autoTranslateContent");
                    }
                }

                ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID,
                        ISettingsService.class);
                StringResourceModel titleModel = new StringResourceModel("translate-title", TranslationWorkflowPlugin.this);
                return new DocumentTranslationDialog(settingsService, this, titleModel,
                        folders, autoTranslateModel, languageModel.getObject(), language,
                        getLocaleProvider());
            } catch (Exception e) {
                log.error("Error creating document translation dialog (" + e.getMessage() + ")", e);
                error(e.getMessage());
            }
            return null;
        }

        private void populateFolders(Node handle) throws RepositoryException {
            Node sourceFolder = handle;
            try {
                while (!sourceFolder.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                    sourceFolder = sourceFolder.getParent();
                }
            } catch (ItemNotFoundException infe) {
                log.warn("Parent folder of translatable document could not be found", infe);
                return;
            } catch (AccessDeniedException ade) {
                log.warn("Parent folder of translatable document is not accessible", ade);
                return;
            }

            TranslatedFolder sourceTranslatedFolder = new TranslatedFolder(sourceFolder);

            // walk up the source tree until a translated ancestor is found
            while (sourceTranslatedFolder.getSibling(language) == null) {
                FolderTranslation ft = JcrFolderTranslationFactory.createFolderTranslation(sourceTranslatedFolder.node,
                        null);
                ft.setEditable(true);
                folders.add(ft);

                sourceTranslatedFolder = sourceTranslatedFolder.getParent();
                if (sourceTranslatedFolder == null) {
                    break;
                }
            }
            if (sourceTranslatedFolder == null) {
                throw new RepositoryException("Unable to find root folder for language " + language);
            }

            TranslatedFolder targetTranslatedFolder = sourceTranslatedFolder.getSibling(language);
            assert targetTranslatedFolder != null;
            findSourceTranslatedFolder(sourceTranslatedFolder, targetTranslatedFolder);
            Collections.reverse(folders);
        }

        private void findSourceTranslatedFolder(final TranslatedFolder sourceTranslatedFolder
                , final TranslatedFolder targetTranslatedFolder) throws RepositoryException {
            if (sourceTranslatedFolder != null) {
                FolderTranslation ft = JcrFolderTranslationFactory.createFolderTranslation(
                        sourceTranslatedFolder.node, targetTranslatedFolder.node);
                ft.setEditable(false);
                folders.add(ft);

                // walk up the source tree until a translated ancestor is found
                TranslatedSourceAncestorFinder translatedSourceAncestorFinder = new TranslatedSourceAncestorFinder(sourceTranslatedFolder).invoke();
                if (translatedSourceAncestorFinder.isFound()) {
                    return;
                }
                TranslatedFolder sourceSibling = translatedSourceAncestorFinder.getSourceSibling();
                findSourceTranslatedFolder(translatedSourceAncestorFinder.getSourceTranslatedFolder(),
                        findTranslatedTargetAncestor(targetTranslatedFolder, sourceSibling));
            }
        }

        private TranslatedFolder findTranslatedTargetAncestor(TranslatedFolder targetTranslatedFolder, final TranslatedFolder sourceSibling) throws RepositoryException {
            targetTranslatedFolder = targetTranslatedFolder.getParent();
            if (targetTranslatedFolder != null && targetTranslatedFolder.equals(sourceSibling)) {
                return targetTranslatedFolder;
            }
            while (targetTranslatedFolder != null) {
                TranslatedFolder backLink = targetTranslatedFolder.getSibling(languageModel.getObject());
                if ((backLink != null)) {
                    break;
                }

                FolderTranslation ft2 = JcrFolderTranslationFactory.createFolderTranslation(null,
                        targetTranslatedFolder.node);
                ft2.setEditable(false);
                folders.add(ft2);

                targetTranslatedFolder = targetTranslatedFolder.getParent();
            }
            return targetTranslatedFolder;
        }

        private boolean saveFolder(FolderTranslation ft, javax.jcr.Session session) {
            if (!ft.isEditable()) {
                throw new UnsupportedOperationException("Translation is immutable");
            }
            String id = ft.getId();
            try {
                Node node = session.getNodeByIdentifier(id);
                WorkflowManager manager = ((HippoWorkspace) node.getSession().getWorkspace()).getWorkflowManager();
                TranslationWorkflow tw = (TranslationWorkflow) manager.getWorkflow("translation", node);
                String namefr = ft.getNamefr();
                String urlfr = ft.getUrlfr();
                Document translationDoc = tw.addTranslation(language, urlfr);
                if (namefr != null && !urlfr.equals(namefr)) {
                    DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", translationDoc);
                    defaultWorkflow.setDisplayName(namefr);
                }
                return true;
            } catch (RepositoryException e) {
                log.error("Could not persist folder translation for {}", id, e);
            } catch (RemoteException e) {
                log.error("Could not contact repository when storing folder translation for {}", id, e);
            } catch (WorkflowException e) {
                log.error("Workflow prevented storing translation for {}", id, e);
            }
            return false;
        }

        private class TranslatedSourceAncestorFinder {
            private boolean myResult;
            private TranslatedFolder sourceTranslatedFolder;
            private TranslatedFolder sourceSibling;

            public TranslatedSourceAncestorFinder(final TranslatedFolder sourceTranslatedFolder) {
                this.sourceTranslatedFolder = sourceTranslatedFolder;
            }

            boolean isFound() {
                return myResult;
            }

            public TranslatedFolder getSourceTranslatedFolder() {
                return sourceTranslatedFolder;
            }

            public TranslatedFolder getSourceSibling() {
                return sourceSibling;
            }

            public TranslatedSourceAncestorFinder invoke() throws RepositoryException {
                sourceTranslatedFolder = sourceTranslatedFolder.getParent();
                if (sourceTranslatedFolder == null) {
                    myResult = true;
                    return this;
                }
                sourceSibling = sourceTranslatedFolder.getSibling(language);
                while (sourceSibling == null) {
                    FolderTranslation folderTranslation = JcrFolderTranslationFactory.createFolderTranslation(
                            sourceTranslatedFolder.node, null);
                    folderTranslation.setEditable(false);
                    folders.add(folderTranslation);

                    sourceTranslatedFolder = sourceTranslatedFolder.getParent();
                    if (sourceTranslatedFolder == null) {
                        break;
                    }
                    sourceSibling = sourceTranslatedFolder.getSibling(language);
                }
                if (sourceTranslatedFolder == null) {
                    myResult = true;
                    return this;
                }
                myResult = false;
                return this;
            }
        }
    }

    private class HippoLocaleDataView extends DataView<HippoLocale> {

        private final IModel<String> languageModel;

        HippoLocaleDataView(final AvailableLocaleProvider localeProvider, final IModel<String> languageModel) {
            super("languages", localeProvider);
            this.languageModel = languageModel;
            onPopulate();
        }

        @Override
        protected void populateItem(final Item<HippoLocale> item) {
            final HippoLocale locale = item.getModelObject();
            final String language = locale.getName();

            final TranslationAction translationsItem = new TranslationAction("language", new LoadableDetachableModel<String>() {

                @Override
                protected String load() {
                    String base = locale.getDisplayName(getLocale());
                    if (!hasLocale(language)) {
                        return base + "...";
                    }
                    return base;
                }
            }, item.getModel(), language, languageModel
            );
            final Boolean canAddTranslation = translationsModel.getObject().canAddTranslation();
            translationsItem.setEnabled(canAddTranslation != null || hasLocale(language));
            item.add(translationsItem);
        }

        @Override
        protected void onDetach() {
            languageModel.detach();
            super.onDetach();
        }
    }

    private class TranslationMenuDescription extends MenuDescription {
        private final ILocaleProvider localeProvider;
        private final IModel<String> languageModel;

        TranslationMenuDescription(final ILocaleProvider localeProvider, final IModel<String> languageModel) {
            this.localeProvider = localeProvider;
            this.languageModel = languageModel;
        }

        @Override
        public Component getLabel() {
            Fragment fragment = new Fragment("label", "label", TranslationWorkflowPlugin.this);
            HippoLocale locale = localeProvider.getLocale(languageModel.getObject());
            ResourceReference resourceRef = locale.getIcon(IconSize.M, LocaleState.EXISTS);
            fragment.add(new CachingImage("img", resourceRef));
            fragment.add(new Label("current-language", locale.getDisplayName(getLocale())));
            return fragment;
        }

        @Override
        public MarkupContainer getContent() {
            Fragment fragment = new Fragment("content", "languages", TranslationWorkflowPlugin.this);
            final AvailableLocaleProvider availableLocaleProvider = new AvailableLocaleProvider(localeProvider);
            fragment.add(new HippoLocaleDataView(availableLocaleProvider, languageModel));
            TranslationWorkflowPlugin.this.addOrReplace(fragment);
            return fragment;
        }
    }
}
