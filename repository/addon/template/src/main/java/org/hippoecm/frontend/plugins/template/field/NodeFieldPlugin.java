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

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.template.FieldDescriptor;
import org.hippoecm.frontend.template.ItemDescriptor;
import org.hippoecm.frontend.template.config.TypeConfig;
import org.hippoecm.frontend.template.model.ItemModel;
import org.hippoecm.frontend.template.model.NodeTemplateProvider;
import org.hippoecm.frontend.template.model.TemplateModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeFieldPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(NodeFieldPlugin.class);

    private FieldDescriptor field;
    private NodeTemplateProvider provider;
    private TemplateView view;

    public NodeFieldPlugin(PluginDescriptor pluginDescriptor, IPluginModel pluginModel, Plugin parentPlugin) {
        super(pluginDescriptor, new ItemModel(pluginModel), parentPlugin);

        ItemModel model = (ItemModel) getPluginModel();
        ItemDescriptor descriptor = (ItemDescriptor) model.getDescriptor();

        TypeConfig config = parentPlugin.getPluginManager().getTemplateEngine().getTypeConfig(); 
        field = config.getTypeDescriptor(descriptor.getType()).getField(descriptor.getField());

        List<String> captions = pluginDescriptor.getParameter("caption");
        if(captions != null && captions.size() > 0) {
            add(new Label("name", captions.get(0)));
        } else {
            add(new Label("name", ""));
        }

        provider = new NodeTemplateProvider(field, getPluginManager().getTemplateEngine(), model.getNodeModel());
        view = new TemplateView("field", provider, this, provider.getDescriptor());
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

    public void onAddNode(AjaxRequestTarget target) {
        provider.addNew();

        // refresh
        replace(createAddLink());

        if (target != null) {
            target.addComponent(this);
        }
    }

    public void onRemoveNode(TemplateModel childModel, AjaxRequestTarget target) {
        provider.remove(childModel);

        // refresh
        replace(createAddLink());

        if (target != null) {
            target.addComponent(this);
        }
    }

    public void onMoveNodeUp(TemplateModel model, AjaxRequestTarget target) {
        provider.moveUp(model);

        if (target != null) {
            target.addComponent(this);
        }
    }

    protected Component createAddLink() {
        if (field.isMultiple() || (provider.size() == 0)) {
            return new AjaxLink("add") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    NodeFieldPlugin.this.onAddNode(target);
                }
            };
        } else {
            return new Label("add", "");
        }
    }
}
