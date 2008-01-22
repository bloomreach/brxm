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
package org.hippoecm.frontend.plugins.template;

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.PluginModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugins.template.config.FieldDescriptor;
import org.hippoecm.frontend.plugins.template.config.TemplateConfig;
import org.hippoecm.frontend.plugins.template.config.TemplateDescriptor;
import org.hippoecm.frontend.plugins.template.model.FieldModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateEngine implements IClusterable {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(TemplateEngine.class);

    private TemplateConfig config;
    private Plugin plugin;

    public TemplateEngine(String wicketId, TemplateConfig config, Plugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public TemplateConfig getConfig() {
        return config;
    }

    public Component createTemplate(String wicketId, JcrNodeModel model, TemplateDescriptor descriptor) {
        return new Template(wicketId, model, descriptor, this);
    }
    
    public Component createTemplate(String wicketId, FieldModel fieldModel) {
        FieldDescriptor field = fieldModel.getDescriptor();
        JcrItemModel itemModel = fieldModel.getItemModel();

        if (field.isNode()) {
            if (field.isMultiple() || !field.isMandatory()) {
                // wrap multi-valued fields (i.e. same-name siblings or optional fields)
                // in a MultiTemplate.  Fields can thus be added, removed and ordered.
                return new MultiTemplate(wicketId, fieldModel, this);

            } else {
                // for nodes, a template must be defined for the node type.
                // the field specifies a template
                TemplateDescriptor descriptor = getConfig().getTemplate(field.getType());
                descriptor.setName(field.getName());
                return createTemplate(wicketId, new JcrNodeModel(fieldModel.getChildModel()), descriptor);
            }
        } else {
            if (field.getRenderer() != null) {
                // the field specifies a renderer, instantiate the plugin with the parent of the
                // node.  The field description is passed with initTemplatePlugin call.

                // create a new channel
                // FIXME: should the outgoing channel be shared between plugins?
                Channel outgoing = getPlugin().getPluginManager().getChannelFactory().createChannel();

                // instantiate the plugin that should handle the field

                // template does not apply to parent of root => parent exists
                PluginModel pluginModel = new PluginModel();
                pluginModel.putAll(new JcrNodeModel(itemModel).getMapRepresentation());
                pluginModel.put("field", field.getMapRepresentation());

                String className = field.getRenderer();
                PluginDescriptor pluginDescriptor = new PluginDescriptor(wicketId, className, outgoing);
                PluginFactory pluginFactory = new PluginFactory(getPlugin().getPluginManager());
                return pluginFactory.createPlugin(pluginDescriptor, pluginModel, getPlugin());

            } else {
                JcrPropertyModel model = new JcrPropertyModel(itemModel.getPath() + "/" + field.getPath());
                return new ValueTemplate(wicketId, model, field, this);
            }
        }
    }
}
