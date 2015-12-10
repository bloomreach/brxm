/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.time.Duration;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.logout.ActiveLogoutPlugin;
import org.hippoecm.frontend.plugins.console.behavior.ParameterHistoryBehavior;
import org.hippoecm.frontend.plugins.yui.layout.PageLayoutBehavior;
import org.hippoecm.frontend.plugins.yui.layout.PageLayoutSettings;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppBehavior;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppSettings;
import org.hippoecm.frontend.service.ILogoutService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.useractivity.UserActivityHeaderItem;
import org.hippoecm.frontend.util.MappingException;
import org.hippoecm.frontend.util.PluginConfigMapper;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.hippoecm.frontend.widgets.Pinger;

public class RootPlugin extends RenderPlugin {

    public static final String CONFIG_PINGER_INTERVAL = "pinger.interval";
    public static final String CONFIG_MAX_INACTIVE_INTERVAL_MINUTES = "max.inactive.interval.minutes";

    private boolean rendered = false;
    private ParameterHistoryBehavior parameterHistoryBehavior;

    public RootPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        addPinger();
        addActiveLogout();

        if (config.getString(RenderService.MODEL_ID) != null) {
            String modelId = config.getString(RenderService.MODEL_ID);
            ModelReference<Node> modelService = new ModelReference<>(modelId, new JcrNodeModel("/"));
            modelService.init(context);

            parameterHistoryBehavior = new ParameterHistoryBehavior(modelService);
            context.registerService(parameterHistoryBehavior, IObserver.class.getName());
            add(parameterHistoryBehavior);
        }

        PageLayoutSettings plSettings = new PageLayoutSettings();
        try {
            PluginConfigMapper.populate(plSettings, config.getPluginConfig("yui.config"));
        } catch (MappingException e) {
            throw new RuntimeException(e);
        }
        add(new PageLayoutBehavior(plSettings));

        add(new Label("pageTitle", getPageTitle(config)));

        final String faviconPath = config.getString("favicon.path", "console-red.ico");
        add(new ResourceLink("faviconLink", new PackageResourceReference(RootPlugin.class, faviconPath)));
    }

    private void addPinger() {
        final Duration pingerInterval = getPluginConfig().getAsDuration(CONFIG_PINGER_INTERVAL);
        add(new Pinger("pinger", pingerInterval));
    }

    private void addActiveLogout() {
        final ILogoutService logoutService = getPluginContext().getService(ILogoutService.SERVICE_ID, ILogoutService.class);
        add(new ActiveLogoutPlugin("activeLogout", getMaxInactiveIntervalMinutes(), logoutService));
    }

    private Integer getMaxInactiveIntervalMinutes() {
        return getPluginConfig().getAsInteger(CONFIG_MAX_INACTIVE_INTERVAL_MINUTES, WebApplicationHelper.getMaxInactiveIntervalMinutes());
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (!rendered) {
            WebAppSettings settings = new WebAppSettings();
            getPage().add(new WebAppBehavior(settings));
            rendered = true;
        }
        super.render(target);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(ConsoleHeaderItem.get());
        response.render(new UserActivityHeaderItem(getMaxInactiveIntervalMinutes()));
    }

    private String getPageTitle(IPluginConfig config) {
        StringBuilder pageTitle = new StringBuilder(config.getString("page.title", "Hippo CMS Console"));
        if(config.getAsBoolean("page.title.showservername", false)) {
            pageTitle.append(config.getString("page.title.separator", "@"));
            pageTitle.append(getServerName());
        }
        return pageTitle.toString();
    }

    private String getServerName() {
        final Object request = getRequest().getContainerRequest();
        if(request instanceof HttpServletRequest) {
            return ((HttpServletRequest) request).getServerName();
        }
        return StringUtils.EMPTY;
    }
}

