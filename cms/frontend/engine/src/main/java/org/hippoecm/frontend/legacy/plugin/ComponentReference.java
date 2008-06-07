/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.legacy.plugin;

import org.apache.wicket.Component;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * @deprecated use org.hippoecm.frontend.sa.* instead
 */
@Deprecated
public class ComponentReference extends LoadableDetachableModel {
    private static final long serialVersionUID = 1L;

    private PluginReference pluginRef;
    private String path;

    public ComponentReference(Component component) {
        super(component);

        Plugin plugin = (Plugin) component.findParent(Plugin.class);
        if (plugin == null) {
            throw new IllegalArgumentException("Component isn't attached to a plugin");
        }

        this.pluginRef = new PluginReference(plugin);
        this.path = "";
        Component parent = component;
        while (parent != plugin) {
            this.path = parent.getId() + ":" + this.path;
            parent = parent.getParent();
        }
    }

    @Override
    protected Object load() {
        Plugin plugin = pluginRef.getPlugin();
        return plugin.get(path);
    }
}
