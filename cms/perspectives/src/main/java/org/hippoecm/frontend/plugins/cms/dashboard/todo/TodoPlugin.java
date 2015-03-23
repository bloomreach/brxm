/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Component;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.dashboard.BrowseLinkTarget;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TodoPlugin extends RenderPlugin {

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
            try {
                @SuppressWarnings("unchecked")
                final IModel<Node> nodeModel = item.getModel();
                final BrowseLinkTarget target = new BrowseLinkTarget(nodeModel.getObject().getPath());
                final Request request = new Request(nodeModel, this);
                item.add(new TodoLink(getPluginContext(), getPluginConfig(), "link", target,
                        new PropertyModel<>(request, "username"),
                        new PropertyModel<>(request, "localType")));
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

        @SuppressWarnings("unused")
        public String getLocalType() {
            try {
                Node node = nodeModel.getObject();
                String type = node.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_TYPE).getString();
                return container.getString(type);
            } catch (RepositoryException ignored) {
            }
            return null;
        }

        public String getUsername() {
            try {
                Node node = nodeModel.getObject();
                return node.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME).getString();
            } catch (RepositoryException ignored) {
            }
            return null;
        }

    }
}
