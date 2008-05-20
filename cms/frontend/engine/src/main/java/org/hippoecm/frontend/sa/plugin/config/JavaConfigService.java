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
package org.hippoecm.frontend.sa.plugin.config;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.sa.core.impl.PluginConfig;

/**
 * Hardcoded plugin configuration.
 * It uses only core plugins and shows the Hippo ECM Admin Console.
 */
public class JavaConfigService implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<PluginConfig> plugins;

    public JavaConfigService() {
        plugins = new LinkedList<PluginConfig>();

        PluginConfig config = new PluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.sa.plugin.root.RootPlugin");
        config.put("wicket.id", "service.root");
        config.put("wicket.dialog", "service.dialog");
        config.put("content", "service.content");
        plugins.add(config);

        config = new PluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.sa.plugins.standards.tabs.TabsPlugin");
        config.put("wicket.id", "service.content");
        config.put("tabs", "service.tab");
        plugins.add(config);

        config = new PluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.sa.plugins.cms.browse.BrowserPerspective");
        config.put("service.pid", "service.browse");
        config.put("wicket.id", "service.tab");
        config.put("wicket.model", "service.browse.node");
        config.put("perspective.title", "browse");
        config.put("browserBreadcrumbPlugin", "service.browse.breadcrumb");
        config.put("browserPlugin", "service.browse.tree");
        config.put("listPlugin", "service.browse.list");
        config.put("workflowsPlugin", "service.browse.workflows");
        plugins.add(config);

        config = new PluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.sa.plugin.browser.BrowserPlugin");
        config.put("wicket.id", "service.browse.tree");
        config.put("wicket.model", "model.browse.node");
        plugins.add(config);

        config = new PluginConfig();
        config.put("plugin.class", 
                "org.hippoecm.frontend.sa.plugins.cms.browse.list.DocumentListingPlugin");
        config.put("wicket.id", "service.browse.list");
        config.put("wicket.model", "model.browse.node");
        plugins.add(config);

        config = new PluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.sa.plugin.render.ListViewPlugin");
        config.put("wicket.id", "service.browse.workflows");
        config.put("item", "service.browse.workflows.workflow");
        plugins.add(config);

        config = new PluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.sa.plugin.workflow.WorkflowPlugin");
        config.put("workflow.viewer", "service.edit");
        config.put("workflow.display", "workflows.id");
        config.put("wicket.model", "model.browse.node");
        config.put("workflow.categories", new String[] {"internal", "reviewed-action"});

        // instance properties
        config.put("wicket.id", "service.browse.workflows.workflow");
        config.put("wicket.dialog", "service.dialog");
        plugins.add(config);

        config = new PluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.sa.plugin.editor.MultiEditorPlugin");
        config.put("editor.class", "org.hippoecm.frontend.sa.plugin.editor.EditorPlugin");
        config.put("service.pid", "service.edit");
        config.put("wicket.dialog", "service.dialog");

        // instance properties
        config.put("wicket.id", "service.tab");
        config.put("editor", "editor.id");
        plugins.add(config);
    }

    public List<PluginConfig> getPlugins() {
        return plugins;
    }
}
