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

import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.ChannelFactory;

/**
 * Hardcoded plugin configuration.
 * It uses only core plugins and shows the Hippo ECM Admin Console.
 */
public class PluginJavaConfig implements PluginConfig {
    private static final long serialVersionUID = 1L;

    private PluginDescriptor root;
    private Map<String, PluginDescriptor> childrenOfRoot;
    private ChannelFactory factory;

    public PluginJavaConfig() {
        factory = new ChannelFactory();
        Channel outgoing = factory.createChannel();

        String className = "org.hippoecm.frontend.plugins.admin.RootPlugin";
        PluginDescriptor descriptor = new PluginDescriptor("rootPlugin", className, outgoing);
        root = descriptor;

        childrenOfRoot = new HashMap<String, PluginDescriptor>();

        className = "org.hippoecm.frontend.plugins.admin.browser.BrowserPlugin";
        descriptor = new PluginDescriptor("navigationPlugin", className, factory.createChannel());
        childrenOfRoot.put("navigationPlugin", descriptor);

        className = "org.hippoecm.frontend.plugins.admin.menu.MenuPlugin";
        descriptor = new PluginDescriptor("menuPlugin", className, factory.createChannel());
        childrenOfRoot.put("menuPlugin", descriptor);

        className = "org.hippoecm.frontend.plugins.admin.logout.LogoutPlugin";
        descriptor = new PluginDescriptor("logoutPlugin", className, factory.createChannel());
        childrenOfRoot.put("logoutPlugin", descriptor);

        className = "org.hippoecm.frontend.plugins.admin.editor.EditorPlugin";
        descriptor = new PluginDescriptor("contentPlugin", className, factory.createChannel());
        childrenOfRoot.put("contentPlugin", descriptor);

        className = "org.hippoecm.frontend.plugins.admin.breadcrumb.BreadcrumbPlugin";
        descriptor = new PluginDescriptor("breadcrumbPlugin", className, factory.createChannel());
        childrenOfRoot.put("breadcrumbPlugin", descriptor);
    }

    public PluginDescriptor getRoot() {
        return root;
    }

    public List<PluginDescriptor> getChildren(String pluginId) {
        List<PluginDescriptor> result = new ArrayList<PluginDescriptor>();
        if (pluginId.equals("rootPlugin")) {
            result.addAll(childrenOfRoot.values());
        }
        return result;
    }

    public PluginDescriptor getPlugin(String pluginId) {
        if (pluginId.equals("rootPlugin")) {
            return root;
        }
        return childrenOfRoot.get(pluginId);
    }

    public ChannelFactory getChannelFactory() {
        return factory;
    }
}
