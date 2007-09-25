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
package org.hippoecm.frontend.plugin.config;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.frontend.plugin.PluginDescriptor;

/**
 * Acts as default configuration if custom configuration fails
 */
public class PluginJavaConfig implements PluginConfig {
    private static final long serialVersionUID = 1L;

    private PluginDescriptor rootPlugin;
    private PluginDescriptor navigationPlugin;
    private PluginDescriptor menuPlugin;
    private PluginDescriptor loginPlugin;
    private PluginDescriptor contentPlugin;
    private PluginDescriptor workflowPlugin;

    public PluginJavaConfig() {
        rootPlugin = new PluginDescriptor("0:rootPlugin", 
                "org.hippoecm.frontend.plugins.admin.RootPlugin");
        navigationPlugin = new PluginDescriptor("0:rootPlugin:navigationPlugin",
                "org.hippoecm.frontend.plugins.admin.browser.BrowserPlugin");
        menuPlugin = new PluginDescriptor("0:rootPlugin:menuPlugin",
                "org.hippoecm.frontend.plugins.admin.menu.MenuPlugin");
        loginPlugin = new PluginDescriptor("0:rootPlugin:loginPlugin",
                "org.hippoecm.frontend.plugins.admin.login.LoginPlugin");
        contentPlugin = new PluginDescriptor("0:rootPlugin:contentPlugin",
                "org.hippoecm.frontend.plugins.admin.editor.EditorPlugin");
        workflowPlugin = new PluginDescriptor("0:rootPlugin:workflowPlugin",
                "org.hippoecm.frontend.plugin.empty.EmptyPlugin");
    }

    public PluginDescriptor getRoot() {
        return rootPlugin;

    }

    public List getChildren(PluginDescriptor pluginDescriptor) {
        List result = new ArrayList();
        if (pluginDescriptor.getPath().endsWith(":rootPlugin")) {
            result.add(navigationPlugin);
            result.add(menuPlugin);
            result.add(loginPlugin);
            result.add(contentPlugin);
            result.add(workflowPlugin);
        }
        return result;
    }

    public PluginDescriptor getPlugin(String path) {
        PluginDescriptor result = null;
        if (path.endsWith(":rootPlugin")) {
            result = rootPlugin;
        } else if (path.endsWith(":navigationPlugin")) {
            return navigationPlugin;
        } else if (path.endsWith(":menuPlugin")) {
            return menuPlugin;
        } else if (path.endsWith(":loginPlugin")) {
            return loginPlugin;
        } else if (path.endsWith(":contentPlugin")) {
            return contentPlugin;
        } else if (path.endsWith(":workflowPlugin")) {
            return workflowPlugin;
        }
        return result;
    }
}
