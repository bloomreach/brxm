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
package org.hippoecm.frontend.plugins.template;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.template.FieldDescriptor;
import org.hippoecm.frontend.template.config.TypeConfig;
import org.hippoecm.frontend.template.model.ItemModel;
import org.hippoecm.frontend.template.model.WildcardFieldProvider;
import org.hippoecm.frontend.template.model.WildcardModel;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListFieldPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ListFieldPlugin.class);

    private WildcardFieldProvider provider;

    public ListFieldPlugin(PluginDescriptor pluginDescriptor, IPluginModel fieldModel, Plugin parentPlugin) {
        super(pluginDescriptor, new ItemModel(fieldModel), parentPlugin);

        ItemModel model = (ItemModel) getPluginModel();
        FieldDescriptor descriptor = (FieldDescriptor) model.getDescriptor();
        TypeConfig config = getPluginManager().getTemplateEngine().getTypeConfig();

        add(new Label("name", descriptor.getName()));

        provider = new WildcardFieldProvider(descriptor, config, model.getNodeModel());
        add(new ListView("items", provider));

        add(new AjaxLink("add") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onAddNode(target);
            }
        });

        setOutputMarkupId(true);
    }

    public void onAddNode(AjaxRequestTarget target) {
        provider.addNew();

        JcrNodeModel nodeModel = ((ItemModel) getPluginModel()).getNodeModel();

        Channel channel = getTopChannel();
        Request request = channel.createRequest("flush", nodeModel);
        channel.send(request);

        request.getContext().addRefresh(this);
        request.getContext().apply(target);
    }

    public void onRemoveNode(WildcardModel childModel, AjaxRequestTarget target) {
        provider.remove(childModel);

        JcrNodeModel nodeModel = ((ItemModel) getPluginModel()).getNodeModel();

        Channel channel = getTopChannel();
        Request request = channel.createRequest("flush", nodeModel);
        channel.send(request);

        request.getContext().addRefresh(this);
        request.getContext().apply(target);
    }

    public void onEdit(WildcardModel childModel, AjaxRequestTarget target) {
        try {
            JcrNodeModel nodeModel;
            String path = childModel.getPath();
            Node parent = childModel.getNodeModel().getNode();
            nodeModel = new JcrNodeModel(parent.getNode(path));

            Channel channel = getTopChannel();
            Request request = channel.createRequest("edit", nodeModel);
            channel.send(request);

            request.getContext().apply(target);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    class ListView extends AbstractView {
        private static final long serialVersionUID = 1L;

        public ListView(String wicketId, WildcardFieldProvider provider) {
            super(wicketId, provider, ListFieldPlugin.this);
        }

        @Override
        protected void populateItem(Item item) {
            final WildcardModel model = (WildcardModel) item.getModel();

            item.add(new AjaxLink("edit") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    ListFieldPlugin.this.onEdit(model, target);
                }
            });

            item.add(new TextFieldWidget("item-name", new IModel() {
                private static final long serialVersionUID = 1L;

                public Object getObject() {
                    return model.getPath();
                }

                public void setObject(Object object) {
                    model.setPath((String) object);
                }

                public void detach() {
                }
            }));

            item.add(new AjaxLink("remove") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    ListFieldPlugin.this.onRemoveNode(model, target);
                }
            });
        }
    }

}
