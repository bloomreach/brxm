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
package org.hippoecm.frontend.translation;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;
import org.hippoecm.frontend.translation.ILocaleProvider.IconType;
import org.hippoecm.frontend.widgets.AbstractView;
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

public class TranslationWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final static String COULD_NOT_CREATE_FOLDERS = "could-not-create-folders";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(TranslationWorkflowPlugin.class);

    private AbstractView<HippoLocale> rv;

    static class FolderTranslation implements IDetachable {
        private static final long serialVersionUID = 1L;

        private JcrNodeModel originalFolder;
        private String localeName;
        private String name;
        private String url;

        public FolderTranslation(Node node, String localeName) throws RepositoryException {
            this.originalFolder = new JcrNodeModel(node);
            this.localeName = localeName;

            this.url = node.getName();
            this.name = this.url;
            if (node instanceof HippoNode) {
                this.name = ((HippoNode) node).getLocalizedName();
            }
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public boolean persist() {
            Node node = originalFolder.getNode();
            if (node != null) {
                try {
                    WorkflowManager manager = ((HippoWorkspace) node.getSession().getWorkspace()).getWorkflowManager();
                    TranslationWorkflow tw = (TranslationWorkflow) manager.getWorkflow("translation", node);
                    Document translationDoc = tw.addTranslation(localeName, url);
                    if (name != null && !url.equals(name)) {
                        DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", translationDoc);
                        defaultWorkflow.localizeName(name);
                    }
                    return true;
                } catch (RepositoryException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (WorkflowException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return false;
        }

        public void detach() {
            originalFolder.detach();
        }
    }

    public TranslationWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final IModel<String> languageModel = new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) TranslationWorkflowPlugin.this
                        .getDefaultModel();
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
        };
        ILocaleProvider localeProvider = getLocaleProvider();
        rv = new AbstractView<HippoLocale>("languages", new ListDataProvider<HippoLocale>(localeProvider.getLocales())) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<HippoLocale> item) {
                HippoLocale locale = item.getModelObject();
                String currentLocaleName = languageModel.getObject();
                final boolean currentLocale = currentLocaleName != null ? currentLocaleName.equals(locale.getName())
                        : false;
                final String language = locale.getName();
                item.add(new WorkflowAction("language", locale.getDisplayName(getLocale()), locale
                        .getIcon(IconType.SMALL)) {
                    private static final long serialVersionUID = 1L;

                    public String name;
                    public String url;

                    public List<FolderTranslation> folders;

                    @Override
                    protected Dialog createRequestDialog() {
                        if (currentLocale) {
                            return null;
                        }
                        try {
                            Node node = ((WorkflowDescriptorModel) TranslationWorkflowPlugin.this.getDefaultModel())
                                    .getNode();
                            url = node.getName();
                            name = url;
                            if (node instanceof HippoNode) {
                                name = ((HippoNode) node).getLocalizedName();
                            }
                            folders = new LinkedList<FolderTranslation>();
                            Node folder = node.getParent();
                            while (folder.getDepth() != 0) {
                                if (folder.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                                    Node links = folder.getNode(HippoTranslationNodeType.TRANSLATIONS);
                                    if (links.hasNode(language)) {
                                        break;
                                    }
                                    folders.add(new FolderTranslation(folder, language));
                                }
                                folder = folder.getParent();
                            }
                            Collections.reverse(folders);

                            return new TranslateDocumentDialog(this, new StringResourceModel("translate-title",
                                    TranslationWorkflowPlugin.this, null));
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            error(e.getMessage());
                        }
                        return null;
                    }

                    @Override
                    protected String execute(Workflow wf) throws Exception {
                        for (FolderTranslation folder : folders) {
                            if (!folder.persist()) {
                                return COULD_NOT_CREATE_FOLDERS;
                            }
                        }

                        TranslationWorkflow workflow = (TranslationWorkflow) wf;
                        Document translation = workflow.addTranslation(language, url);
                        javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
                        WorkflowManager manager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                        DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", translation);
                        if (name != null && !url.equals(name)) {
                            defaultWorkflow.localizeName(name);
                        }
                        return null;
                    }
                });

            }

            @Override
            protected void onDetach() {
                languageModel.detach();
                super.onDetach();
            }
        };
        add(rv);

        rv.populate();
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        rv.populate();
    }

    protected ILocaleProvider getLocaleProvider() {
        return getPluginContext().getService(getPluginConfig().getString("locale.id", ILocaleProvider.class.getName()),
                ILocaleProvider.class);
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

    public class TranslateDocumentDialog extends WorkflowAction.WorkflowDialog {
        private IModel<String> title;
        private TextField<String> nameComponent;
        private TextField<String> uriComponent;
        private boolean uriModified;
        private PropertyModel<List<FolderTranslation>> foldersModel;

        public TranslateDocumentDialog(WorkflowAction action, IModel<String> title) {
            action.super();
            this.title = title;

            final PropertyModel<String> nameModel = new PropertyModel<String>(action, "name");
            final PropertyModel<String> urlModel = new PropertyModel<String>(action, "url");
            foldersModel = new PropertyModel<List<FolderTranslation>>(action, "folders");

            String s1 = nameModel.getObject();
            String s2 = urlModel.getObject();
            uriModified = (s1 != s2) && (s1 == null || !s1.equals(s2));

            nameComponent = new TextField<String>("name", nameModel);
            nameComponent.setRequired(true);
            nameComponent.setLabel(new StringResourceModel("name-label", TranslationWorkflowPlugin.this, null));
            nameComponent.add(new OnChangeAjaxBehavior() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    if (!uriModified) {
                        urlModel.setObject(getNodeNameCodec().encode(nameModel.getObject()));
                        target.addComponent(uriComponent);
                    }
                }
            }.setThrottleDelay(Duration.milliseconds(500)));
            nameComponent.setOutputMarkupId(true);
            setFocus(nameComponent);
            add(nameComponent);

            add(uriComponent = new TextField<String>("uriinput", urlModel) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isEnabled() {
                    return uriModified;
                }
            });

            uriComponent.add(new CssClassAppender(new AbstractReadOnlyModel<String>() {
                private static final long serialVersionUID = 1L;

                @Override
                public String getObject() {
                    return uriModified ? "grayedin" : "grayedout";
                }
            }));
            uriComponent.setOutputMarkupId(true);

            AjaxLink<Boolean> uriAction = new AjaxLink<Boolean>("uriAction") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    uriModified = !uriModified;
                    if (!uriModified) {
                        urlModel.setObject(Strings.isEmpty(nameModel.getObject()) ? "" : getNodeNameCodec().encode(
                                nameModel.getObject()));
                    }
                    target.addComponent(TranslateDocumentDialog.this);
                }
            };
            uriAction.add(new Label("uriActionLabel", new AbstractReadOnlyModel<String>() {
                private static final long serialVersionUID = 1L;

                @Override
                public String getObject() {
                    return uriModified ? getString("url-reset") : getString("url-edit");
                }
            }));
            add(uriAction);
        }

        @Override
        protected void onDetach() {
            for (FolderTranslation ft : foldersModel.getObject()) {
                ft.detach();
            }
            super.onDetach();
        }

        @Override
        public IModel<String> getTitle() {
            return title;
        }

        @Override
        public IValueMap getProperties() {
            return MEDIUM;
        }
    }

}
