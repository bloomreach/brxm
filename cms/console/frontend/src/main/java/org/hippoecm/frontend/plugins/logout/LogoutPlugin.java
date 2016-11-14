/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.logout;

import javax.jcr.Node;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.SystemInfoDataProvider;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TitleAttribute;
import org.hippoecm.frontend.service.ILogoutService;
import org.hippoecm.frontend.service.render.ListViewPlugin;

public class LogoutPlugin extends ListViewPlugin<Node> {
    private static SystemInfoDataProvider systemDataProvider = new SystemInfoDataProvider();

    public LogoutPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final String username = getSession().getJcrSession().getUserID();
        add(new Label("username", Model.of(username)));

        final ILogoutService logoutService = getPluginContext().getService(ILogoutService.SERVICE_ID, ILogoutService.class);
        add(new LogoutLink("logout-link", logoutService));

        final WebMarkupContainer logo = new WebMarkupContainer("logo");
        logo.add(TitleAttribute.set("Hippo Release Version: " + systemDataProvider.getReleaseVersion()));
        add(logo);
    }
}
