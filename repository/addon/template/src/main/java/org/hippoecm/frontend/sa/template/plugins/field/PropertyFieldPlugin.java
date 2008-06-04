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
package org.hippoecm.frontend.sa.template.plugins.field;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.service.IRenderService;
import org.hippoecm.frontend.sa.template.FieldDescriptor;
import org.hippoecm.frontend.sa.template.TypeDescriptor;
import org.hippoecm.frontend.sa.template.model.AbstractProvider;
import org.hippoecm.frontend.sa.template.model.ValueTemplateProvider;
import org.hippoecm.frontend.template.config.TemplateConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyFieldPlugin extends FieldPlugin<JcrNodeModel, JcrPropertyValueModel> {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PropertyFieldPlugin.class);

    public PropertyFieldPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        updateProvider();

        String caption = config.getString("caption");
        add(new Label("name", caption));
        add(createAddLink());
    }

    @Override
    protected AbstractProvider<JcrPropertyValueModel> newProvider(FieldDescriptor descriptor, TypeDescriptor type,
            JcrNodeModel nodeModel) {
        if (!descriptor.getPath().equals("*")) {
            JcrItemModel itemModel = new JcrItemModel(nodeModel.getItemModel().getPath() + "/" + descriptor.getPath());
            return new ValueTemplateProvider(descriptor, type, itemModel);
        }
        return null;
    }

    @Override
    public void onModelChanged() {
        updateProvider();
        replace(createAddLink());
    }

    @Override
    protected void onAddRenderService(Item item, IRenderService renderer) {
        final JcrPropertyValueModel model = (JcrPropertyValueModel) renderer.getComponent().getModel();

        MarkupContainer remove = new AjaxLink("remove") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onRemoveItem(model, target);
            }
        };
        if (!TemplateConfig.EDIT_MODE.equals(mode) || (field == null) || field.isMandatory()) {
            remove.setVisible(false);
        }
        item.add(remove);

        MarkupContainer upLink = new AjaxLink("up") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onMoveItemUp(model, target);
            }
        };
        if (!TemplateConfig.EDIT_MODE.equals(mode) || (field == null) || !field.isOrdered()) {
            upLink.setVisible(false);
        }
        if (item.getIndex() == 0) {
            upLink.setEnabled(false);
        }
        item.add(upLink);
    }

    // privates

    protected Component createAddLink() {
        if (TemplateConfig.EDIT_MODE.equals(mode) && (field != null) && (field.isMultiple() || (provider.size() == 0))) {
            return new AjaxLink("add") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    PropertyFieldPlugin.this.onAddItem(target);
                }
            };
        } else {
            return new Label("add", "");
        }
    }
}
