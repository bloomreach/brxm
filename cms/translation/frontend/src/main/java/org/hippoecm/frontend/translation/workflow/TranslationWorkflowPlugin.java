/*
 *  Copyright 2010 Hippo.
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.ActionDescription;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.MenuDescription;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;
import org.hippoecm.frontend.translation.ILocaleProvider.LocaleState;
import org.hippoecm.frontend.translation.components.document.FolderTranslation;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.hippoecm.repository.translation.TranslationWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TranslationWorkflowPlugin extends CompatibilityWorkflowPlugin {

    private final class LanguageModel extends LoadableDetachableModel<String> {
        private static final long serialVersionUID = 1L;

        @Override
        protected String load() {
            WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) TranslationWorkflowPlugin.this.getDefaultModel();
            if (wdm != null) {
                WorkflowDescriptor descriptor = (WorkflowDescriptor) wdm.getObject();
                WorkflowManager manager = ((UserSession) org.apache.wicket.Session.get()).getWorkflowManager();
                try {
                    TranslationWorkflow translationWorkflow = (TranslationWorkflow) manager.getWorkflow(descriptor);
                    return (String) translationWorkflow.hints().get("locale");
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage(), ex);
                } catch (RemoteException ex) {
                    log.error(ex.getMessage(), ex);
                } catch (WorkflowException ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
            return "unknown";
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
            availableLocales = new LinkedList<HippoLocale>();
            for (String language : getAvailableLanguages()) {
                availableLocales.add(localeProvider.getLocale(language));
            }
        }

        public Iterator<? extends HippoLocale> iterator(int first, int count) {
            if (availableLocales == null) {
                load();
            }
            return availableLocales.subList(first, first + count).iterator();
        }

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

        public int size() {
            if (availableLocales == null) {
                load();
            }
            return availableLocales.size();
        }

        public void detach() {
            availableLocales = null;
        }
    }

    private final class DisplayTranslationAction extends WorkflowAction {
        private final String language;
        private static final long serialVersionUID = 1L;

        private DisplayTranslationAction(String id, String name, ResourceReference iconModel, String language) {
            super(id, name, iconModel);
            this.language = language;
        }

        @Override
        protected String execute(Workflow wf) throws Exception {
            IBrowseService<JcrNodeModel> browser = getBrowserService();
            if (browser != null) {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) TranslationWorkflowPlugin.this
                        .getDefaultModel();
                if (wdm != null) {
                    Node node;
                    try {
                        node = wdm.getNode();
                        if (node != null) {
                            Node translations = node.getNode(HippoTranslationNodeType.TRANSLATIONS);
                            HippoNode translation = (HippoNode) translations.getNode(language);
                            browser.browse(new JcrNodeModel(translation.getCanonicalNode().getParent()));
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
                log.warn("Cannot navigate to translation - configured browser.id '"
                        + getPluginConfig().getString("browser.id") + "' is invalid.");
            }
            return null;
        }
    }

    private final class AddTranslationAction extends WorkflowAction {
        private final String language;
        private final IModel<String> languageModel;
        private static final long serialVersionUID = 1L;
        public String name;
        public String url;
        public List<FolderTranslation> folders;

        private AddTranslationAction(String id, String name, ResourceReference iconModel, String language,
                IModel<String> languageModel) {
            super(id, name, iconModel);
            this.language = language;
            this.languageModel = languageModel;
        }

        @Override
        protected Dialog createRequestDialog() {
            try {
                Node docNode = ((WorkflowDescriptorModel) TranslationWorkflowPlugin.this.getDefaultModel()).getNode();
                url = docNode.getName();
                name = url;
                if (docNode instanceof HippoNode) {
                    name = ((HippoNode) docNode).getLocalizedName();
                }
                folders = new LinkedList<FolderTranslation>();
                Node handle = docNode.getParent();
                FolderTranslation docTranslation = JcrFolderTranslationFactory.createFolderTranslation(handle);
                folders.add(docTranslation);

                Node folder = handle;
                boolean mutable = true;
                // FIXME: OUCH!
                while (!"/content/documents".equals(folder.getPath())) {
                    if (folder.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                        Node links = folder.getNode(HippoTranslationNodeType.TRANSLATIONS);
                        FolderTranslation ft;
                        if (links.hasNode(language)) {
                            mutable = false;
                            HippoNode translation = (HippoNode) links.getNode(language);
                            ft = JcrFolderTranslationFactory.createFolderTranslation(folder, translation
                                    .getCanonicalNode());
                        } else {
                            ft = JcrFolderTranslationFactory.createFolderTranslation(folder);
                        }
                        ft.setEditable(mutable);
                        folders.add(ft);
                    }
                    folder = folder.getParent();
                }
                Collections.reverse(folders);

                return new DocumentTranslationDialog(TranslationWorkflowPlugin.this, getPluginContext().getService(
                        ISettingsService.SERVICE_ID, ISettingsService.class), this, new StringResourceModel(
                        "translate-title", TranslationWorkflowPlugin.this, null), folders, languageModel.getObject(),
                        language, getLocaleProvider());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                error(e.getMessage());
            }
            return null;
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
                    defaultWorkflow.localizeName(namefr);
                }
                return true;
            } catch (RepositoryException e) {
                log.error("Could not persist folder translation for " + id + " due to " + e.getMessage());
            } catch (RemoteException e) {
                log.error("Could not contact repository when storing folder translation for " + id + " due to "
                        + e.getMessage());
            } catch (WorkflowException e) {
                log.error("Workflow prevented storing translation for " + id + " due to " + e.getMessage());
            }
            return false;
        }

        @Override
        protected String execute(Workflow wf) throws Exception {
            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();

            for (int i = 0; i < (folders.size() - 1); i++) {
                FolderTranslation folder = folders.get(i);
                if (!folder.isEditable()) {
                    continue;
                }
                if (!saveFolder(folder, session)) {
                    return COULD_NOT_CREATE_FOLDERS;
                }
            }

            FolderTranslation docTranslation = folders.get(folders.size() - 1);
            this.name = docTranslation.getNamefr();
            this.url = docTranslation.getUrlfr();

            TranslationWorkflow workflow = (TranslationWorkflow) wf;
            Document translation = workflow.addTranslation(language, url);
            WorkflowManager manager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
            DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", translation);
            if (name != null && !url.equals(name)) {
                String localized = getLocalizeCodec().encode(name);
                defaultWorkflow.localizeName(localized);
            }
            IBrowseService<JcrNodeModel> browser = getBrowserService();
            if (browser != null) {
                browser.browse(new JcrNodeModel(session.getNodeByUUID(translation.getIdentity())));
            } else {
                log.warn("Cannot open newly created document - configured browser.id "
                        + getPluginConfig().getString("browser.id") + " is invalid.");
            }
            return null;
        }
    }

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final static String COULD_NOT_CREATE_FOLDERS = "could-not-create-folders";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(TranslationWorkflowPlugin.class);

    public boolean hasLocale(String locale) {
        WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) TranslationWorkflowPlugin.this.getDefaultModel();
        if (wdm != null) {
            Node node;
            try {
                node = wdm.getNode();
                if (node != null) {
                    Node translations = node.getNode(HippoTranslationNodeType.TRANSLATIONS);
                    return translations.hasNode(locale);
                }
            } catch (RepositoryException e) {
                log.error("Failed to determine whether model is of locale " + locale);
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Set<String> getAvailableLanguages() {
        WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) TranslationWorkflowPlugin.this.getDefaultModel();
        if (wdm != null) {
            WorkflowDescriptor descriptor = (WorkflowDescriptor) wdm.getObject();
            WorkflowManager manager = ((UserSession) org.apache.wicket.Session.get()).getWorkflowManager();
            try {
                TranslationWorkflow translationWorkflow = (TranslationWorkflow) manager.getWorkflow(descriptor);
                return (Set<String>) translationWorkflow.hints().get("available");
            } catch (RepositoryException ex) {
                log.error(ex.getMessage(), ex);
            } catch (RemoteException ex) {
                log.error(ex.getMessage(), ex);
            } catch (WorkflowException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
        return Collections.emptySet();
    }

    public TranslationWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final IModel<String> languageModel = new LanguageModel();
        final ILocaleProvider localeProvider = getLocaleProvider();
        add(new MenuDescription() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getLabel() {
                Fragment fragment = new Fragment("label", "label", TranslationWorkflowPlugin.this);
                HippoLocale locale = localeProvider.getLocale(languageModel.getObject());
                if (locale != null) {
                    ResourceReference resourceRef = locale.getIcon(IconSize.TINY, LocaleState.EXISTS);
                    fragment.add(new Image("img", resourceRef));
                    fragment.add(new Label("current-language", locale.getDisplayName(getLocale())));
                } else {
                    setVisible(false);
                    fragment.add(new Image("img", new ResourceReference(getClass(), "translate-16.png")));
                    fragment.add(new Label("current-language"));
                }
                return fragment;
            }

        });

        add(new DataView<HippoLocale>("languages", new AvailableLocaleProvider(localeProvider)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<HippoLocale> item) {
                final HippoLocale locale = item.getModelObject();
                final String language = locale.getName();
                if (!hasLocale(language)) {
                    item.add(new AddTranslationAction("language", locale.getDisplayName(getLocale()), locale.getIcon(
                            IconSize.TINY, LocaleState.AVAILABLE), language, languageModel));
                } else {
                    item.add(new DisplayTranslationAction("language", locale.getDisplayName(getLocale()), locale
                            .getIcon(IconSize.TINY, LocaleState.EXISTS), language));
                }
            }

            @Override
            protected void onDetach() {
                languageModel.detach();
                super.onDetach();
            }
        });
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
        if (wdm != null) {
            WorkflowDescriptor descriptor = (WorkflowDescriptor) wdm.getObject();
            if (descriptor != null) {
                try {
                    Map<String, Serializable> hints = descriptor.hints();
                    if (hints.containsKey("addTranslation") && hints.get("addTranslation").equals(Boolean.FALSE)) {
                        this.visitChildren(new IVisitor() {

                            public Object component(Component component) {
                                if (component instanceof ActionDescription) {
                                    component.setVisible(false);
                                    return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                                }
                                return IVisitor.CONTINUE_TRAVERSAL;
                            }

                        });
                    }
                } catch (RepositoryException e) {
                    log.error("Failed to analyze hints for translations workflow", e);
                }
            }
        }
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
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID,
                ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.display");
    }

}
