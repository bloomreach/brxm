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
package org.hippoecm.frontend.plugin.composite;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.core.impl.PluginConfig;
import org.hippoecm.frontend.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.plugin.render.RenderPlugin;
import org.hippoecm.frontend.service.IDynamicService;
import org.hippoecm.frontend.service.IFactoryService;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.util.ServiceTracker;

public abstract class Perspective extends RenderPlugin implements ITitleDecorator, IDynamicService {
    private static final long serialVersionUID = 1L;

    public static final String TITLE = "perspective.title";
    public static final String PLUGINS = "perspective.plugins";

    private ServiceTracker<IFactoryService> factory;
    private List<Plugin> plugins;
    private String title = "title";

    public Perspective() {
        plugins = new LinkedList<Plugin>();
        factory = new ServiceTracker<IFactoryService>(IFactoryService.class);
    }

    @Override
    public void init(PluginContext context, String serviceId, Map<String, ParameterValue> properties) {
        super.init(context, serviceId, properties);

        if (properties.get(TITLE) != null) {
            title = properties.get(TITLE).getStrings().get(0);
        }

        if (properties.get(Plugin.FACTORY_ID) != null) {
            factory.open(context, properties.get(Plugin.FACTORY_ID).getStrings().get(0));
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

        factory.close();

        title = "title";

        super.destroy();
    }

    // ITitleDecorator

    public String getTitle() {
        return title;
    }

    // IDynamicService

    public boolean canDelete() {
        return (factory.getServices().size() > 0);
    }

    public void delete() {
        if (factory.getServices().size() > 0) {
            IFactoryService factoryService = factory.getServices().get(0);
            factoryService.delete(this);
        }
    }

}
