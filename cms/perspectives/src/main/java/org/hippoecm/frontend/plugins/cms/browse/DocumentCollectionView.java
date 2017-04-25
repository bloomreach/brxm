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
package org.hippoecm.frontend.plugins.cms.browse;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection.DocumentCollectionType;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DocumentCollectionView extends WebMarkupContainer {

    private static final Logger log = LoggerFactory.getLogger(DocumentCollectionView.class);

    public static final String FOLDER_VIEWERS = "browser.viewers";
    public static final String SEARCH_VIEWERS = "search.viewers";

    private String viewerName;
    private IClusterControl viewer;
    private DocumentCollectionType viewerType;

    private final ModelReference providerReference;
    private final IRenderService parentRenderService;
    private final IPluginContext context;
    private final IPluginConfig config;
    private transient boolean redraw = false;
    private IRenderService doclisting;

    public DocumentCollectionView(final String id, final IPluginContext context, final IPluginConfig config,
                                  final IModel<DocumentCollection> collection, final IRenderService parentRenderService) {
        super(id, collection);

        this.parentRenderService = parentRenderService;
        this.config = config;
        this.context = context;

        setOutputMarkupId(true);

        providerReference = new ModelReference(getProviderId(), new Model(null));
        providerReference.init(context);

        if (collection instanceof IObservable) {
            context.registerService(new IObserver() {

                public IObservable getObservable() {
                    return (IObservable) DocumentCollectionView.this.getDefaultModel();
                }

                public void onEvent(final Iterator events) {
                    updateView();
                }

            }, IObserver.class.getName());
        }

        add(new EmptyPanel("extension.list"));
        context.registerTracker(new ServiceTracker<IRenderService>(IRenderService.class) {

            @Override
            protected void onServiceAdded(final IRenderService service, final String name) {
                super.onServiceAdded(service, name);
                service.bind(DocumentCollectionView.this.parentRenderService, "extension.list");
                replace(service.getComponent());
                doclisting = service;
                redraw();
            }

            @Override
            protected void onRemoveService(final IRenderService service, final String name) {
                doclisting = null;
                replace(new EmptyPanel("extension.list"));
                service.unbind();
                super.onRemoveService(service, name);
                redraw();
            }

        }, getExtensionPoint());

        updateView();
    }

    protected abstract String getExtensionPoint();

    protected String getProviderId() {
        return context.getReference(parentRenderService).getServiceId() + ".provider";
    }

    protected DocumentCollection getCollection() {
        return (DocumentCollection) getDefaultModelObject();
    }

    protected void redraw() {
        redraw = true;
    }

    public void render(final PluginRequestTarget target) {
        if (redraw) {
            if (target != null) {
                target.add(this);
            }
        }
        if (doclisting != null) {
            doclisting.render(target);
        }
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        redraw = false;
    }

    protected void updateView() {
        boolean shown = false;
        final DocumentCollection collection = getCollection();
        if (collection != null) {
            try {
                switch (collection.getType()) {
                case FOLDER:
                    shown = showFolder(collection.getFolder().getObject());
                    break;
                case SEARCHRESULT:
                    shown = showSearch(collection.getSearchResult().getObject());
                    break;
                case UNKNOWN:
                    return;
                }
            } catch (final RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        if (!shown) {
            stopViewer();
        }
    }

    private boolean showFolder(final Node node) throws RepositoryException {
        final IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        final String viewerFolder = config.getString(FOLDER_VIEWERS);
        for (final String type : pluginConfigService.listClusters(config.getString(FOLDER_VIEWERS))) {
            if (node.isNodeType(type)) {
                if (viewerType != DocumentCollectionType.FOLDER || !type.equals(viewerName)) {
                    stopViewer();

                    providerReference.setModel(getCollection().getFolder());

                    final IClusterConfig cluster = pluginConfigService.getCluster(viewerFolder + "/" + type);
                    final IPluginConfig parameters = new JavaPluginConfig(config.getPluginConfig("browser.options"));
                    parameters.put("wicket.id", getExtensionPoint());
                    parameters.put("model.folder", getProviderId());
                    parameters.put("model.document", config.getString("model.document"));
                    viewer = context.newCluster(cluster, parameters);
                    viewer.start();
                    viewerName = type;
                    viewerType = DocumentCollectionType.FOLDER;
                } else {
                    providerReference.setModel(getCollection().getFolder());
                }
                return true;
            }
        }
        return false;
    }

    private boolean showSearch(final BrowserSearchResult bsr) throws RepositoryException {
        final IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        final String viewerFolder = config.getString(SEARCH_VIEWERS);
        for (final String queryName : pluginConfigService.listClusters(config.getString(SEARCH_VIEWERS))) {
            if (queryName.equals(bsr.getQueryName())) {
                if (viewerType != DocumentCollectionType.SEARCHRESULT || !queryName.equals(viewerName)) {
                    stopViewer();

                    providerReference.setModel(getCollection().getSearchResult());

                    final IClusterConfig cluster = pluginConfigService.getCluster(viewerFolder + "/" + queryName);
                    final IPluginConfig parameters = new JavaPluginConfig(config.getPluginConfig("browser.options"));
                    parameters.put("wicket.id", getExtensionPoint());
                    parameters.put("model.search", getProviderId());
                    parameters.put("model.document", config.getString("model.document"));
                    viewer = context.newCluster(cluster, parameters);
                    viewer.start();
                    viewerName = queryName;
                    viewerType = DocumentCollectionType.SEARCHRESULT;
                } else {
                    providerReference.setModel(getCollection().getSearchResult());
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

}
