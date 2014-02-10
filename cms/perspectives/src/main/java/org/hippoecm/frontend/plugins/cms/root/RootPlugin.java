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
package org.hippoecm.frontend.plugins.cms.root;

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.CmsHeaderItem;
import org.hippoecm.frontend.PluginApplication;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.extjs.ExtHippoThemeBehavior;
import org.hippoecm.frontend.extjs.ExtWidgetRegistry;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.standards.tabs.TabbedPanel;
import org.hippoecm.frontend.plugins.standards.tabs.TabsPlugin;
import org.hippoecm.frontend.plugins.yui.ajax.AjaxIndicatorBehavior;
import org.hippoecm.frontend.plugins.yui.layout.PageLayoutBehavior;
import org.hippoecm.frontend.plugins.yui.layout.PageLayoutSettings;
import org.hippoecm.frontend.plugins.yui.layout.UnitBehavior;
import org.hippoecm.frontend.plugins.yui.layout.WireframeBehavior;
import org.hippoecm.frontend.plugins.yui.layout.WireframeSettings;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppBehavior;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppSettings;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.ListViewService;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.frontend.widgets.Pinger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.util.ExtResourcesHeaderItem;

public class RootPlugin extends TabsPlugin {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(RootPlugin.class);

    private boolean rendered = false;
    private final ExtWidgetRegistry extWidgetRegistry;

    @SuppressWarnings("unused")
    private String username;

    private AbstractView<IRenderService> view;
    private List<IRenderService> services;
    private ServiceTracker<IRenderService> tracker;

    private static class RenderServiceModel extends Model<IRenderService> {
        private static final long serialVersionUID = 1L;

        RenderServiceModel(IRenderService service) {
            super(service);
        }

        @Override
        public int hashCode() {
            return getObject().hashCode() * 19;
        }

        @Override
        public boolean equals(Object that) {
            if (that != null && that instanceof RenderServiceModel) {
                return ((RenderServiceModel) that).getObject() == getObject();
            }
            return false;
        }
    }

    public RootPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.containsKey("pinger.interval")) {
            add(new Pinger("pinger", config.getAsDuration("pinger.interval")));
        } else {
            add(new Pinger("pinger"));
        }

        String userID = getSession().getJcrSession().getUserID();
        username = new User(userID).getDisplayName();

        add(new Label("username", new PropertyModel(this, "username")));

        add(new LogoutLink("logout"));

        services = new LinkedList<IRenderService>();

        final IDataProvider<IRenderService> provider = new ListDataProvider<IRenderService>(services) {
            private static final long serialVersionUID = 1L;

            @Override
            public IModel<IRenderService> model(IRenderService object) {
                return new RenderServiceModel(object);
            }
        };

        view = new AbstractView<IRenderService>("view", provider) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<IRenderService> item) {
                IRenderService renderer = item.getModelObject();
                renderer.bind(RootPlugin.this, "item");
                item.add(renderer.getComponent());
                RootPlugin.this.onAddRenderService(item, renderer);
                item.setVisible(renderer.getComponent().isVisible());
            }

            @Override
            protected void destroyItem(Item<IRenderService> item) {
                IRenderService renderer = item.getModelObject();
                item.remove(renderer.getComponent());
                RootPlugin.this.onRemoveRenderService(item, renderer);
                renderer.unbind();
            }
        };

        String itemId = getItemId();
        if (itemId != null) {
            tracker = new ServiceTracker<IRenderService>(IRenderService.class) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onServiceAdded(IRenderService service, String name) {
                    log.debug("adding " + service + " to ListViewService at " + name);
                    services.add(service);
                }

                @Override
                public void onServiceChanged(IRenderService service, String name) {
                }

                @Override
                public void onRemoveService(IRenderService service, String name) {
                    log.debug("removing " + service + " from ListViewService at " + name);
                    services.remove(service);
                }
            };
            context.registerTracker(tracker, itemId);
        } else {
            log.warn("No item id configured");
        }

        add(view);

        add(new AjaxIndicatorBehavior());

        add(new ExtHippoThemeBehavior());

        extWidgetRegistry = new ExtWidgetRegistry(getPluginContext());
        add(extWidgetRegistry);

        addExtensionPoint("top");

        TabbedPanel tabbedPanel = getTabbedPanel();
        tabbedPanel.setIconType(IconSize.SMALL);
        tabbedPanel.add(new WireframeBehavior(new WireframeSettings(config.getPluginConfig("layout.wireframe"))));

        get("tabs:panel-container").add(new UnitBehavior("center"));
        get("tabs:tabs-container").add(new UnitBehavior("left"));

        PageLayoutSettings plSettings = new PageLayoutSettings();
        plSettings.setHeaderHeight(25);
        // TODO: update settings from config
        add(new PageLayoutBehavior(plSettings));
        add(new ResourceLink("faviconLink", ((PluginApplication)getApplication()).getPluginApplicationFavIconReference()));
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (!rendered) {
            WebAppSettings settings = new WebAppSettings();
            settings.setLoadCssFonts(true);
            settings.setLoadCssGrids(true);
            settings.setLoadCssReset(true);
            getPage().add(new WebAppBehavior(settings));
            rendered = true;
        }
        super.render(target);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(ExtResourcesHeaderItem.get());
        response.render(CmsHeaderItem.get());
    }

    protected String getItemId() {
        return getPluginConfig().getString(ListViewService.ITEM);
    }

    protected void onAddRenderService(Item<IRenderService> item, IRenderService renderer) {
    }

    protected void onRemoveRenderService(Item<IRenderService> item, IRenderService renderer) {
    }

}
