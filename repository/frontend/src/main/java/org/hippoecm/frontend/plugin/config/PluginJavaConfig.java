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

    public PluginDescriptor getRoot() {
        return new PluginDescriptor("0:rootPlugin", "org.hippoecm.repository.plugins.admin.RootPlugin");
    }

    public List getChildren(PluginDescriptor pluginDescriptor) {
        List result = new ArrayList();
        if (pluginDescriptor.getId().equals("rootPlugin")) {
            result.add(new PluginDescriptor("0:rootPlugin:navigationPlugin",
                    "org.hippoecm.repository.plugins.admin.browser.BrowserPlugin"));
            result.add(new PluginDescriptor("0:rootPlugin:menuPlugin",
                    "org.hippoecm.repository.plugins.admin.menu.MenuPlugin"));
            result.add(new PluginDescriptor("0:rootPlugin:contentPlugin",
                    "org.hippoecm.repository.plugins.admin.editor.EditorPlugin"));
            result.add(new PluginDescriptor("0:rootPlugin:workflowPlugin",
                    "org.hippoecm.frontend.plugin.empty.EmptyPlugin"));
        }
        return result;
    }
}
