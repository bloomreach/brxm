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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.template.TemplateDescriptor;
import org.hippoecm.frontend.template.model.TemplateModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyTemplatePlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PropertyTemplatePlugin.class);

    private JcrPropertyModel propertyModel;
    private TemplateDescriptor descriptor;

    public PropertyTemplatePlugin(PluginDescriptor pluginDescriptor, IPluginModel pluginModel, Plugin parentPlugin) {
        super(pluginDescriptor, new TemplateModel(pluginModel, parentPlugin.getPluginManager().getTemplateEngine()),
                parentPlugin);

        TemplateModel model = (TemplateModel) getPluginModel();
        descriptor = model.getTemplateDescriptor();

        propertyModel = new JcrPropertyModel(model.getNodeModel().getItemModel().getPath() + "/" + model.getPath());

        add(createAddLink(propertyModel));
        add(new ValueView("values", propertyModel, descriptor.isMultiple()));

        setOutputMarkupId(true);
    }

    @Override
    public void onDetach() {
        propertyModel.detach();
        super.onDetach();
    }

    // Called when a new value is added to a multi-valued property

    protected void onAddValue(AjaxRequestTarget target) {
        TemplateModel model = (TemplateModel) getPluginModel();
        try {
            Property prop = propertyModel.getProperty();
            Value value = descriptor.createValue("");
            if (prop != null) {
                Value[] oldValues = prop.getValues();
                Value[] newValues = new Value[oldValues.length + 1];
                for (int i = 0; i < oldValues.length; i++) {
                    newValues[i] = oldValues[i];
                }
                newValues[oldValues.length] = value;
                prop.setValue(newValues);
            } else {
                // get the path to the node
                JcrItemModel itemModel = propertyModel.getItemModel();
                Node parent = (Node) itemModel.getParentModel().getObject();
                if (descriptor.isMultiple()) {
                    parent.setProperty(model.getPath(), new Value[] { value });
                } else {
                    parent.setProperty(model.getPath(), value);
                }

                // use a fresh model; the property has to be re-retrieved
                propertyModel.detach();
            }

            // update labels/links
            replace(createAddLink(propertyModel));

            if (target != null) {
                target.addComponent(this);
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }

    protected void onRemoveValue(AjaxRequestTarget target, JcrPropertyValueModel model) {
        try {
            Property prop = propertyModel.getProperty();
            Value[] values = prop.getValues();
            values = (Value[]) ArrayUtils.remove(values, model.getIndex());
            if (values.length > 0) {
                prop.setValue(values);
            } else {
                log.info("removing empty array property");
                prop.remove();
            }

            Channel channel = getDescriptor().getIncoming();
            Request request = channel.createRequest("flush", ((TemplateModel) getPluginModel()).getNodeModel());
            if (values.length > 0) {
                request.getContext().addRefresh(this);
            }
            channel.send(request);

            request.getContext().apply(target);
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }

    // privates

    private Component createAddLink(final JcrPropertyModel model) {
        String id = "add";
        if (descriptor.isMultiple() || !model.getItemModel().exists()) {
            return new AjaxLink(id, model) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    PropertyTemplatePlugin.this.onAddValue(target);
                }
            };
        } else {
            return new Label("add", "");
        }
    }
}
