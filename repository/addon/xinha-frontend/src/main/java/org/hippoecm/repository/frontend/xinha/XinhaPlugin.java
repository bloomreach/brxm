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
package org.hippoecm.repository.frontend.xinha;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugins.template.FieldDescriptor;
import org.hippoecm.frontend.plugins.template.ITemplatePlugin;
import org.hippoecm.frontend.plugins.template.TemplateEngine;

public class XinhaPlugin extends Plugin implements ITemplatePlugin {
    private static final long serialVersionUID = 1L;

    private FieldDescriptor descriptor;
    private TemplateEngine engine;
    private XinhaEditor widget;
    private JcrPropertyValueModel valueModel;

    public XinhaPlugin(PluginDescriptor descriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(descriptor, model, parentPlugin);

        add(new Label("name", new PropertyModel(this, "name")));

        add(widget = new XinhaEditor("editor", null));
    }

    public String getName() {
        if(descriptor != null) {
            return descriptor.getName();
        }
        return null;
    }

    public void initTemplatePlugin(FieldDescriptor descriptor, TemplateEngine engine) {
        this.descriptor = descriptor;
        this.engine = engine;

        setModel(getModel());
    }

    @Override
    public Component setModel(IModel model) {
        JcrNodeModel jcrModel = (JcrNodeModel) model;
        try {
            Property property = jcrModel.getNode().getProperty(descriptor.getPath());
            if (property.getDefinition().isMultiple()) {
                System.err.println("error: property is multivalued");
                widget.setModel(null);
                return super.setModel(model);
            }
            JcrPropertyModel propModel = new JcrPropertyModel(property);
            widget.setModel(new JcrPropertyValueModel(0, property.getValue().getString(), propModel));
        } catch (RepositoryException ex) {
            ex.printStackTrace();
        }
        return super.setModel(model);
    }
}
