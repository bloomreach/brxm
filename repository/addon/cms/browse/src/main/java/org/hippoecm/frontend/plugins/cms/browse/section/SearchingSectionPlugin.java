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
package org.hippoecm.frontend.plugins.cms.browse.section;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.IChangeListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection.DocumentCollectionType;
import org.hippoecm.frontend.plugins.cms.browse.service.IBrowserSection;
import org.hippoecm.frontend.plugins.standards.browse.BrowserHelper;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.plugins.standards.search.TextSearchBuilder;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchingSectionPlugin extends RenderPlugin implements IBrowserSection {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(SearchingSectionPlugin.class);

    private class FolderModelService extends ModelReference<Node> {
        private static final long serialVersionUID = 1L;

        FolderModelService(IPluginConfig config, IModel<Node> document) {
            super(config.getString("model.folder"), document);
        }

        public void updateModel(IModel<Node> model) {
            super.setModel(model);
        }

        @Override
        public void setModel(IModel<Node> model) {
            if (model == null) {
                throw new IllegalArgumentException("invalid folder model null");
            } else if (model.getObject() == null) {
                throw new IllegalArgumentException("invalid folder node null");
            }
            onFolderChange(model);
        }

    }

    private class SearchModelService extends ModelReference<BrowserSearchResult> {
        private static final long serialVersionUID = 1L;

        private IObserver searchObserver;

        SearchModelService(IPluginConfig config) {
            super(config.getString("model.search"), new AbstractReadOnlyModel<BrowserSearchResult>() {
                private static final long serialVersionUID = 1L;

                @Override
                public BrowserSearchResult getObject() {
                    return null;
                }
            });
        }

        public void updateModel(final IModel<BrowserSearchResult> model) {
            IPluginContext context = getPluginContext();
            if (searchObserver != null) {
                context.unregisterService(searchObserver, IObserver.class.getName());
                searchObserver = null;
            }
            super.setModel(model);
            if (model instanceof IObservable) {
                searchObserver = new IObserver() {
                    private static final long serialVersionUID = 1L;

                    public IObservable getObservable() {
                        return (IObservable) model;
                    }

                    public void onEvent(Iterator events) {
                        onSearchModelChange(model);
                    }

                };
                context.registerService(searchObserver, IObserver.class.getName());
            }
        }

        @Override
        public void setModel(IModel<BrowserSearchResult> model) {
            if (model == null) {
                throw new IllegalArgumentException("invalid folder model null");
            }
            onSearchModelChange(model);
        }

    }

    private String rootPath;
    private FolderModelService folderService;
    private SearchModelService searchModelService;
    private DocumentCollection collection;

    private IModel<Node> scopeModel;
    private String query;
    private transient boolean redrawSearch = false;

    private MarkupContainer container;

    public SearchingSectionPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        this.rootPath = config.getString("model.folder.root", "/");
        scopeModel = new JcrNodeModel(rootPath);

        collection = new DocumentCollection();

        folderService = new FolderModelService(config, new JcrNodeModel(rootPath));
        searchModelService = new SearchModelService(config);
        collection.setFolder(folderService.getModel());

        collection.addListener(new IChangeListener() {
            private static final long serialVersionUID = 1L;

            public void onChange() {
                redrawSearch();
            }

        });

        container = new WebMarkupContainer("container") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onBeforeRender() {
                redrawSearch = false;
                super.onBeforeRender();
            }
        };
        container.setOutputMarkupId(true);

        TextField tx = new TextField("searchBox", new PropertyModel(this, "query"));
        tx.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateSearch(true);
            }
        });
        tx.add(new CssClassAppender(new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return (collection.getType() == DocumentCollectionType.SEARCHRESULT ? "grayedin" : "grayedout");
            }
        }));
        container.add(tx);

        final AjaxLink browseLink = new AjaxLink("browse") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                collection.setSearchResult(new Model(null));
            }

            @Override
            public boolean isEnabled() {
                return collection.getType() == DocumentCollectionType.SEARCHRESULT;
            }

        };
        browseLink.add(new CssClassAppender(new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return (browseLink.isEnabled() ? "grayedin" : "grayedout");
            }
        }));
        browseLink.add(new Image("browse-img", new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                if (browseLink.isEnabled()) {
                    return "browse-disabled-16.png";
                } else {
                    return "browse-16.png";
                }
            }
        }) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onDetach() {
                setDefaultModel(getDefaultModel());
                super.onDetach();
            }
        });
        container.add(browseLink);

        final AjaxLink searchLink = new AjaxLink("search") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                updateSearch(true);
            }

            @Override
            public boolean isEnabled() {
                return collection.getType() == DocumentCollectionType.FOLDER;
            }

        };
        searchLink.add(new CssClassAppender(new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return (searchLink.isEnabled() ? "grayedin" : "grayedout");
            }
        }));
        searchLink.add(new Image("search-img", new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                if (searchLink.isEnabled()) {
                    return "search-disabled-16.png";
                } else {
                    return "search-16.png";
                }
            }
        }) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onDetach() {
                setDefaultModel(getDefaultModel());
                super.onDetach();
            }
        });
        container.add(searchLink);

        AjaxLink scopeLink = new AjaxLink("scope") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                folderService.setModel(scopeModel);
            }

            @Override
            public boolean isEnabled() {
                JcrNodeModel rootModel = new JcrNodeModel(rootPath);
                return !rootModel.equals(scopeModel) && !scopeModel.equals(folderService.getModel());
            }

        };
        container.add(scopeLink);
        scopeLink.add(new Label("scope-label", new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return new NodeTranslator(scopeModel).getNodeName().getObject();
            }

        }));

        container.add(new AjaxLink("all") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                scopeModel = folderService.getModel();
                folderService.setModel(new JcrNodeModel(rootPath));
            }

            @Override
            public boolean isEnabled() {
                JcrNodeModel rootModel = new JcrNodeModel(rootPath);
                return !rootModel.equals(folderService.getModel());
            }

        });

        add(container);
    }

    void redrawSearch() {
        redrawSearch = true;
    }

    private void updateSearch(boolean forceQuery) {
        IModel<Node> model = folderService.getModel();
        String scope = "/";
        if (model instanceof JcrNodeModel) {
            scope = ((JcrNodeModel) model).getItemModel().getPath();
        }
        if (forceQuery || collection.getType() == DocumentCollectionType.SEARCHRESULT) {
            if (Strings.isEmpty(query)) {
                searchModelService.setModel(new Model(null));
            } else {
                TextSearchBuilder sb = new TextSearchBuilder();
                sb.setScope(new String[] { scope });
                sb.setWildcardSearch(true);
                sb.setText(query);
                searchModelService.setModel(sb.getResultModel());
            }
        }
    }

    @Override
    public void onStart() {
        folderService.init(getPluginContext());
        searchModelService.init(getPluginContext());
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        searchModelService.destroy();
        folderService.destroy();
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (target != null) {
            if (redrawSearch) {
                target.addComponent(container);
            }
        }
        super.render(target);
    }

    public void onFolderChange(IModel<Node> model) {
        folderService.updateModel(model);
        collection.setFolder(model);

        searchModelService.updateModel(new Model(null));
    }

    public void onSearchModelChange(IModel<BrowserSearchResult> model) {
        searchModelService.updateModel(model);
        collection.setSearchResult(searchModelService.getModel());
    }

    public void select(IModel<Node> document) {
        if (collection.getType() == DocumentCollectionType.SEARCHRESULT && !BrowserHelper.isFolder(document)) {
            return;
        }

        IModel<Node> folder = document;
        while (!BrowserHelper.isFolder(folder)) {
            folder = BrowserHelper.getParent(folder);
        }

        folderService.updateModel(folder);
        collection.setFolder(folder);

        if (BrowserHelper.isFolder(document)) {
            searchModelService.updateModel(new Model(null));
            collection.setSearchResult(new Model(null));
        }
    }

    public DocumentCollection getCollection() {
        return collection;
    }

    public boolean contains(IModel<Node> nodeModel) {
        try {
            return nodeModel.getObject().getPath().startsWith(rootPath);
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return false;
    }

    public IModel<String> getTitle() {
        return new StringResourceModel(getPluginConfig().getString("title", getPluginConfig().getName()), this, null);
    }

}
