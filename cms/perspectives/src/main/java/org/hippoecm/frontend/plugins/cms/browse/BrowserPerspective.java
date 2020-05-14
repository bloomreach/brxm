/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ObservableModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.ParentApiCaller;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.plugins.standards.tabs.TabsPlugin;
import org.hippoecm.frontend.plugins.yui.layout.IExpandableCollapsable;
import org.hippoecm.frontend.plugins.yui.layout.WireframeBehavior;
import org.hippoecm.frontend.plugins.yui.layout.WireframeSettings;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.util.DocumentUtils;
import org.hippoecm.repository.util.JcrUtils;

public class BrowserPerspective extends Perspective {

    private static final String SERVICE_LISTING = "service.browse.list";
    private static final String SERVICE_TABS = "service.browse.editor";
    private static final String MODEL_DOCUMENT = "model.document";

    private static final JcrNodeModel NULL_MODEL = new JcrNodeModel((Node) null);

    private final WireframeBehavior wireframe;
    private IExpandableCollapsable listing;
    private TabsPlugin tabs;
    private ObservableModel<String> sectionModel;
    private ModelService<Node> nodeService;
    private BrowseState state;

    public BrowserPerspective(final IPluginContext context, final IPluginConfig config) {
        super(context, config, "content");

        state = new BrowseState();

        addExtensionPoint("center");
        addExtensionPoint("left");

        // register as the IRenderService for the browser service
        String browserId = config.getString("browser.id");
        context.registerService(this, browserId);

        context.registerTracker(new ServiceTracker<IExpandableCollapsable>(IExpandableCollapsable.class) {
            @Override
            protected void onServiceAdded(IExpandableCollapsable service, String name) {
                // Sync listing expanded state (collapsed is default) in case #render is not called
                if (listing == null && state.isExpanded()) {
                    service.expand();
                }
                listing = service;
                state.onListingChanged();
            }

            @Override
            protected void onRemoveService(IExpandableCollapsable service, String name) {
                listing = null;
            }
        }, SERVICE_LISTING);

        context.registerTracker(new ServiceTracker<TabsPlugin>(TabsPlugin.class) {
            @Override
            protected void onServiceAdded(TabsPlugin service, String name) {
                tabs = service;
            }

            @Override
            protected void onRemoveService(TabsPlugin service, String name) {
                tabs = null;
            }
        }, SERVICE_TABS);

        sectionModel = ObservableModel.from(context, Navigator.SELECTED_SECTION_MODEL);
        context.registerService(new IObserver<ObservableModel<String>>() {
            @Override
            public ObservableModel<String> getObservable() {
                return sectionModel;
            }

            @Override
            public void onEvent(final Iterator<? extends IEvent<ObservableModel<String>>> events) {
                state.onSectionChanged(sectionModel.getObject());
            }
        }, IObserver.class.getName());

        nodeService = new ModelService<Node>(config.getString(MODEL_DOCUMENT)) {
            @Override
            void onUpdateModel(final IModel<Node> oldModel, final IModel<Node> newModel) {
                final String newTab = JcrUtils.getNodePathQuietly(newModel.getObject());
                final String documentName = getDocumentName(newModel);

                updateNavLocation(newTab, documentName);

                state.onTabChanged(newTab);
            }

            String getDocumentName(IModel<Node> model) {
                try {
                    final IModel<String> nameModel = DocumentUtils.getDocumentNameModel(model);
                    
                    return nameModel != null ? nameModel.getObject() : "";
                } catch (RepositoryException e) {
                    return "";
                }
            }
        };

        final WireframeSettings wireframeSettings = new WireframeSettings(config.getPluginConfig("layout.wireframe"));
        add(wireframe = new WireframeBehavior(wireframeSettings) {
            @Override
            protected void onToggle(final boolean expand, final String position) {
                if (expand) {
                    state.onExpand();
                } else {
                    state.onCollapse();
                }
            }

            @Override
            protected void onExpandDefault() {
                state.onExpand();
            }
        });

        // Explicitly start with the expanded view
        wireframe.expandDefault();
    }

    @Override
    public void render(final PluginRequestTarget target) {
        if (state.processChanges(hasOpenTabs())) {
            if (state.isExpandDefault()) {
                wireframe.expandDefault();
            }
            if (state.isCollapseAll()) {
                wireframe.collapseAll();
            }
            if (state.isExpandListing() && isExpandCollapsable()) {
                listing.expand();
            }
            if (state.isCollapseListing() && isExpandCollapsable()) {
                listing.collapse();
            }
            if (state.isFocusTabs() && tabs != null) {
                tabs.focusRecentTab();
            }
            if (state.isBlurTabs() && tabs != null) {
                tabs.hide();
            }
            if (state.isShelveSelection()) {
                nodeService.setModel(NULL_MODEL);
            }
            if (state.isRestoreSelection()) {
                nodeService.setModel(new JcrNodeModel(state.getTab()));
            }
        }
        state.reset();

        super.render(target);
    }

    @Override
    protected void onDetach() {
        sectionModel.detach();
        nodeService.detach();

        super.onDetach();
    }

    @Override
    protected void onActivated() {
        super.onActivated();
        tabs.focusRecentTabUnlessHidden();
        updateNavLocation(tabs.getSelectedTabPath(), null);
    }

    @Override
    protected void onDeactivated() {
        super.onDeactivated();
        tabs.blurTabs();
        tabs.disableTabRefocus();
    }

    private boolean hasOpenTabs() {
        return tabs != null && tabs.hasOpenTabs();
    }

    private boolean isExpandCollapsable() {
        return listing != null && listing.isSupported();
    }

    private class ModelService<T> implements IDetachable {

        private IModelReference<T> reference;

        public ModelService(final String serviceId) {

            getPluginContext().registerTracker(new ServiceTracker<IModelReference>(IModelReference.class) {

                IObserver observer;

                @Override
                protected void onServiceAdded(final IModelReference service, String name) {
                    reference = service;

                    if (observer == null) {
                        getPluginContext().registerService(observer = new IObserver() {

                            public IObservable getObservable() {
                                return reference;
                            }

                            public void onEvent(Iterator events) {
                                IModelReference.IModelChangeEvent<T> event = (IModelReference.IModelChangeEvent<T>) events.next();
                                onUpdateModel(event.getOldModel(), event.getNewModel());
                            }
                        }, IObserver.class.getName());
                    }
                    super.onServiceAdded(service, name);
                }

                @Override
                protected void onRemoveService(IModelReference service, String name) {
                    super.onRemoveService(service, name);
                    if (observer != null) {
                        getPluginContext().unregisterService(observer, IObserver.class.getName());
                        observer = null;
                    }
                }
            }, serviceId);
        }

        void onUpdateModel(final IModel<T> oldModel, final IModel<T> newModel) {
        }

        T getObject() {
            if (reference != null) {
                return reference.getModel().getObject();
            }
            return null;
        }

        public T getModel() {
            if (reference != null) {
                return (T) reference.getModel();
            }
            return null;
        }

        void setModel(IModel<T> model) {
            if (reference != null) {
                reference.setModel(model);
            }
        }

        @Override
        public void detach() {
            if (reference != null) {
                reference.detach();
            }
        }

    }

    private void updateNavLocation(String selectedTabPath, String breadcrumbLabel) {
        final String appPath = getAppPath();
        final String path = Optional.ofNullable(selectedTabPath)
                .map(tabPath -> String.format("%s/path%s", appPath, tabPath))
                .orElse(appPath);
        new ParentApiCaller().updateNavLocation(path, breadcrumbLabel);
    }
}
