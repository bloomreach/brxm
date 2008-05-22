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
package org.hippoecm.frontend.sa.plugin.perspective;

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.sa.plugin.IPlugin;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.sa.plugin.impl.RenderPlugin;
import org.hippoecm.frontend.sa.service.ITitleDecorator;
import org.hippoecm.frontend.sa.service.IViewService;
import org.hippoecm.frontend.sa.service.ServiceTracker;

public abstract class Perspective extends RenderPlugin implements ITitleDecorator, IViewService {
    private static final long serialVersionUID = 1L;

    public static final String TITLE = "perspective.title";
    public static final String PLUGINS = "perspective.plugins";

    private List<IPlugin> plugins;
    private String title = "title";
    private ServiceTracker<IPluginConfigService> pluginConfigTracker;

    public Perspective() {
        plugins = new LinkedList<IPlugin>();
        pluginConfigTracker = new ServiceTracker<IPluginConfigService>(IPluginConfigService.class);
    }

    @Override
    public void init(IPluginContext context, IPluginConfig properties) {
        super.init(context, properties);

        if (properties.getString(TITLE) != null) {
            title = properties.getString(TITLE);
        }
    }
    
    @Override
    public void start(IPluginContext context) {
        super.start(context);
        
        // TODO: uncomment this when IPluginConfigService.getPlugins(String key)
        // actually uses the key, currently it returns ALL configured IPluginConfigs
        
//        pluginConfigTracker.open(context, "service.plugin.config");
//        IPluginConfigService pluginConfigService = pluginConfigTracker.getService();
//        for (IPluginConfig config : pluginConfigService.getPlugins(PLUGINS)) {
//            IPlugin plugin = context.start(config); 
//            plugins.add(plugin);
//            plugin.start(context);
//        }
    }

    @Override
    public void destroy() {
        for (IPlugin plugin : plugins) {
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
