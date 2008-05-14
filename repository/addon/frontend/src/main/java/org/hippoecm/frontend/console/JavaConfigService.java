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
package org.hippoecm.frontend.console;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.impl.PluginConfig;
import org.hippoecm.frontend.plugin.config.ConfigValue;
import org.hippoecm.frontend.plugin.editor.MultiEditorPlugin;
import org.hippoecm.frontend.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.plugin.perspective.Perspective;
import org.hippoecm.frontend.plugin.render.RenderPlugin;
import org.hippoecm.frontend.plugin.root.RootPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowPlugin;
import org.hippoecm.frontend.plugins.standards.tabs.TabsPlugin;

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
        config.put(Plugin.CLASSNAME, new ConfigValue("org.hippoecm.frontend.plugin.root.RootPlugin"));
        config.put(RenderPlugin.WICKET_ID, new ConfigValue("service.root"));
        config.put(RootPlugin.DIALOG_ID, new ConfigValue("service.dialog"));
        config.put("browser", new ConfigValue("service.browser"));
        config.put("content", new ConfigValue("service.content"));
        plugins.add(config);

        config = new PluginConfig();
        config.put(Plugin.CLASSNAME, new ConfigValue("org.hippoecm.frontend.plugin.browser.BrowserPlugin"));
        config.put(RenderPlugin.WICKET_ID, new ConfigValue("service.browser"));
        config.put(RenderPlugin.MODEL_ID, new ConfigValue("model.node"));
        plugins.add(config);

        config = new PluginConfig();
        config.put(Plugin.CLASSNAME, new ConfigValue("org.hippoecm.frontend.plugins.standards.tabs.TabsPlugin"));
        config.put(RenderPlugin.WICKET_ID, new ConfigValue("service.content"));
        config.put(TabsPlugin.TAB_ID, new ConfigValue("service.tab"));
        plugins.add(config);

        config = new PluginConfig();
        config.put(Plugin.CLASSNAME, new ConfigValue("org.hippoecm.frontend.plugins.cms.browse.BrowserPerspective"));
        config.put(RenderPlugin.SERVICE_ID, new ConfigValue("service.browse"));
        config.put(RenderPlugin.WICKET_ID, new ConfigValue("service.tab"));
        config.put(RenderPlugin.MODEL_ID, new ConfigValue("service.browse.node"));
        config.put(Perspective.TITLE, new ConfigValue("browse"));
        config.put("browserBreadcrumbPlugin", new ConfigValue("service.browse.breadcrumb"));
        config.put("browserPlugin", new ConfigValue("service.browse.tree"));
        config.put("listPlugin", new ConfigValue("service.browse.list"));
        config.put("workflowsPlugin", new ConfigValue("service.browse.workflows"));
        plugins.add(config);

        config = new PluginConfig();
        config.put(Plugin.CLASSNAME, new ConfigValue("org.hippoecm.frontend.plugin.browser.BrowserPlugin"));
        config.put(RenderPlugin.WICKET_ID, new ConfigValue("service.browse.tree"));
        config.put(RenderPlugin.MODEL_ID, new ConfigValue("model.browse.node"));
        plugins.add(config);

        config = new PluginConfig();
        config.put(Plugin.CLASSNAME, new ConfigValue(
                "org.hippoecm.frontend.plugins.cms.browse.list.DocumentListingPlugin"));
        config.put(RenderPlugin.WICKET_ID, new ConfigValue("service.browse.list"));
        config.put(RenderPlugin.MODEL_ID, new ConfigValue("model.browse.node"));
        plugins.add(config);

        config = new PluginConfig();
        config.put(Plugin.CLASSNAME, new ConfigValue("org.hippoecm.frontend.plugin.render.ListViewPlugin"));
        config.put(RenderPlugin.WICKET_ID, new ConfigValue("service.browse.workflows"));
        config.put("item", new ConfigValue("service.browse.workflows.workflow"));
        plugins.add(config);

        config = new PluginConfig();
        config.put(Plugin.CLASSNAME, new ConfigValue("org.hippoecm.frontend.plugin.workflow.WorkflowPlugin"));
        config.put(WorkflowPlugin.VIEWER_ID, new ConfigValue("model.browse"));
        config.put(WorkflowPlugin.WORKFLOW_ID, new ConfigValue("workflows.id"));
        config.put(RenderPlugin.MODEL_ID, new ConfigValue("model.browse.node"));
        List<String> categories = new ArrayList<String>(2);
        categories.add("internal");
        categories.add("reviewed-action");
        config.put(WorkflowPlugin.CATEGORIES, new ParameterValue(categories));

        // instance properties
        config.put(RenderPlugin.WICKET_ID, new ConfigValue("service.browse.workflows.workflow"));
        config.put(RenderPlugin.DIALOG_ID, new ConfigValue("service.dialog"));
        plugins.add(config);

        config = new PluginConfig();
        config.put(Plugin.CLASSNAME, new ConfigValue("org.hippoecm.frontend.plugin.editor.MultiEditorPlugin"));
        config.put(MultiEditorPlugin.EDITOR_CLASS, new ConfigValue("org.hippoecm.frontend.plugin.editor.EditorPlugin"));
        config.put(MultiEditorPlugin.EDITOR_MODEL, new ConfigValue("model.node"));
        config.put(RenderPlugin.DIALOG_ID, new ConfigValue("service.dialog"));

        // instance properties
        config.put(RenderPlugin.WICKET_ID, new ConfigValue("service.tab"));
        config.put(MultiEditorPlugin.EDITOR_ID, new ConfigValue("editor.id"));
        plugins.add(config);
    }

    public List<PluginConfig> getPlugins() {
        return plugins;
    }
}
