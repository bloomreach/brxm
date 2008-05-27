/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.sa.plugin.config.impl;

import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfigService;

//The builtin console application
class ConsoleConfigService implements IPluginConfigService {
    private static final long serialVersionUID = 1L;

    private List<IPluginConfig> plugins;

    ConsoleConfigService() {
        plugins = new LinkedList<IPluginConfig>();
        
        IPluginConfig config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.RootPlugin");
        config.put("wicket.id", "service.root");
        config.put("wicket.dialog", "service.dialog");
        config.put("browserPlugin", "service.browser");
        config.put("breadcrumbPlugin", "service.breadcrumb");
        config.put("editorPlugin", "service.editor");
        config.put("menuPlugin", "service.menu");
        config.put("logoutPlugin", "service.logout");
        plugins.add(config);
        
        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.browser.BrowserPlugin");
        config.put("wicket.id", "service.browser");
        config.put("wicket.model", "service.model");
        plugins.add(config);

        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.breadcrumb.BreadcrumbPlugin");
        config.put("wicket.id", "service.breadcrumb");
        config.put("wicket.model", "service.model");
        plugins.add(config);
        
        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.editor.EditorPlugin");
        config.put("wicket.id", "service.editor");
        config.put("wicket.model", "service.model");
        plugins.add(config);
        
        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.menu.MenuPlugin");
        config.put("wicket.id", "service.menu");
        config.put("wicket.model", "service.model");
        config.put("wicket.dialog", "service.dialog");
        plugins.add(config);
        
        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.logout.LogoutPlugin");
        config.put("wicket.id", "service.logout");
        config.put("wicket.model", "service.model");
        config.put("wicket.dialog", "service.logout.dialog");
        plugins.add(config);
    }

    public List<IPluginConfig> getPlugins(String key) {
        return plugins;
    }
    

}
