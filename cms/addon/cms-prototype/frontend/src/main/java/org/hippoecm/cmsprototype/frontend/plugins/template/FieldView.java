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
package org.hippoecm.cmsprototype.frontend.plugins.template;

import javax.jcr.Node;
import javax.jcr.Property;

import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;

public class FieldView extends DataView {
    private static final long serialVersionUID = 1L;

    private TemplateDescriptor descriptor;
    private TemplateEngine engine;

    public FieldView(String wicketId, TemplateDescriptor descriptor, TemplateProvider provider, TemplateEngine engine) {
        super(wicketId, provider);

        this.descriptor = descriptor;
        this.engine = engine;
    }

    public TemplateDescriptor getTemplateDescriptor() {
        return descriptor;
    }

    public Plugin getTemplatePlugin() {
        if (engine != null) {
            return engine.getPlugin();
        }
        return null;
    }

    @Override
    protected void populateItem(Item item) {
        FieldModel fieldModel = (FieldModel) item.getModel();
        FieldDescriptor field = fieldModel.getDescriptor();

        if (field.getRenderer() != null) {
            // the field specifies a renderer, let it handle the item
            Node node = (Node) fieldModel.getObject();
            JcrNodeModel model = new JcrNodeModel((JcrNodeModel) getModel(), node);

            String className = fieldModel.getDescriptor().getRenderer();
            PluginDescriptor pluginDescriptor = new PluginDescriptor("sub", className, null, null);
            PluginFactory pluginFactory = new PluginFactory(getTemplatePlugin().getPluginManager());
            Plugin child = pluginFactory.createPlugin(pluginDescriptor, model, getTemplatePlugin());
            item.add(child);
        } else if (field.getTemplate() != null) {
            // the field specifies a template
            TemplateDescriptor descriptor = engine.getConfig().getTemplate(field.getTemplate());
            Template template = new Template("sub", null, descriptor, engine);
            item.add(template);
        } else {
            Property prop = (Property) fieldModel.getObject();
            JcrPropertyModel model = new JcrPropertyModel(prop);

            ValueTemplate template = new ValueTemplate("sub", model, field, engine);
            item.add(template);
        }
    }
}
