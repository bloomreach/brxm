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
package org.hippoecm.frontend.yui.dragdrop;

import java.util.List;

import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;

public abstract class PluginDragDropBehavior extends AbstractDragDropBehavior {
    private static final long serialVersionUID = 1L;
    private Plugin plugin;

    public PluginDragDropBehavior(String... groups) {
        super(groups);
    }

    @Override
    protected void onBind() {
        super.onBind();
        PluginDescriptor descriptor = getPlugin().getDescriptor();
        List<String> groups = descriptor.getParameter("dd-groups").getStrings();
        if (groups.size() > 0) {
            if (descriptor.getParameter("dd-groups-overwrite").getBoolean()) {
                clearGroups();
            }
            for (String group : groups) {
                addGroup(group);
            }
        }
    }

    protected Plugin getPlugin() {
        if (plugin == null) {
            if (getComponent() instanceof Plugin) {
                plugin = (Plugin) getComponent();
            } else {
                plugin = (Plugin) getComponent().findParent(Plugin.class);
            }
        }
        return plugin;
    }

}
