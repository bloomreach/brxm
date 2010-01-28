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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection;
import org.hippoecm.frontend.plugins.cms.browse.service.IBrowserSection;
import org.hippoecm.frontend.plugins.standards.browse.BrowserHelper;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
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
            selectFolder(model);
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
                        setSearch(model);
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
            setSearch(model);
        }

    }

    private String rootPath;
    private FolderModelService folderService;
    private SearchModelService searchModelService;
    private DocumentCollection collection;

    private String query;

    public SearchingSectionPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        this.rootPath = config.getString("model.folder.root", "/");

        collection = new DocumentCollection();

        folderService = new FolderModelService(config, new JcrNodeModel(rootPath));
        searchModelService = new SearchModelService(config);
        collection.setFolder(folderService.getModel());

        TextField tx = new TextField("searchBox", new PropertyModel(this, "query"));
        tx.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateSearch();
            }
        });
        add(tx);
    }

    private void updateSearch() {
            IModel<Node> model = folderService.getModel();
            String scope = "/";
            if (model instanceof JcrNodeModel) {
                scope = ((JcrNodeModel) model).getItemModel().getPath();
            }
            if (Strings.isEmpty(query)) {
                searchModelService.setModel(new Model(null));
            } else {
                TextSearchBuilder sb = new TextSearchBuilder();
                sb.setScope(new String[] { scope });
                sb.setText(query);
                searchModelService.setModel(sb.getResultModel());
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

    public void selectFolder(IModel<Node> model) {
        folderService.updateModel(model);
        collection.setFolder(model);

        updateSearch();
    }

    public void setSearch(IModel<BrowserSearchResult> model) {
        searchModelService.updateModel(model);
        collection.setSearchResult(searchModelService.getModel());
    }

    public void select(IModel<Node> folder) {
        while (!BrowserHelper.isFolder(folder)) {
            folder = BrowserHelper.getParent(folder);
        }

        folderService.updateModel(folder);
        collection.setFolder(folder);
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
