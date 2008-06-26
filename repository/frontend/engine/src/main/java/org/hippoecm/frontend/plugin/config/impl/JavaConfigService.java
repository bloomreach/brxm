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

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;

class JavaConfigService implements IPluginConfigService {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private Map<String, IClusterConfig> builtinConfigs;

    private String defaultCluster;

    JavaConfigService(String defaultCluster) {
        this.defaultCluster = defaultCluster;
        builtinConfigs = new HashMap<String, IClusterConfig>();
        builtinConfigs.put("login", initLogin());
        builtinConfigs.put("console", initConsole());
    }

    public IClusterConfig getCluster(String key) {
        return builtinConfigs.get(key);
    }

    public IClusterConfig getDefaultCluster() {
        return builtinConfigs.get(defaultCluster);
    }

    public void detach() {
    }

    //privates

    private IClusterConfig initLogin() {
        JavaClusterConfig plugins = new JavaClusterConfig();

        IPluginConfig config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.login.LoginPlugin");
        config.put("wicket.id", "service.root");
        plugins.addPlugin(config);

        return plugins;
    }

    private IClusterConfig initConsole() {
        JavaClusterConfig plugins = new JavaClusterConfig();

        IPluginConfig config = new JavaPluginConfig();
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

        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.browser.BrowserPlugin");
        config.put("wicket.id", "service.browser");
        config.put("wicket.model", "service.model");
        plugins.addPlugin(config);

        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.breadcrumb.BreadcrumbPlugin");
        config.put("wicket.id", "service.breadcrumb");
        config.put("wicket.model", "service.model");
        plugins.addPlugin(config);

        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.editor.EditorPlugin");
        config.put("wicket.id", "service.editor");
        config.put("wicket.model", "service.model");
        plugins.addPlugin(config);

        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.menu.MenuPlugin");
        config.put("wicket.id", "service.menu");
        config.put("wicket.model", "service.model");
        config.put("wicket.dialog", "service.dialog");
        plugins.addPlugin(config);

        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.logout.LogoutPlugin");
        config.put("wicket.id", "service.logout");
        config.put("wicket.model", "service.model");
        config.put("wicket.dialog", "service.logout.dialog");
        plugins.addPlugin(config);

        return plugins;
    }

}
