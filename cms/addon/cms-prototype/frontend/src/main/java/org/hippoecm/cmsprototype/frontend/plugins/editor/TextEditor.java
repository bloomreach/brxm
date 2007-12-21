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
package org.hippoecm.cmsprototype.frontend.plugins.editor;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugins.template.FieldDescriptor;
import org.hippoecm.frontend.plugins.template.ITemplatePlugin;
import org.hippoecm.frontend.plugins.template.TemplateEngine;
import org.hippoecm.frontend.widgets.TextAreaWidget;

public class TextEditor extends Plugin implements ITemplatePlugin {
    private static final long serialVersionUID = 1L;

    private FieldDescriptor descriptor;
    private TemplateEngine engine;
    private TextAreaWidget widget;
    private Wrapper valueModel;

    public TextEditor(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        add(new Label("name", new PropertyModel(this, "name")));

        valueModel = new Wrapper();
        widget = new TextAreaWidget("editor", valueModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onUpdate(AjaxRequestTarget target) {
                if (TextEditor.this.engine != null) {
                    TextEditor.this.engine.onChange(target);
                }
            }
        };
        add(widget);
    }

    public String getName() {
        if(descriptor != null)
            return descriptor.getName();
        return "[ property name ]";
    }
    
    // implement ITemplatePlugin

    public void initTemplatePlugin(FieldDescriptor descriptor, TemplateEngine engine) {
        this.engine = engine;
        this.descriptor = descriptor;
        setModel(getNodeModel());
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
            valueModel.setChainedModel(new JcrPropertyValueModel(0, property.getValue().getString(), propModel));
        } catch (RepositoryException ex) {
            ex.printStackTrace();
        }
        return super.setModel(model);
    }

    // Helper class to wrap the JcrPropertyValueModel
    // It is given to the TextAreaWidget; the chained model is swapped
    // when a different JcrPropertyValueModel should be used for the
    // widget.
    private class Wrapper extends Model implements IChainingModel {
        private static final long serialVersionUID = 1L;

        private JcrPropertyValueModel model;

        Wrapper() {
            model = null;
        }

        public Object getObject() {
            if(model != null)
                return model.getObject();
            return null;
        }

        public void setObject(Object object) {
            if(model != null)
                model.setObject(object);
        }

        public void setChainedModel(IModel model) {
            this.model = (JcrPropertyValueModel) model;
        }

        public IModel getChainedModel() {
            return model;
        }
    }
}
