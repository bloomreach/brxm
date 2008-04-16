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
import org.hippoecm.frontend.core.PluginConfig;

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
        config.put(Plugin.NAME, "root");
        config.put(Plugin.CLASSNAME, "org.hippoecm.frontend.console.RootPlugin");
        config.put(RenderPlugin.WICKET_ID, "root");
        config.put("browser", "browser");
        config.put("content", "content");
        plugins.add(config);

        config = new PluginConfig();
        config.put(Plugin.NAME, "browser");
        config.put(Plugin.CLASSNAME, "org.hippoecm.frontend.console.browser.BrowserPlugin");
        config.put(RenderPlugin.WICKET_ID, "browser");
        config.put("model", "model");
        plugins.add(config);

        config = new PluginConfig();
        config.put(Plugin.NAME, "content");
        config.put(Plugin.CLASSNAME, "org.hippoecm.frontend.console.editor.EditorPlugin");
        config.put(RenderPlugin.WICKET_ID, "content");
        config.put("model", "model");
        plugins.add(config);
    }

    public List<PluginConfig> getPlugins() {
        return plugins;
    }
}
