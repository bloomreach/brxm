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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.ILogoutService;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class LogoutPlugin extends RenderPlugin<Node> {

    public LogoutPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final String username = getSession().getJcrSession().getUserID();
        add(new Label("username", Model.of(username)));

        final ILogoutService logoutService = getPluginContext().getService(ILogoutService.SERVICE_ID, ILogoutService.class);
        final IDialogService dialogService = getDialogService();

        add(new LogoutLink("logout-link", logoutService, dialogService));
    }
}
