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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hippoecm.frontend.plugin.EventChannel;
import org.hippoecm.frontend.plugin.PluginDescriptor;

/**
 * Hardcoded plugin configuration. 
 * It uses only core plugins and shows the Hippo ECM Admin Console.
 */
public class PluginJavaConfig implements PluginConfig {
    private static final long serialVersionUID = 1L;

    private PluginDescriptor root;
    private Map<String, PluginDescriptor> childrenOfRoot;

    public PluginJavaConfig() {
        EventChannel defaultChannel = new EventChannel("default");
        Set<EventChannel> incoming = new HashSet<EventChannel>();
        incoming.add(defaultChannel);
        Set<EventChannel> outgoing = new HashSet<EventChannel>();
        outgoing.add(defaultChannel);

        String className = "org.hippoecm.frontend.plugins.admin.RootPlugin";
        root = new PluginDescriptor("rootPlugin", className, incoming, outgoing);

        childrenOfRoot = new HashMap<String, PluginDescriptor>();

        className = "org.hippoecm.frontend.plugins.admin.browser.BrowserPlugin";
        childrenOfRoot.put("navigationPlugin", new PluginDescriptor("navigationPlugin", className, incoming, outgoing));

        className = "org.hippoecm.frontend.plugins.admin.menu.MenuPlugin";
        childrenOfRoot.put("menuPlugin", new PluginDescriptor("menuPlugin", className, incoming, outgoing));

        className = "org.hippoecm.frontend.plugins.admin.logout.LogoutPlugin";
        childrenOfRoot.put("logoutPlugin", new PluginDescriptor("logoutPlugin", className, incoming, outgoing));

        className = "org.hippoecm.frontend.plugins.admin.editor.EditorPlugin";
        childrenOfRoot.put("contentPlugin", new PluginDescriptor("contentPlugin", className, incoming, outgoing));

        className = "org.hippoecm.frontend.plugins.admin.breadcrumb.BreadcrumbPlugin";
        childrenOfRoot.put("breadcrumbPlugin", new PluginDescriptor("breadcrumbPlugin", className, incoming, outgoing));
    }

    public PluginDescriptor getRoot() {
        return root;
    }

    public List getChildren(PluginDescriptor pluginDescriptor) {
        List result = new ArrayList();
        if (pluginDescriptor.getPluginId().equals("rootPlugin")) {
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
}
