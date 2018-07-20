/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

public final class TranslationWorkflowPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(TranslationWorkflowPlugin.class);
    private final IModel<Boolean> canTranslateModel;

    private final class LanguageModel extends LoadableDetachableModel<String> {
        private static final long serialVersionUID = 1L;

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
                throw new RuntimeException("could not determine whether nodes are equivalent", e);
            }
        }

        @Override
        public int hashCode() {
            try {
                return node.getPath().hashCode();
            } catch (RepositoryException e) {
                throw new RuntimeException("could not determine path of node", e);
            }
        }
    }

    private final class AvailableLocaleProvider implements IDataProvider<HippoLocale> {
        private final ILocaleProvider localeProvider;
        private static final long serialVersionUID = 1L;
        private transient List<HippoLocale> availableLocales;

        private AvailableLocaleProvider(ILocaleProvider localeProvider) {
            this.localeProvider = localeProvider;
        }

        private void load() {
            availableLocales = new LinkedList<>();
            for (String language : getAvailableLanguages()) {
                availableLocales.add(localeProvider.getLocale(language));
            }
            Collections.sort(availableLocales, new Comparator<HippoLocale>() {
                @Override
                public int compare(HippoLocale o1, HippoLocale o2) {
                    return o1.getDisplayName(getLocale()).compareTo(o2.getDisplayName(getLocale()));
                }
            });
        }

        @Override
        public Iterator<? extends HippoLocale> iterator(long first, long count) {
            if (availableLocales == null) {
                load();
            }
            return availableLocales.subList((int) first, (int)(first + count)).iterator();
        }

        @Override
        public IModel<HippoLocale> model(HippoLocale object) {
            final String id = object.getName();
            return new LoadableDetachableModel<HippoLocale>() {
                private static final long serialVersionUID = 1L;

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
    }

    private final class TranslationAction extends StdWorkflow<TranslationWorkflow> {
        private static final long serialVersionUID = 1L;

        private final String language;
        private final IModel<String> languageModel;
        private final IModel<HippoLocale> localeModel;

        public String name;
        public String url;
        boolean autoTranslateContent;

        public List<FolderTranslation> folders;
        private final IModel<String> title;

        private TranslationAction(String id, IModel<String> name, IModel<HippoLocale> localeModel, String language, IModel<String> languageModel) {
            super(id, name, getPluginContext(), (WorkflowDescriptorModel) TranslationWorkflowPlugin.this.getModel());
            this.language = language;
            this.title = name;
            this.languageModel = languageModel;
            this.localeModel = localeModel;
        }

        @Override
        public boolean isVisible() {
            if (super.isVisible() && findPage() != null) {
                return canTranslateModel.getObject();
            }
            return false;
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

        private String executeAvailableTransaction() throws Exception {
            IBrowseService<JcrNodeModel> browser = getBrowserService();
            if (browser != null) {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) TranslationWorkflowPlugin.this.getDefaultModel();
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
                log.warn("Cannot navigate to translation - configured browser.id '" + getPluginConfig().getString(
                        "browser.id") + "' is invalid.");
            }
            return null;
        }

        protected String executeNonAvailableTranslation(TranslationWorkflow workflow) throws Exception {
            javax.jcr.Session session = UserSession.get().getJcrSession();

            // Find the index of the deepest translated folder.
            // The caller is to guarantee that at least the root node is translated (hence starting i at 1),
            // and that there is a document handle node at the end of the list (representing the to-be-translated document).
            final int indexOfDeepestFolder = folders.size() - 1;
            int i;
            for (i = 1; i < indexOfDeepestFolder && !folders.get(i).isEditable(); i++) {
                // do nothing
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
                if (autoTranslateContent) {
                    Workflow translateWorkflow = manager.getWorkflow("translate", translatedVariant);
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
                IBrowseService<JcrNodeModel> browser = getBrowserService();
                if (browser != null) {
                    browser.browse(new JcrNodeModel(session.getNodeByIdentifier(translatedDocument.getIdentity())));
                } else {
                    log.warn(
                            "Cannot open newly created document - configured browser.id " + getPluginConfig().getString(
                                    "browser.id") + " is invalid.");
                }
            }
            return null;
        }

        /**
         * Prevent the creation of same-name-sibling (SNS) folders when translating a document (or folder?).
         * This affects
         *
         *   1) the case where the deepest existing folder already has a child node with the same (node-)name
         *   2) the case where the deepest existing folder already has a child node with the same localized name
         *
         * An exception of type {@link WorkflowSNSException} will be thrown if there is an SNS issue.
         *
         * @param session
         * @param indexOfDeepestTranslatedFolder
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
                WorkflowDescriptor workflow = manager.getWorkflowDescriptor("translate", docNode);
                if (workflow != null) {
                    final Serializable available = workflow.hints().get("translate");
                    if (available != null && (Boolean) available) {
                        autoTranslateModel = new PropertyModel<>(TranslationAction.this, "autoTranslateContent");
                        autoTranslateContent = false; // default when translation is available
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
            while (sourceTranslatedFolder != null) {
                {
                    FolderTranslation ft = JcrFolderTranslationFactory.createFolderTranslation(
                            sourceTranslatedFolder.node, targetTranslatedFolder.node);
                    ft.setEditable(false);
                    folders.add(ft);
                }

                // walk up the source tree until a translated ancestor is found
                sourceTranslatedFolder = sourceTranslatedFolder.getParent();
                if (sourceTranslatedFolder == null) {
                    break;
                }
                TranslatedFolder sourceSibling = sourceTranslatedFolder.getSibling(language);
                while (sourceSibling == null) {
                    FolderTranslation ft = JcrFolderTranslationFactory.createFolderTranslation(
                            sourceTranslatedFolder.node, null);
                    ft.setEditable(false);
                    folders.add(ft);

                    sourceTranslatedFolder = sourceTranslatedFolder.getParent();
                    if (sourceTranslatedFolder == null) {
                        break;
                    }
                    sourceSibling = sourceTranslatedFolder.getSibling(language);
                }
                if (sourceTranslatedFolder == null) {
                    break;
                }
                assert sourceSibling != null;

                // walk up the target tree until a translated ancestor is found
                targetTranslatedFolder = targetTranslatedFolder.getParent();
                while (targetTranslatedFolder != null) {
                    if (targetTranslatedFolder.equals(sourceSibling)) {
                        break;
                    }
                    TranslatedFolder backLink = targetTranslatedFolder.getSibling(languageModel.getObject());
                    if (backLink != null) {
                        if (!targetTranslatedFolder.equals(sourceSibling)) {
                            break;
                        }
                    }

                    FolderTranslation ft = JcrFolderTranslationFactory.createFolderTranslation(null,
                            targetTranslatedFolder.node);
                    ft.setEditable(false);
                    folders.add(ft);

                    targetTranslatedFolder = targetTranslatedFolder.getParent();
                }
                if (targetTranslatedFolder == null || !targetTranslatedFolder.equals(sourceSibling)) {
                    break;
                }
            }
            Collections.reverse(folders);
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
                log.error("Could not persist folder translation for " + id + " due to " + e.getMessage());
            } catch (RemoteException e) {
                log.error(
                        "Could not contact repository when storing folder translation for " + id + " due to " + e.getMessage());
            } catch (WorkflowException e) {
                log.error("Workflow prevented storing translation for " + id + " due to " + e.getMessage());
            }
            return false;
        }

        @Override
        protected void onDetach() {
            super.onDetach();
        }
    }

    final static String COULD_NOT_CREATE_FOLDERS = "could-not-create-folders";

    private final DocumentTranslationProvider translationProvider;

    public TranslationWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final IModel<String> languageModel = new LanguageModel();
        final ILocaleProvider localeProvider = getLocaleProvider();

        Node documentNode = null;
        DocumentTranslationProvider docTranslationProvider = null;
        try {
            documentNode = getDocumentNode();
            docTranslationProvider = new DocumentTranslationProvider(new JcrNodeModel(documentNode),
                    localeProvider);
        } catch (RepositoryException e) {
            log.warn("Unable to find document node");
        }
        translationProvider = docTranslationProvider;

        // lazily determine whether the document can be translated
        canTranslateModel = new LoadableDetachableModel<Boolean>() {
            @Override
            protected Boolean load() {
                WorkflowDescriptor descriptor = getModelObject();
                if (descriptor != null) {
                    try {
                        Map<String, Serializable> hints = descriptor.hints();
                        if (hints.containsKey("addTranslation") && hints.get("addTranslation").equals(Boolean.FALSE)) {
                            return false;
                        }

                    } catch (RepositoryException e) {
                        log.error("Failed to analyze hints for translations workflow", e);
                    }
                }
                return true;
            }
        };


        try {
            if (!TranslationUtil.isNtTranslated(documentNode.getParent().getParent()) &&
                (!TranslationUtil.isNtTranslated(documentNode) || !localeProvider.isKnown(languageModel.getObject()))) {
                return;
            }
        } catch (RepositoryException e) {
           log.warn("Could not determine translations status of document", e);
        }

        add(new EmptyPanel("content"));

        add(new MenuDescription() {
            private static final long serialVersionUID = 1L;

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
                fragment.add(new DataView<HippoLocale>("languages", new AvailableLocaleProvider(localeProvider)) {
                    private static final long serialVersionUID = 1L;

                    {
                        onPopulate();
                    }

                    @Override
                    protected void populateItem(Item<HippoLocale> item) {
                        final HippoLocale locale = item.getModelObject();
                        final String language = locale.getName();

                        item.add(new TranslationAction("language", new LoadableDetachableModel<String>() {

                            @Override
                            protected String load() {
                                String base = locale.getDisplayName(getLocale());
                                if (!hasLocale(language)) {
                                    return base + "...";
                                }
                                return base;
                            }
                        }, item.getModel(), language, languageModel
                        ));
                    }

                    @Override
                    protected void onDetach() {
                        languageModel.detach();
                        super.onDetach();
                    }
                });
                TranslationWorkflowPlugin.this.addOrReplace(fragment);
                return fragment;
            }
        });

    }

    public boolean hasLocale(String locale) {
        return translationProvider != null && translationProvider.contains(locale);
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

    protected IBrowseService getBrowserService() {
        return getPluginContext().getService(getPluginConfig().getString(IBrowseService.BROWSER_ID, "service.browse"),
                IBrowseService.class);
    }

    protected StringCodec getLocalizeCodec() {
        return CodecUtils.getDisplayNameCodec(getPluginContext());
    }

    @Override
    protected void onDetach() {
        if (translationProvider != null) {
            translationProvider.detach();
        }
        this.canTranslateModel.detach();
        super.onDetach();
    }

    private static void collectFields(String relPath, String nodeType, Set<String> plainTextFields, Set<String> richTextFields) throws StoreException {
        try {
            JcrTypeStore jcrTypeStore = new JcrTypeStore();
            ITypeDescriptor type = jcrTypeStore.load(nodeType);
            for (Map.Entry<String, IFieldDescriptor> field : type.getFields().entrySet()) {
                IFieldDescriptor fieldDescriptor = field.getValue();
                if ("*".equals(fieldDescriptor.getPath())) {
                    continue;
                }
                ITypeDescriptor fieldType = fieldDescriptor.getTypeDescriptor();
                if (fieldType.getType().equals(HippoStdNodeType.NT_HTML)) {
                    richTextFields.add(
                            (relPath != null ? relPath + "/" : "") + fieldDescriptor.getPath() + "/" + HippoStdNodeType.HIPPOSTD_CONTENT);
                } else if (fieldType.getName().equals("Text") || fieldType.getName().equals("Label")) {
                    plainTextFields.add((relPath != null ? relPath + "/" : "") + fieldDescriptor.getPath());
                } else if (fieldType.getName().equals("Html")) {
                    richTextFields.add((relPath != null ? relPath + "/" : "") + fieldDescriptor.getPath());
                } else if (fieldType.isNode()) {
                    collectFields((relPath != null ? relPath + "/" : "") + fieldDescriptor.getPath(),
                            fieldType.getType(), plainTextFields, richTextFields);
                }
            }
        } catch (StoreException ex) {
            // ignore nt:base
        }
    }

}
