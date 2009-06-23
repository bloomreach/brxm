/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugin.config.impl;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceFactory;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuiltinApplicationFactory implements IApplicationFactory {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(BuiltinApplicationFactory.class);

    public BuiltinApplicationFactory() {
    }

    public IServiceFactory<IPluginConfigService> getDefaultApplication() {
        return getApplication("console");
    }
    
    public IServiceFactory<IPluginConfigService> getApplication(String name) {
        log.info("Starting builtin application: " + name);
        JavaConfigService configService;
        if ("login".equals(name)) {
            configService = new JavaConfigService("login");
        } else {
            configService = new JavaConfigService("console");
        }
        configService.addClusterConfig("login", initLogin());
        configService.addClusterConfig("console", initConsole());
        return new Factory(configService);
    }

    private IClusterConfig initLogin() {
        JavaClusterConfig plugins = new JavaClusterConfig();

        IPluginConfig config = new JavaPluginConfig("login");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.login.LoginPlugin");
        config.put("wicket.id", "service.root");
        plugins.addPlugin(config);

        return plugins;
    }

    private IClusterConfig initConsole() {
        JavaClusterConfig plugins = new JavaClusterConfig();

        IPluginConfig config = new JavaPluginConfig("root");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.RootPlugin");
        config.put("wicket.id", "service.root");
        config.put("wicket.dialog", "service.dialog");
        config.put("wicket.model", "service.model");

        String[] extensions = new String[] { "extension.browser", "extension.breadcrumb", "extension.editor", "extension.logout", "extension.menu" };
        config.put("wicket.extensions", extensions);

        config.put("extension.browser", "service.browser");
        config.put("extension.breadcrumb", "service.breadcrumb");
        config.put("extension.editor", "service.editor");
        config.put("extension.menu", "service.menu");
        config.put("extension.logout", "service.logout");
        plugins.addPlugin(config);

        config = new JavaPluginConfig("browser");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.browser.BrowserPlugin");
        config.put("wicket.id", "service.browser");
        config.put("wicket.model", "service.model");
        plugins.addPlugin(config);

        config = new JavaPluginConfig("breadcrumb");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.breadcrumb.BreadcrumbPlugin");
        config.put("wicket.id", "service.breadcrumb");
        config.put("wicket.model", "service.model");
        plugins.addPlugin(config);

        config = new JavaPluginConfig("editor");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.editor.EditorPlugin");
        config.put("wicket.id", "service.editor");
        config.put("wicket.model", "service.model");
        plugins.addPlugin(config);

        config = new JavaPluginConfig("menu");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.menu.MenuPlugin");
        config.put("wicket.id", "service.menu");
        config.put("wicket.model", "service.model");
        config.put("wicket.dialog", "service.dialog");
        plugins.addPlugin(config);

        config = new JavaPluginConfig("logout");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.logout.LogoutPlugin");
        config.put("wicket.id", "service.logout");
        config.put("wicket.model", "service.model");
        config.put("wicket.dialog", "service.logout.dialog");
        plugins.addPlugin(config);

        return plugins;
    }

    private static class Factory implements IServiceFactory<IPluginConfigService> {
        private static final long serialVersionUID = 1L;

        IPluginConfigService service;

        Factory(IPluginConfigService service) {
            this.service = service;
        }

        public IPluginConfigService getService(IPluginContext context) {
            return service;
        }

        public Class<? extends IPluginConfigService> getServiceClass() {
            return IPluginConfigService.class;
        }

        public void releaseService(IPluginContext context, IPluginConfigService service) {
        }
    }

}
