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
package org.hippoecm.frontend.plugins.template.builder;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.model.PluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.plugin.channel.Channel;
import org.hippoecm.frontend.legacy.plugin.channel.Request;
import org.hippoecm.frontend.legacy.template.ItemDescriptor;
import org.hippoecm.frontend.legacy.template.TemplateEngine;
import org.hippoecm.frontend.legacy.template.model.ItemModel;
import org.hippoecm.repository.api.HippoNodeType;

public class PreviewItemPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    Plugin item;

    public PreviewItemPlugin(PluginDescriptor pluginDescriptor, IPluginModel pluginModel, Plugin parentPlugin) {
        super(pluginDescriptor, new ItemModel(pluginModel), parentPlugin);

        ItemModel model = (ItemModel) getPluginModel();
        ItemDescriptor descriptor = model.getDescriptor();
        List<ItemDescriptor> children = descriptor.getItems();
        assert (children.size() == 1);

        ItemDescriptor child = new ItemChildrenFilter(children.get(0));
        final ItemModel itemModel = new ItemModel(child, model.getNodeModel());
        TemplateEngine engine = parentPlugin.getPluginManager().getTemplateEngine();
        add(item = engine.createItem("item", itemModel, this));

        add(new AjaxLink("up") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                sendRequest("up", target);
            }
        });
        add(new AjaxLink("down") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                sendRequest("down", target);
            }
        });
        add(new AjaxLink("edit") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                sendRequest("focus", target);
            }
        });
        add(new AjaxLink("remove") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                sendRequest("remove", target);
            }
        });
    }

    protected void sendRequest(String operation, AjaxRequestTarget target) {
        Channel channel = getTopChannel();
        PluginModel model = new PluginModel();
        model.put("plugin", getPluginPath());
        Request request = channel.createRequest(operation, model);
        channel.send(request);
        request.getContext().apply(target);
    }

    public static class ItemFilter extends ItemDescriptor {
        private static final long serialVersionUID = 1L;

        ItemDescriptor delegate;

        public ItemFilter(ItemDescriptor delegate) {
            super(delegate.getId(), new PluginDescriptor(HippoNodeType.HIPPO_ITEM, PreviewItemPlugin.class.getName()),
                    delegate.getMode());

            this.delegate = delegate;
        }

        @Override
        public List<ItemDescriptor> getItems() {
            List<ItemDescriptor> children = new ArrayList<ItemDescriptor>(1);
            children.add(delegate);
            return children;
        }
    }

    static class ItemChildrenFilter extends ItemDescriptor {
        private static final long serialVersionUID = 1L;

        ItemDescriptor delegate;

        ItemChildrenFilter(ItemDescriptor delegate) {
            super(delegate.getMapRepresentation());

            this.delegate = delegate;
        }

        @Override
        public List<ItemDescriptor> getItems() {
            List<ItemDescriptor> children = delegate.getItems();
            List<ItemDescriptor> filtered = new ArrayList<ItemDescriptor>(children.size());
            for (ItemDescriptor child : children) {
                filtered.add(new ItemFilter(child));
            }
            return filtered;
        }
    }
}
