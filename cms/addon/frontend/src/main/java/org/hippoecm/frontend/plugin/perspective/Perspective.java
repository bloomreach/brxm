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
package org.hippoecm.frontend.plugin.perspective;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.core.impl.PluginConfig;
import org.hippoecm.frontend.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.plugin.render.RenderPlugin;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.service.IViewService;

public abstract class Perspective extends RenderPlugin implements ITitleDecorator, IViewService {
    private static final long serialVersionUID = 1L;

    public static final String TITLE = "perspective.title";
    public static final String PLUGINS = "perspective.plugins";

    private List<Plugin> plugins;
    private String title = "title";

    public Perspective() {
        plugins = new LinkedList<Plugin>();
    }

    @Override
    public void init(PluginContext context, Map<String, ParameterValue> properties) {
        super.init(context, properties);

        if (properties.get(TITLE) != null) {
            title = properties.get(TITLE).getStrings().get(0);
        }

        if (properties.get(PLUGINS) != null) {
            Map<String, ParameterValue> pluginConfigs = properties.get(PLUGINS).getMap();
            for (Map.Entry<String, ParameterValue> entry : pluginConfigs.entrySet()) {
                PluginConfig config = new PluginConfig();
                config.putAll(entry.getValue().getMap());

                plugins.add(context.start(config));
            }
        }
    }

    @Override
    public void destroy() {
        for (Plugin plugin : plugins) {
            plugin.stop();
            plugins.remove(plugin);
        }

        title = "title";

        super.destroy();
    }

    // ITitleDecorator

    public String getTitle() {
        return title;
    }

    // IViewService

    public void view(IModel model) {
        setModel(model);
    }

}
