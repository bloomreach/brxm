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
import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.impl.PluginConfig;
import org.hippoecm.frontend.plugin.editor.MultiEditorPlugin;
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
        config.put(Plugin.CLASSNAME, "org.hippoecm.frontend.plugin.root.RootPlugin");
        config.put(RenderPlugin.WICKET_ID, "service.root");
        config.put(RootPlugin.DIALOG_ID, "service.dialog");
        config.put("content", "service.content");
        plugins.add(config);

        config = new PluginConfig();
        config.put(Plugin.CLASSNAME, "org.hippoecm.frontend.plugins.standards.tabs.TabsPlugin");
        config.put(RenderPlugin.WICKET_ID, "service.content");
        config.put(TabsPlugin.TAB_ID, "service.tab");
        plugins.add(config);

        config = new PluginConfig();
        config.put(Plugin.CLASSNAME, "org.hippoecm.frontend.plugins.cms.browse.BrowserPerspective");
        config.put(RenderPlugin.SERVICE_ID, "service.browse");
        config.put(RenderPlugin.WICKET_ID, "service.tab");
        config.put(RenderPlugin.MODEL_ID, "service.browse.node");
        config.put(Perspective.TITLE, "browse");
        config.put("browserBreadcrumbPlugin", "service.browse.breadcrumb");
        config.put("browserPlugin", "service.browse.tree");
        config.put("listPlugin", "service.browse.list");
        config.put("workflowsPlugin", "service.browse.workflows");
        plugins.add(config);

        config = new PluginConfig();
        config.put(Plugin.CLASSNAME, "org.hippoecm.frontend.plugin.browser.BrowserPlugin");
        config.put(RenderPlugin.WICKET_ID, "service.browse.tree");
        config.put(RenderPlugin.MODEL_ID, "model.browse.node");
        plugins.add(config);

        config = new PluginConfig();
        config.put(Plugin.CLASSNAME, 
                "org.hippoecm.frontend.plugins.cms.browse.list.DocumentListingPlugin");
        config.put(RenderPlugin.WICKET_ID, "service.browse.list");
        config.put(RenderPlugin.MODEL_ID, "model.browse.node");
        plugins.add(config);

        config = new PluginConfig();
        config.put(Plugin.CLASSNAME, "org.hippoecm.frontend.plugin.render.ListViewPlugin");
        config.put(RenderPlugin.WICKET_ID, "service.browse.workflows");
        config.put("item", "service.browse.workflows.workflow");
        plugins.add(config);

        config = new PluginConfig();
        config.put(Plugin.CLASSNAME, "org.hippoecm.frontend.plugin.workflow.WorkflowPlugin");
        config.put(WorkflowPlugin.VIEWER_ID, "service.edit");
        config.put(WorkflowPlugin.WORKFLOW_ID, "workflows.id");
        config.put(RenderPlugin.MODEL_ID, "model.browse.node");
        config.put(WorkflowPlugin.CATEGORIES, new String[] {"internal", "reviewed-action"});

        // instance properties
        config.put(RenderPlugin.WICKET_ID, "service.browse.workflows.workflow");
        config.put(RenderPlugin.DIALOG_ID, "service.dialog");
        plugins.add(config);

        config = new PluginConfig();
        config.put(Plugin.CLASSNAME, "org.hippoecm.frontend.plugin.editor.MultiEditorPlugin");
        config.put(MultiEditorPlugin.EDITOR_CLASS, "org.hippoecm.frontend.plugin.editor.EditorPlugin");
        config.put(Plugin.SERVICE_ID, "service.edit");
        config.put(RenderPlugin.DIALOG_ID, "service.dialog");

        // instance properties
        config.put(RenderPlugin.WICKET_ID, "service.tab");
        config.put(MultiEditorPlugin.EDITOR_ID, "editor.id");
        plugins.add(config);
    }

    public List<PluginConfig> getPlugins() {
        return plugins;
    }
}
