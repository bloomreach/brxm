/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.plugins.template.field;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.plugin.channel.Notification;
import org.hippoecm.frontend.legacy.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.legacy.template.FieldDescriptor;
import org.hippoecm.frontend.legacy.template.ItemDescriptor;
import org.hippoecm.frontend.legacy.template.config.TemplateConfig;
import org.hippoecm.frontend.legacy.template.model.ItemModel;
import org.hippoecm.frontend.legacy.template.model.TemplateModel;
import org.hippoecm.frontend.legacy.template.model.ValueTemplateProvider;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyFieldPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PropertyFieldPlugin.class);

    private FieldDescriptor field;
    private ValueTemplateProvider provider;
    private ValueView view;
    private String mode;

    public PropertyFieldPlugin(PluginDescriptor pluginDescriptor, IPluginModel pluginModel, Plugin parentPlugin) {
        super(pluginDescriptor, new ItemModel(pluginModel), parentPlugin);

        ItemModel model = (ItemModel) getPluginModel();
        ItemDescriptor descriptor = (ItemDescriptor) model.getDescriptor();

        field = descriptor.getTemplate().getTypeDescriptor().getField(descriptor.getField());
        mode = descriptor.getMode();

        ParameterValue captionValue = pluginDescriptor.getParameter("caption");
        if (captionValue != null && captionValue.getStrings() != null && captionValue.getStrings().size() > 0) {
            add(new Label("name", captionValue.getStrings().get(0)));
        } else {
            add(new Label("name", ""));
        }

        provider = new ValueTemplateProvider(field, getPluginManager().getTemplateEngine(), model.getNodeModel(), mode,
                field.getPath());
        view = new ValueView("values", provider, this, pluginDescriptor.getParameters(), mode);
        add(view);

        add(createAddLink());

        setOutputMarkupId(true);
    }

    @Override
    public void receive(Notification notification) {
        if ("flush".equals(notification.getOperation())) {
            ItemModel fieldModel = (ItemModel) getPluginModel();
            String currentPath = fieldModel.getNodeModel().getItemModel().getPath();

            // refresh the provider if the sent node is a subnode
            JcrNodeModel model = new JcrNodeModel(notification.getModel());
            String path = model.getItemModel().getPath();
            if (path.length() >= currentPath.length()) {
                if (currentPath.equals(path.substring(0, path.length()))) {
                    provider.refresh();
                    notification.getContext().addRefresh(this);

                    replace(createAddLink());
                    view.populate();
                }
            }
        }
        super.receive(notification);
    }

    // Called when a new value is added to a multi-valued property

    protected void onAddValue(AjaxRequestTarget target) {
        provider.addNew();

        // update labels/links
        replace(createAddLink());

        if (target != null) {
            target.addComponent(this);
        }
    }

    protected void onRemoveValue(AjaxRequestTarget target, TemplateModel model) {
        provider.remove(model);

        // update labels/links
        replace(createAddLink());

        if (target != null) {
            target.addComponent(this);
        }
    }

    // privates

    protected Component createAddLink() {
        if (TemplateConfig.EDIT_MODE.equals(mode) && (field.isMultiple() || (provider.size() == 0))) {
            return new AjaxLink("add") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    PropertyFieldPlugin.this.onAddValue(target);
                }
            };
        } else {
            return new Label("add", "");
        }
    }
}
