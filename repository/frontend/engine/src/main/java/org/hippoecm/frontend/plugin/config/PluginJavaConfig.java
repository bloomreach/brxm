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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.plugin.PluginDescriptor;

/**
 * Hardcoded plugin configuration.
 * It uses only core plugins and shows the Hippo ECM Admin Console.
 * @deprecated use org.hippoecm.frontend.sa.core.impl.JavaPluginConfig instead
 */
@Deprecated
public class PluginJavaConfig implements PluginConfig {
    private static final long serialVersionUID = 1L;

    private PluginDescriptor root;
    private PluginDescriptor login;
    
    private Map<String, PluginDescriptor> childrenOfRoot;

    public PluginJavaConfig() {
        String className = "org.hippoecm.frontend.plugins.admin.login.LoginPlugin";
        PluginDescriptor descriptor = new PluginDescriptor(Home.LOGIN_PLUGIN, className);
        descriptor.setWicketId(Home.ROOT_PLUGIN);
        login = descriptor;

        className = "org.hippoecm.frontend.plugins.admin.RootPlugin";
        descriptor = new PluginDescriptor(Home.ROOT_PLUGIN, className) {
            private static final long serialVersionUID = 1L;

            @Override
            public List<PluginDescriptor> getChildren() {
                List<PluginDescriptor> result = new ArrayList<PluginDescriptor>();
                result.addAll(childrenOfRoot.values());
                return result;
            }
        };
        root = descriptor;

        childrenOfRoot = new HashMap<String, PluginDescriptor>();

        className = "org.hippoecm.frontend.plugins.admin.browser.BrowserPlugin";
        descriptor = new PluginDescriptor("navigationPlugin", className);
        childrenOfRoot.put("navigationPlugin", descriptor);

        className = "org.hippoecm.frontend.plugins.admin.menu.MenuPlugin";
        descriptor = new PluginDescriptor("menuPlugin", className);
        childrenOfRoot.put("menuPlugin", descriptor);

        className = "org.hippoecm.frontend.plugins.admin.logout.LogoutPlugin";
        descriptor = new PluginDescriptor("logoutPlugin", className);
        childrenOfRoot.put("logoutPlugin", descriptor);

        className = "org.hippoecm.frontend.plugins.admin.editor.EditorPlugin";
        descriptor = new PluginDescriptor("contentPlugin", className);
        childrenOfRoot.put("contentPlugin", descriptor);

        className = "org.hippoecm.frontend.plugins.admin.breadcrumb.BreadcrumbPlugin";
        descriptor = new PluginDescriptor("breadcrumbPlugin", className);
        childrenOfRoot.put("breadcrumbPlugin", descriptor);
    }

    public PluginDescriptor getPlugin(String pluginId) {
        if (pluginId.equals(Home.ROOT_PLUGIN)) {
            return root;
        } else if (pluginId.equals(Home.LOGIN_PLUGIN)) {
            return login;
        }
        return childrenOfRoot.get(pluginId);
    }
}
