/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.dashboard.todo;

import java.io.Serializable;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.dashboard.BrowseLink;
import org.hippoecm.frontend.plugins.cms.dashboard.BrowseLinkTarget;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TodoPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TodoPlugin.class);

    public TodoPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        if (!(getDefaultModel() instanceof IDataProvider)) {
            throw new IllegalArgumentException("TodoPlugin needs an IDataProvider as Plugin model.");
        }
        add(new TodoView("view", getDefaultModel()));
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        redraw();
    }

    private class TodoView extends RefreshingView {
        private static final long serialVersionUID = 1L;

        public TodoView(String id, IModel model) {
            super(id, model);
        }

        @Override
        protected Iterator getItemModels() {
            final IDataProvider dataProvider = (IDataProvider) getDefaultModel();
            return dataProvider.iterator(0, 0);
        }

        @Override
        protected void populateItem(final Item item) {
            item.add(new AttributeModifier("class", true, new AbstractReadOnlyModel() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object getObject() {
                    return (item.getIndex() % 2 == 1) ? "even" : "odd";
                }
            }));

            try {
                Node node = (Node) item.getModelObject();
                BrowseLinkTarget target = new BrowseLinkTarget(node.getPath());
                item.add(new BrowseLink(getPluginContext(), getPluginConfig(), "request", target,
                        new PropertyModel<String>(target, "name")));

                Request request = new Request(item.getModel(), this);
                item.add(new Label("request-description", new PropertyModel(request, "localType")));
                item.add(new Label("request-owner", new PropertyModel(request, "username")));
            } catch (RepositoryException e) {
                log.error("Failed to create todo item from publication request node", e);
            }
        }
    }

    private static class Request implements Serializable {

        private final IModel<Node> nodeModel;
        private final Component container;

        private Request(IModel<Node> nodeModel, Component container) {
            this.nodeModel = nodeModel;
            this.container = container;
        }

        public String getLocalType() {
            try {
                Node node = nodeModel.getObject();
                return new StringResourceModel(node.getProperty("hippostdpubwf:type").getString(), container, null).getString();
            } catch (RepositoryException ignored) {
            }
            return null;
        }

        public String getUsername() {
            try {
                Node node = nodeModel.getObject();
                return node.getProperty("hippostdpubwf:username").getString();
            } catch (RepositoryException ignored) {
            }
            return null;
        }

    }
}
