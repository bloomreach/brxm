/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.time.Duration;
import org.hippoecm.frontend.CmsHeaderItem;
import org.hippoecm.frontend.PluginApplication;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.extjs.ExtHippoThemeBehavior;
import org.hippoecm.frontend.extjs.ExtWidgetRegistry;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.logout.ActiveLogoutPlugin;
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
import org.hippoecm.frontend.service.ILogoutService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.ListViewService;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.usagestatistics.UsageStatisticsHeaderItem;
import org.hippoecm.frontend.useractivity.MonitorExtUserActivityHeaderItem;
import org.hippoecm.frontend.useractivity.UserActivityHeaderItem;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.frontend.widgets.Pinger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.util.ExtResourcesHeaderItem;

public class RootPlugin extends TabsPlugin {

    private static final Logger log = LoggerFactory.getLogger(RootPlugin.class);

    public static final String CONFIG_PINGER_INTERVAL = "pinger.interval";
    public static final String CONFIG_MAX_INACTIVE_INTERVAL_MINUTES = "max.inactive.interval.minutes";

    private boolean rendered = false;
    private final ExtWidgetRegistry extWidgetRegistry;

    private AbstractView<IRenderService> view;
    private List<IRenderService> services;
    private ServiceTracker<IRenderService> tracker;

    private static class RenderServiceModel extends Model<IRenderService> {

        RenderServiceModel(IRenderService service) {
            super(service);
        }

        @Override
        public int hashCode() {
            return getObject().hashCode() * 19;
        }

        @Override
        public boolean equals(Object that) {
            return that instanceof RenderServiceModel && ((RenderServiceModel) that).getObject() == getObject();
        }
    }

    public RootPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        // keep all feedback messages after each request cycle
        getApplication().getApplicationSettings().setFeedbackMessageCleanupFilter(IFeedbackMessageFilter.NONE);

        addPinger();

        add(new Label("pageTitle", getString("page.title", null, "Hippo CMS 10")));

        addUserMenu();

        services = new LinkedList<>();

        final IDataProvider<IRenderService> provider = new ListDataProvider<IRenderService>(services) {
            @Override
            public IModel<IRenderService> model(IRenderService object) {
                return new RenderServiceModel(object);
            }
        };

        view = new AbstractView<IRenderService>("view", provider) {

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

        if (config.containsKey("top")) {
            log.warn("Usage of property 'top' on the RootPlugin is deprecated. The documents tabs is now configured " +
                    "as an extension. Add a value to property wicket.extensions named 'extension.tabs.documents' and " +
                    "add a property named 'extension.tabs.documents' with the value of the document tabs service, " +
                    "by default it's 'service.browse.tabscontainer'.");
        }

        TabbedPanel tabbedPanel = getTabbedPanel();
        tabbedPanel.setIconType(IconSize.L);
        tabbedPanel.add(new WireframeBehavior(new WireframeSettings(config.getPluginConfig("layout.wireframe"))));

        get("tabs:panel-container").add(new UnitBehavior("center"));
        get("tabs:tabs-container").add(new UnitBehavior("left"));

        final PageLayoutSettings pageLayoutSettings = getPageLayoutSettings(config);
        add(new PageLayoutBehavior(pageLayoutSettings));
        add(new ResourceLink("faviconLink", ((PluginApplication)getApplication()).getPluginApplicationFavIconReference()));
    }

    private void addPinger() {
        final Duration pingerInterval = getPluginConfig().getAsDuration(CONFIG_PINGER_INTERVAL);
        add(Pinger.every(pingerInterval));
    }

    private void addUserMenu() {
        final ILogoutService logoutService = getPluginContext().getService(ILogoutService.SERVICE_ID, ILogoutService.class);
        add(new UserMenu("userMenu", getCurrentUser(), logoutService));
        add(new ActiveLogoutPlugin("activeLogout", getMaxInactiveIntervalMinutes(), logoutService));
    }

    private Integer getMaxInactiveIntervalMinutes() {
        return getPluginConfig().getAsInteger(CONFIG_MAX_INACTIVE_INTERVAL_MINUTES, WebApplicationHelper.getMaxInactiveIntervalMinutes());
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (!rendered) {
            WebAppSettings settings = new WebAppSettings();
            settings.setLoadCssGrids(true);
            getPage().add(new WebAppBehavior(settings));
            rendered = true;
        }
        final IDataProvider<IRenderService> provider = view.getDataProvider();
        final Iterator<? extends IRenderService> children = provider.iterator(0, provider.size());
        while (children.hasNext()) {
            IRenderService child = children.next();
            child.render(target);
        }
        super.render(target);
    }

    @Override
    public void onComponentTag(final ComponentTag tag) {
        final Response response = RequestCycle.get().getResponse();
        response.write(Icon.getIconSprite());
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(CmsHeaderItem.get());
        response.render(ExtResourcesHeaderItem.get());

        final UserActivityHeaderItem userActivity = new UserActivityHeaderItem(getMaxInactiveIntervalMinutes());
        response.render(new MonitorExtUserActivityHeaderItem(userActivity));

        response.render(UsageStatisticsHeaderItem.get());
    }

    protected String getItemId() {
        return getPluginConfig().getString(ListViewService.ITEM);
    }

    protected void onAddRenderService(Item<IRenderService> item, IRenderService renderer) {
    }

    protected void onRemoveRenderService(Item<IRenderService> item, IRenderService renderer) {
    }

    private User getCurrentUser() {
        final String userID = getSession().getJcrSession().getUserID();
        return new User(userID);
    }

    private PageLayoutSettings getPageLayoutSettings(final IPluginConfig config) {
        final IPluginConfig pageLayoutConfig = config.getPluginConfig("layout.page");
        if (pageLayoutConfig != null) {
            return new PageLayoutSettings(pageLayoutConfig);
        } else {
            log.warn("Could not find page layout settings at node 'layout.page', falling back to built-in settings");
            PageLayoutSettings settings = new PageLayoutSettings();
            settings.setFooterHeight(28);
            return settings;
        }
    }
}
