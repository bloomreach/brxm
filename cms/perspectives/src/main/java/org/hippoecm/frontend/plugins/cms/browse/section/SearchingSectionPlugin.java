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
package org.hippoecm.frontend.plugins.cms.browse.section;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.l10n.ResourceBundleModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.NodeNameModel;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection.DocumentCollectionType;
import org.hippoecm.frontend.plugins.cms.browse.service.IBrowserSection;
import org.hippoecm.frontend.plugins.cms.widgets.SubmittingTextField;
import org.hippoecm.frontend.plugins.standards.browse.BrowserHelper;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.standards.search.GeneralSearchBuilder;
import org.hippoecm.frontend.plugins.standards.search.TextSearchBuilder;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchingSectionPlugin extends RenderPlugin implements IBrowserSection {

    private static final Logger log = LoggerFactory.getLogger(SearchingSectionPlugin.class);

    private static final String GALLERY_PATH = "/content/gallery";
    private static final String IMAGE_QUERY_NAME = "image";
    private static final IModel<BrowserSearchResult> NO_RESULTS = new Model<>(null);
    private static final String RESULT_LIMIT_PROPERTY = "result.limit";
    private static final int DEFAULT_RESULT_LIMIT = -1;

    private final FolderModelService folderService;
    private final DocumentCollection collection;
    private final WebMarkupContainer sectionTop;
    private final String rootPath;
    private final IModel<Node> rootModel;
    private final int limit;

    private IModel<Node> scopeModel;
    private String query;

    private transient boolean redrawSearch = false;

    public SearchingSectionPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        rootPath = config.getString("model.folder.root", "/");
        limit = config.getInt(RESULT_LIMIT_PROPERTY, DEFAULT_RESULT_LIMIT);
        rootModel = new JcrNodeModel(rootPath);
        scopeModel = new JcrNodeModel(rootPath);

        collection = new DocumentCollection();

        folderService = new FolderModelService(config, rootModel);
        collection.setFolder(folderService.getModel());

        collection.addListener(this::redrawSearch);

        final Form form = new Form("form") {

            @Override
            protected void onBeforeRender() {
                redrawSearch = false;
                super.onBeforeRender();
            }

            @Override
            protected void onSubmit() {
            }
        };
        form.setOutputMarkupId(true);

        final TextField<String> tx = new SubmittingTextField("searchBox", PropertyModel.of(this, "query")) {
            @Override
            public void onEnter(final AjaxRequestTarget target) {
                super.onEnter(target);
                updateSearch(true);
            }
        };
        tx.setLabel(Model.of(getString("placeholder")));
        form.add(tx);

        final AjaxSubmitLink browseLink = new AjaxSubmitLink("toggle") {
            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                if (hasSearchResult()) {
                    collection.setSearchResult(NO_RESULTS);
                    query = "";
                } else {
                    updateSearch(true);
                }
            }
        };
        browseLink.add(createSearchIcon("search-icon", collection));
        form.add(browseLink);

        final WebMarkupContainer scopeContainer = new WebMarkupContainer("scope-container") {
            @Override
            public boolean isVisible() {
                return hasSearchResult() && !rootModel.equals(scopeModel);
            }
        };

        final AjaxLink scopeLink = new AjaxLink("scope") {

            @Override
            public void onClick(final AjaxRequestTarget target) {
                folderService.setModel(scopeModel);
            }

            @Override
            public boolean isEnabled() {
                return hasSearchResult() && !rootModel.equals(scopeModel) && !scopeModel.equals(folderService.getModel());
            }

        };
        scopeContainer.add(CssClass.append(new SearchScopeModel(scopeLink)));
        scopeLink.add(new Label("scope-label", new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                return new NodeNameModel(scopeModel).getObject();
            }

        }).setRenderBodyOnly(true));
        scopeContainer.add(scopeLink);
        form.add(scopeContainer);

        final WebMarkupContainer allContainer = new WebMarkupContainer("all-container") {
            @Override
            public boolean isVisible() {
                return hasSearchResult();
            }
        };
        final AjaxLink allLink = new AjaxLink("all") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                final IModel<Node> backup = folderService.getModel();
                folderService.setModel(rootModel);
                scopeModel = backup;
            }

            @Override
            public boolean isEnabled() {
                return hasSearchResult() && !rootModel.equals(folderService.getModel());
            }
        };
        allContainer.add(CssClass.append(new SearchScopeModel(allLink)));
        allContainer.add(allLink);
        form.add(allContainer);

        sectionTop = new WebMarkupContainer("section-top");
        sectionTop.setOutputMarkupId(true);
        sectionTop.add(CssClass.append(ReadOnlyModel.of(() -> hasSearchResult() ? "search-result" : "")));
        sectionTop.add(form);
        add(sectionTop);
    }

    void redrawSearch() {
        redrawSearch = true;
    }

    private void updateSearch(final boolean forceQuery) {
        final IModel<Node> model = folderService.getModel();
        String scope = "/";
        if (model instanceof JcrNodeModel) {
            scope = ((JcrNodeModel) model).getItemModel().getPath();
        }
        if (forceQuery || hasSearchResult()) {
            if (Strings.isEmpty(query)) {
                collection.setSearchResult(NO_RESULTS);
            } else {
                final String queryName = StringUtils.startsWith(scope, GALLERY_PATH) ?
                        IMAGE_QUERY_NAME : GeneralSearchBuilder.TEXT_QUERY_NAME;
                final TextSearchBuilder sb = new TextSearchBuilder(queryName);
                sb.setScope(new String[]{scope});
                sb.setWildcardSearch(true);
                sb.setText(query);
                sb.setIncludePrimaryTypes(getPluginConfig().getStringArray("nodetypes"));
                sb.setLimit(limit);
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
    public void render(final PluginRequestTarget target) {
        if (target != null) {
            if (redrawSearch && isVisibleInHierarchy()) {
                target.add(sectionTop);
            }
        }
        super.render(target);
    }

    public void onFolderChange(final IModel<Node> model) {
        folderService.updateModel(model);
        collection.setFolder(model);

        if (hasSearchResult()) {
            updateSearch(false);
        }
    }

    public void select(final IModel<Node> document) {
        if (document == null) {
            return;
        }

        boolean toBrowseMode = false;
        if (hasSearchResult()) {
            if (!BrowserHelper.isFolder(document)) {
                toBrowseMode = true;
                final Node docNode = document.getObject();
                if (docNode != null) {
                    final BrowserSearchResult bsr = collection.getSearchResult().getObject();
                    final QueryResult result = bsr.getQueryResult();
                    try {
                        final NodeIterator nodes = result.getNodes();
                        while (nodes.hasNext()) {
                            final Node node = nodes.nextNode();
                            if (node != null && node.getDepth() > 0) {
                                final Node parent = node.getParent();
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
                    } catch (final RepositoryException ex) {
                        log.error("Error processing query results", ex);
                        return;
                    }
                }
            } else {
                if (document.equals(folderService.getModel())) {
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
            collection.setSearchResult(NO_RESULTS);
        }
    }

    public DocumentCollection getCollection() {
        return collection;
    }

    public Match contains(final IModel<Node> nodeModel) {
        try {
            final String path = nodeModel.getObject().getPath();
            if (path != null && path.startsWith(rootPath)) {
                Node node = nodeModel.getObject();
                int distance = 0;
                while (node.getDepth() > 0 && node.getPath().startsWith(rootPath)) {
                    distance++;
                    node = node.getParent();
                }
                final Match match = new Match();
                match.setDistance(distance);
                return match;
            }
        } catch (final RepositoryException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public IModel<String> getTitle() {
        final String titleKey = getPluginConfig().getString("title", getPluginConfig().getName());
        return new ResourceBundleModel(getBundleName(), titleKey);
    }

    @Override
    protected String getBundleName() {
        return "hippo:cms.sections";
    }

    @Override
    public ResourceReference getIcon(final IconSize type) {
        return null;
    }

    @Override
    protected void onDetach() {
        rootModel.detach();
        scopeModel.detach();
        super.onDetach();
    }

    private boolean hasSearchResult() {
        return collection.getType() == DocumentCollectionType.SEARCHRESULT;
    }

    private Component createSearchIcon(final String id, final DocumentCollection collection) {
        final IModel<Icon> iconModel = new LoadableDetachableModel<Icon>() {
            @Override
            protected Icon load() {
                return collection.getType() == DocumentCollectionType.SEARCHRESULT ? Icon.TIMES : Icon.SEARCH;
            }
        };
        return HippoIcon.fromSprite(id, iconModel);
    }

    private class FolderModelService extends ModelReference<Node> {

        FolderModelService(final IPluginConfig config, final IModel<Node> document) {
            super(config.getString("model.folder"), document);
        }

        public void updateModel(final IModel<Node> model) {
            if (hasSearchResult()
                    || (getModel() != null && getModel().equals(scopeModel))) {
                scopeModel = model;
            }
            super.setModel(model);
        }

        @Override
        public void setModel(final IModel<Node> model) {
            if (model == null) {
                throw new IllegalArgumentException("invalid folder model null");
            } else if (model.getObject() == null) {
                log.warn("Node no longer exists");
                return;
            }
            onFolderChange(model);
        }
    }

    private static class SearchScopeModel extends LoadableDetachableModel<String> {

        private final AjaxLink scopeLink;

        SearchScopeModel(final AjaxLink scopeLink) {
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
