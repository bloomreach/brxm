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
package org.hippoecm.frontend.template;

import java.util.Map;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.PluginManager;
import org.hippoecm.frontend.plugin.channel.ChannelFactory;
import org.hippoecm.frontend.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.template.config.TemplateConfig;
import org.hippoecm.frontend.template.config.TypeConfig;
import org.hippoecm.frontend.template.model.ItemModel;
import org.hippoecm.frontend.template.model.TemplateModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class TemplateEngine implements IClusterable {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(TemplateEngine.class);

    private TypeConfig typeConfig;
    private TemplateConfig templateConfig;
    private PluginManager manager;

    public TemplateEngine(TypeConfig typeConfig, TemplateConfig templateConfig, PluginManager manager) {
        this.typeConfig = typeConfig;
        this.templateConfig = templateConfig;
        this.manager = manager;
    }

    public TypeConfig getTypeConfig() {
        return typeConfig;
    }

    public TemplateConfig getTemplateConfig() {
        return templateConfig;
    }

    public ChannelFactory getChannelFactory() {
        return manager.getChannelFactory();
    }

    public Plugin createTemplate(String wicketId, TemplateModel model, Plugin parentPlugin,
            Map<String, ParameterValue> config) {
        TemplateDescriptor templateDescriptor = model.getTemplateDescriptor();
        PluginDescriptor pluginDescriptor = templateDescriptor.getPlugin();
        pluginDescriptor.setWicketId(wicketId);

        if (config != null) {
            Map<String, ParameterValue> parameters = pluginDescriptor.getParameters();
            for (Map.Entry<String, ParameterValue> entry : config.entrySet()) {
                if (entry.getKey().startsWith("template.")) {
                    String key = entry.getKey().substring("template.".length());
                    parameters.put(key, entry.getValue());
                }
            }
        }
        return createPlugin(pluginDescriptor, model, parentPlugin);
    }

    public Plugin createItem(String wicketId, ItemModel model, Plugin parentPlugin) {
        PluginDescriptor pluginDescriptor = model.getDescriptor().getPlugin();
        pluginDescriptor.setWicketId(wicketId);

        return createPlugin(pluginDescriptor, model, parentPlugin);
    }

    private Plugin createPlugin(PluginDescriptor pluginDescriptor, IPluginModel pluginModel, Plugin parentPlugin) {
        PluginFactory pluginFactory = new PluginFactory(manager);
        Plugin plugin = pluginFactory.createPlugin(pluginDescriptor, pluginModel, parentPlugin);
        plugin.addChildren();
        return plugin;
    }

}
