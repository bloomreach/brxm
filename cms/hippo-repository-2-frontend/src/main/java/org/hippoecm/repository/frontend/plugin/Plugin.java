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
package org.hippoecm.repository.frontend.plugin;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.repository.frontend.model.JcrNodeModel;
import org.hippoecm.repository.frontend.plugin.config.PluginConfig;
import org.hippoecm.repository.frontend.plugin.config.PluginDescriptor;

public abstract class Plugin extends Panel {

    public Plugin(String id, JcrNodeModel model) {
        super(id, model);
        setOutputMarkupId(true);
    }

    public void addChildren(PluginConfig pluginConfig) {
        List children = pluginConfig.getChildren(new PluginDescriptor(this));
        Iterator it = children.iterator();
        while (it.hasNext()) {
            PluginDescriptor childDescriptor = (PluginDescriptor) it.next();
            Plugin child = new PluginFactory(childDescriptor).getPlugin((JcrNodeModel) getModel());
            add(child);
            child.addChildren(pluginConfig);
        }
    }

    public abstract void update(AjaxRequestTarget target, JcrNodeModel model);

}
