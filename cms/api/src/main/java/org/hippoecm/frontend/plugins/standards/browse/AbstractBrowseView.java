/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.browse;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBrowseView implements IBrowseService<JcrNodeModel>, IDetachable {
    private static final long serialVersionUID = 1L;


    private static final Logger log = LoggerFactory.getLogger(AbstractBrowseView.class);

    public static final String VIEWERS = "browser.viewers";
    public static final String SEARCH_VIEWS = "search.views";

    enum ViewerType {
        FOLDER, SEARCH
    }

    private String viewerName;
    private IClusterControl viewer;
    private ViewerType viewerType;

    private IPluginContext context;
    private IPluginConfig config;
    private BrowseService browseService;

    protected AbstractBrowseView(IPluginContext context, IPluginConfig config, JcrNodeModel document) {
        this.config = config;
        this.context = context;

        if (document == null) {
            document = new JcrNodeModel(config.getString("model.folder.root", "/"));
        }

        browseService = new BrowseService(context, config, document);

        @SuppressWarnings("unchecked")
        final IModelReference<Node> folderReference = context.getService(config.getString("model.folder"),
                IModelReference.class);
        if (folderReference != null) {
            context.registerService(new IObserver<IModelReference<Node>>() {
                private static final long serialVersionUID = 1L;

                public IModelReference<Node> getObservable() {
                    return folderReference;
                }

                public void onEvent(Iterator<? extends IEvent<IModelReference<Node>>> event) {
                    updateView();
                }

            }, IObserver.class.getName());
        }

        if (config.getString("model.search") != null) {
            @SuppressWarnings("unchecked")
            final IModelReference<BrowserSearchResult> searchModelReference = context.getService(config
                    .getString("model.search"), IModelReference.class);
            if (searchModelReference != null) {
                context.registerService(new IObserver<IModelReference<BrowserSearchResult>>() {
                    private static final long serialVersionUID = 1L;

                    public IModelReference<BrowserSearchResult> getObservable() {
                        return searchModelReference;
                    }

                    public void onEvent(Iterator<? extends IEvent<IModelReference<BrowserSearchResult>>> event) {
                        updateView();
                    }

                }, IObserver.class.getName());
            }
        }
    }

    public void start() {
        updateView();
    }

    protected void updateView() {
        @SuppressWarnings("unchecked")
        final IModelReference<Node> folderReference = context.getService(config.getString("model.folder"),
                IModelReference.class);
        if (folderReference == null) {
            return;
        }
        IModel<Node> folder = folderReference.getModel();

        IModel<BrowserSearchResult> search = null;
        if (config.getString("model.search") != null) {
            @SuppressWarnings("unchecked")
            final IModelReference<BrowserSearchResult> searchModelReference = context.getService(config
                    .getString("model.search"), IModelReference.class);
            if (searchModelReference != null) {
                search = searchModelReference.getModel();
            }
        }

        boolean shown = false;
        try {
            Node node = folder.getObject();
            if (node == null) {
                return;
            }

            if (search != null && search.getObject() != null) {
                shown = showSearch(search.getObject());
            } else {
                shown = showFolder(node);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        if (!shown) {
            stopViewer();
        }
    }

    private boolean showFolder(Node node) throws RepositoryException {
        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        String viewerFolder = config.getString(VIEWERS);
        for (String type : pluginConfigService.listClusters(config.getString(VIEWERS))) {
            if (node.isNodeType(type)) {
                if (viewerType != ViewerType.FOLDER || !type.equals(viewerName)) {
                    stopViewer();

                    IClusterConfig cluster = pluginConfigService.getCluster(viewerFolder + "/" + type);
                    IPluginConfig parameters = new JavaPluginConfig(config.getPluginConfig("browser.options"));
                    parameters.put("wicket.id", getExtensionPoint());
                    parameters.put("model.folder", config.getString("model.folder"));
                    parameters.put("model.document", config.getString("model.document"));
                    viewer = context.newCluster(cluster, parameters);
                    viewer.start();
                    viewerName = type;
                    viewerType = ViewerType.FOLDER;
                }
                return true;
            }
        }
        return false;
    }

    private boolean showSearch(BrowserSearchResult bsr) throws RepositoryException {
        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        String viewerFolder = config.getString(SEARCH_VIEWS);
        for (String queryName : pluginConfigService.listClusters(config.getString(SEARCH_VIEWS))) {
            if (queryName.equals(bsr.getQueryName())) {
                if (viewerType != ViewerType.SEARCH || !queryName.equals(viewerName)) {
                    stopViewer();

                    IClusterConfig cluster = pluginConfigService.getCluster(viewerFolder + "/" + queryName);
                    IPluginConfig parameters = new JavaPluginConfig(config.getPluginConfig("browser.options"));
                    parameters.put("wicket.id", getExtensionPoint());
                    parameters.put("model.search", config.getString("model.search"));
                    viewer = context.newCluster(cluster, parameters);
                    viewer.start();
                    viewerName = queryName;
                    viewerType = ViewerType.SEARCH;
                }
                return true;
            }
        }
        return false;
    }

    private void stopViewer() {
        if (viewer != null) {
            viewer.stop();
            viewer = null;
            viewerName = null;
            viewerType = null;
        }
    }

    public void browse(JcrNodeModel model) {
        browseService.browse(model);
        onBrowse();
    }

    public void detach() {
        browseService.detach();
    }

    protected abstract String getExtensionPoint();

    protected void onBrowse() {
    }

}
