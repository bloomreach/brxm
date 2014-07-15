/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmittingComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.behaviors.OnEnterAjaxBehavior;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.IChangeListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection.DocumentCollectionType;
import org.hippoecm.frontend.plugins.cms.browse.service.IBrowserSection;
import org.hippoecm.frontend.plugins.standards.browse.BrowserHelper;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.hippoecm.frontend.plugins.standards.image.CachingImage;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.plugins.standards.search.TextSearchBuilder;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchingSectionPlugin extends RenderPlugin implements IBrowserSection {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(SearchingSectionPlugin.class);
    private static final CssResourceReference SEARCH_SKIN = new CssResourceReference(SearchingSectionPlugin.class, "search.css");

    private final class SubmittingTextField extends TextField<String> implements IFormSubmittingComponent {
        private static final long serialVersionUID = 1L;

        private SubmittingTextField(String id, IModel<String> model) {
            super(id, model);

            add(new OnEnterAjaxBehavior() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget target) {
                    updateSearch(true);
                }

                @Override
                protected void onError(AjaxRequestTarget target) {
                }
            });
        }

        @Override
        public Component setDefaultFormProcessing(final boolean defaultFormProcessing) {
            return this;
        }

        @Override
        public boolean getDefaultFormProcessing() {
            return true;
        }

        @Override
        public void onSubmit() {
        }

        @Override
        public void onAfterSubmit() {
        }

        @Override
        public void onError() {
        }
    }

    private class FolderModelService extends ModelReference<Node> {
        private static final long serialVersionUID = 1L;

        FolderModelService(IPluginConfig config, IModel<Node> document) {
            super(config.getString("model.folder"), document);
        }

        public void updateModel(IModel<Node> model) {
            if (collection.getType() == DocumentCollectionType.SEARCHRESULT
                    || (getModel() != null && getModel().equals(scopeModel))) {
                scopeModel = model;
            }
            super.setModel(model);
        }

        @Override
        public void setModel(IModel<Node> model) {
            if (model == null) {
                throw new IllegalArgumentException("invalid folder model null");
            } else if (model.getObject() == null) {
                log.warn("Node no longer exists");
                return;
            }
            onFolderChange(model);
        }

    }

    private String rootPath;
    private FolderModelService folderService;
    private final DocumentCollection collection;

    private IModel<Node> scopeModel;
    private String query;
    private transient boolean redrawSearch = false;

    private Form container;

    public SearchingSectionPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        this.rootPath = config.getString("model.folder.root", "/");
        scopeModel = new JcrNodeModel(rootPath);

        collection = new DocumentCollection();

        folderService = new FolderModelService(config, new JcrNodeModel(rootPath));
        collection.setFolder(folderService.getModel());

        collection.addListener(new IChangeListener() {
            private static final long serialVersionUID = 1L;

            public void onChange() {
                redrawSearch();
            }

        });

        container = new Form("container") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onBeforeRender() {
                redrawSearch = false;
                super.onBeforeRender();
            }

            @Override
            protected void onSubmit() {
            }
        };
        container.setOutputMarkupId(true);

        TextField<String> tx = new SubmittingTextField("searchBox", new PropertyModel<String>(this, "query"));
        container.add(tx);

        final AjaxSubmitLink browseLink = new AjaxSubmitLink("toggle") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                if (collection.getType() == DocumentCollectionType.SEARCHRESULT) {
                    collection.setSearchResult(new Model(null));
                } else {
                    updateSearch(true);
                }
            }

        };
        browseLink.add(new SearchIcon(collection));
        container.add(browseLink);

        WebMarkupContainer scopeContainer = new WebMarkupContainer("scope-container") {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return collection.getType() == DocumentCollectionType.SEARCHRESULT;
            }

        };
        final AjaxLink scopeLink = new AjaxLink("scope") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                folderService.setModel(scopeModel);
            }

            @Override
            public boolean isEnabled() {
                if (collection.getType() == DocumentCollectionType.SEARCHRESULT) {
                    JcrNodeModel rootModel = new JcrNodeModel(rootPath);
                    return !rootModel.equals(scopeModel) && !scopeModel.equals(folderService.getModel());
                }
                return false;
            }

        };
        scopeContainer.add(new CssClassAppender(new SearchScopeModel(scopeLink)));
        scopeLink.add(new Label("scope-label", new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return new NodeTranslator(scopeModel).getNodeName().getObject();
            }

        }));
        scopeContainer.add(scopeLink);
        container.add(scopeContainer);

        WebMarkupContainer allContainer = new WebMarkupContainer("all-container") {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return collection.getType() == DocumentCollectionType.SEARCHRESULT;
            }
        };
        final AjaxLink allLink = new AjaxLink("all") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                IModel<Node> backup = folderService.getModel();
                folderService.setModel(new JcrNodeModel(rootPath));
                scopeModel = backup;
            }

            @Override
            public boolean isEnabled() {
                if (collection.getType() == DocumentCollectionType.SEARCHRESULT) {
                    JcrNodeModel rootModel = new JcrNodeModel(rootPath);
                    return !rootModel.equals(folderService.getModel());
                }
                return false;
            }
        };
        allContainer.add(new CssClassAppender(new SearchScopeModel(allLink)));
        allContainer.add(allLink);
        container.add(allContainer);

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
                collection.setSearchResult(new Model(null));
            } else {
                TextSearchBuilder sb = new TextSearchBuilder();
                sb.setScope(new String[] { scope });
                sb.setWildcardSearch(true);
                sb.setText(query);
                sb.setIncludePrimaryTypes(getPluginConfig().getStringArray("nodetypes"));
                collection.setSearchResult(sb.getResultModel());
            }
        }
    }

    @Override
    public void onStart() {
        folderService.init(getPluginContext());
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        folderService.destroy();
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (target != null) {
            if (redrawSearch && isVisibleInHierarchy()) {
                target.add(container);
            }
        }
        super.render(target);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(SEARCH_SKIN));
    }

    public void onFolderChange(IModel<Node> model) {
        folderService.updateModel(model);
        collection.setFolder(model);

        if (collection.getType() == DocumentCollectionType.SEARCHRESULT) {
            updateSearch(false);
        }
    }

    public void select(IModel<Node> document) {
        if (document == null) {
            return;
        }

        boolean toBrowseMode = false;
        if (collection.getType() == DocumentCollectionType.SEARCHRESULT && !BrowserHelper.isFolder(document)) {
            toBrowseMode = true;
            Node docNode = document.getObject();
            if (docNode != null) {
                BrowserSearchResult bsr = collection.getSearchResult().getObject();
                QueryResult result = bsr.getQueryResult();
                try {
                    NodeIterator nodes = result.getNodes();
                    while (nodes.hasNext()) {
                        Node node = nodes.nextNode();
                        if (node != null && node.getDepth() > 0) {
                            Node parent = node.getParent();
                            if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                                if (parent.isSame(docNode)) {
                                    return;
                                }
                            } else {
                                if (node.isSame(docNode)) {
                                    return;
                                }
                            }
                        }
                    }
                } catch (RepositoryException ex) {
                    log.error("Error processing query results", ex);
                    return;
                }
            }
        }

        IModel<Node> folder = document;
        while (!BrowserHelper.isFolder(folder)) {
            folder = BrowserHelper.getParent(folder);
        }

        folderService.updateModel(folder);
        collection.setFolder(folder);

        if (toBrowseMode || BrowserHelper.isFolder(document)) {
            collection.setSearchResult(new Model(null));
        }
    }

    public DocumentCollection getCollection() {
        return collection;
    }

    public Match contains(IModel<Node> nodeModel) {
        try {
            String path = nodeModel.getObject().getPath();
            if (path != null && path.startsWith(rootPath)) {
                Node node = nodeModel.getObject();
                int distance = 0;
                while (node.getDepth() > 0 && node.getPath().startsWith(rootPath)) {
                    distance++;
                    node = node.getParent();
                }
                Match match = new Match();
                match.setDistance(distance);
                return match;
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public IModel<String> getTitle() {
        return new StringResourceModel(getPluginConfig().getString("title", getPluginConfig().getName()), this, null);
    }

    public ResourceReference getIcon(IconSize type) {
        return null;
    }

    private static class SearchIcon extends CachingImage {
        private static final long serialVersionUID = 1L;

        public SearchIcon(DocumentCollection collection) {
            super("search-icon", new SearchIconModel(collection));
        }

        @Override
        protected void onDetach() {
            setDefaultModel(getDefaultModel());
            super.onDetach();
        }

    }

    private static class SearchIconModel extends LoadableDetachableModel<String> {
        private static final long serialVersionUID = 1L;
        private final DocumentCollection collection;

        public SearchIconModel(DocumentCollection collection) {
            this.collection = collection;
        }

        @Override
        protected String load() {
            if (collection.getType() == DocumentCollectionType.SEARCHRESULT) {
                return "cancel.png";
            } else {
                return "magnify.png";
            }
        }
    }

    private static class SearchScopeModel extends LoadableDetachableModel<String> {
        private static final long serialVersionUID = 1L;
        private final AjaxLink scopeLink;

        public SearchScopeModel(final AjaxLink scopeLink) {
            this.scopeLink = scopeLink;
        }

        @Override
        protected String load() {
            if (scopeLink.isEnabled()) {
                return "hippo-search-inactive-scope";
            } else {
                return "hippo-search-active-scope";
            }
        }
    }

}
