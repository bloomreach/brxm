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
package org.hippoecm.repository.frontend.plugin.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Acts as default configuration if custom configuration fails
 */
public class PluginJavaConfig implements PluginConfig {
    private static final long serialVersionUID = 1L;

    public PluginDescriptor getRoot() {
        return new PluginDescriptor("0:rootPanel", "org.hippoecm.repository.plugins.admin.RootPlugin");
    }

    public List getChildren(PluginDescriptor pluginDescriptor) {
        List result = new ArrayList();
        if (pluginDescriptor.getId().equals("rootPanel")) {
            result.add(new PluginDescriptor("0:rootPanel:navigationPanel",
                    "org.hippoecm.repository.plugins.admin.browser.BrowserPlugin"));
            result.add(new PluginDescriptor("0:rootPanel:menuPanel",
                    "org.hippoecm.repository.plugins.admin.menu.MenuPlugin"));
            result.add(new PluginDescriptor("0:rootPanel:contentPanel",
                    "org.hippoecm.repository.plugins.admin.editor.EditorPlugin"));
        }
        return result;
    }
}
