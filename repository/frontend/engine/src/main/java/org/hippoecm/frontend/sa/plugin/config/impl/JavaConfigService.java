/*
 * Copyright 2008 Hippo
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

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.frontend.sa.plugin.config.IClusterConfig;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfigService;

public class JavaConfigService implements IPluginConfigService {
    private static final long serialVersionUID = 1L;

    private Map<String, IClusterConfig> builtinConfigs;

    public JavaConfigService() {
        builtinConfigs = new HashMap<String, IClusterConfig>();
        builtinConfigs.put("login", initLogin());
        builtinConfigs.put("console", initConsole());
        builtinConfigs.put("cms", initCms());
        builtinConfigs.put("prototype", initPrototype());
    }

    public IClusterConfig getPlugins(String key) {
        return builtinConfigs.get(key);
    }
    
    public IClusterConfig getDefaultCluster() {
        return builtinConfigs.get("console");
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

    private IClusterConfig initCms() {
        JavaClusterConfig plugins = new JavaClusterConfig();

        IPluginConfig config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.cms.root.sa.RootPlugin");
        config.put("wicket.id", "service.root");
        config.put("wicket.dialog", "service.dialog");
        config.put("tabsPlugin", "service.tabs");
        config.put("logoutPlugin", "service.logout");
        plugins.addPlugin(config);

        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.standards.sa.tabs.TabsPlugin");
        config.put("wicket.id", "service.tabs");
        config.put("tabs", "service.tab");
        plugins.addPlugin(config);

        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.cms.dashboard.sa.DashboardPerspective");
        config.put("wicket.id", "service.tab");
        config.put("wicket.model", "service.dashboard.node");
        config.put("service.pid", "service.dashboard");
        config.put("perspective.title", "Dashboard");
        plugins.addPlugin(config);
        
        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.cms.browse.sa.BrowserPerspective");
        config.put("wicket.id", "service.tab");
        config.put("wicket.model", "service.browse.node");
        config.put("service.pid", "service.browse");
        config.put("perspective.title", "Browse");
        plugins.addPlugin(config);
        
        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.logout.LogoutPlugin");
        config.put("wicket.id", "service.logout");
        config.put("wicket.model", "service.model");
        config.put("wicket.dialog", "service.logout.dialog");
        plugins.addPlugin(config);
        
        return plugins;
    }

    private IClusterConfig initConsole() {
        JavaClusterConfig plugins = new JavaClusterConfig();

        IPluginConfig config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.RootPlugin");
        config.put("wicket.id", "service.root");
        config.put("wicket.dialog", "service.dialog");
        config.put("browserPlugin", "service.browser");
        config.put("breadcrumbPlugin", "service.breadcrumb");
        config.put("editorPlugin", "service.editor");
        config.put("menuPlugin", "service.menu");
        config.put("logoutPlugin", "service.logout");
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

    private IClusterConfig initPrototype() {
        JavaClusterConfig plugins = new JavaClusterConfig();

        IPluginConfig config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.sa.plugin.root.RootPlugin");
        config.put("wicket.id", "service.root");
        config.put("wicket.dialog", "service.dialog");
        config.put("content", "service.content");
        plugins.addPlugin(config);

        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.standards.sa.tabs.TabsPlugin");
        config.put("wicket.id", "service.content");
        config.put("tabs", "service.tab");
        plugins.addPlugin(config);

        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.cms.browse.sa.BrowserPerspective");
        config.put("service.pid", "service.browse");
        config.put("wicket.id", "service.tab");
        config.put("wicket.model", "service.browse.node");
        config.put("perspective.title", "browse");
        config.put("browserBreadcrumbPlugin", "service.browse.breadcrumb");
        config.put("browserPlugin", "service.browse.tree");
        config.put("listPlugin", "service.browse.list");
        config.put("workflowsPlugin", "service.browse.workflows");
        plugins.addPlugin(config);

        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.browser.BrowserPlugin");
        config.put("wicket.id", "service.browse.tree");
        config.put("wicket.model", "model.browse.node");
        plugins.addPlugin(config);

        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.sa.plugins.cms.browse.list.DocumentListingPlugin");
        config.put("wicket.id", "service.browse.list");
        config.put("wicket.model", "model.browse.node");
        plugins.addPlugin(config);

        config = new JavaPluginConfig();                                    
        config.put("plugin.class", "org.hippoecm.frontend.sa.service.render.ListViewPlugin");
        config.put("wicket.id", "service.browse.workflows");
        config.put("item", "service.browse.workflows.workflow");
        plugins.addPlugin(config);

        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.sa.plugin.workflow.WorkflowPlugin");
        config.put("workflow.viewer", "service.edit");
        config.put("workflow.display", "workflows.id");
        config.put("wicket.model", "model.browse.node");
        config.put("workflow.categories", new String[] { "internal", "reviewed-action" });

        // instance properties
        config.put("wicket.id", "service.browse.workflows.workflow");
        config.put("wicket.dialog", "service.dialog");
        plugins.addPlugin(config);

        config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.sa.plugin.editor.MultiEditorPlugin");
        config.put("editor.class", "org.hippoecm.frontend.sa.template.editor.EditorPlugin");
        config.put("service.pid", "service.edit");
        config.put("wicket.dialog", "service.dialog");

        // instance properties
        config.put("wicket.id", "service.tab");
        config.put("editor", "editor.id");
        plugins.addPlugin(config);

        return plugins;
    }

}